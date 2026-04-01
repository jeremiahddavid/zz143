package com.zz143.replay

import com.zz143.core.id.UlidGenerator
import com.zz143.core.model.*
import com.zz143.replay.execution.ExecutionContext
import com.zz143.replay.execution.StepResult
import com.zz143.replay.state.ReplayState
import com.zz143.replay.state.ReplayStateMachine
import com.zz143.replay.strategy.StrategySelector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow

class ReplayEngine(
    private val strategySelector: StrategySelector,
    private val stateMachine: ReplayStateMachine = ReplayStateMachine()
) {
    val state: StateFlow<ReplayState> get() = stateMachine.state

    suspend fun execute(workflow: Workflow): ReplayResult {
        val executionId = UlidGenerator.next()
        val context = ExecutionContext(workflow, executionId)
        val startMs = System.currentTimeMillis()
        var currentStepIndex = 0

        stateMachine.transition(ReplayState.PREPARING)

        try {
            val steps = workflow.steps

            while (currentStepIndex < steps.size) {
                val step = steps[currentStepIndex]

                stateMachine.transition(ReplayState.EXECUTING_STEP)

                val strategy = context.getStrategy(currentStepIndex)
                    ?: strategySelector.selectStrategy(step, context)

                if (strategy == null) {
                    if (step.isOptional) {
                        currentStepIndex++
                        continue
                    }
                    stateMachine.transition(ReplayState.FAILED)
                    return buildResult(executionId, startMs, ReplayStatus.FAILED, currentStepIndex, steps.size,
                        ReplayError(ReplayErrorType.ACTION_FAILED, "No strategy available", currentStepIndex))
                }

                val stepResult = strategy.execute(step, context)

                when (stepResult) {
                    is StepResult.Success -> {
                        context.recordStepResult(currentStepIndex, stepResult)
                        currentStepIndex++
                    }
                    is StepResult.Failed -> {
                        if (context.retryCount(currentStepIndex) < step.retryPolicy.maxRetries) {
                            context.incrementRetry(currentStepIndex)
                            stateMachine.transition(ReplayState.RECOVERING)
                            stateMachine.transition(ReplayState.EXECUTING_STEP)
                            continue
                        }
                        if (step.isOptional) {
                            currentStepIndex++
                            continue
                        }
                        stateMachine.transition(ReplayState.FAILED)
                        return buildResult(executionId, startMs, ReplayStatus.FAILED, currentStepIndex, steps.size,
                            ReplayError(stepResult.errorType, stepResult.message ?: "Unknown", currentStepIndex))
                    }
                }
            }

            stateMachine.transition(ReplayState.COMPLETED)
            stateMachine.transition(ReplayState.IDLE)
            return buildResult(executionId, startMs, ReplayStatus.SUCCESS, steps.size, steps.size, null)

        } catch (e: CancellationException) {
            stateMachine.reset()
            return buildResult(executionId, startMs, ReplayStatus.ABORTED_BY_USER, currentStepIndex, workflow.steps.size, null)
        } catch (e: Exception) {
            stateMachine.reset()
            return buildResult(executionId, startMs, ReplayStatus.FAILED, currentStepIndex, workflow.steps.size,
                ReplayError(ReplayErrorType.UNKNOWN, e.message ?: "Unknown error", currentStepIndex))
        }
    }

    fun abort() {
        if (stateMachine.currentState !in setOf(ReplayState.IDLE, ReplayState.COMPLETED, ReplayState.FAILED)) {
            stateMachine.reset()
        }
    }

    private fun buildResult(
        executionId: String, startMs: Long, status: ReplayStatus,
        stepsCompleted: Int, totalSteps: Int, error: ReplayError?
    ) = ReplayResult(
        workflowId = "",
        executionId = executionId,
        startedAtMs = startMs,
        completedAtMs = System.currentTimeMillis(),
        status = status,
        stepsCompleted = stepsCompleted,
        totalSteps = totalSteps,
        failedStepIndex = error?.stepIndex,
        error = error
    )
}
