package com.cpen321.usermanagement.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.BuildConfig
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.LocationBias
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AddressSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

data class SelectedAddress(
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double
)

@Composable
fun AddressAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    onAddressSelected: (SelectedAddress) -> Unit,
    label: String = "Address",
    placeholder: String = "e.g. 123 Main St",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var suggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Initialize Places API
    val placesClient = remember {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
        Places.createClient(context)
    }

    // Debounced search
    LaunchedEffect(value) {
        if (value.length >= 3) {
            isLoading = true
            delay(500) // Debounce delay

            coroutineScope.launch {
                try {
                    val predictions = fetchAddressPredictions(
                        placesClient = placesClient,
                        query = value,
                        context = context
                    )
                    suggestions = predictions
                    showSuggestions = predictions.isNotEmpty()
                } catch (e: Exception) {
                    e.printStackTrace()
                    suggestions = emptyList()
                    showSuggestions = false
                } finally {
                    isLoading = false
                }
            }
        } else {
            suggestions = emptyList()
            showSuggestions = false
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        )

        if (showSuggestions && suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 250.dp)
                ) {
                    items(suggestions) { suggestion ->
                        AddressSuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val address = fetchPlaceDetails(
                                            placesClient = placesClient,
                                            placeId = suggestion.placeId
                                        )
                                        if (address != null) {
                                            onValueChange(address.formattedAddress)
                                            onAddressSelected(address)
                                            showSuggestions = false
                                            suggestions = emptyList()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                        if (suggestion != suggestions.last()) {
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressSuggestionItem(
    suggestion: AddressSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column {
            Text(
                text = suggestion.primaryText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (suggestion.secondaryText.isNotEmpty()) {
                Text(
                    text = suggestion.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun fetchAddressPredictions(
    placesClient: PlacesClient,
    query: String,
    context: Context
): List<AddressSuggestion> {
    try {
        val token = AutocompleteSessionToken.newInstance()

        // Bias results to Greater Vancouver area
        val bounds = RectangularBounds.newInstance(
            com.google.android.gms.maps.model.LatLng(49.0, -123.3), // Southwest
            com.google.android.gms.maps.model.LatLng(49.4, -122.5)  // Northeast
        )

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(token)
            .setQuery(query)
            .setLocationBias(bounds)
            .setCountries("CA")
            .build()

        val response = placesClient.findAutocompletePredictions(request).await()

        return response.autocompletePredictions.map { prediction ->
            AddressSuggestion(
                placeId = prediction.placeId,
                primaryText = prediction.getPrimaryText(null).toString(),
                secondaryText = prediction.getSecondaryText(null).toString(),
                fullText = prediction.getFullText(null).toString()
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return emptyList()
    }
}

private suspend fun fetchPlaceDetails(
    placesClient: PlacesClient,
    placeId: String
): SelectedAddress? {
    try {
        val placeFields = listOf(
            com.google.android.libraries.places.api.model.Place.Field.ID,
            com.google.android.libraries.places.api.model.Place.Field.NAME,
            com.google.android.libraries.places.api.model.Place.Field.ADDRESS,
            com.google.android.libraries.places.api.model.Place.Field.LAT_LNG
        )

        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        val response = placesClient.fetchPlace(request).await()

        val place = response.place
        val latLng = place.latLng
        val address = place.address

        if (latLng != null && address != null) {
            return SelectedAddress(
                formattedAddress = address,
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

