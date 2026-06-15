package com.example.relab_tool.data

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Size
import com.example.relab_tool.R
import com.example.relab_tool.model.CameraSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.atan
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CameraSpecRepository(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    companion object {
        private var cachedSpecs: List<CameraSpec>? = null
        private var cachedLocale: String? = null

        fun invalidateCache() {
            cachedSpecs = null
            cachedLocale = null
        }
    }

    suspend fun getCameraSpecs(): List<CameraSpec> = withContext(Dispatchers.IO) {
        val currentLocale = context.resources.configuration.locales[0].toString()
        if (cachedLocale != currentLocale) {
            cachedSpecs = null
        }
        cachedSpecs?.let { return@withContext it }
        val specs = mutableListOf<CameraSpec>()
        val physicalIdsToSkip = mutableSetOf<String>()
        val cm = cameraManager ?: return@withContext emptyList()
        
        try {
            val cameraIds = cm.cameraIdList.toMutableList()
            
            // Try to find hidden/AUX camera IDs (common range 0-63)
            for (i in 0..63) {
                val sId = i.toString()
                if (!cameraIds.contains(sId)) {
                    try {
                        cm.getCameraCharacteristics(sId)
                        cameraIds.add(sId)
                    } catch (_: Throwable) {
                        // Xiaomi/OPPO camera HAL may crash with AssertionError
                    }
                }
            }

            // Identify all physical IDs that are part of logical cameras to avoid top-level duplication
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (id in cameraIds) {
                    try {
                        val chars = cm.getCameraCharacteristics(id)
                        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                        val isLogical = capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
                        if (isLogical) {
                            physicalIdsToSkip.addAll(chars.physicalCameraIds)
                        }
                    } catch (_: Throwable) {}
                }
            }

            for (id in cameraIds) {
                // Skip if this is a physical camera that will be added as a sub-camera of a logical one
                if (physicalIdsToSkip.contains(id)) continue

                try {
                    val chars = cm.getCameraCharacteristics(id)
                    specs.add(parseCameraChars(id, chars))

                    // Check for physical cameras if it's a logical camera (API 28+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val physicalIds = chars.physicalCameraIds
                        for (physicalId in physicalIds) {
                            try {
                                val physicalChars = cm.getCameraCharacteristics(physicalId)
                                specs.add(parseCameraChars("$id > $physicalId", physicalChars, isPhysicalChild = true))
                            } catch (_: Throwable) {
                                // Physical camera access can fail on Xiaomi/OPPO
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Camera HAL failures should not crash the app
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Final deduplication and filtering: 
        // Remove cameras with near-zero resolution, redundant identical sensors,
        // and "ghost" IDs that have no output capabilities.
        val seenHardwareSignatures = mutableSetOf<String>()
        
        // Sort to prioritize internal cameras over external/logical for signature "ownership"
        val sortedSpecs = specs.sortedBy { spec ->
            when {
                spec.facing.contains(context.getString(R.string.back_label), true) -> 0
                spec.facing.contains(context.getString(R.string.front_label), true) -> 1
                else -> 2
            }
        }

        val logFile = java.io.File(context.cacheDir, "camera_diag_log.txt")
        val logSb = java.lang.StringBuilder()
        logSb.append("=== Camera spec repo filtering ===\n")
        logSb.append("Specs count before filtering: ${sortedSpecs.size}\n")
        for (s in sortedSpecs) {
            logSb.append("Raw Spec: ID=${s.id}, facing=${s.facing}, physicalResolutionMp=${s.physicalResolutionMp}, binnedResolutionSize=${s.binnedResolutionSize}, sensorModel=${s.sensorModel}\n")
        }

        val filtered = sortedSpecs.filter { spec ->
            val focalLen = "%.2f".format(spec.focalLengthMm ?: 0f)
            val res = "%.2f".format(spec.physicalResolutionMp)
            val hardwareSignature = "${spec.facing}_${res}_${focalLen}_${spec.sensorSizeMm}"
            
            val logMsg = "Filtering ID=${spec.id}, facing=${spec.facing}, res=${res}, binnedSize=${spec.binnedResolutionSize}, signature=${hardwareSignature}"
            android.util.Log.d("CameraSpecRepo", logMsg)
            logSb.append(logMsg).append("\n")
            
            // 0. Skip logical multi-cameras (virtual combos of physical cameras, not real lenses)
            if (spec.isLogical) {
                val filterMsg = "-> Filtered OUT: logical (virtual) camera"
                android.util.Log.d("CameraSpecRepo", filterMsg)
                logSb.append(filterMsg).append("\n")
                return@filter false
            }

            // 1. Resolution sanity check
            if (spec.physicalResolutionMp < 0.5f) {
                val filterMsg = "-> Filtered OUT: resolution < 0.5 MP"
                android.util.Log.d("CameraSpecRepo", filterMsg)
                logSb.append(filterMsg).append("\n")
                return@filter false
            }
            
            // 2. Capabilities check: real cameras must have supported JPEG sizes
            if (spec.binnedResolutionSize == context.getString(R.string.unknown_label) || 
                spec.binnedResolutionSize == "N/A") {
                val filterMsg = "-> Filtered OUT: unknown binned size"
                android.util.Log.d("CameraSpecRepo", filterMsg)
                logSb.append(filterMsg).append("\n")
                return@filter false
            }

            if (seenHardwareSignatures.contains(hardwareSignature)) {
                val filterMsg = "-> Filtered OUT: duplicate signature"
                android.util.Log.d("CameraSpecRepo", filterMsg)
                logSb.append(filterMsg).append("\n")
                false
            } else {
                seenHardwareSignatures.add(hardwareSignature)
                val filterMsg = "-> Filtered IN"
                android.util.Log.d("CameraSpecRepo", filterMsg)
                logSb.append(filterMsg).append("\n")
                true
            }
        }
        
        logSb.append("Final returned specs count: ${filtered.size}\n")
        android.util.Log.i("CameraSpecRepo", "Final returned specs count: ${filtered.size}")
        try {
            logFile.writeText(logSb.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedLocale = context.resources.configuration.locales[0].toString()
        cachedSpecs = filtered
        return@withContext filtered
    }

    private fun parseCameraChars(
        id: String,
        chars: CameraCharacteristics,
        isPhysicalChild: Boolean = false
    ): CameraSpec {
        val facingInt = try { chars.get(CameraCharacteristics.LENS_FACING) } catch(e: Exception) { null }
        val isLogical = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
                ) == true
            } catch(e: Exception) { false }
        } else false

        val facing = when (facingInt) {
            CameraMetadata.LENS_FACING_BACK -> if (isLogical && !isPhysicalChild) context.getString(R.string.back_logical) else context.getString(R.string.back_label)
            CameraMetadata.LENS_FACING_FRONT -> context.getString(R.string.front_label)
            CameraMetadata.LENS_FACING_EXTERNAL -> context.getString(R.string.external_label)
            else -> context.getString(R.string.unknown_label)
        }

        val actualCameraId = if (isPhysicalChild && id.contains(" > ")) id.substringAfter(" > ").trim() else id
        val parentId = if (isPhysicalChild && id.contains(" > ")) id.substringBefore(" > ").trim() else null
        val parentChars = parentId?.let { 
            try { cameraManager?.getCameraCharacteristics(it) } catch(_: Throwable) { null } 
        }

        // 1. Physical pixel array & resolution details from UltimateCameraDiagnostics
        val pixelArraySize = try { chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) } catch(e: Exception) { null }
        val profile = com.example.relab_tool.utils.UltimateCameraDiagnostics.getTrueCameraSpecs(context, actualCameraId)

        val rawStreamMap = try { chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) } catch(e: Exception) { null }
        val streamMap = rawStreamMap ?: parentChars?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val jpegSizes = streamMap?.getOutputSizes(ImageFormat.JPEG)?.sortedByDescending { it.width.toLong() * it.height } ?: emptyList()
        val maxJpegSize = jpegSizes.firstOrNull()

        // 2. Binned resolution from standard JPEG sizes
        val binnedResolutionMp = if (maxJpegSize != null) {
            (maxJpegSize.width.toLong() * maxJpegSize.height) / 1_000_000f
        } else 0f

        val binnedResolutionSize = maxJpegSize?.let { "${it.width}×${it.height}" } ?: context.getString(R.string.unknown_label)

        // Sensor info (needed for focal length computation before override lookup)
        val sensorPhysSize = try { chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) } catch(e: Exception) { null }
        val diagonalMm = sensorPhysSize?.let { sqrt(it.width * it.width + it.height * it.height) } ?: 0f

        val aperture = try { chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull() } catch(e: Exception) { null }
        val focalLengthMm = try { chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() } catch(e: Exception) { null }
        
        val cropFactor = if (diagonalMm > 0) 43.27f / diagonalMm else 0f
        val focalLength35mmEquiv = if (focalLengthMm != null && cropFactor > 0) focalLengthMm * cropFactor else null

        // Camera2 API — single source of truth (device-specific database removed
        // because the bidirectional pattern matching caused false matches on phones
        // not in the database, producing wrong resolution/aperture overrides).
        val physicalResolutionMp: Float = if (profile.trueMegaPixels > 0) {
            profile.trueMegaPixels.toFloat()
        } else if (pixelArraySize != null) {
            (pixelArraySize.width.toLong() * pixelArraySize.height) / 1_000_000f
        } else binnedResolutionMp
        val truePixelWidth: Int = if (profile.width > 0) profile.width else (pixelArraySize?.width ?: 0)

        // Use SENSOR_INFO_ACTIVE_ARRAY_SIZE for pixel size calculation —
        // it excludes optical black pixels and gives the true imaging resolution.
        val activeArraySize = try { chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) } catch(e: Exception) { null }
        val activePixelWidth = activeArraySize?.width() ?: pixelArraySize?.width ?: 0

        val pixelSizeUm = if (sensorPhysSize != null && activePixelWidth > 0) {
            (sensorPhysSize.width * 1000f) / activePixelWidth
        } else 0f

        val sensorSizeMm = sensorPhysSize?.let { 
            "%.2f×%.2f mm (%s)".format(Locale.US, it.width, it.height, profile.opticalFormat) 
        } ?: context.getString(R.string.unknown_label)

        val binningRatio = if (binnedResolutionMp > 0) physicalResolutionMp / binnedResolutionMp else 1f
        val binningFactor = when {
            binningRatio >= 13.5f -> "4×4"  // 16x (e.g. 200MP -> 12.5MP)
            binningRatio >= 7.5f -> "Nona" // 9x (e.g. 108MP -> 12MP)
            binningRatio >= 2.5f -> "2×2"  // 4x (e.g. 48MP -> 12MP / 50MP -> 12.5MP)
            else -> "none"
        }
        val binningType = when (binningFactor) {
            "none" -> context.getString(R.string.binning_full_readout)
            "2×2" -> context.getString(R.string.binning_2x2)
            "4×4" -> context.getString(R.string.binning_4x4)
            "Nona" -> context.getString(R.string.binning_nona)
            else -> context.getString(R.string.unknown)
        }

        // High resolution support check
        val hasHighResConfigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION) != null } catch(e: Exception) { false }
        } else false

        val highResSupport = hasHighResConfigs || profile.extractionMethod.contains("TIER_1") ||
                profile.extractionMethod.contains("TIER_2") ||
                profile.extractionMethod.contains("TIER_3") ||
                profile.extractionMethod.contains("TIER_4")

        // Determine human-readable camera role from focal length or facing
        val cameraRole = when {
            facingInt == CameraMetadata.LENS_FACING_FRONT -> context.getString(R.string.role_front)
            facingInt == CameraMetadata.LENS_FACING_EXTERNAL -> context.getString(R.string.external_label)
            isLogical && !isPhysicalChild -> context.getString(R.string.cam_multicam)
            else -> {
                val equiv = focalLength35mmEquiv ?: 0f
                when {
                    equiv <= 0f    -> context.getString(R.string.role_main) // Fallback
                    equiv < 18f   -> context.getString(R.string.role_ultra_wide)
                    equiv < 30f   -> context.getString(R.string.role_wide)
                    equiv < 52f   -> context.getString(R.string.role_main)
                    equiv < 100f  -> context.getString(R.string.role_telephoto)
                    else          -> context.getString(R.string.role_super_tele)
                }
            }
        }

        val afModes = try { chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) } catch(e: Exception) { null } ?: intArrayOf()
        val focusModes = afModes.map { mode ->
            when (mode) {
                0 -> context.getString(R.string.cam_af_off)
                1 -> context.getString(R.string.cam_af_auto)
                2 -> context.getString(R.string.cam_af_macro)
                3 -> context.getString(R.string.cam_af_continuous_video)
                4 -> context.getString(R.string.cam_af_continuous_picture)
                5 -> context.getString(R.string.cam_af_edof)
                else -> mode.toString()
            }
        }

        val maxDigitalZoom = try { chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) } catch(e: Exception) { null } ?: 1f

        val formats = streamMap?.outputFormats ?: intArrayOf()
        val imageFormats = formats.map { format ->
            when (format) {
                ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
                ImageFormat.JPEG -> "JPEG"
                ImageFormat.YUV_420_888 -> "YUV_420_888"
                ImageFormat.RAW10 -> "RAW10"
                ImageFormat.RAW12 -> "RAW12"
                ImageFormat.HEIC -> "HEIC"
                else -> format.toString()
            }
        }

        val angleOfViewDiag = if (focalLengthMm != null && diagonalMm > 0) {
            (2 * atan(diagonalMm / (2 * focalLengthMm)) * 180 / Math.PI).toFloat()
        } else null
        
        val angleOfViewHoriz = if (focalLengthMm != null && sensorPhysSize != null) {
            (2 * atan(sensorPhysSize.width / (2 * focalLengthMm)) * 180 / Math.PI).toFloat()
        } else null

        val sensitivityRange = try { chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) } catch(e: Exception) { null }
        val isoRange = sensitivityRange?.let { "${it.lower}-${it.upper}" } ?: context.getString(R.string.unknown_label)

        val exposureTimeRange = try { chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) } catch(e: Exception) { null }
        val shutterSpeedRange = if (exposureTimeRange != null) {
            val min = 1.0 / (exposureTimeRange.upper / 1_000_000_000.0)
            val max = exposureTimeRange.lower / 1_000_000_000.0
            "1/%.0f - %.4f s".format(Locale.US, min, max)
        } else context.getString(R.string.unknown_label)

        val colorFilterInt = try { chars.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT) } catch(e: Exception) { null }
        val colorFilter = when (colorFilterInt) {
            0 -> "RGGB"
            1 -> "GRBG"
            2 -> "GBRG"
            3 -> "BGGR"
            4 -> "RGB"
            5 -> "MONO"
            else -> colorFilterInt?.toString() ?: context.getString(R.string.unknown_label)
        }

        val orientation = try { chars.get(CameraCharacteristics.SENSOR_ORIENTATION) } catch(e: Exception) { null } ?: 0
        val hasFlash = try { chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) } catch(e: Exception) { null } ?: false

        // Capabilities
        val maxPhotoMp = physicalResolutionMp // Use physical resolution as requested
        
        // Video capabilities — check standard resolutions to get accurate FPS.
        // Using getOutputMinFrameDuration at the max SurfaceTexture size gives
        // wrong low values (20fps, 24fps) because full-sensor readout is slow.
        // Instead, find the best standard resolution that supports >= 24 fps.
        var maxVideoRes = context.getString(R.string.unknown_label)
        var maxVideoSize = context.getString(R.string.unknown_label)
        if (streamMap != null) {
            val standardVideoSizes = listOf(
                Size(3840, 2160) to "4K",
                Size(2560, 1440) to "2K",
                Size(1920, 1080) to "1080p",
                Size(1280, 720) to "720p"
            )
            val availableSurfaceSizes = try {
                streamMap.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.toSet() ?: emptySet()
            } catch(e: Exception) { emptySet() }

            var bestLabel = ""
            var bestFps = 0
            var bestSize: Size? = null

            for ((targetSize, label) in standardVideoSizes) {
                // Find the closest available size >= the target width
                val matchedSize = availableSurfaceSizes
                    .filter { it.width >= targetSize.width && it.height >= targetSize.height }
                    .minByOrNull { it.width.toLong() * it.height }
                    ?: availableSurfaceSizes
                        .filter { it.width >= targetSize.width - 80 }
                        .minByOrNull { it.width.toLong() * it.height }

                if (matchedSize != null) {
                    val duration = try {
                        streamMap.getOutputMinFrameDuration(android.graphics.SurfaceTexture::class.java, matchedSize)
                    } catch(e: Exception) { 0L }
                    val fps = if (duration > 0) (1_000_000_000L / duration).toInt() else 30
                    if (fps >= 24 && bestLabel.isEmpty()) {
                        bestLabel = label
                        bestFps = fps
                        bestSize = matchedSize
                    }
                }
            }

            // Fallback: if no standard resolution matched >= 24fps, use the largest available
            if (bestSize == null) {
                val fallbackSize = availableSurfaceSizes.maxByOrNull { it.width.toLong() * it.height }
                if (fallbackSize != null) {
                    bestSize = fallbackSize
                    bestFps = 30
                    bestLabel = when {
                        fallbackSize.width >= 3840 -> "4K"
                        fallbackSize.width >= 2560 -> "2K"
                        fallbackSize.width >= 1920 -> "1080p"
                        fallbackSize.width >= 1280 -> "720p"
                        else -> "${fallbackSize.height}p"
                    }
                }
            }

            if (bestSize != null) {
                maxVideoSize = "${bestSize.width}×${bestSize.height}"
                maxVideoRes = "$bestLabel@$bestFps"
            }
        }

        val videoStabModes = try { chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) } catch(e: Exception) { null } ?: intArrayOf()
        val hasVideoStabilization = videoStabModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)

        val oisModes = try { chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) } catch(e: Exception) { null } ?: intArrayOf()
        val hasOis = oisModes.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)

        val hasAeLock = try { chars.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) } catch(e: Exception) { null } ?: false
        val hasAwbLock = try { chars.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE) } catch(e: Exception) { null } ?: false
        val hasRaw = formats.contains(ImageFormat.RAW_SENSOR)

        val capabilities = try { chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) } catch(e: Exception) { null } ?: intArrayOf()
        val hasManualSensor = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val hasManualPostProcessing = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING)
        val hasBurst = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE)
        val hasMultiCamera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
        } else false

        // Camera Modes
        val aeModes = try { chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES) } catch(e: Exception) { null } ?: intArrayOf()
        val exposureModes = aeModes.map { mode ->
            when (mode) {
                0 -> context.getString(R.string.cam_ae_off)
                1 -> context.getString(R.string.cam_ae_auto)
                2 -> context.getString(R.string.cam_ae_auto_flash)
                3 -> context.getString(R.string.cam_ae_always_flash)
                4 -> context.getString(R.string.cam_ae_auto_redeye)
                else -> mode.toString()
            }
        }

        val awbModes = try { chars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) } catch(e: Exception) { null } ?: intArrayOf()
        val whiteBalanceModes = awbModes.map { mode ->
            when (mode) {
                0 -> context.getString(R.string.cam_awb_off)
                1 -> context.getString(R.string.cam_awb_auto)
                2 -> context.getString(R.string.cam_awb_incandescent)
                3 -> context.getString(R.string.cam_awb_fluorescent)
                4 -> context.getString(R.string.cam_awb_warm_fluorescent)
                5 -> context.getString(R.string.cam_awb_daylight)
                6 -> context.getString(R.string.cam_awb_cloudy)
                7 -> context.getString(R.string.cam_awb_twilight)
                8 -> context.getString(R.string.cam_awb_shade)
                else -> mode.toString()
            }
        }

        val sceneModesInt = try { chars.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES) } catch(e: Exception) { null } ?: intArrayOf()
        val sceneModes = sceneModesInt.map { mode ->
            when (mode) {
                CameraMetadata.CONTROL_SCENE_MODE_DISABLED -> context.getString(R.string.cam_scene_disabled)
                CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY -> context.getString(R.string.cam_scene_face_priority)
                CameraMetadata.CONTROL_SCENE_MODE_ACTION -> context.getString(R.string.cam_scene_action)
                CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT -> context.getString(R.string.cam_scene_portrait)
                CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE -> context.getString(R.string.cam_scene_landscape)
                CameraMetadata.CONTROL_SCENE_MODE_NIGHT -> context.getString(R.string.cam_scene_night)
                CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT -> context.getString(R.string.cam_scene_night_portrait)
                CameraMetadata.CONTROL_SCENE_MODE_THEATRE -> context.getString(R.string.cam_scene_theatre)
                CameraMetadata.CONTROL_SCENE_MODE_BEACH -> context.getString(R.string.cam_scene_beach)
                CameraMetadata.CONTROL_SCENE_MODE_SNOW -> context.getString(R.string.cam_scene_snow)
                CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS -> context.getString(R.string.cam_scene_fireworks)
                CameraMetadata.CONTROL_SCENE_MODE_SPORTS -> context.getString(R.string.cam_scene_sports)
                CameraMetadata.CONTROL_SCENE_MODE_PARTY -> context.getString(R.string.cam_scene_party)
                CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT -> context.getString(R.string.cam_scene_candlelight)
                CameraMetadata.CONTROL_SCENE_MODE_BARCODE -> context.getString(R.string.cam_scene_barcode)
                CameraMetadata.CONTROL_SCENE_MODE_HIGH_SPEED_VIDEO -> context.getString(R.string.cam_scene_high_speed_video)
                CameraMetadata.CONTROL_SCENE_MODE_HDR -> context.getString(R.string.cam_scene_hdr)
                else -> mode.toString()
            }
        }

        // Hardware
        val hwLevelInt = try { chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) } catch(e: Exception) { null }
        val camera2ApiLevel = when (hwLevelInt) {
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY(0)"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED(1)"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL(2)"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3(3)"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL(4)"
            else -> hwLevelInt?.toString() ?: context.getString(R.string.unknown_label)
        }

        return CameraSpec(
            id = id,
            facing = facing,
            cameraRole = cameraRole,
            isLogical = isLogical && !isPhysicalChild,
            physicalResolutionMp = physicalResolutionMp,
            binnedResolutionMp = binnedResolutionMp,
            binnedResolutionSize = binnedResolutionSize,
            binningFactor = binningFactor,
            binningType = binningType,
            highResSupport = highResSupport,
            sensorModel = profile.sensorModelName ?: context.getString(R.string.unknown_label),
            outputResolutionMp = binnedResolutionMp,
            outputResolutionSize = binnedResolutionSize,
            aperture = aperture,
            focalLengthMm = focalLengthMm,
            focalLength35mmEquiv = focalLength35mmEquiv,
            focusModes = focusModes,
            sensorSizeMm = sensorSizeMm,
            diagonalMm = diagonalMm,
            pixelSizeUm = pixelSizeUm,
            maxDigitalZoom = maxDigitalZoom,
            imageFormats = imageFormats,
            angleOfViewDiag = angleOfViewDiag,
            angleOfViewHoriz = angleOfViewHoriz,
            cropFactor = cropFactor,
            isoRange = isoRange,
            shutterSpeedRange = shutterSpeedRange,
            colorFilter = colorFilter,
            orientation = orientation,
            hasFlash = hasFlash,
            maxPhotoMp = maxPhotoMp,
            maxVideoResolution = maxVideoRes,
            maxVideoSize = maxVideoSize,
            hasVideoStabilization = hasVideoStabilization,
            hasOis = hasOis,
            hasAeLock = hasAeLock,
            hasAwbLock = hasAwbLock,
            hasRaw = hasRaw,
            hasManualSensor = hasManualSensor,
            hasManualPostProcessing = hasManualPostProcessing,
            hasBurst = hasBurst,
            hasMultiCamera = hasMultiCamera,
            exposureModes = exposureModes,
            whiteBalanceModes = whiteBalanceModes,
            sceneModes = sceneModes,
            camera2ApiLevel = camera2ApiLevel
        )
    }
}
