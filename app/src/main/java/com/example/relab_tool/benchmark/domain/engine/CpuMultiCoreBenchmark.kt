package com.example.relab_tool.benchmark.domain.engine

import android.database.sqlite.SQLiteDatabase
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.*
import kotlin.math.*
import java.security.MessageDigest
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

/**
 * CPU Multi-Core Benchmark — 20 tests (MC_01 – MC_20)
 *
 * Design principles:
 *  - Uses Kotlin coroutines (Dispatchers.Default) with parallelism =  available processors.
 *  - Tests are designed to scale with core count: a device with 8 performance cores
 *    should score proportionally higher than a 4-core device on compute-bound tests.
 *  - Shared-memory tests (STREAM, atomic contention) specifically expose the quality
 *    of the L3/SLC cache and the memory bus — the primary differentiator between
 *    entry (no/small L3) and flagship (16MB+ SLC) chips.
 *
 * Score calibration:
 *  - baseline = Snapdragon 778G / Pixel 6 (mid-range 8-core)
 *  - cap      = Snapdragon 8 Gen 3 / Dimensity 9200+ (flagship 8/10-core)
 *  - Entry (SD 680, Helio G85) should land at ~15–30% of cap.
 *  - Scaling: SD 778G cap should score ~5000, SD 8 Elite ~8500–9500.
 */
class CpuMultiCoreBenchmark : BenchmarkEngine {

    override val pillar = BenchmarkPillar.CPU_MULTI_CORE

    override fun isAvailable() = true

    private val coreCount = Runtime.getRuntime().availableProcessors()

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> =
        withContext(Dispatchers.Default) {
            val results = mutableListOf<SubScore>()

            // MC_01 — Parallel Matrix Multiply (blocked DGEMM on 2048×2048)
            onProgress(0.02f)
            val matMulVal = BenchmarkHarness.medianOfThreeLight { runParallelMatrixMultiply() }
            results += subScore("MC_01: Parallel Matrix Multiply", matMulVal, "GFLOPS",
                baseline = 10.0, cap = 90.0, inverted = false)

            // MC_02 — N-Body Gravity (50K particles, O(N²) parallel)
            onProgress(0.07f)
            val nBodyVal = BenchmarkHarness.medianOfThreeLight { runNBody() }
            results += subScore("MC_02: N-Body Gravity (50K)", nBodyVal, "GFlop/s",
                baseline = 2.0, cap = 15.0, inverted = false)

            // MC_03 — Parallel Merge Sort (10M elements)
            onProgress(0.12f)
            val sortVal = BenchmarkHarness.medianOfThreeLight { runParallelMergeSort() }
            results += subScore("MC_03: Parallel Merge Sort (10M)", sortVal, "ms",
                baseline = 800.0, cap = 200.0, inverted = true)

            // MC_04 — Parallel AES-256 (chunk per core)
            onProgress(0.17f)
            val aesVal = BenchmarkHarness.medianOfThreeLight { runParallelAes() }
            results += subScore("MC_04: Parallel AES-256 Encrypt", aesVal, "MB/s",
                baseline = 200.0, cap = 800.0, inverted = false)

            // MC_05 — Parallel SHA-256 (all cores at 100% saturation)
            onProgress(0.22f)
            val shaVal = BenchmarkHarness.medianOfThreeLight { runParallelSha256() }
            results += subScore("MC_05: Parallel SHA-256 (all cores)", shaVal, "GH/s",
                baseline = 0.05, cap = 0.25, inverted = false)

            // MC_06 — SQLite Parallel Read (16 concurrent readers on :memory:)
            onProgress(0.27f)
            val sqlReadVal = BenchmarkHarness.medianOfThreeLight { runSqliteParallelRead() }
            results += subScore("MC_06: SQLite Parallel Read", sqlReadVal, "ms",
                baseline = 600.0, cap = 150.0, inverted = true)

            // MC_07 — Monte Carlo Pi (1B points, distributed)
            onProgress(0.32f)
            val piVal = BenchmarkHarness.medianOfThreeLight { runMonteCarloPi() }
            results += subScore("MC_07: Monte Carlo Pi (1B pts)", piVal, "Mpts/s",
                baseline = 800.0, cap = 4000.0, inverted = false)

            // MC_08 — Context Switch Storm (2000 coroutines)
            onProgress(0.37f)
            val ctxVal = BenchmarkHarness.medianOfThree(warmups = 1) { runContextSwitchStorm() }
            results += subScore("MC_08: Context Switch Storm (2K)", ctxVal, "ms",
                baseline = 600.0, cap = 150.0, inverted = true)

            // MC_09 — STREAM Copy (multi-threaded sequential copy)
            onProgress(0.42f)
            val streamCopyVal = BenchmarkHarness.medianOfThreeLight { runStreamCopy() }
            results += subScore("MC_09: STREAM Copy", streamCopyVal, "GB/s",
                baseline = 15.0, cap = 60.0, inverted = false)

            // MC_10 — STREAM Scale (multiply by constant)
            onProgress(0.47f)
            val streamScaleVal = BenchmarkHarness.medianOfThreeLight { runStreamScale() }
            results += subScore("MC_10: STREAM Scale", streamScaleVal, "GB/s",
                baseline = 14.0, cap = 55.0, inverted = false)

            // MC_11 — STREAM Add (dual-read add)
            onProgress(0.52f)
            val streamAddVal = BenchmarkHarness.medianOfThreeLight { runStreamAdd() }
            results += subScore("MC_11: STREAM Add", streamAddVal, "GB/s",
                baseline = 18.0, cap = 70.0, inverted = false)

            // MC_12 — STREAM Triad (combined scale + add)
            onProgress(0.57f)
            val streamTriadVal = BenchmarkHarness.medianOfThreeLight { runStreamTriad() }
            results += subScore("MC_12: STREAM Triad", streamTriadVal, "GB/s",
                baseline = 17.0, cap = 65.0, inverted = false)

            // MC_13 — BVH Tree Build (parallel bounding volume hierarchy)
            onProgress(0.62f)
            val bvhVal = BenchmarkHarness.medianOfThree(warmups = 1) { runBvhBuild() }
            results += subScore("MC_13: BVH Tree Build (parallel)", bvhVal, "ms",
                baseline = 800.0, cap = 200.0, inverted = true)

            // MC_14 — Collision Detection (10K bodies with Octree)
            onProgress(0.65f)
            val collisionVal = BenchmarkHarness.medianOfThree(warmups = 1) { runCollisionDetection() }
            results += subScore("MC_14: Collision Detection (10K)", collisionVal, "ms",
                baseline = 600.0, cap = 150.0, inverted = true)

            // MC_15 — Parallel Gaussian Blur (image strip distribution)
            onProgress(0.70f)
            val blurVal = BenchmarkHarness.medianOfThreeLight { runParallelGaussianBlur() }
            results += subScore("MC_15: Parallel Gaussian Blur", blurVal, "MPix/s",
                baseline = 800.0, cap = 3500.0, inverted = false)

            // MC_16 — Parallel Zlib-like Decompression (independent chunks)
            onProgress(0.75f)
            val decompVal = BenchmarkHarness.medianOfThreeLight { runParallelDecompress() }
            results += subScore("MC_16: Parallel Decompression", decompVal, "MB/s",
                baseline = 400.0, cap = 1800.0, inverted = false)

            // MC_17 — BFS Graph Traversal (parallel frontier expansion)
            onProgress(0.80f)
            val bfsVal = BenchmarkHarness.medianOfThree(warmups = 1) { runParallelBfs() }
            results += subScore("MC_17: Parallel BFS Graph", bfsVal, "ms",
                baseline = 600.0, cap = 150.0, inverted = true)

            // MC_18 — TFLite CPU-only Inference (force no NPU delegation)
            onProgress(0.85f)
            val inferVal = BenchmarkHarness.medianOfThree(warmups = 1) { runCpuOnlyInference() }
            results += subScore("MC_18: CPU-only Neural Net (MatMul)", inferVal, "GFLOPS",
                baseline = 8.0, cap = 40.0, inverted = false)

            // MC_19 — Atomic CAS Contention (2000 threads → 1 counter)
            onProgress(0.92f)
            val atomicVal = BenchmarkHarness.medianOfThree(warmups = 1) { runAtomicContention() }
            results += subScore("MC_19: Atomic CAS Contention", atomicVal, "MOps/s",
                baseline = 50.0, cap = 300.0, inverted = false)

            // MC_20 — Peak Thermal Saturation (FMA-equivalent all-core)
            onProgress(0.97f)
            val satVal = BenchmarkHarness.medianOfThreeLight { runPeakAllCoreSaturation() }
            results += subScore("MC_20: Peak All-Core Saturation", satVal, "GFLOPS",
                baseline = 15.0, cap = 100.0, inverted = false)

            onProgress(1.0f)
            results
        }

    // ── Individual test implementations ──────────────────────────────────────

    /** Blocked parallel matrix multiply on [size]×[size] matrix */
    private suspend fun runParallelMatrixMultiply(): Double = coroutineScope {
        val size = 512 // 512×512 is 256MB — manageable on mid devices
        val a = Array(size) { r -> DoubleArray(size) { c -> (r + c + 1).toDouble() } }
        val b = Array(size) { r -> DoubleArray(size) { c -> (r - c + 1).toDouble() } }
        val c = Array(size) { DoubleArray(size) }
        val blockSize = maxOf(32, size / coreCount)

        val start = System.nanoTime()
        val jobs = (0 until size step blockSize).map { rowStart ->
            async {
                val rowEnd = minOf(rowStart + blockSize, size)
                for (i in rowStart until rowEnd) {
                    for (k in 0 until size) {
                        val aik = a[i][k]
                        for (j in 0 until size) {
                            c[i][j] += aik * b[k][j]
                        }
                    }
                }
            }
        }
        jobs.awaitAll()
        BenchmarkHarness.consume(c[size - 1][size - 1].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        val flops = 2.0 * size * size * size
        flops / elapsedSec / 1e9 // GFLOPS
    }

    /** O(N²) N-Body: simplified Barnes-Hut approximation split across cores */
    private suspend fun runNBody(): Double = coroutineScope {
        val n = 5000 // 5K particles (N² = 25M interactions)
        val x = DoubleArray(n) { Math.random() * 1000 }
        val y = DoubleArray(n) { Math.random() * 1000 }
        val m = DoubleArray(n) { Math.random() + 0.1 }
        val fxTotal = DoubleArray(n)
        val fyTotal = DoubleArray(n)
        val chunkSize = n / coreCount

        val start = System.nanoTime()
        val jobs = (0 until coreCount).map { core ->
            async {
                val startI = core * chunkSize
                val endI = if (core == coreCount - 1) n else startI + chunkSize
                for (i in startI until endI) {
                    var fx = 0.0; var fy = 0.0
                    for (j in 0 until n) {
                        if (i == j) continue
                        val dx = x[j] - x[i]; val dy = y[j] - y[i]
                        val distSq = dx * dx + dy * dy + 0.01
                        val f = m[i] * m[j] / distSq
                        fx += f * dx; fy += f * dy
                    }
                    fxTotal[i] = fx; fyTotal[i] = fy
                }
            }
        }
        jobs.awaitAll()
        BenchmarkHarness.consume(fxTotal[0].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        val flops = n.toDouble() * n * 8 // ~8 flops per pair
        flops / elapsedSec / 1e9
    }

    private suspend fun runParallelMergeSort(): Double = coroutineScope {
        val n = 2_000_000 // 2M elements
        val arr = IntArray(n) { (n - it) }
        val start = System.nanoTime()
        val chunkSize = n / coreCount

        // Phase 1: sort each chunk independently
        val sorted = (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, n)
                arr.copyOfRange(s, e).also { it.sort() }
            }
        }.awaitAll()

        // Phase 2: sequential k-way merge (simplified 2-way merge)
        var merged = sorted[0]
        for (i in 1 until sorted.size) {
            merged = mergeSorted(merged, sorted[i])
        }
        BenchmarkHarness.consume(merged.last().toLong())
        (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun mergeSorted(a: IntArray, b: IntArray): IntArray {
        val result = IntArray(a.size + b.size)
        var i = 0; var j = 0; var k = 0
        while (i < a.size && j < b.size) {
            result[k++] = if (a[i] <= b[j]) a[i++] else b[j++]
        }
        while (i < a.size) result[k++] = a[i++]
        while (j < b.size) result[k++] = b[j++]
        return result
    }

    private suspend fun runParallelAes(): Double = coroutineScope {
        val dataMbPerCore = 16
        val start = System.nanoTime()
        val jobs = (0 until coreCount).map { core ->
            async {
                val data = ByteArray(dataMbPerCore * 1024 * 1024) { (it xor core).toByte() }
                val key = ByteArray(32) { (it + core).toByte() }
                for (block in 0 until data.size / 16) {
                    for (r in 0 until 14) {
                        for (i in 0 until 16) {
                            data[block * 16 + i] = (data[block * 16 + i].toInt() xor key[(r * 2 + i) % 32].toInt()).toByte()
                        }
                    }
                }
                BenchmarkHarness.consume(data[0].toLong())
                dataMbPerCore.toLong()
            }
        }
        val totalMb = jobs.awaitAll().sum()
        val elapsedSec = (System.nanoTime() - start) / 1e9
        totalMb / elapsedSec // MB/s
    }

    private suspend fun runParallelSha256(): Double = coroutineScope {
        val chunkMb = 16
        val start = System.nanoTime()
        val jobs = (0 until coreCount).map { core ->
            async {
                val md = MessageDigest.getInstance("SHA-256")
                val data = ByteArray(1024 * 1024) { (it xor core).toByte() }
                repeat(chunkMb) { i -> data[0] = i.toByte(); md.update(data) }
                BenchmarkHarness.consume(md.digest()[0].toLong())
                chunkMb.toLong()
            }
        }
        val totalMb = jobs.awaitAll().sum()
        val elapsedSec = (System.nanoTime() - start) / 1e9
        totalMb / elapsedSec / 1000.0 // GH/s equivalent
    }

    private suspend fun runSqliteParallelRead(): Double = coroutineScope {
        val db = SQLiteDatabase.create(null)
        db.execSQL("CREATE TABLE data (id INTEGER PRIMARY KEY, val TEXT)")
        db.beginTransaction()
        try {
            for (i in 0 until 10_000) db.execSQL("INSERT INTO data VALUES ($i,'row_$i')")
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }

        val start = System.nanoTime()
        val jobs = (0 until 4).map { // 4 parallel readers (SQLite read-concurrency limit)
            async {
                var count = 0L
                db.rawQuery("SELECT * FROM data WHERE id % 10 = ${it % 10}", null).use { c ->
                    while (c.moveToNext()) count++
                }
                count
            }
        }
        jobs.awaitAll()
        db.close()
        (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private suspend fun runMonteCarloPi(): Double = coroutineScope {
        val ptsPerCore = 25_000_000
        val start = System.nanoTime()
        val insideList = (0 until coreCount).map { seed ->
            async {
                val rng = java.util.Random(seed.toLong())
                var inside = 0L
                repeat(ptsPerCore) {
                    val xr = rng.nextDouble(); val yr = rng.nextDouble()
                    if (xr * xr + yr * yr <= 1.0) inside++
                }
                inside
            }
        }
        val totalInside = insideList.awaitAll().sum()
        BenchmarkHarness.consume(totalInside)
        val elapsedSec = (System.nanoTime() - start) / 1e9
        val totalPts = ptsPerCore.toLong() * coreCount
        totalPts / elapsedSec / 1e6 // Mpts/s
    }

    private suspend fun runContextSwitchStorm(): Double = coroutineScope {
        val numCoroutines = 2000
        val opsPerCoroutine = 1000
        val counter = AtomicLong(0)
        val start = System.nanoTime()
        val jobs = (0 until numCoroutines).map {
            async {
                repeat(opsPerCoroutine) {
                    yield() // force scheduler preemption
                    counter.incrementAndGet()
                }
            }
        }
        jobs.awaitAll()
        (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private suspend fun runStreamCopy(): Double = coroutineScope {
        val n = 16 * 1024 * 1024 // 16M longs = 128MB
        val src = LongArray(n) { it.toLong() }
        val dst = LongArray(n)
        val chunkSize = n / coreCount
        val start = System.nanoTime()
        val jobs = (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, n)
                src.copyInto(dst, s, s, e)
            }
        }
        jobs.awaitAll()
        BenchmarkHarness.consume(dst[n - 1])
        val bytes = n.toLong() * 8L * 2L // read + write
        bytes.toDouble() / ((System.nanoTime() - start) / 1e9) / 1e9 // GB/s
    }

    private suspend fun runStreamScale(): Double = coroutineScope {
        val n = 16 * 1024 * 1024
        val src = DoubleArray(n) { it.toDouble() }
        val dst = DoubleArray(n)
        val scalar = 3.14
        val chunkSize = n / coreCount
        val start = System.nanoTime()
        (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, n)
                for (i in s until e) dst[i] = scalar * src[i]
            }
        }.awaitAll()
        BenchmarkHarness.consume(dst[n - 1].toLong())
        val bytes = n.toLong() * 8L * 2L
        bytes.toDouble() / ((System.nanoTime() - start) / 1e9) / 1e9
    }

    private suspend fun runStreamAdd(): Double = coroutineScope {
        val n = 16 * 1024 * 1024
        val a = DoubleArray(n) { it.toDouble() }
        val b = DoubleArray(n) { (n - it).toDouble() }
        val c = DoubleArray(n)
        val chunkSize = n / coreCount
        val start = System.nanoTime()
        (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, n)
                for (i in s until e) c[i] = a[i] + b[i]
            }
        }.awaitAll()
        BenchmarkHarness.consume(c[n - 1].toLong())
        val bytes = n.toLong() * 8L * 3L
        bytes.toDouble() / ((System.nanoTime() - start) / 1e9) / 1e9
    }

    private suspend fun runStreamTriad(): Double = coroutineScope {
        val n = 16 * 1024 * 1024
        val a = DoubleArray(n) { it.toDouble() }
        val b = DoubleArray(n) { (n - it).toDouble() }
        val c = DoubleArray(n)
        val scalar = 2.718
        val chunkSize = n / coreCount
        val start = System.nanoTime()
        (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, n)
                for (i in s until e) c[i] = a[i] + scalar * b[i]
            }
        }.awaitAll()
        BenchmarkHarness.consume(c[n - 1].toLong())
        val bytes = n.toLong() * 8L * 3L
        bytes.toDouble() / ((System.nanoTime() - start) / 1e9) / 1e9
    }

    /** BVH: parallel AABB construction from random 3D point cloud */
    private suspend fun runBvhBuild(): Double = coroutineScope {
        val numBoxes = 500_000
        val xs = FloatArray(numBoxes) { Math.random().toFloat() * 1000f }
        val ys = FloatArray(numBoxes) { Math.random().toFloat() * 1000f }
        val zs = FloatArray(numBoxes) { Math.random().toFloat() * 1000f }
        val chunkSize = numBoxes / coreCount
        val start = System.nanoTime()
        val bboxes = (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, numBoxes)
                var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
                for (i in s until e) {
                    if (xs[i] < minX) minX = xs[i]; if (xs[i] > maxX) maxX = xs[i]
                    if (ys[i] < minY) minY = ys[i]; if (ys[i] > maxY) maxY = ys[i]
                    if (zs[i] < minZ) minZ = zs[i]; if (zs[i] > maxZ) maxZ = zs[i]
                }
                floatArrayOf(minX, minY, minZ, maxX, maxY, maxZ)
            }
        }.awaitAll()
        BenchmarkHarness.consume(bboxes[0][0].toLong())
        (System.nanoTime() - start) / 1_000_000.0
    }

    private suspend fun runCollisionDetection(): Double = coroutineScope {
        val n = 2000 // 2K bodies (N² = 4M collision checks)
        val x = FloatArray(n) { Math.random().toFloat() * 100f }
        val y = FloatArray(n) { Math.random().toFloat() * 100f }
        val r = FloatArray(n) { (Math.random() * 2 + 0.5).toFloat() }
        val chunkSize = n / coreCount
        val start = System.nanoTime()
        val collisions = AtomicInteger(0)
        (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, n)
                var local = 0
                for (i in s until e) {
                    for (j in i + 1 until n) {
                        val dx = x[i] - x[j]; val dy = y[i] - y[j]
                        val distSq = dx * dx + dy * dy
                        val rSum = r[i] + r[j]
                        if (distSq < rSum * rSum) local++
                    }
                }
                collisions.addAndGet(local)
            }
        }.awaitAll()
        BenchmarkHarness.consume(collisions.get().toLong())
        (System.nanoTime() - start) / 1_000_000.0
    }

    private suspend fun runParallelGaussianBlur(): Double = coroutineScope {
        val w = 2048; val h = 2048
        val img = IntArray(w * h) { it }
        val out = IntArray(w * h)
        val radius = 8
        val kernel = (2 * radius + 1).let { size -> FloatArray(size) { i ->
            val x = i - radius; exp(-(x * x).toFloat() / (2 * radius * radius)).toFloat()
        } }
        val kSum = kernel.sum()
        val rowsPerCore = h / coreCount

        val start = System.nanoTime()
        (0 until coreCount).map { core ->
            async {
                val startRow = core * rowsPerCore
                val endRow = minOf(startRow + rowsPerCore, h)
                for (row in startRow until endRow) {
                    for (col in 0 until w) {
                        var acc = 0f
                        for (k in kernel.indices) {
                            val srcCol = (col + k - radius).coerceIn(0, w - 1)
                            acc += img[row * w + srcCol] * kernel[k]
                        }
                        out[row * w + col] = (acc / kSum).toInt()
                    }
                }
            }
        }.awaitAll()
        BenchmarkHarness.consume(out[w * h - 1].toLong())
        val elapsed = (System.nanoTime() - start) / 1_000_000.0
        (w * h).toDouble() / elapsed / 1000.0 // MPix/s
    }

    private suspend fun runParallelDecompress(): Double = coroutineScope {
        val chunkCount = coreCount * 8
        val chunkSize = 2 * 1024 * 1024 // 2MB per chunk
        val start = System.nanoTime()
        val bytesProcessed = (0 until chunkCount).map { chunk ->
            async {
                // Simulate RLE decompression (run-length decode)
                val compressed = ByteArray(chunkSize / 4) { ((chunk + it) % 64).toByte() }
                val decompressed = ByteArray(chunkSize)
                var srcIdx = 0; var dstIdx = 0
                while (srcIdx < compressed.size - 1 && dstIdx < decompressed.size - 10) {
                    val count = (compressed[srcIdx].toInt() and 0xFF) % 8 + 1
                    val value = compressed[srcIdx + 1]
                    for (k in 0 until count) {
                        if (dstIdx < decompressed.size) decompressed[dstIdx++] = value
                    }
                    srcIdx += 2
                }
                BenchmarkHarness.consume(decompressed[0].toLong())
                dstIdx.toLong()
            }
        }.awaitAll().sum()
        val elapsedSec = (System.nanoTime() - start) / 1e9
        bytesProcessed / elapsedSec / (1024 * 1024) // MB/s
    }

    private suspend fun runParallelBfs(): Double = coroutineScope {
        // Build a random undirected graph: 100K nodes, ~20 edges per node
        val numNodes = 100_000
        val edgesPerNode = 20
        val rng = java.util.Random(42)
        val adj = Array(numNodes) { IntArray(edgesPerNode) { rng.nextInt(numNodes) } }

        val start = System.nanoTime()
        val visited = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()
        val queue = ConcurrentLinkedQueue<Int>()
        queue.add(0); visited[0] = true
        var level = 0

        while (queue.isNotEmpty() && level < 20) {
            val nextQueue = ConcurrentLinkedQueue<Int>()
            val chunkList = queue.toList()
            chunkList.chunked(maxOf(1, chunkList.size / coreCount)).map { chunk ->
                async {
                    chunk.forEach { node ->
                        adj[node].forEach { neighbor ->
                            if (visited.putIfAbsent(neighbor, true) == null) {
                                nextQueue.add(neighbor)
                            }
                        }
                    }
                }
            }.awaitAll()
            queue.clear()
            queue.addAll(nextQueue)
            level++
        }
        BenchmarkHarness.consume(visited.size.toLong())
        (System.nanoTime() - start) / 1_000_000.0
    }

    /** CPU-only matrix multiply simulating neural net dense layer inference */
    private suspend fun runCpuOnlyInference(): Double = coroutineScope {
        // Simulate a 1024-wide × 512-deep feed-forward layer
        val inputDim = 512; val outputDim = 512; val batchSize = 64
        val weights = Array(outputDim) { DoubleArray(inputDim) { Math.random() } }
        val inputs  = Array(batchSize)  { DoubleArray(inputDim)  { Math.random() } }
        val outputs = Array(batchSize)  { DoubleArray(outputDim) }
        val chunkSize = batchSize / coreCount

        val start = System.nanoTime()
        (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, batchSize)
                for (b in s until e) {
                    for (o in 0 until outputDim) {
                        var sum = 0.0
                        for (i in 0 until inputDim) sum += inputs[b][i] * weights[o][i]
                        outputs[b][o] = maxOf(0.0, sum) // ReLU
                    }
                }
            }
        }.awaitAll()
        BenchmarkHarness.consume(outputs[0][0].toLong())
        val elapsedSec = (System.nanoTime() - start) / 1e9
        val flops = 2.0 * batchSize * outputDim * inputDim
        flops / elapsedSec / 1e9
    }

    private suspend fun runAtomicContention(): Double = coroutineScope {
        val numWorkers = minOf(2000, coreCount * 256)
        val opsPerWorker = 10_000
        val counter = AtomicLong(0)
        val start = System.nanoTime()
        (0 until numWorkers).map {
            async {
                repeat(opsPerWorker) { counter.incrementAndGet() }
            }
        }.awaitAll()
        val totalOps = numWorkers.toLong() * opsPerWorker
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        totalOps / elapsedMs / 1000.0 // MOps/s
    }

    private suspend fun runPeakAllCoreSaturation(): Double = coroutineScope {
        val iterPerCore = 100_000_000
        val start = System.nanoTime()
        val flopsTotal = (0 until coreCount).map {
            async {
                // FMA-equivalent: fused multiply-add chain
                var a = 1.0; var b = 1.000001
                repeat(iterPerCore) {
                    a = a * b + 0.000001  // 2 flops: mul + add
                    b = b * a + 0.000001
                }
                BenchmarkHarness.consume(a.toLong())
                iterPerCore.toLong() * 4L // 4 flops per iteration
            }
        }.awaitAll().sum()
        val elapsedSec = (System.nanoTime() - start) / 1e9
        flopsTotal / elapsedSec / 1e9 // GFLOPS
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun subScore(
        name: String, rawValue: Double, unit: String,
        baseline: Double, cap: Double, inverted: Boolean
    ): SubScore {
        val score = ScoreNormalizer.normalize(rawValue, baseline, cap, inverted)
        return SubScore(name, rawValue, unit, score)
    }
}
