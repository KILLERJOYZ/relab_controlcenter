package com.example.relab_tool.benchmark.domain.engine

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Random
import java.util.regex.Pattern
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.CRC32
import android.util.Base64
import java.util.PriorityQueue

class CpuSingleCoreBenchmark : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.CPU_SINGLE_CORE

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()

        // 1. Fibonacci (Integer ALU)
        onProgress(0.00f)
        val fibScore = runFibonacci()
        list.add(SubScore("Integer ALU (Fibonacci)", fibScore, "M-ops/s", ScoreNormalizer.normalize(fibScore, 200.0, 1000.0, false)))

        // 2. Sieve of Eratosthenes
        onProgress(0.05f)
        val sieveScore = runSieve()
        list.add(SubScore("Sieve of Eratosthenes", sieveScore, "M-elements/s", ScoreNormalizer.normalize(sieveScore, 100.0, 500.0, false)))

        // 3. Merge Sort
        onProgress(0.10f)
        val mergeSortScore = runMergeSort()
        list.add(SubScore("Merge Sort", mergeSortScore, "M-elems/s", ScoreNormalizer.normalize(mergeSortScore, 10.0, 50.0, false)))

        // 4. Quick Sort
        onProgress(0.15f)
        val quickSortScore = runQuickSort()
        list.add(SubScore("Quick Sort", quickSortScore, "M-elems/s", ScoreNormalizer.normalize(quickSortScore, 12.0, 60.0, false)))

        // 5. AES-256 Encryption
        onProgress(0.20f)
        val aesScore = runAes()
        list.add(SubScore("AES-256 Encryption", aesScore, "MB/s", ScoreNormalizer.normalize(aesScore, 200.0, 1000.0, false)))

        // 6. SHA-256 Hashing
        onProgress(0.25f)
        val shaScore = runSha()
        list.add(SubScore("SHA-256 Hashing", shaScore, "MB/s", ScoreNormalizer.normalize(shaScore, 150.0, 750.0, false)))

        // 7. Mandelbrot (FP64)
        onProgress(0.30f)
        val mandelScore = runMandelbrot()
        list.add(SubScore("FP64 Mandelbrot", mandelScore, "M-px/s", ScoreNormalizer.normalize(mandelScore, 10.0, 50.0, false)))

        // 8. Matrix Multiply (DGEMM)
        onProgress(0.35f)
        val matrixScore = runMatrixMultiply()
        list.add(SubScore("Matrix Multiply (DGEMM)", matrixScore, "M-flops", ScoreNormalizer.normalize(matrixScore, 1000.0, 5000.0, false)))

        // 9. String Processing
        onProgress(0.40f)
        val stringScore = runStringProcessing()
        list.add(SubScore("String Processing", stringScore, "ops/s", ScoreNormalizer.normalize(stringScore, 20.0, 100.0, false)))

        // 10. Deflate Compression
        onProgress(0.45f)
        val deflateScore = runDeflate()
        list.add(SubScore("Deflate Compression", deflateScore, "MB/s", ScoreNormalizer.normalize(deflateScore, 25.0, 125.0, false)))

        // 11. Regex Engine Stress
        onProgress(0.50f)
        val regexScore = runRegex()
        list.add(SubScore("Regex Engine Stress", regexScore, "matches/s", ScoreNormalizer.normalize(regexScore, 500.0, 2500.0, false)))

        // 12. Binary Search
        onProgress(0.55f)
        val binSearchScore = runBinarySearch()
        list.add(SubScore("Binary Search", binSearchScore, "M-searches/s", ScoreNormalizer.normalize(binSearchScore, 5.0, 25.0, false)))

        // 13. Linked List Traversal
        onProgress(0.60f)
        val linkedListScore = runLinkedListTraversal()
        list.add(SubScore("Linked List Traversal", linkedListScore, "M-chases/s", ScoreNormalizer.normalize(linkedListScore, 10.0, 50.0, false)))

        // 14. Bitwise Operations
        onProgress(0.65f)
        val bitwiseScore = runBitwiseOps()
        list.add(SubScore("Bitwise Operations", bitwiseScore, "M-ops/s", ScoreNormalizer.normalize(bitwiseScore, 100.0, 500.0, false)))

        // 15. Prime Factorization
        onProgress(0.70f)
        val factorScore = runPrimeFactorization()
        list.add(SubScore("Prime Factorization", factorScore, "k-primes/s", ScoreNormalizer.normalize(factorScore, 10.0, 50.0, false)))

        // 16. CRC32 Checksum
        onProgress(0.75f)
        val crcScore = runCrc32()
        list.add(SubScore("CRC32 Checksum", crcScore, "MB/s", ScoreNormalizer.normalize(crcScore, 300.0, 1500.0, false)))

        // 17. Base64 Encode/Decode
        onProgress(0.80f)
        val base64Score = runBase64()
        list.add(SubScore("Base64 Encode/Decode", base64Score, "MB/s", ScoreNormalizer.normalize(base64Score, 100.0, 500.0, false)))

        // 18. Huffman Coding
        onProgress(0.85f)
        val huffmanScore = runHuffman()
        list.add(SubScore("Huffman Coding", huffmanScore, "k-ops/s", ScoreNormalizer.normalize(huffmanScore, 50.0, 250.0, false)))

        // 19. N-Queens Solver
        onProgress(0.90f)
        val nqueensScore = runNQueens()
        list.add(SubScore("N-Queens Solver (N=12)", nqueensScore, "solves/s", ScoreNormalizer.normalize(nqueensScore, 5.0, 25.0, false)))

        // 20. Ray-Plane Intersection
        onProgress(0.95f)
        val rayPlaneScore = runRayPlaneIntersection()
        list.add(SubScore("Ray-Plane Intersection", rayPlaneScore, "M-rays/s", ScoreNormalizer.normalize(rayPlaneScore, 5.0, 25.0, false)))

        onProgress(1.00f)
        list
    }

    // 1. Fibonacci
    private fun runFibonacci(): Double {
        val startTime = System.nanoTime()
        var sum = 0L
        for (i in 0 until 50_000) {
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
        val operations = 4.5e6
        return (operations / elapsed) / 1e6
    }

    // 2. Sieve of Eratosthenes
    private fun runSieve(): Double {
        val startTime = System.nanoTime()
        val limit = 2_000_000
        val isPrime = BooleanArray(limit + 1) { true }
        isPrime[0] = false; isPrime[1] = false
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

    // 3. Merge Sort
    private fun runMergeSort(): Double {
        val size = 20000
        val array = LongArray(size) { (it * 31 + 17).toLong() xor (it * 97).toLong() }
        val temp = LongArray(size)
        val startTime = System.nanoTime()
        for (pass in 0 until 5) {
            val copy = array.clone()
            mergeSort(copy, temp, 0, size - 1)
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (size.toDouble() * 5) / elapsed / 1e6
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

    // 4. Quick Sort
    private fun runQuickSort(): Double {
        val size = 20000
        val array = LongArray(size) { (it * 31 + 17).toLong() xor (it * 97).toLong() }
        val startTime = System.nanoTime()
        for (pass in 0 until 5) {
            val copy = array.clone()
            quickSort(copy, 0, size - 1)
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (size.toDouble() * 5) / elapsed / 1e6
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

    // 5. AES Encryption
    private fun runAes(): Double {
        val data = ByteArray(64 * 1024) { (it % 256).toByte() }
        val keyBytes = ByteArray(32) { (it + 5).toByte() }
        val ivBytes = ByteArray(16) { (it + 7).toByte() }
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val iv = IvParameterSpec(ivBytes)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
        
        val startTime = System.nanoTime()
        var outputLength = 0L
        for (i in 0 until 20) {
            val encrypted = cipher.doFinal(data)
            outputLength += encrypted.size
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (64.0 * 1024.0 * 20.0 / (1024.0 * 1024.0)) / elapsed
    }

    // 6. SHA Hashing
    private fun runSha(): Double {
        val data = ByteArray(1 * 1024 * 1024) { (it % 256).toByte() }
        val digest = MessageDigest.getInstance("SHA-256")
        
        val startTime = System.nanoTime()
        var hashSum = 0L
        for (i in 0 until 20) {
            digest.reset()
            val hash = digest.digest(data)
            hashSum += hash[0]
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 20.0 / elapsed
    }

    // 7. Mandelbrot
    private fun runMandelbrot(): Double {
        val width = 256
        val height = 256
        val maxIter = 100
        val startTime = System.nanoTime()
        var insideCount = 0
        for (y in 0 until height) {
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
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (width * height / 1e6) / elapsed
    }

    // 8. Matrix Multiply
    private fun runMatrixMultiply(): Double {
        val size = 128
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
        return (flops / elapsed) / 1e6
    }

    // 9. String Processing
    private fun runStringProcessing(): Double {
        val baseStr = "The quick brown fox jumps over the lazy dog. 1234567890!@#$%\n"
        val sb = StringBuilder()
        for (i in 0 until 100) sb.append(baseStr)
        val content = sb.toString()
        val startTime = System.nanoTime()
        var matchCount = 0
        for (i in 0 until 500) {
            val replaced = content.replace("fox", "cat")
            val split = replaced.split(" ")
            matchCount += split.size
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 500.0 / elapsed
    }

    // 10. Deflate Compression
    private fun runDeflate(): Double {
        val random = Random(42)
        val input = ByteArray(256 * 1024)
        random.nextBytes(input)
        val compressBuffer = ByteArray(512 * 1024)
        
        val startTime = System.nanoTime()
        var bytesCompressed = 0L
        val deflater = Deflater()
        for (i in 0 until 5) {
            deflater.setInput(input)
            deflater.finish()
            val compSize = deflater.deflate(compressBuffer)
            bytesCompressed += compSize
            deflater.reset()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (256.0 * 1024.0 * 5.0 / (1024.0 * 1024.0)) / elapsed
    }

    // 11. Regex Engine Stress
    private fun runRegex(): Double {
        val text = "User: Alice (id: 12345) Email: alice@example.com Phone: +1-555-0199 " +
                   "User: Bob (id: 67890) Email: bob@site.org Phone: 555-0100"
        val pattern = Pattern.compile("(?i)User:\\s+(\\w+)\\s+\\(id:\\s+(\\d+)\\)\\s+Email:\\s+(\\S+)")
        val startTime = System.nanoTime()
        var matches = 0
        for (i in 0 until 1000) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                matches++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return matches.toDouble() / elapsed
    }

    // 12. Binary Search
    private fun runBinarySearch(): Double {
        val size = 50000
        val array = IntArray(size) { it }
        val random = Random(42)
        val targets = IntArray(size) { random.nextInt(size) }
        val startTime = System.nanoTime()
        var hitCount = 0
        for (pass in 0 until 20) {
            for (i in 0 until size) {
                val index = array.binarySearch(targets[i])
                if (index >= 0) hitCount++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (size.toDouble() * 20) / elapsed / 1e6
    }

    // 13. Linked List Traversal
    private fun runLinkedListTraversal(): Double {
        val size = 100000
        val nextIndices = IntArray(size) { (it + 1) % size }
        // Shuffle to break prefetcher
        val random = Random(42)
        for (i in size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = nextIndices[i]
            nextIndices[i] = nextIndices[j]
            nextIndices[j] = temp
        }
        var p = 0
        val startTime = System.nanoTime()
        for (i in 0 until 2_000_000) {
            p = nextIndices[p]
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 2.0e6 / elapsed / 1e6
    }

    // 14. Bitwise Operations
    private fun runBitwiseOps(): Double {
        val startTime = System.nanoTime()
        var result = 0xAA55AA55L
        for (i in 0 until 50_000_000) {
            result = result xor (i.toLong() shl 3)
            result = result and 0xFFFFFFFFL
            result = result or (result ushr 5)
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 50.0e6 / elapsed / 1e6
    }

    // 15. Prime Factorization
    private fun runPrimeFactorization(): Double {
        val startTime = System.nanoTime()
        var factorCount = 0
        // Factorize a set of numbers up to 1,000,000
        for (num in 900_000 until 901_000) {
            var n = num
            var i = 2
            while (i * i <= n) {
                while (n % i == 0) {
                    factorCount++
                    n /= i
                }
                i++
            }
            if (n > 1) factorCount++
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 1000.0 / elapsed / 1000.0
    }

    // 16. CRC32 Checksum
    private fun runCrc32(): Double {
        val data = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val crc = CRC32()
        val startTime = System.nanoTime()
        for (pass in 0 until 50) {
            crc.reset()
            crc.update(data)
            val v = crc.value
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (1.0 * 50) / elapsed
    }

    // 17. Base64 Encode/Decode
    private fun runBase64(): Double {
        val data = ByteArray(256 * 1024) { (it % 256).toByte() }
        val startTime = System.nanoTime()
        for (pass in 0 until 20) {
            val encoded = Base64.encode(data, Base64.DEFAULT)
            val decoded = Base64.decode(encoded, Base64.DEFAULT)
            val first = decoded[0]
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (256.0 * 1024.0 * 20.0 / (1024.0 * 1024.0)) / elapsed
    }

    // 18. Huffman Coding
    private fun runHuffman(): Double {
        class HuffmanNode(val freq: Int, val char: Char?, val left: HuffmanNode? = null, val right: HuffmanNode? = null) : Comparable<HuffmanNode> {
            override fun compareTo(other: HuffmanNode): Int = this.freq - other.freq
        }
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore."
        val startTime = System.nanoTime()
        var runCount = 0
        for (pass in 0 until 500) {
            val freqs = text.groupingBy { it }.eachCount()
            val pq = PriorityQueue<HuffmanNode>()
            freqs.forEach { (char, freq) -> pq.add(HuffmanNode(freq, char)) }
            while (pq.size > 1) {
                val l = pq.poll()!!
                val r = pq.poll()!!
                pq.add(HuffmanNode(l.freq + r.freq, null, l, r))
            }
            val root = pq.poll()
            if (root != null) runCount++
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return runCount.toDouble() / elapsed / 1000.0
    }

    // 19. N-Queens Solver
    private fun runNQueens(): Double {
        var solutionsCount = 0
        val n = 12
        val cols = BooleanArray(n)
        val diag1 = BooleanArray(2 * n)
        val diag2 = BooleanArray(2 * n)
        
        fun solve(row: Int) {
            if (row == n) {
                solutionsCount++
                return
            }
            for (col in 0 until n) {
                if (!cols[col] && !diag1[row + col] && !diag2[row - col + n]) {
                    cols[col] = true; diag1[row + col] = true; diag2[row - col + n] = true
                    solve(row + 1)
                    cols[col] = false; diag1[row + col] = false; diag2[row - col + n] = false
                }
            }
        }
        val startTime = System.nanoTime()
        for (pass in 0 until 2) {
            solve(0)
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return 2.0 / elapsed
    }

    // 20. Ray-Plane Intersection
    private fun runRayPlaneIntersection(): Double {
        val count = 200000
        val ox = FloatArray(count) { it.toFloat() * 0.001f }
        val oy = FloatArray(count) { it.toFloat() * 0.002f }
        val oz = FloatArray(count) { it.toFloat() * 0.003f }
        val dx = FloatArray(count) { 0f }; val dy = FloatArray(count) { 0f }; val dz = FloatArray(count) { 1f }
        
        // Plane: dot(P - A, N) = 0
        val px = 0f; val py = 0f; val pz = 10f
        val nx = 0f; val ny = 0f; val nz = -1f
        
        var intersectCount = 0
        val startTime = System.nanoTime()
        for (pass in 0 until 5) {
            for (i in 0 until count) {
                val denom = dx[i] * nx + dy[i] * ny + dz[i] * nz
                if (Math.abs(denom) > 1e-6) {
                    val t = ((px - ox[i]) * nx + (py - oy[i]) * ny + (pz - oz[i]) * nz) / denom
                    if (t >= 0) {
                        intersectCount++
                    }
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (count.toDouble() * 5) / elapsed / 1e6
    }
}
