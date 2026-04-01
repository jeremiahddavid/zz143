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
                    title = "Complete '${workflow.name}'?",
                    description = "$remaining steps remaining — save ~${remaining * 3}s",
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

    private fun runAnalysis() {
        val actions: List<SemanticAction>
        val screenIds: List<ScreenId>

        synchronized(recentActions) {
            if (recentActions.size < 6) return // Need at least a few actions
            actions = recentActions.toList()
            screenIds = recentScreenIds.toList()
        }

        // Simple n-gram analysis directly on in-memory actions
        val ngramCounts = mutableMapOf<String, MutableList<Int>>()
        val minN = 2
        val maxN = _config.minPatternOccurrences.coerceAtMost(8)

        for (n in minN..maxN.coerceAtMost(actions.size)) {
            for (i in 0..(actions.size - n)) {
                val key = actions.subList(i, i + n).joinToString("|") { it.actionType }
                ngramCounts.getOrPut(key) { mutableListOf() }.add(i)
            }
        }

        // Find sequences that repeat enough times
        val candidates = ngramCounts.filter { it.value.size >= _config.minPatternOccurrences }
            .entries
            .sortedByDescending { it.key.split("|").size * it.value.size }

        val newWorkflows = mutableListOf<Workflow>()

        for ((key, occurrences) in candidates.take(_config.maxWorkflows)) {
            val actionTypes = key.split("|")
            val existingWorkflow = _workflows.value.find { w ->
                w.steps.map { it.action.actionType } == actionTypes
            }

            if (existingWorkflow != null) continue // Already known

            val steps = actionTypes.mapIndexed { index, type ->
                WorkflowStep(
                    stepIndex = index,
                    action = SemanticAction(
                        actionType = type,
                        actionSource = ActionSource.INFERRED,
                        targetElementId = null,
                        parameters = emptyMap()
                    ),
                    expectedScreenId = ScreenId("current"),
                    parameters = emptyList()
                )
            }

            val name = actionTypes.joinToString(" → ") {
                it.replace("_", " ")
            }

            val confidence = (occurrences.size.toFloat() / actions.size * 2).coerceIn(0f, 1f)

            if (confidence >= _config.minConfidenceScore) {
                newWorkflows.add(
                    Workflow(
                        workflowId = UlidGenerator.next(),
                        name = name,
                        description = "${occurrences.size} occurrences, ${actionTypes.size} steps",
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
            log("Analysis found ${newWorkflows.size} new workflows (total: ${_workflows.value.size})")
        }
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
                            // Simple invocation — call with default args
                            method.isAccessible = true
                            when (method.parameterCount) {
                                0 -> method.invoke(target)
                                1 -> method.invoke(target, params.values.firstOrNull() ?: "")
                                2 -> {
                                    val values = params.values.toList()
                                    method.invoke(target, values.getOrElse(0) { "" }, values.getOrElse(1) { 1 })
                                }
                                else -> method.invoke(target)
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
