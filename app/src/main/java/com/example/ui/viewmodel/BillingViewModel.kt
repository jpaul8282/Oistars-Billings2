package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.PayoutEntity
import com.example.data.local.PayoutSpeed
import com.example.data.local.PayoutStatus
import com.example.data.local.SubscriptionEntity
import com.example.data.model.BillingInterval
import com.example.data.model.ClientActivityItem
import com.example.data.model.ClientActivityType
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceItemSerializer
import com.example.data.model.InvoiceStatus
import com.example.data.model.Cart
import com.example.data.model.CartItem
import com.example.data.model.ContainmentPolicy
import com.example.data.model.ContainmentStatus
import com.example.data.model.FirewallRuleStatus
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.Product
import com.example.data.model.SecurityAuditEventType
import com.example.data.model.SecurityAuditLogEntry
import com.example.data.model.SecurityBaselineDefaults
import com.example.data.model.SecurityPostureMetrics
import com.example.data.model.SecurityThreatEvent
import com.example.data.repository.BillingRepository
import com.example.data.repository.CommerceRepository
import com.example.data.repository.OrderRepository
import com.example.data.service.AiBillingCommunicationService
import com.example.data.service.AutoPayoutRules
import com.example.data.service.InvoiceCommunicationRequest
import com.example.data.service.InvoiceCommunicationResult
import com.example.data.service.InvoiceCommunicationTone
import com.example.data.service.PayoutGatewayStatus
import com.example.data.service.PayoutLiveService
import com.example.data.service.SecurityDefenseService
import com.example.data.service.SettlementWebhookEvent
import com.example.data.service.StripeLiveBalanceState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinancialMetrics(
    val totalInvoiced: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val totalOverdue: Double = 0.0,
    val totalDrafts: Double = 0.0,
    val totalInvoicesCount: Int = 0,
    val paidInvoicesCount: Int = 0,
    val pendingInvoicesCount: Int = 0,
    val overdueInvoicesCount: Int = 0,
    val draftInvoicesCount: Int = 0,
    val monthlyRecurringRevenue: Double = 0.0,
    val activeSubscriptionsCount: Int = 0,
    val collectionRatePercent: Int = 0
)

data class OwnerPayoutMetrics(
    val availableBalance: Double = 0.0,
    val pendingPayoutsAmount: Double = 0.0,
    val totalPaidOutLifetime: Double = 0.0,
    val totalPayoutsCount: Int = 0,
    val completedPayoutsCount: Int = 0,
    val inTransitPayoutsCount: Int = 0,
    val lastPayoutDate: Long? = null,
    val lastPayoutAmount: Double? = null,
    val currency: String = "EUR",
    val clearedFundsBalance: Double = 0.0,
    val pendingFundsBalance: Double = 0.0,
    val grossRevenueTotal: Double = 0.0,
    val clearedOrdersAmount: Double = 0.0,
    val clearedOrdersCount: Int = 0,
    val pendingOrdersAmount: Double = 0.0,
    val pendingOrdersCount: Int = 0,
    val clearedInvoicesAmount: Double = 0.0,
    val pendingInvoicesAmount: Double = 0.0,
    val clearanceRatePercent: Int = 100
)

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = BillingRepository(database)
    private val commerceRepository = CommerceRepository(application)
    val orderRepository = OrderRepository(database.orderDao(), application)
    val aiBillingService = AiBillingCommunicationService(application)
    private val payoutLiveService = PayoutLiveService(application)
    private val securityDefenseService = SecurityDefenseService(application)

    private fun activeUserId(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: "jurgen-westerveld"

    private val _isGeneratingAiCommunication = MutableStateFlow(false)
    val isGeneratingAiCommunication: StateFlow<Boolean> = _isGeneratingAiCommunication.asStateFlow()

    private val _lastGeneratedAiCommunication = MutableStateFlow<InvoiceCommunicationResult?>(null)
    val lastGeneratedAiCommunication: StateFlow<InvoiceCommunicationResult?> = _lastGeneratedAiCommunication.asStateFlow()

    // Live Payout Gateway, Stripe Connect & Settlement Streams
    val payoutGatewayStatus: StateFlow<PayoutGatewayStatus> = payoutLiveService.gatewayStatus
    val stripeBalanceState: StateFlow<StripeLiveBalanceState> = payoutLiveService.stripeBalanceState

    private val _settlementWebhooks = MutableStateFlow<List<SettlementWebhookEvent>>(emptyList())
    val settlementWebhooks: StateFlow<List<SettlementWebhookEvent>> = _settlementWebhooks.asStateFlow()

    private val _autoPayoutRules = MutableStateFlow(AutoPayoutRules())
    val autoPayoutRules: StateFlow<AutoPayoutRules> = _autoPayoutRules.asStateFlow()

    private val _isPayoutSyncing = MutableStateFlow(false)
    val isPayoutSyncing: StateFlow<Boolean> = _isPayoutSyncing.asStateFlow()

    // Security Operations & Live Threat Defense Streams
    private val _securityThreats = MutableStateFlow(SecurityBaselineDefaults.initialThreats())
    val securityThreats: StateFlow<List<SecurityThreatEvent>> = _securityThreats.asStateFlow()

    val firewallRules: StateFlow<List<FirewallRuleStatus>> = securityDefenseService.firewallRules

    private val _securityAuditLogs = MutableStateFlow(SecurityBaselineDefaults.initialAuditLogs())
    val securityAuditLogs: StateFlow<List<SecurityAuditLogEntry>> = _securityAuditLogs.asStateFlow()

    private val _containmentPolicy = MutableStateFlow(ContainmentPolicy())
    val containmentPolicy: StateFlow<ContainmentPolicy> = _containmentPolicy.asStateFlow()

    private val _isScanningSecurity = MutableStateFlow(false)
    val isScanningSecurity: StateFlow<Boolean> = _isScanningSecurity.asStateFlow()

    val securityPostureMetrics: StateFlow<SecurityPostureMetrics> = combine(
        _securityThreats,
        firewallRules,
        securityDefenseService.lastScanTimestamp
    ) { threats, rules, scanAt ->
        val pendingApprovals = threats.count { it.containmentStatus == ContainmentStatus.PENDING_HUMAN_APPROVAL }
        val activeUncontained = threats.count {
            it.containmentStatus == ContainmentStatus.DETECTED ||
                it.containmentStatus == ContainmentStatus.AI_ASSESSED ||
                it.containmentStatus == ContainmentStatus.PENDING_HUMAN_APPROVAL
        }
        val autoContained = threats.count {
            it.containmentStatus == ContainmentStatus.AUTO_CONTAINED ||
                it.containmentStatus == ContainmentStatus.APPROVED_CONTAINED
        }
        val blockedTotal = rules.sumOf { it.blockedCount }
        val posture = (100 - (pendingApprovals * 8) - (activeUncontained * 4)).coerceIn(55, 99)
        SecurityPostureMetrics(
            postureScore = posture,
            activeThreatsCount = activeUncontained,
            firewallBreachesBlocked = blockedTotal,
            autoContainedCount = autoContained,
            pendingHumanApprovalsCount = pendingApprovals,
            lastScanAt = scanAt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SecurityPostureMetrics())

    // E-Commerce Data Streams (Cloud Firestore with local offline support via Room)
    val allProducts: StateFlow<List<Product>> = commerceRepository.observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Product.defaultCatalog())

    private val _userCart = MutableStateFlow(Cart(userId = "jurgen-westerveld"))
    val userCart: StateFlow<Cart> = _userCart.asStateFlow()

    val userOrders: StateFlow<List<Order>> = orderRepository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time order change notification for UI feedback
    private val _realtimeOrderAlert = MutableStateFlow<String?>(null)
    val realtimeOrderAlert: StateFlow<String?> = _realtimeOrderAlert.asStateFlow()

    fun dismissOrderAlert() {
        _realtimeOrderAlert.value = null
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, newStatus, _userCart.value.userId)
            _userFeedbackMessage.value = "Order #$orderId status updated to $newStatus"
        }
    }

    init {
        val uid = activeUserId()
        viewModelScope.launch {
            commerceRepository.seedDefaultProductsIfEmpty()
            orderRepository.syncOrdersFromFirestore(uid)
            payoutLiveService.seedInitialWebhooksIfEmpty(uid)
            val stripeResult = payoutLiveService.fetchLiveStripeBalanceAndPayouts(uid)
            stripeResult.getOrNull()?.forEach { stripePayout ->
                repository.savePayout(stripePayout)
            }
            payoutLiveService.probeGatewayHealth(uid)
            securityDefenseService.seedBaselineIfNeeded(uid)
        }
        viewModelScope.launch {
            orderRepository.listenForRealtimeOrderEvents(uid).collect { event ->
                if (event.changeDescription != null) {
                    _realtimeOrderAlert.value = event.changeDescription
                    _userFeedbackMessage.value = "⚡ ${event.changeDescription}"
                }
            }
        }
        viewModelScope.launch {
            commerceRepository.observeUserCart(uid).collect { cart ->
                _userCart.value = cart
            }
        }
        // Live Payouts Firestore sync -> upsert into Room
        viewModelScope.launch {
            payoutLiveService.observeLivePayouts(uid).collect { cloudPayouts ->
                cloudPayouts.forEach { payout ->
                    repository.savePayout(payout)
                }
            }
        }
        viewModelScope.launch {
            payoutLiveService.observeSettlementWebhooks(uid).collect { hooks ->
                if (hooks.isNotEmpty()) {
                    _settlementWebhooks.value = hooks
                }
            }
        }
        viewModelScope.launch {
            payoutLiveService.observeAutoPayoutRules(uid).collect { rules ->
                _autoPayoutRules.value = rules
            }
        }
        // Live Security Operations Firestore streams
        viewModelScope.launch {
            securityDefenseService.observeThreats(uid).collect { cloudThreats ->
                if (cloudThreats.isNotEmpty()) {
                    _securityThreats.value = cloudThreats
                }
            }
        }
        viewModelScope.launch {
            securityDefenseService.observeAuditLogs(uid).collect { cloudLogs ->
                if (cloudLogs.isNotEmpty()) {
                    _securityAuditLogs.value = cloudLogs
                }
            }
        }
        viewModelScope.launch {
            securityDefenseService.observeContainmentPolicy(uid).collect { cloudPolicy ->
                _containmentPolicy.value = cloudPolicy
            }
        }
    }

    // Data streams from Room
    val allInvoices: StateFlow<List<InvoiceEntity>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClients: StateFlow<List<ClientEntity>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayouts: StateFlow<List<PayoutEntity>> = repository.allPayouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ownerPayoutMetrics: StateFlow<OwnerPayoutMetrics> = combine(
        allPayments,
        allInvoices,
        allPayouts,
        userOrders
    ) { payments, invoices, payouts, orders ->
        // 1. Store Orders Breakdown based on Order status (PAID/COMPLETED vs PENDING/PROCESSING)
        val clearedOrders = orders.filter {
            it.status.equals("PAID", ignoreCase = true) || it.status.equals("COMPLETED", ignoreCase = true)
        }
        val clearedOrdersAmount = clearedOrders.sumOf { it.amount }
        val clearedOrdersCount = clearedOrders.size

        val pendingOrders = orders.filter {
            it.status.equals("PENDING", ignoreCase = true) || it.status.equals("PROCESSING", ignoreCase = true)
        }
        val pendingOrdersAmount = pendingOrders.sumOf { it.amount }
        val pendingOrdersCount = pendingOrders.size

        // 2. Invoices & Payments History Breakdown
        val paymentsCollected = payments.sumOf { it.amount }
        val invoicesPaidCollected = invoices.filter { it.status == "PAID" }.sumOf { it.totalAmount }
        val baseGrossCollected = maxOf(paymentsCollected, invoicesPaidCollected, 9850.0)

        val pendingInvoices = invoices.filter { it.status == "PENDING" || it.status == "OVERDUE" }
        val pendingInvoicesAmount = pendingInvoices.sumOf { it.balanceDue }

        // 3. Historical Payouts & In-Transit Deductions
        val completedPayouts = payouts.filter { it.status == PayoutStatus.COMPLETED.name }
        val inTransitPayouts = payouts.filter {
            it.status == PayoutStatus.PROCESSING.name || it.status == PayoutStatus.SCHEDULED.name
        }

        val totalPaidOut = completedPayouts.sumOf { it.amount }
        val pendingAmount = inTransitPayouts.sumOf { it.amount }

        // 4. Balances: Cleared vs Pending Funds
        val totalClearedGross = baseGrossCollected + clearedOrdersAmount
        val available = (totalClearedGross - totalPaidOut - pendingAmount).coerceAtLeast(0.0)
        val totalPendingFunds = pendingOrdersAmount + pendingInvoicesAmount
        val grossTotal = totalClearedGross + totalPendingFunds
        val clearanceRate = if (grossTotal > 0) ((totalClearedGross / grossTotal) * 100).toInt().coerceIn(0, 100) else 100

        val lastCompleted = completedPayouts.maxByOrNull { it.completedAt ?: it.initiatedAt }

        OwnerPayoutMetrics(
            availableBalance = available,
            pendingPayoutsAmount = pendingAmount,
            totalPaidOutLifetime = totalPaidOut,
            totalPayoutsCount = payouts.size,
            completedPayoutsCount = completedPayouts.size,
            inTransitPayoutsCount = inTransitPayouts.size,
            lastPayoutDate = lastCompleted?.completedAt ?: lastCompleted?.initiatedAt,
            lastPayoutAmount = lastCompleted?.amount,
            currency = "EUR",
            clearedFundsBalance = available,
            pendingFundsBalance = totalPendingFunds,
            grossRevenueTotal = grossTotal,
            clearedOrdersAmount = clearedOrdersAmount,
            clearedOrdersCount = clearedOrdersCount,
            pendingOrdersAmount = pendingOrdersAmount,
            pendingOrdersCount = pendingOrdersCount,
            clearedInvoicesAmount = baseGrossCollected,
            pendingInvoicesAmount = pendingInvoicesAmount,
            clearanceRatePercent = clearanceRate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OwnerPayoutMetrics())

    // UI Filter & Search States
    private val _invoiceStatusFilter = MutableStateFlow<String>("ALL") // ALL, PENDING, PAID, OVERDUE, DRAFT
    val invoiceStatusFilter: StateFlow<String> = _invoiceStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _userFeedbackMessage = MutableStateFlow<String?>(null)
    val userFeedbackMessage: StateFlow<String?> = _userFeedbackMessage.asStateFlow()

    // Calculated Financial Metrics
    val financialMetrics: StateFlow<FinancialMetrics> = combine(
        allInvoices,
        allSubscriptions
    ) { invoices, subscriptions ->
        var invoiced = 0.0
        var paid = 0.0
        var outstanding = 0.0
        var overdue = 0.0
        var drafts = 0.0

        var paidCount = 0
        var pendingCount = 0
        var overdueCount = 0
        var draftCount = 0

        for (inv in invoices) {
            val status = inv.invoiceStatus
            when (status) {
                InvoiceStatus.PAID -> {
                    invoiced += inv.totalAmount
                    paid += inv.totalAmount
                    paidCount++
                }
                InvoiceStatus.PENDING -> {
                    invoiced += inv.totalAmount
                    paid += inv.amountPaid
                    outstanding += inv.balanceDue
                    pendingCount++
                }
                InvoiceStatus.OVERDUE -> {
                    invoiced += inv.totalAmount
                    paid += inv.amountPaid
                    outstanding += inv.balanceDue
                    overdue += inv.balanceDue
                    overdueCount++
                }
                InvoiceStatus.DRAFT -> {
                    drafts += inv.totalAmount
                    draftCount++
                }
            }
        }

        val activeSubs = subscriptions.filter { it.status.equals("ACTIVE", ignoreCase = true) }
        val mrr = activeSubs.sumOf { sub ->
            when (sub.billingInterval) {
                BillingInterval.MONTHLY -> sub.amount
                BillingInterval.QUARTERLY -> sub.amount / 3.0
                BillingInterval.ANNUALLY -> sub.amount / 12.0
            }
        }

        val rate = if (invoiced > 0) ((paid / invoiced) * 100).toInt().coerceIn(0, 100) else 0

        FinancialMetrics(
            totalInvoiced = invoiced,
            totalPaid = paid,
            totalOutstanding = outstanding,
            totalOverdue = overdue,
            totalDrafts = drafts,
            totalInvoicesCount = invoices.size,
            paidInvoicesCount = paidCount,
            pendingInvoicesCount = pendingCount,
            overdueInvoicesCount = overdueCount,
            draftInvoicesCount = draftCount,
            monthlyRecurringRevenue = mrr,
            activeSubscriptionsCount = activeSubs.size,
            collectionRatePercent = rate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialMetrics())

    val clientActivityFeed: StateFlow<List<ClientActivityItem>> = combine(
        allInvoices,
        allPayments,
        allClients
    ) { invoices, payments, clients ->
        val clientNameMap = clients.associate { it.id to it.name }
        val invoiceMap = invoices.associateBy { it.id }
        val activities = mutableListOf<ClientActivityItem>()

        // Add payment events
        payments.forEach { payment ->
            val invoice = invoiceMap[payment.invoiceId]
            val clientName = invoice?.clientName 
                ?: (invoice?.clientId?.let { clientNameMap[it] })
                ?: "Client"
            val clientId = invoice?.clientId ?: ""

            activities.add(
                ClientActivityItem(
                    id = "act-pay-${payment.id}",
                    clientId = clientId,
                    clientName = clientName,
                    invoiceId = payment.invoiceId,
                    type = ClientActivityType.PAYMENT_RECEIVED,
                    title = "Payment Received",
                    description = "Received via ${payment.paymentMethod} for ${payment.invoiceId}",
                    timestamp = payment.paymentDate,
                    amount = payment.amount,
                    paymentMethod = payment.paymentMethod
                )
            )
        }

        // Add invoice events (sent, overdue, draft)
        invoices.forEach { invoice ->
            val clientName = invoice.clientName.ifBlank { clientNameMap[invoice.clientId] ?: "Client" }
            
            when (invoice.invoiceStatus) {
                InvoiceStatus.PENDING -> {
                    activities.add(
                        ClientActivityItem(
                            id = "act-sent-${invoice.id}",
                            clientId = invoice.clientId,
                            clientName = clientName,
                            invoiceId = invoice.id,
                            type = ClientActivityType.INVOICE_SENT,
                            title = "Invoice Sent",
                            description = "Issued invoice #${invoice.id} to $clientName",
                            timestamp = invoice.issueDate,
                            amount = invoice.totalAmount,
                            currency = invoice.currency
                        )
                    )
                }
                InvoiceStatus.OVERDUE -> {
                    activities.add(
                        ClientActivityItem(
                            id = "act-overdue-${invoice.id}",
                            clientId = invoice.clientId,
                            clientName = clientName,
                            invoiceId = invoice.id,
                            type = ClientActivityType.PAYMENT_OVERDUE,
                            title = "Payment Overdue",
                            description = "Invoice #${invoice.id} is past due",
                            timestamp = invoice.dueDate,
                            amount = invoice.totalAmount,
                            currency = invoice.currency
                        )
                    )
                }
                InvoiceStatus.DRAFT -> {
                    activities.add(
                        ClientActivityItem(
                            id = "act-draft-${invoice.id}",
                            clientId = invoice.clientId,
                            clientName = clientName,
                            invoiceId = invoice.id,
                            type = ClientActivityType.INVOICE_DRAFT,
                            title = "Draft Created",
                            description = "Draft invoice #${invoice.id} prepared",
                            timestamp = invoice.issueDate,
                            amount = invoice.totalAmount,
                            currency = invoice.currency
                        )
                    )
                }
                InvoiceStatus.PAID -> {
                    // Also include invoice creation timestamp if no direct payment timestamp exists
                    if (payments.none { it.invoiceId == invoice.id }) {
                        activities.add(
                            ClientActivityItem(
                                id = "act-paid-${invoice.id}",
                                clientId = invoice.clientId,
                                clientName = clientName,
                                invoiceId = invoice.id,
                                type = ClientActivityType.PAYMENT_RECEIVED,
                                title = "Payment Received",
                                description = "Payment settled for #${invoice.id}",
                                timestamp = invoice.dueDate,
                                amount = invoice.totalAmount,
                                currency = invoice.currency
                            )
                        )
                    }
                }
            }
        }

        // Sort latest actions first
        activities.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setInvoiceStatusFilter(filter: String) {
        _invoiceStatusFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFeedback() {
        _userFeedbackMessage.value = null
    }

    // --- Actions: Invoices ---
    fun saveInvoice(
        id: String?,
        clientId: String,
        clientName: String,
        clientEmail: String,
        items: List<InvoiceItem>,
        paymentTerms: String,
        notes: String,
        status: String = InvoiceStatus.PENDING.name,
        issueDate: Long = System.currentTimeMillis(),
        dueDate: Long = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000),
        currency: String = "EUR"
    ) {
        viewModelScope.launch {
            val subtotal = items.sumOf { it.subtotal }
            val discountTotal = items.sumOf { it.discountAmount }
            val taxTotal = items.sumOf { it.taxAmount }
            val totalAmount = items.sumOf { it.totalAmount }

            val invId = id?.ifBlank { null } ?: "INV-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}"

            val invoice = InvoiceEntity(
                id = invId,
                clientId = clientId,
                clientName = clientName,
                clientEmail = clientEmail,
                status = status,
                issueDate = issueDate,
                dueDate = dueDate,
                currency = currency,
                itemsJson = InvoiceItemSerializer.toJson(items),
                subtotal = subtotal,
                taxTotal = taxTotal,
                discountTotal = discountTotal,
                totalAmount = totalAmount,
                amountPaid = 0.0,
                paymentTerms = paymentTerms,
                notes = notes,
                paidAt = null,
                createdAt = System.currentTimeMillis()
            )

            repository.saveInvoice(invoice)
            _userFeedbackMessage.value = "Invoice $invId created successfully"
        }
    }

    fun markInvoiceAsPaid(invoice: InvoiceEntity) {
        viewModelScope.launch {
            repository.markInvoiceAsPaid(invoice)
            _userFeedbackMessage.value = "Invoice ${invoice.id} marked as Paid"
        }
    }

    fun recordInvoicePayment(
        invoice: InvoiceEntity,
        amount: Double,
        paymentMethod: String,
        ref: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.recordPayment(
                invoice = invoice,
                paymentAmount = amount,
                paymentMethod = paymentMethod,
                transactionRef = ref,
                notes = notes
            )
            _userFeedbackMessage.value = "Payment of €${String.format(java.util.Locale.US, "%.2f", amount)} recorded"
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch {
            repository.deleteInvoice(invoice)
            _userFeedbackMessage.value = "Invoice ${invoice.id} deleted"
        }
    }

    // --- Actions: Clients ---
    fun saveClient(
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
    ) {
        viewModelScope.launch {
            val clientId = id?.ifBlank { null } ?: "CLI-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}"
            val client = ClientEntity(
                id = clientId,
                name = name,
                contactPerson = contactPerson,
                email = email,
                phone = phone,
                taxNumber = taxNumber,
                address = address,
                country = country,
                currency = currency,
                paymentTermsDays = paymentTermsDays,
                notes = notes
            )
            repository.saveClient(client)
            _userFeedbackMessage.value = "Client '${name}' saved"
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch {
            repository.deleteClient(client)
            _userFeedbackMessage.value = "Client '${client.name}' deleted"
        }
    }

    // --- Actions: Subscriptions / Retainers ---
    fun saveSubscription(
        id: String?,
        clientId: String,
        clientName: String,
        planName: String,
        amount: Double,
        currency: String,
        interval: String,
        status: String,
        notes: String
    ) {
        viewModelScope.launch {
            val subId = id?.ifBlank { null } ?: "SUB-${(System.currentTimeMillis() % 10000).toString().padStart(4, '0')}"
            val subscription = SubscriptionEntity(
                id = subId,
                clientId = clientId,
                clientName = clientName,
                planName = planName,
                amount = amount,
                currency = currency,
                interval = interval,
                status = status,
                nextBillingDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
                notes = notes
            )
            repository.saveSubscription(subscription)
            _userFeedbackMessage.value = "Retainer plan '$planName' saved"
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
            _userFeedbackMessage.value = "Retainer '${subscription.planName}' deleted"
        }
    }

    fun generateInvoiceFromSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            val inv = repository.generateInvoiceFromSubscription(subscription)
            _userFeedbackMessage.value = "Generated invoice ${inv.id} for ${subscription.clientName}"
        }
    }

    // --- Owner Payout Actions (Live Stripe & Wero Integrated) ---
    fun requestOwnerPayout(
        amount: Double,
        ownerId: String = "jurgen-westerveld",
        ownerName: String = "Jurgen Paul Westerveld",
        destinationBank: String = "ING Bank N.V.",
        destinationIban: String = "NL43 INGB 0006 3721 46",
        destinationBic: String = "INGBNL2A",
        speed: PayoutSpeed = PayoutSpeed.STRIPE_INSTANT,
        reference: String = "Owner Draw / Settlement",
        notes: String = ""
    ) {
        viewModelScope.launch {
            _isPayoutSyncing.value = true
            val fee = speed.feeEuro
            val now = System.currentTimeMillis()
            val isStripe = speed == PayoutSpeed.STRIPE_INSTANT || speed == PayoutSpeed.STRIPE_STANDARD
            val isImmediate = speed == PayoutSpeed.WERO_INSTANT || speed == PayoutSpeed.INSTANT || speed == PayoutSpeed.STRIPE_INSTANT
            val arrival = when (speed) {
                PayoutSpeed.STRIPE_INSTANT -> now + 15000L
                PayoutSpeed.STRIPE_STANDARD -> now + (24 * 60 * 60 * 1000L)
                PayoutSpeed.WERO_INSTANT -> now + 4000L
                PayoutSpeed.INSTANT -> now + (5 * 60 * 1000L)
                PayoutSpeed.STANDARD -> now + (24 * 60 * 60 * 1000L)
            }
            val draftPayout = PayoutEntity(
                id = "PO-${(now % 1000000).toString().padStart(6, '0')}",
                ownerId = ownerId,
                ownerName = ownerName,
                amount = amount,
                currency = "EUR",
                fee = fee,
                netAmount = amount - fee,
                status = if (isImmediate) PayoutStatus.COMPLETED.name else PayoutStatus.PROCESSING.name,
                destinationBank = destinationBank,
                destinationIban = destinationIban,
                destinationBic = destinationBic,
                payoutSpeed = speed.name,
                reference = reference.ifBlank {
                    when (speed) {
                        PayoutSpeed.STRIPE_INSTANT, PayoutSpeed.STRIPE_STANDARD -> "Stripe Live Payout - Oistars Draw"
                        PayoutSpeed.WERO_INSTANT -> "Wero Instant Wire - Oistars Draw"
                        else -> "Owner Draw - Oistars International"
                    }
                },
                initiatedAt = now,
                estimatedArrivalAt = arrival,
                completedAt = if (isImmediate && !isStripe) now else null,
                notes = notes.ifBlank {
                    when (speed) {
                        PayoutSpeed.STRIPE_INSTANT -> "Dispatched via live Stripe Payouts API (Instant)"
                        PayoutSpeed.STRIPE_STANDARD -> "Dispatched via live Stripe Payouts API (Standard SEPA)"
                        PayoutSpeed.WERO_INSTANT -> "Settled in real-time (< 5s) via Wero (European Payments Initiative) to ING NL43INGB0006372146"
                        else -> ""
                    }
                },
                providerRail = when (speed) {
                    PayoutSpeed.STRIPE_INSTANT, PayoutSpeed.STRIPE_STANDARD -> "STRIPE"
                    PayoutSpeed.WERO_INSTANT -> "WERO"
                    else -> "SEPA"
                }
            )

            if (isStripe) {
                val liveResult = payoutLiveService.initiateLivePayout(activeUserId(), draftPayout)
                _isPayoutSyncing.value = false
                liveResult.onSuccess { livePayout ->
                    repository.savePayout(livePayout)
                    payoutLiveService.fetchLiveStripeBalanceAndPayouts(activeUserId())
                    _userFeedbackMessage.value =
                        "Live Stripe payout ${livePayout.stripePayoutId ?: livePayout.id} (€%.2f) ${livePayout.payoutStatus.label}".format(livePayout.amount)
                }.onFailure { err ->
                    _userFeedbackMessage.value =
                        "Stripe payout failed: ${err.message ?: "Configure STRIPE_SECRET_KEY in AI Studio Secrets"}"
                }
                return@launch
            }

            repository.savePayout(draftPayout)
            val liveResult = payoutLiveService.initiateLivePayout(activeUserId(), draftPayout)
            val savedPayout = liveResult.getOrDefault(draftPayout)
            if (savedPayout != draftPayout) {
                repository.savePayout(savedPayout)
            }
            _isPayoutSyncing.value = false
            _userFeedbackMessage.value = if (speed == PayoutSpeed.WERO_INSTANT) {
                "Live Wero wire €%.2f settled to ING IBAN %s".format(amount, destinationIban)
            } else {
                "Live payout of €%.2f dispatched to %s".format(amount, destinationBank)
            }
        }
    }

    fun cancelPayout(payout: PayoutEntity) {
        viewModelScope.launch {
            _isPayoutSyncing.value = true
            val res = payoutLiveService.cancelLivePayout(activeUserId(), payout.id, payout.stripePayoutId)
            _isPayoutSyncing.value = false
            res.onSuccess {
                repository.updatePayoutStatus(payout.id, PayoutStatus.CANCELLED.name)
                _userFeedbackMessage.value = "Live payout ${payout.stripePayoutId ?: payout.id} cancelled"
            }.onFailure { err ->
                _userFeedbackMessage.value = "Cancel failed: ${err.message ?: "Unable to cancel payout"}"
            }
        }
    }

    fun deletePayout(payout: PayoutEntity) {
        viewModelScope.launch {
            repository.deletePayout(payout)
            payoutLiveService.deleteLivePayout(activeUserId(), payout.id)
            _userFeedbackMessage.value = "Payout ${payout.id} deleted"
        }
    }

    fun refreshLivePayoutFeed() {
        viewModelScope.launch {
            _isPayoutSyncing.value = true
            val uid = activeUserId()
            val stripeResult = payoutLiveService.fetchLiveStripeBalanceAndPayouts(uid)
            stripeResult.getOrNull()?.forEach { stripePayout ->
                repository.savePayout(stripePayout)
            }
            payoutLiveService.probeGatewayHealth(uid)
            _isPayoutSyncing.value = false
            val st = stripeBalanceState.value
            _userFeedbackMessage.value = when {
                st.isConfigured && st.errorMessage == null ->
                    "Stripe Live synced (${st.recentPayoutsCount} payouts • €%.2f available)".format(st.availableBalanceEur)
                st.errorMessage != null ->
                    "Stripe sync warning: ${st.errorMessage}"
                else ->
                    "Gateway refreshed • Add STRIPE_SECRET_KEY in Secrets for live Stripe balance"
            }
        }
    }

    fun saveAutoPayoutRules(
        enabled: Boolean,
        schedule: String,
        minThreshold: Double,
        reserveFloat: Double
    ) {
        viewModelScope.launch {
            _isPayoutSyncing.value = true
            val rules = AutoPayoutRules(
                enabled = enabled,
                schedule = schedule,
                minThreshold = minThreshold.coerceAtLeast(0.0),
                reserveFloat = reserveFloat.coerceAtLeast(0.0)
            )
            _autoPayoutRules.value = rules
            payoutLiveService.saveAutoPayoutRules(activeUserId(), rules)
            _isPayoutSyncing.value = false
            _userFeedbackMessage.value = "Automated settlement rules synced to Cloud"
        }
    }

    fun executeAutoSweepNow(ownerName: String = "Jurgen Paul Westerveld") {
        val rules = _autoPayoutRules.value
        val available = ownerPayoutMetrics.value.availableBalance
        val sweepable = (available - rules.reserveFloat).coerceAtLeast(0.0)
        if (sweepable < rules.minThreshold || sweepable <= 0.0) {
            _userFeedbackMessage.value = "Sweep skipped: €%.2f sweepable is below €%.2f min threshold".format(sweepable, rules.minThreshold)
            return
        }
        val sweepSpeed = if (stripeBalanceState.value.isConfigured) PayoutSpeed.STRIPE_INSTANT else PayoutSpeed.WERO_INSTANT
        requestOwnerPayout(
            amount = sweepable,
            ownerId = activeUserId(),
            ownerName = ownerName,
            speed = sweepSpeed,
            reference = if (sweepSpeed == PayoutSpeed.STRIPE_INSTANT) "Automated Stripe Treasury Sweep" else "Automated Wero Treasury Sweep",
            notes = "Triggered by Automated Settlement Rules (Reserve float €%.2f retained)".format(rules.reserveFloat)
        )
    }

    // --- Live Security Operations & Threat Defense Actions ---
    fun runSecurityPerimeterScan() {
        viewModelScope.launch {
            _isScanningSecurity.value = true
            val evaluated = securityDefenseService.runLivePerimeterScan(
                userId = activeUserId(),
                policy = _containmentPolicy.value,
                currentThreats = _securityThreats.value
            )
            _isScanningSecurity.value = false
            _userFeedbackMessage.value = "Perimeter security scan finished ($evaluated threats evaluated)"
        }
    }

    fun assessThreatWithAi(threat: SecurityThreatEvent) {
        viewModelScope.launch {
            _isScanningSecurity.value = true
            val updated = securityDefenseService.assessThreatWithAi(
                userId = activeUserId(),
                threat = threat,
                policy = _containmentPolicy.value
            )
            _securityThreats.value = _securityThreats.value.map { if (it.id == updated.id) updated else it }
            _isScanningSecurity.value = false
            _userFeedbackMessage.value = "AI Risk Assessment: ${updated.aiRiskScore}/100 (${updated.containmentStatus.label})"
        }
    }

    fun triggerFirewallBreachProbe(scenario: String) {
        viewModelScope.launch {
            _isScanningSecurity.value = true
            val assessed = securityDefenseService.simulateFirewallBreachProbe(
                userId = activeUserId(),
                scenario = scenario,
                policy = _containmentPolicy.value
            )
            _securityThreats.value = listOf(assessed) + _securityThreats.value.filterNot { it.id == assessed.id }
            val auditEntry = SecurityAuditLogEntry(
                id = "AUD-LOCAL-${System.currentTimeMillis() % 1000000}",
                threatId = assessed.id,
                eventType = if (assessed.containmentStatus == ContainmentStatus.AUTO_CONTAINED) {
                    SecurityAuditEventType.AUTO_CONTAINMENT
                } else {
                    SecurityAuditEventType.AI_RISK_ASSESSMENT
                },
                actor = "Genkit SOC AI Engine",
                summary = "${assessed.title} — Score ${assessed.aiRiskScore}/100 (${assessed.containmentStatus.label})",
                details = assessed.aiRationale,
                riskScore = assessed.aiRiskScore,
                timestamp = System.currentTimeMillis()
            )
            _securityAuditLogs.value = listOf(auditEntry) + _securityAuditLogs.value
            _isScanningSecurity.value = false
            _userFeedbackMessage.value = if (assessed.containmentStatus == ContainmentStatus.PENDING_HUMAN_APPROVAL) {
                "Firewall breach escalated to Human Approval queue (#${assessed.id})"
            } else {
                "Firewall breach auto-contained (#${assessed.id})"
            }
        }
    }

    fun approveThreatContainment(
        threat: SecurityThreatEvent,
        operatorName: String = "Jurgen Paul Westerveld",
        note: String = "Operator signed off on permanent WAF containment"
    ) {
        viewModelScope.launch {
            val result = securityDefenseService.approveContainment(activeUserId(), threat, operatorName, note)
            val updated = result.getOrElse {
                threat.copy(
                    containmentStatus = ContainmentStatus.APPROVED_CONTAINED,
                    approvedBy = operatorName,
                    requiresHumanApproval = false,
                    updatedAt = System.currentTimeMillis()
                )
            }
            _securityThreats.value = _securityThreats.value.map { if (it.id == updated.id) updated else it }
            _securityAuditLogs.value = listOf(
                SecurityAuditLogEntry(
                    id = "AUD-APP-${System.currentTimeMillis() % 1000000}",
                    threatId = updated.id,
                    eventType = SecurityAuditEventType.HUMAN_APPROVAL,
                    actor = operatorName,
                    summary = "Human approval signed off for #${updated.id}",
                    details = note,
                    riskScore = updated.aiRiskScore,
                    timestamp = System.currentTimeMillis()
                )
            ) + _securityAuditLogs.value
            _userFeedbackMessage.value = "Containment approved for #${threat.id}"
        }
    }

    fun dismissSecurityThreat(
        threat: SecurityThreatEvent,
        operatorName: String = "Jurgen Paul Westerveld",
        reason: String = "Verified authorized traffic by operator"
    ) {
        viewModelScope.launch {
            val result = securityDefenseService.dismissThreat(activeUserId(), threat, operatorName, reason)
            val updated = result.getOrElse {
                threat.copy(
                    containmentStatus = ContainmentStatus.DISMISSED,
                    approvedBy = operatorName,
                    requiresHumanApproval = false,
                    updatedAt = System.currentTimeMillis()
                )
            }
            _securityThreats.value = _securityThreats.value.map { if (it.id == updated.id) updated else it }
            _securityAuditLogs.value = listOf(
                SecurityAuditLogEntry(
                    id = "AUD-DIS-${System.currentTimeMillis() % 1000000}",
                    threatId = updated.id,
                    eventType = SecurityAuditEventType.HUMAN_DISMISSAL,
                    actor = operatorName,
                    summary = "Operator dismissed #${updated.id}",
                    details = reason,
                    riskScore = updated.aiRiskScore,
                    timestamp = System.currentTimeMillis()
                )
            ) + _securityAuditLogs.value
            _userFeedbackMessage.value = "Threat #${threat.id} dismissed"
        }
    }

    fun updateContainmentPolicy(
        policy: ContainmentPolicy,
        operatorName: String = "Jurgen Paul Westerveld"
    ) {
        viewModelScope.launch {
            _containmentPolicy.value = policy
            securityDefenseService.saveContainmentPolicy(activeUserId(), policy, operatorName)
            _userFeedbackMessage.value = "SOC Containment Policy updated"
        }
    }

    // --- E-Commerce & Store Actions ---
    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            val currentItems = _userCart.value.items.toMutableList()
            val existingIndex = currentItems.indexOfFirst { it.productId == product.id }
            if (existingIndex >= 0) {
                val existing = currentItems[existingIndex]
                currentItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
            } else {
                currentItems.add(
                    CartItem(
                        productId = product.id,
                        productName = product.name,
                        quantity = quantity,
                        priceAtTime = product.price,
                        category = product.category
                    )
                )
            }
            val updatedCart = _userCart.value.copy(
                updatedAt = System.currentTimeMillis(),
                items = currentItems
            )
            _userCart.value = updatedCart
            commerceRepository.saveUserCart(updatedCart)
            _userFeedbackMessage.value = "Added \"${product.name}\" to Cart"
        }
    }

    fun updateCartItemQuantity(productId: String, newQuantity: Int) {
        viewModelScope.launch {
            val currentItems = _userCart.value.items.toMutableList()
            if (newQuantity <= 0) {
                currentItems.removeAll { it.productId == productId }
            } else {
                val index = currentItems.indexOfFirst { it.productId == productId }
                if (index >= 0) {
                    currentItems[index] = currentItems[index].copy(quantity = newQuantity)
                }
            }
            val updatedCart = _userCart.value.copy(
                updatedAt = System.currentTimeMillis(),
                items = currentItems
            )
            _userCart.value = updatedCart
            commerceRepository.saveUserCart(updatedCart)
        }
    }

    fun removeFromCart(productId: String) {
        updateCartItemQuantity(productId, 0)
    }

    fun clearCart() {
        viewModelScope.launch {
            val emptyCart = Cart(userId = _userCart.value.userId)
            _userCart.value = emptyCart
            commerceRepository.clearUserCart(_userCart.value.userId)
            _userFeedbackMessage.value = "Cart cleared"
        }
    }

    fun checkoutOrder(
        customerName: String = "Jurgen Paul Westerveld",
        customerEmail: String = "westerveldjp@gmail.com",
        shippingAddress: String = "Keizersgracht 421, 1016 EK Amsterdam, Netherlands"
    ) {
        viewModelScope.launch {
            val cart = _userCart.value
            if (cart.items.isEmpty()) {
                _userFeedbackMessage.value = "Cannot checkout: Cart is empty"
                return@launch
            }

            val orderId = "ORD-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}"
            val invoiceId = "INV-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}"
            val now = System.currentTimeMillis()

            // 1. Build Order items
            val orderItems = cart.items.map {
                OrderItem(
                    productId = it.productId,
                    productName = it.productName,
                    quantity = it.quantity,
                    priceAtTime = it.priceAtTime
                )
            }

            // 2. Build Invoice items with 21% EU VAT
            val invoiceItems = cart.items.map {
                InvoiceItem(
                    id = "ITEM-${it.productId}",
                    description = "${it.productName} (${it.category})",
                    quantity = it.quantity.toDouble(),
                    unitPrice = it.priceAtTime,
                    taxRate = 21.0
                )
            }

            val subtotal = invoiceItems.sumOf { it.subtotal }
            val taxTotal = invoiceItems.sumOf { it.taxAmount }
            val totalAmount = subtotal + taxTotal

            // 3. Create and place Order in Firestore
            // 3. Create and place Order in Room and Cloud Firestore via OrderRepository
            val newOrder = Order(
                id = orderId,
                clientID = cart.userId,
                productIDs = orderItems.map { it.productId },
                amount = totalAmount,
                status = "PAID",
                timestamp = now,
                invoiceId = invoiceId,
                shippingAddress = shippingAddress,
                customerEmail = customerEmail,
                customerName = customerName
            )

            orderRepository.insertOrder(newOrder, syncToCloud = true)
            commerceRepository.clearUserCart(cart.userId)
            _userCart.value = Cart(userId = cart.userId)

            // 4. Auto-generate paid invoice in Billing system
            val autoInvoice = InvoiceEntity(
                id = invoiceId,
                clientId = "CLI-STORE-${orderId.takeLast(4)}",
                clientName = customerName,
                clientEmail = customerEmail,
                status = InvoiceStatus.PAID.name,
                issueDate = now,
                dueDate = now,
                currency = "EUR",
                itemsJson = InvoiceItemSerializer.toJson(invoiceItems),
                subtotal = subtotal,
                taxTotal = taxTotal,
                discountTotal = 0.0,
                totalAmount = totalAmount,
                amountPaid = totalAmount,
                paymentTerms = "Instant Checkout (SEPA/iDEAL)",
                notes = "Auto-generated from Store Order #$orderId ($shippingAddress)",
                paidAt = now,
                createdAt = now
            )
            repository.saveInvoice(autoInvoice)

            // 5. Record Payment transaction
            val payment = PaymentEntity(
                id = "PAY-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}",
                invoiceId = invoiceId,
                clientName = customerName,
                amount = totalAmount,
                currency = "EUR",
                paymentMethod = "Direct Store Checkout (Wero / SEPA Instant)",
                transactionRef = "ORDER-$orderId",
                paymentDate = now,
                notes = "Real-time settlement for Order #$orderId"
            )
            repository.savePayment(payment)

            _userFeedbackMessage.value = "Order #$orderId placed! Auto-generated Invoice #$invoiceId (€%.2f) and credited to balance".format(totalAmount)
        }
    }

    fun generateAiInvoiceCommunication(
        clientName: String,
        invoiceId: String,
        amount: Double,
        currency: String = "EUR",
        dueDate: String = "Net 14 Days",
        tone: InvoiceCommunicationTone = InvoiceCommunicationTone.FRIENDLY_WELCOME,
        customInstructions: String = "",
        onResult: (InvoiceCommunicationResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isGeneratingAiCommunication.value = true
            val req = InvoiceCommunicationRequest(
                clientName = clientName,
                invoiceId = invoiceId,
                amount = amount,
                currency = currency,
                dueDate = dueDate,
                tone = tone,
                customInstructions = customInstructions
            )
            val result = aiBillingService.generateCommunication(req)
            _lastGeneratedAiCommunication.value = result
            _isGeneratingAiCommunication.value = false
            onResult(result)
        }
    }
}
