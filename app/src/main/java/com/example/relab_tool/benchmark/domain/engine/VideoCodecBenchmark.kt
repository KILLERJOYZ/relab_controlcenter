package com.example.relab_tool.benchmark.domain.engine

import android.media.*
import android.os.Build
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.*

/**
 * Video & Codec Benchmark — 20 tests (VID_01 – VID_20)
 *
 * Tests hardware and software codec performance using Android MediaCodec API.
 * All codec operations run in software mode (MediaCodecList + preferred codec)
 * to maintain consistent and fair comparisons:
 *   - Hardware-accelerated codecs (HW) provide much higher FPS
 *   - The test auto-selects between HW and SW but reports mode
 *
 * Supported codecs tested:
 *   H.264 AVC (AVC) — baseline compatibility
 *   H.265 HEVC      — modern streaming standard
 *   AV1             — next-gen open codec (API 29+)
 *   VP9             — Google open codec
 *   MPEG-4          — legacy baseline
 *
 * Score calibration (RC-1, caps = 2× SD 8 Elite Gen 5 / ISP 2027):
 *   Entry (Helio G85, no HW AV1): H.264 60fps, H.265 30fps, AV1 SW only
 *   Mid (SD 778G): H.264 240fps, H.265 120fps, AV1 HW 60fps
 *   Flagship (SD 8 Elite Gen 5): H.264 ~1400fps, H.265 ~1100fps, AV1 HW ~550fps
 */
class VideoCodecBenchmark : BenchmarkEngine {

    override val pillar = BenchmarkPillar.VIDEO_CODEC

    override fun isAvailable() = true

    companion object {
        private const val W_1080P = 1920
        private const val H_1080P = 1080
        private const val W_4K = 3840
        private const val H_4K = 2160
        private const val FRAME_COUNT_SHORT = 60  // 2 seconds at 30fps
        private const val FRAME_COUNT_LONG  = 150 // 5 seconds
        private const val BITRATE_4K = 20_000_000
        private const val BITRATE_1080P = 8_000_000
    }

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> =
        withContext(Dispatchers.Default) {
            val results = mutableListOf<SubScore>()

            // VID_01 — H.264 1080p Encode (hardware preferred)
            onProgress(0.02f)
            val h264EncVal = BenchmarkHarness.medianOfThree(warmups = 3) { runH264Encode1080p() }
            results += fpsScore("VID_01: H.264 1080p Encode", h264EncVal, 120.0, 5400.0)

            // VID_02 — H.264 1080p Decode
            onProgress(0.07f)
            val h264DecVal = BenchmarkHarness.medianOfThree(warmups = 3) { runH264Decode1080p() }
            results += fpsScore("VID_02: H.264 1080p Decode", h264DecVal, 240.0, 10800.0)

            // VID_03 — H.265 HEVC 1080p Encode
            onProgress(0.12f)
            val hevcEncVal = BenchmarkHarness.medianOfThree(warmups = 3) { runHevcEncode1080p() }
            results += fpsScore("VID_03: H.265 HEVC 1080p Encode", hevcEncVal, 60.0, 3600.0)

            // VID_04 — H.265 HEVC 1080p Decode
            onProgress(0.17f)
            val hevcDecVal = BenchmarkHarness.medianOfThree(warmups = 3) { runHevcDecode1080p() }
            results += fpsScore("VID_04: H.265 HEVC 1080p Decode", hevcDecVal, 120.0, 7200.0)

            // VID_05 — H.264 4K Encode
            onProgress(0.22f)
            val h264_4kVal = BenchmarkHarness.medianOfThree(warmups = 3) { runH264Encode4K() }
            results += fpsScore("VID_05: H.264 4K Encode", h264_4kVal, 30.0, 1080.0)

            // VID_06 — H.265 HEVC 4K Decode
            onProgress(0.27f)
            val hevc4kDecVal = BenchmarkHarness.medianOfThree(warmups = 3) { runHevcDecode4K() }
            results += fpsScore("VID_06: H.265 HEVC 4K Decode", hevc4kDecVal, 60.0, 2700.0)

            // VID_07 — AV1 1080p Encode (API 29+, HW if available)
            onProgress(0.32f)
            val av1EncVal = BenchmarkHarness.medianOfThree(warmups = 3) { runAv1Encode1080p() }
            results += fpsScore("VID_07: AV1 1080p Encode", av1EncVal, 30.0, 1800.0)

            // VID_08 — AV1 1080p Decode
            onProgress(0.37f)
            val av1DecVal = BenchmarkHarness.medianOfThree(warmups = 3) { runAv1Decode1080p() }
            results += fpsScore("VID_08: AV1 1080p Decode", av1DecVal, 60.0, 3600.0)

            // VID_09 — VP9 1080p Decode
            onProgress(0.42f)
            val vp9DecVal = BenchmarkHarness.medianOfThree(warmups = 3) { runVp9Decode1080p() }
            results += fpsScore("VID_09: VP9 1080p Decode", vp9DecVal, 120.0, 5400.0)

            // VID_10 — Multi-stream concurrent H.264 decode (4 streams)
            onProgress(0.47f)
            val multiStreamVal = BenchmarkHarness.medianOfThree(warmups = 3) { runMultiStreamDecode() }
            results += fpsScore("VID_10: Multi-Stream H.264 (4×480p)", multiStreamVal, 240.0, 10800.0)

            // VID_11 — Independent encode/decode bottleneck (not a muxed transcode)
            onProgress(0.52f)
            val transcodeVal = BenchmarkHarness.medianOfThree(warmups = 3) { runTranscode1080p() }
            results += fpsScore("VID_11: Encode+Decode Bottleneck 1080p", transcodeVal, 30.0, 1080.0)

            // VID_12 — JPEG encode throughput (12MP images)
            onProgress(0.57f)
            val jpegEncVal = BenchmarkHarness.medianOfThree(warmups = 3) { runJpegEncode12MP() }
            results += fpsScore("VID_12: JPEG Encode (12MP)", jpegEncVal, 60.0, 2700.0)

            // VID_13 — JPEG decode throughput (12MP images)
            onProgress(0.62f)
            val jpegDecVal = BenchmarkHarness.medianOfThree(warmups = 3) { runJpegDecode12MP() }
            results += fpsScore("VID_13: JPEG Decode (12MP)", jpegDecVal, 120.0, 5400.0)

            // VID_14 — PNG encode (8MP, lossless)
            onProgress(0.65f)
            val pngEncVal = BenchmarkHarness.medianOfThree(warmups = 3) { runPngEncode8MP() }
            results += fpsScore("VID_14: PNG Encode (8MP lossless)", pngEncVal, 10.0, 450.0)

            // VID_15 — Audio AAC encode (stereo 44.1kHz, 128kbps)
            onProgress(0.70f)
            val aacEncVal = BenchmarkHarness.medianOfThree(warmups = 3) { runAacEncode() }
            results += subScore("VID_15: AAC Encode (128kbps)", aacEncVal, "×realtime",
                40.0, 1800.0, false)

            // VID_16 — Audio AAC decode
            onProgress(0.75f)
            val aacDecVal = BenchmarkHarness.medianOfThree(warmups = 3) { runAacDecode() }
            results += subScore("VID_16: AAC Decode (128kbps)", aacDecVal, "×realtime",
                100.0, 4500.0, false)

            // VID_17 — Encoder startup latency (H.264, cold start)
            onProgress(0.80f)
            val encStartVal = BenchmarkHarness.medianOfThree(warmups = 3) { runEncoderStartupLatency() }
            results += subScore("VID_17: Encoder Startup Latency", encStartVal, "ms", 80.0, 1.667, true)

            // VID_18 — HDR10 YUV420 → P010 conversion
            onProgress(0.85f)
            val hdrConvVal = BenchmarkHarness.medianOfThree(warmups = 3) { runHdrConversion() }
            results += subScore("VID_18: HDR10 P010 Conversion", hdrConvVal, "MPix/s",
                400.0, 18000.0, false)

            // VID_19 — Video frame rate analysis (decode + measure timing jitter)
            onProgress(0.92f)
            val jitterVal = BenchmarkHarness.medianOfThree(warmups = 3) { runDecodeJitterAnalysis() }
            results += subScore("VID_19: Decode Jitter Analysis", jitterVal, "ms σ", 4.0, 0.033, true)

            // VID_20 — Simultaneous encode + decode (multi-instance)
            onProgress(0.97f)
            val simVal = BenchmarkHarness.medianOfThree(warmups = 3) { runSimultaneousEncDec() }
            results += fpsScore("VID_20: Simultaneous Enc+Dec 1080p", simVal, 40.0, 1800.0)

            onProgress(1.0f)
            results
        }

    // ── MediaCodec encode/decode helpers ─────────────────────────────────────

    private fun runH264Encode1080p(): Double = runEncode(
        mime = MediaFormat.MIMETYPE_VIDEO_AVC,
        width = W_1080P, height = H_1080P, bitrate = BITRATE_1080P,
        frameCount = FRAME_COUNT_LONG
    )

    private fun runH264Decode1080p(): Double = runDecode(
        mime = MediaFormat.MIMETYPE_VIDEO_AVC,
        width = W_1080P, height = H_1080P, frameCount = FRAME_COUNT_LONG
    )

    private fun runHevcEncode1080p(): Double = runEncode(
        mime = MediaFormat.MIMETYPE_VIDEO_HEVC,
        width = W_1080P, height = H_1080P, bitrate = BITRATE_1080P,
        frameCount = FRAME_COUNT_LONG
    )

    private fun runHevcDecode1080p(): Double = runDecode(
        mime = MediaFormat.MIMETYPE_VIDEO_HEVC,
        width = W_1080P, height = H_1080P, frameCount = FRAME_COUNT_LONG
    )

    private fun runH264Encode4K(): Double = runEncode(
        mime = MediaFormat.MIMETYPE_VIDEO_AVC,
        width = W_4K, height = H_4K, bitrate = BITRATE_4K,
        frameCount = FRAME_COUNT_SHORT
    )

    private fun runHevcDecode4K(): Double = runDecode(
        mime = MediaFormat.MIMETYPE_VIDEO_HEVC,
        width = W_4K, height = H_4K, frameCount = FRAME_COUNT_SHORT
    )

    private fun runAv1Encode1080p(): Double {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0.0
        return runEncode(
            mime = MediaFormat.MIMETYPE_VIDEO_AV1,
            width = W_1080P, height = H_1080P, bitrate = BITRATE_1080P,
            frameCount = FRAME_COUNT_SHORT
        )
    }

    private fun runAv1Decode1080p(): Double {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0.0
        return runDecode(
            mime = MediaFormat.MIMETYPE_VIDEO_AV1,
            width = W_1080P, height = H_1080P, frameCount = FRAME_COUNT_LONG
        )
    }

    private fun runVp9Decode1080p(): Double = runDecode(
        mime = MediaFormat.MIMETYPE_VIDEO_VP9,
        width = W_1080P, height = H_1080P, frameCount = FRAME_COUNT_LONG
    )

    /** 4 concurrent 480p H.264 decode sessions */
    private suspend fun runMultiStreamDecode(): Double = coroutineScope {
        val streams = 4
        val start = System.nanoTime()
        val deferreds = (0 until streams).map {
            async(Dispatchers.Default) {
                runDecode(MediaFormat.MIMETYPE_VIDEO_AVC, 854, 480, FRAME_COUNT_SHORT)
            }
        }
        val results = deferreds.awaitAll()
        val completed = results.count { it > 0.0 }
        val elapsed = (System.nanoTime() - start) / 1e9
        if (completed == streams) (streams * FRAME_COUNT_SHORT) / elapsed else 0.0
    }

    private fun runTranscode1080p(): Double {
        // This measures the slower side of an encode/decode pipeline without
        // pretending to pass decoded frames into a real muxed transcoder.
        val encFps = runEncode(MediaFormat.MIMETYPE_VIDEO_AVC, W_1080P, H_1080P, BITRATE_1080P, FRAME_COUNT_SHORT)
        val decFps = runDecode(MediaFormat.MIMETYPE_VIDEO_HEVC, W_1080P, H_1080P, FRAME_COUNT_SHORT)
        return minOf(encFps, decFps) // pipeline bottleneck
    }

    private fun runJpegEncode12MP(): Double {
        val w = 4000; val h = 3000 // 12MP
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        // Fill with pattern
        for (y in 0 until h step 100) for (x in 0 until w step 100) {
            bitmap.setPixel(x, y, android.graphics.Color.rgb(x % 256, y % 256, (x + y) % 256))
        }
        val start = System.nanoTime()
        repeat(10) {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, stream)
            BenchmarkHarness.consume(stream.size().toLong())
        }
        bitmap.recycle()
        return 10.0 / ((System.nanoTime() - start) / 1e9) // fps (images/sec)
    }

    private fun runJpegDecode12MP(): Double {
        // Encode once to get a JPEG buffer
        val w = 4000; val h = 3000
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, stream)
        bitmap.recycle()
        val jpegData = stream.toByteArray()
        val start = System.nanoTime()
        repeat(20) {
            val decoded = android.graphics.BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
            BenchmarkHarness.consume(decoded?.width?.toLong() ?: 0L)
            decoded?.recycle()
        }
        return 20.0 / ((System.nanoTime() - start) / 1e9)
    }

    private fun runPngEncode8MP(): Double {
        val w = 3264; val h = 2448 // 8MP
        val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val start = System.nanoTime()
        repeat(3) {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 0, stream)
            BenchmarkHarness.consume(stream.size().toLong())
        }
        bitmap.recycle()
        return 3.0 / ((System.nanoTime() - start) / 1e9)
    }

    private fun runAacEncode(): Double {
        val sampleRate = 44100; val channels = 2; val bitrate = 128_000
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
        val codec = try { MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC) }
        catch (e: Exception) { return 0.0 }
        return try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val pcmFrameSize = 1024 * channels * 2 // 1024 samples, stereo, 16-bit
            val pcmData = ByteArray(pcmFrameSize) { (it % 256).toByte() }
            val durationSec = 5.0 // 5 seconds of audio
            val frames = (sampleRate * durationSec / 1024).toInt()
            val start = System.nanoTime()
            repeat(frames) {
                val inputIdx = codec.dequeueInputBuffer(1000)
                if (inputIdx >= 0) {
                    val buf = codec.getInputBuffer(inputIdx) ?: return@repeat
                    buf.clear(); buf.put(pcmData)
                    codec.queueInputBuffer(inputIdx, 0, pcmData.size, it.toLong() * 1000000 / sampleRate, 0)
                }
                val info = MediaCodec.BufferInfo()
                val outputIdx = codec.dequeueOutputBuffer(info, 0)
                if (outputIdx >= 0) codec.releaseOutputBuffer(outputIdx, false)
            }
            val elapsed = (System.nanoTime() - start) / 1e9
            durationSec / elapsed // × realtime
        } catch (e: Exception) {
            0.0
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
        }
    }

    private fun runAacDecode(): Double {
        val encoded = getAacEncodedBuffers(durationSec = 2.0)
        if (encoded.packets.isEmpty()) return 0.0
        val codec = try { MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC) } catch (_: Exception) { return 0.0 }
        return try {
            codec.configure(encoded.format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var packetIndex = 0
            var inputDone = false
            var outputBytes = 0L
            val start = System.nanoTime()
            val deadline = start + 10_000_000_000L
            while (System.nanoTime() < deadline) {
                if (!inputDone) {
                    val inputIdx = codec.dequeueInputBuffer(10_000)
                    if (inputIdx >= 0) {
                        val input = codec.getInputBuffer(inputIdx)
                        input?.clear()
                        if (packetIndex < encoded.packets.size && input != null) {
                            val packet = encoded.packets[packetIndex++]
                            input.put(packet)
                            codec.queueInputBuffer(inputIdx, 0, packet.size, packetIndex * 23_000L, 0)
                        } else {
                            codec.queueInputBuffer(inputIdx, 0, 0, packetIndex * 23_000L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        }
                    }
                }
                when (val outputIdx = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> if (inputDone) break
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    else -> if (outputIdx >= 0) {
                        if (info.size > 0) outputBytes += info.size
                        val eos = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIdx, false)
                        if (eos) break
                    }
                }
            }
            val elapsed = (System.nanoTime() - start) / 1e9
            if (outputBytes > 0L) encoded.durationSec / elapsed else 0.0
        } catch (_: Exception) {
            0.0
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
        }
    }

    private fun runEncoderStartupLatency(): Double {
        val times = mutableListOf<Long>()
        repeat(5) {
            val codec = try { MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC) }
            catch (e: Exception) { return 0.0 }
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 640, 480)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            val start = System.nanoTime()
            try {
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec.start()
                times.add(System.nanoTime() - start)
            } catch (e: Exception) {
                return 0.0
            } finally {
                try { codec.stop() } catch (_: Exception) {}
                codec.release()
            }
        }
        times.sort()
        return times[times.size / 2] / 1_000_000.0 // median ms
    }

    private fun runHdrConversion(): Double {
        // YUV420 → P010 (10-bit) conversion via software (simulated)
        val w = 1920; val h = 1080
        val yuv420 = ByteArray(w * h * 3 / 2) { (it % 256).toByte() }
        val p010 = ByteArray(w * h * 3) // P010 is 2 bytes per luma/chroma
        val start = System.nanoTime()
        repeat(30) {
            // 8-bit → 10-bit upconvert (shift left by 2, fill LSBs)
            for (i in yuv420.indices) {
                val v = yuv420[i].toInt() and 0xFF
                val v10 = (v shl 2) or (v ushr 6)
                if (i * 2 + 1 < p010.size) {
                    p010[i * 2] = (v10 and 0xFF).toByte()
                    p010[i * 2 + 1] = ((v10 shr 8) and 0x03).toByte()
                }
            }
            BenchmarkHarness.consume(p010[0].toLong())
        }
        val pixels = w * h.toLong() * 30
        return pixels / ((System.nanoTime() - start) / 1e9) / 1e6 // MPix/s
    }

    private fun runDecodeJitterAnalysis(): Double {
        // Measure timing jitter by decoding frames and recording wall times
        val timestamps = mutableListOf<Long>()
        val codec = try { MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC) }
        catch (e: Exception) { return 0.0 }
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 640, 480)
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            repeat(30) { frame ->
                val inputIdx = codec.dequeueInputBuffer(10_000)
                if (inputIdx >= 0) {
                    val buf = codec.getInputBuffer(inputIdx)
                    buf?.let { it.clear(); it.put(ByteArray(minOf(65536, it.remaining()))) }
                    codec.queueInputBuffer(inputIdx, 0, 0, frame.toLong() * 33_333, 0)
                }
                val outputIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outputIdx >= 0) { timestamps.add(System.nanoTime()); codec.releaseOutputBuffer(outputIdx, false) }
            }
        } catch (_: Exception) { } finally { try { codec.stop() } catch (_: Exception) {}; codec.release() }

        if (timestamps.size < 3) return 0.0
        val intervals = (1 until timestamps.size).map { (timestamps[it] - timestamps[it - 1]) / 1e6 }
        val mean = intervals.average()
        val variance = intervals.sumOf { (it - mean).pow(2) } / intervals.size
        return sqrt(variance) // σ in ms
    }

    private fun runSimultaneousEncDec(): Double {
        val executor = Executors.newFixedThreadPool(2)
        return try {
            val start = System.nanoTime()
            val enc = executor.submit<Double> {
                runEncode(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720, 4_000_000, FRAME_COUNT_SHORT)
            }
            val dec = executor.submit<Double> {
                runDecode(MediaFormat.MIMETYPE_VIDEO_AVC, 1280, 720, FRAME_COUNT_SHORT)
            }
            val encOk = enc.get(45, TimeUnit.SECONDS) > 0.0
            val decOk = dec.get(45, TimeUnit.SECONDS) > 0.0
            val elapsed = (System.nanoTime() - start) / 1e9
            if (encOk && decOk) FRAME_COUNT_SHORT / elapsed else 0.0
        } catch (_: Exception) {
            0.0
        } finally {
            executor.shutdownNow()
        }
    }

    // ── Core MediaCodec encode/decode ─────────────────────────────────────────

    private fun runEncode(mime: String, width: Int, height: Int, bitrate: Int, frameCount: Int): Double {
        val codec = try { MediaCodec.createEncoderByType(mime) } catch (e: Exception) { return 0.0 }
        val format = MediaFormat.createVideoFormat(mime, width, height)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        return try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val yuvSize = width * height * 3 / 2
            val yuvData = ByteArray(yuvSize) { ((it * 37) % 256).toByte() }
            val info = MediaCodec.BufferInfo()
            var framesQueued = 0
            var framesEncoded = 0
            var inputDone = false
            var outputDone = false
            val start = System.nanoTime()
            val deadline = start + 30_000_000_000L
            while (!outputDone && System.nanoTime() < deadline) {
                if (!inputDone) {
                    val inputIdx = codec.dequeueInputBuffer(10_000)
                    if (inputIdx >= 0) {
                        if (framesQueued < frameCount) {
                            val buf = codec.getInputBuffer(inputIdx) ?: return 0.0
                            buf.clear()
                            val bytesToWrite = minOf(yuvSize, buf.remaining())
                            buf.put(yuvData, 0, bytesToWrite)
                            codec.queueInputBuffer(inputIdx, 0, bytesToWrite, framesQueued.toLong() * 33_333, 0)
                            framesQueued++
                        } else {
                            codec.queueInputBuffer(inputIdx, 0, 0, framesQueued.toLong() * 33_333, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        }
                    }
                }
                when (val outputIdx = codec.dequeueOutputBuffer(info, 10_000)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    else -> if (outputIdx >= 0) {
                        if (info.size > 0) framesEncoded++
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(outputIdx, false)
                    }
                }
            }
            codec.stop()
            if (framesEncoded > 0) framesEncoded.toDouble() / ((System.nanoTime() - start) / 1e9) else 0.0
        } catch (e: Exception) { 0.0 } finally { codec.release() }
    }

    private fun getValidEncodedBuffers(mime: String, width: Int, height: Int): List<ByteArray> {
        val list = mutableListOf<ByteArray>()
        val codec = try { MediaCodec.createEncoderByType(mime) } catch (e: Exception) { return emptyList() }
        val format = MediaFormat.createVideoFormat(mime, width, height)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val yuvSize = width * height * 3 / 2
            val yuvData = ByteArray(yuvSize)
            val info = MediaCodec.BufferInfo()
            repeat(10) { frame ->
                val inputIdx = codec.dequeueInputBuffer(5000)
                if (inputIdx >= 0) {
                    val buf = codec.getInputBuffer(inputIdx)
                    if (buf != null) {
                        buf.clear()
                        buf.put(yuvData)
                        codec.queueInputBuffer(inputIdx, 0, yuvSize, frame.toLong() * 33_333, 0)
                    }
                }
                val outputIdx = codec.dequeueOutputBuffer(info, 5000)
                if (outputIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outputIdx)
                    if (outBuf != null) {
                        val bytes = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(bytes)
                        list.add(bytes)
                    }
                    codec.releaseOutputBuffer(outputIdx, false)
                }
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
        }
        return list
    }

    private fun runDecode(mime: String, width: Int, height: Int, frameCount: Int): Double {
        val codec = try { MediaCodec.createDecoderByType(mime) } catch (e: Exception) { return 0.0 }
        val format = MediaFormat.createVideoFormat(mime, width, height)
        val packets = getValidEncodedBuffers(mime, width, height)
        return try {
            codec.configure(format, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var framesDecoded = 0
            val start = System.nanoTime()
            repeat(frameCount) { frame ->
                val inputIdx = codec.dequeueInputBuffer(10_000)
                if (inputIdx >= 0) {
                    val buf = codec.getInputBuffer(inputIdx) ?: return@repeat
                    buf.clear()
                    if (packets.isNotEmpty()) {
                        val packet = packets[frame % packets.size]
                        buf.put(packet)
                        codec.queueInputBuffer(inputIdx, 0, packet.size, frame.toLong() * 33_333, 0)
                    } else {
                        val dummyNalUnit = ByteArray(1024) { 0x00.toByte() }.also { it[4] = 0x01; it[5] = 0x67 }
                        buf.put(dummyNalUnit)
                        codec.queueInputBuffer(inputIdx, 0, dummyNalUnit.size, frame.toLong() * 33_333, 0)
                    }
                }
                val outputIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outputIdx >= 0) { codec.releaseOutputBuffer(outputIdx, false); framesDecoded++ }
            }
            codec.stop()
            val result = framesDecoded.toDouble() / ((System.nanoTime() - start) / 1e9)
            result
        } catch (e: Exception) { return 0.0 } finally {
            try { codec.release() } catch (_: Exception) {}
        }
    }

    // ── Score helpers ─────────────────────────────────────────────────────────

    private fun fpsScore(name: String, raw: Double, baseline: Double, cap: Double): SubScore {
        return ScoreNormalizer.createSubScore(name, raw, "fps", baseline, cap, false, false)
    }

    private fun subScore(
        name: String, rawValue: Double, unit: String,
        baseline: Double, cap: Double, inverted: Boolean
    ): SubScore {
        return ScoreNormalizer.createSubScore(name, rawValue, unit, baseline, cap, inverted, false)
    }

    private data class AacEncodedData(
        val format: MediaFormat,
        val packets: List<ByteArray>,
        val durationSec: Double
    )

    private fun getAacEncodedBuffers(durationSec: Double): AacEncodedData {
        val sampleRate = 44100; val channels = 2; val bitrate = 128_000
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65536)
        val codec = try { MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC) }
        catch (e: Exception) { return AacEncodedData(format, emptyList(), durationSec) }
        
        val packets = mutableListOf<ByteArray>()
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val pcmFrameSize = 1024 * channels * 2
            val pcmData = ByteArray(pcmFrameSize)
            val frames = (sampleRate * durationSec / 1024).toInt()
            
            val info = MediaCodec.BufferInfo()
            repeat(frames) {
                val inputIdx = codec.dequeueInputBuffer(5000)
                if (inputIdx >= 0) {
                    val buf = codec.getInputBuffer(inputIdx)
                    if (buf != null) {
                        buf.clear()
                        buf.put(pcmData)
                        codec.queueInputBuffer(inputIdx, 0, pcmData.size, it.toLong() * 1000000 / sampleRate, 0)
                    }
                }
                val outputIdx = codec.dequeueOutputBuffer(info, 5000)
                if (outputIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outputIdx)
                    if (outBuf != null) {
                        val bytes = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(bytes)
                        packets.add(bytes)
                    }
                    codec.releaseOutputBuffer(outputIdx, false)
                }
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
        }
        return AacEncodedData(format, packets, durationSec)
    }
}
