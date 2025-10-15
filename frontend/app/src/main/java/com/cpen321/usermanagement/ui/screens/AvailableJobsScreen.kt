package com.cpen321.usermanagement.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.data.local.models.Job
import com.cpen321.usermanagement.di.SocketClientEntryPoint
import com.cpen321.usermanagement.ui.components.AvailableJobCard
import com.cpen321.usermanagement.ui.viewmodels.JobViewModel
import com.cpen321.usermanagement.ui.viewmodels.MoverAvailabilityViewModel
import com.cpen321.usermanagement.utils.TimeUtils
import dagger.hilt.android.EntryPointAccessors
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun AvailableJobsScreen(
    onJobDetails: (Job) -> Unit,
    modifier: Modifier = Modifier,
    jobViewModel: JobViewModel = hiltViewModel(),
    moverAvailabilityViewModel: MoverAvailabilityViewModel = hiltViewModel()
) {
    val jobUiState by jobViewModel.uiState.collectAsState()
    val moverAvailabilityUiState by moverAvailabilityViewModel.uiState.collectAsState()
    var showOnlyAvailable by remember { mutableStateOf(false) }

    // Load available jobs when screen is first composed
    // JobViewModel handles socket events (job.created, job.updated) automatically
    LaunchedEffect(Unit) {
        jobViewModel.loadAvailableJobs()
        moverAvailabilityViewModel.loadAvailability()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Jobs",
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (!showOnlyAvailable) "Show All" else "Within Availability",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showOnlyAvailable,
                    onCheckedChange = { showOnlyAvailable = it }
                )
            }
        }

        when {
            jobUiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            jobUiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: ${jobUiState.error}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { jobViewModel.loadAvailableJobs() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                val jobsToShow = remember(jobUiState.availableJobs, showOnlyAvailable, moverAvailabilityUiState.availability) {
                    if (showOnlyAvailable) {
                        jobUiState.availableJobs.filter { job ->
                            val dt = job.scheduledTime           // LocalDateTime
                            val day = dt.dayOfWeek
                            val t   = dt.toLocalTime()

                            val slots: List<Pair<LocalTime, LocalTime>> = moverAvailabilityUiState.availability[day].orEmpty()

                            slots.any { slot: Pair<LocalTime, LocalTime> ->
                                val (start: LocalTime, end: LocalTime) = slot
                                TimeUtils.isTimeInRange(t, start, end)
                            }
                        }
                    } else {
                        jobUiState.availableJobs
                    }
                }

                if (jobsToShow.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No available jobs",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(jobsToShow) { job ->
                            AvailableJobCard(
                                job = job,
                                onAcceptClick = { jobViewModel.acceptJob(job.id) },
                                onDetailsClick = { onJobDetails(job) }
                            )
                        }
                    }
                }
            }
        }
    }
}
