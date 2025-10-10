package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import com.cpen321.usermanagement.ui.viewmodels.MoverAvailabilityViewModel
import com.cpen321.usermanagement.utils.TimeUtils

@Composable
fun SetAvailabilityScreen(
    modifier: Modifier = Modifier,
    viewModel: MoverAvailabilityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTimePickerDialog by remember { mutableStateOf<DayOfWeek?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Clear messages when screen is first loaded
    LaunchedEffect(Unit) {
        viewModel.loadAvailability()
        viewModel.clearSuccessMessage()
        viewModel.clearError()
    }

    // Show snackbar for success/error messages and immediately clear them
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Set Availability",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DayOfWeek.values()) { day ->
                        DayAvailabilityItem(
                            day = day,
                            timeSlots = uiState.availability[day] ?: emptyList(),
                            onAddTimeSlot = { showTimePickerDialog = day },
                            onRemoveTimeSlot = { slot ->
                                viewModel.removeTimeSlot(day, slot)
                            }
                        )
                    }
                }

                if (showTimePickerDialog != null) {
                    val day = showTimePickerDialog!!
                    TimeSlotPickerDialog(
                        onDismiss = { showTimePickerDialog = null },
                        onConfirm = { startTime, endTime ->
                            viewModel.addTimeSlot(day, startTime, endTime)
                            showTimePickerDialog = null
                        }
                    )
                }

                Button(
                    onClick = { viewModel.saveAvailability() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Availability")
                    }
                }
            }
        }

        // Snackbar at the bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun DayAvailabilityItem(
    day: DayOfWeek,
    timeSlots: List<Pair<LocalTime, LocalTime>>,
    onAddTimeSlot: () -> Unit,
    onRemoveTimeSlot: (Pair<LocalTime, LocalTime>) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.name,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onAddTimeSlot) {
                    Icon(Icons.Default.Add, "Add time slot")
                }
            }

            timeSlots.forEach { slot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${TimeUtils.formatTime24(slot.first)} - ${TimeUtils.formatTime24(slot.second)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(
                        onClick = { onRemoveTimeSlot(slot) }
                    ) {
                        Icon(Icons.Default.Delete, "Remove time slot")
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSlotPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalTime, LocalTime) -> Unit
) {
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(17, 0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Time Slot") },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimePickerRow("Start Time:", startTime) { startTime = it }
                TimePickerRow("End Time:", endTime) { endTime = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (TimeUtils.isStartBeforeEnd(startTime, endTime)) {
                        onConfirm(startTime, endTime)
                    }
                },
                enabled = TimeUtils.isStartBeforeEnd(startTime, endTime)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TimePickerRow(
    label: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    var textValue by remember(time) { mutableStateOf(TimeUtils.formatTime24(time)) }
    var isError by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        OutlinedTextField(
            value = textValue,
            onValueChange = { input ->
                textValue = input
                if (TimeUtils.isValidTimeFormat(input)) {
                    TimeUtils.parseTime24(input)?.let { parsedTime ->
                        onTimeChange(parsedTime)
                        isError = false
                    } ?: run {
                        isError = true
                    }
                } else {
                    isError = input.isNotEmpty()
                }
            },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            isError = isError,
            placeholder = { Text("HH:mm") },
            supportingText = if (isError) {
                { Text("Use HH:mm format", style = MaterialTheme.typography.bodySmall) }
            } else null
        )
    }
}
