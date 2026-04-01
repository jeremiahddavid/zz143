package com.zz143.demo.tabs.settings

import com.zz143.replay.annotation.WatchAction
import com.zz143.replay.annotation.WatchParam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsActions {

    // Reactive state for each setting
    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _notifications = MutableStateFlow(true)
    val notifications: StateFlow<Boolean> = _notifications.asStateFlow()

    private val _location = MutableStateFlow(true)
    val location: StateFlow<Boolean> = _location.asStateFlow()

    private val _language = MutableStateFlow("en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _units = MutableStateFlow("imperial")
    val units: StateFlow<String> = _units.asStateFlow()

    private val _autoUpdate = MutableStateFlow(true)
    val autoUpdate: StateFlow<Boolean> = _autoUpdate.asStateFlow()

    @WatchAction(type = "apply_settings", idempotent = true)
    fun applySettings(
        @WatchParam(name = "darkMode") darkMode: String,
        @WatchParam(name = "notifications") notifications: String,
        @WatchParam(name = "location") location: String,
        @WatchParam(name = "language") language: String,
        @WatchParam(name = "units") units: String,
        @WatchParam(name = "autoUpdate") autoUpdate: String
    ): Boolean {
        _darkMode.value = darkMode.toBooleanStrictOrNull() ?: false
        _notifications.value = notifications.toBooleanStrictOrNull() ?: true
        _location.value = location.toBooleanStrictOrNull() ?: true
        _language.value = language
        _units.value = units
        _autoUpdate.value = autoUpdate.toBooleanStrictOrNull() ?: true
        return true
    }

    @WatchAction(type = "reset_defaults")
    fun resetDefaults(): Boolean {
        _darkMode.value = false
        _notifications.value = true
        _location.value = true
        _language.value = "en"
        _units.value = "imperial"
        _autoUpdate.value = true
        return true
    }
}
