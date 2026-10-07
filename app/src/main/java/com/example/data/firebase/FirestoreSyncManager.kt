package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.PayoutEntity
import com.example.data.local.SubscriptionEntity
import com.example.data.model.FirestoreClient
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.oistars.billings.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreSyncManager"

class FirestoreSyncManager(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        context.getString(R.string.firestore_database_id)
    )
) {
    fun observeClientsFromFirestore(userId: String): Flow<List<FirestoreClient>> = callbackFlow {
        val collectionRef = db.collection("users")
            .document(userId)
            .collection("clients")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error observing Firestore clients: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { FirestoreClient.fromMap(it, fallbackId = doc.id) }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getClientsFromFirestore(userId: String): Result<List<FirestoreClient>> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("clients")
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { FirestoreClient.fromMap(it, fallbackId = doc.id) }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching clients from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
    suspend fun syncClientsToCloud(
        userId: String,
        clients: List<ClientEntity>,
        invoices: List<InvoiceEntity> = emptyList()
    ): Result<Int> {
        return try {
            val batch = db.batch()
            for (client in clients) {
                val docRef = db.collection("users")
                    .document(userId)
                    .collection("clients")
                    .document(client.id)

                val clientInvoices = invoices.filter { it.clientId == client.id }
                val overdueCount = clientInvoices.count { it.status.equals("OVERDUE", ignoreCase = true) }
                val pendingCount = clientInvoices.count { it.status.equals("PENDING", ignoreCase = true) }
                val paidCount = clientInvoices.count { it.status.equals("PAID", ignoreCase = true) }
                val totalInvoiced = clientInvoices.sumOf { it.totalAmount }
                val totalPaid = clientInvoices.sumOf { it.amountPaid }
                val outstanding = (totalInvoiced - totalPaid).coerceAtLeast(0.0)

                val derivedStatus = when {
                    overdueCount > 0 -> "OVERDUE"
                    pendingCount > 0 -> "PENDING_BALANCE"
                    paidCount > 0 -> "GOOD_STANDING"
                    else -> "ACTIVE"
                }

                val data = mapOf(
                    "id" to client.id,
                    "userId" to userId,
                    "name" to client.name,
                    "contactPerson" to client.contactPerson,
                    "email" to client.email,
                    "phone" to client.phone,
                    "taxNumber" to client.taxNumber,
                    "address" to client.address,
                    "country" to client.country,
                    "currency" to client.currency,
                    "paymentTermsDays" to client.paymentTermsDays,
                    "notes" to client.notes,
                    "accountStatus" to derivedStatus,
                    "totalInvoiced" to totalInvoiced,
                    "totalPaid" to totalPaid,
                    "outstandingBalance" to outstanding,
                    "openInvoicesCount" to (overdueCount + pendingCount),
                    "createdAt" to client.createdAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(clients.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing clients to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun saveClientToFirestore(userId: String, client: FirestoreClient): Result<Unit> {
        return try {
            val docId = client.id.ifBlank { "CLI-${(System.currentTimeMillis() % 100000)}" }
            val docRef = db.collection("users")
                .document(userId)
                .collection("clients")
                .document(docId)

            val data = mapOf(
                "id" to docId,
                "userId" to userId,
                "name" to client.name,
                "contactPerson" to client.contactPerson,
                "email" to client.email,
                "phone" to client.phone,
                "taxNumber" to client.taxNumber,
                "address" to client.address,
                "country" to client.country,
                "currency" to client.currency,
                "paymentTermsDays" to client.paymentTermsDays,
                "notes" to client.notes,
                "accountStatus" to client.accountStatus.name,
                "totalInvoiced" to client.totalInvoiced,
                "totalPaid" to client.totalPaid,
                "outstandingBalance" to client.outstandingBalance,
                "openInvoicesCount" to client.openInvoicesCount,
                "createdAt" to client.createdAt
            )
            docRef.set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving client to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun updateClientAccountStatus(
        userId: String,
        clientId: String,
        newStatus: com.example.data.model.ClientAccountStatus
    ): Result<Unit> {
        return try {
            db.collection("users")
                .document(userId)
                .collection("clients")
                .document(clientId)
                .update("accountStatus", newStatus.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating client status in Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteClientFromFirestore(userId: String, clientId: String): Result<Unit> {
        return try {
            db.collection("users")
                .document(userId)
                .collection("clients")
                .document(clientId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting client from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun syncInvoicesToCloud(userId: String, invoices: List<InvoiceEntity>): Result<Int> {
        return try {
            val batch = db.batch()
            for (invoice in invoices) {
                val docRef = db.collection("users")
                    .document(userId)
                    .collection("invoices")
                    .document(invoice.id)
                val data = mapOf(
                    "id" to invoice.id,
                    "userId" to userId,
                    "clientId" to invoice.clientId,
                    "clientName" to invoice.clientName,
                    "clientEmail" to invoice.clientEmail,
                    "status" to invoice.status,
                    "issueDate" to invoice.issueDate,
                    "dueDate" to invoice.dueDate,
                    "currency" to invoice.currency,
                    "itemsJson" to invoice.itemsJson,
                    "subtotal" to invoice.subtotal,
                    "taxTotal" to invoice.taxTotal,
                    "discountTotal" to invoice.discountTotal,
                    "totalAmount" to invoice.totalAmount,
                    "amountPaid" to invoice.amountPaid,
                    "paymentTerms" to invoice.paymentTerms,
                    "notes" to invoice.notes,
                    "paidAt" to (invoice.paidAt ?: 0L),
                    "createdAt" to invoice.createdAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(invoices.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing invoices to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun syncSubscriptionsToCloud(userId: String, subscriptions: List<SubscriptionEntity>): Result<Int> {
        return try {
            val batch = db.batch()
            for (sub in subscriptions) {
                val docRef = db.collection("users")
                    .document(userId)
                    .collection("subscriptions")
                    .document(sub.id)
                val data = mapOf(
                    "id" to sub.id,
                    "userId" to userId,
                    "clientId" to sub.clientId,
                    "clientName" to sub.clientName,
                    "planName" to sub.planName,
                    "amount" to sub.amount,
                    "currency" to sub.currency,
                    "interval" to sub.interval,
                    "status" to sub.status,
                    "nextBillingDate" to sub.nextBillingDate,
                    "notes" to sub.notes,
                    "createdAt" to sub.createdAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(subscriptions.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing subscriptions to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun syncPaymentsToCloud(userId: String, payments: List<PaymentEntity>): Result<Int> {
        return try {
            val batch = db.batch()
            for (payment in payments) {
                val docRef = db.collection("users")
                    .document(userId)
                    .collection("payments")
                    .document(payment.id)
                val data = mapOf(
                    "id" to payment.id,
                    "userId" to userId,
                    "invoiceId" to payment.invoiceId,
                    "clientName" to payment.clientName,
                    "amount" to payment.amount,
                    "currency" to payment.currency,
                    "paymentMethod" to payment.paymentMethod,
                    "transactionRef" to payment.transactionRef,
                    "paymentDate" to payment.paymentDate,
                    "notes" to payment.notes
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(payments.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing payments to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun syncPayoutsToCloud(userId: String, payouts: List<PayoutEntity>): Result<Int> {
        return try {
            val batch = db.batch()
            for (payout in payouts) {
                val docRef = db.collection("users")
                    .document(userId)
                    .collection("payouts")
                    .document(payout.id)
                val data = mapOf(
                    "id" to payout.id,
                    "ownerId" to userId,
                    "ownerName" to payout.ownerName,
                    "amount" to payout.amount,
                    "currency" to payout.currency,
                    "fee" to payout.fee,
                    "netAmount" to payout.netAmount,
                    "status" to payout.status,
                    "destinationBank" to payout.destinationBank,
                    "destinationIban" to payout.destinationIban,
                    "destinationBic" to payout.destinationBic,
                    "payoutSpeed" to payout.payoutSpeed,
                    "reference" to payout.reference,
                    "initiatedAt" to payout.initiatedAt,
                    "estimatedArrivalAt" to payout.estimatedArrivalAt,
                    "completedAt" to payout.completedAt,
                    "notes" to payout.notes
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(payouts.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing payouts to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun syncAllToCloud(
        userId: String,
        clients: List<ClientEntity>,
        invoices: List<InvoiceEntity>,
        subscriptions: List<SubscriptionEntity>,
        payments: List<PaymentEntity>,
        payouts: List<PayoutEntity> = emptyList()
    ): Result<String> {
        return try {
            syncClientsToCloud(userId, clients, invoices)
            syncInvoicesToCloud(userId, invoices)
            syncSubscriptionsToCloud(userId, subscriptions)
            syncPaymentsToCloud(userId, payments)
            if (payouts.isNotEmpty()) {
                syncPayoutsToCloud(userId, payouts)
            }
            Result.success("Cloud sync complete: ${clients.size} clients, ${invoices.size} invoices, ${subscriptions.size} subscriptions, ${payments.size} payments, ${payouts.size} payouts")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
