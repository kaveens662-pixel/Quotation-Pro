package com.example.domain.calculator

import com.example.domain.model.ExpenseFrequency
import com.example.domain.model.RecurringExpense
import java.math.BigDecimal
import java.math.RoundingMode

data class CategoryExpenseSummary(
    val category: String,
    val monthlyTotal: BigDecimal,
    val count: Int,
    val percentageOfTotal: Double
)

data class RecurringCostSummary(
    val totalActiveCount: Int,
    val totalMonthlyEquivalent: BigDecimal,
    val totalAnnualEquivalent: BigDecimal,
    val categoryBreakdown: List<CategoryExpenseSummary>,
    val currency: String
)

object RecurringCostCalculator {
    private val SCALE = 2
    private val ROUNDING_MODE = RoundingMode.HALF_UP

    fun calculateMonthlyEquivalent(amount: BigDecimal, frequency: ExpenseFrequency): BigDecimal {
        val safeAmount = amount.coerceAtLeast(BigDecimal.ZERO)
        return when (frequency) {
            ExpenseFrequency.DAILY -> safeAmount.multiply(BigDecimal("365"))
                .divide(BigDecimal("12"), SCALE, ROUNDING_MODE)
            ExpenseFrequency.WEEKLY -> safeAmount.multiply(BigDecimal("52"))
                .divide(BigDecimal("12"), SCALE, ROUNDING_MODE)
            ExpenseFrequency.MONTHLY -> safeAmount.setScale(SCALE, ROUNDING_MODE)
            ExpenseFrequency.QUARTERLY -> safeAmount.divide(BigDecimal("3"), SCALE, ROUNDING_MODE)
            ExpenseFrequency.YEARLY -> safeAmount.divide(BigDecimal("12"), SCALE, ROUNDING_MODE)
        }
    }

    fun summarize(expenses: List<RecurringExpense>, targetCurrency: String = "USD"): RecurringCostSummary {
        val activeExpenses = expenses.filter { it.isActive && it.currency.equals(targetCurrency, ignoreCase = true) }
        
        var totalMonthly = BigDecimal.ZERO
        val categoryMap = mutableMapOf<String, Pair<BigDecimal, Int>>()

        for (expense in activeExpenses) {
            val monthly = calculateMonthlyEquivalent(expense.amount, expense.frequency)
            totalMonthly = totalMonthly.add(monthly)

            val current = categoryMap[expense.category] ?: Pair(BigDecimal.ZERO, 0)
            categoryMap[expense.category] = Pair(current.first.add(monthly), current.second + 1)
        }

        totalMonthly = totalMonthly.setScale(SCALE, ROUNDING_MODE)
        val totalAnnual = totalMonthly.multiply(BigDecimal("12")).setScale(SCALE, ROUNDING_MODE)

        val breakdown = categoryMap.map { (cat, data) ->
            val monthly = data.first.setScale(SCALE, ROUNDING_MODE)
            val pct = if (totalMonthly > BigDecimal.ZERO) {
                monthly.divide(totalMonthly, 4, ROUNDING_MODE).toDouble() * 100.0
            } else {
                0.0
            }
            CategoryExpenseSummary(
                category = cat,
                monthlyTotal = monthly,
                count = data.second,
                percentageOfTotal = pct
            )
        }.sortedByDescending { it.monthlyTotal }

        return RecurringCostSummary(
            totalActiveCount = activeExpenses.size,
            totalMonthlyEquivalent = totalMonthly,
            totalAnnualEquivalent = totalAnnual,
            categoryBreakdown = breakdown,
            currency = targetCurrency
        )
    }

    /**
     * Calculates the estimated overhead per billable hour based on total monthly recurring costs
     * and monthly billable hours (e.g. 120-160 hours/month).
     */
    fun calculateOverheadPerHour(monthlyTotal: BigDecimal, monthlyHours: BigDecimal): BigDecimal {
        if (monthlyHours <= BigDecimal.ZERO) return BigDecimal.ZERO
        return monthlyTotal.divide(monthlyHours, SCALE, ROUNDING_MODE)
    }
}
