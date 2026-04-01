package com.zz143.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zz143.core.ZZ143
import com.zz143.core.model.Suggestion
import com.zz143.demo.actions.DemoActions
import com.zz143.demo.data.Product
import com.zz143.demo.data.SampleData
import com.zz143.suggest.compose.ZZ143SuggestionSheet

class MainActivity : ComponentActivity() {

    private val demoActions = DemoActions()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ZZ143.registerActions(demoActions)

        setContent {
            MaterialTheme {
                val activeSuggestion by ZZ143.activeSuggestion.collectAsState()

                Box {
                    DemoApp(demoActions)

                    // Suggestion bottom sheet — appears when SDK detects a pattern
                    ZZ143SuggestionSheet(
                        suggestion = activeSuggestion,
                        onAccept = { suggestion ->
                            ZZ143.acceptSuggestion(suggestion.suggestionId)
                        },
                        onDismiss = { suggestion ->
                            ZZ143.dismissSuggestion(suggestion.suggestionId)
                        },
                        onReject = { suggestion ->
                            ZZ143.rejectSuggestion(suggestion.suggestionId)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ZZ143.unregisterActions(demoActions)
    }
}

@Composable
fun DemoApp(actions: DemoActions) {
    var currentScreen by remember { mutableStateOf("menu") }
    var cartCount by remember { mutableIntStateOf(0) }
    var orderResult by remember { mutableStateOf<String?>(null) }

    // Show detected workflows count in the top bar
    val workflows by ZZ143.workflows.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("zz143 Coffee Demo") },
                actions = {
                    if (workflows.isNotEmpty()) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("${workflows.size}")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    TextButton(onClick = { currentScreen = "cart" }) {
                        Text("Cart ($cartCount)")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "menu" -> MenuScreen(
                    products = SampleData.products,
                    onAddToCart = { product ->
                        actions.addToCart(product.id, 1)
                        ZZ143.trackAction("add_to_cart", mapOf("productId" to product.id))
                        cartCount = actions.getCart().sumOf { it.quantity }
                    }
                )
                "cart" -> CartScreen(
                    items = actions.getCart(),
                    total = actions.getTotal(),
                    onCheckout = { delivery ->
                        val result = actions.checkout(delivery)
                        ZZ143.trackAction("checkout", mapOf("deliveryMethod" to delivery))
                        orderResult = result
                        cartCount = 0
                        currentScreen = "confirmation"
                    },
                    onBack = { currentScreen = "menu" }
                )
                "confirmation" -> ConfirmationScreen(
                    message = orderResult ?: "Order placed!",
                    onNewOrder = {
                        orderResult = null
                        currentScreen = "menu"
                    }
                )
            }
        }
    }
}

@Composable
fun MenuScreen(products: List<Product>, onAddToCart: (Product) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products) { product ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold)
                        Text(product.description, style = MaterialTheme.typography.bodySmall)
                        Text("$${product.price}", color = MaterialTheme.colorScheme.primary)
                    }
                    Button(onClick = { onAddToCart(product) }) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(
    items: List<com.zz143.demo.data.CartItem>,
    total: Double,
    onCheckout: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (items.isEmpty()) {
            Text("Cart is empty")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Browse menu") }
        } else {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.product.name} x${item.quantity}")
                    Text("${"%.2f".format(item.product.price * item.quantity)}")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontWeight = FontWeight.Bold)
                Text("${"%.2f".format(total)}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onCheckout("pickup") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Checkout (Pickup)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onCheckout("delivery") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Checkout (Delivery)")
            }
        }
    }
}

@Composable
fun ConfirmationScreen(message: String, onNewOrder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNewOrder) {
            Text("New Order")
        }
    }
}
