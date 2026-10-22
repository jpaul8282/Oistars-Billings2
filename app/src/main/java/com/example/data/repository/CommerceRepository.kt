package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Cart
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.oistars.billings.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "CommerceRepository"

class CommerceRepository(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        context.getString(R.string.firestore_database_id)
    )
) {
    // --- Products ---
    fun observeProducts(): Flow<List<Product>> = callbackFlow {
        val collectionRef = db.collection("products")
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error observing products: ${error.message}", error)
                // Fallback to default catalog on error or offline
                trySend(Product.defaultCatalog())
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Product.fromMap(it, fallbackId = doc.id) }
                }
                trySend(list)
            } else {
                // If cloud collection is empty, emit default catalog
                trySend(Product.defaultCatalog())
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun seedDefaultProductsIfEmpty(): Result<Unit> {
        return try {
            val snapshot = db.collection("products").limit(1).get().await()
            if (snapshot.isEmpty) {
                val batch = db.batch()
                for (prod in Product.defaultCatalog()) {
                    val docRef = db.collection("products").document(prod.id)
                    batch.set(docRef, prod.toMap())
                }
                batch.commit().await()
                Log.d(TAG, "Successfully seeded default products to Firestore")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Seed default products skipped or offline: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Cart ---
    fun observeUserCart(userId: String): Flow<Cart> = callbackFlow {
        val docRef = db.collection("users")
            .document(userId)
            .collection("cart")
            .document("active_cart")

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error observing cart: ${error.message}", error)
                trySend(Cart(userId = userId))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val cart = snapshot.data?.let { Cart.fromMap(it) } ?: Cart(userId = userId)
                trySend(cart)
            } else {
                trySend(Cart(userId = userId))
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveUserCart(cart: Cart): Result<Unit> {
        return try {
            db.collection("users")
                .document(cart.userId)
                .collection("cart")
                .document("active_cart")
                .set(cart.toMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cart: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun clearUserCart(userId: String): Result<Unit> {
        return try {
            db.collection("users")
                .document(userId)
                .collection("cart")
                .document("active_cart")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cart: ${e.message}", e)
            Result.failure(e)
        }
    }

    // --- Orders ---
    fun observeUserOrders(userId: String): Flow<List<Order>> = callbackFlow {
        val collectionRef = db.collection("users")
            .document(userId)
            .collection("orders")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Error observing orders: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Order.fromMap(it, fallbackId = doc.id) }
                }.sortedByDescending { it.createdAt }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun placeOrder(order: Order): Result<Order> {
        return try {
            val batch = db.batch()
            val targetUserId = order.clientID.ifEmpty { order.userId }

            // 1. Write order doc
            val orderDocRef = db.collection("users")
                .document(targetUserId)
                .collection("orders")
                .document(order.id)
            batch.set(orderDocRef, order.toMap(), SetOptions.merge())

            // 2. Clear user's active cart
            val cartDocRef = db.collection("users")
                .document(targetUserId)
                .collection("cart")
                .document("active_cart")
            batch.delete(cartDocRef)

            batch.commit().await()
            Result.success(order)
        } catch (e: Exception) {
            Log.e(TAG, "Error placing order in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
}
