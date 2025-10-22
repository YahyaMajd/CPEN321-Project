package com.cpen321.usermanagement.data.repository

import android.util.Log
import com.cpen321.usermanagement.data.remote.api.RouteApiService
import com.cpen321.usermanagement.data.remote.dto.SmartRouteData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val routeApiService: RouteApiService
) {
    suspend fun getSmartRoute(currentLat: Double, currentLon: Double, maxDuration: Int? = null): Result<SmartRouteData> {
        return try {
            val response = routeApiService.getSmartRoute(currentLat, currentLon, maxDuration)
            
            if (response.isSuccessful) {
                val smartRouteResponse = response.body()
                if (smartRouteResponse?.data != null) {
                    Result.success(smartRouteResponse.data)
                } else {
                    Result.failure(Exception(smartRouteResponse?.message ?: "No route data available"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Failed to fetch smart route"
                Log.e("RouteRepository", "Error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("RouteRepository", "Exception fetching smart route", e)
            Result.failure(e)
        }
    }
}
