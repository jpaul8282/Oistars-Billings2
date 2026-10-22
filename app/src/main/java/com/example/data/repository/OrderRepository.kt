package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.OrderDao
import com.example.data.model.Order
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.oistars.billings.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "OrderRepository"

/**
 * Repository integrating Room Database for offline-first local caching and
 * Cloud Firestore for real-time cloud synchronization of Order records.
 */
class OrderRepository(
    private val orderDao: OrderDao,
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        context.getString(R.string.firestore_database_id)
    )
) {
    /**
     * Reactive stream of all orders persisted locally in Room.
     */
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()

    /**
     * Reactive stream of orders for a specific client from Room.
     */
    fun getOrdersForClient(clientId: String): Flow<List<Order>> {
        return orderDao.getOrdersForClient(clientId)
    }

    /**
     * Retrieve a single order by ID from local cache.
     */
    suspend fun getOrderById(id: String): Order? {
        return orderDao.getOrderById(id)
    }

    /**
     * Insert or replace an order locally in Room, and immediately sync with Cloud Firestore.
     */
    suspend fun insertOrder(order: Order, syncToCloud: Boolean = true): Result<Order> {
        return try {
            // 1. Persist locally to Room
            orderDao.insertOrder(order)

            // 2. Sync to Cloud Firestore
            if (syncToCloud) {
                syncSingleOrderToFirestore(order)
            }
            Result.success(order)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting order: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update an order in Room and update the corresponding Firestore document.
     */
    suspend fun updateOrder(order: Order, syncToCloud: Boolean = true): Result<Order> {
        return try {
            orderDao.updateOrder(order)
            if (syncToCloud) {
                syncSingleOrderToFirestore(order)
            }
            Result.success(order)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete an order from Room and remove from Firestore.
     */
    suspend fun deleteOrder(order: Order, syncToCloud: Boolean = true): Result<Unit> {
        return try {
            orderDao.deleteOrder(order)
            if (syncToCloud) {
                val targetOwner = order.clientID.ifEmpty { "jurgen-westerveld" }
                db.collection("users")
                    .document(targetOwner)
                    .collection("orders")
                    .document(order.id)
                    .delete()
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting order: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pulls orders from Cloud Firestore for a specified client / user account
     * and saves them into the local Room database.
     */
    suspend fun syncOrdersFromFirestore(clientId: String = "jurgen-westerveld"): Result<List<Order>> {
        return try {
            val snapshot = db.collection("users")
                .document(clientId)
                .collection("orders")
                .get()
                .await()

            val remoteOrders = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Order.fromMap(it, fallbackId = doc.id) }
            }

            if (remoteOrders.isNotEmpty()) {
                orderDao.insertOrders(remoteOrders)
            }
            Log.d(TAG, "Synced ${remoteOrders.size} orders from Firestore to Room for client $clientId")
            Result.success(remoteOrders)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching orders from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pushes all local Room orders for a client up to Cloud Firestore using a batch write.
     */
    suspend fun syncOrdersToFirestore(clientId: String = "jurgen-westerveld"): Result<Int> {
        return try {
            val localList = orderDao.getOrdersForClient(clientId).firstOrNull()
                ?: orderDao.getAllOrders().firstOrNull()
                ?: emptyList()

            if (localList.isEmpty()) {
                return Result.success(0)
            }

            val batch = db.batch()
            for (order in localList) {
                val targetDoc = db.collection("users")
                    .document(clientId)
                    .collection("orders")
                    .document(order.id)
                batch.set(targetDoc, order.toMap(), SetOptions.merge())
            }
            batch.commit().await()
            Log.d(TAG, "Successfully synced ${localList.size} orders to Firestore")
            Result.success(localList.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing orders to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time snapshot listener on Cloud Firestore.
     * Caches all remote updates into Room so the UI can observe Room with zero network lag.
     */
    fun observeAndSyncRealtime(clientId: String = "jurgen-westerveld"): Flow<List<Order>> = callbackFlow {
        val collectionRef = db.collection("users")
            .document(clientId)
            .collection("orders")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error in Firestore order realtime listener: ${error.message}", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val remoteOrders = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Order.fromMap(it, fallbackId = doc.id) }
                }
                if (remoteOrders.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            orderDao.insertOrders(remoteOrders)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed caching remote orders into Room: ${e.message}")
                        }
                    }
                }
                trySend(remoteOrders)
            }
        }
        awaitClose { listener.remove() }
    }

    private suspend fun syncSingleOrderToFirestore(order: Order) {
        val targetOwner = order.clientID.ifEmpty { "jurgen-westerveld" }
        db.collection("users")
            .document(targetOwner)
            .collection("orders")
            .document(order.id)
            .set(order.toMap(), SetOptions.merge())
            .await()
    }
}
