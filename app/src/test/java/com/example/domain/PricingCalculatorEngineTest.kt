package com.example.domain

import com.example.domain.calculator.PricingCalculatorEngine
import com.example.domain.calculator.PricingInput
import com.example.domain.model.PricingStrategy
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PricingCalculatorEngineTest {

    @Test
    fun testProfitMarginCalculation() {
        // Direct costs: materials 100, 10 hrs @ 50 = 500, outsourcing 100, delivery 50 = 750
        // Overhead: software 50, other 50 = 100
        // Subtotal = 850
        // Contingency 10% = 85
        // Total Estimated Cost = 935.00
        // Desired margin 20%: Selling price = 935 / (1 - 0.20) = 935 / 0.80 = 1168.75
        // Profit = 1168.75 - 935.00 = 233.75
        // Margin = 233.75 / 1168.75 * 100 = 20.0%
        val input = PricingInput(
            materialsCost = BigDecimal("100.00"),
            laborHours = BigDecimal("10.0"),
            hourlyRate = BigDecimal("50.00"),
            outsourcingCost = BigDecimal("100.00"),
            deliveryCost = BigDecimal("50.00"),
            softwareOverhead = BigDecimal("50.00"),
            otherOverhead = BigDecimal("50.00"),
            contingencyPercent = BigDecimal("10.0"),
            strategy = PricingStrategy.PROFIT_MARGIN,
            targetPercent = BigDecimal("20.0"),
            taxPercent = BigDecimal.ZERO
        )

        val result = PricingCalculatorEngine.calculate(input)

        assertEquals(BigDecimal("750.00"), result.totalDirectCosts)
        assertEquals(BigDecimal("100.00"), result.totalOverhead)
        assertEquals(BigDecimal("85.00"), result.contingencyAllowance)
        assertEquals(BigDecimal("935.00"), result.totalEstimatedCost)
        assertEquals(BigDecimal("1168.75"), result.suggestedSellingPricePreTax)
        assertEquals(BigDecimal("233.75"), result.expectedGrossProfit)
        assertEquals(BigDecimal("20.0"), result.actualGrossMarginPercent)
        assertEquals(BigDecimal("1168.75"), result.finalCustomerPrice)
    }

    @Test
    fun testMarkupCalculation() {
        // Total Estimated Cost = 1000.00
        // 25% Markup: Selling price = 1000 * 1.25 = 1250.00
        // Tax 10%: final price = 1250 * 1.10 = 1375.00
        val input = PricingInput(
            materialsCost = BigDecimal("1000.00"),
            laborHours = BigDecimal.ZERO,
            hourlyRate = BigDecimal.ZERO,
            outsourcingCost = BigDecimal.ZERO,
            deliveryCost = BigDecimal.ZERO,
            softwareOverhead = BigDecimal.ZERO,
            otherOverhead = BigDecimal.ZERO,
            contingencyPercent = BigDecimal.ZERO,
            strategy = PricingStrategy.MARKUP,
            targetPercent = BigDecimal("25.0"),
            taxPercent = BigDecimal("10.0")
        )

        val result = PricingCalculatorEngine.calculate(input)

        assertEquals(BigDecimal("1000.00"), result.totalEstimatedCost)
        assertEquals(BigDecimal("1250.00"), result.suggestedSellingPricePreTax)
        assertEquals(BigDecimal("250.00"), result.expectedGrossProfit)
        assertEquals(BigDecimal("25.0"), result.actualMarkupPercent)
        assertEquals(BigDecimal("125.00"), result.taxAmount)
        assertEquals(BigDecimal("1375.00"), result.finalCustomerPrice)
    }
}
