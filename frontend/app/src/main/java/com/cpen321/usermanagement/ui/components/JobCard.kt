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
                    text = job.id,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = job.startTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = job.date.format(DateTimeFormatter.ISO_DATE),
                style = MaterialTheme.typography.bodyMedium
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
                    text = job.id,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${String.format("%.2f", job.price)}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pickup: ${job.pickupAddress}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Drop-off: ${job.dropoffAddress}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = job.date.format(DateTimeFormatter.ISO_DATE),
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
