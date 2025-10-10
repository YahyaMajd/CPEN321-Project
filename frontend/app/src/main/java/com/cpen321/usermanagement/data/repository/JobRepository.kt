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
    
    suspend fun acceptJob(jobId: String): Resource<Unit> {
        return try {
            val response = jobApiService.updateJobStatus(
                jobId,
                UpdateJobStatusRequest(status = DtoJobStatus.ASSIGNED.value)
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
}
