package com.example.relab_tool.benchmark.domain.engine

/**
 * BenchmarkNativeBridge — Kotlin-side JNI interface for the rlcc_bench native library.
 *
 * Provides access to:
 *   - O_DIRECT file I/O operations (bypasses Linux kernel page cache)
 *   - JNI roundtrip overhead measurement (empty void call)
 *   - Native ALU and FPU throughput loops (immune to ART JIT DCE)
 *
 * If the native library fails to load (e.g., unsupported ABI, missing NDK build),
 * all functions return -1.0 and [isAvailable] returns false.
 * Callers must check [isAvailable] and implement Kotlin fallbacks.
 */
object BenchmarkNativeBridge {

    private var _isAvailable = false

    /** True if the native library loaded successfully. */
    val isAvailable: Boolean get() = _isAvailable

    init {
        try {
            System.loadLibrary("rlcc_bench")
            _isAvailable = true
        } catch (e: UnsatisfiedLinkError) {
            _isAvailable = false
        }
    }

    // ── IO_11: O_DIRECT sequential write ───────────────────────────────────────
    /**
     * Write [sizeBytes] to [filePath] using O_DIRECT (bypasses page cache).
     * @return Throughput in MB/s, or -1.0 on error.
     */
    @JvmStatic
    external fun nativeODirectWrite(filePath: String, sizeBytes: Long): Double

    // ── IO_12: O_DIRECT sequential read ────────────────────────────────────────
    /**
     * Read [sizeBytes] from [filePath] using O_DIRECT.
     * @return Throughput in MB/s, or -1.0 on error.
     */
    @JvmStatic
    external fun nativeODirectRead(filePath: String, sizeBytes: Long): Double

    // ── IO_05/IO_06: O_DIRECT random 4KB reads ─────────────────────────────────
    /**
     * Perform [opCount] random 4KB O_DIRECT reads from [filePath].
     * @return IOPS (operations per second), or -1.0 on error.
     */
    @JvmStatic
    external fun nativeODirectRandomRead(filePath: String, opCount: Int): Double

    @JvmStatic
    external fun nativeEvictCache(filePath: String): Boolean


    // ── JNI roundtrip overhead measurement (formerly SC_20, now unused as a test) ──
    /** Empty function used as the target for JNI overhead measurement. */
    @JvmStatic
    external fun nativeJniVoidCall()

    // ── Test 01: Native 64-bit integer ALU ────────────────────────────────────
    /**
     * Run [iterations] chained 64-bit integer operations.
     * @return Throughput in Giga-operations/second.
     */
    @JvmStatic
    external fun nativeIntAluGops(iterations: Long): Double

    // ── Test 02: Native double-precision FPU ──────────────────────────────────
    /**
     * Run [iterations] double-precision transcendental operations (sin/cos/log).
     * @return Throughput in Mega-operations/second.
     */
    @JvmStatic
    external fun nativeFpuMops(iterations: Long): Double

    // ── Safe wrappers that return -1.0 if native not available ─────────────────

    fun safeODirectWrite(filePath: String, sizeBytes: Long): Double =
        if (_isAvailable) nativeODirectWrite(filePath, sizeBytes) else -1.0

    fun safeODirectRead(filePath: String, sizeBytes: Long): Double =
        if (_isAvailable) nativeODirectRead(filePath, sizeBytes) else -1.0

    fun safeODirectRandomRead(filePath: String, opCount: Int): Double =
        if (_isAvailable) nativeODirectRandomRead(filePath, opCount) else -1.0

    fun safeEvictCache(filePath: String): Boolean =
        if (_isAvailable) nativeEvictCache(filePath) else false


    /**
     * Measure JNI call overhead in nanoseconds per roundtrip.
     * Calls [nativeJniVoidCall] [count] times and measures wall time.
     */
    fun measureJniOverheadNs(count: Int = 1_000_000): Double {
        if (!_isAvailable) return -1.0
        val start = System.nanoTime()
        repeat(count) { nativeJniVoidCall() }
        val elapsed = System.nanoTime() - start
        return elapsed.toDouble() / count
    }

    fun safeIntAluGops(iterations: Long): Double =
        if (_isAvailable) nativeIntAluGops(iterations) else -1.0

    fun safeFpuMops(iterations: Long): Double =
        if (_isAvailable) nativeFpuMops(iterations) else -1.0
}
