package com.zz143.demo.actions

import com.zz143.demo.data.CartItem
import com.zz143.demo.data.SampleData
import com.zz143.replay.annotation.WatchAction
import com.zz143.replay.annotation.WatchParam

class DemoActions {

    private val cart = mutableListOf<CartItem>()

    @WatchAction(type = "add_to_cart", description = "Add a drink to the cart")
    fun addToCart(
        @WatchParam(name = "productId", description = "Product identifier") productId: String,
        @WatchParam(name = "quantity", description = "Number to add") quantity: Int = 1
    ): Boolean {
        val product = SampleData.products.find { it.id == productId } ?: return false
        val existing = cart.find { it.product.id == productId }
        if (existing != null) {
            cart.remove(existing)
            cart.add(existing.copy(quantity = existing.quantity + quantity))
        } else {
            cart.add(CartItem(product, quantity))
        }
        return true
    }

    @WatchAction(type = "remove_from_cart", description = "Remove a drink from the cart")
    fun removeFromCart(
        @WatchParam(name = "productId") productId: String
    ): Boolean {
        return cart.removeAll { it.product.id == productId }
    }

    @WatchAction(
        type = "checkout",
        description = "Complete the order",
        idempotent = false
    )
    fun checkout(
        @WatchParam(name = "deliveryMethod", description = "pickup or delivery") deliveryMethod: String = "pickup"
    ): String {
        val total = cart.sumOf { it.product.price * it.quantity }
        val orderId = "ORD-${System.currentTimeMillis().toString().takeLast(6)}"
        cart.clear()
        return "Order $orderId placed! Total: ${"%.2f".format(total)} ($deliveryMethod)"
    }

    @WatchAction(type = "apply_promo", description = "Apply a promo code")
    fun applyPromo(
        @WatchParam(name = "code", description = "Promo code") code: String
    ): Boolean {
        return code.uppercase() in setOf("SAVE10", "WELCOME", "COFFEE20")
    }

    fun getCart(): List<CartItem> = cart.toList()
    fun getTotal(): Double = cart.sumOf { it.product.price * it.quantity }
}
