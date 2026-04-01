package com.zz143.capture.gesture

import android.view.MotionEvent
import android.view.Window
import com.zz143.core.event.EventBus
import com.zz143.core.id.UlidGenerator
import com.zz143.core.model.*

internal class GestureInterceptor(
    private val eventBus: EventBus,
    private val sessionProvider: () -> SessionId?,
    private val screenProvider: () -> ScreenId
) {
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime = 0L
    private val classifier = GestureClassifier()

    fun wrapCallback(original: Window.Callback): Window.Callback {
        return GestureCapturingCallback(original, this)
    }

    internal fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.rawX
                touchStartY = event.rawY
                touchStartTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                val sessionId = sessionProvider() ?: return
                val duration = System.currentTimeMillis() - touchStartTime
                val gestureType = classifier.classify(
                    startX = touchStartX, startY = touchStartY,
                    endX = event.rawX, endY = event.rawY,
                    durationMs = duration
                )

                val gestureEvent = GestureEvent(
                    type = gestureType,
                    targetElementId = null, // resolved later by CaptureEngine
                    startX = touchStartX,
                    startY = touchStartY,
                    endX = if (gestureType == GestureType.TAP) null else event.rawX,
                    endY = if (gestureType == GestureType.TAP) null else event.rawY,
                    durationMs = duration,
                    pointerCount = event.pointerCount
                )

                val zz143Event = ZZ143Event.Gesture(
                    eventId = UlidGenerator.next(),
                    sessionId = sessionId,
                    timestampMs = System.currentTimeMillis(),
                    uptimeMs = android.os.SystemClock.elapsedRealtime(),
                    screenId = screenProvider(),
                    gesture = gestureEvent
                )

                eventBus.tryEmit(zz143Event)
            }
        }
    }
}

private class GestureCapturingCallback(
    private val original: Window.Callback,
    private val interceptor: GestureInterceptor
) : Window.Callback by original {

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event?.let { interceptor.onTouchEvent(it) }
        return original.dispatchTouchEvent(event)
    }
}
