package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceStatus
import com.example.ui.components.Formatters
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusPaid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceSheet(
    clients: List<ClientEntity>,
    preselectedClient: ClientEntity? = null,
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
    ) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedClientId by remember { mutableStateOf(preselectedClient?.id ?: clients.firstOrNull()?.id ?: "") }
    var customClientName by remember { mutableStateOf(preselectedClient?.name ?: "") }
    var customClientEmail by remember { mutableStateOf(preselectedClient?.email ?: "") }
    var isNewClientMode by remember { mutableStateOf(clients.isEmpty()) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    var selectedCurrency by remember { mutableStateOf("EUR") }
    var paymentTerms by remember { mutableStateOf("Net 14 Days") }
    var termsDropdownExpanded by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf("Payment via SEPA / iDEAL to Oistars Billings Account.") }

    // Line items list
    val lineItems = remember {
        mutableStateListOf(
            InvoiceItem(
                description = "Monthly E-Commerce Platform Retainer",
                quantity = 1.0,
                unitPrice = 750.00,
                taxRate = 21.0
            )
        )
    }

    val selectedClient = clients.firstOrNull { it.id == selectedClientId }

    val subtotal = lineItems.sumOf { it.subtotal }
    val taxTotal = lineItems.sumOf { it.taxAmount }
    val grandTotal = lineItems.sumOf { it.totalAmount }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create New Invoice",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Client Selection
            Text(
                text = "Client / Store Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!isNewClientMode && clients.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = clientDropdownExpanded,
                    onExpandedChange = { clientDropdownExpanded = !clientDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClient?.name ?: "Select Client Store",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Client Store") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
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
                                        if (client.email.isNotBlank()) {
                                            Text(client.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedClientId = client.id
                                    clientDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                TextButton(
                    onClick = { isNewClientMode = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("+ Enter New Store Info")
                }
            } else {
                OutlinedTextField(
                    value = customClientName,
                    onValueChange = { customClientName = it },
                    label = { Text("Store / Company Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customClientEmail,
                    onValueChange = { customClientEmail = it },
                    label = { Text("Client Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (clients.isNotEmpty()) {
                    TextButton(
                        onClick = { isNewClientMode = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("← Select From Existing Stores")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Terms & Currency
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val termsList = listOf("Net 7 Days", "Net 14 Days", "Net 30 Days", "Due on Receipt")
                ExposedDropdownMenuBox(
                    expanded = termsDropdownExpanded,
                    onExpandedChange = { termsDropdownExpanded = !termsDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = paymentTerms,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Terms") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termsDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = termsDropdownExpanded,
                        onDismissRequest = { termsDropdownExpanded = false }
                    ) {
                        termsList.forEach { term ->
                            DropdownMenuItem(
                                text = { Text(term) },
                                onClick = {
                                    paymentTerms = term
                                    termsDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = selectedCurrency,
                    onValueChange = { selectedCurrency = it.uppercase() },
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.weight(0.6f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Line Items Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Line Items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(
                    onClick = {
                        lineItems.add(
                            InvoiceItem(
                                description = "",
                                quantity = 1.0,
                                unitPrice = 0.0,
                                taxRate = 21.0
                            )
                        )
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }

            // Line Items List
            lineItems.forEachIndexed { index, item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Item #${index + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (lineItems.size > 1) {
                                IconButton(
                                    onClick = { lineItems.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove Item",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = item.description,
                            onValueChange = { newDesc ->
                                lineItems[index] = item.copy(description = newDesc)
                            },
                            label = { Text("Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var qtyStr by remember { mutableStateOf(if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()) }
                            var priceStr by remember { mutableStateOf(if (item.unitPrice > 0) item.unitPrice.toString() else "") }
                            var taxStr by remember { mutableStateOf(item.taxRate.toInt().toString()) }

                            OutlinedTextField(
                                value = qtyStr,
                                onValueChange = {
                                    qtyStr = it
                                    val q = it.toDoubleOrNull() ?: 1.0
                                    lineItems[index] = item.copy(quantity = q)
                                },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = priceStr,
                                onValueChange = {
                                    priceStr = it
                                    val p = it.toDoubleOrNull() ?: 0.0
                                    lineItems[index] = item.copy(unitPrice = p)
                                },
                                label = { Text("Price (€)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1.3f)
                            )

                            OutlinedTextField(
                                value = taxStr,
                                onValueChange = {
                                    taxStr = it
                                    val t = it.toDoubleOrNull() ?: 21.0
                                    lineItems[index] = item.copy(taxRate = t)
                                },
                                label = { Text("VAT %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Item Total: ${Formatters.formatCurrency(item.totalAmount, selectedCurrency)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Invoice Notes & Instructions") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Totals Summary Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal:", fontSize = 13.sp)
                        Text(Formatters.formatCurrency(subtotal, selectedCurrency), fontSize = 13.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("VAT Total:", fontSize = 13.sp)
                        Text(Formatters.formatCurrency(taxTotal, selectedCurrency), fontSize = 13.sp)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            Formatters.formatCurrency(grandTotal, selectedCurrency),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Create Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val cName = if (isNewClientMode) customClientName.ifBlank { "New Client Store" } else selectedClient?.name ?: "Client Store"
                        val cEmail = if (isNewClientMode) customClientEmail else selectedClient?.email ?: ""
                        val cId = if (isNewClientMode) "CLI-${System.currentTimeMillis() % 10000}" else selectedClientId

                        onSaveInvoice(
                            cId,
                            cName,
                            cEmail,
                            lineItems.filter { it.description.isNotBlank() },
                            paymentTerms,
                            notesText,
                            InvoiceStatus.DRAFT.name,
                            selectedCurrency
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Draft")
                }

                Button(
                    onClick = {
                        val cName = if (isNewClientMode) customClientName.ifBlank { "New Client Store" } else selectedClient?.name ?: "Client Store"
                        val cEmail = if (isNewClientMode) customClientEmail else selectedClient?.email ?: ""
                        val cId = if (isNewClientMode) "CLI-${System.currentTimeMillis() % 10000}" else selectedClientId

                        val validItems = if (lineItems.none { it.description.isNotBlank() }) {
                            listOf(InvoiceItem(description = "E-Commerce Billing Services", quantity = 1.0, unitPrice = 500.0, taxRate = 21.0))
                        } else {
                            lineItems.filter { it.description.isNotBlank() }
                        }

                        onSaveInvoice(
                            cId,
                            cName,
                            cEmail,
                            validItems,
                            paymentTerms,
                            notesText,
                            InvoiceStatus.PENDING.name,
                            selectedCurrency
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_invoice_submit_btn")
                ) {
                    Text("Issue Invoice", color = Color.White)
                }
            }
        }
    }
}
