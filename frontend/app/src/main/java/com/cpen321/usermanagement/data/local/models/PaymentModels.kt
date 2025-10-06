package com.cpen321.usermanagement.data.local.models

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentIntentRequest(
    val amount: Double,
    val currency: String = "CAD"
)

@Serializable
data class CreatePaymentIntentResponse(
    val id: String,  // Backend returns "id", not "paymentIntentId"
    val clientSecret: String,
    val amount: Double,
    val currency: String,
    val status: String? = null
)

@Serializable
data class ProcessPaymentRequest(
    val paymentIntentId: String,
    val paymentMethodId: String = "pm_card_visa", // Test payment method
    val customerInfo: CustomerInfo
)

@Serializable
data class CustomerInfo(
    val name: String,
    val email: String,
    val address: PaymentAddress
)

@Serializable
data class PaymentAddress(
    val line1: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String = "CA"
)

@Serializable
data class ProcessPaymentResponse(
    val paymentId: String,
    val status: String,
    val amount: Double,
    val currency: String
)

@Serializable
data class PaymentStatusResponse(
    val paymentId: String,
    val status: String,
    val amount: Double,
    val currency: String
)

enum class PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELED
}

data class PaymentDetails(
    val cardNumber: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cvc: String = "",
    val cardholderName: String = "",
    val email: String = "",
    val isValid: Boolean = false
)

// Test payment methods for demo
object TestPaymentMethods {
    const val VISA_SUCCESS = "pm_card_visa"
    const val VISA_DECLINED = "pm_card_visa_chargeDeclined"
    const val MASTERCARD_SUCCESS = "pm_card_mastercard"
    
    val TEST_CARDS = listOf(
        TestCard("4242424242424242", "Visa - Success", VISA_SUCCESS),
        TestCard("4000000000000002", "Visa - Declined", VISA_DECLINED),
        TestCard("5555555555554444", "Mastercard - Success", MASTERCARD_SUCCESS)
    )
}

data class TestCard(
    val number: String,
    val description: String,
    val paymentMethodId: String
)