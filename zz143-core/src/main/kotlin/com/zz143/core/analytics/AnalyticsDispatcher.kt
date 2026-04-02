package com.zz143.core.analytics

import com.zz143.core.model.ReplayResult
import com.zz143.core.model.Suggestion
import com.zz143.core.model.Workflow
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe dispatcher that fans out SDK events to all registered
 * [AnalyticsListener] instances.
 *
 * Uses [CopyOnWriteArrayList] so listeners can be added/removed at any time
 * without external synchronisation or risk of ConcurrentModificationException.
 */
internal class AnalyticsDispatcher {

    private val listeners = CopyOnWriteArrayList<AnalyticsListener>()

    fun addListener(listener: AnalyticsListener) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: AnalyticsListener) {
        listeners.remove(listener)
    }

    fun listenerCount(): Int = listeners.size

    fun dispatchWorkflowDetected(workflow: Workflow) {
        for (listener in listeners) {
            runCatching { listener.onWorkflowDetected(workflow) }
        }
    }

    fun dispatchSuggestionShown(suggestion: Suggestion) {
        for (listener in listeners) {
            runCatching { listener.onSuggestionShown(suggestion) }
        }
    }

    fun dispatchSuggestionAccepted(suggestion: Suggestion) {
        for (listener in listeners) {
            runCatching { listener.onSuggestionAccepted(suggestion) }
        }
    }

    fun dispatchSuggestionDismissed(suggestion: Suggestion) {
        for (listener in listeners) {
            runCatching { listener.onSuggestionDismissed(suggestion) }
        }
    }

    fun dispatchSuggestionRejected(suggestion: Suggestion) {
        for (listener in listeners) {
            runCatching { listener.onSuggestionRejected(suggestion) }
        }
    }

    fun dispatchReplayStarted(workflow: Workflow) {
        for (listener in listeners) {
            runCatching { listener.onReplayStarted(workflow) }
        }
    }

    fun dispatchReplayCompleted(workflow: Workflow, result: ReplayResult) {
        for (listener in listeners) {
            runCatching { listener.onReplayCompleted(workflow, result) }
        }
    }
}
