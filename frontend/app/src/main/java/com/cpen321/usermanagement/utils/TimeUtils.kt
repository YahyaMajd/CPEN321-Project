package com.cpen321.usermanagement.utils

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {

    // ============================================================================
    // STANDARD DATE-TIME FORMATTING (Use these for all job/order displays)
    // ============================================================================

    /**
     * STANDARD FORMAT: "MMM d, yyyy h:mm a" in Pacific Time
     * 
     * Use this for ALL job and order time displays to ensure consistency.
     * Converts ISO string (UTC) to Pacific time.
     * 
     * Example: "Oct 25, 2025 10:00 AM"
     */
    fun formatDateTime(isoString: String): String {
        return try {
            val zoned: ZonedDateTime = try {
                ZonedDateTime.parse(isoString)
            } catch (e1: Exception) {
                try {
                    OffsetDateTime.parse(isoString).toZonedDateTime()
                } catch (e2: Exception) {
                    val ldt = LocalDateTime.parse(isoString)
                    ldt.atZone(ZoneId.of("UTC"))
                }
            }
            // Convert to Pacific Time
            val pacific = zoned.withZoneSameInstant(ZoneId.of("America/Los_Angeles"))
            pacific.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
        } catch (e: Exception) {
            isoString // fallback to raw string if parsing fails
        }
    }

    /**
     * STANDARD FORMAT: "MMM d, yyyy h:mm a" in Pacific Time
     * 
     * Use this for ALL job time displays from LocalDateTime.
     * Converts LocalDateTime (assumed UTC) to Pacific time.
     * 
     * Example: "Oct 25, 2025 10:00 AM"
     */
    fun formatDateTime(localDateTime: LocalDateTime): String {
        return try {
            val utcZone = ZoneId.of("UTC")
            val pacificZone = ZoneId.of("America/Los_Angeles")
            val zonedUtc = localDateTime.atZone(utcZone)
            val pacific = zonedUtc.withZoneSameInstant(pacificZone)
            pacific.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
        } catch (e: Exception) {
            localDateTime.toString()
        }
    }

    // ============================================================================
    // DATE PICKER FORMATTING (Material3 DatePicker support)
    // ============================================================================

    /**
     * Format UTC milliseconds from Material3 DatePicker to display date.
     * 
     * Material3 DatePicker returns UTC milliseconds at midnight UTC representing the selected date.
     * To display the correct date (the one the user saw and selected), we must format using UTC timezone.
     * 
     * For example:
     * - User selects Oct 25 in picker
     * - DatePicker returns Oct 25 00:00:00 UTC (1729814400000 ms)
     * - If we format as Pacific (UTC-8), it shows Oct 24 16:00 = Oct 24 ❌
     * - If we format as UTC, it shows Oct 25 00:00 = Oct 25 ✅
     * 
     * @param utcMillis UTC milliseconds from DatePicker (midnight UTC)
     * @param pattern SimpleDateFormat pattern (default: "MMMM dd, yyyy")
     * @return Formatted date string displaying the date the user selected
     */
    fun formatDatePickerDate(utcMillis: Long, pattern: String = "MMMM dd, yyyy"): String {
        return try {
            val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
            // Must use UTC to display the date the user selected in the picker
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.format(java.util.Date(utcMillis))
        } catch (e: Exception) {
            "Invalid date"
        }
    }

    // ============================================================================
    // TIME-ONLY FORMATTING (For availability slots, time pickers, etc.)
    // ============================================================================

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

    /**
     * Check if time is within a given range (inclusive)
     */
    fun isTimeInRange(time: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        val timeMinutes = toMinutesSinceMidnight(time)
        val startMinutes = toMinutesSinceMidnight(start)
        val endMinutes = toMinutesSinceMidnight(end)
        return timeMinutes in startMinutes..endMinutes
    }
}