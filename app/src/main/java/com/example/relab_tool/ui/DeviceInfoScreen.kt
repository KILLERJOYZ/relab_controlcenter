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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
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
fun BatteryHistoryCard(
    batteryHistory: List<Triple<Long, Int, Boolean>>,
    lastFullChargeTs: Long,
    lastStoppedChargingTs: Long
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val chargingColor = Color(0xFF4CAF50)
    val dischargingColor = Color(0xFF607D8B)

    InfoGroupCard(
        title = stringResource(R.string.battery_history_title),
        icon = Icons.Default.History,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                val gridColor = onSurfaceColor.copy(alpha = 0.1f)
                drawLine(gridColor, Offset(0f, 0f), Offset(0f, size.height))
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height))
                val textPaint = android.graphics.Paint().apply {
                    color = onSurfaceColor.copy(alpha = 0.4f).toArgb()
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                listOf(0, 12, 24).forEach { h ->
                    val x = (h / 24f) * size.width
                    drawContext.canvas.nativeCanvas.drawText(h.toString(), x, size.height + 14.dp.toPx(), textPaint)
                }
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                drawContext.canvas.nativeCanvas.drawText("100%", size.width + 4.dp.toPx(), 10.dp.toPx(), textPaint)
                drawContext.canvas.nativeCanvas.drawText("0%", size.width + 4.dp.toPx(), size.height, textPaint)
                if (batteryHistory.isNotEmpty()) {
                    val sortedPoints = batteryHistory.sortedBy { it.first }
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
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                        )
                    }
                }
                val now = System.currentTimeMillis()
                val nowX = ((now - startOfDay).toFloat() / (24 * 60 * 60 * 1000f)) * size.width
                if (nowX in 0f..size.width) {
                    val trianglePath = Path().apply { moveTo(nowX, size.height); lineTo(nowX - 6.dp.toPx(), size.height + 8.dp.toPx()); lineTo(nowX + 6.dp.toPx(), size.height + 8.dp.toPx()); close() }
                    drawPath(trianglePath, Color.White)
                    drawLine(Color.White.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(nowX, 0f), androidx.compose.ui.geometry.Offset(nowX, size.height), strokeWidth = 1.dp.toPx())
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(dischargingColor)); Spacer(modifier = Modifier.width(4.dp)); Text("Discharging", style = MaterialTheme.typography.labelSmall) }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(chargingColor)); Spacer(modifier = Modifier.width(4.dp)); Text("Charging", style = MaterialTheme.typography.labelSmall) }
        }
        if (lastFullChargeTs > 0 || lastStoppedChargingTs > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            if (lastFullChargeTs > 0) {
                val duration = System.currentTimeMillis() - lastFullChargeTs
                val hours = duration / (1000 * 60 * 60); val minutes = (duration / (1000 * 60)) % 60
                Text("Last charge to 100%: ${hours}h ${minutes}m ago", style = MaterialTheme.typography.labelSmall, color = onSurfaceColor.copy(alpha = 0.5f))
            }
            if (lastStoppedChargingTs > 0) {
                val duration = System.currentTimeMillis() - lastStoppedChargingTs
                val minutes = duration / (1000 * 60)
                if (minutes < 60) Text("Stopped charging $minutes minutes ago", style = MaterialTheme.typography.labelSmall, color = onSurfaceColor.copy(alpha = 0.5f))
                else { val hours = minutes / 60; Text("Stopped charging ${hours}h ${minutes % 60}m ago", style = MaterialTheme.typography.labelSmall, color = onSurfaceColor.copy(alpha = 0.5f)) }
            }
        }
    }
}

@Composable
fun WattageHistoryCard(wattage: String, wattageHistory: List<Float>, onClick: (() -> Unit)? = null) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val isCharging = (wattage.replace(" W", "").toFloatOrNull() ?: 0f) >= 0
    InfoGroupCard(
        title = if (isCharging) stringResource(R.string.charging_speed) else "Discharging Speed",
        icon = Icons.Default.Bolt,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.current_now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(wattage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = secondaryColor) }
            Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (wattageHistory.isNotEmpty()) {
                        val maxWattage = (wattageHistory.maxOrNull() ?: 1f).coerceAtLeast(10f); val minWattage = (wattageHistory.minOrNull() ?: 0f).coerceAtMost(0f); val range = (maxWattage - minWattage).coerceAtLeast(1f)
                        if (minWattage < 0) { val zeroY = size.height - ((0f - minWattage) / range * size.height); drawLine(color = onSurfaceColor.copy(alpha = 0.2f), start = androidx.compose.ui.geometry.Offset(0f, zeroY), end = androidx.compose.ui.geometry.Offset(size.width, zeroY), strokeWidth = 1.dp.toPx()) }
                        val path = Path(); val stepX = if (wattageHistory.size > 1) size.width / (wattageHistory.size - 1) else size.width
                        wattageHistory.forEachIndexed { i, v -> val x = i * stepX; val y = size.height - ((v - minWattage) / range * size.height); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                        drawPath(path, secondaryColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
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
        shape = RoundedCornerShape(28.dp),
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
    Surface(modifier = Modifier.width(160.dp).height(100.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("${c.coreCount} Cores", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(c.architecture, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${c.minFreq} - ${c.maxFreq}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun CpuCoreBox(index: Int, freq: Long, history: List<Float>, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(100.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Box(modifier = Modifier.fillMaxSize()) { if (history.isNotEmpty()) { val chartColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f); Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter)) { val path = Path(); val stepX = size.width / 19f; history.forEachIndexed { i, v -> val x = i * stepX; val y = size.height - (v / 100f * size.height); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; if (history.size > 1) { val fill = Path().apply { addPath(path); lineTo((history.size - 1) * stepX, size.height); lineTo(0f, size.height); close() }; drawPath(fill, chartColor); drawPath(path, chartColor.copy(alpha = 0.4f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)) } } } ; Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(stringResource(R.string.core_format, index), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)); Spacer(modifier = Modifier.height(4.dp)); Text(if (freq > 0) "$freq" else stringResource(R.string.offline), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (freq > 0) MaterialTheme.colorScheme.primary else Color.Gray); if (freq > 0) Text("MHz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontWeight = FontWeight.Bold) } } }
}

@Composable fun UsageHistoryDialog(title: String, current: Int, history: List<Float>, color: Color, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("$current%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = color); Box(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 16.dp)) { Canvas(modifier = Modifier.fillMaxSize()) { if (history.isNotEmpty()) { val path = Path(); val stepX = size.width / 20f; history.forEachIndexed { i, v -> val x = i * stepX; val y = size.height - (v / 100f * size.height); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; drawPath(path, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) } } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
}

@Composable fun WattageFullHistoryDialog(wattage: String, history: List<Float>, onDismiss: () -> Unit) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error
    val isCharging = (wattage.replace(" W", "").toFloatOrNull() ?: 0f) >= 0
    val peakWattage = history.maxOrNull() ?: 0f
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isCharging) stringResource(R.string.charging_speed) else "Discharging Speed") }, text = {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.current_now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(wattage, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = secondaryColor) }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Peak Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("%.2f W".format(Locale.US, peakWattage), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = errorColor) }
            Box(modifier = Modifier.fillMaxWidth().height(280.dp).padding(start = 32.dp, top = 16.dp, bottom = 32.dp, end = 8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridColor = onSurfaceColor.copy(alpha = 0.1f)
                    val labelPaint = android.graphics.Paint().apply { color = onSurfaceColor.copy(alpha = 0.4f).toArgb(); textSize = 10.dp.toPx() }
                    val maxW = (history.maxOrNull() ?: 0f).coerceAtLeast(25f); val minW = (history.minOrNull() ?: 0f).coerceAtMost(0f)
                    val yLimit = (((maxW / 25).toInt() + 1) * 25).toFloat(); val yMin = if (minW < 0) (((minW / 25).toInt() - 1) * 25).toFloat() else 0f
                    val yRange = yLimit - yMin
                    var yS = yMin; while (yS <= yLimit) { val yP = size.height - ((yS - yMin) / yRange * size.height); drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, yP), androidx.compose.ui.geometry.Offset(size.width, yP)); drawContext.canvas.nativeCanvas.drawText("${yS.toInt()}W", -32.dp.toPx(), yP + 4.dp.toPx(), labelPaint); yS += 25f }
                    val pointsPer5Min = 1000f; val xInt = (history.size / pointsPer5Min).toInt()
                    for (i in 0..xInt) { val xP = (i * pointsPer5Min / history.size.coerceAtLeast(1).toFloat()) * size.width; drawLine(gridColor, androidx.compose.ui.geometry.Offset(xP, 0f), androidx.compose.ui.geometry.Offset(xP, size.height)); labelPaint.textAlign = android.graphics.Paint.Align.CENTER; drawContext.canvas.nativeCanvas.drawText("${i * 5}m", xP, size.height + 16.dp.toPx(), labelPaint) }
                    val peakY = size.height - ((peakWattage - yMin) / yRange * size.height); drawLine(color = errorColor.copy(alpha = 0.4f), start = androidx.compose.ui.geometry.Offset(0f, peakY), end = androidx.compose.ui.geometry.Offset(size.width, peakY), strokeWidth = 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    if (history.isNotEmpty()) { val path = Path(); val stepX = size.width / (history.size - 1).coerceAtLeast(1); history.forEachIndexed { i, v -> val x = i * stepX; val y = size.height - ((v - yMin) / yRange * size.height); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; drawPath(path, secondaryColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)) }
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
    val context = LocalContext.current; var exp by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), onClick = { exp = !exp }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Text(app.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } }; Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(app.packageName, style = MaterialTheme.typography.bodySmall, maxLines = 1) }; Row { if (app.isGame) BadgeTag(stringResource(R.string.tag_game), MaterialTheme.colorScheme.primaryContainer); if (app.isSystem) BadgeTag(stringResource(R.string.tag_system), MaterialTheme.colorScheme.tertiaryContainer) } }; AnimatedVisibility(visible = exp, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) { Column(modifier = Modifier.padding(top = 16.dp)) { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(modifier = Modifier.height(12.dp)); InfoRow(stringResource(R.string.version), app.version); InfoRow(stringResource(R.string.target_sdk), app.sdk); if (app.updateUrl != null) { Spacer(modifier = Modifier.height(8.dp)); Button(onClick = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(app.updateUrl))) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.update_apkpure)) } } } } } }
}

@Composable fun DashboardRamCard(
    total: Long,
    used: Long,
    history: List<Float>,
    size: CardSize,
    modifier: Modifier = Modifier
) {
    val usedPercent = if (total > 0) (used.toFloat() / total.toFloat() * 100).toInt() else 0
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
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
                            val path = Path()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                val fill = Path().apply {
                                    addPath(path)
                                    lineTo((history.size - 1) * stepX, canvasHeight)
                                    lineTo(0f, canvasHeight)
                                    close()
                                }
                                drawPath(fill, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.4f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
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
    val progress by animateFloatAsState(usage / 100f, tween(400), label = "cpu")
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
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
                            val path = Path()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                val fill = Path().apply {
                                    addPath(path)
                                    lineTo((history.size - 1) * stepX, canvasHeight)
                                    lineTo(0f, canvasHeight)
                                    close()
                                }
                                drawPath(fill, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.3f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
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
                            val path = Path()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                val fill = Path().apply {
                                    addPath(path)
                                    lineTo((history.size - 1) * stepX, canvasHeight)
                                    lineTo(0f, canvasHeight)
                                    close()
                                }
                                drawPath(fill, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.3f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
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
    val progress by animateFloatAsState(usage / 100f, tween(400), label = "gpu")
    Card(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
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
                            val path = Path()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                val fill = Path().apply {
                                    addPath(path)
                                    lineTo((history.size - 1) * stepX, canvasHeight)
                                    lineTo(0f, canvasHeight)
                                    close()
                                }
                                drawPath(fill, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.3f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
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
                            val path = Path()
                            val stepX = canvasWidth / 19f
                            history.forEachIndexed { i, v ->
                                val x = i * stepX
                                val y = canvasHeight - (v / 100f * canvasHeight)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            if (history.size > 1) {
                                val fill = Path().apply {
                                    addPath(path)
                                    lineTo((history.size - 1) * stepX, canvasHeight)
                                    lineTo(0f, canvasHeight)
                                    close()
                                }
                                drawPath(fill, chartColor)
                                drawPath(path, chartColor.copy(alpha = 0.3f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
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
    Card(modifier = modifier.height(110.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)) {
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
        shape = RoundedCornerShape(24.dp)
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
    content: @Composable (CardSize) -> Unit
) {

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Calculate grid dimensions dynamically for snapping
    val screenWidthDp = configuration.screenWidthDp.dp
    val gridWidthDp = screenWidthDp - 32.dp
    val cellWidthDp = (gridWidthDp - 36.dp) / 4
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

    // Clamping values
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
        val targetSpan = (visualWidthPx / cellWidthPx).roundToInt().coerceIn(1, 4)
        val targetHeightCells = (visualHeightPx / cellHeightPx).roundToInt().coerceIn(1, 4)
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
                        shape = RoundedCornerShape(24.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
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
                        shape = RoundedCornerShape(24.dp)
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
    val batteryHistory by viewModel.batteryCapacityHistory.collectAsStateWithLifecycle()
    val lastFullChargeTs by viewModel.lastFullChargeTs.collectAsStateWithLifecycle()
    val lastStoppedChargingTs by viewModel.lastStoppedChargingTs.collectAsStateWithLifecycle()
    val hasData by remember { derivedStateOf { dataState != null } }
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    var showBatteryDialog by remember { mutableStateOf(false) }
    val bottomContentPadding = if (isWideScreen) 16.dp else 120.dp

    val onMemoryClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(7) } }
    val onSoCClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(1) } }
    val onWifiClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(8) } }
    val onBluetoothClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(4) } }
    val onCellularClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(9) } }
    val onDisplayClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(5) } }
    val onDrmClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(6) } }
    val onSensorsClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(12) } }
    val onAppsClick = remember(onNavigateToInfoTab) { { onNavigateToInfoTab(13) } }
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
        val statusItems = remember(data) {
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
                    onClick = { onNavigateToInfoTab(14) }
                ),
                StatusCardItem(
                    key = "touch_sampling",
                    title = context.getString(R.string.touch_sampling_rate),
                    icon = Icons.Outlined.TouchApp,
                    value = "${data.touchSamplingRate} Hz",
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
                    value = data.diskReadSpeed,
                    subtext = "${context.getString(R.string.write_speed)}: ${data.diskWriteSpeed}",
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
                if (data.ambientLightLux > 0 || data.pressureHpa > 0) {
                    StatusCardItem(
                        key = "ambient_light",
                        title = context.getString(R.string.ambient_light),
                        icon = Icons.Outlined.LightMode,
                        value = context.getString(R.string.unit_lux, data.ambientLightLux),
                        subtext = context.getString(R.string.unit_hpa, data.pressureHpa),
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
                    onClick = onBatteryClick
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
                    value = context.getString(R.string.fps_format, data.currentRefreshRate),
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
                    value = data.downloadSpeed,
                    subtext = context.getString(R.string.tab_network),
                    containerColor = primary,
                    contentColor = onPrimary,
                    onClick = onWifiClick
                ),
                StatusCardItem(
                    key = "upload",
                    title = context.getString(R.string.tab_network) + context.getString(R.string.network_ul),
                    icon = Icons.Default.ArrowUpward,
                    value = data.uploadSpeed,
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
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = bottomContentPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
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
                                    onReorderEnd = onReorderEndCallback) { size ->
                                    DashboardRamCard(total = data.ramTotal, used = data.ramUsed, history = data.ramHistory, size = size)
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
                                    onReorderEnd = onReorderEndCallback) { size ->
                                    DashboardCpuCard(usage = data.cpuUsage, history = data.cpuHistory, size = size)
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
                                    onReorderEnd = onReorderEndCallback) { size ->
                                    DashboardGpuCard(usage = data.gpuUsage, history = data.gpuHistory, size = size)
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
                                    onReorderEnd = onReorderEndCallback) { size ->
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
                                                Spacer(modifier = Modifier.height(12.dp))
                                                val columns = if (size == CardSize.SIZE_4x2 || size == CardSize.SIZE_4x4) {
                                                    if (isWideScreen) 8 else 4
                                                } else 2
                                                
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    data.cpuCoreFrequencies.withIndex().chunked(columns).forEach { chunk ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            chunk.forEach { indexedFreq ->
                                                                CpuCoreBox(
                                                                    index = indexedFreq.index,
                                                                    freq = indexedFreq.value,
                                                                    history = data.cpuCoreHistory.getOrNull(indexedFreq.index) ?: emptyList(),
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }
                                                            repeat(columns - chunk.size) {
                                                                Spacer(modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (size == CardSize.SIZE_2x1) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "Max Freq: ${data.cpuCoreFrequencies.maxOrNull() ?: 0} MHz",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    "${(data.cpuCoreFrequencies.maxOrNull() ?: 0) / 1000f}G",
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
                                    onReorderEnd = onReorderEndCallback) { size ->
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
                                    onReorderEnd = onReorderEndCallback) { size ->

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
                                        onReorderEnd = onReorderEndCallback) { sizeParam ->

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

@Composable fun DeviceTab(viewModel: DeviceInfoViewModel) {
    val summary by viewModel.deviceSummary.collectAsState(); val system by viewModel.systemInfo.collectAsState(); val socName = remember { SoCUtils.getCommercialName(viewModel.getApplication()) }
    summary?.let { s -> LazyColumn(contentPadding = PaddingValues(16.dp)) { item { ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Smartphone, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) } }; Spacer(modifier = Modifier.height(12.dp)); Text(s.model, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) } } }; item { Spacer(modifier = Modifier.height(16.dp)) }; item { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Card(modifier = Modifier.weight(1f).height(110.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) { Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Outlined.Android, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.height(8.dp)); Text("Android ${s.androidVersion}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSecondaryContainer, textAlign = TextAlign.Center); Text("Version", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f), textAlign = TextAlign.Center) } }; Card(modifier = Modifier.weight(1f).height(110.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))) { Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.height(8.dp)); Text(system?.osVersion ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onTertiaryContainer, textAlign = TextAlign.Center, maxLines = 2); Text("UI Version", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f), textAlign = TextAlign.Center) } } } }; item { Spacer(modifier = Modifier.height(16.dp)) }; item { InfoGroupCard(stringResource(R.string.hardware_platform), Icons.Outlined.SettingsInputComponent, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) { InfoRow(stringResource(R.string.model), s.model); system?.let { InfoRow(stringResource(R.string.model_name), it.modelName) }; InfoRow(stringResource(R.string.board), s.board); InfoRow(stringResource(R.string.platform), s.platform); InfoRow(stringResource(R.string.hardware), s.hardware); InfoRow(stringResource(R.string.processor), socName); InfoRow(stringResource(R.string.tab_memory), s.ramType); InfoRow(stringResource(R.string.flash), s.flashType) } }; item { Spacer(modifier = Modifier.height(16.dp)) }; item { InfoGroupCard(stringResource(R.string.tab_system), Icons.Outlined.Android, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.secondary) { InfoRow(stringResource(R.string.android), s.androidVersion); system?.let { InfoRow(stringResource(R.string.api), it.sdkLevel); InfoRow(stringResource(R.string.codename), it.codeName); InfoRow(stringResource(R.string.security), it.securityPatch) }; InfoRow(stringResource(R.string.kernel), s.kernel) } }; item { Spacer(modifier = Modifier.height(16.dp)) }; item { InfoGroupCard(stringResource(R.string.build_details), Icons.Outlined.Build, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.tertiary) { system?.let { InfoRow(stringResource(R.string.build), it.buildId); InfoRow(stringResource(R.string.java_vm), it.javaVm); InfoRow(stringResource(R.string.baseband), it.baseband); InfoRow(stringResource(R.string.bootloader), it.bootloader); InfoRow(stringResource(R.string.gps), it.gps); InfoRow(stringResource(R.string.bluetooth), it.bluetoothVersion); InfoRow(stringResource(R.string.build_type), it.buildType); InfoRow(stringResource(R.string.tags), it.tags); InfoRow(stringResource(R.string.incremental), it.incremental) } } }; item { Spacer(modifier = Modifier.height(16.dp)) }; item { InfoGroupCard(stringResource(R.string.screen), Icons.Outlined.DisplaySettings, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary) { InfoRow(stringResource(R.string.resolution), s.resolution); InfoRow(stringResource(R.string.touchscreen), s.touchscreen) } }; item { Spacer(modifier = Modifier.height(16.dp)) }; item { InfoGroupCard(stringResource(R.string.system_identifiers), Icons.Outlined.Fingerprint, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant) { InfoRow(stringResource(R.string.android_id), s.androidId); s.gsfId?.let { gsf -> InfoRow(stringResource(R.string.gsf_id), gsf) }; InfoRow(stringResource(R.string.tab_device), s.device); system?.let { InfoRow(stringResource(R.string.product), it.product) }; InfoRow(stringResource(R.string.fingerprint), s.buildFingerprint) } }; item { Spacer(modifier = Modifier.height(16.dp)) }; item { InfoGroupCard(stringResource(R.string.miscellaneous), Icons.Outlined.Dashboard, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)) { system?.let { InfoRow(stringResource(R.string.gms), it.googlePlayServices); InfoRow(stringResource(R.string.device_features), it.deviceFeatures); InfoRow(stringResource(R.string.language), it.language); InfoRow(stringResource(R.string.timezone), it.timezone); InfoRow(stringResource(R.string.uptime), it.uptime) } } } } }
}

@Composable fun SocTab(v: DeviceInfoViewModel) {
    val info by v.socInfo.collectAsState(); val context = LocalContext.current; val socName = remember { SoCUtils.getCommercialName(context) }
    info?.let { i -> 
        LazyColumn(contentPadding = PaddingValues(16.dp)) { 
            item {
                DashboardStatusCard(
                    title = stringResource(R.string.processor),
                    icon = Icons.Outlined.Memory,
                    value = socName,
                    subtext = i.vendor,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.cores),
                        icon = Icons.Outlined.Numbers,
                        value = i.cores,
                        subtext = i.bigLittle,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.gpu),
                        icon = Icons.Outlined.GraphicEq,
                        value = i.gpu,
                        subtext = i.gpuVendor,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            if (i.cpuClusters.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(12.dp)) }
                item {
                    InfoGroupCard(
                        stringResource(R.string.clusters),
                        Icons.Outlined.Layers,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            items(i.cpuClusters) { cluster -> CpuClusterBox(cluster) }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { 
                InfoGroupCard(
                    stringResource(R.string.cpu), 
                    Icons.Outlined.Memory, 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                    MaterialTheme.colorScheme.primary
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
                                shape = RoundedCornerShape(8.dp),
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
                    stringResource(R.string.gpu), 
                    Icons.Outlined.GraphicEq, 
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), 
                    MaterialTheme.colorScheme.secondary
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
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { 
                InfoGroupCard(
                    stringResource(R.string.technology), 
                    Icons.Outlined.Science, 
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), 
                    MaterialTheme.colorScheme.tertiary
                ) { 
                    InfoRow(stringResource(R.string.process), i.process) 
                } 
            }
        }
    }
}

@Composable fun BatteryTab(viewModel: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val info by viewModel.batteryInfo.collectAsState()
    val batteryHistory by viewModel.batteryCapacityHistory.collectAsState()
    val lastFullChargeTs by viewModel.lastFullChargeTs.collectAsState()
    val lastStoppedChargingTs by viewModel.lastStoppedChargingTs.collectAsState()
    val dashboardData by viewModel.dashboardData.collectAsState()
    val wattageHistory = dashboardData?.wattageHistory ?: emptyList()
    val fullWattageHistory by viewModel.fullWattageHistory.collectAsState()
    var showFullWattage by remember { mutableStateOf(false) }
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    if (showFullWattage) {
        WattageFullHistoryDialog(info?.wattage ?: "0.00 W", fullWattageHistory) { showFullWattage = false }
    }

    info?.let { i -> 
        LazyColumn(contentPadding = PaddingValues(16.dp)) { 
            item { 
                val cardColor = if (i.isCharging) Color(0xFF2CBF6C) else MaterialTheme.colorScheme.primary
                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor, contentColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background bubbles (Xiaomi style)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(Color.White.copy(alpha = 0.1f), radius = 20.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.2f))
                            drawCircle(Color.White.copy(alpha = 0.05f), radius = 40.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.7f))
                            drawCircle(Color.White.copy(alpha = 0.08f), radius = 15.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * 0.6f, size.height * 0.85f))
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
                                    .background(Color.White.copy(alpha = 0.3f))
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
                                    color = Color.White,
                                    maxLines = 1
                                )
                                
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                    if (i.isCharging) Icon(Icons.Default.Bolt, null, modifier = Modifier.size(22.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (i.isCharging) {
                                            if (i.timeToFull > 0) stringResource(id = R.string.time_to_full) + " | " + stringResource(id = R.string.charged_format, i.level) else stringResource(id = R.string.calculating_charge_time) + " | ${i.level}%"
                                        } else {
                                            if (i.timeToFull > 0) stringResource(id = R.string.remaining_usage_time) + " | " + stringResource(id = R.string.battery_format, i.level) else stringResource(id = R.string.analyzing_usage) + " | " + stringResource(id = R.string.battery_format, i.level)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = 0.95f),
                                        textAlign = TextAlign.Center
                                    )
                                }
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
                    Triple("Power", Icons.Outlined.Power, Pair(i.powerSource, stringResource(id = R.string.source)))
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
                                DashboardStatusCard(
                                    modifier = Modifier.weight(1f),
                                    title = when(title) {
                                        "Temperature" -> stringResource(id = R.string.tab_thermal)
                                        "Wattage" -> stringResource(id = R.string.charging_speed)
                                        "Power" -> stringResource(id = R.string.source)
                                        else -> title
                                    },
                                    icon = icon,
                                    value = value,
                                    subtext = subtext,
                                    containerColor = containerColor,
                                    contentColor = contentColor,
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
            item { InfoGroupCard(stringResource(R.string.health_specs), Icons.Outlined.HealthAndSafety, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) { InfoRow(stringResource(R.string.health), i.health); InfoRow(stringResource(id = R.string.battery_wear), i.wear); InfoRow(stringResource(id = R.string.actual_capacity), i.actualCapacity); InfoRow(stringResource(R.string.capacity), i.capacity); InfoRow(stringResource(R.string.technology), i.technology); InfoRow(stringResource(id = R.string.charging_speed), i.wattage); InfoRow(stringResource(R.string.current_now), i.currentNow); InfoRow(stringResource(R.string.cycle_count), i.chargeCounter) } }
        } 
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable fun DisplayTab(v: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val info by v.displayInfo.collectAsState()
    info?.let { i -> 
        LazyColumn(contentPadding = PaddingValues(16.dp)) { 
            item {
                DashboardStatusCard(
                    title = stringResource(R.string.resolution),
                    icon = Icons.Outlined.Monitor,
                    value = i.currentResolution,
                    subtext = stringResource(id = R.string.aspect_ratio) + ": ${i.aspectRatio}",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.color_depth),
                        icon = Icons.Outlined.Palette,
                        value = i.colorDepth,
                        subtext = i.colorDepthSubtext,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.refresh_rate),
                        icon = Icons.Outlined.Refresh,
                        value = i.currentRefreshRate,
                        subtext = stringResource(id = R.string.high_refresh_support),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.physical_size),
                        icon = Icons.Outlined.AspectRatio,
                        value = i.physicalSize,
                        subtext = i.ppi,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(id = R.string.wide_color_gamut),
                        icon = Icons.Outlined.Palette,
                        value = if (i.wideColorGamut) stringResource(id = R.string.supported) else stringResource(id = R.string.not_supported),
                        subtext = if (i.wideColorGamut) stringResource(id = R.string.p3_display) else stringResource(id = R.string.standard_rgb),
                        containerColor = if (i.wideColorGamut) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        contentColor = if (i.wideColorGamut) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { 
                InfoGroupCard(
                    stringResource(R.string.screen_specs), 
                    Icons.Outlined.Monitor, 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                    MaterialTheme.colorScheme.secondary
                ) { 
                    InfoRow(stringResource(R.string.density), i.density)
                    InfoRow("X DPI", i.xDpi)
                    InfoRow("Y DPI", i.yDpi)
                    InfoRow("PPI", i.ppi)
                    InfoRow(stringResource(R.string.brightness), i.brightnessLevel)
                    InfoRow(stringResource(R.string.timeout), i.screenTimeout)
                    InfoRow("Orientation", i.orientation) 
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
                InfoGroupCard(stringResource(R.string.tab_memory), Icons.Outlined.Memory, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), MaterialTheme.colorScheme.primary) {
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
                InfoGroupCard(stringResource(R.string.internal_storage), Icons.Outlined.SdStorage, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f), MaterialTheme.colorScheme.secondary) {
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
                            StorageDetailRow(stringResource(id = R.string.apps_and_data), i.internalUsedByApps, Color(0xFF2CBF6C))
                            StorageDetailRow(stringResource(id = R.string.tab_system), i.internalUsedBySystem, Color(0xFF006400))
                            StorageDetailRow(stringResource(id = R.string.free_format, "").trim(), i.internalFree, MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            item {
                InfoGroupCard(stringResource(R.string.filesystem_details), Icons.Outlined.Dns, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f), MaterialTheme.colorScheme.tertiary) {
                    InfoRow("FS Type", i.internalFsType)
                    InfoRow("Block Size", i.internalBlockSize)
                    InfoRow("Memory Page Size", i.memoryPageSize)
                    InfoRow("Partition", i.internalPartition)
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
    val greenApps = Color(0xFF2CBF6C)
    val darkGreenSystem = Color(0xFF006400)

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
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(appsRatio).background(greenApps))
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
                                .background(darkGreenSystem)
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

@Composable fun StorageCard(title: String, icon: ImageVector, total: String, available: String, totalBytes: Long, availableBytes: Long, container: Color, content: Color) {
    val used = if (totalBytes > 0) ((totalBytes - availableBytes).toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val context = androidx.compose.ui.platform.LocalContext.current
    val usedStr = android.text.format.Formatter.formatFileSize(context, (totalBytes - availableBytes).coerceAtLeast(0L))
    
    InfoGroupCard(title, icon, container, content) {
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
    val info by v.networkInfo.collectAsState()
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    info?.let { i -> 
        LazyColumn(contentPadding = PaddingValues(16.dp)) { 
            item {
                DashboardStatusCard(
                    title = "SSID",
                    icon = Icons.Outlined.Wifi,
                    value = i.wifiSsid ?: "Disconnected",
                    subtext = i.wifiBssid ?: i.state,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.link_speed),
                        icon = Icons.Outlined.Speed,
                        value = i.linkSpeed ?: "N/A",
                        subtext = "Current Speed",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    DashboardStatusCard(
                        modifier = Modifier.weight(1f),
                        title = "Standard",
                        icon = Icons.Outlined.SettingsInputAntenna,
                        value = i.standard?.substringAfter("(")?.substringBefore(")") ?: "N/A",
                        subtext = i.standard?.substringBefore("(") ?: "WiFi Standard",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }

            item {
                val featureColumns = if (isWideScreen) 5 else 3
                val wifiStandardStr = "802.11 b/a/g/n/ac/ax"
                val wifi5GHzStr = "5 GHz Support"
                val wifi6GHzStr = "6 GHz Support"
                val wifiDirectStr = "Wi-Fi Direct"
                val wifiAwareStr = "Wi-Fi Aware"
                val hotspotStr = "Mobile Hotspot"
                
                val features = remember(i.is5GHzSupported, i.is6GHzSupported, i.isWifiDirectSupported, i.isWifiAwareSupported, i.isApSupported) {
                    listOf(
                        Triple(wifiStandardStr, Icons.Outlined.SettingsInputAntenna, true),
                        Triple(wifi5GHzStr, Icons.Outlined.Wifi, i.is5GHzSupported),
                        Triple(wifi6GHzStr, Icons.Outlined.Wifi, i.is6GHzSupported),
                        Triple(wifiDirectStr, Icons.Outlined.WifiTethering, i.isWifiDirectSupported),
                        Triple(wifiAwareStr, Icons.Outlined.NearbyError, i.isWifiAwareSupported),
                        Triple(hotspotStr, Icons.Outlined.WifiTethering, i.isApSupported)
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Hardware Capabilities",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    features.chunked(featureColumns).forEach { chunk ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunk.forEach { (t, ic, s) ->
                                DashboardFeatureCard(modifier = Modifier.weight(1f), title = t, icon = ic, isSupported = s)
                            }
                            repeat(featureColumns - chunk.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            item { 
                InfoGroupCard(stringResource(R.string.connection), if (i.type == "WiFi") Icons.Outlined.Wifi else Icons.Outlined.SignalCellularAlt, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) { 
                    InfoRow(stringResource(R.string.status), i.state)
                    InfoRow(stringResource(R.string.type), i.type)
                    i.interfaceName?.let { InfoRow("Interface", it) }
                    i.vendor?.let { InfoRow("Vendor", it) }
                } 
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            if (i.type == "WiFi") {
                item {
                    InfoGroupCard("WiFi Details", Icons.Outlined.SettingsInputAntenna, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary) {
                        i.frequency?.let { InfoRow(stringResource(R.string.frequency), it) }
                        i.channel?.let { InfoRow("Channel", it) }
                        i.width?.let { InfoRow("Bandwidth", it) }
                        i.signalStrength?.let { InfoRow("Signal Strength", it) }
                        i.security?.let { InfoRow("Security", it) }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            item {
                InfoGroupCard("TCP/IP & DHCP", Icons.Outlined.Router, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f), MaterialTheme.colorScheme.tertiary) {
                    i.ipAddress?.let { InfoRow("IPv4 Address", it) }
                    i.ipv6Address?.let { InfoRow("IPv6 Address", it) }
                    i.gateway?.let { InfoRow("Gateway", it) }
                    i.netmask?.let { InfoRow("Netmask", it) }
                    i.dns1?.let { InfoRow("DNS 1", it) }
                    i.dns2?.let { InfoRow("DNS 2", it) }
                    i.dhcpServer?.let { InfoRow("DHCP Server", it) }
                    i.leaseDuration?.let { InfoRow("Lease Duration", it) }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        } 
    } ?: EmptyState(Icons.Outlined.SignalWifiOff, stringResource(R.string.no_network))
}

@Composable
fun CellularTab(v: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val info by v.networkInfo.collectAsState()
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
                                    modifier = Modifier.weight(1f),
                                    title = "SIM $slot Operator",
                                    icon = Icons.Outlined.SignalCellularAlt,
                                    value = sim?.carrier ?: "Disconnected",
                                    subtext = if (slot == 1) cell.state else (sim?.state ?: "Disconnected"),
                                    containerColor = if (slot == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                                    contentColor = if (slot == 1) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onTertiary
                                )
                            }
                        }
                    } else {
                        DashboardStatusCard(
                            title = "Network Operator",
                            icon = Icons.Outlined.SignalCellularAlt,
                            value = cell.networkOperator ?: "Disconnected",
                            subtext = cell.state,
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    InfoGroupCard(
                        "Cellular Details",
                        Icons.Outlined.SignalCellularAlt,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.secondary
                    ) {
                        InfoRow("Status", cell.state)
                        InfoRow("SIM Support", cell.multiSimSupport)
                        InfoRow("Network Operator", cell.networkOperator ?: "N/A")
                        InfoRow("Network Type", cell.networkType ?: "N/A")
                        InfoRow("APN", cell.apn ?: "N/A")
                        InfoRow("IPv4 Address", cell.ipV4 ?: "N/A")
                        if (cell.ipV6.isNotEmpty()) {
                            InfoRow("IPv6 Address", cell.ipV6.joinToString("\n"))
                        }
                        InfoRow("Interface", cell.interfaceName ?: "N/A")
                    }
                }

                cell.simInfos.forEach { sim ->
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        InfoGroupCard(
                            "SIM ${sim.slot}",
                            Icons.Outlined.SimCard,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.tertiary
                        ) {
                            InfoRow("Carrier", sim.carrier)
                            sim.phoneNumber?.let { InfoRow("Phone Number", it) }
                            InfoRow("Country ISO", sim.countryIso?.uppercase() ?: "N/A")
                            InfoRow("MCC", sim.mcc ?: "N/A")
                            InfoRow("MNC", sim.mnc ?: "N/A")
                            InfoRow("Roaming", if (sim.roaming) "Enabled" else "Disabled")
                            InfoRow("State", sim.state)
                        }
                    }
                }
            } else {
                item {
                    EmptyState(
                        Icons.Outlined.SignalCellularConnectedNoInternet0Bar,
                        "Cellular information requires Phone permission or hardware support."
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    } ?: EmptyState(Icons.Outlined.SignalCellularConnectedNoInternet0Bar, "No Network Information")
}


@Composable fun CodecsTab(v: DeviceInfoViewModel) {
    val codecs by v.codecs.collectAsState(); LazyColumn(contentPadding = PaddingValues(16.dp)) { itemsIndexed(codecs) { idx, c -> InfoGroupCard(c.name, Icons.Outlined.Audiotrack, (if (idx % 2 == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer).copy(alpha = 0.2f), if (idx % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary) { InfoRow(stringResource(R.string.type), c.type); InfoRow(stringResource(R.string.mime), c.mimeType) }; Spacer(modifier = Modifier.height(12.dp)) } }
}

@Composable fun UsbTab(v: DeviceInfoViewModel) {
    val devices by v.usbDevices.collectAsState(); if (devices.isEmpty()) EmptyState(Icons.Outlined.UsbOff, stringResource(R.string.no_usb))
    else LazyColumn(contentPadding = PaddingValues(16.dp)) { itemsIndexed(devices) { idx, u -> InfoGroupCard(u.name, Icons.Outlined.Usb, (if (idx % 2 == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer).copy(alpha = 0.2f), if (idx % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary) { InfoRow(stringResource(R.string.vendor_id), u.vendorId); InfoRow(stringResource(R.string.product_id), u.productId); InfoRow(stringResource(R.string.class_label), u.deviceClass) }; Spacer(modifier = Modifier.height(12.dp)) } }
}

@Composable fun SensorsTab(v: DeviceInfoViewModel) {
    val sensors by v.sensors.collectAsState(); LazyColumn(contentPadding = PaddingValues(16.dp)) { 
        itemsIndexed(sensors, key = { _, s -> s.name }, contentType = { _, _ -> "sensor" }) { idx, s -> 
            val c = when (idx % 3) { 0 -> MaterialTheme.colorScheme.primaryContainer; 1 -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.tertiaryContainer }
            val tc = when (idx % 3) { 0 -> MaterialTheme.colorScheme.primary; 1 -> MaterialTheme.colorScheme.secondary; else -> MaterialTheme.colorScheme.tertiary }
            InfoGroupCard(s.name, Icons.Outlined.Sensors, c.copy(alpha = 0.2f), tc) { 
                InfoRow(stringResource(R.string.vendor), s.vendor)
                InfoRow(stringResource(R.string.big_little), "${s.power} mA")
                InfoRow(stringResource(R.string.resolution), s.resolution.toString())
                InfoRow(stringResource(R.string.frequency), if (s.minDelay > 0) "${1000000 / s.minDelay} Hz" else "N/A") 
            }
            Spacer(modifier = Modifier.height(12.dp)) 
        } 
    }
}

@Composable fun AppsTab(v: DeviceInfoViewModel) {
    val apps by v.installedApps.collectAsState(); val playStore by v.isPlayStoreAvailable.collectAsState(); Column { if (!playStore) Button(onClick = { v.checkForUpdates() }, modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Update, null); Spacer(modifier = Modifier.width(8.dp)); Text(stringResource(R.string.check_updates)) }; LazyColumn(contentPadding = PaddingValues(16.dp)) { items(apps, key = { it.packageName }, contentType = { "app" }) { a -> AppEntryCard(a); Spacer(modifier = Modifier.height(8.dp)) } } }
}

@Composable fun CamerasTab(viewModel: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val cameraSpecViewModel: CameraSpecViewModel = viewModel()
    CameraSpecScreen(cameraSpecViewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(viewModel: DeviceInfoViewModel, windowSizeClass: WindowSizeClass) {
    val tabs = listOf(
        TabItem(stringResource(R.string.tab_device), Icons.Outlined.Smartphone),
        TabItem(stringResource(R.string.tab_soc), Icons.Outlined.Memory),
        TabItem(stringResource(R.string.tab_cameras), Icons.Outlined.PhotoCamera),
        TabItem(stringResource(R.string.tab_battery), Icons.Outlined.BatteryStd),
        TabItem(stringResource(R.string.tab_display), Icons.Outlined.DisplaySettings),
        TabItem(stringResource(R.string.tab_memory), Icons.Outlined.Storage),
        TabItem(stringResource(R.string.tab_wifi), Icons.Outlined.Wifi),
        TabItem(stringResource(R.string.tab_cellular), Icons.Outlined.SignalCellularAlt),
        TabItem(stringResource(R.string.tab_usb), Icons.Outlined.Usb),
        TabItem(stringResource(R.string.tab_codecs), Icons.Outlined.Audiotrack),
        TabItem(stringResource(R.string.tab_sensors), Icons.Outlined.Sensors),
        TabItem(stringResource(R.string.tab_apps), Icons.Outlined.Apps)
    )
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

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
                        4 -> DisplayTab(viewModel, windowSizeClass)
                        5 -> MemoryTab(viewModel, windowSizeClass)
                        6 -> WifiTab(viewModel, windowSizeClass)
                        7 -> CellularTab(viewModel, windowSizeClass)
                        8 -> UsbTab(viewModel)
                        9 -> CodecsTab(viewModel)
                        10 -> SensorsTab(viewModel)
                        11 -> AppsTab(viewModel)
                    }
                }
            }
        }
    }
}
