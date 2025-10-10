package com.cpen321.usermanagement.ui.screens

import Icon
import androidx.compose.runtime.Composable
import com.cpen321.usermanagement.ui.viewmodels.OrderViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cpen321.usermanagement.data.local.models.Order
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.components.MessageSnackbar
import com.cpen321.usermanagement.ui.components.MessageSnackbarState
import com.cpen321.usermanagement.ui.viewmodels.ProfileUiState
import com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.delay

private data class ManageOrdersScreenData(
    val orders: List<Order>,
    val uiState: ProfileUiState,
    val snackBarHostState: SnackbarHostState,
    val onSuccessMessageShown: () -> Unit,
    val onErrorMessageShown: () -> Unit
)

private data class ManageOrdersScreenActions(
    val onBackClick: () -> Unit,
    val onHobbyToggle: (String) -> Unit,
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

    // Load all orders on launch and refresh periodically
    LaunchedEffect(Unit) {
        while(true) {
            orderViewModel.getAllOrders()?.let { fetchedOrders ->
                orders = fetchedOrders
            }
            delay(5000) // Refresh every 5 seconds
        }
    }

    ManageOrdersContent(
        data = ManageOrdersScreenData(
            orders = orders,
            uiState = uiState,
            snackBarHostState = snackBarHostState,
            onSuccessMessageShown = profileViewModel::clearSuccessMessage,
            onErrorMessageShown = profileViewModel::clearError
        ),
        actions = ManageOrdersScreenActions(
            onBackClick = onBackClick,
            onHobbyToggle = profileViewModel::toggleHobby,
            onSaveClick = profileViewModel::saveHobbies
        )
    )
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
            ManageOrdersBody(data.orders)
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
    orders: List<Order>
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
                    OrderListItem(order)
                }
            }
        }
    }

}

@Composable
fun OrderListItem(order: Order) {
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
            Text("Pickup: ${order.pickupTime}", style = MaterialTheme.typography.bodySmall)
            Text("Return: ${order.returnTime}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
