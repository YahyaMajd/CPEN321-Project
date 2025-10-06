package com.cpen321.usermanagement.data.local.models

/**
 * API Models for pricing endpoints
 */
data class Address(
    val lat: Double,
    val lon: Double,
    val formattedAddress: String
)

data class GetQuoteRequest(
    val studentAddress: Address,
    val studentId: String
)

data class GetQuoteResponse(
    val distancePrice: Double,
    val warehouseAddress: Address
)

/**
 * Local pricing rules derived from API response
 */
data class PricingRules(
    val pricePerDay: Double = 2.50,        // $2.50 per day
    val baseFee: Double = 4.99,            // Base service fee
    val distanceServiceFee: Double,        // From API distancePrice
    val processingFee: Double = 1.99,      // Payment processing
    val boxPrices: Map<String, Double> = mapOf(
        "Small" to 5.00,   // $5 per small box
        "Medium" to 8.00,  // $8 per medium box  
        "Large" to 12.00   // $12 per large box
    )
) {
    val totalServiceFee: Double
        get() = baseFee + distanceServiceFee + processingFee
}

/**
 * Enhanced price breakdown with daily fees
 */
data class PriceBreakdown(
    val boxTotal: Double,
    val boxDetails: List<BoxLineItem>,
    val dailyFee: Double,
    val days: Int,
    val serviceFee: Double,
    val distanceServiceFee: Double,
    val processingFee: Double,
    val baseFee: Double,
    val subtotal: Double,
    val total: Double
)

data class BoxLineItem(
    val boxType: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

/**
 * Order creation steps
 */
enum class OrderCreationStep {
    ADDRESS_CAPTURE,
    LOADING_QUOTE,
    BOX_SELECTION,
    PAYMENT_DETAILS,
    PROCESSING_PAYMENT,
    ORDER_CONFIRMATION
}