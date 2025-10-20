package com.cpen321.usermanagement.utils

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {

    fun formatPickupTime(isoString: String): String {
        return try {
            val zoned: ZonedDateTime = try {
                ZonedDateTime.parse(isoString)
            } catch (e1: Exception) {
                try {
                    OffsetDateTime.parse(isoString).toZonedDateTime()
                } catch (e2: Exception) {
                    val ldt = LocalDateTime.parse(isoString)
                    ldt.atZone(ZoneId.systemDefault())
                }
            }
            // Convert to Pacific Time
            val pacific = zoned.withZoneSameInstant(ZoneId.of("America/Los_Angeles"))
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")
            pacific.format(formatter)
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

    /**
     * Check if time is within a given range (inclusive)
     */
    fun isTimeInRange(time: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        val timeMinutes = toMinutesSinceMidnight(time)
        val startMinutes = toMinutesSinceMidnight(start)
        val endMinutes = toMinutesSinceMidnight(end)
        return timeMinutes in startMinutes..endMinutes
    }

    /**
     * Take a LocalDateTime (assumed to be in UTC) and format it into Pacific Time
     * using the pattern: "MMM d, yyyy 'at' h:mm a". This matches the UI's expected format.
     */
    fun formatLocalDateTimeToPacific(localDateTime: java.time.LocalDateTime): String {
        return try {
            val utcZone = ZoneId.of("UTC")
            val pacificZone = ZoneId.of("America/Los_Angeles")
            val zonedUtc = localDateTime.atZone(utcZone)
            val pacific = zonedUtc.withZoneSameInstant(pacificZone)
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
            pacific.format(formatter)
        } catch (e: Exception) {
            localDateTime.toString()
        }
    }

    /**
     * Format LocalDateTime's time portion into Pacific time "HH:mm" (24h) for compact displays.
     */
    fun formatLocalDateTimeTimeOnlyToPacific(localDateTime: java.time.LocalDateTime): String {
        return try {
            val utcZone = ZoneId.of("UTC")
            val pacificZone = ZoneId.of("America/Los_Angeles")
            val pacific = localDateTime.atZone(utcZone).withZoneSameInstant(pacificZone)
            pacific.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            localDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        }
    }

    /**
     * Format LocalDateTime's date portion into Pacific date "MMM dd, yyyy".
     */
    fun formatLocalDateTimeDateOnlyToPacific(localDateTime: java.time.LocalDateTime): String {
        return try {
            val utcZone = ZoneId.of("UTC")
            val pacificZone = ZoneId.of("America/Los_Angeles")
            val pacific = localDateTime.atZone(utcZone).withZoneSameInstant(pacificZone)
            pacific.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        } catch (e: Exception) {
            localDateTime.toLocalDate().toString()
        }
    }
}