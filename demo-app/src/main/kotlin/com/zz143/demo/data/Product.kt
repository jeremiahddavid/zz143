package com.zz143.demo.data

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrl: String = ""
)

data class CartItem(
    val product: Product,
    val quantity: Int
)

object SampleData {
    val products = listOf(
        Product("latte", "Caffe Latte", 4.95, "Smooth espresso with steamed milk"),
        Product("cappuccino", "Cappuccino", 4.50, "Rich espresso with foamy milk"),
        Product("americano", "Americano", 3.75, "Bold espresso with hot water"),
        Product("mocha", "Mocha", 5.50, "Espresso with chocolate and steamed milk"),
        Product("cold-brew", "Cold Brew", 4.25, "Slow-steeped cold coffee"),
        Product("matcha", "Matcha Latte", 5.25, "Japanese green tea with milk"),
        Product("chai", "Chai Latte", 4.75, "Spiced tea with steamed milk"),
        Product("espresso", "Espresso", 3.00, "Pure concentrated coffee")
    )
}
