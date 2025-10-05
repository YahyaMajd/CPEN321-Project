package com.cpen321.usermanagement.business

import com.cpen321.usermanagement.data.local.models.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Dynamic price calculator that uses pricing rules from backend
 */
class DynamicPriceCalculator(private val pricingRules: PricingRules) {
    
    fun calculateTotal(
        boxQuantities: List<BoxQuantity>,
        returnDate: String,
        pickupDate: String = getCurrentDate()
    ): PriceBreakdown {
        val days = calculateDaysBetween(pickupDate, returnDate)
        val boxDetails = calculateBoxDetails(boxQuantities)
        val boxTotal = boxDetails.sumOf { it.totalPrice }
        val dailyFee = pricingRules.pricePerDay * days
        val subtotal = boxTotal + dailyFee
        val total = subtotal + pricingRules.totalServiceFee
        
        return PriceBreakdown(
            boxTotal = boxTotal,
            boxDetails = boxDetails,
            dailyFee = dailyFee,
            days = days,
            serviceFee = pricingRules.totalServiceFee,
            distanceServiceFee = pricingRules.distanceServiceFee,
            processingFee = pricingRules.processingFee,
            baseFee = pricingRules.baseFee,
            subtotal = subtotal,
            total = total
        )
    }
    
    private fun calculateBoxDetails(boxQuantities: List<BoxQuantity>): List<BoxLineItem> {
        return boxQuantities.filter { it.quantity > 0 }.map { boxQuantity ->
            val unitPrice = pricingRules.boxPrices[boxQuantity.boxSize.type] ?: 0.0
            BoxLineItem(
                boxType = boxQuantity.boxSize.type,
                quantity = boxQuantity.quantity,
                unitPrice = unitPrice,
                totalPrice = unitPrice * boxQuantity.quantity
            )
        }
    }
    
    private fun calculateDaysBetween(startDate: String, endDate: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)
            
            if (start != null && end != null) {
                val diffInMillis = end.time - start.time
                val days = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS).toInt()
                maxOf(1, days) // Minimum 1 day
            } else {
                1 // Default to 1 day if parsing fails
            }
        } catch (e: Exception) {
            1 // Default to 1 day if calculation fails
        }
    }
    
    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }
}
