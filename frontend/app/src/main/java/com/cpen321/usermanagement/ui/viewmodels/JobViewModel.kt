package com.cpen321.usermanagement.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.local.models.Job
import com.cpen321.usermanagement.data.repository.JobRepository
import com.cpen321.usermanagement.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.cpen321.usermanagement.network.SocketClient
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

data class JobUiState(
    val availableJobs: List<Job> = emptyList(),
    val moverJobs: List<Job> = emptyList(),
    val studentJobs: List<Job> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingConfirmationJobId: String? = null // Job awaiting student confirmation
)

@HiltViewModel
class JobViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketClient: SocketClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(JobUiState())
    val uiState: StateFlow<JobUiState> = _uiState.asStateFlow()

    init {
        // Start collecting socket events at ViewModel level (survives screen navigation)
        listenForJobUpdates()
    }

    /**
     * Check if there are any jobs awaiting student confirmation
     * This is called when student logs in or opens the main screen
     * to catch cases where the event was emitted while they were logged out
     */
    fun checkForPendingConfirmations() {
        viewModelScope.launch {
            try {
                // Load student jobs to check for pending confirmations
                val response = jobRepository.getStudentJobs()
                response.collect { resource ->
                    if (resource is Resource.Success) {
                        val awaitingJob = resource.data?.find { job ->
                            job.status == com.cpen321.usermanagement.data.local.models.JobStatus.AWAITING_STUDENT_CONFIRMATION &&
                            job.jobType == com.cpen321.usermanagement.data.local.models.JobType.STORAGE
                        }
                        
                        if (awaitingJob != null) {
                            android.util.Log.d("JobViewModel", "Found pending confirmation job: ${awaitingJob.id}")
                            _uiState.value = _uiState.value.copy(pendingConfirmationJobId = awaitingJob.id)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("JobViewModel", "Error checking for pending confirmations", e)
            }
        }
    }

    private fun listenForJobUpdates() {
        viewModelScope.launch {
            socketClient.events.collect { event ->
                when (event.name) {
                    "job.updated" -> {
                        handleJobUpdatedEvent(event.payload)
                        // Refresh all job lists to reflect updated job status
                        loadAvailableJobs()
                        loadMoverJobs()
                        loadStudentJobs() // Refresh student jobs for button visibility
                    }
                    "job.created" -> {
                        // New job created, refresh all job lists
                        android.util.Log.d("JobViewModel", "New job created, refreshing job lists")
                        loadAvailableJobs()
                        loadStudentJobs() // Refresh student jobs when return job is created
                    }
                }
            }
        }
    }

    private fun handleJobUpdatedEvent(payload: org.json.JSONObject?) {
        try {
            // Parse payload to extract job info
            val jobData = when {
                payload == null -> null
                payload.has("job") -> payload.optJSONObject("job")
                payload.has("data") && payload.optJSONObject("data")?.has("job") == true -> 
                    payload.optJSONObject("data")?.optJSONObject("job")
                else -> payload
            }

            val status = jobData?.optString("status")
            val jobType = jobData?.optString("jobType")
            val jobId = jobData?.optString("id")

            // If job is awaiting student confirmation, store it in state
            if (jobId != null && jobId.isNotBlank() && 
                status == "AWAITING_STUDENT_CONFIRMATION" && 
                jobType == "STORAGE") {
                _uiState.value = _uiState.value.copy(pendingConfirmationJobId = jobId)
            }

            // Clear pending confirmation when job moves past that state
            if (jobId != null && status != "AWAITING_STUDENT_CONFIRMATION" && 
                jobId == _uiState.value.pendingConfirmationJobId) {
                _uiState.value = _uiState.value.copy(pendingConfirmationJobId = null)
            }
        } catch (e: Exception) {
            // Log but don't crash
            android.util.Log.e("JobViewModel", "Error handling job.updated event", e)
        }
    }
    
    fun loadAvailableJobs() {
        viewModelScope.launch {
            jobRepository.getAvailableJobs().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            availableJobs = resource.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
    
    fun loadMoverJobs() {
        viewModelScope.launch {
            jobRepository.getMoverJobs().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            moverJobs = resource.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
    
    fun loadStudentJobs() {
        viewModelScope.launch {
            jobRepository.getStudentJobs().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            studentJobs = resource.data ?: emptyList(),
                            isLoading = false,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = resource.message
                        )
                    }
                }
            }
        }
    }
    
    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            when (val result = jobRepository.acceptJob(jobId)) {
                is Resource.Success -> {
                    // Refresh available jobs after accepting
                    loadAvailableJobs()
                    loadMoverJobs()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Resource.Loading -> { /* Handle if needed */ }
            }
        }
    }

    fun requestPickupConfirmation(jobId: String) {
        viewModelScope.launch {
            when (val result = jobRepository.requestPickupConfirmation(jobId)) {
                is Resource.Success -> {
                    // wait for socket events to update state
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Resource.Loading -> { }
            }
        }
    }

    fun confirmPickup(jobId: String) {
        viewModelScope.launch {
            when (val result = jobRepository.confirmPickup(jobId)) {
                is Resource.Success -> {
                    // Clear the pending confirmation after successful confirm
                    _uiState.value = _uiState.value.copy(pendingConfirmationJobId = null)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Resource.Loading -> { }
            }
        }
    }

    fun clearPendingConfirmation() {
        _uiState.value = _uiState.value.copy(pendingConfirmationJobId = null)
    }
    
    fun updateJobStatus(jobId: String, newStatus: com.cpen321.usermanagement.data.local.models.JobStatus) {
        viewModelScope.launch {
            when (val result = jobRepository.updateJobStatus(jobId, newStatus)) {
                is Resource.Success -> {
                    // Refresh jobs after status update
                    loadMoverJobs()
                    loadAvailableJobs()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Resource.Loading -> { /* Handle if needed */ }
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
