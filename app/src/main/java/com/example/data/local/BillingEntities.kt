package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BillingInterval
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceItemSerializer
import com.example.data.model.InvoiceStatus
import com.example.data.model.SubscriptionStatus

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey
    val id: String = "CLI-${System.currentTimeMillis() % 100000}",
    val name: String,
    val contactPerson: String = "",
    val email: String = "",
    val phone: String = "",
    val taxNumber: String = "", // VAT / Tax ID
    val address: String = "",
    val country: String = "Netherlands",
    val currency: String = "EUR",
    val paymentTermsDays: Int = 14,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey
    val id: String = "INV-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}",
    val clientId: String,
    val clientName: String,
    val clientEmail: String = "",
    val status: String = InvoiceStatus.PENDING.name, // PAID, PENDING, OVERDUE, DRAFT
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + (14L * 24 * 60 * 60 * 1000),
    val currency: String = "EUR",
    val itemsJson: String = "[]",
    val subtotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val discountTotal: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentTerms: String = "Net 14 Days",
    val notes: String = "",
    val paidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val items: List<InvoiceItem>
        get() = InvoiceItemSerializer.fromJson(itemsJson)

    val invoiceStatus: InvoiceStatus
        get() {
            val s = InvoiceStatus.fromString(status)
            if (s == InvoiceStatus.PENDING && dueDate < System.currentTimeMillis() && amountPaid < totalAmount) {
                return InvoiceStatus.OVERDUE
            }
            return s
        }

    val balanceDue: Double
        get() = (totalAmount - amountPaid).coerceAtLeast(0.0)
}

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey
    val id: String = "SUB-${System.currentTimeMillis() % 100000}",
    val clientId: String,
    val clientName: String,
    val planName: String,
    val amount: Double,
    val currency: String = "EUR",
    val interval: String = BillingInterval.MONTHLY.name,
    val status: String = SubscriptionStatus.ACTIVE.name,
    val nextBillingDate: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val subscriptionStatus: SubscriptionStatus
        get() = SubscriptionStatus.fromString(status)

    val billingInterval: BillingInterval
        get() = BillingInterval.fromString(interval)
}

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey
    val id: String = "PAY-${System.currentTimeMillis() % 100000}",
    val invoiceId: String,
    val clientName: String,
    val amount: Double,
    val currency: String = "EUR",
    val paymentMethod: String = "Bank Transfer (SEPA)", // iDEAL, Credit Card, Bank Transfer, Stripe, PayPal
    val transactionRef: String = "",
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)

enum class PayoutStatus(val label: String) {
    COMPLETED("Completed"),
    PROCESSING("In Transit"),
    SCHEDULED("Scheduled"),
    FAILED("Failed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): PayoutStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: COMPLETED
        }
    }
}

enum class PayoutSpeed(val label: String, val feeEuro: Double, val etaDescription: String) {
    INSTANT("Instant SEPA", 0.0, "Within minutes"),
    STANDARD("Standard SEPA", 0.0, "1-2 business days");

    companion object {
        fun fromString(value: String): PayoutSpeed {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: INSTANT
        }
    }
}

@Entity(tableName = "payouts")
data class PayoutEntity(
    @PrimaryKey
    val id: String = "PO-${(System.currentTimeMillis() % 1000000).toString().padStart(6, '0')}",
    val ownerId: String = "jurgen-westerveld",
    val ownerName: String = "Jurgen Paul Westerveld",
    val amount: Double,
    val currency: String = "EUR",
    val fee: Double = 0.0,
    val netAmount: Double = amount - fee,
    val status: String = PayoutStatus.COMPLETED.name,
    val destinationBank: String = "ING Bank N.V.",
    val destinationIban: String = "NL91 INGB 0412 8923 00",
    val destinationBic: String = "INGBNL2A",
    val payoutSpeed: String = PayoutSpeed.INSTANT.name,
    val reference: String = "Owner Draw / Profit Distribution",
    val initiatedAt: Long = System.currentTimeMillis(),
    val estimatedArrivalAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = System.currentTimeMillis(),
    val notes: String = ""
) {
    val payoutStatus: PayoutStatus
        get() = PayoutStatus.fromString(status)

    val speed: PayoutSpeed
        get() = PayoutSpeed.fromString(payoutSpeed)
}
