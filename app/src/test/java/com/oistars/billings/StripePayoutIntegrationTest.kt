package com.oistars.billings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PayoutEntity
import com.example.data.local.PayoutSpeed
import com.example.data.local.PayoutStatus
import com.example.data.service.PayoutLiveService
import com.example.data.service.StripeHelpers
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StripePayoutIntegrationTest {

    private fun startServer(handler: (HttpExchange) -> Pair<Int, String>): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val (code, body) = handler(exchange)
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        return server
    }

    @Test
    fun `stripe key validation rejects placeholders and publishable keys`() {
        assertFalse(StripeHelpers.isValidStripeSecretKey(null))
        assertFalse(StripeHelpers.isValidStripeSecretKey(""))
        assertFalse(StripeHelpers.isValidStripeSecretKey("STRIPE_SECRET_KEY_PLACEHOLDER"))
        assertFalse(StripeHelpers.isValidStripeSecretKey("pk_test_51ABC123"))
        assertFalse(StripeHelpers.isValidStripeSecretKey("sk_test_PLACEHOLDER"))

        assertTrue(StripeHelpers.isValidStripeSecretKey("sk_test_51ExampleSecret"))
        assertTrue(StripeHelpers.isValidStripeSecretKey("sk_live_51ExampleSecret"))
        assertTrue(StripeHelpers.isValidStripeSecretKey("rk_live_51ExampleRestricted"))

        assertFalse(StripeHelpers.isLiveStripeKey("sk_test_51ExampleSecret"))
        assertTrue(StripeHelpers.isLiveStripeKey("sk_live_51ExampleSecret"))
        assertTrue(StripeHelpers.isLiveStripeKey("rk_live_51ExampleRestricted"))

        assertNull(StripeHelpers.validConnectAccountId("STRIPE_CONNECT_ACCOUNT_ID_PLACEHOLDER"))
        assertNull(StripeHelpers.validConnectAccountId("invalid_acct"))
        assertEquals("acct_1OistarsTest", StripeHelpers.validConnectAccountId(" acct_1OistarsTest "))
    }

    @Test
    fun `stripe cents conversion avoids floating point precision errors`() {
        assertEquals(0L, StripeHelpers.eurosToStripeCents(0.0))
        assertEquals(0L, StripeHelpers.eurosToStripeCents(-15.0))
        assertEquals(1999L, StripeHelpers.eurosToStripeCents(19.99))
        assertEquals(245050L, StripeHelpers.eurosToStripeCents(2450.50))
    }

    @Test
    fun `stripe payout statuses map cleanly to app PayoutStatus`() {
        assertEquals(PayoutStatus.COMPLETED, StripeHelpers.mapStripeStatus("paid"))
        assertEquals(PayoutStatus.PROCESSING, StripeHelpers.mapStripeStatus("pending"))
        assertEquals(PayoutStatus.PROCESSING, StripeHelpers.mapStripeStatus("in_transit"))
        assertEquals(PayoutStatus.CANCELLED, StripeHelpers.mapStripeStatus("canceled"))
        assertEquals(PayoutStatus.FAILED, StripeHelpers.mapStripeStatus("failed"))
    }

    @Test
    fun `room database v4 persists and retrieves stripe payout metadata`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val stripePayout = PayoutEntity(
            id = "po_1QStripeTest999",
            ownerId = "jurgen-westerveld",
            ownerName = "Jurgen Paul Westerveld",
            amount = 1425.75,
            currency = "EUR",
            fee = 0.0,
            netAmount = 1425.75,
            status = PayoutStatus.PROCESSING.name,
            destinationBank = "ING Bank N.V.",
            destinationIban = "NL43 INGB 0006 3721 46",
            destinationBic = "INGBNL2A",
            payoutSpeed = PayoutSpeed.STRIPE_INSTANT.name,
            reference = "Stripe Instant Owner Draw",
            initiatedAt = 1775000000000L,
            estimatedArrivalAt = 1775000015000L,
            completedAt = null,
            notes = "Stripe Payout (po_1QStripeTest999) • Status: in_transit • Method: instant",
            stripePayoutId = "po_1QStripeTest999",
            stripeBalanceTxId = "txn_1QBalanceTx888",
            stripeArrivalDate = 1775000015000L,
            providerRail = "STRIPE"
        )

        db.payoutDao().insertPayout(stripePayout)
        val loaded = db.payoutDao().getPayoutById("po_1QStripeTest999")

        assertNotNull(loaded)
        assertTrue(loaded!!.isStripePayout)
        assertEquals("po_1QStripeTest999", loaded.stripePayoutId)
        assertEquals("txn_1QBalanceTx888", loaded.stripeBalanceTxId)
        assertEquals(1775000015000L, loaded.stripeArrivalDate)
        assertEquals("STRIPE", loaded.providerRail)
        assertEquals(PayoutSpeed.STRIPE_INSTANT, loaded.speed)
        assertEquals(PayoutStatus.PROCESSING, loaded.payoutStatus)
        assertEquals(1425.75, loaded.amount, 0.001)

        db.close()
    }

    @Test
    fun `live stripe service fetches balance account and payouts over HTTP`() = runBlocking {
        val observedHeaders = mutableListOf<String>()
        val stripeServer = startServer { exchange ->
            val auth = exchange.requestHeaders.getFirst("Authorization").orEmpty()
            val acct = exchange.requestHeaders.getFirst("Stripe-Account").orEmpty()
            observedHeaders.add("${exchange.requestURI.path}|$auth|$acct")
            when (exchange.requestURI.path) {
                "/v1/balance" -> 200 to """
                    {
                      "object": "balance",
                      "available": [{"amount": 425050, "currency": "eur"}],
                      "pending": [{"amount": 68000, "currency": "eur"}]
                    }
                """.trimIndent()
                "/v1/accounts/acct_1TestConnect" -> 200 to """
                    {
                      "id": "acct_1TestConnect",
                      "email": "westerveldjp@gmail.com",
                      "payouts_enabled": true,
                      "external_accounts": {
                        "data": [{"bank_name": "ING Bank N.V.", "last4": "2146"}]
                      },
                      "settings": {
                        "payouts": { "schedule": { "interval": "daily" } }
                      }
                    }
                """.trimIndent()
                "/v1/payouts" -> 200 to """
                    {
                      "object": "list",
                      "data": [
                        {
                          "id": "po_1LiveSync001",
                          "amount": 125000,
                          "currency": "eur",
                          "status": "paid",
                          "method": "instant",
                          "created": 1775000000,
                          "arrival_date": 1775000010,
                          "balance_transaction": "txn_1LiveSync001",
                          "description": "Weekly Owner Settlement"
                        }
                      ]
                    }
                """.trimIndent()
                else -> 404 to """{"error":{"message":"Not found"}}"""
            }
        }

        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val service = PayoutLiveService(
                context = context,
                backendBaseUrl = "http://127.0.0.1:1", // unreachable backend forces direct Stripe REST path
                stripeApiBase = "http://127.0.0.1:${stripeServer.address.port}/v1",
                stripeKeyOverride = "sk_test_51LiveHttpTest",
                connectAccountOverride = "acct_1TestConnect"
            )

            val result = service.fetchLiveStripeBalanceAndPayouts("jurgen-westerveld")
            assertTrue("Expected live Stripe fetch to succeed", result.isSuccess)

            val payouts = result.getOrThrow()
            assertEquals(1, payouts.size)
            assertEquals("po_1LiveSync001", payouts[0].stripePayoutId)
            assertEquals("txn_1LiveSync001", payouts[0].stripeBalanceTxId)
            assertEquals(1250.0, payouts[0].amount, 0.001)
            assertEquals(PayoutStatus.COMPLETED.name, payouts[0].status)
            assertEquals(PayoutSpeed.STRIPE_INSTANT.name, payouts[0].payoutSpeed)

            val state = service.stripeBalanceState.value
            assertTrue(state.isConfigured)
            assertFalse(state.isLiveMode)
            assertEquals("acct_1TestConnect", state.accountId)
            assertEquals(4250.50, state.availableBalanceEur, 0.001)
            assertEquals(680.00, state.pendingBalanceEur, 0.001)
            assertEquals("ING Bank N.V.", state.bankName)
            assertEquals("2146", state.ibanLast4)
            assertEquals("daily", state.scheduleInterval)
            assertTrue(state.payoutsEnabled)
            assertTrue(observedHeaders.any { it == "/v1/balance|Bearer sk_test_51LiveHttpTest|acct_1TestConnect" })
        } finally {
            stripeServer.stop(0)
        }
    }

    @Test
    fun `live stripe payout initiation posts cents and handles rejection without faking ids`() = runBlocking {
        var capturedFormBody = ""
        var returnError = false
        val stripeServer = startServer { exchange ->
            if (exchange.requestURI.path == "/v1/payouts" && exchange.requestMethod == "POST") {
                capturedFormBody = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
                if (returnError) {
                    402 to """{"error":{"code":"balance_insufficient","message":"Insufficient funds in your Stripe account"}}"""
                } else {
                    200 to """
                        {
                          "id": "po_1CreatedLive99",
                          "amount": 12550,
                          "currency": "eur",
                          "status": "in_transit",
                          "method": "instant",
                          "created": 1775001000,
                          "arrival_date": 1775001015,
                          "balance_transaction": "txn_1CreatedLive99",
                          "description": "Live Stripe Test Transfer"
                        }
                    """.trimIndent()
                }
            } else if (exchange.requestURI.path == "/v1/payouts/po_1CreatedLive99/cancel" && exchange.requestMethod == "POST") {
                200 to """{"id": "po_1CreatedLive99", "status": "canceled"}"""
            } else {
                404 to """{"error":{"message":"Not found"}}"""
            }
        }

        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val service = PayoutLiveService(
                context = context,
                backendBaseUrl = "http://127.0.0.1:1",
                stripeApiBase = "http://127.0.0.1:${stripeServer.address.port}/v1",
                stripeKeyOverride = "sk_live_51RealKeyTest"
            )

            val draft = PayoutEntity(
                id = "PO-DRAFT-1",
                amount = 125.50,
                payoutSpeed = PayoutSpeed.STRIPE_INSTANT.name,
                reference = "Live Stripe Test Transfer",
                providerRail = "STRIPE"
            )

            val successRes = service.initiateLivePayout("jurgen-westerveld", draft)
            assertTrue("Expected Stripe payout creation to succeed", successRes.isSuccess)
            val created = successRes.getOrThrow()
            assertEquals("po_1CreatedLive99", created.id)
            assertEquals("po_1CreatedLive99", created.stripePayoutId)
            assertEquals("txn_1CreatedLive99", created.stripeBalanceTxId)
            assertEquals(PayoutStatus.PROCESSING.name, created.status)

            val decodedForm = URLDecoder.decode(capturedFormBody, "UTF-8")
            assertTrue(decodedForm.contains("amount=12550"))
            assertTrue(decodedForm.contains("currency=eur"))
            assertTrue(decodedForm.contains("method=instant"))

            // Test cancellation hits /v1/payouts/{id}/cancel
            val cancelRes = service.cancelLivePayout("jurgen-westerveld", created.id, created.stripePayoutId)
            assertTrue(cancelRes.isSuccess)

            // Test Stripe 402 rejection does not fabricate a payout
            returnError = true
            val failRes = service.initiateLivePayout("jurgen-westerveld", draft)
            assertTrue("Expected Stripe 402 to fail", failRes.isFailure)
            assertTrue(failRes.exceptionOrNull()?.message?.contains("Insufficient funds") == true)
        } finally {
            stripeServer.stop(0)
        }
    }

    @Test
    fun `backend proxy live feed health probe and stripe endpoints work end to end`() = runBlocking {
        val backendServer = startServer { exchange ->
            when (exchange.requestURI.path) {
                "/api/payouts/live-feed" -> 200 to """{"success":true,"gatewayOnline":true,"payouts":[],"webhooks":[]}"""
                "/api/stripe/balance" -> 200 to """
                    {
                      "configured": true,
                      "mode": "live",
                      "availableEur": 8900.25,
                      "pendingEur": 310.0,
                      "accountId": "acct_BackendProxy01",
                      "payoutsEnabled": true,
                      "bankName": "ING Bank N.V.",
                      "ibanLast4": "2146",
                      "scheduleInterval": "manual"
                    }
                """.trimIndent()
                "/api/stripe/payouts" -> 200 to """
                    {
                      "configured": true,
                      "count": 1,
                      "payouts": [
                        {
                          "id": "po_Backend01",
                          "ownerId": "jurgen-westerveld",
                          "ownerName": "Jurgen Paul Westerveld",
                          "amount": 500.0,
                          "currency": "EUR",
                          "status": "COMPLETED",
                          "payoutSpeed": "STRIPE_STANDARD",
                          "stripePayoutId": "po_Backend01",
                          "stripeBalanceTxId": "txn_Backend01",
                          "providerRail": "STRIPE"
                        }
                      ]
                    }
                """.trimIndent()
                else -> 404 to """{"error":"not found"}"""
            }
        }

        try {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val service = PayoutLiveService(
                context = context,
                backendBaseUrl = "http://127.0.0.1:${backendServer.address.port}",
                stripeApiBase = "http://127.0.0.1:1/v1",
                stripeKeyOverride = "STRIPE_SECRET_KEY_PLACEHOLDER"
            )

            val syncRes = service.fetchLiveStripeBalanceAndPayouts("jurgen-westerveld")
            assertTrue(syncRes.isSuccess)
            assertEquals(1, syncRes.getOrThrow().size)
            assertEquals("po_Backend01", syncRes.getOrThrow()[0].stripePayoutId)
            assertTrue(service.stripeBalanceState.value.isLiveMode)
            assertEquals(8900.25, service.stripeBalanceState.value.availableBalanceEur, 0.001)

            val healthRes = service.probeGatewayHealth("jurgen-westerveld")
            assertTrue(healthRes.isSuccess)
            val gateway = healthRes.getOrThrow()
            assertTrue(gateway.isConnected)
            assertTrue(gateway.stripeConnected)
            assertEquals("Stripe Live", gateway.stripeModeLabel)
            assertEquals("acct_BackendProxy01", gateway.stripeAccountId)
        } finally {
            backendServer.stop(0)
        }
    }
}
