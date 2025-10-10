package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.ApiResponse
import com.cpen321.usermanagement.data.remote.dto.JobListResponse
import com.cpen321.usermanagement.data.remote.dto.JobResponse
import com.cpen321.usermanagement.data.remote.dto.UpdateJobStatusRequest
import retrofit2.Response
import retrofit2.http.*

interface JobApiService {
    @GET("jobs/available")
    suspend fun getAvailableJobs(): Response<ApiResponse<JobListResponse>>
    
    @GET("jobs/mover")
    suspend fun getMoverJobs(): Response<ApiResponse<JobListResponse>>
    
    @GET("jobs/{id}")
    suspend fun getJobById(@Path("id") jobId: String): Response<ApiResponse<JobResponse>>
    
    @PATCH("jobs/{id}/status")
    suspend fun updateJobStatus(
        @Path("id") jobId: String,
        @Body request: UpdateJobStatusRequest
    ): Response<ApiResponse<JobResponse>>
}
