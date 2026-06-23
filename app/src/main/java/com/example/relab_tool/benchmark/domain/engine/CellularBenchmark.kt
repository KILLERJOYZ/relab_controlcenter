package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.CellSignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class CellularBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.CELLULAR

    companion object {
        private const val TAG = "CellularBenchmark"
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
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SubScore>()
        
        // 11a. Cellular Download
        onProgress(0.0f)
        val downloadMbps = runDownloadSpeedTest("https://speed.cloudflare.com/__down?bytes=25000000")
        list.add(SubScore("Cellular Download Speed", downloadMbps, "Mbps", ScoreNormalizer.normalize(downloadMbps, 80.0, 320.0, false)))
        
        // 11b. Cellular Latency
        onProgress(0.33f)
        val latencyMs = runRttTest()
        list.add(SubScore("Cellular Latency RTT", latencyMs, "ms", ScoreNormalizer.normalize(latencyMs, 28.0, 6.0, true)))
        
        // 11c. DNS over Cellular
        onProgress(0.66f)
        val dnsMs = runDnsTest()
        list.add(SubScore("Cellular DNS Speed", dnsMs, "ms", ScoreNormalizer.normalize(dnsMs, 40.0, 8.0, true)))
        
        // 11d. Signal Quality Score (0-100)
        onProgress(0.9f)
        val signalQuality = getSignalQualityScore()
        list.add(SubScore("Cellular Signal Quality", signalQuality.toDouble(), "quality", ScoreNormalizer.normalize(signalQuality.toDouble(), 60.0, 100.0, false)))
        
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
                val timeLimit = 8000L
                
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
            Log.e(TAG, "Cellular download speed test failed", e)
            return 5.0
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
                // ignore
            }
        }
        
        if (latencies.isEmpty()) return 80.0
        latencies.sort()
        val medianNs = latencies[latencies.size / 2]
        return medianNs.toDouble() / 1e6
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
        
        if (latencies.isEmpty()) return 100.0
        latencies.sort()
        val medianNs = latencies[latencies.size / 2]
        return medianNs.toDouble() / 1e6
    }

    private fun getSignalQualityScore(): Int {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val signalStrength = tm.signalStrength
                if (signalStrength != null) {
                    val level = signalStrength.level // 0..4 range
                    return (level * 25).coerceIn(0, 100)
                }
            }
            80 // default fallback
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException reading cell signal strength", e)
            75 // permission not granted fallback
        } catch (e: Exception) {
            70
        }
    }
}
