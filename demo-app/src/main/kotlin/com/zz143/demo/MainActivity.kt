package com.zz143.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.zz143.core.ZZ143
import com.zz143.demo.tabs.coffee.CoffeeActions
import com.zz143.demo.tabs.expense.ExpenseActions
import com.zz143.demo.tabs.expense.ExpenseTab
import com.zz143.demo.tabs.settings.SettingsActions
import com.zz143.demo.tabs.settings.SettingsTab

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val coffeeActions = CoffeeActions()
    private val expenseActions = ExpenseActions()
    private val settingsActions = SettingsActions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ZZ143.registerActions(coffeeActions, expenseActions, settingsActions)

        setContent {
            MaterialTheme {
                DemoNavigation(
                    coffeeActions = coffeeActions,
                    expenseContent = { ExpenseTab(actions = expenseActions) },
                    settingsContent = { SettingsTab(settingsActions = settingsActions) }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ZZ143.unregisterActions(coffeeActions, expenseActions, settingsActions)
    }
}
