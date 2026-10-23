package com.example.domain.calculator

import com.example.domain.model.PricingStrategy
import java.math.BigDecimal
import java.math.RoundingMode

data class PricingInput(
    val materialsCost: BigDecimal = BigDecimal.ZERO,
    val laborHours: BigDecimal = BigDecimal.ZERO,
    val hourlyRate: BigDecimal = BigDecimal.ZERO,
    val outsourcingCost: BigDecimal = BigDecimal.ZERO,
    val deliveryCost: BigDecimal = BigDecimal.ZERO,
    val softwareOverhead: BigDecimal = BigDecimal.ZERO,
    val otherOverhead: BigDecimal = BigDecimal.ZERO,
    val contingencyPercent: BigDecimal = BigDecimal.ZERO,
    val strategy: PricingStrategy = PricingStrategy.PROFIT_MARGIN,
    val targetPercent: BigDecimal = BigDecimal("25.0"), // margin or markup %
    val taxPercent: BigDecimal = BigDecimal.ZERO
)

data class PricingResult(
    val directLaborCost: BigDecimal,
    val totalDirectCosts: BigDecimal,
    val totalOverhead: BigDecimal,
    val baseSubtotal: BigDecimal,
    val contingencyAllowance: BigDecimal,
    val totalEstimatedCost: BigDecimal,
    val suggestedSellingPricePreTax: BigDecimal,
    val expectedGrossProfit: BigDecimal,
    val actualGrossMarginPercent: BigDecimal,
    val actualMarkupPercent: BigDecimal,
    val taxAmount: BigDecimal,
    val finalCustomerPrice: BigDecimal,
    val explanation: String
)

object PricingCalculatorEngine {
    private val HUNDRED = BigDecimal("100")
    private val SCALE = 2
    private val CALC_SCALE = 6
    private val ROUNDING_MODE = RoundingMode.HALF_UP

    fun calculate(input: PricingInput): PricingResult {
        val safeMaterials = input.materialsCost.coerceAtLeast(BigDecimal.ZERO)
        val safeHours = input.laborHours.coerceAtLeast(BigDecimal.ZERO)
        val safeRate = input.hourlyRate.coerceAtLeast(BigDecimal.ZERO)
        val safeOutsourcing = input.outsourcingCost.coerceAtLeast(BigDecimal.ZERO)
        val safeDelivery = input.deliveryCost.coerceAtLeast(BigDecimal.ZERO)
        val safeSoftware = input.softwareOverhead.coerceAtLeast(BigDecimal.ZERO)
        val safeOtherOverhead = input.otherOverhead.coerceAtLeast(BigDecimal.ZERO)
        val safeContingency = input.contingencyPercent.coerceAtLeast(BigDecimal.ZERO)
        val safeTax = input.taxPercent.coerceAtLeast(BigDecimal.ZERO)

        val directLabor = safeHours.multiply(safeRate).setScale(SCALE, ROUNDING_MODE)
        val totalDirect = safeMaterials.add(directLabor).add(safeOutsourcing).add(safeDelivery)
            .setScale(SCALE, ROUNDING_MODE)

        val totalOverhead = safeSoftware.add(safeOtherOverhead)
            .setScale(SCALE, ROUNDING_MODE)

        val baseSubtotal = totalDirect.add(totalOverhead).setScale(SCALE, ROUNDING_MODE)

        val contingencyAllowance = baseSubtotal.multiply(safeContingency)
            .divide(HUNDRED, SCALE, ROUNDING_MODE)

        val totalEstimatedCost = baseSubtotal.add(contingencyAllowance).setScale(SCALE, ROUNDING_MODE)

        val sellingPricePreTax: BigDecimal
        val targetPercent = input.targetPercent.coerceAtLeast(BigDecimal.ZERO)

        if (totalEstimatedCost.compareTo(BigDecimal.ZERO) == 0) {
            sellingPricePreTax = BigDecimal.ZERO
        } else if (input.strategy == PricingStrategy.PROFIT_MARGIN) {
            // Margin formula: Cost / (1 - Margin%)
            // Only valid when 0 <= Margin% < 100
            val clampedMargin = targetPercent.coerceAtMost(BigDecimal("99.0"))
            val marginFraction = clampedMargin.divide(HUNDRED, CALC_SCALE, ROUNDING_MODE)
            val divisor = BigDecimal.ONE.subtract(marginFraction)
            sellingPricePreTax = if (divisor > BigDecimal.ZERO) {
                totalEstimatedCost.divide(divisor, SCALE, ROUNDING_MODE)
            } else {
                totalEstimatedCost
            }
        } else {
            // Markup formula: Cost * (1 + Markup%)
            val markupFraction = targetPercent.divide(HUNDRED, CALC_SCALE, ROUNDING_MODE)
            val multiplier = BigDecimal.ONE.add(markupFraction)
            sellingPricePreTax = totalEstimatedCost.multiply(multiplier).setScale(SCALE, ROUNDING_MODE)
        }

        val expectedGrossProfit = sellingPricePreTax.subtract(totalEstimatedCost)
            .coerceAtLeast(BigDecimal.ZERO)
            .setScale(SCALE, ROUNDING_MODE)

        val actualGrossMargin: BigDecimal = if (sellingPricePreTax > BigDecimal.ZERO) {
            expectedGrossProfit.multiply(HUNDRED).divide(sellingPricePreTax, 1, ROUNDING_MODE)
        } else {
            BigDecimal.ZERO
        }

        val actualMarkup: BigDecimal = if (totalEstimatedCost > BigDecimal.ZERO) {
            expectedGrossProfit.multiply(HUNDRED).divide(totalEstimatedCost, 1, ROUNDING_MODE)
        } else {
            BigDecimal.ZERO
        }

        val taxAmount = sellingPricePreTax.multiply(safeTax)
            .divide(HUNDRED, SCALE, ROUNDING_MODE)

        val finalPrice = sellingPricePreTax.add(taxAmount).setScale(SCALE, ROUNDING_MODE)

        val explanation = buildString {
            append("Total internal cost is ${totalEstimatedCost}. ")
            if (input.strategy == PricingStrategy.PROFIT_MARGIN) {
                append("Using ${targetPercent}% profit margin on selling price, your pre-tax quote should be ${sellingPricePreTax}. ")
                append("This delivers ${expectedGrossProfit} gross profit (${actualGrossMargin}% of the selling price).")
            } else {
                append("Using ${targetPercent}% markup on costs, your pre-tax quote is ${sellingPricePreTax}. ")
                append("This delivers ${expectedGrossProfit} gross profit (${actualMarkup}% markup over costs).")
            }
            if (safeTax > BigDecimal.ZERO) {
                append(" Adding ${safeTax}% tax (${taxAmount}) results in ${finalPrice} final customer price.")
            }
        }

        return PricingResult(
            directLaborCost = directLabor,
            totalDirectCosts = totalDirect,
            totalOverhead = totalOverhead,
            baseSubtotal = baseSubtotal,
            contingencyAllowance = contingencyAllowance,
            totalEstimatedCost = totalEstimatedCost,
            suggestedSellingPricePreTax = sellingPricePreTax,
            expectedGrossProfit = expectedGrossProfit,
            actualGrossMarginPercent = actualGrossMargin,
            actualMarkupPercent = actualMarkup,
            taxAmount = taxAmount,
            finalCustomerPrice = finalPrice,
            explanation = explanation
        )
    }
}
