package com.cpen321.usermanagement.data.remote.dto

import com.google.gson.annotations.SerializedName

data class Address(
    val lat: Double,
    val lon: Double,
    val formattedAddress: String
)

data class JobDto(
    val id: String,
    val jobType: String,
    val volume: Double,
    val price: Double,
    val pickupAddress: Address,
    val dropoffAddress: Address,
    val scheduledTime: String,
    val status: String
)

data class JobDetailDto(
    val id: String,
    val orderId: String,
    val studentId: String,
    val moverId: String? = null,
    val jobType: String,
    val status: String,
    val volume: Double,
    val price: Double,
    val pickupAddress: Address,
    val dropoffAddress: Address,
    val scheduledTime: String,
    val createdAt: String,
    val updatedAt: String
)

data class JobListResponse(
    val jobs: List<JobDto>
)

data class JobResponse(
    val job: JobDetailDto
)

data class UpdateJobStatusRequest(
    val status: String,
    val moverId: String? = null
)

enum class JobStatus(val value: String) {
    AVAILABLE("AVAILABLE"),
    ASSIGNED("ASSIGNED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED")
}

enum class JobType(val value: String) {
    STORAGE("STORAGE"),
    RETURN("RETURN")
}
