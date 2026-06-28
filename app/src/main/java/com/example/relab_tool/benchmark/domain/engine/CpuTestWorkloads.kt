package com.example.relab_tool.benchmark.domain.engine

import org.json.JSONArray
import java.security.MessageDigest
import java.util.PriorityQueue
import kotlin.math.*

/**
 * CpuTestWorkloads — 20 stateless test workloads shared by both
 * CpuSingleCoreBenchmark and CpuMultiCoreBenchmark.
 *
 * Every function returns a throughput value (Double, higher = better).
 * SC mode calls each function once on a pinned core.
 * MC mode calls each function N times in parallel (one per core) and sums the results.
 *
 * This design guarantees that SC and MC scores are directly comparable:
 * both run byte-for-byte identical computation, differing only in parallelism.
 *
 * Scoring calibration:
 *  - baseline = Snapdragon 778G class (50th-percentile mid-range)
 *  - cap      = 2× Snapdragon 8 Elite Gen 5 / Adreno 840 class (headroom through 2027)
 */
object CpuTestWorkloads {

    // ── 01: Integer ALU (64-bit) ─────────────────────────────────────────────────
    /** Chained multiply-xor-shift chain. Returns: GOps/s */
    fun runIntAlu(): Double {
        val nativeResult = BenchmarkNativeBridge.safeIntAluGops(800_000_000L)
        if (nativeResult > 0) return nativeResult

        var a = 0x123456789ABCDEF0L
        var b = -0x123456789ABCDEF0L
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

    // ── 02: FPU Transcendental (sin/cos/ln) ──────────────────────────────────────
    /** Double-precision transcendental throughput. Returns: MOps/s */
    fun runFpu(): Double {
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
        val elapsed = (System.nanoTime() - start) / 1_000_000.0
        return iterations * 3.0 / elapsed / 1000.0 // MOps/s
    }

    // ── 03: Fibonacci Iterative ──────────────────────────────────────────────────
    /** Branch prediction + ALU pipeline throughput. Returns: MOps/s */
    fun runFibonacciIterative(): Double {
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

    // ── 04: Prime Sieve (to 10M) ─────────────────────────────────────────────────
    /** Sieve of Eratosthenes throughput. Returns: MOps/s (10M elements / time) */
    fun runPrimeSieve(sieve: BooleanArray): Double {
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
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return limit / elapsedSec / 1e6 // MOps/s
    }

    // ── 05: L1 Cache Throughput (32KB pointer chase) ─────────────────────────────
    /** L1 cache pointer-chase throughput. Returns: MOps/s */
    fun runL1CacheThroughput(indices: IntArray): Double {
        val accesses = 2_000_000
        var idx = 0
        val start = System.nanoTime()
        repeat(accesses) { idx = indices[idx] }
        BenchmarkHarness.consume(idx.toLong())
        val elapsedNs = System.nanoTime() - start
        // accesses / elapsedNs gives ops/ns = GOps/s; multiply by 1000 for MOps/s... no.
        // ops/ns = GOps/s; we want MOps/s = ops/ns * 1000? No, 1 GOps = 1000 MOps.
        // Actually: accesses / elapsedNs = ops_per_ns. ops_per_ns * 1e9 = ops/s. / 1e6 = MOps/s.
        // = accesses * 1e9 / elapsedNs / 1e6 = accesses * 1000.0 / elapsedNs
        return accesses * 1000.0 / elapsedNs // MOps/s
    }

    // ── 06: L2 Cache Throughput (1MB pointer chase) ──────────────────────────────
    /** L2 cache pointer-chase throughput. Returns: MOps/s */
    fun runL2CacheThroughput(indices: IntArray): Double {
        val accesses = 2_000_000
        var idx = 0
        val start = System.nanoTime()
        repeat(accesses) { idx = indices[idx] }
        BenchmarkHarness.consume(idx.toLong())
        val elapsedNs = System.nanoTime() - start
        return accesses * 1000.0 / elapsedNs // MOps/s
    }

    // ── 07: Sequential RAM Bandwidth (64MB copy) ─────────────────────────────────
    /** Sequential memory copy bandwidth. Returns: GB/s */
    fun runSequentialRam(src: LongArray, dst: LongArray): Double {
        val bytes = src.size.toLong() * 8L
        val start = System.nanoTime()
        System.arraycopy(src, 0, dst, 0, src.size)
        BenchmarkHarness.consume(dst[0])
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return (bytes / 1e9) / elapsedSec // GB/s
    }

    // ── 08: Random RAM Access (4M accesses into 32MB array) ──────────────────────
    /** Random memory access throughput. Returns: MOps/s */
    fun runRandomRam(arr: LongArray): Double {
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

    // ── 09: AES-256 Software (manual bit-ops, no HW assist) ──────────────────────
    /** Software AES-256 throughput. Returns: MB/s */
    fun runAesSoftware(block: ByteArray): Double {
        val blockSize = 16
        val dataMb = 32
        val totalBlocks = dataMb * 1024 * 1024 / blockSize
        val key = ByteArray(32) { it.toByte() }
        block.fill(0)

        val start = System.nanoTime()
        repeat(totalBlocks) {
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

    // ── 10: SHA-256 Hash (256MB) ─────────────────────────────────────────────────
    /** SHA-256 hashing throughput. Returns: MB/s */
    fun runSha256(data: ByteArray): Double {
        val md = MessageDigest.getInstance("SHA-256")
        val totalMb = 256
        val start = System.nanoTime()
        repeat(totalMb) {
            data[0] = it.toByte()
            md.update(data)
        }
        val hash = md.digest()
        BenchmarkHarness.consume(hash[0].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return totalMb / elapsedSec // MB/s
    }

    // ── 11: BWT Compression (2MB) ────────────────────────────────────────────────
    /** Burrows-Wheeler Transform simulation throughput. Returns: MB/s */
    fun runBwtCompression(data: ByteArray, block: ByteArray): Double {
        val dataMb = 2
        val blockSize = 512
        val start = System.nanoTime()
        var checksum = 0L
        for (offset in 0 until data.size step blockSize) {
            val copyLen = minOf(blockSize, data.size - offset)
            System.arraycopy(data, offset, block, 0, copyLen)
            for (i in 1 until copyLen) {
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

    // ── 12: JSON Parse (5K objects, ~1.1MB payload) ──────────────────────────────
    /** JSON parsing throughput. Returns: Kops/s */
    fun runJsonParse(jsonString: String): Double {
        val objectCount = 5000
        val start = System.nanoTime()
        val arr = JSONArray(jsonString)
        var sum = 0L
        for (i in 0 until arr.length()) {
            sum += arr.getJSONObject(i).getInt("id")
        }
        BenchmarkHarness.consume(sum)
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return objectCount / elapsedSec / 1000.0 // Kops/s
    }

    // ── 13: Regex Matching (128KB text) ──────────────────────────────────────────
    /** Regex search throughput over text. Returns: KB/s processed */
    fun runRegexMatch(input: String, pattern: Regex): Double {
        val inputKb = input.length / 1024.0
        val start = System.nanoTime()
        var count = 0
        pattern.findAll(input).forEach { mr ->
            count += mr.groupValues[2].toIntOrNull() ?: 0
        }
        BenchmarkHarness.consume(count.toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return inputKb / elapsedSec // KB/s
    }

    // ── 14: SQLite In-Memory Insert (50K rows) ──────────────────────────────────
    /** SQLite insert throughput. Returns: Kinserts/s */
    fun runSqliteInsert(): Double {
        val insertCount = 50_000
        val db = android.database.sqlite.SQLiteDatabase.create(null)
        return try {
            db.execSQL("CREATE TABLE bench (id INTEGER PRIMARY KEY, val TEXT, num REAL)")
            val start = System.nanoTime()
            db.beginTransaction()
            try {
                for (i in 0 until insertCount) {
                    db.execSQL("INSERT INTO bench VALUES ($i,'item_$i',${i * 3.14})")
                }
                db.setTransactionSuccessful()
            } finally { db.endTransaction() }
            val elapsedSec = (System.nanoTime() - start) / 1e9
            insertCount / elapsedSec / 1000.0 // Kinserts/s
        } finally { db.close() }
    }

    // ── 15: A* Pathfinding (500×500 grid) ────────────────────────────────────────
    /** A* pathfinding throughput. Returns: Knodes/s */
    fun runAStar(gCost: Array<FloatArray>): Double {
        val w = 500; val h = 500
        val gridArea = w * h
        val start = System.nanoTime()
        data class Node(val x: Int, val y: Int, val g: Float, val f: Float)
        val openSet = PriorityQueue<Node>(compareBy { it.f })
        for (y in 0 until h) { gCost[y].fill(Float.MAX_VALUE) }
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
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return gridArea / elapsedSec / 1000.0 // Knodes/s (normalized by grid area)
    }

    // ── 16: FFT 65536-point ──────────────────────────────────────────────────────
    /** Cooley-Tukey FFT butterfly throughput. Returns: MOps/s */
    fun runFft(real: DoubleArray, imag: DoubleArray): Double {
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
            if (i < j) {
                real[i] = real[j].also { real[j] = real[i] }
                imag[i] = imag[j].also { imag[j] = imag[i] }
            }
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
        val ops = (n * log2(n.toDouble())) * 5
        return ops / elapsedMs / 1000.0 // MOps/s
    }

    // ── 17: Matrix Multiply (512×512 DGEMM) ──────────────────────────────────────
    /** Dense double-precision matrix multiply. Returns: GFLOPS */
    fun runMatrixMultiply(
        a: Array<DoubleArray>,
        b: Array<DoubleArray>,
        c: Array<DoubleArray>
    ): Double {
        val size = a.size
        for (i in 0 until size) { c[i].fill(0.0) }
        val start = System.nanoTime()
        for (i in 0 until size) {
            for (k in 0 until size) {
                val aik = a[i][k]
                for (j in 0 until size) {
                    c[i][j] += aik * b[k][j]
                }
            }
        }
        BenchmarkHarness.consume(c[size - 1][size - 1].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        val flops = 2.0 * size * size * size
        return flops / elapsedSec / 1e9 // GFLOPS
    }

    // ── 18: Matrix Transpose (2048×2048) ─────────────────────────────────────────
    /** Cache-hostile matrix transpose throughput. Returns: MPix/s */
    fun runMatrixTranspose(src: Array<IntArray>, dst: Array<IntArray>): Double {
        val n = src.size
        val elements = n.toLong() * n
        val start = System.nanoTime()
        for (r in 0 until n) for (c in 0 until n) dst[c][r] = src[r][c]
        BenchmarkHarness.consume(dst[n - 1][n - 1].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return elements / elapsedSec / 1e6 // MPix/s
    }

    // ── 19: Monte Carlo Pi (50M points) ──────────────────────────────────────────
    /** RNG + FP Monte Carlo throughput. Returns: Mpts/s */
    fun runMonteCarloPi(seed: Long = 0L): Double {
        val points = 50_000_000
        val rng = java.util.Random(seed)
        val start = System.nanoTime()
        var inside = 0L
        repeat(points) {
            val x = rng.nextDouble(); val y = rng.nextDouble()
            if (x * x + y * y <= 1.0) inside++
        }
        BenchmarkHarness.consume(inside)
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return points / elapsedSec / 1e6 // Mpts/s
    }

    // ── 20: Gaussian Blur (2048×2048, radius 8) ──────────────────────────────────
    /** 1D horizontal Gaussian blur throughput. Returns: MPix/s */
    fun runGaussianBlur(img: IntArray, out: IntArray, w: Int = 2048, h: Int = 2048): Double {
        val radius = 8
        val kernel = (2 * radius + 1).let { size ->
            FloatArray(size) { i ->
                val x = i - radius
                exp(-(x * x).toFloat() / (2 * radius * radius)).toFloat()
            }
        }
        val kSum = kernel.sum()

        val start = System.nanoTime()
        for (row in 0 until h) {
            for (col in 0 until w) {
                var acc = 0f
                for (k in kernel.indices) {
                    val srcCol = (col + k - radius).coerceIn(0, w - 1)
                    acc += img[row * w + srcCol] * kernel[k]
                }
                out[row * w + col] = (acc / kSum).toInt()
            }
        }
        BenchmarkHarness.consume(out[w * h - 1].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        return (w.toLong() * h) / elapsedSec / 1e6 // MPix/s
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Generate Sattolo-shuffled index array for cache-latency pointer chasing. */
    fun generateShuffledIndices(size: Int, seed: Long = 42L): IntArray {
        val indices = IntArray(size) { it }
        val rng = java.util.Random(seed)
        for (i in size - 1 downTo 1) {
            val j = rng.nextInt(i)
            val tmp = indices[i]; indices[i] = indices[j]; indices[j] = tmp
        }
        return indices
    }

    /** Generate the 5K-object JSON payload for test 12. */
    fun generateJsonString(): String {
        val sb = StringBuilder(1_100_000)
        sb.append("[")
        for (i in 0 until 5000) {
            if (i > 0) sb.append(",")
            sb.append("{\"id\":$i,\"name\":\"item_$i\",\"value\":${i * 3.14},\"active\":${i % 2 == 0}}")
        }
        sb.append("]")
        return sb.toString()
    }

    /** Generate the 128KB regex test input for test 13. */
    fun generateRegexInput(): String {
        val size = 128 * 1024
        return buildString(size) {
            repeat(size / 20) { append("item_${it}_value_${it * 7} ") }
        }
    }

    /** Pre-compiled regex pattern for test 13. */
    val REGEX_PATTERN = Regex("""(\w+)_(\d+)_value_(\d+)""")
}
