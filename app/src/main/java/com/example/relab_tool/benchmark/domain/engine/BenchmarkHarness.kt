package com.example.relab_tool.benchmark.domain.engine

/**
 * BenchmarkHarness — shared utilities for measurement precision.
 *
 * Provides:
 * - A `@Volatile` blackhole sink to prevent dead-code elimination (DCE) by the JIT/ART compiler.
 *   Any computed result that is not externally observable MUST be piped into [consume] to guarantee
 *   the compiler cannot optimize it away.
 *
 * - A [medianOfThree] harness that performs warm-up iterations (to let JIT compile hot paths)
 *   followed by 3 timed runs, returning the **median** value. This guards against:
 *   (a) JIT compilation overhead on first invocation (U-1),
 *   (b) outlier runs from GC pauses or thermal throttling (U-6).
 *
 * Usage:
 * ```kotlin
 * // In run() method:
 * val score = BenchmarkHarness.medianOfThree { runFibonacci() }
 *
 * // Inside runFibonacci():
 * var sum = 0L
 * // ... computation ...
 * BenchmarkHarness.consume(sum)  // DCE guard
 * return (operations / elapsed) / 1e6
 * ```
 */
object BenchmarkHarness {

    /**
     * Volatile sink — the JIT/ART compiler cannot eliminate stores to volatile fields
     * because it must assume another thread may read this value.
     */
    @Volatile
    @JvmField
    var blackhole: Long = 0L

    /**
     * Consume a Long value to prevent dead-code elimination.
     * The value is stored into the volatile [blackhole] field, forcing the compiler
     * to materialize the computation.
     */
    @JvmStatic
    fun consume(value: Long) {
        blackhole = value
    }

    /**
     * Consume a Double value to prevent dead-code elimination.
     * Converts to raw long bits to preserve full precision in the volatile sink.
     */
    @JvmStatic
    fun consume(value: Double) {
        blackhole = java.lang.Double.doubleToRawLongBits(value)
    }

    /**
     * Consume an Int value to prevent dead-code elimination.
     */
    @JvmStatic
    fun consume(value: Int) {
        blackhole = value.toLong()
    }

    /**
     * Consume a Float value to prevent dead-code elimination.
     */
    @JvmStatic
    fun consume(value: Float) {
        blackhole = java.lang.Float.floatToRawIntBits(value).toLong()
    }

    /**
     * Run a benchmark block with warm-up iterations followed by 3 timed runs,
     * returning the **median** result.
     *
     * This satisfies:
     * - U-1 (Warm-up Gate): [warmups] untimed iterations let JIT/ART compile the hot path.
     * - U-6 (Statistical Rigor): 3 timed runs with median selection rejects outliers.
     *
     * @param warmups Number of untimed warm-up iterations (default: 2).
     * @param timedRuns Number of timed measurement runs (default: 3). Must be odd for true median.
     * @param block The benchmark function to execute. Must return the measured score as a Double.
     * @return The median score from [timedRuns] executions.
     */
    @JvmStatic
    inline fun medianOfThree(warmups: Int = 2, timedRuns: Int = 3, block: () -> Double): Double {
        // Warm-up phase: let JIT/ART optimize the hot path
        repeat(warmups) {
            block()
        }

        // Timed measurement phase
        val results = DoubleArray(timedRuns) { block() }
        results.sort()

        // Return median (middle element of sorted array)
        return results[timedRuns / 2]
    }

    /**
     * Lightweight variant for GPU/IO tests where full warm-up is expensive.
     * Performs 1 warm-up and 3 timed runs.
     */
    @JvmStatic
    inline fun medianOfThreeLight(block: () -> Double): Double {
        return medianOfThree(warmups = 1, timedRuns = 3, block = block)
    }

    /**
     * Five-run variant for high-variance tests (RC-4).
     * Use for tests with high scheduling noise: context switches, atomic contention.
     * Performs [warmups] warm-up runs then 5 timed runs, returning the median.
     *
     * @param warmups Number of untimed warm-up iterations (default: 3).
     * @param block The benchmark function to execute.
     */
    @JvmStatic
    inline fun medianOfFive(warmups: Int = 3, block: () -> Double): Double {
        return medianOfThree(warmups = warmups, timedRuns = 5, block = block)
    }
}
