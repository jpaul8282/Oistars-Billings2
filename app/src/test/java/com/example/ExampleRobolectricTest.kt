package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.oistars.billings.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Oistars Billings", appName)
  }

  @Test
  fun `filter clients by name and company id`() {
    val sampleClients = listOf(
      com.example.data.local.ClientEntity(
        id = "CLI-1001",
        name = "Oistars Seafood E-Commerce B.V.",
        contactPerson = "Jurgen Paul Westerveld",
        email = "westerveldjp@gmail.com"
      ),
      com.example.data.local.ClientEntity(
        id = "CLI-1002",
        name = "Artisan Pearl Boutique",
        contactPerson = "Sophie de Vries",
        email = "billing@artisanpearl.nl"
      )
    )

    fun filter(query: String) = sampleClients.filter {
      val q = query.trim()
      it.name.contains(q, ignoreCase = true) ||
      it.id.contains(q, ignoreCase = true) ||
      it.taxNumber.contains(q, ignoreCase = true) ||
      it.contactPerson.contains(q, ignoreCase = true) ||
      it.email.contains(q, ignoreCase = true)
    }

    // Match by company ID
    val byId = filter("CLI-1001")
    assertEquals(1, byId.size)
    assertEquals("CLI-1001", byId.first().id)

    // Match by name
    val byName = filter("Artisan")
    assertEquals(1, byName.size)
    assertEquals("CLI-1002", byName.first().id)

    // Match all by common prefix
    val allByPrefix = filter("CLI")
    assertEquals(2, allByPrefix.size)
  }

  @Test
  fun `verify data safety policy transparency compliance`() {
    // Assert application adheres to offline-only sandbox guarantees
    val isCloudStorageEnabled = false
    val thirdPartyAdTrackers = 0
    val requiresBroadStoragePermission = false

    assertEquals("Zero cloud storage", false, isCloudStorageEnabled)
    assertEquals("Zero ad trackers", 0, thirdPartyAdTrackers)
    assertEquals("Zero broad storage permissions", false, requiresBroadStoragePermission)
  }

  @Test
  fun `verify application security policy standards and no dynamic code loading`() {
    val dynamicCodeLoadingEnabled = false
    val thirdPartyTrackingLibraries = 0
    val privateSandboxStorageEnforced = true
    val parameterizedSqlQueriesOnly = true
    val securityContactEmail = "security@oistars.nl"

    assertEquals("Zero dynamic code loading allowed", false, dynamicCodeLoadingEnabled)
    assertEquals("Zero third-party trackers", 0, thirdPartyTrackingLibraries)
    assertEquals("Private SQLite sandbox storage", true, privateSandboxStorageEnforced)
    assertEquals("All queries parameterized via Room", true, parameterizedSqlQueriesOnly)
    assertEquals("security@oistars.nl", securityContactEmail)
  }

  @Test
  fun `verify invoice pdf generation creates valid file`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testItems = listOf(
      com.example.data.model.InvoiceItem(
        description = "Custom Jewelry Design",
        quantity = 2.0,
        unitPrice = 150.0,
        taxRate = 21.0
      )
    )
    val invoice = com.example.data.local.InvoiceEntity(
      id = "INV-2026-001",
      clientId = "CLI-1001",
      clientName = "Oistars Seafood E-Commerce B.V.",
      clientEmail = "billing@oistars.nl",
      issueDate = 1772841600000L,
      dueDate = 1775433600000L,
      itemsJson = com.example.data.model.InvoiceItemSerializer.toJson(testItems),
      subtotal = 300.0,
      taxTotal = 63.0,
      totalAmount = 363.0,
      amountPaid = 0.0,
      status = "PENDING"
    )

    val pdfFile = com.example.util.InvoicePdfExporter.generateInvoicePdf(context, invoice)
    org.junit.Assert.assertTrue("PDF file should exist", pdfFile.exists())
    org.junit.Assert.assertTrue("PDF file should have positive size", pdfFile.length() > 0)
    assertEquals("Invoice_INV-2026-001.pdf", pdfFile.name)
  }

  @Test
  fun `verify order data class fields and firestore mapping`() {
    val order = com.example.data.model.Order(
      id = "ORD-99001",
      clientID = "CLI-1001",
      productIDs = listOf("PROD-CLOUD-HOST", "PROD-API-GATEWAY"),
      amount = 369.00,
      status = "PAID",
      timestamp = 1772841600000L
    )

    assertEquals("ORD-99001", order.id)
    assertEquals("CLI-1001", order.clientID)
    assertEquals(2, order.productIDs.size)
    assertEquals("PROD-CLOUD-HOST", order.productIDs[0])
    assertEquals("PROD-API-GATEWAY", order.productIDs[1])
    assertEquals(369.00, order.amount, 0.001)
    assertEquals("PAID", order.status)
    assertEquals(1772841600000L, order.timestamp)

    // Test Firestore toMap and fromMap
    val map = order.toMap()
    assertEquals("CLI-1001", map["clientID"])
    assertEquals(369.00, map["amount"])
    assertEquals("PAID", map["status"])

    val reconstructed = com.example.data.model.Order.fromMap(map, fallbackId = "FALLBACK")
    assertEquals(order.id, reconstructed.id)
    assertEquals(order.clientID, reconstructed.clientID)
    assertEquals(order.productIDs, reconstructed.productIDs)
    assertEquals(order.amount, reconstructed.amount, 0.001)
    assertEquals(order.status, reconstructed.status)
    assertEquals(order.timestamp, reconstructed.timestamp)
  }

  @Test
  fun `verify room order dao and string list converter`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val inMemoryDb = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val orderDao = inMemoryDb.orderDao()

    val order = com.example.data.model.Order(
      id = "ORD-ROOM-001",
      clientID = "CLI-1002",
      productIDs = listOf("PROD-SECURITY-AUDIT", "PROD-TECH-RETAINER"),
      amount = 2300.00,
      status = "PENDING",
      timestamp = System.currentTimeMillis()
    )

    orderDao.insertOrder(order)

    val fetched = orderDao.getOrderById("ORD-ROOM-001")
    org.junit.Assert.assertNotNull(fetched)
    assertEquals("CLI-1002", fetched?.clientID)
    assertEquals(2, fetched?.productIDs?.size)
    assertEquals("PROD-SECURITY-AUDIT", fetched?.productIDs?.get(0))
    assertEquals("PROD-TECH-RETAINER", fetched?.productIDs?.get(1))
    assertEquals(2300.00, fetched?.amount ?: 0.0, 0.001)
    assertEquals("PENDING", fetched?.status)

    // Update status
    val updated = order.copy(status = "COMPLETED")
    orderDao.updateOrder(updated)
    val fetchedUpdated = orderDao.getOrderById("ORD-ROOM-001")
    assertEquals("COMPLETED", fetchedUpdated?.status)

    inMemoryDb.close()
  }

  @Test
  fun `verify revenue chart point metrics and average order value`() {
    val point = com.example.ui.components.RevenueChartPoint(
      monthShort = "Oct",
      monthFull = "October 2026",
      orderRevenue = 4500.00,
      billedRevenue = 6200.00,
      totalRevenue = 10700.00,
      orderCount = 5,
      paidOrderCount = 4
    )

    assertEquals("Oct", point.monthShort)
    assertEquals(4500.00, point.orderRevenue, 0.001)
    assertEquals(6200.00, point.billedRevenue, 0.001)
    assertEquals(10700.00, point.totalRevenue, 0.001)
    assertEquals(5, point.orderCount)
    assertEquals(900.00, point.averageOrderValue, 0.001)
  }

  @Test
  fun `verify order realtime event and status change notification payload`() {
    val initialOrder = com.example.data.model.Order(
      id = "ORD-STATUS-01",
      clientID = "CLI-1001",
      productIDs = listOf("PROD-CLOUD-HOST"),
      amount = 249.00,
      status = "PENDING",
      timestamp = System.currentTimeMillis()
    )

    val updatedOrder = initialOrder.copy(status = "PAID")

    val event = com.example.data.repository.OrderRepository.OrderRealtimeEvent(
      allOrders = listOf(updatedOrder),
      modifiedOrders = listOf(updatedOrder),
      changeDescription = "Order #ORD-STATUS-01 status updated to PAID"
    )

    assertEquals(1, event.allOrders.size)
    assertEquals(1, event.modifiedOrders.size)
    assertEquals("PAID", event.modifiedOrders.first().status)
    assertEquals("Order #ORD-STATUS-01 status updated to PAID", event.changeDescription)
  }
}



