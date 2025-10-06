package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Job
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentJobCard(
    job: Job,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onDetailsClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${job.jobType.value} Job",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = job.scheduledTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = job.scheduledTime.format(DateTimeFormatter.ISO_LOCAL_DATE),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Status: ${job.status.value}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun AvailableJobCard(
    job: Job,
    onDetailsClick: () -> Unit,
    onAcceptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${job.jobType.value} Job",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${String.format("%.2f", job.price)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pickup: ${job.pickupAddress.formattedAddress}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Drop-off: ${job.dropoffAddress.formattedAddress}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Volume: ${String.format("%.1f", job.volume)} m³",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = job.scheduledTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDetailsClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }
                Button(
                    onClick = onAcceptClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept")
                }
            }
        }
    }
}
