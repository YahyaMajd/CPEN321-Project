package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.BoxQuantity
import com.cpen321.usermanagement.data.local.models.STANDARD_BOX_SIZES
import com.cpen321.usermanagement.data.local.models.OrderRequest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderBottomSheet(
    onDismiss: () -> Unit,
    onSubmitOrder: (OrderRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxQuantities by remember {
        mutableStateOf(STANDARD_BOX_SIZES.map { BoxQuantity(it, 0) })
    }
    
    // Address fields
    var streetAddress by remember { mutableStateOf("123 Main St") }
    var city by remember { mutableStateOf("Vancouver") }
    var province by remember { mutableStateOf("BC") }
    var postalCode by remember { mutableStateOf("V6T 1Z4") }
    var isAddressExpanded by remember { mutableStateOf(false) }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { 
        mutableStateOf(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)) // 7 days from now
    }
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val returnDate = dateFormatter.format(Date(selectedDateMillis))
    
    val totalBoxes = boxQuantities.sumOf { it.quantity }
    val canSubmit = totalBoxes > 0
    val fullAddress = "$streetAddress, $city, $province $postalCode"
    
    Column(
        modifier = modifier.padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Create Order",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Box Selection
        Text(
            text = "Select Boxes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(boxQuantities) { boxQuantityItem ->
                BoxSelectionItem(
                    boxQuantity = boxQuantityItem,
                    onQuantityChange = { newQuantity ->
                        boxQuantities = boxQuantities.map { item ->
                            if (item.boxSize == boxQuantityItem.boxSize) {
                                item.copy(quantity = newQuantity)
                            } else item
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Order Details
        Text(
            text = "Order Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Address Section
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Address Header (clickable to expand/collapse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pickup Address",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isAddressExpanded) "Edit address" else fullAddress,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { isAddressExpanded = !isAddressExpanded }
                    ) {
                        Icon(
                            if (isAddressExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                            contentDescription = if (isAddressExpanded) "Collapse" else "Edit address"
                        )
                    }
                }
                
                // Expandable Address Form
                if (isAddressExpanded) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = streetAddress,
                            onValueChange = { streetAddress = it },
                            label = { Text("Street Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = { Text("City") },
                                modifier = Modifier.weight(1f)
                            )
                            
                            OutlinedTextField(
                                value = province,
                                onValueChange = { province = it },
                                label = { Text("Province") },
                                modifier = Modifier.weight(0.5f)
                            )
                        }
                        
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = { Text("Postal Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Return Date (clickable to open date picker)
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Return Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = returnDate,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change date",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Submit Button
        Button(
            onClick = {
                val orderRequest = OrderRequest(
                    boxQuantities = boxQuantities.filter { it.quantity > 0 },
                    currentAddress = fullAddress,
                    returnDate = returnDate
                )
                onSubmitOrder(orderRequest)
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Order ($totalBoxes boxes)")
        }
        
        // Extra space for gesture area
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            initialSelectedDateMillis = selectedDateMillis,
            onDateSelected = { dateMillis ->
                dateMillis?.let { selectedDateMillis = it }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialSelectedDateMillis: Long,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun BoxSelectionItem(
    boxQuantity: BoxQuantity,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${boxQuantity.boxSize.type} Box",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = boxQuantity.boxSize.dimensions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = boxQuantity.boxSize.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            QuantityCounter(
                quantity = boxQuantity.quantity,
                onQuantityChange = onQuantityChange
            )
        }
    }
}