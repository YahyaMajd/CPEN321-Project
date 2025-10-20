package com.cpen321.usermanagement.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.OrderStatus
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current

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
        
        // Map showing relevant location based on order status
        if (activeOrder != null) {
            // Show warehouse location when items are in storage, otherwise show pickup location
            val (displayAddress, markerTitle) = if (activeOrder.status == com.cpen321.usermanagement.data.local.models.OrderStatus.IN_STORAGE) {
                Pair(activeOrder.warehouseAddress.formattedAddress, "Storage Location")
            } else {
                Pair(activeOrder.studentAddress.formattedAddress, "Pickup Location")
            }
            
            OrderMapView(
                address = displayAddress,
                markerTitle = markerTitle,
                modifier = Modifier.fillMaxWidth()
            )
            
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (activeOrder != null && activeOrder.status == OrderStatus.ACCEPTED) {
            // parse pickup time
            val zoned: ZonedDateTime = try {
                ZonedDateTime.parse(activeOrder.pickupTime)
            } catch (e1: Exception) {
                try {
                    OffsetDateTime.parse(activeOrder.pickupTime).toZonedDateTime()
                } catch (e2: Exception) {
                    val ldt = LocalDateTime.parse(activeOrder.pickupTime)
                    ldt.atZone(ZoneId.systemDefault())
                }
            }

            val pacificStart = zoned.withZoneSameInstant(ZoneId.of("America/Los_Angeles"))
            val pacificEnd = pacificStart.plusMinutes(15)
            // so Google shows Pacific time
            val dateFormatterLocal = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

            val title = Uri.encode("DormDash Storage Pickup")
            val details = Uri.encode("Make sure to meet your mover on time!")
            val location = Uri.encode(activeOrder.studentAddress.formattedAddress)

            val calendarEventUrl = "https://www.google.com/calendar/render?action=TEMPLATE" +
                "&text=$title" +
                "&dates=${pacificStart.format(dateFormatterLocal)}/${pacificEnd.format(dateFormatterLocal)}" +
                "&details=$details" +
                "&location=$location" +
                "&ctz=America/Los_Angeles"

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(calendarEventUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Pickup to Calendar")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}