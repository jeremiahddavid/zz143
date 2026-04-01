package com.zz143.demo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zz143.core.ZZ143
import com.zz143.demo.debug.DebugOverlay
import com.zz143.demo.tabs.coffee.CoffeeActions
import com.zz143.demo.tabs.coffee.CoffeeTab
import com.zz143.suggest.compose.ZZ143SuggestionSheet

private enum class DemoTab(val label: String, val icon: ImageVector) {
    COFFEE("Coffee", Icons.Default.ShoppingCart),
    EXPENSE("Expense", Icons.Default.Edit),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DemoNavigation(
    coffeeActions: CoffeeActions,
    expenseContent: @Composable () -> Unit = { PlaceholderTab("Expense") },
    settingsContent: @Composable () -> Unit = { PlaceholderTab("Settings") }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDebug by remember { mutableStateOf(false) }

    val workflows by ZZ143.workflows.collectAsState()
    val activeSuggestion by ZZ143.activeSuggestion.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    DemoTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                if (tab == DemoTab.COFFEE && workflows.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                modifier = Modifier.combinedClickable(
                                                    onClick = {},
                                                    onLongClick = { showDebug = !showDebug }
                                                )
                                            ) {
                                                Text("${workflows.size}")
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> CoffeeTab(actions = coffeeActions)
                    1 -> expenseContent()
                    2 -> settingsContent()
                }
            }
        }

        // Suggestion overlay
        ZZ143SuggestionSheet(
            suggestion = activeSuggestion,
            onAccept = { suggestion -> ZZ143.acceptSuggestion(suggestion.suggestionId) },
            onDismiss = { suggestion -> ZZ143.dismissSuggestion(suggestion.suggestionId) },
            onReject = { suggestion -> ZZ143.rejectSuggestion(suggestion.suggestionId) }
        )

        // Debug overlay
        if (showDebug) {
            DebugOverlay(
                modifier = Modifier.align(Alignment.BottomCenter),
                onDismiss = { showDebug = false }
            )
        }
    }
}

@Composable
fun PlaceholderTab(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$name tab — handled by another agent",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
