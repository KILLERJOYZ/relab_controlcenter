package com.example.relab_tool.benchmark.domain.engine

import android.content.Context
import android.util.Log
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.*
import okhttp3.ConnectionPool as OkConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Random
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class BrowserBenchmark(private val context: Context) : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.BROWSER_WEB

    companion object {
        private const val TAG = "BrowserBenchmark"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SubScore>()
        
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
        val isOnline = caps != null && (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR))

        // 1. HTML Parse Speed
        onProgress(0.00f)
        val parseSpeed = runHtmlParseTest()
        list.add(SubScore("HTML Parse Speed", parseSpeed, "KB/s", ScoreNormalizer.normalize(parseSpeed, 8000.0, 32000.0, false)))
        
        // 2. Asset Parallel Fetch
        onProgress(0.05f)
        val fetchDurationMs = if (isOnline) runAssetParallelFetch() else 500.0
        val normalizedFetch = if (fetchDurationMs > 0) 1000.0 / fetchDurationMs * 100.0 else 50.0
        list.add(SubScore("Asset Parallel Fetch", fetchDurationMs, "ms", ScoreNormalizer.normalize(fetchDurationMs, 1000.0, 100.0, true), !isOnline))
        
        // 3. Page Load Simulation
        onProgress(0.10f)
        val loadTimeMs = if (isOnline) runPageLoadSimulation() else 400.0
        list.add(SubScore("Page Load Duration", loadTimeMs, "ms", ScoreNormalizer.normalize(loadTimeMs, 600.0, 60.0, true), !isOnline))
        
        // 4. SSL/TLS Handshake
        onProgress(0.15f)
        val tlsHandshakeMs = if (isOnline) runTlsHandshakeTest() else 120.0
        list.add(SubScore("SSL/TLS Handshake Latency", tlsHandshakeMs, "ms", ScoreNormalizer.normalize(tlsHandshakeMs, 100.0, 15.0, true), !isOnline))
        
        // 5. JS Engine Simulation
        onProgress(0.20f)
        val jsProxyScore = runJsProxyTest()
        list.add(SubScore("JS Engine Simulation", jsProxyScore, "ops/ms", ScoreNormalizer.normalize(jsProxyScore, 80.0, 400.0, false)))
        
        // 6. DOM Tree Build
        onProgress(0.25f)
        val domTreeVal = runDomTreeBuild()
        list.add(SubScore("DOM Tree Construction", domTreeVal, "nodes/s", ScoreNormalizer.normalize(domTreeVal, 100000.0, 500000.0, false)))

        // 7. CSS Selector Match
        onProgress(0.30f)
        val cssVal = runCssSelectorMatch()
        list.add(SubScore("CSS Selector Matching", cssVal, "matches/s", ScoreNormalizer.normalize(cssVal, 5000.0, 25000.0, false)))

        // 8. JSON Parse Speed
        onProgress(0.35f)
        val jsonParseVal = runJsonParseSpeed()
        list.add(SubScore("JSON Deserialization", jsonParseVal, "MB/s", ScoreNormalizer.normalize(jsonParseVal, 10.0, 50.0, false)))

        // 9. JSON Stringify Speed
        onProgress(0.40f)
        val jsonStringifyVal = runJsonStringifySpeed()
        list.add(SubScore("JSON Serialization", jsonStringifyVal, "MB/s", ScoreNormalizer.normalize(jsonStringifyVal, 8.0, 40.0, false)))

        // 10. XML Parse Speed
        onProgress(0.45f)
        val xmlParseVal = runXmlParseSpeed()
        list.add(SubScore("XML Parsing Speed", xmlParseVal, "KB/s", ScoreNormalizer.normalize(xmlParseVal, 1000.0, 5000.0, false)))

        // 11. URL Encode/Decode
        onProgress(0.50f)
        val urlProcessVal = runUrlEncodeDecode()
        list.add(SubScore("URL Parsing & Encoding", urlProcessVal, "k-ops/s", ScoreNormalizer.normalize(urlProcessVal, 2.0, 10.0, false)))

        // 12. Cookie Processing
        onProgress(0.55f)
        val cookieVal = runCookieProcessing()
        list.add(SubScore("Cookie Parser Throughput", cookieVal, "k-cookies/s", ScoreNormalizer.normalize(cookieVal, 10.0, 50.0, false)))

        // 13. HTTP Header Parse
        onProgress(0.60f)
        val headerVal = runHttpHeaderParse()
        list.add(SubScore("HTTP Header Processing", headerVal, "k-headers/s", ScoreNormalizer.normalize(headerVal, 20.0, 100.0, false)))

        // 14. Base64 Processing
        onProgress(0.65f)
        val base64Val = runBase64Processing()
        list.add(SubScore("Base64 Transcoding", base64Val, "MB/s", ScoreNormalizer.normalize(base64Val, 50.0, 250.0, false)))

        // 15. UTF-8 Encode/Decode
        onProgress(0.70f)
        val utf8Val = runUtf8Processing()
        list.add(SubScore("UTF-8 Encoding Speed", utf8Val, "MB/s", ScoreNormalizer.normalize(utf8Val, 80.0, 400.0, false)))

        // 16. Regex URL Extraction
        onProgress(0.75f)
        val regexUrlVal = runRegexUrlExtraction()
        list.add(SubScore("Regex URL Parsing", regexUrlVal, "urls/s", ScoreNormalizer.normalize(regexUrlVal, 500.0, 2500.0, false)))

        // 17. HTML Entity Decode
        onProgress(0.80f)
        val entityVal = runHtmlEntityDecode()
        list.add(SubScore("HTML Entity Decoding", entityVal, "k-chars/s", ScoreNormalizer.normalize(entityVal, 100.0, 500.0, false)))

        // 18. Markdown Parse
        onProgress(0.85f)
        val markdownVal = runMarkdownParse()
        list.add(SubScore("Markdown Renderer Speed", markdownVal, "k-chars/s", ScoreNormalizer.normalize(markdownVal, 50.0, 250.0, false)))

        // 19. Form Data Encoding
        onProgress(0.90f)
        val formVal = runFormDataEncoding()
        list.add(SubScore("Form Data Encoding", formVal, "k-fields/s", ScoreNormalizer.normalize(formVal, 10.0, 50.0, false)))

        // 20. Response Streaming
        onProgress(0.95f)
        val streamVal = runResponseStreaming()
        list.add(SubScore("Stream Parser Speed", streamVal, "MB/s", ScoreNormalizer.normalize(streamVal, 20.0, 100.0, false)))

        onProgress(1.00f)
        list
    }

    private fun runHtmlParseTest(): Double {
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<!DOCTYPE html><html><head><title>Mock HTML</title></head><body>")
        for (i in 0 until 500) {
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
        for (pass in 0 until 10) {
            val images = imageRegex.findAll(html).count()
            val links = linkRegex.findAll(html).count()
            matches += images + links
        }
        val elapsed = (System.nanoTime() - start) / 1e9
        return (dataSizeKb * 10.0) / elapsed
    }

    private suspend fun runAssetParallelFetch(): Double = coroutineScope {
        val request = Request.Builder().url("https://www.google.com").head().build()
        val start = System.currentTimeMillis()
        val jobs = List(3) {
            async(Dispatchers.IO) {
                try {
                    client.newCall(request).execute().use { response ->
                        response.code
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
        val request = Request.Builder().url("https://www.google.com").head().build()
        val start = System.currentTimeMillis()
        try {
            client.newCall(request).execute().close()
        } catch (e: Exception) {
            return 500.0
        }
        return (System.currentTimeMillis() - start).toDouble()
    }

    private fun runTlsHandshakeTest(): Double {
        val noPoolClient = OkHttpClient.Builder()
            .connectionPool(OkConnectionPool(0, 1, TimeUnit.MILLISECONDS))
            .connectTimeout(2, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url("https://www.google.com").head().build()
        val latencies = mutableListOf<Long>()
        
        for (i in 0 until 2) {
            val start = System.nanoTime()
            try {
                noPoolClient.newCall(request).execute().close()
                latencies.add(System.nanoTime() - start)
            } catch (e: Exception) {
            }
        }
        
        if (latencies.isEmpty()) return 100.0
        return latencies.average() / 1e6
    }

    private fun runJsProxyTest(): Double {
        val startTime = System.nanoTime()
        var sum = 0L
        for (i in 0 until 20_000) {
            val val1 = fibonacci(15)
            val str = "V8EngineJsProxy_${val1}_$i"
            val replaced = str.replace("Proxy", "Adapter")
            sum += replaced.length + val1
        }
        val elapsedMs = (System.nanoTime() - startTime) / 1e6
        return 20000.0 / elapsedMs
    }

    private fun fibonacci(n: Int): Long {
        if (n <= 1) return n.toLong()
        var a = 0L; var b = 1L
        for (i in 2..n) {
            val next = a + b
            a = b; b = next
        }
        return b
    }

    private fun runDomTreeBuild(): Double {
        class DomElement(val tag: String, val id: String) {
            val children = mutableListOf<DomElement>()
        }
        val startTime = System.nanoTime()
        var count = 0
        for (pass in 0 until 10) {
            val root = DomElement("html", "root")
            val body = DomElement("body", "body")
            root.children.add(body)
            count += 2
            for (i in 0 until 500) {
                val div = DomElement("div", "div_$i")
                body.children.add(div)
                count++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return count.toDouble() / elapsed
    }

    private fun runCssSelectorMatch(): Double {
        val classes = List(100) { "class_$it" }
        val random = Random(42)
        val elements = List(1000) { classes[random.nextInt(100)] }
        val selectors = List(50) { classes[random.nextInt(100)] }
        val startTime = System.nanoTime()
        var matchCount = 0
        for (pass in 0 until 10) {
            for (selector in selectors) {
                for (element in elements) {
                    if (element == selector) {
                        matchCount++
                    }
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (50.0 * 1000.0 * 10) / elapsed
    }

    private fun runJsonParseSpeed(): Double {
        val root = JSONObject()
        val array = JSONArray()
        for (i in 0 until 1000) {
            val item = JSONObject()
            item.put("id", i)
            item.put("val", "test_json_parse_speed_value_$i")
            array.put(item)
        }
        root.put("items", array)
        val jsonStr = root.toString()
        val dataSize = jsonStr.length.toDouble() / (1024.0 * 1024.0)
        
        val startTime = System.nanoTime()
        for (pass in 0 until 10) {
            val parsed = JSONObject(jsonStr)
            val len = parsed.getJSONArray("items").length()
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (dataSize * 10.0) / elapsed
    }

    private fun runJsonStringifySpeed(): Double {
        val list = List(1000) { i ->
            mapOf("id" to i, "val" to "test_json_stringify_speed_value_$i")
        }
        val startTime = System.nanoTime()
        var totalLength = 0L
        for (pass in 0 until 10) {
            val arr = JSONArray(list)
            val jsonStr = arr.toString()
            totalLength += jsonStr.length
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val totalMb = totalLength.toDouble() / (1024.0 * 1024.0)
        return totalMb / elapsed
    }

    private fun runXmlParseSpeed(): Double {
        val xmlBuilder = StringBuilder()
        xmlBuilder.append("<root>")
        for (i in 0 until 100) {
            xmlBuilder.append("<item id=\"$i\"><name>Name $i</name></item>")
        }
        xmlBuilder.append("</root>")
        val xml = xmlBuilder.toString()
        val sizeKb = xml.length.toDouble() / 1024.0
        
        val startTime = System.nanoTime()
        val idPattern = Pattern.compile("id=\"(\\d+)\"")
        for (pass in 0 until 20) {
            val matcher = idPattern.matcher(xml)
            while (matcher.find()) {
                val id = matcher.group(1)
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (sizeKb * 20.0) / elapsed
    }

    private fun runUrlEncodeDecode(): Double {
        val url = "https://www.google.com/search?q=relab+control+center+android+benchmark+suite+2026&client=chrome"
        val startTime = System.nanoTime()
        var count = 0
        for (pass in 0 until 1000) {
            val enc = URLEncoder.encode(url, "UTF-8")
            val dec = URLDecoder.decode(enc, "UTF-8")
            count += 2
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (count.toDouble() / 1000.0) / elapsed
    }

    private fun runCookieProcessing(): Double {
        val cookies = "session=12345; user=alice; theme=dark; locale=en-US; debug=true; tracking=false"
        val startTime = System.nanoTime()
        var parsedCount = 0
        for (pass in 0 until 5000) {
            val parts = cookies.split("; ")
            for (part in parts) {
                val kv = part.split("=")
                if (kv.size == 2) {
                    parsedCount++
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (parsedCount.toDouble() / 1000.0) / elapsed
    }

    private fun runHttpHeaderParse(): Double {
        val headers = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nContent-Length: 1024\r\nServer: nginx\r\nConnection: keep-alive\r\n\r\n"
        val startTime = System.nanoTime()
        var parsedCount = 0
        for (pass in 0 until 5000) {
            val lines = headers.split("\r\n")
            for (line in lines) {
                if (line.contains(": ")) {
                    parsedCount++
                }
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (parsedCount.toDouble() / 1000.0) / elapsed
    }

    private fun runBase64Processing(): Double {
        val data = ByteArray(128 * 1024) { (it % 256).toByte() }
        val startTime = System.nanoTime()
        for (pass in 0 until 20) {
            val enc = android.util.Base64.encode(data, android.util.Base64.DEFAULT)
            val dec = android.util.Base64.decode(enc, android.util.Base64.DEFAULT)
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        val totalMb = (128.0 * 1024.0 * 20.0 * 2.0 / (1024.0 * 1024.0))
        return totalMb / elapsed
    }

    private fun runUtf8Processing(): Double {
        val text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(100)
        val startTime = System.nanoTime()
        var bytesProcessed = 0L
        for (pass in 0 until 200) {
            val bytes = text.toByteArray(StandardCharsets.UTF_8)
            val str = String(bytes, StandardCharsets.UTF_8)
            bytesProcessed += bytes.size * 2
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (bytesProcessed.toDouble() / (1024.0 * 1024.0)) / elapsed
    }

    private fun runRegexUrlExtraction(): Double {
        val html = "Visit <a href=\"https://google.com\">Google</a> and <a href=\"https://github.com\">GitHub</a>."
        val pattern = Pattern.compile("href=\"(https?://[^\"]+)\"")
        val startTime = System.nanoTime()
        var urlsFound = 0
        for (pass in 0 until 2000) {
            val matcher = pattern.matcher(html)
            while (matcher.find()) {
                val url = matcher.group(1)
                urlsFound++
            }
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return urlsFound.toDouble() / elapsed
    }

    private fun runHtmlEntityDecode(): Double {
        val text = "Alice &amp; Bob &lt;Charlie&gt; &quot;Data&quot;"
        val startTime = System.nanoTime()
        var charCount = 0L
        for (pass in 0 until 5000) {
            val decoded = text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
            charCount += decoded.length
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (charCount.toDouble() / 1000.0) / elapsed
    }

    private fun runMarkdownParse(): Double {
        val md = "# Title\n\nSome **bold** text and *italic* text.\n\n- Item 1\n- Item 2"
        val startTime = System.nanoTime()
        var charCount = 0L
        for (pass in 0 until 2000) {
            val html = md.replace(Regex("^# (.*)$", RegexOption.MULTILINE), "<h1>$1</h1>")
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
                .replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
            charCount += html.length
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (charCount.toDouble() / 1000.0) / elapsed
    }

    private fun runFormDataEncoding(): Double {
        val map = mapOf("username" to "alice", "password" to "secret123", "action" to "submit", "token" to "xyz987")
        val startTime = System.nanoTime()
        var count = 0
        for (pass in 0 until 5000) {
            val sb = StringBuilder()
            map.forEach { (k, v) ->
                if (sb.isNotEmpty()) sb.append("&")
                sb.append(URLEncoder.encode(k, "UTF-8")).append("=").append(URLEncoder.encode(v, "UTF-8"))
            }
            count += map.size
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (count.toDouble() / 1000.0) / elapsed
    }

    private fun runResponseStreaming(): Double {
        val size = 5 * 1024 * 1024
        val data = ByteArray(size)
        val startTime = System.nanoTime()
        val stream = java.io.ByteArrayInputStream(data)
        val buf = ByteArray(32768)
        var totalRead = 0L
        while (true) {
            val read = stream.read(buf)
            if (read == -1) break
            totalRead += read
        }
        val elapsed = (System.nanoTime() - startTime) / 1e9
        return (totalRead.toDouble() / (1024.0 * 1024.0)) / elapsed
    }
}
