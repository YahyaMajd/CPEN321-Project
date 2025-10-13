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
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class JobViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketClient: SocketClient
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(JobUiState())
    val uiState: StateFlow<JobUiState> = _uiState.asStateFlow()
    
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

//    init {
//        // Subscribe to socket events and refresh lists when relevant events arrive.
//        viewModelScope.launch {
//            try {
//                socketClient.events.collect { ev ->
//                    when (ev.name) {
//                        "job.updated", "job.created" -> {
//                            // A job changed — refresh available and mover-specific lists
//                            loadAvailableJobs()
//                            loadMoverJobs()
//                        }
//                        "order.updated" -> {
//                            // Orders changing can affect available jobs (cancellations/accepts)
//                            loadAvailableJobs()
//                            loadMoverJobs()
//                        }
//                        else -> {
//                            // ignore other events
//                        }
//                    }
//                }
//            } catch (err: Exception) {
//                // Swallow to avoid crashing ViewModel; logs can be added if needed
//            }
//        }
//    }
    
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
