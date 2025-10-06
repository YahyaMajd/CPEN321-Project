package com.cpen321.usermanagement.data.local.models

import com.cpen321.usermanagement.data.remote.dto.Address
import java.time.LocalDateTime

data class Job(
    val id: String,
    val orderId: String? = null,
    val studentId: String? = null,
    val moverId: String? = null,
    val jobType: JobType,
    val status: JobStatus,
    val volume: Double,
    val price: Double,
    val pickupAddress: Address,
    val dropoffAddress: Address,
    val scheduledTime: LocalDateTime,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

enum class JobType(val value: String) {
    STORAGE("STORAGE"),
    RETURN("RETURN")
}

enum class JobStatus(val value: String) {
    AVAILABLE("AVAILABLE"),
    ASSIGNED("ASSIGNED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED")
}
