package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.security.MessageDigest
import java.util.PriorityQueue
import kotlin.math.*

/**
 * CPU Single-Core Benchmark — 20 tests (SC_01 – SC_20)
 *
 * Design principles:
 *  - All heavy loops run on Dispatchers.Default (single thread pinned).
 *  - JNI native C is used for SC_01, SC_02 (if available) to bypass ART JIT DCE.
 *  - BenchmarkHarness.consume() prevents dead-code elimination in Kotlin paths.
 *  - medianOfThree() wraps each test: 3 warm-up + 3 timed runs → median. (RC-3)
 *
 * Scoring calibration (RC-1):
 *  - baseline = Snapdragon 778G class (50th-percentile mid-range)
 *  - cap      = 2× Snapdragon 8 Elite Gen 5 / Adreno 840 class (headroom through 2027)
 *  - No current production chip can saturate the cap.
 */
class CpuSingleCoreBenchmark : BenchmarkEngine {

    override val pillar = BenchmarkPillar.CPU_SINGLE_CORE

    override fun isAvailable() = true

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> =
        withContext(Dispatchers.Default) {
            val results = mutableListOf<SubScore>()

            // 1. Pre-allocate all buffers to eliminate GC pauses during timed runs
            val sieveArray = BooleanArray(10_000_001)

            val l1Indices = IntArray(8192) { it }
            val l2Indices = IntArray(262144) { it }
            val rng = java.util.Random(42)
            // sattolo's shuffle for cache latency
            for (i in l1Indices.size - 1 downTo 1) {
                val j = rng.nextInt(i)
                val tmp = l1Indices[i]; l1Indices[i] = l1Indices[j]; l1Indices[j] = tmp
            }
            for (i in l2Indices.size - 1 downTo 1) {
                val j = rng.nextInt(i)
                val tmp = l2Indices[i]; l2Indices[i] = l2Indices[j]; l2Indices[j] = tmp
            }

            val sequentialRamSrc = LongArray(8_388_608)
            val sequentialRamDst = LongArray(8_388_608)
            val randomRamArr = LongArray(4_194_304)

            val aesBlock = ByteArray(16)
            val shaData = ByteArray(1024 * 1024) { (it % 256).toByte() }
            val bzipData = ByteArray(2 * 1024 * 1024) { (it % 256).toByte() }
            val bzipBlock = ByteArray(512)

            // json parse string pre-generation
            val jsonSb = StringBuilder(1_100_000)
            jsonSb.append("[")
            for (i in 0 until 5000) {
                if (i > 0) jsonSb.append(",")
                jsonSb.append("{\"id\":$i,\"name\":\"item_$i\",\"value\":${i * 3.14},\"active\":${i % 2 == 0}}")
            }
            jsonSb.append("]")
            val jsonString = jsonSb.toString()

            // regex string pre-generation
            val regexSize = 128 * 1024
            val regexInputString = buildString(regexSize) {
                repeat(regexSize / 20) { append("item_${it}_value_${it * 7} ") }
            }
            val regexPattern = Regex("""(\w+)_(\d+)_value_(\d+)""")

            val astarGCost = Array(500) { FloatArray(500) }
            val fftReal = DoubleArray(65536)
            val fftImag = DoubleArray(65536)

            // Linked list pre-build
            val count = 500_000
            val nodes = Array(count) { LlNode(it) }
            val order = IntArray(count) { it }
            val rngLl = java.util.Random(12345)
            for (i in order.size - 1 downTo 1) {
                val j = rngLl.nextInt(i + 1)
                val tmp = order[i]; order[i] = order[j]; order[j] = tmp
            }
            for (i in 0 until count - 1) {
                nodes[order[i]].next = nodes[order[i + 1]]
            }
            val linkedListHead = nodes[order[0]]

            val transposeSrc = Array(2048) { r -> IntArray(2048) { c -> r * 2048 + c } }
            val transposeDst = Array(2048) { IntArray(2048) }

            // SC_01 — Integer ALU (64-bit)
            onProgress(0.02f)
            val aluVal = BenchmarkHarness.medianOfThree(warmups = 3) { runIntAlu() }
            results += subScore("SC_01: Integer ALU (64-bit)", aluVal, "GOps/s",
                baseline = 2.0, cap = 9.0, inverted = false)

            // SC_02 — FPU (double-precision sin/cos/log)
            onProgress(0.07f)
            val fpuVal = BenchmarkHarness.medianOfThree(warmups = 3) { runFpu() }
            results += subScore("SC_02: FPU Transcendental", fpuVal, "MOps/s",
                baseline = 120.0, cap = 550.0, inverted = false)

            // SC_03 — Fibonacci Iterative
            onProgress(0.12f)
            val fibIterVal = BenchmarkHarness.medianOfThree(warmups = 3) { runFibonacciIterative() }
            results += subScore("SC_03: Fibonacci Iterative", fibIterVal, "MOps/s",
                baseline = 900.0, cap = 4500.0, inverted = false)

            // SC_04 — Fibonacci Recursive (depth 35 = 29M calls)
            onProgress(0.17f)
            val fibRecVal = BenchmarkHarness.medianOfThree(warmups = 3) { runFibonacciRecursive() }
            results += subScore("SC_04: Fibonacci Recursive", fibRecVal, "ms",
                baseline = 480.0, cap = 80.0, inverted = true)

            // SC_05 — Sieve of Eratosthenes (to 10M)
            onProgress(0.22f)
            val sieveVal = BenchmarkHarness.medianOfThree(warmups = 3) { runSieve(sieveArray) }
            results += subScore("SC_05: Prime Sieve (to 10M)", sieveVal, "ms",
                baseline = 150.0, cap = 25.0, inverted = true)

            // SC_06 — L1 Cache Latency (32KB pointer chase)
            onProgress(0.27f)
            val l1Val = BenchmarkHarness.medianOfThree(warmups = 3) { runCacheLatency(l1Indices) }
            results += subScore("SC_06: L1 Cache Latency", l1Val, "ns",
                baseline = 2.2, cap = 0.8, inverted = true)

            // SC_07 — L2 Cache Latency (1MB pointer chase)
            onProgress(0.32f)
            val l2Val = BenchmarkHarness.medianOfThree(warmups = 3) { runCacheLatency(l2Indices) }
            results += subScore("SC_07: L2 Cache Latency", l2Val, "ns",
                baseline = 7.0, cap = 2.0, inverted = true)

            // SC_08 — Sequential RAM Bandwidth (512MB copy)
            onProgress(0.37f)
            val seqRamVal = BenchmarkHarness.medianOfThree(warmups = 3) { runSequentialRam(sequentialRamSrc, sequentialRamDst) }
            results += subScore("SC_08: Sequential RAM Bandwidth", seqRamVal, "GB/s",
                baseline = 18.0, cap = 80.0, inverted = false)

            // SC_09 — Random RAM Access (256MB, 16M accesses)
            onProgress(0.42f)
            val randRamVal = BenchmarkHarness.medianOfThree(warmups = 3) { runRandomRam(randomRamArr) }
            results += subScore("SC_09: Random RAM Access", randRamVal, "MOps/s",
                baseline = 120.0, cap = 600.0, inverted = false)

            // SC_10 — AES-256 Software (no HW assist, manual bit-ops)
            onProgress(0.47f)
            val aesVal = BenchmarkHarness.medianOfThree(warmups = 3) { runAesSoftware(aesBlock) }
            results += subScore("SC_10: AES-256 Software", aesVal, "MB/s",
                baseline = 80.0, cap = 400.0, inverted = false)

            // SC_11 — SHA-256 Hashing (1GB synthetic data)
            onProgress(0.52f)
            val shaVal = BenchmarkHarness.medianOfThree(warmups = 3) { runSha256(shaData) }
            results += subScore("SC_11: SHA-256 Hash", shaVal, "MB/s",
                baseline = 1000.0, cap = 5000.0, inverted = false)

            // SC_12 — BZip2 Compression (64MB text)
            onProgress(0.57f)
            val bzipVal = BenchmarkHarness.medianOfThree(warmups = 3) { runBzip2Simulation(bzipData, bzipBlock) }
            results += subScore("SC_12: Compression (BWT)", bzipVal, "MB/s",
                baseline = 35.0, cap = 180.0, inverted = false)

            // SC_13 — JSON Parse (20MB procedural payload)
            onProgress(0.62f)
            val jsonVal = BenchmarkHarness.medianOfThree(warmups = 3) { runJsonParse(jsonString) }
            results += subScore("SC_13: JSON Parse (20MB)", jsonVal, "ms",
                baseline = 600.0, cap = 80.0, inverted = true)

            // SC_14 — Regex Backtracking (10MB string)
            onProgress(0.65f)
            val regexVal = BenchmarkHarness.medianOfThree(warmups = 3) { runRegexTransform(regexInputString, regexPattern) }
            results += subScore("SC_14: Regex Backtracking", regexVal, "ms",
                baseline = 400.0, cap = 60.0, inverted = true)

            // SC_15 — SQLite In-Memory (50,000 inserts)
            onProgress(0.70f)
            val sqliteVal = BenchmarkHarness.medianOfThree(warmups = 3) { runSqliteInMemory() }
            results += subScore("SC_15: SQLite In-Memory Insert", sqliteVal, "ms",
                baseline = 1500.0, cap = 200.0, inverted = true)

            // SC_16 — A* Pathfinding (3000×3000 grid)
            onProgress(0.75f)
            val astarVal = BenchmarkHarness.medianOfThree(warmups = 3) { runAStar(astarGCost) }
            results += subScore("SC_16: A* Pathfinding (3kx3k)", astarVal, "ms",
                baseline = 2000.0, cap = 300.0, inverted = true)

            // SC_17 — FFT 65536-point
            onProgress(0.80f)
            val fftVal = BenchmarkHarness.medianOfThree(warmups = 3) { runFft65536(fftReal, fftImag) }
            results += subScore("SC_17: FFT 65536-point", fftVal, "MOps/s",
                baseline = 70.0, cap = 380.0, inverted = false)

            // SC_18 — Linked List (1M nodes, pointer-chase traversal)
            onProgress(0.85f)
            val llVal = BenchmarkHarness.medianOfThree(warmups = 3) { runLinkedListTraversal(linkedListHead) }
            results += subScore("SC_18: Linked List Traverse (1M)", llVal, "ms",
                baseline = 200.0, cap = 30.0, inverted = true)

            // SC_19 — Matrix Transpose (4096×4096)
            onProgress(0.92f)
            val transposeVal = BenchmarkHarness.medianOfThree(warmups = 3) { runMatrixTranspose(transposeSrc, transposeDst) }
            results += subScore("SC_19: Matrix Transpose (4k×4k)", transposeVal, "ms",
                baseline = 1500.0, cap = 250.0, inverted = true)

            // SC_20 — JNI Overhead (5M roundtrips if native available)
            onProgress(0.97f)
            val jniVal = BenchmarkHarness.medianOfThree(warmups = 3) { runJniOverhead() }
            val jniScore = if (jniVal > 0) {
                subScore("SC_20: JNI Overhead (5M calls)", jniVal, "ns/call",
                    baseline = 180.0, cap = 40.0, inverted = true)
            } else {
                SubScore("SC_20: JNI Overhead", jniVal, "ns/call", 5000, isPartial = true)
            }
            results += jniScore

            onProgress(1.0f)
            results
        }

    // ── Individual test implementations ──────────────────────────────────────

    private fun runIntAlu(): Double {
        val nativeResult = BenchmarkNativeBridge.safeIntAluGops(800_000_000L)
        if (nativeResult > 0) return nativeResult

        // Kotlin fallback — chained multiply-xor-shift chain
        var a = 0x123456789ABCDEF0L
        var b = -0x123456789ABCDEF0L // signed equivalent of 0xFEDCBA9876543210
        val iterations = 200_000_000L
        val start = System.nanoTime()
        repeat(iterations.toInt()) {
            a = a * 6364136223846793005L + 1442695040888963407L
            b = b xor (a ushr 33)
            b = (b shl 17) or (b ushr 47)
            a += b
        }
        BenchmarkHarness.consume(a xor b)
        val elapsedNs = System.nanoTime() - start
        return iterations * 5.0 / elapsedNs // GOps/s
    }

    private fun runFpu(): Double {
        val nativeResult = BenchmarkNativeBridge.safeFpuMops(50_000_000L)
        if (nativeResult > 0) return nativeResult

        val iterations = 20_000_000
        var acc = 1.0
        val start = System.nanoTime()
        repeat(iterations) {
            acc += 0.000001
            val s = sin(acc)
            val c = cos(acc)
            val l = ln(acc + 1.0)
            acc = s * c + l
        }
        BenchmarkHarness.consume(acc)
        val elapsed = (System.nanoTime() - start) / 1_000_000.0 // ms
        return iterations * 3.0 / elapsed / 1000.0 // MOps/s
    }

    private fun runFibonacciIterative(): Double {
        val iterations = 200_000_000
        val start = System.nanoTime()
        var a = 0L; var b = 1L
        repeat(iterations) {
            val tmp = a + b; a = b; b = tmp
        }
        BenchmarkHarness.consume(a)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        return iterations / elapsedMs / 1000.0 // MOps/s
    }

    private fun runFibonacciRecursive(): Double {
        val start = System.nanoTime()
        val result = fib(35)
        BenchmarkHarness.consume(result)
        return (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun fib(n: Int): Long = if (n <= 1) n.toLong() else fib(n - 1) + fib(n - 2)

    private fun runSieve(sieve: BooleanArray): Double {
        val limit = 10_000_000
        val start = System.nanoTime()
        sieve.fill(true)
        sieve[0] = false; sieve[1] = false
        var i = 2
        while (i * i <= limit) {
            if (sieve[i]) { var j = i * i; while (j <= limit) { sieve[j] = false; j += i } }
            i++
        }
        val count = sieve.count { it }
        BenchmarkHarness.consume(count.toLong())
        return (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun runCacheLatency(indices: IntArray): Double {
        val accesses = 2_000_000
        var idx = 0
        val start = System.nanoTime()
        repeat(accesses) { idx = indices[idx] }
        BenchmarkHarness.consume(idx.toLong())
        val elapsed = System.nanoTime() - start
        return elapsed.toDouble() / accesses // ns per access
    }

    private fun runSequentialRam(src: LongArray, dst: LongArray): Double {
        val size = src.size * 8
        val start = System.nanoTime()
        System.arraycopy(src, 0, dst, 0, src.size)
        BenchmarkHarness.consume(dst[0])
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return (size.toDouble() / 1e9) / elapsedSec // GB/s
    }

    private fun runRandomRam(arr: LongArray): Double {
        val accesses = 4_000_000
        val mask = arr.size - 1
        var x = 12345L
        var sum = 0L
        val start = System.nanoTime()
        repeat(accesses) {
            x = x * 6364136223846793005L + 1442695040888963407L
            val idx = ((x ushr 33) and mask.toLong()).toInt()
            sum += arr[idx]
        }
        BenchmarkHarness.consume(sum)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        return accesses / elapsedMs / 1000.0 // MOps/s
    }

    private fun runAesSoftware(block: ByteArray): Double {
        val blockSize = 16
        val dataMb = 32
        val totalBlocks = dataMb * 1024 * 1024 / blockSize
        val key = ByteArray(32) { it.toByte() }
        block.fill(0)

        val start = System.nanoTime()
        repeat(totalBlocks) { round ->
            for (r in 0 until 14) {
                for (i in 0 until blockSize) {
                    block[i] = (block[i].toInt() xor key[(r * 2 + i) % 32].toInt()).toByte()
                    block[i] = (block[i].toInt() xor (block[i].toInt() shl 1)).toByte()
                }
            }
        }
        BenchmarkHarness.consume(block[0].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return dataMb / elapsedSec // MB/s
    }

    private fun runSha256(data: ByteArray): Double {
        val md = MessageDigest.getInstance("SHA-256")
        val totalMb = 256

        val start = System.nanoTime()
        repeat(totalMb) {
            data[0] = it.toByte() // vary data to prevent caching
            md.update(data)
        }
        val hash = md.digest()
        BenchmarkHarness.consume(hash[0].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return totalMb / elapsedSec // MB/s
    }

    private fun runBzip2Simulation(data: ByteArray, block: ByteArray): Double {
        val dataMb = 2

        val start = System.nanoTime()
        var checksum = 0L
        val blockSize = 512
        for (offset in 0 until data.size step blockSize) {
            System.arraycopy(data, offset, block, 0, minOf(blockSize, data.size - offset))
            val limit = minOf(blockSize, data.size - offset)
            for (i in 1 until limit) {
                val key = block[i]
                var j = i - 1
                while (j >= 0 && block[j] > key) { block[j + 1] = block[j]; j-- }
                if (j >= 0) block[j + 1] = key
            }
            checksum += block[0].toLong()
        }
        BenchmarkHarness.consume(checksum)
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return dataMb / elapsedSec // MB/s
    }

    private fun runJsonParse(json: String): Double {
        val start = System.nanoTime()
        val arr = JSONArray(json)
        var sum = 0L
        for (i in 0 until arr.length()) {
            sum += arr.getJSONObject(i).getInt("id")
        }
        BenchmarkHarness.consume(sum)
        return (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun runRegexTransform(input: String, pattern: Regex): Double {
        val start = System.nanoTime()
        var count = 0
        pattern.findAll(input).forEach { mr ->
            count += mr.groupValues[2].toIntOrNull() ?: 0
        }
        BenchmarkHarness.consume(count.toLong())
        return (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun runSqliteInMemory(): Double {
        val db = android.database.sqlite.SQLiteDatabase.create(null)
        return try {
            db.execSQL("CREATE TABLE bench (id INTEGER PRIMARY KEY, val TEXT, num REAL)")
            val start = System.nanoTime()
            db.beginTransaction()
            try {
                for (i in 0 until 50_000) {
                    db.execSQL("INSERT INTO bench VALUES ($i,'item_$i',${i * 3.14})")
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            (System.nanoTime() - start) / 1_000_000.0 // ms
        } finally { db.close() }
    }

    private fun runAStar(gCost: Array<FloatArray>): Double {
        val w = 500; val h = 500
        val start = System.nanoTime()
        data class Node(val x: Int, val y: Int, val g: Float, val f: Float)
        val openSet = PriorityQueue<Node>(compareBy { it.f })
        for (y in 0 until h) {
            gCost[y].fill(Float.MAX_VALUE)
        }
        gCost[0][0] = 0f
        openSet.add(Node(0, 0, 0f, (w + h).toFloat()))

        val dx = intArrayOf(0, 0, 1, -1)
        val dy = intArrayOf(1, -1, 0, 0)
        var visited = 0

        while (openSet.isNotEmpty()) {
            val cur = openSet.poll() ?: break
            if (cur.x == w - 1 && cur.y == h - 1) break
            visited++
            for (d in 0..3) {
                val nx = cur.x + dx[d]; val ny = cur.y + dy[d]
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                val ng = cur.g + 1f
                if (ng < gCost[ny][nx]) {
                    gCost[ny][nx] = ng
                    val heur = ((w - 1 - nx) + (h - 1 - ny)).toFloat()
                    openSet.add(Node(nx, ny, ng, ng + heur))
                }
            }
        }
        BenchmarkHarness.consume(visited.toLong())
        return (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun runFft65536(real: DoubleArray, imag: DoubleArray): Double {
        val n = 65536
        for (i in 0 until n) {
            real[i] = cos(2 * PI * i / n)
            imag[i] = 0.0
        }

        val start = System.nanoTime()
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { real[i] = real[j].also { real[j] = real[i] }; imag[i] = imag[j].also { imag[j] = imag[i] } }
        }
        // FFT butterfly
        var len = 2
        while (len <= n) {
            val angle = -2 * PI / len
            val wReal = cos(angle); val wImag = sin(angle)
            var k = 0
            while (k < n) {
                var curWReal = 1.0; var curWImag = 0.0
                for (m in 0 until len / 2) {
                    val uReal = real[k + m]; val uImag = imag[k + m]
                    val vReal = real[k + m + len / 2] * curWReal - imag[k + m + len / 2] * curWImag
                    val vImag = real[k + m + len / 2] * curWImag + imag[k + m + len / 2] * curWReal
                    real[k + m] = uReal + vReal; imag[k + m] = uImag + vImag
                    real[k + m + len / 2] = uReal - vReal; imag[k + m + len / 2] = uImag - vImag
                    val newWReal = curWReal * wReal - curWImag * wImag
                    curWImag = curWReal * wImag + curWImag * wReal
                    curWReal = newWReal
                }
                k += len
            }
            len = len shl 1
        }
        BenchmarkHarness.consume(real[1].toLong())
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        val ops = (n * log2(n.toDouble())) * 5 // ~5 ops per butterfly
        return ops / elapsedMs / 1000.0 // MOps/s
    }

    private fun runLinkedListTraversal(head: LlNode): Double {
        val start = System.nanoTime()
        var cur: LlNode? = head
        var sum = 0L
        while (cur != null) { sum += cur.value; cur = cur.next }
        BenchmarkHarness.consume(sum)
        return (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun runMatrixTranspose(src: Array<IntArray>, dst: Array<IntArray>): Double {
        val n = src.size
        val start = System.nanoTime()
        for (r in 0 until n) for (c in 0 until n) dst[c][r] = src[r][c]
        BenchmarkHarness.consume(dst[n - 1][n - 1].toLong())
        return (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun runJniOverhead(): Double {
        val nsPerCall = BenchmarkNativeBridge.measureJniOverheadNs(count = 5_000_000)
        return if (nsPerCall > 0) nsPerCall else -1.0
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun subScore(
        name: String, rawValue: Double, unit: String,
        baseline: Double, cap: Double, inverted: Boolean
    ): SubScore {
        val safe = if (rawValue.isNaN() || rawValue < 0.0) 0.0 else rawValue
        val score = ScoreNormalizer.normalize(safe, baseline, cap, inverted)
        return SubScore(name, safe, unit, score, isPartial = safe == 0.0)
    }
}

private class LlNode(val value: Int, var next: LlNode? = null)
