package com.cpen321.usermanagement.data.local.models

import com.cpen321.usermanagement.data.local.models.Address

enum class OrderStatus {
    PENDING,
    ACCEPTED,
    PICKED_UP,
    IN_STORAGE,
    CANCELLED,
    RETURNED,
    COMPLETED
}

data class Order(
    val id: String? = null,
    val studentId: String,
    val moverId: String? = null,
    val status: OrderStatus,
    val volume: Double,
    val price: Double,
    val studentAddress: Address,
    val warehouseAddress: Address,
    val returnAddress: Address? = null,
    val pickupTime: String, // ISO date string
    val returnTime: String  // ISO date string
)

// Helper extension to get status display text
val OrderStatus.displayText: String
    get() = when (this) {
        OrderStatus.PENDING -> "Pending Confirmation"
        OrderStatus.ACCEPTED -> "Accepted"
        OrderStatus.PICKED_UP -> "Picked Up"
        OrderStatus.IN_STORAGE -> "In Storage"
        OrderStatus.CANCELLED -> "Cancelled"
        OrderStatus.RETURNED -> "Returned"
        OrderStatus.COMPLETED -> "Completed"
    }