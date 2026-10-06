package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.ClientEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.PaymentEntity
import com.example.data.local.SubscriptionEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.oistars.billings.R
import kotlinx.coroutines.tasks.await

private const val TAG = "FirestoreSyncManager"

class FirestoreSyncManager(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        context.getString(R.string.firestore_database_id)
    )
) {
    suspend fun syncClientsToCloud(userId: String, clients: List<ClientEntity>): Result<Int> {
        return try {
            val batch = db.batch()
            for (client in clients) {
                val docRef = db.collection("users")
                    .document(userId)
                    .collection("clients")
                    .document(client.id)
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

    suspend fun syncAllToCloud(
        userId: String,
        clients: List<ClientEntity>,
        invoices: List<InvoiceEntity>,
        subscriptions: List<SubscriptionEntity>,
        payments: List<PaymentEntity>
    ): Result<String> {
        return try {
            syncClientsToCloud(userId, clients)
            syncInvoicesToCloud(userId, invoices)
            syncSubscriptionsToCloud(userId, subscriptions)
            syncPaymentsToCloud(userId, payments)
            Result.success("Cloud sync complete: ${clients.size} clients, ${invoices.size} invoices, ${subscriptions.size} subscriptions, ${payments.size} payments")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
