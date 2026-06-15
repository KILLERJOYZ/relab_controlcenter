package com.example.relab_tool.model

data class CameraSpec(
    val id: String,
    val facing: String,                  // "BACK (Logical)", "FRONT", "EXTERNAL"
    val cameraRole: String,              // "Main", "Ultra Wide", "Telephoto", "Front", "Multi-Cam"
    val isLogical: Boolean,

    // Resolution block
    val physicalResolutionMp: Float,     // from SENSOR_INFO_PIXEL_ARRAY_SIZE
    val binnedResolutionMp: Float,       // from max JPEG output size
    val binnedResolutionSize: String,    // e.g. "4096x3072"
    val binningFactor: String,           // "none" / "2×2" / "4×4" / "Nona"
    val binningType: String,             // "Full Readout" / "2×2 Binning" etc.
    val highResSupport: Boolean,

    // Sensor info
    val sensorModel: String,             // always "Unknown" (not exposed by API)
    val outputResolutionMp: Float,       // same as binnedResolutionMp
    val outputResolutionSize: String,    // e.g. "4096x3072"
    val aperture: Float?,                // LENS_INFO_AVAILABLE_APERTURES first value
    val focalLengthMm: Float?,           // LENS_INFO_AVAILABLE_FOCAL_LENGTHS first value
    val focalLength35mmEquiv: Float?,    // calculate: focalLengthMm * cropFactor
    val focusModes: List<String>,        // CONTROL_AF_AVAILABLE_MODES mapped to names
    val sensorSizeMm: String,            // "8.19×6.14" from SENSOR_INFO_PHYSICAL_SIZE
    val diagonalMm: Float,               // sqrt(w²+h²) of physical sensor mm
    val pixelSizeUm: Float,              // (sensorWidthMm * 1000) / physicalPixelWidth
    val maxDigitalZoom: Float,           // SCALER_AVAILABLE_MAX_DIGITAL_ZOOM
    val imageFormats: List<String>,      // from getOutputFormats() mapped to names
    val angleOfViewDiag: Float?,         // calculate: 2*atan(diagonalMm/(2*focalLengthMm))
    val angleOfViewHoriz: Float?,
    val cropFactor: Float,               // 43.27 / diagonalMm
    val isoRange: String,                // SENSOR_INFO_SENSITIVITY_RANGE "100-3200"
    val shutterSpeedRange: String,       // SENSOR_INFO_EXPOSURE_TIME_RANGE formatted
    val colorFilter: String,             // SENSOR_INFO_COLOR_FILTER_ARRANGEMENT
    val orientation: Int,                // SENSOR_ORIENTATION
    val hasFlash: Boolean,               // FLASH_INFO_AVAILABLE

    // Capabilities
    val maxPhotoMp: Float,
    val maxVideoResolution: String,      // e.g. "4K@60", "1080p@30"
    val maxVideoSize: String,            // e.g. "3840×2160"
    val hasVideoStabilization: Boolean,  // CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
    val hasOis: Boolean,                 // LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
    val hasAeLock: Boolean,              // CONTROL_AE_LOCK_AVAILABLE
    val hasAwbLock: Boolean,             // CONTROL_AWB_LOCK_AVAILABLE
    val hasRaw: Boolean,                 // check RAW_SENSOR in output formats
    val hasManualSensor: Boolean,        // REQUEST_AVAILABLE_CAPABILITIES contains MANUAL_SENSOR
    val hasManualPostProcessing: Boolean,// MANUAL_POST_PROCESSING
    val hasBurst: Boolean,               // BURST_CAPTURE
    val hasMultiCamera: Boolean,         // LOGICAL_MULTI_CAMERA

    // Camera Modes
    val exposureModes: List<String>,     // CONTROL_AE_AVAILABLE_MODES mapped
    val whiteBalanceModes: List<String>, // CONTROL_AWB_AVAILABLE_MODES mapped
    val sceneModes: List<String>,        // CONTROL_AVAILABLE_SCENE_MODES mapped to names

    // Hardware
    val camera2ApiLevel: String          // INFO_SUPPORTED_HARDWARE_LEVEL
)
