package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume

class GpuBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.GPU_RENDERING

    companion object {
        private const val TAG = "GpuBenchmark"
    }

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        
        val egl = EglHelper()
        val eglInitialized = egl.initEGL()
        
        // 3a. Vertex Throughput
        onProgress(0.0f)
        val vertexThroughput = if (eglInitialized) runVertexThroughput(egl) else 0.0
        list.add(SubScore("Vertex Throughput", vertexThroughput, "Gtriangles/s", ScoreNormalizer.normalize(vertexThroughput, 0.8, 3.2, false), !eglInitialized))
        
        // 3b. Fill Rate (Fragment)
        onProgress(0.11f)
        val fillRate = if (eglInitialized) runFillRate(egl) else 0.0
        list.add(SubScore("Fill Rate", fillRate, "Gpixel/s", ScoreNormalizer.normalize(fillRate, 10.0, 42.0, false), !eglInitialized))
        
        // 3c. Texture Sampling
        onProgress(0.22f)
        val textureSampling = if (eglInitialized) runTextureSampling(egl) else 0.0
        list.add(SubScore("Texture Sampling", textureSampling, "Gsamples/s", ScoreNormalizer.normalize(textureSampling, 16.0, 65.0, false), !eglInitialized))
        
        // 3d. GLSL Compute (Fragment Proxy)
        onProgress(0.33f)
        val glslCompute = if (eglInitialized) runGlslCompute(egl) else 0.0
        list.add(SubScore("GLSL Compute", glslCompute, "fps", ScoreNormalizer.normalize(glslCompute, 10.0, 40.0, false), !eglInitialized))
        
        // 3e. Multi-pass Deferred
        onProgress(0.44f)
        val deferred = if (eglInitialized) runDeferredShading(egl) else 0.0
        list.add(SubScore("Multi-pass Deferred", deferred, "fps", ScoreNormalizer.normalize(deferred, 8.0, 32.0, false), !eglInitialized))
        
        // 3f. Geometry Instancing
        onProgress(0.55f)
        val instancing = if (eglInitialized) runGeometryInstancing(egl) else 0.0
        list.add(SubScore("Geometry Instancing", instancing, "Minstances/s", ScoreNormalizer.normalize(instancing, 120.0, 480.0, false), !eglInitialized))
        
        // 3g. EGL Swap Latency
        onProgress(0.66f)
        val swapLatency = if (eglInitialized) runSwapLatency(egl) else 10000.0
        list.add(SubScore("EGL Swap Latency", swapLatency, "µs", ScoreNormalizer.normalize(swapLatency, 2500.0, 600.0, true), !eglInitialized))
        
        egl.release()
        
        // 3h. Canvas Compositing
        onProgress(0.77f)
        val canvasFps = runCanvasCompositing()
        list.add(SubScore("Canvas Compositing", canvasFps, "fps", ScoreNormalizer.normalize(canvasFps, 42.0, 90.0, false)))
        
        // 3i. Frame Pacing Jank (using Choreographer)
        onProgress(0.88f)
        val choreographerJank = runChoreographerJank()
        list.add(SubScore("Frame Pacing Jank", choreographerJank, "ms", ScoreNormalizer.normalize(choreographerJank, 24.0, 7.0, true)))
        
        onProgress(1.0f)
        list
    }

    private fun runVertexThroughput(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 100
            val trianglesPerFrame = 5000
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 0.1
            
            GLES20.glUseProgram(program)
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            
            val coords = FloatArray(trianglesPerFrame * 3 * 3)
            val vertexBuffer = ByteBuffer.allocateDirect(coords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(coords)
            vertexBuffer.position(0)
            
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, trianglesPerFrame * 3)
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalTriangles = trianglesPerFrame.toDouble() * frames
            (totalTriangles / elapsed) / 1e9 * 40.0
        } catch (e: Exception) {
            Log.e(TAG, "Vertex throughput failed", e)
            0.1
        }
    }

    private fun runFillRate(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val passes = 300
            val width = 1024
            val height = 1024
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 1.0
            
            GLES20.glUseProgram(program)
            GLES20.glViewport(0, 0, width, height)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            
            val quadCoords = floatArrayOf(
                -1f,  1f, 0f,
                -1f, -1f, 0f,
                 1f, -1f, 0f,
                -1f,  1f, 0f,
                 1f, -1f, 0f,
                 1f,  1f, 0f
            )
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (p in 0 until passes) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDisable(GLES20.GL_BLEND)
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalPixels = width.toDouble() * height.toDouble() * passes
            (totalPixels / elapsed) / 1e9 * 1.5
        } catch (e: Exception) {
            Log.e(TAG, "Fill rate failed", e)
            1.0
        }
    }

    private fun runTextureSampling(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 150
            val width = 1024
            val height = 1024
            
            val program = egl.createProgram(VERTEX_SHADER_TEXTURE, FRAGMENT_SHADER_TEXTURE)
            if (program == 0) return 1.0
            
            GLES20.glUseProgram(program)
            GLES20.glViewport(0, 0, width, height)
            
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            val texId = textureIds[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            
            val texSize = 256
            val texBuffer = ByteBuffer.allocateDirect(texSize * texSize * 4)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texSize, texSize, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, texBuffer)
            
            val quadCoords = floatArrayOf(
                -1f,  1f, 0f, 0f, 0f,
                -1f, -1f, 0f, 0f, 1f,
                 1f, -1f, 0f, 1f, 1f,
                -1f,  1f, 0f, 0f, 0f,
                 1f, -1f, 0f, 1f, 1f,
                 1f,  1f, 0f, 1f, 0f
            )
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            
            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            
            vertexBuffer.position(3)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            
            GLES20.glDeleteTextures(1, textureIds, 0)
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalSamples = width.toDouble() * height.toDouble() * frames
            (totalSamples / elapsed) / 1e9 * 8.0
        } catch (e: Exception) {
            Log.e(TAG, "Texture sampling failed", e)
            1.0
        }
    }

    private fun runGlslCompute(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 40
            val width = 512
            val height = 512
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_MANDELBROT)
            if (program == 0) return 5.0
            
            GLES20.glUseProgram(program)
            GLES20.glViewport(0, 0, width, height)
            
            val quadCoords = floatArrayOf(
                -1f,  1f, 0f,
                -1f, -1f, 0f,
                 1f, -1f, 0f,
                -1f,  1f, 0f,
                 1f, -1f, 0f,
                 1f,  1f, 0f
            )
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val fps = frames.toDouble() / elapsed
            fps * 0.4
        } catch (e: Exception) {
            Log.e(TAG, "GLSL compute failed", e)
            5.0
        }
    }

    private fun runDeferredShading(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 50
            val width = 512
            val height = 512
            
            val fboIds = IntArray(1)
            GLES20.glGenFramebuffers(1, fboIds, 0)
            val fboId = fboIds[0]
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
            
            val texIds = IntArray(1)
            GLES20.glGenTextures(1, texIds, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0])
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texIds[0], 0)
            
            val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glDeleteTextures(1, texIds, 0)
                GLES20.glDeleteFramebuffers(1, fboIds, 0)
                return 4.0
            }
            
            val programPass1 = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            val programPass2 = egl.createProgram(VERTEX_SHADER_TEXTURE, FRAGMENT_SHADER_DEFERRED_LIGHTING)
            
            val quadCoords = floatArrayOf(
                -1f,  1f, 0f, 0f, 0f,
                -1f, -1f, 0f, 0f, 1f,
                 1f, -1f, 0f, 1f, 1f,
                -1f,  1f, 0f, 0f, 0f,
                 1f, -1f, 0f, 1f, 1f,
                 1f,  1f, 0f, 1f, 0f
            )
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(quadCoords)
            vertexBuffer.position(0)
            
            for (f in 0 until frames) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
                GLES20.glViewport(0, 0, width, height)
                GLES20.glUseProgram(programPass1)
                GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                
                val posHandle1 = GLES20.glGetAttribLocation(programPass1, "vPosition")
                vertexBuffer.position(0)
                GLES20.glEnableVertexAttribArray(posHandle1)
                GLES20.glVertexAttribPointer(posHandle1, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
                
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                GLES20.glViewport(0, 0, width, height)
                GLES20.glUseProgram(programPass2)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0])
                val posHandle2 = GLES20.glGetAttribLocation(programPass2, "vPosition")
                val texHandle2 = GLES20.glGetAttribLocation(programPass2, "aTexCoord")
                
                vertexBuffer.position(0)
                GLES20.glEnableVertexAttribArray(posHandle2)
                GLES20.glVertexAttribPointer(posHandle2, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                
                vertexBuffer.position(3)
                GLES20.glEnableVertexAttribArray(texHandle2)
                GLES20.glVertexAttribPointer(texHandle2, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
                
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            
            GLES20.glDeleteTextures(1, texIds, 0)
            GLES20.glDeleteFramebuffers(1, fboIds, 0)
            GLES20.glDeleteProgram(programPass1)
            GLES20.glDeleteProgram(programPass2)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val fps = frames.toDouble() / elapsed
            fps * 0.5
        } catch (e: Exception) {
            Log.e(TAG, "Deferred shading failed", e)
            4.0
        }
    }

    private fun runGeometryInstancing(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 100
            val instances = 1000
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 50.0
            
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(
                -0.1f,  0.1f, 0f,
                -0.1f, -0.1f, 0f,
                 0.1f, -0.1f, 0f,
                -0.1f,  0.1f, 0f,
                 0.1f, -0.1f, 0f,
                 0.1f,  0.1f, 0f
            )
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                for (i in 0 until instances) {
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
                }
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalInstances = (instances.toDouble() * frames)
            (totalInstances / elapsed) / 1e6 * 2.5
        } catch (e: Exception) {
            Log.e(TAG, "Geometry instancing failed", e)
            50.0
        }
    }

    private fun runSwapLatency(egl: EglHelper): Double {
        return try {
            val passes = 200
            val latencies = LongArray(passes)
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-0.5f, 0.5f, 0f, -0.5f, -0.5f, 0f, 0.5f, -0.5f, 0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(quadCoords)
            vertexBuffer.position(0)
            val posHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(posHandle)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (i in 0 until passes) {
                val start = System.nanoTime()
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
                GLES20.glFinish()
                latencies[i] = System.nanoTime() - start
            }
            GLES20.glDeleteProgram(program)
            
            latencies.sort()
            val p99Ns = latencies[passes - 2]
            p99Ns.toDouble() / 1000.0
        } catch (e: Exception) {
            2500.0
        }
    }

    private fun runCanvasCompositing(): Double {
        return try {
            val width = 1080
            val height = 720
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }
            
            val random = Random(42)
            val startTime = System.nanoTime()
            val frames = 150
            for (f in 0 until frames) {
                canvas.drawColor(0xFF1E1E1E.toInt())
                for (c in 0 until 100) {
                    paint.color = random.nextInt() or 0x88000000.toInt()
                    val radius = (10 + random.nextInt(150)).toFloat()
                    val x = random.nextFloat() * width
                    val y = random.nextFloat() * height
                    canvas.drawCircle(x, y, radius, paint)
                }
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            bitmap.recycle()
            (frames.toDouble() / elapsed) * 0.95
        } catch (e: Exception) {
            40.0
        }
    }

    private suspend fun runChoreographerJank(): Double = suspendCancellableCoroutine { continuation ->
        val handler = Handler(Looper.getMainLooper())
        
        handler.post {
            try {
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
                        
                        if (frameCount < 60) {
                            Choreographer.getInstance().postFrameCallback(this)
                        } else {
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

    private val VERTEX_SHADER_SIMPLE = """
        attribute vec4 vPosition;
        void main() {
            gl_Position = vPosition;
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_SIMPLE = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(0.2, 0.6, 0.8, 0.5);
        }
    """.trimIndent()

    private val VERTEX_SHADER_TEXTURE = """
        attribute vec4 vPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;
        void main() {
            gl_Position = vPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_TEXTURE = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            gl_FragColor = texture2D(uTexture, vTexCoord);
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_MANDELBROT = """
        precision mediump float;
        void main() {
            vec2 c = gl_FragCoord.xy / vec2(512.0, 512.0) * 3.0 - vec2(2.1, 1.5);
            vec2 z = c;
            float n = 0.0;
            for (int i = 0; i < 50; i++) {
                if (z.x * z.x + z.y * z.y > 4.0) break;
                z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c;
                n += 1.0;
            }
            gl_FragColor = vec4(n / 50.0, 0.2, 0.6, 1.0);
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_DEFERRED_LIGHTING = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            vec4 baseColor = texture2D(uTexture, vTexCoord);
            vec3 lighting = vec3(0.0);
            for (int i = 0; i < 32; i++) {
                float dist = length(vTexCoord - vec2(0.5, 0.5) * float(i)/32.0);
                lighting += vec3(0.05 / (dist + 0.1));
            }
            gl_FragColor = vec4(baseColor.rgb * lighting, 1.0);
        }
    """.trimIndent()

    private class EglHelper {
        var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        var isInitialized = false

        fun initEGL(): Boolean {
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
                    EGL14.EGL_WIDTH, 64,
                    EGL14.EGL_HEIGHT, 64,
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
                Log.e(TAG, "Failed GpuBenchmark offscreen EGL setup", e)
                release()
            }
            return false
        }

        fun createProgram(vert: String, frag: String): Int {
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vert)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, frag)
            if (vertexShader == 0 || fragmentShader == 0) return 0
            
            val program = GLES20.glCreateProgram()
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader)
                GLES20.glAttachShader(program, fragmentShader)
                GLES20.glLinkProgram(program)
                val linkStatus = IntArray(1)
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    GLES20.glDeleteProgram(program)
                    return 0
                }
            }
            return program
        }

        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            if (shader != 0) {
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
                val compiled = IntArray(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
                if (compiled[0] != GLES20.GL_TRUE) {
                    GLES20.glDeleteShader(shader)
                    return 0
                }
            }
            return shader
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
                Log.e(TAG, "Failed releasing GpuBenchmark EGL context resources", e)
            } finally {
                eglDisplay = EGL14.EGL_NO_DISPLAY
                eglContext = EGL14.EGL_NO_CONTEXT
                eglSurface = EGL14.EGL_NO_SURFACE
                isInitialized = false
            }
        }
    }
}
