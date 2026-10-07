package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.model.InvoiceStatus
import com.example.data.model.SubscriptionStatus
import com.example.ui.components.ExportPdfDialog
import com.example.ui.components.Formatters
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusDraft
import com.example.ui.theme.StatusDraftBg
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingBg

/**
 * Detailed modal bottom sheet triggered when a client is selected from the Dashboard.
 * Displays:
 * 1. Contact Information & business credentials (with quick-dial, quick-email, and clipboard copy).
 * 2. Specific Billing History (invoices, total billed, amount collected, balance due, and status filtering).
 * 3. Active Subscriptions & Retainers (MRR, renewal dates, intervals, and instant invoice generation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailModal(
    client: ClientEntity,
    invoices: List<InvoiceEntity>,
    subscriptions: List<SubscriptionEntity>,
    onDismiss: () -> Unit,
    onCreateInvoiceForClient: (ClientEntity) -> Unit,
    onViewInvoiceDetail: ((InvoiceEntity) -> Unit)? = null,
    onMarkInvoiceAsPaid: ((InvoiceEntity) -> Unit)? = null,
    onBillSubscriptionNow: ((SubscriptionEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var invoiceStatusFilter by remember { mutableStateOf("ALL") }
    var invoiceToExportPdf by remember { mutableStateOf<InvoiceEntity?>(null) }

    // Filter data specifically for this client
    val clientInvoices = remember(invoices, client.id) {
        invoices.filter { it.clientId == client.id || it.clientName.equals(client.name, ignoreCase = true) }
    }
    val clientSubscriptions = remember(subscriptions, client.id) {
        subscriptions.filter { it.clientId == client.id || it.clientName.equals(client.name, ignoreCase = true) }
    }

    // Calculate specific financial metrics for this client
    val totalBilled = clientInvoices.sumOf { it.totalAmount }
    val totalPaid = clientInvoices.sumOf { it.amountPaid }
    val totalOutstanding = clientInvoices.sumOf { it.balanceDue }
    val overdueCount = clientInvoices.count { it.invoiceStatus == InvoiceStatus.OVERDUE }
    val overdueAmount = clientInvoices.filter { it.invoiceStatus == InvoiceStatus.OVERDUE }.sumOf { it.balanceDue }
    val pendingCount = clientInvoices.count { it.invoiceStatus == InvoiceStatus.PENDING }
    val paidCount = clientInvoices.count { it.invoiceStatus == InvoiceStatus.PAID }

    val activeSubscriptionsCount = clientSubscriptions.count { it.subscriptionStatus == SubscriptionStatus.ACTIVE }
    val monthlyRetainerRevenue = clientSubscriptions
        .filter { it.subscriptionStatus == SubscriptionStatus.ACTIVE }
        .sumOf { sub ->
            val months = sub.billingInterval.months.coerceAtLeast(1)
            sub.amount / months
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            )
        },
        modifier = Modifier.testTag("client_detail_modal_${client.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar with Avatar, Store Name, and Health Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Initials
                    val initials = client.name
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString("")
                        .ifBlank { "CL" }

                    Surface(
                        shape = CircleShape,
                        color = NavyDark,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = client.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = client.id,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (client.country.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${client.country}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("client_detail_modal_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Account Health Pill & Terms Status Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    overdueCount > 0 -> StatusOverdueBg
                    pendingCount > 0 -> StatusPendingBg
                    paidCount > 0 -> StatusPaidBg
                    else -> StatusDraftBg
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                overdueCount > 0 -> Icons.Default.Warning
                                pendingCount > 0 -> Icons.Default.HourglassEmpty
                                paidCount > 0 -> Icons.Default.CheckCircle
                                else -> Icons.Default.Business
                            },
                            contentDescription = null,
                            tint = when {
                                overdueCount > 0 -> StatusOverdue
                                pendingCount > 0 -> StatusPending
                                paidCount > 0 -> StatusPaid
                                else -> StatusDraft
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                overdueCount > 0 -> "Action Required: $overdueCount invoice(s) overdue (${Formatters.formatCurrency(overdueAmount, client.currency)})"
                                pendingCount > 0 -> "Active Account: $pendingCount open invoice(s) awaiting settlement"
                                paidCount > 0 -> "Good Standing: All issued invoices settled"
                                else -> "New Client: Ready for initial invoicing"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                overdueCount > 0 -> StatusOverdue
                                pendingCount > 0 -> StatusPending
                                paidCount > 0 -> StatusPaid
                                else -> StatusDraft
                            }
                        )
                    }

                    Text(
                        text = "Net ${client.paymentTermsDays}d",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Client KPI Cards Matrix
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClientKpiCard(
                    title = "Total Invoiced",
                    amount = totalBilled,
                    currency = client.currency,
                    subtitle = "${clientInvoices.size} invoices",
                    modifier = Modifier.weight(1f)
                )

                ClientKpiCard(
                    title = "Paid to Date",
                    amount = totalPaid,
                    currency = client.currency,
                    amountColor = StatusPaid,
                    subtitle = "$paidCount settled",
                    modifier = Modifier.weight(1f)
                )

                ClientKpiCard(
                    title = "Balance Due",
                    amount = totalOutstanding,
                    currency = client.currency,
                    amountColor = if (totalOutstanding > 0) StatusPending else MaterialTheme.colorScheme.onSurface,
                    subtitle = if (overdueCount > 0) "$overdueCount overdue" else "current",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Navigation with Badges
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = OceanBlue
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Contact Info",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("client_modal_tab_contact")
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        BadgedBox(
                            badge = {
                                if (clientInvoices.isNotEmpty()) {
                                    Badge(
                                        containerColor = if (overdueCount > 0) StatusOverdue else OceanBlue,
                                        contentColor = Color.White
                                    ) {
                                        Text("${clientInvoices.size}")
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Billing History",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    },
                    icon = {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("client_modal_tab_billing")
                )

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        BadgedBox(
                            badge = {
                                if (clientSubscriptions.isNotEmpty()) {
                                    Badge(
                                        containerColor = AccentGold,
                                        contentColor = Color.Black
                                    ) {
                                        Text("${clientSubscriptions.size}")
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "Subscriptions",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    },
                    icon = {
                        Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("client_modal_tab_subscriptions")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: Contact Information & Business Details
                    ClientContactInfoSection(
                        client = client,
                        context = context
                    )
                }
                1 -> {
                    // TAB 1: Specific Billing History
                    ClientBillingHistorySection(
                        client = client,
                        invoices = clientInvoices,
                        currentFilter = invoiceStatusFilter,
                        onFilterChange = { invoiceStatusFilter = it },
                        onViewInvoiceDetail = onViewInvoiceDetail,
                        onMarkInvoiceAsPaid = onMarkInvoiceAsPaid,
                        onExportPdf = { invoiceToExportPdf = it },
                        onCreateInvoice = { onCreateInvoiceForClient(client) }
                    )
                }
                2 -> {
                    // TAB 2: Active Subscriptions & Retainers
                    ClientSubscriptionsSection(
                        client = client,
                        subscriptions = clientSubscriptions,
                        monthlyRetainerRevenue = monthlyRetainerRevenue,
                        activeCount = activeSubscriptionsCount,
                        onBillSubscriptionNow = onBillSubscriptionNow,
                        onCreateInvoice = { onCreateInvoiceForClient(client) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            // Primary Bottom Action Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }

                Button(
                    onClick = {
                        onDismiss()
                        onCreateInvoiceForClient(client)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("client_modal_new_invoice_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Invoice", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // PDF Export Dialog if requested from billing history
    invoiceToExportPdf?.let { inv ->
        ExportPdfDialog(
            invoice = inv,
            onDismissRequest = { invoiceToExportPdf = null }
        )
    }
}

/**
 * KPI Summary Card for Client Modal
 */
@Composable
private fun ClientKpiCard(
    title: String,
    amount: Double,
    currency: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    amountColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Formatters.formatCurrency(amount, currency),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Tab 0: Comprehensive Contact Information Section
 */
@Composable
private fun ClientContactInfoSection(
    client: ClientEntity,
    context: Context
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Contact Person Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "PRIMARY BILLING CONTACT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OceanBlue,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = OceanBlue.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = OceanBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = client.contactPerson.ifBlank { "Primary Store Account Representative" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Commercial & Billing Contact",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                // Email Row with Send Email & Copy actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Email", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = client.email.ifBlank { "No email registered" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (client.email.isNotBlank()) {
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Client Email", client.email))
                                    Toast.makeText(context, "Copied ${client.email}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Email", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${client.email}")
                                        putExtra(Intent.EXTRA_SUBJECT, "Oistars Billings - ${client.name}")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = "Send Email", tint = OceanBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Row with Call & Copy actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Phone", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = client.phone.ifBlank { "No phone registered" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (client.phone.isNotBlank()) {
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Client Phone", client.phone))
                                    Toast.makeText(context, "Copied ${client.phone}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Phone", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${client.phone}")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Unable to dial phone", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", tint = OceanBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Business & Tax Credentials Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "BUSINESS CREDENTIALS & FISCAL DATA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OceanBlue,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Tax / VAT ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("VAT / Tax Identification", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = client.taxNumber.ifBlank { "Not provided (Domestic / exempt)" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (client.taxNumber.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("VAT Number", client.taxNumber))
                                Toast.makeText(context, "Copied VAT ID", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy VAT", modifier = Modifier.size(15.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                // Address
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Registered Business Address", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (client.address.isNotBlank()) "${client.address}, ${client.country}" else client.country.ifBlank { "Europe" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                // Commercial Terms & Currency
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Payment Terms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Net ${client.paymentTermsDays} Days", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("Invoicing Currency", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(client.currency, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("Client Since", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Formatters.formatShortDate(client.createdAt), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (client.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ACCOUNT MEMO & NOTES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = client.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

/**
 * Tab 1: Comprehensive Specific Billing History
 */
@Composable
private fun ClientBillingHistorySection(
    client: ClientEntity,
    invoices: List<InvoiceEntity>,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    onViewInvoiceDetail: ((InvoiceEntity) -> Unit)?,
    onMarkInvoiceAsPaid: ((InvoiceEntity) -> Unit)?,
    onExportPdf: (InvoiceEntity) -> Unit,
    onCreateInvoice: () -> Unit
) {
    val filteredInvoices = remember(invoices, currentFilter) {
        when (currentFilter) {
            "PAID" -> invoices.filter { it.invoiceStatus == InvoiceStatus.PAID }
            "PENDING" -> invoices.filter { it.invoiceStatus == InvoiceStatus.PENDING }
            "OVERDUE" -> invoices.filter { it.invoiceStatus == InvoiceStatus.OVERDUE }
            else -> invoices
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = currentFilter == "ALL",
                onClick = { onFilterChange("ALL") },
                label = { Text("All (${invoices.size})", fontSize = 11.sp) }
            )

            FilterChip(
                selected = currentFilter == "PENDING",
                onClick = { onFilterChange("PENDING") },
                label = {
                    val count = invoices.count { it.invoiceStatus == InvoiceStatus.PENDING }
                    Text("Pending ($count)", fontSize = 11.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusPendingBg,
                    selectedLabelColor = StatusPending
                )
            )

            FilterChip(
                selected = currentFilter == "OVERDUE",
                onClick = { onFilterChange("OVERDUE") },
                label = {
                    val count = invoices.count { it.invoiceStatus == InvoiceStatus.OVERDUE }
                    Text("Overdue ($count)", fontSize = 11.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusOverdueBg,
                    selectedLabelColor = StatusOverdue
                )
            )

            FilterChip(
                selected = currentFilter == "PAID",
                onClick = { onFilterChange("PAID") },
                label = {
                    val count = invoices.count { it.invoiceStatus == InvoiceStatus.PAID }
                    Text("Settled ($count)", fontSize = 11.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusPaidBg,
                    selectedLabelColor = StatusPaid
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredInvoices.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (invoices.isEmpty()) "No billing history for ${client.name}" else "No invoices matching '$currentFilter'",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Create an invoice to bill this client for services, products, or subscriptions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onCreateInvoice,
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Create Invoice", fontSize = 12.sp)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredInvoices.forEach { invoice ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewInvoiceDetail?.invoke(invoice) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Invoice ID & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = invoice.id,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = OceanBlue
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${Formatters.formatShortDate(invoice.issueDate)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(status = invoice.invoiceStatus)
                            }

                            // Items preview
                            val itemsSummary = invoice.items.firstOrNull()?.description
                            if (!itemsSummary.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (invoice.items.size > 1) "$itemsSummary (+${invoice.items.size - 1} more items)" else itemsSummary,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Amounts & Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = Formatters.formatCurrency(invoice.totalAmount, invoice.currency),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    if (invoice.balanceDue > 0 && invoice.invoiceStatus != InvoiceStatus.DRAFT) {
                                        Text(
                                            text = "Due ${Formatters.formatShortDate(invoice.dueDate)} (${Formatters.formatCurrency(invoice.balanceDue, invoice.currency)} left)",
                                            fontSize = 11.sp,
                                            color = if (invoice.invoiceStatus == InvoiceStatus.OVERDUE) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (invoice.invoiceStatus == InvoiceStatus.OVERDUE) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onExportPdf(invoice) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = "PDF",
                                            tint = OceanBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (invoice.invoiceStatus == InvoiceStatus.PENDING || invoice.invoiceStatus == InvoiceStatus.OVERDUE) {
                                        onMarkInvoiceAsPaid?.let { markPaid ->
                                            TextButton(
                                                onClick = { markPaid(invoice) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = StatusPaid,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mark Paid", fontSize = 11.sp, color = StatusPaid)
                                            }
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ArrowForwardIos,
                                        contentDescription = "Details",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Active Subscriptions & Recurring Retainers
 */
@Composable
private fun ClientSubscriptionsSection(
    client: ClientEntity,
    subscriptions: List<SubscriptionEntity>,
    monthlyRetainerRevenue: Double,
    activeCount: Int,
    onBillSubscriptionNow: ((SubscriptionEntity) -> Unit)?,
    onCreateInvoice: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Subscriptions Overview Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RECURRING MRR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanBlue,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = Formatters.formatCurrency(monthlyRetainerRevenue, client.currency) + " / month",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OceanBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$activeCount active retainer(s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (subscriptions.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No subscriptions configured for ${client.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Set up recurring retainers in the Retainers tab to automate monthly client billing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subscriptions.forEach { sub ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.planName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${sub.billingInterval.label} Retainer Plan",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (sub.subscriptionStatus == SubscriptionStatus.ACTIVE) StatusPaidBg else StatusDraftBg
                                ) {
                                    Text(
                                        text = sub.subscriptionStatus.name,
                                        color = if (sub.subscriptionStatus == SubscriptionStatus.ACTIVE) StatusPaid else StatusDraft,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${Formatters.formatCurrency(sub.amount, sub.currency)} / ${sub.billingInterval.label.lowercase()}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanBlue
                                    )
                                    Text(
                                        text = "Next Renewal: ${Formatters.formatShortDate(sub.nextBillingDate)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (sub.subscriptionStatus == SubscriptionStatus.ACTIVE) {
                                    onBillSubscriptionNow?.let { billNow ->
                                        OutlinedButton(
                                            onClick = { billNow(sub) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Receipt,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Bill Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (sub.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Terms: ${sub.notes}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
