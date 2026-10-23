package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Client
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LiquidGlassCard
import com.example.ui.localization.AppStrings
import com.example.ui.viewmodel.QuotationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: QuotationViewModel,
    onNavigateToCreateQuoteForClient: ((Client) -> Unit)? = null
) {
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var clientToEdit by remember { mutableStateOf<Client?>(null) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }
    var clientDetailView by remember { mutableStateOf<Client?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = if (searchQuery.isBlank()) clients else {
        val q = searchQuery.lowercase()
        clients.filter {
            it.name.lowercase().contains(q) ||
            it.companyName.lowercase().contains(q) ||
            it.email.lowercase().contains(q) ||
            it.phone.contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.get("nav_clients", prefs.language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_client")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("clients_screen")
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(AppStrings.get("client_search_hint", prefs.language)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("client_search_input")
            )

            if (filtered.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Person,
                    title = "No Clients Found",
                    message = if (searchQuery.isNotBlank()) "No clients match your search query." else "Add your business clients to issue customized quotations.",
                    actionLabel = if (searchQuery.isBlank()) "Add Client" else null,
                    onAction = { showAddDialog = true },
                    testTag = "clients_empty_state"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { client ->
                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { clientDetailView = client }
                                .testTag("client_card_${client.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (client.companyName.isNotBlank()) Icons.Default.Business else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = client.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val sub = listOf(client.phone, client.email).filter { it.isNotBlank() }.joinToString(" • ")
                                        if (sub.isNotBlank()) {
                                            Text(
                                                text = sub,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { clientToEdit = client },
                                        modifier = Modifier.testTag("edit_client_btn_${client.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(
                                        onClick = { clientToDelete = client },
                                        modifier = Modifier.testTag("delete_client_btn_${client.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add or Edit Client Dialog
    if (showAddDialog || clientToEdit != null) {
        val editing = clientToEdit
        ClientFormDialog(
            client = editing,
            onDismiss = {
                showAddDialog = false
                clientToEdit = null
            },
            onSave = { saved ->
                viewModel.saveClient(saved)
                showAddDialog = false
                clientToEdit = null
            }
        )
    }

    // Client Detail Dialog
    if (clientDetailView != null) {
        val c = clientDetailView!!
        AlertDialog(
            onDismissRequest = { clientDetailView = null },
            title = { Text(c.displayName) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (c.companyName.isNotBlank() && c.companyName != c.name) {
                        DetailItem("Company", c.companyName)
                    }
                    if (c.contactPerson.isNotBlank()) {
                        DetailItem("Contact Person", c.contactPerson)
                    }
                    if (c.phone.isNotBlank()) {
                        DetailItem("Phone", c.phone)
                    }
                    if (c.email.isNotBlank()) {
                        DetailItem("Email", c.email)
                    }
                    if (c.billingAddress.isNotBlank()) {
                        DetailItem("Billing Address", c.billingAddress)
                    }
                    if (c.taxNumber.isNotBlank()) {
                        DetailItem("Tax / Registration", c.taxNumber)
                    }
                    if (c.notes.isNotBlank()) {
                        DetailItem("Notes", c.notes)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clientToEdit = c
                    clientDetailView = null
                }) {
                    Text("Edit")
                }
            },
            dismissButton = {
                TextButton(onClick = { clientDetailView = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (clientToDelete != null) {
        val c = clientToDelete!!
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            title = { Text("Delete Client") },
            text = { Text("Are you sure you want to delete '${c.displayName}'? Historical quotation snapshots will be preserved safely.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteClient(c.id)
                        clientToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_client_btn")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ClientFormDialog(
    client: Client?,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit
) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var company by remember { mutableStateOf(client?.companyName ?: "") }
    var contact by remember { mutableStateOf(client?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var address by remember { mutableStateOf(client?.billingAddress ?: "") }
    var taxNumber by remember { mutableStateOf(client?.taxNumber ?: "") }
    var notes by remember { mutableStateOf(client?.notes ?: "") }

    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (client != null) "Edit Client" else "Add Client") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("Client Name *") },
                    isError = nameError,
                    supportingText = if (nameError) { { Text("Name is required") } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_form_name_input")
                )

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Organization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_form_company_input")
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_form_phone_input")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_form_email_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Billing Address") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = taxNumber,
                    onValueChange = { taxNumber = it },
                    label = { Text("Tax / VAT / Registration Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Internal Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() && company.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    onSave(
                        Client(
                            id = client?.id ?: 0,
                            name = name.trim().ifBlank { company.trim() },
                            companyName = company.trim(),
                            contactPerson = contact.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            billingAddress = address.trim(),
                            taxNumber = taxNumber.trim(),
                            notes = notes.trim(),
                            createdAt = client?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.testTag("client_form_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
