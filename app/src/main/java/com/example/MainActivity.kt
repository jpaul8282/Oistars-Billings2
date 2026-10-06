package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.firebase.FirebaseAuthManager
import com.example.data.firebase.FirestoreSyncManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.example.data.local.ClientEntity
import com.example.ui.screens.AccountSettingsScreen
import com.example.ui.screens.ClientsScreen
import com.example.ui.screens.CreateInvoiceSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoicesScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.theme.AccentGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusOverdue
import com.example.ui.viewmodel.BillingViewModel

enum class NavigationDestination(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    INVOICES("Invoices", Icons.Default.Receipt, "nav_invoices"),
    CLIENTS("Stores", Icons.Default.Business, "nav_clients"),
    SUBSCRIPTIONS("Retainers", Icons.Default.Autorenew, "nav_subscriptions"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainBillingApp(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBillingApp(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    viewModel: BillingViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestoreSyncManager = remember { FirestoreSyncManager(context) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }

    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val currentFirebaseUser = firebaseAuth.currentUser

    var isLoggedIn by remember { mutableStateOf(currentFirebaseUser != null || true) }
    var userEmail by remember { mutableStateOf(currentFirebaseUser?.email ?: "westerveldjp@gmail.com") }
    var userName by remember { mutableStateOf(currentFirebaseUser?.displayName ?: "Jurgen Paul Westerveld") }

    LaunchedEffect(currentFirebaseUser) {
        currentFirebaseUser?.let { u ->
            userEmail = u.email ?: userEmail
            userName = u.displayName ?: userName
        }
    }

    var currentDestination by remember { mutableStateOf(NavigationDestination.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe ViewModel Streams
    val metrics by viewModel.financialMetrics.collectAsStateWithLifecycle()
    val invoices by viewModel.allInvoices.collectAsStateWithLifecycle()
    val clients by viewModel.allClients.collectAsStateWithLifecycle()
    val subscriptions by viewModel.allSubscriptions.collectAsStateWithLifecycle()
    val invoiceFilter by viewModel.invoiceStatusFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val userFeedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()

    // Create Invoice Sheet State triggered from Clients or Dashboard
    var clientForNewInvoice by remember { mutableStateOf<ClientEntity?>(null) }
    var showCreateInvoiceSheet by remember { mutableStateOf(false) }

    // Feedback Toast Snackbar
    LaunchedEffect(userFeedbackMessage) {
        userFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { email, name ->
                userEmail = email
                userName = name
                isLoggedIn = true
            },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    val overdueCount = metrics.overdueInvoicesCount

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NavyDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Oistars Billings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    // Dark / Light Mode Toggle
                    IconButton(
                        onClick = { onToggleDarkTheme(!isDarkTheme) },
                        modifier = Modifier.testTag("action_toggle_theme")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // User Profile / Settings Avatar Shortcut
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(OceanBlue.copy(alpha = 0.2f))
                            .clickable { currentDestination = NavigationDestination.SETTINGS },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.take(2).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationDestination.entries.forEach { destination ->
                    val selected = currentDestination == destination

                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentDestination = destination },
                        icon = {
                            if (destination == NavigationDestination.INVOICES && overdueCount > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = StatusOverdue) {
                                        Text("$overdueCount", fontSize = 10.sp, color = Color.White)
                                    }
                                }) {
                                    Icon(destination.icon, contentDescription = destination.title)
                                }
                            } else {
                                Icon(destination.icon, contentDescription = destination.title)
                            }
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OceanBlue,
                            selectedTextColor = OceanBlue,
                            indicatorColor = OceanBlue.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag(destination.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentDestination,
            modifier = Modifier.padding(innerPadding),
            label = "ScreenTransition"
        ) { destination ->
            when (destination) {
                NavigationDestination.DASHBOARD -> {
                    DashboardScreen(
                        metrics = metrics,
                        recentInvoices = invoices,
                        activeSubscriptions = subscriptions,
                        clients = clients,
                        onNavigateToInvoices = { currentDestination = NavigationDestination.INVOICES },
                        onNavigateToClients = { currentDestination = NavigationDestination.CLIENTS },
                        onNavigateToSubscriptions = { currentDestination = NavigationDestination.SUBSCRIPTIONS },
                        onMarkAsPaid = { viewModel.markInvoiceAsPaid(it) },
                        onRecordPayment = { inv, amt, method, ref, notes ->
                            viewModel.recordInvoicePayment(inv, amt, method, ref, notes)
                        },
                        onDeleteInvoice = { viewModel.deleteInvoice(it) },
                        onGenerateFromSubscription = { viewModel.generateInvoiceFromSubscription(it) },
                        onSaveInvoice = { clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency ->
                            viewModel.saveInvoice(
                                id = null,
                                clientId = clientId,
                                clientName = clientName,
                                clientEmail = clientEmail,
                                items = items,
                                paymentTerms = paymentTerms,
                                notes = notes,
                                status = status,
                                currency = currency
                            )
                        }
                    )
                }

                NavigationDestination.INVOICES -> {
                    InvoicesScreen(
                        invoices = invoices,
                        clients = clients,
                        selectedFilter = invoiceFilter,
                        searchQuery = searchQuery,
                        onFilterChange = { viewModel.setInvoiceStatusFilter(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onMarkAsPaid = { viewModel.markInvoiceAsPaid(it) },
                        onRecordPayment = { inv, amt, method, ref, notes ->
                            viewModel.recordInvoicePayment(inv, amt, method, ref, notes)
                        },
                        onDeleteInvoice = { viewModel.deleteInvoice(it) },
                        onSaveInvoice = { clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency ->
                            viewModel.saveInvoice(
                                id = null,
                                clientId = clientId,
                                clientName = clientName,
                                clientEmail = clientEmail,
                                items = items,
                                paymentTerms = paymentTerms,
                                notes = notes,
                                status = status,
                                currency = currency
                            )
                        }
                    )
                }

                NavigationDestination.CLIENTS -> {
                    ClientsScreen(
                        clients = clients,
                        invoices = invoices,
                        onSaveClient = { id, name, contact, email, phone, tax, address, country, currency, terms, notes ->
                            viewModel.saveClient(id, name, contact, email, phone, tax, address, country, currency, terms, notes)
                        },
                        onDeleteClient = { viewModel.deleteClient(it) },
                        onCreateInvoiceForClient = { client ->
                            clientForNewInvoice = client
                            showCreateInvoiceSheet = true
                        }
                    )
                }

                NavigationDestination.SUBSCRIPTIONS -> {
                    SubscriptionsScreen(
                        subscriptions = subscriptions,
                        clients = clients,
                        onSaveSubscription = { id, clientId, clientName, planName, amount, currency, interval, status, notes ->
                            viewModel.saveSubscription(id, clientId, clientName, planName, amount, currency, interval, status, notes)
                        },
                        onDeleteSubscription = { viewModel.deleteSubscription(it) },
                        onGenerateInvoice = { viewModel.generateInvoiceFromSubscription(it) }
                    )
                }

                NavigationDestination.SETTINGS -> {
                    AccountSettingsScreen(
                        userEmail = userEmail,
                        userName = userName,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onLogout = {
                            firebaseAuth.signOut()
                            isLoggedIn = false
                        },
                        onSyncToCloud = {
                            coroutineScope.launch {
                                val uid = firebaseAuth.currentUser?.uid ?: "local-owner"
                                syncStatusMessage = "Syncing local records with Firestore..."
                                val payments = viewModel.allPayments.value
                                val result = firestoreSyncManager.syncAllToCloud(uid, clients, invoices, subscriptions, payments)
                                result.onSuccess {
                                    syncStatusMessage = "Synced ${clients.size} stores, ${invoices.size} invoices & ${subscriptions.size} retainers to Firestore!"
                                    snackbarHostState.showSnackbar("Synced to Firestore successfully!")
                                }.onFailure { e ->
                                    syncStatusMessage = "Sync error: ${e.localizedMessage ?: "Network error"}"
                                    snackbarHostState.showSnackbar("Sync failed: ${e.localizedMessage ?: "Error"}")
                                }
                            }
                        },
                        syncStatusMessage = syncStatusMessage
                    )
                }
            }
        }
    }

    // Modal sheet for creating invoice directly for a client
    if (showCreateInvoiceSheet) {
        CreateInvoiceSheet(
            clients = clients,
            preselectedClient = clientForNewInvoice,
            onDismiss = {
                showCreateInvoiceSheet = false
                clientForNewInvoice = null
            },
            onSaveInvoice = { clientId, clientName, clientEmail, items, paymentTerms, notes, status, currency ->
                viewModel.saveInvoice(
                    id = null,
                    clientId = clientId,
                    clientName = clientName,
                    clientEmail = clientEmail,
                    items = items,
                    paymentTerms = paymentTerms,
                    notes = notes,
                    status = status,
                    currency = currency
                )
                showCreateInvoiceSheet = false
                clientForNewInvoice = null
            }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
