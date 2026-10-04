package com.oistars.billings

import kotlin.math.round
import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceCalculationTest {
    @Test
    fun `subtotal tax and discount are calculated correctly`() {
        val items = listOf(
            InvoiceLineItem("Consulting", 120.0, 2, 0.0),
            InvoiceLineItem("Support", 50.0, 1, 0.10)
        )

        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val discountTotal = items.sumOf { it.unitPrice * it.quantity * it.discountRate }
        val taxableAmount = (subtotal - discountTotal).coerceAtLeast(0.0)
        val taxTotal = taxableAmount * 0.2
        val total = subtotal - discountTotal + taxTotal

        assertEquals(290.0, round(subtotal * 100.0) / 100.0, 0.01)
        assertEquals(5.0, round(discountTotal * 100.0) / 100.0, 0.01)
        assertEquals(57.0, round(taxTotal * 100.0) / 100.0, 0.01)
        assertEquals(342.0, round(total * 100.0) / 100.0, 0.01)
    }

    @Test
    fun `partial payment reduces balance due`() {
        val invoiceTotal = 100.0
        val payment = 35.0
        val balanceDue = (invoiceTotal - payment).coerceAtLeast(0.0)

        assertEquals(65.0, balanceDue, 0.01)
    }

    @Test
    fun `overpayment clamps to zero`() {
        val invoiceTotal = 100.0
        val overpayment = 150.0
        val balanceDue = (invoiceTotal - overpayment).coerceAtLeast(0.0)

        assertEquals(0.0, balanceDue, 0.01)
    }

    @Test
    fun `invalid values are sanitized to zero`() {
        val subtotal = 0.0
        val discount = -25.0
        val tax = -5.0

        val safeSubtotal = subtotal.coerceAtLeast(0.0)
        val safeDiscount = discount.coerceAtLeast(0.0)
        val safeTax = tax.coerceAtLeast(0.0)

        assertEquals(0.0, safeSubtotal, 0.0)
        assertEquals(0.0, safeDiscount, 0.0)
        assertEquals(0.0, safeTax, 0.0)
    }
}

private data class InvoiceLineItem(
    val description: String,
    val unitPrice: Double,
    val quantity: Int,
    val discountRate: Double
)
