package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.ClientEntity
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceStatus
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusPaid
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateInvoiceDialog(
    clients: List<ClientEntity>,
    onDismiss: () -> Unit,
    onSaveInvoice: (
        clientId: String,
        clientName: String,
        clientEmail: String,
        items: List<InvoiceItem>,
        paymentTerms: String,
        notes: String,
        status: String,
        currency: String
    ) -> Unit,
    onOpenFullEditor: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Client selection state
    var isNewClient by remember { mutableStateOf(clients.isEmpty()) }
    var selectedClientId by remember { mutableStateOf(clients.firstOrNull()?.id ?: "") }
    var customClientName by remember { mutableStateOf("") }
    var customClientEmail by remember { mutableStateOf("") }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    val currentClient = remember(selectedClientId, clients) {
        clients.firstOrNull { it.id == selectedClientId } ?: clients.firstOrNull()
    }

    // Invoice Details
    var description by remember { mutableStateOf("Monthly E-Commerce Retainer") }
    var amountText by remember { mutableStateOf("750.00") }
    var quantityText by remember { mutableStateOf("1") }
    var selectedTaxRate by remember { mutableDoubleStateOf(21.0) }
    var selectedCurrency by remember { mutableStateOf(currentClient?.currency ?: "EUR") }
    var paymentTerms by remember { mutableStateOf("Net 14 Days") }
    var notesText by remember { mutableStateOf("Thank you for your business. Payment via SEPA/iDEAL.") }

    val quickDescriptionSuggestions = listOf(
        "Monthly Retainer",
        "E-Commerce Maintenance",
        "Consulting Services",
        "Store Optimization",
        "Custom Feature Dev"
    )

    // Derived Financial Math
    val parsedAmount by remember {
        derivedStateOf { amountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0 }
    }
    val parsedQty by remember {
        derivedStateOf { quantityText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0 }
    }
    val subtotal by remember {
        derivedStateOf { parsedAmount * parsedQty }
    }
    val taxAmount by remember {
        derivedStateOf { subtotal * (selectedTaxRate / 100.0) }
    }
    val totalAmount by remember {
        derivedStateOf { subtotal + taxAmount }
    }

    val isFormValid by remember {
        derivedStateOf {
            val hasClient = if (isNewClient) customClientName.isNotBlank() else currentClient != null
            val hasDesc = description.isNotBlank()
            val hasAmount = parsedAmount > 0.0
            hasClient && hasDesc && hasAmount
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier
            .fillMaxWidth(0.94f)
            .testTag("quick_create_invoice_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Quick Create Invoice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fast client invoice generation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Client Selection
                Text(
                    text = "Client Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                if (clients.isNotEmpty() && !isNewClient) {
                    ExposedDropdownMenuBox(
                        expanded = clientDropdownExpanded,
                        onExpandedChange = { clientDropdownExpanded = !clientDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentClient?.let { "${it.name} (${it.id})" } ?: "Select Client",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Client") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = OceanBlue)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("quick_invoice_client_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = clientDropdownExpanded,
                            onDismissRequest = { clientDropdownExpanded = false }
                        ) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(client.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${client.id} • ${client.email}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedClientId = client.id
                                        selectedCurrency = client.currency
                                        clientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isNewClient = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Client", fontSize = 12.sp)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customClientName,
                        onValueChange = { customClientName = it },
                        label = { Text("Client or Company Name *") },
                        placeholder = { Text("e.g. Nordic Gourmet B.V.") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_invoice_client_name_input")
                    )

                    OutlinedTextField(
                        value = customClientEmail,
                        onValueChange = { customClientEmail = it },
                        label = { Text("Client Email") },
                        placeholder = { Text("billing@company.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (clients.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isNewClient = false }) {
                                Text("Select Existing Client", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Section 2: Invoice Item Description & Suggestions
                Text(
                    text = "Invoice Item & Services",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Service Description *") },
                    placeholder = { Text("e.g. Monthly E-Commerce Retainer") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_invoice_description_input")
                )

                // Quick suggestions horizontal chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickDescriptionSuggestions.forEach { suggestion ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (description == suggestion) OceanBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { description = suggestion }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                fontWeight = if (description == suggestion) FontWeight.Bold else FontWeight.Normal,
                                color = if (description == suggestion) OceanBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Section 3: Amount, Quantity & Currency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Unit Price ($selectedCurrency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("quick_invoice_amount_input")
                    )

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Currency and Tax Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Currency", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("EUR", "USD", "GBP").forEach { curr ->
                                FilterChip(
                                    selected = selectedCurrency == curr,
                                    onClick = { selectedCurrency = curr },
                                    label = { Text(curr, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Tax / VAT", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(0.0, 9.0, 21.0).forEach { rate ->
                                FilterChip(
                                    selected = selectedTaxRate == rate,
                                    onClick = { selectedTaxRate = rate },
                                    label = { Text("${rate.toInt()}%", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Payment Terms
                Column {
                    Text("Payment Terms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Due on Receipt", "Net 7 Days", "Net 14 Days", "Net 30 Days").forEach { term ->
                            FilterChip(
                                selected = paymentTerms == term,
                                onClick = { paymentTerms = term },
                                label = { Text(term, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Live Total Preview Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCurrency(subtotal, selectedCurrency), fontSize = 12.sp)
                        }
                        if (taxAmount > 0.0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tax (${selectedTaxRate.toInt()}%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.formatCurrency(taxAmount, selectedCurrency), fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Invoice Amount:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = Formatters.formatCurrency(totalAmount, selectedCurrency),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = OceanBlue
                            )
                        }
                    }
                }

                if (onOpenFullEditor != null) {
                    TextButton(
                        onClick = onOpenFullEditor,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Need multiple line items? Open Full Editor", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isFormValid) {
                        val finalClientId = if (isNewClient) {
                            "CLI-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}"
                        } else {
                            currentClient?.id ?: "CLI-1001"
                        }
                        val finalClientName = if (isNewClient) customClientName.trim() else (currentClient?.name ?: "Client")
                        val finalClientEmail = if (isNewClient) customClientEmail.trim() else (currentClient?.email ?: "")

                        val lineItem = InvoiceItem(
                            description = description.trim(),
                            quantity = parsedQty,
                            unitPrice = parsedAmount,
                            taxRate = selectedTaxRate,
                            discountPercent = 0.0
                        )

                        onSaveInvoice(
                            finalClientId,
                            finalClientName,
                            finalClientEmail,
                            listOf(lineItem),
                            paymentTerms,
                            notesText,
                            InvoiceStatus.PENDING.name,
                            selectedCurrency
                        )
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OceanBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("quick_invoice_confirm_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Invoice", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("quick_invoice_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}
