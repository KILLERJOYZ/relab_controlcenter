package com.example.relab_tool.benchmark.util

import android.opengl.*
import android.util.Log

class EglComputeHelper {
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    companion object {
        private const val TAG = "EglComputeHelper"
    }

    /**
     * Initializes EGL with a mandatory 16x16 PBuffer surface.
     * Resolves the ARM Mali/Immortalis driver issue where surfaceless contexts fail.
     */
    fun initEGL(): Boolean {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "Unable to obtain EGL14 Display")
            return false
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            Log.e(TAG, "Failed to initialize EGL14")
            return false
        }

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)) {
            Log.e(TAG, "eglChooseConfig failed")
            return false
        }

        val eglConfig = configs[0]
        if (eglConfig == null) {
            Log.e(TAG, "No compatible EGL Configuration found")
            return false
        }

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, // Request OpenGL ES 3.1+ Compute capabilities
            EGL14.EGL_NONE
        )

        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "Failed to create EGL Context")
            return false
        }

        // Bug Fix: Allocate a tiny 16x16 PBuffer surface.
        // Headless context binding (EGL_NO_SURFACE) causes eglMakeCurrent to fail on ARM Mali drivers.
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 16,
            EGL14.EGL_HEIGHT, 16,
            EGL14.EGL_NONE
        )

        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "Failed to create PBuffer Surface")
            release()
            return false
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e(TAG, "eglMakeCurrent failed to bind the context to the active surface")
            release()
            return false
        }

        return true
    }

    /**
     * Compile and link compute shader program helper.
     */
    fun createComputeProgram(src: String): Int {
        return try {
            val shader = GLES31.glCreateShader(GLES31.GL_COMPUTE_SHADER)
            if (shader == 0) return 0
            GLES31.glShaderSource(shader, src)
            GLES31.glCompileShader(shader)
            val ok = IntArray(1)
            GLES31.glGetShaderiv(shader, GLES31.GL_COMPILE_STATUS, ok, 0)
            if (ok[0] != GLES31.GL_TRUE) {
                Log.e(TAG, "CS compile fail: ${GLES31.glGetShaderInfoLog(shader)}")
                GLES31.glDeleteShader(shader)
                return 0
            }
            val prog = GLES31.glCreateProgram()
            GLES31.glAttachShader(prog, shader)
            GLES31.glLinkProgram(prog)
            GLES31.glGetProgramiv(prog, GLES31.GL_LINK_STATUS, ok, 0)
            if (ok[0] != GLES31.GL_TRUE) {
                Log.e(TAG, "CS link fail: ${GLES31.glGetProgramInfoLog(prog)}")
                GLES31.glDeleteProgram(prog)
                return 0
            }
            prog
        } catch (e: Exception) {
            0
        }
    }

    fun release() {
        try {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    eglDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
                )
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                    eglSurface = EGL14.EGL_NO_SURFACE
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                    eglContext = EGL14.EGL_NO_CONTEXT
                }
                EGL14.eglTerminate(eglDisplay)
                eglDisplay = EGL14.EGL_NO_DISPLAY
            }
        } catch (_: Exception) {}
    }
}
