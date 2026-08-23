package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceItemSerializer
import com.example.data.model.InvoiceStatus
import kotlinx.coroutines.flow.Flow

class BillingRepository(private val database: AppDatabase) {
    private val clientDao = database.clientDao()
    private val invoiceDao = database.invoiceDao()
    private val subscriptionDao = database.subscriptionDao()
    private val paymentDao = database.paymentDao()

    // --- Clients ---
    val allClients: Flow<List<ClientEntity>> = clientDao.getAllClients()

    suspend fun getClientById(id: String): ClientEntity? = clientDao.getClientById(id)

    suspend fun saveClient(client: ClientEntity) {
        clientDao.insertClient(client)
    }

    suspend fun updateClient(client: ClientEntity) {
        clientDao.updateClient(client)
    }

    suspend fun deleteClient(client: ClientEntity) {
        clientDao.deleteClient(client)
    }

    // --- Invoices ---
    val allInvoices: Flow<List<InvoiceEntity>> = invoiceDao.getAllInvoices()

    suspend fun getInvoiceById(id: String): InvoiceEntity? = invoiceDao.getInvoiceById(id)

    suspend fun saveInvoice(invoice: InvoiceEntity) {
        invoiceDao.insertInvoice(invoice)
    }

    suspend fun updateInvoice(invoice: InvoiceEntity) {
        invoiceDao.updateInvoice(invoice)
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) {
        invoiceDao.deleteInvoice(invoice)
    }

    suspend fun recordPayment(
        invoice: InvoiceEntity,
        paymentAmount: Double,
        paymentMethod: String,
        transactionRef: String,
        notes: String
    ) {
        val newAmountPaid = invoice.amountPaid + paymentAmount
        val isFullyPaid = newAmountPaid >= invoice.totalAmount - 0.01
        val newStatus = if (isFullyPaid) InvoiceStatus.PAID.name else InvoiceStatus.PENDING.name
        val paidTimestamp = if (isFullyPaid) System.currentTimeMillis() else invoice.paidAt

        // 1. Update Invoice status & amountPaid
        invoiceDao.updateInvoicePaymentStatus(
            invoiceId = invoice.id,
            status = newStatus,
            paidAt = paidTimestamp,
            amountPaid = newAmountPaid
        )

        // 2. Insert Payment Record
        val payment = PaymentEntity(
            id = "PAY-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}",
            invoiceId = invoice.id,
            clientName = invoice.clientName,
            amount = paymentAmount,
            currency = invoice.currency,
            paymentMethod = paymentMethod,
            transactionRef = transactionRef.ifBlank { "TRX-${System.currentTimeMillis() % 1000000}" },
            paymentDate = System.currentTimeMillis(),
            notes = notes
        )
        paymentDao.insertPayment(payment)
    }

    suspend fun markInvoiceAsPaid(invoice: InvoiceEntity) {
        val remaining = (invoice.totalAmount - invoice.amountPaid).coerceAtLeast(0.0)
        if (remaining > 0) {
            recordPayment(
                invoice = invoice,
                paymentAmount = remaining,
                paymentMethod = "Manual Mark as Paid",
                transactionRef = "MANUAL-${System.currentTimeMillis() % 10000}",
                notes = "Marked as fully settled directly in dashboard"
            )
        } else {
            invoiceDao.updateInvoicePaymentStatus(
                invoiceId = invoice.id,
                status = InvoiceStatus.PAID.name,
                paidAt = System.currentTimeMillis(),
                amountPaid = invoice.totalAmount
            )
        }
    }

    // --- Subscriptions ---
    val allSubscriptions: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    suspend fun saveSubscription(subscription: SubscriptionEntity) {
        subscriptionDao.insertSubscription(subscription)
    }

    suspend fun updateSubscription(subscription: SubscriptionEntity) {
        subscriptionDao.updateSubscription(subscription)
    }

    suspend fun deleteSubscription(subscription: SubscriptionEntity) {
        subscriptionDao.deleteSubscription(subscription)
    }

    suspend fun generateInvoiceFromSubscription(subscription: SubscriptionEntity): InvoiceEntity {
        val items = listOf(
            InvoiceItem(
                description = "${subscription.planName} (${subscription.billingInterval.label} Retainer)",
                quantity = 1.0,
                unitPrice = subscription.amount,
                taxRate = 21.0
            )
        )
        val subtotal = items.sumOf { it.subtotal }
        val tax = items.sumOf { it.taxAmount }
        val total = subtotal + tax

        val client = clientDao.getClientById(subscription.clientId)
        val days = client?.paymentTermsDays ?: 14
        val now = System.currentTimeMillis()

        val invoice = InvoiceEntity(
            id = "INV-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}",
            clientId = subscription.clientId,
            clientName = subscription.clientName,
            clientEmail = client?.email ?: "",
            status = InvoiceStatus.PENDING.name,
            issueDate = now,
            dueDate = now + (days.toLong() * 24 * 60 * 60 * 1000),
            currency = subscription.currency,
            itemsJson = InvoiceItemSerializer.toJson(items),
            subtotal = subtotal,
            taxTotal = tax,
            discountTotal = 0.0,
            totalAmount = total,
            amountPaid = 0.0,
            paymentTerms = "Net $days Days",
            notes = "Auto-generated recurring subscription billing for plan: ${subscription.planName}.",
            paidAt = null,
            createdAt = now
        )

        invoiceDao.insertInvoice(invoice)

        // Advance next billing date
        val advanceMs = subscription.billingInterval.months * 30L * 24 * 60 * 60 * 1000
        val updatedSub = subscription.copy(nextBillingDate = subscription.nextBillingDate + advanceMs)
        subscriptionDao.updateSubscription(updatedSub)

        return invoice
    }

    // --- Payments ---
    val allPayments: Flow<List<PaymentEntity>> = paymentDao.getAllPayments()

    suspend fun savePayment(payment: PaymentEntity) {
        paymentDao.insertPayment(payment)
    }
}
