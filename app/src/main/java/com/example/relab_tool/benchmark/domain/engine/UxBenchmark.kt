package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import android.graphics.*
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

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        
        // 1. UI Stress Pacing
        onProgress(0.00f)
        val stressRenderMs = runComposeStressRender()
        list.add(SubScore("UI Stress Pacing", stressRenderMs, "ms", ScoreNormalizer.normalize(stressRenderMs, 24.0, 7.0, true)))
        
        // 2. Text Layout Latency
        onProgress(0.05f)
        val scrollLayoutUs = BenchmarkHarness.medianOfThree { runTextLayoutSimulation() }
        list.add(SubScore("Text Layout Latency", scrollLayoutUs, "µs", ScoreNormalizer.normalize(scrollLayoutUs, 380.0, 90.0, true)))
        
        // 3. Animation Jitter
        onProgress(0.10f)
        val jitterMs = runAnimationJitter()
        list.add(SubScore("Animation Jitter", jitterMs, "ms", ScoreNormalizer.normalize(jitterMs, 20.0, 5.0, true)))
        
        // 4. Bitmap Decode (JPEG)
        onProgress(0.15f)
        val jpegDecodeSpeed = BenchmarkHarness.medianOfThree { runBitmapDecodeSpeed(Bitmap.CompressFormat.JPEG) }
        list.add(SubScore("Bitmap Decode (JPEG)", jpegDecodeSpeed, "imgs/s", ScoreNormalizer.normalize(jpegDecodeSpeed, 15.0, 60.0, false)))
        
        // 5. Bitmap Decode (PNG)
        onProgress(0.20f)
        val pngDecodeSpeed = BenchmarkHarness.medianOfThree { runBitmapDecodeSpeed(Bitmap.CompressFormat.PNG) }
        list.add(SubScore("Bitmap Decode (PNG)", pngDecodeSpeed, "imgs/s", ScoreNormalizer.normalize(pngDecodeSpeed, 10.0, 40.0, false)))
        
        // 6. Bitmap Scale
        onProgress(0.25f)
        val scaleVal = BenchmarkHarness.medianOfThree { runBitmapScale() }
        list.add(SubScore("Bitmap Scaling Throughput", scaleVal, "imgs/s", ScoreNormalizer.normalize(scaleVal, 20.0, 100.0, false)))
        
        // 7. View Inflation Sim
        onProgress(0.30f)
        val inflationVal = BenchmarkHarness.medianOfThree { runViewInflationSimulation() }
        list.add(SubScore("View Layout Simulation", inflationVal, "ms", ScoreNormalizer.normalize(inflationVal, 50.0, 5.0, true)))
        
        // 8. Touch Response Latency
        onProgress(0.35f)
        val touchLatency = runTouchResponseLatency()
        list.add(SubScore("Frame Dispatch Latency", touchLatency, "ms", ScoreNormalizer.normalize(touchLatency, 10.0, 1.0, true)))
        
        // 9. Complex Path Drawing
        onProgress(0.40f)
        val pathDrawVal = BenchmarkHarness.medianOfThree { runComplexPathDrawing() }
        list.add(SubScore("Complex Path Drawing", pathDrawVal, "ms", ScoreNormalizer.normalize(pathDrawVal, 50.0, 5.0, true)))
        
        // 10. Shadow/Elevation
        onProgress(0.45f)
        val shadowVal = BenchmarkHarness.medianOfThree { runShadowElevation() }
        list.add(SubScore("Shadow Rendering Cost", shadowVal, "ms", ScoreNormalizer.normalize(shadowVal, 40.0, 4.0, true)))
        
        // 11. Gradient Fill
        onProgress(0.50f)
        val gradientVal = BenchmarkHarness.medianOfThree { runGradientFill() }
        list.add(SubScore("Gradient Fill Throughput", gradientVal, "ms", ScoreNormalizer.normalize(gradientVal, 30.0, 3.0, true)))
        
        // 12. Text Rendering (varied fonts)
        onProgress(0.55f)
        val fontVal = BenchmarkHarness.medianOfThree { runTextRenderingVaried() }
        list.add(SubScore("Varied Text Rendering", fontVal, "ms", ScoreNormalizer.normalize(fontVal, 50.0, 5.0, true)))
        
        // 13. Canvas Clip Operations
        onProgress(0.60f)
        val clipVal = BenchmarkHarness.medianOfThree { runCanvasClipOps() }
        list.add(SubScore("Canvas Clip Operations", clipVal, "ms", ScoreNormalizer.normalize(clipVal, 40.0, 4.0, true)))
        
        // 14. Color Matrix Filter
        onProgress(0.65f)
        val filterVal = BenchmarkHarness.medianOfThree { runColorMatrixFilter() }
        list.add(SubScore("Color Matrix Filtering", filterVal, "ms", ScoreNormalizer.normalize(filterVal, 80.0, 8.0, true)))
        
        // 15. Bitmap Alpha Blend
        onProgress(0.70f)
        val blendVal = BenchmarkHarness.medianOfThree { runBitmapAlphaBlend() }
        list.add(SubScore("Alpha Compositing Speed", blendVal, "ms", ScoreNormalizer.normalize(blendVal, 60.0, 6.0, true)))
        
        // 16. Anti-aliased Circle Fill
        onProgress(0.75f)
        val aaCircleVal = BenchmarkHarness.medianOfThree { runAntiAliasedCircles() }
        list.add(SubScore("AA Circle Rasterization", aaCircleVal, "ms", ScoreNormalizer.normalize(aaCircleVal, 50.0, 5.0, true)))
        
        // 17. Canvas Save/Restore
        onProgress(0.80f)
        val saveRestoreVal = BenchmarkHarness.medianOfThree { runCanvasSaveRestore() }
        list.add(SubScore("Canvas State Transforms", saveRestoreVal, "ms", ScoreNormalizer.normalize(saveRestoreVal, 30.0, 3.0, true)))
        
        // 18. Arc Drawing
        onProgress(0.85f)
        val arcVal = BenchmarkHarness.medianOfThree { runArcDrawing() }
        list.add(SubScore("Canvas Arc Drawing", arcVal, "ms", ScoreNormalizer.normalize(arcVal, 40.0, 4.0, true)))
        
        // 19. Multi-line Text Measure
        onProgress(0.90f)
        val multiLineVal = BenchmarkHarness.medianOfThree { runMultiLineTextMeasure() }
        list.add(SubScore("Multi-line Text Measure", multiLineVal, "ms", ScoreNormalizer.normalize(multiLineVal, 60.0, 6.0, true)))
        
        // 20. Paint Style Switching
        onProgress(0.95f)
        val styleVal = BenchmarkHarness.medianOfThree { runPaintStyleSwitching() }
        list.add(SubScore("Paint State Switching", styleVal, "ms", ScoreNormalizer.normalize(styleVal, 30.0, 3.0, true)))

        onProgress(1.00f)
        list
    }

    private suspend fun runComposeStressRender(): Double = suspendCancellableCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            try {
                val width = 256
                val height = 256
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
                        
                        for (i in 0 until 100) {
                            paint.color = random.nextInt()
                            canvas.drawLine(
                                random.nextFloat() * width, random.nextFloat() * height,
                                random.nextFloat() * width, random.nextFloat() * height, paint
                            )
                        }
                        
                        if (frameCount < 40) {
                            if (continuation.isActive) {
                                Choreographer.getInstance().postFrameCallback(this)
                            } else {
                                bitmap.recycle()
                            }
                        } else {
                            bitmap.recycle()
                            if (continuation.isActive) {
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
                }
                Choreographer.getInstance().postFrameCallback(callback)
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(20.0)
                }
            }
        }
    }

    private fun runTextLayoutSimulation(): Double {
        val textPaint = TextPaint().apply {
            textSize = 14f
            color = Color.BLACK
        }
        val width = 1080
        val text = "ControlCenter UX benchmark text layout."
        val sb = StringBuilder()
        for (i in 0 until 5) sb.append(text).append("\n")
        val fullText = sb.toString()
        
        val iterations = 100
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
        }
        val elapsed = System.nanoTime() - startTime
        return (elapsed.toDouble() / iterations) / 1000.0
    }

    private fun runAnimationJitter(): Double {
        val iterations = 10_000
        val start = System.nanoTime()
        var currentValue = 0f
        val times = DoubleArray(50)
        var sampleIndex = 0
        
        for (i in 0 until iterations) {
            val progress = i.toFloat() / iterations
            currentValue = progress * progress * (3f - 2f * progress)
            if (i % 200 == 0 && sampleIndex < 50) {
                times[sampleIndex++] = System.nanoTime().toDouble()
            }
        }
        
        val elapsed = (System.nanoTime() - start) / 1e6
        val intervals = DoubleArray(49)
        for (i in 0 until 49) {
            intervals[i] = (times[i + 1] - times[i]) / 1e6
        }
        intervals.sort()
        val p99Interval = intervals[48]
        val medianInterval = intervals[25]
        return Math.abs(p99Interval - medianInterval).coerceIn(0.1, 50.0)
    }

    private fun runBitmapDecodeSpeed(format: Bitmap.CompressFormat): Double {
        return try {
            val width = 128
            val height = 128
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.BLUE)
            
            val out = ByteArrayOutputStream()
            bitmap.compress(format, 90, out)
            val bytes = out.toByteArray()
            bitmap.recycle()
            
            val iterations = 20
            val startTime = System.nanoTime()
            for (i in 0 until iterations) {
                val opts = BitmapFactory.Options().apply { inMutable = true }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                decoded?.recycle()
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            iterations.toDouble() / elapsed
        } catch (e: Exception) {
            10.0
        }
    }

    private fun runBitmapScale(): Double {
        return try {
            val src = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val startTime = System.nanoTime()
            for (i in 0 until 20) {
                val dst = Bitmap.createScaledBitmap(src, 128, 128, true)
                dst.recycle()
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            src.recycle()
            20.0 / elapsed
        } catch (e: Exception) {
            10.0
        }
    }

    private fun runViewInflationSimulation(): Double {
        val startTime = System.nanoTime()
        for (i in 0 until 50) {
            val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            canvas.drawRect(Rect(0, 0, 100, 100), paint)
            bitmap.recycle()
        }
        val elapsed = System.nanoTime() - startTime
        return elapsed.toDouble() / 1e6
    }

    private fun runTouchResponseLatency(): Double {
        // Touch events simulation latency
        val start = System.nanoTime()
        var temp = 0f
        for (i in 0 until 5000) {
            temp += Math.sin(i.toDouble()).toFloat()
        }
        val elapsed = System.nanoTime() - start
        return elapsed.toDouble() / 1e6
    }

    private fun runComplexPathDrawing(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { style = Paint.Style.STROKE }
        val path = Path()
        path.moveTo(0f, 0f)
        for (i in 0 until 50) {
            path.quadTo(i * 5f, i * 2f, i * 4f, i * 5f)
        }
        val startTime = System.nanoTime()
        for (i in 0 until 20) {
            canvas.drawPath(path, paint)
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runShadowElevation(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            setShadowLayer(10f, 5f, 5f, Color.GRAY)
        }
        val startTime = System.nanoTime()
        for (i in 0 until 20) {
            canvas.drawCircle(128f, 128f, 50f, paint)
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runGradientFill(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, 256f, 256f, Color.RED, Color.BLUE, Shader.TileMode.CLAMP)
        }
        val startTime = System.nanoTime()
        for (i in 0 until 20) {
            canvas.drawRect(0f, 0f, 256f, 256f, paint)
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runTextRenderingVaried(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val startTime = System.nanoTime()
        for (i in 0 until 50) {
            canvas.drawText("Varied Text Rendering $i", 10f, i * 4f, paint)
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runCanvasClipOps(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val path = Path().apply { addCircle(128f, 128f, 60f, Path.Direction.CW) }
        val paint = Paint()
        val startTime = System.nanoTime()
        for (i in 0 until 20) {
            canvas.save()
            canvas.clipPath(path)
            canvas.drawColor(Color.YELLOW)
            canvas.restore()
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runColorMatrixFilter(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        val startTime = System.nanoTime()
        for (i in 0 until 10) {
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runBitmapAlphaBlend(): Double {
        val src = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val dst = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dst)
        val paint = Paint().apply { alpha = 128 }
        val startTime = System.nanoTime()
        for (i in 0 until 20) {
            canvas.drawBitmap(src, 0f, 0f, paint)
        }
        val elapsed = System.nanoTime() - startTime
        src.recycle()
        dst.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runAntiAliasedCircles(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.RED
        }
        val startTime = System.nanoTime()
        for (i in 0 until 50) {
            canvas.drawCircle(i * 5f, i * 5f, 20f, paint)
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runCanvasSaveRestore(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val startTime = System.nanoTime()
        for (i in 0 until 200) {
            canvas.save()
            canvas.translate(10f, 10f)
            canvas.rotate(5f)
            canvas.restore()
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runArcDrawing(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rect = RectF(10f, 10f, 200f, 200f)
        val paint = Paint().apply { isAntiAlias = true }
        val startTime = System.nanoTime()
        for (i in 0 until 50) {
            canvas.drawArc(rect, 0f, 270f, true, paint)
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }

    private fun runMultiLineTextMeasure(): Double {
        val textPaint = TextPaint().apply { textSize = 16f }
        val text = "Lorem ipsum dolor sit amet. ".repeat(10)
        val startTime = System.nanoTime()
        for (i in 0 until 100) {
            val width = textPaint.measureText(text)
        }
        val elapsed = System.nanoTime() - startTime
        return elapsed.toDouble() / 1e6
    }

    private fun runPaintStyleSwitching(): Double {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        val startTime = System.nanoTime()
        for (i in 0 until 200) {
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, 100f, 100f, paint)
            paint.style = Paint.Style.STROKE
            canvas.drawRect(0f, 0f, 100f, 100f, paint)
        }
        val elapsed = System.nanoTime() - startTime
        bitmap.recycle()
        return elapsed.toDouble() / 1e6
    }
}
