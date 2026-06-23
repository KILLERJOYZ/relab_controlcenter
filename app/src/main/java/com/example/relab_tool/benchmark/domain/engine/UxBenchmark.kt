package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.Choreographer
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Random
import kotlin.coroutines.resume

class UxBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.UX_SMOOTHNESS

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        
        // 7a. Compose Stress Render
        onProgress(0.0f)
        val stressRenderMs = runComposeStressRender()
        list.add(SubScore("UI Stress Pacing", stressRenderMs, "ms", ScoreNormalizer.normalize(stressRenderMs, 24.0, 7.0, true)))
        
        // 7b. Scroll Simulation
        onProgress(0.25f)
        val scrollLayoutUs = runTextLayoutSimulation()
        list.add(SubScore("Text Layout Latency", scrollLayoutUs, "µs", ScoreNormalizer.normalize(scrollLayoutUs, 380.0, 90.0, true)))
        
        // 7c. Animation Jitter
        onProgress(0.5f)
        val jitterMs = runAnimationJitter()
        list.add(SubScore("Animation Jitter", jitterMs, "ms", ScoreNormalizer.normalize(jitterMs, 20.0, 5.0, true)))
        
        // 7d. Bitmap Decode
        onProgress(0.75f)
        val decodeSpeed = runBitmapDecodeSpeed()
        list.add(SubScore("Bitmap Decode Speed", decodeSpeed, "images/s", ScoreNormalizer.normalize(decodeSpeed, 14.0, 52.0, false)))
        
        onProgress(1.0f)
        list
    }

    private suspend fun runComposeStressRender(): Double = suspendCancellableCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            try {
                val width = 500
                val height = 500
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val paint = Paint().apply { isAntiAlias = true }
                val random = Random(123)
                
                var frameCount = 0
                var lastFrameTime = 0L
                val intervals = mutableListOf<Long>()
                
                val callback = object : Choreographer.FrameCallback {
                    override fun doFrame(frameTimeNanos: Long) {
                        frameCount++
                        if (lastFrameTime > 0) {
                            intervals.add(frameTimeNanos - lastFrameTime)
                        }
                        lastFrameTime = frameTimeNanos
                        
                        for (i in 0 until 500) {
                            paint.color = random.nextInt()
                            canvas.drawLine(
                                random.nextFloat() * width, random.nextFloat() * height,
                                random.nextFloat() * width, random.nextFloat() * height, paint
                            )
                        }
                        
                        if (frameCount < 100) {
                            Choreographer.getInstance().postFrameCallback(this)
                        } else {
                            bitmap.recycle()
                            if (intervals.isEmpty()) {
                                continuation.resume(16.6)
                            } else {
                                intervals.sort()
                                val p99Index = (intervals.size * 0.99).toInt().coerceIn(0, intervals.size - 1)
                                val p99Ms = intervals[p99Index].toDouble() / 1e6
                                continuation.resume(p99Ms)
                            }
                        }
                    }
                }
                Choreographer.getInstance().postFrameCallback(callback)
            } catch (e: Exception) {
                continuation.resume(20.0)
            }
        }
    }

    private fun runTextLayoutSimulation(): Double {
        val textPaint = TextPaint().apply {
            textSize = 14f
            color = Color.BLACK
        }
        val width = 1080
        val text = "ControlCenter UX benchmark text layout. " +
                "This text simulates rendering paragraphs inside scrolling views. " +
                "We repeat this multiple times to measure average layout and measurement performance. " +
                "Android uses StaticLayout internally for Compose text blocks. " +
                "The quick brown fox jumps over the lazy dog."
        
        val sb = StringBuilder()
        for (i in 0 until 10) {
            sb.append(text).append("\n")
        }
        val fullText = sb.toString()
        
        val iterations = 500
        val startTime = System.nanoTime()
        for (i in 0 until iterations) {
            val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(fullText, 0, fullText.length, textPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0.0f, 1.0f)
                    .setIncludePad(false)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(fullText, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
            }
            val h = layout.height
            if (h == 0) {
                Log.d("UxBenchmark", "h is 0")
            }
        }
        val elapsed = System.nanoTime() - startTime
        return (elapsed.toDouble() / iterations) / 1000.0
    }

    private fun runAnimationJitter(): Double {
        val iterations = 50_000
        val start = System.nanoTime()
        var currentValue = 0f
        val times = DoubleArray(100)
        var sampleIndex = 0
        
        for (i in 0 until iterations) {
            val progress = i.toFloat() / iterations
            currentValue = progress * progress * (3f - 2f * progress)
            if (i % 500 == 0 && sampleIndex < 100) {
                times[sampleIndex++] = System.nanoTime().toDouble()
            }
        }
        
        val elapsed = (System.nanoTime() - start) / 1e6
        val intervals = DoubleArray(99)
        for (i in 0 until 99) {
            intervals[i] = (times[i + 1] - times[i]) / 1e6
        }
        intervals.sort()
        val p99Interval = intervals[98]
        val medianInterval = intervals[50]
        val jitter = Math.abs(p99Interval - medianInterval)
        return if (currentValue > 0f) jitter.coerceIn(0.1, 50.0) else 5.0
    }

    private fun runBitmapDecodeSpeed(): Double {
        return try {
            val width = 256
            val height = 256
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.BLUE)
            val paint = Paint().apply { color = Color.RED }
            canvas.drawCircle(128f, 128f, 64f, paint)
            
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            val jpegBytes = out.toByteArray()
            bitmap.recycle()
            
            val iterations = 40
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                val opts = BitmapFactory.Options().apply {
                    inMutable = true
                }
                val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, opts)
                decoded?.recycle()
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            iterations.toDouble() / elapsed
        } catch (e: Exception) {
            15.0
        }
    }
}
