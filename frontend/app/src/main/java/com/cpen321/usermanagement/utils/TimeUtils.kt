package com.cpen321.usermanagement.utils

import java.time.LocalTime
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

    /**
     * Format LocalTime to "HH:mm" string (24-hour format)
     */
    fun formatTime24(time: LocalTime): String {
        return time.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    /**
     * Parse "HH:mm" string to LocalTime
     */
    fun parseTime24(timeString: String): LocalTime? {
        return try {
            LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format LocalTime to human-readable 12-hour format
     */
    fun formatTime12(time: LocalTime): String {
        return time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    /**
     * Check if a time string is valid "HH:mm" format
     */
    fun isValidTimeFormat(timeString: String): Boolean {
        return timeString.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))
    }

    /**
     * Convert LocalTime to minutes since midnight
     */
    fun toMinutesSinceMidnight(time: LocalTime): Int {
        return time.hour * 60 + time.minute
    }

    /**
     * Check if start time is before end time
     */
    fun isStartBeforeEnd(start: LocalTime, end: LocalTime): Boolean {
        return toMinutesSinceMidnight(start) < toMinutesSinceMidnight(end)
    }
}