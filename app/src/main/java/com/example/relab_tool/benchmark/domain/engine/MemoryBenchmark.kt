package com.example.relab_tool.benchmark.domain.engine

import android.app.ActivityManager
import android.content.Context
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random

class MemoryBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.MEMORY

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val isLowMem = memInfo.availMem < 200 * 1024 * 1024
        
        val size = if (isLowMem) 16 * 1024 * 1024 else 128 * 1024 * 1024
        
        // 4a. Sequential Write BW
        onProgress(0.0f)
        val writeScoreResult = runSeqWriteBw(size, isLowMem)
        list.add(SubScore("Sequential Write BW", writeScoreResult.bw, "GB/s", ScoreNormalizer.normalize(writeScoreResult.bw, 18.0, 72.0, false), writeScoreResult.isPartial))
        
        // 4b. Sequential Read BW
        onProgress(0.2f)
        val readScoreResult = runSeqReadBw(size, isLowMem)
        list.add(SubScore("Sequential Read BW", readScoreResult.bw, "GB/s", ScoreNormalizer.normalize(readScoreResult.bw, 24.0, 96.0, false), readScoreResult.isPartial))
        
        // 4c. Random Read Latency
        onProgress(0.4f)
        val latencyScoreResult = runRandomReadLatency()
        list.add(SubScore("Random Read Latency", latencyScoreResult.latency, "ns", ScoreNormalizer.normalize(latencyScoreResult.latency, 85.0, 16.0, true), latencyScoreResult.isPartial))
        
        // 4d. Allocation Pressure
        onProgress(0.6f)
        val allocScoreResult = runAllocationPressure()
        list.add(SubScore("Allocation Pressure", allocScoreResult.allocsPerSec, "allocs/s", ScoreNormalizer.normalize(allocScoreResult.allocsPerSec, 85.0, 340.0, false), allocScoreResult.isPartial))
        
        // 4e. Cache Probe
        onProgress(0.8f)
        val cacheProbeResult = runCacheProbe()
        list.add(SubScore("DRAM Latency", cacheProbeResult.latency, "ns", ScoreNormalizer.normalize(cacheProbeResult.latency, 90.0, 18.0, true), cacheProbeResult.isPartial))
        
        onProgress(1.0f)
        list
    }

    data class BwResult(val bw: Double, val isPartial: Boolean)
    data class LatencyResult(val latency: Double, val isPartial: Boolean)
    data class AllocResult(val allocsPerSec: Double, val isPartial: Boolean)

    private fun runSeqWriteBw(size: Int, isLowMem: Boolean): BwResult {
        return try {
            val arr = ByteArray(size)
            val passes = if (isLowMem) 4 else 8
            val startTime = System.nanoTime()
            for (p in 0 until passes) {
                arr.fill(0xFF.toByte())
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * passes
            val gbPerSec = totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
            BwResult(gbPerSec, isLowMem)
        } catch (e: OutOfMemoryError) {
            BwResult(0.0, true)
        }
    }

    private fun runSeqReadBw(size: Int, isLowMem: Boolean): BwResult {
        return try {
            val arr = ByteArray(size) { 0x55.toByte() }
            val passes = if (isLowMem) 4 else 8
            val startTime = System.nanoTime()
            var sum = 0L
            for (p in 0 until passes) {
                for (i in arr.indices) {
                    sum += arr[i].toLong()
                }
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * passes
            val gbPerSec = totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
            if (sum == 0L) {
                BwResult(gbPerSec + 0.0001, isLowMem)
            } else {
                BwResult(gbPerSec, isLowMem)
            }
        } catch (e: OutOfMemoryError) {
            BwResult(0.0, true)
        }
    }

    private fun runRandomReadLatency(): LatencyResult {
        return try {
            val size = 2 * 1024 * 1024
            val array = IntArray(size)
            val indices = IntArray(size) { it }
            val random = Random(42)
            for (i in size - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val temp = indices[i]
                indices[i] = indices[j]
                indices[j] = temp
            }
            for (i in 0 until size) {
                array[indices[i]] = indices[(i + 1) % size]
            }
            val iterations = 1_000_000
            var p = 0
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                p = array[p]
            }
            val elapsed = System.nanoTime() - startTime
            val nsPerAccess = elapsed.toDouble() / iterations.toDouble()
            if (p == -1) {
                LatencyResult(nsPerAccess + 0.0001, false)
            } else {
                LatencyResult(nsPerAccess, false)
            }
        } catch (e: OutOfMemoryError) {
            LatencyResult(500.0, true)
        }
    }

    private fun runAllocationPressure(): AllocResult {
        return try {
            val passes = 100
            val size = 512 * 1024
            val startTime = System.nanoTime()
            var count = 0
            for (i in 0 until passes) {
                val temp = ByteArray(size)
                temp[0] = 1
                if (temp[0] == 1.toByte()) {
                    count++
                }
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val allocsPerSec = count.toDouble() / elapsed
            AllocResult(allocsPerSec / 2.0, false)
        } catch (e: OutOfMemoryError) {
            AllocResult(0.0, true)
        }
    }

    private fun runCacheProbe(): LatencyResult {
        return try {
            val size = 4 * 1024 * 1024 / 4
            val array = IntArray(size)
            val random = Random(12345)
            val indices = IntArray(size) { it }
            for (i in size - 1 downTo 1) {
                val j = random.nextInt(i + 1)
                val temp = indices[i]
                indices[i] = indices[j]
                indices[j] = temp
            }
            for (i in 0 until size) {
                array[indices[i]] = indices[(i + 1) % size]
            }
            val iterations = 500_000
            var p = 0
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                p = array[p]
            }
            val elapsed = System.nanoTime() - startTime
            val nsPerAccess = elapsed.toDouble() / iterations.toDouble()
            LatencyResult(nsPerAccess, false)
        } catch (e: OutOfMemoryError) {
            LatencyResult(500.0, true)
        }
    }
}
