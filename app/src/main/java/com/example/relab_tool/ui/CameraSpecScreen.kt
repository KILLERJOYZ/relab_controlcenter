package com.example.relab_tool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.relab_tool.R
import com.example.relab_tool.model.CameraSpec
import java.util.*
import com.example.relab_tool.ui.InfoGroupCard
import com.example.relab_tool.ui.InfoRow
import com.example.relab_tool.ui.DashboardStatusCard

// Modern Professional Color Palette
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSpecScreen(viewModel: CameraSpecViewModel) {
    val cameraSpecs by viewModel.cameraSpecs.collectAsState()
    val selectedIdx by viewModel.selectedCameraIndex.collectAsState()
    
    CameraSpecContent(
        cameraSpecs = cameraSpecs,
        selectedIdx = selectedIdx,
        onCameraSelected = { viewModel.selectCamera(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSpecContent(
    cameraSpecs: List<CameraSpec>,
    selectedIdx: Int,
    onCameraSelected: (Int) -> Unit
) {
    if (cameraSpecs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))
            
            CameraSelector(cameraSpecs, selectedIdx, onCameraSelected)
            
            val selectedCamera = cameraSpecs.getOrNull(selectedIdx)
            if (selectedCamera != null) {
                CameraDetails(selectedCamera)
            }
        }
    }
}

@Composable
fun CameraSelector(specs: List<CameraSpec>, selectedIdx: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(specs) { index, spec ->
            val isSelected = index == selectedIdx
            
            // System color palette for selected, muted surface for others
            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            val accentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            }

            Card(
                modifier = Modifier
                    .width(220.dp)
                    .height(130.dp)
                    .clickable { onSelect(index) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 0.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(
                                text = "%.1f MP - %s".format(Locale.US, spec.physicalResolutionMp, spec.facing.substringBefore(" (")),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = spec.binnedResolutionSize,
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = spec.focalLength35mmEquiv?.let { stringResource(id = R.string.focal_length_format, it) } ?: stringResource(id = R.string.na),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                    
                    if (isSelected) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).size(24.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraDetails(camera: CameraSpec) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { MainHighlights(camera) }
        item { CapabilitiesSection(camera) }
        item { DetailedSpecSection(camera) }
        item { CameraModesSection(camera) }
        item { Camera2ApiLevelCard(camera.camera2ApiLevel) }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun MainHighlights(camera: CameraSpec) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DashboardStatusCard(
            modifier = Modifier.weight(1f).height(130.dp),
            title = stringResource(id = R.string.aperture),
            icon = Icons.Outlined.BrightnessLow,
            value = camera.aperture?.let { "f/$it" } ?: stringResource(id = R.string.na),
            subtext = stringResource(R.string.cam_lens_opening),
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        DashboardStatusCard(
            modifier = Modifier.weight(1f).height(130.dp),
            title = stringResource(id = R.string.focal_length),
            icon = Icons.Outlined.Straighten,
            value = camera.focalLength35mmEquiv?.let { stringResource(id = R.string.focal_length_format, it) } ?: stringResource(id = R.string.na),
            subtext = camera.focalLengthMm?.let { "Actual: %.2f mm".format(Locale.US, it) } ?: stringResource(id = R.string.focal_length_35mm),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        DashboardStatusCard(
            modifier = Modifier.weight(1f).height(130.dp),
            title = stringResource(R.string.cam_pixel_size),
            icon = Icons.Outlined.BlurOn,
            value = "%.2f µm".format(Locale.US, camera.pixelSizeUm),
            subtext = stringResource(R.string.cam_sensor_pixel),
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CapabilitiesSection(camera: CameraSpec) {
    Column {
        SectionHeader(stringResource(R.string.cam_core_capabilities), Icons.Outlined.AutoAwesome)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStatusCard(
                modifier = Modifier.weight(1f).height(130.dp),
                title = stringResource(R.string.cam_resolution),
                icon = Icons.Outlined.SensorWindow,
                value = "%.1f MP".format(Locale.US, camera.physicalResolutionMp),
                subtext = stringResource(R.string.cam_resolution_physical),
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            DashboardStatusCard(
                modifier = Modifier.weight(1f).height(130.dp),
                title = stringResource(R.string.cam_video_quality),
                icon = Icons.Outlined.Videocam,
                value = camera.maxVideoResolution,
                subtext = stringResource(R.string.cam_video_max),
                containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        CapabilityBadgeGrid(camera)
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CapabilityBadgeGrid(camera: CameraSpec) {
    val isWideScreen = LocalConfiguration.current.screenWidthDp > 600
    val cols = if (isWideScreen) 5 else 3
    val items = listOf(
        Triple(stringResource(R.string.cam_eis),      Icons.Outlined.VideoSettings,     camera.hasVideoStabilization),
        Triple(stringResource(R.string.ois),          Icons.Outlined.CenterFocusStrong,  camera.hasOis),
        Triple(stringResource(R.string.ae_lock),      Icons.Outlined.Lock,               camera.hasAeLock),
        Triple(stringResource(R.string.awb_lock),     Icons.Outlined.WbAuto,             camera.hasAwbLock),
        Triple(stringResource(R.string.cam_raw),      Icons.Outlined.RawOn,              camera.hasRaw),
        Triple(stringResource(R.string.cam_manual),   Icons.Outlined.Tune,               camera.hasManualSensor),
        Triple(stringResource(R.string.cam_ai_post),  Icons.Outlined.AutoFixHigh,        camera.hasManualPostProcessing),
        Triple(stringResource(R.string.cam_burst),    Icons.Outlined.BurstMode,          camera.hasBurst),
        Triple(stringResource(R.string.cam_multicam), Icons.Outlined.Cameraswitch,       camera.hasMultiCamera)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(cols).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (label, icon, supported) ->
                    DashboardFeatureCard(
                        modifier = Modifier.weight(1f),
                        title = label,
                        icon = icon,
                        isSupported = supported
                    )
                }
                repeat(cols - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun DetailedSpecSection(camera: CameraSpec) {
    InfoGroupCard(
        title = stringResource(R.string.cam_technical_params),
        icon = Icons.Outlined.Analytics,
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        InfoRow(stringResource(R.string.cam_phys_resolution),   "%.1f MP".format(Locale.US, camera.physicalResolutionMp))
        InfoRow(stringResource(R.string.cam_binned_resolution), "%.1f MP".format(Locale.US, camera.binnedResolutionMp))
        InfoRow(stringResource(R.string.cam_binning_factor),    camera.binningFactor)
        InfoRow(stringResource(R.string.aperture),              camera.aperture?.let { "f/$it" } ?: stringResource(R.string.na))
        InfoRow(stringResource(R.string.cam_focal_std),         camera.focalLength35mmEquiv?.let { "%.1f mm".format(Locale.US, it) } ?: stringResource(R.string.na))
        InfoRow(stringResource(R.string.cam_focal_actual),      camera.focalLengthMm?.let { "%.2f mm".format(Locale.US, it) } ?: stringResource(R.string.na))
        InfoRow(stringResource(R.string.cam_sensor_size),       camera.sensorSizeMm)
        InfoRow(stringResource(R.string.cam_pixel_size),        "~%.2f µm".format(Locale.US, camera.pixelSizeUm))
        InfoRow(stringResource(R.string.cam_iso_range),         camera.isoRange)
        InfoRow(stringResource(R.string.cam_shutter_speed),     camera.shutterSpeedRange)
        InfoRow(stringResource(R.string.cam_angle_of_view),     "%.1f°".format(Locale.US, camera.angleOfViewDiag ?: 0f))
        InfoRow(stringResource(R.string.cam_crop_factor),       "%.1fx".format(Locale.US, camera.cropFactor))
        InfoRow(stringResource(R.string.cam_color_filter),      camera.colorFilter)
        InfoRow(stringResource(R.string.cam_orientation),       "${camera.orientation}°")
        InfoRow(stringResource(R.string.cam_flash_support),     if (camera.hasFlash) stringResource(R.string.cam_flash_available) else stringResource(R.string.cam_flash_none))
    }
}

@Composable
fun CameraModesSection(camera: CameraSpec) {
    Column {
        SectionHeader(stringResource(R.string.cam_supported_modes), Icons.Outlined.Camera)
        Spacer(modifier = Modifier.height(16.dp))
        InfoGroupCard(
            stringResource(R.string.cam_photography_exposure),
            Icons.Outlined.Exposure,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            ModeGroup(stringResource(R.string.cam_mode_exposure),      camera.exposureModes,     Icons.Outlined.Exposure)
            Spacer(modifier = Modifier.height(24.dp))
            ModeGroup(stringResource(R.string.cam_mode_white_balance), camera.whiteBalanceModes, Icons.Outlined.WbSunny)
            Spacer(modifier = Modifier.height(24.dp))
            ModeGroup(stringResource(R.string.cam_mode_scene),         camera.sceneModes,        Icons.Outlined.Landscape)
            Spacer(modifier = Modifier.height(24.dp))
            ModeGroup(stringResource(R.string.cam_mode_focus),         camera.focusModes,        Icons.Outlined.FilterCenterFocus)
        }
    }
}

@Composable
fun ModeGroup(title: String, modes: List<String>, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        @OptIn(ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modes.forEach { mode ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = mode,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun Camera2ApiLevelCard(level: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseSurface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Terminal, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.inverseOnSurface)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.cam_api_implementation), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.cam_camera2_level), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.inverseOnSurface)
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    level.uppercase(),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraSpecScreenPreview() {
    val mockCamera = CameraSpec(
        id = "0",
        facing = "BACK (Logical)",
        isLogical = true,
        physicalResolutionMp = 108.0f,
        binnedResolutionMp = 12.0f,
        binnedResolutionSize = "4000x3000",
        binningFactor = "3×3",
        binningType = "Nona-Bayer",
        highResSupport = true,
        sensorModel = "Unknown",
        outputResolutionMp = 12.0f,
        outputResolutionSize = "4000x3000",
        aperture = 1.89f,
        focalLengthMm = 6.06f,
        focalLength35mmEquiv = 23.0f,
        focusModes = listOf("auto", "continuous-picture", "macro"),
        sensorSizeMm = "9.50×7.10",
        diagonalMm = 11.86f,
        pixelSizeUm = 0.8f,
        maxDigitalZoom = 100.0f,
        imageFormats = listOf("JPEG", "RAW_SENSOR", "HEIC"),
        angleOfViewDiag = 84.0f,
        angleOfViewHoriz = 74.0f,
        cropFactor = 3.6f,
        isoRange = "50-12800",
        shutterSpeedRange = "1/12000 - 30s",
        colorFilter = "RGGB",
        orientation = 90,
        hasFlash = true,
        maxPhotoMp = 108.0f,
        maxVideoResolution = "8K@24",
        maxVideoSize = "7680×4320",
        hasVideoStabilization = true,
        hasOis = true,
        hasAeLock = true,
        hasAwbLock = true,
        hasRaw = true,
        hasManualSensor = true,
        hasManualPostProcessing = true,
        hasBurst = true,
        hasMultiCamera = true,
        exposureModes = listOf("Auto", "Off", "Flash"),
        whiteBalanceModes = listOf("Auto", "Cloudy", "Shade", "Fluorescent"),
        sceneModes = listOf("Portrait", "Night", "Action", "Landscape"),
        camera2ApiLevel = "LEVEL_3(3)"
    )
    
    CameraSpecContent(
        cameraSpecs = listOf(mockCamera),
        selectedIdx = 0,
        onCameraSelected = {}
    )
}
