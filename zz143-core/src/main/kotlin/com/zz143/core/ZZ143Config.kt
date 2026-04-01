package com.zz143.core

import com.zz143.core.event.EventFilter
import com.zz143.core.model.RetryPolicy
import com.zz143.core.model.SuggestionDisplayType

data class ZZ143Config(
    // Capture
    val snapshotIntervalMs: Long = 1000L,
    val maxSnapshotDepth: Int = 30,
    val maxNodesPerSnapshot: Int = 2000,
    val captureGestures: Boolean = true,
    val captureNavigation: Boolean = true,
    val captureTextInput: Boolean = true,
    val skipInvisibleViews: Boolean = true,

    // Batching
    val batchSize: Int = 50,
    val batchWindowMs: Long = 5000L,

    // Storage
    val maxStorageMb: Int = 50,
    val maxQueueSizeMb: Int = 10,
    val eventRetentionDays: Int = 90,

    // Pattern detection
    val minPatternOccurrences: Int = 3,
    val minConfidenceScore: Float = 0.6f,
    val patternAnalysisIntervalMs: Long = 6 * 3600_000L,
    val maxWorkflows: Int = 50,

    // Suggestions
    val suggestionsEnabled: Boolean = true,
    val maxSuggestionsPerDay: Int = 10,
    val suggestionDisplayType: SuggestionDisplayType = SuggestionDisplayType.BOTTOM_SHEET,
    val suggestionAutoExpireMs: Long = 300_000L,

    // Replay
    val defaultStepTimeoutMs: Long = 10_000L,
    val defaultRetryPolicy: RetryPolicy = RetryPolicy(),
    val requireUserConfirmation: Boolean = true,

    // Privacy
    val redactPasswords: Boolean = true,
    val redactEmails: Boolean = true,
    val redactPhoneNumbers: Boolean = true,
    val sensitiveScreens: Set<String> = emptySet(),
    val sensitiveElements: Set<String> = emptySet(),
    val captureTextValues: Boolean = false,

    // Filtering
    val eventFilter: EventFilter = EventFilter.default(),

    // Compose
    val composeEnabled: Boolean = true,

    // Debug
    val debugLogging: Boolean = false
) {
    class Builder {
        private var config = ZZ143Config()

        fun snapshotIntervalMs(ms: Long) = apply { config = config.copy(snapshotIntervalMs = ms) }
        fun maxSnapshotDepth(depth: Int) = apply { config = config.copy(maxSnapshotDepth = depth) }
        fun captureGestures(enabled: Boolean) = apply { config = config.copy(captureGestures = enabled) }
        fun captureNavigation(enabled: Boolean) = apply { config = config.copy(captureNavigation = enabled) }
        fun captureTextInput(enabled: Boolean) = apply { config = config.copy(captureTextInput = enabled) }
        fun minPatternOccurrences(n: Int) = apply { config = config.copy(minPatternOccurrences = n) }
        fun minConfidenceScore(score: Float) = apply { config = config.copy(minConfidenceScore = score) }
        fun suggestionsEnabled(enabled: Boolean) = apply { config = config.copy(suggestionsEnabled = enabled) }
        fun suggestionDisplayType(type: SuggestionDisplayType) = apply { config = config.copy(suggestionDisplayType = type) }
        fun requireUserConfirmation(required: Boolean) = apply { config = config.copy(requireUserConfirmation = required) }
        fun redactPasswords(redact: Boolean) = apply { config = config.copy(redactPasswords = redact) }
        fun captureTextValues(capture: Boolean) = apply { config = config.copy(captureTextValues = capture) }
        fun sensitiveScreens(screens: Set<String>) = apply { config = config.copy(sensitiveScreens = screens) }
        fun sensitiveElements(elements: Set<String>) = apply { config = config.copy(sensitiveElements = elements) }
        fun patternAnalysisIntervalMs(ms: Long) = apply { config = config.copy(patternAnalysisIntervalMs = ms) }
        fun debugLogging(enabled: Boolean) = apply { config = config.copy(debugLogging = enabled) }

        fun build(): ZZ143Config = config
    }

    companion object {
        fun builder() = Builder()
    }
}
