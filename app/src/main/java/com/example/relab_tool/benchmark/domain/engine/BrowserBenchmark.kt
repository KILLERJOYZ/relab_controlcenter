package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import android.util.Log
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool as OkConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class BrowserBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.BROWSER_WEB

    companion object {
        private const val TAG = "BrowserBenchmark"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SubScore>()
        
        // 12a. HTML Parse Speed
        onProgress(0.0f)
        val parseSpeedKb = runHtmlParseTest()
        list.add(SubScore("HTML Parse Speed", parseSpeedKb, "KB/s", ScoreNormalizer.normalize(parseSpeedKb, 8000.0, 32000.0, false)))
        
        // 12b. Asset Parallel Fetch
        onProgress(0.2f)
        val fetchDurationMs = runAssetParallelFetch()
        val normalizedFetch = if (fetchDurationMs > 0) 200000.0 / fetchDurationMs else 1200.0
        list.add(SubScore("Asset Parallel Fetch", fetchDurationMs, "ms", ScoreNormalizer.normalize(fetchDurationMs, 1200.0, normalizedFetch, true)))
        
        // 12c. Page Load Simulation
        onProgress(0.4f)
        val loadTimeMs = runPageLoadSimulation()
        list.add(SubScore("Page Load Duration", loadTimeMs, "ms", ScoreNormalizer.normalize(loadTimeMs, 800.0, 80.0, true)))
        
        // 12d. SSL/TLS Handshake
        onProgress(0.6f)
        val tlsHandshakeMs = runTlsHandshakeTest()
        list.add(SubScore("SSL/TLS Handshake Latency", tlsHandshakeMs, "ms", ScoreNormalizer.normalize(tlsHandshakeMs, 80.0, 12.0, true)))
        
        // 12e. JavaScript Proxy (CPU)
        onProgress(0.8f)
        val jsProxyScore = runJsProxyTest()
        list.add(SubScore("JS Engine Simulation", jsProxyScore, "ops/ms", ScoreNormalizer.normalize(jsProxyScore, 120.0, 480.0, false)))
        
        onProgress(1.0f)
        list
    }

    private fun runHtmlParseTest(): Double {
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<!DOCTYPE html><html><head><title>Mock HTML</title></head><body>")
        for (i in 0 until 1000) {
            htmlBuilder.append("<div id='item_$i' class='data-row'>")
            htmlBuilder.append("<span class='label'>Item Name $i</span>")
            htmlBuilder.append("<a href='https://example.com/item/$i'>Link $i</a>")
            htmlBuilder.append("<img src='https://example.com/img/$i.png' />")
            htmlBuilder.append("</div>")
        }
        htmlBuilder.append("</body></html>")
        val html = htmlBuilder.toString()
        val dataSizeKb = html.length.toDouble() / 1024.0
        
        val start = System.nanoTime()
        val imageRegex = Regex("<img\\s+src='([^']+)'\\s*/>")
        val linkRegex = Regex("<a\\s+href='([^']+)'\\s*>")
        
        var matches = 0
        for (pass in 0 until 50) {
            val images = imageRegex.findAll(html).count()
            val links = linkRegex.findAll(html).count()
            matches += images + links
        }
        val elapsed = (System.nanoTime() - start) / 1e9
        return (dataSizeKb * 50.0) / elapsed
    }

    private suspend fun runAssetParallelFetch(): Double = coroutineScope {
        val request = Request.Builder().url("http://example.com").build()
        val start = System.currentTimeMillis()
        val jobs = List(5) {
            async(Dispatchers.IO) {
                try {
                    client.newCall(request).execute().use { response ->
                        response.body?.string()?.length ?: 0
                    }
                } catch (e: Exception) {
                    0
                }
            }
        }
        jobs.awaitAll()
        val elapsed = System.currentTimeMillis() - start
        elapsed.toDouble()
    }

    private fun runPageLoadSimulation(): Double {
        val request = Request.Builder().url("http://example.com").build()
        val start = System.currentTimeMillis()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body
                if (body != null) {
                    body.string().length
                }
            }
        } catch (e: Exception) {
            return 800.0
        }
        val elapsed = System.currentTimeMillis() - start
        return elapsed.toDouble()
    }

    private fun runTlsHandshakeTest(): Double {
        val noPoolClient = OkHttpClient.Builder()
            .connectionPool(OkConnectionPool(0, 1, TimeUnit.MILLISECONDS))
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url("https://www.google.com").head().build()
        val latencies = mutableListOf<Long>()
        
        for (i in 0 until 3) {
            val start = System.nanoTime()
            try {
                noPoolClient.newCall(request).execute().use {
                    latencies.add(System.nanoTime() - start)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        
        if (latencies.isEmpty()) return 80.0
        latencies.sort()
        val medianMs = (latencies[latencies.size / 2]).toDouble() / 1e6
        return (medianMs / 2.0).coerceIn(5.0, 150.0)
    }

    private fun runJsProxyTest(): Double {
        val startTime = System.nanoTime()
        var sum = 0L
        for (i in 0 until 50_000) {
            val val1 = fibonacci(20)
            val str = "V8EngineJsProxy_${val1}_$i"
            val replaced = str.replace("Proxy", "Adapter")
            sum += replaced.length + val1
        }
        val elapsedMs = (System.nanoTime() - startTime) / 1e6
        return 50000.0 / elapsedMs
    }

    private fun fibonacci(n: Int): Long {
        if (n <= 1) return n.toLong()
        var a = 0L
        var b = 1L
        for (i in 2..n) {
            val next = a + b
            a = b
            b = next
        }
        return b
    }
}
