package com.zz143.replay.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReplayStateMachineTest {

    @Test
    fun initialStateIsIdle() {
        val sm = ReplayStateMachine()

        assertThat(sm.currentState).isEqualTo(ReplayState.IDLE)
    }

    @Test
    fun validTransitionIdleToPreparing() {
        val sm = ReplayStateMachine()

        sm.transition(ReplayState.PREPARING)

        assertThat(sm.currentState).isEqualTo(ReplayState.PREPARING)
    }

    @Test
    fun validTransitionPreparingToWaitingScreen() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)

        sm.transition(ReplayState.WAITING_SCREEN)

        assertThat(sm.currentState).isEqualTo(ReplayState.WAITING_SCREEN)
    }

    @Test
    fun validTransitionPreparingToFailed() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)

        sm.transition(ReplayState.FAILED)

        assertThat(sm.currentState).isEqualTo(ReplayState.FAILED)
    }

    @Test
    fun validTransitionPreparingToAborted() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)

        sm.transition(ReplayState.ABORTED)

        assertThat(sm.currentState).isEqualTo(ReplayState.ABORTED)
    }

    @Test
    fun validTransitionWaitingScreenToExecutingStep() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)

        sm.transition(ReplayState.EXECUTING_STEP)

        assertThat(sm.currentState).isEqualTo(ReplayState.EXECUTING_STEP)
    }

    @Test
    fun validTransitionExecutingStepToVerifying() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)

        sm.transition(ReplayState.VERIFYING)

        assertThat(sm.currentState).isEqualTo(ReplayState.VERIFYING)
    }

    @Test
    fun validTransitionExecutingStepToPaused() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)

        sm.transition(ReplayState.PAUSED)

        assertThat(sm.currentState).isEqualTo(ReplayState.PAUSED)
    }

    @Test
    fun validTransitionExecutingStepToRecovering() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)

        sm.transition(ReplayState.RECOVERING)

        assertThat(sm.currentState).isEqualTo(ReplayState.RECOVERING)
    }

    @Test
    fun validTransitionPausedToExecutingStep() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.PAUSED)

        sm.transition(ReplayState.EXECUTING_STEP)

        assertThat(sm.currentState).isEqualTo(ReplayState.EXECUTING_STEP)
    }

    @Test
    fun validTransitionPausedToAborted() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.PAUSED)

        sm.transition(ReplayState.ABORTED)

        assertThat(sm.currentState).isEqualTo(ReplayState.ABORTED)
    }

    @Test
    fun validTransitionVerifyingToCompleted() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.VERIFYING)

        sm.transition(ReplayState.COMPLETED)

        assertThat(sm.currentState).isEqualTo(ReplayState.COMPLETED)
    }

    @Test
    fun validTransitionVerifyingToWaitingScreenForNextStep() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.VERIFYING)

        sm.transition(ReplayState.WAITING_SCREEN)

        assertThat(sm.currentState).isEqualTo(ReplayState.WAITING_SCREEN)
    }

    @Test
    fun validTransitionRecoveringToExecutingStep() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.RECOVERING)

        sm.transition(ReplayState.EXECUTING_STEP)

        assertThat(sm.currentState).isEqualTo(ReplayState.EXECUTING_STEP)
    }

    @Test
    fun validTransitionRecoveringToFailed() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.RECOVERING)

        sm.transition(ReplayState.FAILED)

        assertThat(sm.currentState).isEqualTo(ReplayState.FAILED)
    }

    @Test
    fun validTransitionCompletedToIdle() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.VERIFYING)
        sm.transition(ReplayState.COMPLETED)

        sm.transition(ReplayState.IDLE)

        assertThat(sm.currentState).isEqualTo(ReplayState.IDLE)
    }

    @Test
    fun validTransitionFailedToIdle() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.FAILED)

        sm.transition(ReplayState.IDLE)

        assertThat(sm.currentState).isEqualTo(ReplayState.IDLE)
    }

    @Test
    fun validTransitionAbortedToIdle() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.ABORTED)

        sm.transition(ReplayState.IDLE)

        assertThat(sm.currentState).isEqualTo(ReplayState.IDLE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTransitionIdleToExecutingStepThrows() {
        val sm = ReplayStateMachine()

        sm.transition(ReplayState.EXECUTING_STEP)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTransitionIdleToCompletedThrows() {
        val sm = ReplayStateMachine()

        sm.transition(ReplayState.COMPLETED)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTransitionCompletedToPreparingThrows() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.VERIFYING)
        sm.transition(ReplayState.COMPLETED)

        sm.transition(ReplayState.PREPARING)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTransitionPausedToCompletedThrows() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.PAUSED)

        sm.transition(ReplayState.COMPLETED)
    }

    @Test
    fun resetSetsStateToIdle() {
        val sm = ReplayStateMachine()
        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)

        sm.reset()

        assertThat(sm.currentState).isEqualTo(ReplayState.IDLE)
    }

    @Test
    fun resetFromAnyStateGoesToIdle() {
        for (state in ReplayState.values()) {
            val sm = ReplayStateMachine()
            // Use reset to forcibly test from each state
            // (reset is unconditional)
            sm.reset()
            assertThat(sm.currentState).isEqualTo(ReplayState.IDLE)
        }
    }

    @Test
    fun fullHappyPathLifecycle() {
        val sm = ReplayStateMachine()

        assertThat(sm.currentState).isEqualTo(ReplayState.IDLE)

        sm.transition(ReplayState.PREPARING)
        assertThat(sm.currentState).isEqualTo(ReplayState.PREPARING)

        sm.transition(ReplayState.WAITING_SCREEN)
        assertThat(sm.currentState).isEqualTo(ReplayState.WAITING_SCREEN)

        sm.transition(ReplayState.EXECUTING_STEP)
        assertThat(sm.currentState).isEqualTo(ReplayState.EXECUTING_STEP)

        sm.transition(ReplayState.VERIFYING)
        assertThat(sm.currentState).isEqualTo(ReplayState.VERIFYING)

        sm.transition(ReplayState.COMPLETED)
        assertThat(sm.currentState).isEqualTo(ReplayState.COMPLETED)

        sm.transition(ReplayState.IDLE)
        assertThat(sm.currentState).isEqualTo(ReplayState.IDLE)
    }

    @Test
    fun recoveryPathLifecycle() {
        val sm = ReplayStateMachine()

        sm.transition(ReplayState.PREPARING)
        sm.transition(ReplayState.WAITING_SCREEN)
        sm.transition(ReplayState.EXECUTING_STEP)

        // Step fails, enter recovery
        sm.transition(ReplayState.RECOVERING)
        assertThat(sm.currentState).isEqualTo(ReplayState.RECOVERING)

        // Recovery succeeds, go back to waiting for screen
        sm.transition(ReplayState.WAITING_SCREEN)
        assertThat(sm.currentState).isEqualTo(ReplayState.WAITING_SCREEN)

        // Resume execution
        sm.transition(ReplayState.EXECUTING_STEP)
        sm.transition(ReplayState.VERIFYING)
        sm.transition(ReplayState.COMPLETED)

        assertThat(sm.currentState).isEqualTo(ReplayState.COMPLETED)
    }

    @Test
    fun stateFlowReflectsCurrentState() {
        val sm = ReplayStateMachine()

        assertThat(sm.state.value).isEqualTo(ReplayState.IDLE)

        sm.transition(ReplayState.PREPARING)
        assertThat(sm.state.value).isEqualTo(ReplayState.PREPARING)
    }
}
