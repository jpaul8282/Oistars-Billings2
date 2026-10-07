package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.model.ClientAccountStatus
import com.example.data.model.FirestoreClient
import com.example.ui.components.Formatters
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDashboardScreen(
    userId: String,
    billingOwnerName: String,
    firestoreSyncManager: FirestoreSyncManager,
    localClients: List<ClientEntity> = emptyList(),
    localInvoices: List<InvoiceEntity> = emptyList(),
    onCreateInvoiceForClient: (clientId: String, clientName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var firestoreClients by remember { mutableStateOf<List<FirestoreClient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<ClientAccountStatus?>(null) }

    var showAddClientDialog by remember { mutableStateOf(false) }
    var clientToChangeStatus by remember { mutableStateOf<FirestoreClient?>(null) }
    var clientToDelete by remember { mutableStateOf<FirestoreClient?>(null) }
    var isSyncingLocal by remember { mutableStateOf(false) }
    var statusFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Function to reload clients from Firestore
    fun loadClientsFromFirestore() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = firestoreSyncManager.getClientsFromFirestore(userId)
            result.onSuccess { list ->
                firestoreClients = list
                isLoading = false
                // If Firestore is completely empty and local records exist, auto-seed with proper statuses
                if (list.isEmpty() && localClients.isNotEmpty()) {
                    isSyncingLocal = true
                    firestoreSyncManager.syncClientsToCloud(userId, localClients, localInvoices)
                    val refreshed = firestoreSyncManager.getClientsFromFirestore(userId)
                    refreshed.onSuccess { seededList ->
                        firestoreClients = seededList
                        statusFeedbackMessage = "Synchronized ${seededList.size} clients to Cloud Firestore"
                    }
                    isSyncingLocal = false
                }
            }.onFailure { e ->
                isLoading = false
                errorMessage = e.localizedMessage ?: "Failed to retrieve clients from Firestore"
                // Graceful fallback to local cache if Firestore is unreachable or pending authentication
                if (firestoreClients.isEmpty() && localClients.isNotEmpty()) {
                    firestoreClients = localClients.map { c ->
                        val clientInvoices = localInvoices.filter { it.clientId == c.id }
                        val overdueCount = clientInvoices.count { it.status.equals("OVERDUE", ignoreCase = true) }
                        val pendingCount = clientInvoices.count { it.status.equals("PENDING", ignoreCase = true) }
                        val paidCount = clientInvoices.count { it.status.equals("PAID", ignoreCase = true) }
                        val totalInvoiced = clientInvoices.sumOf { it.totalAmount }
                        val totalPaid = clientInvoices.sumOf { it.amountPaid }
                        val outstanding = (totalInvoiced - totalPaid).coerceAtLeast(0.0)
                        val status = when {
                            overdueCount > 0 -> ClientAccountStatus.OVERDUE
                            pendingCount > 0 -> ClientAccountStatus.PENDING_BALANCE
                            paidCount > 0 -> ClientAccountStatus.GOOD_STANDING
                            else -> ClientAccountStatus.ACTIVE
                        }
                        FirestoreClient(
                            id = c.id,
                            userId = userId,
                            name = c.name,
                            contactPerson = c.contactPerson,
                            email = c.email,
                            phone = c.phone,
                            taxNumber = c.taxNumber,
                            address = c.address,
                            country = c.country,
                            currency = c.currency,
                            paymentTermsDays = c.paymentTermsDays,
                            notes = c.notes,
                            accountStatus = status,
                            totalInvoiced = totalInvoiced,
                            totalPaid = totalPaid,
                            outstandingBalance = outstanding,
                            openInvoicesCount = overdueCount + pendingCount,
                            createdAt = c.createdAt
                        )
                    }
                    statusFeedbackMessage = "Loaded ${localClients.size} clients from local cache (Cloud sync pending)"
                }
            }
        }
    }

    // Real-time snapshot listener from Firestore
    LaunchedEffect(userId) {
        loadClientsFromFirestore()
    }

    // Filtered clients list
    val filteredClients = firestoreClients.filter { client ->
        val matchesStatus = selectedStatusFilter == null || client.accountStatus == selectedStatusFilter
        val matchesSearch = if (searchQuery.isBlank()) true else {
            val q = searchQuery.trim()
            client.name.contains(q, ignoreCase = true) ||
            client.contactPerson.contains(q, ignoreCase = true) ||
            client.email.contains(q, ignoreCase = true) ||
            client.id.contains(q, ignoreCase = true) ||
            client.country.contains(q, ignoreCase = true)
        }
        matchesStatus && matchesSearch
    }

    // Metric counts
    val totalCount = firestoreClients.size
    val goodStandingCount = firestoreClients.count { it.accountStatus == ClientAccountStatus.GOOD_STANDING }
    val activeCount = firestoreClients.count { it.accountStatus == ClientAccountStatus.ACTIVE }
    val pendingBalanceCount = firestoreClients.count { it.accountStatus == ClientAccountStatus.PENDING_BALANCE }
    val overdueCount = firestoreClients.count { it.accountStatus == ClientAccountStatus.OVERDUE }
    val inactiveCount = firestoreClients.count { it.accountStatus == ClientAccountStatus.INACTIVE }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Dashboard Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_dashboard_header"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Firestore Client Dashboard",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lead: $billingOwnerName • Region: europe-west2",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { loadClientsFromFirestore() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("refresh_firestore_clients_btn")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = AccentGold,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh from Firestore",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncingLocal = true
                                        val result = firestoreSyncManager.syncClientsToCloud(userId, localClients, localInvoices)
                                        result.onSuccess {
                                            statusFeedbackMessage = "Synced $it stores to Firestore"
                                            loadClientsFromFirestore()
                                        }.onFailure {
                                            statusFeedbackMessage = "Sync failed: ${it.localizedMessage}"
                                        }
                                        isSyncingLocal = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("sync_to_firestore_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSyncingLocal) "Syncing..." else "Sync Cloud",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cloud Database ID details chip
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = StatusPaid,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DB: ai-studio-android-oistarsb-ae8638b7 • Collection: /users/$userId/clients",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Summary Cards Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClientMetricCard(
                    title = "Total Clients",
                    count = totalCount,
                    isSelected = selectedStatusFilter == null,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    accentColor = OceanBlue,
                    onClick = { selectedStatusFilter = null }
                )
                ClientMetricCard(
                    title = "Good Standing",
                    count = goodStandingCount,
                    isSelected = selectedStatusFilter == ClientAccountStatus.GOOD_STANDING,
                    containerColor = StatusPaidBg,
                    accentColor = StatusPaid,
                    onClick = { selectedStatusFilter = ClientAccountStatus.GOOD_STANDING }
                )
                ClientMetricCard(
                    title = "Active",
                    count = activeCount,
                    isSelected = selectedStatusFilter == ClientAccountStatus.ACTIVE,
                    containerColor = OceanBlue.copy(alpha = 0.15f),
                    accentColor = OceanBlue,
                    onClick = { selectedStatusFilter = ClientAccountStatus.ACTIVE }
                )
                ClientMetricCard(
                    title = "Pending Balance",
                    count = pendingBalanceCount,
                    isSelected = selectedStatusFilter == ClientAccountStatus.PENDING_BALANCE,
                    containerColor = StatusPendingBg,
                    accentColor = StatusPending,
                    onClick = { selectedStatusFilter = ClientAccountStatus.PENDING_BALANCE }
                )
                ClientMetricCard(
                    title = "Overdue",
                    count = overdueCount,
                    isSelected = selectedStatusFilter == ClientAccountStatus.OVERDUE,
                    containerColor = StatusOverdueBg,
                    accentColor = StatusOverdue,
                    onClick = { selectedStatusFilter = ClientAccountStatus.OVERDUE }
                )
                ClientMetricCard(
                    title = "Inactive",
                    count = inactiveCount,
                    isSelected = selectedStatusFilter == ClientAccountStatus.INACTIVE,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    accentColor = StatusDraft,
                    onClick = { selectedStatusFilter = ClientAccountStatus.INACTIVE }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar & Add Client Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, status, or email...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("firestore_client_search_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showAddClientDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("add_firestore_client_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status message toast banner
            AnimatedVisibility(visible = statusFeedbackMessage != null) {
                Surface(
                    color = OceanBlue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = statusFeedbackMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = OceanBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { statusFeedbackMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Client List Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Clients in Firestore (${filteredClients.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (selectedStatusFilter != null) {
                    TextButton(onClick = { selectedStatusFilter = null }) {
                        Text("Clear Filter", fontSize = 12.sp, color = OceanBlue)
                    }
                }
            }

            // Client Cards List
            if (isLoading && firestoreClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = OceanBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Retrieving clients from Cloud Firestore...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (errorMessage != null && firestoreClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusOverdue,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Failed to load Firestore clients",
                            fontWeight = FontWeight.Bold,
                            color = StatusOverdue
                        )
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadClientsFromFirestore() },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                        ) {
                            Text("Retry Connection")
                        }
                    }
                }
            } else if (filteredClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedStatusFilter != null)
                                    "No clients match the active filter"
                                else
                                    "No client documents in Firestore",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sync your local records or add a new client to populate the cloud registry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSyncingLocal = true
                                        firestoreSyncManager.syncClientsToCloud(userId, localClients, localInvoices)
                                        loadClientsFromFirestore()
                                        isSyncingLocal = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Local Ledger to Cloud")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        FirestoreClientItemCard(
                            client = client,
                            onUpdateStatus = { clientToChangeStatus = client },
                            onCreateInvoice = { onCreateInvoiceForClient(client.id, client.name) },
                            onDelete = { clientToDelete = client }
                        )
                    }
                }
            }
        }
    }

    // Add Client to Firestore Dialog
    if (showAddClientDialog) {
        AddFirestoreClientDialog(
            userId = userId,
            onDismiss = { showAddClientDialog = false },
            onClientAdded = { newClient ->
                coroutineScope.launch {
                    val result = firestoreSyncManager.saveClientToFirestore(userId, newClient)
                    result.onSuccess {
                        statusFeedbackMessage = "Added '${newClient.name}' to Firestore with status ${newClient.accountStatus.label}"
                        loadClientsFromFirestore()
                    }.onFailure {
                        statusFeedbackMessage = "Error adding client: ${it.localizedMessage}"
                    }
                    showAddClientDialog = false
                }
            }
        )
    }

    // Update Client Status Dialog
    clientToChangeStatus?.let { client ->
        UpdateClientStatusDialog(
            client = client,
            onDismiss = { clientToChangeStatus = null },
            onStatusSelected = { newStatus ->
                coroutineScope.launch {
                    val result = firestoreSyncManager.updateClientAccountStatus(userId, client.id, newStatus)
                    result.onSuccess {
                        statusFeedbackMessage = "Updated '${client.name}' status to ${newStatus.label}"
                        loadClientsFromFirestore()
                    }.onFailure {
                        statusFeedbackMessage = "Error updating status: ${it.localizedMessage}"
                    }
                    clientToChangeStatus = null
                }
            }
        )
    }

    // Delete Client Confirmation Dialog
    clientToDelete?.let { client ->
        AlertDialog(
            onDismissRequest = { clientToDelete = null },
            title = { Text("Delete Client from Firestore") },
            text = { Text("Are you sure you want to remove '${client.name}' from your Cloud Firestore collection? This will not delete historical invoices.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = firestoreSyncManager.deleteClientFromFirestore(userId, client.id)
                            result.onSuccess {
                                statusFeedbackMessage = "Deleted '${client.name}' from Firestore"
                                loadClientsFromFirestore()
                            }.onFailure {
                                statusFeedbackMessage = "Error deleting: ${it.localizedMessage}"
                            }
                            clientToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOverdue)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { clientToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ClientMetricCard(
    title: String,
    count: Int,
    isSelected: Boolean,
    containerColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else containerColor,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, accentColor) else null,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = accentColor
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FirestoreClientItemCard(
    client: FirestoreClient,
    onUpdateStatus: () -> Unit,
    onCreateInvoice: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_client_card_${client.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Client Name and Account Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("client_name_${client.id}")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = client.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        if (client.contactPerson.isNotBlank()) {
                            Text(
                                text = " • ${client.contactPerson}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Account Status Badge
                FirestoreStatusBadge(status = client.accountStatus)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Contact & Financial Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    if (client.email.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = client.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    if (client.country.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${client.country} • ${client.currency} (Net ${client.paymentTermsDays}d)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Balance summary
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(0.8f)
                ) {
                    Text(
                        text = "Outstanding",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatCurrency(client.outstandingBalance, client.currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (client.outstandingBalance > 0.0) StatusOverdue else StatusPaid
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Actions row: Change Status, Create Invoice, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onUpdateStatus,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("change_status_btn_${client.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onCreateInvoice,
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("create_invoice_for_${client.id}")
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete client",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FirestoreStatusBadge(status: ClientAccountStatus) {
    val (bgColor, textColor, icon) = when (status) {
        ClientAccountStatus.GOOD_STANDING -> Triple(StatusPaidBg, StatusPaid, Icons.Default.CheckCircle)
        ClientAccountStatus.ACTIVE -> Triple(OceanBlue.copy(alpha = 0.15f), OceanBlue, Icons.Default.Business)
        ClientAccountStatus.PENDING_BALANCE -> Triple(StatusPendingBg, StatusPending, Icons.Default.HourglassEmpty)
        ClientAccountStatus.OVERDUE -> Triple(StatusOverdueBg, StatusOverdue, Icons.Default.Warning)
        ClientAccountStatus.INACTIVE -> Triple(MaterialTheme.colorScheme.surfaceVariant, StatusDraft, Icons.Default.PauseCircle)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFirestoreClientDialog(
    userId: String,
    onDismiss: () -> Unit,
    onClientAdded: (FirestoreClient) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var taxNumber by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Netherlands") }
    var currency by remember { mutableStateOf("EUR") }
    var paymentTermsDays by remember { mutableStateOf("14") }
    var selectedStatus by remember { mutableStateOf(ClientAccountStatus.ACTIVE) }
    var notes by remember { mutableStateOf("") }

    var statusDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Client to Firestore", fontWeight = FontWeight.Bold) },
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
                    label = { Text("Company / Store Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Billing Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Account Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = statusDropdownExpanded,
                    onExpandedChange = { statusDropdownExpanded = !statusDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedStatus.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {
                        ClientAccountStatus.entries.forEach { statusOption ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(statusOption.label, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            statusOption.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedStatus = statusOption
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency") },
                        modifier = Modifier.weight(0.7f)
                    )
                }

                OutlinedTextField(
                    value = paymentTermsDays,
                    onValueChange = { paymentTermsDays = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Terms (Days)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Internal Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val client = FirestoreClient(
                            id = "CLI-${System.currentTimeMillis() % 100000}",
                            userId = userId,
                            name = name.trim(),
                            contactPerson = contactPerson.trim(),
                            email = email.trim(),
                            phone = phone.trim(),
                            taxNumber = taxNumber.trim(),
                            country = country.trim(),
                            currency = currency.trim(),
                            paymentTermsDays = paymentTermsDays.toIntOrNull() ?: 14,
                            accountStatus = selectedStatus,
                            notes = notes.trim(),
                            createdAt = System.currentTimeMillis()
                        )
                        onClientAdded(client)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
            ) {
                Text("Save to Firestore")
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
fun UpdateClientStatusDialog(
    client: FirestoreClient,
    onDismiss: () -> Unit,
    onStatusSelected: (ClientAccountStatus) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Update Account Status", fontWeight = FontWeight.Bold)
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClientAccountStatus.entries.forEach { status ->
                    val isCurrent = client.accountStatus == status
                    Surface(
                        onClick = { onStatusSelected(status) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCurrent) OceanBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, OceanBlue) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FirestoreStatusBadge(status = status)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = status.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = status.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCurrent) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = OceanBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
