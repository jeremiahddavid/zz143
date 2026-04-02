package com.zz143.core.consent

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages user consent for SDK data collection.
 *
 * Consent is persisted to SharedPreferences so it survives app restarts.
 * When consent is revoked (set to [ConsentLevel.NONE]), the [onRevoked]
 * callback is invoked so the caller can clear captured data.
 */
class ConsentManager internal constructor(
    private val context: Context,
    private val onRevoked: () -> Unit
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _level = MutableStateFlow(loadPersistedLevel())
    val level: StateFlow<ConsentLevel> = _level.asStateFlow()

    /**
     * Grant consent at the given [level].
     * Takes effect immediately and is persisted.
     */
    fun grant(level: ConsentLevel) {
        val previous = _level.value
        _level.value = level
        persist(level)
        if (level == ConsentLevel.NONE && previous != ConsentLevel.NONE) {
            onRevoked()
        }
    }

    /**
     * Revoke all consent. Equivalent to `grant(ConsentLevel.NONE)`.
     * Triggers data clearing via the [onRevoked] callback.
     */
    fun revoke() {
        grant(ConsentLevel.NONE)
    }

    /**
     * Whether the SDK is allowed to capture events and learn patterns.
     */
    fun canCapture(): Boolean = _level.value == ConsentLevel.FULL

    /**
     * Whether the SDK is allowed to fire analytics callbacks.
     */
    fun canFireAnalytics(): Boolean =
        _level.value == ConsentLevel.FULL || _level.value == ConsentLevel.ANALYTICS_ONLY

    private fun persist(level: ConsentLevel) {
        prefs.edit().putString(KEY_CONSENT_LEVEL, level.name).apply()
    }

    private fun loadPersistedLevel(): ConsentLevel {
        val stored = prefs.getString(KEY_CONSENT_LEVEL, null) ?: return ConsentLevel.FULL
        return try {
            ConsentLevel.valueOf(stored)
        } catch (_: IllegalArgumentException) {
            ConsentLevel.FULL
        }
    }

    companion object {
        internal const val PREFS_NAME = "zz143_consent"
        internal const val KEY_CONSENT_LEVEL = "consent_level"
    }
}
