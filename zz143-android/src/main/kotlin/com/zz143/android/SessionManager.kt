package com.zz143.android

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.zz143.core.ZZ143
import com.zz143.core.id.UlidGenerator
import com.zz143.core.model.*

class SessionManager : DefaultLifecycleObserver {

    fun install() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        // App foregrounded
        val session = ZZ143.sessionId.value ?: return
        val event = ZZ143Event.Lifecycle(
            eventId = UlidGenerator.next(),
            sessionId = session,
            timestampMs = System.currentTimeMillis(),
            uptimeMs = android.os.SystemClock.elapsedRealtime(),
            screenId = ScreenId("app"),
            type = LifecycleType.APP_FOREGROUNDED
        )
        ZZ143.eventBus.tryEmit(event)
    }

    override fun onStop(owner: LifecycleOwner) {
        // App backgrounded
        val session = ZZ143.sessionId.value ?: return
        val event = ZZ143Event.Lifecycle(
            eventId = UlidGenerator.next(),
            sessionId = session,
            timestampMs = System.currentTimeMillis(),
            uptimeMs = android.os.SystemClock.elapsedRealtime(),
            screenId = ScreenId("app"),
            type = LifecycleType.APP_BACKGROUNDED
        )
        ZZ143.eventBus.tryEmit(event)
    }
}
