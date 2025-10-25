package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.local.models.*
import com.cpen321.usermanagement.data.remote.api.PaymentInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentInterface: PaymentInterface
) {
    
    suspend fun createPaymentIntent(amount: Double): Result<CreatePaymentIntentResponse> {
        return try {
            println("PaymentRepository: Creating payment intent for amount $amount")
            val response = paymentInterface.createPaymentIntent(
                CreatePaymentIntentRequest(amount = amount)
            )
            
            if (response.isSuccessful) {
                response.body()?.let { paymentIntent ->
                    println("PaymentRepository: Successfully created payment intent: ${paymentIntent.id}")
                    Result.success(paymentIntent)
                } ?: run {
                    println("PaymentRepository: Empty response body")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                println("PaymentRepository: HTTP error ${response.code()}: ${response.message()}")
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            println("PaymentRepository: Exception creating payment intent: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun processPayment(
        paymentIntentId: String,
        customerInfo: CustomerInfo,
        paymentMethodId: String = TestPaymentMethods.VISA_SUCCESS
    ): Result<ProcessPaymentResponse> {
        return try {
            println("PaymentRepository: Processing payment with intent ID: $paymentIntentId")
            if (paymentIntentId.isBlank()) {
                println("PaymentRepository: ERROR - Payment intent ID is blank!")
                return Result.failure(Exception("Payment intent ID is blank"))
            }
            
            val response = paymentInterface.processPayment(
                ProcessPaymentRequest(
                    paymentIntentId = paymentIntentId,
                    paymentMethodId = paymentMethodId,
                    customerInfo = customerInfo
                )
            )
            
            if (response.isSuccessful) {
                response.body()?.let { paymentResult ->
                    println("PaymentRepository: Payment processed successfully: ${paymentResult.status}")
                    Result.success(paymentResult)
                } ?: run {
                    println("PaymentRepository: Empty response body for payment processing")
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                println("PaymentRepository: HTTP error processing payment ${response.code()}: ${response.message()}")
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            println("PaymentRepository: Exception processing payment: ${e.message}")
            Result.failure(e)
        }
    }
    
}