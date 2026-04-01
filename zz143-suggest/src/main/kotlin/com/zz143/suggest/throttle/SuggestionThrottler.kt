package com.zz143.suggest.throttle

import com.zz143.core.model.Suggestion
import com.zz143.suggest.preference.UserPreferenceStore
import kotlin.math.pow

class SuggestionThrottler(
    private val maxPerHour: Int = 3,
    private val maxPerDay: Int = 10,
    private val cooldownAfterDismissalMs: Long = 4 * 3600_000L,
    private val cooldownAfterRejectionMs: Long = 7 * 86400_000L,
    private val backoffMultiplier: Float = 2.0f
) {
    private val recentTimestamps = ArrayDeque<Long>()

    fun canShow(suggestion: Suggestion, prefs: UserPreferenceStore): Boolean {
        val now = System.currentTimeMillis()
        val workflowId = suggestion.workflow.workflowId

        // Global rate limits
        val lastHour = recentTimestamps.count { now - it < 3600_000L }
        if (lastHour >= maxPerHour) return false

        val lastDay = recentTimestamps.count { now - it < 86400_000L }
        if (lastDay >= maxPerDay) return false

        // Per-workflow checks
        if (prefs.isExplicitlyDisabled(workflowId)) return false

        val lastShownMs = prefs.lastSuggestionShownMs(workflowId)
        val dismissCount = prefs.consecutiveDismissals(workflowId)
        val isRejected = prefs.isRejected(workflowId)

        val cooldown = when {
            isRejected -> cooldownAfterRejectionMs
            dismissCount > 0 -> (cooldownAfterDismissalMs * backoffMultiplier.pow(dismissCount - 1)).toLong()
            else -> 0L
        }

        if (lastShownMs > 0 && now - lastShownMs < cooldown) return false

        return true
    }

    fun recordShown(suggestion: Suggestion) {
        val now = System.currentTimeMillis()
        recentTimestamps.addLast(now)
        // Trim old entries
        while (recentTimestamps.isNotEmpty() && now - recentTimestamps.first() > 86400_000L) {
            recentTimestamps.removeFirst()
        }
    }
}
