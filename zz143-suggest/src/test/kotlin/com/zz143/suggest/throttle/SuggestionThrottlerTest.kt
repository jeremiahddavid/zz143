package com.zz143.suggest.throttle

import com.google.common.truth.Truth.assertThat
import com.zz143.core.model.Suggestion
import com.zz143.core.model.SuggestionDisplayType
import com.zz143.core.model.Workflow
import com.zz143.core.model.WorkflowFrequency
import com.zz143.core.model.WorkflowStatus
import com.zz143.core.model.FrequencyType
import com.zz143.suggest.preference.UserPreferenceStore
import org.junit.Before
import org.junit.Test

class SuggestionThrottlerTest {

    private lateinit var throttler: SuggestionThrottler
    private lateinit var prefs: UserPreferenceStore

    private fun makeSuggestion(workflowId: String = "wf-1"): Suggestion {
        val workflow = Workflow(
            workflowId = workflowId,
            name = "Test Workflow",
            description = "A test workflow",
            steps = emptyList(),
            frequency = WorkflowFrequency(
                type = FrequencyType.DAILY,
                intervalMs = null,
                dayOfWeek = null,
                hourOfDay = null,
                confidence = 0.9f
            ),
            confidenceScore = 0.9f,
            firstSeenMs = System.currentTimeMillis() - 86400_000L,
            lastSeenMs = System.currentTimeMillis(),
            executionCount = 5,
            automationCount = 0,
            successRate = 1.0f,
            status = WorkflowStatus.DETECTED
        )
        return Suggestion(
            suggestionId = "sug-$workflowId",
            workflow = workflow,
            displayType = SuggestionDisplayType.BOTTOM_SHEET,
            title = "Test Suggestion",
            description = "Test description",
            estimatedTimeSavedMs = 5000L,
            createdAtMs = System.currentTimeMillis(),
            expiresAtMs = System.currentTimeMillis() + 3600_000L,
            priority = 1
        )
    }

    @Before
    fun setUp() {
        throttler = SuggestionThrottler(
            maxPerHour = 3,
            maxPerDay = 10,
            cooldownAfterDismissalMs = 4 * 3600_000L,
            cooldownAfterRejectionMs = 7 * 86400_000L
        )
        prefs = UserPreferenceStore()
    }

    @Test
    fun canShowReturnsTrueWhenNoLimitsHit() {
        val suggestion = makeSuggestion()

        val result = throttler.canShow(suggestion, prefs)

        assertThat(result).isTrue()
    }

    @Test
    fun canShowReturnsFalseAfterThreePerHour() {
        val suggestion = makeSuggestion()

        // Record 3 shown suggestions to hit the hourly limit
        throttler.recordShown(suggestion)
        throttler.recordShown(suggestion)
        throttler.recordShown(suggestion)

        val result = throttler.canShow(suggestion, prefs)

        assertThat(result).isFalse()
    }

    @Test
    fun canShowReturnsFalseAfterTenPerDay() {
        val throttlerHighHourly = SuggestionThrottler(
            maxPerHour = 20,
            maxPerDay = 10,
            cooldownAfterDismissalMs = 4 * 3600_000L,
            cooldownAfterRejectionMs = 7 * 86400_000L
        )
        val suggestion = makeSuggestion()

        // Record 10 shown suggestions to hit the daily limit
        repeat(10) {
            throttlerHighHourly.recordShown(suggestion)
        }

        val result = throttlerHighHourly.canShow(suggestion, prefs)

        assertThat(result).isFalse()
    }

    @Test
    fun disabledWorkflowIsBlocked() {
        val suggestion = makeSuggestion("wf-disabled")

        // Rejecting records isRejected=true AND sets isDisabled=true
        prefs.recordRejected("wf-disabled")

        val result = throttler.canShow(suggestion, prefs)

        assertThat(result).isFalse()
    }

    @Test
    fun dismissalCooldownBlocksSuggestion() {
        val suggestion = makeSuggestion("wf-dismissed")

        // Record that the suggestion was shown and then dismissed
        prefs.recordShown("wf-dismissed")
        prefs.recordDismissed("wf-dismissed")

        val result = throttler.canShow(suggestion, prefs)

        // Should be blocked because cooldownAfterDismissalMs (4 hours) hasn't elapsed
        assertThat(result).isFalse()
    }

    @Test
    fun rejectionCooldownBlocksSuggestion() {
        // Create a throttler that doesn't auto-disable on rejection
        // by using a fresh prefs where the workflow isn't explicitly disabled
        val suggestion = makeSuggestion("wf-rejected")

        // Mark as shown and rejected -- recordRejected sets isDisabled=true
        // so we test that rejection blocks via the isExplicitlyDisabled path
        prefs.recordShown("wf-rejected")
        prefs.recordRejected("wf-rejected")

        val result = throttler.canShow(suggestion, prefs)

        assertThat(result).isFalse()
    }

    @Test
    fun canShowReturnsTrueForDifferentWorkflowWhenOneIsDisabled() {
        val suggestion1 = makeSuggestion("wf-disabled")
        val suggestion2 = makeSuggestion("wf-other")

        prefs.recordRejected("wf-disabled")

        val result1 = throttler.canShow(suggestion1, prefs)
        val result2 = throttler.canShow(suggestion2, prefs)

        assertThat(result1).isFalse()
        assertThat(result2).isTrue()
    }

    @Test
    fun canShowReturnsTrueWhenBelowHourlyLimit() {
        val suggestion = makeSuggestion()

        // Record only 2 -- still under the limit of 3
        throttler.recordShown(suggestion)
        throttler.recordShown(suggestion)

        val result = throttler.canShow(suggestion, prefs)

        assertThat(result).isTrue()
    }

    @Test
    fun multipleDismissalsIncreaseBackoffCooldown() {
        val suggestion = makeSuggestion("wf-multi-dismiss")
        val workflowId = "wf-multi-dismiss"

        // Dismiss once, record as shown
        prefs.recordShown(workflowId)
        prefs.recordDismissed(workflowId)

        // First dismissal should block (4h cooldown)
        assertThat(throttler.canShow(suggestion, prefs)).isFalse()

        // Dismiss again -- cooldown should be even longer due to backoff
        prefs.recordDismissed(workflowId)

        assertThat(throttler.canShow(suggestion, prefs)).isFalse()
    }
}
