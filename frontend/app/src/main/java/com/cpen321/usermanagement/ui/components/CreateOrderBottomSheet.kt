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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.*
import com.cpen321.usermanagement.business.DynamicPriceCalculator
import com.cpen321.usermanagement.data.repository.OrderRepository
import com.cpen321.usermanagement.utils.LocationUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderBottomSheet(
    onDismiss: () -> Unit,
    onSubmitOrder: (OrderRequest) -> Unit,
    orderRepository: OrderRepository,
    modifier: Modifier = Modifier
) {
    // Step management
    var currentStep by remember { mutableStateOf(OrderCreationStep.ADDRESS_CAPTURE) }
    var studentAddress by remember { mutableStateOf<Address?>(null) }
    var pricingRules by remember { mutableStateOf<PricingRules?>(null) }
    
    // Error handling
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = modifier.padding(24.dp)
    ) {
        // Header with back button (if not on first step)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentStep != OrderCreationStep.ADDRESS_CAPTURE) {
                    IconButton(
                        onClick = { 
                            currentStep = when(currentStep) {
                                OrderCreationStep.BOX_SELECTION -> OrderCreationStep.ADDRESS_CAPTURE
                                else -> OrderCreationStep.ADDRESS_CAPTURE
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = when(currentStep) {
                        OrderCreationStep.ADDRESS_CAPTURE -> "Enter Address"
                        OrderCreationStep.LOADING_QUOTE -> "Getting Quote"
                        OrderCreationStep.BOX_SELECTION -> "Select Boxes"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Error message display
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Step content
        when (currentStep) {
            OrderCreationStep.ADDRESS_CAPTURE -> {
                AddressCaptureStep(
                    onAddressConfirmed = { address ->
                        studentAddress = address
                        currentStep = OrderCreationStep.LOADING_QUOTE
                        errorMessage = null
                        
                        // Real API call to get quote
                        coroutineScope.launch {
                            try {
                                val result = orderRepository.getQuote(address)
                                result.fold(
                                    onSuccess = { quoteResponse ->
                                        pricingRules = PricingRules(
                                            distanceServiceFee = quoteResponse.distancePrice
                                        )
                                        currentStep = OrderCreationStep.BOX_SELECTION
                                    },
                                    onFailure = { exception ->
                                        errorMessage = "Failed to get pricing: ${exception.message}"
                                        currentStep = OrderCreationStep.ADDRESS_CAPTURE
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = "Failed to get pricing. Please try again."
                                currentStep = OrderCreationStep.ADDRESS_CAPTURE
                            }
                        }
                    },
                    onError = { error ->
                        errorMessage = error
                    }
                )
            }
            
            OrderCreationStep.LOADING_QUOTE -> {
                LoadingQuoteStep()
            }
            
            OrderCreationStep.BOX_SELECTION -> {
                pricingRules?.let { rules ->
                    BoxSelectionStep(
                        pricingRules = rules,
                        studentAddress = studentAddress!!,
                        onSubmitOrder = onSubmitOrder
                    )
                }
            }
        }
    }
}

// Step 1: Address Capture
@Composable
private fun AddressCaptureStep(
    onAddressConfirmed: (Address) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Address fields
    var streetAddress by remember { mutableStateOf("123 Main St") }
    var city by remember { mutableStateOf("Vancouver") }
    var province by remember { mutableStateOf("BC") }
    var postalCode by remember { mutableStateOf("V6T 1Z4") }
    var isGeocoding by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "We need your pickup address to calculate pricing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Address form
        OutlinedTextField(
            value = streetAddress,
            onValueChange = { streetAddress = it },
            label = { Text("Street Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("City") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            OutlinedTextField(
                value = province,
                onValueChange = { province = it },
                label = { Text("Province") },
                modifier = Modifier.weight(0.5f),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = postalCode,
            onValueChange = { postalCode = it },
            label = { Text("Postal Code") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                val fullAddress = "$streetAddress, $city, $province $postalCode"
                if (streetAddress.isBlank() || city.isBlank()) {
                    onError("Please fill in all address fields")
                    return@Button
                }
                
                isGeocoding = true
                coroutineScope.launch {
                    try {
                        val coordinates = LocationUtils.geocodeAddress(context, fullAddress)
                            ?: LocationUtils.getFallbackCoordinates(fullAddress)
                        
                        val address = Address(
                            lat = coordinates.latitude,
                            lon = coordinates.longitude,
                            formattedAddress = fullAddress
                        )
                        onAddressConfirmed(address)
                    } catch (e: Exception) {
                        onError("Failed to process address. Please check and try again.")
                    } finally {
                        isGeocoding = false
                    }
                }
            },
            enabled = !isGeocoding && streetAddress.isNotBlank() && city.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGeocoding) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                }
            } else {
                Text("Get Pricing")
            }
        }
    }
}

// Step 2: Loading Quote
@Composable
private fun LoadingQuoteStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Getting pricing for your location...",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "This may take a few seconds",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Step 3: Box Selection with Dynamic Pricing
@Composable
private fun BoxSelectionStep(
    pricingRules: PricingRules,
    studentAddress: Address,
    onSubmitOrder: (OrderRequest) -> Unit
) {
    var boxQuantities by remember {
        mutableStateOf(STANDARD_BOX_SIZES.map { BoxQuantity(it, 0) })
    }
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { 
        mutableStateOf(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L)) // 7 days from now
    }
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    val returnDate = dateFormatter.format(Date(selectedDateMillis))
    
    val calculator = remember { DynamicPriceCalculator(pricingRules) }
    val currentPrice = calculator.calculateTotal(boxQuantities, returnDate)
    val totalBoxes = boxQuantities.sumOf { it.quantity }
    val canSubmit = totalBoxes > 0
    
    Column {
        // Box Selection
        Text(
            text = "Select Boxes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(boxQuantities) { boxQuantityItem ->
                BoxSelectionItem(
                    boxQuantity = boxQuantityItem,
                    unitPrice = pricingRules.boxPrices[boxQuantityItem.boxSize.type] ?: 0.0,
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
        
        // Return Date Selection
        Text(
            text = "Return Date",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
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
                Text(
                    text = returnDate,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change date",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Price Breakdown
        PriceBreakdownCard(priceBreakdown = currentPrice)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Submit Button
        Button(
            onClick = {
                val orderRequest = OrderRequest(
                    boxQuantities = boxQuantities.filter { it.quantity > 0 },
                    currentAddress = studentAddress.formattedAddress,
                    returnDate = returnDate
                )
                onSubmitOrder(orderRequest)
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Order - $${String.format("%.2f", currentPrice.total)}")
        }
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

@Composable
private fun BoxSelectionItem(
    boxQuantity: BoxQuantity,
    unitPrice: Double,
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
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${boxQuantity.boxSize.type} Box",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$${String.format("%.0f", unitPrice)}/box",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
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

@Composable
private fun PriceBreakdownCard(
    priceBreakdown: PriceBreakdown,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Total (always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${String.format("%.2f", priceBreakdown.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Expandable breakdown
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isExpanded) "Hide breakdown" else "Show breakdown")
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Box details
                priceBreakdown.boxDetails.forEach { boxItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${boxItem.boxType} (${boxItem.quantity}×)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$${String.format("%.2f", boxItem.totalPrice)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                // Daily fee
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rental (${priceBreakdown.days} days)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$${String.format("%.2f", priceBreakdown.dailyFee)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Service fees
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Service & Distance Fee",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$${String.format("%.2f", priceBreakdown.serviceFee)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
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