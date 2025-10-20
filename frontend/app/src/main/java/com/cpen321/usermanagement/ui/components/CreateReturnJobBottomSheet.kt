package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Address
import com.cpen321.usermanagement.data.local.models.CreateReturnJobRequest
import com.cpen321.usermanagement.data.local.models.Order
import com.cpen321.usermanagement.data.local.models.TestPaymentMethods
import com.cpen321.usermanagement.data.local.models.TestCard
import com.cpen321.usermanagement.data.local.models.CustomerInfo
import com.cpen321.usermanagement.data.local.models.PaymentAddress
import com.cpen321.usermanagement.data.repository.PaymentRepository
import com.cpen321.usermanagement.utils.LocationUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReturnJobBottomSheet(
    activeOrder: Order,
    paymentRepository: PaymentRepository,
    onDismiss: () -> Unit,
    onSubmit: (CreateReturnJobRequest, String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Step state
    var currentStep by remember { mutableStateOf(ReturnJobStep.SELECT_DATE) }
    
    // Date selection state
    var selectedDateMillis by remember { 
        mutableStateOf(System.currentTimeMillis())
    }
    var returnHour by remember { mutableStateOf(17) } // Default 5 PM
    var returnMinute by remember { mutableStateOf(0) }
    val dateFormatter = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    
    // Address state
    var useCustomAddress by remember { mutableStateOf(false) }
    var streetAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var customAddress by remember { mutableStateOf<Address?>(null) }
    var isGeocoding by remember { mutableStateOf(false) }
    
    // Fee calculation
    val expectedReturnDate = remember { 
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(activeOrder.returnTime)?.time ?: System.currentTimeMillis()
    }
    
    val daysDifference = remember(selectedDateMillis) {
        val diff = (selectedDateMillis - expectedReturnDate) / (1000 * 60 * 60 * 24)
        diff.toInt()
    }
    
    val adjustmentAmount = remember(daysDifference) {
        Math.abs(daysDifference) * 5.0
    }
    
    val isEarlyReturn = daysDifference < 0
    val isLateReturn = daysDifference > 0
    
    // Payment state
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentIntentId by remember { mutableStateOf<String?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Return Job",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Content based on step
            when (currentStep) {
                ReturnJobStep.SELECT_DATE -> {
                    DateSelectionStep(
                        expectedReturnDate = dateFormatter.format(Date(expectedReturnDate)),
                        selectedDate = dateFormatter.format(Date(selectedDateMillis)),
                        onDateClick = { showDatePicker = true },
                        returnHour = returnHour,
                        returnMinute = returnMinute,
                        onTimeClick = { showTimeDialog = true },
                        daysDifference = daysDifference,
                        adjustmentAmount = adjustmentAmount,
                        isEarlyReturn = isEarlyReturn,
                        isLateReturn = isLateReturn,
                        onNext = {
                            if (isLateReturn) {
                                currentStep = ReturnJobStep.PAYMENT
                            } else {
                                currentStep = ReturnJobStep.ADDRESS
                            }
                        }
                    )
                }
                
                ReturnJobStep.ADDRESS -> {
                    AddressSelectionStep(
                        defaultAddress = activeOrder.returnAddress?.formattedAddress 
                            ?: activeOrder.studentAddress.formattedAddress,
                        useCustomAddress = useCustomAddress,
                        onUseCustomAddressChange = { useCustomAddress = it },
                        streetAddress = streetAddress,
                        onStreetAddressChange = { streetAddress = it },
                        city = city,
                        onCityChange = { city = it },
                        province = province,
                        onProvinceChange = { province = it },
                        postalCode = postalCode,
                        onPostalCodeChange = { postalCode = it },
                        isGeocoding = isGeocoding,
                        onConfirm = {
                            if (useCustomAddress) {
                                val fullAddress = "$streetAddress, $city, $province $postalCode"
                                if (streetAddress.isBlank() || city.isBlank()) {
                                    return@AddressSelectionStep
                                }
                                
                                isGeocoding = true
                                coroutineScope.launch {
                                    try {
                                        val coordinates = LocationUtils.geocodeAddress(context, fullAddress)
                                            ?: LocationUtils.getFallbackCoordinates(fullAddress)

                                        customAddress = Address(
                                            lat = coordinates.latitude,
                                            lon = coordinates.longitude,
                                            formattedAddress = fullAddress
                                        )
                                        
                                        // Submit the return job
                                        submitReturnJob(
                                            selectedDateMillis = selectedDateMillis,
                                            returnHour = returnHour,
                                            returnMinute = returnMinute,
                                            customAddress = customAddress,
                                            isEarlyReturn = isEarlyReturn,
                                            paymentIntentId = null,
                                            onSubmit = onSubmit
                                        )
                                    } catch (e: Exception) {
                                        // Handle error
                                    } finally {
                                        isGeocoding = false
                                    }
                                }
                            } else {
                                // Use default address
                                submitReturnJob(
                                    selectedDateMillis = selectedDateMillis,
                                    returnHour = returnHour,
                                    returnMinute = returnMinute,
                                    customAddress = null,
                                    isEarlyReturn = isEarlyReturn,
                                    paymentIntentId = null,
                                    onSubmit = onSubmit
                                )
                            }
                        }
                    )
                }
                
                ReturnJobStep.PAYMENT -> {
                    PaymentStep(
                        lateFee = adjustmentAmount,
                        isProcessing = isProcessingPayment,
                        onPayment = { selectedCard ->
                            isProcessingPayment = true
                            coroutineScope.launch {
                                try {
                                    val intentResult = paymentRepository.createPaymentIntent(adjustmentAmount)
                                    
                                    intentResult.fold(
                                        onSuccess = { intent ->
                                            val customerInfo = CustomerInfo(
                                                name = "Student",
                                                email = "student@example.com",
                                                address = PaymentAddress(
                                                    line1 = activeOrder.studentAddress.formattedAddress,
                                                    city = "Vancouver",
                                                    state = "BC",
                                                    postalCode = "V6T1Z4",
                                                    country = "CA"
                                                )
                                            )
                                            
                                            val paymentResult = paymentRepository.processPayment(
                                                intent.id,
                                                customerInfo,
                                                selectedCard.paymentMethodId
                                            )
                                            
                                            paymentResult.fold(
                                                onSuccess = { payment ->
                                                    if (payment.status == "SUCCEEDED") {
                                                        paymentIntentId = intent.id
                                                        currentStep = ReturnJobStep.ADDRESS
                                                    } else {
                                                        // Payment failed
                                                    }
                                                    isProcessingPayment = false
                                                },
                                                onFailure = { exception ->
                                                    // Handle payment error
                                                    isProcessingPayment = false
                                                }
                                            )
                                        },
                                        onFailure = { exception ->
                                            // Handle intent creation error
                                            isProcessingPayment = false
                                        }
                                    )
                                } catch (e: Exception) {
                                    // Handle payment error
                                    isProcessingPayment = false
                                }
                            }
                        }
                    )
                }
            }
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
    
    // Time Picker Dialog
    if (showTimeDialog) {
        TimePickerDialog(
            initialHour = returnHour,
            initialMinute = returnMinute,
            onTimeSelected = { hour, minute ->
                returnHour = hour
                returnMinute = minute
                showTimeDialog = false
            },
            onDismiss = { showTimeDialog = false }
        )
    }
}

private fun submitReturnJob(
    selectedDateMillis: Long,
    returnHour: Int,
    returnMinute: Int,
    customAddress: Address?,
    isEarlyReturn: Boolean,
    paymentIntentId: String?,
    onSubmit: (CreateReturnJobRequest, String?) -> Unit
) {
    // Combine date and time
    val calendar = Calendar.getInstance().apply {
        timeInMillis = selectedDateMillis
        set(Calendar.HOUR_OF_DAY, returnHour)
        set(Calendar.MINUTE, returnMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val actualReturnDate = isoFormatter.format(calendar.time)
    
    val request = CreateReturnJobRequest(
        returnAddress = customAddress,
        actualReturnDate = actualReturnDate
    )
    
    onSubmit(request, paymentIntentId)
}

@Composable
private fun DateSelectionStep(
    expectedReturnDate: String,
    selectedDate: String,
    onDateClick: () -> Unit,
    returnHour: Int,
    returnMinute: Int,
    onTimeClick: () -> Unit,
    daysDifference: Int,
    adjustmentAmount: Double,
    isEarlyReturn: Boolean,
    isLateReturn: Boolean,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "Select Return Date & Time",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Expected return date: $expectedReturnDate",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Card
            OutlinedCard(
                modifier = Modifier.weight(1f),
                onClick = onDateClick
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedDate,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Time Card
            OutlinedCard(
                modifier = Modifier.weight(1f),
                onClick = onTimeClick
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%02d:%02d", returnHour, returnMinute),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Fee preview
        if (isEarlyReturn || isLateReturn) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEarlyReturn) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (isEarlyReturn) "Early Return Refund" else "Late Return Fee",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isEarlyReturn) {
                            "You're returning ${Math.abs(daysDifference)} days early"
                        } else {
                            "You're returning $daysDifference days late"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (isEarlyReturn) {
                            "You'll receive a refund of $${String.format("%.2f", adjustmentAmount)}"
                        } else {
                            "Additional charge: $${String.format("%.2f", adjustmentAmount)} (${Math.abs(daysDifference)} days × $5/day)"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLateReturn) "Proceed to Payment" else "Continue")
        }
    }
}

@Composable
private fun AddressSelectionStep(
    defaultAddress: String,
    useCustomAddress: Boolean,
    onUseCustomAddressChange: (Boolean) -> Unit,
    streetAddress: String,
    onStreetAddressChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    province: String,
    onProvinceChange: (String) -> Unit,
    postalCode: String,
    onPostalCodeChange: (String) -> Unit,
    isGeocoding: Boolean,
    onConfirm: () -> Unit
) {
    Column {
        Text(
            text = "Return Address",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Default address option
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = !useCustomAddress,
                onClick = { onUseCustomAddressChange(false) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Use default address",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = defaultAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Custom address option
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = useCustomAddress,
                onClick = { onUseCustomAddressChange(true) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Use custom address",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        
        if (useCustomAddress) {
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = streetAddress,
                onValueChange = onStreetAddressChange,
                label = { Text("Street Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("City") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = province,
                    onValueChange = onProvinceChange,
                    label = { Text("Province") },
                    modifier = Modifier.weight(0.5f),
                    singleLine = true
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = postalCode,
                onValueChange = onPostalCodeChange,
                label = { Text("Postal Code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onConfirm,
            enabled = !isGeocoding && (!useCustomAddress || (streetAddress.isNotBlank() && city.isNotBlank())),
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
                Text("Create Return Job")
            }
        }
    }
}

@Composable
private fun PaymentStep(
    lateFee: Double,
    isProcessing: Boolean,
    onPayment: (TestCard) -> Unit
) {
    var selectedTestCard by remember { mutableStateOf(TestPaymentMethods.TEST_CARDS[0]) }
    var showCardSelector by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "Payment Required",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Late Return Fee",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Total Amount Due",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "$${String.format("%.2f", lateFee)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test card selector
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showCardSelector = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedTestCard.description,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "•••• ${selectedTestCard.number.takeLast(4)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change card"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { onPayment(selectedTestCard) },
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing Payment...")
                }
            } else {
                Text("Pay $${String.format("%.2f", lateFee)}")
            }
        }
    }
    
    if (showCardSelector) {
        TestCardSelectorDialog(
            selectedCard = selectedTestCard,
            onCardSelected = { card ->
                selectedTestCard = card
                showCardSelector = false
            },
            onDismiss = { showCardSelector = false }
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
    // Prevent selecting dates in the past
    val today = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= today
            }
        }
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
private fun TestCardSelectorDialog(
    selectedCard: TestCard,
    onCardSelected: (TestCard) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Test Card") },
        text = {
            Column {
                TestPaymentMethods.TEST_CARDS.forEach { card ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = card == selectedCard,
                            onClick = { onCardSelected(card) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = card.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "•••• ${card.number.takeLast(4)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour selector
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase hour",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = String.format("%02d", selectedHour),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { selectedHour = if (selectedHour > 0) selectedHour - 1 else 23 }) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease hour",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // Minute selector
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { selectedMinute = (selectedMinute + 15) % 60 }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase minute",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = String.format("%02d", selectedMinute),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { selectedMinute = if (selectedMinute >= 15) selectedMinute - 15 else 45 }) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Decrease minute",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onTimeSelected(selectedHour, selectedMinute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private enum class ReturnJobStep {
    SELECT_DATE,
    PAYMENT,
    ADDRESS
}
