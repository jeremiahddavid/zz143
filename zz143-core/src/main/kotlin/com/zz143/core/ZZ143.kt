package com.zz143.core

import android.content.Context
import com.zz143.core.event.EventBatchCollector
import com.zz143.core.event.EventBus
import com.zz143.core.event.EventEncoder
import com.zz143.core.id.UlidGenerator
import com.zz143.core.model.*
import com.zz143.core.storage.FileQueue
import com.zz143.core.storage.FileQueueDrainer
import com.zz143.core.storage.ZZ143Database
import com.zz143.core.threading.ZZ143Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

object ZZ143 {
    private var _isInitialized = false
    val isInitialized: Boolean get() = _isInitialized

    private var _config = ZZ143Config()
    val config: ZZ143Config get() = _config

    // Core infrastructure (public for cross-module access within SDK)
    lateinit var eventBus: EventBus
        internal set
    lateinit var dispatchers: ZZ143Dispatchers
        internal set
    lateinit var database: ZZ143Database
        internal set
    lateinit var fileQueue: FileQueue
        internal set
    lateinit var scope: CoroutineScope
        internal set
    lateinit var appContext: Context
        internal set

    // Pipeline components
    internal lateinit var batchCollector: EventBatchCollector
    internal lateinit var fileQueueDrainer: FileQueueDrainer

    // Action registry
    private val actionRegistrations = mutableListOf<Any>()
    private val registeredActionTypes = mutableSetOf<String>()

    // Session state
    private val _sessionId = MutableStateFlow<SessionId?>(null)
    val sessionId: StateFlow<SessionId?> = _sessionId.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    // Action tracking for pattern detection (in-memory, fed to PatternEngine)
    private val recentActions = mutableListOf<SemanticAction>()
    private val recentScreenIds = mutableListOf<ScreenId>()
    private val actionTimestamps = mutableMapOf<String, MutableList<Long>>()

    // Workflow state
    private val _workflows = MutableStateFlow<List<Workflow>>(emptyList())
    val workflows: StateFlow<List<Workflow>> = _workflows.asStateFlow()

    // Suggestions
    private val _suggestions = MutableSharedFlow<Suggestion>(replay = 1, extraBufferCapacity = 8)
    val suggestions: SharedFlow<Suggestion> = _suggestions.asSharedFlow()

    private val _activeSuggestion = MutableStateFlow<Suggestion?>(null)
    val activeSuggestion: StateFlow<Suggestion?> = _activeSuggestion.asStateFlow()

    // Replay state
    private val _replayState = MutableStateFlow<ReplayStatus?>(null)
    val replayState: StateFlow<ReplayStatus?> = _replayState.asStateFlow()

    // Background jobs
    private var analysisJob: Job? = null

    // =========================================================================
    // Initialization
    // =========================================================================

    fun init(context: Context, config: ZZ143Config = ZZ143Config()) {
        if (_isInitialized) return

        appContext = context.applicationContext
        _config = config
        dispatchers = ZZ143Dispatchers()
        scope = CoroutineScope(SupervisorJob() + dispatchers.io)

        // Core infrastructure
        eventBus = EventBus(config.eventFilter)
        database = ZZ143Database(appContext)
        fileQueue = FileQueue(
            directory = File(appContext.filesDir, "zz143/queue"),
            maxTotalSize = config.maxQueueSizeMb * 1024L * 1024L,
            dispatchers = dispatchers
        )

        // Initialize file queue
        scope.launch { fileQueue.initialize() }

        // Pipeline: EventBus → BatchCollector → FileQueue
        batchCollector = EventBatchCollector(
            eventBus, EventEncoder(), fileQueue, _config, scope, dispatchers
        )
        batchCollector.start()

        // Pipeline: FileQueue → SQLite
        fileQueueDrainer = FileQueueDrainer(
            fileQueue, database, _config, scope, dispatchers
        )
        fileQueueDrainer.start()

        // Subscribe to action events for real-time pattern matching
        scope.launch(dispatchers.computation) {
            eventBus.events.collect { event ->
                if (event is ZZ143Event.Action) {
                    onActionCaptured(event.action, event.screenId)
                }
            }
        }

        // Load persisted workflows from SQLite
        scope.launch(dispatchers.io) {
            val loaded = loadWorkflowsFromDb()
            if (loaded.isNotEmpty()) {
                _workflows.value = loaded
                log("Loaded ${loaded.size} persisted workflows from database")
            }
        }

        _isInitialized = true
        log("ZZ143 initialized (v${BuildConfig.VERSION_NAME})")
    }

    fun init(context: Context, block: ZZ143Config.Builder.() -> Unit) {
        val builder = ZZ143Config.Builder()
        builder.block()
        init(context, builder.build())
    }

    // =========================================================================
    // Capture Control
    // =========================================================================

    fun startCapturing() {
        checkInitialized()
        if (_isCapturing.value) return
        _sessionId.value = SessionId(UlidGenerator.next())
        _isCapturing.value = true

        // Start periodic pattern analysis
        startAnalysisLoop()

        log("Capturing started (session: ${_sessionId.value?.value})")
    }

    fun stopCapturing() {
        if (!_isCapturing.value) return
        _isCapturing.value = false
        _sessionId.value = null
        analysisJob?.cancel()
        analysisJob = null
        log("Capturing stopped")
    }

    // =========================================================================
    // Action Tracking
    // =========================================================================

    fun registerActions(vararg targets: Any) {
        actionRegistrations.addAll(targets)
        // Scan for @WatchAction annotations to know what types are registered
        for (target in targets) {
            for (method in target::class.java.declaredMethods) {
                val annotation = method.annotations.find {
                    it.annotationClass.simpleName == "WatchAction"
                }
                if (annotation != null) {
                    try {
                        val type = annotation.annotationClass.java.getMethod("type").invoke(annotation) as String
                        registeredActionTypes.add(type)
                    } catch (_: Exception) {}
                }
            }
        }
        log("Registered ${targets.size} action targets (${registeredActionTypes.size} action types)")
    }

    fun unregisterActions(vararg targets: Any) {
        actionRegistrations.removeAll(targets.toSet())
    }

    fun getRegisteredTargets(): List<Any> = actionRegistrations.toList()

    fun trackAction(actionType: String, parameters: Map<String, String> = emptyMap()) {
        checkInitialized()
        val session = _sessionId.value ?: return
        val screenId = ScreenId("current") // Will be resolved by CaptureEngine if attached

        val action = SemanticAction(
            actionType = actionType,
            actionSource = ActionSource.ANNOTATION,
            targetElementId = null,
            parameters = parameters
        )

        val event = ZZ143Event.Action(
            eventId = UlidGenerator.next(),
            sessionId = session,
            timestampMs = System.currentTimeMillis(),
            uptimeMs = android.os.SystemClock.elapsedRealtime(),
            screenId = screenId,
            action = action
        )
        eventBus.tryEmit(event)
    }

    // =========================================================================
    // Pattern Detection (real-time, in-memory)
    // =========================================================================

    private fun onActionCaptured(action: SemanticAction, screenId: ScreenId) {
        synchronized(recentActions) {
            recentActions.add(action)
            recentScreenIds.add(screenId)

            // Track timestamps per action type for frequency analysis
            val timestamps = actionTimestamps.getOrPut(action.actionType) { mutableListOf() }
            timestamps.add(System.currentTimeMillis())

            // Keep bounded
            if (recentActions.size > 500) {
                recentActions.removeFirst()
                recentScreenIds.removeFirst()
            }
        }

        // Check for prefix match against known workflows (instant suggestions)
        checkForPrefixMatch(action)
    }

    private fun checkForPrefixMatch(latestAction: SemanticAction) {
        if (!_config.suggestionsEnabled) return

        val currentWorkflows = _workflows.value
        if (currentWorkflows.isEmpty()) return

        val recentTypes = synchronized(recentActions) {
            recentActions.takeLast(10).map { it.actionType }
        }

        for (workflow in currentWorkflows) {
            if (workflow.status !in setOf(WorkflowStatus.DETECTED, WorkflowStatus.ACCEPTED, WorkflowStatus.ACTIVE)) continue

            val workflowTypes = workflow.steps.map { it.action.actionType }

            // Check if any recent suffix matches the start of the workflow
            // e.g., recent=[..., checkout, add_to_cart], workflow=[add_to_cart, checkout]
            // We want to find that the last action "add_to_cart" matches workflow[0]
            var bestMatch = 0
            for (suffixLen in 1..workflowTypes.size.coerceAtMost(recentTypes.size)) {
                val suffix = recentTypes.takeLast(suffixLen)
                val m = matchPrefix(suffix, workflowTypes)
                if (m == suffixLen) bestMatch = m // Only count contiguous matches from suffix start
            }
            log("  Prefix: recent_last=${recentTypes.lastOrNull()} wf=$workflowTypes → bestMatch=$bestMatch")

            val minMatch = if (workflowTypes.size <= 3) 1 else 2
            if (bestMatch >= minMatch && bestMatch < workflowTypes.size) {
                val remaining = workflowTypes.size - bestMatch
                val suggestion = Suggestion(
                    suggestionId = UlidGenerator.next(),
                    workflow = workflow,
                    displayType = _config.suggestionDisplayType,
                    title = workflow.name + "?",
                    description = workflow.description,
                    estimatedTimeSavedMs = remaining * 3000L,
                    createdAtMs = System.currentTimeMillis(),
                    expiresAtMs = System.currentTimeMillis() + _config.suggestionAutoExpireMs,
                    priority = 0
                )
                _activeSuggestion.value = suggestion
                _suggestions.tryEmit(suggestion)
                log("Suggestion emitted: ${workflow.name} ($remaining steps remaining)")
            }
        }
    }

    private fun matchPrefix(recent: List<String>, workflow: List<String>): Int {
        var matched = 0
        for (i in recent.indices) {
            if (i < workflow.size && recent[i] == workflow[i]) {
                matched++
            } else {
                break
            }
        }
        return matched
    }

    // =========================================================================
    // Analysis Loop (periodic, background)
    // =========================================================================

    private fun startAnalysisLoop() {
        analysisJob?.cancel()
        analysisJob = scope.launch(dispatchers.computation) {
            // Initial delay before first analysis
            delay(30_000L)

            while (isActive) {
                runAnalysis()
                delay(_config.patternAnalysisIntervalMs.coerceAtMost(60_000L))
                // Cap at 60s for demo; production uses config value (6 hours)
            }
        }
    }

    fun forceAnalysis() {
        checkInitialized()
        scope.launch(dispatchers.computation) { runAnalysis() }
    }

    private fun runAnalysis() {
        val actions: List<SemanticAction>
        val screenIds: List<ScreenId>

        synchronized(recentActions) {
            if (recentActions.size < 3) return
            actions = recentActions.toList()
            screenIds = recentScreenIds.toList()
        }

        // N-gram analysis by action type
        val ngramCounts = mutableMapOf<String, MutableList<Int>>()
        val minN = 1 // Allow single-action workflows (settings)
        val maxN = _config.minPatternOccurrences.coerceAtMost(8)

        for (n in minN..maxN.coerceAtMost(actions.size)) {
            for (i in 0..(actions.size - n)) {
                val key = actions.subList(i, i + n).joinToString("|") { it.actionType }
                ngramCounts.getOrPut(key) { mutableListOf() }.add(i)
            }
        }

        val candidates = ngramCounts.filter { it.value.size >= _config.minPatternOccurrences }
            .entries
            .sortedByDescending { it.key.split("|").size * it.value.size }

        val newWorkflows = mutableListOf<Workflow>()

        for ((key, occurrences) in candidates.take(_config.maxWorkflows)) {
            val actionTypes = key.split("|")
            val existingWorkflow = _workflows.value.find { w ->
                w.steps.map { it.action.actionType } == actionTypes
            }
            if (existingWorkflow != null) continue

            // --- Parameter-aware learning ---
            // For each step position, collect params from ALL occurrences
            val stepParams = actionTypes.indices.map { stepIdx ->
                val allParamsAtStep = occurrences.mapNotNull { startIdx ->
                    val actionIdx = startIdx + stepIdx
                    if (actionIdx < actions.size) actions[actionIdx].parameters else null
                }
                learnParameters(allParamsAtStep)
            }

            val steps = actionTypes.mapIndexed { index, type ->
                val learnedParams = stepParams[index]
                val fixedParams = learnedParams
                    .filter { !it.isVariable && it.defaultValue != null }
                    .associate { it.name to it.defaultValue!! }

                WorkflowStep(
                    stepIndex = index,
                    action = SemanticAction(
                        actionType = type,
                        actionSource = ActionSource.INFERRED,
                        targetElementId = null,
                        parameters = fixedParams
                    ),
                    expectedScreenId = ScreenId("current"),
                    parameters = learnedParams
                )
            }

            // Build rich name from fixed params
            val paramSummary = steps.flatMap { step ->
                step.parameters.filter { !it.isVariable && it.defaultValue != null }
                    .map { it.defaultValue!! }
            }.distinct()

            val name = if (paramSummary.isNotEmpty()) {
                paramSummary.take(4).joinToString(", ") {
                    it.replaceFirstChar { c -> c.uppercase() }
                }
            } else {
                actionTypes.joinToString(" → ") { it.replace("_", " ") }
            }

            val confidence = (occurrences.size.toFloat() / actions.size * 2).coerceIn(0f, 1f)

            if (confidence >= _config.minConfidenceScore) {
                newWorkflows.add(
                    Workflow(
                        workflowId = UlidGenerator.next(),
                        name = name,
                        description = buildWorkflowDescription(steps, occurrences.size),
                        steps = steps,
                        frequency = WorkflowFrequency(FrequencyType.IRREGULAR, null, null, null, 0f),
                        confidenceScore = confidence,
                        firstSeenMs = System.currentTimeMillis(),
                        lastSeenMs = System.currentTimeMillis(),
                        executionCount = occurrences.size,
                        automationCount = 0,
                        successRate = 0f,
                        status = WorkflowStatus.DETECTED
                    )
                )
            }
        }

        if (newWorkflows.isNotEmpty()) {
            _workflows.value = _workflows.value + newWorkflows
            // Persist to SQLite for survival across app restarts
            scope.launch(dispatchers.io) {
                for (wf in newWorkflows) {
                    saveWorkflowToDb(wf)
                }
            }
            log("Analysis found ${newWorkflows.size} new workflows (total: ${_workflows.value.size})")
        }

        // Time-based proactive suggestions
        checkTimeBasedSuggestions()
    }

    /**
     * Learn which parameters are fixed vs variable across multiple occurrences.
     */
    private fun learnParameters(allParamsAtStep: List<Map<String, String>>): List<StepParameter> {
        if (allParamsAtStep.isEmpty()) return emptyList()

        val allKeys = allParamsAtStep.flatMap { it.keys }.toSet()
        return allKeys.map { key ->
            val values = allParamsAtStep.mapNotNull { it[key] }
            val allSame = values.size == allParamsAtStep.size && values.toSet().size == 1
            StepParameter(
                name = key,
                type = ParameterType.STRING,
                isVariable = !allSame,
                defaultValue = if (allSame) values.first() else null,
                sourceExpression = null
            )
        }
    }

    /**
     * Build a human-readable workflow description from learned parameters.
     */
    private fun buildWorkflowDescription(steps: List<WorkflowStep>, occurrences: Int): String {
        val fixedParams = steps.flatMap { step ->
            step.parameters.filter { !it.isVariable && it.defaultValue != null }
                .map { "${it.name}: ${it.defaultValue}" }
        }
        val desc = if (fixedParams.isNotEmpty()) {
            fixedParams.take(5).joinToString(" · ")
        } else {
            "${steps.size} steps"
        }
        return "$desc ($occurrences×)"
    }

    // =========================================================================
    // Suggestion Handling
    // =========================================================================

    fun acceptSuggestion(suggestionId: String) {
        val suggestion = _activeSuggestion.value ?: return
        if (suggestion.suggestionId != suggestionId) return

        _activeSuggestion.value = null
        log("Suggestion accepted: ${suggestion.workflow.name}")

        // Execute the remaining steps
        scope.launch(dispatchers.main) {
            executeWorkflow(suggestion.workflow)
        }
    }

    fun dismissSuggestion(suggestionId: String) {
        _activeSuggestion.value = null
        log("Suggestion dismissed")
    }

    fun rejectSuggestion(suggestionId: String) {
        val suggestion = _activeSuggestion.value ?: return
        _activeSuggestion.value = null

        // Mark workflow as rejected
        _workflows.value = _workflows.value.map { w ->
            if (w.workflowId == suggestion.workflow.workflowId) {
                w.copy(status = WorkflowStatus.REJECTED)
            } else w
        }
        log("Suggestion rejected: ${suggestion.workflow.name}")
    }

    // =========================================================================
    // Workflow Execution
    // =========================================================================

    suspend fun executeWorkflow(workflow: Workflow): ReplayResult {
        checkInitialized()
        val executionId = UlidGenerator.next()
        val startMs = System.currentTimeMillis()

        log("Executing workflow: ${workflow.name} (${workflow.steps.size} steps)")
        _replayState.value = ReplayStatus.SUCCESS // Mark as running

        var stepsCompleted = 0

        for (step in workflow.steps) {
            val actionType = step.action.actionType

            // Find a registered target that handles this action
            val handler = findHandler(actionType)

            if (handler != null) {
                try {
                    val params = step.action.parameters
                    handler.invoke(params)
                    stepsCompleted++
                    log("  Step ${step.stepIndex}: $actionType — success")
                } catch (e: Exception) {
                    log("  Step ${step.stepIndex}: $actionType — failed: ${e.message}")

                    if (!step.isOptional) {
                        _replayState.value = ReplayStatus.FAILED
                        return ReplayResult(
                            workflowId = workflow.workflowId,
                            executionId = executionId,
                            startedAtMs = startMs,
                            completedAtMs = System.currentTimeMillis(),
                            status = ReplayStatus.FAILED,
                            stepsCompleted = stepsCompleted,
                            totalSteps = workflow.steps.size,
                            failedStepIndex = step.stepIndex,
                            error = ReplayError(
                                ReplayErrorType.ACTION_FAILED,
                                e.message ?: "Unknown",
                                step.stepIndex
                            )
                        )
                    }
                }
            } else if (!step.isOptional) {
                log("  Step ${step.stepIndex}: $actionType — no handler found")
            }
        }

        // Update workflow stats
        _workflows.value = _workflows.value.map { w ->
            if (w.workflowId == workflow.workflowId) {
                w.copy(
                    automationCount = w.automationCount + 1,
                    successRate = ((w.successRate * w.automationCount + 1f) / (w.automationCount + 1)),
                    status = WorkflowStatus.ACTIVE
                )
            } else w
        }

        _replayState.value = ReplayStatus.SUCCESS
        log("Workflow completed: ${workflow.name} ($stepsCompleted/${workflow.steps.size} steps)")

        return ReplayResult(
            workflowId = workflow.workflowId,
            executionId = executionId,
            startedAtMs = startMs,
            completedAtMs = System.currentTimeMillis(),
            status = if (stepsCompleted == workflow.steps.size) ReplayStatus.SUCCESS else ReplayStatus.PARTIAL_SUCCESS,
            stepsCompleted = stepsCompleted,
            totalSteps = workflow.steps.size
        )
    }

    /**
     * Find a handler function for the given action type by scanning registered targets
     * for @WatchAction annotations matching the type.
     */
    private fun findHandler(actionType: String): ((Map<String, String>) -> Any?)? {
        for (target in actionRegistrations) {
            for (method in target::class.java.declaredMethods) {
                val annotation = method.annotations.find {
                    it.annotationClass.simpleName == "WatchAction"
                } ?: continue

                try {
                    val type = annotation.annotationClass.java.getMethod("type").invoke(annotation) as String
                    if (type == actionType) {
                        return { params ->
                            method.isAccessible = true

                            // Name-based parameter mapping via @WatchParam annotations
                            // Uses parameterTypes + parameterAnnotations (API 1) instead of
                            // Method.getParameters() (API 26) for minSdk 24 compat
                            val paramTypes = method.parameterTypes
                            val paramAnnotations = method.parameterAnnotations
                            val args = paramTypes.indices.map { i ->
                                val annotations = paramAnnotations[i]
                                val watchParam = annotations.find {
                                    it.annotationClass.simpleName == "WatchParam"
                                }
                                val paramName = if (watchParam != null) {
                                    try {
                                        watchParam.annotationClass.java.getMethod("name")
                                            .invoke(watchParam) as String
                                    } catch (_: Exception) { "arg$i" }
                                } else {
                                    // Fallback: try matching by position from params map
                                    params.keys.elementAtOrNull(i) ?: "arg$i"
                                }

                                val rawValue = params[paramName]
                                when (paramTypes[i]) {
                                    String::class.java, java.lang.String::class.java ->
                                        rawValue ?: ""
                                    Int::class.java, Integer::class.java, java.lang.Integer::class.java ->
                                        rawValue?.toIntOrNull() ?: 0
                                    Boolean::class.java, java.lang.Boolean::class.java ->
                                        rawValue?.toBooleanStrictOrNull() ?: false
                                    Float::class.java, java.lang.Float::class.java ->
                                        rawValue?.toFloatOrNull() ?: 0f
                                    Double::class.java, java.lang.Double::class.java ->
                                        rawValue?.toDoubleOrNull() ?: 0.0
                                    else -> rawValue ?: ""
                                }
                            }

                            if (args.isEmpty()) {
                                method.invoke(target)
                            } else {
                                method.invoke(target, *args.toTypedArray())
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    // =========================================================================
    // Workflow Management
    // =========================================================================

    fun getWorkflows(): List<Workflow> = _workflows.value

    fun getWorkflow(workflowId: String): Workflow? =
        _workflows.value.find { it.workflowId == workflowId }

    // =========================================================================
    // Workflow Persistence
    // =========================================================================

    private fun saveWorkflowToDb(workflow: Workflow) {
        try {
            val db = database.writableDatabase
            val stepsJson = workflow.steps.joinToString(";") { step ->
                val params = step.parameters.joinToString(",") { p ->
                    "${p.name}=${p.defaultValue ?: ""}:${p.isVariable}"
                }
                "${step.action.actionType}|$params"
            }
            val values = android.content.ContentValues().apply {
                put("workflow_id", workflow.workflowId)
                put("name", workflow.name)
                put("description", workflow.description)
                put("steps_json", stepsJson)
                put("frequency_type", workflow.frequency.type.ordinal)
                put("frequency_json", "${workflow.frequency.intervalMs ?: 0}|${workflow.frequency.dayOfWeek ?: -1}|${workflow.frequency.hourOfDay ?: -1}|${workflow.frequency.confidence}")
                put("confidence_score", workflow.confidenceScore.toDouble())
                put("first_seen_ms", workflow.firstSeenMs)
                put("last_seen_ms", workflow.lastSeenMs)
                put("execution_count", workflow.executionCount)
                put("automation_count", workflow.automationCount)
                put("success_rate", workflow.successRate.toDouble())
                put("status", workflow.status.ordinal)
                put("version", workflow.version)
            }
            db.insertWithOnConflict("workflows", null, values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            log("Workflow persisted: ${workflow.name}")
        } catch (e: Exception) {
            log("Failed to persist workflow: ${e.message}")
        }
    }

    private fun loadWorkflowsFromDb(): List<Workflow> {
        try {
            val db = database.readableDatabase
            val cursor = db.rawQuery(
                "SELECT * FROM workflows WHERE status < ?",
                arrayOf("${WorkflowStatus.DEPRECATED.ordinal}")
            )
            val workflows = mutableListOf<Workflow>()
            while (cursor.moveToNext()) {
                try {
                    val stepsJson = cursor.getString(cursor.getColumnIndexOrThrow("steps_json"))
                    val steps = parseStepsJson(stepsJson)
                    val freqJson = cursor.getString(cursor.getColumnIndexOrThrow("frequency_json"))
                    val freqParts = freqJson.split("|")

                    workflows.add(Workflow(
                        workflowId = cursor.getString(cursor.getColumnIndexOrThrow("workflow_id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        steps = steps,
                        frequency = WorkflowFrequency(
                            type = FrequencyType.entries[cursor.getInt(cursor.getColumnIndexOrThrow("frequency_type"))],
                            intervalMs = freqParts.getOrNull(0)?.toLongOrNull(),
                            dayOfWeek = freqParts.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 },
                            hourOfDay = freqParts.getOrNull(2)?.toIntOrNull()?.takeIf { it >= 0 },
                            confidence = freqParts.getOrNull(3)?.toFloatOrNull() ?: 0f
                        ),
                        confidenceScore = cursor.getFloat(cursor.getColumnIndexOrThrow("confidence_score")),
                        firstSeenMs = cursor.getLong(cursor.getColumnIndexOrThrow("first_seen_ms")),
                        lastSeenMs = cursor.getLong(cursor.getColumnIndexOrThrow("last_seen_ms")),
                        executionCount = cursor.getInt(cursor.getColumnIndexOrThrow("execution_count")),
                        automationCount = cursor.getInt(cursor.getColumnIndexOrThrow("automation_count")),
                        successRate = cursor.getFloat(cursor.getColumnIndexOrThrow("success_rate")),
                        status = WorkflowStatus.entries[cursor.getInt(cursor.getColumnIndexOrThrow("status"))],
                        version = cursor.getInt(cursor.getColumnIndexOrThrow("version"))
                    ))
                } catch (_: Exception) { /* skip corrupt rows */ }
            }
            cursor.close()
            return workflows
        } catch (e: Exception) {
            log("Failed to load workflows: ${e.message}")
            return emptyList()
        }
    }

    private fun parseStepsJson(stepsJson: String): List<WorkflowStep> {
        return stepsJson.split(";").mapIndexed { index, stepStr ->
            val parts = stepStr.split("|", limit = 2)
            val actionType = parts[0]
            val params = if (parts.size > 1 && parts[1].isNotBlank()) {
                parts[1].split(",").mapNotNull { paramStr ->
                    val eqParts = paramStr.split("=", limit = 2)
                    if (eqParts.size == 2) {
                        val colonParts = eqParts[1].split(":", limit = 2)
                        StepParameter(
                            name = eqParts[0],
                            type = ParameterType.STRING,
                            isVariable = colonParts.getOrNull(1)?.toBooleanStrictOrNull() ?: false,
                            defaultValue = colonParts[0].ifEmpty { null },
                            sourceExpression = null
                        )
                    } else null
                }
            } else emptyList()

            val fixedParams = params.filter { !it.isVariable && it.defaultValue != null }
                .associate { it.name to it.defaultValue!! }

            WorkflowStep(
                stepIndex = index,
                action = SemanticAction(
                    actionType = actionType,
                    actionSource = ActionSource.INFERRED,
                    targetElementId = null,
                    parameters = fixedParams
                ),
                expectedScreenId = ScreenId("current"),
                parameters = params
            )
        }
    }

    // =========================================================================
    // Time-Based Proactive Suggestions
    // =========================================================================

    private fun checkTimeBasedSuggestions() {
        if (!_config.suggestionsEnabled) return

        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val currentDow = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 + 1 // Mon=1..Sun=7

        for (workflow in _workflows.value) {
            if (workflow.status == WorkflowStatus.REJECTED) continue
            if (workflow.status == WorkflowStatus.DEPRECATED) continue

            val freq = workflow.frequency
            if (freq.confidence < 0.5f) continue
            if (freq.hourOfDay == null) continue

            val hourMatch = currentHour == freq.hourOfDay
            val dayMatch = freq.dayOfWeek == null || currentDow == freq.dayOfWeek

            if (hourMatch && dayMatch) {
                // Don't suggest if already suggested recently (within 1 hour)
                val timeSinceLastSeen = now - workflow.lastSeenMs
                if (timeSinceLastSeen < 3600_000L) continue

                val suggestion = Suggestion(
                    suggestionId = UlidGenerator.next(),
                    workflow = workflow,
                    displayType = SuggestionDisplayType.NOTIFICATION,
                    title = "Time for '${workflow.name}'?",
                    description = "You usually do this around now",
                    estimatedTimeSavedMs = workflow.steps.size * 3000L,
                    createdAtMs = now,
                    expiresAtMs = now + _config.suggestionAutoExpireMs,
                    priority = 1
                )
                _activeSuggestion.value = suggestion
                _suggestions.tryEmit(suggestion)
                log("Time-based suggestion: ${workflow.name} (hour=$currentHour, day=$currentDow)")
                break // Only one time-based suggestion per analysis cycle
            }
        }
    }

    // =========================================================================
    // Data Management
    // =========================================================================

    fun clearAllData() {
        checkInitialized()
        val db = database.writableDatabase
        db.execSQL("DELETE FROM events")
        db.execSQL("DELETE FROM semantic_actions")
        db.execSQL("DELETE FROM workflows")
        db.execSQL("DELETE FROM workflow_instances")
        db.execSQL("DELETE FROM suggestions")
        db.execSQL("DELETE FROM pattern_ngrams")
        db.execSQL("DELETE FROM user_preferences")
        synchronized(recentActions) {
            recentActions.clear()
            recentScreenIds.clear()
            actionTimestamps.clear()
        }
        _workflows.value = emptyList()
        _activeSuggestion.value = null
        log("All data cleared")
    }

    // =========================================================================
    // Debug
    // =========================================================================

    fun enableDebugLogging(enabled: Boolean) {
        _config = _config.copy(debugLogging = enabled)
    }

    private fun checkInitialized() {
        check(_isInitialized) { "ZZ143.init() must be called before using the SDK" }
    }

    internal fun log(message: String) {
        if (_config.debugLogging) {
            android.util.Log.d("ZZ143", message)
        }
    }

    internal object BuildConfig {
        const val VERSION_NAME = "0.1.0-alpha01"
    }
}
