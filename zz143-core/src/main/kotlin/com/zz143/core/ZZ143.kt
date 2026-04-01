package com.zz143.core

import android.content.Context
import com.zz143.core.event.EventBus
import com.zz143.core.id.UlidGenerator
import com.zz143.core.model.*
import com.zz143.core.storage.FileQueue
import com.zz143.core.storage.ZZ143Database
import com.zz143.core.threading.ZZ143Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object ZZ143 {
    private var _isInitialized = false
    val isInitialized: Boolean get() = _isInitialized

    private var _config = ZZ143Config()
    val config: ZZ143Config get() = _config

    internal lateinit var eventBus: EventBus
    internal lateinit var dispatchers: ZZ143Dispatchers
    internal lateinit var database: ZZ143Database
    internal lateinit var fileQueue: FileQueue
    internal lateinit var scope: CoroutineScope
    internal lateinit var appContext: Context

    private val _sessionId = MutableStateFlow<SessionId?>(null)
    val sessionId: StateFlow<SessionId?> = _sessionId.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    fun init(context: Context, config: ZZ143Config = ZZ143Config()) {
        if (_isInitialized) return

        appContext = context.applicationContext
        _config = config
        dispatchers = ZZ143Dispatchers()
        scope = CoroutineScope(SupervisorJob() + dispatchers.io)
        eventBus = EventBus(config.eventFilter)
        database = ZZ143Database(appContext)
        fileQueue = FileQueue(
            directory = File(appContext.filesDir, "zz143/queue"),
            maxTotalSize = config.maxQueueSizeMb * 1024L * 1024L,
            dispatchers = dispatchers
        )

        _isInitialized = true
        log("ZZ143 initialized (v${BuildConfig.VERSION_NAME})")
    }

    fun init(context: Context, block: ZZ143Config.Builder.() -> Unit) {
        val builder = ZZ143Config.Builder()
        builder.block()
        init(context, builder.build())
    }

    fun startCapturing() {
        checkInitialized()
        if (_isCapturing.value) return
        _sessionId.value = SessionId(UlidGenerator.next())
        _isCapturing.value = true
        log("Capturing started (session: ${_sessionId.value?.value})")
    }

    fun stopCapturing() {
        if (!_isCapturing.value) return
        _isCapturing.value = false
        _sessionId.value = null
        log("Capturing stopped")
    }

    fun trackAction(actionType: String, parameters: Map<String, String> = emptyMap()) {
        checkInitialized()
        val session = _sessionId.value ?: return
        val event = ZZ143Event.Action(
            eventId = UlidGenerator.next(),
            sessionId = session,
            timestampMs = System.currentTimeMillis(),
            uptimeMs = android.os.SystemClock.elapsedRealtime(),
            screenId = ScreenId("unknown"),
            action = SemanticAction(
                actionType = actionType,
                actionSource = ActionSource.ANNOTATION,
                targetElementId = null,
                parameters = parameters
            )
        )
        eventBus.tryEmit(event)
    }

    // Action registration (delegates to replay module if available)
    private val actionRegistrations = mutableListOf<Any>()

    fun registerActions(vararg targets: Any) {
        actionRegistrations.addAll(targets)
        log("Registered ${targets.size} action targets")
    }

    fun unregisterActions(vararg targets: Any) {
        actionRegistrations.removeAll(targets.toSet())
        log("Unregistered ${targets.size} action targets")
    }

    fun getRegisteredTargets(): List<Any> = actionRegistrations.toList()

    fun clearAllData() {
        checkInitialized()
        val db = database.writableDatabase
        db.execSQL("DELETE FROM events")
        db.execSQL("DELETE FROM semantic_actions")
        db.execSQL("DELETE FROM workflows")
        db.execSQL("DELETE FROM workflow_instances")
        db.execSQL("DELETE FROM suggestions")
        db.execSQL("DELETE FROM pattern_ngrams")
        db.execSQL("DELETE FROM user_preferences")
        log("All data cleared")
    }

    private fun checkInitialized() {
        check(_isInitialized) { "ZZ143.init() must be called before using the SDK" }
    }

    internal fun log(message: String) {
        if (_config.debugLogging) {
            android.util.Log.d("ZZ143", message)
        }
    }

    // Visible for testing
    internal object BuildConfig {
        const val VERSION_NAME = "0.1.0-alpha01"
    }
}
