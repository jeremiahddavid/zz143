package com.zz143.replay.execution

import com.zz143.core.model.ReplayErrorType
import com.zz143.core.model.Workflow
import com.zz143.replay.strategy.ReplayStrategy

class ExecutionContext(
    val workflow: Workflow,
    val executionId: String
) {
    private val stepResults = mutableMapOf<Int, StepResult>()
    private val retryCounts = mutableMapOf<Int, Int>()
    private val strategyOverrides = mutableMapOf<Int, ReplayStrategy>()
    private val variables = mutableMapOf<String, Any?>()

    fun recordStepResult(stepIndex: Int, result: StepResult) {
        stepResults[stepIndex] = result
    }

    fun getStepResult(stepIndex: Int): StepResult? = stepResults[stepIndex]

    fun retryCount(stepIndex: Int): Int = retryCounts.getOrDefault(stepIndex, 0)

    fun incrementRetry(stepIndex: Int) {
        retryCounts[stepIndex] = retryCount(stepIndex) + 1
    }

    fun setStrategy(stepIndex: Int, strategy: ReplayStrategy) {
        strategyOverrides[stepIndex] = strategy
    }

    fun getStrategy(stepIndex: Int): ReplayStrategy? = strategyOverrides[stepIndex]

    fun setVariable(key: String, value: Any?) {
        variables[key] = value
    }

    fun getVariable(key: String): Any? = variables[key]
}

sealed class StepResult {
    data class Success(val result: Any?) : StepResult()
    data class Failed(val errorType: ReplayErrorType, val message: String?) : StepResult()
}
