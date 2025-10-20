package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.local.models.*
import com.cpen321.usermanagement.data.remote.api.OrderInterface
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderApi: OrderInterface,
    private val authRepository: AuthRepository
) {

    // Store last quote response for order creation
    private var lastQuoteResponse: GetQuoteResponse? = null
    private var lastStudentAddress: Address? = null
    private var isSubmitting: Boolean = false
    
    /**
     * Helper to get current user ID
     */
     private suspend fun getCurrentUserId(): String? {
        return authRepository.getCurrentUser()?._id
    }
    
    /**
     * Transform frontend OrderRequest to backend CreateOrderRequest
     */
    private suspend fun transformToCreateOrderRequest(
        orderRequest: OrderRequest, 
        studentAddr: Address? = null, 
        warehouseAddr: Address? = null,
        paymentIntentId: String? = null
    ): CreateOrderRequest? {
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
        
        // Use the price calculated by the UI (which uses backend pricing rules)
        val totalPrice = orderRequest.totalPrice
        
        // Both addresses should come from the quote response
        // Student address: from the geocoded address in quote request
        // Warehouse address: from the quote response
        val studentAddress = studentAddr 
            ?: return null // Cannot create order without proper student address from quote
        
        val warehouseAddress = warehouseAddr 
            ?: return null // Cannot create order without warehouse address from backend quote
        
        // Format return date - pickup time comes from OrderRequest
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val now = Date()
        val pickupTime = orderRequest.pickupTime // Use the pickup time from UI (already in ISO format)
        val returnTime = dateFormat.format(SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).parse(orderRequest.returnDate) ?: Date(now.time + 7 * 24 * 60 * 60 * 1000))
        
        return CreateOrderRequest(
            studentId = userId,
            volume = volume,
            totalPrice = totalPrice,
            studentAddress = studentAddress,
            warehouseAddress = warehouseAddress,
            pickupTime = pickupTime,
            returnTime = returnTime,
            paymentIntentId = paymentIntentId // Include payment intent ID for refunds
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
    suspend fun submitOrder(orderRequest: OrderRequest, paymentIntentId: String? = null): Result<Order> {
        // prevent concurrent submits
        if (isSubmitting) return Result.failure(Exception("Already submitting"))
        isSubmitting = true
        // generate an idempotency key for this flow
        val idempotencyKey = java.util.UUID.randomUUID().toString()
        RetrofitClient.setIdempotencyKeyProvider { idempotencyKey }

        return try {
            println("🚀 OrderRepository: Starting order submission (idempotency=$idempotencyKey)")
            println("📦 OrderRequest: $orderRequest")
            println("💳 PaymentIntentId: $paymentIntentId")

            val createOrderRequest = transformToCreateOrderRequest(
                orderRequest, 
                lastStudentAddress, 
                lastQuoteResponse?.warehouseAddress,
                paymentIntentId
            ) ?: return Result.failure(Exception("Failed to create order request"))

            println("🌐 CreateOrderRequest: $createOrderRequest")
            val response = orderApi.placeOrder(createOrderRequest)
            println("📡 Response code: ${response.code()}")
            println("📡 Response message: ${response.message()}")
            if (response.isSuccessful) {
                response.body()?.let { order ->
                    Result.success(order)
                } ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Failed to place order: ${response.message()}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            isSubmitting = false
            // clear provider
            RetrofitClient.setIdempotencyKeyProvider { null }
        }
    }
    
    /**
     * Get current active order
     */
    suspend fun getActiveOrder(): Order? {
        return try {
            val response = orderApi.getActiveOrder()
            if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
                null
            }
    }

    /**
     * Get all orders
     */
    suspend fun getAllOrders(): List<Order>? {
        val response = orderApi.getAllOrders()
        if (response.isSuccessful) {
            val orders = response.body()?.orders?.map{ dto ->
                Order(
                    id = dto.id,
                    studentId = dto.studentId,
                    moverId = dto.moverId,
                    status = OrderStatus.valueOf(dto.status),
                    volume = dto.volume,
                    price = dto.totalPrice,
                    studentAddress = dto.studentAddress,
                    warehouseAddress = dto.warehouseAddress,
                    returnAddress = dto.returnAddress,
                    pickupTime = dto.pickupTime,
                    returnTime = dto.returnTime
                )
            }
            return orders
        } else {
            println("❌ Failed to fetch orders: ${response.code()} - ${response.message()}")
        }
        return null
    }

    /**
     * Clear active order (when starting new order or dismissing completed one)
     */
    suspend fun cancelOrder(){
        val response = orderApi.cancelOrder()
        if (!response.isSuccessful) {
            throw Exception("Failed to cancel order: ${response.code()} ${response.message()}")
        }
    }

    suspend fun createReturnJob(request: CreateReturnJobRequest): CreateReturnJobResponse {
        val response = orderApi.createReturnJob(request)
        if (!response.isSuccessful) {
            throw Exception("Failed to create return job: ${response.code()} ${response.message()}")
        }
        return response.body() ?: throw Exception("Empty response from server")
    }
    
    /**
     * Complete order (move from active to history only)
     */
    suspend fun completeOrder() {
        // updateOrderStatus(orderId, OrderStatus.IN_STORAGE)
        // Keep in active until user dismisses or starts new order
    }
}