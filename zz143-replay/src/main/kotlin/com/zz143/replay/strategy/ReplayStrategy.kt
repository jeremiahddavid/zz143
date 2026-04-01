package com.zz143.replay.strategy

import com.zz143.core.model.WorkflowStep
import com.zz143.replay.execution.ExecutionContext
import com.zz143.replay.execution.StepResult

interface ReplayStrategy {
    val name: String
    val priority: Int

    suspend fun canExecute(step: WorkflowStep, context: ExecutionContext): Boolean
    suspend fun execute(step: WorkflowStep, context: ExecutionContext): StepResult
}

class StrategySelector(
    private val strategies: List<ReplayStrategy>
) {
    suspend fun selectStrategy(step: WorkflowStep, context: ExecutionContext): ReplayStrategy? {
        return strategies
            .sortedBy { it.priority }
            .firstOrNull { it.canExecute(step, context) }
    }

    suspend fun selectAlternative(
        step: WorkflowStep,
        context: ExecutionContext,
        exclude: ReplayStrategy
    ): ReplayStrategy? {
        return strategies
            .sortedBy { it.priority }
            .filter { it != exclude }
            .firstOrNull { it.canExecute(step, context) }
    }
}
