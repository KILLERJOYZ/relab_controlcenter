package com.example.relab_tool.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.relab_tool.R
import com.example.relab_tool.model.GnssSatellite
import com.example.relab_tool.model.SatelliteCompassUiState
import java.util.Locale

// ── Shared constellation helper ───────────────────────────────────────────────
// Single source of truth — used by both InfoPanelWidget and SatelliteListPanel.
// Returns a pair of (localizedName, brandColor).
@Composable
private fun constellationInfo(type: Int): Pair<String, Color> = when (type) {
    1 -> stringResource(R.string.constellation_gps)     to Color(0xFF3B6D11)
    2 -> stringResource(R.string.constellation_sbas)    to Color(0xFF888780)
    3 -> stringResource(R.string.constellation_glonass) to Color(0xFF185FA5)
    4 -> stringResource(R.string.constellation_qzss)    to Color(0xFF888780)
    5 -> stringResource(R.string.constellation_beidou)  to Color(0xFF993C1D)
    6 -> stringResource(R.string.constellation_galileo) to Color(0xFF534AB7)
    7 -> stringResource(R.string.constellation_irnss)   to Color(0xFF888780)
    else -> stringResource(R.string.constellation_other) to Color(0xFF888780)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatelliteCompassScreen(
    onBack: () -> Unit,
    viewModel: SatelliteCompassViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastLocation by viewModel.lastLocation.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val compassManager = viewModel.compassManager
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    DisposableEffect(lifecycleOwner, lastLocation) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.startTracking()
                lastLocation?.let { compassManager.start(it) }
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.stopTracking()
                compassManager.stop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.startTracking()
            lastLocation?.let { compassManager.start(it) }
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopTracking()
            compassManager.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.satellite_compass_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (isWideScreen) {
            // ── TABLET: Side-by-side ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left pane: Compass + heading chip + countdown
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CompassWidget(
                        deviceHeading = uiState.deviceHeading,
                        targetSatellite = uiState.targetSatellite,
                        modifier = Modifier.widthIn(max = 480.dp)
                    )
                    HeadingChip(heading = uiState.deviceHeading)
                    CountdownWidget(nextUpdateSec = uiState.nextUpdateSec)
                }

                // Right pane: Info + satellite list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoPanelWidget(uiState = uiState)
                    SatelliteListPanel(satellites = uiState.allSatellites)
                }
            }
        } else {
            // ── PHONE: Vertical stack ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CompassWidget(
                    deviceHeading = uiState.deviceHeading,
                    targetSatellite = uiState.targetSatellite
                )
                CountdownWidget(nextUpdateSec = uiState.nextUpdateSec)
                InfoPanelWidget(uiState = uiState)
                SatelliteListPanel(satellites = uiState.allSatellites)
            }
        }
    }
}

// ── Heading chip (tablet left pane only) ─────────────────────────────────────
@Composable
private fun HeadingChip(heading: Float) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = String.format(Locale.US, "%.1f°", heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.device_heading_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Compass canvas ────────────────────────────────────────────────────────────
@Composable
fun CompassWidget(
    deviceHeading: Float,
    targetSatellite: GnssSatellite?,
    modifier: Modifier = Modifier
) {
    val animatedHeading by animateFloatAsState(
        targetValue = deviceHeading,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animated_heading"
    )

    val onSurface = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()

    val labels = remember {
        listOf(
            0 to "N", 45 to "NE", 90 to "E", 135 to "SE",
            180 to "S", 225 to "SW", 270 to "W", 315 to "NW"
        )
    }

    val measuredLabels = remember(onSurface, textMeasurer) {
        labels.map { (angle, text) ->
            val isCardinal = angle % 90 == 0
            val isNorth = angle == 0
            val color = if (isNorth) Color(0xFFE24B4A) else if (isCardinal) onSurface else onSurface.copy(alpha = 0.5f)
            val fontSize = if (isCardinal) 16.sp else 11.sp
            val fontWeight = if (isCardinal) FontWeight.Bold else FontWeight.Normal
            angle to textMeasurer.measure(
                text = text,
                style = TextStyle(color = color, fontSize = fontSize, fontWeight = fontWeight)
            )
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val dashedStroke = remember(density) {
        with(density) {
            Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
    }
    val satDashedEffect = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    val trianglePath = remember { Path() }

    // Satellite color is derived from type — we resolve it here outside Canvas to avoid
    // calling @Composable inside a lambda.
    val satColor = targetSatellite?.let { constellationInfo(it.constellationType).second }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (maxWidth < 40.dp || maxHeight < 40.dp) return@BoxWithConstraints
        val widgetSize = minOf(maxWidth, maxHeight)
        Box(modifier = Modifier.size(widgetSize), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.minDimension / 2f
                val ringRadius = radius * 0.85f
                val innerRadius = (ringRadius - 16.dp.toPx()).coerceAtLeast(0f)

                rotate(degrees = -animatedHeading, pivot = Offset(cx, cy)) {
                    // Ticks
                    for (angle in 0 until 360 step 5) {
                        val angleRad = Math.toRadians(angle.toDouble())
                        val isCardinal = angle % 90 == 0
                        val isMajor = angle % 30 == 0
                        val tickLength = if (isCardinal) 12.dp.toPx() else if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                        val startR = ringRadius - tickLength
                        drawLine(
                            color = onSurface.copy(alpha = if (isCardinal) 0.8f else if (isMajor) 0.5f else 0.25f),
                            start = Offset(cx + startR * Math.sin(angleRad).toFloat(), cy - startR * Math.cos(angleRad).toFloat()),
                            end   = Offset(cx + ringRadius * Math.sin(angleRad).toFloat(), cy - ringRadius * Math.cos(angleRad).toFloat()),
                            strokeWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
                        )
                    }

                    // Cardinal labels
                    measuredLabels.forEach { (angle, textLayout) ->
                        val angleRad = Math.toRadians(angle.toDouble())
                        val textRadius = (innerRadius - 12.dp.toPx()).coerceAtLeast(0f)
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                cx + textRadius * Math.sin(angleRad).toFloat() - textLayout.size.width / 2f,
                                cy - textRadius * Math.cos(angleRad).toFloat() - textLayout.size.height / 2f
                            )
                        )
                    }

                    // Elevation rings at 0°, 30°, 60°
                    listOf(0f, 30f, 60f).forEach { elev ->
                        drawCircle(
                            color = onSurface.copy(alpha = 0.2f),
                            radius = (innerRadius * (90f - elev) / 90f).coerceAtLeast(0f),
                            center = Offset(cx, cy),
                            style = dashedStroke
                        )
                    }

                    // Target satellite dot
                    if (targetSatellite != null && satColor != null) {
                        val satRadius = (innerRadius * (90f - targetSatellite.elevationDegrees) / 90f).coerceAtLeast(0f)
                        val satAngleRad = Math.toRadians(targetSatellite.azimuthDegrees.toDouble())
                        val satX = cx + satRadius * Math.sin(satAngleRad).toFloat()
                        val satY = cy - satRadius * Math.cos(satAngleRad).toFloat()

                        drawLine(
                            color = onSurface.copy(alpha = 0.3f),
                            start = Offset(cx, cy), end = Offset(satX, satY),
                            strokeWidth = 1.5.dp.toPx(), pathEffect = satDashedEffect
                        )
                        val pulseRadius = (10.dp.toPx() + (targetSatellite.cn0DbHz / 50f) * 8.dp.toPx()).coerceAtLeast(0f)
                        drawCircle(color = satColor.copy(alpha = 0.25f), radius = pulseRadius, center = Offset(satX, satY))
                        drawCircle(color = satColor, radius = 10.dp.toPx(), center = Offset(satX, satY))
                    }
                }

                // Fixed center dot
                drawCircle(color = onSurface, radius = 6.dp.toPx(), center = Offset(cx, cy))

                // Fixed North triangle
                trianglePath.reset()
                val topY = cy - ringRadius - 14.dp.toPx()
                val baseY = cy - ringRadius - 4.dp.toPx()
                trianglePath.moveTo(cx, topY)
                trianglePath.lineTo(cx - 8.dp.toPx(), baseY)
                trianglePath.lineTo(cx + 8.dp.toPx(), baseY)
                trianglePath.close()
                drawPath(path = trianglePath, color = Color(0xFFE24B4A))
            }
        }
    }
}

// ── Countdown chip ────────────────────────────────────────────────────────────
@Composable
fun CountdownWidget(nextUpdateSec: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = stringResource(R.string.cd_countdown),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.countdown_refresh_format, nextUpdateSec / 60, nextUpdateSec % 60),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

// ── Satellite lock info panel ─────────────────────────────────────────────────
@Composable
fun InfoPanelWidget(uiState: SatelliteCompassUiState) {
    val sat = uiState.targetSatellite
    InfoGroupCard(
        title = stringResource(R.string.satellite_lock_info),
        icon = Icons.Default.CompassCalibration,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        if (sat != null) {
            val (constName, _) = constellationInfo(sat.constellationType)
            val satLabel = "$constName #${sat.svid}"

            InfoRow(label = stringResource(R.string.satellite_label), value = satLabel)

            // C/N0 with signal strength bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.cn0_signal_ratio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.65f)
                    )
                    Text(
                        // Use string resource for unit to support localization
                        text = String.format(Locale.US, "%.1f %s", sat.cn0DbHz, stringResource(R.string.unit_db_hz)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = LocalContentColor.current
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (sat.cn0DbHz / 50f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }

            InfoRow(label = stringResource(R.string.azimuth_label),        value = String.format(Locale.US, "%.1f°", sat.azimuthDegrees))
            InfoRow(label = stringResource(R.string.elevation_label),      value = String.format(Locale.US, "%.1f°", sat.elevationDegrees))
            InfoRow(label = stringResource(R.string.relative_bearing_label), value = String.format(Locale.US, "%.1f°", uiState.relativeBearing))

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = LocalContentColor.current.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Turn instruction
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.calibration_guide),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.turnInstruction?.let { ti ->
                        val dir = stringResource(if (ti.direction == com.example.relab_tool.model.TurnDirection.RIGHT) R.string.turn_right else R.string.turn_left)
                        stringResource(R.string.turn_instruction_format, dir, ti.degrees, ti.elevationDegrees)
                    } ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.waiting_satellite_lock),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Satellite list panel ──────────────────────────────────────────────────────
@Composable
fun SatelliteListPanel(
    satellites: List<GnssSatellite>,
    modifier: Modifier = Modifier
) {
    if (satellites.isEmpty()) return

    val usedCount  = remember(satellites) { satellites.count { it.usedInFix } }
    val sortedSats = remember(satellites) { satellites.sortedByDescending { it.cn0DbHz } }

    InfoGroupCard(
        title = stringResource(R.string.all_satellites_panel),
        icon = Icons.Default.SatelliteAlt,
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
        contentColor = MaterialTheme.colorScheme.secondary,
        modifier = modifier
    ) {
        // Summary chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SatelliteSummaryChip(
                text = stringResource(R.string.satellite_visible_count, satellites.size),
                color = MaterialTheme.colorScheme.secondary
            )
            SatelliteSummaryChip(
                text = stringResource(R.string.satellite_used_count, usedCount),
                color = MaterialTheme.colorScheme.primary
            )
        }

        sortedSats.forEach { sat ->
            SatelliteRow(satellite = sat)
        }
    }
}

@Composable
private fun SatelliteSummaryChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SatelliteRow(satellite: GnssSatellite) {
    // Resolve name and color from the shared helper — no duplicate when() block.
    val (constName, constColor) = constellationInfo(satellite.constellationType)
    val usedLabel = stringResource(if (satellite.usedInFix) R.string.sat_used_in_fix_label else R.string.sat_not_used_label)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Constellation color dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(constColor)
        )

        // ID + constellation — bumped to bodySmall for readability
        Text(
            text = "$constName #${satellite.svid}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(84.dp),
            maxLines = 1
        )

        // C/N0 signal bar
        LinearProgressIndicator(
            progress = { (satellite.cn0DbHz / 50f).coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = constColor,
            trackColor = constColor.copy(alpha = 0.12f)
        )

        // Signal value + unit
        Text(
            text = String.format(Locale.US, "%.0f", satellite.cn0DbHz),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )

        // Used-in-fix badge with text label for accessibility
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (satellite.usedInFix) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
        ) {
            Text(
                text = usedLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (satellite.usedInFix) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}
