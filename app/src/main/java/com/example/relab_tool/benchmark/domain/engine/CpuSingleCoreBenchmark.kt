package com.example.relab_tool.benchmark.domain.engine

import android.util.Log
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import com.example.relab_tool.benchmark.util.CoreAffinityHarness
import kotlinx.coroutines.withContext

/**
 * CPU Single-Core Benchmark — 20 tests (01 – 20)
 *
 * Runs the SAME 20 workloads as CpuMultiCoreBenchmark, but on a single
 * pinned core. This makes SC and MC scores directly comparable:
 * the MC/SC ratio represents the device's parallelism efficiency.
 *
 * All workloads are defined in [CpuTestWorkloads] and return throughput
 * (higher = better, inverted = false for all tests).
 *
 * Design principles:
 *  - Execution pinned to the dynamically detected prime core (highest-perf core
 *    in big.LITTLE topology, detected via cpuinfo_max_freq or native profiling).
 *  - medianOfThree() wraps each test: 3 warm-up + 3 timed runs → median.
 *  - BenchmarkHarness.consume() prevents dead-code elimination.
 *  - JNI native C used for tests 01, 02 (if available) to bypass ART JIT DCE.
 *
 * Scoring calibration:
 *  - baseline = Snapdragon 778G class (50th-percentile mid-range, single-core)
 *  - cap      = 2× Snapdragon 8 Elite Gen 5 class (headroom through 2027)
 */
class CpuSingleCoreBenchmark : BenchmarkEngine {

    companion object {
        private const val TAG = "CpuSingleCoreBenchmark"
    }

    override val pillar = BenchmarkPillar.CPU_SINGLE_CORE

    override fun isAvailable() = true

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> {
        val targetCore = CoreAffinityHarness.getPrimeCoreIndex()
        Log.d(TAG, "Detected prime core index: $targetCore (of ${CoreAffinityHarness.getHardwareCoreCount()} cores)")
        val pinnedDispatcher = CoreAffinityHarness.createPinnedDispatcher(targetCore)
        val results = mutableListOf<SubScore>()
        return try {
            withContext(pinnedDispatcher.dispatcher) {
                try {

            // ── Pre-allocate shared buffers ──────────────────────────────────────
            val l1Indices = CpuTestWorkloads.generateShuffledIndices(8192, 42L)
            val l2Indices = CpuTestWorkloads.generateShuffledIndices(262144, 42L)
            val jsonString = CpuTestWorkloads.generateJsonString()
            val regexInput = CpuTestWorkloads.generateRegexInput()

            // ── 01: Integer ALU (64-bit) ─────────────────────────────────────────
            onProgress(0.03f)
            val v01 = BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runIntAlu() }
            results += sc("01: Integer ALU (64-bit)", v01, "GOps/s", 2.0, 9.0)

            // ── 02: FPU Transcendental ───────────────────────────────────────────
            onProgress(0.08f)
            val v02 = BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runFpu() }
            results += sc("02: FPU Transcendental", v02, "MOps/s", 120.0, 550.0)

            // ── 03: Fibonacci Iterative ──────────────────────────────────────────
            onProgress(0.13f)
            val v03 = BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runFibonacciIterative() }
            results += sc("03: Fibonacci Iterative", v03, "MOps/s", 900.0, 4500.0)

            // ── 04: Prime Sieve (10M) ────────────────────────────────────────────
            onProgress(0.18f)
            val v04 = run {
                val sieve = BooleanArray(10_000_001)
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runPrimeSieve(sieve) }
            }
            results += sc("04: Prime Sieve (10M)", v04, "MOps/s", 66.7, 400.0)

            // ── 05: L1 Cache Throughput ──────────────────────────────────────────
            onProgress(0.23f)
            val v05 = BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runL1CacheThroughput(l1Indices) }
            results += sc("05: L1 Cache Throughput", v05, "MOps/s", 455.0, 1250.0)

            // ── 06: L2 Cache Throughput ──────────────────────────────────────────
            onProgress(0.28f)
            val v06 = BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runL2CacheThroughput(l2Indices) }
            results += sc("06: L2 Cache Throughput", v06, "MOps/s", 143.0, 500.0)

            // ── 07: Sequential RAM Bandwidth ─────────────────────────────────────
            onProgress(0.33f)
            val v07 = run {
                val src = LongArray(8_388_608)
                val dst = LongArray(8_388_608)
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runSequentialRam(src, dst) }
            }
            results += sc("07: Sequential RAM Bandwidth", v07, "GB/s", 18.0, 80.0)

            // ── 08: Random RAM Access ────────────────────────────────────────────
            onProgress(0.38f)
            val v08 = run {
                val arr = LongArray(4_194_304)
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runRandomRam(arr) }
            }
            results += sc("08: Random RAM Access", v08, "MOps/s", 120.0, 600.0)

            // ── 09: AES-256 Software ─────────────────────────────────────────────
            onProgress(0.43f)
            val v09 = run {
                val block = ByteArray(16)
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runAesSoftware(block) }
            }
            results += sc("09: AES-256 Software", v09, "MB/s", 80.0, 400.0)

            // ── 10: SHA-256 Hash ─────────────────────────────────────────────────
            onProgress(0.48f)
            val v10 = run {
                val data = ByteArray(1024 * 1024) { (it % 256).toByte() }
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runSha256(data) }
            }
            results += sc("10: SHA-256 Hash", v10, "MB/s", 1000.0, 5000.0)

            // ── 11: Compression (BWT) ────────────────────────────────────────────
            onProgress(0.53f)
            val v11 = run {
                val data = ByteArray(2 * 1024 * 1024) { (it % 256).toByte() }
                val block = ByteArray(512)
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runBwtCompression(data, block) }
            }
            results += sc("11: Compression (BWT)", v11, "MB/s", 35.0, 180.0)

            // ── 12: JSON Parse ───────────────────────────────────────────────────
            onProgress(0.58f)
            val v12 = BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runJsonParse(jsonString) }
            results += sc("12: JSON Parse (5K obj)", v12, "Kops/s", 8.3, 62.5)

            // ── 13: Regex Matching ───────────────────────────────────────────────
            onProgress(0.63f)
            val v13 = BenchmarkHarness.medianOfThree(warmups = 3) {
                CpuTestWorkloads.runRegexMatch(regexInput, CpuTestWorkloads.REGEX_PATTERN)
            }
            results += sc("13: Regex Matching", v13, "KB/s", 320.0, 2133.0)

            // ── 14: SQLite In-Memory Insert ──────────────────────────────────────
            onProgress(0.68f)
            val v14 = BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runSqliteInsert() }
            results += sc("14: SQLite In-Memory Insert", v14, "Kinserts/s", 33.3, 250.0)

            // ── 15: A* Pathfinding (500×500) ─────────────────────────────────────
            onProgress(0.73f)
            val v15 = run {
                val gCost = Array(500) { FloatArray(500) }
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runAStar(gCost) }
            }
            results += sc("15: A* Pathfinding (500x500)", v15, "Knodes/s", 125.0, 833.0)

            // ── 16: FFT 65536-point ──────────────────────────────────────────────
            onProgress(0.78f)
            val v16 = run {
                val real = DoubleArray(65536)
                val imag = DoubleArray(65536)
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runFft(real, imag) }
            }
            results += sc("16: FFT 65536-point", v16, "MOps/s", 70.0, 380.0)

            // ── 17: Matrix Multiply (512×512) ────────────────────────────────────
            onProgress(0.83f)
            val v17 = run {
                val sz = 512
                val a = Array(sz) { r -> DoubleArray(sz) { c -> (r + c + 1).toDouble() } }
                val b = Array(sz) { r -> DoubleArray(sz) { c -> (r - c + 1).toDouble() } }
                val c = Array(sz) { DoubleArray(sz) }
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runMatrixMultiply(a, b, c) }
            }
            results += sc("17: Matrix Multiply (512x512)", v17, "GFLOPS", 0.5, 3.5)

            // ── 18: Matrix Transpose (2048×2048) ─────────────────────────────────
            onProgress(0.88f)
            val v18 = run {
                val src = Array(2048) { r -> IntArray(2048) { c -> r * 2048 + c } }
                val dst = Array(2048) { IntArray(2048) }
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runMatrixTranspose(src, dst) }
            }
            results += sc("18: Matrix Transpose (2Kx2K)", v18, "MPix/s", 2.8, 16.8)

            // ── 19: Monte Carlo Pi (50M) ─────────────────────────────────────────
            onProgress(0.93f)
            val v19 = BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runMonteCarloPi(seed = 42L) }
            results += sc("19: Monte Carlo Pi (50M)", v19, "Mpts/s", 150.0, 800.0)

            // ── 20: Gaussian Blur (2K×2K) ────────────────────────────────────────
            onProgress(0.97f)
            val v20 = run {
                val w = 2048; val h = 2048
                val img = IntArray(w * h) { it }
                val out = IntArray(w * h)
                BenchmarkHarness.medianOfThree(warmups = 3) { CpuTestWorkloads.runGaussianBlur(img, out, w, h) }
            }
            results += sc("20: Gaussian Blur (2Kx2K)", v20, "MPix/s", 4.0, 25.0)

            onProgress(1.0f)
                } catch (e: Exception) {
                    Log.e(TAG, "Engine crashed midway", e)
                }

                if (CoreAffinityHarness.getHardwareCoreCount() > 1 && !pinnedDispatcher.wasPinned.get()) {
                    Log.w(TAG, "Core affinity pinning failed for core $targetCore")
                    results.add(0, ScoreNormalizer.createSubScore(
                        "CPU Affinity Pinning Failed (core $targetCore)", 0.0, "N/A", 1.0, 1.0, false, true
                    ))
                }
                results
            }
        } finally {
            pinnedDispatcher.close()
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────────

    /** All SC tests use inverted=false (throughput: higher is better). */
    private fun sc(
        name: String,
        rawValue: Double,
        unit: String,
        baseline: Double,
        cap: Double
    ): SubScore {
        return ScoreNormalizer.createSubScore(name, rawValue, unit, baseline, cap, false, false)
    }
}
