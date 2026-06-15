package com.example.relab_tool.data

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.util.Log
import com.example.relab_tool.data.PerformanceProfile

/**
 * Applies system-level performance changes when the HUD profile changes.
 *
 * Uses only APIs that do NOT require system/root permissions:
 *   - PerformanceHintManager (API 31+): CPU/GPU workload hints
 *   - Process.setThreadPriority: Adjusts our service thread priority
 *   - PowerManager partial WakeLock: Keeps CPU active in Gaming mode
 */
class PerformanceProfileManager(private val context: Context) {

    private var wakeLock: PowerManager.WakeLock? = null

    // PerformanceHintManager is available from API 31
    private var hintManager: Any? = null
    private var hintSession: Any? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hintManager = context.getSystemService("performance_hint")
        }
    }

    /**
     * Call this whenever the user taps a profile button in the overlay.
     * All operations are no-ops on unsupported API levels and catch exceptions silently.
     */
    fun apply(profile: PerformanceProfile) {
        TelemetryManager.setProfile(profile)
        applyThreadPriority(profile)
        applyWakeLock(profile)
        applyPerformanceHint(profile)
        Log.d("PerfProfile", "Applied profile: ${profile.label}")
    }

    // ── Thread priority ────────────────────────────────────────────────────────

    private fun applyThreadPriority(profile: PerformanceProfile) {
        try {
            val priority = when (profile) {
                PerformanceProfile.GAMING  -> Process.THREAD_PRIORITY_FOREGROUND      // -2
                PerformanceProfile.DEFAULT -> Process.THREAD_PRIORITY_DEFAULT          //  0
                PerformanceProfile.SAVER   -> Process.THREAD_PRIORITY_BACKGROUND       // 10
            }
            Process.setThreadPriority(priority)
        } catch (e: Exception) {
            Log.w("PerfProfile", "setThreadPriority failed: ${e.message}")
        }
    }

    // ── WakeLock ───────────────────────────────────────────────────────────────

    private fun applyWakeLock(profile: PerformanceProfile) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            // Release any existing lock first
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null

            if (profile == PerformanceProfile.GAMING) {
                // Partial wake lock keeps CPU running at full speed
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "RelabTool:GamingMode"
                ).also {
                    // Auto-released after 30 minutes to avoid battery drain if user forgets
                    it.acquire(30 * 60 * 1000L)
                }
            }
        } catch (e: Exception) {
            Log.w("PerfProfile", "WakeLock failed: ${e.message}")
        }
    }

    // ── PerformanceHintManager (API 31+) ────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun applyPerformanceHint(profile: PerformanceProfile) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            val manager = hintManager ?: return

            // Close existing session
            hintSession?.let { session ->
                try {
                    session.javaClass.getMethod("close").invoke(session)
                } catch (_: Exception) {}
            }
            hintSession = null

            if (profile == PerformanceProfile.SAVER) return // No hint needed for saver

            // Target duration in nanoseconds:
            // Gaming = 8ms (target 120 fps workload), Default = 16ms (60 fps)
            val targetNs = when (profile) {
                PerformanceProfile.GAMING  -> 8_000_000L   // 8 ms
                PerformanceProfile.DEFAULT -> 16_666_666L  // 16.67 ms
                PerformanceProfile.SAVER   -> 33_333_333L  // 33 ms
            }

            // Thread IDs for the hint session (current thread)
            val tids = intArrayOf(Process.myTid())

            // Reflectively call createHintSession(int[], long)
            val session = manager.javaClass
                .getMethod("createHintSession", IntArray::class.java, Long::class.javaPrimitiveType)
                .invoke(manager, tids, targetNs)
            hintSession = session
        } catch (e: Exception) {
            Log.w("PerfProfile", "PerformanceHint failed: ${e.message}")
        }
    }

    /** Must be called when the Service is destroyed to avoid resource leaks. */
    fun release() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
            hintSession?.let { session ->
                try {
                    session.javaClass.getMethod("close").invoke(session)
                } catch (_: Exception) {}
            }
            hintSession = null
        } catch (e: Exception) {
            Log.w("PerfProfile", "release failed: ${e.message}")
        }
    }
}
