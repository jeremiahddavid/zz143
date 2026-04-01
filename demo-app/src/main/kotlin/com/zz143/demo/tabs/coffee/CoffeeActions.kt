package com.zz143.demo.tabs.coffee

import com.zz143.replay.annotation.WatchAction
import com.zz143.replay.annotation.WatchParam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CoffeeActions {

    // Internal reactive state
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _activePromo = MutableStateFlow<String?>(null)
    val activePromo: StateFlow<String?> = _activePromo.asStateFlow()

    private val _deliveryMethod = MutableStateFlow("pickup")
    val deliveryMethod: StateFlow<String> = _deliveryMethod.asStateFlow()

    // Last customization held in memory so add_to_cart can reference it
    private var pendingSize: Size = Size.MEDIUM
    private var pendingMilk: MilkType = MilkType.WHOLE
    private var pendingExtraShot: Boolean = false
    private var pendingNoSugar: Boolean = false

    val total: Double
        get() {
            val subtotal = _cart.value.sumOf { it.totalPrice }
            val discount = when (_activePromo.value) {
                "SAVE10" -> subtotal * 0.10
                "WELCOME" -> 1.00
                "COFFEE20" -> subtotal * 0.20
                else -> 0.0
            }
            return (subtotal - discount).coerceAtLeast(0.0)
        }

    // -----------------------------------------------------------------
    // @WatchAction methods — invoked by SDK replay and tracked manually
    // -----------------------------------------------------------------

    @WatchAction(
        type = "customize_drink",
        description = "Customize a drink with size, milk, and extras",
        screen = "coffee_customize"
    )
    fun customizeDrink(
        @WatchParam(name = "productId") productId: String,
        @WatchParam(name = "size") size: String,
        @WatchParam(name = "milk") milk: String,
        @WatchParam(name = "extraShot") extraShot: String,
        @WatchParam(name = "noSugar") noSugar: String
    ) {
        pendingSize = Size.entries.find { it.name.equals(size, ignoreCase = true) } ?: Size.MEDIUM
        pendingMilk = MilkType.entries.find { it.name.equals(milk, ignoreCase = true) } ?: MilkType.WHOLE
        pendingExtraShot = extraShot.toBooleanStrictOrNull() ?: false
        pendingNoSugar = noSugar.toBooleanStrictOrNull() ?: false
    }

    @WatchAction(
        type = "add_to_cart",
        description = "Add a customized drink to the cart",
        screen = "coffee_customize"
    )
    fun addToCart(
        @WatchParam(name = "productId") productId: String,
        @WatchParam(name = "quantity") quantity: Int = 1
    ) {
        val product = SampleProducts.all.find { it.id == productId } ?: return
        val item = CartItem(
            product = product,
            size = pendingSize,
            milk = pendingMilk,
            extraShot = pendingExtraShot,
            noSugar = pendingNoSugar,
            quantity = quantity
        )
        _cart.update { current -> current + item }
    }

    @WatchAction(
        type = "apply_promo",
        description = "Validate and apply a promo code",
        screen = "coffee_cart"
    )
    fun applyPromo(
        @WatchParam(name = "code") code: String
    ): Boolean {
        val normalized = code.uppercase().trim()
        return if (normalized in VALID_PROMOS) {
            _activePromo.value = normalized
            true
        } else {
            _activePromo.value = null
            false
        }
    }

    @WatchAction(
        type = "select_delivery",
        description = "Choose pickup or delivery",
        screen = "coffee_delivery"
    )
    fun selectDelivery(
        @WatchParam(name = "method") method: String
    ) {
        _deliveryMethod.value = method
    }

    @WatchAction(
        type = "confirm_order",
        description = "Place the order and clear the cart",
        screen = "coffee_delivery",
        idempotent = false
    )
    fun confirmOrder(): String {
        val orderId = "ORD-${System.currentTimeMillis().toString().takeLast(6)}"
        _cart.value = emptyList()
        _activePromo.value = null
        _deliveryMethod.value = "pickup"
        // Reset customization defaults
        pendingSize = Size.MEDIUM
        pendingMilk = MilkType.WHOLE
        pendingExtraShot = false
        pendingNoSugar = false
        return orderId
    }

    companion object {
        private val VALID_PROMOS = setOf("SAVE10", "WELCOME", "COFFEE20")
    }
}
