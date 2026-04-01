package com.zz143.android

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.zz143.core.ZZ143

class ZZ143Initializer : Initializer<ZZ143> {
    override fun create(context: Context): ZZ143 {
        // Auto-init with defaults if not already initialized
        // Developers can call ZZ143.init() manually before this for custom config
        if (!ZZ143.isInitialized) {
            ZZ143.init(context)
        }

        // Install lifecycle tracking
        val app = context.applicationContext as? Application
        app?.let {
            val lifecycleTracker = ActivityLifecycleTracker()
            it.registerActivityLifecycleCallbacks(lifecycleTracker)
        }

        return ZZ143
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
