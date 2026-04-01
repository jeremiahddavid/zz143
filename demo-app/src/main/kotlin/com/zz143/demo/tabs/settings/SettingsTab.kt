package com.zz143.demo.tabs.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zz143.core.ZZ143
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(settingsActions: SettingsActions) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Observe reactive state from SettingsActions so SDK replay updates toggles
    val darkMode by settingsActions.darkMode.collectAsState()
    val notifications by settingsActions.notifications.collectAsState()
    val location by settingsActions.location.collectAsState()
    val language by settingsActions.language.collectAsState()
    val units by settingsActions.units.collectAsState()
    val autoUpdate by settingsActions.autoUpdate.collectAsState()

    // Local mutable copies for editing before "Apply"
    var localDarkMode by remember { mutableStateOf(darkMode) }
    var localNotifications by remember { mutableStateOf(notifications) }
    var localLocation by remember { mutableStateOf(location) }
    var localLanguage by remember { mutableStateOf(language) }
    var localUnits by remember { mutableStateOf(units) }
    var localAutoUpdate by remember { mutableStateOf(autoUpdate) }

    // Sync local state when the observed StateFlows change (e.g. from SDK replay)
    LaunchedEffect(darkMode) { localDarkMode = darkMode }
    LaunchedEffect(notifications) { localNotifications = notifications }
    LaunchedEffect(location) { localLocation = location }
    LaunchedEffect(language) { localLanguage = language }
    LaunchedEffect(units) { localUnits = units }
    LaunchedEffect(autoUpdate) { localAutoUpdate = autoUpdate }

    // Track "open_settings" when the tab is first displayed
    LaunchedEffect(Unit) {
        ZZ143.trackAction("open_settings")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Dark Mode
            SettingSwitchRow(
                label = "Dark Mode",
                checked = localDarkMode,
                onCheckedChange = { localDarkMode = it }
            )

            // Notifications
            SettingSwitchRow(
                label = "Notifications",
                checked = localNotifications,
                onCheckedChange = { localNotifications = it }
            )

            // Location Services
            SettingSwitchRow(
                label = "Location Services",
                checked = localLocation,
                onCheckedChange = { localLocation = it }
            )

            // Language
            Text(
                text = "Language",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val languages = listOf("en" to "EN", "es" to "ES", "fr" to "FR", "de" to "DE", "ja" to "JA")
                for ((code, label) in languages) {
                    FilterChip(
                        selected = localLanguage == code,
                        onClick = { localLanguage = code },
                        label = { Text(label) }
                    )
                }
            }

            // Units
            Text(
                text = "Units",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = localUnits == "metric",
                    onClick = { localUnits = "metric" },
                    label = { Text("Metric") }
                )
                FilterChip(
                    selected = localUnits == "imperial",
                    onClick = { localUnits = "imperial" },
                    label = { Text("Imperial") }
                )
            }

            // Auto-Update
            SettingSwitchRow(
                label = "Auto-Update",
                checked = localAutoUpdate,
                onCheckedChange = { localAutoUpdate = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Apply Settings button
            Button(
                onClick = {
                    val params = mapOf(
                        "darkMode" to localDarkMode.toString(),
                        "notifications" to localNotifications.toString(),
                        "location" to localLocation.toString(),
                        "language" to localLanguage,
                        "units" to localUnits,
                        "autoUpdate" to localAutoUpdate.toString()
                    )
                    settingsActions.applySettings(
                        darkMode = localDarkMode.toString(),
                        notifications = localNotifications.toString(),
                        location = localLocation.toString(),
                        language = localLanguage,
                        units = localUnits,
                        autoUpdate = localAutoUpdate.toString()
                    )
                    ZZ143.trackAction("open_settings")
                    ZZ143.trackAction("apply_settings", params)
                    scope.launch {
                        snackbarHostState.showSnackbar("Settings applied!")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Settings")
            }

            // Reset to Defaults button
            TextButton(
                onClick = {
                    settingsActions.resetDefaults()
                    ZZ143.trackAction("reset_defaults")
                    scope.launch {
                        snackbarHostState.showSnackbar("Settings reset to defaults")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
