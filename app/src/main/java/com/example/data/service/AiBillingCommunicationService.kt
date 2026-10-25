package com.example.data.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "AiBillingService"

enum class InvoiceCommunicationTone(
    val key: String,
    val displayName: String,
    val description: String
) {
    FRIENDLY_WELCOME(
        "friendly_welcome",
        "Friendly Welcome",
        "Warm onboarding & partnership appreciation"
    ),
    FORMAL_B2B(
        "formal_b2b",
        "Corporate B2B",
        "Formal invoice notice for enterprise AP departments"
    ),
    PAYMENT_REMINDER(
        "payment_reminder",
        "Payment Reminder",
        "Polite payment reminder for upcoming/due invoice"
    ),
    OVERDUE_NOTICE(
        "overdue_notice",
        "Overdue Notice",
        "Urgent notice with instant settlement instructions"
    ),
    WERO_SEPA_DETAILS(
        "wero_sepa_details",
        "Wero / SEPA Wire",
        "Direct European instant settlement & banking details"
    )
}

data class InvoiceCommunicationRequest(
    val clientName: String,
    val invoiceId: String,
    val amount: Double,
    val currency: String = "EUR",
    val dueDate: String,
    val tone: InvoiceCommunicationTone = InvoiceCommunicationTone.FRIENDLY_WELCOME,
    val customInstructions: String = ""
)

data class InvoiceCommunicationResult(
    val success: Boolean,
    val subject: String,
    val communicationText: String,
    val tone: String,
    val source: String
)

/**
 * Service invoking the server-side Genkit Cloud Function / API endpoint
 * to generate customer notes and invoice communications without leaking
 * the Gemini API key into the client APK.
 */
class AiBillingCommunicationService(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Default local backend endpoint (Android emulator host mapping or configurable endpoint)
    private val backendBaseUrl = "http://10.0.2.2:5001"

    suspend fun generateCommunication(request: InvoiceCommunicationRequest): InvoiceCommunicationResult =
        withContext(Dispatchers.IO) {
            try {
                // 1. Attempt server-side Genkit Cloud Function REST endpoint
                val jsonPayload = JSONObject().apply {
                    put("clientName", request.clientName)
                    put("invoiceId", request.invoiceId)
                    put("amount", request.amount)
                    put("currency", request.currency)
                    put("dueDate", request.dueDate)
                    put("tone", request.tone.key)
                    put("customInstructions", request.customInstructions)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = jsonPayload.toString().toRequestBody(mediaType)
                val httpRequest = Request.Builder()
                    .url("$backendBaseUrl/api/ai/invoice-communication")
                    .post(body)
                    .build()

                val response = httpClient.newCall(httpRequest).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string().orEmpty()
                    val jsonResponse = JSONObject(responseBody)
                    val subject = jsonResponse.optString("subject", "Invoice ${request.invoiceId}")
                    val text = jsonResponse.optString("communicationText", "")
                    val source = jsonResponse.optString("source", "genkit_server")

                    if (text.isNotBlank()) {
                        return@withContext InvoiceCommunicationResult(
                            success = true,
                            subject = subject,
                            communicationText = text,
                            tone = request.tone.key,
                            source = source
                        )
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Server endpoint unreachable, falling back to local template engine: ${e.message}")
            }

            // 2. High-quality deterministic fallback template generation
            generateDeterministicTemplate(request)
        }

    /**
     * Context-aware deterministic template generator for offline or prototype fallback.
     */
    fun generateDeterministicTemplate(request: InvoiceCommunicationRequest): InvoiceCommunicationResult {
        val client = request.clientName.ifBlank { "Valued Client" }
        val inv = request.invoiceId.ifBlank { "INV-NEW" }
        val curr = request.currency.ifBlank { "EUR" }
        val formattedAmount = "$curr %.2f".format(request.amount)
        val due = request.dueDate.ifBlank { "Upon receipt" }
        val extra = if (request.customInstructions.isNotBlank()) "\nSpecial Note: ${request.customInstructions}" else ""

        val (subject, text) = when (request.tone) {
            InvoiceCommunicationTone.FRIENDLY_WELCOME -> {
                "Invoice $inv - Thank you for your partnership!" to
                        """
                        Dear $client,

                        Thank you for choosing Oistars Billings. We truly appreciate your continued trust and collaboration.

                        Please find attached invoice $inv for $formattedAmount, with payment scheduled for $due.$extra

                        Instant settlement is available via European Wero or SEPA Direct. Please let our billing desk know if you have any questions.

                        Warm regards,
                        Oistars Billing & Commerce Team
                        """.trimIndent()
            }

            InvoiceCommunicationTone.FORMAL_B2B -> {
                "Tax Invoice $inv - Settlement Notice for $client" to
                        """
                        Dear Accounts Payable Team,

                        Please review tax invoice $inv issued to $client for the total payable amount of $formattedAmount. In accordance with agreed corporate billing terms, settlement is due by $due.$extra

                        Kindly remit funds directly to our treasury account:
                        Beneficiary: Oistars Webshop Brands B.V.
                        IBAN: NL43 INGB 0006 3721 46
                        BIC: INGBNL2A
                        Reference: $inv

                        Sincerely,
                        Finance & Corporate Billing Department
                        Oistars Brands B.V.
                        """.trimIndent()
            }

            InvoiceCommunicationTone.PAYMENT_REMINDER -> {
                "Payment Reminder: Invoice $inv ($formattedAmount)" to
                        """
                        Hello $client,

                        This is a friendly reminder that invoice $inv for $formattedAmount is due on $due.$extra

                        If payment has already been initiated, please disregard this notice with our thanks. Otherwise, we kindly invite you to settle via Wero or SEPA instant wire:
                        IBAN: NL43 INGB 0006 3721 46 (Reference: $inv).

                        Best regards,
                        Oistars Collections Team
                        """.trimIndent()
            }

            InvoiceCommunicationTone.OVERDUE_NOTICE -> {
                "URGENT: Overdue Notice for Invoice $inv ($formattedAmount)" to
                        """
                        Dear $client,

                        Our ledger indicates that invoice $inv in the amount of $formattedAmount passed its due date ($due) and remains pending.$extra

                        To ensure uninterrupted service and avoid late charges, please arrange immediate remittance via Wero or SEPA Instant (IBAN: NL43 INGB 0006 3721 46, BIC: INGBNL2A).

                        Please contact accounting immediately if this invoice has already been cleared.

                        Urgent regards,
                        Oistars Treasury & Collections
                        """.trimIndent()
            }

            InvoiceCommunicationTone.WERO_SEPA_DETAILS -> {
                "Instant Wire Details for Invoice $inv via Wero & SEPA" to
                        """
                        Dear $client,

                        For instant zero-fee settlement of invoice $inv ($formattedAmount), you can transfer directly using European Wero or SEPA Instant Wire:

                        Beneficiary: Oistars Webshop Brands B.V.
                        Bank: ING Bank Amsterdam
                        IBAN: NL43 INGB 0006 3721 46
                        BIC: INGBNL2A
                        Payment Reference: $inv
                        Due Date: $due$extra

                        Thank you for your prompt business!
                        Oistars Treasury Desk
                        """.trimIndent()
            }
        }

        return InvoiceCommunicationResult(
            success = true,
            subject = subject,
            communicationText = text,
            tone = request.tone.key,
            source = "local_template_engine"
        )
    }
}
