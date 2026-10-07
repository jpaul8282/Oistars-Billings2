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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceStatus
import com.example.ui.components.ExportPdfDialog
import com.example.ui.components.Formatters
import com.example.ui.components.SearchBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun InvoicesScreen(
    invoices: List<InvoiceEntity>,
    clients: List<ClientEntity>,
    selectedFilter: String,
    searchQuery: String,
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onMarkAsPaid: (InvoiceEntity) -> Unit,
    onRecordPayment: (InvoiceEntity, Double, String, String, String) -> Unit,
    onDeleteInvoice: (InvoiceEntity) -> Unit,
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
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedInvoiceForDetail by remember { mutableStateOf<InvoiceEntity?>(null) }

    val filterOptions = listOf(
        "ALL" to "All (${invoices.size})",
        "PENDING" to "Pending (${invoices.count { it.invoiceStatus == InvoiceStatus.PENDING }})",
        "PAID" to "Paid (${invoices.count { it.invoiceStatus == InvoiceStatus.PAID }})",
        "OVERDUE" to "Overdue (${invoices.count { it.invoiceStatus == InvoiceStatus.OVERDUE }})",
        "DRAFT" to "Draft (${invoices.count { it.invoiceStatus == InvoiceStatus.DRAFT }})"
    )

    val filteredInvoices = invoices.filter { invoice ->
        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "PENDING" -> invoice.invoiceStatus == InvoiceStatus.PENDING
            "PAID" -> invoice.invoiceStatus == InvoiceStatus.PAID
            "OVERDUE" -> invoice.invoiceStatus == InvoiceStatus.OVERDUE
            "DRAFT" -> invoice.invoiceStatus == InvoiceStatus.DRAFT
            else -> true
        }

        val matchesSearch = if (searchQuery.isBlank()) true else {
            invoice.id.contains(searchQuery, ignoreCase = true) ||
            invoice.clientName.contains(searchQuery, ignoreCase = true) ||
            invoice.notes.contains(searchQuery, ignoreCase = true) ||
            invoice.items.any { it.description.contains(searchQuery, ignoreCase = true) }
        }

        matchesFilter && matchesSearch
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Input
            PaddingValues(horizontal = 16.dp, vertical = 8.dp).let {
                Box(modifier = Modifier.padding(it)) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchChange,
                        placeholder = "Search by invoice # or client name..."
                    )
                }
            }

            // Filter Tabs
            val selectedTabIndex = filterOptions.indexOfFirst { it.first == selectedFilter }.coerceAtLeast(0)
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                divider = {}
            ) {
                filterOptions.forEachIndexed { index, (key, label) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { onFilterChange(key) },
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Invoice List
            if (filteredInvoices.isEmpty()) {
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No invoices found for '$searchQuery'" else "No invoices in this view",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { showCreateSheet = true }) {
                            Text("+ Create Invoice")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredInvoices, key = { it.id }) { invoice ->
                        InvoiceListItemCard(
                            invoice = invoice,
                            onClick = { selectedInvoiceForDetail = invoice },
                            onMarkAsPaid = { onMarkAsPaid(invoice) }
                        )
                    }
                }
            }
        }

        // FAB Create Invoice
        FloatingActionButton(
            onClick = { showCreateSheet = true },
            containerColor = OceanBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("create_invoice_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Invoice")
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Invoice", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCreateSheet) {
        CreateInvoiceSheet(
            clients = clients,
            onDismiss = { showCreateSheet = false },
            onSaveInvoice = { clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency ->
                onSaveInvoice(clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency)
            }
        )
    }

    selectedInvoiceForDetail?.let { inv ->
        InvoiceDetailSheet(
            invoice = inv,
            onDismiss = { selectedInvoiceForDetail = null },
            onMarkAsPaid = {
                onMarkAsPaid(it)
                selectedInvoiceForDetail = null
            },
            onRecordPayment = { invoice, amount, method, ref, notes ->
                onRecordPayment(invoice, amount, method, ref, notes)
                selectedInvoiceForDetail = null
            },
            onDelete = {
                onDeleteInvoice(it)
                selectedInvoiceForDetail = null
            }
        )
    }
}

@Composable
fun InvoiceListItemCard(
    invoice: InvoiceEntity,
    onClick: () -> Unit,
    onMarkAsPaid: () -> Unit,
    onSelectClient: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showExportPdf by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("invoice_card_${invoice.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Invoice ID & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = invoice.id,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${Formatters.formatShortDate(invoice.issueDate)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = invoice.invoiceStatus)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Client Name & Item Summary
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onSelectClient != null) {
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectClient(invoice.clientId) }
                } else Modifier
            ) {
                Text(
                    text = invoice.clientName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (onSelectClient != null) OceanBlue else MaterialTheme.colorScheme.onSurface
                )
                if (onSelectClient != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "View Store",
                        tint = OceanBlue.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            val firstItem = invoice.items.firstOrNull()?.description
            if (!firstItem.isNullOrBlank()) {
                Text(
                    text = if (invoice.items.size > 1) "$firstItem + ${invoice.items.size - 1} more" else firstItem,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Total Amount & Balance / Quick action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Formatters.formatCurrency(invoice.totalAmount, invoice.currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (invoice.balanceDue > 0 && invoice.invoiceStatus != InvoiceStatus.DRAFT) {
                        Text(
                            text = "Due: ${Formatters.formatShortDate(invoice.dueDate)} (${Formatters.formatCurrency(invoice.balanceDue, invoice.currency)} left)",
                            fontSize = 11.sp,
                            color = if (invoice.invoiceStatus == InvoiceStatus.OVERDUE) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (invoice.invoiceStatus == InvoiceStatus.OVERDUE) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { showExportPdf = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("invoice_card_pdf_btn_${invoice.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Export PDF",
                            tint = OceanBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (invoice.invoiceStatus == InvoiceStatus.PENDING || invoice.invoiceStatus == InvoiceStatus.OVERDUE) {
                        TextButton(
                            onClick = onMarkAsPaid,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusPaid, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Paid", fontSize = 12.sp, color = StatusPaid)
                        }
                    }

                    Icon(
                        Icons.Default.ArrowForwardIos,
                        contentDescription = "View Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    if (showExportPdf) {
        ExportPdfDialog(
            invoice = invoice,
            onDismissRequest = { showExportPdf = false }
        )
    }
}
