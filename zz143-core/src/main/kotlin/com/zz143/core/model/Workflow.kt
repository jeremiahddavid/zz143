package com.zz143.core.model

data class Workflow(
    val workflowId: String,
    val name: String,
    val description: String,
    val steps: List<WorkflowStep>,
    val frequency: WorkflowFrequency,
    val confidenceScore: Float,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val executionCount: Int,
    val automationCount: Int,
    val successRate: Float,
    val status: WorkflowStatus,
    val version: Int = 1
)

enum class WorkflowStatus {
    DETECTED, SUGGESTED, ACCEPTED, REJECTED, ACTIVE, PAUSED, DEPRECATED
}

data class WorkflowStep(
    val stepIndex: Int,
    val action: SemanticAction,
    val expectedScreenId: ScreenId,
    val parameters: List<StepParameter>,
    val timeoutMs: Long = 10_000L,
    val isOptional: Boolean = false,
    val retryPolicy: RetryPolicy = RetryPolicy()
)

data class StepParameter(
    val name: String,
    val type: ParameterType,
    val isVariable: Boolean,
    val defaultValue: String?,
    val sourceExpression: String?
)

enum class ParameterType {
    STRING, INT, FLOAT, BOOLEAN, ELEMENT_REF, SCREEN_REF, TIMESTAMP
}

data class WorkflowFrequency(
    val type: FrequencyType,
    val intervalMs: Long?,
    val dayOfWeek: Int?,
    val hourOfDay: Int?,
    val confidence: Float
)

enum class FrequencyType {
    MULTIPLE_DAILY, DAILY, WEEKLY, BIWEEKLY, MONTHLY, IRREGULAR, ON_DEMAND
}

data class RetryPolicy(
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000L,
    val backoffMultiplier: Float = 2.0f
)

data class Suggestion(
    val suggestionId: String,
    val workflow: Workflow,
    val displayType: SuggestionDisplayType,
    val title: String,
    val description: String,
    val estimatedTimeSavedMs: Long = 0L,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val priority: Int = 0
)

enum class SuggestionDisplayType {
    BOTTOM_SHEET, NOTIFICATION, INLINE_BANNER
}

data class ReplayResult(
    val workflowId: String,
    val executionId: String,
    val startedAtMs: Long,
    val completedAtMs: Long,
    val status: ReplayStatus,
    val stepsCompleted: Int,
    val totalSteps: Int,
    val failedStepIndex: Int? = null,
    val error: ReplayError? = null
)

enum class ReplayStatus {
    SUCCESS, PARTIAL_SUCCESS, FAILED, ABORTED_BY_USER, TIMED_OUT
}

data class ReplayError(
    val type: ReplayErrorType,
    val message: String,
    val stepIndex: Int,
    val elementId: ElementId? = null,
    val expectedState: String? = null,
    val actualState: String? = null
)

enum class ReplayErrorType {
    ELEMENT_NOT_FOUND, SCREEN_MISMATCH, ACTION_FAILED,
    TIMEOUT, PRECONDITION_FAILED, PERMISSION_DENIED,
    APP_CRASHED, NETWORK_ERROR, UNKNOWN
}
