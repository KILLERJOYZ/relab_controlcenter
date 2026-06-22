package com.example.relab_tool.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * Manager class responsible for retrieving system-wide GPU utilization on Android devices
 * without requiring root access, utilizing a 3-tier fallback architecture.
 */
object GpuTelemetryManager {

    private const val TAG = "GpuTelemetryManager"

    /**
     * Represents the telemetry state of the GPU.
     */
    sealed class GpuStatus {
        /** Telemetry is initializing or loading. */
        object Loading : GpuStatus()

        /** Active reading with percentage (0-100). */
        data class Active(val usagePercent: Int) : GpuStatus()

        /** Reading restricted by SELinux / OS security policies. */
        data class Restricted(val reason: String) : GpuStatus()
    }

    // Curated list of known unprivileged sysfs paths across various SoC/OEM implementations
    private val TIER_1_PATHS = listOf(
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",  // Older Adreno/Qualcomm
        "/sys/class/kgsl/kgsl-3d0/gpu_busy_percent",     // Adreno variations
        "/sys/class/kgsl/kgsl-3d0/gpubusy",              // Returns "busy total" structure
        "/sys/class/devfreq/1c000000.gpu/utilization",   // Samsung Exynos
        "/sys/class/misc/mali0/device/utilization",      // ARM Mali
        "/sys/vendor/pvr/gpu_loading",                   // PowerVR
        "/sys/class/devfreq/gpufreq/gpu_utilization",    // MediaTek / Custom OEM
        "/sys/class/misc/mali0/device/utilisation",      // Mali alternate spelling
        "/sys/devices/platform/soc/1c00000.gpu/utilisation" // Alternate Mali platform path
    )

    /**
     * Polls the GPU utilization at the specified interval as an asynchronous flow.
     *
     * @param context Application context.
     * @param intervalMs Delay between polling cycles in milliseconds.
     * @return Flow emitting [GpuStatus] updates.
     */
    fun pollGpuUsage(context: Context, intervalMs: Long = 1000L): Flow<GpuStatus> = flow {
        emit(GpuStatus.Loading)
        while (true) {
            val status = getGpuUsageStatus(context)
            emit(status)
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Orchestrates the 3-Tier fallback resolution sequence.
     */
    private fun getGpuUsageStatus(context: Context): GpuStatus {
        // Tier 1: Public Vendor Sysfs Scraping
        val tier1Value = getGpuViaSysfs()
        if (tier1Value != null) {
            return GpuStatus.Active(tier1Value)
        }

        // Tier 2: Shizuku / ADB Shell Bridge Fallback
        val tier2Value = getGpuViaShizuku(context)
        if (tier2Value != null) {
            return GpuStatus.Active(tier2Value)
        }

        // Tier 3: Graceful UI State / Degradation
        return GpuStatus.Restricted(
            "Access to GPU usage metrics is restricted by SELinux security policy in this environment."
        )
    }

    /**
     * Tier 1: Scrapes unprivileged sysfs hardware paths.
     */
    private fun getGpuViaSysfs(): Int? {
        for (path in TIER_1_PATHS) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val content = file.readText().trim()
                    val parsed = parseRawGpuContent(content)
                    if (parsed != null) {
                        return parsed
                    }
                }
            } catch (e: SecurityException) {
                Log.d(TAG, "SELinux blocked direct access to path: $path")
            } catch (e: Exception) {
                Log.d(TAG, "Failed reading path $path: ${e.message}")
            }
        }
        return null
    }

    /**
     * Tier 2: Queries vendor-specific metrics via Shizuku shell bridge if granted.
     */
    private fun getGpuViaShizuku(context: Context): Int? {
        try {
            // Check if Shizuku runtime classes exist (keeps code compiling without hard dependencies)
            val shizukuClass = Class.forName("dev.rikka.shizuku.Shizuku")
            val getVersionMethod = shizukuClass.getMethod("getLatestServiceVersion")
            val isShizukuReady = (getVersionMethod.invoke(null) as? Int ?: 0) > 0
            
            if (isShizukuReady) {
                // Standard check for permission (Shizuku.checkSelfPermission())
                val checkPermMethod = shizukuClass.getMethod("checkSelfPermission")
                val permResult = checkPermMethod.invoke(null) as? Int ?: -1
                
                // 0 indicates permission granted (PackageManager.PERMISSION_GRANTED)
                if (permResult == 0) {
                    // Placeholder for actual privileged command invocation.
                    // This uses Shizuku's SystemServiceBinderWrapper to query system dumpsys or spawn processes.
                    // For example:
                    // val process = shizukuClass.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, File::class.java)
                    //     .invoke(null, arrayOf("cat", "/sys/class/kgsl/kgsl-3d0/gpu_busy_percent"), null, null) as Process
                    // val output = process.inputStream.bufferedReader().readText().trim()
                    // return parseRawGpuContent(output)
                    return null
                }
            }
        } catch (e: ClassNotFoundException) {
            // Shizuku is not integrated on classpath. Fall through.
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Shizuku status: ${e.message}")
        }
        return null
    }

    /**
     * General purpose robust parser for sysfs GPU node content.
     * Can parse singular integers ("45"), percentages ("45%"), and split ratio formats ("235 1000").
     */
    fun parseRawGpuContent(content: String): Int? {
        if (content.isEmpty()) return null
        
        try {
            // Split tokens by whitespace, slashes, or percentage signs
            val tokens = content.split(Regex("[\\s/%]+")).filter { it.isNotEmpty() }
            if (tokens.size >= 2) {
                val busy = tokens[0].toLongOrNull()
                val total = tokens[1].toLongOrNull()
                if (busy != null && total != null && total > 0) {
                    return (busy * 100 / total).toInt().coerceIn(0, 100)
                }
            } else if (tokens.isNotEmpty()) {
                val value = tokens[0].toIntOrNull()
                if (value != null) {
                    return value.coerceIn(0, 100)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GPU telemetry content: '$content'", e)
        }
        return null
    }
}
