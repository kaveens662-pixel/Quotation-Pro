package com.example.domain

import com.example.domain.calculator.RecurringCostCalculator
import com.example.domain.model.ExpenseFrequency
import com.example.domain.model.RecurringExpense
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class RecurringCostCalculatorTest {

    @Test
    fun testFrequencyConversions() {
        // Daily: 10 * 365 / 12 = 3650 / 12 = 304.17
        val daily = RecurringCostCalculator.calculateMonthlyEquivalent(BigDecimal("10.00"), ExpenseFrequency.DAILY)
        assertEquals(BigDecimal("304.17"), daily)

        // Weekly: 100 * 52 / 12 = 5200 / 12 = 433.33
        val weekly = RecurringCostCalculator.calculateMonthlyEquivalent(BigDecimal("100.00"), ExpenseFrequency.WEEKLY)
        assertEquals(BigDecimal("433.33"), weekly)

        // Monthly: 50.00
        val monthly = RecurringCostCalculator.calculateMonthlyEquivalent(BigDecimal("50.00"), ExpenseFrequency.MONTHLY)
        assertEquals(BigDecimal("50.00"), monthly)

        // Quarterly: 300 / 3 = 100.00
        val quarterly = RecurringCostCalculator.calculateMonthlyEquivalent(BigDecimal("300.00"), ExpenseFrequency.QUARTERLY)
        assertEquals(BigDecimal("100.00"), quarterly)

        // Yearly: 1200 / 12 = 100.00
        val yearly = RecurringCostCalculator.calculateMonthlyEquivalent(BigDecimal("1200.00"), ExpenseFrequency.YEARLY)
        assertEquals(BigDecimal("100.00"), yearly)
    }

    @Test
    fun testSummarizeAndOverheadPerHour() {
        val expenses = listOf(
            RecurringExpense(name = "SaaS 1", category = "Software", amount = BigDecimal("50.00"), frequency = ExpenseFrequency.MONTHLY),
            RecurringExpense(name = "Hosting", category = "Infrastructure", amount = BigDecimal("600.00"), frequency = ExpenseFrequency.YEARLY), // 50/mo
            RecurringExpense(name = "Inactive", category = "Software", amount = BigDecimal("100.00"), frequency = ExpenseFrequency.MONTHLY, isActive = false)
        )

        val summary = RecurringCostCalculator.summarize(expenses, "USD")
        // Total monthly = 50 + 50 = 100.00
        assertEquals(BigDecimal("100.00"), summary.totalMonthlyEquivalent)
        // Total annual = 100 * 12 = 1200.00
        assertEquals(BigDecimal("1200.00"), summary.totalAnnualEquivalent)
        assertEquals(2, summary.totalActiveCount)

        // Overhead per hour with 100 billable hours = 100 / 100 = 1.00
        val hourly = RecurringCostCalculator.calculateOverheadPerHour(summary.totalMonthlyEquivalent, BigDecimal("100"))
        assertEquals(BigDecimal("1.00"), hourly)
    }
}
