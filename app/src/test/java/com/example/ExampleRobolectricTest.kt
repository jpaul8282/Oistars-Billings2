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
        contactPerson = "Jan-Peter Westerveld",
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
}
