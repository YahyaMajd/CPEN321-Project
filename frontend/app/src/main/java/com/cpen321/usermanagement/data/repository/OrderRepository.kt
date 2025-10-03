package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.local.models.Order
import com.cpen321.usermanagement.data.local.models.OrderRequest
import com.cpen321.usermanagement.data.local.models.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class OrderRepository {
    
    // Local state management
    private val _activeOrder = MutableStateFlow<Order?>(null)
    val activeOrder: StateFlow<Order?> = _activeOrder.asStateFlow()
    
    private val _orderHistory = MutableStateFlow<List<Order>>(emptyList())
    val orderHistory: StateFlow<List<Order>> = _orderHistory.asStateFlow()
    
    // For now - mock user ID, later get from auth
    private fun getCurrentUserId(): String = "user_123"
    
    /**
     * Submit order locally (for testing)
     * Later: This will call backend API
     */
    suspend fun submitOrder(orderRequest: OrderRequest): Result<Order> {
        return try {
            val order = Order(
                id = UUID.randomUUID().toString(),
                userId = getCurrentUserId(),
                boxQuantities = orderRequest.boxQuantities,
                pickupAddress = orderRequest.currentAddress,
                returnDate = orderRequest.returnDate,
                status = OrderStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                estimatedPickupTime = "Tomorrow 10:00 AM" // Mock data
            )
            
            // Update active order
            _activeOrder.value = order
            
            // Add to history
            val currentHistory = _orderHistory.value.toMutableList()
            currentHistory.add(0, order) // Add to beginning
            _orderHistory.value = currentHistory
            
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current active order
     */
    fun getActiveOrder(): Order? = _activeOrder.value
    
    /**
     * Update order status (for testing state transitions)
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        val currentOrder = _activeOrder.value
        if (currentOrder?.id == orderId) {
            val updatedOrder = currentOrder.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            _activeOrder.value = updatedOrder
            
            // Update in history as well
            val currentHistory = _orderHistory.value.toMutableList()
            val index = currentHistory.indexOfFirst { it.id == orderId }
            if (index != -1) {
                currentHistory[index] = updatedOrder
                _orderHistory.value = currentHistory
            }
        }
    }
    
    /**
     * Complete order (move from active to history only)
     */
    suspend fun completeOrder(orderId: String) {
        updateOrderStatus(orderId, OrderStatus.DELIVERED)
        // Keep in active until user dismisses or starts new order
    }
    
    /**
     * Clear active order (when starting new order or dismissing completed one)
     */
    suspend fun clearActiveOrder() {
        _activeOrder.value = null
    }
    
    /**
     * Get all orders
     */
    fun getAllOrders(): List<Order> = _orderHistory.value
    
    /**
     * Mock method to simulate order progression (for testing)
     */
    suspend fun simulateOrderProgress(orderId: String) {
        val currentOrder = _activeOrder.value ?: return
        if (currentOrder.id != orderId) return
        
        when (currentOrder.status) {
            OrderStatus.PENDING -> updateOrderStatus(orderId, OrderStatus.CONFIRMED)
            OrderStatus.CONFIRMED -> updateOrderStatus(orderId, OrderStatus.IN_TRANSIT)
            OrderStatus.IN_TRANSIT -> updateOrderStatus(orderId, OrderStatus.DELIVERED)
            else -> { /* No change */ }
        }
    }
}