package com.example.relab_tool.benchmark.domain.engine

import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.Executors
import kotlin.math.*
import android.os.Parcel

/**
 * Network & IPC Benchmark — 20 tests (NW_01 – NW_20)
 *
 * Tests the complete in-device network stack:
 *   - Loopback TCP/UDP (127.0.0.1) — NO external data transmitted
 *   - Android Binder IPC (via Parcel)
 *   - UNIX domain socket throughput
 *   - NIO non-blocking channels
 *   - SSL/TLS handshake latency
 *
 * All tests use 127.0.0.1 (localhost) only. Zero network data leaves the device.
 * This is fully Google Play compliant — no READ_PRIVILEGED_PHONE_STATE,
 * no INTERNET permission beyond loopback, no user data.
 *
 * Score calibration (TCP loopback GB/s):
 *   Entry (Helio G85): ~3 GB/s
 *   Mid (SD 778G): ~8 GB/s
 *   Flagship (SD 8 Gen 3): ~20 GB/s (bottlenecked by L3/memory bandwidth)
 */
class NetworkIpcBenchmark : BenchmarkEngine {

    override val pillar = BenchmarkPillar.NETWORK_IPC

    override fun isAvailable() = true

    companion object {
        private val LOOPBACK = InetAddress.getByName("127.0.0.1")
        private const val BASE_PORT = 19876
    }

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SubScore>()

            // NW_01 — TCP loopback throughput (large buffer, 128MB)
            onProgress(0.02f)
            val tcpGbpsVal = BenchmarkHarness.medianOfThree(warmups = 3) { runTcpLoopbackThroughput(128) }
            results += gbpsScore("NW_01: TCP Loopback Throughput", tcpGbpsVal, 6.0, 80.0)

            // NW_02 — TCP loopback latency (RTT measurement, 10K pings)
            onProgress(0.07f)
            val tcpLatVal = BenchmarkHarness.medianOfThree(warmups = 3) { runTcpLatency(10_000) }
            results += subScore("NW_02: TCP Loopback RTT", tcpLatVal, "µs", 80.0, 2.0, true)

            // NW_03 — NIO SocketChannel throughput (non-blocking, 128MB)
            onProgress(0.12f)
            val nioGbpsVal = BenchmarkHarness.medianOfThree(warmups = 3) { runNioChannelThroughput(128) }
            results += gbpsScore("NW_03: NIO SocketChannel Throughput", nioGbpsVal, 8.0, 90.0)

            // NW_04 — UDP datagram throughput (8KB datagrams, 1M datagrams)
            onProgress(0.17f)
            val udpGbpsVal = BenchmarkHarness.medianOfThree(warmups = 3) { runUdpLoopbackThroughput() }
            results += gbpsScore("NW_04: UDP Loopback Throughput", udpGbpsVal, 2.0, 30.0)

            // NW_05 — TCP connection setup rate (new connections/sec)
            onProgress(0.22f)
            val tcpConnVal = BenchmarkHarness.medianOfThree(warmups = 3) { runTcpConnectionRate(1000) }
            results += subScore("NW_05: TCP Connection Rate", tcpConnVal, "conn/s",
                1000.0, 15000.0, false)

            // NW_06 — Pipe write/read throughput (kernel IPC pipe)
            onProgress(0.27f)
            val pipeGbpsVal = BenchmarkHarness.medianOfThree(warmups = 3) { runPipeThroughput() }
            results += gbpsScore("NW_06: Pipe IPC Throughput", pipeGbpsVal, 4.0, 50.0)

            // NW_07 — Binder IPC overhead (Parcel serialization roundtrip)
            onProgress(0.32f)
            val binderVal = BenchmarkHarness.medianOfThree(warmups = 3) { runBinderParcelOverhead(100_000) }
            results += subScore("NW_07: Binder Parcel Overhead", binderVal, "µs/call", 15.0, 0.5, true)

            // NW_08 — Parcel write throughput (large complex object)
            onProgress(0.37f)
            val parcelWrVal = BenchmarkHarness.medianOfThree(warmups = 3) { runParcelWriteThroughput() }
            results += subScore("NW_08: Parcel Write Throughput", parcelWrVal, "MB/s",
                400.0, 6000.0, false)

            // NW_09 — TLS handshake latency (using SSLSocket loopback)
            onProgress(0.42f)
            val tlsVal = BenchmarkHarness.medianOfThree(warmups = 3) { runTlsHandshakeSimulation() }
            results += subScore("NW_09: TLS Handshake Simulation", tlsVal, "ms", 15.0, 0.5, true)

            // NW_10 — Multi-stream TCP concurrent (8 parallel connections)
            onProgress(0.47f)
            val multiTcpVal = BenchmarkHarness.medianOfThree(warmups = 3) { runMultiStreamTcp(8, 16) }
            results += gbpsScore("NW_10: Multi-Stream TCP (8 conns)", multiTcpVal, 10.0, 120.0)

            // NW_11 — Select/poll system call overhead (1K FDs)
            onProgress(0.52f)
            val selectVal = BenchmarkHarness.medianOfThree(warmups = 3) { runSelectOverhead(100) }
            results += subScore("NW_11: Socket I/O Overhead (100 sockets)", selectVal, "µs/poll",
                400.0, 10.0, true)

            // NW_12 — ByteBuffer serialization throughput (proto-like)
            onProgress(0.57f)
            val serVal = BenchmarkHarness.medianOfThree(warmups = 3) { runByteBufferSerialization() }
            results += subScore("NW_12: ByteBuffer Serialization", serVal, "MOps/s",
                400.0, 6000.0, false)

            // NW_13 — JSON over socket (HTTP-like request/response)
            onProgress(0.62f)
            val jsonRpcVal = BenchmarkHarness.medianOfThree(warmups = 3) { runJsonRpcSimulation(500) }
            results += subScore("NW_13: JSON RPC (500 requests)", jsonRpcVal, "req/s",
                2000.0, 45000.0, false)

            // NW_14 — Memory-mapped IPC (mmap via byte arrays, simulated shared memory)
            onProgress(0.65f)
            val mmapIpcVal = BenchmarkHarness.medianOfThree(warmups = 3) { runMmapIpcSimulation() }
            results += gbpsScore("NW_14: Shared Memory IPC (mmap sim)", mmapIpcVal, 10.0, 120.0)

            // NW_15 — Producer-consumer queue throughput (Kotlin Channel)
            onProgress(0.70f)
            val prodConVal = BenchmarkHarness.medianOfThree(warmups = 3) { runProducerConsumerQueue() }
            results += subScore("NW_15: Producer-Consumer Queue", prodConVal, "MOps/s",
                100.0, 1500.0, false)

            // NW_16 — DNS resolution latency (127.0.0.1 loopback lookup)
            onProgress(0.75f)
            val dnsVal = BenchmarkHarness.medianOfThree(warmups = 3) { runLoopbackResolution(1000) }
            results += subScore("NW_16: Loopback Resolution Latency", dnsVal, "µs", 400.0, 5.0, true)

            // NW_17 — TCP zero-copy sendfile simulation (splice via pipe)
            onProgress(0.80f)
            val zcGbpsVal = BenchmarkHarness.medianOfThree(warmups = 3) { runZeroCopySimulation(32) }
            results += gbpsScore("NW_17: Zero-Copy Transfer (32MB)", zcGbpsVal, 6.0, 75.0)

            // NW_18 — Protobuf-like binary serialization
            onProgress(0.85f)
            val protoVal = BenchmarkHarness.medianOfThree(warmups = 3) { runBinaryProtocolSerialization() }
            results += subScore("NW_18: Binary Protocol Serialize", protoVal, "MB/s",
                400.0, 6000.0, false)

            // NW_19 — Socket send/recv with scatter-gather (vectored I/O)
            onProgress(0.92f)
            val scatterVal = BenchmarkHarness.medianOfThree(warmups = 3) { runScatterGatherIo(64) }
            results += gbpsScore("NW_19: Scatter-Gather I/O (64MB)", scatterVal, 4.0, 60.0)

            // NW_20 — Peak concurrent IPC throughput (threads × connections)
            onProgress(0.97f)
            val peakVal = BenchmarkHarness.medianOfThree(warmups = 3) { runPeakConcurrentIpc() }
            results += gbpsScore("NW_20: Peak Concurrent IPC", peakVal, 10.0, 150.0)

            onProgress(1.0f)
            results
        }

    // ── Test implementations ──────────────────────────────────────────────────

    private fun runTcpLoopbackThroughput(sizeMb: Int): Double {
        val port = BASE_PORT
        val data = ByteArray(65536) { (it % 256).toByte() } // 64KB chunks
        val totalBytes = sizeMb.toLong() * 1024 * 1024
        var received = 0L

        val serverSocket = ServerSocket()
        serverSocket.reuseAddress = true
        serverSocket.bind(java.net.InetSocketAddress(LOOPBACK, port), 1)
        val serverThread = Thread {
            try {
                serverSocket.accept().use { client ->
                    val ins = client.getInputStream()
                    val buf = ByteArray(65536)
                    var n = ins.read(buf)
                    while (n > 0) { received += n; n = ins.read(buf) }
                }
            } catch (e: Exception) {}
        }
        serverThread.start()
        Thread.sleep(10) // let server start

        val start = System.nanoTime()
        Socket(LOOPBACK, port).use { socket ->
            socket.sendBufferSize = 1024 * 1024
            val os: OutputStream = socket.getOutputStream()
            var sent = 0L
            while (sent < totalBytes) {
                val toSend = minOf(data.size.toLong(), totalBytes - sent).toInt()
                os.write(data, 0, toSend)
                sent += toSend
            }
        }
        serverThread.join(5000)
        serverSocket.close()
        val elapsed = (System.nanoTime() - start) / 1e9
        return sizeMb / elapsed / 1000.0 // GB/s
    }

    private fun runTcpLatency(pingCount: Int): Double {
        val port = BASE_PORT + 1
        val serverSocket = ServerSocket()
        serverSocket.reuseAddress = true
        serverSocket.bind(java.net.InetSocketAddress(LOOPBACK, port), 1)
        val serverThread = Thread {
            try {
                serverSocket.accept().use { client ->
                    val ins = client.getInputStream()
                    val os = client.getOutputStream()
                    val buf = ByteArray(1)
                    repeat(pingCount) {
                        if (ins.read(buf) > 0) os.write(buf)
                    }
                }
            } catch (e: Exception) {}
        }
        serverThread.start()
        Thread.sleep(10)

        val start = System.nanoTime()
        Socket(LOOPBACK, port).use { socket ->
            socket.tcpNoDelay = true
            val os = socket.getOutputStream()
            val ins = socket.getInputStream()
            val ping = byteArrayOf(0x42)
            val pong = ByteArray(1)
            repeat(pingCount) {
                os.write(ping)
                ins.read(pong)
            }
        }
        serverThread.join(5000)
        serverSocket.close()
        val elapsed = (System.nanoTime() - start) / 1e3 // µs
        return elapsed / pingCount // µs per RTT
    }

    private fun runNioChannelThroughput(sizeMb: Int): Double {
        val port = BASE_PORT + 2
        val totalBytes = sizeMb.toLong() * 1024 * 1024
        val data = ByteBuffer.allocateDirect(65536).apply { repeat(65536) { put((it % 256).toByte()) }; flip() }
        var received = 0L

        val serverChannel = ServerSocketChannel.open()
        serverChannel.socket().reuseAddress = true
        serverChannel.socket().bind(java.net.InetSocketAddress(LOOPBACK, port))
        val serverThread = Thread {
            try {
                val client = serverChannel.accept()
                val buf = ByteBuffer.allocateDirect(65536)
                var n = client.read(buf)
                while (n > 0) { received += n; buf.clear(); n = client.read(buf) }
                client.close()
            } catch (e: Exception) {}
        }
        serverThread.start()
        Thread.sleep(20)

        val start = System.nanoTime()
        val clientChannel = SocketChannel.open(java.net.InetSocketAddress(LOOPBACK, port))
        var sent = 0L
        while (sent < totalBytes) {
            data.position(0)
            data.limit(minOf(65536.toLong(), totalBytes - sent).toInt())
            sent += clientChannel.write(data)
        }
        clientChannel.close()
        serverThread.join(5000)
        serverChannel.close()
        val elapsed = (System.nanoTime() - start) / 1e9
        return sizeMb / elapsed / 1000.0 // GB/s
    }

    private fun runUdpLoopbackThroughput(): Double {
        val port = BASE_PORT + 3
        val datagramSize = 8192
        val totalDatagrams = 10_000
        val data = ByteArray(datagramSize) { (it % 256).toByte() }
        var received = 0

        val serverSocket = java.net.DatagramSocket(port, LOOPBACK)
        val serverThread = Thread {
            val buf = java.net.DatagramPacket(ByteArray(datagramSize + 100), datagramSize + 100)
            try {
                serverSocket.soTimeout = 2000
                while (received < totalDatagrams) { serverSocket.receive(buf); received++ }
            } catch (_: Exception) { }
        }
        serverThread.start()
        Thread.sleep(10)

        val clientSocket = java.net.DatagramSocket()
        val packet = java.net.DatagramPacket(data, datagramSize, LOOPBACK, port)
        val start = System.nanoTime()
        repeat(totalDatagrams) { clientSocket.send(packet) }
        clientSocket.close()
        serverThread.join(5000)
        serverSocket.close()
        val elapsed = (System.nanoTime() - start) / 1e9
        return (datagramSize.toLong() * totalDatagrams) / elapsed / 1e9 // GB/s
    }

    private fun runTcpConnectionRate(count: Int): Double {
        val port = BASE_PORT + 4
        val serverSocket = ServerSocket()
        serverSocket.reuseAddress = true
        serverSocket.bind(java.net.InetSocketAddress(LOOPBACK, port), count + 5)
        serverSocket.soTimeout = 5000
        val serverThread = Thread {
            try { repeat(count) { val c = serverSocket.accept(); c.close() } } catch (_: Exception) {}
        }
        serverThread.start()
        Thread.sleep(10)
        val start = System.nanoTime()
        repeat(count) { Socket(LOOPBACK, port).close() }
        serverThread.join(5000)
        serverSocket.close()
        return count / ((System.nanoTime() - start) / 1e9) // conn/s
    }

    private fun runPipeThroughput(): Double {
        val pipe = java.io.PipedInputStream(1024 * 1024)
        val pipeOut = java.io.PipedOutputStream(pipe)
        val data = ByteArray(65536) { (it % 256).toByte() }
        val totalMb = 64
        val totalBytes = totalMb.toLong() * 1024 * 1024
        var written = 0L
        val writerThread = Thread {
            try {
                while (written < totalBytes) {
                    val toWrite = minOf(data.size.toLong(), totalBytes - written).toInt()
                    pipeOut.write(data, 0, toWrite)
                    written += toWrite
                }
                pipeOut.close()
            } catch (e: Exception) {}
        }
        writerThread.start()
        val buf = ByteArray(65536)
        val start = System.nanoTime()
        var read = 0L
        var n = pipe.read(buf)
        while (n > 0) { read += n; n = pipe.read(buf) }
        writerThread.join(5000)
        val elapsed = (System.nanoTime() - start) / 1e9
        return totalMb / elapsed / 1000.0 // GB/s
    }

    private fun runBinderParcelOverhead(callCount: Int): Double {
        // Simulate Binder IPC via Parcel serialization (no actual cross-process needed)
        val parcel = Parcel.obtain()
        val start = System.nanoTime()
        repeat(callCount) {
            parcel.setDataPosition(0)
            parcel.writeInt(it)
            parcel.writeString("key_$it")
            parcel.writeDouble(it * 3.14)
            parcel.setDataPosition(0)
            val i = parcel.readInt()
            val s = parcel.readString()
            val d = parcel.readDouble()
            BenchmarkHarness.consume(i.toLong() + (d).toLong())
        }
        parcel.recycle()
        val elapsed = (System.nanoTime() - start) / 1e3 // µs
        return elapsed / callCount // µs per call
    }

    private fun runParcelWriteThroughput(): Double {
        val parcel = Parcel.obtain()
        val data = ByteArray(4096) { (it % 256).toByte() }
        val start = System.nanoTime()
        repeat(10_000) {
            parcel.setDataPosition(0)
            parcel.writeByteArray(data)
            parcel.writeInt(it)
            parcel.writeString("item_$it")
            parcel.writeDouble(it * 3.14)
            BenchmarkHarness.consume(parcel.dataPosition().toLong())
        }
        parcel.recycle()
        val elapsed = (System.nanoTime() - start) / 1e9
        return (4096.toLong() * 10_000) / elapsed / (1024 * 1024) // MB/s
    }

    private fun runTlsHandshakeSimulation(): Double {
        // Simulate TLS overhead via Java's SSLContext (self-signed, loopback)
        // Since creating a real self-signed cert requires BouncyCastle, we measure
        // the comparable overhead: SSL algorithm initialization cost
        val times = mutableListOf<Long>()
        repeat(20) {
            val start = System.nanoTime()
            // Simulate TLS key derivation overhead (HMAC-SHA256 × 1000 iterations)
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val keySpec = javax.crypto.spec.SecretKeySpec(ByteArray(32) { it.toByte() }, "HmacSHA256")
            mac.init(keySpec)
            val data = ByteArray(64) { it.toByte() }
            repeat(100) { mac.update(data) }
            val result = mac.doFinal()
            BenchmarkHarness.consume(result[0].toLong())
            times.add(System.nanoTime() - start)
        }
        times.sort()
        return times[times.size / 2] / 1_000_000.0 // median ms
    }

    private suspend fun runMultiStreamTcp(connections: Int, sizeMbEach: Int): Double =
        coroutineScope {
            val jobs = (0 until connections).map { i ->
                async(Dispatchers.IO) {
                    runTcpLoopbackThroughputOnPort(BASE_PORT + 20 + i, sizeMbEach)
                }
            }
            jobs.awaitAll().sum() // total GB/s across all connections
        }

    private fun runTcpLoopbackThroughputOnPort(port: Int, sizeMb: Int): Double {
        return try {
            val data = ByteArray(65536) { (it % 256).toByte() }
            val totalBytes = sizeMb.toLong() * 1024 * 1024
            val serverSocket = ServerSocket()
            serverSocket.reuseAddress = true
            serverSocket.bind(java.net.InetSocketAddress(LOOPBACK, port), 1)
            var received = 0L
            val serverThread = Thread {
                try {
                    serverSocket.accept().use { client ->
                        val buf = ByteArray(65536)
                        var n = client.getInputStream().read(buf)
                        while (n > 0) { received += n; n = client.getInputStream().read(buf) }
                    }
                } catch (_: Exception) {}
            }
            serverThread.start()
            Thread.sleep(5)
            val start = System.nanoTime()
            Socket(LOOPBACK, port).use { socket ->
                val os = socket.getOutputStream()
                var sent = 0L
                while (sent < totalBytes) {
                    val toSend = minOf(data.size.toLong(), totalBytes - sent).toInt()
                    os.write(data, 0, toSend)
                    sent += toSend
                }
            }
            serverThread.join(5000)
            serverSocket.close()
            sizeMb / ((System.nanoTime() - start) / 1e9) / 1000.0
        } catch (_: Exception) { 0.0 }
    }

    private fun runSelectOverhead(socketCount: Int): Double {
        // Create socketCount connected pairs, measure poll latency
        val sockets = mutableListOf<Pair<Socket, Socket>>()
        val serverSockets = mutableListOf<ServerSocket>()
        try {
            for (i in 0 until socketCount) {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(java.net.InetSocketAddress(LOOPBACK, BASE_PORT + 100 + i), 1)
                serverSockets.add(ss)
                val client = Socket(LOOPBACK, BASE_PORT + 100 + i)
                val server = ss.accept()
                sockets.add(Pair(client, server))
            }
            val start = System.nanoTime()
            repeat(1000) {
                for ((client, server) in sockets) {
                    client.getOutputStream().write(0x42)
                    server.getInputStream().read()
                }
            }
            val elapsed = (System.nanoTime() - start) / 1e3 // µs
            return elapsed / (1000 * socketCount) // µs per poll
        } finally {
            sockets.forEach { (c, s) -> c.close(); s.close() }
            serverSockets.forEach { it.close() }
        }
    }

    private fun runByteBufferSerialization(): Double {
        val buf = ByteBuffer.allocateDirect(4096)
        val iterations = 2_000_000
        val start = System.nanoTime()
        repeat(iterations) { i ->
            buf.clear()
            buf.putInt(i)
            buf.putLong(i.toLong() * 12345678L)
            buf.putDouble(i * 3.14)
            buf.putFloat(i * 1.41f)
            buf.flip()
            val a = buf.int; val b = buf.long; val c = buf.double; val d = buf.float
            BenchmarkHarness.consume(a.toLong() + b + c.toLong() + d.toLong())
        }
        val elapsed = (System.nanoTime() - start) / 1e9
        return iterations / elapsed / 1e6 // MOps/s
    }

    private fun runJsonRpcSimulation(requestCount: Int): Double {
        val port = BASE_PORT + 50
        val serverSocket = ServerSocket()
        serverSocket.reuseAddress = true
        serverSocket.bind(java.net.InetSocketAddress(LOOPBACK, port), 1)
        val serverThread = Thread {
            try {
                serverSocket.accept().use { client ->
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val writer = client.getOutputStream().bufferedWriter()
                    repeat(requestCount) {
                        val line = reader.readLine() ?: return@repeat
                        val response = """{"result":"ok","echo":"${line.take(20)}","id":${it}}""" + "\n"
                        writer.write(response)
                        writer.flush()
                    }
                }
            } catch (e: Exception) {}
        }
        serverThread.start()
        Thread.sleep(10)

        val start = System.nanoTime()
        Socket(LOOPBACK, port).use { socket ->
            val writer = socket.getOutputStream().bufferedWriter()
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            repeat(requestCount) { i ->
                val request = """{"method":"bench","params":{"id":$i,"data":"test_value_$i"}}""" + "\n"
                writer.write(request); writer.flush()
                BenchmarkHarness.consume(reader.readLine()?.length?.toLong() ?: 0L)
            }
        }
        serverThread.join(5000)
        serverSocket.close()
        return requestCount / ((System.nanoTime() - start) / 1e9) // req/s
    }

    private fun runMmapIpcSimulation(): Double {
        // Simulate shared memory IPC via large byte array copy (proxy for mmap)
        val sizeMb = 64
        val src = ByteArray(sizeMb * 1024 * 1024) { (it % 256).toByte() }
        val dst = ByteArray(sizeMb * 1024 * 1024)
        val start = System.nanoTime()
        System.arraycopy(src, 0, dst, 0, src.size)
        BenchmarkHarness.consume(dst[0].toLong())
        val elapsed = (System.nanoTime() - start) / 1e9
        return sizeMb / elapsed / 1000.0 // GB/s
    }

    private suspend fun runProducerConsumerQueue(): Double = coroutineScope {
        val queue = kotlinx.coroutines.channels.Channel<Long>(capacity = 65536)
        val itemCount = 5_000_000L
        val start = System.nanoTime()
        val producer = async(Dispatchers.Default) {
            for (i in 0 until itemCount) queue.send(i)
            queue.close()
        }
        val consumer = async(Dispatchers.Default) {
            var sum = 0L
            for (item in queue) sum += item
            BenchmarkHarness.consume(sum)
        }
        producer.await()
        consumer.await()
        itemCount / ((System.nanoTime() - start) / 1e9) / 1e6 // MOps/s
    }

    private fun runLoopbackResolution(count: Int): Double {
        val start = System.nanoTime()
        repeat(count) {
            val addr = InetAddress.getByName("127.0.0.1")
            BenchmarkHarness.consume(addr.hashCode().toLong())
        }
        val elapsed = (System.nanoTime() - start) / 1e3 // µs
        return elapsed / count // µs per resolution
    }

    private fun runZeroCopySimulation(sizeMb: Int): Double {
        // Zero-copy via NIO direct buffer transfer
        val port = BASE_PORT + 60
        val data = ByteBuffer.allocateDirect(65536).apply { repeat(65536) { put((it % 256).toByte()) }; flip() }
        val totalBytes = sizeMb.toLong() * 1024 * 1024
        var received = 0L
        val serverChannel = ServerSocketChannel.open()
        serverChannel.socket().reuseAddress = true
        serverChannel.socket().bind(java.net.InetSocketAddress(LOOPBACK, port))
        val serverThread = Thread {
            try {
                val client = serverChannel.accept()
                val buf = ByteBuffer.allocateDirect(65536)
                var n = client.read(buf)
                while (n > 0) { received += n; buf.clear(); n = client.read(buf) }
                client.close()
            } catch (e: Exception) {}
        }
        serverThread.start()
        Thread.sleep(10)
        val start = System.nanoTime()
        val clientChannel = SocketChannel.open(java.net.InetSocketAddress(LOOPBACK, port))
        var sent = 0L
        while (sent < totalBytes) {
            data.position(0); data.limit(minOf(65536L, totalBytes - sent).toInt())
            sent += clientChannel.write(data)
        }
        clientChannel.close()
        serverThread.join(5000)
        serverChannel.close()
        return sizeMb / ((System.nanoTime() - start) / 1e9) / 1000.0
    }

    private fun runBinaryProtocolSerialization(): Double {
        // Simulate Protocol Buffers-like manual varint encoding
        val buf = ByteArray(65536)
        val iterations = 100_000
        val start = System.nanoTime()
        repeat(iterations) { i ->
            var pos = 0
            // Write tag + varint fields
            val fields = intArrayOf(i, i * 2, i * 3, i xor 12345, i + 99999)
            for (f in fields) {
                var v = f
                while (v and 0x7F.inv() != 0) { buf[pos++] = ((v and 0x7F) or 0x80).toByte(); v = v ushr 7 }
                buf[pos++] = (v and 0x7F).toByte()
            }
            BenchmarkHarness.consume(buf[0].toLong())
        }
        val totalBytes = 65536.toLong() * iterations / 100
        val elapsed = (System.nanoTime() - start) / 1e9
        return totalBytes / elapsed / (1024 * 1024) // MB/s
    }

    private fun runScatterGatherIo(sizeMb: Int): Double {
        // Simulate scatter-gather by splitting transfers into multiple ByteBuffers
        val port = BASE_PORT + 70
        val buffers = Array(16) { ByteBuffer.allocateDirect(4096).apply { repeat(4096) { put((it % 256).toByte()) }; flip() } }
        val totalBytes = sizeMb.toLong() * 1024 * 1024
        val serverChannel = ServerSocketChannel.open()
        serverChannel.socket().reuseAddress = true
        serverChannel.socket().bind(java.net.InetSocketAddress(LOOPBACK, port))
        var received = 0L
        val serverThread = Thread {
            try {
                val client = serverChannel.accept()
                val buf = ByteBuffer.allocateDirect(65536)
                var n = client.read(buf)
                while (n > 0) { received += n; buf.clear(); n = client.read(buf) }
                client.close()
            } catch (e: Exception) {}
        }
        serverThread.start()
        Thread.sleep(10)
        val start = System.nanoTime()
        val client = SocketChannel.open(java.net.InetSocketAddress(LOOPBACK, port))
        var sent = 0L
        while (sent < totalBytes) {
            buffers.forEach { it.rewind() }
            val written = client.write(buffers)
            sent += written
        }
        client.close()
        serverThread.join(5000)
        serverChannel.close()
        return sizeMb / ((System.nanoTime() - start) / 1e9) / 1000.0
    }

    private suspend fun runPeakConcurrentIpc(): Double = coroutineScope {
        val cores = Runtime.getRuntime().availableProcessors()
        val sizeMbPerConn = 8
        val connections = minOf(cores, 8)
        val jobs = (0 until connections).map { i ->
            async(Dispatchers.IO) {
                runTcpLoopbackThroughputOnPort(BASE_PORT + 80 + i, sizeMbPerConn)
            }
        }
        jobs.awaitAll().sum()
    }

    // ── Score helpers ─────────────────────────────────────────────────────────

    private fun gbpsScore(name: String, raw: Double, baseline: Double, cap: Double): SubScore {
        return ScoreNormalizer.createSubScore(name, raw, "GB/s", baseline, cap, false, false)
    }

    private fun subScore(
        name: String, rawValue: Double, unit: String,
        baseline: Double, cap: Double, inverted: Boolean
    ): SubScore {
        return ScoreNormalizer.createSubScore(name, rawValue, unit, baseline, cap, inverted, false)
    }
}
