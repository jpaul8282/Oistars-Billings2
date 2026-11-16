package com.oistars.billings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.example.data.local.PayoutEntity
import com.example.data.local.PayoutSpeed
import com.example.data.local.PayoutStatus
import com.example.data.service.AutoPayoutRules
import com.example.data.service.PayoutGatewayStatus
import com.example.data.service.SettlementWebhookEvent
import com.example.data.service.StripeLiveBalanceState
import com.example.ui.screens.OwnerPayoutScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OwnerPayoutMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OwnerPayoutLiveUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleMetrics() = OwnerPayoutMetrics(
        availableBalance = 5200.00,
        pendingPayoutsAmount = 750.00,
        totalPaidOutLifetime = 12400.00,
        totalPayoutsCount = 2,
        completedPayoutsCount = 1,
        inTransitPayoutsCount = 1,
        currency = "EUR",
        clearedFundsBalance = 5200.00,
        pendingFundsBalance = 400.00,
        grossRevenueTotal = 18000.00,
        clearedOrdersAmount = 1819.00,
        clearedOrdersCount = 2,
        pendingOrdersAmount = 0.0,
        pendingOrdersCount = 0,
        clearedInvoicesAmount = 15781.00,
        pendingInvoicesAmount = 400.00,
        clearanceRatePercent = 97
    )

    private fun sampleStripePayouts() = listOf(
        PayoutEntity(
            id = "po_1LivePaid001",
            ownerId = "jurgen-westerveld",
            ownerName = "Jurgen Paul Westerveld",
            amount = 2100.00,
            currency = "EUR",
            status = PayoutStatus.COMPLETED.name,
            destinationBank = "ING Bank N.V.",
            destinationIban = "NL43 INGB 0006 3721 46",
            destinationBic = "INGBNL2A",
            payoutSpeed = PayoutSpeed.STRIPE_INSTANT.name,
            reference = "Stripe Instant Owner Draw",
            stripePayoutId = "po_1LivePaid001",
            stripeBalanceTxId = "txn_1LivePaid001",
            providerRail = "STRIPE"
        ),
        PayoutEntity(
            id = "po_1LiveTransit002",
            ownerId = "jurgen-westerveld",
            ownerName = "Jurgen Paul Westerveld",
            amount = 750.00,
            currency = "EUR",
            status = PayoutStatus.PROCESSING.name,
            destinationBank = "ING Bank N.V.",
            destinationIban = "NL43 INGB 0006 3721 46",
            destinationBic = "INGBNL2A",
            payoutSpeed = PayoutSpeed.STRIPE_STANDARD.name,
            reference = "Stripe Standard Treasury Batch",
            stripePayoutId = "po_1LiveTransit002",
            stripeBalanceTxId = "txn_1LiveTransit002",
            providerRail = "STRIPE"
        )
    )

    @Test
    fun `live payout strip and stripe connect card show unconfigured setup notice when key is missing`() {
        var refreshCalled = false

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerPayoutScreen(
                    metrics = sampleMetrics(),
                    payouts = emptyList(),
                    gatewayStatus = PayoutGatewayStatus(
                        isConnected = true,
                        gatewayName = "Stripe Connect + Wero EPI",
                        latencyMs = 31L,
                        stripeConnected = false,
                        stripeModeLabel = "Stripe Setup Needed"
                    ),
                    stripeBalanceState = StripeLiveBalanceState(
                        isConfigured = false,
                        statusMessage = "Add STRIPE_SECRET_KEY in AI Studio Secrets"
                    ),
                    onRequestPayout = { _, _, _, _, _, _, _, _, _ -> },
                    onCancelPayout = {},
                    onDeletePayout = {},
                    onRefreshLiveFeed = { refreshCalled = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_payout_gateway_strip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stripe Setup Needed").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_stripe_connect_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stripe API Key Not Configured").assertIsDisplayed()
        composeTestRule.onNodeWithText("KEY REQUIRED").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_refresh_live_gateway").performClick()
        composeTestRule.waitForIdle()
        assertTrue(refreshCalled)
    }

    @Test
    fun `live stripe connect card displays live mode balance webhooks and triggers sync`() {
        var syncClicks = 0
        val liveWebhooks = listOf(
            SettlementWebhookEvent(
                id = "WHK-STR-1",
                provider = "STRIPE_WEBHOOK",
                transactionRef = "po_1LivePaid001",
                amount = 2100.00,
                currency = "EUR",
                iban = "NL43 INGB 0006 3721 46",
                status = "SETTLED"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerPayoutScreen(
                    metrics = sampleMetrics(),
                    payouts = sampleStripePayouts(),
                    gatewayStatus = PayoutGatewayStatus(
                        isConnected = true,
                        gatewayName = "Stripe API + Cloud Functions Gateway",
                        latencyMs = 24L,
                        stripeConnected = true,
                        stripeModeLabel = "Stripe Live",
                        stripeAccountId = "acct_1OistarsLive"
                    ),
                    stripeBalanceState = StripeLiveBalanceState(
                        isConfigured = true,
                        isLiveMode = true,
                        accountId = "acct_1OistarsLive",
                        payoutsEnabled = true,
                        availableBalanceEur = 4250.50,
                        pendingBalanceEur = 680.00,
                        bankName = "ING Bank N.V.",
                        ibanLast4 = "2146",
                        scheduleInterval = "daily",
                        recentPayoutsCount = 2,
                        statusMessage = "Stripe REST API Connected (Live • 24ms)"
                    ),
                    settlementWebhooks = liveWebhooks,
                    onRequestPayout = { _, _, _, _, _, _, _, _, _ -> },
                    onCancelPayout = {},
                    onDeletePayout = {},
                    onRefreshLiveFeed = { syncClicks++ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Stripe Live").assertIsDisplayed()
        composeTestRule.onNodeWithText("LIVE MODE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account: acct_1OistarsLive").assertIsDisplayed()
        composeTestRule.onNodeWithText("External Bank: ING Bank N.V. •••• 2146").assertIsDisplayed()

        composeTestRule.onNodeWithTag("btn_sync_stripe_live").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, syncClicks)

        // Scroll to live webhook card and payout history items
        composeTestRule.onNodeWithTag("owner_payout_screen")
            .performScrollToNode(hasTestTag("live_settlement_webhook_card"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Live Stripe & Wero Webhook Stream").assertIsDisplayed()
        composeTestRule.onNodeWithText("STRIPE_WEBHOOK • po_1LivePaid001").assertIsDisplayed()
    }

    @Test
    fun `request payout dialog submits live stripe instant payout with entered amount`() {
        var capturedAmount: Double? = null
        var capturedSpeed: PayoutSpeed? = null
        var capturedIban: String? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerPayoutScreen(
                    metrics = sampleMetrics(),
                    payouts = sampleStripePayouts(),
                    stripeBalanceState = StripeLiveBalanceState(
                        isConfigured = true,
                        isLiveMode = false,
                        availableBalanceEur = 3500.00,
                        payoutsEnabled = true
                    ),
                    onRequestPayout = { amount, _, _, _, iban, _, speed, _, _ ->
                        capturedAmount = amount
                        capturedSpeed = speed
                        capturedIban = iban
                    },
                    onCancelPayout = {},
                    onDeletePayout = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owner_payout_screen")
            .performScrollToNode(hasTestTag("btn_request_payout"))
        composeTestRule.onNodeWithTag("btn_request_payout").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("request_payout_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("payout_amount_input").performTextReplacement("425.75")
        composeTestRule.onNodeWithTag("option_stripe_instant").performClick()
        composeTestRule.onNodeWithTag("confirm_payout_button").performClick()
        composeTestRule.waitForIdle()

        assertNotNull(capturedAmount)
        assertEquals(425.75, capturedAmount!!, 0.001)
        assertEquals(PayoutSpeed.STRIPE_INSTANT, capturedSpeed)
        assertEquals("NL43 INGB 0006 3721 46", capturedIban)
    }

    @Test
    fun `clicking in-transit stripe payout shows receipt ids and triggers cancel callback`() {
        var cancelledPayout: PayoutEntity? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerPayoutScreen(
                    metrics = sampleMetrics(),
                    payouts = sampleStripePayouts(),
                    stripeBalanceState = StripeLiveBalanceState(isConfigured = true, availableBalanceEur = 3500.00),
                    onRequestPayout = { _, _, _, _, _, _, _, _, _ -> },
                    onCancelPayout = { cancelledPayout = it },
                    onDeletePayout = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owner_payout_screen")
            .performScrollToNode(hasTestTag("payout_item_po_1LiveTransit002"))
        composeTestRule.onNodeWithTag("payout_item_po_1LiveTransit002").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("payout_detail_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("po_1LiveTransit002").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("txn_1LiveTransit002").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel Payout").performClick()
        composeTestRule.waitForIdle()
        assertEquals("po_1LiveTransit002", cancelledPayout?.stripePayoutId)
    }

    @Test
    fun `automated settlement rules save and auto-sweep actions trigger callbacks`() {
        var rulesSaved = false
        var autoSweepTriggered = false

        composeTestRule.setContent {
            MyApplicationTheme {
                OwnerPayoutScreen(
                    metrics = sampleMetrics(),
                    payouts = emptyList(),
                    autoPayoutRules = AutoPayoutRules(enabled = true, minThreshold = 250.0, reserveFloat = 100.0),
                    onRequestPayout = { _, _, _, _, _, _, _, _, _ -> },
                    onCancelPayout = {},
                    onDeletePayout = {},
                    onSaveAutoPayoutRules = { _, _, _, _ -> rulesSaved = true },
                    onExecuteAutoSweepNow = { autoSweepTriggered = true }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("owner_payout_screen")
            .performScrollToNode(hasTestTag("btn_save_auto_payout_rules"))
        composeTestRule.onNodeWithTag("btn_save_auto_payout_rules").performClick()
        composeTestRule.onNodeWithTag("btn_execute_auto_sweep").performClick()
        composeTestRule.waitForIdle()

        assertTrue(rulesSaved)
        assertTrue(autoSweepTriggered)
    }
}
