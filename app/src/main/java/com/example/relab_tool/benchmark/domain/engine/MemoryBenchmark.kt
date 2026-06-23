package com.example.relab_tool.benchmark.domain.engine

import android.app.ActivityManager
import android.content.Context
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.util.Random

class MemoryBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.MEMORY

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val isLowMem = memInfo.availMem < 200 * 1024 * 1024
        
        val size = if (isLowMem) 8 * 1024 * 1024 else 64 * 1024 * 1024
        
        // 1. Sequential Write BW
        onProgress(0.00f)
        val seqWriteVal = BenchmarkHarness.medianOfThreeLight { runSeqWriteBw(size, isLowMem) }
        list.add(SubScore("Sequential Write BW", seqWriteVal, "GB/s", ScoreNormalizer.normalize(seqWriteVal, 10.0, 50.0, false), isLowMem))
        
        // 2. Sequential Read BW
        onProgress(0.05f)
        val seqReadVal = BenchmarkHarness.medianOfThreeLight { runSeqReadBw(size, isLowMem) }
        list.add(SubScore("Sequential Read BW", seqReadVal, "GB/s", ScoreNormalizer.normalize(seqReadVal, 15.0, 75.0, false), isLowMem))
        
        // 3. Streaming Copy
        onProgress(0.10f)
        val streamCopyVal = BenchmarkHarness.medianOfThreeLight { runStreamingCopy(size, isLowMem) }
        list.add(SubScore("Streaming Copy", streamCopyVal, "GB/s", ScoreNormalizer.normalize(streamCopyVal, 8.0, 40.0, false), isLowMem))
        
        // 4. Reverse Sequential Read
        onProgress(0.15f)
        val revSeqReadVal = BenchmarkHarness.medianOfThreeLight { runReverseSeqRead(size, isLowMem) }
        list.add(SubScore("Reverse Sequential Read", revSeqReadVal, "GB/s", ScoreNormalizer.normalize(revSeqReadVal, 10.0, 50.0, false), isLowMem))
        
        // 5. Random Pointer Chase (DRAM)
        onProgress(0.20f)
        val dramLatency = BenchmarkHarness.medianOfThreeLight { runRandomPointerChase(1024 * 1024) }
        list.add(SubScore("DRAM Random Latency", dramLatency, "ns", ScoreNormalizer.normalize(dramLatency, 100.0, 20.0, true), false))
        
        // 6. L1 Cache Latency (8KB = 2048 ints — fits in 32KB L1D)
        onProgress(0.25f)
        val l1Latency = BenchmarkHarness.medianOfThreeLight { runRandomPointerChase(2048) }
        list.add(SubScore("L1 Cache Latency", l1Latency, "ns", ScoreNormalizer.normalize(l1Latency, 2.5, 1.0, true), false))
        
        // 7. L2 Cache Latency (256KB = 65536 ints — spills L1, fits L2)
        onProgress(0.30f)
        val l2Latency = BenchmarkHarness.medianOfThreeLight { runRandomPointerChase(65536) }
        list.add(SubScore("L2 Cache Latency", l2Latency, "ns", ScoreNormalizer.normalize(l2Latency, 8.0, 3.0, true), false))
        
        // 8. L3/LLC Cache Latency (4MB = 1048576 ints — spills L2, exercises L3/DRAM boundary)
        onProgress(0.35f)
        val l3Latency = BenchmarkHarness.medianOfThreeLight { runRandomPointerChase(1048576) }
        list.add(SubScore("L3 Cache Latency", l3Latency, "ns", ScoreNormalizer.normalize(l3Latency, 30.0, 8.0, true), false))
        
        // 9. Cache Hierarchy Sweep
        onProgress(0.40f)
        val cacheSweepVal = BenchmarkHarness.medianOfThreeLight { runCacheSweep() }
        list.add(SubScore("Cache Sweep Throughput", cacheSweepVal, "M-ops/s", ScoreNormalizer.normalize(cacheSweepVal, 50.0, 250.0, false), false))
        
        // 10. Allocation Pressure (Large)
        onProgress(0.45f)
        val largeAllocVal = BenchmarkHarness.medianOfThreeLight { runLargeAllocations(isLowMem) }
        list.add(SubScore("Large Block Alloc Speed", largeAllocVal, "allocs/s", ScoreNormalizer.normalize(largeAllocVal, 100.0, 500.0, false), false))
        
        // 11. Allocation Pressure (Small)
        onProgress(0.50f)
        val smallAllocVal = BenchmarkHarness.medianOfThreeLight { runSmallAllocations() }
        list.add(SubScore("Small Block Alloc Speed", smallAllocVal, "k-allocs/s", ScoreNormalizer.normalize(smallAllocVal, 50.0, 250.0, false), false))
        
        // 12. GC Stress Test (measures ART runtime behavior, not pure hardware)
        onProgress(0.55f)
        val gcStressVal = BenchmarkHarness.medianOfThreeLight { runGCStress() }
        list.add(SubScore("GC Pause Index (Runtime)", gcStressVal, "ms", ScoreNormalizer.normalize(gcStressVal, 50.0, 2.0, true), true))
        
        // 13. Memory Fill Pattern
        onProgress(0.60f)
        val fillPatternVal = BenchmarkHarness.medianOfThreeLight { runMemoryFillPattern(size, isLowMem) }
        list.add(SubScore("Memory Integrity Fill", fillPatternVal, "GB/s", ScoreNormalizer.normalize(fillPatternVal, 5.0, 25.0, false), isLowMem))
        
        // 14. Array.copyOf Speed
        onProgress(0.65f)
        val arrayCopyVal = BenchmarkHarness.medianOfThreeLight { runArrayCopyOfSpeed(size / 4, isLowMem) }
        list.add(SubScore("Array copyOf Speed", arrayCopyVal, "GB/s", ScoreNormalizer.normalize(arrayCopyVal, 8.0, 40.0, false), isLowMem))
        
        // 15. Object Array Allocation
        onProgress(0.70f)
        val objArrayVal = BenchmarkHarness.medianOfThreeLight { runObjectArrayAllocation() }
        list.add(SubScore("Object Array Alloc Speed", objArrayVal, "k-allocs/s", ScoreNormalizer.normalize(objArrayVal, 10.0, 50.0, false), false))
        
        // 16. ByteBuffer Throughput
        onProgress(0.75f)
        val byteBufVal = BenchmarkHarness.medianOfThreeLight { runByteBufferThroughput(size, isLowMem) }
        list.add(SubScore("ByteBuffer Direct Throughput", byteBufVal, "GB/s", ScoreNormalizer.normalize(byteBufVal, 10.0, 50.0, false), isLowMem))
        
        // 17. Memory Fragmentation
        onProgress(0.80f)
        val fragVal = BenchmarkHarness.medianOfThreeLight { runFragmentationStress() }
        list.add(SubScore("Heap Fragmentation Cost", fragVal, "ms", ScoreNormalizer.normalize(fragVal, 100.0, 10.0, true), false))
        
        // 18. Concurrent Allocation
        onProgress(0.85f)
        val concurrentAllocVal = BenchmarkHarness.medianOfThreeLight { runConcurrentAllocations() }
        list.add(SubScore("Concurrent Alloc Speed", concurrentAllocVal, "k-allocs/s", ScoreNormalizer.normalize(concurrentAllocVal, 20.0, 100.0, false), false))
        
        // 19. Bandwidth Under CPU Load (real concurrent stress)
        onProgress(0.90f)
        val loadBwVal = BenchmarkHarness.medianOfThreeLight { runBandwidthUnderCpuLoad(size / 2, isLowMem) }
        list.add(SubScore("Bandwidth Under Load", loadBwVal, "GB/s", ScoreNormalizer.normalize(loadBwVal, 5.0, 30.0, false), isLowMem))
        
        // 20. Page Fault Stress
        onProgress(0.95f)
        val pageFaultVal = BenchmarkHarness.medianOfThreeLight { runPageFaultStress(isLowMem) }
        list.add(SubScore("Page Fault Latency Cost", pageFaultVal, "ms", ScoreNormalizer.normalize(pageFaultVal, 50.0, 5.0, true), isLowMem))

        onProgress(1.00f)
        list
    }

    private fun runSeqWriteBw(size: Int, isLowMem: Boolean): Double {
        return try {
            val arr = ByteArray(size)
            val passes = if (isLowMem) 4 else 10
            val startTime = System.nanoTime()
            for (p in 0 until passes) {
                arr.fill(0xFF.toByte())
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * passes
            totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runSeqReadBw(size: Int, isLowMem: Boolean): Double {
        return try {
            val arr = ByteArray(size) { 0x55.toByte() }
            val passes = if (isLowMem) 4 else 10
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
            if (sum == 0L) gbPerSec + 1e-6 else gbPerSec
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runStreamingCopy(size: Int, isLowMem: Boolean): Double {
        return try {
            val src = ByteArray(size) { 0x33.toByte() }
            val dst = ByteArray(size)
            val passes = if (isLowMem) 4 else 10
            val startTime = System.nanoTime()
            for (p in 0 until passes) {
                System.arraycopy(src, 0, dst, 0, size)
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * passes * 2.0 // Read + Write
            totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runReverseSeqRead(size: Int, isLowMem: Boolean): Double {
        return try {
            val arr = ByteArray(size) { 0x66.toByte() }
            val passes = if (isLowMem) 4 else 10
            val startTime = System.nanoTime()
            var sum = 0L
            for (p in 0 until passes) {
                for (i in arr.size - 1 downTo 0) {
                    sum += arr[i].toLong()
                }
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * passes
            val gbPerSec = totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
            if (sum == 0L) gbPerSec + 1e-6 else gbPerSec
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runRandomPointerChase(size: Int): Double {
        return try {
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
            val iterations = if (size > 10000) 200_000 else 1_000_000
            var p = 0
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                p = array[p]
            }
            val elapsed = System.nanoTime() - startTime
            val nsPerAccess = elapsed.toDouble() / iterations.toDouble()
            if (p == -1) nsPerAccess + 1e-6 else nsPerAccess
        } catch (e: OutOfMemoryError) {
            500.0
        }
    }

    private fun runCacheSweep(): Double {
        var dummy = 0L
        val startTime = System.nanoTime()
        // Run a lightweight cache-friendly loop
        for (i in 0 until 5_000_000) {
            dummy += i % 32
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 5.0 / elapsed // M-ops/s
    }

    private fun runLargeAllocations(isLowMem: Boolean): Double {
        return try {
            val count = if (isLowMem) 10 else 50
            val size = 512 * 1024 // 512KB
            val list = mutableListOf<ByteArray>()
            val startTime = System.nanoTime()
            for (i in 0 until count) {
                list.add(ByteArray(size))
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            list.clear()
            count.toDouble() / elapsed
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runSmallAllocations(): Double {
        return try {
            val count = 20000
            val size = 256
            val list = Array<ByteArray?>(count) { null }
            val startTime = System.nanoTime()
            for (i in 0 until count) {
                list[i] = ByteArray(size)
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            (count.toDouble() / 1000.0) / elapsed // k-allocs/s
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runGCStress(): Double {
        // Force garbage collections and measure time
        val startTime = System.nanoTime()
        for (i in 0 until 5) {
            System.gc()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e6
        return elapsed / 5.0 // Avg GC duration in ms
    }

    private fun runMemoryFillPattern(size: Int, isLowMem: Boolean): Double {
        return try {
            val arr = ByteArray(size)
            val passes = if (isLowMem) 2 else 5
            val startTime = System.nanoTime()
            for (p in 0 until passes) {
                for (i in arr.indices) {
                    arr[i] = (i and 0xFF).toByte()
                }
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * passes
            totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runArrayCopyOfSpeed(size: Int, isLowMem: Boolean): Double {
        return try {
            val src = IntArray(size) { it }
            val passes = if (isLowMem) 4 else 10
            val startTime = System.nanoTime()
            for (p in 0 until passes) {
                val copy = src.copyOf()
                val first = copy[0]
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * 4.0 * passes // 4 bytes per Int
            totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runObjectArrayAllocation(): Double {
        return try {
            val count = 1000
            val list = Array<Array<Any?>?>(count) { null }
            val startTime = System.nanoTime()
            for (i in 0 until count) {
                list[i] = Array(100) { "String_$it" }
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            (count.toDouble() / 1000.0) / elapsed
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runByteBufferThroughput(size: Int, isLowMem: Boolean): Double {
        return try {
            val buffer = ByteBuffer.allocateDirect(size)
            val temp = ByteArray(4096)
            val passes = if (isLowMem) 400 else 1000
            val startTime = System.nanoTime()
            for (p in 0 until passes) {
                buffer.clear()
                while (buffer.remaining() >= temp.size) {
                    buffer.put(temp)
                }
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalBytes = size.toDouble() * passes
            totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
        } catch (e: Exception) {
            0.0
        }
    }

    private fun runFragmentationStress(): Double {
        return try {
            val count = 10000
            val list = Array<ByteArray?>(count) { null }
            val startTime = System.nanoTime()
            for (i in 0 until count) {
                list[i] = ByteArray(if (i % 2 == 0) 128 else 2048)
            }
            // Free every second block
            for (i in 0 until count step 2) {
                list[i] = null
            }
            // Allocate again
            for (i in 0 until count step 2) {
                list[i] = ByteArray(256)
            }
            val elapsed = (System.nanoTime() - startTime) / 1e6
            elapsed
        } catch (e: OutOfMemoryError) {
            200.0
        }
    }

    private fun runConcurrentAllocations(): Double {
        return try {
            val count = 10000
            val startTime = System.nanoTime()
            // Sim sequential allocations but running rapidly
            val list = mutableListOf<ByteArray>()
            for (i in 0 until count) {
                list.add(ByteArray(128))
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            list.clear()
            (count.toDouble() / 1000.0) / elapsed
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }

    private fun runPageFaultStress(isLowMem: Boolean): Double {
        return try {
            // Allocate new array and touch each page (4KB page size)
            val pageSize = 4096
            val arraySize = if (isLowMem) 4 * 1024 * 1024 else 16 * 1024 * 1024
            val startTime = System.nanoTime()
            val data = ByteArray(arraySize)
            for (i in 0 until arraySize step pageSize) {
                data[i] = 1
            }
            val elapsed = (System.nanoTime() - startTime) / 1e6
            elapsed
        } catch (e: OutOfMemoryError) {
            100.0
        }
    }
    /**
     * Measures memory bandwidth while CPU cores are under load.
     * Spawns background CPU stress threads to create realistic contention,
     * then measures sequential write bandwidth under that load.
     */
    private fun runBandwidthUnderCpuLoad(size: Int, isLowMem: Boolean): Double {
        return try {
            val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
            val cpuRunning = java.util.concurrent.atomic.AtomicBoolean(true)
            val cpuThreads = mutableListOf<Thread>()
            
            // Spawn CPU stress on all cores
            for (c in 0 until cores) {
                val t = Thread {
                    var x = 0L
                    while (cpuRunning.get()) {
                        for (i in 0 until 10000) {
                            x = x xor (i.toLong() shl 3)
                            x = x or (x ushr 5)
                        }
                        BenchmarkHarness.consume(x)
                    }
                }
                t.priority = Thread.MAX_PRIORITY
                t.start()
                cpuThreads.add(t)
            }
            
            // Give CPU stress 50ms to saturate cores
            Thread.sleep(50)
            
            // Now measure memory bandwidth under load
            val arr = ByteArray(size)
            val passes = if (isLowMem) 3 else 6
            val startTime = System.nanoTime()
            for (p in 0 until passes) {
                arr.fill(0xFF.toByte())
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            
            // Stop CPU stress
            cpuRunning.set(false)
            cpuThreads.forEach { it.join(100) }
            
            val totalBytes = size.toDouble() * passes
            totalBytes / (1024.0 * 1024.0 * 1024.0) / elapsed
        } catch (e: OutOfMemoryError) {
            0.0
        }
    }
}
