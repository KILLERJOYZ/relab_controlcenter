package com.example.relab_tool.data

import com.example.relab_tool.R
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.system.measureTimeMillis

class PerformanceRepository(private val context: Context) {

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking = _isBenchmarking.asStateFlow()

    private val _isStabilityActive = MutableStateFlow(false)
    val isStabilityActive = _isStabilityActive.asStateFlow()

    private val _benchmarkResult = MutableStateFlow<String?>(null)
    val benchmarkResult = _benchmarkResult.asStateFlow()

    private val _throttlingHistory = MutableStateFlow<List<Pair<Long, Int>>>(emptyList())
    val throttlingHistory = _throttlingHistory.asStateFlow()

    @Volatile private var cpuChecksum = 0.0

    private suspend fun runBenchmark(label: Int, work: suspend () -> String) = withContext(Dispatchers.Default) {
        if (!_isBenchmarking.compareAndSet(false, true)) return@withContext
        try {
            _benchmarkResult.value = context.getString(label)
            _benchmarkResult.value = work()
        } catch (cancelled: CancellationException) {
            _benchmarkResult.value = context.getString(R.string.audit_benchmark_cancelled)
            throw cancelled
        } catch (error: Exception) {
            _benchmarkResult.value = context.getString(R.string.audit_benchmark_error)
        } catch (error: OutOfMemoryError) {
            _benchmarkResult.value = context.getString(R.string.audit_benchmark_error)
        } finally {
            _isBenchmarking.value = false
        }
    }

    suspend fun runCpuBenchmark() = runBenchmark(R.string.audit_cpu_running) {
        val size = 500
        val random = java.util.Random(0L)
        val matrixA = Array(size) { DoubleArray(size) { random.nextDouble() } }
        val matrixB = Array(size) { DoubleArray(size) { random.nextDouble() } }
        var checksum = 0.0
        val started = System.nanoTime()
        for (i in 0 until size) {
            currentCoroutineContext().ensureActive()
            for (j in 0 until size) {
                var cell = 0.0
                for (k in 0 until size) cell += matrixA[i][k] * matrixB[k][j]
                checksum += cell
            }
        }
        val elapsedMs = ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(1L)
        cpuChecksum = checksum
        context.getString(R.string.audit_cpu_result, 500000L / elapsedMs, elapsedMs)
    }

    suspend fun runRamBenchmark() = runBenchmark(R.string.audit_ram_running) {
        val size = 50 * 1024 * 1024
        val array = ByteArray(size)
        var checksum = 0L
        val started = System.nanoTime()
        for (i in array.indices) {
            if (i % 1_000_000 == 0) currentCoroutineContext().ensureActive()
            array[i] = (i % 256).toByte()
        }
        for (i in array.indices) {
            if (i % 1_000_000 == 0) currentCoroutineContext().ensureActive()
            checksum += array[i].toLong()
        }
        check(checksum == -size / 2L) { "Memory verification failed" }
        val seconds = (System.nanoTime() - started).coerceAtLeast(1L) / 1_000_000_000.0
        val speed = (2.0 * size / (1024 * 1024)) / seconds
        context.getString(R.string.audit_ram_result, speed, checksum)
    }

    private var stabilityJob: Job? = null
    fun startStabilityTest(scope: CoroutineScope) {
        stabilityJob?.cancel()
        _throttlingHistory.value = emptyList()
        _isStabilityActive.value = true
        stabilityJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val freq = getCurrentMaxFreq()
                    _throttlingHistory.value = _throttlingHistory.value + (System.currentTimeMillis() to freq)
                    if (_throttlingHistory.value.size > 100) {
                        _throttlingHistory.value = _throttlingHistory.value.drop(1)
                    }
                    delay(2000)
                }
            } finally {
                _isStabilityActive.value = false
            }
        }
    }

    fun stopStabilityTest() {
        stabilityJob?.cancel()
        stabilityJob = null
        _isStabilityActive.value = false
    }

    private fun getCurrentMaxFreq(): Int {
        var max = 0
        try {
            val cpuDir = File("/sys/devices/system/cpu/")
            val cpuFiles = cpuDir.listFiles { _, name -> name.matches(Regex("cpu[0-9]+")) } ?: emptyArray()
            for (file in cpuFiles) {
                val f = File(file, "cpufreq/scaling_cur_freq")
                if (f.exists()) {
                    val freq = f.readText().trim().toInt() / 1000
                    if (freq > max) max = freq
                }
            }
        } catch (e: Exception) {}
        return max
    }
}
