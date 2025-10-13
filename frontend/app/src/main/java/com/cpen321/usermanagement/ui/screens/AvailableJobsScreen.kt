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
import dagger.hilt.android.EntryPointAccessors

@Composable
fun AvailableJobsScreen(
    onJobDetails: (Job) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JobViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showOnlyAvailable by remember { mutableStateOf(false) }

    val appCtx = LocalContext.current.applicationContext
    // Load available jobs when screen is first composed and update on socket updates
    LaunchedEffect(Unit) {
        // initial load on open
        viewModel.loadAvailableJobs()

        // obtain SocketClient via Hilt EntryPoint
        val entry = EntryPointAccessors.fromApplication(appCtx, SocketClientEntryPoint::class.java)
        val socketClient = entry.socketClient()

        // Diagnostic: log initial connection state when the screen starts
        try {
            Log.d("AvailableJobsScreen", "initial socket connected=${socketClient.isConnected()}")
        } catch (_: Throwable) { }

        socketClient.events.collect { ev ->
            if (ev.name == "job.created" || ev.name == "job.updated") {
                // Diagnostic log: confirm we received the event and whether socket is connected
                try {
                    Log.d("AvailableJobsScreen", "socket event=${ev.name} connected=${socketClient.isConnected()} payload=${ev.payload}")
                } catch (_: Throwable) { }
                viewModel.loadAvailableJobs()
            }
        }
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
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: ${uiState.error}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadAvailableJobs() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                val jobsToShow = remember(uiState.availableJobs, showOnlyAvailable) {
                    if (showOnlyAvailable) {
                        // TODO: This is a placeholder. You need to implement the actual filtering
                        // logic based on the mover's availability, which may require fetching that data.
                        // For example: uiState.availableJobs.filter { job -> isWithinAvailability(job) }
                        uiState.availableJobs
                    } else {
                        uiState.availableJobs
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
                                onAcceptClick = { viewModel.acceptJob(job.id) },
                                onDetailsClick = { onJobDetails(job) }
                            )
                        }
                    }
                }
            }
        }
    }
}
