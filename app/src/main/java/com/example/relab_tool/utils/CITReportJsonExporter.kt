package com.example.relab_tool.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.relab_tool.ui.cit.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object CITReportJsonExporter {

    fun exportAndShareJson(context: Context, session: DiagnosticSession) {
        val json = JSONObject()
        
        val devJson = JSONObject()
        devJson.put("model", session.deviceInfo.model)
        devJson.put("manufacturer", session.deviceInfo.manufacturer)
        devJson.put("androidVersion", session.deviceInfo.androidVersion)
        devJson.put("buildId", session.deviceInfo.buildId)
        json.put("deviceInfo", devJson)
        
        json.put("startedAt", session.startedAt)
        json.put("completedAt", session.completedAt ?: -1L)
        
        val catsArray = JSONArray()
        session.categories.forEach { cat ->
            val catJson = JSONObject()
            catJson.put("categoryId", cat.categoryId)
            catJson.put("categoryName", cat.categoryName)
            
            val testsArray = JSONArray()
            cat.tests.forEach { test ->
                val testJson = JSONObject()
                testJson.put("id", test.id)
                testJson.put("name", test.name)
                testJson.put("status", test.status.name)
                testJson.put("value", test.value ?: JSONObject.NULL)
                testJson.put("notes", test.notes ?: JSONObject.NULL)
                testJson.put("timestamp", test.timestamp)
                testsArray.put(testJson)
            }
            catJson.put("tests", testsArray)
            catsArray.put(catJson)
        }
        json.put("categories", catsArray)

        // Save JSON to Documents folder
        val docDir = context.getExternalFilesDir("Documents")
        if (docDir != null && !docDir.exists()) {
            docDir.mkdirs()
        }
        val reportFile = File(docDir, "relab_diagnostic_report.json")

        try {
            val fos = FileOutputStream(reportFile)
            fos.write(json.toString(4).toByteArray())
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Share via FileProvider
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "com.relab.controlcenter.fileprovider",
            reportFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Relab Hardware Diagnostics JSON Report")
            putExtra(Intent.EXTRA_TEXT, "Structured hardware diagnostic wizard session JSON data generated on-device.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share JSON Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}
