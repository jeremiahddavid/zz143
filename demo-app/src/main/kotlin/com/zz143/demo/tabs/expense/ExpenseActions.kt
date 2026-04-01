package com.zz143.demo.tabs.expense

import com.zz143.replay.annotation.WatchAction
import com.zz143.replay.annotation.WatchParam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExpenseActions {

    // Submitted expenses
    private val _expenses = MutableStateFlow<List<ExpenseEntry>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntry>> = _expenses.asStateFlow()

    // Pre-fill state (set by replay, observed by the form)
    private val _preFillCategory = MutableStateFlow<ExpenseCategory?>(null)
    val preFillCategory: StateFlow<ExpenseCategory?> = _preFillCategory.asStateFlow()

    private val _preFillProject = MutableStateFlow<ExpenseProject?>(null)
    val preFillProject: StateFlow<ExpenseProject?> = _preFillProject.asStateFlow()

    private val _preFillHasReceipt = MutableStateFlow<Boolean?>(null)
    val preFillHasReceipt: StateFlow<Boolean?> = _preFillHasReceipt.asStateFlow()

    // Current form state held here so submitExpense can read it
    private var currentCategory: ExpenseCategory = ExpenseCategory.OTHER
    private var currentAmount: Double = 0.0
    private var currentProject: ExpenseProject = ExpenseProject.INTERNAL
    private var currentDescription: String = ""
    private var currentHasReceipt: Boolean = false

    @WatchAction(type = "fill_expense_form", description = "Pre-fill the expense form fields")
    fun fillExpenseForm(
        @WatchParam(name = "category", description = "Expense category") category: String,
        @WatchParam(name = "amount", description = "Expense amount") amount: String,
        @WatchParam(name = "project", description = "Project name") project: String,
        @WatchParam(name = "description", description = "Expense description") description: String,
        @WatchParam(name = "hasReceipt", description = "Whether a receipt is attached") hasReceipt: String
    ): Boolean {
        val parsedCategory = ExpenseCategory.fromString(category)
        val parsedProject = ExpenseProject.fromString(project)
        val parsedReceipt = hasReceipt.toBooleanStrictOrNull()

        // Update pre-fill state so the Compose form can observe and auto-populate
        if (parsedCategory != null) _preFillCategory.value = parsedCategory
        if (parsedProject != null) _preFillProject.value = parsedProject
        if (parsedReceipt != null) _preFillHasReceipt.value = parsedReceipt

        // Also set current form state for submitExpense
        if (parsedCategory != null) currentCategory = parsedCategory
        if (parsedProject != null) currentProject = parsedProject
        if (parsedReceipt != null) currentHasReceipt = parsedReceipt
        amount.toDoubleOrNull()?.let { currentAmount = it }
        if (description.isNotBlank()) currentDescription = description

        return true
    }

    @WatchAction(
        type = "submit_expense",
        description = "Submit the current expense entry",
        idempotent = false
    )
    fun submitExpense(): String {
        val entry = ExpenseEntry(
            category = currentCategory,
            amount = currentAmount,
            project = currentProject,
            description = currentDescription,
            hasReceipt = currentHasReceipt
        )
        _expenses.value = _expenses.value + entry

        // Reset current form state
        currentCategory = ExpenseCategory.OTHER
        currentAmount = 0.0
        currentProject = ExpenseProject.INTERNAL
        currentDescription = ""
        currentHasReceipt = false

        // Clear pre-fill state
        _preFillCategory.value = null
        _preFillProject.value = null
        _preFillHasReceipt.value = null

        return entry.id
    }

    /**
     * Update the current form state from the Compose UI so that submitExpense
     * creates the correct entry.
     */
    fun updateFormState(
        category: ExpenseCategory,
        amount: Double,
        project: ExpenseProject,
        description: String,
        hasReceipt: Boolean
    ) {
        currentCategory = category
        currentAmount = amount
        currentProject = project
        currentDescription = description
        currentHasReceipt = hasReceipt
    }

    fun getExpenses(): List<ExpenseEntry> = _expenses.value
}
