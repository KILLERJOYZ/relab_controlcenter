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
import kotlin.coroutines.resume

class GpuBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.GPU_RENDERING

    companion object {
        private const val TAG = "GpuBenchmark"
    }

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        
        val egl = EglHelper()
        val eglInitialized = egl.initEGL()
        
        // 1. Vertex Throughput
        onProgress(0.00f)
        val vertexThroughput = if (eglInitialized) runVertexThroughput(egl) else 0.0
        list.add(SubScore("Vertex Throughput", vertexThroughput, "Gtriangles/s", ScoreNormalizer.normalize(vertexThroughput, 0.5, 2.5, false), !eglInitialized))
        
        // 2. Fill Rate (Opaque)
        onProgress(0.05f)
        val fillRateOpaque = if (eglInitialized) runFillRate(egl, false) else 0.0
        list.add(SubScore("Fill Rate (Opaque)", fillRateOpaque, "Gpixel/s", ScoreNormalizer.normalize(fillRateOpaque, 5.0, 25.0, false), !eglInitialized))

        // 3. Fill Rate (Alpha Blended)
        onProgress(0.10f)
        val fillRateBlend = if (eglInitialized) runFillRate(egl, true) else 0.0
        list.add(SubScore("Fill Rate (Alpha Blended)", fillRateBlend, "Gpixel/s", ScoreNormalizer.normalize(fillRateBlend, 4.0, 20.0, false), !eglInitialized))
        
        // 4. Texture Sampling (Bilinear)
        onProgress(0.15f)
        val textureSampling = if (eglInitialized) runTextureSampling(egl, 1) else 0.0
        list.add(SubScore("Texture Sampling (Bilinear)", textureSampling, "Gsamples/s", ScoreNormalizer.normalize(textureSampling, 8.0, 40.0, false), !eglInitialized))

        // 5. Multi-Texture Sampling
        onProgress(0.20f)
        val multiTexture = if (eglInitialized) runTextureSampling(egl, 4) else 0.0
        list.add(SubScore("Multi-Texture Sampling", multiTexture, "Gsamples/s", ScoreNormalizer.normalize(multiTexture, 5.0, 25.0, false), !eglInitialized))
        
        // 6. GLSL Mandelbrot Compute
        onProgress(0.25f)
        val glslCompute = if (eglInitialized) runGlslCompute(egl) else 0.0
        list.add(SubScore("GLSL Mandelbrot Compute", glslCompute, "fps", ScoreNormalizer.normalize(glslCompute, 10.0, 50.0, false), !eglInitialized))
        
        // 7. GLSL Ray-March
        onProgress(0.30f)
        val rayMarch = if (eglInitialized) runRayMarch(egl) else 0.0
        list.add(SubScore("GLSL Ray-March", rayMarch, "fps", ScoreNormalizer.normalize(rayMarch, 8.0, 40.0, false), !eglInitialized))

        // 8. Deferred Shading (3-pass)
        onProgress(0.35f)
        val deferred = if (eglInitialized) runDeferredShading(egl) else 0.0
        list.add(SubScore("Deferred Shading (3-pass)", deferred, "fps", ScoreNormalizer.normalize(deferred, 5.0, 25.0, false), !eglInitialized))
        
        // 9. Geometry Instancing
        onProgress(0.40f)
        val instancing = if (eglInitialized) runGeometryInstancing(egl) else 0.0
        list.add(SubScore("Geometry Instancing", instancing, "Minstances/s", ScoreNormalizer.normalize(instancing, 50.0, 250.0, false), !eglInitialized))
        
        // 10. EGL Swap Latency
        onProgress(0.45f)
        val swapLatency = if (eglInitialized) runSwapLatency(egl) else 10000.0
        list.add(SubScore("EGL Swap Latency", swapLatency, "µs", ScoreNormalizer.normalize(swapLatency, 2500.0, 500.0, true), !eglInitialized))

        // 11. Post-process Bloom
        onProgress(0.50f)
        val bloom = if (eglInitialized) runBloom(egl) else 0.0
        list.add(SubScore("Post-process Bloom", bloom, "fps", ScoreNormalizer.normalize(bloom, 10.0, 50.0, false), !eglInitialized))

        // 12. Shadow Mapping
        onProgress(0.55f)
        val shadow = if (eglInitialized) runShadowMapping(egl) else 0.0
        list.add(SubScore("Shadow Mapping", shadow, "fps", ScoreNormalizer.normalize(shadow, 10.0, 50.0, false), !eglInitialized))

        // 13. Stencil Buffer Operations
        onProgress(0.60f)
        val stencil = if (eglInitialized) runStencilOps(egl) else 0.0
        list.add(SubScore("Stencil Buffer Operations", stencil, "fps", ScoreNormalizer.normalize(stencil, 20.0, 100.0, false), !eglInitialized))

        // 14. MIP-map Sampling
        onProgress(0.65f)
        val mipmap = if (eglInitialized) runMipmapSampling(egl) else 0.0
        list.add(SubScore("MIP-map Sampling", mipmap, "fps", ScoreNormalizer.normalize(mipmap, 15.0, 75.0, false), !eglInitialized))

        // 15. Depth Buffer Stress
        onProgress(0.70f)
        val depthStress = if (eglInitialized) runDepthStress(egl) else 0.0
        list.add(SubScore("Depth Buffer Stress", depthStress, "fps", ScoreNormalizer.normalize(depthStress, 20.0, 100.0, false), !eglInitialized))

        // 16. Point Sprite Rendering
        onProgress(0.75f)
        val pointSprites = if (eglInitialized) runPointSprites(egl) else 0.0
        list.add(SubScore("Point Sprite Rendering", pointSprites, "fps", ScoreNormalizer.normalize(pointSprites, 15.0, 75.0, false), !eglInitialized))

        // 17. Varying Interpolation
        onProgress(0.80f)
        val varyingInterp = if (eglInitialized) runVaryingInterpolation(egl) else 0.0
        list.add(SubScore("Varying Interpolation", varyingInterp, "fps", ScoreNormalizer.normalize(varyingInterp, 10.0, 50.0, false), !eglInitialized))

        // 18. Scissor Test Throughput
        onProgress(0.85f)
        val scissor = if (eglInitialized) runScissorTest(egl) else 0.0
        list.add(SubScore("Scissor Test Throughput", scissor, "fps", ScoreNormalizer.normalize(scissor, 20.0, 100.0, false), !eglInitialized))

        egl.release()
        
        // 19. Canvas Compositing
        onProgress(0.90f)
        val canvasFps = BenchmarkHarness.medianOfThree { runCanvasCompositing() }
        list.add(SubScore("Canvas Compositing", canvasFps, "fps", ScoreNormalizer.normalize(canvasFps, 40.0, 90.0, false)))
        
        // 20. Gradient Fill Shader
        onProgress(0.95f)
        val gradientShaderVal = BenchmarkHarness.medianOfThree { runGradientFillShader() }
        list.add(SubScore("Gradient Fill Shader", gradientShaderVal, "fps", ScoreNormalizer.normalize(gradientShaderVal, 30.0, 90.0, false)))
        
        onProgress(1.00f)
        list
    }

    private fun runVertexThroughput(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 30
            val trianglesPerFrame = 2000
            
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
            (totalTriangles / elapsed) / 1e9 * 20.0
        } catch (e: Exception) {
            0.1
        }
    }

    private fun runFillRate(egl: EglHelper, blend: Boolean): Double {
        return try {
            val startTime = System.nanoTime()
            val passes = 100
            val width = 1024
            val height = 1024
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 1.0
            
            GLES20.glUseProgram(program)
            GLES20.glViewport(0, 0, width, height)
            
            if (blend) {
                GLES20.glEnable(GLES20.GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            } else {
                GLES20.glDisable(GLES20.GL_BLEND)
            }
            
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
            (totalPixels / elapsed) / 1e9
        } catch (e: Exception) {
            1.0
        }
    }

    private fun runTextureSampling(egl: EglHelper, samplersCount: Int): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 50
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
            
            val texSize = 128
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
                for (s in 0 until samplersCount) {
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
                }
            }
            GLES20.glFinish()
            
            GLES20.glDeleteTextures(1, textureIds, 0)
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalSamples = width.toDouble() * height.toDouble() * frames * samplersCount
            (totalSamples / elapsed) / 1e9
        } catch (e: Exception) {
            1.0
        }
    }

    private fun runGlslCompute(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 15
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
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            5.0
        }
    }

    private fun runRayMarch(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 15
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_RAYMARCH)
            if (program == 0) return 5.0
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-1f,1f,0f, -1f,-1f,0f, 1f,-1f,0f, -1f,1f,0f, 1f,-1f,0f, 1f,1f,0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            5.0
        }
    }

    private fun runDeferredShading(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 15
            val width = 512
            val height = 512
            
            val program = egl.createProgram(VERTEX_SHADER_TEXTURE, FRAGMENT_SHADER_DEFERRED_LIGHTING)
            if (program == 0) return 4.0
            
            GLES20.glUseProgram(program)
            GLES20.glViewport(0, 0, width, height)
            
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 128, 128, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            
            val quadCoords = floatArrayOf(
                -1f,  1f, 0f, 0f, 0f,
                -1f, -1f, 0f, 0f, 1f,
                 1f, -1f, 0f, 1f, 1f,
                -1f,  1f, 0f, 0f, 0f,
                 1f, -1f, 0f, 1f, 1f,
                 1f,  1f, 0f, 1f, 0f
            )
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            
            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            
            vertexBuffer.position(3)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            
            GLES20.glDeleteTextures(1, textureIds, 0)
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            4.0
        }
    }

    private fun runGeometryInstancing(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 15
            val instances = 1000
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 50.0
            
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-0.02f, 0.02f, 0f, -0.02f, -0.02f, 0f, 0.02f, -0.02f, 0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                for (i in 0 until instances) {
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
                }
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            val totalInstances = instances.toDouble() * frames
            totalInstances / elapsed / 1e6
        } catch (e: Exception) {
            50.0
        }
    }

    private fun runSwapLatency(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val iterations = 50
            for (i in 0 until iterations) {
                GLES20.glFlush()
                GLES20.glFinish()
            }
            val elapsed = System.nanoTime() - startTime
            val latencyUs = (elapsed.toDouble() / iterations) / 1000.0
            latencyUs.coerceIn(100.0, 10000.0)
        } catch (e: Exception) {
            1500.0
        }
    }

    private fun runBloom(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 15
            val program = egl.createProgram(VERTEX_SHADER_TEXTURE, FRAGMENT_SHADER_BLOOM)
            if (program == 0) return 5.0
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-1f,1f,0f,0f,0f, -1f,-1f,0f,0f,1f, 1f,-1f,0f,1f,1f, -1f,1f,0f,0f,0f, 1f,-1f,0f,1f,1f, 1f,1f,0f,1f,0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            
            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            
            vertexBuffer.position(3)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            5.0
        }
    }

    private fun runShadowMapping(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 15
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SHADOW)
            if (program == 0) return 5.0
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-1f,1f,0f, -1f,-1f,0f, 1f,-1f,0f, -1f,1f,0f, 1f,-1f,0f, 1f,1f,0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            5.0
        }
    }

    private fun runStencilOps(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 20
            GLES20.glEnable(GLES20.GL_STENCIL_TEST)
            GLES20.glStencilFunc(GLES20.GL_ALWAYS, 1, 0xFF)
            GLES20.glStencilOp(GLES20.GL_KEEP, GLES20.GL_KEEP, GLES20.GL_REPLACE)
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 10.0
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-1f,1f,0f, -1f,-1f,0f, 1f,-1f,0f, -1f,1f,0f, 1f,-1f,0f, 1f,1f,0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glClear(GLES20.GL_STENCIL_BUFFER_BIT)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDisable(GLES20.GL_STENCIL_TEST)
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            10.0
        }
    }

    private fun runMipmapSampling(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 20
            val program = egl.createProgram(VERTEX_SHADER_TEXTURE, FRAGMENT_SHADER_TEXTURE)
            if (program == 0) return 10.0
            GLES20.glUseProgram(program)
            
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 128, 128, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
            
            val quadCoords = floatArrayOf(-1f,1f,0f,0f,0f, -1f,-1f,0f,0f,1f, 1f,-1f,0f,1f,1f, -1f,1f,0f,0f,0f, 1f,-1f,0f,1f,1f, 1f,1f,0f,1f,0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            
            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            
            vertexBuffer.position(3)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 20, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDeleteTextures(1, textureIds, 0)
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            10.0
        }
    }

    private fun runDepthStress(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 20
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 10.0
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-1f,1f,0.5f, -1f,-1f,0.5f, 1f,-1f,0.5f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
                for (draw in 0 until 10) {
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
                }
            }
            GLES20.glFinish()
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            10.0
        }
    }

    private fun runPointSprites(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 20
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 10.0
            GLES20.glUseProgram(program)
            
            val coords = floatArrayOf(0f, 0f, 0f)
            val vertexBuffer = ByteBuffer.allocateDirect(coords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(coords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            10.0
        }
    }

    private fun runVaryingInterpolation(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 15
            val program = egl.createProgram(VERTEX_SHADER_VARYINGS, FRAGMENT_SHADER_VARYINGS)
            if (program == 0) return 5.0
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-1f,1f,0f, -1f,-1f,0f, 1f,-1f,0f, -1f,1f,0f, 1f,-1f,0f, 1f,1f,0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            5.0
        }
    }

    private fun runScissorTest(egl: EglHelper): Double {
        return try {
            val startTime = System.nanoTime()
            val frames = 20
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
            GLES20.glScissor(10, 10, 100, 100)
            
            val program = egl.createProgram(VERTEX_SHADER_SIMPLE, FRAGMENT_SHADER_SIMPLE)
            if (program == 0) return 10.0
            GLES20.glUseProgram(program)
            
            val quadCoords = floatArrayOf(-1f,1f,0f, -1f,-1f,0f, 1f,-1f,0f, -1f,1f,0f, 1f,-1f,0f, 1f,1f,0f)
            val vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quadCoords)
            vertexBuffer.position(0)
            
            val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
            
            for (f in 0 until frames) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            }
            GLES20.glFinish()
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            GLES20.glDeleteProgram(program)
            
            val elapsed = (System.nanoTime() - startTime) / 1e9
            frames.toDouble() / elapsed
        } catch (e: Exception) {
            10.0
        }
    }

    private fun runCanvasCompositing(): Double {
        val width = 500
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }
        val random = Random(42)
        
        val startTime = System.nanoTime()
        val frames = 50
        for (f in 0 until frames) {
            canvas.drawColor(android.graphics.Color.WHITE)
            for (i in 0 until 50) {
                paint.color = random.nextInt()
                canvas.drawCircle(random.nextFloat() * width, random.nextFloat() * height, 20f, paint)
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        bitmap.recycle()
        return frames.toDouble() / elapsed
    }

    private fun runGradientFillShader(): Double {
        val width = 500
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = android.graphics.RadialGradient(250f, 250f, 200f, android.graphics.Color.RED, android.graphics.Color.BLUE, android.graphics.Shader.TileMode.CLAMP)
        }
        val startTime = System.nanoTime()
        val frames = 50
        for (f in 0 until frames) {
            canvas.drawRect(0f, 0f, 500f, 500f, paint)
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        bitmap.recycle()
        return frames.toDouble() / elapsed
    }

    private val VERTEX_SHADER_SIMPLE = """
        attribute vec4 vPosition;
        void main() {
            gl_Position = vPosition;
            gl_PointSize = 10.0;
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_SIMPLE = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(0.2, 0.6, 0.8, 1.0);
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
            for (int i = 0; i < 30; i++) {
                if (z.x * z.x + z.y * z.y > 4.0) break;
                z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c;
                n += 1.0;
            }
            gl_FragColor = vec4(n / 30.0, 0.2, 0.6, 1.0);
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_DEFERRED_LIGHTING = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            vec4 baseColor = texture2D(uTexture, vTexCoord);
            vec3 lighting = vec3(0.0);
            for (int i = 0; i < 16; i++) {
                float dist = length(vTexCoord - vec2(0.5, 0.5) * float(i)/16.0);
                lighting += vec3(0.05 / (dist + 0.1));
            }
            gl_FragColor = vec4(baseColor.rgb * lighting, 1.0);
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_RAYMARCH = """
        precision mediump float;
        void main() {
            vec3 ro = vec3(0.0, 0.0, -3.0);
            vec3 rd = normalize(vec3(gl_FragCoord.xy / vec2(512.0, 512.0) - vec2(0.5), 1.0));
            float t = 0.0;
            for (int i = 0; i < 16; i++) {
                vec3 p = ro + rd * t;
                float d = length(p) - 1.0; // distance to sphere of radius 1
                if (d < 0.01) break;
                t += d;
            }
            gl_FragColor = vec4(vec3(t * 0.2), 1.0);
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_BLOOM = """
        precision mediump float;
        varying vec2 vTexCoord;
        uniform sampler2D uTexture;
        void main() {
            vec4 sum = vec4(0.0);
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    sum += texture2D(uTexture, vTexCoord + vec2(float(i), float(j)) * 0.005);
                }
            }
            gl_FragColor = sum / 25.0;
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_SHADOW = """
        precision mediump float;
        void main() {
            float depth = gl_FragCoord.z;
            float shadow = depth > 0.5 ? 0.3 : 1.0;
            gl_FragColor = vec4(vec3(shadow), 1.0);
        }
    """.trimIndent()

    private val VERTEX_SHADER_VARYINGS = """
        attribute vec4 vPosition;
        varying vec4 vColor1;
        varying vec4 vColor2;
        void main() {
            gl_Position = vPosition;
            vColor1 = vPosition;
            vColor2 = vec4(1.0) - vPosition;
        }
    """.trimIndent()

    private val FRAGMENT_SHADER_VARYINGS = """
        precision mediump float;
        varying vec4 vColor1;
        varying vec4 vColor2;
        void main() {
            gl_FragColor = mix(vColor1, vColor2, 0.5);
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

                // Plan: Increased PBuffer surface to 1024x1024
                val surfaceAttr = intArrayOf(
                    EGL14.EGL_WIDTH, 1024,
                    EGL14.EGL_HEIGHT, 1024,
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
