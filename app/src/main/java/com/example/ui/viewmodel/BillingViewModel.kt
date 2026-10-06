package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.model.BillingInterval
import com.example.data.model.ClientActivityItem
import com.example.data.model.ClientActivityType
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceItemSerializer
import com.example.data.model.InvoiceStatus
import com.example.data.repository.BillingRepository
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

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = BillingRepository(database)

    // Data streams from Room
    val allInvoices: StateFlow<List<InvoiceEntity>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClients: StateFlow<List<ClientEntity>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubscriptions: StateFlow<List<SubscriptionEntity>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
