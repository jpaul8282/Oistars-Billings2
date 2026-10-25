package com.oistars.billings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.service.AiBillingCommunicationService
import com.example.data.service.InvoiceCommunicationRequest
import com.example.data.service.InvoiceCommunicationTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiCommunicationGeneratorTest {

    private lateinit var service: AiBillingCommunicationService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        service = AiBillingCommunicationService(context)
    }

    @Test
    fun `friendly welcome communication generates warm tone with client details`() {
        val request = InvoiceCommunicationRequest(
            clientName = "Acme Seafood B.V.",
            invoiceId = "INV-2026-001",
            amount = 1250.00,
            currency = "EUR",
            dueDate = "March 15, 2026",
            tone = InvoiceCommunicationTone.FRIENDLY_WELCOME
        )

        val result = service.generateDeterministicTemplate(request)

        assertTrue(result.success)
        assertTrue(result.subject.contains("INV-2026-001"))
        assertTrue(result.communicationText.contains("Acme Seafood B.V."))
        assertTrue(result.communicationText.contains("1250.00"))
        assertTrue(result.communicationText.contains("Warm regards"))
        assertEquals("friendly_welcome", result.tone)
    }

    @Test
    fun `formal b2b communication generates enterprise terms with wire info`() {
        val request = InvoiceCommunicationRequest(
            clientName = "Artisan Brands Corp",
            invoiceId = "INV-8899",
            amount = 4500.50,
            currency = "EUR",
            dueDate = "Net 30 Days",
            tone = InvoiceCommunicationTone.FORMAL_B2B
        )

        val result = service.generateDeterministicTemplate(request)

        assertTrue(result.success)
        assertTrue(result.subject.contains("Tax Invoice INV-8899"))
        assertTrue(result.communicationText.contains("Accounts Payable"))
        assertTrue(result.communicationText.contains("NL43 INGB 0006 3721 46"))
        assertTrue(result.communicationText.contains("INGBNL2A"))
        assertEquals("formal_b2b", result.tone)
    }

    @Test
    fun `payment reminder communication contains invoice id and courteous prompt`() {
        val request = InvoiceCommunicationRequest(
            clientName = "Nordic Pearls ApS",
            invoiceId = "INV-7001",
            amount = 750.00,
            currency = "EUR",
            dueDate = "Tomorrow",
            tone = InvoiceCommunicationTone.PAYMENT_REMINDER
        )

        val result = service.generateDeterministicTemplate(request)

        assertTrue(result.success)
        assertTrue(result.subject.contains("Payment Reminder: Invoice INV-7001"))
        assertTrue(result.communicationText.contains("friendly reminder"))
        assertTrue(result.communicationText.contains("Wero or SEPA"))
    }

    @Test
    fun `overdue notice generates urgent demand with penalty prevention`() {
        val request = InvoiceCommunicationRequest(
            clientName = "Delta Logistics",
            invoiceId = "INV-3344",
            amount = 980.00,
            currency = "EUR",
            dueDate = "January 31, 2026",
            tone = InvoiceCommunicationTone.OVERDUE_NOTICE
        )

        val result = service.generateDeterministicTemplate(request)

        assertTrue(result.success)
        assertTrue(result.subject.contains("URGENT: Overdue Notice"))
        assertTrue(result.communicationText.contains("passed its due date"))
        assertTrue(result.communicationText.contains("immediate remittance"))
    }

    @Test
    fun `wero sepa details communication contains instant settlement banking data`() {
        val request = InvoiceCommunicationRequest(
            clientName = "Sophie de Vries",
            invoiceId = "INV-9900",
            amount = 320.00,
            currency = "EUR",
            dueDate = "Net 7 Days",
            tone = InvoiceCommunicationTone.WERO_SEPA_DETAILS
        )

        val result = service.generateDeterministicTemplate(request)

        assertTrue(result.success)
        assertTrue(result.communicationText.contains("Wero or SEPA Instant Wire"))
        assertTrue(result.communicationText.contains("NL43 INGB 0006 3721 46"))
        assertTrue(result.communicationText.contains("ING Bank Amsterdam"))
    }

    @Test
    fun `custom instructions are appended properly`() {
        val request = InvoiceCommunicationRequest(
            clientName = "Custom Client",
            invoiceId = "INV-100",
            amount = 50.0,
            dueDate = "Net 14",
            tone = InvoiceCommunicationTone.FRIENDLY_WELCOME,
            customInstructions = "Include 2% discount if paid within 48h."
        )

        val result = service.generateDeterministicTemplate(request)

        assertTrue(result.communicationText.contains("Special Note: Include 2% discount if paid within 48h."))
    }

    @Test
    fun `blank inputs use sensible fallbacks without crashing`() {
        val request = InvoiceCommunicationRequest(
            clientName = "",
            invoiceId = "",
            amount = 0.0,
            dueDate = ""
        )

        val result = service.generateDeterministicTemplate(request)

        assertNotNull(result)
        assertTrue(result.communicationText.contains("Valued Client"))
        assertTrue(result.communicationText.contains("INV-NEW"))
    }
}
