package com.example.relab_tool.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.relab_tool.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportGenerator {

    fun generateHardwareReport(
        context: Context,
        summary: DeviceSummary?,
        system: SystemInfo?,
        battery: BatteryInfo?
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 40f
        
        // Title
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Relab Control Center - Hardware Report", 40f, y, paint)
        
        y += 30f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $date", 40f, y, paint)
        
        y += 40f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Device Summary", 40f, y, paint)
        
        y += 25f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        
        fun drawLine(label: String, value: String?) {
            canvas.drawText("$label: ${value ?: "N/A"}", 60f, y, paint)
            y += 20f
        }

        summary?.let {
            drawLine("Model", it.model)
            drawLine("Manufacturer", it.manufacturer)
            drawLine("Platform", it.platform)
            drawLine("Android ID", it.androidId)
        }

        y += 20f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("Battery Status", 40f, y, paint)
        
        y += 25f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        
        battery?.let {
            drawLine("Health", it.health)
            drawLine("Level", "${it.level}%")
            drawLine("Capacity", it.capacity)
            drawLine("Technology", it.technology)
        }

        document.finishPage(page)

        val fileName = "Relab_Report_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        return try {
            document.writeTo(FileOutputStream(file))
            document.close()
            file
        } catch (e: Exception) {
            document.close()
            null
        }
    }
}
