package com.zz143.demo.tabs.coffee

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zz143.core.ZZ143

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoffeeTab(actions: CoffeeActions) {
    var screen by remember { mutableStateOf("menu") }
    var selectedProduct by remember { mutableStateOf<CoffeeProduct?>(null) }
    var lastOrderId by remember { mutableStateOf("") }

    // Observe reactive state
    val cartItems by actions.cart.collectAsState()

    AnimatedContent(targetState = screen, label = "coffee_screen") { currentScreen ->
        when (currentScreen) {
            "menu" -> MenuScreen(
                cartItemCount = cartItems.size,
                onProductTap = { product ->
                    selectedProduct = product
                    screen = "customize"
                },
                onCartTap = { screen = "cart" }
            )
            "customize" -> {
                val product = selectedProduct
                if (product != null) {
                    CustomizeScreen(
                        product = product,
                        actions = actions,
                        onAddedToCart = { screen = "menu" },
                        onBack = { screen = "menu" }
                    )
                }
            }
            "cart" -> CartScreen(
                actions = actions,
                onCheckout = { screen = "delivery" },
                onBack = { screen = "menu" }
            )
            "delivery" -> DeliveryScreen(
                actions = actions,
                onConfirmed = { orderId ->
                    lastOrderId = orderId
                    screen = "confirmation"
                },
                onBack = { screen = "cart" }
            )
            "confirmation" -> ConfirmationScreen(
                orderId = lastOrderId,
                onNewOrder = { screen = "menu" }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Menu
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuScreen(
    cartItemCount: Int,
    onProductTap: (CoffeeProduct) -> Unit,
    onCartTap: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coffee Menu") },
                actions = {
                    IconButton(onClick = onCartTap) {
                        BadgedBox(
                            badge = {
                                if (cartItemCount > 0) {
                                    Badge { Text("$cartItemCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SampleProducts.all, key = { it.id }) { product ->
                ProductCard(product = product, onClick = { onProductTap(product) })
            }
        }
    }
}

@Composable
private fun ProductCard(product: CoffeeProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                Spacer(Modifier.height(4.dp))
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${"%.2f".format(product.price)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Customize
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeScreen(
    product: CoffeeProduct,
    actions: CoffeeActions,
    onAddedToCart: () -> Unit,
    onBack: () -> Unit
) {
    var selectedSize by remember { mutableStateOf(Size.MEDIUM) }
    var selectedMilk by remember { mutableStateOf(MilkType.WHOLE) }
    var extraShot by remember { mutableStateOf(false) }
    var noSugar by remember { mutableStateOf(false) }

    val unitPrice = product.price +
        selectedSize.priceAdder +
        selectedMilk.price +
        if (extraShot) 0.75 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            // Product header
            Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(product.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))

            // Size
            Text("Size", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Size.entries.forEach { size ->
                    FilterChip(
                        selected = selectedSize == size,
                        onClick = { selectedSize = size },
                        label = {
                            Text(
                                if (size.priceAdder > 0) "${size.label} (+${"%.2f".format(size.priceAdder)})"
                                else size.label
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Milk
            Text("Milk", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MilkType.entries.forEach { milk ->
                    FilterChip(
                        selected = selectedMilk == milk,
                        onClick = { selectedMilk = milk },
                        label = {
                            Text(
                                if (milk.price > 0) "${milk.label} (+${"%.2f".format(milk.price)})"
                                else milk.label
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Extras
            Text("Extras", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = extraShot,
                    onClick = { extraShot = !extraShot },
                    label = { Text("Extra Shot (+0.75)") }
                )
                FilterChip(
                    selected = noSugar,
                    onClick = { noSugar = !noSugar },
                    label = { Text("No Sugar") }
                )
            }

            Spacer(Modifier.weight(1f))

            // Price and add button
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${"%.2f".format(unitPrice)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = {
                        // Track customization
                        ZZ143.trackAction(
                            "customize_drink",
                            mapOf(
                                "productId" to product.id,
                                "size" to selectedSize.name,
                                "milk" to selectedMilk.name,
                                "extraShot" to extraShot.toString(),
                                "noSugar" to noSugar.toString()
                            )
                        )
                        // Apply customization locally
                        actions.customizeDrink(
                            product.id,
                            selectedSize.name,
                            selectedMilk.name,
                            extraShot.toString(),
                            noSugar.toString()
                        )
                        // Track add to cart
                        ZZ143.trackAction(
                            "add_to_cart",
                            mapOf("productId" to product.id, "quantity" to "1")
                        )
                        actions.addToCart(product.id, 1)

                        onAddedToCart()
                    }) {
                        Text("Add to Cart")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Cart
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartScreen(
    actions: CoffeeActions,
    onCheckout: () -> Unit,
    onBack: () -> Unit
) {
    val cartItems by actions.cart.collectAsState()
    val activePromo by actions.activePromo.collectAsState()

    var promoInput by remember { mutableStateOf("") }
    var promoError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cart") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your cart is empty", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onBack) { Text("Browse menu") }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                // Item list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems) { item ->
                        CartItemRow(item)
                    }

                    // Promo code section
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("Promo Code", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = promoInput,
                                onValueChange = {
                                    promoInput = it
                                    promoError = false
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Enter code") },
                                singleLine = true,
                                isError = promoError
                            )
                            Button(onClick = {
                                ZZ143.trackAction(
                                    "apply_promo",
                                    mapOf("code" to promoInput)
                                )
                                val valid = actions.applyPromo(promoInput)
                                promoError = !valid
                            }) {
                                Text("Apply")
                            }
                        }
                        if (activePromo != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Promo $activePromo applied",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (promoError) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Invalid promo code",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Total and checkout
                Surface(tonalElevation = 2.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${"%.2f".format(actions.total)}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onCheckout,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Checkout")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(item: CartItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, fontWeight = FontWeight.SemiBold)
                Text(
                    item.customizationSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${"%.2f".format(item.totalPrice)}",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Delivery
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryScreen(
    actions: CoffeeActions,
    onConfirmed: (String) -> Unit,
    onBack: () -> Unit
) {
    val selectedMethod by actions.deliveryMethod.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delivery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("How would you like your order?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            DeliveryOptionCard(
                title = "Pickup",
                subtitle = "Ready in 5-10 minutes",
                selected = selectedMethod == "pickup",
                onClick = {
                    ZZ143.trackAction("select_delivery", mapOf("method" to "pickup"))
                    actions.selectDelivery("pickup")
                }
            )

            Spacer(Modifier.height(12.dp))

            DeliveryOptionCard(
                title = "Delivery",
                subtitle = "Delivered in 15-25 minutes",
                selected = selectedMethod == "delivery",
                onClick = {
                    ZZ143.trackAction("select_delivery", mapOf("method" to "delivery"))
                    actions.selectDelivery("delivery")
                }
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    ZZ143.trackAction("confirm_order", emptyMap())
                    val orderId = actions.confirmOrder()
                    onConfirmed(orderId)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm Order")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        border = if (selected) {
            CardDefaults.outlinedCardBorder()
        } else {
            null
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------
// Confirmation
// ---------------------------------------------------------------------------

@Composable
private fun ConfirmationScreen(
    orderId: String,
    onNewOrder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Order Confirmed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            orderId,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your order is being prepared.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNewOrder) {
            Text("New Order")
        }
    }
}
