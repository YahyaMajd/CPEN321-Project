package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.local.models.*
import com.cpen321.usermanagement.data.remote.api.OrderInterface
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderApi: OrderInterface,
    private val authRepository: AuthRepository
) {
    
    // Local state management
    private val _activeOrder = MutableStateFlow<Order?>(null)
    val activeOrder: StateFlow<Order?> = _activeOrder.asStateFlow()
    
    private val _orderHistory = MutableStateFlow<List<Order>>(emptyList())
    val orderHistory: StateFlow<List<Order>> = _orderHistory.asStateFlow()
    
    // Store last quote response for order creation
    private var lastQuoteResponse: GetQuoteResponse? = null
    private var lastStudentAddress: Address? = null
    
    /**
     * Helper to get current user ID
     */
    private suspend fun getCurrentUserId(): String? {
        return authRepository.getCurrentUser()?._id
    }
    
    /**
     * Transform frontend OrderRequest to backend CreateOrderRequest
     */
    private suspend fun transformToCreateOrderRequest(orderRequest: OrderRequest, studentAddr: Address? = null, warehouseAddr: Address? = null): CreateOrderRequest? {
        val userId = getCurrentUserId() ?: return null
        
        // Calculate volume from box quantities
        val volume = orderRequest.boxQuantities.sumOf { boxQuantity ->
            when (boxQuantity.boxSize.type) {
                "Small" -> boxQuantity.quantity * 0.5  // 0.5 cubic meters per small box
                "Medium" -> boxQuantity.quantity * 0.8  // 0.8 cubic meters per medium box
                "Large" -> boxQuantity.quantity * 1.2   // 1.2 cubic meters per large box
                else -> boxQuantity.quantity * 0.8      // Default to medium
            }
        }
        
        // Calculate total price (we'll need to get this from the quote response)
        // For now, use a simple calculation
        val totalPrice = orderRequest.boxQuantities.sumOf { boxQuantity ->
            val unitPrice = when (boxQuantity.boxSize.type) {
                "Small" -> 5.0
                "Medium" -> 8.0
                "Large" -> 12.0
                else -> 8.0
            }
            boxQuantity.quantity * unitPrice
        } + 10.0 // Add base service fee
        
        // Use provided addresses or fallback to defaults
        val studentAddress = studentAddr ?: Address(
            lat = 49.2827,
            lon = -123.1207,
            formattedAddress = orderRequest.currentAddress
        )
        
        val warehouseAddress = warehouseAddr ?: Address(
            lat = 49.2844,
            lon = -123.1086,
            formattedAddress = "123 Warehouse St, Vancouver, BC"
        )
        
        // Format dates
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val now = Date()
        val pickupTime = dateFormat.format(Date(now.time + 24 * 60 * 60 * 1000)) // Tomorrow
        val returnTime = dateFormat.format(SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).parse(orderRequest.returnDate) ?: Date(now.time + 7 * 24 * 60 * 60 * 1000))
        
        return CreateOrderRequest(
            studentId = userId,
            volume = volume,
            totalPrice = totalPrice,
            studentAddress = studentAddress,
            warehouseAddress = warehouseAddress,
            pickupTime = pickupTime,
            returnTime = returnTime
        )
    }
    
    /**
     * Get pricing quote from backend API
     */
    suspend fun getQuote(address: Address): Result<GetQuoteResponse> {
        return try {
            val userId = getCurrentUserId() 
                ?: return Result.failure(Exception("User not authenticated"))
                
            val request = GetQuoteRequest(
                studentAddress = address,
                studentId = userId
            )
            val response = orderApi.getQuote(request)
            
            if (response.isSuccessful) {
                response.body()?.let { quoteResponse ->
                    // Store for later use in order creation
                    lastQuoteResponse = quoteResponse
                    lastStudentAddress = address
                    Result.success(quoteResponse)
                } ?: Result.failure(Exception("Empty response from server"))
            } else {
                Result.failure(Exception("Failed to get quote: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit order to backend API
     */
    suspend fun submitOrder(orderRequest: OrderRequest): Result<Order> {
        return try {
            // Transform OrderRequest to CreateOrderRequest using stored addresses
            val createOrderRequest = transformToCreateOrderRequest(orderRequest, lastStudentAddress, lastQuoteResponse?.warehouseAddress)
                ?: return Result.failure(Exception("Failed to transform order request"))
            
            val response = orderApi.placeOrder(createOrderRequest)
            
            if (response.isSuccessful) {
                response.body()?.let { order ->
                    // Update active order
                    _activeOrder.value = order
                    
                    // Add to history
                    val currentHistory = _orderHistory.value.toMutableList()
                    currentHistory.add(0, order) // Add to beginning
                    _orderHistory.value = currentHistory
                    
                    Result.success(order)
                } ?: Result.failure(Exception("Empty response from server"))
            } else {
                Result.failure(Exception("Failed to place order: ${response.message()}"))
            }
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