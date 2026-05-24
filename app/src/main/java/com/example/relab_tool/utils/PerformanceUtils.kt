package com.example.relab_tool.utils

import android.app.ActivityManager
import android.content.Context
import java.io.File
import java.io.FileFilter
import java.util.regex.Pattern

object PerformanceUtils {
    private var isLowEndCache: Boolean? = null

    fun isLowEndDevice(context: Context): Boolean {
        isLowEndCache?.let { return it }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        
        // 1. Check if the OS flags it as a low RAM device (e.g., Android Go)
        if (activityManager?.isLowRamDevice == true) {
            isLowEndCache = true
            return true
        }

        // 2. Check total RAM memory
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        val totalMemoryGb = memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
        
        // Devices with <= 4.5 GB total RAM are considered low-end
        if (totalMemoryGb > 0 && totalMemoryGb <= 4.5) {
            isLowEndCache = true
            return true
        }

        // 3. Check CPU core count
        val cores = getNumberOfCpuCores()
        if (cores > 0 && cores <= 4) {
            isLowEndCache = true
            return true
        }

        isLowEndCache = false
        return false
    }

    private fun getNumberOfCpuCores(): Int {
        return try {
            val dir = File("/sys/devices/system/cpu/")
            val files = dir.listFiles(FileFilter { 
                Pattern.matches("cpu[0-9]+", it.name) 
            })
            files?.size ?: Runtime.getRuntime().availableProcessors()
        } catch (e: Exception) {
            Runtime.getRuntime().availableProcessors()
        }
    }
}
