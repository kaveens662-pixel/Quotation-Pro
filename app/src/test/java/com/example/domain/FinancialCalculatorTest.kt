package com.example.domain

import com.example.domain.calculator.FinancialCalculator
import com.example.domain.model.QuotationItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class FinancialCalculatorTest {

    @Test
    fun testLineItemCalculation_standard() {
        // 5 units @ $20.00 = $100.00 gross
        // 10% discount = $10.00 discount -> $90.00 taxable
        // 15% tax on $90.00 = $13.50 tax
        // line total = $103.50
        val calc = FinancialCalculator.calculateLineItem(
            quantity = BigDecimal("5"),
            unitPrice = BigDecimal("20.00"),
            discountPercent = BigDecimal("10.0"),
            discountFixed = BigDecimal.ZERO,
            taxPercent = BigDecimal("15.0")
        )

        assertEquals(BigDecimal("100.00"), calc.grossAmount)
        assertEquals(BigDecimal("10.00"), calc.discountAmount)
        assertEquals(BigDecimal("90.00"), calc.taxableAmount)
        assertEquals(BigDecimal("13.50"), calc.taxAmount)
        assertEquals(BigDecimal("103.50"), calc.lineTotal)
    }

    @Test
    fun testLineItemCalculation_fixedAndPercentDiscount() {
        // 2 units @ $100.00 = $200.00 gross
        // 10% ($20.00) + $15.00 fixed = $35.00 discount -> $165.00 taxable
        // 0% tax -> line total = $165.00
        val calc = FinancialCalculator.calculateLineItem(
            quantity = BigDecimal("2"),
            unitPrice = BigDecimal("100.00"),
            discountPercent = BigDecimal("10.0"),
            discountFixed = BigDecimal("15.00"),
            taxPercent = BigDecimal.ZERO
        )

        assertEquals(BigDecimal("200.00"), calc.grossAmount)
        assertEquals(BigDecimal("35.00"), calc.discountAmount)
        assertEquals(BigDecimal("165.00"), calc.taxableAmount)
        assertEquals(BigDecimal.ZERO.setScale(2), calc.taxAmount)
        assertEquals(BigDecimal("165.00"), calc.lineTotal)
    }

    @Test
    fun testLineItemCalculation_zeroAndNegativeGuards() {
        // Zero quantity
        val zeroQty = FinancialCalculator.calculateLineItem(
            quantity = BigDecimal.ZERO,
            unitPrice = BigDecimal("50.00")
        )
        assertEquals(BigDecimal.ZERO.setScale(2), zeroQty.grossAmount)
        assertEquals(BigDecimal.ZERO.setScale(2), zeroQty.lineTotal)

        // Negative price should clamp to zero
        val negPrice = FinancialCalculator.calculateLineItem(
            quantity = BigDecimal("3"),
            unitPrice = BigDecimal("-20.00")
        )
        assertEquals(BigDecimal.ZERO.setScale(2), negPrice.grossAmount)
        assertEquals(BigDecimal.ZERO.setScale(2), negPrice.lineTotal)

        // Discount exceeding gross amount should clamp to gross amount
        val hugeDiscount = FinancialCalculator.calculateLineItem(
            quantity = BigDecimal("1"),
            unitPrice = BigDecimal("50.00"),
            discountFixed = BigDecimal("100.00")
        )
        assertEquals(BigDecimal("50.00"), hugeDiscount.discountAmount)
        assertEquals(BigDecimal.ZERO.setScale(2), hugeDiscount.taxableAmount)
        assertEquals(BigDecimal.ZERO.setScale(2), hugeDiscount.lineTotal)
    }

    @Test
    fun testQuotationTotalsCalculation() {
        val item1 = QuotationItem(
            name = "Item 1",
            quantity = BigDecimal("2"),
            unitPrice = BigDecimal("500.00"), // gross = 1000.00
            discountPercent = BigDecimal("10.0"), // disc = 100.00, taxable = 900.00
            taxPercent = BigDecimal("10.0") // tax = 90.00, total = 990.00
        )
        val item2 = QuotationItem(
            name = "Item 2",
            quantity = BigDecimal("1"),
            unitPrice = BigDecimal("200.00"), // gross = 200.00
            discountPercent = BigDecimal.ZERO,
            taxPercent = BigDecimal("10.0") // tax = 20.00, total = 220.00
        )

        // Overall discount $50.00, Additional fee $25.00
        val totals = FinancialCalculator.calculateQuotationTotals(
            items = listOf(item1, item2),
            overallDiscount = BigDecimal("50.00"),
            additionalFee = BigDecimal("25.00")
        )

        // Subtotal = 1000 + 200 = 1200.00
        assertEquals(BigDecimal("1200.00"), totals.subtotal)
        // Total line discounts = 100.00
        assertEquals(BigDecimal("100.00"), totals.totalLineDiscounts)
        // Base taxable = 900 + 200 = 1100.00. Minus overall discount 50.00 = 1050.00
        assertEquals(BigDecimal("1050.00"), totals.taxableAmount)
        // Total tax = 90 + 20 = 110.00
        assertEquals(BigDecimal("110.00"), totals.totalTax)
        // Additional fee = 25.00
        assertEquals(BigDecimal("25.00"), totals.additionalFee)
        // Grand total = 1050.00 + 110.00 + 25.00 = 1185.00
        assertEquals(BigDecimal("1185.00"), totals.grandTotal)
    }
}
