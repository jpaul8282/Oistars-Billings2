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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.model.InvoiceItem
import com.example.ui.components.Formatters
import com.example.ui.components.MetricStatCard
import com.example.ui.components.QuickCreateInvoiceDialog
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import com.example.ui.theme.StatusPending
import com.example.ui.viewmodel.FinancialMetrics

@Composable
fun DashboardScreen(
    metrics: FinancialMetrics,
    recentInvoices: List<InvoiceEntity>,
    activeSubscriptions: List<SubscriptionEntity>,
    clients: List<ClientEntity>,
    onNavigateToInvoices: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onMarkAsPaid: (InvoiceEntity) -> Unit,
    onRecordPayment: (InvoiceEntity, Double, String, String, String) -> Unit,
    onDeleteInvoice: (InvoiceEntity) -> Unit,
    onGenerateFromSubscription: (SubscriptionEntity) -> Unit,
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
    var showQuickCreateDialog by remember { mutableStateOf(false) }
    var showCreateInvoiceSheet by remember { mutableStateOf(false) }
    var selectedInvoiceForDetail by remember { mutableStateOf<InvoiceEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Platform Hero Banner with Primary Financial Metrics
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "OISTARS BILLINGS PLATFORM",
                                color = AccentGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Financial Overview",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusPaid)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${metrics.collectionRatePercent}% Collected",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Total Invoiced Revenue",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = Formatters.formatCurrency(metrics.totalInvoiced),
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("dashboard_total_revenue_text")
                            )
                        }

                        Button(
                            onClick = { showQuickCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("dashboard_new_invoice_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Invoice", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cash Flow Visual Health Bar
            val total = (metrics.totalPaid + metrics.totalOutstanding).coerceAtLeast(1.0)
            val paidFraction = (metrics.totalPaid / total).toFloat().coerceIn(0f, 1f)
            val overdueFraction = (metrics.totalOverdue / total).toFloat().coerceIn(0f, 1f)
            val pendingFraction = ((metrics.totalOutstanding - metrics.totalOverdue).coerceAtLeast(0.0) / total).toFloat().coerceIn(0f, 1f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Cash Flow Health",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-color Progress bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (paidFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(paidFraction.coerceAtLeast(0.01f))
                                    .height(10.dp)
                                    .background(StatusPaid)
                            )
                        }
                        if (pendingFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(pendingFraction.coerceAtLeast(0.01f))
                                    .height(10.dp)
                                    .background(StatusPending)
                            )
                        }
                        if (overdueFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(overdueFraction.coerceAtLeast(0.01f))
                                    .height(10.dp)
                                    .background(StatusOverdue)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusPaid))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paid: ${Formatters.formatCurrency(metrics.totalPaid)}", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusPending))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pending: ${Formatters.formatCurrency(metrics.totalOutstanding - metrics.totalOverdue)}", fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusOverdue))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Overdue: ${Formatters.formatCurrency(metrics.totalOverdue)}", fontSize = 11.sp, color = StatusOverdue)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metric Stat Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Revenue Collected",
                    amount = metrics.totalPaid,
                    icon = Icons.Default.CheckCircle,
                    iconColor = StatusPaid,
                    subtitle = "${metrics.paidInvoicesCount} invoices settled",
                    modifier = Modifier.weight(1f).testTag("dashboard_stat_revenue_collected")
                )

                MetricStatCard(
                    title = "Outstanding Balance",
                    amount = metrics.totalOutstanding,
                    icon = Icons.Default.HourglassEmpty,
                    iconColor = StatusPending,
                    subtitle = "${metrics.pendingInvoicesCount + metrics.overdueInvoicesCount} awaiting payment",
                    modifier = Modifier.weight(1f).testTag("dashboard_stat_outstanding")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Monthly MRR",
                    amount = metrics.monthlyRecurringRevenue,
                    icon = Icons.Default.Autorenew,
                    iconColor = OceanBlue,
                    subtitle = "${metrics.activeSubscriptionsCount} active subscriptions",
                    modifier = Modifier.weight(1f).testTag("dashboard_stat_mrr")
                )

                MetricStatCard(
                    title = "Overdue Balances",
                    amount = metrics.totalOverdue,
                    icon = Icons.Default.Warning,
                    iconColor = StatusOverdue,
                    subtitle = "${metrics.overdueInvoicesCount} invoices past due",
                    modifier = Modifier.weight(1f).testTag("dashboard_stat_overdue")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateToInvoices,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Invoices", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = onNavigateToClients,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stores (${clients.size})", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = onNavigateToSubscriptions,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retainers", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Active Subscriptions Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Subscriptions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${metrics.activeSubscriptionsCount} active • ${Formatters.formatCurrency(metrics.monthlyRecurringRevenue)}/mo MRR",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onNavigateToSubscriptions) {
                    Text("View All (${activeSubscriptions.size})", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (activeSubscriptions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No active subscriptions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Set up recurring retainers for clients to automate monthly billing.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onNavigateToSubscriptions) {
                            Text("Add Recurring Retainer", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                activeSubscriptions.take(3).forEach { sub ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sub.clientName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(sub.planName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Next: ${Formatters.formatShortDate(sub.nextBillingDate)} • ${Formatters.formatCurrency(sub.amount, sub.currency)} / ${sub.billingInterval.label.lowercase()}",
                                    fontSize = 11.sp,
                                    color = OceanBlue
                                )
                            }

                            OutlinedButton(
                                onClick = { onGenerateFromSubscription(sub) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Bill Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recent Invoices Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Invoices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToInvoices) {
                    Text("See All (${recentInvoices.size})", fontSize = 12.sp)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentInvoices.take(4).forEach { invoice ->
                    InvoiceListItemCard(
                        invoice = invoice,
                        onClick = { selectedInvoiceForDetail = invoice },
                        onMarkAsPaid = { onMarkAsPaid(invoice) }
                    )
                }
            }

            // Bottom spacer so content can scroll past the FAB
            Spacer(modifier = Modifier.height(88.dp))
        }

        // Floating Action Button (FAB) to quickly create a new invoice for a client
        ExtendedFloatingActionButton(
            onClick = { showQuickCreateDialog = true },
            icon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            text = {
                Text(
                    text = "New Invoice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            containerColor = AccentGold,
            contentColor = Color.Black,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .testTag("dashboard_create_invoice_fab")
        )
    }

    // Quick Create Invoice Dialog
    if (showQuickCreateDialog) {
        QuickCreateInvoiceDialog(
            clients = clients,
            onDismiss = { showQuickCreateDialog = false },
            onSaveInvoice = { clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency ->
                onSaveInvoice(clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency)
                showQuickCreateDialog = false
            },
            onOpenFullEditor = {
                showQuickCreateDialog = false
                showCreateInvoiceSheet = true
            }
        )
    }

    // Modal sheet for advanced multi-item invoice creation
    if (showCreateInvoiceSheet) {
        CreateInvoiceSheet(
            clients = clients,
            onDismiss = { showCreateInvoiceSheet = false },
            onSaveInvoice = { clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency ->
                onSaveInvoice(clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency)
                showCreateInvoiceSheet = false
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
