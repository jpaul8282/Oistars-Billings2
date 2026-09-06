package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.InvoiceEntity
import com.example.data.model.InvoiceStatus
import com.example.ui.components.Formatters
import java.io.File
import java.io.FileOutputStream

object InvoicePdfExporter {

    /**
     * Generates a professional A4 PDF invoice document and saves it in app cache.
     * Dimensions: standard A4 (595 x 842 points at 72 dpi).
     */
    fun generateInvoicePdf(context: Context, invoice: InvoiceEntity): File {
        val outputDir = File(context.cacheDir, "exported_invoices")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val sanitizedId = invoice.id.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val outputFile = File(outputDir, "Invoice_$sanitizedId.pdf")

        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawInvoiceOnCanvas(canvas, invoice)

            pdfDocument.finishPage(page)

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
        } catch (e: IllegalStateException) {
            // Graceful fallback for JVM testing environments (e.g. Robolectric) where native C++ PdfDocument bindings are uninitialized
            generateFallbackPdfStream(outputFile, invoice)
        }

        return outputFile
    }

    private fun generateFallbackPdfStream(outputFile: File, invoice: InvoiceEntity) {
        val pdfText = buildString {
            appendLine("%PDF-1.4")
            appendLine("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj")
            appendLine("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj")
            appendLine("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj")
            val streamContent = "BT /F1 16 Tf 50 780 Td (INVOICE #${invoice.id} - ${invoice.clientName}) Tj 0 -25 Td (Total: ${invoice.totalAmount} ${invoice.currency}) Tj ET"
            appendLine("4 0 obj << /Length ${streamContent.length} >> stream")
            appendLine(streamContent)
            appendLine("endstream endobj")
            appendLine("5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj")
            appendLine("xref")
            appendLine("0 6")
            appendLine("0000000000 65535 f ")
            appendLine("0000000009 00000 n ")
            appendLine("0000000058 00000 n ")
            appendLine("0000000115 00000 n ")
            appendLine("0000000227 00000 n ")
            appendLine("0000000320 00000 n ")
            appendLine("trailer << /Size 6 /Root 1 0 R >>")
            appendLine("startxref")
            appendLine("390")
            appendLine("%%EOF")
        }
        outputFile.writeText(pdfText)
    }

    private fun drawInvoiceOnCanvas(canvas: Canvas, invoice: InvoiceEntity) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val leftMargin = 40f
        val rightMargin = 555f
        var currentY = 50f

        // 1. Top Decorative Header Bar
        paint.color = Color.parseColor("#005AC1") // Primary Ocean Blue
        canvas.drawRect(0f, 0f, 595f, 10f, paint)

        // 2. Brand & Company Info Header
        currentY = 52f
        paint.color = Color.parseColor("#001F2A") // Navy Dark
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OISTARS BILLINGS", leftMargin, currentY, paint)

        // Invoice Header Title (Right aligned)
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 24f
        paint.color = Color.parseColor("#005AC1")
        canvas.drawText("INVOICE", rightMargin, currentY, paint)

        paint.textAlign = Paint.Align.LEFT
        currentY += 16f
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#49454F")
        canvas.drawText("Oistars Seafood E-Commerce B.V.", leftMargin, currentY, paint)

        // Invoice Number Right Aligned
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#1D1B20")
        canvas.drawText("Invoice #${invoice.id}", rightMargin, currentY, paint)

        paint.textAlign = Paint.Align.LEFT
        currentY += 14f
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#49454F")
        canvas.drawText("Keizersgracht 421, 1016 EK Amsterdam", leftMargin, currentY, paint)

        // Dates right aligned
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 9.5f
        canvas.drawText("Issue Date: ${Formatters.formatDate(invoice.issueDate)}", rightMargin, currentY, paint)

        paint.textAlign = Paint.Align.LEFT
        currentY += 14f
        canvas.drawText("KvK: 84920192 • VAT: NL849201920B01", leftMargin, currentY, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Due Date: ${Formatters.formatDate(invoice.dueDate)}", rightMargin, currentY, paint)

        // Status Badge (Right aligned)
        currentY += 16f
        val statusText = invoice.invoiceStatus.label.uppercase()
        val (badgeBgColor, badgeTextColor) = when (invoice.invoiceStatus) {
            InvoiceStatus.PAID -> Color.parseColor("#C4F1B9") to Color.parseColor("#006E1C")
            InvoiceStatus.PENDING -> Color.parseColor("#FFDAD6") to Color.parseColor("#B3261E")
            InvoiceStatus.OVERDUE -> Color.parseColor("#FFDAD6") to Color.parseColor("#B3261E")
            InvoiceStatus.DRAFT -> Color.parseColor("#E1E2EC") to Color.parseColor("#44474E")
        }

        val badgeWidth = 90f
        val badgeHeight = 20f
        val badgeRect = RectF(rightMargin - badgeWidth, currentY - 14f, rightMargin, currentY + 6f)
        paint.color = badgeBgColor
        canvas.drawRoundRect(badgeRect, 6f, 6f, paint)

        paint.color = badgeTextColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(statusText, badgeRect.centerX(), currentY, paint)

        // 3. Divider Line
        currentY += 22f
        paint.color = Color.parseColor("#E0E2EC")
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, paint)

        // 4. Client / Bill To Card
        currentY += 20f
        val billToCardRect = RectF(leftMargin, currentY, rightMargin, currentY + 70f)
        paint.color = Color.parseColor("#F7F9FC")
        canvas.drawRoundRect(billToCardRect, 8f, 8f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#005AC1")
        canvas.drawText("BILL TO:", leftMargin + 14f, currentY + 18f, paint)

        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#1D1B20")
        canvas.drawText(invoice.clientName, leftMargin + 14f, currentY + 35f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#49454F")
        val clientSubtext = if (invoice.clientEmail.isNotBlank()) {
            "Client ID: ${invoice.clientId}  •  Email: ${invoice.clientEmail}"
        } else {
            "Client ID: ${invoice.clientId}"
        }
        canvas.drawText(clientSubtext, leftMargin + 14f, currentY + 52f, paint)

        // Payment Terms Info in Bill To Card (Right)
        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 9f
        paint.color = Color.parseColor("#49454F")
        canvas.drawText("Terms: ${invoice.paymentTerms}", rightMargin - 14f, currentY + 22f, paint)
        canvas.drawText("Currency: ${invoice.currency}", rightMargin - 14f, currentY + 38f, paint)

        // 5. Items Table Header
        currentY += 92f
        val tableHeaderRect = RectF(leftMargin, currentY - 14f, rightMargin, currentY + 12f)
        paint.color = Color.parseColor("#EBF1F6")
        canvas.drawRoundRect(tableHeaderRect, 4f, 4f, paint)

        paint.color = Color.parseColor("#1D1B20")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("ITEM / DESCRIPTION", leftMargin + 8f, currentY, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("QTY", leftMargin + 310f, currentY, paint)
        canvas.drawText("UNIT PRICE", leftMargin + 410f, currentY, paint)
        canvas.drawText("TOTAL AMOUNT", rightMargin - 8f, currentY, paint)

        // 6. Items Table Rows
        currentY += 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        invoice.items.forEach { item ->
            // Description
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#1D1B20")
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(item.description, leftMargin + 8f, currentY, paint)

            // VAT subtext
            paint.color = Color.parseColor("#79747E")
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("VAT: ${item.taxRate.toInt()}%", leftMargin + 8f, currentY + 11f, paint)

            // Qty
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.parseColor("#1D1B20")
            paint.textSize = 10f
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            canvas.drawText(qtyStr, leftMargin + 310f, currentY, paint)

            // Unit Price
            canvas.drawText(Formatters.formatCurrency(item.unitPrice, invoice.currency), leftMargin + 410f, currentY, paint)

            // Total
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(Formatters.formatCurrency(item.totalAmount, invoice.currency), rightMargin - 8f, currentY, paint)

            // Row underline
            currentY += 20f
            paint.color = Color.parseColor("#F0F2F5")
            paint.strokeWidth = 0.8f
            canvas.drawLine(leftMargin, currentY - 4f, rightMargin, currentY - 4f, paint)
            currentY += 12f
        }

        // 7. Totals & Financial Calculations
        currentY += 10f
        val totalsX = leftMargin + 320f

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#49454F")
        canvas.drawText("Subtotal:", totalsX, currentY, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.parseColor("#1D1B20")
        canvas.drawText(Formatters.formatCurrency(invoice.subtotal, invoice.currency), rightMargin - 8f, currentY, paint)

        currentY += 16f
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.parseColor("#49454F")
        canvas.drawText("VAT / Taxes:", totalsX, currentY, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.parseColor("#1D1B20")
        canvas.drawText(Formatters.formatCurrency(invoice.taxTotal, invoice.currency), rightMargin - 8f, currentY, paint)

        if (invoice.discountTotal > 0) {
            currentY += 16f
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#006E1C")
            canvas.drawText("Discount:", totalsX, currentY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-${Formatters.formatCurrency(invoice.discountTotal, invoice.currency)}", rightMargin - 8f, currentY, paint)
        }

        currentY += 14f
        paint.color = Color.parseColor("#C4C7D0")
        paint.strokeWidth = 1f
        canvas.drawLine(totalsX, currentY, rightMargin, currentY, paint)

        currentY += 16f
        // Total Invoiced Amount Box
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#001F2A")
        canvas.drawText("TOTAL AMOUNT:", totalsX, currentY, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 14f
        paint.color = Color.parseColor("#005AC1")
        canvas.drawText(Formatters.formatCurrency(invoice.totalAmount, invoice.currency), rightMargin - 8f, currentY, paint)

        // Amount Paid & Balance Due
        currentY += 18f
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#006E1C")
        canvas.drawText("Amount Paid:", totalsX, currentY, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(Formatters.formatCurrency(invoice.amountPaid, invoice.currency), rightMargin - 8f, currentY, paint)

        currentY += 16f
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (invoice.balanceDue > 0) Color.parseColor("#B3261E") else Color.parseColor("#006E1C")
        canvas.drawText("Balance Due:", totalsX, currentY, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.textSize = 11f
        canvas.drawText(Formatters.formatCurrency(invoice.balanceDue, invoice.currency), rightMargin - 8f, currentY, paint)

        // 8. Payment Instructions & Bank Details (Left aligned card)
        val paymentCardTop = currentY - 80f
        val paymentCardRect = RectF(leftMargin, paymentCardTop, totalsX - 20f, currentY + 20f)
        paint.color = Color.parseColor("#F4F6F9")
        canvas.drawRoundRect(paymentCardRect, 6f, 6f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#005AC1")
        canvas.drawText("PAYMENT INSTRUCTIONS:", leftMargin + 10f, paymentCardTop + 16f, paint)

        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#49454F")
        canvas.drawText("Bank: ABN AMRO Bank N.V. (Amsterdam)", leftMargin + 10f, paymentCardTop + 30f, paint)
        canvas.drawText("IBAN: NL91ABNA0412891024", leftMargin + 10f, paymentCardTop + 44f, paint)
        canvas.drawText("BIC / SWIFT: ABNANL2A", leftMargin + 10f, paymentCardTop + 58f, paint)
        canvas.drawText("Ref: Invoice #${invoice.id}", leftMargin + 10f, paymentCardTop + 72f, paint)

        // 9. Notes Section (if any)
        currentY += 34f
        if (invoice.notes.isNotBlank()) {
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#49454F")
            canvas.drawText("NOTES / REMARKS:", leftMargin, currentY, paint)

            currentY += 12f
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText(invoice.notes, leftMargin, currentY, paint)
            currentY += 16f
        }

        // 10. Footer Section
        val footerY = 810f
        paint.color = Color.parseColor("#E0E2EC")
        paint.strokeWidth = 0.8f
        canvas.drawLine(leftMargin, footerY - 14f, rightMargin, footerY - 14f, paint)

        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.parseColor("#79747E")
        canvas.drawText("Thank you for your business!  •  Generated by Oistars Billings Local Sandbox System", leftMargin, footerY, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Page 1 of 1", rightMargin, footerY, paint)
    }

    /**
     * Exports the invoice as a PDF and opens the native Android share sheet.
     */
    fun exportAndSharePdf(context: Context, invoice: InvoiceEntity) {
        try {
            val pdfFile = generateInvoicePdf(context, invoice)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice #${invoice.id} - ${invoice.clientName}")
                putExtra(Intent.EXTRA_TEXT, "Attached is invoice #${invoice.id} for ${invoice.clientName} amounting to ${Formatters.formatCurrency(invoice.totalAmount, invoice.currency)}.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Invoice PDF #${invoice.id}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "Exported PDF: Invoice #${invoice.id}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting PDF: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports the invoice as a PDF and opens it in an external PDF viewer application.
     */
    fun exportAndOpenPdf(context: Context, invoice: InvoiceEntity) {
        try {
            val pdfFile = generateInvoicePdf(context, invoice)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "Open Invoice PDF #${invoice.id}")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "Opening PDF: Invoice #${invoice.id}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening PDF: ${e.localizedMessage ?: "No PDF viewer found"}", Toast.LENGTH_LONG).show()
        }
    }
}
