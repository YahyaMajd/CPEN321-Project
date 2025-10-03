package com.cpen321.usermanagement.data.local.models

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}

data class Order(
    val id: String,
    val userId: String,
    val boxQuantities: List<BoxQuantity>,
    val pickupAddress: String,
    val returnDate: String,
    val status: OrderStatus,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val estimatedPickupTime: String? = null,
    val actualPickupTime: String? = null,
    val estimatedDeliveryTime: String? = null,
    val actualDeliveryTime: String? = null,
    val moverName: String? = null,
    val moverPhone: String? = null,
    val trackingNotes: List<String> = emptyList()
)

// Helper extension to get total boxes
val Order.totalBoxes: Int
    get() = boxQuantities.sumOf { it.quantity }

// Helper extension to get status display text
val OrderStatus.displayText: String
    get() = when (this) {
        OrderStatus.PENDING -> "Pending Confirmation"
        OrderStatus.CONFIRMED -> "Confirmed"
        OrderStatus.IN_TRANSIT -> "In Transit"
        OrderStatus.DELIVERED -> "Delivered"
        OrderStatus.CANCELLED -> "Cancelled"
    }