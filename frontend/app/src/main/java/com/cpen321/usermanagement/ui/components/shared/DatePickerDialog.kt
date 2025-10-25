package com.cpen321.usermanagement.ui.components.shared

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.util.Calendar
import java.util.TimeZone

/**
 * A reusable date picker dialog component that properly handles UTC timezone.
 * 
 * Material3's DatePicker operates entirely in UTC timezone:
 * - Returns selectedDateMillis as UTC milliseconds (midnight UTC)
 * - Expects selectableDates validation in UTC
 * 
 * This component ensures all date operations use UTC timezone to avoid
 * off-by-one day bugs that occur when mixing UTC and local timezones.
 *
 * @param onDateSelected Callback invoked with selected date in UTC milliseconds
 * @param onDismiss Callback invoked when dialog is dismissed
 * @param title Title text for the dialog (e.g., "Select Pickup Date")
 * @param initialDateMillis Initial date to display in UTC milliseconds (default: today)
 * @param minDateOffsetDays Minimum selectable date offset from today in days (default: 0 = today)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Date",
    initialDateMillis: Long? = null,
    minDateOffsetDays: Int = 0
) {
    // Get today's date at midnight in UTC timezone
    val today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // Calculate minimum selectable date in UTC
    val minSelectableDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = today
        add(Calendar.DAY_OF_YEAR, minDateOffsetDays)
    }.timeInMillis

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: today,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Note: Parameter is explicitly named utcTimeMillis in Material3 API
                // This confirms the DatePicker operates in UTC timezone
                return utcTimeMillis >= minSelectableDate
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        onDateSelected(selectedMillis)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = { Text(title) }
        )
    }
}
