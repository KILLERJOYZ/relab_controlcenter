package com.example.relab_tool.utils

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.Choreographer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/**
 * Google Play Store-compliant manager component that calculates an aggregate,
 * real-time "System Graphics Stress Index" using safe public Android graphics and performance APIs.
 */
class GraphicsPressureTelemetryManager(private val context: Context) {

    companion object {
        private const val TAG = "GraphicsPressure"
    }

    /**
     * Descriptive data model encapsulating fused graphics performance telemetry status.
     */
    data class GraphicsStressState(
        val stressIndex: Int = 0,
        val gpuLatencyNs: Long = 0L,
        val thermalHeadroom: Float = 0f,
        val jankPercentage: Float = 0f,
        val statusLabel: String = "Loading"
    )

    private val _graphicsStressStream = MutableStateFlow(GraphicsStressState())
    val graphicsStressStream: StateFlow<GraphicsStressState> = _graphicsStressStream.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    private val choreographerTracker = ChoreographerTracker(scope)
    private val eglProbe = EglLatencyProbe()
    private val thermalMonitor = ThermalMonitor(context)

    /**
     * Starts the telemetry channels and the background fusion processing loop.
     *
     * @param intervalMs Telemetry loop duration step in milliseconds.
     */
    @Synchronized
    fun startTelemetry(intervalMs: Long = 1000L) {
        if (pollingJob != null) return // Already active

        choreographerTracker.start()
        eglProbe.initEGL()

        pollingJob = scope.launch {
            while (true) {
                try {
                    // 1. Measure GPU Latency via EGL Probe
                    val latencyNs = eglProbe.measureWorkloadLatency()
                    val gpuStress = eglProbe.getGpuStressFactor(latencyNs)

                    // 2. Query thermal status
                    val headroom = thermalMonitor.getThermalHeadroom()
                    val thermalStress = thermalMonitor.getThermalStressFactor(headroom)

                    // 3. Compute Choreographer pacing jitter
                    val jankPercent = choreographerTracker.getJankPercentage()

                    // Fusion Engine algorithm:
                    // GPU latency stretch carries 50% weight (direct rendering bottlenecks).
                    // Choreographer pacing jitter carries 30% weight (system-wide frame synchronization).
                    // Thermal boundary limits carry 20% weight (proactive hardware throttles).
                    val finalStressIndex = ((gpuStress * 0.5f) + (jankPercent * 0.3f) + (thermalStress * 0.2f)).toInt().coerceIn(0, 100)

                    val label = when {
                        finalStressIndex < 20 -> "GPU Status: Light Workload"
                        finalStressIndex < 50 -> "GPU Status: Normal Operations"
                        finalStressIndex < 85 -> "GPU Status: Heavy System Congestion"
                        else -> "GPU Status: Operating System Throttled"
                    }

                    _graphicsStressStream.value = GraphicsStressState(
                        stressIndex = finalStressIndex,
                        gpuLatencyNs = latencyNs,
                        thermalHeadroom = headroom,
                        jankPercentage = jankPercent,
                        statusLabel = label
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during telemetry collection loop", e)
                }

                delay(intervalMs)
            }
        }
    }

    /**
     * Safely tears down and releases resources associated with EGL context and frame pacing listeners.
     */
    @Synchronized
    fun stopTelemetry() {
        pollingJob?.cancel()
        pollingJob = null
        choreographerTracker.stop()
        eglProbe.release()
    }

    /**
     * Channel 1: Offscreen GL Pbuffer execution probe.
     */
    private class EglLatencyProbe {
        private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        private var isInitialized = false

        private var baselineLatencyNs: Long = -1L

        fun initEGL(): Boolean {
            if (isInitialized) return true
            try {
                eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

                val version = IntArray(2)
                if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

                val configAttr = intArrayOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
                )

                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (!EGL14.eglChooseConfig(eglDisplay, configAttr, 0, configs, 0, 1, numConfigs, 0)) return false
                val config = configs[0] ?: return false

                val surfaceAttr = intArrayOf(
                    EGL14.EGL_WIDTH, 1,
                    EGL14.EGL_HEIGHT, 1,
                    EGL14.EGL_NONE
                )
                eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, surfaceAttr, 0)
                if (eglSurface == EGL14.EGL_NO_SURFACE) return false

                val contextAttr = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
                )
                eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttr, 0)
                if (eglContext == EGL14.EGL_NO_CONTEXT) return false

                if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return false

                isInitialized = true
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed offscreen EGL setup", e)
                release()
            }
            return false
        }

        fun measureWorkloadLatency(): Long {
            if (!isInitialized && !initEGL()) return 0L
            try {
                if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) return 0L

                val startTime = System.nanoTime()

                // Render a light, deterministic workload sequence
                GLES20.glClearColor(0.2f, 0.3f, 0.4f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                for (i in 0 until 5) {
                    GLES20.glViewport(0, 0, 1, 1)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                }

                // Force queue depletion and block until operations execute fully on the hardware
                GLES20.glFinish()

                val endTime = System.nanoTime()
                val duration = endTime - startTime

                // Dynamically establish minimum clean response latency as baseline
                if (baselineLatencyNs <= 0L || duration < baselineLatencyNs) {
                    baselineLatencyNs = duration
                }

                return duration
            } catch (e: Exception) {
                Log.e(TAG, "EGL graphics computation run failed", e)
                isInitialized = false
            }
            return 0L
        }

        fun getGpuStressFactor(currentLatency: Long): Int {
            if (currentLatency <= 0L || baselineLatencyNs <= 0L) return 0
            // If latency stretches to 4x baseline (stretchRatio >= 3.0), we treat it as 100% stress
            val stretchRatio = (currentLatency - baselineLatencyNs).toFloat() / baselineLatencyNs
            return (stretchRatio * 100f / 3.0f).toInt().coerceIn(0, 100)
        }

        fun release() {
            try {
                if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    if (eglSurface != EGL14.EGL_NO_SURFACE) {
                        EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    }
                    if (eglContext != EGL14.EGL_NO_CONTEXT) {
                        EGL14.eglDestroyContext(eglDisplay, eglContext)
                    }
                    EGL14.eglTerminate(eglDisplay)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed releasing EGL context resources", e)
            } finally {
                eglDisplay = EGL14.EGL_NO_DISPLAY
                eglContext = EGL14.EGL_NO_CONTEXT
                eglSurface = EGL14.EGL_NO_SURFACE
                isInitialized = false
            }
        }
    }

    /**
     * Channel 2: Thermal Boundary mapping.
     */
    private class ThermalMonitor(private val context: Context) {
        private val powerManager: PowerManager? = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

        fun getThermalHeadroom(): Float {
            if (powerManager == null) return 0f
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    powerManager.getThermalHeadroom(0)
                } catch (e: Exception) {
                    getThermalHeadroomFallback()
                }
            } else {
                getThermalHeadroomFallback()
            }
        }

        private fun getThermalHeadroomFallback(): Float {
            // Fallback for API levels < 30 using Battery Temp as proxy
            // Battery temp returns tenths of degree Celsius (e.g. 350 for 35C).
            // Normal operating temperature range 32C - 42C scaled linearly.
            try {
                val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                val intent = context.registerReceiver(null, filter)
                if (intent != null) {
                    val rawTemp = intent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)
                    val tempCelsius = rawTemp / 10.0f
                    val normalized = (tempCelsius - 32.0f) / 10.0f // Map 32C -> 0.0, 42C -> 1.0
                    return normalized.coerceIn(0.0f, 1.5f)
                }
            } catch (e: Exception) {
                // Fail silently
            }
            return 0f
        }

        fun getThermalStressFactor(headroom: Float): Int {
            // Values close to or above 1.0 indicate device throttling
            return (headroom * 100f).toInt().coerceIn(0, 100)
        }
    }

    /**
     * Channel 3: Choreographer compositor frame jitter calculation.
     */
    private class ChoreographerTracker(private val scope: CoroutineScope) {
        private var isRunning = false
        private var lastFrameTimeNanos: Long = 0L
        private val frameIntervals = ArrayDeque<Long>(60)
        private var frameCallback: Choreographer.FrameCallback? = null

        fun start() {
            if (isRunning) return
            isRunning = true

            frameCallback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!isRunning) return
                    if (lastFrameTimeNanos > 0L) {
                        val interval = frameTimeNanos - lastFrameTimeNanos
                        synchronized(frameIntervals) {
                            if (frameIntervals.size >= 60) {
                                frameIntervals.poll()
                            }
                            frameIntervals.offer(interval)
                        }
                    }
                    lastFrameTimeNanos = frameTimeNanos
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }

            scope.launch(Dispatchers.Main) {
                Choreographer.getInstance().postFrameCallback(frameCallback!!)
            }
        }

        fun stop() {
            isRunning = false
            frameCallback?.let {
                Choreographer.getInstance().removeFrameCallback(it)
            }
            lastFrameTimeNanos = 0L
            synchronized(frameIntervals) {
                frameIntervals.clear()
            }
        }

        fun getJankPercentage(): Float {
            val intervals = synchronized(frameIntervals) { frameIntervals.toList() }
            if (intervals.size < 10) return 0f

            // Auto-detect average render refresh pacing
            val avgInterval = intervals.average()
            if (avgInterval <= 0.0) return 0f

            // A frame is classified as stutter/jank if its duration exceeds 1.5x average frame pacing
            val jankThreshold = avgInterval * 1.5
            val jankCount = intervals.count { it > jankThreshold }
            return (jankCount.toFloat() / intervals.size) * 100f
        }
    }
}
