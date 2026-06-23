package com.example.relab_tool.benchmark.domain.engine

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Pattern
import java.util.zip.Deflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CpuMultiCoreBenchmark : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.CPU_MULTI_CORE

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        // 1. Parallel Fibonacci
        onProgress(0.00f)
        val fibVal = runParallelFibonacci(cores)
        list.add(SubScore("Parallel Fibonacci", fibVal, "M-ops/s", ScoreNormalizer.normalize(fibVal, 1000.0, 5000.0, false)))

        // 2. Parallel Merge Sort
        onProgress(0.05f)
        val mergeSortVal = runParallelMergeSort(cores)
        list.add(SubScore("Parallel Merge Sort", mergeSortVal, "M-elems/s", ScoreNormalizer.normalize(mergeSortVal, 50.0, 250.0, false)))

        // 3. Parallel AES Crypto
        onProgress(0.10f)
        val aesVal = runParallelAes(cores)
        list.add(SubScore("Parallel AES Crypto", aesVal, "MB/s", ScoreNormalizer.normalize(aesVal, 1000.0, 5000.0, false)))

        // 4. Parallel Mandelbrot
        onProgress(0.15f)
        val mandelVal = runParallelMandelbrot(cores)
        list.add(SubScore("Parallel Mandelbrot", mandelVal, "M-px/s", ScoreNormalizer.normalize(mandelVal, 50.0, 250.0, false)))

        // 5. Parallel JSON Serialize
        onProgress(0.20f)
        val jsonVal = runParallelJson(cores)
        list.add(SubScore("Parallel JSON Serialization", jsonVal, "cycles/s", ScoreNormalizer.normalize(jsonVal, 15.0, 75.0, false)))

        // 6. Parallel Quick Sort
        onProgress(0.25f)
        val quickSortVal = runParallelQuickSort(cores)
        list.add(SubScore("Parallel Quick Sort", quickSortVal, "M-elems/s", ScoreNormalizer.normalize(quickSortVal, 60.0, 300.0, false)))

        // 7. Parallel Matrix Multiply
        onProgress(0.30f)
        val dgemmVal = runParallelMatrixMultiply(cores)
        list.add(SubScore("Parallel Matrix Multiply", dgemmVal, "M-flops", ScoreNormalizer.normalize(dgemmVal, 3000.0, 15000.0, false)))

        // 8. Parallel SHA Hash
        onProgress(0.35f)
        val shaVal = runParallelSha(cores)
        list.add(SubScore("Parallel SHA Hash", shaVal, "MB/s", ScoreNormalizer.normalize(shaVal, 500.0, 2500.0, false)))

        // 9. Parallel Compression
        onProgress(0.40f)
        val compressionVal = runParallelCompression(cores)
        list.add(SubScore("Parallel Compression", compressionVal, "MB/s", ScoreNormalizer.normalize(compressionVal, 100.0, 500.0, false)))

        // 10. Producer-Consumer Queue
        onProgress(0.45f)
        val prodConsVal = runProducerConsumerQueue(cores)
        list.add(SubScore("Producer-Consumer Queue", prodConsVal, "k-ops/s", ScoreNormalizer.normalize(prodConsVal, 100.0, 500.0, false)))

        // 11. Lock Contention
        onProgress(0.50f)
        val lockContVal = runLockContention(cores)
        list.add(SubScore("Lock Contention", lockContVal, "k-locks/s", ScoreNormalizer.normalize(lockContVal, 500.0, 2500.0, false)))

        // 12. Read-Write Lock
        onProgress(0.55f)
        val rwLockVal = runReadWriteLock(cores)
        list.add(SubScore("Read-Write Lock", rwLockVal, "k-ops/s", ScoreNormalizer.normalize(rwLockVal, 200.0, 1000.0, false)))

        // 13. Atomic Counter Stress
        onProgress(0.60f)
        val atomicVal = runAtomicCounter(cores)
        list.add(SubScore("Atomic Counter CAS", atomicVal, "M-ops/s", ScoreNormalizer.normalize(atomicVal, 5.0, 25.0, false)))

        // 14. Barrier Synchronization
        onProgress(0.65f)
        val barrierVal = runBarrierSync(cores)
        list.add(SubScore("Barrier Synchronization", barrierVal, "k-syncs/s", ScoreNormalizer.normalize(barrierVal, 10.0, 50.0, false)))

        // 15. Fork-Join Recursive
        onProgress(0.70f)
        val forkJoinVal = runForkJoinRecursive(cores)
        list.add(SubScore("Fork-Join Tasking", forkJoinVal, "tasks/s", ScoreNormalizer.normalize(forkJoinVal, 100.0, 500.0, false)))

        // 16. Thread Pool Throughput
        onProgress(0.75f)
        val poolVal = runThreadPoolThroughput(cores)
        list.add(SubScore("Thread Pool Throughput", poolVal, "k-runnables/s", ScoreNormalizer.normalize(poolVal, 50.0, 250.0, false)))

        // 17. Parallel Stream Processing
        onProgress(0.80f)
        val streamVal = runParallelStream(cores)
        list.add(SubScore("Parallel Stream Processing", streamVal, "M-items/s", ScoreNormalizer.normalize(streamVal, 10.0, 50.0, false)))

        // 18. Work-Stealing Efficiency
        onProgress(0.85f)
        val stealingVal = runWorkStealing(cores)
        list.add(SubScore("Work-Stealing Efficiency", stealingVal, "tasks/s", ScoreNormalizer.normalize(stealingVal, 50.0, 250.0, false)))

        // 19. Parallel Regex
        onProgress(0.90f)
        val regexVal = runParallelRegex(cores)
        list.add(SubScore("Parallel Regex Matching", regexVal, "k-matches/s", ScoreNormalizer.normalize(regexVal, 10.0, 50.0, false)))

        // 20. Core Scaling Ratio
        onProgress(0.95f)
        val singleFib = runSingleFibonacciInline()
        val scalingRatio = if (singleFib > 0) fibVal / singleFib else 1.0
        val finalRatio = scalingRatio.coerceIn(1.0, cores.toDouble() * 1.2)
        list.add(SubScore("Core Scaling Ratio", finalRatio, "x", ScoreNormalizer.normalize(finalRatio, 4.0, cores.toDouble().coerceAtLeast(2.0), false)))

        onProgress(1.00f)
        list
    }

    private suspend fun runParallelFibonacci(cores: Int): Double {
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    var sum = 0L
                    val iterations = 10_000
                    for (i in 0 until iterations) {
                        var a = 0L; var b = 1L
                        for (j in 2..92) {
                            val next = a + b
                            a = b; b = next
                        }
                        sum += b
                    }
                    sum
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val operations = 91.0 * 10_000 * cores
        return (operations / elapsed) / 1e6
    }

    private suspend fun runParallelMergeSort(cores: Int): Double {
        val size = 5000
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
        return (size.toDouble() * cores) / elapsed / 1e6
    }

    private fun mergeSort(array: LongArray, temp: LongArray, left: Int, right: Int) {
        if (left >= right) return
        val mid = (left + right) / 2
        mergeSort(array, temp, left, mid)
        mergeSort(array, temp, mid + 1, right)
        merge(array, temp, left, mid, right)
    }

    private fun merge(array: LongArray, temp: LongArray, left: Int, mid: Int, right: Int) {
        for (i in left..right) temp[i] = array[i]
        var i = left; var j = mid + 1; var k = left
        while (i <= mid && j <= right) {
            if (temp[i] <= temp[j]) array[k++] = temp[i++] else array[k++] = temp[j++]
        }
        while (i <= mid) array[k++] = temp[i++]
    }

    private suspend fun runParallelAes(cores: Int): Double {
        val dataSize = 16 * 1024
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
                    for (i in 0 until 10) {
                        val encrypted = cipher.doFinal(data)
                        outputLength += encrypted.size
                    }
                    outputLength
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val totalMb = (dataSize.toDouble() * 10 * cores) / (1024 * 1024)
        return totalMb / elapsed
    }

    private suspend fun runParallelMandelbrot(cores: Int): Double {
        val width = 256
        val height = 256
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
                            var zx = 0.0; var zy = 0.0; var iter = 0
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
        return (width * height / 1e6) / elapsed
    }

    private suspend fun runParallelJson(cores: Int): Double {
        val jsonCycles = 10
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    var sum = 0
                    for (cycle in 0 until jsonCycles) {
                        val root = JSONObject()
                        val array = JSONArray()
                        for (i in 0 until 100) {
                            val item = JSONObject()
                            item.put("id", i)
                            item.put("name", "Name_$i")
                            array.put(item)
                        }
                        root.put("items", array)
                        val jsonStr = root.toString()
                        val parsedRoot = JSONObject(jsonStr)
                        val parsedArray = parsedRoot.getJSONArray("items")
                        sum += parsedArray.length()
                    }
                    sum
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (jsonCycles.toDouble() * cores) / elapsed
    }

    private suspend fun runParallelQuickSort(cores: Int): Double {
        val size = 5000
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    val array = LongArray(size) { (it * 31 + 17).toLong() xor (it * 97).toLong() }
                    quickSort(array, 0, size - 1)
                    array[0]
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (size.toDouble() * cores) / elapsed / 1e6
    }

    private fun quickSort(arr: LongArray, low: Int, high: Int) {
        if (low < high) {
            val pi = partition(arr, low, high)
            quickSort(arr, low, pi - 1)
            quickSort(arr, pi + 1, high)
        }
    }

    private fun partition(arr: LongArray, low: Int, high: Int): Int {
        val pivot = arr[high]
        var i = low - 1
        for (j in low until high) {
            if (arr[j] < pivot) {
                i++
                val temp = arr[i]
                arr[i] = arr[j]
                arr[j] = temp
            }
        }
        val temp = arr[i + 1]
        arr[i + 1] = arr[high]
        arr[high] = temp
        return i + 1
    }

    private suspend fun runParallelMatrixMultiply(cores: Int): Double {
        val size = 64
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    val a = Array(size) { i -> DoubleArray(size) { j -> (i + j).toDouble() } }
                    val b = Array(size) { i -> DoubleArray(size) { j -> (i - j).toDouble() } }
                    val c = Array(size) { DoubleArray(size) }
                    for (pass in 0 until 5) {
                        for (i in 0 until size) {
                            for (j in 0 until size) {
                                var sum = 0.0
                                for (k in 0 until size) {
                                    sum += a[i][k] * b[k][j]
                                }
                                c[i][j] = sum
                            }
                        }
                    }
                    c[0][0]
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val flops = 2.0 * size * size * size * 5 * cores
        return flops / elapsed / 1e6
    }

    private suspend fun runParallelSha(cores: Int): Double {
        val dataSize = 100 * 1024
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    val data = ByteArray(dataSize) { (it % 256).toByte() }
                    val digest = MessageDigest.getInstance("SHA-256")
                    for (pass in 0 until 10) {
                        digest.reset()
                        digest.update(data)
                        val hash = digest.digest()
                    }
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (dataSize.toDouble() * 10 * cores / (1024.0 * 1024.0)) / elapsed
    }

    private suspend fun runParallelCompression(cores: Int): Double {
        val size = 100 * 1024
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    val data = ByteArray(size) { (it % 256).toByte() }
                    val compressBuffer = ByteArray(size * 2)
                    val deflater = Deflater()
                    for (pass in 0 until 5) {
                        deflater.setInput(data)
                        deflater.finish()
                        deflater.deflate(compressBuffer)
                        deflater.reset()
                    }
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (size.toDouble() * 5 * cores / (1024.0 * 1024.0)) / elapsed
    }

    private suspend fun runProducerConsumerQueue(cores: Int): Double {
        val queue = ConcurrentLinkedQueue<Int>()
        val limit = 5000
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = mutableListOf<Deferred<Unit>>()
            // Launch producers
            repeat(cores.coerceAtMost(2)) {
                jobs.add(async {
                    for (i in 0 until limit) {
                        queue.add(i)
                    }
                })
            }
            // Launch consumers
            repeat(cores.coerceAtMost(2)) {
                jobs.add(async {
                    var count = 0
                    while (count < limit) {
                        val item = queue.poll()
                        if (item != null) count++
                    }
                })
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (limit.toDouble() * cores.coerceAtMost(2)) / elapsed / 1000.0
    }

    private suspend fun runLockContention(cores: Int): Double {
        val lock = ReentrantLock()
        var counter = 0L
        val limit = 5000
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    for (i in 0 until limit) {
                        lock.lock()
                        try {
                            counter++
                        } finally {
                            lock.unlock()
                        }
                    }
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (limit.toDouble() * cores) / elapsed / 1000.0
    }

    private suspend fun runReadWriteLock(cores: Int): Double {
        val rwLock = ReentrantReadWriteLock()
        var counter = 0L
        val limit = 2000
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) { id ->
                async {
                    val isWriter = id % 4 == 0
                    for (i in 0 until limit) {
                        if (isWriter) {
                            rwLock.writeLock().lock()
                            try {
                                counter++
                            } finally {
                                rwLock.writeLock().unlock()
                            }
                        } else {
                            rwLock.readLock().lock()
                            try {
                                val v = counter
                            } finally {
                                rwLock.readLock().unlock()
                            }
                        }
                    }
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (limit.toDouble() * cores) / elapsed / 1000.0
    }

    private suspend fun runAtomicCounter(cores: Int): Double {
        val counter = AtomicLong(0L)
        val limit = 200_000
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    for (i in 0 until limit) {
                        counter.incrementAndGet()
                    }
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (limit.toDouble() * cores) / elapsed / 1e6
    }

    private suspend fun runBarrierSync(cores: Int): Double {
        val barrierCores = cores.coerceAtMost(4)
        if (barrierCores <= 1) return 20.0
        val barrier = CyclicBarrier(barrierCores)
        val limit = 500
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(barrierCores) {
                async {
                    for (i in 0 until limit) {
                        try {
                            barrier.await()
                        } catch (e: Exception) {
                            break
                        }
                    }
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (limit.toDouble() * barrierCores) / elapsed / 1000.0
    }

    private suspend fun runForkJoinRecursive(cores: Int): Double {
        suspend fun recursiveFib(n: Int): Long {
            if (n <= 1) return n.toLong()
            return if (n > 20) {
                coroutineScope {
                    val a = async { recursiveFib(n - 1) }
                    val b = async { recursiveFib(n - 2) }
                    a.await() + b.await()
                }
            } else {
                var a = 0L; var b = 1L
                for (i in 2..n) {
                    val next = a + b
                    a = b; b = next
                }
                b
            }
        }
        val startTime = System.nanoTime()
        val result = recursiveFib(30)
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 1.0 / elapsed
    }

    private suspend fun runThreadPoolThroughput(cores: Int): Double {
        val limit = 5000
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(limit) {
                async(Dispatchers.Default) {
                    var sum = 0L
                    for (i in 0 until 10) sum += i
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return limit.toDouble() / elapsed / 1000.0
    }

    private suspend fun runParallelStream(cores: Int): Double {
        val size = 50000
        val list = List(size) { it }
        val startTime = System.nanoTime()
        coroutineScope {
            val chunks = list.chunked(size / cores)
            val jobs = chunks.map { chunk ->
                async {
                    chunk.map { it * 2 }.filter { it % 3 == 0 }.reduceOrNull { acc, i -> acc + i }
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return size.toDouble() / elapsed / 1e6
    }

    private suspend fun runWorkStealing(cores: Int): Double {
        val taskCount = 100
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(taskCount) { id ->
                async(Dispatchers.Default) {
                    val workTime = if (id % 10 == 0) 500 else 10
                    var sum = 0L
                    for (i in 0 until workTime * 100) sum += i
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return taskCount.toDouble() / elapsed
    }

    private suspend fun runParallelRegex(cores: Int): Double {
        val text = "User: Alice (id: 12345) Email: alice@example.com Phone: +1-555-0199"
        val pattern = Pattern.compile("User:\\s+(\\w+)")
        val startTime = System.nanoTime()
        coroutineScope {
            val jobs = List(cores) {
                async {
                    var matches = 0
                    for (pass in 0 until 100) {
                        val matcher = pattern.matcher(text)
                        while (matcher.find()) {
                            matches++
                        }
                    }
                    matches
                }
            }
            jobs.awaitAll()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (100.0 * cores) / elapsed / 1000.0
    }

    private fun runSingleFibonacciInline(): Double {
        val startTime = System.nanoTime()
        var sum = 0L
        val iterations = 10_000
        for (i in 0 until iterations) {
            var a = 0L; var b = 1L
            for (j in 2..92) {
                val next = a + b
                a = b; b = next
            }
            sum += b
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val operations = 91.0 * 10_000
        return (operations / elapsed) / 1e6
    }
}
