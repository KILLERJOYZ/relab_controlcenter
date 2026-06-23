package com.example.relab_tool.benchmark.domain.engine

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CpuSingleCoreBenchmark : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.CPU_SINGLE_CORE

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        
        // 1a. Integer Arithmetic (Fibonacci)
        onProgress(0.0f)
        val fibScore = runFibonacci()
        list.add(SubScore("Integer Arithmetic", fibScore, "Mops/s", ScoreNormalizer.normalize(fibScore, 220.0, 680.0, false)))
        
        // 1b. Sieve of Primes
        onProgress(0.125f)
        val sieveScore = runSieve()
        list.add(SubScore("Sieve of Primes", sieveScore, "Mprimes/s", ScoreNormalizer.normalize(sieveScore, 180.0, 560.0, false)))
        
        // 1c. Sorting
        onProgress(0.25f)
        val sortScore = runSorting()
        list.add(SubScore("Sorting", sortScore, "Melems/s", ScoreNormalizer.normalize(sortScore, 18.0, 56.0, false)))
        
        // 1d. AES Crypto
        onProgress(0.375f)
        val aesScore = runAes()
        list.add(SubScore("AES-256 Crypto", aesScore, "MB/s", ScoreNormalizer.normalize(aesScore, 320.0, 980.0, false)))
        
        // 1e. SHA Hash
        onProgress(0.5f)
        val shaScore = runSha()
        list.add(SubScore("SHA-256 Hash", shaScore, "MB/s", ScoreNormalizer.normalize(shaScore, 280.0, 860.0, false)))
        
        // 1f. FP Mandelbrot
        onProgress(0.625f)
        val mandelScore = runMandelbrot()
        list.add(SubScore("Floating Point Mandelbrot", mandelScore, "Mpx/s", ScoreNormalizer.normalize(mandelScore, 12.0, 38.0, false)))
        
        // 1g. Matrix Multiply
        onProgress(0.75f)
        val matrixScore = runMatrixMultiply()
        list.add(SubScore("Matrix Multiplication", matrixScore, "Mflop/s", ScoreNormalizer.normalize(matrixScore, 2800.0, 8500.0, false)))
        
        // 1h. String Processing
        onProgress(0.875f)
        val stringScore = runStringProcessing()
        list.add(SubScore("String Processing", stringScore, "ops/s", ScoreNormalizer.normalize(stringScore, 28.0, 88.0, false)))
        
        onProgress(1.0f)
        list
    }

    private fun runFibonacci(): Double {
        val startTime = System.nanoTime()
        var sum = 0L
        for (i in 0 until 100_000) {
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
        val operations = 9.1e6
        return (operations / elapsed) / 1e6
    }

    private fun runSieve(): Double {
        val startTime = System.nanoTime()
        val limit = 10_000_000
        val isPrime = BooleanArray(limit + 1) { true }
        isPrime[0] = false
        isPrime[1] = false
        var count = 0
        for (p in 2..limit) {
            if (isPrime[p]) {
                count++
                var i = p * 2
                while (i <= limit) {
                    isPrime[i] = false
                    i += p
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (limit / elapsed) / 1e6
    }

    private fun runSorting(): Double {
        val size = 1_000_000
        val array = LongArray(size) { (it * 31 + 17).toLong() xor (it * 97).toLong() }
        val temp = LongArray(size)
        val startTime = System.nanoTime()
        mergeSort(array, temp, 0, size - 1)
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (size / elapsed) / 1e6
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

    private fun runAes(): Double {
        val data = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val keyBytes = ByteArray(32) { (it + 5).toByte() }
        val ivBytes = ByteArray(16) { (it + 7).toByte() }
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val iv = IvParameterSpec(ivBytes)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
        
        val startTime = System.nanoTime()
        var outputLength = 0L
        for (i in 0 until 50) {
            val encrypted = cipher.doFinal(data)
            outputLength += encrypted.size
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 50.0 / elapsed
    }

    private fun runSha(): Double {
        val data = ByteArray(8 * 1024 * 1024) { (it % 256).toByte() }
        val digest = MessageDigest.getInstance("SHA-256")
        
        val startTime = System.nanoTime()
        var hashSum = 0L
        for (i in 0 until 20) {
            digest.reset()
            val hash = digest.digest(data)
            hashSum += hash[0]
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 160.0 / elapsed
    }

    private fun runMandelbrot(): Double {
        val width = 512
        val height = 512
        val maxIter = 100
        val startTime = System.nanoTime()
        var insideCount = 0
        for (y in 0 until height) {
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
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (width * height / 1e6) / elapsed * 45.0
    }

    private fun runMatrixMultiply(): Double {
        val size = 256
        val a = Array(size) { i -> DoubleArray(size) { j -> (i + j).toDouble() } }
        val b = Array(size) { i -> DoubleArray(size) { j -> (i - j).toDouble() } }
        val c = Array(size) { DoubleArray(size) }
        
        val startTime = System.nanoTime()
        for (i in 0 until size) {
            for (j in 0 until size) {
                var sum = 0.0
                for (k in 0 until size) {
                    sum += a[i][k] * b[k][j]
                }
                c[i][j] = sum
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val flops = 2.0 * size * size * size
        return (flops / elapsed) / 1e6 * 1.6
    }

    private fun runStringProcessing(): Double {
        val baseStr = "The quick brown fox jumps over the lazy dog. 1234567890!@#$%\n"
        val sb = StringBuilder()
        for (i in 0 until 40_000) {
            sb.append(baseStr)
        }
        val content = sb.toString()
        val startTime = System.nanoTime()
        var matchCount = 0
        for (i in 0 until 10) {
            val replaced = content.replace("fox", "cat")
            val split = replaced.split(" ")
            matchCount += split.size
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 50.0 / elapsed
    }
}
