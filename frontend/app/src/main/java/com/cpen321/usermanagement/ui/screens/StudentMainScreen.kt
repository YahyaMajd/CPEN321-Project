package com.cpen321.usermanagement.ui.screens

import Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.viewmodels.MainUiState
import com.cpen321.usermanagement.ui.viewmodels.MainViewModel
import com.cpen321.usermanagement.ui.viewmodels.AuthViewModel
import com.cpen321.usermanagement.ui.theme.LocalFontSizes
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import com.cpen321.usermanagement.ui.components.OrderPanel
import com.cpen321.usermanagement.ui.components.StatusPanel
import com.cpen321.usermanagement.ui.components.CreateOrderBottomSheet
import com.cpen321.usermanagement.data.local.models.OrderRequest
import com.cpen321.usermanagement.data.local.models.Order
import com.cpen321.usermanagement.ui.viewmodels.OrderViewModel
import com.cpen321.usermanagement.data.repository.PaymentRepository
import com.cpen321.usermanagement.data.remote.api.RetrofitClient
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.collection.orderedScatterSetOf
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import com.cpen321.usermanagement.di.SocketClientEntryPoint
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun StudentMainScreen(
    mainViewModel: MainViewModel,
    orderViewModel: OrderViewModel,
    onProfileClick: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()
    // Observe active order from ViewModel
    val activeOrder by orderViewModel.activeOrder.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val appCtx = LocalContext.current.applicationContext

    // Subscribe to socket order.created/order.updated events and refresh active order

    LaunchedEffect(Unit) {
        // initial load
        orderViewModel.refreshActiveOrder()
        // obtain singleton SocketClient from Hilt via a top-level entry point
        val entry = EntryPointAccessors.fromApplication(appCtx, SocketClientEntryPoint::class.java)
        val socketClient = entry.socketClient()

        // collect socket events and refresh active order when an order is created/updated
        // also show notifications for job status updates
        socketClient.events.collect { ev ->
            when (ev.name) {
                "order.created", "order.updated" -> {
                    orderViewModel.refreshActiveOrder()
                }
                "job.updated" -> {
                    val jobData = ev.payload?.optJSONObject("job")
                    val status = jobData?.optString("status")
                    val jobType = jobData?.optString("jobType")
                    
                    val message = when (status) {
                        "PICKED_UP" -> {
                            when (jobType) {
                                "STORAGE" -> "Mover has arrived at your location and picked up your items!"
                                "RETURN" -> "Mover has arrived at the storage facility and picked up your items!"
                                else -> "Mover has arrived at the pickup location!"
                            }
                        }
                        "COMPLETED" -> {
                            when (jobType) {
                                "STORAGE" -> "Your items have been delivered to the storage facility!"
                                "RETURN" -> "Your items have been returned to you!"
                                else -> "Job completed successfully!"
                            }
                        }
                        else -> null
                    }
                    
                    message?.let {
                        launch {
                            snackBarHostState.showSnackbar(
                                message = it,
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                }
            }
        }
    }

    MainContent(
        uiState = uiState,
        activeOrder = activeOrder,
        orderViewModel = orderViewModel,
        snackBarHostState = snackBarHostState,
        onProfileClick = onProfileClick,
        onSuccessMessageShown = mainViewModel::clearSuccessMessage
    )
}

// use shared SocketClientEntryPoint in com.cpen321.usermanagement.di

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    uiState: MainUiState,
    activeOrder: Order?,
    orderViewModel: OrderViewModel,
    snackBarHostState: SnackbarHostState,
    onProfileClick: () -> Unit,
    onSuccessMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateOrderSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        modifier = modifier,
        topBar = {
            MainTopBar(onProfileClick = onProfileClick)
        },
        snackbarHost = {
            MainSnackbarHost(
                hostState = snackBarHostState,
                successMessage = uiState.successMessage,
                onSuccessMessageShown = onSuccessMessageShown
            )
        }
    ) { paddingValues ->
        MainBody(
            paddingValues = paddingValues,
            activeOrder = activeOrder,
            onCreateOrderClick = { showCreateOrderSheet = true }
        )
    }
    
    // Create Order Bottom Sheet
    if (showCreateOrderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCreateOrderSheet = false },
            sheetState = bottomSheetState
        ) {
            CreateOrderBottomSheet(
                onDismiss = { showCreateOrderSheet = false },
                orderViewModel = orderViewModel,
                paymentRepository = PaymentRepository(RetrofitClient.paymentInterface),
                onSubmitOrder = { orderRequest ->
                    // Handle order submission with repository
                    coroutineScope.launch {
                        val result = orderViewModel.submitOrder(orderRequest)
                        result.onSuccess { order ->
                            println("Order submitted successfully: $order")
                            // Order is now set in repository._activeOrder, StatusPanel will show it
                        }.onFailure { exception ->
                            println("Order submission failed: $exception")
                        }
                        // Close sheet after async operation completes
                        showCreateOrderSheet = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            AppTitle()
        },
        actions = {
            ProfileActionButton(onClick = onProfileClick)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun AppTitle(
    modifier: Modifier = Modifier
) {
    Row{
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            modifier = modifier
        )
        Text(
            text = " (Student)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Light,
            modifier = modifier
        )
    }
    
}

@Composable
private fun ProfileActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    IconButton(
        onClick = onClick,
        modifier = modifier.size(spacing.extraLarge2)
    ) {
        ProfileIcon()
    }
}

@Composable
private fun ProfileIcon() {
    Icon(
        name = R.drawable.ic_account_circle,
    )
}

@Composable
private fun MainSnackbarHost(
    hostState: SnackbarHostState,
    successMessage: String?,
    onSuccessMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    MessageSnackbar(
        hostState = hostState,
        messageState = MessageSnackbarState(
            successMessage = successMessage,
            errorMessage = null,
            onSuccessMessageShown = onSuccessMessageShown,
            onErrorMessageShown = { }
        ),
        modifier = modifier
    )
}

@Composable
private fun MainBody(
    paddingValues: PaddingValues,
    activeOrder: com.cpen321.usermanagement.data.local.models.Order?,
    onCreateOrderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order Panel (Main centerpiece)
        OrderPanel(
            hasActiveOrder = activeOrder != null, // Real state!
            activeOrder = activeOrder, // Pass the actual order data
            onCreateOrderClick = onCreateOrderClick
        )
        
        // Status Panel (Hidden when no active order)
        StatusPanel(
            activeOrder = activeOrder // Pass the actual order data!
        )
    }
}

@Composable
private fun WelcomeMessage(
    modifier: Modifier = Modifier
) {
    val fontSizes = LocalFontSizes.current

    Text(
        text = stringResource(R.string.welcome),
        style = MaterialTheme.typography.bodyLarge,
        fontSize = fontSizes.extraLarge3,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}