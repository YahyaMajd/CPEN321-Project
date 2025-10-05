package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Job
import com.cpen321.usermanagement.ui.components.CurrentJobCard

@Composable
fun CurrentJobsScreen(
    jobs: List<Job>,
    onJobDetails: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Current Jobs",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(jobs) { job ->
                CurrentJobCard(
                    job = job,
                    onDetailsClick = { onJobDetails(job) }
                )
            }
        }
    }
}
