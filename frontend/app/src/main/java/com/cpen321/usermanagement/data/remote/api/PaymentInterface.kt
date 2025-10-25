package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.local.models.*
import retrofit2.Response
import retrofit2.http.*

interface PaymentInterface {
    
    @POST("payment/create-intent")
    suspend fun createPaymentIntent(@Body request: CreatePaymentIntentRequest): Response<CreatePaymentIntentResponse>
    
    @POST("payment/process")
    suspend fun processPayment(@Body request: ProcessPaymentRequest): Response<ProcessPaymentResponse>
    
}