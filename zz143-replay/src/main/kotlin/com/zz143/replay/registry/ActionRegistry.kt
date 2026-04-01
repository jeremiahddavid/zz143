package com.zz143.replay.registry

import com.zz143.core.model.ParameterType
import com.zz143.replay.annotation.WatchAction
import com.zz143.replay.annotation.WatchGuard
import com.zz143.replay.annotation.WatchParam
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberFunctions

class ActionRegistry {
    private val actions = ConcurrentHashMap<String, ActionDescriptor>()

    fun register(vararg targets: Any) {
        for (target in targets) {
            scanTarget(target)
        }
    }

    fun unregister(vararg targets: Any) {
        val targetSet = targets.toSet()
        actions.entries.removeAll { (_, descriptor) ->
            descriptor.targetInstance.get() in targetSet
        }
    }

    fun find(actionType: String): ActionDescriptor? = actions[actionType]

    fun all(): List<ActionDescriptor> = actions.values.toList()

    private fun scanTarget(target: Any) {
        val klass = target::class
        for (function in klass.memberFunctions) {
            val annotation = function.findAnnotation<WatchAction>() ?: continue
            val params = function.parameters
                .filter { it.kind == KParameter.Kind.VALUE && it.findAnnotation<WatchParam>() != null }
                .map { param ->
                    val paramAnnotation = param.findAnnotation<WatchParam>()!!
                    ActionParameter(
                        name = paramAnnotation.name,
                        type = mapKotlinType(param.type.toString()),
                        isSensitive = paramAnnotation.sensitive,
                        description = paramAnnotation.description
                    )
                }

            val guards = function.findAnnotations<WatchGuard>().map { it.expression }

            actions[annotation.type] = ActionDescriptor(
                actionType = annotation.type,
                description = annotation.description,
                expectedScreen = annotation.screen.ifEmpty { null },
                isIdempotent = annotation.idempotent,
                targetInstance = WeakReference(target),
                method = function,
                parameters = params,
                guardExpressions = guards
            )
        }
    }

    private fun mapKotlinType(typeName: String): ParameterType = when {
        "String" in typeName -> ParameterType.STRING
        "Int" in typeName -> ParameterType.INT
        "Float" in typeName || "Double" in typeName -> ParameterType.FLOAT
        "Boolean" in typeName -> ParameterType.BOOLEAN
        else -> ParameterType.STRING
    }
}

data class ActionDescriptor(
    val actionType: String,
    val description: String,
    val expectedScreen: String?,
    val isIdempotent: Boolean,
    val targetInstance: WeakReference<Any>,
    val method: KFunction<*>,
    val parameters: List<ActionParameter>,
    val guardExpressions: List<String>
)

data class ActionParameter(
    val name: String,
    val type: ParameterType,
    val isSensitive: Boolean,
    val description: String
)
