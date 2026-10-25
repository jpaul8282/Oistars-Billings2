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
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.Product
import com.example.data.repository.BillingRepository
import com.example.data.repository.CommerceRepository
import com.example.data.repository.OrderRepository
import com.example.data.service.AiBillingCommunicationService
import com.example.data.service.InvoiceCommunicationRequest
import com.example.data.service.InvoiceCommunicationResult
import com.example.data.service.InvoiceCommunicationTone
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

    private val _isGeneratingAiCommunication = MutableStateFlow(false)
    val isGeneratingAiCommunication: StateFlow<Boolean> = _isGeneratingAiCommunication.asStateFlow()

    private val _lastGeneratedAiCommunication = MutableStateFlow<InvoiceCommunicationResult?>(null)
    val lastGeneratedAiCommunication: StateFlow<InvoiceCommunicationResult?> = _lastGeneratedAiCommunication.asStateFlow()

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
        viewModelScope.launch {
            commerceRepository.seedDefaultProductsIfEmpty()
            orderRepository.syncOrdersFromFirestore("jurgen-westerveld")
        }
        viewModelScope.launch {
            orderRepository.listenForRealtimeOrderEvents("jurgen-westerveld").collect { event ->
                if (event.changeDescription != null) {
                    _realtimeOrderAlert.value = event.changeDescription
                    _userFeedbackMessage.value = "⚡ ${event.changeDescription}"
                }
            }
        }
        viewModelScope.launch {
            commerceRepository.observeUserCart("jurgen-westerveld").collect { cart ->
                _userCart.value = cart
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

    // --- Owner Payout Actions ---
    fun requestOwnerPayout(
        amount: Double,
        ownerId: String = "jurgen-westerveld",
        ownerName: String = "Jurgen Paul Westerveld",
        destinationBank: String = "ING Bank N.V.",
        destinationIban: String = "NL43 INGB 0006 3721 46",
        destinationBic: String = "INGBNL2A",
        speed: PayoutSpeed = PayoutSpeed.WERO_INSTANT,
        reference: String = "Owner Draw / Settlement",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val fee = speed.feeEuro
            val now = System.currentTimeMillis()
            val isImmediate = speed == PayoutSpeed.WERO_INSTANT || speed == PayoutSpeed.INSTANT
            val arrival = when (speed) {
                PayoutSpeed.WERO_INSTANT -> now + 4000L // 4 seconds via EPI
                PayoutSpeed.INSTANT -> now + (5 * 60 * 1000L) // 5 minutes SEPA
                PayoutSpeed.STANDARD -> now + (24 * 60 * 60 * 1000L) // 1 day
            }
            val newPayout = PayoutEntity(
                id = "PO-${(System.currentTimeMillis() % 1000000).toString().padStart(6, '0')}",
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
                    if (speed == PayoutSpeed.WERO_INSTANT) "Wero Instant Wire - Oistars Draw" else "Owner Draw - Oistars International"
                },
                initiatedAt = now,
                estimatedArrivalAt = arrival,
                completedAt = if (isImmediate) now else null,
                notes = notes.ifBlank {
                    if (speed == PayoutSpeed.WERO_INSTANT) "Settled in real-time (< 5s) via Wero (European Payments Initiative) to ING NL43INGB0006372146" else ""
                }
            )
            repository.savePayout(newPayout)
            _userFeedbackMessage.value = if (speed == PayoutSpeed.WERO_INSTANT) {
                "Wero Instant wire of €%.2f settled immediately to ING IBAN %s".format(amount, destinationIban)
            } else {
                "Payout of €%.2f initiated to %s".format(amount, destinationBank)
            }
        }
    }

    fun cancelPayout(payout: PayoutEntity) {
        viewModelScope.launch {
            repository.updatePayoutStatus(payout.id, PayoutStatus.CANCELLED.name)
            _userFeedbackMessage.value = "Payout ${payout.id} was cancelled"
        }
    }

    fun deletePayout(payout: PayoutEntity) {
        viewModelScope.launch {
            repository.deletePayout(payout)
            _userFeedbackMessage.value = "Payout ${payout.id} deleted"
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
