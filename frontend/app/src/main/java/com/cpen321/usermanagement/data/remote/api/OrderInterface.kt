package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.local.models.GetQuoteRequest
import com.cpen321.usermanagement.data.local.models.GetQuoteResponse
import com.cpen321.usermanagement.data.local.models.OrderRequest
import com.cpen321.usermanagement.data.local.models.Order
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OrderInterface {
    
    @POST("get-quote")
    suspend fun getQuote(@Body request: GetQuoteRequest): Response<GetQuoteResponse>
    
    @POST("place-order")
    suspend fun placeOrder(@Body request: OrderRequest): Response<Order>
}