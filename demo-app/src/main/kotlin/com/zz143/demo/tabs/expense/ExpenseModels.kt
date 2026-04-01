package com.zz143.demo.tabs.expense

import java.util.UUID

data class ExpenseEntry(
    val id: String = UUID.randomUUID().toString(),
    val category: ExpenseCategory,
    val amount: Double,
    val project: ExpenseProject,
    val description: String,
    val hasReceipt: Boolean,
    val date: Long = System.currentTimeMillis(),
    val submittedAt: Long = System.currentTimeMillis()
)

enum class ExpenseCategory(val label: String) {
    TRAVEL("Travel"),
    MEALS("Meals"),
    OFFICE_SUPPLIES("Office Supplies"),
    SOFTWARE("Software"),
    EQUIPMENT("Equipment"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): ExpenseCategory? =
            entries.find { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
    }
}

enum class ExpenseProject(val label: String) {
    ALPHA("Alpha"),
    BETA("Beta"),
    GAMMA("Gamma"),
    INTERNAL("Internal");

    companion object {
        fun fromString(value: String): ExpenseProject? =
            entries.find { it.label.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
    }
}
