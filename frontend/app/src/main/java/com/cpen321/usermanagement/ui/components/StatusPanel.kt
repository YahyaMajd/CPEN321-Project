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

@Composable
fun StatusPanel(
    hasActiveOrder: Boolean = false,
    // Future parameters for active order data
    orderStatus: String = "",
    estimatedArrival: String = "",
    moverName: String = "",
    currentLocation: String = "",
    modifier: Modifier = Modifier
) {
    if (hasActiveOrder) {
        // Show status when there's an active order
        ActiveOrderStatusContent(
            orderStatus = orderStatus,
            estimatedArrival = estimatedArrival,
            moverName = moverName,
            currentLocation = currentLocation,
            modifier = modifier
        )
    } else {
        // Hidden placeholder when no active order
        // This maintains layout consistency without taking visual space
        Spacer(modifier = modifier.height(0.dp))
    }
}

@Composable
private fun ActiveOrderStatusContent(
    orderStatus: String,
    estimatedArrival: String,
    moverName: String,
    currentLocation: String,
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
                    text = "Order Status",
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
            
            // Progress Indicator (placeholder)
            LinearProgressIndicator(
                progress = 0.6f, // Future: calculate based on actual status
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Status Details
            StatusDetailRow(
                icon = Icons.Default.CheckCircle,
                label = "Status",
                value = if (orderStatus.isNotEmpty()) orderStatus else "In Progress"
            )
            
            StatusDetailRow(
                icon = Icons.Default.LocationOn,
                label = "Current Location",
                value = if (currentLocation.isNotEmpty()) currentLocation else "On the way"
            )
            
            StatusDetailRow(
                icon = Icons.Default.Person,
                label = "Mover",
                value = if (moverName.isNotEmpty()) moverName else "John Doe"
            )
            
            // ETA Section
            if (estimatedArrival.isNotEmpty() || true) { // Show placeholder for now
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ETA: ${if (estimatedArrival.isNotEmpty()) estimatedArrival else "15 minutes"}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
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