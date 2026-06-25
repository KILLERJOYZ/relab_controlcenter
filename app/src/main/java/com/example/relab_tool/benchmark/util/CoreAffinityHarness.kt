package com.example.relab_tool.benchmark.util

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors

object CoreAffinityHarness {
    private const val TAG = "CoreAffinityHarness"
    private var hasNativeLibrary = false

    init {
        try {
            System.loadLibrary("rlcc_bench")
            hasNativeLibrary = true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load rlcc_bench native library")
        }
    }

    /**
     * Resolves the CPU Core Hopping bug on single-core benchmarks.
     * Pins execution to the highest-performing core index (the super-core).
     */
    fun createPinnedDispatcher(targetCore: Int): CoroutineDispatcher {
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread {
                if (hasNativeLibrary) {
                    val pinned = setThreadAffinity(targetCore)
                    Log.d(TAG, "Thread successfully pinned to core $targetCore: $pinned")
                }
                runnable.run()
            }
        }
        return executor.asCoroutineDispatcher()
    }

    /**
     * Detects the total hardware core count, bypassing SELinux policy issues
     * with /sys/devices/system/cpu/possible.
     */
    fun getHardwareCoreCount(): Int {
        try {
            val possibleFile = File("/sys/devices/system/cpu/possible")
            if (possibleFile.exists() && possibleFile.canRead()) {
                val ranges = possibleFile.readText().trim().split(",")
                var count = 0
                for (range in ranges) {
                    val bounds = range.split("-")
                    count += if (bounds.size == 2) {
                        val start = bounds[0].trim().toIntOrNull() ?: 0
                        val end = bounds[1].trim().toIntOrNull() ?: 0
                        end - start + 1
                    } else {
                        1
                    }
                }
                if (count > 0) return count
            }
        } catch (e: Exception) {
            Log.w(TAG, "SELinux policy blocked /sys/devices/system/cpu/possible access")
        }
        
        // Fallback: Query system configurations via native JNI POSIX call
        if (hasNativeLibrary) {
            try {
                val nativeCores = getNativeCoreCount()
                if (nativeCores > 0) return nativeCores
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query native core count", e)
            }
        }

        return Runtime.getRuntime().availableProcessors()
    }

    // JNI Native Methods
    private external fun setThreadAffinity(coreIndex: Int): Boolean
    private external fun getNativeCoreCount(): Int
}
