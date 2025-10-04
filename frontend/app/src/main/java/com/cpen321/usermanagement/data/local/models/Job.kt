package com.cpen321.usermanagement.data.local.models

import java.time.LocalDate
import java.time.LocalTime

data class Job(
    val id: String,
    val price: Double,
    val date: LocalDate,
    val startTime: LocalTime,
    val pickupAddress: String,
    val dropoffAddress: String,
    val status: JobStatus = JobStatus.AVAILABLE
)

enum class JobStatus {
    AVAILABLE,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED
}
