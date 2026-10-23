package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.calculator.PricingInput
import com.example.domain.model.AppCurrency
import com.example.domain.model.PricingStrategy
import com.example.domain.model.QuotationItem
import com.example.ui.components.LiquidGlassCard
import com.example.ui.localization.AppStrings
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPurple
import com.example.ui.viewmodel.QuotationViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingCalculatorScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit,
    onTransferToNewQuote: () -> Unit
) {
    val context = LocalContext.current
    val input by viewModel.pricingInput.collectAsStateWithLifecycle()
    val result by viewModel.pricingResult.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    var materialsStr by remember(input) { mutableStateOf(input.materialsCost.toPlainString()) }
    var hoursStr by remember(input) { mutableStateOf(input.laborHours.toPlainString()) }
    var rateStr by remember(input) { mutableStateOf(input.hourlyRate.toPlainString()) }
    var outsourcingStr by remember(input) { mutableStateOf(input.outsourcingCost.toPlainString()) }
    var deliveryStr by remember(input) { mutableStateOf(input.deliveryCost.toPlainString()) }
    var softwareStr by remember(input) { mutableStateOf(input.softwareOverhead.toPlainString()) }
    var otherOverheadStr by remember(input) { mutableStateOf(input.otherOverhead.toPlainString()) }
    var contingencyStr by remember(input) { mutableStateOf(input.contingencyPercent.toPlainString()) }
    var targetStr by remember(input) { mutableStateOf(input.targetPercent.toPlainString()) }
    var taxStr by remember(input) { mutableStateOf(input.taxPercent.toPlainString()) }

    fun sync() {
        viewModel.updatePricingInput(
            PricingInput(
                materialsCost = materialsStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                laborHours = hoursStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                hourlyRate = rateStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                outsourcingCost = outsourcingStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                deliveryCost = deliveryStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                softwareOverhead = softwareStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                otherOverhead = otherOverheadStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                contingencyPercent = contingencyStr.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                strategy = input.strategy,
                targetPercent = targetStr.toBigDecimalOrNull() ?: BigDecimal("25.0"),
                taxPercent = taxStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
            )
        )
    }

    val currency = profile.defaultCurrency

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.get("calc_title", prefs.language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("pricing_calculator_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Result Highlight Hero Card
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = AppStrings.get("calc_suggested_price", prefs.language),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = AppCurrency.formatAmount(result.suggestedSellingPricePreTax, currency),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = AppStrings.get("calc_gross_profit", prefs.language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${AppCurrency.formatAmount(result.expectedGrossProfit, currency)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald
                                )
                                Text(
                                    text = "${result.actualGrossMarginPercent}% margin",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentEmerald
                                )
                            }
                        }

                        if (result.taxAmount > BigDecimal.ZERO) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Final Price with Tax (${input.taxPercent}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = AppCurrency.formatAmount(result.finalCustomerPrice, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.startNewQuotation()
                                viewModel.addLineItemToDraft(
                                    QuotationItem(
                                        name = "Project / Service Estimate",
                                        description = "Estimated based on direct costs (${result.totalDirectCosts}), overhead (${result.totalOverhead}), and contingency.",
                                        unitType = "project",
                                        quantity = BigDecimal.ONE,
                                        unitPrice = result.suggestedSellingPricePreTax,
                                        taxPercent = input.taxPercent,
                                        grossAmount = result.suggestedSellingPricePreTax,
                                        lineTotal = result.finalCustomerPrice
                                    )
                                )
                                onTransferToNewQuote()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_transfer_to_quote"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(AppStrings.get("calc_transfer_to_quote", prefs.language))
                        }
                    }
                }
            }

            // Strategy Selector (Profit Margin % vs Markup %)
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Pricing Strategy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = input.strategy == PricingStrategy.PROFIT_MARGIN,
                                onClick = {
                                    viewModel.updatePricingInput(input.copy(strategy = PricingStrategy.PROFIT_MARGIN))
                                },
                                label = { Text(AppStrings.get("calc_mode_margin", prefs.language)) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = input.strategy == PricingStrategy.MARKUP,
                                onClick = {
                                    viewModel.updatePricingInput(input.copy(strategy = PricingStrategy.MARKUP))
                                },
                                label = { Text(AppStrings.get("calc_mode_markup", prefs.language)) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (input.strategy == PricingStrategy.PROFIT_MARGIN) {
                                "Margin formula: Price = Cost / (1 - Margin%). Calculates profit as a % of what the client pays."
                            } else {
                                "Markup formula: Price = Cost × (1 + Markup%). Calculates profit as a % added on top of your costs."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Input Fields Form Card
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Direct Job Costs",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = hoursStr,
                                onValueChange = {
                                    hoursStr = it
                                    sync()
                                },
                                label = { Text(AppStrings.get("calc_labor_hours", prefs.language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = rateStr,
                                onValueChange = {
                                    rateStr = it
                                    sync()
                                },
                                label = { Text(AppStrings.get("calc_hourly_rate", prefs.language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = materialsStr,
                                onValueChange = {
                                    materialsStr = it
                                    sync()
                                },
                                label = { Text(AppStrings.get("calc_materials", prefs.language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = outsourcingStr,
                                onValueChange = {
                                    outsourcingStr = it
                                    sync()
                                },
                                label = { Text(AppStrings.get("calc_outsourcing", prefs.language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = deliveryStr,
                            onValueChange = {
                                deliveryStr = it
                                sync()
                            },
                            label = { Text(AppStrings.get("calc_delivery", prefs.language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "2. Overheads & Contingency",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = softwareStr,
                                onValueChange = {
                                    softwareStr = it
                                    sync()
                                },
                                label = { Text(AppStrings.get("calc_software_overhead", prefs.language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = otherOverheadStr,
                                onValueChange = {
                                    otherOverheadStr = it
                                    sync()
                                },
                                label = { Text(AppStrings.get("calc_other_overhead", prefs.language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = contingencyStr,
                                onValueChange = {
                                    contingencyStr = it
                                    sync()
                                },
                                label = { Text(AppStrings.get("calc_contingency", prefs.language)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = targetStr,
                                onValueChange = {
                                    targetStr = it
                                    sync()
                                },
                                label = { Text(if (input.strategy == PricingStrategy.PROFIT_MARGIN) "Target Margin (%)" else "Target Markup (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = taxStr,
                            onValueChange = {
                                taxStr = it
                                sync()
                            },
                            label = { Text(AppStrings.get("calc_tax_percent", prefs.language)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Calculation Breakdown Explanation
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Financial Breakdown Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = result.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        CalcRow("Direct Labor (${input.laborHours}h × $rateStr)", AppCurrency.formatAmount(result.directLaborCost, currency))
                        CalcRow("Total Direct Costs", AppCurrency.formatAmount(result.totalDirectCosts, currency))
                        CalcRow("Allocated Overhead", AppCurrency.formatAmount(result.totalOverhead, currency))
                        CalcRow("Contingency Allowance (${input.contingencyPercent}%)", AppCurrency.formatAmount(result.contingencyAllowance, currency))
                        CalcRow("Total Internal Estimated Cost", AppCurrency.formatAmount(result.totalEstimatedCost, currency), isBold = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
