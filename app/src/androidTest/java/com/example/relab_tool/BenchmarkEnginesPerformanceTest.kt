package com.example.relab_tool

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import android.util.Log
import com.example.relab_tool.benchmark.domain.engine.*
import com.example.relab_tool.benchmark.domain.model.SubScore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BenchmarkEnginesPerformanceTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun verifyEngineResults(engineName: String, subScores: List<SubScore>) {
        Log.i("BenchmarkEngineTest", "=== Results for $engineName ===")
        assertNotNull("SubScores list should not be null", subScores)
        assertTrue("SubScores list should not be empty", subScores.isNotEmpty())
        
        Log.i("BenchmarkEngineTest", "Total subtests: ${subScores.size}")
        for ((index, subScore) in subScores.withIndex()) {
            Log.i("BenchmarkEngineTest", "  [${index + 1}/20] Name=${subScore.name}, Score=${subScore.score}, RawValue=${subScore.rawValue} ${subScore.unit}")
            assertFalse("SubScore name should not be blank", subScore.name.isBlank())
            
            // Validate that score is a valid double and not NaN or infinite
            val scoreDouble = subScore.score.toDouble()
            assertTrue("SubScore value should not be NaN or infinite", !scoreDouble.isNaN() && !scoreDouble.isInfinite())
            // Check that it's a non-negative score
            assertTrue("SubScore should be >= 0", scoreDouble >= 0.0)

            // Validate that if raw value is 0.0 or invalid, the score is partial
            if (subScore.rawValue == 0.0 || subScore.rawValue.isNaN() || subScore.rawValue.isInfinite()) {
                assertTrue("SubScore with zero or invalid raw value should be marked as partial", subScore.isPartial)
            }
        }
    }

    @Test
    fun testCpuSingleCoreBenchmark() = runBlocking {
        val engine = CpuSingleCoreBenchmark()
        assertTrue(engine.isAvailable())
        val results = engine.run { progress ->
            Log.d("BenchmarkEngineTest", "CpuSingleCore progress: ${(progress * 100).toInt()}%")
        }
        verifyEngineResults("CpuSingleCoreBenchmark", results)
    }

    @Test
    fun testCpuMultiCoreBenchmark() = runBlocking {
        val engine = CpuMultiCoreBenchmark()
        assertTrue(engine.isAvailable())
        val results = engine.run { progress ->
            Log.d("BenchmarkEngineTest", "CpuMultiCore progress: ${(progress * 100).toInt()}%")
        }
        verifyEngineResults("CpuMultiCoreBenchmark", results)
    }

    @Test
    fun testGpuOpenGLBenchmark() = runBlocking {
        val engine = GpuOpenGLBenchmark(context)
        assertTrue(engine.isAvailable())
        val results = engine.run { progress ->
            Log.d("BenchmarkEngineTest", "GpuOpenGL progress: ${(progress * 100).toInt()}%")
        }
        verifyEngineResults("GpuOpenGLBenchmark", results)
    }

    @Test
    fun testGpuVulkanBenchmark() = runBlocking {
        val engine = GpuVulkanBenchmark(context)
        assertTrue(engine.isAvailable())
        val results = engine.run { progress ->
            Log.d("BenchmarkEngineTest", "GpuVulkan progress: ${(progress * 100).toInt()}%")
        }
        verifyEngineResults("GpuVulkanBenchmark", results)
    }

    @Test
    fun testStorageBenchmark() = runBlocking {
        val engine = StorageBenchmark(context)
        assertTrue(engine.isAvailable())
        val results = engine.run { progress ->
            Log.d("BenchmarkEngineTest", "Storage progress: ${(progress * 100).toInt()}%")
        }
        verifyEngineResults("StorageBenchmark", results)
    }

    @Test
    fun testVideoCodecBenchmark() = runBlocking {
        val engine = VideoCodecBenchmark()
        assertTrue(engine.isAvailable())
        val results = engine.run { progress ->
            Log.d("BenchmarkEngineTest", "VideoCodec progress: ${(progress * 100).toInt()}%")
        }
        verifyEngineResults("VideoCodecBenchmark", results)
    }

    @Test
    fun testNetworkIpcBenchmark() = runBlocking {
        val engine = NetworkIpcBenchmark()
        assertTrue(engine.isAvailable())
        val results = engine.run { progress ->
            Log.d("BenchmarkEngineTest", "NetworkIpc progress: ${(progress * 100).toInt()}%")
        }
        verifyEngineResults("NetworkIpcBenchmark", results)
    }

    @Test
    fun testVideoCodecBenchmarkTruth() = runBlocking {
        val engine = VideoCodecBenchmark()
        assertTrue(engine.isAvailable())
        val results = engine.run { }
        val aacEncode = results.find { it.name.contains("AAC Encode") }
        val aacDecode = results.find { it.name.contains("AAC Decode") }
        if (aacEncode != null && aacDecode != null && aacEncode.rawValue > 0.0 && aacDecode.rawValue > 0.0) {
            val ratio = aacDecode.rawValue / aacEncode.rawValue
            assertTrue("AAC Decode should not be a trivial multiple (6x) of Encode", Math.abs(ratio - 6.0) > 1e-4)
        }
    }

    @Test
    fun testGpuVulkanBenchmarkTruth() = runBlocking {
        val engine = GpuVulkanBenchmark(context)
        assertTrue(engine.isAvailable())
        val results = engine.run { }
        for (subScore in results) {
            assertTrue("Vulkan/compute subtest raw value should be non-negative", subScore.rawValue >= 0.0)
        }
    }

    @Test
    fun testStorageBenchmarkTruth() = runBlocking {
        val engine = StorageBenchmark(context)
        assertTrue(engine.isAvailable())
        val results = engine.run { }
        val randomRead = results.find { it.name.contains("Random Read") }
        if (randomRead != null && !randomRead.isPartial) {
            assertTrue("Random read should have non-zero throughput", randomRead.rawValue > 0.0)
        }
    }
}
