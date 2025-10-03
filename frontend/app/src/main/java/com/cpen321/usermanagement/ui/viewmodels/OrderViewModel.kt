package com.cpen321.usermanagement.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cpen321.usermanagement.data.local.models.Order
import com.cpen321.usermanagement.data.local.models.OrderRequest
import com.cpen321.usermanagement.data.repository.OrderRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * OrderViewModel
 * - Single source of truth for the current active order and order-related actions
 * - Wraps OrderRepository and exposes StateFlows for the UI to observe
 */
class OrderViewModel(
    private val repository: OrderRepository = OrderRepository()
) : ViewModel() {

    // Expose repository flows directly
    val activeOrder: StateFlow<Order?> = repository.activeOrder
    val orderHistory = repository.orderHistory

    // Local UI state for submission in progress
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    fun submitOrder(orderRequest: OrderRequest): Result<Order> {
        var result: Result<Order>
        result = Result.failure(Exception("Not initialized"))
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                result = repository.submitOrder(orderRequest)
            } finally {
                
                _isSubmitting.value = false
            }
        }
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }
        return Result.success(result.getOrNull()!!)
    }

    fun clearActiveOrder() {
        viewModelScope.launch {
            repository.clearActiveOrder()
        }
    }

    fun simulateProgress(orderId: String) {
        viewModelScope.launch {
            repository.simulateOrderProgress(orderId)
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: com.cpen321.usermanagement.data.local.models.OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
        }
    }

}
