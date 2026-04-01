package com.zz143.suggest

import com.zz143.core.ZZ143Config
import com.zz143.core.id.UlidGenerator
import com.zz143.core.model.*
import com.zz143.learn.PatternEngine
import com.zz143.suggest.preference.UserPreferenceStore
import com.zz143.suggest.throttle.SuggestionThrottler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SuggestionEngine(
    private val config: ZZ143Config,
    private val patternEngine: PatternEngine,
    private val preferenceStore: UserPreferenceStore = UserPreferenceStore(),
    private val throttler: SuggestionThrottler = SuggestionThrottler()
) {
    private val recentActionTypes = mutableListOf<String>()
    private var activeWorkflows: List<Workflow> = emptyList()

    private val _suggestions = MutableSharedFlow<Suggestion>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val suggestions: SharedFlow<Suggestion> = _suggestions.asSharedFlow()

    fun updateWorkflows(workflows: List<Workflow>) {
        activeWorkflows = workflows.filter {
            it.status in setOf(WorkflowStatus.DETECTED, WorkflowStatus.ACCEPTED, WorkflowStatus.ACTIVE)
        }
    }

    fun onAction(actionType: String) {
        if (!config.suggestionsEnabled) return

        recentActionTypes.add(actionType)
        if (recentActionTypes.size > 20) recentActionTypes.removeFirst()

        for (workflow in activeWorkflows) {
            val prefixMatch = patternEngine.matchesPrefix(recentActionTypes, workflow) ?: continue

            if (prefixMatch.matchLength < 2 || prefixMatch.remainingSteps < 1) continue

            val suggestion = Suggestion(
                suggestionId = UlidGenerator.next(),
                workflow = workflow,
                displayType = config.suggestionDisplayType,
                title = "Complete '${workflow.name}'?",
                description = "${prefixMatch.remainingSteps} steps remaining",
                estimatedTimeSavedMs = prefixMatch.remainingSteps * 3000L,
                createdAtMs = System.currentTimeMillis(),
                expiresAtMs = System.currentTimeMillis() + config.suggestionAutoExpireMs,
                priority = 0
            )

            if (throttler.canShow(suggestion, preferenceStore)) {
                throttler.recordShown(suggestion)
                _suggestions.tryEmit(suggestion)
            }
        }
    }

    fun onTimeTick() {
        if (!config.suggestionsEnabled) return

        val now = System.currentTimeMillis()
        for (workflow in activeWorkflows) {
            val freq = workflow.frequency
            if (freq.hourOfDay == null) continue

            val cal = java.util.Calendar.getInstance()
            val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val currentDay = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 + 1

            val hourMatch = currentHour == freq.hourOfDay
            val dayMatch = freq.dayOfWeek == null || currentDay == freq.dayOfWeek

            if (hourMatch && dayMatch) {
                val suggestion = Suggestion(
                    suggestionId = UlidGenerator.next(),
                    workflow = workflow,
                    displayType = SuggestionDisplayType.NOTIFICATION,
                    title = "Time for '${workflow.name}'?",
                    description = "You usually do this around now",
                    estimatedTimeSavedMs = workflow.steps.size * 3000L,
                    createdAtMs = now,
                    expiresAtMs = now + config.suggestionAutoExpireMs,
                    priority = 1
                )

                if (throttler.canShow(suggestion, preferenceStore)) {
                    throttler.recordShown(suggestion)
                    _suggestions.tryEmit(suggestion)
                }
            }
        }
    }

    fun acceptSuggestion(suggestionId: String, workflowId: String) {
        preferenceStore.recordAccepted(workflowId)
        recentActionTypes.clear()
    }

    fun dismissSuggestion(suggestionId: String, workflowId: String) {
        preferenceStore.recordDismissed(workflowId)
    }

    fun rejectSuggestion(suggestionId: String, workflowId: String) {
        preferenceStore.recordRejected(workflowId)
    }
}
