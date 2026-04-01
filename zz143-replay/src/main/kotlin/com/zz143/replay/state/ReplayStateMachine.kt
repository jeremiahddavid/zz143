package com.zz143.replay.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReplayState {
    IDLE,
    PREPARING,
    WAITING_SCREEN,
    EXECUTING_STEP,
    VERIFYING,
    PAUSED,
    RECOVERING,
    COMPLETED,
    FAILED,
    ABORTED
}

class ReplayStateMachine {
    private val _state = MutableStateFlow(ReplayState.IDLE)
    val state: StateFlow<ReplayState> = _state.asStateFlow()

    private val validTransitions: Map<ReplayState, Set<ReplayState>> = mapOf(
        ReplayState.IDLE to setOf(ReplayState.PREPARING),
        ReplayState.PREPARING to setOf(ReplayState.WAITING_SCREEN, ReplayState.FAILED, ReplayState.ABORTED),
        ReplayState.WAITING_SCREEN to setOf(ReplayState.EXECUTING_STEP, ReplayState.FAILED, ReplayState.ABORTED),
        ReplayState.EXECUTING_STEP to setOf(
            ReplayState.VERIFYING, ReplayState.RECOVERING,
            ReplayState.PAUSED, ReplayState.FAILED, ReplayState.ABORTED
        ),
        ReplayState.VERIFYING to setOf(
            ReplayState.WAITING_SCREEN, ReplayState.EXECUTING_STEP,
            ReplayState.COMPLETED, ReplayState.RECOVERING, ReplayState.FAILED
        ),
        ReplayState.PAUSED to setOf(ReplayState.EXECUTING_STEP, ReplayState.ABORTED),
        ReplayState.RECOVERING to setOf(
            ReplayState.EXECUTING_STEP, ReplayState.WAITING_SCREEN,
            ReplayState.FAILED, ReplayState.ABORTED
        ),
        ReplayState.COMPLETED to setOf(ReplayState.IDLE),
        ReplayState.FAILED to setOf(ReplayState.IDLE),
        ReplayState.ABORTED to setOf(ReplayState.IDLE)
    )

    fun transition(newState: ReplayState) {
        val current = _state.value
        val allowed = validTransitions[current] ?: emptySet()
        require(newState in allowed) {
            "Invalid state transition: $current -> $newState (allowed: $allowed)"
        }
        _state.value = newState
    }

    fun reset() {
        _state.value = ReplayState.IDLE
    }

    val currentState: ReplayState get() = _state.value
}
