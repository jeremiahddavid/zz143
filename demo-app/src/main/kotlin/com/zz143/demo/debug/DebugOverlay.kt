package com.zz143.demo.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zz143.core.ZZ143
import com.zz143.core.model.Workflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Floating debug panel that displays action logs, detected workflows,
 * and controls for force-analysis and data clearing.
 */
@Composable
fun DebugOverlay(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val workflows by ZZ143.workflows.collectAsState()

    // Action log: subscribe to the event bus and record action names + timestamps
    val actionLog = remember { mutableStateListOf<ActionLogEntry>() }

    LaunchedEffect(Unit) {
        ZZ143.eventBus.events.collect { event ->
            if (event is com.zz143.core.model.ZZ143Event.Action) {
                val entry = ActionLogEntry(
                    timestamp = System.currentTimeMillis(),
                    actionType = event.action.actionType,
                    params = event.action.parameters
                )
                actionLog.add(0, entry) // newest first
                if (actionLog.size > 50) actionLog.removeLast()
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
            }

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Debug", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) { Text("Close") }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { ZZ143.forceAnalysis() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Force Analysis")
                }
                OutlinedButton(
                    onClick = {
                        ZZ143.clearAllData()
                        actionLog.clear()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Data")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Tabs for log vs workflows
            var debugTab by remember { mutableIntStateOf(0) }
            TabRow(selectedTabIndex = debugTab) {
                Tab(selected = debugTab == 0, onClick = { debugTab = 0 }, text = { Text("Action Log (${actionLog.size})") })
                Tab(selected = debugTab == 1, onClick = { debugTab = 1 }, text = { Text("Workflows (${workflows.size})") })
            }

            when (debugTab) {
                0 -> ActionLogList(actionLog)
                1 -> WorkflowList(workflows)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Action Log
// ---------------------------------------------------------------------------

data class ActionLogEntry(
    val timestamp: Long,
    val actionType: String,
    val params: Map<String, String>
)

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
private fun ActionLogList(entries: List<ActionLogEntry>) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No actions tracked yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(entries) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        timeFormat.format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(entry.actionType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        if (entry.params.isNotEmpty()) {
                            Text(
                                entry.params.entries.joinToString(", ") { "${it.key}=${it.value}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Workflow List
// ---------------------------------------------------------------------------

@Composable
private fun WorkflowList(workflows: List<Workflow>) {
    if (workflows.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No workflows detected yet.\nUse the app and tap Force Analysis.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(workflows, key = { it.workflowId }) { workflow ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(workflow.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "${"%.0f".format(workflow.confidenceScore * 100)}%",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            workflow.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${workflow.steps.size} steps | ${workflow.status.name.lowercase()} | ${workflow.executionCount}x seen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
