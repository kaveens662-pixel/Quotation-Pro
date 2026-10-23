package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Quotation
import com.example.ui.localization.AppStrings
import com.example.ui.screens.BusinessProfileScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PricingCalculatorScreen
import com.example.ui.screens.QuotationDetailScreen
import com.example.ui.screens.QuotationWizardScreen
import com.example.ui.screens.QuotationsScreen
import com.example.ui.screens.RecurringCostsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.viewmodel.QuotationViewModel

enum class MainTab(
    val titleKey: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("nav_dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    QUOTATIONS("nav_quotations", Icons.Filled.Description, Icons.Outlined.Description, "tab_quotations"),
    CLIENTS("nav_clients", Icons.Filled.People, Icons.Outlined.People, "tab_clients"),
    CATALOG("nav_catalog", Icons.Filled.Inventory2, Icons.Outlined.Inventory2, "tab_catalog"),
    TOOLS("nav_tools", Icons.Filled.Tune, Icons.Outlined.Tune, "tab_tools")
}

sealed class AppDestination {
    data object TabNavigation : AppDestination()
    data class QuotationDetail(val quoteId: Long) : AppDestination()
    data object QuotationWizard : AppDestination()
    data object RecurringCosts : AppDestination()
    data object PricingCalculator : AppDestination()
    data object BusinessProfile : AppDestination()
    data object Settings : AppDestination()
}

@Composable
fun MainAppNav(viewModel: QuotationViewModel) {
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var currentDestination by remember { mutableStateOf<AppDestination>(AppDestination.TabNavigation) }
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    // Back handling for sub-screens
    if (currentDestination !is AppDestination.TabNavigation) {
        BackHandler {
            currentDestination = AppDestination.TabNavigation
        }
    }

    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { dest ->
        when (dest) {
            is AppDestination.TabNavigation -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = AppStrings.get(tab.titleKey, prefs.language)
                                        )
                                    },
                                    label = { Text(AppStrings.get(tab.titleKey, prefs.language)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            MainTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToCreateQuote = {
                                    viewModel.startNewQuotation()
                                    currentDestination = AppDestination.QuotationWizard
                                },
                                onNavigateToQuoteDetail = { id ->
                                    currentDestination = AppDestination.QuotationDetail(id)
                                },
                                onNavigateToQuotes = { currentTab = MainTab.QUOTATIONS },
                                onNavigateToClients = { currentTab = MainTab.CLIENTS },
                                onNavigateToCatalog = { currentTab = MainTab.CATALOG },
                                onNavigateToPricingCalc = { currentDestination = AppDestination.PricingCalculator }
                            )

                            MainTab.QUOTATIONS -> QuotationsScreen(
                                viewModel = viewModel,
                                onNavigateToCreate = {
                                    viewModel.startNewQuotation()
                                    currentDestination = AppDestination.QuotationWizard
                                },
                                onNavigateToDetail = { id ->
                                    currentDestination = AppDestination.QuotationDetail(id)
                                },
                                onNavigateToEdit = { quote ->
                                    viewModel.startEditQuotation(quote)
                                    currentDestination = AppDestination.QuotationWizard
                                }
                            )

                            MainTab.CLIENTS -> ClientsScreen(
                                viewModel = viewModel,
                                onNavigateToCreateQuoteForClient = { client ->
                                    viewModel.startNewQuotation()
                                    viewModel.selectClientForDraft(client)
                                    currentDestination = AppDestination.QuotationWizard
                                }
                            )

                            MainTab.CATALOG -> CatalogScreen(
                                viewModel = viewModel
                            )

                            MainTab.TOOLS -> ToolsHubScreen(
                                viewModel = viewModel,
                                onNavigateToRecurringCosts = { currentDestination = AppDestination.RecurringCosts },
                                onNavigateToPricingCalc = { currentDestination = AppDestination.PricingCalculator },
                                onNavigateToBusinessProfile = { currentDestination = AppDestination.BusinessProfile },
                                onNavigateToSettings = { currentDestination = AppDestination.Settings }
                            )
                        }
                    }
                }
            }

            is AppDestination.QuotationDetail -> {
                QuotationDetailScreen(
                    quotationId = dest.quoteId,
                    viewModel = viewModel,
                    onNavigateBack = { currentDestination = AppDestination.TabNavigation },
                    onNavigateToEdit = { quote ->
                        viewModel.startEditQuotation(quote)
                        currentDestination = AppDestination.QuotationWizard
                    }
                )
            }

            is AppDestination.QuotationWizard -> {
                QuotationWizardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentDestination = AppDestination.TabNavigation },
                    onQuotationSaved = { newId ->
                        currentDestination = AppDestination.QuotationDetail(newId)
                    }
                )
            }

            is AppDestination.RecurringCosts -> {
                RecurringCostsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentDestination = AppDestination.TabNavigation }
                )
            }

            is AppDestination.PricingCalculator -> {
                PricingCalculatorScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentDestination = AppDestination.TabNavigation },
                    onTransferToNewQuote = {
                        currentDestination = AppDestination.QuotationWizard
                    }
                )
            }

            is AppDestination.BusinessProfile -> {
                BusinessProfileScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentDestination = AppDestination.TabNavigation }
                )
            }

            is AppDestination.Settings -> {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentDestination = AppDestination.TabNavigation }
                )
            }
        }
    }
}
