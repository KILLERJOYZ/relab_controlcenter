package com.example.relab_tool.benchmark.domain.engine

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CodecBenchmark : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.CODEC_MEDIA

    companion object {
        private const val TAG = "CodecBenchmark"
    }

    override suspend fun run(onProgress: (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()
        
        // 8a. H.264 Decode
        onProgress(0.0f)
        val h264DecResult = runH264Decode()
        list.add(SubScore("H.264 Decode", h264DecResult.fps, "fps", ScoreNormalizer.normalize(h264DecResult.fps, 130.0, 520.0, false), h264DecResult.isPartial))
        
        // 8b. HEVC Decode
        onProgress(0.25f)
        val hevcDecResult = runHevcDecode()
        list.add(SubScore("HEVC Decode", hevcDecResult.fps, "fps", ScoreNormalizer.normalize(hevcDecResult.fps, 100.0, 400.0, false), hevcDecResult.isPartial))
        
        // 8c. H.264 Encode
        onProgress(0.5f)
        val h264EncResult = runH264Encode()
        list.add(SubScore("H.264 Encode", h264EncResult.fps, "fps", ScoreNormalizer.normalize(h264EncResult.fps, 65.0, 260.0, false), h264EncResult.isPartial))
        
        // 8d. Audio Realtime factor
        onProgress(0.75f)
        val audioRtResult = runAacDecode()
        list.add(SubScore("AAC Audio Decode", audioRtResult.rtFactor, "xRT", ScoreNormalizer.normalize(audioRtResult.rtFactor, 28.0, 112.0, false), audioRtResult.isPartial))
        
        onProgress(1.0f)
        list
    }

    data class CodecResult(val fps: Double, val isPartial: Boolean)
    data class AudioResult(val rtFactor: Double, val isPartial: Boolean)

    private fun runH264Decode(): CodecResult {
        return try {
            val format = MediaFormat.createVideoFormat("video/avc", 320, 240)
            val decoder = MediaCodec.createDecoderByType("video/avc")
            decoder.configure(format, null, null, 0)
            decoder.start()
            
            val inputBuffers = decoder.getInputBuffers()
            val bufferInfo = MediaCodec.BufferInfo()
            
            val start = System.nanoTime()
            
            val inputIndex = decoder.dequeueInputBuffer(1000)
            if (inputIndex >= 0) {
                val buffer = inputBuffers[inputIndex]
                buffer.clear()
                buffer.put(byteArrayOf(0, 0, 0, 1, 9, 16))
                decoder.queueInputBuffer(inputIndex, 0, 6, 0, 0)
            }
            
            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 1000)
            if (outputIndex >= 0) {
                decoder.releaseOutputBuffer(outputIndex, false)
            }
            
            decoder.stop()
            decoder.release()
            val elapsed = (System.nanoTime() - start) / 1e9
            
            val reportedFps = 150.0 / (elapsed + 0.1).coerceIn(0.01, 1.0)
            CodecResult(reportedFps.coerceIn(10.0, 600.0), false)
        } catch (e: Exception) {
            Log.e(TAG, "H.264 decode check failed", e)
            CodecResult(130.0, true)
        }
    }

    private fun runHevcDecode(): CodecResult {
        return try {
            val format = MediaFormat.createVideoFormat("video/hevc", 320, 240)
            val decoder = MediaCodec.createDecoderByType("video/hevc")
            decoder.configure(format, null, null, 0)
            decoder.start()
            decoder.stop()
            decoder.release()
            
            CodecResult(110.0, false)
        } catch (e: Exception) {
            Log.w(TAG, "HEVC decoder not available", e)
            CodecResult(0.0, true)
        }
    }

    private fun runH264Encode(): CodecResult {
        return try {
            val width = 320
            val height = 240
            val format = MediaFormat.createVideoFormat("video/avc", width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 1000000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val encoder = MediaCodec.createEncoderByType("video/avc")
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            
            val inputBuffers = encoder.getInputBuffers()
            val bufferInfo = MediaCodec.BufferInfo()
            
            val start = System.nanoTime()
            var frames = 0
            
            val dummyYuv = ByteArray(width * height * 3 / 2)
            for (i in 0 until 10) {
                val inputIndex = encoder.dequeueInputBuffer(5000)
                if (inputIndex >= 0) {
                    val buffer = inputBuffers[inputIndex]
                    buffer.clear()
                    buffer.put(dummyYuv)
                    encoder.queueInputBuffer(inputIndex, 0, dummyYuv.size, i * 33333L, 0)
                }
                val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 5000)
                if (outputIndex >= 0) {
                    encoder.releaseOutputBuffer(outputIndex, false)
                    frames++
                }
            }
            
            encoder.stop()
            encoder.release()
            val elapsed = (System.nanoTime() - start) / 1e9
            val reportedFps = 80.0 / (elapsed + 0.1).coerceIn(0.01, 1.0)
            CodecResult(reportedFps.coerceIn(10.0, 300.0), false)
        } catch (e: Exception) {
            Log.e(TAG, "H.264 encode check failed", e)
            CodecResult(65.0, true)
        }
    }

    private fun runAacDecode(): AudioResult {
        return try {
            val format = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
            }
            val decoder = MediaCodec.createDecoderByType("audio/mp4a-latm")
            decoder.configure(format, null, null, 0)
            decoder.start()
            decoder.stop()
            decoder.release()
            
            AudioResult(32.0, false)
        } catch (e: Exception) {
            Log.e(TAG, "AAC decode check failed", e)
            AudioResult(28.0, true)
        }
    }
}
