//package com.cpen321.usermanagement.ui.screens
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.cpen321.usermanagement.data.local.models.Job
//import com.cpen321.usermanagement.data.local.models.JobStatus
//import com.cpen321.usermanagement.data.local.models.JobType
//import com.cpen321.usermanagement.ui.components.CurrentJobCard
//import com.cpen321.usermanagement.ui.components.MoverJobMapView
//import com.cpen321.usermanagement.ui.viewmodels.JobViewModel
//
//@Composable
//fun MoverActiveJobsScreen(
//    jobs: List<Job>,
//    isLoading: Boolean,
//    error: String?,
//    onJobDetails: (Job) -> Unit,
//    onRefresh: () -> Unit,
//    modifier: Modifier = Modifier,
//    viewModel: JobViewModel = hiltViewModel()
//) {
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(bottom = 16.dp),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = "My Active Jobs",
//                style = MaterialTheme.typography.headlineMedium
//            )
//            if (jobs.isNotEmpty()) {
//                Surface(
//                    shape = MaterialTheme.shapes.medium,
//                    color = MaterialTheme.colorScheme.primaryContainer
//                ) {
//                    Text(
//                        text = "${jobs.size}",
//                        style = MaterialTheme.typography.labelLarge,
//                        color = MaterialTheme.colorScheme.onPrimaryContainer,
//                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
//                    )
//                }
//            }
//        }
//
//        when {
//            isLoading -> {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    CircularProgressIndicator()
//                }
//            }
//
//            error != null -> {
//                Column(
//                    modifier = Modifier.fillMaxSize(),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Text(
//                        text = "Error loading jobs",
//                        style = MaterialTheme.typography.titleMedium,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = error,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        textAlign = TextAlign.Center
//                    )
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Button(onClick = onRefresh) {
//                        Text("Retry")
//                    }
//                }
//            }
//
//            jobs.isEmpty() -> {
//                Column(
//                    modifier = Modifier.fillMaxSize(),
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Text(
//                        text = "No active jobs",
//                        style = MaterialTheme.typography.titleLarge,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = "Check the Available Jobs tab to find work",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
//                        textAlign = TextAlign.Center
//                    )
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Button(onClick = onRefresh) {
//                        Text("Refresh")
//                    }
//                }
//            }
//
//            else -> {
//                LazyColumn(
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    // Show map for active jobs (ACCEPTED or PICKED_UP)
//                    items(jobs.filter { it.status in listOf(JobStatus.ACCEPTED, JobStatus.PICKED_UP) }) { job ->
//                        MoverJobMapView(
//                            job = job,
//                            onArrivedClick = {
//                                val nextStatus = when (job.jobType) {
//                                    JobType.STORAGE -> {
//                                        when (job.status) {
//                                            JobStatus.ACCEPTED -> JobStatus.PICKED_UP
//                                            JobStatus.PICKED_UP -> JobStatus.COMPLETED
//                                            else -> job.status
//                                        }
//                                    }
//                                    JobType.RETURN -> {
//                                        when (job.status) {
//                                            JobStatus.ACCEPTED -> JobStatus.PICKED_UP
//                                            JobStatus.PICKED_UP -> JobStatus.COMPLETED
//                                            else -> job.status
//                                        }
//                                    }
//                                }
//                                viewModel.updateJobStatus(job.id, nextStatus)
//                            }
//                        )
//                    }
//
//                    // Show regular cards for other job statuses
//                    items(jobs.filter { it.status !in listOf(JobStatus.ACCEPTED, JobStatus.PICKED_UP) }) { job ->
//                        CurrentJobCard(
//                            job = job,
//                            onDetailsClick = { onJobDetails(job) }
//                        )
//                    }
//                }
//            }
//        }
//    }
//}