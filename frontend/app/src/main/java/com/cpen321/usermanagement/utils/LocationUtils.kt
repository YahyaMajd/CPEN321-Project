package com.cpen321.usermanagement.utils

import android.location.Geocoder
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.maps.model.LatLng

/**
 * Location utilities for geocoding addresses to coordinates
 */
object LocationUtils {
    
    /**
     * Convert address string to LatLng coordinates
     * Returns null if geocoding fails
     */
    suspend fun geocodeAddress(context: Context, address: String): LatLng? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context)
                val results = geocoder.getFromLocationName(address, 1)
                
                if (!results.isNullOrEmpty()) {
                    val location = results[0]
                    LatLng(location.latitude, location.longitude)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Fallback coordinates for common areas (when geocoding fails)
     */
    fun getFallbackCoordinates(address: String): LatLng {
        return when {
            address.contains("Vancouver", ignoreCase = true) -> LatLng(49.2827, -123.1207)
            address.contains("UBC", ignoreCase = true) -> LatLng(49.2606, -123.2460)
            address.contains("Burnaby", ignoreCase = true) -> LatLng(49.2488, -122.9805)
            address.contains("Richmond", ignoreCase = true) -> LatLng(49.1666, -123.1336)
            else -> LatLng(49.2827, -123.1207) // Default to Vancouver downtown
        }
    }
}