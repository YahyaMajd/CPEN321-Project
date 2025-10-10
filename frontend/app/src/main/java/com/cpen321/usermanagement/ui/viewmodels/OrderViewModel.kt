package com.cpen321.usermanagement.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.local.models.Order
import com.cpen321.usermanagement.data.local.models.OrderRequest
import com.cpen321.usermanagement.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OrderViewModel
 * - Single source of truth for the current active order and order-related actions
 * - Wraps OrderRepository and exposes StateFlows for the UI to observe
 */
data class OrderUiState(
    val isManaging: Boolean = false,
    val selectedOrder: Order? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)


@HiltViewModel
class OrderViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {

    // UI state

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()
    private val _isSubmitting = MutableStateFlow(false)

    suspend fun submitOrder(orderRequest: OrderRequest): Result<Order> {
        _isSubmitting.value = true
        return try {
            val result = repository.submitOrder(orderRequest)
            result
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSubmitting.value = false
        }
    }

    suspend fun getQuote(address: com.cpen321.usermanagement.data.local.models.Address): Result<com.cpen321.usermanagement.data.local.models.GetQuoteResponse> {
        return try {
            val result = repository.getQuote(address)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllOrders() : List<Order>? {
        return repository.getAllOrders()
    }

    suspend fun getActiveOrder() : Order? {
        return repository.getActiveOrder()
    }

    fun startManaging(order: Order) {
        _uiState.value = _uiState.value.copy(
            isManaging = true,
            selectedOrder = order
        )
    }

    fun stopManaging() {
        _uiState.value = _uiState.value.copy(
            isManaging = false,
            selectedOrder = null
        )
    }


    fun onManageOrder(order: Order) {
        // route to the toggle so UI reacts
        startManaging(order)
    }

    fun cancelOrder(onDone: (Throwable?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.cancelOrder()   // suspend
                // Update UI state if needed
                onDone(null)
            } catch (t: Throwable) {
                onDone(t)
            }
        }
    }





    //    fun simulateProgress(orderId: String) {
    //        viewModelScope.launch {
    //            repository.simulateOrderProgress(orderId)
    //        }
    //    }

    //    fun updateOrderStatus(orderId: String, newStatus: com.cpen321.usermanagement.data.local.models.OrderStatus) {
    //        viewModelScope.launch {
    //            repository.updateOrderStatus(orderId, newStatus)
    //        }
    //    }
}

