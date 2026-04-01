package com.zz143.capture.gesture

import com.zz143.core.model.GestureType
import kotlin.math.abs
import kotlin.math.sqrt

internal class GestureClassifier(
    private val tapThresholdPx: Float = 20f,
    private val longPressThresholdMs: Long = 500L,
    private val swipeThresholdPx: Float = 100f
) {
    fun classify(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        durationMs: Long
    ): GestureType {
        val dx = endX - startX
        val dy = endY - startY
        val distance = sqrt(dx * dx + dy * dy)

        // Tap
        if (distance < tapThresholdPx) {
            return if (durationMs > longPressThresholdMs) {
                GestureType.LONG_PRESS
            } else {
                GestureType.TAP
            }
        }

        // Swipe
        if (distance > swipeThresholdPx) {
            return if (abs(dx) > abs(dy)) {
                if (dx > 0) GestureType.SWIPE_RIGHT else GestureType.SWIPE_LEFT
            } else {
                if (dy > 0) GestureType.SWIPE_DOWN else GestureType.SWIPE_UP
            }
        }

        // Short movement — treat as scroll
        return GestureType.SCROLL
    }
}
