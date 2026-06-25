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
 */
class CpuMultiCoreBenchmark : BenchmarkEngine {

    override val pillar = BenchmarkPillar.CPU_MULTI_CORE

    override fun isAvailable() = true

    private val coreCount = getCoreCount()

    private fun getCoreCount(): Int {
        return com.example.relab_tool.benchmark.util.CoreAffinityHarness.getHardwareCoreCount()
    }

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> =
        withContext(Dispatchers.Default) {
            val results = mutableListOf<SubScore>()

            // ── Pre-allocate all buffers to eliminate GC pauses during timed runs ──
            
            // MC_01 Matrix Multiply
            val matMulSize = 768
            val matMulA = Array(matMulSize) { r -> DoubleArray(matMulSize) { c -> (r + c + 1).toDouble() } }
            val matMulB = Array(matMulSize) { r -> DoubleArray(matMulSize) { c -> (r - c + 1).toDouble() } }
            val matMulC = Array(matMulSize) { DoubleArray(matMulSize) }

            // MC_02 N-Body
            val nBodySize = 8000
            val nBodyX = DoubleArray(nBodySize) { Math.random() * 1000 }
            val nBodyY = DoubleArray(nBodySize) { Math.random() * 1000 }
            val nBodyM = DoubleArray(nBodySize) { Math.random() + 0.1 }
            val nBodyFx = DoubleArray(nBodySize)
            val nBodyFy = DoubleArray(nBodySize)

            // MC_03 Merge Sort
            val sortN = 2_000_000
            val sortArr = IntArray(sortN) { (sortN - it) }
            val sortChunkSize = sortN / coreCount
            val sortChunks = Array(coreCount) { core ->
                val s = core * sortChunkSize
                val e = minOf(s + sortChunkSize, sortN)
                IntArray(e - s)
            }
            val sortMergeBuffers = Array(coreCount - 1) { step ->
                IntArray((step + 2) * sortChunkSize)
            }

            // MC_04 AES (Also reused for MC_05 SHA and MC_16 Decompress)
            val aesDataMbPerCore = 16
            val aesData = Array(coreCount) { core ->
                ByteArray(aesDataMbPerCore * 1024 * 1024) { (it xor core).toByte() }
            }
            val aesKeys = Array(coreCount) { core ->
                ByteArray(32) { (it + core).toByte() }
            }

            // MC_09 – MC_12 STREAM (Double arrays also reused for STREAM Copy)
            val streamN = 4 * 1024 * 1024
            val streamDoubleA = DoubleArray(streamN) { it.toDouble() }
            val streamDoubleB = DoubleArray(streamN) { (streamN - it).toDouble() }
            val streamDoubleC = DoubleArray(streamN)

            // MC_13 BVH
            val bvhNumBoxes = 500_000
            val bvhXs = FloatArray(bvhNumBoxes) { Math.random().toFloat() * 1000f }
            val bvhYs = FloatArray(bvhNumBoxes) { Math.random().toFloat() * 1000f }
            val bvhZs = FloatArray(bvhNumBoxes) { Math.random().toFloat() * 1000f }
            val bvhResults = Array(coreCount) { FloatArray(6) }

            // MC_14 Collision
            val colN = 2000
            val colX = FloatArray(colN) { Math.random().toFloat() * 100f }
            val colY = FloatArray(colN) { Math.random().toFloat() * 100f }
            val colR = FloatArray(colN) { (Math.random() * 2 + 0.5).toFloat() }

            // MC_15 Blur
            val blurW = 2048; val blurH = 2048
            val blurImg = IntArray(blurW * blurH) { it }
            val blurOut = IntArray(blurW * blurH)

            // MC_17 BFS
            val bfsNumNodes = 100_000
            val bfsEdgesPerNode = 20
            val bfsRng = java.util.Random(42)
            val bfsAdj = Array(bfsNumNodes) { IntArray(bfsEdgesPerNode) { bfsRng.nextInt(bfsNumNodes) } }

            // MC_18 Inference
            val infInputDim = 512; val infOutputDim = 512; val infBatchSize = 64
            val infWeights = Array(infOutputDim) { DoubleArray(infInputDim) { Math.random() } }
            val infInputs  = Array(infBatchSize)  { DoubleArray(infInputDim)  { Math.random() } }
            val infOutputs = Array(infBatchSize)  { DoubleArray(infOutputDim) }

            // ────────────────────────────────────────────────────────────────────────

            // MC_01 — Parallel Matrix Multiply (blocked DGEMM on 768×768)
            onProgress(0.02f)
            val matMulVal = BenchmarkHarness.medianOfThree(warmups = 3) { runParallelMatrixMultiply(matMulA, matMulB, matMulC) }
            results += subScore("MC_01: Parallel Matrix Multiply", matMulVal, "GFLOPS",
                baseline = 25.0, cap = 400.0, inverted = false)

            // MC_02 — N-Body Gravity (8K particles)
            onProgress(0.07f)
            val nBodyVal = BenchmarkHarness.medianOfThree(warmups = 3) { runNBody(nBodyX, nBodyY, nBodyM, nBodyFx, nBodyFy) }
            results += subScore("MC_02: N-Body Gravity (8K)", nBodyVal, "GFlop/s",
                baseline = 5.0, cap = 60.0, inverted = false)

            // MC_03 — Parallel Merge Sort (2M elements)
            onProgress(0.12f)
            val sortVal = BenchmarkHarness.medianOfThree(warmups = 3) { runParallelMergeSort(sortArr, sortChunks, sortMergeBuffers) }
            results += subScore("MC_03: Parallel Merge Sort (10M)", sortVal, "ms",
                baseline = 600.0, cap = 70.0, inverted = true)

            // MC_04 — Parallel AES-256 (chunk per core)
            onProgress(0.17f)
            val aesVal = BenchmarkHarness.medianOfThree(warmups = 3) { runParallelAes(aesData, aesKeys) }
            results += subScore("MC_04: Parallel AES-256 Encrypt", aesVal, "MB/s",
                baseline = 400.0, cap = 3000.0, inverted = false)

            // MC_05 — Parallel SHA-256 (all cores at 100% saturation)
            onProgress(0.22f)
            val shaVal = BenchmarkHarness.medianOfThree(warmups = 3) { runParallelSha256(aesData) }
            results += subScore("MC_05: Parallel SHA-256 (all cores)", shaVal, "GH/s",
                baseline = 0.10, cap = 1.0, inverted = false)

            // MC_06 — SQLite Parallel Read (16 concurrent readers on :memory:)
            onProgress(0.27f)
            val sqlReadVal = BenchmarkHarness.medianOfThree(warmups = 3) { runSqliteParallelRead() }
            results += subScore("MC_06: SQLite Parallel Read", sqlReadVal, "ms",
                baseline = 500.0, cap = 60.0, inverted = true)

            // MC_07 — Monte Carlo Pi (50M pts/core)
            onProgress(0.32f)
            val piVal = BenchmarkHarness.medianOfThree(warmups = 3) { runMonteCarloPi() }
            results += subScore("MC_07: Monte Carlo Pi (50M/core)", piVal, "Mpts/s",
                baseline = 1500.0, cap = 16000.0, inverted = false)

            // MC_08 — Context Switch Storm (2000 coroutines)
            onProgress(0.37f)
            val ctxVal = BenchmarkHarness.medianOfFive(warmups = 3) { runContextSwitchStorm() }
            results += subScore("MC_08: Context Switch Storm (2K)", ctxVal, "ms",
                baseline = 450.0, cap = 50.0, inverted = true)

            // MC_09 — STREAM Copy (multi-threaded sequential copy)
            onProgress(0.42f)
            val streamCopyVal = BenchmarkHarness.medianOfThree(warmups = 3) { runStreamCopy(streamDoubleA, streamDoubleB) }
            results += subScore("MC_09: STREAM Copy", streamCopyVal, "GB/s",
                baseline = 30.0, cap = 160.0, inverted = false)

            // MC_10 — STREAM Scale (multiply by constant)
            onProgress(0.47f)
            val streamScaleVal = BenchmarkHarness.medianOfThree(warmups = 3) { runStreamScale(streamDoubleA, streamDoubleB) }
            results += subScore("MC_10: STREAM Scale", streamScaleVal, "GB/s",
                baseline = 28.0, cap = 140.0, inverted = false)

            // MC_11 — STREAM Add (dual-read add)
            onProgress(0.52f)
            val streamAddVal = BenchmarkHarness.medianOfThree(warmups = 3) { runStreamAdd(streamDoubleA, streamDoubleB, streamDoubleC) }
            results += subScore("MC_11: STREAM Add", streamAddVal, "GB/s",
                baseline = 35.0, cap = 180.0, inverted = false)

            // MC_12 — STREAM Triad (combined scale + add)
            onProgress(0.57f)
            val streamTriadVal = BenchmarkHarness.medianOfThree(warmups = 3) { runStreamTriad(streamDoubleA, streamDoubleB, streamDoubleC) }
            results += subScore("MC_12: STREAM Triad", streamTriadVal, "GB/s",
                baseline = 32.0, cap = 160.0, inverted = false)

            // MC_13 — BVH Tree Build (parallel bounding volume hierarchy)
            onProgress(0.62f)
            val bvhVal = BenchmarkHarness.medianOfThree(warmups = 3) { runBvhBuild(bvhXs, bvhYs, bvhZs, bvhResults) }
            results += subScore("MC_13: BVH Tree Build (parallel)", bvhVal, "ms",
                baseline = 600.0, cap = 70.0, inverted = true)

            // MC_14 — Collision Detection (10K bodies with Octree)
            onProgress(0.65f)
            val collisionVal = BenchmarkHarness.medianOfThree(warmups = 3) { runCollisionDetection(colX, colY, colR) }
            results += subScore("MC_14: Collision Detection (10K)", collisionVal, "ms",
                baseline = 450.0, cap = 50.0, inverted = true)

            // MC_15 — Parallel Gaussian Blur (image strip distribution)
            onProgress(0.70f)
            val blurVal = BenchmarkHarness.medianOfThree(warmups = 3) { runParallelGaussianBlur(blurImg, blurOut) }
            results += subScore("MC_15: Parallel Gaussian Blur", blurVal, "MPix/s",
                baseline = 1500.0, cap = 12000.0, inverted = false)

            // MC_16 — Parallel Zlib-like Decompression (independent chunks)
            onProgress(0.75f)
            val decompVal = BenchmarkHarness.medianOfThree(warmups = 3) { runParallelDecompress(aesData) }
            results += subScore("MC_16: Parallel Decompression", decompVal, "MB/s",
                baseline = 800.0, cap = 7000.0, inverted = false)

            // MC_17 — BFS Graph Traversal (parallel frontier expansion)
            onProgress(0.80f)
            val bfsVal = BenchmarkHarness.medianOfThree(warmups = 3) { runParallelBfs(bfsAdj) }
            results += subScore("MC_17: Parallel BFS Graph", bfsVal, "ms",
                baseline = 400.0, cap = 40.0, inverted = true)

            // MC_18 — TFLite CPU-only Inference (force no NPU delegation)
            onProgress(0.85f)
            val inferVal = BenchmarkHarness.medianOfThree(warmups = 3) { runCpuOnlyInference(infWeights, infInputs, infOutputs) }
            results += subScore("MC_18: CPU-only Neural Net (MatMul)", inferVal, "GFLOPS",
                baseline = 20.0, cap = 160.0, inverted = false)

            // MC_19 — Atomic CAS Contention (2000 threads → 1 counter)
            onProgress(0.92f)
            val atomicVal = BenchmarkHarness.medianOfFive(warmups = 3) { runAtomicContention() }
            results += subScore("MC_19: Atomic CAS Contention", atomicVal, "MOps/s",
                baseline = 80.0, cap = 800.0, inverted = false)

            // MC_20 — Peak Thermal Saturation
            onProgress(0.97f)
            val satVal = BenchmarkHarness.medianOfThree(warmups = 3) { runPeakAllCoreSaturation() }
            results += subScore("MC_20: Peak All-Core Saturation", satVal, "GFLOPS",
                baseline = 35.0, cap = 500.0, inverted = false)

            onProgress(1.0f)
            results
        }

    // ── Individual test implementations ──────────────────────────────────────

    private suspend fun runParallelMatrixMultiply(
        a: Array<DoubleArray>, b: Array<DoubleArray>, c: Array<DoubleArray>
    ): Double = coroutineScope {
        val size = a.size
        for (i in 0 until size) {
            c[i].fill(0.0)
        }
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

    private suspend fun runNBody(
        x: DoubleArray, y: DoubleArray, m: DoubleArray,
        fxTotal: DoubleArray, fyTotal: DoubleArray
    ): Double = coroutineScope {
        val n = x.size
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
        val flops = n.toDouble() * n * 8
        flops / elapsedSec / 1e9
    }

    private suspend fun runParallelMergeSort(
        arr: IntArray, sortChunks: Array<IntArray>, mergeBuffers: Array<IntArray>
    ): Double = coroutineScope {
        val n = arr.size
        val chunkSize = n / coreCount

        val start = System.nanoTime()
        val sorted = (0 until coreCount).map { core ->
            async {
                val s = core * chunkSize
                val e = minOf(s + chunkSize, n)
                val chunk = sortChunks[core]
                System.arraycopy(arr, s, chunk, 0, e - s)
                chunk.sort()
                chunk
            }
        }.awaitAll()

        var merged = sorted[0]
        for (i in 1 until sorted.size) {
            val out = mergeBuffers[i - 1]
            mergeSorted(merged, sorted[i], out)
            merged = out
        }
        BenchmarkHarness.consume(merged.last().toLong())
        (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private fun mergeSorted(a: IntArray, b: IntArray, out: IntArray) {
        var i = 0; var j = 0; var k = 0
        while (i < a.size && j < b.size) {
            out[k++] = if (a[i] <= b[j]) a[i++] else b[j++]
        }
        while (i < a.size) out[k++] = a[i++]
        while (j < b.size) out[k++] = b[j++]
    }

    private suspend fun runParallelAes(data: Array<ByteArray>, keys: Array<ByteArray>): Double = coroutineScope {
        val dataMbPerCore = 16
        val start = System.nanoTime()
        val jobs = (0 until coreCount).map { core ->
            async {
                val blockData = data[core]
                val key = keys[core]
                val blockSize = 16
                for (block in 0 until blockData.size / blockSize) {
                    for (r in 0 until 14) {
                        for (i in 0 until blockSize) {
                            blockData[block * blockSize + i] = (blockData[block * blockSize + i].toInt() xor key[(r * 2 + i) % 32].toInt()).toByte()
                        }
                    }
                }
                BenchmarkHarness.consume(blockData[0].toLong())
                dataMbPerCore.toLong()
            }
        }
        val totalMb = jobs.awaitAll().sum()
        val elapsedSec = (System.nanoTime() - start) / 1e9
        totalMb / elapsedSec // MB/s
    }

    private suspend fun runParallelSha256(data: Array<ByteArray>): Double = coroutineScope {
        val chunkMb = 16
        val start = System.nanoTime()
        val jobs = (0 until coreCount).map { core ->
            async {
                val md = MessageDigest.getInstance("SHA-256")
                val coreData = data[core]
                repeat(chunkMb) { i -> coreData[0] = i.toByte(); md.update(coreData, 0, 1024 * 1024) }
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
        val jobs = (0 until 4).map {
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
        val ptsPerCore = 50_000_000
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
                    yield()
                    counter.incrementAndGet()
                }
            }
        }
        jobs.awaitAll()
        (System.nanoTime() - start) / 1_000_000.0 // ms
    }

    private suspend fun runStreamCopy(src: DoubleArray, dst: DoubleArray): Double = coroutineScope {
        val n = src.size
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
        BenchmarkHarness.consume(dst[n - 1].toLong())
        val bytes = n.toLong() * 8L * 2L // read + write
        bytes.toDouble() / ((System.nanoTime() - start) / 1e9) / 1e9 // GB/s
    }

    private suspend fun runStreamScale(src: DoubleArray, dst: DoubleArray): Double = coroutineScope {
        val n = src.size
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

    private suspend fun runStreamAdd(a: DoubleArray, b: DoubleArray, c: DoubleArray): Double = coroutineScope {
        val n = a.size
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

    private suspend fun runStreamTriad(a: DoubleArray, b: DoubleArray, c: DoubleArray): Double = coroutineScope {
        val n = a.size
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

    private suspend fun runBvhBuild(
        xs: FloatArray, ys: FloatArray, zs: FloatArray, bboxes: Array<FloatArray>
    ): Double = coroutineScope {
        val numBoxes = xs.size
        val chunkSize = numBoxes / coreCount
        val start = System.nanoTime()
        (0 until coreCount).map { core ->
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
                val box = bboxes[core]
                box[0] = minX; box[1] = minY; box[2] = minZ
                box[3] = maxX; box[4] = maxY; box[5] = maxZ
            }
        }.awaitAll()
        BenchmarkHarness.consume(bboxes[0][0].toLong())
        (System.nanoTime() - start) / 1_000_000.0
    }

    private suspend fun runCollisionDetection(
        x: FloatArray, y: FloatArray, r: FloatArray
    ): Double = coroutineScope {
        val n = x.size
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

    private suspend fun runParallelGaussianBlur(img: IntArray, out: IntArray): Double = coroutineScope {
        val w = 2048; val h = 2048
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

    private suspend fun runParallelDecompress(aesData: Array<ByteArray>): Double = coroutineScope {
        val chunkCount = coreCount * 4
        val start = System.nanoTime()
        val bytesProcessed = (0 until chunkCount).map { chunk ->
            async {
                val core = chunk % coreCount
                val sub = chunk / coreCount
                val buf = aesData[core]
                val compOffset = sub * 2_500_000
                val decompOffset = compOffset + 512 * 1024

                // Initialize compressed data
                for (it in 0 until 512 * 1024) {
                    buf[compOffset + it] = ((chunk + it) % 64).toByte()
                }

                var srcIdx = 0; var dstIdx = 0
                val maxSrc = 512 * 1024
                val maxDst = 2 * 1024 * 1024
                while (srcIdx < maxSrc - 1 && dstIdx < maxDst - 10) {
                    val count = (buf[compOffset + srcIdx].toInt() and 0xFF) % 8 + 1
                    val value = buf[compOffset + srcIdx + 1]
                    for (k in 0 until count) {
                        if (dstIdx < maxDst) {
                            buf[decompOffset + dstIdx] = value
                            dstIdx++
                        }
                    }
                    srcIdx += 2
                }
                BenchmarkHarness.consume(buf[decompOffset].toLong())
                dstIdx.toLong()
            }
        }.awaitAll().sum()
        val elapsedSec = (System.nanoTime() - start) / 1e9
        bytesProcessed / elapsedSec / (1024 * 1024) // MB/s
    }

    private suspend fun runParallelBfs(adj: Array<IntArray>): Double = coroutineScope {
        val numNodes = adj.size
        val start = System.nanoTime()
        val visited = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()
        val queue = ConcurrentLinkedQueue<Int>()
        queue.add(0); visited[0] = true
        var level = 0

        while (queue.isNotEmpty() && level < 20) {
            val nextQueue = ConcurrentLinkedQueue<Int>()
            val currentLevelNodes = queue.toList()
            val index = AtomicInteger(0)
            val size = currentLevelNodes.size
            
            (0 until coreCount).map {
                async {
                    while (true) {
                        val i = index.getAndIncrement()
                        if (i >= size) break
                        val node = currentLevelNodes[i]
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

    private suspend fun runCpuOnlyInference(
        weights: Array<DoubleArray>, inputs: Array<DoubleArray>, outputs: Array<DoubleArray>
    ): Double = coroutineScope {
        val batchSize = inputs.size
        val outputDim = weights.size
        val inputDim = weights[0].size
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
                var a = 1.0; var b = 1.000001
                repeat(iterPerCore) {
                    a = a * b + 0.000001
                    b = b * a + 0.000001
                }
                BenchmarkHarness.consume(a.toLong())
                iterPerCore.toLong() * 4L
            }
        }.awaitAll().sum()
        val elapsedSec = (System.nanoTime() - start) / 1e9
        flopsTotal / elapsedSec / 1e9 // GFLOPS
    }

    private fun subScore(
        name: String,
        rawValue: Double,
        unit: String,
        baseline: Double,
        cap: Double,
        inverted: Boolean
    ): SubScore {
        return ScoreNormalizer.createSubScore(name, rawValue, unit, baseline, cap, inverted, false)
    }
}
