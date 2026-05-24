package com.example.relab_tool.data

import com.example.relab_tool.utils.AppConfig
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ApkCrawler {
    private val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun getLatestVersionInfo(packageName: String): Pair<String?, String?> {
        val url = String.format(AppConfig.APKPURE_URL_QUERY_TEMPLATE, AppConfig.APKPURE_BASE_URL, packageName)
        val request = Request.Builder()
            .url("$url&ts=${System.currentTimeMillis()}")
            .addHeader(AppConfig.HEADER_CACHE_CONTROL, AppConfig.VALUE_NO_CACHE)
            .addHeader(AppConfig.HEADER_USER_AGENT, AppConfig.DEFAULT_USER_AGENT)
            .build()

        return try {
            val response = executeRequestWithRetry(request)
            if (response.isSuccessful) {
                // In a real app, we'd parse the HTML or use an API. 
                // For this simulation, we'll try to extract from headers or a mock version.
                val latestVersion = response.header(AppConfig.HEADER_APK_VERSION) ?: AppConfig.MOCK_LATEST_VERSION 
                val downloadUrl = response.request.url.toString()
                Pair(latestVersion, downloadUrl)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    private suspend fun executeRequestWithRetry(request: Request, retries: Int = AppConfig.HTTP_MAX_RETRIES): Response {
        var lastException: IOException? = null
        for (i in 0 until retries) {
            try {
                val response = suspendCoroutine<Response> { continuation ->
                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            continuation.resumeWith(Result.failure(e))
                        }
                        override fun onResponse(call: Call, response: Response) {
                            continuation.resume(response)
                        }
                    })
                }
                if (response.isSuccessful) return response
                response.close()
            } catch (e: IOException) {
                lastException = e
                kotlinx.coroutines.delay(AppConfig.HTTP_RETRY_DELAY_MS * (i + 1))
            }
        }
        throw lastException ?: IOException("Request failed after $retries attempts")
    }
}
