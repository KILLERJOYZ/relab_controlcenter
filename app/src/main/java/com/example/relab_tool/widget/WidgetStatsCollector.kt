package com.example.relab_tool.widget

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.example.relab_tool.utils.GpuUtils
import java.io.File
import java.util.Locale

object WidgetStatsCollector {
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastTime = 0L

    data class Stats(
        val cpuUsage: Int,
        val gpuUsage: Int,
        val ramPercent: Int,
        val ramText: String,
        val batteryPercent: Int,
        val batteryText: String,
        val batteryTempText: String,
        val storagePercent: Int,
        val storageText: String,
        val dlSpeedText: String,
        val ulSpeedText: String,
        val uptimeText: String,
        val wifiSsid: String
    )

    fun collect(context: Context): Stats {
        // CPU
        val cpuCount = Runtime.getRuntime().availableProcessors()
        var totalNormalized = 0f
        for (i in 0 until cpuCount) {
            val freq = try {
                File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq").readText().trim().toLong() / 1000
            } catch (e: Exception) { 0L }
            
            val max = try {
                File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq").readText().trim().toLong()
            } catch (e: Exception) { 3000000L }
            
            val normalized = if (max > 0) ((freq * 1000).toFloat() / max.toFloat() * 100f).coerceIn(0f, 100f) else 0f
            totalNormalized += normalized
        }
        val cpuUsage = if (cpuCount > 0) (totalNormalized / cpuCount).toInt() else 0

        // GPU
        val gpuUsage = try { GpuUtils.getGpuUsage() } catch (e: Exception) { 0 }

        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalMem = memoryInfo.totalMem
        val availMem = memoryInfo.availMem
        val ramUsedBytes = totalMem - availMem
        val ramPercent = if (totalMem > 0) (ramUsedBytes * 100 / totalMem).toInt() else 0
        
        val ramTotalGb = totalMem / (1024.0 * 1024 * 1024)
        val ramUsedGb = ramUsedBytes / (1024.0 * 1024 * 1024)
        val ramText = "%.1f / %.1f GB".format(Locale.US, ramUsedGb, ramTotalGb)

        // Storage
        val internalStat = StatFs(Environment.getDataDirectory().path)
        val storageTotalBytes = internalStat.totalBytes
        val storageUsedBytes = storageTotalBytes - internalStat.availableBytes
        val storagePercent = if (storageTotalBytes > 0) (storageUsedBytes * 100 / storageTotalBytes).toInt() else 0
        
        val storageTotalGb = storageTotalBytes / (1024.0 * 1024 * 1024)
        val storageUsedGb = storageUsedBytes / (1024.0 * 1024 * 1024)
        val storageText = "%.1f / %.1f GB".format(Locale.US, storageUsedGb, storageTotalGb)

        // Battery
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val batteryText = "$batteryPercent%" + (if (isCharging) " ⚡" else "")
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val batteryTemp = tempRaw / 10.0
        val batteryTempText = "%.1f°C".format(Locale.US, batteryTemp)

        // Network Speed
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()
        var dlSpeed = 0L
        var ulSpeed = 0L
        if (lastTime > 0) {
            val timeDiff = (currentTime - lastTime) / 1000.0
            if (timeDiff > 0) {
                dlSpeed = ((currentRx - lastRxBytes) / timeDiff).toLong().coerceAtLeast(0L)
                ulSpeed = ((currentTx - lastTxBytes) / timeDiff).toLong().coerceAtLeast(0L)
            }
        }
        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastTime = currentTime

        // Uptime
        val elapsedMs = SystemClock.elapsedRealtime()
        val hours = (elapsedMs / (1000 * 60 * 60)).toInt()
        val minutes = ((elapsedMs / (1000 * 60)) % 60).toInt()
        val uptimeText = "Uptime: ${hours}h ${minutes}m"

        // Wifi SSID
        val wifiSsid = getWifiSsid(context)

        return Stats(
            cpuUsage = cpuUsage,
            gpuUsage = gpuUsage,
            ramPercent = ramPercent,
            ramText = ramText,
            batteryPercent = batteryPercent,
            batteryText = batteryText,
            batteryTempText = batteryTempText,
            storagePercent = storagePercent,
            storageText = storageText,
            dlSpeedText = formatSpeed(dlSpeed),
            ulSpeedText = formatSpeed(ulSpeed),
            uptimeText = uptimeText,
            wifiSsid = wifiSsid
        )
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> "%.1f MB/s".format(Locale.US, bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> "%.1f KB/s".format(Locale.US, bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun getWifiSsid(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiInfo = wifiManager?.connectionInfo
                val ssid = wifiInfo?.ssid
                if (ssid != null && ssid != "<unknown ssid>") {
                    return "Wi-Fi: " + ssid.replace("\"", "")
                }
                return "Wi-Fi: Đã kết nối"
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Mạng di động"
            }
        }
        return "Không có kết nối"
    }
}
