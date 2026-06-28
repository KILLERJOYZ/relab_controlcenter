package com.example.relab_tool.benchmark.util

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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
    data class PinnedDispatcherHandle(
        val dispatcher: CoroutineDispatcher,
        val targetCore: Int,
        val wasPinned: AtomicBoolean
    ) : AutoCloseable {
        override fun close() {
            (dispatcher as? ExecutorCoroutineDispatcher)?.close()
        }
    }

    fun createPinnedDispatcher(targetCore: Int): PinnedDispatcherHandle {
        val wasPinned = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread {
                if (hasNativeLibrary) {
                    val pinned = setThreadAffinity(targetCore)
                    wasPinned.set(pinned)
                    Log.d(TAG, "Thread successfully pinned to core $targetCore: $pinned")
                }
                runnable.run()
            }
        }
        return PinnedDispatcherHandle(executor.asCoroutineDispatcher(), targetCore, wasPinned)
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

    /**
     * Dynamically detects the prime core (highest performance core).
     * Tries to read cpuinfo_max_freq first. If blocked by SELinux, falls back to dynamic profiling.
     */
    fun getPrimeCoreIndex(): Int {
        val cores = getHardwareCoreCount()
        var primeCore = cores - 1 // Default to last core
        var maxFreq = 0L

        try {
            val cpuDir = File("/sys/devices/system/cpu/")
            val cpuFiles = cpuDir.listFiles { _, name -> name.matches(Regex("cpu[0-9]+")) } ?: emptyArray()
            for (file in cpuFiles) {
                val coreIndexStr = file.name.replace("cpu", "")
                val coreIndex = coreIndexStr.toIntOrNull() ?: continue
                val f = File(file, "cpufreq/cpuinfo_max_freq")
                if (f.exists() && f.canRead()) {
                    val freq = f.readText().trim().toLongOrNull() ?: 0L
                    if (freq > maxFreq) {
                        maxFreq = freq
                        primeCore = coreIndex
                    } else if (freq == maxFreq && coreIndex > primeCore) {
                        primeCore = coreIndex // Prefer higher index for identical max frequency
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SELinux policy blocked cpuinfo_max_freq access", e)
        }

        // Only fallback to profiling if maxFreq wasn't found and we have native library
        if (maxFreq == 0L && hasNativeLibrary) {
            Log.d(TAG, "Falling back to dynamic core profiling")
            var bestCore = primeCore
            var bestScore = 0L
            
            for (i in 0 until cores) {
                var score = 0L
                var tempAcc = 0L
                val t = Thread {
                    if (setThreadAffinity(i)) {
                        val start = System.nanoTime()
                        var acc = 0L
                        for (j in 0 until 5_000_000) {
                            acc += j xor (j shl 1)
                        }
                        tempAcc = acc
                        val elapsed = System.nanoTime() - start
                        if (elapsed > 0) {
                            score = 5_000_000L * 1000L / elapsed
                        }
                    }
                }
                t.start()
                t.join()
                
                if (score > bestScore) {
                    bestScore = score
                    bestCore = i
                }
                Log.d(TAG, "Core $i profiling score: $score (acc=$tempAcc)")
            }
            if (bestScore > 0) {
                primeCore = bestCore
            }
        }

        Log.d(TAG, "Detected prime core index: $primeCore")
        return primeCore
    }
}
