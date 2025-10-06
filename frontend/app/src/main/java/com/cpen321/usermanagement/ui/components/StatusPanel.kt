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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Order
import com.cpen321.usermanagement.data.local.models.OrderStatus
import com.cpen321.usermanagement.data.local.models.displayText
import com.cpen321.usermanagement.data.local.models.Address
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun StatusPanel(
    activeOrder: Order?
) {
    if (activeOrder != null) {
        // Show status when there's an active order
        ActiveOrderStatusContent(
            order = activeOrder
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
                value = "${formatPickupTime(order.pickupTime)}"
            )

            StatusDetailRow(
                icon = Icons.Default.Info,
                label ="Return Date",
                value = "${formatPickupTime(order.returnTime)}"
            )
        }
    }
}


fun formatPickupTime(isoString: String): String {
    return try {
        val date = ZonedDateTime.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a")
        date.format(formatter)
    } catch (e: Exception) {
        isoString // fallback to raw string if parsing fails
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