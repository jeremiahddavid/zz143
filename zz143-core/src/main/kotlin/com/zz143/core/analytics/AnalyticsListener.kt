package com.zz143.core.analytics

import com.zz143.core.model.ReplayResult
import com.zz143.core.model.Suggestion
import com.zz143.core.model.Workflow

/**
 * Callback interface for SDK lifecycle events.
 *
 * Implement this to integrate zz143 events into your own analytics pipeline
 * (Firebase, Mixpanel, Amplitude, etc.) without coupling the SDK to any
 * specific vendor.
 *
 * All callbacks are invoked on a background thread. Implementations must
 * not perform long-running blocking work.
 */
interface AnalyticsListener {

    /** A new workflow pattern was detected by the learning engine. */
    fun onWorkflowDetected(workflow: Workflow) {}

    /** A suggestion was shown to the user (bottom sheet, notification, or banner). */
    fun onSuggestionShown(suggestion: Suggestion) {}

    /** The user accepted a suggestion and replay is about to begin. */
    fun onSuggestionAccepted(suggestion: Suggestion) {}

    /** The user dismissed a suggestion without accepting or rejecting. */
    fun onSuggestionDismissed(suggestion: Suggestion) {}

    /** The user explicitly rejected a suggestion (won't see it again). */
    fun onSuggestionRejected(suggestion: Suggestion) {}

    /** An automated workflow replay started. */
    fun onReplayStarted(workflow: Workflow) {}

    /** An automated workflow replay completed (check [result.status] for outcome). */
    fun onReplayCompleted(workflow: Workflow, result: ReplayResult) {}
}
