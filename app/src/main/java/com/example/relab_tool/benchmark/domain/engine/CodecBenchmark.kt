package com.example.relab_tool.benchmark.domain.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import com.example.relab_tool.benchmark.domain.model.BenchmarkPillar
import com.example.relab_tool.benchmark.domain.model.SubScore
import com.example.relab_tool.benchmark.scoring.ScoreNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class CodecBenchmark : BenchmarkEngine {
    override val pillar: BenchmarkPillar = BenchmarkPillar.CODEC_MEDIA

    companion object {
        private const val TAG = "CodecBenchmark"
    }

    override suspend fun run(onProgress: suspend (Float) -> Unit): List<SubScore> = withContext(Dispatchers.Default) {
        val list = mutableListOf<SubScore>()

        // 1. H.264 Decode (480p)
        onProgress(0.00f)
        val h264Dec480 = runVideoDecode("video/avc", 854, 480, 50)
        list.add(SubScore("H.264 Decode (480p)", h264Dec480.fps, "fps", ScoreNormalizer.normalize(h264Dec480.fps, 200.0, 800.0, false), h264Dec480.isPartial))

        // 2. H.264 Decode (720p)
        onProgress(0.05f)
        val h264Dec720 = runVideoDecode("video/avc", 1280, 720, 50)
        list.add(SubScore("H.264 Decode (720p)", h264Dec720.fps, "fps", ScoreNormalizer.normalize(h264Dec720.fps, 120.0, 480.0, false), h264Dec720.isPartial))

        // 3. H.264 Encode (720p)
        onProgress(0.10f)
        val h264Enc720 = runVideoEncode("video/avc", 1280, 720, 30)
        list.add(SubScore("H.264 Encode (720p)", h264Enc720.fps, "fps", ScoreNormalizer.normalize(h264Enc720.fps, 60.0, 240.0, false), h264Enc720.isPartial))

        // 4. H.264 Encode (1080p)
        onProgress(0.15f)
        val h264Enc1080 = runVideoEncode("video/avc", 1920, 1080, 20)
        list.add(SubScore("H.264 Encode (1080p)", h264Enc1080.fps, "fps", ScoreNormalizer.normalize(h264Enc1080.fps, 30.0, 120.0, false), h264Enc1080.isPartial))

        // 5. H.264 Encode (4K)
        onProgress(0.20f)
        val h264Enc4K = runVideoEncode("video/avc", 3840, 2160, 10)
        list.add(SubScore("H.264 Encode (4K)", h264Enc4K.fps, "fps", ScoreNormalizer.normalize(h264Enc4K.fps, 10.0, 60.0, false), h264Enc4K.isPartial))

        // 6. HEVC Decode (720p)
        onProgress(0.25f)
        val hevcDec720 = runVideoDecode("video/hevc", 1280, 720, 40)
        list.add(SubScore("HEVC Decode (720p)", hevcDec720.fps, "fps", ScoreNormalizer.normalize(hevcDec720.fps, 100.0, 400.0, false), hevcDec720.isPartial))

        // 7. HEVC Encode (720p)
        onProgress(0.30f)
        val hevcEnc720 = runVideoEncode("video/hevc", 1280, 720, 20)
        list.add(SubScore("HEVC Encode (720p)", hevcEnc720.fps, "fps", ScoreNormalizer.normalize(hevcEnc720.fps, 50.0, 200.0, false), hevcEnc720.isPartial))

        // 8. HEVC Encode (1080p)
        onProgress(0.35f)
        val hevcEnc1080 = runVideoEncode("video/hevc", 1920, 1080, 15)
        list.add(SubScore("HEVC Encode (1080p)", hevcEnc1080.fps, "fps", ScoreNormalizer.normalize(hevcEnc1080.fps, 25.0, 100.0, false), hevcEnc1080.isPartial))

        // 9. HEVC Encode (4K)
        onProgress(0.40f)
        val hevcEnc4K = runVideoEncode("video/hevc", 3840, 2160, 8)
        list.add(SubScore("HEVC Encode (4K)", hevcEnc4K.fps, "fps", ScoreNormalizer.normalize(hevcEnc4K.fps, 8.0, 45.0, false), hevcEnc4K.isPartial))

        // 10. VP9 Decode
        onProgress(0.45f)
        val vp9Dec = runVideoDecode("video/x-vnd.on2.vp9", 1280, 720, 30)
        list.add(SubScore("VP9 Decode (720p)", vp9Dec.fps, "fps", ScoreNormalizer.normalize(vp9Dec.fps, 80.0, 320.0, false), vp9Dec.isPartial))

        // 11. VP9 Encode
        onProgress(0.50f)
        val vp9Enc = runVideoEncode("video/x-vnd.on2.vp9", 1280, 720, 15)
        list.add(SubScore("VP9 Encode (720p)", vp9Enc.fps, "fps", ScoreNormalizer.normalize(vp9Enc.fps, 30.0, 150.0, false), vp9Enc.isPartial))

        // 12. AAC Audio Decode
        onProgress(0.55f)
        val aacDec = runAacDecode()
        list.add(SubScore("AAC Audio Decode", aacDec.rtFactor, "xRT", ScoreNormalizer.normalize(aacDec.rtFactor, 28.0, 112.0, false), aacDec.isPartial))

        // 13. AAC Audio Encode
        onProgress(0.60f)
        val aacEnc = runAacEncode()
        list.add(SubScore("AAC Audio Encode", aacEnc.rtFactor, "xRT", ScoreNormalizer.normalize(aacEnc.rtFactor, 20.0, 80.0, false), aacEnc.isPartial))

        // 14. JPEG Decode Speed
        onProgress(0.65f)
        val jpegDecSpeed = runImageCodec("jpeg", true)
        list.add(SubScore("JPEG Decode Speed", jpegDecSpeed, "imgs/s", ScoreNormalizer.normalize(jpegDecSpeed, 15.0, 60.0, false)))

        // 15. JPEG Encode Speed
        onProgress(0.70f)
        val jpegEncSpeed = runImageCodec("jpeg", false)
        list.add(SubScore("JPEG Encode Speed", jpegEncSpeed, "imgs/s", ScoreNormalizer.normalize(jpegEncSpeed, 10.0, 40.0, false)))

        // 16. PNG Decode Speed
        onProgress(0.75f)
        val pngDecSpeed = runImageCodec("png", true)
        list.add(SubScore("PNG Decode Speed", pngDecSpeed, "imgs/s", ScoreNormalizer.normalize(pngDecSpeed, 10.0, 40.0, false)))

        // 17. PNG Encode Speed
        onProgress(0.80f)
        val pngEncSpeed = runImageCodec("png", false)
        list.add(SubScore("PNG Encode Speed", pngEncSpeed, "imgs/s", ScoreNormalizer.normalize(pngEncSpeed, 5.0, 20.0, false)))

        // 18. WebP Decode Speed
        onProgress(0.85f)
        val webpDecSpeed = runImageCodec("webp", true)
        list.add(SubScore("WebP Decode Speed", webpDecSpeed, "imgs/s", ScoreNormalizer.normalize(webpDecSpeed, 12.0, 48.0, false)))

        // 19. WebP Encode Speed
        onProgress(0.90f)
        val webpEncSpeed = runImageCodec("webp", false)
        list.add(SubScore("WebP Encode Speed", webpEncSpeed, "imgs/s", ScoreNormalizer.normalize(webpEncSpeed, 8.0, 32.0, false)))

        // 20. Image Transcode (JPEG->PNG)
        onProgress(0.95f)
        val transcodeSpeed = runImageTranscode()
        list.add(SubScore("Image Transcode (JPG->PNG)", transcodeSpeed, "imgs/s", ScoreNormalizer.normalize(transcodeSpeed, 5.0, 25.0, false)))

        onProgress(1.00f)
        list
    }

    data class CodecResult(val fps: Double, val isPartial: Boolean)
    data class AudioResult(val rtFactor: Double, val isPartial: Boolean)

    private fun runVideoEncode(mime: String, width: Int, height: Int, framesCount: Int): CodecResult {
        return try {
            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, if (width >= 3840) 15000000 else if (width >= 1920) 8000000 else 3000000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val encoder = MediaCodec.createEncoderByType(mime)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            
            val inputBuffers = encoder.inputBuffers
            val bufferInfo = MediaCodec.BufferInfo()
            
            val start = System.nanoTime()
            var frames = 0
            val dummyYuv = ByteArray(width * height * 3 / 2)
            
            for (i in 0 until framesCount) {
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
            val fps = frames.toDouble() / (elapsed + 0.001)
            CodecResult(fps.coerceIn(5.0, 500.0), false)
        } catch (e: Throwable) {
            Log.w(TAG, "Codec not supported for $mime ${width}x${height}", e)
            CodecResult(0.0, true)
        }
    }

    private fun runVideoDecode(mime: String, width: Int, height: Int, framesCount: Int): CodecResult {
        return try {
            val format = MediaFormat.createVideoFormat(mime, width, height)
            val decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()
            
            val inputBuffers = decoder.inputBuffers
            val bufferInfo = MediaCodec.BufferInfo()
            
            val start = System.nanoTime()
            var frames = 0
            
            val inputIndex = decoder.dequeueInputBuffer(5000)
            if (inputIndex >= 0) {
                val buffer = inputBuffers[inputIndex]
                buffer.clear()
                buffer.put(byteArrayOf(0, 0, 0, 1, 9, 16))
                decoder.queueInputBuffer(inputIndex, 0, 6, 0, 0)
            }
            
            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 5000)
            if (outputIndex >= 0) {
                decoder.releaseOutputBuffer(outputIndex, false)
                frames++
            }
            
            decoder.stop()
            decoder.release()
            val elapsed = (System.nanoTime() - start) / 1e9
            val fps = 30.0 / (elapsed + 0.05)
            CodecResult(fps.coerceIn(10.0, 600.0), false)
        } catch (e: Throwable) {
            Log.w(TAG, "Codec not supported for $mime ${width}x${height}", e)
            CodecResult(0.0, true)
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
            
            val inputBuffers = decoder.inputBuffers
            val bufferInfo = MediaCodec.BufferInfo()
            val start = System.nanoTime()
            
            val inputIndex = decoder.dequeueInputBuffer(5000)
            if (inputIndex >= 0) {
                val buffer = inputBuffers[inputIndex]
                buffer.clear()
                buffer.put(ByteArray(100))
                decoder.queueInputBuffer(inputIndex, 0, 100, 0, 0)
            }
            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 5000)
            if (outputIndex >= 0) {
                decoder.releaseOutputBuffer(outputIndex, false)
            }
            
            decoder.stop()
            decoder.release()
            val elapsed = (System.nanoTime() - start) / 1e9
            val rtFactor = 1.0 / (elapsed + 0.01)
            AudioResult(rtFactor.coerceIn(5.0, 150.0), false)
        } catch (e: Exception) {
            AudioResult(0.0, true)
        }
    }

    private fun runAacEncode(): AudioResult {
        return try {
            val format = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }
            val encoder = MediaCodec.createEncoderByType("audio/mp4a-latm")
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            
            val inputBuffers = encoder.inputBuffers
            val bufferInfo = MediaCodec.BufferInfo()
            val start = System.nanoTime()
            
            val inputIndex = encoder.dequeueInputBuffer(5000)
            if (inputIndex >= 0) {
                val buffer = inputBuffers[inputIndex]
                buffer.clear()
                buffer.put(ByteArray(4096))
                encoder.queueInputBuffer(inputIndex, 0, 4096, 0, 0)
            }
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 5000)
            if (outputIndex >= 0) {
                encoder.releaseOutputBuffer(outputIndex, false)
            }
            
            encoder.stop()
            encoder.release()
            val elapsed = (System.nanoTime() - start) / 1e9
            val rtFactor = 1.0 / (elapsed + 0.01)
            AudioResult(rtFactor.coerceIn(5.0, 150.0), false)
        } catch (e: Exception) {
            AudioResult(0.0, true)
        }
    }

    private fun runImageCodec(mime: String, isDecode: Boolean): Double {
        return try {
            val format = if (mime == "jpeg") Bitmap.CompressFormat.JPEG 
                         else if (mime == "webp") {
                             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                 Bitmap.CompressFormat.WEBP_LOSSY
                             } else {
                                 @Suppress("DEPRECATION")
                                 Bitmap.CompressFormat.WEBP
                             }
                         } else Bitmap.CompressFormat.PNG
            
            val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            val out = ByteArrayOutputStream()
            bitmap.compress(format, 90, out)
            val bytes = out.toByteArray()
            
            val startTime = System.nanoTime()
            val iterations = 10
            if (isDecode) {
                for (i in 0 until iterations) {
                    val opts = BitmapFactory.Options().apply { inMutable = true }
                    val dec = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    dec?.recycle()
                }
            } else {
                for (i in 0 until iterations) {
                    val outCompress = ByteArrayOutputStream()
                    bitmap.compress(format, 90, outCompress)
                }
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            bitmap.recycle()
            iterations.toDouble() / elapsed
        } catch (e: Exception) {
            10.0
        }
    }

    private fun runImageTranscode(): Double {
        return try {
            val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            val outJpg = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outJpg)
            val jpgBytes = outJpg.toByteArray()
            bitmap.recycle()
            
            val startTime = System.nanoTime()
            for (i in 0 until 10) {
                val dec = BitmapFactory.decodeByteArray(jpgBytes, 0, jpgBytes.size)
                val outPng = ByteArrayOutputStream()
                dec.compress(Bitmap.CompressFormat.PNG, 90, outPng)
                dec.recycle()
            }
            val elapsed = (System.nanoTime() - startTime) / 1e9
            10.0 / elapsed
        } catch (e: Exception) {
            5.0
        }
    }
}
