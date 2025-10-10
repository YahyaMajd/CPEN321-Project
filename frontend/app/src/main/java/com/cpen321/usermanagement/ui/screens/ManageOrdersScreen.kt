package com.cpen321.usermanagement.ui.screens

import Icon
import androidx.compose.runtime.Composable
import com.cpen321.usermanagement.ui.viewmodels.OrderViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Order
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.cpen321.usermanagement.utils.TimeUtils
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.viewmodels.ProfileUiState
import com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.delay
import com.cpen321.usermanagement.di.SocketClientEntryPoint

private data class ManageOrdersScreenData(
    val orders: List<Order>,
    val uiState: ProfileUiState,
    val snackBarHostState: SnackbarHostState,
    val onSuccessMessageShown: () -> Unit,
    val onErrorMessageShown: () -> Unit
)

private data class ManageOrdersScreenActions(
    val onBackClick: () -> Unit,
    val onManageOrderClick: (Order) -> Unit,
    val onSaveClick: () -> Unit
)
@Composable
fun ManageOrdersScreen(
    orderViewModel: OrderViewModel,
    profileViewModel: ProfileViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
     // Local state to hold orders
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    val appCtx = LocalContext.current.applicationContext
    
    // Trigger to force refresh after order operations
    var refreshTrigger by remember { mutableStateOf(0) }

    //orderUI state collection
    val orderUi by orderViewModel.uiState.collectAsState()

    // Subscribe to socket events and refresh orders on open and when refreshTrigger changes
    // Observe orders from ViewModel
    val ordersState by orderViewModel.orders.collectAsState()

    LaunchedEffect(refreshTrigger) {
        // initial load on open (or reload when refreshTrigger increments)
        orderViewModel.refreshAllOrders()

        // obtain SocketClient via Hilt EntryPoint
        val entry = EntryPointAccessors.fromApplication(appCtx, SocketClientEntryPoint::class.java)
        val socketClient = entry.socketClient()

        socketClient.events.collect { ev ->
            if (ev.name == "order.created" || ev.name == "order.updated") {
                orderViewModel.refreshAllOrders()
            }
        }
    }

    ManageOrdersContent(
        data = ManageOrdersScreenData(
            orders = ordersState,
            uiState = uiState,
            snackBarHostState = snackBarHostState,
            onSuccessMessageShown = profileViewModel::clearSuccessMessage,
            onErrorMessageShown = profileViewModel::clearError
        ),
        actions = ManageOrdersScreenActions(
            onBackClick = onBackClick,
            onManageOrderClick = {order -> orderViewModel.onManageOrder(order) },
            onSaveClick = profileViewModel::saveHobbies
        )
    )

    // Simple toggleable panel (bottom sheet or inline card)
    if (orderUi.isManaging && orderUi.selectedOrder != null) {
        ManageOrderSheet(
            order = orderUi.selectedOrder!!,
            orderViewModel,
            onClose = { orderViewModel.stopManaging() },
            onOrderCancelled = {
                // Trigger refresh by incrementing the trigger
                refreshTrigger++
            }
        )
    }
}

@Composable
private fun ManageOrdersContent(
    data: ManageOrdersScreenData,
    actions: ManageOrdersScreenActions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ManageOrdersTopBar(onBackClick = actions.onBackClick)
        },
        snackbarHost = {
            MessageSnackbar(
                hostState = data.snackBarHostState,
                messageState = MessageSnackbarState(
                    successMessage = data.uiState.successMessage,
                    errorMessage = data.uiState.errorMessage,
                    onSuccessMessageShown = data.onSuccessMessageShown,
                    onErrorMessageShown = data.onErrorMessageShown
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            ManageOrdersBody(
                data.orders,
                actions.onManageOrderClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageOrdersTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.manage_orders),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(name = R.drawable.ic_arrow_back)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}


@Composable 
fun ManageOrdersBody(
    orders: List<Order>,
    onManageOrdersClick : (Order)-> Unit
){

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Order History",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (orders.isEmpty()) {
            Text("No orders found.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(orders) { order ->
                    OrderListItem(
                        order,
                        onManageOrdersClick as (Order) -> Unit
                    )
                }
            }
        }
    }

}

@Composable
fun OrderListItem(
    order: Order,
    onManageOrderClick: (Order) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Order ID: ${order.id ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${order.status}", style = MaterialTheme.typography.bodySmall)
            Text("Price: $${order.price}", style = MaterialTheme.typography.bodySmall)
            Text("Pickup: ${TimeUtils.formatPickupTime(order.pickupTime)}", style = MaterialTheme.typography.bodySmall)
            Text("Return: ${TimeUtils.formatPickupTime(order.returnTime)}", style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = { onManageOrderClick(order) },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.End)
            ) {
                Text("Manage Order")
            }
        }
    }
}

@Composable
fun ManageOrderSheet(
    order: Order,
    orderViewModel: OrderViewModel,
    onClose: () -> Unit,
    onOrderCancelled: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Manage Order") },
        text = {
            Column {
                Text("Order ID: ${order.id ?: "N/A"}")
                Text("Status: ${order.status}")
                Text("Pickup: ${TimeUtils.formatPickupTime(order.pickupTime)}")
                Text("Return: ${TimeUtils.formatPickupTime(order.returnTime)}")
                Button(
                    onClick = {
                        orderViewModel.cancelOrder() { err ->
                            // Optionally show a snackbar/toast
                            if (err == null) {
                                // Close sheet and trigger refresh on success
                                onOrderCancelled()
                                onClose()
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.End)
                ) {
                    Text("Cancel Order")
                }
                // add actions: cancel, contact mover, track, etc.
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        }
    )
}
