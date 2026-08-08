package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.FinancialRecordEntity
import com.example.data.model.UserEntity
import java.io.File
import java.io.FileOutputStream

object PdfReceiptGenerator {

    /**
     * Generates a PDF receipt for a processed dues/payment transaction using Android's native PdfDocument framework.
     */
    fun generateAndSavePdfReceipt(
        context: Context,
        record: FinancialRecordEntity,
        user: UserEntity
    ): File? {
        val pdfDocument = PdfDocument()

        // Page info: A4 size (595 x 842 points)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.rgb(27, 94, 32) // Forest Green
            textSize = 20f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isFakeBoldText = true
        }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        paint.color = Color.BLACK
        paint.textSize = 11f

        var y = 50f

        // Header Title
        canvas.drawText("KUNJACHAYA CLUB", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("OFFICIAL PAYMENT MONEY RECEIPT • ELECTRONIC STATEMENT", 40f, y, subtitlePaint)
        y += 12f

        // Divider
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 24f

        fun drawRow(label: String, value: String) {
            canvas.drawText(label, 40f, y, boldPaint)
            canvas.drawText(value, 200f, y, paint)
            y += 20f
        }

        drawRow("Receipt / TxID:", record.transactionId.ifBlank { "TXN-${record.id}" })
        drawRow("Member Name:", user.nameEn)
        drawRow("Member ID:", user.id)
        drawRow("Flat / Unit Holding:", user.holding)
        drawRow("Contact Phone:", user.primaryContact)
        drawRow("Payment Purpose:", record.titleEn)
        drawRow("Billing Month/Year:", record.monthYear)
        drawRow("Payment Gateway:", record.paymentGateway)
        drawRow("Transaction Date:", record.date)
        drawRow("Payment Status:", record.status.uppercase())

        y += 8f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 24f

        // Total Paid Box
        val boxPaint = Paint().apply {
            color = Color.rgb(232, 245, 233) // Light Green
        }
        canvas.drawRect(40f, y - 16f, 555f, y + 24f, boxPaint)

        val totalLabelPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
        }
        canvas.drawText("TOTAL PAID AMOUNT:", 55f, y + 8f, totalLabelPaint)

        val totalAmountPaint = Paint().apply {
            color = Color.rgb(46, 125, 50)
            textSize = 16f
            isFakeBoldText = true
        }
        canvas.drawText("TK ${record.amount.toInt()} BDT", 380f, y + 8f, totalAmountPaint)

        y += 50f

        // Digital Stamp & Verification
        val stampPaint = Paint().apply {
            color = Color.rgb(27, 94, 32)
            textSize = 10f
            isFakeBoldText = true
        }
        canvas.drawText("✓ VERIFIED DIGITAL STAMP - KUNJACHAYA CLUB ACCOUNTS OFFICE", 40f, y, stampPaint)
        y += 16f

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
        }
        canvas.drawText("This receipt is automatically generated upon dues payment confirmation. Valid without manual signature.", 40f, y, footerPaint)

        pdfDocument.finishPage(page)

        // Save PDF file to External Files Dir
        val safeTxnId = record.transactionId.replace("[^a-zA-Z0-9]".toRegex(), "_").ifBlank { "TXN_${record.id}" }
        val filename = "Receipt_$safeTxnId.pdf"
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val file = File(downloadsDir, filename)

        return try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    /**
     * Attempts to open the generated PDF file using a PDF viewer application, or notifies user via Toast.
     */
    fun downloadAndOpenPdf(context: Context, record: FinancialRecordEntity, user: UserEntity) {
        val file = generateAndSavePdfReceipt(context, record, user)
        if (file != null && file.exists()) {
            Toast.makeText(context, "PDF Receipt downloaded to: ${file.name}", Toast.LENGTH_LONG).show()
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // If no PDF app installed, toast location
            }
        } else {
            Toast.makeText(context, "Failed to generate PDF receipt.", Toast.LENGTH_SHORT).show()
        }
    }
}
