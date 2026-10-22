package com.example.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.data.local.StringListConverter
import java.util.UUID

data class CommerceUser(
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val fullName: String = "",
    val shippingAddress: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "email" to email,
        "fullName" to fullName,
        "shippingAddress" to shippingAddress
    )

    companion object {
        fun fromMap(data: Map<String, Any?>, fallbackId: String = ""): CommerceUser {
            return CommerceUser(
                id = (data["id"] as? String) ?: fallbackId,
                email = (data["email"] as? String) ?: "",
                fullName = (data["fullName"] as? String) ?: "",
                shippingAddress = (data["shippingAddress"] as? String) ?: ""
            )
        }
    }
}

data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val stockQuantity: Int,
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "Services"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "price" to price,
        "stockQuantity" to stockQuantity,
        "description" to description,
        "imageUrl" to imageUrl,
        "category" to category
    )

    companion object {
        fun fromMap(data: Map<String, Any?>, fallbackId: String = ""): Product {
            return Product(
                id = (data["id"] as? String) ?: fallbackId,
                name = (data["name"] as? String) ?: "Product",
                price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                stockQuantity = (data["stockQuantity"] as? Number)?.toInt() ?: 0,
                description = (data["description"] as? String) ?: "",
                imageUrl = (data["imageUrl"] as? String) ?: "",
                category = (data["category"] as? String) ?: "Services"
            )
        }

        fun defaultCatalog(): List<Product> = listOf(
            Product(
                id = "PROD-CLOUD-HOST",
                name = "Enterprise Cloud Hosting",
                price = 249.00,
                stockQuantity = 45,
                description = "Dedicated high-performance European cloud node with 99.99% SLA and automated backup.",
                imageUrl = "",
                category = "Infrastructure"
            ),
            Product(
                id = "PROD-TECH-RETAINER",
                name = "Monthly Dev & Maintenance Retainer",
                price = 850.00,
                stockQuantity = 12,
                description = "20 hours of senior Android, Kotlin backend, and API maintenance with priority turnaround.",
                imageUrl = "",
                category = "Services"
            ),
            Product(
                id = "PROD-SECURITY-AUDIT",
                name = "Fintech & OpenSSF Security Audit",
                price = 1450.00,
                stockQuantity = 6,
                description = "Comprehensive ProGuard, zero-trust authentication, and SEPA/EPI compliance audit.",
                imageUrl = "",
                category = "Consulting"
            ),
            Product(
                id = "PROD-API-GATEWAY",
                name = "Billing API Gateway License",
                price = 120.00,
                stockQuantity = 99,
                description = "Annual production API token for webhook settlement triggers and real-time ledger sync.",
                imageUrl = "",
                category = "Software"
            ),
            Product(
                id = "PROD-WERO-INTEGRATION",
                name = "Wero EPI Instant Wire Module",
                price = 399.00,
                stockQuantity = 25,
                description = "Plug-and-play European Payments Initiative (Wero) bank wire SDK with real-time settlement.",
                imageUrl = "",
                category = "Software"
            ),
            Product(
                id = "PROD-VAT-COMPLIANCE",
                name = "EU VAT Reverse-Charge Package",
                price = 180.00,
                stockQuantity = 50,
                description = "Automated cross-border EU tax validation and VIES verification plug-in for invoices.",
                imageUrl = "",
                category = "Services"
            )
        )
    }
}

data class CartItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val priceAtTime: Double,
    val category: String = "Services"
) {
    val totalAmount: Double
        get() = priceAtTime * quantity

    fun toMap(): Map<String, Any?> = mapOf(
        "productId" to productId,
        "productName" to productName,
        "quantity" to quantity,
        "priceAtTime" to priceAtTime,
        "category" to category
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): CartItem {
            return CartItem(
                productId = (data["productId"] as? String) ?: "",
                productName = (data["productName"] as? String) ?: "Item",
                quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
                priceAtTime = (data["priceAtTime"] as? Number)?.toDouble() ?: 0.0,
                category = (data["category"] as? String) ?: "Services"
            )
        }
    }
}

data class Cart(
    val userId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val items: List<CartItem> = emptyList()
) {
    val totalAmount: Double
        get() = items.sumOf { it.totalAmount }

    val itemCount: Int
        get() = items.sumOf { it.quantity }

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "updatedAt" to updatedAt,
        "items" to items.map { it.toMap() }
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): Cart {
            val userId = (data["userId"] as? String) ?: ""
            val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val rawItems = (data["items"] as? List<*>) ?: emptyList<Any>()
            val items = rawItems.mapNotNull { itemMap ->
                (itemMap as? Map<String, Any?>)?.let { CartItem.fromMap(it) }
            }
            return Cart(userId = userId, updatedAt = updatedAt, items = items)
        }
    }
}

data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val priceAtTime: Double
) {
    val totalAmount: Double
        get() = priceAtTime * quantity

    fun toMap(): Map<String, Any?> = mapOf(
        "productId" to productId,
        "productName" to productName,
        "quantity" to quantity,
        "priceAtTime" to priceAtTime
    )

    companion object {
        fun fromMap(data: Map<String, Any?>): OrderItem {
            return OrderItem(
                productId = (data["productId"] as? String) ?: "",
                productName = (data["productName"] as? String) ?: "Item",
                quantity = (data["quantity"] as? Number)?.toInt() ?: 1,
                priceAtTime = (data["priceAtTime"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

@Entity(tableName = "orders")
@TypeConverters(StringListConverter::class)
data class Order(
    @PrimaryKey
    val id: String = "ORD-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}",
    val clientID: String,
    val productIDs: List<String> = emptyList(),
    val amount: Double = 0.0,
    val status: String = "PAID", // PENDING, PAID, SHIPPED, COMPLETED
    val timestamp: Long = System.currentTimeMillis(),
    val invoiceId: String? = null,
    val shippingAddress: String = "",
    val customerEmail: String = "",
    val customerName: String = ""
) {
    @Ignore
    constructor(
        id: String = "ORD-${(System.currentTimeMillis() % 100000).toString().padStart(5, '0')}",
        userId: String,
        totalAmount: Double,
        status: String = "PAID",
        createdAt: Long = System.currentTimeMillis(),
        items: List<OrderItem> = emptyList(),
        invoiceId: String? = null,
        shippingAddress: String = "",
        customerEmail: String = "",
        customerName: String = ""
    ) : this(
        id = id,
        clientID = userId,
        productIDs = items.map { it.productId },
        amount = totalAmount,
        status = status,
        timestamp = createdAt,
        invoiceId = invoiceId,
        shippingAddress = shippingAddress,
        customerEmail = customerEmail,
        customerName = customerName
    )

    @get:Ignore
    val userId: String
        get() = clientID

    @get:Ignore
    val totalAmount: Double
        get() = amount

    @get:Ignore
    val createdAt: Long
        get() = timestamp

    @get:Ignore
    val items: List<OrderItem>
        get() = productIDs.map { prodId ->
            OrderItem(
                productId = prodId,
                productName = "Product $prodId",
                quantity = 1,
                priceAtTime = if (productIDs.isNotEmpty()) amount / productIDs.size else amount
            )
        }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "clientID" to clientID,
        "userId" to clientID,
        "productIDs" to productIDs,
        "amount" to amount,
        "totalAmount" to amount,
        "status" to status,
        "timestamp" to timestamp,
        "createdAt" to timestamp,
        "items" to items.map { it.toMap() },
        "invoiceId" to invoiceId,
        "shippingAddress" to shippingAddress,
        "customerEmail" to customerEmail,
        "customerName" to customerName
    )

    companion object {
        fun fromMap(data: Map<String, Any?>, fallbackId: String = ""): Order {
            val id = (data["id"] as? String) ?: fallbackId
            val clientID = (data["clientID"] as? String) ?: (data["userId"] as? String) ?: ""
            val amount = (data["amount"] as? Number)?.toDouble()
                ?: (data["totalAmount"] as? Number)?.toDouble() ?: 0.0
            val status = (data["status"] as? String) ?: "PAID"
            val timestamp = (data["timestamp"] as? Number)?.toLong()
                ?: (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

            val rawProductIds = data["productIDs"] as? List<*>
            val productIDs: List<String> = if (rawProductIds != null) {
                rawProductIds.mapNotNull { it?.toString() }
            } else {
                val rawItems = (data["items"] as? List<*>) ?: emptyList<Any>()
                rawItems.mapNotNull { itemMap ->
                    (itemMap as? Map<String, Any?>)?.let { OrderItem.fromMap(it).productId }
                }
            }

            val invoiceId = data["invoiceId"] as? String
            val shippingAddress = (data["shippingAddress"] as? String) ?: ""
            val customerEmail = (data["customerEmail"] as? String) ?: ""
            val customerName = (data["customerName"] as? String) ?: ""

            return Order(
                id = id,
                clientID = clientID,
                productIDs = productIDs,
                amount = amount,
                status = status,
                timestamp = timestamp,
                invoiceId = invoiceId,
                shippingAddress = shippingAddress,
                customerEmail = customerEmail,
                customerName = customerName
            )
        }
    }
}

