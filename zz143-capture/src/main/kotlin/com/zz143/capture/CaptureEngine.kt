package com.zz143.capture

import android.app.Activity
import android.view.View
import com.zz143.capture.gesture.GestureInterceptor
import com.zz143.capture.snapshot.DeltaComputer
import com.zz143.capture.viewtree.ViewTreeWalker
import com.zz143.core.ZZ143
import com.zz143.core.ZZ143Config
import com.zz143.core.event.EventBus
import com.zz143.core.id.UlidGenerator
import com.zz143.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaptureEngine(
    private val config: ZZ143Config,
    private val eventBus: EventBus,
    private val scope: CoroutineScope
) {
    private val viewTreeWalker = ViewTreeWalker(config)
    private val deltaComputer = DeltaComputer()

    private var lastSnapshot: ViewNode? = null
    private var lastSnapshotId: String? = null
    private var snapshotJob: Job? = null

    private val _currentScreenId = MutableStateFlow(ScreenId("unknown"))
    val currentScreenId: StateFlow<ScreenId> = _currentScreenId.asStateFlow()

    private val _screenChanges = MutableStateFlow<ScreenId>(ScreenId("unknown"))
    val screenChanges: StateFlow<ScreenId> = _screenChanges.asStateFlow()

    private var gestureInterceptor: GestureInterceptor? = null

    fun attach(activity: Activity) {
        val screenId = ScreenId.fromActivity(activity.javaClass.name)
        _currentScreenId.value = screenId
        _screenChanges.value = screenId

        // Install gesture interceptor
        gestureInterceptor = GestureInterceptor(
            eventBus = eventBus,
            sessionProvider = { ZZ143.sessionId.value },
            screenProvider = { _currentScreenId.value }
        )
        activity.window.callback = gestureInterceptor!!.wrapCallback(activity.window.callback)

        startSnapshotLoop(activity.window.decorView)
    }

    fun detach() {
        snapshotJob?.cancel()
        lastSnapshot = null
        lastSnapshotId = null
        viewTreeWalker.clearCaches()
    }

    fun updateScreenId(screenId: ScreenId) {
        _currentScreenId.value = screenId
        _screenChanges.value = screenId
        // Force a full snapshot on screen change
        lastSnapshot = null
        lastSnapshotId = null
    }

    private fun startSnapshotLoop(rootView: View) {
        snapshotJob?.cancel()
        snapshotJob = scope.launch {
            while (isActive) {
                if (ZZ143.isCapturing.value) {
                    captureSnapshot(rootView)
                }
                delay(config.snapshotIntervalMs)
            }
        }
    }

    private suspend fun captureSnapshot(rootView: View) {
        val sessionId = ZZ143.sessionId.value ?: return
        val screenId = _currentScreenId.value

        // Walk tree on main thread (must access View properties)
        val currentTree = withContext(Dispatchers.Main) {
            viewTreeWalker.walk(rootView, screenId)
        } ?: return

        val previous = lastSnapshot
        val previousId = lastSnapshotId

        if (previous == null || previousId == null) {
            // Full snapshot
            val snapshotId = UlidGenerator.next()
            val snapshot = ScreenSnapshot(
                snapshotId = snapshotId,
                sessionId = sessionId,
                screenId = screenId,
                timestampMs = System.currentTimeMillis(),
                uptimeMs = android.os.SystemClock.elapsedRealtime(),
                rootNode = currentTree,
                screenWidth = currentTree.bounds.width,
                screenHeight = currentTree.bounds.height,
                orientation = 0,
                isFullSnapshot = true
            )
            eventBus.emit(ZZ143Event.Snapshot(
                eventId = UlidGenerator.next(),
                sessionId = sessionId,
                timestampMs = snapshot.timestampMs,
                uptimeMs = snapshot.uptimeMs,
                screenId = screenId,
                snapshot = snapshot
            ))
            lastSnapshot = currentTree
            lastSnapshotId = snapshotId
        } else {
            // Incremental delta
            val mutations = deltaComputer.compute(previous, currentTree)
            if (mutations.isEmpty()) return

            if (deltaComputer.shouldEmitFullSnapshot(mutations, currentTree.nodeCount)) {
                // Too many changes — emit full snapshot instead
                lastSnapshot = null
                lastSnapshotId = null
                captureSnapshot(rootView = rootView as View)
                return
            }

            val delta = IncrementalDelta(
                deltaId = UlidGenerator.next(),
                baseSnapshotId = previousId,
                sessionId = sessionId,
                timestampMs = System.currentTimeMillis(),
                mutations = mutations
            )
            eventBus.emit(ZZ143Event.Delta(
                eventId = UlidGenerator.next(),
                sessionId = sessionId,
                timestampMs = delta.timestampMs,
                uptimeMs = android.os.SystemClock.elapsedRealtime(),
                screenId = screenId,
                delta = delta
            ))
            lastSnapshot = currentTree
        }
    }
}
