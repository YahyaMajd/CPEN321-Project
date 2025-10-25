package com.cpen321.usermanagement.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CallToAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.data.remote.dto.JobInRoute
import com.cpen321.usermanagement.data.remote.dto.RouteMetrics
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.viewmodels.SmartRouteUiState
import com.cpen321.usermanagement.ui.viewmodels.SmartRouteViewModel
import com.cpen321.usermanagement.utils.TimeUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRouteBottomSheet(
    onDismiss: () -> Unit,
    onJobClick: (String) -> Unit,
    onAcceptAll: (List<String>) -> Unit,
    viewModel: SmartRouteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    val removedJobs by viewModel.removedJobs.collectAsState()
    var selectedDuration by remember { mutableStateOf<Int?>(null) } // null = unlimited
    var showDurationSelector by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show snackbar when jobs are removed from route
    LaunchedEffect(removedJobs) {
        if (removedJobs.isNotEmpty()) {
            val message = if (removedJobs.size == 1) {
                "A job was accepted by another mover and removed from your route"
            } else {
                "${removedJobs.size} jobs were accepted by other movers and removed from your route"
            }
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearRemovedJobs()
        }
    }
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted && !showDurationSelector) {
            fetchCurrentLocationAndRoute(context, viewModel, selectedDuration)
        }
    }
    
    LaunchedEffect(Unit) {
        if (hasLocationPermission && !showDurationSelector) {
            fetchCurrentLocationAndRoute(context, viewModel, selectedDuration)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Smart Route Suggestion",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            // Show duration selector first, then route
            if (showDurationSelector) {
                DurationSelector(
                    selectedDuration = selectedDuration,
                    onDurationSelected = { duration ->
                        selectedDuration = duration
                    },
                    onConfirm = {
                        if (!hasLocationPermission) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            fetchCurrentLocationAndRoute(context, viewModel, selectedDuration)
                        }
                        showDurationSelector = false
                    }
                )
            } else {
                // Existing route display logic
                when (uiState) {
                is SmartRouteUiState.Idle -> {
                    if (!hasLocationPermission) {
                        LocationPermissionRequired(
                            onRequestPermission = {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(spacing.large)
                        )
                    }
                }
                
                is SmartRouteUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(spacing.large)
                    )
                    Text(
                        text = "Calculating optimal route...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                is SmartRouteUiState.Success -> {
                    val data = (uiState as SmartRouteUiState.Success).data
                    if (data.route.isEmpty()) {
                        EmptyRouteState()
                    } else {
                        RouteContent(
                            route = data.route,
                            metrics = data.metrics,
                            onJobClick = onJobClick,
                            onAcceptAll = onAcceptAll
                        )
                    }
                }
                
                is SmartRouteUiState.Error -> {
                    ErrorState(
                        message = (uiState as SmartRouteUiState.Error).message,
                        onRetry = {
                            if (hasLocationPermission) {
                                fetchCurrentLocationAndRoute(context, viewModel, selectedDuration)
                            }
                        }
                    )
                }
            }
        }
        }
        }
    }
}

@Composable
private fun LocationPermissionRequired(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Location Permission Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We need your location to calculate the optimal route",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun RouteContent(
    route: List<JobInRoute>,
    metrics: RouteMetrics,
    onJobClick: (String) -> Unit,
    onAcceptAll: (List<String>) -> Unit
) {
    val spacing = LocalSpacing.current
    
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Metrics summary card
        item {
            RouteMetricsCard(metrics = metrics)
        }
        
        // Accept All Button
        item {
            Button(
                onClick = { 
                    onAcceptAll(route.map { it.jobId })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.small),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.CallToAction,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accept All Jobs (${route.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Route jobs
        item {
            Text(
                text = "Optimized Route (${route.size} jobs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = spacing.small)
            )
        }
        
        itemsIndexed(route) { index, job ->
            RouteJobCard(
                job = job,
                index = index,
                onJobClick = { onJobClick(job.jobId) }
            )
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(spacing.large))
        }
    }
}

@Composable
private fun RouteMetricsCard(metrics: RouteMetrics) {
    val spacing = LocalSpacing.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium)
        ) {
            Text(
                text = "Route Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Earnings",
                    value = "$${String.format("%.2f", metrics.totalEarnings)}",
                    icon = Icons.Default.AttachMoney
                )
                MetricItem(
                    label = "Jobs",
                    value = metrics.totalJobs.toString(),
                    icon = Icons.Default.CallToAction
                )
                MetricItem(
                    label = "Distance",
                    value = "${String.format("%.1f", metrics.totalDistance)} km",
                    icon = Icons.Default.DirectionsCar
                )
            }
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Duration",
                    value = "${metrics.totalDuration} min",
                    icon = Icons.Default.Schedule
                )
                MetricItem(
                    label = "$/Hour",
                    value = "$${String.format("%.2f", metrics.earningsPerHour)}",
                    icon = Icons.Default.AttachMoney
                )
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteJobCard(
    job: JobInRoute,
    index: Int,
    onJobClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(spacing.small))
                    Text(
                        text = job.jobType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                Text(
                    text = "$${String.format("%.2f", job.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            // Travel info from previous
            if (job.distanceFromPrevious > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.1f", job.distanceFromPrevious)} km • ${job.travelTimeFromPrevious} min travel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(spacing.extraSmall))
            }
            
            // Pickup address
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = job.pickupAddress.formattedAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            
            // Time and duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = TimeUtils.formatDateTime(job.scheduledTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }
                Text(
                    text = "${job.estimatedDuration} min job",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
            }
            
            // Volume
            Text(
                text = "${String.format("%.1f", job.volume)} m³",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                modifier = Modifier.padding(top = spacing.extraSmall)
            )
            
            Spacer(modifier = Modifier.height(spacing.small))
            
            // Accept button
            Button(
                onClick = onJobClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(spacing.extraSmall))
                Text("Accept Job")
            }
        }
    }
}

@Composable
private fun EmptyRouteState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Jobs Available, Please Adjust Your Availability or Duration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No jobs match your current availability schedule",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun DurationSelector(
    selectedDuration: Int?,
    onDurationSelected: (Int?) -> Unit,
    onConfirm: () -> Unit
) {
    val spacing = LocalSpacing.current
    val durationOptions = listOf(
        null to "Unlimited",
        120 to "2 hours",
        180 to "3 hours",
        240 to "4 hours",
        300 to "5 hours",
        360 to "6 hours",
        480 to "8 hours"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(spacing.medium))
        
        Text(
            text = "Maximum Shift Duration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(spacing.small))
        
        Text(
            text = "How long do you want to work?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(spacing.large))
        
        // Duration options
        durationOptions.forEach { (duration, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedDuration == duration,
                    onClick = { onDurationSelected(duration) }
                )
                Spacer(modifier = Modifier.width(spacing.small))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (duration != null) {
                    Text(
                        text = "$duration min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(spacing.large))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = true
        ) {
            Text("Find Smart Route")
        }
        
        Spacer(modifier = Modifier.height(spacing.medium))
    }
}

private fun fetchCurrentLocationAndRoute(
    context: android.content.Context,
    viewModel: SmartRouteViewModel,
    maxDuration: Int? = null
) {
    
    /* Production code with actual GPS:*/
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    try {
        // Request fresh location update instead of using cached location
        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                Log.d("SmartRoute", "Mover Location (Fresh): ${location.latitude}, ${location.longitude}")
                viewModel.fetchSmartRoute(location.latitude, location.longitude, maxDuration)
            } else {
                // Fallback to Vancouver downtown if no location
                Log.d("SmartRoute", "No location available, using Vancouver fallback")
                viewModel.fetchSmartRoute(49.2827, -123.1207, maxDuration)
            }
        }.addOnFailureListener { e ->
            Log.e("SmartRoute", "Failed to get location: ${e.message}")
            // Fallback to Vancouver downtown
            viewModel.fetchSmartRoute(49.2827, -123.1207, maxDuration)
        }
    } catch (e: SecurityException) {
        Log.e("SmartRoute", "Location permission error: ${e.message}")
        // Fallback to Vancouver downtown
        viewModel.fetchSmartRoute(49.2827, -123.1207, maxDuration)
        }
    }

