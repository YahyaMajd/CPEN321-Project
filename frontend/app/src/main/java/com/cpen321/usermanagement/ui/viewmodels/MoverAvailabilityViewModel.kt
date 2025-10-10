package com.cpen321.usermanagement.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.repository.ProfileRepository
import com.cpen321.usermanagement.utils.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class MoverAvailabilityUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val availability: Map<DayOfWeek, List<Pair<LocalTime, LocalTime>>> = emptyMap(), // DayOfWeek -> List of (startTime, endTime)
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class MoverAvailabilityViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    companion object {
        private const val TAG = "MoverAvailabilityViewModel"
    }

    private val _uiState = MutableStateFlow(MoverAvailabilityUiState())
    val uiState: StateFlow<MoverAvailabilityUiState> = _uiState.asStateFlow()

    fun loadAvailability() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = profileRepository.getProfile()
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val availability = user.availability?.let { convertBackendToFrontend(it) } ?: emptyMap()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availability = availability
                )
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to load availability", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error?.message ?: "Failed to load availability"
                )
            }
        }
    }

    fun addTimeSlot(day: DayOfWeek, startTime: LocalTime, endTime: LocalTime) {
        val currentAvailability = _uiState.value.availability.toMutableMap()
        val daySlots = currentAvailability[day]?.toMutableList() ?: mutableListOf()
        daySlots.add(Pair(startTime, endTime))
        currentAvailability[day] = daySlots

        _uiState.value = _uiState.value.copy(availability = currentAvailability)
    }

    fun removeTimeSlot(day: DayOfWeek, slot: Pair<LocalTime, LocalTime>) {
        val currentAvailability = _uiState.value.availability.toMutableMap()
        val daySlots = currentAvailability[day]?.toMutableList() ?: return
        daySlots.remove(slot)

        if (daySlots.isEmpty()) {
            currentAvailability.remove(day)
        } else {
            currentAvailability[day] = daySlots
        }

        _uiState.value = _uiState.value.copy(availability = currentAvailability)
    }

    fun saveAvailability() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                errorMessage = null,
                successMessage = null
            )

            val backendAvailability = convertFrontendToBackend(_uiState.value.availability)
            val result = profileRepository.updateMoverAvailability(backendAvailability)

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = "Availability updated successfully!"
                )
            } else {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to save availability", error)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = error?.message ?: "Failed to save availability"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    private fun convertFrontendToBackend(
        availability: Map<DayOfWeek, List<Pair<LocalTime, LocalTime>>>
    ): Map<String, List<List<String>>> {
        return availability.mapKeys { it.key.name.take(3) } // "MONDAY" -> "Mon"
            .mapValues { (_, slots) ->
                slots.map { listOf(TimeUtils.formatTime24(it.first), TimeUtils.formatTime24(it.second)) }
            }
    }

    private fun convertBackendToFrontend(
        availability: Map<String, List<List<String>>>
    ): Map<DayOfWeek, List<Pair<LocalTime, LocalTime>>> {
        val result = mutableMapOf<DayOfWeek, List<Pair<LocalTime, LocalTime>>>()

        availability.forEach { (dayName, slots) ->
            // Convert "Mon" to DayOfWeek.MONDAY, etc.
            val dayOfWeek = when (dayName.uppercase()) {
                "MON", "MONDAY" -> DayOfWeek.MONDAY
                "TUE", "TUESDAY" -> DayOfWeek.TUESDAY
                "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY
                "THU", "THURSDAY" -> DayOfWeek.THURSDAY
                "FRI", "FRIDAY" -> DayOfWeek.FRIDAY
                "SAT", "SATURDAY" -> DayOfWeek.SATURDAY
                "SUN", "SUNDAY" -> DayOfWeek.SUNDAY
                else -> null
            }

            if (dayOfWeek != null) {
                result[dayOfWeek] = slots.mapNotNull { slot ->
                    if (slot.size >= 2) {
                        val start = TimeUtils.parseTime24(slot[0])
                        val end = TimeUtils.parseTime24(slot[1])
                        if (start != null && end != null) {
                            Pair(start, end)
                        } else null
                    } else null
                }
            }
        }

        return result
    }
}
