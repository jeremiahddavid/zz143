package com.zz143.core.model

sealed class ZZ143Event {
    abstract val eventId: String
    abstract val sessionId: SessionId
    abstract val timestampMs: Long
    abstract val uptimeMs: Long
    abstract val screenId: ScreenId

    data class Snapshot(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val snapshot: ScreenSnapshot
    ) : ZZ143Event()

    data class Delta(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val delta: IncrementalDelta
    ) : ZZ143Event()

    data class Gesture(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val gesture: GestureEvent
    ) : ZZ143Event()

    data class Navigation(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val navigation: NavigationEvent
    ) : ZZ143Event()

    data class TextInput(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val input: TextInputEvent
    ) : ZZ143Event()

    data class Action(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val action: SemanticAction
    ) : ZZ143Event()

    data class Lifecycle(
        override val eventId: String,
        override val sessionId: SessionId,
        override val timestampMs: Long,
        override val uptimeMs: Long,
        override val screenId: ScreenId,
        val type: LifecycleType,
        val metadata: Map<String, String> = emptyMap()
    ) : ZZ143Event()
}

// Sub-models

data class GestureEvent(
    val type: GestureType,
    val targetElementId: ElementId?,
    val startX: Float,
    val startY: Float,
    val endX: Float?,
    val endY: Float?,
    val durationMs: Long,
    val pointerCount: Int = 1
)

enum class GestureType {
    TAP, DOUBLE_TAP, LONG_PRESS,
    SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT,
    SCROLL, FLING, PINCH, CUSTOM
}

data class NavigationEvent(
    val type: NavigationType,
    val fromScreenId: ScreenId?,
    val toScreenId: ScreenId,
    val route: String?,
    val transitionType: String?
)

enum class NavigationType {
    ACTIVITY_CREATED, ACTIVITY_DESTROYED,
    FRAGMENT_ATTACHED, FRAGMENT_DETACHED,
    COMPOSE_NAVIGATION, DEEP_LINK, BACK_PRESS
}

data class TextInputEvent(
    val targetElementId: ElementId,
    val fieldType: TextFieldType,
    val isRedacted: Boolean,
    val textLength: Int,
    val hashedValue: String?
)

enum class TextFieldType {
    PLAIN, EMAIL, PASSWORD, PHONE, NUMBER, URL, SEARCH, MULTILINE
}

data class SemanticAction(
    val actionType: String,
    val actionSource: ActionSource,
    val targetElementId: ElementId?,
    val parameters: Map<String, String>,
    val preconditions: Map<String, String> = emptyMap(),
    val postconditions: Map<String, String> = emptyMap()
)

enum class ActionSource {
    ANNOTATION, INFERRED, GESTURE
}

enum class LifecycleType {
    SESSION_START, SESSION_END, APP_BACKGROUNDED, APP_FOREGROUNDED
}
