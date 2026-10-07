package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BillingInterval
import com.example.data.model.InvoiceItem
import com.example.data.model.InvoiceItemSerializer
import com.example.data.model.InvoiceStatus
import com.example.data.model.SubscriptionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ClientEntity::class,
        InvoiceEntity::class,
        SubscriptionEntity::class,
        PaymentEntity::class,
        PayoutEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun paymentDao(): PaymentDao
    abstract fun payoutDao(): PayoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "oistars_billings_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    try {
                        val cursor = db.query("SELECT COUNT(*) FROM clients")
                        var count = 0
                        if (cursor.moveToFirst()) {
                            count = cursor.getInt(0)
                        }
                        cursor.close()
                        if (count == 0) {
                            populateInitialData(database)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val clientDao = database.clientDao()
            val invoiceDao = database.invoiceDao()
            val subscriptionDao = database.subscriptionDao()
            val paymentDao = database.paymentDao()

            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000

            // 1. Seed Clients
            val c1 = ClientEntity(
                id = "CLI-1001",
                name = "Nordic Gourmet Brands B.V.",
                contactPerson = "Lars Lindqvist",
                email = "accounting@nordicgourmet.eu",
                phone = "+31 20 555 0192",
                taxNumber = "NL892019482B01",
                address = "Keizersgracht 421, 1016 EK Amsterdam",
                country = "Netherlands",
                currency = "EUR",
                paymentTermsDays = 14,
                notes = "Key enterprise account: multi-store oyster & seafood e-commerce franchise.",
                createdAt = now - (60 * dayMs)
            )

            val c2 = ClientEntity(
                id = "CLI-1002",
                name = "Artisan Pearl Boutique",
                contactPerson = "Sophie de Vries",
                email = "billing@artisanpearl.nl",
                phone = "+31 10 442 8831",
                taxNumber = "NL746201948B02",
                address = "Witte de Withstraat 88, 3012 BT Rotterdam",
                country = "Netherlands",
                currency = "EUR",
                paymentTermsDays = 30,
                notes = "High-end luxury sea-pearl accessories e-shop.",
                createdAt = now - (45 * dayMs)
            )

            val c3 = ClientEntity(
                id = "CLI-1003",
                name = "Oceanic Delicacies SA",
                contactPerson = "Marc Dubois",
                email = "finance@oceanicdelicacies.be",
                phone = "+32 2 710 4400",
                taxNumber = "BE0849201934",
                address = "Avenue Louise 240, 1050 Brussels",
                country = "Belgium",
                currency = "EUR",
                paymentTermsDays = 14,
                notes = "Fresh oyster import & restaurant supply platform.",
                createdAt = now - (30 * dayMs)
            )

            val c4 = ClientEntity(
                id = "CLI-1004",
                name = "Zeeland Fresh Seafoods",
                contactPerson = "Kees van der Meer",
                email = "orders@zeelandfresh.com",
                phone = "+31 11 865 1200",
                taxNumber = "NL654019283B01",
                address = "Havendijk 14, 4331 PR Middelburg",
                country = "Netherlands",
                currency = "EUR",
                paymentTermsDays = 14,
                notes = "Direct-to-consumer chilled oyster subscription service.",
                createdAt = now - (20 * dayMs)
            )

            clientDao.insertClients(listOf(c1, c2, c3, c4))

            // 2. Seed Invoices
            val items1 = listOf(
                InvoiceItem(description = "E-Commerce Gateway & Brand Integration Setup", quantity = 1.0, unitPrice = 1850.00, taxRate = 21.0),
                InvoiceItem(description = "Monthly Managed Cloud Hosting & SLA Support", quantity = 1.0, unitPrice = 450.00, taxRate = 21.0)
            )
            val sub1 = items1.sumOf { it.subtotal }
            val tax1 = items1.sumOf { it.taxAmount }
            val total1 = sub1 + tax1

            val inv1 = InvoiceEntity(
                id = "INV-2026-081",
                clientId = c1.id,
                clientName = c1.name,
                clientEmail = c1.email,
                status = InvoiceStatus.PAID.name,
                issueDate = now - (25 * dayMs),
                dueDate = now - (11 * dayMs),
                currency = "EUR",
                itemsJson = InvoiceItemSerializer.toJson(items1),
                subtotal = sub1,
                taxTotal = tax1,
                discountTotal = 0.0,
                totalAmount = total1,
                amountPaid = total1,
                paymentTerms = "Net 14 Days",
                notes = "Thank you for partnering with Oistars Billings Platform.",
                paidAt = now - (12 * dayMs),
                createdAt = now - (25 * dayMs)
            )

            val items2 = listOf(
                InvoiceItem(description = "Automated Order Processing Module Q3", quantity = 1.0, unitPrice = 1200.00, taxRate = 21.0),
                InvoiceItem(description = "Multi-Currency Checkout Expansion (USD/GBP)", quantity = 1.0, unitPrice = 650.00, taxRate = 21.0),
                InvoiceItem(description = "Payment Fraud Protection Firewall", quantity = 1.0, unitPrice = 300.00, taxRate = 21.0)
            )
            val sub2 = items2.sumOf { it.subtotal }
            val tax2 = items2.sumOf { it.taxAmount }
            val total2 = sub2 + tax2

            val inv2 = InvoiceEntity(
                id = "INV-2026-094",
                clientId = c2.id,
                clientName = c2.name,
                clientEmail = c2.email,
                status = InvoiceStatus.PENDING.name,
                issueDate = now - (5 * dayMs),
                dueDate = now + (9 * dayMs),
                currency = "EUR",
                itemsJson = InvoiceItemSerializer.toJson(items2),
                subtotal = sub2,
                taxTotal = tax2,
                discountTotal = 0.0,
                totalAmount = total2,
                amountPaid = 0.0,
                paymentTerms = "Net 14 Days",
                notes = "Payment via iDEAL or SEPA Bank Transfer to Oistars NL account.",
                paidAt = null,
                createdAt = now - (5 * dayMs)
            )

            val items3 = listOf(
                InvoiceItem(description = "High-Volume Transaction Processing Fee (July)", quantity = 1420.0, unitPrice = 0.85, taxRate = 21.0),
                InvoiceItem(description = "Dedicated Account Manager SLA", quantity = 1.0, unitPrice = 500.00, taxRate = 21.0)
            )
            val sub3 = items3.sumOf { it.subtotal }
            val tax3 = items3.sumOf { it.taxAmount }
            val total3 = sub3 + tax3

            val inv3 = InvoiceEntity(
                id = "INV-2026-099",
                clientId = c3.id,
                clientName = c3.name,
                clientEmail = c3.email,
                status = InvoiceStatus.PENDING.name,
                issueDate = now - (18 * dayMs),
                dueDate = now - (4 * dayMs), // Overdue
                currency = "EUR",
                itemsJson = InvoiceItemSerializer.toJson(items3),
                subtotal = sub3,
                taxTotal = tax3,
                discountTotal = 0.0,
                totalAmount = total3,
                amountPaid = 0.0,
                paymentTerms = "Net 14 Days",
                notes = "Second reminder sent on overdue balance.",
                paidAt = null,
                createdAt = now - (18 * dayMs)
            )

            val items4 = listOf(
                InvoiceItem(description = "Weekly Fresh Catch Subscription Billing Engine", quantity = 1.0, unitPrice = 950.00, taxRate = 21.0),
                InvoiceItem(description = "Customer Portal & Self-Service Invoicing", quantity = 1.0, unitPrice = 400.00, taxRate = 21.0)
            )
            val sub4 = items4.sumOf { it.subtotal }
            val tax4 = items4.sumOf { it.taxAmount }
            val total4 = sub4 + tax4

            val inv4 = InvoiceEntity(
                id = "INV-2026-102",
                clientId = c4.id,
                clientName = c4.name,
                clientEmail = c4.email,
                status = InvoiceStatus.DRAFT.name,
                issueDate = now,
                dueDate = now + (14 * dayMs),
                currency = "EUR",
                itemsJson = InvoiceItemSerializer.toJson(items4),
                subtotal = sub4,
                taxTotal = tax4,
                discountTotal = 0.0,
                totalAmount = total4,
                amountPaid = 0.0,
                paymentTerms = "Net 14 Days",
                notes = "Draft awaiting client confirmation on order volume.",
                paidAt = null,
                createdAt = now
            )

            invoiceDao.insertInvoices(listOf(inv1, inv2, inv3, inv4))

            // 3. Seed Subscriptions (Retainers)
            val s1 = SubscriptionEntity(
                id = "SUB-5001",
                clientId = c1.id,
                clientName = c1.name,
                planName = "Enterprise Multi-Brand Retainer",
                amount = 1450.00,
                currency = "EUR",
                interval = BillingInterval.MONTHLY.name,
                status = SubscriptionStatus.ACTIVE.name,
                nextBillingDate = now + (6 * dayMs),
                notes = "Includes priority webhook monitoring & uptime SLA.",
                createdAt = now - (60 * dayMs)
            )

            val s2 = SubscriptionEntity(
                id = "SUB-5002",
                clientId = c2.id,
                clientName = c2.name,
                planName = "Boutique Webshop Maintenance & Security",
                amount = 450.00,
                currency = "EUR",
                interval = BillingInterval.MONTHLY.name,
                status = SubscriptionStatus.ACTIVE.name,
                nextBillingDate = now + (12 * dayMs),
                notes = "Automated daily catalog sync & checkout security.",
                createdAt = now - (45 * dayMs)
            )

            val s3 = SubscriptionEntity(
                id = "SUB-5003",
                clientId = c4.id,
                clientName = c4.name,
                planName = "Direct Seafood Box Subscription Gateway",
                amount = 680.00,
                currency = "EUR",
                interval = BillingInterval.MONTHLY.name,
                status = SubscriptionStatus.ACTIVE.name,
                nextBillingDate = now + (18 * dayMs),
                notes = "Automated recurring customer SEPA debit batches.",
                createdAt = now - (20 * dayMs)
            )

            subscriptionDao.insertSubscriptions(listOf(s1, s2, s3))

            // 4. Seed Payments
            val p1 = PaymentEntity(
                id = "PAY-8001",
                invoiceId = inv1.id,
                clientName = inv1.clientName,
                amount = total1,
                currency = "EUR",
                paymentMethod = "iDEAL (ABN AMRO)",
                transactionRef = "TRX-OISTAR-992418",
                paymentDate = now - (12 * dayMs),
                notes = "Full settlement for invoice INV-2026-081"
            )

            paymentDao.insertPayments(listOf(p1))

            // 5. Seed Owner Payouts for Jurgen Paul Westerveld
            val payoutDao = database.payoutDao()
            val po1 = PayoutEntity(
                id = "PO-920194",
                ownerId = "jurgen-westerveld",
                ownerName = "Jurgen Paul Westerveld",
                amount = 2300.00,
                currency = "EUR",
                fee = 0.0,
                netAmount = 2300.00,
                status = PayoutStatus.COMPLETED.name,
                destinationBank = "ING Bank N.V.",
                destinationIban = "NL91 INGB 0412 8923 00",
                destinationBic = "INGBNL2A",
                payoutSpeed = PayoutSpeed.INSTANT.name,
                reference = "Bi-weekly Owner Profit Draw #41",
                initiatedAt = now - (10 * dayMs),
                estimatedArrivalAt = now - (10 * dayMs),
                completedAt = now - (10 * dayMs),
                notes = "Settled instantly via SEPA Instant Credit Transfer"
            )

            val po2 = PayoutEntity(
                id = "PO-918402",
                ownerId = "jurgen-westerveld",
                ownerName = "Jurgen Paul Westerveld",
                amount = 1850.00,
                currency = "EUR",
                fee = 0.0,
                netAmount = 1850.00,
                status = PayoutStatus.COMPLETED.name,
                destinationBank = "ING Bank N.V.",
                destinationIban = "NL91 INGB 0412 8923 00",
                destinationBic = "INGBNL2A",
                payoutSpeed = PayoutSpeed.STANDARD.name,
                reference = "Q3 Merchant Settlement Distribution",
                initiatedAt = now - (25 * dayMs),
                estimatedArrivalAt = now - (24 * dayMs),
                completedAt = now - (24 * dayMs),
                notes = "Scheduled automated Friday payout batch"
            )

            val po3 = PayoutEntity(
                id = "PO-923811",
                ownerId = "jurgen-westerveld",
                ownerName = "Jurgen Paul Westerveld",
                amount = 950.00,
                currency = "EUR",
                fee = 0.0,
                netAmount = 950.00,
                status = PayoutStatus.COMPLETED.name,
                destinationBank = "ING Bank N.V.",
                destinationIban = "NL91 INGB 0412 8923 00",
                destinationBic = "INGBNL2A",
                payoutSpeed = PayoutSpeed.INSTANT.name,
                reference = "Retainer & Subscription Commission",
                initiatedAt = now - (4 * dayMs),
                estimatedArrivalAt = now - (4 * dayMs),
                completedAt = now - (4 * dayMs),
                notes = "Direct bank transfer to primary business settlement account"
            )

            payoutDao.insertPayouts(listOf(po1, po2, po3))
        }
    }
}
