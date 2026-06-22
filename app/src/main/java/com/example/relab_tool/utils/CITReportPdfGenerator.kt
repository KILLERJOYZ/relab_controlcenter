package com.example.relab_tool.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.relab_tool.ui.cit.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CITReportPdfGenerator {

    fun generateAndShareReport(context: Context, session: DiagnosticSession) {
        val pdfDocument = PdfDocument()
        val textPaint = Paint().apply { isAntiAlias = true }
        val paint = Paint()

        // --- Page 1: Cover Summary & Specs ---
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Draw top primary banner
        paint.color = Color.parseColor("#1A237E") // Deep Blue primary
        canvas1.drawRect(0f, 0f, 595f, 40f, paint)

        // Header Title
        textPaint.color = Color.WHITE
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas1.drawText("RELAB CONTROL CENTER - DIAGNOSTICS REPORT", 20f, 26f, textPaint)

        // Document Title and Date
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date(session.startedAt))

        textPaint.color = Color.BLACK
        textPaint.textSize = 22f
        textPaint.isFakeBoldText = true
        canvas1.drawText("Hardware Diagnostics Wizard", 20f, 80f, textPaint)

        textPaint.textSize = 10f
        textPaint.isFakeBoldText = false
        textPaint.color = Color.DKGRAY
        canvas1.drawText("Session Started: $dateStr", 20f, 100f, textPaint)

        // Horizontal Separator
        paint.color = Color.LTGRAY
        canvas1.drawLine(20f, 110f, 575f, 110f, paint)

        // Device Metadata Info Block
        textPaint.textSize = 12f
        textPaint.isFakeBoldText = true
        textPaint.color = Color.parseColor("#1A237E")
        canvas1.drawText("Device Specifications", 20f, 132f, textPaint)

        textPaint.color = Color.BLACK
        textPaint.isFakeBoldText = false
        textPaint.textSize = 10f

        val brand = session.deviceInfo.manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val model = session.deviceInfo.model
        val buildId = session.deviceInfo.buildId
        val release = session.deviceInfo.androidVersion

        var yOffset = 155f
        canvas1.drawText("• Manufacturer / Model: $brand $model", 25f, yOffset, textPaint)
        yOffset += 18f
        canvas1.drawText("• OS Version Release: Android $release (Build: $buildId)", 25f, yOffset, textPaint)
        yOffset += 18f
        canvas1.drawText("• CPU Architecture Info: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"}", 25f, yOffset, textPaint)

        // Metrics Summary Cards
        yOffset += 25f
        paint.color = Color.parseColor("#F5F5F5")
        canvas1.drawRoundRect(20f, yOffset, 575f, yOffset + 60f, 8f, 8f, paint)

        val allTests = session.categories.flatMap { it.tests }
        val totalTests = allTests.size
        val passed = allTests.count { it.status == TestStatus.PASS }
        val failed = allTests.count { it.status == TestStatus.FAIL }
        val skipped = allTests.count { it.status == TestStatus.SKIPPED }
        val na = allTests.count { it.status == TestStatus.NOT_APPLICABLE }
        val pending = allTests.count { it.status == TestStatus.PENDING }

        textPaint.textSize = 11f
        textPaint.isFakeBoldText = true
        textPaint.color = Color.BLACK
        canvas1.drawText("Session Metrics: ", 35f, yOffset + 35f, textPaint)
        
        textPaint.isFakeBoldText = false
        textPaint.color = Color.parseColor("#2E7D32")
        canvas1.drawText("Passed: $passed", 140f, yOffset + 35f, textPaint)
        
        textPaint.color = Color.parseColor("#C62828")
        canvas1.drawText("Failed: $failed", 220f, yOffset + 35f, textPaint)
        
        textPaint.color = Color.parseColor("#E65100")
        canvas1.drawText("Skipped: $skipped", 300f, yOffset + 35f, textPaint)

        textPaint.color = Color.GRAY
        canvas1.drawText("N/A: $na", 380f, yOffset + 35f, textPaint)

        textPaint.color = Color.BLACK
        canvas1.drawText("Pending: $pending", 450f, yOffset + 35f, textPaint)

        // Categories Summary Table
        yOffset += 85f
        textPaint.textSize = 12f
        textPaint.isFakeBoldText = true
        textPaint.color = Color.parseColor("#1A237E")
        canvas1.drawText("Categories Summary", 20f, yOffset, textPaint)

        yOffset += 15f
        paint.color = Color.parseColor("#E0E0E0")
        canvas1.drawRect(20f, yOffset, 575f, yOffset + 24f, paint)

        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        textPaint.color = Color.BLACK
        canvas1.drawText("Category Name", 30f, yOffset + 16f, textPaint)
        canvas1.drawText("Progress / Status", 440f, yOffset + 16f, textPaint)

        yOffset += 24f
        textPaint.isFakeBoldText = false

        session.categories.forEach { cat ->
            paint.color = Color.parseColor("#EEEEEE")
            canvas1.drawLine(20f, yOffset, 575f, yOffset, paint)

            val catDone = cat.tests.count { it.status != TestStatus.PENDING }
            val catTotal = cat.tests.size
            val catFail = cat.tests.count { it.status == TestStatus.FAIL }

            textPaint.color = Color.BLACK
            canvas1.drawText(cat.categoryName, 30f, yOffset + 18f, textPaint)

            if (catFail > 0) {
                textPaint.color = Color.parseColor("#C62828")
                canvas1.drawText("[ FAILING ]  $catDone / $catTotal Complete", 440f, yOffset + 18f, textPaint)
            } else if (catDone == catTotal) {
                textPaint.color = Color.parseColor("#2E7D32")
                canvas1.drawText("[ PASS ]  $catDone / $catTotal Complete", 440f, yOffset + 18f, textPaint)
            } else {
                textPaint.color = Color.GRAY
                canvas1.drawText("[ PENDING ]  $catDone / $catTotal Complete", 440f, yOffset + 18f, textPaint)
            }

            yOffset += 25f
        }

        // Draw Cover Page Footer
        paint.color = Color.parseColor("#EEEEEE")
        canvas1.drawRect(0f, 815f, 595f, 842f, paint)
        textPaint.textSize = 8f
        textPaint.color = Color.GRAY
        canvas1.drawText("Relab Diagnostics Suite. Formatted on-device.", 20f, 831f, textPaint)

        pdfDocument.finishPage(page1)

        // --- Page 2 & 3: Detailed wizard results (Paginated) ---
        var currentPageNumber = 2
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 50f

        fun startNewPage() {
            // Draw current page footer before finishing
            paint.color = Color.parseColor("#EEEEEE")
            canvas.drawRect(0f, 815f, 595f, 842f, paint)
            textPaint.textSize = 8f
            textPaint.color = Color.GRAY
            textPaint.isFakeBoldText = false
            canvas.drawText("Relab Diagnostics Suite. Formatted on-device.", 20f, 831f, textPaint)

            pdfDocument.finishPage(page)
            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            
            // Draw page banner header
            paint.color = Color.parseColor("#1A237E")
            canvas.drawRect(0f, 0f, 595f, 25f, paint)
            textPaint.color = Color.WHITE
            textPaint.textSize = 9f
            textPaint.isFakeBoldText = true
            canvas.drawText("RELAB HARDWARE DIAGNOSTICS - DETAIL SHEET", 20f, 16f, textPaint)
            currentY = 45f
        }

        session.categories.forEach { cat ->
            if (currentY > 700f) {
                startNewPage()
            }

            // Category Divider Section
            currentY += 15f
            paint.color = Color.parseColor("#E8EAF6")
            canvas.drawRect(20f, currentY, 575f, currentY + 22f, paint)
            
            textPaint.color = Color.parseColor("#1A237E")
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = true
            canvas.drawText("CATEGORY: ${cat.categoryName.uppercase(Locale.getDefault())}", 25f, currentY + 15f, textPaint)
            currentY += 22f

            cat.tests.forEach { test ->
                if (currentY > 760f) {
                    startNewPage()
                }

                paint.color = Color.parseColor("#EEEEEE")
                canvas.drawLine(20f, currentY, 575f, currentY, paint)

                textPaint.color = Color.BLACK
                textPaint.textSize = 9f
                textPaint.isFakeBoldText = false
                canvas.drawText(test.name, 30f, currentY + 16f, textPaint)

                val valStr = test.value ?: ""
                canvas.drawText(if (valStr.length > 25) valStr.take(22) + "..." else valStr, 280f, currentY + 16f, textPaint)

                // Status columns
                when (test.status) {
                    TestStatus.PASS -> {
                        textPaint.color = Color.parseColor("#2E7D32")
                        canvas.drawText("PASS", 490f, currentY + 16f, textPaint)
                    }
                    TestStatus.FAIL -> {
                        textPaint.color = Color.parseColor("#C62828")
                        canvas.drawText("FAIL", 490f, currentY + 16f, textPaint)
                    }
                    TestStatus.SKIPPED -> {
                        textPaint.color = Color.parseColor("#E65100")
                        canvas.drawText("SKIP", 490f, currentY + 16f, textPaint)
                    }
                    TestStatus.NOT_APPLICABLE -> {
                        textPaint.color = Color.GRAY
                        canvas.drawText("N/A", 490f, currentY + 16f, textPaint)
                    }
                    TestStatus.PENDING -> {
                        textPaint.color = Color.DKGRAY
                        canvas.drawText("PENDING", 490f, currentY + 16f, textPaint)
                    }
                }

                currentY += 22f
            }
        }

        // Draw last page footer before finish
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(0f, 815f, 595f, 842f, paint)
        textPaint.textSize = 8f
        textPaint.color = Color.GRAY
        textPaint.isFakeBoldText = false
        canvas.drawText("Relab Diagnostics Suite. Formatted on-device.", 20f, 831f, textPaint)

        pdfDocument.finishPage(page)

        // Save PDF to Documents
        val docDir = context.getExternalFilesDir("Documents")
        if (docDir != null && !docDir.exists()) {
            docDir.mkdirs()
        }
        val reportFile = File(docDir, "relab_diagnostic_report.pdf")

        try {
            val fos = FileOutputStream(reportFile)
            pdfDocument.writeTo(fos)
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        // Share via FileProvider
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "com.relab.controlcenter.fileprovider",
            reportFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relab Hardware Diagnostics Report")
            putExtra(Intent.EXTRA_TEXT, "Detailed hardware diagnostic wizard report generated on-device.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Diagnostics PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}
