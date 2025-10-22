package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cpen321.usermanagement.utils.TimeUtils
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Order
import com.cpen321.usermanagement.data.local.models.OrderStatus
import com.cpen321.usermanagement.data.local.models.displayText
import com.cpen321.usermanagement.data.local.models.Job
import com.cpen321.usermanagement.data.local.models.JobType
import com.cpen321.usermanagement.data.local.models.JobStatus
// java.time imports removed (unused)

@Composable
fun StatusPanel(
    activeOrder: Order?,
    studentJobs: List<Job> = emptyList(),
    onCreateReturnJob: () -> Unit = {}
) {
    if (activeOrder != null) {
        // Show status when there's an active order
        ActiveOrderStatusContent(
            order = activeOrder,
            studentJobs = studentJobs,
            onCreateReturnJob = onCreateReturnJob
        )
    } else {
        // Hidden placeholder when no active order
        // This maintains layout consistency without taking visual space
        Spacer(modifier = Modifier.height(0.dp))
    }
}

@Composable
private fun ActiveOrderStatusContent(
    order: Order,
    studentJobs: List<Job> = emptyList(),
    onCreateReturnJob: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Status and Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Status",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Progress Indicator (based on order status)
            val progress = when (order.status) {
                OrderStatus.PENDING -> 0.2f
                OrderStatus.ACCEPTED -> 0.4f
                OrderStatus.PICKED_UP -> 0.7f
                OrderStatus.IN_STORAGE -> 1.0f
                OrderStatus.CANCELLED -> 0.0f
                OrderStatus.RETURNED -> 0.2f
                OrderStatus.COMPLETED -> 0.2f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Status Details
            StatusDetailRow(
                icon = Icons.Default.CheckCircle,
                label = "Status",
                value = order.status.displayText
            )
            
            StatusDetailRow(
                icon = Icons.Default.LocationOn,
                label = "Pickup Address",
                value = order.studentAddress.formattedAddress
            )
            
            // Show warehouse/storage location when order is in storage
            if (order.status == OrderStatus.IN_STORAGE) {
                StatusDetailRow(
                    icon = Icons.Default.LocationOn,
                    label = "Storage Location",
                    value = order.warehouseAddress.formattedAddress
                )
            }
            
            // Volume info
            StatusDetailRow(
                icon = Icons.Default.Info,
                label = "Volume",
                value = "${order.volume} cubic units"
            )
            
            // Price info
            StatusDetailRow(
                icon = Icons.Default.Info,
                label = "Total Price",
                value = "$${String.format("%.2f", order.price)}"
            )

            StatusDetailRow(
                icon = Icons.Default.Info,
                label ="Pickup Date",
                value = "${TimeUtils.formatPickupTime(order.pickupTime)}"
            )

            StatusDetailRow(
                icon = Icons.Default.Info,
                label ="Return Date",
                value = "${TimeUtils.formatPickupTime(order.returnTime)}"
            )
            
            // Check if an active return job exists for this order
            // An active return job is one that's not cancelled and not completed yet
            val hasActiveReturnJob = studentJobs.any { job ->
                job.jobType == JobType.RETURN && 
                job.orderId == order.id && 
                job.status != JobStatus.CANCELLED &&
                job.status != JobStatus.COMPLETED
            }
            
            // Show button only if:
            // 1. Order is in storage
            // 2. No active return job exists (completed return jobs don't count)
            if (order.status == OrderStatus.IN_STORAGE && !hasActiveReturnJob) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onCreateReturnJob) {
                        //this is technically creating a create return job, but makes more sense to the user to see it as Confirm Return
                        Text("Confirm Order Return")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}