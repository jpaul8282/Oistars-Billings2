package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Composable screen that displays a list of clients retrieved from Firestore,
 * using Material3 cards to show each client's name and contact email, along with
 * direct contact actions, status indicators, and financial overview.
 */
@Composable
fun FirestoreClientsScreen(
    userId: String,
    firestoreSyncManager: FirestoreSyncManager,
    onClientSelected: ((FirestoreClient) -> Unit)? = null,
    onCreateInvoiceForClient: ((clientId: String, clientName: String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    fallbackClients: List<ClientEntity> = emptyList(),
    fallbackInvoices: List<InvoiceEntity> = emptyList()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var clientsList by remember { mutableStateOf<List<FirestoreClient>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isRealtimeActive by remember { mutableStateOf(false) }
    var showAddClientDialog by remember { mutableStateOf(false) }

    fun refreshClients() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            val result = firestoreSyncManager.getClientsFromFirestore(userId)
            result.onSuccess { clients ->
                if (clients.isNotEmpty()) {
                    clientsList = clients
                    isLoading = false
                } else if (fallbackClients.isNotEmpty()) {
                    // Pre-populate cloud store from local ledger if first-time user
                    firestoreSyncManager.syncClientsToCloud(userId, fallbackClients, fallbackInvoices)
                    val refreshed = firestoreSyncManager.getClientsFromFirestore(userId)
                    refreshed.onSuccess { synced ->
                        clientsList = synced
                    }
                    isLoading = false
                } else {
                    clientsList = emptyList()
                    isLoading = false
                }
            }.onFailure { ex ->
                isLoading = false
                errorMessage = ex.localizedMessage ?: "Failed to retrieve clients from Firestore"
                // Fallback gracefully to offline seed if firestore is unreachable
                if (clientsList.isEmpty() && fallbackClients.isNotEmpty()) {
                    clientsList = fallbackClients.map { c ->
                        FirestoreClient(
                            id = c.id,
                            userId = userId,
                            name = c.name,
                            contactPerson = c.contactPerson,
                            email = c.email,
                            phone = c.phone,
                            country = c.country,
                            currency = c.currency,
                            notes = c.notes,
                            accountStatus = ClientAccountStatus.ACTIVE
                        )
                    }
                }
            }
        }
    }

    // Subscribe to real-time snapshot flow from Firestore
    LaunchedEffect(userId) {
        isLoading = true
        firestoreSyncManager.observeClientsFromFirestore(userId)
            .catch { ex ->
                errorMessage = ex.localizedMessage
                isLoading = false
                refreshClients()
            }
            .collect { updatedList ->
                isLoading = false
                isRealtimeActive = true
                if (updatedList.isNotEmpty()) {
                    clientsList = updatedList
                    errorMessage = null
                } else {
                    refreshClients()
                }
            }
    }

    val filteredClients = remember(clientsList, searchQuery) {
        if (searchQuery.isBlank()) {
            clientsList
        } else {
            val q = searchQuery.trim().lowercase()
            clientsList.filter {
                it.name.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.contactPerson.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("firestore_clients_screen")
    ) {
        // Top Header Banner
        Surface(
            color = NavyDark,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
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
                                .background(OceanBlue.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Firestore Cloud Clients",
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Firestore Clients",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isRealtimeActive) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = AccentGold.copy(alpha = 0.9f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRealtimeActive) "Live Cloud Sync Active" else "Connected to Firestore",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick Add Client Button in Header
                        Button(
                            onClick = { showAddClientDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = NavyDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_header_add_client")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New Client",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Refresh Button
                        IconButton(
                            onClick = { refreshClients() },
                            modifier = Modifier.testTag("btn_refresh_firestore_clients")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Clients",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search clients by name or contact email...",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firestore_client_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Error message banner if any
        AnimatedVisibility(visible = errorMessage != null) {
            Surface(
                color = StatusOverdue.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusOverdue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = StatusOverdue,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(onClick = { refreshClients() }) {
                        Text("Retry", fontSize = 12.sp, color = OceanBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when {
                isLoading && clientsList.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = OceanBlue,
                            modifier = Modifier.testTag("firestore_clients_loading")
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Fetching client directory from Firestore...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                filteredClients.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No clients match \"$searchQuery\"" else "No clients found in Firestore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching with another name or email." else "Synchronize your client ledger to view clients here.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (fallbackClients.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLoading = true
                                        firestoreSyncManager.syncClientsToCloud(userId, fallbackClients, fallbackInvoices)
                                        refreshClients()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                                modifier = Modifier.testTag("btn_sync_fallback_clients")
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync ${fallbackClients.size} Local Clients to Firestore")
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("firestore_clients_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CLIENTS (${filteredClients.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Source: Firestore",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OceanBlue
                                )
                            }
                        }

                        items(filteredClients, key = { it.id }) { client ->
                            FirestoreClientCard(
                                client = client,
                                onClick = { onClientSelected?.invoke(client) },
                                onCreateInvoice = {
                                    onCreateInvoiceForClient?.invoke(client.id, client.name)
                                }
                            )
                        }
                    }
                }
            }

            // Floating Action Button to Add New Client to Firestore
            FloatingActionButton(
                onClick = { showAddClientDialog = true },
                containerColor = OceanBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_add_firestore_client")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Client to Firestore"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Client",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Modal Form Dialog for adding a client to the Firestore collection
    if (showAddClientDialog) {
        AddFirestoreClientDialog(
            userId = userId,
            firestoreSyncManager = firestoreSyncManager,
            onDismiss = { showAddClientDialog = false },
            onClientSaved = { newClient ->
                showAddClientDialog = false
                refreshClients()
            }
        )
    }
}

/**
 * Material3 Card component designed to display a client retrieved from Firestore,
 * showcasing their Name, Contact Email, ID, and quick interaction buttons.
 */
@Composable
fun FirestoreClientCard(
    client: FirestoreClient,
    onClick: () -> Unit,
    onCreateInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("client_card_${client.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Avatar, Client Name, and Account Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Business Avatar",
                            tint = OceanBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = client.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("client_name_text_${client.id}")
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = OceanBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = client.id,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OceanBlue,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }

                            if (client.contactPerson.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = client.contactPerson,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Account Status Pill
                FirestoreStatusBadge(status = client.accountStatus)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact Email Card Highlight
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(OceanBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email icon",
                                tint = OceanBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Contact Email",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (client.email.isNotBlank()) client.email else "No email registered",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (client.email.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("client_email_text_${client.id}")
                            )
                        }
                    }

                    if (client.email.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Copy Email Button
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(client.email))
                                    Toast.makeText(context, "Copied ${client.email}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_copy_email_${client.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy email address",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Launch Email App Button
                            IconButton(
                                onClick = {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:${client.email}")
                                        putExtra(Intent.EXTRA_SUBJECT, "Oistars Billing Update - ${client.name}")
                                    }
                                    try {
                                        context.startActivity(emailIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("btn_send_email_${client.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Send email",
                                    tint = OceanBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Location & Phone Information (if available)
            if (client.country.isNotBlank() || client.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (client.country.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${client.country} (${client.currency})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (client.phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${client.phone}")
                                }
                                try {
                                    context.startActivity(dialIntent)
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = OceanBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = client.phone,
                                fontSize = 11.sp,
                                color = OceanBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Actions & Outstanding Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (client.outstandingBalance > 0.0) "Outstanding Balance" else "Account Status",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (client.outstandingBalance > 0.0)
                            Formatters.formatCurrency(client.outstandingBalance, client.currency)
                        else "Settled / Up to date",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (client.outstandingBalance > 0.0) StatusOverdue else StatusPaid
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onClick,
                        modifier = Modifier.testTag("btn_view_client_${client.id}")
                    ) {
                        Text("View Details", fontSize = 12.sp, color = OceanBlue)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = onCreateInvoice,
                        colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_invoice_client_${client.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Invoice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
