package com.example.relab_tool.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.*
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkUtils {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun resolveRedirect(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            .addHeader("Cache-Control", "no-cache")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext url

                // Check if response is an APK file (or other known binary) directly
                val contentType = response.header("Content-Type")
                if (contentType?.contains("application/vnd.android.package-archive") == true || 
                    contentType?.contains("application/octet-stream") == true || 
                    response.request.url.toString().endsWith(".apk", ignoreCase = true)) {
                    return@withContext response.request.url.toString()
                }

                // OkHttp followRedirects=true will return the final URL in response.request.url
                response.request.url.toString()
            }
        } catch (e: Exception) {
            android.util.Log.w("NetworkUtils", "Failed to resolve redirect for $url: ${e.message}")
            url // Fallback to original URL
        }
    }

    suspend fun performDownloadSpeedTest(url: String, onProgress: (Float) -> Unit): Double =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val call = client.newCall(Request.Builder().url(url).build())
                call.timeout().timeout(10, TimeUnit.SECONDS)
                val cancellationGuard = launch(start = CoroutineStart.UNDISPATCHED) {
                    try { awaitCancellation() } finally { call.cancel() }
                }
                try {
                    call.execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Speed test HTTP failure: " + response.code)
                        val body = response.body ?: throw IOException("Empty response body")
                        val totalBytes = body.contentLength()
                        val started = System.nanoTime()
                        var downloaded = 0L
                        var lastProgress = started
                        val buffer = ByteArray(8192)
                        val source = body.source()
                        while (true) {
                            ensureActive()
                            val read = source.read(buffer)
                            if (read == -1) break
                            downloaded += read
                            val now = System.nanoTime()
                            if (totalBytes > 0 && now - lastProgress >= 100_000_000L) {
                                lastProgress = now
                                withContext(Dispatchers.Main) {
                                    onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                                }
                            }
                        }
                        val seconds = (System.nanoTime() - started).coerceAtLeast(1L) / 1_000_000_000.0
                        downloaded * 8.0 / 1_000_000 / seconds
                    }
                } finally {
                    cancellationGuard.cancel()
                    call.cancel()
                }
            }
        }
}
