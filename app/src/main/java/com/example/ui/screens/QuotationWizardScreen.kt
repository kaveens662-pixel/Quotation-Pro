package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.calculator.FinancialCalculator
import com.example.domain.model.AppCurrency
import com.example.domain.model.CatalogItem
import com.example.domain.model.Client
import com.example.domain.model.QuotationItem
import com.example.domain.model.QuotationStatus
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LiquidGlassCard
import com.example.ui.localization.AppStrings
import com.example.ui.viewmodel.QuotationViewModel
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationWizardScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit,
    onQuotationSaved: (Long) -> Unit
) {
    val context = LocalContext.current
    val draft by viewModel.draftState.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val catalogItems by viewModel.activeCatalogItems.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    if (draft == null) {
        onNavigateBack()
        return
    }

    val currentDraft = draft!!
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEditIndex by remember { mutableStateOf<Int?>(null) }
    var showQuickClientDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentDraft.id == 0L) "New Quotation" else "Edit Quotation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentDraft.quotationNumber} • Step ${currentDraft.currentStep + 1} of 4",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (currentDraft.currentStep > 0) {
                                viewModel.setDraftStep(currentDraft.currentStep - 1)
                            } else {
                                viewModel.cancelDraft()
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("wizard_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentDraft.currentStep > 0) {
                        OutlinedButton(
                            onClick = { viewModel.setDraftStep(currentDraft.currentStep - 1) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(AppStrings.get("btn_back", prefs.language))
                        }
                    } else {
                        TextButton(onClick = {
                            viewModel.cancelDraft()
                            onNavigateBack()
                        }) {
                            Text(AppStrings.get("btn_cancel", prefs.language))
                        }
                    }

                    if (currentDraft.currentStep < 3) {
                        Button(
                            onClick = {
                                if (currentDraft.currentStep == 0 && currentDraft.selectedClient == null) {
                                    Toast.makeText(context, "Please select or add a client first.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (currentDraft.currentStep == 1 && currentDraft.items.isEmpty()) {
                                    Toast.makeText(context, "Please add at least one line item.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.setDraftStep(currentDraft.currentStep + 1)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("wizard_next_btn")
                        ) {
                            Text(AppStrings.get("btn_next", prefs.language))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveDraftQuotation(asStatus = QuotationStatus.DRAFT) { id ->
                                        Toast.makeText(context, "Quotation saved as Draft!", Toast.LENGTH_SHORT).show()
                                        onQuotationSaved(id)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("wizard_save_draft_btn")
                            ) {
                                Text("Save Draft")
                            }

                            Button(
                                onClick = {
                                    viewModel.saveDraftQuotation(asStatus = QuotationStatus.SENT) { id ->
                                        Toast.makeText(context, "Quotation Issued!", Toast.LENGTH_SHORT).show()
                                        onQuotationSaved(id)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("wizard_save_issued_btn")
                            ) {
                                Text("Save & Issue")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Step Indicator Header
            StepProgressRow(currentStep = currentDraft.currentStep)

            when (currentDraft.currentStep) {
                0 -> StepClientSelection(
                    selectedClient = currentDraft.selectedClient,
                    clients = clients,
                    onSelectClient = { viewModel.selectClientForDraft(it) },
                    onAddNewClient = { showQuickClientDialog = true }
                )
                1 -> StepItemsSelection(
                    items = currentDraft.items,
                    currency = currentDraft.currency,
                    onAddItem = {
                        itemToEditIndex = null
                        showAddItemDialog = true
                    },
                    onEditItem = { idx ->
                        itemToEditIndex = idx
                        showAddItemDialog = true
                    },
                    onRemoveItem = { idx -> viewModel.removeLineItemFromDraft(idx) }
                )
                2 -> StepTermsAndDetails(
                    draft = currentDraft,
                    onUpdate = { issue, valid, curr, overallDisc, fee, payTerms, delTerms, notes ->
                        viewModel.updateDraftTerms(issue, valid, curr, overallDisc, fee, payTerms, delTerms, notes)
                    }
                )
                3 -> StepReviewAndTotals(
                    draft = currentDraft
                )
            }
        }
    }

    // Add or Edit Line Item Dialog
    if (showAddItemDialog) {
        val existingItem = if (itemToEditIndex != null && itemToEditIndex!! in currentDraft.items.indices) {
            currentDraft.items[itemToEditIndex!!]
        } else null

        AddLineItemDialog(
            existingItem = existingItem,
            catalogItems = catalogItems,
            defaultCurrency = currentDraft.currency,
            onDismiss = { showAddItemDialog = false },
            onConfirm = { item ->
                if (itemToEditIndex != null) {
                    viewModel.updateLineItemInDraft(itemToEditIndex!!, item)
                } else {
                    viewModel.addLineItemToDraft(item)
                }
                showAddItemDialog = false
            }
        )
    }

    // Quick Add Client Dialog
    if (showQuickClientDialog) {
        QuickAddClientDialog(
            onDismiss = { showQuickClientDialog = false },
            onConfirm = { newClient ->
                viewModel.saveClient(newClient) {
                    viewModel.selectClientForDraft(newClient)
                }
                showQuickClientDialog = false
            }
        )
    }
}

@Composable
private fun StepProgressRow(currentStep: Int) {
    val steps = listOf("Client", "Items", "Terms", "Review")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { idx, label ->
            val isActive = idx == currentStep
            val isPassed = idx < currentStep

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPassed || isActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = (idx + 1).toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StepClientSelection(
    selectedClient: Client?,
    clients: List<Client>,
    onSelectClient: (Client) -> Unit,
    onAddNewClient: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = if (searchQuery.isBlank()) clients else {
        val q = searchQuery.lowercase()
        clients.filter { it.name.lowercase().contains(q) || it.companyName.lowercase().contains(q) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Current Selected Client Card
        if (selectedClient != null) {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected Client:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedClient.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedClient.email.isNotBlank()) {
                            Text(text = selectedClient.email, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Client",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddNewClient,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("wizard_add_client_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Client")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search clients...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Person,
                title = "No Clients Found",
                message = "Add a new client to associate with this quotation.",
                actionLabel = "Add Client",
                onAction = onAddNewClient
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { client ->
                    val isSelected = selectedClient?.id == client.id
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectClient(client) },
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = client.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val sub = listOf(client.phone, client.email).filter { it.isNotBlank() }.joinToString(" • ")
                                if (sub.isNotBlank()) {
                                    Text(text = sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItemsSelection(
    items: List<QuotationItem>,
    currency: String,
    onAddItem: () -> Unit,
    onEditItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quotation Items (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddItem,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("wizard_add_item_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Item")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Add,
                title = "No Items Added",
                message = "Add products, services, or custom line items to your quotation.",
                actionLabel = "Add Item",
                onAction = onAddItem
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${item.quantity} ${item.unitType} × ${AppCurrency.formatAmount(item.unitPrice, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Line Total: ${AppCurrency.formatAmount(item.lineTotal, currency)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row {
                                IconButton(onClick = { onEditItem(index) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                                }
                                IconButton(onClick = { onRemoveItem(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Item", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepTermsAndDetails(
    draft: com.example.ui.viewmodel.QuotationDraftState,
    onUpdate: (Long, Long, String, BigDecimal, BigDecimal, String, String, String) -> Unit
) {
    val context = LocalContext.current
    var issueDate by remember { mutableStateOf(draft.issueDate) }
    var validUntilDate by remember { mutableStateOf(draft.validUntilDate) }
    var selectedCurrency by remember { mutableStateOf(draft.currency) }
    var overallDiscountStr by remember { mutableStateOf(if (draft.overallDiscount > BigDecimal.ZERO) draft.overallDiscount.toPlainString() else "") }
    var additionalFeeStr by remember { mutableStateOf(if (draft.additionalFee > BigDecimal.ZERO) draft.additionalFee.toPlainString() else "") }
    var paymentTerms by remember { mutableStateOf(draft.paymentTerms) }
    var deliveryTerms by remember { mutableStateOf(draft.deliveryTerms) }
    var notes by remember { mutableStateOf(draft.notes) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }

    fun sync() {
        val overallDisc = overallDiscountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val addFee = additionalFeeStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
        onUpdate(issueDate, validUntilDate, selectedCurrency, overallDisc, addFee, paymentTerms, deliveryTerms, notes)
    }

    val sdf = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Terms, Currency & Dates",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Date selection rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LiquidGlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = issueDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val picked = Calendar.getInstance().apply { set(y, m, d) }.timeInMillis
                                issueDate = picked
                                sync()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Issue Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = sdf.format(Date(issueDate)), fontWeight = FontWeight.Bold)
                }
            }

            LiquidGlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = validUntilDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val picked = Calendar.getInstance().apply { set(y, m, d) }.timeInMillis
                                validUntilDate = picked
                                sync()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Valid Until", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = sdf.format(Date(validUntilDate)), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Currency dropdown
        Box {
            OutlinedTextField(
                value = "$selectedCurrency (${AppCurrency.find(selectedCurrency).symbol})",
                onValueChange = {},
                readOnly = true,
                label = { Text("Quotation Currency") },
                trailingIcon = {
                    TextButton(onClick = { showCurrencyDropdown = true }) {
                        Text("Change")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCurrencyDropdown = true }
            )
            DropdownMenu(
                expanded = showCurrencyDropdown,
                onDismissRequest = { showCurrencyDropdown = false }
            ) {
                AppCurrency.ALL.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.code} — ${c.name} (${c.symbol})") },
                        onClick = {
                            selectedCurrency = c.code
                            showCurrencyDropdown = false
                            sync()
                        }
                    )
                }
            }
        }

        // Discounts and Fees
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = overallDiscountStr,
                onValueChange = {
                    overallDiscountStr = it
                    sync()
                },
                label = { Text("Overall Discount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = additionalFeeStr,
                onValueChange = {
                    additionalFeeStr = it
                    sync()
                },
                label = { Text("Additional Fee") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = paymentTerms,
            onValueChange = {
                paymentTerms = it
                sync()
            },
            label = { Text("Payment Terms") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = deliveryTerms,
            onValueChange = {
                deliveryTerms = it
                sync()
            },
            label = { Text("Delivery Terms") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
                sync()
            },
            label = { Text("Notes / Comments") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StepReviewAndTotals(
    draft: com.example.ui.viewmodel.QuotationDraftState
) {
    val recalculated = draft.items.map { item ->
        val c = FinancialCalculator.calculateLineItem(
            quantity = item.quantity,
            unitPrice = item.unitPrice,
            discountPercent = item.discountPercent,
            discountFixed = item.discountFixed,
            taxPercent = item.taxPercent
        )
        item.copy(
            grossAmount = c.grossAmount,
            lineDiscountAmount = c.discountAmount,
            taxableAmount = c.taxableAmount,
            taxAmount = c.taxAmount,
            lineTotal = c.lineTotal
        )
    }

    val totals = FinancialCalculator.calculateQuotationTotals(
        items = recalculated,
        overallDiscount = draft.overallDiscount,
        additionalFee = draft.additionalFee
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Review & Finalize",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Summary Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Quotation: ${draft.quotationNumber}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val client = draft.selectedClient?.displayName ?: "No client selected"
                Text(text = "Client: $client", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Items Count: ${draft.items.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Totals Box
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(text = "Financial Calculation Breakdown", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                ReviewRow("Subtotal", AppCurrency.formatAmount(totals.subtotal, draft.currency))
                if (totals.totalLineDiscounts > BigDecimal.ZERO) {
                    ReviewRow("Line Discounts", "-${AppCurrency.formatAmount(totals.totalLineDiscounts, draft.currency)}")
                }
                if (totals.overallDiscount > BigDecimal.ZERO) {
                    ReviewRow("Overall Discount", "-${AppCurrency.formatAmount(totals.overallDiscount, draft.currency)}")
                }
                ReviewRow("Taxable Amount", AppCurrency.formatAmount(totals.taxableAmount, draft.currency))
                if (totals.totalTax > BigDecimal.ZERO) {
                    ReviewRow("Tax", "+${AppCurrency.formatAmount(totals.totalTax, draft.currency)}")
                }
                if (totals.additionalFee > BigDecimal.ZERO) {
                    ReviewRow("Additional Fee", "+${AppCurrency.formatAmount(totals.additionalFee, draft.currency)}")
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Grand Total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = AppCurrency.formatAmount(totals.grandTotal, draft.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AddLineItemDialog(
    existingItem: QuotationItem?,
    catalogItems: List<CatalogItem>,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onConfirm: (QuotationItem) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var unitType by remember { mutableStateOf(existingItem?.unitType ?: "units") }
    var quantityStr by remember { mutableStateOf(existingItem?.quantity?.toPlainString() ?: "1") }
    var unitPriceStr by remember { mutableStateOf(existingItem?.unitPrice?.toPlainString() ?: "") }
    var discountPercentStr by remember { mutableStateOf(if ((existingItem?.discountPercent ?: BigDecimal.ZERO) > BigDecimal.ZERO) existingItem?.discountPercent?.toPlainString() ?: "" else "") }
    var taxPercentStr by remember { mutableStateOf(if ((existingItem?.taxPercent ?: BigDecimal.ZERO) > BigDecimal.ZERO) existingItem?.taxPercent?.toPlainString() ?: "" else "") }

    var showCatalogPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingItem != null) "Edit Line Item" else "Add Line Item") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (catalogItems.isNotEmpty() && existingItem == null) {
                    OutlinedButton(
                        onClick = { showCatalogPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick from Catalog Items")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unitType,
                        onValueChange = { unitType = it },
                        label = { Text("Unit (hrs, pcs)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = unitPriceStr,
                    onValueChange = { unitPriceStr = it },
                    label = { Text("Unit Price ($defaultCurrency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountPercentStr,
                        onValueChange = { discountPercentStr = it },
                        label = { Text("Discount (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = taxPercentStr,
                        onValueChange = { taxPercentStr = it },
                        label = { Text("Tax (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val qty = quantityStr.toBigDecimalOrNull() ?: BigDecimal.ONE
                    val price = unitPriceStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val disc = discountPercentStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val tax = taxPercentStr.toBigDecimalOrNull() ?: BigDecimal.ZERO

                    val calc = FinancialCalculator.calculateLineItem(qty, price, disc, BigDecimal.ZERO, tax)

                    onConfirm(
                        QuotationItem(
                            id = existingItem?.id ?: 0,
                            name = name.trim(),
                            description = description.trim(),
                            unitType = unitType.trim(),
                            quantity = qty,
                            unitPrice = price,
                            discountPercent = disc,
                            taxPercent = tax,
                            grossAmount = calc.grossAmount,
                            lineDiscountAmount = calc.discountAmount,
                            taxableAmount = calc.taxableAmount,
                            taxAmount = calc.taxAmount,
                            lineTotal = calc.lineTotal
                        )
                    )
                }
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showCatalogPicker) {
        AlertDialog(
            onDismissRequest = { showCatalogPicker = false },
            title = { Text("Select from Catalog") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(catalogItems) { catItem ->
                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    name = catItem.name
                                    description = catItem.description
                                    unitType = catItem.unitType
                                    unitPriceStr = catItem.defaultUnitPrice.toPlainString()
                                    quantityStr = catItem.defaultQuantity.toPlainString()
                                    taxPercentStr = catItem.taxRate.toPlainString()
                                    showCatalogPicker = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = catItem.name, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${AppCurrency.formatAmount(catItem.defaultUnitPrice, defaultCurrency)} / ${catItem.unitType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCatalogPicker = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun QuickAddClientDialog(
    onDismiss: () -> Unit,
    onConfirm: (Client) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Client") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Billing Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() && company.isBlank()) return@Button
                    onConfirm(
                        Client(
                            name = name.trim().ifBlank { company.trim() },
                            companyName = company.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            billingAddress = address.trim()
                        )
                    )
                }
            ) {
                Text("Add Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
