package com.zz143.demo.tabs.coffee

data class CoffeeProduct(
    val id: String,
    val name: String,
    val price: Double,
    val description: String
)

data class CartItem(
    val product: CoffeeProduct,
    val size: Size,
    val milk: MilkType,
    val extraShot: Boolean,
    val noSugar: Boolean,
    val quantity: Int
) {
    val totalPrice: Double
        get() = (product.price + size.priceAdder + milk.price + if (extraShot) 0.75 else 0.0) * quantity

    val customizationSummary: String
        get() = buildString {
            append(size.label)
            append(" / ${milk.label}")
            if (extraShot) append(" / Extra Shot")
            if (noSugar) append(" / No Sugar")
        }
}

enum class Size(val label: String, val priceAdder: Double) {
    SMALL("Small", 0.0),
    MEDIUM("Medium", 0.50),
    LARGE("Large", 1.00)
}

enum class MilkType(val label: String, val price: Double) {
    WHOLE("Whole", 0.0),
    OAT("Oat", 0.60),
    ALMOND("Almond", 0.50)
}

object SampleProducts {
    val all = listOf(
        CoffeeProduct("latte", "Caffe Latte", 4.95, "Smooth espresso with steamed milk"),
        CoffeeProduct("cappuccino", "Cappuccino", 4.50, "Rich espresso with foamy milk"),
        CoffeeProduct("americano", "Americano", 3.75, "Bold espresso with hot water"),
        CoffeeProduct("mocha", "Mocha", 5.50, "Espresso with chocolate and steamed milk"),
        CoffeeProduct("cold-brew", "Cold Brew", 4.25, "Slow-steeped cold coffee"),
        CoffeeProduct("matcha", "Matcha Latte", 5.25, "Japanese green tea with milk"),
        CoffeeProduct("chai", "Chai Latte", 4.75, "Spiced tea with steamed milk"),
        CoffeeProduct("espresso", "Espresso", 3.00, "Pure concentrated coffee")
    )
}
