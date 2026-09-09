package com.example.relab_tool.benchmark.scoring

import android.content.Context
import com.example.relab_tool.benchmark.domain.model.DeviceReferenceEntry
import org.json.JSONArray
import java.io.InputStreamReader

class PercentileCalculator(private val context: Context) {
    val referenceDevices: List<DeviceReferenceEntry> by lazy {
        loadReferenceDevices()
    }

    private fun loadReferenceDevices(): List<DeviceReferenceEntry> {
        val list = mutableListOf<DeviceReferenceEntry>()
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("benchmark/device_scores.json")
            val reader = InputStreamReader(inputStream)
            val jsonString = reader.readText()
            reader.close()
            
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DeviceReferenceEntry(
                        rank = obj.getInt("rank"),
                        deviceName = obj.getString("deviceName"),
                        soc = obj.getString("soc"),
                        score = obj.getInt("score")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            list.addAll(getFallbackDevices())
        }
        return list.sortedBy { it.score }
    }

    fun calculatePercentile(score: Int): Float {
        if (referenceDevices.isEmpty()) return 50.0f
        val countLower = referenceDevices.count { it.score < score }
        return (countLower.toFloat() / referenceDevices.size.toFloat() * 100f).coerceIn(0f, 100f)
    }

    fun getNearestReferenceDevice(score: Int): DeviceReferenceEntry {
        if (referenceDevices.isEmpty()) {
            return DeviceReferenceEntry(100, "Reference Device", "Generic SoC", score)
        }
        return referenceDevices.minByOrNull { Math.abs(it.score - score) } ?: referenceDevices.first()
    }

    private fun getFallbackDevices(): List<DeviceReferenceEntry> {
        return listOf(
            DeviceReferenceEntry(1, "Samsung Galaxy S25 Ultra", "Snapdragon 8 Elite", 952000),
            DeviceReferenceEntry(10, "OnePlus 13", "Snapdragon 8 Elite", 945000),
            DeviceReferenceEntry(25, "Samsung Galaxy S24 Ultra", "Snapdragon 8 Gen 3", 820000),
            DeviceReferenceEntry(50, "Google Pixel 9 Pro XL", "Tensor G4", 785000),
            DeviceReferenceEntry(75, "Samsung Galaxy A55", "Exynos 1480", 445000),
            DeviceReferenceEntry(120, "Samsung Galaxy A15", "Dimensity 6100+", 258000),
            DeviceReferenceEntry(180, "Redmi 13C", "Helio G85", 148000)
        )
    }
}
