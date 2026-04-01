package com.zz143.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.zz143.capture.CaptureEngine
import com.zz143.core.ZZ143
import com.zz143.core.id.UlidGenerator
import com.zz143.core.model.*

internal class ActivityLifecycleTracker : Application.ActivityLifecycleCallbacks {

    private var captureEngine: CaptureEngine? = null
    private var currentActivity: Activity? = null

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val screenId = ScreenId.fromActivity(activity.javaClass.name)
        emitNavigation(NavigationType.ACTIVITY_CREATED, null, screenId)
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity

        if (ZZ143.isCapturing.value) {
            if (captureEngine == null) {
                captureEngine = CaptureEngine(
                    config = ZZ143.config,
                    eventBus = ZZ143.eventBus,
                    scope = ZZ143.scope
                )
            }
            captureEngine?.attach(activity)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity === currentActivity) {
            captureEngine?.detach()
        }
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        val screenId = ScreenId.fromActivity(activity.javaClass.name)
        emitNavigation(NavigationType.ACTIVITY_DESTROYED, screenId, screenId)

        if (activity === currentActivity) {
            currentActivity = null
            captureEngine?.detach()
        }
    }

    private fun emitNavigation(type: NavigationType, from: ScreenId?, to: ScreenId) {
        val session = ZZ143.sessionId.value ?: return
        val event = ZZ143Event.Navigation(
            eventId = UlidGenerator.next(),
            sessionId = session,
            timestampMs = System.currentTimeMillis(),
            uptimeMs = android.os.SystemClock.elapsedRealtime(),
            screenId = to,
            navigation = NavigationEvent(
                type = type,
                fromScreenId = from,
                toScreenId = to,
                route = null,
                transitionType = null
            )
        )
        ZZ143.eventBus.tryEmit(event)
    }
}
