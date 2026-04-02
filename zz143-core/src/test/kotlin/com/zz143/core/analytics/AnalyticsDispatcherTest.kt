package com.zz143.core.analytics

import com.google.common.truth.Truth.assertThat
import com.zz143.core.model.*
import org.junit.Before
import org.junit.Test

class AnalyticsDispatcherTest {

    private lateinit var dispatcher: AnalyticsDispatcher
    private lateinit var recorder: RecordingListener

    @Before
    fun setUp() {
        dispatcher = AnalyticsDispatcher()
        recorder = RecordingListener()
    }

    // --- Listener management ---

    @Test
    fun addListenerIncreasesCount() {
        dispatcher.addListener(recorder)
        assertThat(dispatcher.listenerCount()).isEqualTo(1)
    }

    @Test
    fun removeListenerDecreasesCount() {
        dispatcher.addListener(recorder)
        dispatcher.removeListener(recorder)
        assertThat(dispatcher.listenerCount()).isEqualTo(0)
    }

    @Test
    fun addSameListenerTwiceDoesNotDuplicate() {
        dispatcher.addListener(recorder)
        dispatcher.addListener(recorder)
        assertThat(dispatcher.listenerCount()).isEqualTo(1)
    }

    @Test
    fun removingUnregisteredListenerIsNoOp() {
        dispatcher.removeListener(recorder)
        assertThat(dispatcher.listenerCount()).isEqualTo(0)
    }

    // --- Dispatch: workflowDetected ---

    @Test
    fun dispatchWorkflowDetectedNotifiesListener() {
        dispatcher.addListener(recorder)
        dispatcher.dispatchWorkflowDetected(testWorkflow())

        assertThat(recorder.workflowsDetected).hasSize(1)
        assertThat(recorder.workflowsDetected[0].name).isEqualTo("Test Workflow")
    }

    @Test
    fun dispatchWorkflowDetectedNotifiesMultipleListeners() {
        val second = RecordingListener()
        dispatcher.addListener(recorder)
        dispatcher.addListener(second)
        dispatcher.dispatchWorkflowDetected(testWorkflow())

        assertThat(recorder.workflowsDetected).hasSize(1)
        assertThat(second.workflowsDetected).hasSize(1)
    }

    // --- Dispatch: suggestionShown ---

    @Test
    fun dispatchSuggestionShownNotifiesListener() {
        dispatcher.addListener(recorder)
        dispatcher.dispatchSuggestionShown(testSuggestion())

        assertThat(recorder.suggestionsShown).hasSize(1)
    }

    // --- Dispatch: suggestionAccepted ---

    @Test
    fun dispatchSuggestionAcceptedNotifiesListener() {
        dispatcher.addListener(recorder)
        dispatcher.dispatchSuggestionAccepted(testSuggestion())

        assertThat(recorder.suggestionsAccepted).hasSize(1)
    }

    // --- Dispatch: suggestionDismissed ---

    @Test
    fun dispatchSuggestionDismissedNotifiesListener() {
        dispatcher.addListener(recorder)
        dispatcher.dispatchSuggestionDismissed(testSuggestion())

        assertThat(recorder.suggestionsDismissed).hasSize(1)
    }

    // --- Dispatch: suggestionRejected ---

    @Test
    fun dispatchSuggestionRejectedNotifiesListener() {
        dispatcher.addListener(recorder)
        dispatcher.dispatchSuggestionRejected(testSuggestion())

        assertThat(recorder.suggestionsRejected).hasSize(1)
    }

    // --- Dispatch: replayStarted ---

    @Test
    fun dispatchReplayStartedNotifiesListener() {
        dispatcher.addListener(recorder)
        dispatcher.dispatchReplayStarted(testWorkflow())

        assertThat(recorder.replaysStarted).hasSize(1)
    }

    // --- Dispatch: replayCompleted ---

    @Test
    fun dispatchReplayCompletedNotifiesListener() {
        dispatcher.addListener(recorder)
        dispatcher.dispatchReplayCompleted(testWorkflow(), testReplayResult())

        assertThat(recorder.replaysCompleted).hasSize(1)
        assertThat(recorder.replaysCompleted[0].second.status).isEqualTo(ReplayStatus.SUCCESS)
    }

    // --- Error isolation ---

    @Test
    fun throwingListenerDoesNotBlockOtherListeners() {
        val throwing = object : AnalyticsListener {
            override fun onWorkflowDetected(workflow: Workflow) {
                throw RuntimeException("Boom")
            }
        }
        dispatcher.addListener(throwing)
        dispatcher.addListener(recorder)

        dispatcher.dispatchWorkflowDetected(testWorkflow())

        // recorder still received the event
        assertThat(recorder.workflowsDetected).hasSize(1)
    }

    // --- No listeners ---

    @Test
    fun dispatchWithNoListenersDoesNotThrow() {
        // Should not throw
        dispatcher.dispatchWorkflowDetected(testWorkflow())
        dispatcher.dispatchSuggestionShown(testSuggestion())
        dispatcher.dispatchReplayCompleted(testWorkflow(), testReplayResult())
    }

    // --- Helpers ---

    private fun testWorkflow() = Workflow(
        workflowId = "wf-test",
        name = "Test Workflow",
        description = "A test workflow",
        steps = emptyList(),
        frequency = WorkflowFrequency(FrequencyType.DAILY, null, null, null, 0.9f),
        confidenceScore = 0.9f,
        firstSeenMs = 1000L,
        lastSeenMs = 2000L,
        executionCount = 5,
        automationCount = 0,
        successRate = 1.0f,
        status = WorkflowStatus.DETECTED
    )

    private fun testSuggestion() = Suggestion(
        suggestionId = "sug-1",
        workflow = testWorkflow(),
        displayType = SuggestionDisplayType.BOTTOM_SHEET,
        title = "Test Suggestion",
        description = "desc",
        estimatedTimeSavedMs = 5000L,
        createdAtMs = 1000L,
        expiresAtMs = 2000L,
        priority = 0
    )

    private fun testReplayResult() = ReplayResult(
        workflowId = "wf-test",
        executionId = "exec-1",
        startedAtMs = 1000L,
        completedAtMs = 2000L,
        status = ReplayStatus.SUCCESS,
        stepsCompleted = 3,
        totalSteps = 3
    )

    /** Test double that records all dispatched events. */
    private class RecordingListener : AnalyticsListener {
        val workflowsDetected = mutableListOf<Workflow>()
        val suggestionsShown = mutableListOf<Suggestion>()
        val suggestionsAccepted = mutableListOf<Suggestion>()
        val suggestionsDismissed = mutableListOf<Suggestion>()
        val suggestionsRejected = mutableListOf<Suggestion>()
        val replaysStarted = mutableListOf<Workflow>()
        val replaysCompleted = mutableListOf<Pair<Workflow, ReplayResult>>()

        override fun onWorkflowDetected(workflow: Workflow) { workflowsDetected.add(workflow) }
        override fun onSuggestionShown(suggestion: Suggestion) { suggestionsShown.add(suggestion) }
        override fun onSuggestionAccepted(suggestion: Suggestion) { suggestionsAccepted.add(suggestion) }
        override fun onSuggestionDismissed(suggestion: Suggestion) { suggestionsDismissed.add(suggestion) }
        override fun onSuggestionRejected(suggestion: Suggestion) { suggestionsRejected.add(suggestion) }
        override fun onReplayStarted(workflow: Workflow) { replaysStarted.add(workflow) }
        override fun onReplayCompleted(workflow: Workflow, result: ReplayResult) { replaysCompleted.add(workflow to result) }
    }
}
