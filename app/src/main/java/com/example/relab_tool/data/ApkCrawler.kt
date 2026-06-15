package com.example.relab_tool.data

import android.content.Context
import com.example.relab_tool.utils.AppConfig
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ApkCrawler(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .cache(Cache(File(context.cacheDir, "http_cache"), 10L * 1024 * 1024))
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

    suspend fun getAppIconUrl(packageName: String): String? {
        // Special static overrides for known non-store apps or legacy package names
        if (packageName == "com.apkpure.aegon") {
            return "https://static.apkpure.com/www/static/imgs/logo_new@2x.png"
        }
        if (packageName == "com.gaijinent.warthunder") {
            return "https://wtmobile.com/img/favicon/apple-touch-icon.png"
        }

        // 1. Google Play Store Scraper
        val playUrl = "https://play.google.com/store/apps/details?id=$packageName"
        val playRequest = Request.Builder()
            .url(playUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36")
            .build()
        try {
            val response = executeRequestWithRetry(playRequest)
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val ogImageRegex = """<meta[^>]*property="og:image"[^>]*content="([^"]+)"""".toRegex()
                var match = ogImageRegex.find(html)
                if (match != null) {
                    return match.groupValues[1]
                }
                val ogImageRegex2 = """<meta[^>]*content="([^"]+)"[^>]*property="og:image"""".toRegex()
                match = ogImageRegex2.find(html)
                if (match != null) {
                    return match.groupValues[1]
                }
                val playLhRegex = """https://play-lh\.googleusercontent\.com/[a-zA-Z0-9-_=]+""".toRegex()
                match = playLhRegex.find(html)
                if (match != null) {
                    return match.value
                }
            }
        } catch (e: Exception) {
            // fall through to Aptoide API fallback
        }

        // 2. Aptoide API Fallback
        val aptoideUrl = "https://ws2.aptoide.com/api/7/app/getMeta?package_name=$packageName"
        val aptoideRequest = Request.Builder()
            .url(aptoideUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36")
            .build()
        try {
            val response = executeRequestWithRetry(aptoideRequest)
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                if (json.has("data")) {
                    val data = json.getJSONObject("data")
                    if (data.has("icon")) {
                        val iconUrl = data.getString("icon")
                        if (!iconUrl.isNullOrEmpty()) {
                            return iconUrl
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fall through to APKPure scraper fallback
        }

        // 3. APKPure search fallback (as a last resort)
        val pureUrl = "https://apkpure.com/search?q=$packageName"
        val pureRequest = Request.Builder()
            .url(pureUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36")
            .build()
        try {
            val response = executeRequestWithRetry(pureRequest)
            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val winudfRegex = """https://image\.winudf\.com/v2/image1/[a-zA-Z0-9-_\./]+""".toRegex()
                val match = winudfRegex.find(html)
                if (match != null) {
                    return match.value
                }
            }
        } catch (e: Exception) {
            // Ignore and return null
        }
        return null
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
