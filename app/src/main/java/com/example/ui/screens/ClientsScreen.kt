package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.ui.components.Formatters
import com.example.ui.components.SearchBar
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid

@Composable
fun ClientsScreen(
    clients: List<ClientEntity>,
    invoices: List<InvoiceEntity>,
    onSaveClient: (
        id: String?,
        name: String,
        contactPerson: String,
        email: String,
        phone: String,
        taxNumber: String,
        address: String,
        country: String,
        currency: String,
        paymentTermsDays: Int,
        notes: String
    ) -> Unit,
    onDeleteClient: (ClientEntity) -> Unit,
    onCreateInvoiceForClient: (ClientEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }
    var viewingClientDetails by remember { mutableStateOf<ClientEntity?>(null) }

    val filteredClients = clients.filter { client ->
        if (searchQuery.isBlank()) true else {
            client.name.contains(searchQuery, ignoreCase = true) ||
            client.contactPerson.contains(searchQuery, ignoreCase = true) ||
            client.email.contains(searchQuery, ignoreCase = true) ||
            client.taxNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PaddingValues(horizontal = 16.dp, vertical = 8.dp).let {
                Box(modifier = Modifier.padding(it)) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search client stores or VAT numbers..."
                    )
                }
            }

            if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No stores found" else "No client stores registered",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("+ Add Client Store")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val clientInvoices = invoices.filter { it.clientId == client.id }
                        val totalBilled = clientInvoices.sumOf { it.totalAmount }
                        val totalOutstanding = clientInvoices.sumOf { it.balanceDue }

                        ClientItemCard(
                            client = client,
                            invoicesCount = clientInvoices.size,
                            totalBilled = totalBilled,
                            totalOutstanding = totalOutstanding,
                            onClick = { viewingClientDetails = client },
                            onEdit = { editingClient = client },
                            onCreateInvoice = { onCreateInvoiceForClient(client) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = OceanBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_client_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Store", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showAddDialog || editingClient != null) {
        val clientToEdit = editingClient
        ClientFormDialog(
            client = clientToEdit,
            onDismiss = {
                showAddDialog = false
                editingClient = null
            },
            onSave = { id, name, contact, email, phone, tax, address, country, currency, terms, notes ->
                onSaveClient(id, name, contact, email, phone, tax, address, country, currency, terms, notes)
                showAddDialog = false
                editingClient = null
            }
        )
    }

    viewingClientDetails?.let { client ->
        val clientInvoices = invoices.filter { it.clientId == client.id }
        ClientDetailDialog(
            client = client,
            invoices = clientInvoices,
            onDismiss = { viewingClientDetails = null },
            onEdit = {
                viewingClientDetails = null
                editingClient = client
            },
            onDelete = {
                onDeleteClient(client)
                viewingClientDetails = null
            },
            onCreateInvoice = {
                viewingClientDetails = null
                onCreateInvoiceForClient(client)
            }
        )
    }
}

@Composable
fun ClientItemCard(
    client: ClientEntity,
    invoicesCount: Int,
    totalBilled: Double,
    totalOutstanding: Double,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onCreateInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("client_card_${client.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = client.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (client.contactPerson.isNotBlank()) {
                            Text(
                                text = "${client.contactPerson} • ${client.country}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Client",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Billed: ${Formatters.formatCurrency(totalBilled, client.currency)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (totalOutstanding > 0) {
                        Text(
                            text = "Outstanding: ${Formatters.formatCurrency(totalOutstanding, client.currency)}",
                            fontSize = 11.sp,
                            color = StatusOverdue,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "$invoicesCount invoices settled",
                            fontSize = 11.sp,
                            color = StatusPaid
                        )
                    }
                }

                TextButton(
                    onClick = onCreateInvoice,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Invoice", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ClientFormDialog(
    client: ClientEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        contactPerson: String,
        email: String,
        phone: String,
        taxNumber: String,
        address: String,
        country: String,
        currency: String,
        paymentTermsDays: Int,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var contact by remember { mutableStateOf(client?.contactPerson ?: "") }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var taxNumber by remember { mutableStateOf(client?.taxNumber ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var country by remember { mutableStateOf(client?.country ?: "Netherlands") }
    var currency by remember { mutableStateOf(client?.currency ?: "EUR") }
    var termsDaysText by remember { mutableStateOf((client?.paymentTermsDays ?: 14).toString()) }
    var notes by remember { mutableStateOf(client?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (client == null) "Add Client Store" else "Edit Client Store",
                fontWeight = FontWeight.Bold
            )
        },
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
                    label = { Text("Store / Brand Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Billing Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = taxNumber,
                    onValueChange = { taxNumber = it },
                    label = { Text("VAT / Tax ID (e.g. NL892019482B01)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Business Address") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = termsDaysText,
                        onValueChange = { termsDaysText = it },
                        label = { Text("Net Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Account Notes") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            client?.id,
                            name,
                            contact,
                            email,
                            phone,
                            taxNumber,
                            address,
                            country,
                            currency,
                            termsDaysText.toIntOrNull() ?: 14,
                            notes
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
            ) {
                Text("Save Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ClientDetailDialog(
    client: ClientEntity,
    invoices: List<InvoiceEntity>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCreateInvoice: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = client.name, fontWeight = FontWeight.Bold)
                Text(text = "Client ID: ${client.id}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (client.contactPerson.isNotBlank()) {
                    Text("Contact: ${client.contactPerson}", fontSize = 13.sp)
                }
                if (client.email.isNotBlank()) {
                    Text("Email: ${client.email}", fontSize = 13.sp)
                }
                if (client.phone.isNotBlank()) {
                    Text("Phone: ${client.phone}", fontSize = 13.sp)
                }
                if (client.taxNumber.isNotBlank()) {
                    Text("VAT ID: ${client.taxNumber}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (client.address.isNotBlank()) {
                    Text("Address: ${client.address}, ${client.country}", fontSize = 13.sp)
                }
                Text("Payment Terms: Net ${client.paymentTermsDays} Days", fontSize = 13.sp)

                if (client.notes.isNotBlank()) {
                    Text("Notes: ${client.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Invoice History (${invoices.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                invoices.take(5).forEach { inv ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${inv.id} (${Formatters.formatShortDate(inv.issueDate)})", fontSize = 12.sp)
                        Text(
                            "${Formatters.formatCurrency(inv.totalAmount, inv.currency)} [${inv.status}]",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCreateInvoice,
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
            ) {
                Text("New Invoice")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusOverdue)
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Store") },
            text = { Text("Are you sure you want to delete ${client.name}? Past invoices will remain in the database.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
