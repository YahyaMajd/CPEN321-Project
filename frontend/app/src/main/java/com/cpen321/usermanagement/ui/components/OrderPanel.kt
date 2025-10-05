package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Address

@Composable
fun OrderPanel(
    hasActiveOrder: Boolean = false,
    activeOrder: com.cpen321.usermanagement.data.local.models.Order? = null,
    onCreateOrderClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!hasActiveOrder) {
                // No Active Order State
                NoActiveOrderContent(onCreateOrderClick = onCreateOrderClick)
            } else {
                // Active Order State with Map
                ActiveOrderContent(activeOrder = activeOrder)
            }
        }
    }
}

@Composable
private fun NoActiveOrderContent(
    onCreateOrderClick: () -> Unit
) {
    // Package/Shipping Icon
    Icon(
        imageVector = Icons.Filled.ShoppingCart,
        contentDescription = "No active order",
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Main message
    Text(
        text = "You don't have an active order.",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    // Subtitle
    Text(
        text = "Create a new order to get started with DormDash moving services.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Create Order Button
    Button(
        onClick = onCreateOrderClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "Create New Order",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActiveOrderContent(
    activeOrder: com.cpen321.usermanagement.data.local.models.Order?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Order Status
        Text(
            text = "Order Active",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Status message
        Text(
            text = "Your moving order has been created successfully!",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Map showing pickup location
        if (activeOrder != null) {
            OrderMapView(
                address = activeOrder.studentAddress.formattedAddress,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Order details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Volume",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${activeOrder.volume} cubic units",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Return Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activeOrder.returnTime,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // View Details Button (placeholder for now)
        Button(
            onClick = { /* TODO: Navigate to order details */ },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("View Order Details")
        }
    }
}