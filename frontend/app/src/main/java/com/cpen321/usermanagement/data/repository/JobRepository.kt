package com.cpen321.usermanagement.data.repository

import com.cpen321.usermanagement.data.local.models.Job
import com.cpen321.usermanagement.data.local.models.JobStatus
import com.cpen321.usermanagement.data.local.models.JobType
import com.cpen321.usermanagement.data.remote.api.JobApiService
import com.cpen321.usermanagement.data.remote.dto.JobStatus as DtoJobStatus
import com.cpen321.usermanagement.data.remote.dto.UpdateJobStatusRequest
import com.cpen321.usermanagement.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val jobApiService: JobApiService
) {
    
    fun getAvailableJobs(): Flow<Resource<List<Job>>> = flow {
        try {
            emit(Resource.Loading())
            val response = jobApiService.getAvailableJobs()
            
            if (response.isSuccessful && response.body() != null) {
                val jobs = response.body()!!.data?.jobs?.map { dto ->
                    Job(
                        id = dto.id,
                        jobType = JobType.valueOf(dto.jobType),
                        status = JobStatus.valueOf(dto.status),
                        volume = dto.volume,
                        price = dto.price,
                        pickupAddress = dto.pickupAddress,
                        dropoffAddress = dto.dropoffAddress,
                        scheduledTime = LocalDateTime.parse(dto.scheduledTime, DateTimeFormatter.ISO_DATE_TIME)
                    )
                } ?: emptyList()
                
                emit(Resource.Success(jobs))
            } else {
                emit(Resource.Error("Failed to load available jobs"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    fun getMoverJobs(): Flow<Resource<List<Job>>> = flow {
        try {
            emit(Resource.Loading())
            val response = jobApiService.getMoverJobs()
            
            if (response.isSuccessful && response.body() != null) {
                val jobs = response.body()!!.data?.jobs?.map { dto ->
                    Job(
                        id = dto.id,
                        jobType = JobType.valueOf(dto.jobType),
                        status = JobStatus.valueOf(dto.status),
                        volume = dto.volume,
                        price = dto.price,
                        pickupAddress = dto.pickupAddress,
                        dropoffAddress = dto.dropoffAddress,
                        scheduledTime = LocalDateTime.parse(dto.scheduledTime, DateTimeFormatter.ISO_DATE_TIME)
                    )
                } ?: emptyList()
                
                emit(Resource.Success(jobs))
            } else {
                emit(Resource.Error("Failed to load mover jobs"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    fun getStudentJobs(): Flow<Resource<List<Job>>> = flow {
        try {
            emit(Resource.Loading())
            val response = jobApiService.getStudentJobs()
            
            if (response.isSuccessful && response.body() != null) {
                val jobs = response.body()!!.data?.jobs?.map { dto ->
                    Job(
                        id = dto.id,
                        jobType = JobType.valueOf(dto.jobType),
                        status = JobStatus.valueOf(dto.status),
                        volume = dto.volume,
                        price = dto.price,
                        pickupAddress = dto.pickupAddress,
                        dropoffAddress = dto.dropoffAddress,
                        scheduledTime = LocalDateTime.parse(dto.scheduledTime, DateTimeFormatter.ISO_DATE_TIME)
                    )
                } ?: emptyList()
                
                emit(Resource.Success(jobs))
            } else {
                emit(Resource.Error("Failed to load student jobs"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error occurred"))
        }
    }
    
    suspend fun acceptJob(jobId: String): Resource<Unit> {
        return try {
            val response = jobApiService.updateJobStatus(
                jobId,
                UpdateJobStatusRequest(status = DtoJobStatus.ACCEPTED.value)
            )
            
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to accept job")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    suspend fun updateJobStatus(jobId: String, newStatus: JobStatus): Resource<Unit> {
        return try {
            val dtoStatus = when (newStatus) {
                JobStatus.AVAILABLE -> DtoJobStatus.AVAILABLE
                JobStatus.AWAITING_STUDENT_CONFIRMATION -> DtoJobStatus.AWAITING_STUDENT_CONFIRMATION
                JobStatus.ACCEPTED -> DtoJobStatus.ACCEPTED
                JobStatus.PICKED_UP -> DtoJobStatus.PICKED_UP
                JobStatus.COMPLETED -> DtoJobStatus.COMPLETED
                JobStatus.CANCELLED -> DtoJobStatus.CANCELLED
                JobStatus.IN_STORAGE -> DtoJobStatus.PICKED_UP // Map IN_STORAGE to PICKED_UP for backend
            }
            
            val response = jobApiService.updateJobStatus(
                jobId,
                UpdateJobStatusRequest(status = dtoStatus.value)
            )
            
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Failed to update job status")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun requestPickupConfirmation(jobId: String): Resource<Unit> {
        return try {
            val response = jobApiService.requestPickupConfirmation(jobId)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to request pickup confirmation")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun confirmPickup(jobId: String): Resource<Unit> {
        return try {
            val response = jobApiService.confirmPickup(jobId)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to confirm pickup")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun requestDeliveryConfirmation(jobId: String): Resource<Unit> {
        return try {
            val response = jobApiService.requestDeliveryConfirmation(jobId)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to request delivery confirmation")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }

    suspend fun confirmDelivery(jobId: String): Resource<Unit> {
        return try {
            val response = jobApiService.confirmDelivery(jobId)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to confirm delivery")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Unknown error occurred")
        }
    }
}
