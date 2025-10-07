package com.cpen321.usermanagement.data.local.models


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

// OrderDto.kt
data class OrderDto(
    val studentId: String,
    val moverId: String?,
    val status: String,
    val volume: Double,
    val totalPrice: Double,
    val studentAddress: Address,
    val warehouseAddress: Address,
    val returnAddress: Address?,
    val pickupTime: String,
    val returnTime: String
)

data class GetAllOrdersResponse(
    val success: Boolean,
    val orders: List<OrderDto>,
    val message: String
)