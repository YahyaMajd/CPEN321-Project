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
import com.cpen321.usermanagement.data.local.models.MoverAvailability
import com.cpen321.usermanagement.ui.components.AvailableJobCard

@Composable
fun AvailableJobsScreen(
    jobs: List<Job>,
    moverAvailability: MoverAvailability? = null,
    onJobAccept: (Job) -> Unit,
    onJobDetails: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    var showOnlyAvailable by remember { mutableStateOf(false) }

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
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(jobs) { job ->
                AvailableJobCard(
                    job = job,
                    onAcceptClick = { onJobAccept(job) },
                    onDetailsClick = { onJobDetails(job) }
                )
            }
        }
    }
}
