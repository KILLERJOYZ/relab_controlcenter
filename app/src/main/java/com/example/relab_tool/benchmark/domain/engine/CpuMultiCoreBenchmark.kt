package com.example.relab_tool.benchmark.domain.engine

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.*

/**
 * CPU Multi-Core Benchmark — 20 tests (01 – 20)
 *
 * Runs the SAME 20 workloads as CpuSingleCoreBenchmark, but parallelized
 * across all available cores. Each core runs the full workload independently;
 * per-core throughputs are summed for the aggregate result.
 *
 * This makes SC and MC scores directly comparable:
 * the MC/SC ratio represents the device's parallelism efficiency.
 *
 * All workloads are defined in [CpuTestWorkloads] and return throughput
 * (higher = better, inverted = false for all tests).
 *
 * MC calibration:
 *  - Compute-bound baselines: SC × 4.5, caps: SC × 5.0
 *  - Memory-bound baselines: SC × 2.5, caps: SC × 3.0
 *  - Mixed baselines: SC × 3.5, caps: SC × 4.0
 */
class CpuMultiCoreBenchmark : BenchmarkEngine {

    override val pillar = BenchmarkPillar.CPU_MULTI_CORE

    override fun isAvailable() = true

    private val coreCount = com.example.relab_tool.benchmark.util.CoreAffinityHarness.getHardwareCoreCount()

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> =
        withContext(Dispatchers.Default) {
            val results = mutableListOf<SubScore>()

            // ── Pre-allocate shared read-only data ───────────────────────────────
            val jsonString = CpuTestWorkloads.generateJsonString()
            val regexInput = CpuTestWorkloads.generateRegexInput()

            // ── 01: Integer ALU (64-bit) ─────────────────────────────────────────
            onProgress(0.03f)
            val v01 = BenchmarkHarness.medianOfThree(warmups = 3) {
                coroutineScope {
                    (0 until coreCount).map {
                        async { CpuTestWorkloads.runIntAlu() }
                    }.awaitAll().sum()
                }
            }
            results += mc("01: Integer ALU (64-bit)", v01, "GOps/s", 9.0, 45.0)

            // ── 02: FPU Transcendental ───────────────────────────────────────────
            onProgress(0.08f)
            val v02 = BenchmarkHarness.medianOfThree(warmups = 3) {
                coroutineScope {
                    (0 until coreCount).map {
                        async { CpuTestWorkloads.runFpu() }
                    }.awaitAll().sum()
                }
            }
            results += mc("02: FPU Transcendental", v02, "MOps/s", 540.0, 2750.0)

            // ── 03: Fibonacci Iterative ──────────────────────────────────────────
            onProgress(0.13f)
            val v03 = BenchmarkHarness.medianOfThree(warmups = 3) {
                coroutineScope {
                    (0 until coreCount).map {
                        async { CpuTestWorkloads.runFibonacciIterative() }
                    }.awaitAll().sum()
                }
            }
            results += mc("03: Fibonacci Iterative", v03, "MOps/s", 4050.0, 22500.0)

            // ── 04: Prime Sieve (10M) ────────────────────────────────────────────
            onProgress(0.18f)
            val v04 = run {
                val sieves = Array(coreCount) { BooleanArray(10_000_001) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runPrimeSieve(sieves[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("04: Prime Sieve (10M)", v04, "MOps/s", 233.5, 1600.0)

            // ── 05: L1 Cache Throughput ──────────────────────────────────────────
            onProgress(0.23f)
            val v05 = run {
                val indicesPerCore = Array(coreCount) { CpuTestWorkloads.generateShuffledIndices(8192, (42 + it).toLong()) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runL1CacheThroughput(indicesPerCore[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("05: L1 Cache Throughput", v05, "MOps/s", 1137.5, 3750.0)

            // ── 06: L2 Cache Throughput ──────────────────────────────────────────
            onProgress(0.28f)
            val v06 = run {
                val indicesPerCore = Array(coreCount) { CpuTestWorkloads.generateShuffledIndices(262144, (42 + it).toLong()) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runL2CacheThroughput(indicesPerCore[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("06: L2 Cache Throughput", v06, "MOps/s", 357.5, 1500.0)

            // ── 07: Sequential RAM Bandwidth ─────────────────────────────────────
            onProgress(0.33f)
            val v07 = run {
                val srcs = Array(coreCount) { LongArray(8_388_608) }
                val dsts = Array(coreCount) { LongArray(8_388_608) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runSequentialRam(srcs[core], dsts[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("07: Sequential RAM Bandwidth", v07, "GB/s", 45.0, 240.0)
            delay(400L)

            // ── 08: Random RAM Access ────────────────────────────────────────────
            onProgress(0.38f)
            val v08 = run {
                val arrs = Array(coreCount) { LongArray(4_194_304) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runRandomRam(arrs[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("08: Random RAM Access", v08, "MOps/s", 300.0, 1800.0)
            delay(400L)

            // ── 09: AES-256 Software ─────────────────────────────────────────────
            onProgress(0.43f)
            val v09 = run {
                val blocks = Array(coreCount) { ByteArray(16) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runAesSoftware(blocks[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("09: AES-256 Software", v09, "MB/s", 360.0, 2000.0)

            // ── 10: SHA-256 Hash ─────────────────────────────────────────────────
            onProgress(0.48f)
            val v10 = run {
                val dataPerCore = Array(coreCount) { ByteArray(1024 * 1024) { (it % 256).toByte() } }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runSha256(dataPerCore[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("10: SHA-256 Hash", v10, "MB/s", 4500.0, 25000.0)
            delay(400L)

            // ── 11: Compression (BWT) ────────────────────────────────────────────
            onProgress(0.53f)
            val v11 = run {
                val dataPerCore = Array(coreCount) { ByteArray(2 * 1024 * 1024) { (it % 256).toByte() } }
                val blockPerCore = Array(coreCount) { ByteArray(512) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runBwtCompression(dataPerCore[core], blockPerCore[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("11: Compression (BWT)", v11, "MB/s", 157.5, 900.0)

            // ── 12: JSON Parse ───────────────────────────────────────────────────
            // Read-only JSON string shared across cores (each core creates its own JSONArray)
            onProgress(0.58f)
            val v12 = BenchmarkHarness.medianOfThree(warmups = 3) {
                coroutineScope {
                    (0 until coreCount).map {
                        async { CpuTestWorkloads.runJsonParse(jsonString) }
                    }.awaitAll().sum()
                }
            }
            results += mc("12: JSON Parse (5K obj)", v12, "Kops/s", 29.1, 250.0)

            // ── 13: Regex Matching ───────────────────────────────────────────────
            // Read-only input shared across cores
            onProgress(0.63f)
            val v13 = BenchmarkHarness.medianOfThree(warmups = 3) {
                coroutineScope {
                    (0 until coreCount).map {
                        async { CpuTestWorkloads.runRegexMatch(regexInput, CpuTestWorkloads.REGEX_PATTERN) }
                    }.awaitAll().sum()
                }
            }
            results += mc("13: Regex Matching", v13, "KB/s", 1120.0, 8532.0)

            // ── 14: SQLite In-Memory Insert ──────────────────────────────────────
            // Each core creates its own in-memory SQLite database
            onProgress(0.68f)
            val v14 = BenchmarkHarness.medianOfThree(warmups = 3) {
                coroutineScope {
                    (0 until coreCount).map {
                        async { CpuTestWorkloads.runSqliteInsert() }
                    }.awaitAll().sum()
                }
            }
            results += mc("14: SQLite In-Memory Insert", v14, "Kinserts/s", 116.6, 1000.0)

            // ── 15: A* Pathfinding (500×500) ─────────────────────────────────────
            onProgress(0.73f)
            val v15 = run {
                val gCosts = Array(coreCount) { Array(500) { FloatArray(500) } }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runAStar(gCosts[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("15: A* Pathfinding (500x500)", v15, "Knodes/s", 437.5, 3332.0)

            // ── 16: FFT 65536-point ──────────────────────────────────────────────
            onProgress(0.78f)
            val v16 = run {
                val reals = Array(coreCount) { DoubleArray(65536) }
                val imags = Array(coreCount) { DoubleArray(65536) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runFft(reals[core], imags[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("16: FFT 65536-point", v16, "MOps/s", 315.0, 1900.0)

            // ── 17: Matrix Multiply (512×512) ────────────────────────────────────
            onProgress(0.83f)
            val v17 = run {
                val sz = 512
                val as_ = Array(coreCount) { Array(sz) { r -> DoubleArray(sz) { c -> (r + c + 1).toDouble() } } }
                val bs = Array(coreCount) { Array(sz) { r -> DoubleArray(sz) { c -> (r - c + 1).toDouble() } } }
                val cs = Array(coreCount) { Array(sz) { DoubleArray(sz) } }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runMatrixMultiply(as_[core], bs[core], cs[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("17: Matrix Multiply (512x512)", v17, "GFLOPS", 2.25, 17.5)
            delay(400L)

            // ── 18: Matrix Transpose (2048×2048) ─────────────────────────────────
            onProgress(0.88f)
            val v18 = run {
                val srcs = Array(coreCount) { Array(2048) { r -> IntArray(2048) { c -> r * 2048 + c } } }
                val dsts = Array(coreCount) { Array(2048) { IntArray(2048) } }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runMatrixTranspose(srcs[core], dsts[core]) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("18: Matrix Transpose (2Kx2K)", v18, "MPix/s", 7.0, 50.4)
            delay(400L)

            // ── 19: Monte Carlo Pi (50M) ─────────────────────────────────────────
            onProgress(0.93f)
            val v19 = BenchmarkHarness.medianOfThree(warmups = 3) {
                coroutineScope {
                    (0 until coreCount).map { core ->
                        async { CpuTestWorkloads.runMonteCarloPi(seed = core.toLong()) }
                    }.awaitAll().sum()
                }
            }
            results += mc("19: Monte Carlo Pi (50M)", v19, "Mpts/s", 675.0, 4000.0)

            // ── 20: Gaussian Blur (2K×2K) ────────────────────────────────────────
            onProgress(0.97f)
            val v20 = run {
                val w = 2048; val h = 2048
                val imgs = Array(coreCount) { IntArray(w * h) { it } }
                val outs = Array(coreCount) { IntArray(w * h) }
                BenchmarkHarness.medianOfThree(warmups = 3) {
                    coroutineScope {
                        (0 until coreCount).map { core ->
                            async { CpuTestWorkloads.runGaussianBlur(imgs[core], outs[core], w, h) }
                        }.awaitAll().sum()
                    }
                }
            }
            results += mc("20: Gaussian Blur (2Kx2K)", v20, "MPix/s", 14.0, 100.0)

            onProgress(1.0f)
            results
        }

    // ── Helper ────────────────────────────────────────────────────────────────────

    /** All MC tests use inverted=false (throughput: higher is better). */
    private fun mc(
        name: String,
        rawValue: Double,
        unit: String,
        baseline: Double,
        cap: Double
    ): SubScore {
        return ScoreNormalizer.createSubScore(name, rawValue, unit, baseline, cap, false, false)
    }
}
