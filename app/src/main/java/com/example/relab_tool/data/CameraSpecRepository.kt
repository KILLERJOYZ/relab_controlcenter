package com.example.relab_tool.data

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
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

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    suspend fun getCameraSpecs(): List<CameraSpec> = withContext(Dispatchers.IO) {
        val specs = mutableListOf<CameraSpec>()
        val physicalIdsToSkip = mutableSetOf<String>()
        
        try {
            val cameraIds = cameraManager.cameraIdList.toMutableList()
            
            // Try to find hidden/AUX camera IDs (common range 0-63)
            for (i in 0..63) {
                val sId = i.toString()
                if (!cameraIds.contains(sId)) {
                    try {
                        cameraManager.getCameraCharacteristics(sId)
                        cameraIds.add(sId)
                    } catch (e: Exception) {}
                }
            }

            // Identify all physical IDs that are part of logical cameras to avoid top-level duplication
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (id in cameraIds) {
                    try {
                        val chars = cameraManager.getCameraCharacteristics(id)
                        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                        val isLogical = capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
                        if (isLogical) {
                            physicalIdsToSkip.addAll(chars.physicalCameraIds)
                        }
                    } catch (e: Exception) {}
                }
            }

            for (id in cameraIds) {
                // Skip if this is a physical camera that will be added as a sub-camera of a logical one
                if (physicalIdsToSkip.contains(id)) continue

                try {
                    val chars = cameraManager.getCameraCharacteristics(id)
                    specs.add(parseCameraChars(id, chars))

                    // Check for physical cameras if it's a logical camera (API 28+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val physicalIds = chars.physicalCameraIds
                        for (physicalId in physicalIds) {
                            try {
                                val physicalChars = cameraManager.getCameraCharacteristics(physicalId)
                                specs.add(parseCameraChars("$id > $physicalId", physicalChars, isPhysicalChild = true))
                            } catch (e: Exception) {}
                        }
                    }
                } catch (e: Exception) {}
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

        return@withContext sortedSpecs.filter { spec ->
            // 1. Resolution sanity check
            if (spec.physicalResolutionMp < 0.5f) return@filter false
            
            // 2. Capabilities check: real cameras must have supported JPEG sizes
            if (spec.binnedResolutionSize == context.getString(R.string.unknown_label) || 
                spec.binnedResolutionSize == "N/A") return@filter false

            // 3. Hardware Signature: physical res + focal length + sensor size
            // Exclude facing from signature to catch ghost IDs that misreport facing
            val focalLen = "%.2f".format(spec.focalLengthMm ?: 0f)
            val res = "%.2f".format(spec.physicalResolutionMp)
            val hardwareSignature = "${res}_${focalLen}_${spec.sensorSizeMm}"
            
            if (seenHardwareSignatures.contains(hardwareSignature)) {
                false
            } else {
                seenHardwareSignatures.add(hardwareSignature)
                true
            }
        }
    }

    private fun parseCameraChars(id: String, chars: CameraCharacteristics, isPhysicalChild: Boolean = false): CameraSpec {
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

        // 1. Physical pixel array (TRUE sensor resolution as per user request)
        // We use SENSOR_INFO_PIXEL_ARRAY_SIZE as the definitive source for physical resolution
        // instead of relying on binned JPEG output sizes.
        val pixelArraySize = try { chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) } catch(e: Exception) { null }
        val physicalResolutionMp = if (pixelArraySize != null) {
            (pixelArraySize.width.toLong() * pixelArraySize.height) / 1_000_000f
        } else 0f

        val streamMap = try { chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) } catch(e: Exception) { null }
        val jpegSizes = streamMap?.getOutputSizes(ImageFormat.JPEG)?.sortedByDescending { it.width.toLong() * it.height } ?: emptyList()
        val maxJpegSize = jpegSizes.firstOrNull()

        // 2. Binned resolution from standard JPEG sizes
        val binnedResolutionMp = if (maxJpegSize != null) {
            (maxJpegSize.width.toLong() * maxJpegSize.height) / 1_000_000f
        } else physicalResolutionMp
        val binnedResolutionSize = maxJpegSize?.let { "${it.width}×${it.height}" } ?: context.getString(R.string.unknown_label)

        val binningRatio = if (binnedResolutionMp > 0) physicalResolutionMp / binnedResolutionMp else 1f
        val binningFactor = when {
            binningRatio >= 8.5f -> "Nona" // 9x
            binningRatio >= 3.5f -> "4×4"  // 16x
            binningRatio >= 1.9f -> "2×2"  // 4x
            else -> "none"
        }
        val binningType = when (binningFactor) {
            "none" -> "Full Readout"
            "2×2" -> "2×2 Binning"
            "4×4" -> "4×4 Binning"
            "Nona" -> "Nona Binning"
            else -> "Unknown"
        }

        // High resolution support check
        val hasHighResConfigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION) != null } catch(e: Exception) { false }
        } else false

        val highResSupport = hasHighResConfigs || (pixelArraySize != null && jpegSizes.any { it.width == pixelArraySize.width && it.height == pixelArraySize.height })

        // Sensor info
        val sensorPhysSize = try { chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) } catch(e: Exception) { null }
        val sensorSizeMm = sensorPhysSize?.let { "%.2f×%.2f".format(Locale.US, it.width, it.height) } ?: context.getString(R.string.unknown_label)
        val diagonalMm = sensorPhysSize?.let { sqrt(it.width * it.width + it.height * it.height) } ?: 0f
        val pixelSizeUm = if (sensorPhysSize != null && pixelArraySize != null) {
            (sensorPhysSize.width * 1000f) / pixelArraySize.width
        } else 0f

        val aperture = try { chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull() } catch(e: Exception) { null }
        val focalLengthMm = try { chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() } catch(e: Exception) { null }
        
        val cropFactor = if (diagonalMm > 0) 43.27f / diagonalMm else 0f
        val focalLength35mmEquiv = if (focalLengthMm != null && cropFactor > 0) focalLengthMm * cropFactor else null

        val afModes = try { chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) } catch(e: Exception) { null } ?: intArrayOf()
        val focusModes = afModes.map { mode ->
            when (mode) {
                0 -> "off"
                1 -> "auto"
                2 -> "macro"
                3 -> "continuous-video"
                4 -> "continuous-picture"
                5 -> "edof"
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
        
        // Video capabilities
        var maxVideoRes = context.getString(R.string.unknown_label)
        var maxVideoSize = context.getString(R.string.unknown_label)
        val videoSizes = streamMap?.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.sortedByDescending { it.width.toLong() * it.height } ?: emptyList()
        val maxVidSize = videoSizes.firstOrNull()
        if (maxVidSize != null) {
            maxVideoSize = "${maxVidSize.width}×${maxVidSize.height}"
            val fps = streamMap?.getOutputFrameRates(android.graphics.SurfaceTexture::class.java, maxVidSize)?.maxOrNull() ?: 30
            val label = when {
                maxVidSize.width >= 3840 -> "4K"
                maxVidSize.width >= 2560 -> "2K"
                maxVidSize.width >= 1920 -> "1080p"
                maxVidSize.width >= 1280 -> "720p"
                else -> "${maxVidSize.height}p"
            }
            maxVideoRes = "$label@$fps"
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
                0 -> "Off"
                1 -> "Auto"
                2 -> "Auto Flash"
                3 -> "Always Flash"
                4 -> "Auto Red-Eye"
                else -> mode.toString()
            }
        }

        val awbModes = try { chars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) } catch(e: Exception) { null } ?: intArrayOf()
        val whiteBalanceModes = awbModes.map { mode ->
            when (mode) {
                0 -> "Off"
                1 -> "Auto"
                2 -> "Incandescent"
                3 -> "Fluorescent"
                4 -> "Warm Fluorescent"
                5 -> "Daylight"
                6 -> "Cloudy"
                7 -> "Twilight"
                8 -> "Shade"
                else -> mode.toString()
            }
        }

        val sceneModesInt = try { chars.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES) } catch(e: Exception) { null } ?: intArrayOf()
        val sceneModes = sceneModesInt.map { mode ->
            when (mode) {
                CameraMetadata.CONTROL_SCENE_MODE_DISABLED -> "Disabled"
                CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY -> "Face Priority"
                CameraMetadata.CONTROL_SCENE_MODE_ACTION -> "Action"
                CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT -> "Portrait"
                CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE -> "Landscape"
                CameraMetadata.CONTROL_SCENE_MODE_NIGHT -> "Night"
                CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT -> "Night Portrait"
                CameraMetadata.CONTROL_SCENE_MODE_THEATRE -> "Theatre"
                CameraMetadata.CONTROL_SCENE_MODE_BEACH -> "Beach"
                CameraMetadata.CONTROL_SCENE_MODE_SNOW -> "Snow"
                CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS -> "Fireworks"
                CameraMetadata.CONTROL_SCENE_MODE_SPORTS -> "Sports"
                CameraMetadata.CONTROL_SCENE_MODE_PARTY -> "Party"
                CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT -> "Candlelight"
                CameraMetadata.CONTROL_SCENE_MODE_BARCODE -> "Barcode"
                CameraMetadata.CONTROL_SCENE_MODE_HIGH_SPEED_VIDEO -> "High Speed Video"
                CameraMetadata.CONTROL_SCENE_MODE_HDR -> "HDR"
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
            isLogical = isLogical && !isPhysicalChild,
            physicalResolutionMp = physicalResolutionMp,
            binnedResolutionMp = binnedResolutionMp,
            binnedResolutionSize = binnedResolutionSize,
            binningFactor = binningFactor,
            binningType = binningType,
            highResSupport = highResSupport,
            sensorModel = context.getString(R.string.unknown_label),
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

    private fun StreamConfigurationMap.getOutputFrameRates(format: Class<*>, size: Size): IntArray {
        return try {
            val duration = getOutputMinFrameDuration(format, size)
            if (duration > 0) intArrayOf((1_000_000_000L / duration).toInt()) else intArrayOf()
        } catch (e: Exception) {
            intArrayOf()
        }
    }
}
