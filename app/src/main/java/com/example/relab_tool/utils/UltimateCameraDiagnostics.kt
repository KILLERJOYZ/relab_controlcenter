package com.example.relab_tool.utils

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import android.graphics.SurfaceTexture
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.io.File
import java.util.Locale
import kotlin.math.sqrt

/**
 * Data structure containing extracted real hardware camera configuration information.
 */
data class CameraHardwareProfile(
    val cameraId: String,
    val trueMegaPixels: Double,
    val width: Int,
    val height: Int,
    val physicalSizeMm: Double,
    val opticalFormat: String,
    val extractionMethod: String,
    val sensorModelName: String?
)

/**
 * Advanced AOSP-level Camera diagnostics module that extracts real physical resolution
 * and optical format size, unlocking OEM-hidden resolution mechanisms.
 */
object UltimateCameraDiagnostics {
    private const val TAG = "UltimateCamDiag"

    private val profileCache = java.util.concurrent.ConcurrentHashMap<String, CameraHardwareProfile>()

    // Constants defining data extraction method identifiers
    const val TIER_1_MAX_RES_MAP = "TIER_1: SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION"
    const val TIER_1_HIGH_RES_OUTPUT = "TIER_1: HIGH_RESOLUTION_OUTPUT_SIZES"
    const val TIER_1_PHYSICAL_ARRAY = "TIER_1: SENSOR_INFO_PIXEL_ARRAY_SIZE"
    const val TIER_1_STANDARD_MAP = "TIER_1: STANDARD_STREAM_MAP"
    
    const val TIER_2_SESSION_FORCE = "TIER_2: ACTIVE_SESSION_CONFIG_SIMULATION"
    const val TIER_3_REFLECTION_OEM = "TIER_3: REFLECTION_OEM_PRIVATE_KEY"
    const val TIER_4_LINUX_CONFIG = "TIER_4: LINUX_CONFIG_FILE_PARSER"
    
    const val DEFAULT_FALLBACK = "TIER_0: DEFAULT_FALLBACK"

    private data class ResolutionCandidate(
        val width: Int,
        val height: Int,
        val megaPixels: Double,
        val method: String,
        val modelName: String? = null
    )

    private data class SensorSpec(val modelName: String, val mp: Int, val w: Int, val h: Int)



    /**
     * Main function to extract real Camera ID specs via a fallback chain.
     */
    fun getTrueCameraSpecs(context: Context, cameraId: String): CameraHardwareProfile {
        profileCache[cameraId]?.let { return it }
        
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val chars = try {
            cameraManager?.getCameraCharacteristics(cameraId) ?: throw IllegalStateException("CameraManager unavailable")
        } catch (e: Exception) {
            // Physical camera IDs on some OEMs (Xiaomi, OPPO, etc.) cannot be
            // queried independently — they require the logical parent to be open.
            Log.w(TAG, "Cannot access characteristics for camera $cameraId, returning fallback", e)
            val fallback = CameraHardwareProfile(
                cameraId = cameraId,
                trueMegaPixels = 0.0,
                width = 0,
                height = 0,
                physicalSizeMm = 0.0,
                opticalFormat = "N/A",
                extractionMethod = DEFAULT_FALLBACK,
                sensorModelName = null
            )
            profileCache[cameraId] = fallback
            return fallback
        }
        
        var selectedCandidate: ResolutionCandidate? = null

        // =====================================================================
        // TIER 1: STANDARD & ADVANCED CAMERA2 API QUERIES (AOSP Standard)
        // =====================================================================
        try {
            val tier1Candidates = mutableListOf<ResolutionCandidate>()

            // Step A: Read raw pixel array SENSOR_INFO_PIXEL_ARRAY_SIZE
            val pixelArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            if (pixelArraySize != null && pixelArraySize.width > 0 && pixelArraySize.height > 0) {
                val mp = (pixelArraySize.width.toLong() * pixelArraySize.height) / 1_000_000.0
                tier1Candidates.add(ResolutionCandidate(pixelArraySize.width, pixelArraySize.height, mp, TIER_1_PHYSICAL_ARRAY))
            }

            // Step A2: Read SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val maxPixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE_MAXIMUM_RESOLUTION)
                if (maxPixelArray != null && maxPixelArray.width > 0 && maxPixelArray.height > 0) {
                    val mp = (maxPixelArray.width.toLong() * maxPixelArray.height) / 1_000_000.0
                    tier1Candidates.add(ResolutionCandidate(maxPixelArray.width, maxPixelArray.height, mp, TIER_1_MAX_RES_MAP))
                }
            }

            // Step B (Android 12+): Check capability and maximize configuration
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val hasUltraHighRes = capabilities?.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR
                ) == true
                
                if (hasUltraHighRes) {
                    val maxResMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                    if (maxResMap != null) {
                        var maxW = 0
                        var maxH = 0
                        for (format in maxResMap.outputFormats) {
                            val sizes = maxResMap.getOutputSizes(format)
                            if (sizes != null) {
                                for (size in sizes) {
                                    if (size.width.toLong() * size.height > maxW.toLong() * maxH) {
                                        maxW = size.width
                                        maxH = size.height
                                    }
                                }
                            }
                        }
                        if (maxW > 0 && maxH > 0) {
                            val mp = (maxW.toLong() * maxH) / 1_000_000.0
                            tier1Candidates.add(ResolutionCandidate(maxW, maxH, mp, TIER_1_MAX_RES_MAP))
                        }
                    }
                }
            }

            // Step C (Android 6+): Scan getHighResolutionOutputSizes() from StreamConfigurationMap
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                if (streamMap != null) {
                    var maxW = 0
                    var maxH = 0
                    for (format in streamMap.outputFormats) {
                        val sizes = streamMap.getHighResolutionOutputSizes(format)
                        if (sizes != null) {
                            for (size in sizes) {
                                if (size.width.toLong() * size.height > maxW.toLong() * maxH) {
                                    maxW = size.width
                                    maxH = size.height
                                }
                            }
                        }
                    }
                    if (maxW > 0 && maxH > 0) {
                        val mp = (maxW.toLong() * maxH) / 1_000_000.0
                        tier1Candidates.add(ResolutionCandidate(maxW, maxH, mp, TIER_1_HIGH_RES_OUTPUT))
                    }
                }
            }

            // Standard fallback at Tier 1
            val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (streamMap != null) {
                var maxW = 0
                var maxH = 0
                for (format in streamMap.outputFormats) {
                    val sizes = streamMap.getOutputSizes(format)
                    if (sizes != null) {
                        for (size in sizes) {
                            if (size.width.toLong() * size.height > maxW.toLong() * maxH) {
                                maxW = size.width
                                maxH = size.height
                            }
                        }
                    }
                }
                if (maxW > 0 && maxH > 0) {
                    val mp = (maxW.toLong() * maxH) / 1_000_000.0
                    tier1Candidates.add(ResolutionCandidate(maxW, maxH, mp, TIER_1_STANDARD_MAP))
                }
            }

            // Select the largest candidate from Tier 1
            val bestTier1 = tier1Candidates.maxByOrNull { it.width.toLong() * it.height }
            if (bestTier1 != null && bestTier1.megaPixels > 2.0) {
                selectedCandidate = bestTier1
            }

            // ── Quad-Bayer / Tetrapixel detection ────────────────────────────
            // Many modern sensors (OV50H, S5KHP2, IMX890, etc.) use Quad-Bayer
            // arrangements where SENSOR_INFO_PIXEL_ARRAY_SIZE reports the raw
            // photosite count (e.g. 200MP) instead of the effective pixel count
            // (e.g. 50MP). Detect this by comparing pixel array vs standard output.
            if (bestTier1 != null && bestTier1.megaPixels > 16.0) {
                val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                if (streamMap != null) {
                    var stdMaxW = 0
                    var stdMaxH = 0
                    for (format in streamMap.outputFormats) {
                        val sizes = streamMap.getOutputSizes(format)
                        if (sizes != null) {
                            for (size in sizes) {
                                if (size.width.toLong() * size.height > stdMaxW.toLong() * stdMaxH) {
                                    stdMaxW = size.width
                                    stdMaxH = size.height
                                }
                            }
                        }
                    }
                    if (stdMaxW > 0 && stdMaxH > 0) {
                        val stdMaxMp = (stdMaxW.toLong() * stdMaxH) / 1_000_000.0
                        val ratio = bestTier1.megaPixels / stdMaxMp
                        Log.d(TAG, "Quad-Bayer check: pixelArray=${bestTier1.megaPixels}MP, stdMax=${stdMaxMp}MP, ratio=$ratio")
                        // ratio ≈ 4 → 2×2 Quad-Bayer (e.g. 50MP sensor showing as 200MP, binned to 12.5MP)
                        // ratio ≈ 9 → Nona-Bayer (e.g. 108MP sensor binned to 12MP)
                        // ratio ≈ 16 → 4×4 (e.g. 200MP binned to 12.5MP)
                        if (ratio >= 3.0) {
                            // This is a Quad-Bayer sensor. The "true effective" resolution
                            // is the standard output × binning factor, but for display
                            // purposes we use the standard max output as the effective
                            // resolution (what the camera actually captures in normal mode).
                            // We'll let the catalog (Tier 5) try to find the real sensor
                            // model and true effective MP. For now, store both.
                            Log.i(TAG, "Detected Quad-Bayer sensor: pixelArray=${bestTier1.megaPixels}MP is inflated. Standard output=${stdMaxMp}MP")
                            // Override with standard output as the effective resolution
                            selectedCandidate = ResolutionCandidate(
                                width = stdMaxW,
                                height = stdMaxH,
                                megaPixels = stdMaxMp,
                                method = bestTier1.method + " (Quad-Bayer corrected)"
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tier 1: Standard Camera2 check failed", e)
        }

        // =====================================================================
        // TIER 2: FORCE HARDWARE MAX RESOLUTION MODE (Active Session Config)
        // =====================================================================
        if (selectedCandidate == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Simulate maximum Capture stream configuration via OutputConfiguration
                val surfaceTexture = SurfaceTexture(10)
                val surface = Surface(surfaceTexture)
                val outputConfig = OutputConfiguration(surface)
                
                // Force hardware to activate maximum resolution
                outputConfig.addSensorPixelModeUsed(CameraMetadata.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION)
                
                val maxResMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                if (maxResMap != null) {
                    var maxW = 0
                    var maxH = 0
                    for (format in maxResMap.outputFormats) {
                        val sizes = maxResMap.getOutputSizes(format)
                        if (sizes != null) {
                            for (size in sizes) {
                                if (size.width.toLong() * size.height > maxW.toLong() * maxH) {
                                    maxW = size.width
                                    maxH = size.height
                                }
                            }
                        }
                    }
                    if (maxW > 0 && maxH > 0) {
                        val mp = (maxW.toLong() * maxH) / 1_000_000.0
                        if (mp > 16.0) {
                            selectedCandidate = ResolutionCandidate(maxW, maxH, mp, TIER_2_SESSION_FORCE)
                        }
                    }
                }
                surface.release()
                surfaceTexture.release()
            } catch (e: Throwable) {
                Log.w(TAG, "Tier 2: OutputConfiguration simulation failed or unsupported", e)
            }
        }

        // =====================================================================
        // TIER 3: UNLOCK VIA REFLECTION (Hunt OEM Private Vendor Keys)
        // =====================================================================
        if (selectedCandidate == null) {
            try {
                // 1. Scan predefined list of OEM private keys
                val customKeyNames = listOf(
                    "com.oplus.custom.sensor.info.pixel.array.size",
                    "com.oplus.custom.sensor.info.real.pixel.array.size",
                    "com.oplus.custom.sensor.info.pixel.array.size.maximum.resolution",
                    "oppo.sensor.info.pixel.array.size",
                    "com.oplus.custom.sensor.info.binning.pixel.array.size",
                    "xiaomi.sensor.info.pixelArraySizeMaxResolution",
                    "xiaomi.sensor.info.pixelArraySize",
                    "com.samsung.android.control.sensor.info.pixel.array.size",
                    "com.samsung.android.sensor.info.pixel.array.size",
                    "com.samsung.android.control.highresolution.pixel.array.size",
                    "com.huawei.device.camera.pixel.array.size",
                    "com.huawei.sensor.info.pixel.array.size"
                )

                for (name in customKeyNames) {
                    // Try Size type
                    val keySize = getCustomKey(name, Size::class.java)
                    if (keySize != null) {
                        val value = try { chars.get(keySize) } catch (e: Exception) { null }
                        if (value != null && value.width > 4000) {
                            val mp = (value.width.toLong() * value.height) / 1_000_000.0
                            selectedCandidate = ResolutionCandidate(value.width, value.height, mp, "$TIER_3_REFLECTION_OEM ($name)")
                            break
                        }
                    }
                    // Try IntArray type
                    val keyIntArray = getCustomKey(name, IntArray::class.java)
                    if (keyIntArray != null) {
                        val value = try { chars.get(keyIntArray) } catch (e: Exception) { null }
                        if (value != null && value.size >= 2 && value[0] > 4000) {
                            val mp = (value[0].toLong() * value[1]) / 1_000_000.0
                            selectedCandidate = ResolutionCandidate(value[0], value[1], mp, "$TIER_3_REFLECTION_OEM ($name)")
                            break
                        }
                    }
                    // Try Rect type
                    val keyRect = getCustomKey(name, android.graphics.Rect::class.java)
                    if (keyRect != null) {
                        val value = try { chars.get(keyRect) } catch (e: Exception) { null }
                        if (value != null && value.width() > 4000) {
                            val w = value.width()
                            val h = value.height()
                            val mp = (w.toLong() * h) / 1_000_000.0
                            selectedCandidate = ResolutionCandidate(w, h, mp, "$TIER_3_REFLECTION_OEM ($name)")
                            break
                        }
                    }
                }

                // 2. If still not found, dynamically scan chars.keys excluding standard "android." keys
                if (selectedCandidate == null) {
                    val keys = chars.keys
                    val targetKeywords = listOf("raw", "vendor", "ultra", "xiaomi", "samsung", "oppo", "fixed_fps", "pixelarray")
                    
                    for (key in keys) {
                        val name = key.name.lowercase()
                        if (name.startsWith("android.")) continue // Skip standard keys to avoid misidentifying binned resolution
                        
                        if (targetKeywords.any { name.contains(it) }) {
                            val value = try { chars.get(key) } catch (e: Exception) { null }
                            
                            if (value is Size) {
                                val w = value.width
                                val h = value.height
                                if (w > 4000) {
                                    val mp = (w.toLong() * h) / 1_000_000.0
                                    selectedCandidate = ResolutionCandidate(w, h, mp, "$TIER_3_REFLECTION_OEM (dynamic: ${key.name})")
                                    break
                                }
                            } else if (value is IntArray && value.size >= 2) {
                                val w = value[0]
                                val h = value[1]
                                if (w > 4000) {
                                    val mp = (w.toLong() * h) / 1_000_000.0
                                    selectedCandidate = ResolutionCandidate(w, h, mp, "$TIER_3_REFLECTION_OEM (dynamic: ${key.name})")
                                    break
                                }
                            } else if (value is android.graphics.Rect) {
                                val w = value.width()
                                val h = value.height()
                                if (w > 4000) {
                                    val mp = (w.toLong() * h) / 1_000_000.0
                                    selectedCandidate = ResolutionCandidate(w, h, mp, "$TIER_3_REFLECTION_OEM (dynamic: ${key.name})")
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Tier 3: Reflection search failed", e)
            }
        }

        // =====================================================================
        // TIER 4: PROBE LINUX SYSTEM CONFIGURATION FILES (/vendor/etc/camera)
        // =====================================================================
        if (selectedCandidate == null) {
            try {
                selectedCandidate = tryLinuxConfigFileScan(cameraId)
            } catch (e: Exception) {
                Log.e(TAG, "Tier 4: Linux config file scan failed", e)
            }
        }



        // =====================================================================
        // FINAL FALLBACK TIER (Default fallback)
        // =====================================================================
        if (selectedCandidate == null) {
            val pixelArraySize = try { chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) } catch (e: Exception) { null }
            if (pixelArraySize != null) {
                val mp = (pixelArraySize.width.toLong() * pixelArraySize.height) / 1_000_000.0
                selectedCandidate = ResolutionCandidate(pixelArraySize.width, pixelArraySize.height, mp, DEFAULT_FALLBACK)
            } else {
                selectedCandidate = ResolutionCandidate(4096, 3072, 12.6, DEFAULT_FALLBACK)
            }
        }

        // =====================================================================
        // EXTRACT PHYSICAL SIZE & SENSOR DIAGONAL
        // =====================================================================
        var physicalSizeMm = 0.0
        var opticalFormat = "N/A"
        
        try {
            val sensorPhysSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            if (sensorPhysSize != null && sensorPhysSize.width > 0f && sensorPhysSize.height > 0f) {
                physicalSizeMm = sqrt(
                    (sensorPhysSize.width * sensorPhysSize.width + sensorPhysSize.height * sensorPhysSize.height).toDouble()
                )
                opticalFormat = convertDiagonalToOpticalFormat(physicalSizeMm)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading sensor physical size", e)
        }

        val profile = CameraHardwareProfile(
            cameraId = cameraId,
            trueMegaPixels = selectedCandidate.megaPixels,
            width = selectedCandidate.width,
            height = selectedCandidate.height,
            physicalSizeMm = physicalSizeMm,
            opticalFormat = opticalFormat,
            extractionMethod = selectedCandidate.method,
            sensorModelName = selectedCandidate.modelName
        )
        profileCache[cameraId] = profile
        return profile
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCustomKey(name: String, type: Class<T>): CameraCharacteristics.Key<T>? {
        try {
            val constructor = CameraCharacteristics.Key::class.java.getDeclaredConstructor(String::class.java, Class::class.java)
            constructor.isAccessible = true
            return constructor.newInstance(name, type) as CameraCharacteristics.Key<T>
        } catch (e: Throwable) {
            try {
                return CameraCharacteristics.Key(name, type)
            } catch (ex: Throwable) {
                return null
            }
        }
    }

    /**
     * Scan Linux/AOSP camera configuration directories to extract hardware information.
     */
    private fun tryLinuxConfigFileScan(cameraId: String): ResolutionCandidate? {
        val searchDirs = listOf(
            "/vendor/etc/camera/",
            "/vendor/etc/",
            "/odm/etc/camera/",
            "/odm/etc/",
            "/system/vendor/etc/camera/",
            "/vendor/camera/",
            "/vendor/etc/camera/config/"
        )
        
        for (dirPath in searchDirs) {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile) {
                        val name = file.name.lowercase()
                        if (name.endsWith(".xml") || name.endsWith(".txt") || 
                            name.endsWith(".json") || name.endsWith(".cfg") || name.endsWith(".ini")) {
                            
                            val isEtcDir = dirPath == "/vendor/etc/" || dirPath == "/odm/etc/"
                            if (isEtcDir) {
                                val isCamFile = name.contains("camera") || name.contains("sensor") || name.contains("lens") || name.contains("media")
                                if (!isCamFile) continue
                            }
                            
                            val candidate = parseSingleConfigFile(file, cameraId)
                            if (candidate != null && candidate.megaPixels > 16.0) {
                                return candidate
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Read and parse XML/text config structure to identify sensor chip or max resolution.
     */
    private fun parseSingleConfigFile(file: File, cameraId: String): ResolutionCandidate? {
        val targetId = if (cameraId.contains(">")) cameraId.substringAfter(">").trim() else cameraId.trim()
        val sensorDb = mapOf(
            // Sony IMX series
            "IMX989" to SensorSpec("Sony IMX989", 50, 8192, 6144),
            "IMX766" to SensorSpec("Sony IMX766", 50, 8192, 6144),
            "IMX890" to SensorSpec("Sony IMX890", 50, 8192, 6144),
            "IMX707" to SensorSpec("Sony IMX707", 50, 8192, 6144),
            "IMX882" to SensorSpec("Sony IMX882", 50, 8192, 6144),
            "IMX858" to SensorSpec("Sony IMX858", 50, 8192, 6144),
            "IMX564" to SensorSpec("Sony IMX564", 50, 8192, 6144),
            "IMX686" to SensorSpec("Sony IMX686", 64, 9248, 6944),
            "IMX586" to SensorSpec("Sony IMX586", 48, 8000, 6000),
            "IMX582" to SensorSpec("Sony IMX582", 48, 8000, 6000),
            "IMX709" to SensorSpec("Sony IMX709", 32, 6528, 4896),
            "IMX615" to SensorSpec("Sony IMX615", 32, 6528, 4896),
            "IMX355" to SensorSpec("Sony IMX355", 8, 3264, 2448),
            "IMX563" to SensorSpec("Sony IMX563", 13, 4208, 3120),
            // Sony LYT series
            "LYT900" to SensorSpec("Sony LYT-900", 50, 8192, 6144),
            "LYT808" to SensorSpec("Sony LYT-808", 50, 8192, 6144),
            "LYT700" to SensorSpec("Sony LYT-700", 50, 8192, 6144),
            "LYT600" to SensorSpec("Sony LYT-600", 50, 8192, 6144),
            "LYT800" to SensorSpec("Sony LYT-800", 50, 8192, 6144),
            // Samsung ISOCELL S5K series
            "S5KHP2" to SensorSpec("Samsung S5KHP2", 200, 16384, 12288),
            "S5KHP3" to SensorSpec("Samsung S5KHP3", 200, 16384, 12288),
            "S5KHP1" to SensorSpec("Samsung S5KHP1", 200, 16384, 12288),
            "S5KHP9" to SensorSpec("Samsung S5KHP9", 200, 16384, 12288),
            "S5KHM2" to SensorSpec("Samsung S5KHM2", 108, 12000, 9000),
            "S5KHM6" to SensorSpec("Samsung S5KHM6", 108, 12000, 9000),
            "S5KHMX" to SensorSpec("Samsung S5KHMX", 108, 12000, 9000),
            "S5KGN2" to SensorSpec("Samsung S5KGN2", 50, 8192, 6144),
            "S5KGN9" to SensorSpec("Samsung S5KGN9", 50, 8192, 6144),
            "S5KGN5" to SensorSpec("Samsung S5KGN5", 50, 8192, 6144),
            "S5KJN1" to SensorSpec("Samsung S5KJN1", 50, 8192, 6144),
            "S5KJN5" to SensorSpec("Samsung S5KJN5", 50, 8192, 6144),
            "S5K3J1" to SensorSpec("Samsung S5K3J1", 50, 8192, 6144),
            "S5KGW1" to SensorSpec("Samsung S5KGW1", 64, 9248, 6944),
            "S5KGW2" to SensorSpec("Samsung S5KGW2", 64, 9248, 6944),
            "S5K3P9" to SensorSpec("Samsung S5K3P9", 16, 4672, 3504),
            "S5K4H7" to SensorSpec("Samsung S5K4H7", 8, 3264, 2448),
            "S5K5E9" to SensorSpec("Samsung S5K5E9", 5, 2592, 1944),
            // Samsung ISOCELL branded (GNK, GD1, GD2, HP1)
            "GNK"    to SensorSpec("Samsung ISOCELL GNK", 50, 8192, 6144),
            "GD1"    to SensorSpec("Samsung ISOCELL GD1", 32, 6528, 4896),
            "GD2"    to SensorSpec("Samsung ISOCELL GD2", 32, 6528, 4896),
            "HP1"    to SensorSpec("Samsung ISOCELL HP1", 200, 16384, 12288),
            // OmniVision OV series
            "OV50H"  to SensorSpec("OmniVision OV50H", 50, 8192, 6144),
            "OV50E"  to SensorSpec("OmniVision OV50E", 50, 8192, 6144),
            "OV50D"  to SensorSpec("OmniVision OV50D", 50, 8192, 6144),
            "OV64B"  to SensorSpec("OmniVision OV64B", 64, 9248, 6944),
            "OV48B"  to SensorSpec("OmniVision OV48B", 48, 8000, 6000),
            "OV13B"  to SensorSpec("OmniVision OV13B", 13, 4208, 3120),
            "OV12D"  to SensorSpec("OmniVision OV12D", 12, 4096, 3072),
            "OV08D"  to SensorSpec("OmniVision OV08D", 8, 3264, 2448),
            "OV02B"  to SensorSpec("OmniVision OV02B", 2, 1600, 1200),
            // GalaxyCore
            "GC02M1" to SensorSpec("GalaxyCore GC02M1", 2, 1600, 1200),
            // Xiaomi branded (config files may reference these)
            "LIGHTFUSION" to SensorSpec("OmniVision OV50H (Light Fusion 900)", 50, 8192, 6144),
            "LIGHT_FUSION" to SensorSpec("OmniVision OV50H (Light Fusion 900)", 50, 8192, 6144)
        )

        try {
            var remainingLinesToCheck = 0
            var candidate: ResolutionCandidate? = null
            
            file.useLines { lines ->
                for (line in lines) {
                    val lowerLine = line.lowercase()
                    
                    val matchesCameraId = lowerLine.contains("camera_id") && lowerLine.contains(targetId) ||
                            lowerLine.contains("cameraid") && lowerLine.contains(targetId) ||
                            lowerLine.contains("sensorid") && lowerLine.contains(targetId) ||
                            lowerLine.contains("sensor_id") && lowerLine.contains(targetId) ||
                            lowerLine.contains("camera_$targetId") ||
                            lowerLine.contains("sensor_$targetId") ||
                            lowerLine.contains("<cameraid>$targetId</cameraid>") ||
                            lowerLine.contains("<sensorid>$targetId</sensorid>")
                    
                    if (matchesCameraId) {
                        remainingLinesToCheck = 20
                    }
                    
                    if (remainingLinesToCheck > 0) {
                        for ((key, spec) in sensorDb) {
                            if (lowerLine.contains(key.lowercase())) {
                                val mp = (spec.w.toLong() * spec.h) / 1_000_000.0
                                candidate = ResolutionCandidate(
                                    width = spec.w,
                                    height = spec.h,
                                    megaPixels = mp,
                                    method = "$TIER_4_LINUX_CONFIG ($key)",
                                    modelName = spec.modelName
                                )
                                return@useLines
                            }
                        }
                        
                        val widthRegex = Regex("""(?:max_width|full_size_w|width|pixelarray_w)[^\d]*(\d{4,5})""")
                        val match = widthRegex.find(lowerLine)
                        if (match != null) {
                            val w = match.groupValues[1].toInt()
                            if (w > 4000) {
                                val h = w * 3 / 4
                                val mp = (w.toLong() * h) / 1_000_000.0
                                candidate = ResolutionCandidate(
                                    width = w,
                                    height = h,
                                    megaPixels = mp,
                                    method = TIER_4_LINUX_CONFIG,
                                    modelName = null
                                )
                                return@useLines
                            }
                        }
                        
                        remainingLinesToCheck--
                    }
                }
            }
            return candidate
        } catch (e: Exception) {
            // Ignore single file exceptions
        }
        return null
    }

    /**
     * Convert physical diagonal length (mm) to optical format inch fraction (Optical Format).
     */
    fun convertDiagonalToOpticalFormat(diagonalMm: Double): String {
        if (diagonalMm <= 0.0) return "N/A"
        
        val baseInch = 16.0
        val computedDenom = baseInch / diagonalMm
        
        if (computedDenom < 1.05) {
            return "1\""
        }
        
        // Direct lookup table for common real-world sensors
        val lookupTable = listOf(
            15.86 to "1\"",
            15.87 to "1\"",
            14.00 to "1/1.12\"",
            14.28 to "1/1.12\"",
            13.33 to "1/1.2\"",
            12.50 to "1/1.28\"",
            12.20 to "1/1.28\"",
            12.00 to "1/1.33\"",
            11.43 to "1/1.4\"",
            11.20 to "1/1.43\"",
            10.67 to "1/1.5\"",
            10.50 to "1/1.52\"",
            10.24 to "1/1.56\"",
            9.50 to "1/1.7\"",
            9.30 to "1/1.72\"",
            9.25 to "1/1.73\"",
            8.89 to "1/1.8\"",
            8.00 to "1/2\"",
            7.66 to "1/2.3\"",
            7.18 to "1/2.5\"",
            7.06 to "1/2.55\"",
            7.05 to "1/2.55\"",
            6.72 to "1/2.7\"",
            6.50 to "1/2.76\"",
            6.00 to "1/3\"",
            5.86 to "1/3.06\"",
            5.70 to "1/2.8\"",
            5.50 to "1/2.9\"",
            5.46 to "1/2.93\"",
            5.00 to "1/3.2\"",
            4.56 to "1/4.4\"",
            4.00 to "1/4\"",
            3.20 to "1/5\"",
            2.67 to "1/6\"",
            2.29 to "1/7\""
        )
        
        val lookupTolerance = 0.15 // mm
        var bestLookupMatch: String? = null
        var minLookupDiff = Double.MAX_VALUE
        
        for ((stdDiag, formatStr) in lookupTable) {
            val diff = Math.abs(diagonalMm - stdDiag)
            if (diff < minLookupDiff && diff <= lookupTolerance) {
                minLookupDiff = diff
                bestLookupMatch = formatStr
            }
        }
        
        if (bestLookupMatch != null) {
            return bestLookupMatch
        }
        
        // Snap to standard denominator
        val standardDenominators = listOf(
            1.12, 1.2, 1.28, 1.3, 1.33, 1.4, 1.43, 1.49, 1.5, 1.52, 1.56, 1.67, 1.7, 1.72, 1.73, 1.8, 
            2.0, 2.25, 2.3, 2.4, 2.5, 2.55, 2.7, 2.76, 2.8, 2.9, 2.93, 3.0, 3.06, 3.1, 3.2, 3.4, 3.6, 
            4.0, 4.4, 5.0, 6.0, 7.0
        )
        
        var closestDenom = computedDenom
        var minDiff = Double.MAX_VALUE
        val snapThreshold = 0.03
        
        for (stdDenom in standardDenominators) {
            val diff = Math.abs(computedDenom - stdDenom)
            if (diff < minDiff) {
                minDiff = diff
                closestDenom = stdDenom
            }
        }
        
        val finalDenom = if (minDiff <= snapThreshold) {
            closestDenom
        } else {
            Math.round(computedDenom * 100.0) / 100.0
        }
        
        return if (finalDenom % 1.0 == 0.0 || Math.abs(finalDenom - Math.round(finalDenom)) < 0.001) {
            "1/${Math.round(finalDenom).toInt()}\""
        } else {
            "1/$finalDenom\""
        }
    }
}
