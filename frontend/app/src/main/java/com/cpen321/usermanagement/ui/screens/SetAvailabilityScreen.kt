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
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.cpen321.usermanagement.data.local.models.TimeSlot
import com.cpen321.usermanagement.data.local.models.DayAvailability
import com.cpen321.usermanagement.data.local.models.MoverAvailability

@Composable
fun SetAvailabilityScreen(
    modifier: Modifier = Modifier,
    onAvailabilitySet: (MoverAvailability) -> Unit = {}
) {
    var selectedDays by remember { mutableStateOf(mapOf<DayOfWeek, List<TimeSlot>>()) }
    var showTimePickerDialog by remember { mutableStateOf<DayOfWeek?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Set Availability",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DayOfWeek.values()) { day ->
                DayAvailabilityItem(
                    day = day,
                    timeSlots = selectedDays[day] ?: emptyList(),
                    onAddTimeSlot = { showTimePickerDialog = day },
                    onRemoveTimeSlot = { slot ->
                        selectedDays = selectedDays.toMutableMap().apply {
                            this[day] = (this[day] ?: emptyList()) - slot
                            if (this[day]?.isEmpty() == true) {
                                remove(day)
                            }
                        }
                    }
                )
            }
        }

        if (showTimePickerDialog != null) {
            val day = showTimePickerDialog!!
            TimeSlotPickerDialog(
                onDismiss = { showTimePickerDialog = null },
                onConfirm = { startTime, endTime ->
                    val newSlot = TimeSlot(startTime, endTime)
                    selectedDays = selectedDays.toMutableMap().apply {
                        put(day, (get(day) ?: emptyList()) + newSlot)
                    }
                    showTimePickerDialog = null
                }
            )
        }

        Button(
            onClick = {
                val availability = MoverAvailability(
                    moverId = "TODO", // Replace with actual mover ID
                    availableDays = selectedDays.map { (day, slots) ->
                        DayAvailability(day, slots)
                    }
                )
                onAvailabilitySet(availability)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Save Availability")
        }
    }
}

@Composable
private fun DayAvailabilityItem(
    day: DayOfWeek,
    timeSlots: List<TimeSlot>,
    onAddTimeSlot: () -> Unit,
    onRemoveTimeSlot: (TimeSlot) -> Unit,
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
                        text = "${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}",
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
                    if (endTime.isAfter(startTime)) {
                        onConfirm(startTime, endTime)
                    }
                },
                enabled = endTime.isAfter(startTime)
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
    var textValue by remember(time) { mutableStateOf(formatTime(time)) }
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
                try {
                    if (input.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
                        val parsedTime = LocalTime.parse(input, DateTimeFormatter.ofPattern("HH:mm"))
                        onTimeChange(parsedTime)
                        isError = false
                    } else {
                        isError = true
                    }
                } catch (e: Exception) {
                    isError = true
                }
            },
            modifier = Modifier.width(100.dp),
            singleLine = true,
            isError = isError,
            placeholder = { Text("HH:mm") },
            supportingText = if (isError) {
                { Text("Use HH:mm format") }
            } else null
        )
    }
}

private fun formatTime(time: LocalTime): String {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
}
