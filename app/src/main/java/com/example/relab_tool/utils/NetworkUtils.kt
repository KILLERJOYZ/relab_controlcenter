package com.example.relab_tool.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun performDownloadSpeedTest(url: String, onProgress: (Float) -> Unit): Double = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        var speedMbps = 0.0
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body ?: return@withContext 0.0
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                val startTime = System.currentTimeMillis()
                val source = body.source()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (source.read(buffer).also { bytesRead = it } != -1) {
                    downloadedBytes += bytesRead
                    if (totalBytes > 0) {
                        onProgress(downloadedBytes.toFloat() / totalBytes)
                    }
                    // Limit test to 10 seconds
                    if (System.currentTimeMillis() - startTime > 10000) break
                }
                
                val endTime = System.currentTimeMillis()
                val durationSec = (endTime - startTime) / 1000.0
                if (durationSec > 0) {
                    speedMbps = (downloadedBytes * 8.0 / 1024 / 1024) / durationSec
                }
            }
        } catch (e: Exception) {}
        speedMbps
    }
}
