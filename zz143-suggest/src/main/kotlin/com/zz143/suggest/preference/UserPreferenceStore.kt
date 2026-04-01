package com.zz143.suggest.preference

import java.util.concurrent.ConcurrentHashMap

class UserPreferenceStore {

    private data class WorkflowPrefs(
        var lastShownMs: Long = 0L,
        var consecutiveDismissals: Int = 0,
        var isRejected: Boolean = false,
        var isDisabled: Boolean = false,
        var acceptCount: Int = 0,
        var totalShown: Int = 0
    )

    private val prefs = ConcurrentHashMap<String, WorkflowPrefs>()

    private fun getOrCreate(workflowId: String): WorkflowPrefs =
        prefs.getOrPut(workflowId) { WorkflowPrefs() }

    fun lastSuggestionShownMs(workflowId: String): Long =
        prefs[workflowId]?.lastShownMs ?: 0L

    fun consecutiveDismissals(workflowId: String): Int =
        prefs[workflowId]?.consecutiveDismissals ?: 0

    fun isRejected(workflowId: String): Boolean =
        prefs[workflowId]?.isRejected ?: false

    fun isExplicitlyDisabled(workflowId: String): Boolean =
        prefs[workflowId]?.isDisabled ?: false

    fun recordShown(workflowId: String) {
        val p = getOrCreate(workflowId)
        p.lastShownMs = System.currentTimeMillis()
        p.totalShown++
    }

    fun recordAccepted(workflowId: String) {
        val p = getOrCreate(workflowId)
        p.consecutiveDismissals = 0
        p.isRejected = false
        p.acceptCount++
    }

    fun recordDismissed(workflowId: String) {
        val p = getOrCreate(workflowId)
        p.consecutiveDismissals++
    }

    fun recordRejected(workflowId: String) {
        val p = getOrCreate(workflowId)
        p.isRejected = true
        // After 3 rejections, permanently disable
        if (p.consecutiveDismissals >= 3 || p.isRejected) {
            p.isDisabled = true
        }
    }

    fun reset(workflowId: String) {
        prefs.remove(workflowId)
    }

    fun resetAll() {
        prefs.clear()
    }
}
