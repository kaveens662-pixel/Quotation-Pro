package com.example.domain.calculator

import com.example.domain.model.QuotationItem
import java.math.BigDecimal
import java.math.RoundingMode

object FinancialCalculator {
    private val HUNDRED = BigDecimal("100")
    val SCALE = 2
    val ROUNDING_MODE = RoundingMode.HALF_UP

    fun calculateLineItem(
        quantity: BigDecimal,
        unitPrice: BigDecimal,
        discountPercent: BigDecimal = BigDecimal.ZERO,
        discountFixed: BigDecimal = BigDecimal.ZERO,
        taxPercent: BigDecimal = BigDecimal.ZERO
    ): LineItemCalculation {
        val safeQty = if (quantity < BigDecimal.ZERO) BigDecimal.ZERO else quantity
        val safePrice = if (unitPrice < BigDecimal.ZERO) BigDecimal.ZERO else unitPrice
        val safeDiscPercent = if (discountPercent < BigDecimal.ZERO) BigDecimal.ZERO else discountPercent.coerceAtMost(HUNDRED)
        val safeDiscFixed = if (discountFixed < BigDecimal.ZERO) BigDecimal.ZERO else discountFixed
        val safeTaxPercent = if (taxPercent < BigDecimal.ZERO) BigDecimal.ZERO else taxPercent

        val grossAmount = safeQty.multiply(safePrice).setScale(SCALE, ROUNDING_MODE)

        val percentDiscountAmount = grossAmount.multiply(safeDiscPercent)
            .divide(HUNDRED, SCALE, ROUNDING_MODE)

        val totalDiscountAmount = percentDiscountAmount.add(safeDiscFixed)
            .coerceAtMost(grossAmount)
            .setScale(SCALE, ROUNDING_MODE)

        val taxableAmount = grossAmount.subtract(totalDiscountAmount)
            .coerceAtLeast(BigDecimal.ZERO)
            .setScale(SCALE, ROUNDING_MODE)

        val taxAmount = taxableAmount.multiply(safeTaxPercent)
            .divide(HUNDRED, SCALE, ROUNDING_MODE)

        val lineTotal = taxableAmount.add(taxAmount).setScale(SCALE, ROUNDING_MODE)

        return LineItemCalculation(
            grossAmount = grossAmount,
            discountAmount = totalDiscountAmount,
            taxableAmount = taxableAmount,
            taxAmount = taxAmount,
            lineTotal = lineTotal
        )
    }

    fun calculateQuotationTotals(
        items: List<QuotationItem>,
        overallDiscount: BigDecimal = BigDecimal.ZERO,
        additionalFee: BigDecimal = BigDecimal.ZERO
    ): QuotationCalculation {
        var subtotal = BigDecimal.ZERO
        var totalLineDiscounts = BigDecimal.ZERO
        var totalTaxable = BigDecimal.ZERO
        var totalTax = BigDecimal.ZERO

        for (item in items) {
            val calc = calculateLineItem(
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                discountPercent = item.discountPercent,
                discountFixed = item.discountFixed,
                taxPercent = item.taxPercent
            )
            subtotal = subtotal.add(calc.grossAmount)
            totalLineDiscounts = totalLineDiscounts.add(calc.discountAmount)
            totalTaxable = totalTaxable.add(calc.taxableAmount)
            totalTax = totalTax.add(calc.taxAmount)
        }

        subtotal = subtotal.setScale(SCALE, ROUNDING_MODE)
        totalLineDiscounts = totalLineDiscounts.setScale(SCALE, ROUNDING_MODE)
        totalTaxable = totalTaxable.setScale(SCALE, ROUNDING_MODE)
        totalTax = totalTax.setScale(SCALE, ROUNDING_MODE)

        val safeOverallDiscount = if (overallDiscount < BigDecimal.ZERO) BigDecimal.ZERO 
            else overallDiscount.coerceAtMost(totalTaxable).setScale(SCALE, ROUNDING_MODE)

        val safeAdditionalFee = if (additionalFee < BigDecimal.ZERO) BigDecimal.ZERO 
            else additionalFee.setScale(SCALE, ROUNDING_MODE)

        val adjustedTaxable = totalTaxable.subtract(safeOverallDiscount)
            .coerceAtLeast(BigDecimal.ZERO)
            .setScale(SCALE, ROUNDING_MODE)

        val grandTotal = adjustedTaxable.add(totalTax).add(safeAdditionalFee)
            .setScale(SCALE, ROUNDING_MODE)

        return QuotationCalculation(
            subtotal = subtotal,
            totalLineDiscounts = totalLineDiscounts,
            overallDiscount = safeOverallDiscount,
            taxableAmount = adjustedTaxable,
            totalTax = totalTax,
            additionalFee = safeAdditionalFee,
            grandTotal = grandTotal
        )
    }
}

data class LineItemCalculation(
    val grossAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val lineTotal: BigDecimal
)

data class QuotationCalculation(
    val subtotal: BigDecimal,
    val totalLineDiscounts: BigDecimal,
    val overallDiscount: BigDecimal,
    val taxableAmount: BigDecimal,
    val totalTax: BigDecimal,
    val additionalFee: BigDecimal,
    val grandTotal: BigDecimal
)
