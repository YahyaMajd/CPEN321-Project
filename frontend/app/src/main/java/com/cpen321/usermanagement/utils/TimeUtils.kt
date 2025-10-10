package com.cpen321.usermanagement.utils

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {

    fun formatPickupTime(isoString: String): String {
        return try {
            val date = ZonedDateTime.parse(isoString)
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")
            date.format(formatter)
        } catch (e: Exception) {
            isoString // fallback to raw string if parsing fails
        }
    }
}