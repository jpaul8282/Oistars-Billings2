package com.example.data.model

import com.example.data.local.ClientEntity

enum class ClientAccountStatus(val label: String, val description: String) {
    GOOD_STANDING("Good Standing", "All invoices fully settled"),
    ACTIVE("Active", "Account active with healthy transactions"),
    PENDING_BALANCE("Pending Balance", "Invoices awaiting payment within terms"),
    OVERDUE("Overdue", "One or more invoices past due date"),
    INACTIVE("Inactive", "No recent billing activity");

    companion object {
        fun fromString(value: String): ClientAccountStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

data class FirestoreClient(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val contactPerson: String = "",
    val email: String = "",
    val phone: String = "",
    val taxNumber: String = "",
    val address: String = "",
    val country: String = "Netherlands",
    val currency: String = "EUR",
    val paymentTermsDays: Int = 14,
    val notes: String = "",
    val accountStatus: ClientAccountStatus = ClientAccountStatus.ACTIVE,
    val totalInvoiced: Double = 0.0,
    val totalPaid: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val openInvoicesCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toLocalEntity(): ClientEntity {
        return ClientEntity(
            id = id.ifBlank { "CLI-${System.currentTimeMillis() % 100000}" },
            name = name,
            contactPerson = contactPerson,
            email = email,
            phone = phone,
            taxNumber = taxNumber,
            address = address,
            country = country,
            currency = currency,
            paymentTermsDays = paymentTermsDays,
            notes = notes,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromMap(data: Map<String, Any?>, fallbackId: String = ""): FirestoreClient {
            val id = (data["id"] as? String) ?: fallbackId
            val userId = (data["userId"] as? String) ?: ""
            val name = (data["name"] as? String) ?: "Unknown Client"
            val contact = (data["contactPerson"] as? String) ?: ""
            val email = (data["email"] as? String) ?: ""
            val phone = (data["phone"] as? String) ?: ""
            val taxNumber = (data["taxNumber"] as? String) ?: ""
            val address = (data["address"] as? String) ?: ""
            val country = (data["country"] as? String) ?: "Netherlands"
            val currency = (data["currency"] as? String) ?: "EUR"
            val paymentTermsDays = (data["paymentTermsDays"] as? Number)?.toInt() ?: 14
            val notes = (data["notes"] as? String) ?: ""
            val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()

            val rawStatus = (data["accountStatus"] as? String) ?: "ACTIVE"
            val accountStatus = ClientAccountStatus.fromString(rawStatus)

            val totalInvoiced = (data["totalInvoiced"] as? Number)?.toDouble() ?: 0.0
            val totalPaid = (data["totalPaid"] as? Number)?.toDouble() ?: 0.0
            val outstandingBalance = (data["outstandingBalance"] as? Number)?.toDouble() ?: (totalInvoiced - totalPaid).coerceAtLeast(0.0)
            val openInvoicesCount = (data["openInvoicesCount"] as? Number)?.toInt() ?: 0

            return FirestoreClient(
                id = id,
                userId = userId,
                name = name,
                contactPerson = contact,
                email = email,
                phone = phone,
                taxNumber = taxNumber,
                address = address,
                country = country,
                currency = currency,
                paymentTermsDays = paymentTermsDays,
                notes = notes,
                accountStatus = accountStatus,
                totalInvoiced = totalInvoiced,
                totalPaid = totalPaid,
                outstandingBalance = outstandingBalance,
                openInvoicesCount = openInvoicesCount,
                createdAt = createdAt
            )
        }
    }
}
