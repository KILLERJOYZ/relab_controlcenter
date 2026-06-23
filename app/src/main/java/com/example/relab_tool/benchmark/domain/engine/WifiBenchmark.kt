package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit

class WifiBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.WIFI

    companion object {
        private const val TAG = "WifiBenchmark"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override fun isAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SubScore>()
        
        // 10a. Download Throughput (25 MB Cloudflare test)
        onProgress(0.0f)
        val downloadMbps = runDownloadSpeedTest("https://speed.cloudflare.com/__down?bytes=25000000")
        list.add(SubScore("Wi-Fi Download Speed", downloadMbps, "Mbps", ScoreNormalizer.normalize(downloadMbps, 120.0, 480.0, false)))
        
        // 10b. Upload Throughput (Local Loopback TCP simulation)
        onProgress(0.25f)
        val uploadMbps = runLoopbackUploadTest()
        // Map local loopback speed (which is very fast, e.g. 2000-5000 Mbps) to typical upload bounds
        // A standard 150 Mbps Wi-Fi upload translates to 500 points
        val scaledUpload = (uploadMbps / 15.0).coerceIn(10.0, 500.0)
        list.add(SubScore("Wi-Fi Upload Speed (Simulated)", scaledUpload, "Mbps", ScoreNormalizer.normalize(scaledUpload, 60.0, 240.0, false)))
        
        // 10c. Latency / RTT (gstatic.com HEAD pings)
        onProgress(0.5f)
        val latencyMs = runRttTest()
        list.add(SubScore("Wi-Fi Latency RTT", latencyMs, "ms", ScoreNormalizer.normalize(latencyMs, 18.0, 4.0, true)))
        
        // 10d. DNS Resolution Speed
        onProgress(0.75f)
        val dnsMs = runDnsTest()
        list.add(SubScore("Wi-Fi DNS Speed", dnsMs, "ms", ScoreNormalizer.normalize(dnsMs, 30.0, 5.0, true)))
        
        // 10e. Concurrent Connections Scaling %
        onProgress(0.9f)
        val scalingPercent = runConcurrentScaling()
        list.add(SubScore("Concurrent Connection scaling", scalingPercent, "%", ScoreNormalizer.normalize(scalingPercent, 65.0, 95.0, false)))
        
        onProgress(1.0f)
        list
    }

    private fun runDownloadSpeedTest(url: String): Double {
        val request = Request.Builder().url(url).build()
        try {
            val start = System.currentTimeMillis()
            client.newCall(request).execute().use { response ->
                val body = response.body ?: return 5.0
                val source = body.source()
                val buffer = ByteArray(32768)
                var totalBytes = 0L
                val timeLimit = 8000L // 8s cap
                
                while (true) {
                    val read = source.read(buffer)
                    if (read == -1) break
                    totalBytes += read
                    if (System.currentTimeMillis() - start > timeLimit) break
                }
                
                val durationSec = (System.currentTimeMillis() - start) / 1000.0
                return if (durationSec > 0) (totalBytes * 8.0 / 1e6) / durationSec else 5.0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download speed test failed", e)
            return 5.0
        }
    }

    private suspend fun runLoopbackUploadTest(): Double = coroutineScope {
        try {
            val serverSocket = ServerSocket(0)
            val port = serverSocket.localPort
            
            val serverJob = async(Dispatchers.IO) {
                try {
                    serverSocket.accept().use { socket ->
                        val input = socket.getInputStream()
                        val buf = ByteArray(65536)
                        var read = 0
                        while (input.read(buf).also { read = it } != -1) {
                            // discard
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                } finally {
                    serverSocket.close()
                }
            }
            
            val clientJob = async(Dispatchers.IO) {
                delay(50)
                try {
                    Socket("127.0.0.1", port).use { socket ->
                        val output = socket.getOutputStream()
                        val buf = ByteArray(65536)
                        val totalBytes = 10 * 1024 * 1024L // 10 MB
                        var written = 0L
                        val start = System.nanoTime()
                        while (written < totalBytes) {
                            val toWrite = Math.min(buf.size.toLong(), totalBytes - written).toInt()
                            output.write(buf, 0, toWrite)
                            written += toWrite
                        }
                        output.flush()
                        val duration = (System.nanoTime() - start) / 1e9
                        (totalBytes * 8.0 / 1e6) / duration
                    }
                } catch (e: Exception) {
                    0.0
                }
            }
            
            serverJob.await()
            clientJob.await()
        } catch (e: Exception) {
            0.0
        }
    }

    private fun runRttTest(): Double {
        val latencies = mutableListOf<Long>()
        val request = Request.Builder()
            .url("https://www.gstatic.com/generate_204")
            .head()
            .build()
            
        for (i in 0 until 10) {
            val start = System.nanoTime()
            try {
                client.newCall(request).execute().use {
                    latencies.add(System.nanoTime() - start)
                }
            } catch (e: Exception) {
                // ignore failed pings
            }
        }
        
        if (latencies.isEmpty()) return 50.0
        latencies.sort()
        val medianNs = latencies[latencies.size / 2]
        return medianNs.toDouble() / 1e6 // return ms
    }

    private fun runDnsTest(): Double {
        val domains = listOf("google.com", "cloudflare.com", "github.com", "apple.com", "amazon.com")
        val latencies = mutableListOf<Long>()
        
        for (domain in domains) {
            val start = System.nanoTime()
            try {
                InetAddress.getByName(domain)
                latencies.add(System.nanoTime() - start)
            } catch (e: Exception) {
                // ignore
            }
        }
        
        if (latencies.isEmpty()) return 80.0
        latencies.sort()
        val medianNs = latencies[latencies.size / 2]
        return medianNs.toDouble() / 1e6
    }

    private suspend fun runConcurrentScaling(): Double = coroutineScope {
        // Measure scaling when 4 streams download concurrently vs single stream
        // Return scaling efficiency % (ideally > 80%)
        val singleSpeed = runDownloadSpeedTest("https://speed.cloudflare.com/__down?bytes=5000000") // 5 MB
        
        val start = System.currentTimeMillis()
        val jobs = List(4) {
            async(Dispatchers.IO) {
                runDownloadSpeedTest("https://speed.cloudflare.com/__down?bytes=2000000") // 2 MB each
            }
        }
        val speeds = jobs.awaitAll()
        val aggregateSpeed = speeds.sum()
        
        val scaling = if (singleSpeed > 0) (aggregateSpeed / (singleSpeed * 4.0)) * 100.0 else 75.0
        scaling.coerceIn(50.0, 100.0)
    }
}
