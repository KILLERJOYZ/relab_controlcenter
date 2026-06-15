package com.example.relab_tool.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import java.util.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.relab_tool.R
import com.example.relab_tool.model.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.relab_tool.ui.CameraSpecScreen
import com.example.relab_tool.ui.CameraSpecViewModel
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.relab_tool.utils.SoCUtils
import com.example.relab_tool.ui.theme.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.window.Popup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridState
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

// --- Models & Utils ---
data class TabItem(val title: String, val icon: ImageVector)

// --- Utility Components ---

@Composable fun BatteryChip(icon: ImageVector, text: String) {
    Surface(color = Color.Black.copy(alpha = 0.2f), shape = CircleShape) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BatteryHistoryCard(batteryHistory: List<Triple<Long, Int, Boolean>>, lastFullChargeTs: Long, lastStoppedChargingTs: Long) {
    val chargingColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val dischargingColor = MaterialTheme.colorScheme.outline

    // Pre-allocated drawing objects to avoid object allocations in draw scope
    val calendar = remember { Calendar.getInstance() }
    val textPaint = remember(onSurfaceColor) {
        android.graphics.Paint().apply {
            color = onSurfaceColor.copy(alpha = 0.4f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }
    }
    val trianglePath = remember { Path() }
    val sortedPoints = remember(batteryHistory) {
        batteryHistory.sortedBy { it.first }
    }
    val dashPathEffect = remember { androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f)) }

    InfoGroupCard(
        title = stringResource(R.string.battery_history_title),
        icon = Icons.Default.History,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        cardId = "group_battery_history"
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                val gridColor = onSurfaceColor.copy(alpha = 0.1f)
                drawLine(gridColor, Offset(0f, 0f), Offset(0f, size.height))
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height))
                
                textPaint.textSize = 10.dp.toPx()
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                listOf(0, 12, 24).forEach { h ->
                    val x = (h / 24f) * size.width
                    drawContext.canvas.nativeCanvas.drawText(h.toString(), x, size.height + 14.dp.toPx(), textPaint)
                }
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                drawContext.canvas.nativeCanvas.drawText("100%", size.width + 4.dp.toPx(), 10.dp.toPx(), textPaint)
                drawContext.canvas.nativeCanvas.drawText("0%", size.width + 4.dp.toPx(), size.height, textPaint)
                if (sortedPoints.isNotEmpty()) {
                    for (i in 0 until sortedPoints.size - 1) {
                        val p1 = sortedPoints[i]
                        val p2 = sortedPoints[i + 1]
                        
                        val x1 = ((p1.first - startOfDay).toFloat() / (24 * 60 * 60 * 1000f)) * size.width
                        val x2 = ((p2.first - startOfDay).toFloat() / (24 * 60 * 60 * 1000f)) * size.width
                        
                        val y1 = size.height - (p1.second / 100f) * size.height
                        val y2 = size.height - (p2.second / 100f) * size.height
                        
                        val color = if (p2.third) chargingColor else dischargingColor
                        
                        if (x2 >= 0f && x1 <= size.width && (p2.first - p1.first) < 4 * 60 * 60 * 1000L) {
                            drawLine(
                                color = color,
                                start = androidx.compose.ui.geometry.Offset(x1, y1),
                                end = androidx.compose.ui.geometry.Offset(x2, y2),
                                strokeWidth = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                    
                    val lastP = sortedPoints.last()
                    val nowMs = System.currentTimeMillis()
                    val nowX = ((nowMs - startOfDay).toFloat() / (24 * 60 * 60 * 1000f)) * size.width
                    val lastX = ((lastP.first - startOfDay).toFloat() / (24 * 60 * 60 * 1000f)) * size.width
                    val lastY = size.height - (lastP.second / 100f) * size.height
                    
                    if (nowX > lastX && nowX <= size.width && (nowMs - lastP.first) < 2 * 60 * 60 * 1000L) {
                        drawLine(
                            color = if (lastP.third) chargingColor else dischargingColor,
                            start = androidx.compose.ui.geometry.Offset(lastX, lastY),
                            end = androidx.compose.ui.geometry.Offset(nowX, lastY),
                            strokeWidth = 3.dp.toPx(),
                            pathEffect = dashPathEffect
                        )
                    }
                }
                val now = System.currentTimeMillis()
                val nowX = ((now - startOfDay).toFloat() / (24 * 60 * 60 * 1000f)) * size.width
                if (nowX in 0f..size.width) {
                    trianglePath.reset()
                    trianglePath.moveTo(nowX, size.height)
                    trianglePath.lineTo(nowX - 6.dp.toPx(), size.height + 8.dp.toPx())
                    trianglePath.lineTo(nowX + 6.dp.toPx(), size.height + 8.dp.toPx())
                    trianglePath.close()
                    drawPath(trianglePath, Color.White)
                    drawLine(Color.White.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(nowX, 0f), androidx.compose.ui.geometry.Offset(nowX, size.height), strokeWidth = 1.dp.toPx())
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(dischargingColor)); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(R.string.status_discharging), style = MaterialTheme.typography.labelSmall) }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(chargingColor)); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(R.string.status_charging), style = MaterialTheme.typography.labelSmall) }
        }
        if (lastFullChargeTs > 0 || lastStoppedChargingTs > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            if (lastFullChargeTs > 0) {
                val duration = System.currentTimeMillis() - lastFullChargeTs
                val hours = duration / (1000 * 60 * 60); val minutes = (duration / (1000 * 60)) % 60
                Text(stringResource(R.string.last_charge_full, hours.toInt(), minutes.toInt()), style = MaterialTheme.typography.labelSmall, color = onSurfaceColor.copy(alpha = 0.5f))
            }
            if (lastStoppedChargingTs > 0) {
                val duration = System.currentTimeMillis() - lastStoppedChargingTs
                val minutes = duration / (1000 * 60)
                if (minutes < 60) Text(stringResource(R.string.stopped_charging_min, minutes.toInt()), style = MaterialTheme.typography.labelSmall, color = onSurfaceColor.copy(alpha = 0.5f))
                else { val hours = minutes / 60; Text(stringResource(R.string.stopped_charging_hour, hours.toInt(), (minutes % 60).toInt()), style = MaterialTheme.typography.labelSmall, color = onSurfaceColor.copy(alpha = 0.5f)) }
            }
        }
    }
}

@Composable
fun WattageHistoryCard(wattage: String, wattageHistory: List<Float>, onClick: (() -> Unit)? = null) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val isCharging = (wattage.replace(" W", "").toFloatOrNull() ?: 0f) >= 0
    val maxWattage = remember(wattageHistory) { (wattageHistory.maxOrNull() ?: 1f).coerceAtLeast(10f) }
    val minWattage = remember(wattageHistory) { (wattageHistory.minOrNull() ?: 0f).coerceAtMost(0f) }
    val range = remember(maxWattage, minWattage) { (maxWattage - minWattage).coerceAtLeast(1f) }
    val path = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 3.dp.toPx() }, cap = StrokeCap.Round) }

    InfoGroupCard(
        title = if (isCharging) stringResource(R.string.charging_speed) else stringResource(R.string.discharging_speed),
        icon = Icons.Default.Bolt,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        cardId = "group_battery_history"
    ) {
        Column(modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.current_now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(wattage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = secondaryColor) }
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (wattageHistory.isNotEmpty()) {
                        if (minWattage < 0) { val zeroY = size.height - ((0f - minWattage) / range * size.height); drawLine(color = onSurfaceColor.copy(alpha = 0.2f), start = androidx.compose.ui.geometry.Offset(0f, zeroY), end = androidx.compose.ui.geometry.Offset(size.width, zeroY), strokeWidth = 1.dp.toPx()) }
                        path.reset()
                        val stepX = if (wattageHistory.size > 1) size.width / (wattageHistory.size - 1) else size.width
                        wattageHistory.forEachIndexed { i, v -> val x = i * stepX; val y = size.height - ((v - minWattage) / range * size.height); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                        drawPath(path, secondaryColor, style = stroke)
                    }
                }
            }
        }
    }
}

@Composable fun EmptyState(icon: ImageVector, message: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline); Spacer(modifier = Modifier.height(16.dp)); Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable fun HardwareSensorItem(s: PhysicalSensor) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(s.model, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${s.manufacturer} | ${s.role}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(s.resolution, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable fun CameraSummaryCard(c: CameraInfo, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shadow = if (selected) 8.dp else 1.dp
    
    Card(
        modifier = Modifier.width(180.dp).height(130.dp),
        shape = ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(shadow),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (selected) {
                Icon(
                    Icons.Default.AutoAwesome, null,
                    modifier = Modifier.size(60.dp).align(Alignment.BottomEnd).offset(x = 10.dp, y = 10.dp).alpha(0.1f),
                    tint = Color.White
                )
            }
            Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (c.facing.contains("FRONT", true)) Icons.Default.Portrait else Icons.Default.Camera,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = if (selected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = c.facing.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.1f MP".format(Locale.US, c.physicalMP),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = content
                    )
                    val res = c.sensorResolution.substringAfter("(").substringBefore(")")
                    if (res.contains("x")) {
                        Text(
                            text = res,
                            style = MaterialTheme.typography.labelSmall,
                            color = content.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val fl35 = c.focalLength35mm
                    val flText = if (fl35.isNotEmpty() && fl35 != "N/A") "${c.focalLength} ($fl35)" else c.focalLength
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = if (selected) Color.White else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = flText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable fun CpuClusterBox(c: CpuCluster) {
    Surface(modifier = Modifier.width(160.dp).height(100.dp), shape = ShapeMedium, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.unit_cores, c.coreCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(c.architecture, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${c.minFreq} - ${c.maxFreq}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ClusterSparkline(clusterHistory: List<Float>, chartColor: Color, modifier: Modifier = Modifier) {
    val path = remember { Path() }
    val fillPath = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 2.dp.toPx() }, cap = StrokeCap.Round) }

    Canvas(modifier = modifier) {
        path.reset()
        val stepX = size.width / (clusterHistory.size - 1).coerceAtLeast(1)
        clusterHistory.forEachIndexed { ci, cv ->
            val x = ci * stepX
            val y = size.height - (cv / 100f * size.height)
            if (ci == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (clusterHistory.size > 1) {
            fillPath.reset()
            fillPath.addPath(path)
            fillPath.lineTo((clusterHistory.size - 1) * stepX, size.height)
            fillPath.lineTo(0f, size.height)
            fillPath.close()
            drawPath(fillPath, chartColor.copy(alpha = 0.15f))
            drawPath(path, chartColor.copy(alpha = 0.6f), style = stroke)
        }
    }
}

@Composable
fun CoreHistorySparkline(coreHistory: List<Float>, chartColor: Color, modifier: Modifier = Modifier) {
    val path = remember { Path() }
    val fillPath = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 1.5.dp.toPx() }, cap = StrokeCap.Round) }

    Canvas(modifier = modifier) {
        path.reset()
        val stepX = size.width / 19f
        coreHistory.forEachIndexed { ci, cv ->
            val x = ci * stepX
            val y = size.height - (cv / 100f * size.height)
            if (ci == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        if (coreHistory.size > 1) {
            fillPath.reset()
            fillPath.addPath(path)
            fillPath.lineTo((coreHistory.size - 1) * stepX, size.height)
            fillPath.lineTo(0f, size.height)
            fillPath.close()
            drawPath(fillPath, chartColor)
            drawPath(path, chartColor.copy(alpha = 0.4f), style = stroke)
        }
    }
}

@Composable fun CpuCoreBox(index: Int, freq: Long, history: List<Float>, modifier: Modifier = Modifier) {
    val path = remember { Path() }
    val fillPath = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 1.5.dp.toPx() }, cap = StrokeCap.Round) }
    Card(modifier = modifier.height(100.dp), shape = ShapeCard, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (history.isNotEmpty()) {
                val chartColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter)) {
                    path.reset()
                    val stepX = size.width / 19f
                    history.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - (v / 100f * size.height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    if (history.size > 1) {
                        fillPath.reset()
                        fillPath.addPath(path)
                        fillPath.lineTo((history.size - 1) * stepX, size.height)
                        fillPath.lineTo(0f, size.height)
                        fillPath.close()
                        drawPath(fillPath, chartColor)
                        drawPath(path, chartColor.copy(alpha = 0.4f), style = stroke)
                    }
                }
            }
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.core_format, index), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(if (freq > 0) "$freq" else stringResource(R.string.offline), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (freq > 0) MaterialTheme.colorScheme.primary else Color.Gray)
                if (freq > 0) Text("MHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable fun CpuClusterLiveBox(name: String, freq: Long, history: List<Float>, modifier: Modifier = Modifier) {
    val path = remember { Path() }
    val fillPath = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 1.5.dp.toPx() }, cap = StrokeCap.Round) }
    Card(modifier = modifier.height(100.dp), shape = ShapeCard, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (history.isNotEmpty()) {
                val chartColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter)) {
                    path.reset()
                    val stepX = size.width / 19f
                    history.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - (v / 100f * size.height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    if (history.size > 1) {
                        fillPath.reset()
                        fillPath.addPath(path)
                        fillPath.lineTo((history.size - 1) * stepX, size.height)
                        fillPath.lineTo(0f, size.height)
                        fillPath.close()
                        drawPath(fillPath, chartColor)
                        drawPath(path, chartColor.copy(alpha = 0.4f), style = stroke)
                    }
                }
            }
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(if (freq > 0) "$freq" else stringResource(R.string.offline), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (freq > 0) MaterialTheme.colorScheme.primary else Color.Gray)
                if (freq > 0) Text("MHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable fun MiniGraphBox(title: String, value: String, icon: ImageVector, history: List<Float>, modifier: Modifier = Modifier, maxVal: Float = 100f) {
    val path = remember { Path() }
    val fillPath = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 1.5.dp.toPx() }, cap = StrokeCap.Round) }
    val graphMax = remember(history, maxVal) {
        val maxInHistory = history.maxOrNull() ?: maxVal
        maxOf(maxVal, maxInHistory)
    }
    Card(modifier = modifier.fillMaxWidth().height(64.dp), shape = ShapeCard, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (history.isNotEmpty()) {
                val chartColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                Canvas(modifier = Modifier.fillMaxWidth().height(32.dp).align(Alignment.BottomCenter)) {
                    path.reset()
                    val stepX = size.width / 19f
                    history.forEachIndexed { i, v ->
                        val x = i * stepX
                        val y = size.height - (v / graphMax * size.height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    if (history.size > 1) {
                        fillPath.reset()
                        fillPath.addPath(path)
                        fillPath.lineTo((history.size - 1) * stepX, size.height)
                        fillPath.lineTo(0f, size.height)
                        fillPath.close()
                        drawPath(fillPath, chartColor)
                        drawPath(path, chartColor.copy(alpha = 0.4f), style = stroke)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable fun NetworkSpeedGraphBox(
    downloadSpeed: String,
    uploadSpeed: String,
    downloadHistory: List<Float>,
    uploadHistory: List<Float>,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    lineColorDl: Color = MaterialTheme.colorScheme.primary,
    lineColorUl: Color = MaterialTheme.colorScheme.secondary
) {
    val pathDl = remember { Path() }
    val fillPathDl = remember { Path() }
    val pathUl = remember { Path() }
    val fillPathUl = remember { Path() }
    
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 2.dp.toPx() }, cap = StrokeCap.Round) }
    
    val graphMaxDl = remember(downloadHistory) {
        val maxInHistory = downloadHistory.maxOrNull() ?: 1f
        maxOf(1f, maxInHistory)
    }
    
    val graphMaxUl = remember(uploadHistory) {
        val maxInHistory = uploadHistory.maxOrNull() ?: 1f
        maxOf(1f, maxInHistory)
    }

    Row(
        modifier = modifier.height(100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Download Card
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = ShapeCard,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (downloadHistory.isNotEmpty()) {
                    val fillColor = lineColorDl.copy(alpha = 0.15f)
                    val strokeColor = lineColorDl.copy(alpha = 0.8f)
                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter)) {
                        pathDl.reset()
                        val stepX = size.width / 19f
                        downloadHistory.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = size.height - (v / graphMaxDl * size.height)
                            if (i == 0) pathDl.moveTo(x, y) else pathDl.lineTo(x, y)
                        }
                        if (downloadHistory.size > 1) {
                            fillPathDl.reset()
                            fillPathDl.addPath(pathDl)
                            fillPathDl.lineTo((downloadHistory.size - 1) * stepX, size.height)
                            fillPathDl.lineTo(0f, size.height)
                            fillPathDl.close()
                            drawPath(fillPathDl, fillColor)
                            drawPath(pathDl, strokeColor, style = stroke)
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(16.dp), tint = lineColorDl)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.network_dl), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(downloadSpeed, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = lineColorDl)
                }
            }
        }

        // Upload Card
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = ShapeCard,
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (uploadHistory.isNotEmpty()) {
                    val fillColor = lineColorUl.copy(alpha = 0.15f)
                    val strokeColor = lineColorUl.copy(alpha = 0.8f)
                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter)) {
                        pathUl.reset()
                        val stepX = size.width / 19f
                        uploadHistory.forEachIndexed { i, v ->
                            val x = i * stepX
                            val y = size.height - (v / graphMaxUl * size.height)
                            if (i == 0) pathUl.moveTo(x, y) else pathUl.lineTo(x, y)
                        }
                        if (uploadHistory.size > 1) {
                            fillPathUl.reset()
                            fillPathUl.addPath(pathUl)
                            fillPathUl.lineTo((uploadHistory.size - 1) * stepX, size.height)
                            fillPathUl.lineTo(0f, size.height)
                            fillPathUl.close()
                            drawPath(fillPathUl, fillColor)
                            drawPath(pathUl, strokeColor, style = stroke)
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp), tint = lineColorUl)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.network_ul), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(uploadSpeed, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = lineColorUl)
                }
            }
        }
    }
}


@Composable fun UsageHistoryDialog(title: String, current: Int, history: List<Float>, color: Color, onDismiss: () -> Unit) {
    val path = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 3.dp.toPx() }, cap = StrokeCap.Round) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("$current%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = color); Box(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 16.dp)) { Canvas(modifier = Modifier.fillMaxSize()) { if (history.isNotEmpty()) { path.reset(); val stepX = size.width / 20f; history.forEachIndexed { i, v -> val x = i * stepX; val y = size.height - (v / 100f * size.height); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; drawPath(path, color, style = stroke) } } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}

@Composable fun WattageFullHistoryDialog(wattage: String, history: List<Float>, onDismiss: () -> Unit) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error
    val isCharging = (wattage.replace(" W", "").toFloatOrNull() ?: 0f) >= 0
    val peakWattage = remember(history) { history.maxOrNull() ?: 0f }
    
    val path = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 2.5.dp.toPx() }, cap = StrokeCap.Round) }
    val dashPathEffect = remember { androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) }
    
    val labelPaint = remember(onSurfaceColor, density) {
        android.graphics.Paint().apply {
            color = onSurfaceColor.copy(alpha = 0.4f).toArgb()
            textSize = with(density) { 10.dp.toPx() }
        }
    }
    
    val calculatedLimits = remember(history) {
        val maxW = (history.maxOrNull() ?: 0f).coerceAtLeast(25f)
        val minW = (history.minOrNull() ?: 0f).coerceAtMost(0f)
        val yLimit = (((maxW / 25).toInt() + 1) * 25).toFloat()
        val yMin = if (minW < 0) (((minW / 25).toInt() - 1) * 25).toFloat() else 0f
        val yRange = yLimit - yMin
        Triple(yLimit, yMin, yRange)
    }
    val yLimit = calculatedLimits.first
    val yMin = calculatedLimits.second
    val yRange = calculatedLimits.third

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isCharging) stringResource(R.string.charging_speed) else "Discharging Speed") }, text = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.current_now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(wattage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = secondaryColor) }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Peak Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("%.2f W".format(Locale.US, peakWattage), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = errorColor) }
            Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(start = 32.dp, top = 16.dp, bottom = 32.dp, end = 8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridColor = onSurfaceColor.copy(alpha = 0.1f)
                    var yS = yMin; while (yS <= yLimit) { val yP = size.height - ((yS - yMin) / yRange * size.height); drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, yP), androidx.compose.ui.geometry.Offset(size.width, yP)); drawContext.canvas.nativeCanvas.drawText("${yS.toInt()}W", -32.dp.toPx(), yP + 4.dp.toPx(), labelPaint); yS += 25f }
                    val pointsPer5Min = 1000f; val xInt = (history.size / pointsPer5Min).toInt()
                    for (i in 0..xInt) { val xP = (i * pointsPer5Min / history.size.coerceAtLeast(1).toFloat()) * size.width; drawLine(gridColor, androidx.compose.ui.geometry.Offset(xP, 0f), androidx.compose.ui.geometry.Offset(xP, size.height)); labelPaint.textAlign = android.graphics.Paint.Align.CENTER; drawContext.canvas.nativeCanvas.drawText("${i * 5}m", xP, size.height + 16.dp.toPx(), labelPaint) }
                    val peakY = size.height - ((peakWattage - yMin) / yRange * size.height); drawLine(color = errorColor.copy(alpha = 0.4f), start = androidx.compose.ui.geometry.Offset(0f, peakY), end = androidx.compose.ui.geometry.Offset(size.width, peakY), strokeWidth = 1.dp.toPx(), pathEffect = dashPathEffect)
                    if (history.isNotEmpty()) { path.reset(); val stepX = size.width / (history.size - 1).coerceAtLeast(1); history.forEachIndexed { i, v -> val x = i * stepX; val y = size.height - ((v - yMin) / yRange * size.height); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; drawPath(path, secondaryColor, style = stroke) }
                }
            }
            Text("Total data points: ${history.size} (~%.1f minutes)".format(Locale.US, history.size * 0.3 / 60.0), style = MaterialTheme.typography.labelSmall, color = onSurfaceColor.copy(alpha = 0.5f))
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}

@Composable fun BatteryHistoryDialog(
    level: Int,
    wattage: String,
    batteryHistory: List<Triple<Long, Int, Boolean>>,
    wattageHistory: List<Float>,
    fullWattageHistory: List<Float>,
    lastFullChargeTs: Long,
    lastStoppedChargingTs: Long,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    var showFullWattage by remember { mutableStateOf(false) }

    if (showFullWattage) {
        WattageFullHistoryDialog(wattage, fullWattageHistory) { showFullWattage = false }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.tab_battery), fontWeight = FontWeight.Bold) }, text = {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$level%", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = primaryColor)
            Text(stringResource(R.string.capacity), style = MaterialTheme.typography.labelMedium, color = onSurfaceColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(16.dp))
            BatteryHistoryCard(batteryHistory, lastFullChargeTs, lastStoppedChargingTs)
            Spacer(modifier = Modifier.height(16.dp))
            WattageHistoryCard(wattage, wattageHistory) { showFullWattage = true }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}

@Composable fun AppEntryCard(app: AppEntry) {
    val context = LocalContext.current
    var exp by remember { mutableStateOf(false) }
    
    val iconDrawable by produceState<android.graphics.drawable.Drawable?>(initialValue = null, keys = arrayOf(app.packageName)) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShapeCard,
        onClick = { exp = !exp },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (iconDrawable != null) {
                            AsyncImage(
                                model = iconDrawable,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = app.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                Row {
                    if (app.isGame) BadgeTag(stringResource(R.string.tag_game), MaterialTheme.colorScheme.primaryContainer)
                    if (app.isSystem) BadgeTag(stringResource(R.string.tag_system), MaterialTheme.colorScheme.tertiaryContainer)
                }
            }
            AnimatedVisibility(
                visible = exp,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(stringResource(R.string.version), app.version)
                    InfoRow(stringResource(R.string.target_sdk), app.sdk)
                    if (app.updateUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(app.updateUrl)
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.update_apkpure))
                        }
                    }
                }
            }
        }
    }
}

@Composable fun DashboardRamCard(
    total: Long,
    used: Long,
    history: List<Float>,
    size: CardSize,
    modifier: Modifier = Modifier
) {
    val path = remember { Path() }
    val fillPath = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 2.dp.toPx() }, cap = StrokeCap.Round) }
    val usedPercent = if (total > 0) (used.toFloat() / total.toFloat() * 100).toInt() else 0
    Card(
        modifier = modifier.fillMaxSize(),
        shape = ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (size) {
                CardSize.SIZE_1x1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Memory, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$usedPercent%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("RAM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                    }
                }
                CardSize.SIZE_2x1 -> {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Memory, null, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("RAM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("$usedPercent%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { usedPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.onPrimary,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
                CardSize.SIZE_2x2 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("RAM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp).weight(1f, fill = false)) {
                            CircularProgressIndicator(
                                progress = { usedPercent / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 6.dp,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                            Text("$usedPercent%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(stringResource(R.string.ram_used_format, used / 1024 / 1024), style = MaterialTheme.typography.labelSmall)
                    }
                }
                CardSize.SIZE_4x2, CardSize.SIZE_4x4 -> {
                    if (history.isNotEmpty()) {
                        val chartColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                        Canvas(modifier = Modifier.fillMaxWidth().height(if (size == CardSize.SIZE_4x4) 140.dp else 60.dp).align(Alignment.BottomCenter)) {
                            val canvasWidth = this.size.width
                            val canvasHeight = this.size.height
                            path.reset()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                fillPath.reset()
                                fillPath.addPath(path)
                                fillPath.lineTo((history.size - 1) * stepX, canvasHeight)
                                fillPath.lineTo(0f, canvasHeight)
                                fillPath.close()
                                drawPath(fillPath, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.4f), style = stroke)
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.ram_label, total / 1024 / 1024), style = MaterialTheme.typography.labelLarge)
                            Text(stringResource(R.string.ram_used_format, used / 1024 / 1024), style = MaterialTheme.typography.labelLarge)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                CircularProgressIndicator(
                                    progress = { usedPercent / 100f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text("$usedPercent%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            if (size == CardSize.SIZE_4x4) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Free RAM: ${(total - used) / 1024 / 1024} MB", style = MaterialTheme.typography.bodyMedium)
                                    Text("Total Capacity: ${total / 1024 / 1024 / 1024f} GB", style = MaterialTheme.typography.bodyMedium)
                                    Text("Status: Stable", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Text(
                            stringResource(R.string.ram_free_format, (total - used) / 1024 / 1024),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

@Composable fun DashboardCpuCard(
    usage: Int,
    history: List<Float>,
    size: CardSize,
    modifier: Modifier = Modifier
) {
    val path = remember { Path() }
    val fillPath = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 2.dp.toPx() }, cap = StrokeCap.Round) }
    val progress by animateFloatAsState(usage / 100f, tween(400), label = "cpu")
    Card(
        modifier = modifier.fillMaxSize(),
        shape = ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (size) {
                CardSize.SIZE_1x1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Memory, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$usage%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("CPU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                    }
                }
                CardSize.SIZE_2x1 -> {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Memory, null, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CPU", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("$usage%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
                CardSize.SIZE_2x2 -> {
                    if (history.isNotEmpty()) {
                        val chartColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        Canvas(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)) {
                            val canvasWidth = this.size.width
                            val canvasHeight = this.size.height
                            path.reset()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                fillPath.reset()
                                fillPath.addPath(path)
                                fillPath.lineTo((history.size - 1) * stepX, canvasHeight)
                                fillPath.lineTo(0f, canvasHeight)
                                fillPath.close()
                                drawPath(fillPath, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.3f), style = stroke)
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.cpu_utilisation), style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                strokeWidth = 6.dp,
                                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                            Text("$usage%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                CardSize.SIZE_4x2, CardSize.SIZE_4x4 -> {
                    if (history.isNotEmpty()) {
                        val chartColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        Canvas(modifier = Modifier.fillMaxWidth().height(if (size == CardSize.SIZE_4x4) 140.dp else 60.dp).align(Alignment.BottomCenter)) {
                            val canvasWidth = this.size.width
                            val canvasHeight = this.size.height
                            path.reset()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                fillPath.reset()
                                fillPath.addPath(path)
                                fillPath.lineTo((history.size - 1) * stepX, canvasHeight)
                                fillPath.lineTo(0f, canvasHeight)
                                fillPath.close()
                                drawPath(fillPath, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.3f), style = stroke)
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.cpu_utilisation), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Current: $usage%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text("$usage%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("CPU Load: $usage%", style = MaterialTheme.typography.bodyMedium)
                                Text("Scheduler: CFS", style = MaterialTheme.typography.bodyMedium)
                                if (size == CardSize.SIZE_4x4) {
                                    Text("Governor: Interactive/Schedutil", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable fun DashboardGpuCard(
    usage: Int,
    history: List<Float>,
    size: CardSize,
    modifier: Modifier = Modifier
) {
    val path = remember { Path() }
    val fillPath = remember { Path() }
    val density = LocalDensity.current
    val stroke = remember(density) { Stroke(with(density) { 2.dp.toPx() }, cap = StrokeCap.Round) }
    val progress by animateFloatAsState(usage / 100f, tween(400), label = "gpu")
    Card(
        modifier = modifier.fillMaxSize(),
        shape = ShapeCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (size) {
                CardSize.SIZE_1x1 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.GraphicEq, null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$usage%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("GPU", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                    }
                }
                CardSize.SIZE_2x1 -> {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.GraphicEq, null, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GPU", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("$usage%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
                CardSize.SIZE_2x2 -> {
                    if (history.isNotEmpty()) {
                        val chartColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f)
                        Canvas(modifier = Modifier.fillMaxWidth().height(60.dp).align(Alignment.BottomCenter)) {
                            val canvasWidth = this.size.width
                            val canvasHeight = this.size.height
                            path.reset()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                fillPath.reset()
                                fillPath.addPath(path)
                                fillPath.lineTo((history.size - 1) * stepX, canvasHeight)
                                fillPath.lineTo(0f, canvasHeight)
                                fillPath.close()
                                drawPath(fillPath, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.3f), style = stroke)
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(stringResource(R.string.gpu_utilisation), style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                strokeWidth = 6.dp,
                                trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                                strokeCap = StrokeCap.Round
                            )
                            Text("$usage%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                CardSize.SIZE_4x2, CardSize.SIZE_4x4 -> {
                    if (history.isNotEmpty()) {
                        val chartColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f)
                        Canvas(modifier = Modifier.fillMaxWidth().height(if (size == CardSize.SIZE_4x4) 140.dp else 60.dp).align(Alignment.BottomCenter)) {
                            val canvasWidth = this.size.width
                            val canvasHeight = this.size.height
                            path.reset()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                fillPath.reset()
                                fillPath.addPath(path)
                                fillPath.lineTo((history.size - 1) * stepX, canvasHeight)
                                fillPath.lineTo(0f, canvasHeight)
                                fillPath.close()
                                drawPath(fillPath, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.3f), style = stroke)
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.gpu_utilisation), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Current: $usage%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                CircularProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    strokeWidth = 8.dp,
                                    trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f),
                                    strokeCap = StrokeCap.Round
                                )
                                Text("$usage%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("GPU Load: $usage%", style = MaterialTheme.typography.bodyMedium)
                                Text("Vulkan Support: Yes", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable fun HighlightCard(modifier: Modifier = Modifier, title: String, value: String, subtext: String, containerColor: Color, contentColor: Color) {
    Card(modifier = modifier.height(110.dp), shape = ShapeCard, colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = contentColor.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            if (subtext.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtext, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.5f), textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

// --- Tab Screens ---

@OptIn(ExperimentalComposeUiApi::class)
data class StatusCardItem(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val value: String,
    val subtext: String,
    val containerColor: Color,
    val contentColor: Color,
    val progress: Float? = null,
    val isCharging: Boolean = false,
    val isCounter: Boolean = false,
    val onClick: (() -> Unit)? = null
)

private fun getSpanCount(size: CardSize): Int = when (size) {
    CardSize.SIZE_1x1 -> 1
    CardSize.SIZE_2x1, CardSize.SIZE_2x2 -> 2
    CardSize.SIZE_4x2, CardSize.SIZE_4x4 -> 4
}

private fun getHeight(size: CardSize): Dp = when (size) {
    CardSize.SIZE_1x1, CardSize.SIZE_2x1 -> 80.dp
    CardSize.SIZE_2x2, CardSize.SIZE_4x2 -> 160.dp
    CardSize.SIZE_4x4 -> 320.dp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.dashboardCardBorder(isResizing: Boolean): Modifier {
    if (!isResizing) return this
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    return this.border(
        width = 2.5.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha),
        shape = ShapeCard
    )
}

private fun findClosestCardSize(targetSpan: Int, targetHeightCells: Int): CardSize {
    val options = listOf(
        CardSize.SIZE_1x1 to (1 to 1),
        CardSize.SIZE_2x1 to (2 to 1),
        CardSize.SIZE_2x2 to (2 to 2),
        CardSize.SIZE_4x2 to (4 to 2),
        CardSize.SIZE_4x4 to (4 to 4)
    )
    return options.minByOrNull { (_, spec) ->
        val (optSpan, optHeight) = spec
        val spanDiff = kotlin.math.abs(optSpan - targetSpan)
        val heightDiff = kotlin.math.abs(optHeight - targetHeightCells)
        spanDiff + heightDiff
    }?.first ?: CardSize.SIZE_2x2
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyGridItemScope.ResizableCardContainer(
    cardId: String,
    currentSize: CardSize,
    isResizingMode: Boolean,
    isResizeHandleVisible: Boolean,
    onSizeChanged: (CardSize) -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onReorderDrag: (Offset) -> Offset,
    onReorderEnd: () -> Unit,
    modifier: Modifier = Modifier,
    gridColumns: Int = 4,
    content: @Composable (CardSize) -> Unit
) {

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Calculate grid dimensions dynamically for snapping
    val screenWidthDp = configuration.screenWidthDp.dp
    val gridWidthDp = minOf(screenWidthDp, 1280.dp) - 32.dp
    val spacingDp = 12.dp
    val totalSpacingDp = spacingDp * (gridColumns - 1)
    val cellWidthDp = (gridWidthDp - totalSpacingDp) / gridColumns
    val cellWidthPx = with(density) { cellWidthDp.toPx() }
    val cellHeightPx = with(density) { 80.dp.toPx() }

    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    var reorderDragOffset by remember { mutableStateOf(Offset.Zero) }
    var isReorderDragging by remember { mutableStateOf(false) }

    // Measure the actual layout size of the card
    var initialWidthPx by remember { mutableStateOf(0f) }
    var initialHeightPx by remember { mutableStateOf(0f) }

    // Clamping values (max span is 4 columns)
    val minWidthPx = cellWidthPx
    val minHeightPx = cellHeightPx
    val maxWidthPx = cellWidthPx * 4 + with(density) { 36.dp.toPx() }
    val maxHeightPx = cellHeightPx * 4 + with(density) { 36.dp.toPx() }

    // Current visual size during dragging
    val visualWidthPx = if (isDragging) {
        (initialWidthPx + dragOffsetX).coerceIn(minWidthPx, maxWidthPx)
    } else {
        initialWidthPx
    }
    val visualHeightPx = if (isDragging) {
        (initialHeightPx + dragOffsetY).coerceIn(minHeightPx, maxHeightPx)
    } else {
        initialHeightPx
    }

    // Determine target size for snapping preview in real-time
    val candidateSize = if (isDragging) {
        val spacingPx = with(density) { 12.dp.toPx() }
        val targetSpan = ((visualWidthPx + spacingPx) / (cellWidthPx + spacingPx)).roundToInt().coerceIn(1, 4)
        val targetHeightCells = ((visualHeightPx + spacingPx) / (cellHeightPx + spacingPx)).roundToInt().coerceIn(1, 4)
        findClosestCardSize(targetSpan, targetHeightCells)
    } else {
        currentSize
    }

    // Trigger haptic tick feedback if candidate size changes during drag
    var lastCandidateSize by remember { mutableStateOf(currentSize) }
    LaunchedEffect(candidateSize) {
        if (isDragging && candidateSize != lastCandidateSize) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastCandidateSize = candidateSize
        }
    }

    // Wrap in rememberUpdatedState to avoid stale capture in long-lived gesture pointerInput lambdas
    val currentOnSizeChanged by rememberUpdatedState(onSizeChanged)
    val currentCandidateSize by rememberUpdatedState(candidateSize)
    val currentCardSize by rememberUpdatedState(currentSize)
    val currentOnReorderDrag by rememberUpdatedState(onReorderDrag)
    val currentOnReorderEnd by rememberUpdatedState(onReorderEnd)

    Box(
        modifier = modifier
            .animateItem()
            .zIndex(if (isDragging || isReorderDragging) 100f else 1f)
            .height(getHeight(currentSize))
            .offset {
                if (isReorderDragging) {
                    IntOffset(reorderDragOffset.x.roundToInt(), reorderDragOffset.y.roundToInt())
                } else {
                    IntOffset.Zero
                }
            }
            .onGloballyPositioned { coordinates ->
                if (!isDragging && !isReorderDragging) {
                    initialWidthPx = coordinates.size.width.toFloat()
                    initialHeightPx = coordinates.size.height.toFloat()
                }
            }
    ) {
        // Inner content box — handles gestures & border
        Box(
            modifier = (if (isDragging) {
                Modifier.requiredSize(
                    width = with(density) { visualWidthPx.toDp() },
                    height = with(density) { visualHeightPx.toDp() }
                )
            } else {
                Modifier.fillMaxSize()
            })
            .dashboardCardBorder(isResizeHandleVisible)
            .then(
                if (isResizingMode) {
                    Modifier.pointerInput(cardId) {
                        detectDragGestures(
                            onDragStart = { startPosition ->
                                val handleSizePx = with(density) { 56.dp.toPx() }
                                val isTouchOnHandle = isResizeHandleVisible &&
                                        startPosition.x >= (initialWidthPx - handleSizePx) &&
                                        startPosition.y >= (initialHeightPx - handleSizePx)

                                if (!isTouchOnHandle) {
                                    isReorderDragging = true
                                    reorderDragOffset = Offset.Zero
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (isReorderDragging) {
                                    change.consume()
                                    val newOffset = reorderDragOffset + dragAmount
                                    reorderDragOffset = currentOnReorderDrag(newOffset)
                                }
                            },
                            onDragEnd = {
                                if (isReorderDragging) {
                                    isReorderDragging = false
                                    currentOnReorderEnd()
                                }
                            },
                            onDragCancel = {
                                if (isReorderDragging) {
                                    isReorderDragging = false
                                    currentOnReorderEnd()
                                }
                            }
                        )
                    }
                } else Modifier
            )
            .then(
                if (isResizingMode) {
                    if (!isResizeHandleVisible) {
                        Modifier.combinedClickable(
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                    } else Modifier
                } else {
                    Modifier.combinedClickable(
                        onLongClick = onLongClick,
                        onClick = onClick
                    )
                }
            )
        ) {
            content(currentSize)
        }

        // Snap preview overlay during drag (ghost outline only, uses requiredSize so it escapes constraints)
        if (isDragging) {
            Box(
                modifier = Modifier
                    .requiredSize(
                        width = with(density) { visualWidthPx.toDp() },
                        height = with(density) { visualHeightPx.toDp() }
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = ShapeCard
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = ShapeCard
                    )
            )

            // Target snap outline
            Box(
                modifier = Modifier
                    .requiredSize(
                        width = with(density) {
                            val span = getSpanCount(candidateSize)
                            val w = if (span == 1) cellWidthDp
                                    else if (span == 2) cellWidthDp * 2 + 12.dp
                                    else cellWidthDp * 4 + 36.dp
                            w
                        },
                        height = getHeight(candidateSize)
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = ShapeCard
                    )
            )
        }

        // Resize Drag Handle (only visible for the actively selected card)
        if (isResizeHandleVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp)
                    .pointerInput(cardId) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                                lastCandidateSize = currentCardSize
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX += dragAmount.x
                                dragOffsetY += dragAmount.y
                            },
                            onDragEnd = {
                                isDragging = false
                                currentOnSizeChanged(currentCandidateSize)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragCancel = {
                                isDragging = false
                            }
                        )
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Resize",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer(rotationZ = 45f)
                    )
                }
            }
        }

        // Long press overlay for entering edit mode (only in non-resizing mode)
        if (!isResizingMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onLongClick = onLongClick,
                        onClick = onClick
                    )
            )
        }
    }
}



@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable fun DashboardTab(
    viewModel: DeviceInfoViewModel,
    windowSizeClass: WindowSizeClass,
    onNavigateToInfoTab: (Int) -> Unit = {}
) {
    val dataState by viewModel.dashboardData.collectAsStateWithLifecycle()
    val realtimeState by viewModel.dashboardRealtime.collectAsStateWithLifecycle()
    val socInfoState by viewModel.socInfo.collectAsStateWithLifecycle()
    val batteryHistory by viewModel.batteryCapacityHistory.collectAsStateWithLifecycle()
    val lastFullChargeTs by viewModel.lastFullChargeTs.collectAsStateWithLifecycle()
    val lastStoppedChargingTs by viewModel.lastStoppedChargingTs.collectAsStateWithLifecycle()
    val hasData by remember { derivedStateOf { dataState != null && realtimeState != null } }
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val gridColumns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 4
        WindowWidthSizeClass.Medium -> 6
        WindowWidthSizeClass.Expanded -> 8
        else -> 4
    }
    var showBatteryDialog by remember { mutableStateOf(false) }
    val bottomContentPadding = if (isWideScreen) 16.dp else 120.dp

    val onMemoryClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(6) } }
    val onSoCClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(1) } }
    val onWifiClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(7) } }
    val onBluetoothClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(4) } }
    val onCellularClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(8) } }
    val onDisplayClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(5) } }
    val onDrmClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(14) } }
    val onSensorsClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(11) } }
    val onAppsClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(12) } }
    val onBatteryClick = remember { { showBatteryDialog = true } }

    val context = LocalContext.current
    DashboardLayoutSettings.initIfNeeded(context)
    val cardSizes by DashboardLayoutSettings.cardSizes.collectAsStateWithLifecycle()
    val cardOrder by DashboardLayoutSettings.cardOrder.collectAsStateWithLifecycle()
    
    // UI Stability Overhaul: Local states to decouple from rapid background updates
    var localCardOrder by remember(cardOrder) { mutableStateOf(cardOrder) }
    var localCardSizes by remember(cardSizes) { mutableStateOf(cardSizes) }
    var resizingCardId by remember { mutableStateOf<String?>(null) }
    val isEditing by remember { derivedStateOf { resizingCardId != null } }

    // Debounced bulk persistence
    LaunchedEffect(localCardOrder, localCardSizes, isEditing) {
        if (!isEditing && (localCardOrder != cardOrder || localCardSizes != cardSizes)) {
            // Save immediately when exiting editing mode
            DashboardLayoutSettings.setBulkLayout(localCardOrder, localCardSizes)
        } else if (localCardOrder != cardOrder || localCardSizes != cardSizes) {
            // Debounce while editing
            delay(1000)
            DashboardLayoutSettings.setBulkLayout(localCardOrder, localCardSizes)
        }
    }

    val gridState = rememberLazyGridState()
    val haptic = LocalHapticFeedback.current

    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val onSecondary = MaterialTheme.colorScheme.onSecondary
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val onSecondaryContainer = MaterialTheme.colorScheme.onSecondaryContainer
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onTertiary = MaterialTheme.colorScheme.onTertiary
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val onTertiaryContainer = MaterialTheme.colorScheme.onTertiaryContainer
    val error = MaterialTheme.colorScheme.error
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val inverseSurface = MaterialTheme.colorScheme.inverseSurface
    val inverseOnSurface = MaterialTheme.colorScheme.inverseOnSurface

    if (hasData) {
        val dataForDialog = dataState
        if (showBatteryDialog && dataForDialog != null) {
            val fullWattageHistory by viewModel.fullWattageHistory.collectAsStateWithLifecycle()
            BatteryHistoryDialog(
                level = dataForDialog.batteryLevel,
                wattage = dataForDialog.batteryWattage,
                batteryHistory = batteryHistory,
                wattageHistory = dataForDialog.wattageHistory,
                fullWattageHistory = fullWattageHistory,
                lastFullChargeTs = lastFullChargeTs,
                lastStoppedChargingTs = lastStoppedChargingTs,
                onDismiss = { showBatteryDialog = false }
            )
        }

        val data = dataState ?: return
        val rt = realtimeState ?: return
        val statusItems = remember(data, rt) {
            listOfNotNull(
                StatusCardItem(
                    key = "thermal",
                    title = context.getString(R.string.tab_thermal),
                    icon = Icons.Outlined.Thermostat,
                    value = context.getString(data.thermalStatus),
                    subtext = data.batteryTemp,
                    containerColor = when(data.thermalStatus) {
                        R.string.thermal_cool -> primary
                        R.string.thermal_warm -> secondary
                        R.string.thermal_hot -> tertiary
                        R.string.thermal_throttling -> error
                        else -> surfaceVariant
                    },
                    contentColor = onSurface,
                    onClick = { onNavigateToInfoTab(13) }
                ),
                StatusCardItem(
                    key = "touch_sampling",
                    title = context.getString(R.string.touch_sampling_rate),
                    icon = Icons.Outlined.TouchApp,
                    value = "${rt.touchSamplingRate} Hz",
                    subtext = context.getString(R.string.live_monitoring),
                    containerColor = secondary,
                    contentColor = onSecondary
                ),
                StatusCardItem(
                    key = "wifi",
                    title = context.getString(R.string.tab_wifi),
                    icon = Icons.Outlined.Wifi,
                    value = data.wifiSsid ?: context.getString(R.string.disconnected),
                    subtext = (data.wifiStandard ?: context.getString(R.string.ssid)) + (data.wifiSignalDbm?.let { " | $it dBm" } ?: ""),
                    containerColor = primary,
                    contentColor = onPrimary,
                    onClick = onWifiClick
                ),
                *data.simInfos.map { sim ->
                    StatusCardItem(
                        key = "sim_${sim.slot}",
                        title = context.getString(R.string.sim_slot, sim.slot),
                        icon = Icons.Outlined.SignalCellularAlt,
                        value = sim.carrier,
                        subtext = sim.networkType + (sim.signalDbm?.let { " | $it dBm" } ?: ""),
                        containerColor = tertiary.copy(alpha = 0.45f),
                        contentColor = onTertiary,
                        onClick = onCellularClick
                    )
                }.toTypedArray(),
                if (data.isCharging || data.chargingCurrentMa != 0) {
                    StatusCardItem(
                        key = "charging_current",
                        title = context.getString(R.string.charging_current),
                        icon = Icons.Outlined.Bolt,
                        value = "${data.chargingCurrentMa} mA",
                        subtext = context.getString(R.string.unit_v_precise, data.chargingVoltageV),
                        containerColor = tertiary,
                        contentColor = onTertiary,
                        onClick = onBatteryClick
                    )
                } else null,
                StatusCardItem(
                    key = "battery_health",
                    title = context.getString(R.string.battery_health_pct),
                    icon = Icons.Outlined.HealthAndSafety,
                    value = "${data.batteryHealthPct}%",
                    subtext = "${context.getString(R.string.battery_cycles)}: ${data.batteryCycleCount}",
                    containerColor = primary,
                    contentColor = onPrimary,
                    onClick = onBatteryClick
                ),
                StatusCardItem(
                    key = "uptime",
                    title = context.getString(R.string.uptime),
                    icon = Icons.Outlined.Timer,
                    value = data.uptime,
                    subtext = "${context.getString(R.string.deep_sleep_ratio)}: ${(data.deepSleepRatio * 100).toInt()}%",
                    containerColor = tertiaryContainer,
                    contentColor = onTertiaryContainer
                ),
                StatusCardItem(
                    key = "disk_io",
                    title = context.getString(R.string.disk_io),
                    icon = Icons.Outlined.Save,
                    value = rt.diskReadSpeed,
                    subtext = "${context.getString(R.string.write_speed)}: ${rt.diskWriteSpeed}",
                    containerColor = primaryContainer,
                    contentColor = onPrimaryContainer,
                    onClick = onMemoryClick
                ),
                if (data.bluetoothConnectedDevices > 0 && data.bluetoothCodec != null) {
                    StatusCardItem(
                        key = "bt_codec",
                        title = context.getString(R.string.bt_audio_codec),
                        icon = Icons.Outlined.Audiotrack,
                        value = data.bluetoothCodec,
                        subtext = context.getString(R.string.bluetooth),
                        containerColor = secondary,
                        contentColor = onSecondary,
                        onClick = onBluetoothClick
                    )
                } else null,
                if (rt.ambientLightLux > 0 || rt.pressureHpa > 0) {
                    StatusCardItem(
                        key = "ambient_light",
                        title = context.getString(R.string.ambient_light),
                        icon = Icons.Outlined.LightMode,
                        value = context.getString(R.string.unit_lux, rt.ambientLightLux),
                        subtext = context.getString(R.string.unit_hpa, rt.pressureHpa),
                        containerColor = secondaryContainer,
                        contentColor = onSecondaryContainer,
                        onClick = onSensorsClick
                    )
                } else null,
                StatusCardItem(
                    key = "security",
                    title = context.getString(R.string.security),
                    icon = Icons.Outlined.Security,
                    value = if (data.isRooted) context.getString(R.string.yes) else context.getString(R.string.no),
                    subtext = "SELinux: ${data.selinuxStatus}\n${context.getString(R.string.security_patch_level)}: ${data.securityPatch}",
                    containerColor = if (data.isRooted) error else primary,
                    contentColor = onPrimary,
                    onClick = { onNavigateToInfoTab(14) }
                ),
                StatusCardItem(
                    key = "bluetooth_count",
                    title = "",
                    icon = Icons.Outlined.Bluetooth,
                    value = data.bluetoothConnectedDevices.toString(),
                    subtext = context.getString(R.string.connected_devices),
                    containerColor = secondary,
                    contentColor = onSecondary,
                    onClick = onBluetoothClick,
                    isCounter = true
                ),
                StatusCardItem(
                    key = "storage",
                    title = context.getString(R.string.storage),
                    icon = Icons.Outlined.SdStorage,
                    value = context.getString(R.string.unit_percent, ((data.storageUsed.toFloat() / data.storageTotal.coerceAtLeast(1).toFloat()) * 100).toInt()),
                    subtext = context.getString(R.string.free_format, viewModel.formatSize(data.storageTotal - data.storageUsed)),
                    progress = data.storageUsed.toFloat() / data.storageTotal.coerceAtLeast(1).toFloat(),
                    containerColor = primaryContainer,
                    contentColor = onPrimaryContainer,
                    onClick = onMemoryClick
                ),
                StatusCardItem(
                    key = "screen",
                    title = context.getString(R.string.screen),
                    icon = Icons.Outlined.DisplaySettings,
                    value = data.screenResolution,
                    subtext = data.screenInfo,
                    containerColor = surfaceVariant,
                    contentColor = onSurfaceVariant,
                    onClick = onDisplayClick
                ),
                StatusCardItem(
                    key = "battery",
                    title = context.getString(R.string.tab_battery),
                    icon = Icons.Outlined.BatteryStd,
                    value = context.getString(R.string.unit_percent, data.batteryLevel),
                    subtext = "${data.batteryWattage} | ${data.batteryTemp}",
                    progress = data.batteryLevel / 100f,
                    isCharging = data.isCharging,
                    containerColor = tertiaryContainer,
                    contentColor = onTertiaryContainer,
                    onClick = onBatteryClick
                ),
                StatusCardItem(
                    key = "refresh_rate",
                    title = context.getString(R.string.refresh_rate),
                    icon = Icons.Outlined.Refresh,
                    value = context.getString(R.string.fps_format, rt.currentRefreshRate),
                    subtext = context.getString(R.string.live_monitoring),
                    containerColor = secondaryContainer,
                    contentColor = onSecondaryContainer,
                    onClick = onDisplayClick
                ),
                StatusCardItem(
                    key = "color_depth",
                    title = context.getString(R.string.color_depth),
                    icon = Icons.Outlined.Palette,
                    value = data.colorDepth,
                    subtext = data.colorDepthSubtext,
                    containerColor = primaryContainer,
                    contentColor = onPrimaryContainer,
                    onClick = onDisplayClick
                ),
                StatusCardItem(
                    key = "drm",
                    title = context.getString(R.string.drm_status),
                    icon = Icons.Outlined.VerifiedUser,
                    value = data.widevineLevel,
                    subtext = context.getString(R.string.google_widevine),
                    containerColor = inverseSurface,
                    contentColor = inverseOnSurface,
                    onClick = onDrmClick
                ),
                StatusCardItem(
                    key = "download",
                    title = context.getString(R.string.tab_network) + context.getString(R.string.network_dl),
                    icon = Icons.Default.ArrowDownward,
                    value = rt.downloadSpeed,
                    subtext = context.getString(R.string.tab_network),
                    containerColor = primary,
                    contentColor = onPrimary,
                    onClick = onWifiClick
                ),
                StatusCardItem(
                    key = "upload",
                    title = context.getString(R.string.tab_network) + context.getString(R.string.network_ul),
                    icon = Icons.Default.ArrowUpward,
                    value = rt.uploadSpeed,
                    subtext = context.getString(R.string.tab_network),
                    containerColor = secondary,
                    contentColor = onSecondary,
                    onClick = onWifiClick
                ),
                StatusCardItem(
                    key = "sensors",
                    title = "",
                    icon = Icons.Outlined.Sensors,
                    value = data.sensorsCount.toString(),
                    subtext = context.getString(R.string.tab_sensors),
                    containerColor = tertiary,
                    contentColor = onTertiary,
                    onClick = onSensorsClick,
                    isCounter = true
                ),
                StatusCardItem(
                    key = "apps",
                    title = "",
                    icon = Icons.Outlined.Apps,
                    value = data.appsCount.toString(),
                    subtext = context.getString(R.string.tab_apps),
                    containerColor = primaryContainer,
                    contentColor = onPrimaryContainer,
                    onClick = onAppsClick,
                    isCounter = true
                )
            ).distinctBy { it.key }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isEditing) {
                        Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            resizingCardId = null
                        }
                    } else Modifier
                )
        ) {
            val activeCardOrder = remember(localCardOrder, statusItems) {
                val baseItems = listOf("ram", "cpu", "gpu", "cpu_freqs", "system_health", "features")
                val statusKeys = statusItems.map { it.key }
                val allKnownKeys = (baseItems + statusKeys).toSet()
                
                // Keep order from localCardOrder, but only for items that exist in current state
                val existingInOrder = localCardOrder.filter { it in allKnownKeys }
                
                // Find items that are in current state but NOT in localCardOrder yet
                val missingFromOrder = (baseItems + statusKeys).filter { it !in existingInOrder.toSet() }
                
                (existingInOrder + missingFromOrder).distinct()
            }

            key(isEditing, localCardSizes) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(gridColumns),
                    contentPadding = PaddingValues(
                        start = if (isWideScreen) 24.dp else 16.dp,
                        top = if (isWideScreen) 24.dp else 16.dp,
                        end = if (isWideScreen) 24.dp else 16.dp,
                        bottom = bottomContentPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1280.dp)
                        .align(Alignment.TopCenter)
                ) {
                    items(activeCardOrder, key = { it }, span = { key ->
                        val size = localCardSizes[key] ?: CardSize.SIZE_2x2
                        GridItemSpan(getSpanCount(size))
                    }) { key ->
                        val cardSize = localCardSizes[key] ?: CardSize.SIZE_2x2

                        val onReorderDragCallback: (Offset) -> Offset = { offset ->
                            val layoutInfo = gridState.layoutInfo
                            val visibleItems = layoutInfo.visibleItemsInfo
                            val draggedItem = visibleItems.firstOrNull { it.key == key }
                            var adjustedOffset = offset
                            if (draggedItem != null) {
                                val draggedCenter = Offset(
                                    x = draggedItem.offset.x + draggedItem.size.width / 2f + offset.x,
                                    y = draggedItem.offset.y + draggedItem.size.height / 2f + offset.y
                                )
                                val targetItem = visibleItems.firstOrNull { item ->
                                    item.key != key &&
                                    draggedCenter.x >= item.offset.x &&
                                    draggedCenter.x <= item.offset.x + item.size.width &&
                                    draggedCenter.y >= item.offset.y &&
                                    draggedCenter.y <= item.offset.y + item.size.height
                                }
                                if (targetItem != null) {
                                    val targetKey = targetItem.key as? String
                                    if (targetKey != null && targetKey != key) {
                                        val currentList = localCardOrder
                                        val draggedIndex = currentList.indexOf(key)
                                        val targetIndex = currentList.indexOf(targetKey)
                                        if (draggedIndex != -1 && targetIndex != -1) {
                                            val newList = currentList.toMutableList()
                                            java.util.Collections.swap(newList, draggedIndex, targetIndex)
                                            
                                            val dx = targetItem.offset.x - draggedItem.offset.x
                                            val dy = targetItem.offset.y - draggedItem.offset.y
                                            adjustedOffset = Offset(offset.x - dx, offset.y - dy)
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            localCardOrder = newList
                                        }
                                    }
                                }
                            }
                            adjustedOffset
                        }

                        val onReorderEndCallback = {}

                        when (key) {
                            "ram" -> {
                                val ramSize = localCardSizes["ram"] ?: CardSize.SIZE_4x2
                                ResizableCardContainer(
                                    cardId = "ram",
                                    currentSize = ramSize,
                                    isResizingMode = isEditing,
                                    isResizeHandleVisible = resizingCardId == "ram",
                                    onSizeChanged = { newSize -> 
                                        localCardSizes += ("ram" to newSize)
                                    },
                                    onLongClick = {
                                        resizingCardId = "ram"
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onClick = onMemoryClick,
                                    onReorderDrag = onReorderDragCallback,
                                    onReorderEnd = onReorderEndCallback,
                                    gridColumns = gridColumns) { size ->
                                    DashboardRamCard(total = rt.ramTotal, used = rt.ramUsed, history = rt.ramHistory, size = size)
                                }
                            }
                            "cpu" -> {
                                val cpuSize = localCardSizes["cpu"] ?: CardSize.SIZE_2x2
                                ResizableCardContainer(
                                    cardId = "cpu",
                                    currentSize = cpuSize,
                                    isResizingMode = isEditing,
                                    isResizeHandleVisible = resizingCardId == "cpu",
                                    onSizeChanged = { newSize -> 
                                        localCardSizes += ("cpu" to newSize)
                                    },
                                    onLongClick = {
                                        resizingCardId = "cpu"
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onClick = onSoCClick,
                                    onReorderDrag = onReorderDragCallback,
                                    onReorderEnd = onReorderEndCallback,
                                    gridColumns = gridColumns) { size ->
                                    DashboardCpuCard(usage = rt.cpuUsage, history = rt.cpuHistory, size = size)
                                }
                            }
                            "gpu" -> {
                                val gpuSize = localCardSizes["gpu"] ?: CardSize.SIZE_2x2
                                ResizableCardContainer(
                                    cardId = "gpu",
                                    currentSize = gpuSize,
                                    isResizingMode = isEditing,
                                    isResizeHandleVisible = resizingCardId == "gpu",
                                    onSizeChanged = { newSize -> 
                                        localCardSizes += ("gpu" to newSize)
                                    },
                                    onLongClick = {
                                        resizingCardId = "gpu"
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onClick = onSoCClick,
                                    onReorderDrag = onReorderDragCallback,
                                    onReorderEnd = onReorderEndCallback,
                                    gridColumns = gridColumns) { size ->
                                    DashboardGpuCard(usage = rt.gpuUsage, history = rt.gpuHistory, size = size)
                                }
                            }
                            "cpu_freqs" -> {
                                val cpuFreqsSize = localCardSizes["cpu_freqs"] ?: CardSize.SIZE_4x2
                                ResizableCardContainer(
                                    cardId = "cpu_freqs",
                                    currentSize = cpuFreqsSize,
                                    isResizingMode = isEditing,
                                    isResizeHandleVisible = resizingCardId == "cpu_freqs",
                                    onSizeChanged = { newSize -> 
                                        localCardSizes += ("cpu_freqs" to newSize)
                                    },
                                    onLongClick = {
                                        resizingCardId = "cpu_freqs"
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onClick = {},
                                    onReorderDrag = onReorderDragCallback,
                                    onReorderEnd = onReorderEndCallback,
                                    gridColumns = gridColumns) { size ->
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            contentColor = primary
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Outlined.Speed, null, tint = primary, modifier = Modifier.size(22.dp))
                                                if (size != CardSize.SIZE_1x1) {
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        stringResource(R.string.cpu_frequencies),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = primary,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                }
                                            }
                                            if (size != CardSize.SIZE_1x1 && size != CardSize.SIZE_2x1) {
                                                if (size == CardSize.SIZE_4x4) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        rt.cpuCoreFrequencies.withIndex().chunked(4).forEach { chunk ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                chunk.forEach { indexedFreq ->
                                                                    CpuCoreBox(
                                                                        index = indexedFreq.index,
                                                                        freq = indexedFreq.value,
                                                                        history = rt.cpuCoreHistory.getOrNull(indexedFreq.index) ?: emptyList(),
                                                                        modifier = Modifier.weight(1f)
                                                                    )
                                                                }
                                                                repeat(4 - chunk.size) {
                                                                    Spacer(modifier = Modifier.weight(1f))
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else if (size == CardSize.SIZE_4x2) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    val clusters = socInfoState?.cpuClusters ?: emptyList()
                                                    if (clusters.isNotEmpty()) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            clusters.forEachIndexed { clusterIdx, cluster ->
                                                                val coresFreqs = cluster.coreIndices.map { coreIdx ->
                                                                    rt.cpuCoreFrequencies.getOrNull(coreIdx) ?: 0L
                                                                }
                                                                val clusterFreq = coresFreqs.maxOrNull() ?: 0L
                                                                val clusterHistory = rt.clusterHistories.getOrNull(clusterIdx) ?: emptyList()
                                                                CpuClusterLiveBox(
                                                                    name = cluster.name,
                                                                    freq = clusterFreq,
                                                                    history = clusterHistory,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                            Text(stringResource(R.string.unknown), style = MaterialTheme.typography.bodyMedium)
                                                        }
                                                    }
                                                } else if (size == CardSize.SIZE_2x2) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        val cpuUtil = rt.cpuUsage
                                                        MiniGraphBox(
                                                            title = stringResource(R.string.cpu_utilisation),
                                                            value = "$cpuUtil%",
                                                            icon = Icons.Outlined.Memory,
                                                            history = rt.cpuHistory,
                                                            modifier = Modifier.weight(1f),
                                                            maxVal = 100f
                                                        )
                                                        val cpuTemp = rt.cpuTemperature
                                                        val cpuTempStr = if (cpuTemp > 0f) "${"%.1f".format(cpuTemp)}°C" else "N/A"
                                                        MiniGraphBox(
                                                            title = stringResource(R.string.tab_thermal),
                                                            value = cpuTempStr,
                                                            icon = Icons.Outlined.Thermostat,
                                                            history = rt.cpuTempHistory,
                                                            modifier = Modifier.weight(1f),
                                                            maxVal = 100f
                                                        )
                                                    }
                                                }
                                            } else if (size == CardSize.SIZE_2x1) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    stringResource(R.string.max_freq_format, rt.cpuCoreFrequencies.maxOrNull() ?: 0),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "${(rt.cpuCoreFrequencies.maxOrNull() ?: 0) / 1000f}G",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            "system_health" -> {
                                val systemHealthSize = localCardSizes["system_health"] ?: CardSize.SIZE_4x2
                                ResizableCardContainer(
                                    cardId = "system_health",
                                    currentSize = systemHealthSize,
                                    isResizingMode = isEditing,
                                    isResizeHandleVisible = resizingCardId == "system_health",
                                    onSizeChanged = { newSize -> 
                                        localCardSizes += ("system_health" to newSize)
                                    },
                                    onLongClick = {
                                        resizingCardId = "system_health"
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onClick = {},
                                    onReorderDrag = onReorderDragCallback,
                                    onReorderEnd = onReorderEndCallback,
                                    gridColumns = gridColumns) { size ->
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.HealthAndSafety, null, tint = tertiary)
                                                if (size != CardSize.SIZE_1x1) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(stringResource(R.string.system_info), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tertiary)
                                                }
                                            }
                                            if (size != CardSize.SIZE_1x1 && size != CardSize.SIZE_2x1) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(stringResource(R.string.storage_smart_status) + ": " + stringResource(data.storageSmartStatus), style = MaterialTheme.typography.bodyMedium)
                                            } else if (size == CardSize.SIZE_2x1) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("SMART: " + stringResource(data.storageSmartStatus), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            } else {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("OK", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            "features" -> {
                                val usbStorageStr = stringResource(R.string.usb_storage)
                                val usbAccessoryStr = stringResource(R.string.usb_accessory)
                                val nfcStr = stringResource(R.string.nfc_support)
                                val secureNfcStr = stringResource(R.string.secure_nfc)
                                val faceStr = stringResource(R.string.face_recognition)
                                val irisStr = stringResource(R.string.iris_scanner)
                                val infraredStr = stringResource(R.string.infrared)
                                val uwbStr = stringResource(R.string.uwb)
                                val gpsStr = stringResource(R.string.gps_support)
                                
                                val features = remember(data.usbStorage, data.usbAccessory, data.nfc, data.secureNfc, data.faceRecognition, data.irisScanner, data.infrared, data.uwb, data.gps) {
                                    listOf(
                                        Triple(usbStorageStr, Icons.Default.Usb, data.usbStorage),
                                        Triple(usbAccessoryStr, Icons.Default.Usb, data.usbAccessory),
                                        Triple(nfcStr, Icons.Default.Nfc, data.nfc),
                                        Triple(secureNfcStr, Icons.Default.Security, data.secureNfc),
                                        Triple(faceStr, Icons.Default.Face, data.faceRecognition),
                                        Triple(irisStr, Icons.Default.RemoveRedEye, data.irisScanner),
                                        Triple(infraredStr, Icons.Default.SettingsRemote, data.infrared),
                                        Triple(uwbStr, Icons.Default.WifiTethering, data.uwb),
                                        Triple(gpsStr, Icons.Default.GpsFixed, data.gps)
                                    )
                                }
                                
                                val featuresSize = localCardSizes["features"] ?: CardSize.SIZE_4x2
                                ResizableCardContainer(
                                    cardId = "features",
                                    currentSize = featuresSize,
                                    isResizingMode = isEditing,
                                    isResizeHandleVisible = resizingCardId == "features",
                                    onSizeChanged = { newSize -> 
                                        localCardSizes += ("features" to newSize)
                                    },
                                    onLongClick = {
                                        resizingCardId = "features"
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onClick = {},
                                    onReorderDrag = onReorderDragCallback,
                                    onReorderEnd = onReorderEndCallback,
                                    gridColumns = gridColumns) { size ->

                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    ) {
                                        val style = when (size) {
                                            CardSize.SIZE_4x4 -> FeatureCardStyle.SQUARE
                                            CardSize.SIZE_4x2 -> FeatureCardStyle.RECTANGLE
                                            else -> FeatureCardStyle.COMPACT_ROW
                                        }
                                        val showTitle = false
                                        val featureColumns = if (size == CardSize.SIZE_4x2 || size == CardSize.SIZE_4x4) {
                                            if (isWideScreen) 5 else 3
                                        } else 2
                                        val spacing = when (style) {
                                            FeatureCardStyle.SQUARE -> 12.dp
                                            FeatureCardStyle.RECTANGLE -> 6.dp
                                            FeatureCardStyle.COMPACT_ROW -> 4.dp
                                        }
                                        val outerPadding = when (style) {
                                            FeatureCardStyle.SQUARE -> 16.dp
                                            FeatureCardStyle.RECTANGLE -> 8.dp
                                            FeatureCardStyle.COMPACT_ROW -> 6.dp
                                        }
                                        Column(modifier = Modifier.padding(outerPadding).fillMaxSize()) {
                                            if (showTitle) {
                                                Text(stringResource(R.string.miscellaneous), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primary)
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }
                                            
                                            if (size != CardSize.SIZE_1x1 && size != CardSize.SIZE_2x1) {
                                                 val scrollState = rememberScrollState()
                                                 Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                                     Column(
                                                         modifier = Modifier
                                                             .fillMaxWidth()
                                                             .verticalScroll(scrollState),
                                                         verticalArrangement = Arrangement.spacedBy(spacing)
                                                     ) {
                                                         features.chunked(featureColumns).forEach { chunk ->
                                                             Row(
                                                                 modifier = Modifier.fillMaxWidth(),
                                                                 horizontalArrangement = Arrangement.spacedBy(spacing)
                                                             ) {
                                                                 chunk.forEach { (t, ic, s) ->
                                                                     DashboardFeatureCard(
                                                                         modifier = Modifier.weight(1f),
                                                                         title = t,
                                                                         icon = ic,
                                                                         isSupported = s,
                                                                         style = style
                                                                     )
                                                                 }
                                                                 repeat(featureColumns - chunk.size) {
                                                                     Spacer(modifier = Modifier.weight(1f))
                                                                 }
                                                             }
                                                         }
                                                     }
                                                 }
                                            } else if (size == CardSize.SIZE_2x1) {
                                                val supportedCount = features.count { it.third }
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                                                    Icon(Icons.Default.Star, null, tint = primary)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("$supportedCount / ${features.size} Features", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                val supportedCount = features.count { it.third }
                                                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                    Icon(Icons.Default.Star, null, tint = primary)
                                                    Text("$supportedCount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "wifi" -> {
                                val item = statusItems.firstOrNull { it.key == "wifi" }
                                if (item != null) {
                                    val size = localCardSizes["wifi"] ?: CardSize.SIZE_2x2
                                    ResizableCardContainer(
                                        cardId = "wifi",
                                        currentSize = size,
                                        isResizingMode = isEditing,
                                        isResizeHandleVisible = resizingCardId == "wifi",
                                        onSizeChanged = { newSize -> 
                                            localCardSizes += ("wifi" to newSize)
                                        },
                                        onLongClick = {
                                            resizingCardId = "wifi"
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onClick = { item.onClick?.invoke() },
                                        onReorderDrag = onReorderDragCallback,
                                        onReorderEnd = onReorderEndCallback,
                                        gridColumns = gridColumns) { sizeParam ->
                                        if (sizeParam == CardSize.SIZE_4x4) {
                                            val bg = item.containerColor.copy(alpha = 1f)
                                            val safeContent = contrastColor(bg)
                                            Card(
                                                modifier = Modifier.fillMaxSize(),
                                                shape = RoundedCornerShape(24.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = bg,
                                                    contentColor = safeContent
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(item.icon, null, modifier = Modifier.size(24.dp), tint = safeContent.copy(alpha = 0.9f))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = item.title,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = safeContent.copy(alpha = 0.85f),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Column(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        AutoSizeText(
                                                            text = translateValue(item.value),
                                                            style = MaterialTheme.typography.headlineMedium,
                                                            fontWeight = FontWeight.Black,
                                                            color = safeContent,
                                                            maxLines = 1,
                                                            textAlign = TextAlign.Center
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = item.subtext,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = safeContent.copy(alpha = 0.8f),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    NetworkSpeedGraphBox(
                                                        downloadSpeed = rt.downloadSpeed,
                                                        uploadSpeed = rt.uploadSpeed,
                                                        downloadHistory = rt.downloadSpeedHistory,
                                                        uploadHistory = rt.uploadSpeedHistory,
                                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                                        containerColor = if (safeContent == Color.White) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f),
                                                        contentColor = safeContent,
                                                        lineColorDl = safeContent,
                                                        lineColorUl = safeContent.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        } else {
                                            DashboardStatusCard(
                                                title = item.title,
                                                icon = item.icon,
                                                value = item.value,
                                                subtext = item.subtext,
                                                progress = item.progress,
                                                isCharging = item.isCharging,
                                                containerColor = item.containerColor,
                                                contentColor = item.contentColor,
                                                size = sizeParam
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                val item = statusItems.firstOrNull { it.key == key }
                                if (item != null) {
                                    val size = localCardSizes[key] ?: CardSize.SIZE_2x2
                                    ResizableCardContainer(
                                        cardId = key,
                                        currentSize = size,
                                        isResizingMode = isEditing,
                                        isResizeHandleVisible = resizingCardId == key,
                                        onSizeChanged = { newSize -> 
                                            localCardSizes += (key to newSize)
                                        },
                                        onLongClick = {
                                            resizingCardId = key
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onClick = { item.onClick?.invoke() },
                                        onReorderDrag = onReorderDragCallback,
                                        onReorderEnd = onReorderEndCallback,
                                        gridColumns = gridColumns) { sizeParam ->

                                        if (item.isCounter) {
                                            DashboardCounterCard(
                                                icon = item.icon,
                                                count = item.value,
                                                label = item.subtext,
                                                containerColor = item.containerColor,
                                                contentColor = item.contentColor,
                                                size = sizeParam
                                            )
                                        } else {
                                            DashboardStatusCard(
                                                title = item.title,
                                                icon = item.icon,
                                                value = item.value,
                                                subtext = item.subtext,
                                                progress = item.progress,
                                                isCharging = item.isCharging,
                                                containerColor = item.containerColor,
                                                contentColor = item.contentColor,
                                                size = sizeParam
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Done FAB button (ColorOS style)
            if (isEditing) {
                ExtendedFloatingActionButton(
                    text = { Text("Done", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Check, null) },
                    onClick = { resizingCardId = null },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomContentPadding + 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceSummaryCard(
    modifier: Modifier = Modifier,
    model: String,
    manufacturer: String,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    cardId: String? = null
) {
    val logoUrl = remember(manufacturer, model) {
        val m = manufacturer.lowercase().trim()
        val md = model.lowercase().trim()
        if (m.contains("bkav") || md.contains("bphone")) {
            "https://gsmfind.com/img/brand/bphone.png"
        } else {
            val domain = when {
                m.contains("motorola") -> "motorola.com"
                m.contains("samsung") -> "samsung.com"
                m.contains("google") -> "google.com"
                m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> "xiaomi.com"
                m.contains("oneplus") -> "oneplus.com"
                m.contains("oppo") -> "oppo.com"
                m.contains("vivo") -> "vivo.com"
                m.contains("realme") -> "realme.com"
                m.contains("nothing") -> "nothing.tech"
                m.contains("huawei") -> "huawei.com"
                m.contains("honor") -> "hihonor.com"
                m.contains("asus") -> "asus.com"
                m.contains("meizu") -> "meizu.com"
                m.contains("zte") -> "zte.com.cn"
                m.contains("nubia") -> "nubia.com"
                m.contains("sony") -> "sony.com"
                m.contains("lg") -> "lg.com"
                m.contains("htc") -> "htc.com"
                m.contains("lenovo") -> "lenovo.com"
                else -> "$m.com"
            }
            "https://logos.hunter.io/$domain"
        }
    }
    val context = LocalContext.current
    val imageRequest = remember(logoUrl) {
        ImageRequest.Builder(context)
            .data(logoUrl)
            .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .crossfade(true)
            .build()
    }
    val handler = LocalCardInteractionHandler.current

    val finalModifier = modifier.then(
        if (cardId != null && handler != null) {
            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = {},
                onLongClick = {
                    handler.triggerLongPress(cardId)
                }
            )
        } else Modifier
    )

    ElevatedCard(
        modifier = finalModifier,
        shape = ShapeCard,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "$manufacturer logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.tab_device),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AutoSizeText(
                        text = model,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = contentColor,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = manufacturer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            if (cardId != null && handler != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .alpha(0.5f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            handler.showCardInfo(cardId)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = contentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProcessorSummaryCard(
    modifier: Modifier = Modifier,
    socName: String,
    vendor: String,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    cardId: String? = null
) {
    val logoUrl = remember(socName, vendor) {
        val s = socName.lowercase().trim()
        val v = vendor.lowercase().trim()
        when {
            s.contains("snapdragon") || s.contains("dragonwing") || v.contains("qualcomm") -> {
                "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Snapdragon_Logo.svg/250px-Snapdragon_Logo.svg.png"
            }
            s.contains("dimensity") || s.contains("helio") || v.contains("mediatek") -> {
                "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5f/MediaTek_logo.svg/250px-MediaTek_logo.svg.png"
            }
            s.contains("exynos") || v.contains("samsung") -> {
                "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Exynos_Logo.svg/250px-Exynos_Logo.svg.png"
            }
            s.contains("kirin") || s.contains("hisilicon") || v.contains("hisilicon") || v.contains("huawei") -> {
                "https://upload.wikimedia.org/wikipedia/commons/7/7b/Hisilicon_logo.png"
            }
            s.contains("xring") || s.contains("surge") || v.contains("xiaomi") -> {
                "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/Xiaomi_logo_%282021-%29.svg/250px-Xiaomi_logo_%282021-%29.svg.png"
            }
            s.contains("unisoc") || s.contains("spreadtrum") || v.contains("unisoc") || v.contains("spreadtrum") -> {
                "https://upload.wikimedia.org/wikipedia/commons/d/d5/Unisoc_Logo.png"
            }
            s.contains("tensor") || v.contains("google") -> {
                "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2f/Google_2015_logo.svg/500px-Google_2015_logo.svg.png"
            }
            else -> {
                "https://logos.hunter.io/${v.replace(" ", "")}.com"
            }
        }
    }

    val context = LocalContext.current
    val imageRequest = remember(logoUrl) {
        ImageRequest.Builder(context)
            .data(logoUrl)
            .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .crossfade(true)
            .build()
    }
    val handler = LocalCardInteractionHandler.current

    val finalModifier = modifier.then(
        if (cardId != null && handler != null) {
            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = {},
                onLongClick = {
                    handler.triggerLongPress(cardId)
                }
            )
        } else Modifier
    )

    ElevatedCard(
        modifier = finalModifier,
        shape = ShapeCard,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "$vendor logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.processor),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AutoSizeText(
                        text = socName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = contentColor,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = vendor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            if (cardId != null && handler != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .alpha(0.5f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            handler.showCardInfo(cardId)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = contentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable fun DeviceTab(viewModel: DeviceInfoViewModel) {
    val summary by viewModel.deviceSummary.collectAsStateWithLifecycle()
    val system by viewModel.systemInfo.collectAsStateWithLifecycle()
    val cpu by viewModel.cpuInfo.collectAsStateWithLifecycle()
    val socName = cpu?.processor ?: stringResource(id = R.string.unknown)
    summary?.let { s -> 
        LazyColumn(contentPadding = PaddingValues(16.dp)) { 
            item { 
                DeviceSummaryCard(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    model = s.model,
                    manufacturer = s.manufacturer,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    cardId = "device_model"
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { 
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { 
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(id = R.string.android),
                        icon = Icons.Outlined.Android,
                        value = stringResource(R.string.android_version_format, s.androidVersion),
                        subtext = stringResource(R.string.version),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "android_version"
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = "OS",
                        icon = Icons.Outlined.AutoAwesome,
                        value = system?.osVersion ?: stringResource(R.string.unknown),
                        subtext = stringResource(R.string.ui_version),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "os_version"
                    )
                } 
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                InfoGroupCard(
                    title = stringResource(R.string.hardware_platform),
                    icon = Icons.Outlined.SettingsInputComponent,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    cardId = "group_hardware_platform"
                ) {
                    InfoRow(stringResource(R.string.model), s.model)
                    system?.let { InfoRow(stringResource(R.string.model_name), it.modelName) }
                    InfoRow(stringResource(R.string.board), s.board)
                    InfoRow(stringResource(R.string.platform), s.platform)
                    InfoRow(stringResource(R.string.hardware), s.hardware)
                    InfoRow(stringResource(R.string.processor), socName)
                    InfoRow(stringResource(R.string.tab_memory), s.ramType)
                    InfoRow(stringResource(R.string.flash), s.flashType)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                InfoGroupCard(
                    title = stringResource(R.string.tab_system),
                    icon = Icons.Outlined.Android,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    cardId = "group_system"
                ) {
                    InfoRow(stringResource(R.string.android), s.androidVersion)
                    system?.let {
                        InfoRow(stringResource(R.string.api), it.sdkLevel)
                        InfoRow(stringResource(R.string.codename), it.codeName)
                        InfoRow(stringResource(R.string.security), it.securityPatch)
                    }
                    InfoRow(stringResource(R.string.kernel), s.kernel)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                InfoGroupCard(
                    title = stringResource(R.string.build_details),
                    icon = Icons.Outlined.Build,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                    cardId = "group_build_details"
                ) {
                    system?.let {
                        InfoRow(stringResource(R.string.build), it.buildId)
                        InfoRow(stringResource(R.string.java_vm), it.javaVm)
                        InfoRow(stringResource(R.string.baseband), it.baseband)
                        InfoRow(stringResource(R.string.bootloader), it.bootloader)
                        InfoRow(stringResource(R.string.gps), it.gps)
                        InfoRow(stringResource(R.string.bluetooth), it.bluetoothVersion)
                        InfoRow(stringResource(R.string.build_type), it.buildType)
                        InfoRow(stringResource(R.string.tags), it.tags)
                        InfoRow(stringResource(R.string.incremental), it.incremental)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                InfoGroupCard(
                    title = stringResource(R.string.screen),
                    icon = Icons.Outlined.DisplaySettings,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    cardId = "group_screen"
                ) {
                    InfoRow(stringResource(R.string.resolution), s.resolution)
                    InfoRow(stringResource(R.string.touchscreen), s.touchscreen)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                InfoGroupCard(
                    title = stringResource(R.string.system_identifiers),
                    icon = Icons.Outlined.Fingerprint,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cardId = "group_system_identifiers"
                ) {
                    InfoRow(stringResource(R.string.android_id), s.androidId)
                    s.gsfId?.let { gsf -> InfoRow(stringResource(R.string.gsf_id), gsf) }
                    InfoRow(stringResource(R.string.tab_device), s.device)
                    system?.let { InfoRow(stringResource(R.string.product), it.product) }
                    InfoRow(stringResource(R.string.fingerprint), s.buildFingerprint)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                InfoGroupCard(
                    title = stringResource(R.string.miscellaneous),
                    icon = Icons.Outlined.Dashboard,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    cardId = "group_miscellaneous"
                ) {
                    system?.let {
                        InfoRow(stringResource(R.string.gms), it.googlePlayServices)
                        InfoRow(stringResource(R.string.device_features), it.deviceFeatures)
                        InfoRow(stringResource(R.string.language), it.language)
                        InfoRow(stringResource(R.string.timezone), it.timezone)
                        InfoRow(stringResource(R.string.uptime), it.uptime)
                    }
                }
            }
        }
    }
}

enum class SocTabType { CPU, GPU }

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SocTab(v: DeviceInfoViewModel) {
    var showCoreDetails by remember { mutableStateOf(false) }
    
    AnimatedContent(
        targetState = showCoreDetails,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "soc_tab_details_transition"
    ) { targetShowDetails ->
        if (targetShowDetails) {
            CpuCoresDetailScreen(v, onBack = { showCoreDetails = false })
        } else {
            var activeTab by remember { mutableStateOf(SocTabType.CPU) }
        val info by v.socInfo.collectAsStateWithLifecycle(); val socName = info?.processor ?: stringResource(id = R.string.unknown)
        val realtimeDataState by v.dashboardRealtime.collectAsStateWithLifecycle()
        info?.let { i -> 
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = activeTab == SocTabType.CPU,
                            onClick = { activeTab = SocTabType.CPU },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text(stringResource(R.string.cpu)) }
                        )
                        SegmentedButton(
                            selected = activeTab == SocTabType.GPU,
                            onClick = { activeTab = SocTabType.GPU },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text(stringResource(R.string.gpu)) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                if (activeTab == SocTabType.CPU) {
                    item {
                        ProcessorSummaryCard(
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            socName = socName,
                            vendor = i.vendor,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            cardId = "soc"
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        DashboardStatusCard(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            title = stringResource(R.string.cores),
                            icon = Icons.Outlined.Numbers,
                            value = i.cores,
                            subtext = i.bigLittle,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            size = CardSize.SIZE_4x2,
                            cardId = "cores"
                        )
                    }
                    if (i.cpuClusters.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                        item {
                            InfoGroupCard(
                                title = stringResource(R.string.clusters),
                                icon = Icons.Outlined.Layers,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                onClick = { showCoreDetails = true },
                                cardId = "group_cpu"
                            ) {
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    items(i.cpuClusters) { cluster -> CpuClusterBox(cluster) }
                                }
                            }
                        }
                    }

                    // ── Live CPU Cluster Utilisation ──
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item {
                        Text(
                            stringResource(R.string.cpu_frequencies),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    if (i.cpuClusters.isNotEmpty()) {
                        realtimeDataState?.let { realtimeData ->
                            i.cpuClusters.forEachIndexed { clusterIdx, cluster ->
                                item {
                                    InfoGroupCard(
                                        "${cluster.name} (${cluster.coreCount} cores)",
                                        Icons.Outlined.Speed,
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f + clusterIdx * 0.1f)
                                    ) {
                                        Text(
                                            "${cluster.architecture} · ${cluster.minFreq} - ${cluster.maxFreq}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        // Cluster aggregate sparkline
                                        val clusterHistory = realtimeData.clusterHistories.getOrNull(clusterIdx)
                                        if (clusterHistory != null && clusterHistory.isNotEmpty()) {
                                            val clusterAvg = clusterHistory.lastOrNull()?.toInt() ?: 0
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                Text("${clusterAvg}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                val chartColor = MaterialTheme.colorScheme.primary
                                                ClusterSparkline(
                                                    clusterHistory = clusterHistory,
                                                    chartColor = chartColor,
                                                    modifier = Modifier.weight(1f).height(40.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        // Per-core freq boxes within cluster
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            cluster.coreIndices.forEach { coreIdx ->
                                                val coreFreq = realtimeData.cpuCoreFrequencies.getOrNull(coreIdx) ?: 0L
                                                val coreHistory = realtimeData.cpuCoreHistory.getOrNull(coreIdx) ?: emptyList()
                                                CpuCoreBox(
                                                    index = coreIdx,
                                                    freq = coreFreq,
                                                    history = coreHistory,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(12.dp)) }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    item { 
                        InfoGroupCard(
                            stringResource(R.string.cpu), 
                            Icons.Outlined.Memory, 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            cardId = "group_cpu"
                        ) { 
                            InfoRow(stringResource(R.string.processor), socName)
                            InfoRow(stringResource(R.string.cpu), i.processor)
                            InfoRow(stringResource(R.string.vendor), i.vendor)
                            InfoRow(stringResource(R.string.cores), i.cores)
                            InfoRow(stringResource(R.string.big_little), i.bigLittle)
                            if (i.cpuClusters.isEmpty()) {
                                InfoRow(stringResource(R.string.clusters), i.clusters) 
                            }
                            InfoRow(stringResource(R.string.family), i.family)
                            InfoRow(stringResource(R.string.mode), i.mode)
                            InfoRow(stringResource(R.string.machine), i.machine)
                            InfoRow(stringResource(R.string.abi), i.abi)
                            InfoRow(stringResource(R.string.revision), i.revision)
                            InfoRow(stringResource(R.string.clock_speed), i.clockSpeed)
                            InfoRow(stringResource(R.string.governor), i.governor)
                            InfoRow(stringResource(R.string.supported_abi), i.supportedAbi) 
                        } 
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item {
                        InfoGroupCard(
                            stringResource(R.string.instructions),
                            Icons.Outlined.Terminal,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.primary,
                            cardId = "group_instructions"
                        ) {
                            val instructionsList = i.instructions.split(Regex("[,\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                instructionsList.forEach { instruction ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = ShapeSmall,
                                    ) {
                                        Text(
                                            text = instruction,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { 
                        InfoGroupCard(
                            stringResource(R.string.technology), 
                            Icons.Outlined.Science, 
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), 
                            MaterialTheme.colorScheme.tertiary,
                            cardId = "group_technology"
                        ) { 
                            InfoRow(stringResource(R.string.process), i.process) 
                        } 
                    }
                } else {
                    item {
                        DashboardStatusCard(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            title = stringResource(R.string.gpu),
                            icon = Icons.Outlined.GraphicEq,
                            value = i.gpu,
                            subtext = i.gpuVendor,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            size = CardSize.SIZE_4x2,
                            cardId = "gpu"
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        realtimeDataState?.let { realtimeData ->
                            val gpuProgress by animateFloatAsState(realtimeData.gpuUsage / 100f, tween(400), label = "socGpu")
                            InfoGroupCard(
                                stringResource(R.string.gpu_utilisation),
                                Icons.Outlined.GraphicEq,
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.tertiary,
                                cardId = "gpu"
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                        CircularProgressIndicator(
                                            progress = { gpuProgress },
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.tertiary,
                                            strokeWidth = 6.dp,
                                            trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                            strokeCap = StrokeCap.Round
                                        )
                                        Text("${realtimeData.gpuUsage}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Freq: ${"%.0f".format(realtimeData.gpuFreq)} MHz", style = MaterialTheme.typography.bodyMedium)
                                        Text("Memory: ${"%.0f".format(realtimeData.gpuMemory)} MB", style = MaterialTheme.typography.bodyMedium)
                                        Text("Temp: ${"%.1f".format(realtimeData.gpuTemperature)}°C", style = MaterialTheme.typography.bodyMedium)
                                        if (realtimeData.cpuTemperature > 0) {
                                            Text("CPU Temp: ${"%.1f".format(realtimeData.cpuTemperature)}°C", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                                // GPU Frequency History Chart
                                if (realtimeData.gpuFreqHistory.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("GPU Frequency History", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val chartColor = MaterialTheme.colorScheme.tertiary
                                    val gpuFreqPath = remember { Path() }
                                    val gpuFreqFillPath = remember { Path() }
                                    val density = LocalDensity.current
                                    val gpuStroke = remember(density) { Stroke(with(density) { 2.dp.toPx() }, cap = StrokeCap.Round) }
                                    val history = realtimeData.gpuFreqHistory
                                    val maxVal = remember(history) { history.maxOrNull()?.coerceAtLeast(1f) ?: 1f }
                                    Canvas(modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                        gpuFreqPath.reset()
                                        val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                                        history.forEachIndexed { gi, gv ->
                                            val x = gi * stepX
                                            val y = size.height - (gv / maxVal * size.height)
                                            if (gi == 0) gpuFreqPath.moveTo(x, y) else gpuFreqPath.lineTo(x, y)
                                        }
                                        if (history.size > 1) {
                                            gpuFreqFillPath.reset()
                                            gpuFreqFillPath.addPath(gpuFreqPath)
                                            gpuFreqFillPath.lineTo((history.size - 1) * stepX, size.height)
                                            gpuFreqFillPath.lineTo(0f, size.height)
                                            gpuFreqFillPath.close()
                                            drawPath(gpuFreqFillPath, chartColor.copy(alpha = 0.1f))
                                            drawPath(gpuFreqPath, chartColor.copy(alpha = 0.5f), style = gpuStroke)
                                        }
                                    }
                                }
                                // GPU Temperature History Chart
                                if (realtimeData.gpuTempHistory.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("GPU Temperature History", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val tempColor = MaterialTheme.colorScheme.error
                                    val gpuTempPath = remember { Path() }
                                    val gpuTempFillPath = remember { Path() }
                                    val density = LocalDensity.current
                                    val gpuStroke = remember(density) { Stroke(with(density) { 2.dp.toPx() }, cap = StrokeCap.Round) }
                                    val history = realtimeData.gpuTempHistory
                                    val maxVal = remember(history) { history.maxOrNull()?.coerceAtLeast(1f) ?: 1f }
                                    Canvas(modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                        gpuTempPath.reset()
                                        val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                                        history.forEachIndexed { gi, gv ->
                                            val x = gi * stepX
                                            val y = size.height - (gv / maxVal * size.height)
                                            if (gi == 0) gpuTempPath.moveTo(x, y) else gpuTempPath.lineTo(x, y)
                                        }
                                        if (history.size > 1) {
                                            gpuTempFillPath.reset()
                                            gpuTempFillPath.addPath(gpuTempPath)
                                            gpuTempFillPath.lineTo((history.size - 1) * stepX, size.height)
                                            gpuTempFillPath.lineTo(0f, size.height)
                                            gpuTempFillPath.close()
                                            drawPath(gpuTempFillPath, tempColor.copy(alpha = 0.1f))
                                            drawPath(gpuTempPath, tempColor.copy(alpha = 0.5f), style = gpuStroke)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { 
                        InfoGroupCard(
                            stringResource(R.string.gpu), 
                            Icons.Outlined.GraphicEq, 
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), 
                            MaterialTheme.colorScheme.secondary,
                            cardId = "gpu"
                        ) { 
                            InfoRow(stringResource(R.string.gpu), i.gpu)
                            InfoRow(stringResource(R.string.vendor), i.gpuVendor)
                            InfoRow(stringResource(R.string.architecture), i.gpuArch)
                            InfoRow(stringResource(R.string.gpu_cores), i.gpuCores)
                            InfoRow(stringResource(R.string.clock_speed), i.gpuClockSpeed)
                            InfoRow(stringResource(R.string.l2_cache), i.gpuL2Cache)
                            InfoRow(stringResource(R.string.bus_width), i.gpuBusWidth)
                            InfoRow(stringResource(R.string.open_gl_es), i.gpuFullVersion)
                            InfoRow(stringResource(R.string.vulkan), i.vulkanVersion)
                            InfoRow(stringResource(R.string.gpu_extensions), i.gpuExtensions) 
                        } 
                    }
                }
            }
        }
    }
}
}

@Composable fun BatteryTab(viewModel: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val info by viewModel.batteryInfo.collectAsStateWithLifecycle()
    val batteryHistory by viewModel.batteryCapacityHistory.collectAsStateWithLifecycle()
    val lastFullChargeTs by viewModel.lastFullChargeTs.collectAsStateWithLifecycle()
    val lastStoppedChargingTs by viewModel.lastStoppedChargingTs.collectAsStateWithLifecycle()
    val dashboardData by viewModel.dashboardData.collectAsStateWithLifecycle()
    val wattageHistory = dashboardData?.wattageHistory ?: emptyList()
    val fullWattageHistory by viewModel.fullWattageHistory.collectAsStateWithLifecycle()
    var showFullWattage by remember { mutableStateOf(false) }
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    if (showFullWattage) {
        WattageFullHistoryDialog(info?.wattage ?: "0.00 W", fullWattageHistory) { showFullWattage = false }
    }

    info?.let { i -> 
        LazyColumn(contentPadding = PaddingValues(16.dp)) { 
            item { 
                val cardColor = if (i.isCharging) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                val contentColor = if (i.isCharging) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                val handler = LocalCardInteractionHandler.current
                val finalCardModifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .then(
                        if (handler != null) {
                            val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            Modifier.combinedClickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = true),
                                onClick = {},
                                onLongClick = {
                                    handler.triggerLongPress("battery_main")
                                }
                            )
                        } else Modifier
                    )
                Card(
                    modifier = finalCardModifier,
                    shape = ShapeExtraLarge,
                    colors = CardDefaults.cardColors(containerColor = cardColor, contentColor = contentColor)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background bubbles (Xiaomi style)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(contentColor.copy(alpha = 0.1f), radius = 20.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.2f))
                            drawCircle(contentColor.copy(alpha = 0.05f), radius = 40.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.7f))
                            drawCircle(contentColor.copy(alpha = 0.08f), radius = 15.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.85f))
                        }
                        
                        // Right side progress bar
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(12.dp)
                                .background(Color.Black.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(i.level / 100f)
                                    .background(contentColor.copy(alpha = 0.3f))
                            )
                        }

                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                val timeRemainingMs = if (i.timeToFull > 0) i.timeToFull else 0L
                                val hours = timeRemainingMs / (1000 * 60 * 60)
                                val minutes = (timeRemainingMs / (1000 * 60)) % 60
                                
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)) {
                                            append(hours.toString())
                                        }
                                        withStyle(style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                                            append("h ")
                                        }
                                        withStyle(style = SpanStyle(fontSize = 64.sp, fontWeight = FontWeight.ExtraBold)) {
                                            append(minutes.toString())
                                        }
                                        withStyle(style = SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                                            append("m")
                                        }
                                    },
                                    color = contentColor,
                                    maxLines = 1
                                )
                                
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                    if (i.isCharging) Icon(Icons.Default.Bolt, null, modifier = Modifier.size(22.dp), tint = contentColor)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (i.isCharging) {
                                            if (i.timeToFull > 0) stringResource(id = R.string.time_to_full) + " | " + stringResource(id = R.string.charged_format, i.level) else stringResource(id = R.string.calculating_charge_time) + " | ${i.level}%"
                                        } else {
                                            if (i.timeToFull > 0) stringResource(id = R.string.remaining_usage_time) + " | " + stringResource(id = R.string.battery_format, i.level) else stringResource(id = R.string.analyzing_usage) + " | " + stringResource(id = R.string.battery_format, i.level)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = contentColor.copy(alpha = 0.95f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (handler != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(end = 20.dp, top = 8.dp)
                                    .size(18.dp)
                                    .alpha(0.5f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        handler.showCardInfo("battery_main")
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "Info",
                                    tint = contentColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                val statusColumns = if (isWideScreen) 3 else 2
                val items = listOf(
                    Triple("Temperature", Icons.Outlined.Thermostat, Pair(i.temperature, stringResource(id = R.string.battery_temp_label))),
                    Triple("Wattage", Icons.Outlined.Bolt, Pair(i.wattage, stringResource(id = R.string.charging_speed_label))),
                    Triple("Power", Icons.Outlined.Power, Pair(i.powerSource, stringResource(id = R.string.source))),
                    Triple("WirelessCharging", Icons.Outlined.Contactless, Pair(if (i.isWirelessSupported) stringResource(id = R.string.supported) else stringResource(id = R.string.not_supported), stringResource(id = R.string.wireless_charging)))
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.chunked(statusColumns).forEach { chunk ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunk.forEach { (title, icon, dataPair) ->
                                val (value, subtext) = dataPair
                                val (containerColor, contentColor) = when(title) {
                                    "Temperature" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                                    "Wattage" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                val cardId = when(title) {
                                    "Temperature" -> "battery_temp"
                                    "Wattage" -> "battery_wattage"
                                    "Power" -> "battery_power"
                                    "WirelessCharging" -> "battery_wireless"
                                    else -> null
                                }
                                DashboardStatusCard(
                                    modifier = Modifier.weight(1f).height(160.dp),
                                    title = when(title) {
                                        "Temperature" -> stringResource(id = R.string.tab_thermal)
                                        "Wattage" -> stringResource(id = R.string.charging_speed)
                                        "Power" -> stringResource(id = R.string.source)
                                        "WirelessCharging" -> stringResource(id = R.string.wireless_charging)
                                        else -> title
                                    },
                                    icon = icon,
                                    value = value,
                                    subtext = subtext,
                                    containerColor = containerColor,
                                    contentColor = contentColor,
                                    size = CardSize.SIZE_2x2,
                                    cardId = cardId,
                                    onClick = if (title == "Wattage") { { showFullWattage = true } } else null
                                )
                            }
                            repeat(statusColumns - chunk.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { BatteryHistoryCard(batteryHistory, lastFullChargeTs, lastStoppedChargingTs) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { WattageHistoryCard(i.wattage, wattageHistory) { showFullWattage = true } }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                InfoGroupCard(
                    title = stringResource(R.string.health_specs),
                    icon = Icons.Outlined.HealthAndSafety,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    cardId = "group_health_specs"
                ) {
                    InfoRow(stringResource(R.string.health), i.health)
                    InfoRow(stringResource(id = R.string.battery_wear), i.wear)
                    InfoRow(stringResource(id = R.string.actual_capacity), i.actualCapacity)
                    InfoRow(stringResource(R.string.capacity), i.capacity)
                    InfoRow(stringResource(R.string.technology), i.technology)
                    InfoRow(stringResource(id = R.string.charging_speed), i.wattage)
                    InfoRow(stringResource(R.string.current_now), i.currentNow)
                    InfoRow(stringResource(R.string.cycle_count), i.chargeCounter)
                }
            }
        } 
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable fun DisplayTab(v: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val info by v.displayInfo.collectAsStateWithLifecycle()
    info?.let { i -> 
        LazyColumn(contentPadding = PaddingValues(16.dp)) { 
            item {
                DashboardStatusCard(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    title = stringResource(R.string.resolution),
                    icon = Icons.Outlined.Monitor,
                    value = i.currentResolution,
                    subtext = stringResource(id = R.string.aspect_ratio) + ": ${i.aspectRatio}",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    size = CardSize.SIZE_4x2,
                    cardId = "display_resolution"
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(id = R.string.color_depth),
                        icon = Icons.Outlined.Palette,
                        value = i.colorDepth,
                        subtext = i.colorDepthSubtext,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "display_color_depth"
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(R.string.refresh_rate),
                        icon = Icons.Outlined.Refresh,
                        value = i.currentRefreshRate,
                        subtext = stringResource(id = R.string.high_refresh_support),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "display_refresh_rate"
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(R.string.physical_size),
                        icon = Icons.Outlined.AspectRatio,
                        value = i.physicalSize,
                        subtext = i.ppi,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "display_size"
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(id = R.string.wide_color_gamut),
                        icon = Icons.Outlined.Palette,
                        value = if (i.wideColorGamut) stringResource(id = R.string.supported) else stringResource(id = R.string.not_supported),
                        subtext = if (i.wideColorGamut) stringResource(id = R.string.p3_display) else stringResource(id = R.string.standard_rgb),
                        containerColor = if (i.wideColorGamut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        contentColor = if (i.wideColorGamut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        size = CardSize.SIZE_2x2,
                        cardId = "display_gamut"
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { 
                InfoGroupCard(
                    stringResource(R.string.screen_specs), 
                    Icons.Outlined.Monitor, 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    cardId = "group_screen"
                ) { 
                    InfoRow(stringResource(R.string.resolution), i.highestResolution)
                    InfoRow(stringResource(id = R.string.aspect_ratio), i.aspectRatio)
                    InfoRow(stringResource(R.string.refresh_rate), i.currentRefreshRate)
                    InfoRow(stringResource(id = R.string.supported_rates), i.supportedRefreshRates)
                    InfoRow(stringResource(id = R.string.color_depth), i.colorDepth)
                    InfoRow(stringResource(id = R.string.color_space), i.colorDepthSubtext)
                    InfoRow(stringResource(R.string.hdr_support), i.hdrSupport)
                } 
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { 
                InfoGroupCard(
                    stringResource(R.string.density_config), 
                    Icons.Outlined.SettingsOverscan, 
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), 
                    MaterialTheme.colorScheme.secondary,
                    cardId = "group_screen"
                ) { 
                    InfoRow(stringResource(R.string.density), i.density)
                    InfoRow(stringResource(R.string.x_dpi), i.xDpi)
                    InfoRow(stringResource(R.string.y_dpi), i.yDpi)
                    InfoRow(stringResource(R.string.ppi_label), i.ppi)
                    InfoRow(stringResource(R.string.brightness), i.brightnessLevel)
                    InfoRow(stringResource(R.string.timeout), i.screenTimeout)
                    InfoRow(stringResource(R.string.orientation), i.orientation) 
                } 
            }
        } 
    } 
}

@Composable fun MemoryTab(v: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val info by v.memoryInfo.collectAsStateWithLifecycle()
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val bottomContentPadding = if (isWideScreen) 16.dp else 120.dp
    info?.let { i -> 
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomContentPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) { 
            item {
                InfoGroupCard(stringResource(R.string.tab_memory), Icons.Outlined.Memory, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), MaterialTheme.colorScheme.primary, cardId = "group_tab_memory") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Header with RAM Type
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.ram_type_label), style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.7f))
                            BadgeTag(i.ramType, MaterialTheme.colorScheme.primary)
                        }
                        
                        // Main RAM Progress
                        MemoryVisualProgress(
                            label = "RAM",
                            total = i.totalRam,
                            available = i.availableRam,
                            totalBytes = i.totalRamBytes,
                            availableBytes = i.availableRamBytes,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        if (i.zRamTotal != "0.00 B" && i.zRamTotal != "N/A") {
                            HorizontalDivider(thickness = 0.5.dp, color = LocalContentColor.current.copy(alpha = 0.1f))
                            // Swap Progress
                            MemoryVisualProgress(
                                label = stringResource(id = R.string.swap),
                                total = i.zRamTotal,
                                available = i.zRamUsed,
                                totalBytes = i.zRamTotalBytes,
                                availableBytes = i.zRamTotalBytes - i.zRamUsedBytes,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            
            item {
                InfoGroupCard(stringResource(R.string.internal_storage), Icons.Outlined.SdStorage, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f), MaterialTheme.colorScheme.secondary, cardId = "group_internal_storage") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Header with Storage Type
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(id = R.string.storage_type), style = MaterialTheme.typography.labelMedium, color = LocalContentColor.current.copy(alpha = 0.7f))
                            BadgeTag(i.flashType, MaterialTheme.colorScheme.secondary)
                        }

                        // Multi-segment progress
                        StorageMultiSegmentProgress(i)
                        
                        // Details Breakdown
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StorageDetailRow(stringResource(id = R.string.apps_and_data), i.internalUsedByApps, MaterialTheme.colorScheme.primary)
                            StorageDetailRow(stringResource(id = R.string.tab_system), i.internalUsedBySystem, MaterialTheme.colorScheme.secondary)
                            StorageDetailRow(stringResource(id = R.string.free_format, "").trim(), i.internalFree, MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            item {
                InfoGroupCard(stringResource(R.string.filesystem_details), Icons.Outlined.Dns, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f), MaterialTheme.colorScheme.tertiary, cardId = "group_filesystem_details") {
                    InfoRow(stringResource(R.string.fs_type), i.internalFsType)
                    InfoRow(stringResource(R.string.block_size), i.internalBlockSize)
                    InfoRow(stringResource(R.string.memory_page_size), i.memoryPageSize)
                    InfoRow(stringResource(R.string.partition_label), i.internalPartition)
                }
            }

            if (i.externalStorages.isNotEmpty()) {
                items(i.externalStorages) { storage ->
                    StorageCard(
                        title = storage.name,
                        icon = if (storage.type == "Removable") Icons.Outlined.SdCard else Icons.Outlined.Storage,
                        total = storage.total,
                        available = storage.free,
                        totalBytes = storage.totalBytes,
                        availableBytes = storage.freeBytes,
                        container = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                        content = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        } 
    }
}

@Composable fun MemoryVisualProgress(label: String, total: String, available: String, totalBytes: Long, availableBytes: Long, color: Color) {
    val usedRatio = if (totalBytes > 0) ((totalBytes - availableBytes).toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val usedPercent = (usedRatio * 100).toInt()
    val context = androidx.compose.ui.platform.LocalContext.current
    val usedStr = android.text.format.Formatter.formatFileSize(context, (totalBytes - availableBytes).coerceAtLeast(0L))
    val contentColor = LocalContentColor.current
    val progressColor = color

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = contentColor)
                Text(stringResource(id = R.string.total_label, total), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f))
            }
            Text("$usedPercent%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = contentColor)
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // High visibility adaptive progress bar track: semi-transparent contentColor background with specified progress color
        Surface(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            shape = CircleShape,
            color = contentColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
            shadowElevation = 2.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (usedRatio > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(usedRatio)
                            .background(progressColor, CircleShape)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(progressColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.used_label, usedStr), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = contentColor)
            }
            Text(stringResource(id = R.string.free_label, available), style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.7f))
        }
    }
}

@Composable fun StorageMultiSegmentProgress(i: MemoryInfo) {
    val total = i.internalTotalBytes.coerceAtLeast(1L).toFloat()
    val appsRatio = (i.internalUsedByAppsBytes.toFloat() / total).coerceIn(0f, 1f)
    val systemRatio = (i.internalUsedBySystemBytes.toFloat() / total).coerceIn(0f, 1f)
    
    val contentColor = LocalContentColor.current
    val appsColor = MaterialTheme.colorScheme.primary
    val systemColor = MaterialTheme.colorScheme.secondary

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(stringResource(id = R.string.storage), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = contentColor)
            Text(stringResource(id = R.string.total_label, i.internalTotal), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f))
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Multi-segmented Bar with adaptive track and clean spacing between segments
        Surface(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            shape = CircleShape,
            color = contentColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.12f)),
            shadowElevation = 2.dp
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (appsRatio > 0) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(appsRatio).background(appsColor))
                }
                if (appsRatio > 0 && systemRatio > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                if (systemRatio > 0) {
                    val remainingWidth = 1f - appsRatio
                    if (remainingWidth > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(systemRatio / remainingWidth)
                                .background(systemColor)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable fun StorageDetailRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.8f))
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LocalContentColor.current)
    }
}

@Composable fun StorageCard(title: String, icon: ImageVector, total: String, available: String, totalBytes: Long, availableBytes: Long, container: Color, content: Color, cardId: String? = "group_internal_storage") {
    val used = if (totalBytes > 0) ((totalBytes - availableBytes).toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val context = androidx.compose.ui.platform.LocalContext.current
    val usedStr = android.text.format.Formatter.formatFileSize(context, (totalBytes - availableBytes).coerceAtLeast(0L))
    
    InfoGroupCard(title, icon, container, content, cardId = cardId) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            LinearProgressIndicator(progress = { used }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = if (used > 0.9f) MaterialTheme.colorScheme.error else content, trackColor = content.copy(alpha = 0.1f), strokeCap = StrokeCap.Round)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(id = R.string.used_label, usedStr), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(id = R.string.total_label, total), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(id = R.string.free_label, available), style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.8f))
        }
    }
}

@Composable fun WifiTab(v: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val info by v.networkInfo.collectAsStateWithLifecycle()
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    info?.let { i -> 
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { 
            item(span = { GridItemSpan(3) }) {
                DashboardStatusCard(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    title = "SSID",
                    icon = Icons.Outlined.Wifi,
                    value = i.wifiSsid ?: stringResource(R.string.disconnected),
                    subtext = i.wifiBssid ?: i.state,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    size = CardSize.SIZE_4x2,
                    cardId = "wifi_card"
                )
            }

            item(span = { GridItemSpan(3) }) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(R.string.link_speed),
                        icon = Icons.Outlined.Speed,
                        value = i.linkSpeed ?: stringResource(R.string.na),
                        subtext = stringResource(R.string.current_speed),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "wifi_card"
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(R.string.wifi_standard),
                        icon = Icons.Outlined.SettingsInputAntenna,
                        value = i.standard?.substringAfter("(")?.substringBefore(")") ?: stringResource(R.string.na),
                        subtext = i.standard?.substringBefore("(") ?: stringResource(R.string.wifi_standard_label),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "wifi_card"
                    )
                }
            }

            item(span = { GridItemSpan(3) }) {
                Text(
                    text = stringResource(R.string.tab_wifi) + " " + stringResource(R.string.capabilities),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            val features = listOf(
                Triple(R.string.wifi_standard_default, Icons.Outlined.SettingsInputAntenna, true),
                Triple(R.string.wifi_5ghz, Icons.Outlined.Wifi, i.is5GHzSupported),
                Triple(R.string.wifi_6ghz, Icons.Outlined.Wifi, i.is6GHzSupported),
                Triple(R.string.wifi_direct, Icons.Outlined.WifiTethering, i.isWifiDirectSupported),
                Triple(R.string.wifi_aware, Icons.Outlined.NearbyError, i.isWifiAwareSupported),
                Triple(R.string.wifi_hotspot, Icons.Outlined.WifiTethering, i.isApSupported)
            )
            
            items(features) { (tRes, ic, s) ->
                HardwareCapabilityCard(
                    icon = ic,
                    label = stringResource(tRes),
                    isSupported = s,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item(span = { GridItemSpan(3) }) { 
                InfoGroupCard(stringResource(R.string.connection), if (i.type == "WiFi") Icons.Outlined.Wifi else Icons.Outlined.SignalCellularAlt, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), cardId = "group_connection") { 
                    InfoRow(stringResource(R.string.status), i.state)
                    InfoRow(stringResource(R.string.type), i.type)
                    i.interfaceName?.let { InfoRow(stringResource(R.string.interface_label), it) }
                    i.vendor?.let { InfoRow(stringResource(R.string.vendor_label), it) }
                } 
            }
            
            if (i.type == "WiFi") {
                item(span = { GridItemSpan(3) }) {
                    InfoGroupCard(stringResource(R.string.wifi_details_label), Icons.Outlined.SettingsInputAntenna, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary, cardId = "group_wifi_details") {
                        i.frequency?.let { InfoRow(stringResource(R.string.frequency), it) }
                        i.channel?.let { InfoRow(stringResource(R.string.channel_label), it) }
                        i.width?.let { InfoRow(stringResource(R.string.bandwidth_label), it) }
                        i.signalStrength?.let { InfoRow(stringResource(R.string.signal_strength), it) }
                        i.security?.let { InfoRow(stringResource(R.string.security_label), it) }
                    }
                }
            }

            item(span = { GridItemSpan(3) }) {
                InfoGroupCard("TCP/IP & DHCP", Icons.Outlined.Router, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.tertiary, cardId = "group_wifi_dhcp") {
                    i.ipAddress?.let { InfoRow(stringResource(R.string.ipv4_address), it) }
                    i.ipv6Address?.let { InfoRow(stringResource(R.string.ipv6_address), it) }
                    i.gateway?.let { InfoRow(stringResource(R.string.gateway_label), it) }
                    i.netmask?.let { InfoRow(stringResource(R.string.netmask_label), it) }
                    i.dns1?.let { InfoRow(stringResource(R.string.dns1_label), it) }
                    i.dns2?.let { InfoRow(stringResource(R.string.dns2_label), it) }
                    i.dhcpServer?.let { InfoRow(stringResource(R.string.dhcp_server), it) }
                    i.leaseDuration?.let { InfoRow(stringResource(R.string.lease_duration), it) }
                }
            }
            item(span = { GridItemSpan(3) }) { Spacer(modifier = Modifier.height(80.dp)) }
        }
    } ?: EmptyState(Icons.Outlined.SignalWifiOff, stringResource(R.string.no_network))
}

@Composable
fun CellularTab(v: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val info by v.networkInfo.collectAsStateWithLifecycle()
    info?.let { i ->
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            if (i.cellularInfo != null) {
                val cell = i.cellularInfo
                item {
                    val phoneCount = cell.phoneCount
                    if (phoneCount > 1) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (slot in 1..2) {
                                val sim = cell.simInfos.find { it.slot == slot }
                                DashboardStatusCard(
                                    modifier = Modifier.weight(1f).height(160.dp),
                                    title = stringResource(R.string.sim_operator_format, slot),
                                    icon = Icons.Outlined.SignalCellularAlt,
                                    value = sim?.carrier ?: stringResource(R.string.disconnected),
                                    subtext = if (slot == 1) cell.state else (sim?.state ?: stringResource(R.string.disconnected)),
                                    containerColor = if (slot == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                                    contentColor = if (slot == 1) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onTertiary,
                                    size = CardSize.SIZE_2x2,
                                    cardId = "cellular_card"
                                )
                            }
                        }
                    } else {
                        DashboardStatusCard(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            title = stringResource(R.string.network_operator),
                            icon = Icons.Outlined.SignalCellularAlt,
                            value = cell.networkOperator ?: stringResource(R.string.disconnected),
                            subtext = cell.state,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            size = CardSize.SIZE_4x2,
                            cardId = "cellular_card"
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    InfoGroupCard(
                        stringResource(R.string.cellular_details),
                        Icons.Outlined.SignalCellularAlt,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.secondary,
                        cardId = "cellular_card"
                    ) {
                        InfoRow(stringResource(R.string.status), cell.state)
                        InfoRow(stringResource(R.string.sim_support), cell.multiSimSupport)
                        InfoRow(stringResource(R.string.network_operator), cell.networkOperator ?: stringResource(R.string.na))
                        InfoRow(stringResource(R.string.network_type), cell.networkType ?: stringResource(R.string.na))
                        InfoRow(stringResource(R.string.apn_label), cell.apn ?: stringResource(R.string.na))
                        InfoRow(stringResource(R.string.ipv4_address), cell.ipV4 ?: stringResource(R.string.na))
                        if (cell.ipV6.isNotEmpty()) {
                            InfoRow(stringResource(R.string.ipv6_address), cell.ipV6.joinToString("\n"))
                        }
                        InfoRow(stringResource(R.string.interface_label), cell.interfaceName ?: stringResource(R.string.na))
                    }
                }

                cell.simInfos.forEach { sim ->
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        InfoGroupCard(
                            stringResource(R.string.sim_slot, sim.slot),
                            Icons.Outlined.SimCard,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiary,
                            cardId = "group_sim_status"
                        ) {
                            InfoRow(stringResource(R.string.carrier), sim.carrier)
                            sim.phoneNumber?.let { InfoRow(stringResource(R.string.phone_number), it) }
                            InfoRow(stringResource(R.string.country_iso), sim.countryIso?.uppercase() ?: stringResource(R.string.na))
                            InfoRow(stringResource(R.string.mcc_label), sim.mcc ?: stringResource(R.string.na))
                            InfoRow(stringResource(R.string.mnc_label), sim.mnc ?: stringResource(R.string.na))
                            InfoRow(stringResource(R.string.roaming_label), if (sim.roaming) stringResource(R.string.enabled) else stringResource(R.string.disabled))
                            InfoRow(stringResource(R.string.state_label), sim.state)
                        }
                    }
                }
            } else {
                item {
                    EmptyState(
                        Icons.Outlined.SignalCellularConnectedNoInternet0Bar,
                        stringResource(R.string.cellular_requires_permission)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    } ?: EmptyState(Icons.Outlined.SignalCellularConnectedNoInternet0Bar, "No Network Information")
}


@Composable fun CodecsTab(v: DeviceInfoViewModel) {
    val codecs by v.codecs.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { 
        itemsIndexed(codecs, key = { _, c -> c.name }) { idx, c -> 
            InfoGroupCard(
                title = c.name, 
                icon = Icons.Outlined.Audiotrack, 
                containerColor = (if (idx % 2 == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer).copy(alpha = 0.2f), 
                contentColor = if (idx % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                cardId = "group_codecs"
            ) { 
                InfoRow(stringResource(R.string.type), c.type)
                InfoRow(stringResource(R.string.mime), c.mimeType) 
            }
        } 
    }
}

@Composable fun UsbTab(v: DeviceInfoViewModel) {
    val devices by v.usbDevices.collectAsStateWithLifecycle()
    if (devices.isEmpty()) {
        EmptyState(Icons.Outlined.UsbOff, stringResource(R.string.no_usb))
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { 
            itemsIndexed(devices, key = { _, u -> u.name + u.productId }) { idx, u -> 
                InfoGroupCard(
                    title = u.name, 
                    icon = Icons.Outlined.Usb, 
                    containerColor = (if (idx % 2 == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer).copy(alpha = 0.2f), 
                    contentColor = if (idx % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    cardId = "group_usb"
                ) { 
                    InfoRow(stringResource(R.string.vendor_id), u.vendorId)
                    InfoRow(stringResource(R.string.product_id), u.productId)
                    InfoRow(stringResource(R.string.class_label), u.deviceClass) 
                }
            } 
        }
    }
}

@Composable fun SensorsTab(v: DeviceInfoViewModel) {
    val sensors by v.sensors.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) { 
        itemsIndexed(sensors, key = { _, s -> s.name }, contentType = { _, _ -> "sensor" }) { idx, s -> 
            val c = when (idx % 3) { 
                0 -> MaterialTheme.colorScheme.primaryContainer
                1 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.tertiaryContainer 
            }
            val tc = when (idx % 3) { 
                0 -> MaterialTheme.colorScheme.primary
                1 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.tertiary 
            }
            InfoGroupCard(title = s.name, icon = Icons.Outlined.Sensors, containerColor = c.copy(alpha = 0.2f), contentColor = tc, cardId = "group_sensors") { 
                InfoRow(stringResource(R.string.vendor), s.vendor)
                InfoRow(stringResource(R.string.big_little), "${s.power} mA")
                InfoRow(stringResource(R.string.resolution), s.resolution.toString())
                InfoRow(stringResource(R.string.frequency), if (s.minDelay > 0) "${1000000 / s.minDelay} Hz" else "N/A") 
            }
        } 
    }
}

enum class AppFilterType { ALL, INSTALLED, SYSTEM }

@Composable fun AppsTab(v: DeviceInfoViewModel) {
    val apps by v.installedApps.collectAsStateWithLifecycle()
    val playStore by v.isPlayStoreAvailable.collectAsStateWithLifecycle()
    var activeFilter by remember { mutableStateOf(AppFilterType.ALL) }
    
    val totalCount = apps.size
    val systemCount = apps.count { it.isSystem }
    val installedCount = totalCount - systemCount

    val filteredApps = remember(apps, activeFilter) {
        when (activeFilter) {
            AppFilterType.ALL -> apps
            AppFilterType.INSTALLED -> apps.filter { !it.isSystem }
            AppFilterType.SYSTEM -> apps.filter { it.isSystem }
        }
    }

    Column { 
        if (!playStore) {
            Button(
                onClick = { v.checkForUpdates() }, 
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), 
                shape = ShapeMedium
            ) { 
                Icon(Icons.Default.Update, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.check_updates)) 
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) { 
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Big Card: Total Apps
                    DashboardStatusCard(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        title = stringResource(R.string.apps_total),
                        icon = Icons.Outlined.Apps,
                        value = totalCount.toString(),
                        subtext = stringResource(R.string.tab_apps),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        size = CardSize.SIZE_4x2,
                        cardId = "apps_installed"
                    )

                    // Side-by-side Cards: Installed and System
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Installed Card
                        DashboardStatusCard(
                            modifier = Modifier.weight(1f).height(160.dp),
                            title = stringResource(R.string.apps_installed),
                            icon = Icons.Outlined.Download,
                            value = installedCount.toString(),
                            subtext = stringResource(R.string.status_installed),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            size = CardSize.SIZE_2x2,
                            cardId = "apps_installed"
                        )

                        // System Card
                        DashboardStatusCard(
                            modifier = Modifier.weight(1f).height(160.dp),
                            title = stringResource(R.string.apps_system),
                            icon = Icons.Outlined.Settings,
                            value = systemCount.toString(),
                            subtext = stringResource(R.string.tag_system),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            size = CardSize.SIZE_2x2,
                            cardId = "apps_installed"
                        )
                    }
                }
            }

            // Filtering Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        AppFilterType.ALL to stringResource(R.string.filter_all),
                        AppFilterType.INSTALLED to stringResource(R.string.filter_installed),
                        AppFilterType.SYSTEM to stringResource(R.string.filter_system)
                    )
                    filters.forEach { (type, label) ->
                        val isSelected = activeFilter == type
                        val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(CircleShape)
                                .clickable { activeFilter = type },
                            shape = CircleShape,
                            color = containerColor,
                            contentColor = contentColor,
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            items(filteredApps, key = { it.packageName }, contentType = { "app" }) { a -> 
                AppEntryCard(a)
            } 
        } 
    }
}

@Composable fun CamerasTab(viewModel: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val cameraSpecViewModel: CameraSpecViewModel = viewModel()
    CameraSpecScreen(cameraSpecViewModel)
}

@Composable fun BluetoothTab(v: DeviceInfoViewModel) {
    val info by v.bluetoothInfo.collectAsStateWithLifecycle()
    info?.let { bt ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                DashboardStatusCard(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    title = stringResource(R.string.tab_bluetooth),
                    icon = Icons.Outlined.Bluetooth,
                    value = bt.version,
                    subtext = bt.state,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    size = CardSize.SIZE_4x2,
                    cardId = "bluetooth_version"
                )
            }
            item(span = { GridItemSpan(3) }) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(R.string.bt_paired_devices),
                        icon = Icons.Outlined.Devices,
                        value = bt.pairedDevicesCount.toString(),
                        subtext = stringResource(R.string.bt_paired_devices),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "bluetooth_paired"
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f).height(160.dp),
                        title = stringResource(R.string.connected_devices),
                        icon = Icons.Outlined.BluetoothConnected,
                        value = bt.connectedDevicesCount.toString(),
                        subtext = stringResource(R.string.connected_devices),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        size = CardSize.SIZE_2x2,
                        cardId = "bluetooth_connected"
                    )
                }
            }
            item(span = { GridItemSpan(3) }) {
                InfoGroupCard(
                    stringResource(R.string.tab_bluetooth),
                    Icons.Outlined.Bluetooth,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    cardId = "group_bluetooth"
                ) {
                    InfoRow(stringResource(R.string.bt_state), bt.state)
                    InfoRow(stringResource(R.string.bt_version), bt.version)
                    InfoRow(stringResource(R.string.bt_device_name), bt.name)
                    InfoRow(stringResource(R.string.bt_mac_address), bt.address)
                    InfoRow(stringResource(R.string.bt_paired_devices), bt.pairedDevicesCount.toString())
                    InfoRow(stringResource(R.string.connected_devices), bt.connectedDevicesCount.toString())
                }
            }
            bt.featureGroups.forEach { group ->
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = stringResource(group.titleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(group.features) { feature ->
                    HardwareCapabilityCard(
                        icon = Icons.Outlined.Bluetooth,
                        label = stringResource(feature.nameRes),
                        isSupported = feature.isSupported,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            item(span = { GridItemSpan(3) }) { Spacer(modifier = Modifier.height(80.dp)) }
        }
    } ?: EmptyState(Icons.Outlined.BluetoothDisabled, stringResource(R.string.bt_permission_required))
}


@Composable fun ThermalTab(v: DeviceInfoViewModel) {
    val thermalZones by v.thermalZones.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        if (thermalZones.isEmpty()) {
            item { EmptyState(Icons.Outlined.Thermostat, stringResource(R.string.tab_thermal)) }
        } else {
            itemsIndexed(thermalZones) { idx, zone ->
                val temp = zone.temperature.replace("°C", "").trim().toFloatOrNull() ?: 0f
                val containerColor = when {
                    temp >= 70 -> MaterialTheme.colorScheme.errorContainer
                    temp >= 50 -> MaterialTheme.colorScheme.tertiaryContainer
                    temp >= 35 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }.copy(alpha = 0.3f)
                val titleColor = when {
                    temp >= 70 -> MaterialTheme.colorScheme.error
                    temp >= 50 -> MaterialTheme.colorScheme.tertiary
                    temp >= 35 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
                InfoGroupCard(title = zone.name, icon = Icons.Outlined.Thermostat, containerColor = containerColor, contentColor = titleColor, cardId = "group_thermal") {
                    InfoRow(stringResource(R.string.thermal_zone), zone.name)
                    InfoRow(stringResource(R.string.battery_temperature), zone.temperature)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable fun SecurityTab(v: DeviceInfoViewModel) {
    val info by v.securityInfo.collectAsStateWithLifecycle()
    val drmInfos by v.drmInfos.collectAsStateWithLifecycle()
    info?.let { sec ->
        val isSecure = !sec.rootAccess.contains("rooted", ignoreCase = true) && sec.selinuxStatus.equals("Enforcing", ignoreCase = true)
        val scoreColor = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                DashboardStatusCard(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    title = stringResource(R.string.sec_overview),
                    icon = Icons.Outlined.Security,
                    value = if (isSecure) stringResource(R.string.sec_status_good) else stringResource(R.string.sec_status_warning),
                    subtext = stringResource(R.string.security_patch) + ": " + sec.securityPatch,
                    containerColor = scoreColor,
                    contentColor = if (isSecure) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                    size = CardSize.SIZE_4x2,
                    cardId = "security_patch"
                )
            }
            
            // Section 1: System Status
            item(span = { GridItemSpan(3) }) {
                InfoGroupCard(
                    stringResource(R.string.sec_system_status),
                    Icons.Outlined.Shield,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    cardId = "group_sec_system_status"
                ) {
                    InfoRow(stringResource(R.string.sec_root_access), sec.rootAccess)
                    InfoRow(stringResource(R.string.sec_selinux), sec.selinuxStatus)
                    InfoRow(stringResource(R.string.sec_encryption), sec.encryptionStatus)
                    InfoRow(stringResource(R.string.sec_verified_boot), sec.verifiedBootState)
                    InfoRow(stringResource(R.string.bootloader), sec.bootloaderStatus)
                    InfoRow(stringResource(R.string.sec_avb_version), sec.avbVersion)
                    InfoRow(stringResource(R.string.sec_dm_verity), sec.dmVerity)
                }
            }
            
            item {
                HardwareCapabilityCard(
                    icon = Icons.Outlined.EnhancedEncryption,
                    label = stringResource(R.string.sec_secure_enclave),
                    isSupported = sec.hasStrongBox,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Spacer(modifier = Modifier.fillMaxWidth()) }
            item { Spacer(modifier = Modifier.fillMaxWidth()) }
            
            // Section 2: Biometrics
            item(span = { GridItemSpan(3) }) {
                Text(
                    text = stringResource(R.string.sec_biometrics),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            val biometricFeatures = listOf(
                Triple(R.string.sec_fingerprint_supp, Icons.Outlined.Fingerprint, sec.hasFingerprint),
                Triple(R.string.sec_face_supp, Icons.Outlined.Face, sec.hasFaceUnlock),
                Triple(R.string.sec_iris_supp, Icons.Outlined.RemoveRedEye, sec.hasIrisScanner)
            )
            items(biometricFeatures) { (tRes, ic, s) ->
                HardwareCapabilityCard(icon = ic, label = stringResource(tRes), isSupported = s, modifier = Modifier.fillMaxWidth())
            }
            
            item(span = { GridItemSpan(3) }) {
                InfoGroupCard(
                    stringResource(R.string.sec_biometrics) + " Info",
                    Icons.Outlined.Fingerprint,
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.secondary,
                    cardId = "group_sec_biometrics"
                ) {
                    InfoRow(stringResource(R.string.sec_biometric_class), sec.biometricClass)
                }
            }
            
            // Section 3: Play Integrity
            item(span = { GridItemSpan(3) }) {
                Text(
                    text = stringResource(R.string.sec_play_integrity),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            val integrityFeatures = listOf(
                Triple(R.string.sec_meets_basic, Icons.Outlined.VerifiedUser, sec.meetsBasicIntegrity),
                Triple(R.string.sec_meets_device, Icons.Outlined.VerifiedUser, sec.meetsDeviceIntegrity),
                Triple(R.string.sec_meets_strong, Icons.Outlined.VerifiedUser, sec.meetsStrongIntegrity)
            )
            items(integrityFeatures) { (tRes, ic, s) ->
                HardwareCapabilityCard(icon = ic, label = stringResource(tRes), isSupported = s, modifier = Modifier.fillMaxWidth())
            }
            
            sec.integrityFailureReason?.let { reason ->
                item(span = { GridItemSpan(3) }) {
                    InfoGroupCard(
                        stringResource(R.string.sec_play_integrity) + " Details",
                        Icons.Outlined.VerifiedUser,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.tertiary,
                        cardId = "group_sec_play_integrity"
                    ) {
                        InfoRow(stringResource(R.string.sec_failure_reason), reason)
                    }
                }
            }
            
            // Section 4: Encryption & Storage
            item(span = { GridItemSpan(3) }) {
                Text(
                    text = stringResource(R.string.sec_encryption_storage),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item(span = { GridItemSpan(3) }) {
                InfoGroupCard(
                    stringResource(R.string.sec_encryption_storage),
                    Icons.Outlined.Lock,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    cardId = "group_sec_encryption_storage"
                ) {
                    InfoRow(stringResource(R.string.sec_keystore_type), sec.keystoreType)
                    InfoRow(stringResource(R.string.sec_encryption_alg), sec.encryptionAlgorithm)
                }
            }
            item {
                HardwareCapabilityCard(
                    icon = Icons.Outlined.Lock,
                    label = stringResource(R.string.sec_hw_keystore),
                    isSupported = sec.hardwareBackedKeystore,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Spacer(modifier = Modifier.fillMaxWidth()) }
            item { Spacer(modifier = Modifier.fillMaxWidth()) }
            
            // Section 5: DRM Status
            if (drmInfos.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = stringResource(R.string.drm_status),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                itemsIndexed(drmInfos, span = { _, _ -> GridItemSpan(3) }) { idx, drm ->
                    val containerColor = when (idx % 3) {
                        0 -> MaterialTheme.colorScheme.primaryContainer
                        1 -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    }.copy(alpha = 0.3f)
                    val titleColor = when (idx % 3) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                    InfoGroupCard(title = drm.name, icon = Icons.Outlined.VerifiedUser, containerColor = containerColor, contentColor = titleColor, cardId = "group_drm") {
                        InfoRow(stringResource(R.string.vendor), drm.vendor)
                        InfoRow(stringResource(R.string.version), drm.version)
                        InfoRow(stringResource(R.string.drm_description), drm.description)
                        InfoRow(stringResource(R.string.security_level), drm.securityLevel)
                        InfoRow(stringResource(R.string.max_hdcp_level), drm.maxHdcpLevel)
                        InfoRow(stringResource(R.string.current_hdcp_level), drm.currentHdcpLevel)
                        InfoRow(stringResource(R.string.drm_system_id), drm.systemId)
                    }
                }
            }
            
            // Section 6: Network Connection security
            item(span = { GridItemSpan(3) }) {
                Text(
                    text = "Network Security",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            val networkSecFeatures = listOf(
                Triple(R.string.sec_vpn_active, Icons.Outlined.VpnLock, sec.vpnActive),
                Triple(R.string.sec_wifi_mac_rand, Icons.Outlined.PhonelinkSetup, sec.randomMacEnabled),
                Triple(R.string.sec_cleartext_traffic, Icons.Outlined.LockOpen, sec.cleartextPermitted)
            )
            items(networkSecFeatures) { (tRes, ic, s) ->
                HardwareCapabilityCard(icon = ic, label = stringResource(tRes), isSupported = s, modifier = Modifier.fillMaxWidth())
            }
            
            item(span = { GridItemSpan(3) }) {
                InfoGroupCard(
                    stringResource(R.string.sec_network_conn) + " Details",
                    Icons.Outlined.Wifi,
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.secondary,
                    cardId = "group_sec_network_conn"
                ) {
                    InfoRow(stringResource(R.string.sec_private_dns), sec.privateDnsStatus)
                }
            }
            
            // Section 7: Updates
            item(span = { GridItemSpan(3) }) {
                Text(
                    text = "System Update Capabilities",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            val updateFeatures = listOf(
                Triple(R.string.sec_treble, Icons.Outlined.SystemUpdate, sec.projectTreble),
                Triple(R.string.sec_mainline, Icons.Outlined.SystemUpdate, sec.projectMainline),
                Triple(R.string.sec_dynamic_partitions, Icons.Outlined.SystemUpdate, sec.dynamicPartitions),
                Triple(R.string.sec_seamless_updates, Icons.Outlined.SystemUpdate, sec.seamlessUpdates)
            )
            items(updateFeatures) { (tRes, ic, s) ->
                HardwareCapabilityCard(icon = ic, label = stringResource(tRes), isSupported = s, modifier = Modifier.fillMaxWidth())
            }
            
            item(span = { GridItemSpan(3) }) {
                InfoGroupCard(
                    stringResource(R.string.sec_updates) + " Details",
                    Icons.Outlined.SystemUpdate,
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.tertiary,
                    cardId = "group_sec_updates"
                ) {
                    InfoRow(stringResource(R.string.sec_active_slot), sec.activeSlot)
                }
            }
            
            // Section 8: Perm audit
            item(span = { GridItemSpan(3) }) {
                InfoGroupCard(
                    stringResource(R.string.sec_perm_audit),
                    Icons.Outlined.AdminPanelSettings,
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    MaterialTheme.colorScheme.error,
                    cardId = "group_sec_perm_audit"
                ) {
                    InfoRow(stringResource(R.string.sec_apps_camera), sec.appsCameraCount.toString())
                    InfoRow(stringResource(R.string.sec_apps_mic), sec.appsMicCount.toString())
                    InfoRow(stringResource(R.string.sec_apps_location), sec.appsLocationCount.toString())
                    InfoRow(stringResource(R.string.sec_apps_contacts_sms), sec.appsContactsSmsCount.toString())
                    InfoRow(stringResource(R.string.sec_system_alerts), sec.overlayAppsCount.toString())
                    InfoRow(stringResource(R.string.sec_unknown_sources), sec.unknownSourcesCount.toString())
                    InfoRow(stringResource(R.string.sec_non_play_store), sec.nonPlayStoreCount.toString())
                }
            }
            
            // Section 9: Apps settings
            item(span = { GridItemSpan(3) }) {
                Text(
                    text = "Developer Capabilities",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            val devFeatures = listOf(
                Triple(R.string.sec_dev_options, Icons.Outlined.Code, sec.developerOptionsEnabled),
                Triple(R.string.sec_adb_enabled, Icons.Outlined.Terminal, sec.adbEnabled),
                Triple(R.string.sec_wireless_debugging, Icons.Outlined.DeveloperMode, sec.wirelessDebuggingEnabled)
            )
            items(devFeatures) { (tRes, ic, s) ->
                HardwareCapabilityCard(icon = ic, label = stringResource(tRes), isSupported = s, modifier = Modifier.fillMaxWidth())
            }
            
            if (sec.accessibilityApps.isNotEmpty() || sec.deviceAdminApps.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    InfoGroupCard(
                        stringResource(R.string.sec_apps) + " Info",
                        Icons.Outlined.Apps,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        cardId = "group_sec_apps"
                    ) {
                        if (sec.accessibilityApps.isNotEmpty()) {
                            InfoRow(stringResource(R.string.sec_accessibility_services), sec.accessibilityApps.joinToString(", "))
                        }
                        if (sec.deviceAdminApps.isNotEmpty()) {
                            InfoRow(stringResource(R.string.sec_device_admins), sec.deviceAdminApps.joinToString(", "))
                        }
                    }
                }
            }
            item(span = { GridItemSpan(3) }) { Spacer(modifier = Modifier.height(80.dp)) }
        }
    } ?: EmptyState(Icons.Outlined.Security, stringResource(R.string.tab_security))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(viewModel: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    var activeCardInfoId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interactionHandler = remember(context, haptic) {
        object : CardInteractionHandler {
            override fun showCardInfo(cardId: String) {
                activeCardInfoId = cardId
            }
            override fun triggerLongPress(cardId: String) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                CardDetailsHelper.launchSettingsIntent(context, cardId)
            }
        }
    }

    val tabs = listOf(
        TabItem(stringResource(R.string.tab_device), Icons.Outlined.Smartphone),
        TabItem(stringResource(R.string.tab_soc), Icons.Outlined.Memory),
        TabItem(stringResource(R.string.tab_cameras), Icons.Outlined.PhotoCamera),
        TabItem(stringResource(R.string.tab_battery), Icons.Outlined.BatteryStd),
        TabItem(stringResource(R.string.tab_bluetooth), Icons.Outlined.Bluetooth),
        TabItem(stringResource(R.string.tab_display), Icons.Outlined.DisplaySettings),
        TabItem(stringResource(R.string.tab_memory), Icons.Outlined.Storage),
        TabItem(stringResource(R.string.tab_wifi), Icons.Outlined.Wifi),
        TabItem(stringResource(R.string.tab_cellular), Icons.Outlined.SignalCellularAlt),
        TabItem(stringResource(R.string.tab_usb), Icons.Outlined.Usb),
        TabItem(stringResource(R.string.tab_codecs), Icons.Outlined.Audiotrack),
        TabItem(stringResource(R.string.tab_sensors), Icons.Outlined.Sensors),
        TabItem(stringResource(R.string.tab_apps), Icons.Outlined.Apps),
        TabItem(stringResource(R.string.tab_thermal), Icons.Outlined.Thermostat),
        TabItem(stringResource(R.string.tab_security), Icons.Outlined.Security)
    )
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabs.size })
    
    val requestedInfoTab by viewModel.requestedInfoTab.collectAsStateWithLifecycle()
    LaunchedEffect(requestedInfoTab) {
        requestedInfoTab?.let { tabIndex ->
            pagerState.scrollToPage(tabIndex)
            viewModel.clearRequestedInfoTab()
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    CompositionLocalProvider(LocalCardInteractionHandler provides interactionHandler) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationRailItem(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (!isWideScreen) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    Text(
                                        tab.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                icon = { Icon(tab.icon, null, modifier = Modifier.size(20.dp)) }
                            )
                        }
                    }
                }
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true,
                    beyondViewportPageCount = 1
                ) { page ->
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (page) {
                            0 -> DeviceTab(viewModel)
                            1 -> SocTab(viewModel)
                            2 -> CamerasTab(viewModel, windowSizeClass)
                            3 -> BatteryTab(viewModel, windowSizeClass)
                            4 -> BluetoothTab(viewModel)
                            5 -> DisplayTab(viewModel, windowSizeClass)
                            6 -> MemoryTab(viewModel, windowSizeClass)
                            7 -> WifiTab(viewModel, windowSizeClass)
                            8 -> CellularTab(viewModel, windowSizeClass)
                            9 -> UsbTab(viewModel)
                            10 -> CodecsTab(viewModel)
                            11 -> SensorsTab(viewModel)
                            12 -> AppsTab(viewModel)
                            13 -> ThermalTab(viewModel)
                            14 -> SecurityTab(viewModel)
                        }
                    }
                }
            }
        }
    }

    if (activeCardInfoId != null) {
        ModalBottomSheet(
            onDismissRequest = { activeCardInfoId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            CardDetailsSheetContent(
                cardId = activeCardInfoId!!,
                onDismiss = { activeCardInfoId = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailsSheetContent(
    cardId: String,
    onDismiss: () -> Unit
) {
    val details = remember(cardId) { CardDetailsHelper.getDetailsForCard(cardId) }
    if (details == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No information available for this component.")
        }
        return
    }

    val title = stringResource(details.titleRes)
    val explanation = if (details.explanationRes == R.string.card_details_explanation_template) {
        stringResource(details.explanationRes, title)
    } else {
        stringResource(details.explanationRes)
    }
    val howItWorks = if (details.howItWorksRes == R.string.card_details_how_it_works_template) {
        stringResource(details.howItWorksRes, title)
    } else {
        stringResource(details.howItWorksRes)
    }
    val whyItMatters = if (details.whyItMattersRes == R.string.card_details_why_it_matters_template) {
        stringResource(details.whyItMattersRes, title)
    } else {
        stringResource(details.whyItMattersRes)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = details.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Explanation Section
        Text(
            text = stringResource(R.string.card_details_what_is_it),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // How it works Section
        Text(
            text = stringResource(R.string.card_details_how_it_works),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = howItWorks,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Why it matters Section
        Text(
            text = stringResource(R.string.card_details_why_it_matters),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = whyItMatters,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeMedium
        ) {
            Text(stringResource(R.string.done))
        }
    }
}

private fun getCoreTemperature(coreIdx: Int, zones: List<ThermalInfo>, fallbackTemp: Float): String {
    val queries = listOf("cpu$coreIdx", "cpu-$coreIdx", "tsens_tz_sensor$coreIdx", "cpu_${coreIdx}_usr")
    val zone = zones.find { zone ->
        val nameLower = zone.name.lowercase()
        queries.any { query -> nameLower.contains(query) }
    }
    return if (zone != null) {
        zone.temperature
    } else {
        if (fallbackTemp > 0f) "${"%.1f".format(fallbackTemp)}°C" else "N/A"
    }
}

@Composable
fun CpuCoresDetailScreen(
    v: DeviceInfoViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val info by v.socInfo.collectAsStateWithLifecycle()
    val realtimeDataState by v.dashboardRealtime.collectAsStateWithLifecycle()
    val thermalZones by v.thermalZones.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.cpu_cores_detail),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        info?.let { i ->
            val realtimeData = realtimeDataState
            if (realtimeData != null) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeExtraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(70.dp)
                                ) {
                                    val progress by animateFloatAsState(
                                        realtimeData.cpuUsage / 100f,
                                        tween(400),
                                        label = "cpuTotal"
                                    )
                                    CircularProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 6.dp,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        strokeCap = StrokeCap.Round
                                    )
                                    Text(
                                        "${realtimeData.cpuUsage}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = SoCUtils.getCommercialName(context),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${i.cores} Cores · ${i.bigLittle}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    if (realtimeData.cpuTemperature > 0f) {
                                        Text(
                                            text = "CPU Temp: ${"%.1f".format(realtimeData.cpuTemperature)}°C",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    i.cpuClusters.forEach { cluster ->
                        item {
                            Text(
                                text = "${cluster.name} (${cluster.architecture})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        
                        items(cluster.coreIndices) { coreIdx ->
                            val coreFreq = realtimeData.cpuCoreFrequencies.getOrNull(coreIdx) ?: 0L
                            val coreHistory = realtimeData.cpuCoreHistory.getOrNull(coreIdx) ?: emptyList()
                            val coreUtil = coreHistory.lastOrNull()?.toInt() ?: 0
                            val coreTemp = getCoreTemperature(coreIdx, thermalZones, realtimeData.cpuTemperature)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ShapeCard,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.core_format, coreIdx),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (coreFreq > 0) "$coreFreq MHz" else stringResource(R.string.offline),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (coreFreq > 0) MaterialTheme.colorScheme.primary else Color.Gray
                                            )
                                            Text(
                                                text = coreTemp,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "$coreUtil%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(45.dp)
                                        )
                                        
                                        val progressVal by animateFloatAsState(
                                            coreUtil / 100f,
                                            tween(400),
                                            label = "coreProgress_$coreIdx"
                                        )
                                        
                                        LinearProgressIndicator(
                                            progress = { progressVal },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(8.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                    
                                    if (coreHistory.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val chartColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        CoreHistorySparkline(
                                            coreHistory = coreHistory,
                                            chartColor = chartColor,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
