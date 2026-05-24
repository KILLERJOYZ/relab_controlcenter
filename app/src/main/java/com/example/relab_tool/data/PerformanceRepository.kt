package com.example.relab_tool.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.system.measureTimeMillis

class PerformanceRepository(private val context: Context) {

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking = _isBenchmarking.asStateFlow()

    private val _benchmarkResult = MutableStateFlow<String?>(null)
    val benchmarkResult = _benchmarkResult.asStateFlow()

    private val _throttlingHistory = MutableStateFlow<List<Pair<Long, Int>>>(emptyList())
    val throttlingHistory = _throttlingHistory.asStateFlow()

    fun runCpuBenchmark() = CoroutineScope(Dispatchers.Default).launch {
        _isBenchmarking.value = true
        _benchmarkResult.value = "Running CPU Benchmark..."
        
        val size = 500
        val matrixA = Array(size) { DoubleArray(size) { Math.random() } }
        val matrixB = Array(size) { DoubleArray(size) { Math.random() } }
        val result = Array(size) { DoubleArray(size) }

        val time = measureTimeMillis {
            for (i in 0 until size) {
                for (j in 0 until size) {
                    for (k in 0 until size) {
                        result[i][j] += matrixA[i][k] * matrixB[k][j]
                    }
                }
            }
        }

        _benchmarkResult.value = "CPU Score: ${500000 / time.coerceAtLeast(1)} units (${time}ms)"
        _isBenchmarking.value = false
    }

    fun runRamBenchmark() = CoroutineScope(Dispatchers.Default).launch {
        _isBenchmarking.value = true
        _benchmarkResult.value = "Running RAM Benchmark..."

        val size = 50 * 1024 * 1024 // 50MB
        val array = ByteArray(size)
        
        val time = measureTimeMillis {
            for (i in 0 until size) {
                array[i] = (i % 256).toByte()
            }
            var sum = 0L
            for (i in 0 until size) {
                sum += array[i].toLong()
            }
        }

        val speed = (size.toDouble() / 1024 / 1024) / (time.toDouble() / 1000)
        _benchmarkResult.value = "RAM Speed: %.2f MB/s".format(speed)
        _isBenchmarking.value = false
    }

    private var stabilityJob: Job? = null
    fun startStabilityTest() {
        stabilityJob?.cancel()
        _throttlingHistory.value = emptyList()
        stabilityJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val freq = getCurrentMaxFreq()
                _throttlingHistory.value = _throttlingHistory.value + (System.currentTimeMillis() to freq)
                if (_throttlingHistory.value.size > 100) {
                    _throttlingHistory.value = _throttlingHistory.value.drop(1)
                }
                delay(2000)
            }
        }
    }

    fun stopStabilityTest() {
        stabilityJob?.cancel()
        stabilityJob = null
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
