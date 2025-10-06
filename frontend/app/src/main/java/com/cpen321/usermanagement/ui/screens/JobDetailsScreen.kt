package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.data.local.models.JobStatus
import com.cpen321.usermanagement.ui.viewmodels.JobViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsScreen(
    jobId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JobViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Find the job from available or mover jobs
    val job = remember(uiState.availableJobs, uiState.moverJobs, jobId) {
        uiState.availableJobs.find { it.id == jobId } 
            ?: uiState.moverJobs.find { it.id == jobId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (job == null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Job not found")
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Job Type and Status
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${job.jobType.value} Job",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when (job.status) {
                                    JobStatus.AVAILABLE -> MaterialTheme.colorScheme.primaryContainer
                                    JobStatus.ASSIGNED -> MaterialTheme.colorScheme.secondaryContainer
                                    JobStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiaryContainer
                                    JobStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
                                    JobStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                                }
                            ) {
                                Text(
                                    text = job.status.value,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        Text(
                            text = "Job ID: ${job.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Price and Volume
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Job Information",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Payment",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${String.format("%.2f", job.price)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = "Volume",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.1f", job.volume)} m³",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }

                // Addresses
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Locations",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Pickup",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = job.pickupAddress.formattedAddress,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Divider()
                        
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Drop-off",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = job.dropoffAddress.formattedAddress,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Scheduled Time
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Scheduled Time",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = job.scheduledTime.format(
                                DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = job.scheduledTime.format(
                                DateTimeFormatter.ofPattern("hh:mm a")
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                if (job.status == JobStatus.AVAILABLE) {
                    Button(
                        onClick = { 
                            viewModel.acceptJob(job.id)
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text("Accept This Job")
                    }
                }
            }
        }
    }
}
