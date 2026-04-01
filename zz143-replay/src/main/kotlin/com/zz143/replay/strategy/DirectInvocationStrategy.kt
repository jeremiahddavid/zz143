package com.zz143.replay.strategy

import com.zz143.core.model.ReplayErrorType
import com.zz143.core.model.WorkflowStep
import com.zz143.replay.execution.ExecutionContext
import com.zz143.replay.execution.StepResult
import com.zz143.replay.registry.ActionRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DirectInvocationStrategy(
    private val registry: ActionRegistry
) : ReplayStrategy {

    override val name = "direct_invocation"
    override val priority = 0

    override suspend fun canExecute(step: WorkflowStep, context: ExecutionContext): Boolean {
        return registry.find(step.action.actionType) != null
    }

    override suspend fun execute(step: WorkflowStep, context: ExecutionContext): StepResult {
        val descriptor = registry.find(step.action.actionType)
            ?: return StepResult.Failed(ReplayErrorType.ACTION_FAILED, "Action '${step.action.actionType}' not registered")

        val target = descriptor.targetInstance.get()
            ?: return StepResult.Failed(ReplayErrorType.ACTION_FAILED, "Target instance was garbage collected")

        val resolvedParams = step.parameters.map { param ->
            when {
                !param.isVariable -> param.defaultValue?.let { castValue(it, param.type) }
                param.sourceExpression != null -> resolveExpression(param.sourceExpression, context)
                else -> param.defaultValue?.let { castValue(it, param.type) }
            }
        }

        return try {
            val result = withContext(Dispatchers.Main) {
                descriptor.method.call(target, *resolvedParams.toTypedArray())
            }
            StepResult.Success(result)
        } catch (e: Exception) {
            StepResult.Failed(
                ReplayErrorType.ACTION_FAILED,
                "Action '${step.action.actionType}' failed: ${e.message}"
            )
        }
    }

    private fun castValue(value: String, type: com.zz143.core.model.ParameterType): Any? = when (type) {
        com.zz143.core.model.ParameterType.STRING -> value
        com.zz143.core.model.ParameterType.INT -> value.toIntOrNull()
        com.zz143.core.model.ParameterType.FLOAT -> value.toFloatOrNull()
        com.zz143.core.model.ParameterType.BOOLEAN -> value.toBooleanStrictOrNull()
        else -> value
    }

    private fun resolveExpression(expression: String, context: ExecutionContext): Any? {
        val parts = expression.split(".")
        return when (parts.firstOrNull()) {
            "context" -> context.getVariable(parts.drop(1).joinToString("."))
            "step" -> {
                val stepIndex = parts.getOrNull(1)
                    ?.removeSurrounding("[", "]")
                    ?.toIntOrNull() ?: return null
                context.getStepResult(stepIndex)
            }
            else -> null
        }
    }
}
