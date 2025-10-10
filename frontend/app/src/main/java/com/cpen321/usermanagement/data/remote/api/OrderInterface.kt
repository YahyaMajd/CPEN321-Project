package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.local.models.GetQuoteRequest
import com.cpen321.usermanagement.data.local.models.GetQuoteResponse
import com.cpen321.usermanagement.data.local.models.CreateOrderRequest
import com.cpen321.usermanagement.data.local.models.GetAllOrdersResponse
import com.cpen321.usermanagement.data.local.models.CancelOrderRequest
import com.cpen321.usermanagement.data.local.models.CancelOrderResponse
import com.cpen321.usermanagement.data.local.models.Order
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface OrderInterface {
    
    @POST("order/quote")
    suspend fun getQuote(@Body request: GetQuoteRequest): Response<GetQuoteResponse>
    
    @POST("order")
    suspend fun placeOrder(@Body request: CreateOrderRequest): Response<Order>

    @GET("order/all-orders")
    suspend fun getAllOrders(): Response<GetAllOrdersResponse>

    @GET("order/active-order")
    suspend fun getActiveOrder(): Response<Order>

    @DELETE("order/cancel-order")
    suspend fun cancelOrder(): Response<CancelOrderResponse>
}