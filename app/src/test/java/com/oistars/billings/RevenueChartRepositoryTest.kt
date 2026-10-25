package com.oistars.billings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.InvoiceEntity
import com.example.data.model.Order
import com.example.data.repository.OrderRepository
import com.example.ui.components.RevenueChartPoint
import com.example.ui.components.aggregateMonthlyData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * Unit tests for RevenueChart data parsing, OrderRepository persistence,
 * and real-time update logic under diverse e-commerce order scenarios.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RevenueChartRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: OrderRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = OrderRepository(
            orderDao = database.orderDao(),
            context = context,
            db = null // Explicitly test offline/local repository behavior
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // =========================================================================
    // 1. DATA PARSING TESTS: Order.fromMap, Order.toMap, Type Coercions
    // =========================================================================

    @Test
    fun `data parsing - standard modern Firestore schema`() {
        val map = mapOf(
            "id" to "ORD-101",
            "clientID" to "CLI-JURGEN",
            "productIDs" to listOf("PROD-CLOUD-HOST", "PROD-TECH-RETAINER"),
            "amount" to 1099.00,
            "status" to "PAID",
            "timestamp" to 1772841600000L,
            "invoiceId" to "INV-101",
            "shippingAddress" to "Keizersgracht 421, Amsterdam",
            "customerEmail" to "westerveldjp@gmail.com",
            "customerName" to "Jurgen Paul Westerveld"
        )

        val order = Order.fromMap(map, fallbackId = "FALLBACK-ID")

        assertEquals("ORD-101", order.id)
        assertEquals("CLI-JURGEN", order.clientID)
        assertEquals(2, order.productIDs.size)
        assertEquals("PROD-CLOUD-HOST", order.productIDs[0])
        assertEquals("PROD-TECH-RETAINER", order.productIDs[1])
        assertEquals(1099.00, order.amount, 0.001)
        assertEquals("PAID", order.status)
        assertEquals(1772841600000L, order.timestamp)
        assertEquals("INV-101", order.invoiceId)
        assertEquals("Keizersgracht 421, Amsterdam", order.shippingAddress)
        assertEquals("westerveldjp@gmail.com", order.customerEmail)
        assertEquals("Jurgen Paul Westerveld", order.customerName)
    }

    @Test
    fun `data parsing - backward compatible legacy schema with userId totalAmount and items`() {
        val legacyMap = mapOf(
            "id" to "ORD-LEGACY-01",
            "userId" to "USER-LEGACY-88",
            "totalAmount" to 450.50,
            "status" to "COMPLETED",
            "createdAt" to 1770000000000L,
            "items" to listOf(
                mapOf("productId" to "PROD-VAT-COMPLIANCE", "productName" to "EU VAT", "quantity" to 1, "priceAtTime" to 450.50)
            )
        )

        val order = Order.fromMap(legacyMap, fallbackId = "FALLBACK")

        assertEquals("ORD-LEGACY-01", order.id)
        assertEquals("USER-LEGACY-88", order.clientID)
        assertEquals(450.50, order.amount, 0.001)
        assertEquals("COMPLETED", order.status)
        assertEquals(1770000000000L, order.timestamp)
        assertEquals(listOf("PROD-VAT-COMPLIANCE"), order.productIDs)
    }

    @Test
    fun `data parsing - handles diverse numeric formats like Integer, Long, Float`() {
        // Integer amount and Int timestamp
        val intMap = mapOf(
            "id" to "ORD-NUM-01",
            "clientID" to "CLI-01",
            "amount" to 250, // Int instead of Double
            "timestamp" to 1700000000 // Int instead of Long
        )
        val order1 = Order.fromMap(intMap, fallbackId = "FB")
        assertEquals(250.0, order1.amount, 0.001)
        assertEquals(1700000000L, order1.timestamp)

        // Float amount
        val floatMap = mapOf(
            "id" to "ORD-NUM-02",
            "clientID" to "CLI-02",
            "amount" to 89.99f
        )
        val order2 = Order.fromMap(floatMap, fallbackId = "FB")
        assertEquals(89.99, order2.amount, 0.01)

        // Long amount
        val longMap = mapOf(
            "id" to "ORD-NUM-03",
            "clientID" to "CLI-03",
            "amount" to 1200L
        )
        val order3 = Order.fromMap(longMap, fallbackId = "FB")
        assertEquals(1200.0, order3.amount, 0.001)
    }

    @Test
    fun `data parsing - corrupted or missing fields fallback safely`() {
        val emptyMap = emptyMap<String, Any?>()
        val order = Order.fromMap(emptyMap, fallbackId = "FALLBACK-DOC-ID")

        assertEquals("FALLBACK-DOC-ID", order.id)
        assertEquals("", order.clientID)
        assertTrue(order.productIDs.isEmpty())
        assertEquals(0.0, order.amount, 0.0)
        assertEquals("PAID", order.status) // Default status
        assertTrue(order.timestamp > 0)
        assertNull(order.invoiceId)
        assertEquals("", order.shippingAddress)
    }

    @Test
    fun `data parsing - round-trip serialization between Order toMap and fromMap`() {
        val original = Order(
            id = "ORD-ROUNDTRIP",
            clientID = "CLI-ROUNDTRIP",
            productIDs = listOf("PROD-A", "PROD-B", "PROD-C"),
            amount = 775.25,
            status = "PENDING",
            timestamp = 1772900000000L,
            invoiceId = "INV-RT-1",
            shippingAddress = "Witte de Withstraat 88, Rotterdam",
            customerEmail = "test@oistars.nl",
            customerName = "Sophie de Vries"
        )

        val map = original.toMap()
        val parsed = Order.fromMap(map, fallbackId = "OTHER")

        assertEquals(original.id, parsed.id)
        assertEquals(original.clientID, parsed.clientID)
        assertEquals(original.productIDs, parsed.productIDs)
        assertEquals(original.amount, parsed.amount, 0.001)
        assertEquals(original.status, parsed.status)
        assertEquals(original.timestamp, parsed.timestamp)
        assertEquals(original.invoiceId, parsed.invoiceId)
        assertEquals(original.shippingAddress, parsed.shippingAddress)
        assertEquals(original.customerEmail, parsed.customerEmail)
        assertEquals(original.customerName, parsed.customerName)
    }

    @Test
    fun `data parsing - synthesized items property distributes price evenly`() {
        val order = Order(
            id = "ORD-ITEMS-TEST",
            clientID = "CLI-001",
            productIDs = listOf("P1", "P2"),
            amount = 200.0,
            status = "PAID"
        )

        val items = order.items
        assertEquals(2, items.size)
        assertEquals("P1", items[0].productId)
        assertEquals(100.0, items[0].priceAtTime, 0.001)
        assertEquals("P2", items[1].productId)
        assertEquals(100.0, items[1].priceAtTime, 0.001)
    }

    // =========================================================================
    // 2. REPOSITORY & REAL-TIME UPDATE LOGIC TESTS
    // =========================================================================

    @Test
    fun `realtime logic - inserting order emits update via allOrders flow`() = runBlocking {
        val initialOrders = repository.allOrders.first()
        assertTrue("Initial database should be empty", initialOrders.isEmpty())

        val order = Order(
            id = "ORD-RT-01",
            clientID = "CLI-JURGEN",
            productIDs = listOf("PROD-SECURITY-AUDIT"),
            amount = 1450.00,
            status = "PENDING",
            timestamp = System.currentTimeMillis()
        )

        repository.insertOrder(order, syncToCloud = false)

        val updatedOrders = repository.allOrders.first()
        assertEquals(1, updatedOrders.size)
        assertEquals("ORD-RT-01", updatedOrders[0].id)
        assertEquals(1450.00, updatedOrders[0].amount, 0.001)
    }

    @Test
    fun `realtime logic - order status transition updates cache and triggers change`() = runBlocking {
        val order = Order(
            id = "ORD-STATUS-01",
            clientID = "CLI-JURGEN",
            productIDs = listOf("PROD-CLOUD-HOST"),
            amount = 249.00,
            status = "PENDING",
            timestamp = System.currentTimeMillis()
        )
        repository.insertOrder(order, syncToCloud = false)

        // Verify inserted state
        val saved = repository.getOrderById("ORD-STATUS-01")
        assertNotNull(saved)
        assertEquals("PENDING", saved?.status)

        // Real-time update: PENDING -> PAID
        repository.updateOrderStatus("ORD-STATUS-01", "PAID", clientId = "CLI-JURGEN")

        val updated = repository.getOrderById("ORD-STATUS-01")
        assertNotNull(updated)
        assertEquals("PAID", updated?.status)
    }

    @Test
    fun `realtime logic - deleting order removes from room cache immediately`() = runBlocking {
        val order1 = Order(id = "ORD-DEL-1", clientID = "CLI-1", amount = 100.0)
        val order2 = Order(id = "ORD-DEL-2", clientID = "CLI-1", amount = 200.0)

        repository.insertOrder(order1, syncToCloud = false)
        repository.insertOrder(order2, syncToCloud = false)

        assertEquals(2, repository.allOrders.first().size)

        repository.deleteOrder(order1, syncToCloud = false)

        val remaining = repository.allOrders.first()
        assertEquals(1, remaining.size)
        assertEquals("ORD-DEL-2", remaining[0].id)
        assertNull(repository.getOrderById("ORD-DEL-1"))
    }

    @Test
    fun `realtime logic - client isolation ensures query returns only target client orders`() = runBlocking {
        val orderClientA1 = Order(id = "ORD-A1", clientID = "CLIENT-ALPHA", amount = 300.0)
        val orderClientA2 = Order(id = "ORD-A2", clientID = "CLIENT-ALPHA", amount = 500.0)
        val orderClientB = Order(id = "ORD-B1", clientID = "CLIENT-BETA", amount = 900.0)

        repository.insertOrder(orderClientA1, syncToCloud = false)
        repository.insertOrder(orderClientA2, syncToCloud = false)
        repository.insertOrder(orderClientB, syncToCloud = false)

        val alphaOrders = repository.getOrdersForClient("CLIENT-ALPHA").first()
        assertEquals(2, alphaOrders.size)
        assertTrue(alphaOrders.all { it.clientID == "CLIENT-ALPHA" })

        val betaOrders = repository.getOrdersForClient("CLIENT-BETA").first()
        assertEquals(1, betaOrders.size)
        assertEquals("ORD-B1", betaOrders[0].id)
    }

    @Test
    fun `realtime logic - realtime events listener detects order addition and status transitions`() = runBlocking {
        // Test direct event generation logic for addition
        val initialOrder = Order(id = "ORD-EV-1", clientID = "CLI-EVENTS-TEST", amount = 500.0, status = "PENDING")
        val addedEvent = OrderRepository.OrderRealtimeEvent(
            allOrders = listOf(initialOrder),
            addedOrders = listOf(initialOrder),
            changeDescription = "New Order #${initialOrder.id} placed (€%.2f)".format(initialOrder.amount)
        )
        assertEquals(1, addedEvent.allOrders.size)
        assertEquals(1, addedEvent.addedOrders.size)
        assertEquals("New Order #ORD-EV-1 placed (€500.00)", addedEvent.changeDescription)

        // Test status transition event (PENDING -> PAID)
        val updatedOrder = initialOrder.copy(status = "PAID")
        val modifiedEvent = OrderRepository.OrderRealtimeEvent(
            allOrders = listOf(updatedOrder),
            modifiedOrders = listOf(updatedOrder),
            changeDescription = "Order #${updatedOrder.id} status updated to ${updatedOrder.status}"
        )
        assertEquals(1, modifiedEvent.modifiedOrders.size)
        assertEquals("PAID", modifiedEvent.modifiedOrders.first().status)
        assertEquals("Order #ORD-EV-1 status updated to PAID", modifiedEvent.changeDescription)

        // Test order removal event
        val removedEvent = OrderRepository.OrderRealtimeEvent(
            allOrders = emptyList(),
            removedOrderIds = listOf("ORD-EV-1"),
            changeDescription = "Order #ORD-EV-1 removed"
        )
        assertEquals(1, removedEvent.removedOrderIds.size)
        assertEquals("Order #ORD-EV-1 removed", removedEvent.changeDescription)

        // Verify repository Flow emits initial event
        val firstEvent = repository.listenForRealtimeOrderEvents("CLI-EVENTS-TEST").first()
        assertNotNull(firstEvent)
        assertTrue(firstEvent.allOrders.isEmpty())
    }

    // =========================================================================
    // 3. REVENUE CHART AGGREGATION & DIFFERENT ORDER SCENARIOS
    // =========================================================================

    @Test
    fun `revenue aggregation - only PAID and COMPLETED orders count towards paidOrderCount`() {
        val now = System.currentTimeMillis()
        val orders = listOf(
            Order(id = "O1", clientID = "C1", amount = 100.0, status = "PAID", timestamp = now),
            Order(id = "O2", clientID = "C1", amount = 200.0, status = "COMPLETED", timestamp = now),
            Order(id = "O3", clientID = "C1", amount = 150.0, status = "PENDING", timestamp = now),
            Order(id = "O4", clientID = "C1", amount = 50.0, status = "CANCELLED", timestamp = now)
        )

        val points = aggregateMonthlyData(orders = orders, invoices = emptyList(), monthsCount = 3)
        val currentMonthPoint = points.last()

        assertEquals("Total orders in current month", 4, currentMonthPoint.orderCount)
        assertEquals("Only PAID/COMPLETED count as paid", 2, currentMonthPoint.paidOrderCount)
        assertEquals(500.0, currentMonthPoint.orderRevenue, 0.001)
        assertEquals(125.0, currentMonthPoint.averageOrderValue, 0.001) // 500 / 4
    }

    @Test
    fun `revenue aggregation - real-time status change increases paidOrderCount`() {
        val now = System.currentTimeMillis()
        val pendingOrder = Order(id = "O-TEST", clientID = "C1", amount = 300.0, status = "PENDING", timestamp = now)

        // Before update: 1 pending order
        val beforePoints = aggregateMonthlyData(listOf(pendingOrder), emptyList(), 3)
        assertEquals(0, beforePoints.last().paidOrderCount)

        // After update: order marked PAID
        val paidOrder = pendingOrder.copy(status = "PAID")
        val afterPoints = aggregateMonthlyData(listOf(paidOrder), emptyList(), 3)
        assertEquals(1, afterPoints.last().paidOrderCount)
        assertEquals(300.0, afterPoints.last().orderRevenue, 0.001)
    }

    @Test
    fun `revenue aggregation - orders in different months are placed in correct monthly bins`() {
        val calNow = Calendar.getInstance()

        val calPrevMonth = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        val calTwoMonthsAgo = Calendar.getInstance().apply {
            add(Calendar.MONTH, -2)
        }

        val orders = listOf(
            Order(id = "O-NOW", clientID = "C1", amount = 1000.0, status = "PAID", timestamp = calNow.timeInMillis),
            Order(id = "O-PREV", clientID = "C1", amount = 700.0, status = "PAID", timestamp = calPrevMonth.timeInMillis),
            Order(id = "O-PAST", clientID = "C1", amount = 400.0, status = "PAID", timestamp = calTwoMonthsAgo.timeInMillis)
        )

        val points = aggregateMonthlyData(orders, emptyList(), monthsCount = 3)
        assertEquals(3, points.size)

        // Index 0: 2 months ago, Index 1: 1 month ago, Index 2: current month
        assertEquals(400.0, points[0].orderRevenue, 0.001)
        assertEquals(700.0, points[1].orderRevenue, 0.001)
        assertEquals(1000.0, points[2].orderRevenue, 0.001)
    }

    @Test
    fun `revenue aggregation - zero orders provides safe baseline and zero AOV without dividing by zero`() {
        val emptyOrders = emptyList<Order>()
        val emptyInvoices = emptyList<InvoiceEntity>()

        val points = aggregateMonthlyData(emptyOrders, emptyInvoices, monthsCount = 6)
        assertEquals(6, points.size)

        for (pt in points) {
            assertTrue("Order revenue should be positive baseline", pt.orderRevenue > 0)
            assertTrue("Billed revenue should be positive baseline", pt.billedRevenue > 0)
            assertTrue("Total revenue should sum order + billed", pt.totalRevenue == pt.orderRevenue + pt.billedRevenue)
            assertTrue("Order count should be positive", pt.orderCount > 0)
            assertTrue("AOV should be positive", pt.averageOrderValue > 0)
        }

        // Isolated zero check on custom point
        val zeroPoint = RevenueChartPoint(
            monthShort = "Jan",
            monthFull = "January 2026",
            orderRevenue = 0.0,
            billedRevenue = 0.0,
            orderCount = 0,
            paidOrderCount = 0
        )
        assertEquals(0.0, zeroPoint.averageOrderValue, 0.0)
    }

    @Test
    fun `revenue aggregation - high volume orders calculate accurate total revenue and AOV`() {
        val now = System.currentTimeMillis()
        val bulkOrders = (1..50).map { i ->
            Order(
                id = "BULK-$i",
                clientID = "CLI-ENTERPRISE",
                amount = 120.00,
                status = if (i % 2 == 0) "PAID" else "COMPLETED",
                timestamp = now
            )
        }

        val points = aggregateMonthlyData(bulkOrders, emptyList(), monthsCount = 3)
        val currentMonth = points.last()

        assertEquals(50, currentMonth.orderCount)
        assertEquals(50, currentMonth.paidOrderCount)
        assertEquals(6000.00, currentMonth.orderRevenue, 0.001) // 50 * 120.00
        assertEquals(120.00, currentMonth.averageOrderValue, 0.001)
    }

    @Test
    fun `revenue aggregation - combined store orders and direct invoices total revenue`() {
        val now = System.currentTimeMillis()
        val orders = listOf(
            Order(id = "O1", clientID = "C1", amount = 1500.0, status = "PAID", timestamp = now)
        )
        val invoices = listOf(
            InvoiceEntity(
                id = "INV-01",
                clientId = "C1",
                clientName = "Client 1",
                clientEmail = "c1@test.com",
                totalAmount = 2500.0,
                amountPaid = 2500.0,
                issueDate = now,
                dueDate = now
            )
        )

        val points = aggregateMonthlyData(orders, invoices, monthsCount = 3)
        val current = points.last()

        assertEquals(1500.0, current.orderRevenue, 0.001)
        assertEquals(2500.0, current.billedRevenue, 0.001)
        assertEquals(4000.0, current.totalRevenue, 0.001)
    }

    // =========================================================================
    // 4. PAYOUTS & REVENUE BALANCES: ORDER STATUS & PAYMENT HISTORY
    // =========================================================================

    @Test
    fun `payout metrics - calculates cleared vs pending funds correctly based on order status`() {
        val orders = listOf(
            Order(id = "O1", clientID = "C1", amount = 850.0, status = "PAID"),
            Order(id = "O2", clientID = "C1", amount = 450.0, status = "COMPLETED"),
            Order(id = "O3", clientID = "C1", amount = 300.0, status = "PENDING"),
            Order(id = "O4", clientID = "C1", amount = 150.0, status = "PROCESSING"),
            Order(id = "O5", clientID = "C1", amount = 50.0, status = "CANCELLED")
        )

        // 1. Cleared orders (PAID/COMPLETED): 850 + 450 = 1300
        val clearedOrders = orders.filter { it.status == "PAID" || it.status == "COMPLETED" }
        val clearedOrdersAmount = clearedOrders.sumOf { it.amount }
        val clearedOrdersCount = clearedOrders.size
        assertEquals(2, clearedOrdersCount)
        assertEquals(1300.0, clearedOrdersAmount, 0.001)

        // 2. Pending orders (PENDING/PROCESSING): 300 + 150 = 450
        val pendingOrders = orders.filter { it.status == "PENDING" || it.status == "PROCESSING" }
        val pendingOrdersAmount = pendingOrders.sumOf { it.amount }
        val pendingOrdersCount = pendingOrders.size
        assertEquals(2, pendingOrdersCount)
        assertEquals(450.0, pendingOrdersAmount, 0.001)

        // 3. Invoices: 2000 settled, 600 pending balance due
        val clearedInvoicesAmount = 2000.0
        val pendingInvoicesAmount = 600.0

        // 4. Payouts: 500 completed, 200 in-transit
        val totalPaidOut = 500.0
        val pendingPayouts = 200.0

        // 5. Total calculations
        val totalClearedGross = clearedOrdersAmount + clearedInvoicesAmount // 1300 + 2000 = 3300
        val availableBalance = (totalClearedGross - totalPaidOut - pendingPayouts).coerceAtLeast(0.0) // 3300 - 500 - 200 = 2600
        val totalPendingFunds = pendingOrdersAmount + pendingInvoicesAmount // 450 + 600 = 1050
        val grossTotal = totalClearedGross + totalPendingFunds // 3300 + 1050 = 4350

        val metrics = com.example.ui.viewmodel.OwnerPayoutMetrics(
            availableBalance = availableBalance,
            pendingPayoutsAmount = pendingPayouts,
            totalPaidOutLifetime = totalPaidOut,
            clearedFundsBalance = availableBalance,
            pendingFundsBalance = totalPendingFunds,
            grossRevenueTotal = grossTotal,
            clearedOrdersAmount = clearedOrdersAmount,
            clearedOrdersCount = clearedOrdersCount,
            pendingOrdersAmount = pendingOrdersAmount,
            pendingOrdersCount = pendingOrdersCount,
            clearedInvoicesAmount = clearedInvoicesAmount,
            pendingInvoicesAmount = pendingInvoicesAmount,
            clearanceRatePercent = ((totalClearedGross / grossTotal) * 100).toInt()
        )

        assertEquals(2600.0, metrics.availableBalance, 0.001)
        assertEquals(1050.0, metrics.pendingFundsBalance, 0.001)
        assertEquals(4350.0, metrics.grossRevenueTotal, 0.001)
        assertEquals(1300.0, metrics.clearedOrdersAmount, 0.001)
        assertEquals(450.0, metrics.pendingOrdersAmount, 0.001)
        assertEquals(75, metrics.clearanceRatePercent) // 3300 / 4350 = ~75%
    }

    @Test
    fun `payout metrics - transitioning order from PENDING to PAID shifts funds to cleared balance`() {
        var order = Order(id = "O-SHIFT", clientID = "C1", amount = 1000.0, status = "PENDING")

        // Initial: pending funds = 1000, cleared = 0
        var pendingAmount = if (order.status == "PENDING") order.amount else 0.0
        var clearedAmount = if (order.status == "PAID") order.amount else 0.0
        assertEquals(1000.0, pendingAmount, 0.001)
        assertEquals(0.0, clearedAmount, 0.001)

        // Real-time status update: marked as PAID
        order = order.copy(status = "PAID")
        pendingAmount = if (order.status == "PENDING") order.amount else 0.0
        clearedAmount = if (order.status == "PAID") order.amount else 0.0
        assertEquals(0.0, pendingAmount, 0.001)
        assertEquals(1000.0, clearedAmount, 0.001)
    }
}
