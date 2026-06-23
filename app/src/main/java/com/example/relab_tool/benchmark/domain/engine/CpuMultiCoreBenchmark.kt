package com.example.relab_tool.benchmark.domain.engine

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CpuMultiCoreBenchmark : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.CPU_MULTI_CORE

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        
        // 2a. Parallel Integer
        onProgress(0.0f)
        val parallelIntScore = runParallelInteger(cores)
        list.add(SubScore("Parallel Integer Arithmetic", parallelIntScore, "Mops/s", ScoreNormalizer.normalize(parallelIntScore, 1400.0, 5000.0, false)))
        
        // 2b. Parallel Sort
        onProgress(0.15f)
        val parallelSortScore = runParallelSort(cores)
        list.add(SubScore("Parallel Sort", parallelSortScore, "Melems/s", ScoreNormalizer.normalize(parallelSortScore, 110.0, 380.0, false)))
        
        // 2c. Parallel AES
        onProgress(0.3f)
        val parallelAesScore = runParallelAes(cores)
        list.add(SubScore("Parallel AES Crypto", parallelAesScore, "MB/s", ScoreNormalizer.normalize(parallelAesScore, 1800.0, 6500.0, false)))
        
        // 2d. Parallel FP Mandelbrot
        onProgress(0.45f)
        val parallelMandelScore = runParallelMandelbrot(cores)
        list.add(SubScore("Parallel Mandelbrot", parallelMandelScore, "Mpx/s", ScoreNormalizer.normalize(parallelMandelScore, 80.0, 280.0, false)))
        
        // 2e. Parallel JSON Serialize
        onProgress(0.60f)
        val parallelJsonScore = runParallelJson(cores)
        list.add(SubScore("Parallel JSON Serialization", parallelJsonScore, "cycles/s", ScoreNormalizer.normalize(parallelJsonScore, 18.0, 60.0, false)))
        
        // 2f. Core Scaling Ratio
        onProgress(0.75f)
        val singleFib = runSingleFibonacciInline()
        val scalingRatio = if (singleFib > 0) parallelIntScore / (singleFib * 9.1 / 91.0) else 1.0
        val finalRatio = scalingRatio.coerceIn(1.0, cores.toDouble() * 1.2)
        list.add(SubScore("Core Scaling Ratio", finalRatio, "x", ScoreNormalizer.normalize(finalRatio, 5.5, 11.0, false)))
        
        // 2g. Memory Bandwidth under Load
        onProgress(0.90f)
        val bwPreservation = runMemoryBandwidthUnderLoad(cores)
        list.add(SubScore("Memory Bandwidth under Load", bwPreservation, "%", ScoreNormalizer.normalize(bwPreservation, 55.0, 90.0, false)))
        
        onProgress(1.0f)
        list
    }

    private suspend fun runParallelInteger(cores: Int): Double {
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    var sum = 0L
                    val iterations = (100_000 / cores).coerceAtLeast(1000)
                    for (i in 0 until iterations) {
                        var a = 0L
                        var b = 1L
                        for (j in 2..92) {
                            val next = a + b
                            a = b
                            b = next
                        }
                        sum += b
                    }
                    sum
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val operations = 91.0 * (100_000 / cores).coerceAtLeast(1000) * cores
        return (operations / elapsed) / 1e6
    }

    private suspend fun runParallelSort(cores: Int): Double {
        val size = 200_000
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    val array = LongArray(size) { (it * 31 + 17).toLong() xor (it * 97).toLong() }
                    val temp = LongArray(size)
                    mergeSort(array, temp, 0, size - 1)
                    array[0]
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val totalSorted = size.toDouble() * cores
        return (totalSorted / elapsed) / 1e6
    }

    private fun mergeSort(array: LongArray, temp: LongArray, left: Int, right: Int) {
        if (left >= right) return
        val mid = (left + right) / 2
        mergeSort(array, temp, left, mid)
        mergeSort(array, temp, mid + 1, right)
        merge(array, temp, left, mid, right)
    }

    private fun merge(array: LongArray, temp: LongArray, left: Int, mid: Int, right: Int) {
        for (i in left..right) {
            temp[i] = array[i]
        }
        var i = left
        var j = mid + 1
        var k = left
        while (i <= mid && j <= right) {
            if (temp[i] <= temp[j]) {
                array[k++] = temp[i++]
            } else {
                array[k++] = temp[j++]
            }
        }
        while (i <= mid) {
            array[k++] = temp[i++]
        }
    }

    private suspend fun runParallelAes(cores: Int): Double {
        val dataSize = 256 * 1024
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    val data = ByteArray(dataSize) { (it % 256).toByte() }
                    val keyBytes = ByteArray(32) { (it + 5).toByte() }
                    val ivBytes = ByteArray(16) { (it + 7).toByte() }
                    val secretKey = SecretKeySpec(keyBytes, "AES")
                    val iv = IvParameterSpec(ivBytes)
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
                    var outputLength = 0L
                    for (i in 0 until 40) {
                        val encrypted = cipher.doFinal(data)
                        outputLength += encrypted.size
                    }
                    outputLength
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val totalMb = (dataSize.toDouble() * 40 * cores) / (1024 * 1024)
        return totalMb / elapsed
    }

    private suspend fun runParallelMandelbrot(cores: Int): Double {
        val width = 512
        val height = 512
        val maxIter = 100
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) { threadIndex ->
                async {
                    val startY = threadIndex * (height / cores)
                    val endY = if (threadIndex == cores - 1) height else (threadIndex + 1) * (height / cores)
                    var insideCount = 0
                    for (y in startY until endY) {
                        val cy = -1.5 + y * (3.0 / height)
                        for (x in 0 until width) {
                            val cx = -2.0 + x * (3.0 / width)
                            var zx = 0.0
                            var zy = 0.0
                            var iter = 0
                            while (zx * zx + zy * zy < 4.0 && iter < maxIter) {
                                val temp = zx * zx - zy * zy + cx
                                zy = 2.0 * zx * zy + cy
                                zx = temp
                                iter++
                            }
                            if (iter == maxIter) insideCount++
                        }
                    }
                    insideCount
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (width * height / 1e6) / elapsed * 45.0
    }

    private suspend fun runParallelJson(cores: Int): Double {
        val jsonCycles = 30
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    var sum = 0
                    for (cycle in 0 until jsonCycles) {
                        val root = JSONObject()
                        val array = JSONArray()
                        for (i in 0 until 500) {
                            val item = JSONObject()
                            item.put("id", i)
                            item.put("name", "SubScoreName_$i")
                            item.put("score", i * 2)
                            item.put("isPartial", i % 2 == 0)
                            array.put(item)
                        }
                        root.put("subScores", array)
                        val jsonStr = root.toString()
                        
                        val parsedRoot = JSONObject(jsonStr)
                        val parsedArray = parsedRoot.getJSONArray("subScores")
                        sum += parsedArray.length()
                    }
                    sum
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val totalCycles = jsonCycles.toDouble() * cores
        return totalCycles / elapsed
    }

    private fun runSingleFibonacciInline(): Double {
        val startTime = System.nanoTime()
        var sum = 0L
        for (i in 0 until 1000) {
            var a = 0L
            var b = 1L
            for (j in 2..92) {
                val next = a + b
                a = b
                b = next
            }
            sum += b
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (1000.0) / elapsed
    }

    private suspend fun runMemoryBandwidthUnderLoad(cores: Int): Double {
        val size = 4 * 1024 * 1024
        
        val singleBw = withContext(Dispatchers.Default) {
            val arr = ByteArray(size)
            val startTime = System.nanoTime()
            for (pass in 0 until 20) {
                arr.fill(0x55.toByte())
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            (size.toDouble() * 20.0) / (1024 * 1024 * 1024) / elapsed
        }

        val multiBw = withContext(Dispatchers.Default) {
            val startTime = System.nanoTime()
            coroutineScope {
                val jobs = List(cores) {
                    async {
                        val arr = ByteArray(size)
                        for (pass in 0 until 20) {
                            arr.fill(0x55.toByte())
                        }
                        arr[0]
                    }
                }
                jobs.awaitAll()
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            (size.toDouble() * 20.0 * cores) / (1024 * 1024 * 1024) / elapsed
        }

        val preservation = if (singleBw > 0) (multiBw / (singleBw * cores)) * 100.0 else 50.0
        return preservation.coerceIn(10.0, 100.0)
    }
}
