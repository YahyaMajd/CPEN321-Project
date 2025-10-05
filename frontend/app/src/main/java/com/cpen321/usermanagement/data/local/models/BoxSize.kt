package com.cpen321.usermanagement.data.local.models

data class BoxSize(
    val type: String,
    val dimensions: String,
    val description: String
)

data class BoxQuantity(
    val boxSize: BoxSize,
    val quantity: Int = 0
)

data class OrderRequest(
    val boxQuantities: List<BoxQuantity>,
    val currentAddress: String,
    val returnDate: String,
    val totalPrice: Double // Price calculated by UI using backend pricing rules
)

data class CreateOrderRequest(
    val studentId: String,
    val volume: Double,
    val totalPrice: Double,
    val studentAddress: Address,
    val warehouseAddress: Address,
    val pickupTime: String, // ISO string format
    val returnTime: String  // ISO string format
)

val STANDARD_BOX_SIZES = listOf(
    BoxSize(
        type = "Small",
        dimensions = "16\" × 12\" × 12\"",
        description = "Books, documents, small items"
    ),
    BoxSize(
        type = "Medium",
        dimensions = "18\" × 14\" × 12\"",
        description = "Clothes, kitchenware, electronics"
    ),
    BoxSize(
        type = "Large",
        dimensions = "20\" × 20\" × 15\"",
        description = "Bedding, pillows, large items"
    )
)