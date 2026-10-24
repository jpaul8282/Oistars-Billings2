package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.OrderDao
import com.example.data.model.Order
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentChange
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
     * Real-time update event details detailing additions and status changes.
     */
    data class OrderRealtimeEvent(
        val allOrders: List<Order>,
        val addedOrders: List<Order> = emptyList(),
        val modifiedOrders: List<Order> = emptyList(),
        val removedOrderIds: List<String> = emptyList(),
        val changeDescription: String? = null
    )

    /**
     * Listens for real-time updates from the Firestore 'Orders' collection:
     * - Detects newly added orders and status changes (e.g. PENDING -> PAID).
     * - Persists changes into the local Room database (which automatically emits
     *   through Room Flow to update the RevenueChart in real time).
     * - Emits the full updated order list directly.
     */
    fun listenForRealtimeOrderUpdates(clientId: String = "jurgen-westerveld"): Flow<List<Order>> = callbackFlow {
        val collectionRef = db.collection("users")
            .document(clientId)
            .collection("orders")

        Log.d(TAG, "Attaching real-time Firestore listener for orders of client: $clientId")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error in Firestore real-time orders listener: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val allOrders = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Order.fromMap(it, fallbackId = doc.id) }
                }

                val addedOrders = mutableListOf<Order>()
                val modifiedOrders = mutableListOf<Order>()
                val removedOrderIds = mutableListOf<String>()

                for (change in snapshot.documentChanges) {
                    val order = change.document.data?.let { Order.fromMap(it, fallbackId = change.document.id) }
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (order != null) addedOrders.add(order)
                        }
                        DocumentChange.Type.MODIFIED -> {
                            if (order != null) {
                                modifiedOrders.add(order)
                                Log.d(TAG, "Real-time status change: Order #${order.id} status is now '${order.status}'")
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            removedOrderIds.add(change.document.id)
                        }
                    }
                }

                // Caching in Room triggers Room Flow, keeping RevenueChart in sync offline & online
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val toUpsert = addedOrders + modifiedOrders
                        if (toUpsert.isNotEmpty()) {
                            orderDao.insertOrders(toUpsert)
                        }
                        for (removedId in removedOrderIds) {
                            orderDao.deleteOrderById(removedId)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed caching real-time orders update into Room: ${e.message}")
                    }
                }

                trySend(allOrders)
            }
        }

        awaitClose {
            Log.d(TAG, "Detaching real-time Firestore orders listener for client: $clientId")
            listener.remove()
        }
    }

    /**
     * Streams rich real-time update events with descriptions of new orders and status transitions.
     */
    fun listenForRealtimeOrderEvents(clientId: String = "jurgen-westerveld"): Flow<OrderRealtimeEvent> = callbackFlow {
        val collectionRef = db.collection("users")
            .document(clientId)
            .collection("orders")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error in Firestore real-time orders event listener: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val allOrders = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Order.fromMap(it, fallbackId = doc.id) }
                }

                val added = mutableListOf<Order>()
                val modified = mutableListOf<Order>()
                val removed = mutableListOf<String>()

                for (change in snapshot.documentChanges) {
                    val order = change.document.data?.let { Order.fromMap(it, fallbackId = change.document.id) }
                    when (change.type) {
                        DocumentChange.Type.ADDED -> order?.let { added.add(it) }
                        DocumentChange.Type.MODIFIED -> order?.let { modified.add(it) }
                        DocumentChange.Type.REMOVED -> removed.add(change.document.id)
                    }
                }

                val desc = when {
                    modified.isNotEmpty() -> "Order #${modified.first().id} status updated to ${modified.first().status}"
                    added.isNotEmpty() -> "New Order #${added.first().id} placed (€%.2f)".format(added.first().amount)
                    removed.isNotEmpty() -> "Order #${removed.first()} removed"
                    else -> null
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val toUpsert = added + modified
                    if (toUpsert.isNotEmpty()) orderDao.insertOrders(toUpsert)
                    removed.forEach { orderDao.deleteOrderById(it) }
                }

                trySend(
                    OrderRealtimeEvent(
                        allOrders = allOrders,
                        addedOrders = added,
                        modifiedOrders = modified,
                        removedOrderIds = removed,
                        changeDescription = desc
                    )
                )
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Backward-compatible alias for observeAndSyncRealtime.
     */
    fun observeAndSyncRealtime(clientId: String = "jurgen-westerveld"): Flow<List<Order>> {
        return listenForRealtimeOrderUpdates(clientId)
    }

    /**
     * Updates an order's status both in Room and Cloud Firestore, which triggers
     * the real-time listener and updates the RevenueChart.
     */
    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        clientId: String = "jurgen-westerveld"
    ): Result<Unit> {
        return try {
            val existing = orderDao.getOrderById(orderId)
            val updated = existing?.copy(status = newStatus) ?: Order(
                id = orderId,
                clientID = clientId,
                status = newStatus
            )

            // Update in Room
            orderDao.insertOrder(updated)

            // Update in Firestore
            db.collection("users")
                .document(clientId)
                .collection("orders")
                .document(orderId)
                .update("status", newStatus)
                .await()

            Log.d(TAG, "Order #$orderId status updated to $newStatus in Room & Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed updating order status: ${e.message}", e)
            Result.failure(e)
        }
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

