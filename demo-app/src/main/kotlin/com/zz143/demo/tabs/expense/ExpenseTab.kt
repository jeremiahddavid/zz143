package com.zz143.demo.tabs.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zz143.core.ZZ143
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTab(actions: ExpenseActions) {
    var screen by remember { mutableStateOf("list") }
    var lastSubmittedEntry by remember { mutableStateOf<ExpenseEntry?>(null) }

    when (screen) {
        "list" -> ExpenseListScreen(
            actions = actions,
            onNewExpense = { screen = "form" }
        )
        "form" -> ExpenseFormScreen(
            actions = actions,
            onSubmitted = { entry ->
                lastSubmittedEntry = entry
                screen = "confirmation"
            },
            onBack = { screen = "list" }
        )
        "confirmation" -> ExpenseConfirmationScreen(
            entry = lastSubmittedEntry,
            onSubmitAnother = { screen = "form" },
            onBackToList = { screen = "list" }
        )
    }
}

// ---------------------------------------------------------------------------
// List screen
// ---------------------------------------------------------------------------

@Composable
private fun ExpenseListScreen(
    actions: ExpenseActions,
    onNewExpense: () -> Unit
) {
    val expenses by actions.expenses.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewExpense) {
                Icon(Icons.Default.Add, contentDescription = "New expense")
            }
        }
    ) { padding ->
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No expenses submitted yet.\nTap + to create one.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses, key = { it.id }) { entry ->
                    ExpenseCard(entry)
                }
            }
        }
    }
}

@Composable
private fun ExpenseCard(entry: ExpenseEntry) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryIcon(entry.category),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.category.label, fontWeight = FontWeight.Bold)
                Text(entry.project.label, style = MaterialTheme.typography.bodySmall)
                if (entry.description.isNotBlank()) {
                    Text(
                        entry.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Text(
                    dateFormat.format(Date(entry.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${"%.2f".format(entry.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (entry.hasReceipt) {
                    Text(
                        "Receipt",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

private fun categoryIcon(category: ExpenseCategory): String = when (category) {
    ExpenseCategory.TRAVEL -> "\u2708"          // airplane
    ExpenseCategory.MEALS -> "\u2615"           // hot beverage
    ExpenseCategory.OFFICE_SUPPLIES -> "\u270F" // pencil
    ExpenseCategory.SOFTWARE -> "\u2328"        // keyboard
    ExpenseCategory.EQUIPMENT -> "\u2699"       // gear
    ExpenseCategory.OTHER -> "\u2022"           // bullet
}

// ---------------------------------------------------------------------------
// Form screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseFormScreen(
    actions: ExpenseActions,
    onSubmitted: (ExpenseEntry) -> Unit,
    onBack: () -> Unit
) {
    // Observe pre-fill state from replay
    val preFillCategory by actions.preFillCategory.collectAsState()
    val preFillProject by actions.preFillProject.collectAsState()
    val preFillHasReceipt by actions.preFillHasReceipt.collectAsState()

    // Form fields
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.OTHER) }
    var selectedProject by remember { mutableStateOf(ExpenseProject.INTERNAL) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hasReceipt by remember { mutableStateOf(false) }

    // Apply pre-fill when replay sets values (category, project, hasReceipt only)
    LaunchedEffect(preFillCategory) {
        preFillCategory?.let { selectedCategory = it }
    }
    LaunchedEffect(preFillProject) {
        preFillProject?.let { selectedProject = it }
    }
    LaunchedEffect(preFillHasReceipt) {
        preFillHasReceipt?.let { hasReceipt = it }
    }

    // Dropdown expanded state
    var categoryExpanded by remember { mutableStateOf(false) }
    var projectExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("New Expense", style = MaterialTheme.typography.headlineSmall)

        // Category dropdown
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedCategory.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                ExpenseCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.label) },
                        onClick = {
                            selectedCategory = category
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        // Project dropdown
        ExposedDropdownMenuBox(
            expanded = projectExpanded,
            onExpandedChange = { projectExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedProject.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Project") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = projectExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = projectExpanded,
                onDismissRequest = { projectExpanded = false }
            ) {
                ExpenseProject.entries.forEach { project ->
                    DropdownMenuItem(
                        text = { Text(project.label) },
                        onClick = {
                            selectedProject = project
                            projectExpanded = false
                        }
                    )
                }
            }
        }

        // Amount
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                // Allow only valid decimal input
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    amount = newValue
                }
            },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )

        // Receipt switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Attach Receipt")
            Switch(
                checked = hasReceipt,
                onCheckedChange = { hasReceipt = it }
            )
        }

        Spacer(Modifier.weight(1f))

        // Submit button
        val parsedAmount = amount.toDoubleOrNull()
        Button(
            onClick = {
                val amt = parsedAmount ?: return@Button

                // Sync form state into actions so submitExpense reads current values
                actions.updateFormState(
                    category = selectedCategory,
                    amount = amt,
                    project = selectedProject,
                    description = description,
                    hasReceipt = hasReceipt
                )

                // Track the fill action for pattern detection
                ZZ143.trackAction(
                    "fill_expense_form",
                    mapOf(
                        "category" to selectedCategory.label,
                        "amount" to amount,
                        "project" to selectedProject.label,
                        "description" to description,
                        "hasReceipt" to hasReceipt.toString()
                    )
                )

                // Track submission
                ZZ143.trackAction("submit_expense")

                // Actually submit
                val entryId = actions.submitExpense()

                // Build the entry for confirmation display
                val entry = ExpenseEntry(
                    id = entryId,
                    category = selectedCategory,
                    amount = amt,
                    project = selectedProject,
                    description = description,
                    hasReceipt = hasReceipt
                )
                onSubmitted(entry)
            },
            enabled = parsedAmount != null && parsedAmount > 0.0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Expense")
        }

        // Back link
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

// ---------------------------------------------------------------------------
// Confirmation screen
// ---------------------------------------------------------------------------

@Composable
private fun ExpenseConfirmationScreen(
    entry: ExpenseEntry?,
    onSubmitAnother: () -> Unit,
    onBackToList: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Expense submitted!", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(24.dp))

        if (entry != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SummaryRow("Category", entry.category.label)
                    SummaryRow("Amount", "${"%.2f".format(entry.amount)}")
                    SummaryRow("Project", entry.project.label)
                    if (entry.description.isNotBlank()) {
                        SummaryRow("Description", entry.description)
                    }
                    SummaryRow("Receipt", if (entry.hasReceipt) "Yes" else "No")
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onSubmitAnother,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Another")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBackToList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to List")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
