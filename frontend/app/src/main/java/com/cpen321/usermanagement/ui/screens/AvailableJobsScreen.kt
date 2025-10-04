package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Job
import com.cpen321.usermanagement.ui.components.AvailableJobCard

@Composable
fun AvailableJobsScreen(
    jobs: List<Job>,
    onJobClick: (Job) -> Unit,
    onAcceptJob: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllJobs by remember { mutableStateOf(true) }

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
                    text = if (showAllJobs) "Show All" else "Within Availability",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = showAllJobs,
                    onCheckedChange = { showAllJobs = it }
                )
            }
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(jobs) { job ->
                AvailableJobCard(
                    job = job,
                    onDetailsClick = { onJobClick(job) },
                    onAcceptClick = { onAcceptJob(job) }
                )
            }
        }
    }
}
