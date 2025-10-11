//package com.cpen321.usermanagement.ui.components
//
//import android.content.Context
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.LocationOn
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.model.CameraPosition
//import com.google.android.gms.maps.model.LatLng
//import com.google.maps.android.compose.*
//import com.cpen321.usermanagement.data.local.models.Job
//import com.cpen321.usermanagement.data.local.models.JobType
//import com.cpen321.usermanagement.data.local.models.JobStatus
//import com.cpen321.usermanagement.data.remote.dto.Address
//import com.cpen321.usermanagement.utils.LocationUtils
//import kotlinx.coroutines.launch
//
//@Composable
//fun MoverJobMapView(
//    job: Job,
//    onArrivedClick: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val context = LocalContext.current
//    val coroutineScope = rememberCoroutineScope()
//
//    var mapLocation by remember { mutableStateOf<LatLng?>(null) }
//    var isLoading by remember { mutableStateOf(true) }
//    var hasError by remember { mutableStateOf(false) }
//
//    // Determine current destination based on job type and status
//    val currentDestination = remember(job.jobType, job.status) {
//        when (job.jobType) {
//            JobType.STORAGE -> {
//                when (job.status) {
//                    JobStatus.ACCEPTED -> job.pickupAddress // Go to student location first
//                    JobStatus.PICKED_UP -> job.dropoffAddress // Then to warehouse
//                    else -> job.pickupAddress // Default fallback
//                }
//            }
//            JobType.RETURN -> {
//                when (job.status) {
//                    JobStatus.ACCEPTED -> job.pickupAddress // Go to warehouse first
//                    JobStatus.PICKED_UP -> job.dropoffAddress // Then to student location
//                    else -> job.pickupAddress // Default fallback
//                }
//            }
//        }
//    }
//
//    // Destination info for UI display
//    val destinationInfo = remember(job.jobType, job.status) {
//        when (job.jobType) {
//            JobType.STORAGE -> {
//                when (job.status) {
//                    JobStatus.ACCEPTED -> "Student Pickup Location"
//                    JobStatus.PICKED_UP -> "Storage Warehouse"
//                    else -> "Pickup Location"
//                }
//            }
//            JobType.RETURN -> {
//                when (job.status) {
//                    JobStatus.ACCEPTED -> "Storage Warehouse"
//                    JobStatus.PICKED_UP -> "Student Delivery Location"
//                    else -> "Pickup Location"
//                }
//            }
//        }
//    }
//
//    // Show arrived button only when appropriate
//    val showArrivedButton = remember(job.status) {
//        job.status == JobStatus.ACCEPTED || job.status == JobStatus.PICKED_UP
//    }
//
//    // Button text based on current phase
//    val arrivedButtonText = remember(job.jobType, job.status) {
//        when (job.jobType) {
//            JobType.STORAGE -> {
//                when (job.status) {
//                    JobStatus.ACCEPTED -> "Arrived at Pickup"
//                    JobStatus.PICKED_UP -> "Delivered to Storage"
//                    else -> "Mark as Arrived"
//                }
//            }
//            JobType.RETURN -> {
//                when (job.status) {
//                    JobStatus.ACCEPTED -> "Picked up from Storage"
//                    JobStatus.PICKED_UP -> "Delivered to Student"
//                    else -> "Mark as Arrived"
//                }
//            }
//        }
//    }
//
//    // Geocode the current destination when it changes
//    LaunchedEffect(currentDestination) {
//        isLoading = true
//        coroutineScope.launch {
//            try {
//                val address = currentDestination.formattedAddress
//                val location = LocationUtils.geocodeAddress(context, address)
//                    ?: LatLng(currentDestination.lat, currentDestination.lon)
//
//                mapLocation = location
//                hasError = false
//            } catch (e: Exception) {
//                // Fallback to coordinates from Address object or LocationUtils
//                mapLocation = try {
//                    LatLng(currentDestination.lat, currentDestination.lon)
//                } catch (coordError: Exception) {
//                    LocationUtils.getFallbackCoordinates(currentDestination.formattedAddress)
//                }
//                hasError = true
//            } finally {
//                isLoading = false
//            }
//        }
//    }
//
//    Card(
//        modifier = modifier,
//        shape = RoundedCornerShape(12.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//    ) {
//        Column {
//            // Header with destination info
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(16.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(
//                    imageVector = Icons.Default.LocationOn,
//                    contentDescription = null,
//                    tint = MaterialTheme.colorScheme.primary,
//                    modifier = Modifier.size(24.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Column(modifier = Modifier.weight(1f)) {
//                    Text(
//                        text = destinationInfo,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold,
//                        color = MaterialTheme.colorScheme.onSurface
//                    )
//                    Text(
//                        text = currentDestination.formattedAddress,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//            // Map view
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(200.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                when {
//                    isLoading -> {
//                        CircularProgressIndicator(
//                            modifier = Modifier.size(40.dp),
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
//
//                    mapLocation != null -> {
//                        val cameraPositionState = rememberCameraPositionState {
//                            position = CameraPosition.fromLatLngZoom(mapLocation!!, 16f)
//                        }
//
//                        GoogleMap(
//                            modifier = Modifier.fillMaxSize(),
//                            cameraPositionState = cameraPositionState,
//                            uiSettings = MapUiSettings(
//                                zoomControlsEnabled = true,
//                                compassEnabled = true,
//                                mapToolbarEnabled = false,
//                                myLocationButtonEnabled = true
//                            )
//                        ) {
//                            Marker(
//                                state = MarkerState(position = mapLocation!!),
//                                title = destinationInfo,
//                                snippet = currentDestination.formattedAddress
//                            )
//                        }
//
//                        // Error indicator overlay if using fallback coordinates
//                        if (hasError) {
//                            Card(
//                                modifier = Modifier
//                                    .align(Alignment.TopEnd)
//                                    .padding(8.dp),
//                                colors = CardDefaults.cardColors(
//                                    containerColor = MaterialTheme.colorScheme.errorContainer
//                                )
//                            ) {
//                                Text(
//                                    text = "Approximate location",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    color = MaterialTheme.colorScheme.onErrorContainer,
//                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
//                                )
//                            }
//                        }
//                    }
//
//                    else -> {
//                        // Fallback UI if everything fails
//                        Column(
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            Text(
//                                text = "Map unavailable",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text(
//                                text = currentDestination.formattedAddress,
//                                style = MaterialTheme.typography.bodySmall,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
//                    }
//                }
//            }
//
//            // Arrived button
//            if (showArrivedButton) {
//                Divider()
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(16.dp),
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    Button(
//                        onClick = onArrivedClick,
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = MaterialTheme.colorScheme.primary
//                        )
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.CheckCircle,
//                            contentDescription = null,
//                            modifier = Modifier.size(18.dp)
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text(text = arrivedButtonText)
//                    }
//                }
//            }
//        }
//    }
//}