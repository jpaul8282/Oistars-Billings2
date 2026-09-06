package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.VerticalDivider
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
import com.example.data.model.InvoiceStatus
import com.example.ui.components.Formatters
import com.example.ui.components.SearchBar
import com.example.ui.components.StatusBadge
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

enum class ClientViewMode {
    TABLE,
    CARDS
}

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
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, OVERDUE, PENDING, PAID
    var viewMode by remember { mutableStateOf(ClientViewMode.TABLE) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }
    var viewingClientDetails by remember { mutableStateOf<ClientEntity?>(null) }

    // Pre-calculate client metrics for filtering and badges
    val clientMetricsMap = remember(clients, invoices) {
        clients.associate { client ->
            val clientInvs = invoices.filter { it.clientId == client.id }
            val paidCount = clientInvs.count { it.invoiceStatus == InvoiceStatus.PAID }
            val pendingCount = clientInvs.count { it.invoiceStatus == InvoiceStatus.PENDING }
            val overdueCount = clientInvs.count { it.invoiceStatus == InvoiceStatus.OVERDUE }
            val totalBilled = clientInvs.sumOf { it.totalAmount }
            val totalOutstanding = clientInvs.sumOf { it.balanceDue }
            client.id to ClientInvoiceStats(
                invoicesCount = clientInvs.size,
                paidCount = paidCount,
                pendingCount = pendingCount,
                overdueCount = overdueCount,
                totalBilled = totalBilled,
                totalOutstanding = totalOutstanding
            )
        }
    }

    val filteredClients = clients.filter { client ->
        val stats = clientMetricsMap[client.id] ?: ClientInvoiceStats()

        val matchesStatus = when (selectedStatusFilter) {
            "OVERDUE" -> stats.overdueCount > 0
            "PENDING" -> stats.pendingCount > 0
            "PAID" -> stats.paidCount > 0 && stats.overdueCount == 0 && stats.pendingCount == 0
            else -> true
        }

        val matchesSearch = if (searchQuery.isBlank()) true else {
            val q = searchQuery.trim()
            client.name.contains(q, ignoreCase = true) ||
            client.id.contains(q, ignoreCase = true) ||
            client.taxNumber.contains(q, ignoreCase = true) ||
            client.contactPerson.contains(q, ignoreCase = true) ||
            client.email.contains(q, ignoreCase = true)
        }

        matchesStatus && matchesSearch
    }

    val overdueClientsCount = clients.count { (clientMetricsMap[it.id]?.overdueCount ?: 0) > 0 }
    val pendingClientsCount = clients.count { (clientMetricsMap[it.id]?.pendingCount ?: 0) > 0 }
    val settledClientsCount = clients.count {
        val s = clientMetricsMap[it.id] ?: ClientInvoiceStats()
        s.paidCount > 0 && s.overdueCount == 0 && s.pendingCount == 0
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Input shown when in Cards View mode (In Table mode, it's integrated at the top of the table)
            if (viewMode == ClientViewMode.CARDS) {
                PaddingValues(horizontal = 16.dp, vertical = 6.dp).let {
                    Box(modifier = Modifier.padding(it)) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Filter by name or company ID (e.g. CLI-, NL)..."
                        )
                    }
                }
            }

            // Controls Bar: Filter Chips & View Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Filter Chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == "ALL",
                        onClick = { selectedStatusFilter = "ALL" },
                        label = { Text("All (${clients.size})", fontSize = 12.sp) }
                    )

                    FilterChip(
                        selected = selectedStatusFilter == "OVERDUE",
                        onClick = { selectedStatusFilter = "OVERDUE" },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(StatusOverdue))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Overdue ($overdueClientsCount)", fontSize = 12.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusOverdueBg,
                            selectedLabelColor = StatusOverdue
                        )
                    )

                    FilterChip(
                        selected = selectedStatusFilter == "PENDING",
                        onClick = { selectedStatusFilter = "PENDING" },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(StatusPending))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pending ($pendingClientsCount)", fontSize = 12.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusPendingBg,
                            selectedLabelColor = StatusPending
                        )
                    )

                    FilterChip(
                        selected = selectedStatusFilter == "PAID",
                        onClick = { selectedStatusFilter = "PAID" },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(StatusPaid))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Settled ($settledClientsCount)", fontSize = 12.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusPaidBg,
                            selectedLabelColor = StatusPaid
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // View Mode Toggle Button: Table vs Cards
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        IconButton(
                            onClick = { viewMode = ClientViewMode.TABLE },
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (viewMode == ClientViewMode.TABLE)
                                        Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    else Modifier
                                )
                                .testTag("client_view_mode_table")
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = "Table View",
                                tint = if (viewMode == ClientViewMode.TABLE) OceanBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewMode = ClientViewMode.CARDS },
                            modifier = Modifier
                                .size(32.dp)
                                .then(
                                    if (viewMode == ClientViewMode.CARDS)
                                        Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                    else Modifier
                                )
                                .testTag("client_view_mode_cards")
                        ) {
                            Icon(
                                Icons.Default.ViewAgenda,
                                contentDescription = "Cards View",
                                tint = if (viewMode == ClientViewMode.CARDS) OceanBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Client Content Area
            if (viewMode == ClientViewMode.TABLE) {
                // Full Table Component with integrated Search Bar at the top of the table
                ClientTableComponent(
                    clients = filteredClients,
                    totalClientsCount = clients.size,
                    clientMetricsMap = clientMetricsMap,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onClickClient = { viewingClientDetails = it },
                    onEditClient = { editingClient = it },
                    onCreateInvoice = { onCreateInvoiceForClient(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            } else {
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
                                text = if (searchQuery.isNotBlank() || selectedStatusFilter != "ALL")
                                    "No client stores match \"$searchQuery\""
                                else "No client stores registered",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotBlank()) {
                                TextButton(onClick = { searchQuery = "" }) {
                                    Text("Clear Search")
                                }
                            } else {
                                TextButton(onClick = { showAddDialog = true }) {
                                    Text("+ Add Client Store")
                                }
                            }
                        }
                    }
                } else {
                    // Cards View with color badges
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredClients, key = { it.id }) { client ->
                            val stats = clientMetricsMap[client.id] ?: ClientInvoiceStats()

                            ClientItemCard(
                                client = client,
                                stats = stats,
                                onClick = { viewingClientDetails = client },
                                onEdit = { editingClient = client },
                                onCreateInvoice = { onCreateInvoiceForClient(client) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
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
                Icon(Icons.Default.Add, contentDescription = "Add Store")
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
        val stats = clientMetricsMap[client.id] ?: ClientInvoiceStats()
        ClientDetailDialog(
            client = client,
            stats = stats,
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

data class ClientInvoiceStats(
    val invoicesCount: Int = 0,
    val paidCount: Int = 0,
    val pendingCount: Int = 0,
    val overdueCount: Int = 0,
    val totalBilled: Double = 0.0,
    val totalOutstanding: Double = 0.0
)

/**
 * Color-coded status badges component for client invoice tracking:
 * - Paid: Emerald Green badge
 * - Pending: Amber badge
 * - Overdue: Crimson Red badge
 */
@Composable
fun ClientInvoiceStatusBadges(
    paidCount: Int,
    pendingCount: Int,
    overdueCount: Int,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Paid Badge (Green)
        Surface(
            color = if (paidCount > 0) StatusPaidBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Paid",
                    tint = if (paidCount > 0) StatusPaid else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (compact) "$paidCount" else "$paidCount Paid",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (paidCount > 0) StatusPaid else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Pending Badge (Amber)
        Surface(
            color = if (pendingCount > 0) StatusPendingBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Pending",
                    tint = if (pendingCount > 0) StatusPending else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (compact) "$pendingCount" else "$pendingCount Pending",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pendingCount > 0) StatusPending else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Overdue Badge (Crimson Red)
        Surface(
            color = if (overdueCount > 0) StatusOverdueBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Overdue",
                    tint = if (overdueCount > 0) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (compact) "$overdueCount" else "$overdueCount Overdue",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (overdueCount > 0) StatusOverdue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Consolidated account-level status pill: OVERDUE, PENDING, ALL SETTLED, NEW
 */
@Composable
fun ClientHealthPill(
    overdueCount: Int,
    pendingCount: Int,
    paidCount: Int,
    modifier: Modifier = Modifier
) {
    val (label, bgColor, textColor) = when {
        overdueCount > 0 -> Triple("OVERDUE ($overdueCount)", StatusOverdueBg, StatusOverdue)
        pendingCount > 0 -> Triple("PENDING ($pendingCount)", StatusPendingBg, StatusPending)
        paidCount > 0 -> Triple("ALL SETTLED", StatusPaidBg, StatusPaid)
        else -> Triple("NEW STORE", StatusDraftBg, StatusDraft)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * High-density Client Table Component with integrated top search bar and explicit color-coded status badges.
 */
@Composable
fun ClientTableComponent(
    clients: List<ClientEntity>,
    totalClientsCount: Int,
    clientMetricsMap: Map<String, ClientInvoiceStats>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClickClient: (ClientEntity) -> Unit,
    onEditClient: (ClientEntity) -> Unit,
    onCreateInvoice: (ClientEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("client_table_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar at the top of the client table
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = "Filter by name or company ID (e.g. CLI-, NL)...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = OceanBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("client_table_search_bar")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank())
                                "Showing all $totalClientsCount client stores"
                            else
                                "Found ${clients.size} of $totalClientsCount stores matching \"$searchQuery\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (searchQuery.isNotBlank()) {
                            TextButton(
                                onClick = { onSearchQueryChange("") },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Clear Filter", fontSize = 11.sp, color = OceanBlue)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Table Header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STORE / COMPANY ID",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "STATUS BADGES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "BALANCE DUE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Table Body or Empty State
            if (clients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "No clients match \"$searchQuery\"",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Search checks client names and company IDs (e.g. CLI-1001)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(onClick = { onSearchQueryChange("") }) {
                            Text("Reset Search")
                        }
                    }
                }
            } else {
                // Table Rows
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(clients, key = { it.id }) { client ->
                        val stats = clientMetricsMap[client.id] ?: ClientInvoiceStats()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickClient(client) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("client_table_row_${client.id}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Col 1: Store & Company ID / VAT
                            Column(modifier = Modifier.weight(1.4f)) {
                                Text(
                                    text = client.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        color = OceanBlue.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = client.id,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OceanBlue,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = if (client.taxNumber.isNotBlank()) client.taxNumber else client.country,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Col 2: Color-Coded Status Badges (Paid, Pending, Overdue)
                            Column(modifier = Modifier.weight(1.4f)) {
                                ClientInvoiceStatusBadges(
                                    paidCount = stats.paidCount,
                                    pendingCount = stats.pendingCount,
                                    overdueCount = stats.overdueCount,
                                    compact = true
                                )
                            }

                            // Col 3: Balance & Quick View
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = if (stats.totalOutstanding > 0)
                                        Formatters.formatCurrency(stats.totalOutstanding, client.currency)
                                    else "Settled",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stats.overdueCount > 0) StatusOverdue
                                    else if (stats.totalOutstanding > 0) StatusPending
                                    else StatusPaid
                                )
                                Text(
                                    text = "Billed: ${Formatters.formatCurrency(stats.totalBilled, client.currency)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}

@Composable
fun ClientItemCard(
    client: ClientEntity,
    stats: ClientInvoiceStats,
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
            // Top Row: Store Name, Health Pill & Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            tint = OceanBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = client.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = OceanBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = client.id,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OceanBlue,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (client.contactPerson.isNotBlank()) {
                            Text(
                                text = "${client.contactPerson} • ${client.country}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClientHealthPill(
                        overdueCount = stats.overdueCount,
                        pendingCount = stats.pendingCount,
                        paidCount = stats.paidCount
                    )

                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Client",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Color-Coded Status Badges for Scannability
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invoices:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ClientInvoiceStatusBadges(
                        paidCount = stats.paidCount,
                        pendingCount = stats.pendingCount,
                        overdueCount = stats.overdueCount
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Financial Metrics & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Billed: ${Formatters.formatCurrency(stats.totalBilled, client.currency)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (stats.totalOutstanding > 0) {
                        Text(
                            text = "Outstanding: ${Formatters.formatCurrency(stats.totalOutstanding, client.currency)}",
                            fontSize = 11.sp,
                            color = if (stats.overdueCount > 0) StatusOverdue else StatusPending,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "${stats.invoicesCount} invoices settled in full",
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
    stats: ClientInvoiceStats,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = client.name, fontWeight = FontWeight.Bold)
                    ClientHealthPill(
                        overdueCount = stats.overdueCount,
                        pendingCount = stats.pendingCount,
                        paidCount = stats.paidCount
                    )
                }
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
                // Color-Coded Status Badges Banner in Detail Dialog
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ledger Status:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ClientInvoiceStatusBadges(
                            paidCount = stats.paidCount,
                            pendingCount = stats.pendingCount,
                            overdueCount = stats.overdueCount
                        )
                    }
                }

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

                if (invoices.isEmpty()) {
                    Text(
                        text = "No invoices issued for this client yet.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    invoices.forEach { inv ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = inv.id,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = Formatters.formatShortDate(inv.issueDate),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = Formatters.formatCurrency(inv.totalAmount, inv.currency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(status = inv.invoiceStatus)
                            }
                        }
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
