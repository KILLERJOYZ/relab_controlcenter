package com.example.relab_tool.benchmark.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.relab_tool.R
import com.example.relab_tool.benchmark.domain.model.*
import com.example.relab_tool.benchmark.scoring.RadarChartData
import com.example.relab_tool.benchmark.scoring.RadarChartEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TierBadge(tier: ScoreTier) {
    val containerColor = when (tier) {
        ScoreTier.ENTRY -> MaterialTheme.colorScheme.errorContainer
        ScoreTier.MID -> MaterialTheme.colorScheme.tertiaryContainer
        ScoreTier.HIGH -> MaterialTheme.colorScheme.secondaryContainer
        ScoreTier.FLAGSHIP -> MaterialTheme.colorScheme.primaryContainer
        ScoreTier.ELITE -> MaterialTheme.colorScheme.inversePrimary
    }
    
    val textColor = when (tier) {
        ScoreTier.ENTRY -> MaterialTheme.colorScheme.onErrorContainer
        ScoreTier.MID -> MaterialTheme.colorScheme.onTertiaryContainer
        ScoreTier.HIGH -> MaterialTheme.colorScheme.onSecondaryContainer
        ScoreTier.FLAGSHIP -> MaterialTheme.colorScheme.onPrimaryContainer
        ScoreTier.ELITE -> MaterialTheme.colorScheme.onPrimary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = stringResource(tier.labelRes).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

data class RadarChartLabelEntry(
    val entry: RadarChartEntry,
    val label: String
)

@Composable
fun RadarChartView(
    pillarScores: List<PillarScore>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val entriesWithLabels = remember(pillarScores) {
        RadarChartData.fromPillarScores(pillarScores).map { entry ->
            val baseLabel = when (entry.pillar) {
                BenchmarkPillar.CPU_SINGLE_CORE -> "CPU Single"
                BenchmarkPillar.CPU_MULTI_CORE -> "CPU Multi"
                BenchmarkPillar.GPU_RENDERING -> "GPU Render"
                BenchmarkPillar.GAMING_SIMULATION -> "Gaming"
                BenchmarkPillar.MEMORY -> "Memory"
                BenchmarkPillar.STORAGE_IO -> "Storage"
                BenchmarkPillar.AI_ML -> "AI/ML"
                BenchmarkPillar.UX_SMOOTHNESS -> "UX Smooth"
                BenchmarkPillar.CODEC_MEDIA -> "Codec"
                BenchmarkPillar.THERMAL_EFFICIENCY -> "Thermal"
                BenchmarkPillar.WIFI -> "Wi-Fi"
                BenchmarkPillar.CELLULAR -> "Cellular"
                BenchmarkPillar.BROWSER_WEB -> "Browser"
            }
            val scoreStr = if (entry.score > 0) " (${entry.score})" else ""
            RadarChartLabelEntry(entry, "$baseLabel$scoreStr")
        }
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val textColor = MaterialTheme.colorScheme.onSurface
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (Math.min(size.width, size.height) / 2f) * 0.62f
        val count = entriesWithLabels.size
        
        val gridLabelPaint = android.graphics.Paint().apply {
            color = textColor.copy(alpha = 0.4f).toArgb()
            textSize = 7.5.sp.toPx()
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }
        
        // 1. Draw 4 circular concentric guidelines (250, 500, 750, 1000 scores)
        for (i in 1..4) {
            val r = maxRadius * (i / 4f)
            drawCircle(
                color = outlineColor,
                radius = r,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Draw scale values (250, 500, 750, 1000) along the top vertical axis, slightly offset to the right
            drawContext.canvas.nativeCanvas.drawText(
                (i * 250).toString(),
                center.x + 6.dp.toPx(),
                center.y - r + 3.dp.toPx(),
                gridLabelPaint
            )
        }
        
        if (count == 0) return@Canvas
        val angleStep = (2f * Math.PI / count).toFloat()
        
        // 2. Draw spoke lines radiating from center & label text
        val labelPaint = android.graphics.Paint().apply {
            color = textColor.toArgb()
            textSize = 9.sp.toPx()
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        
        for (i in 0 until count) {
            val angle = i * angleStep - Math.PI.toFloat() / 2f
            val endX = center.x + maxRadius * cos(angle)
            val endY = center.y + maxRadius * sin(angle)
            drawLine(
                color = outlineColor,
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
            
            // Draw Label outside maxRadius
            val cosVal = cos(angle)
            val sinVal = sin(angle)
            val labelRadius = maxRadius + 14.dp.toPx()
            val labelX = center.x + labelRadius * cosVal
            val labelY = center.y + labelRadius * sinVal + when {
                sinVal < -0.8f -> -2.dp.toPx()
                sinVal > 0.8f -> 7.dp.toPx()
                else -> 3.dp.toPx()
            }
            
            labelPaint.textAlign = when {
                cosVal < -0.2f -> android.graphics.Paint.Align.RIGHT
                cosVal > 0.2f -> android.graphics.Paint.Align.LEFT
                else -> android.graphics.Paint.Align.CENTER
            }
            
            drawContext.canvas.nativeCanvas.drawText(
                entriesWithLabels[i].label,
                labelX,
                labelY,
                labelPaint
            )
        }
        
        // 3. Draw Reference Poly (baseline: score 500 on all spokes)
        val refPath = Path()
        for (i in 0 until count) {
            val angle = i * angleStep - Math.PI.toFloat() / 2f
            val r = maxRadius * 0.5f // 500 / 1000
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) refPath.moveTo(x, y) else refPath.lineTo(x, y)
        }
        refPath.close()
        drawPath(
            path = refPath,
            color = secondaryColor.copy(alpha = 0.5f),
            style = Stroke(width = 1.5.dp.toPx())
        )
        
        // 4. Draw Device Score Poly
        val devicePath = Path()
        for (i in 0 until count) {
            val angle = i * angleStep - Math.PI.toFloat() / 2f
            val scoreFraction = (entriesWithLabels[i].entry.score / 1000f).coerceIn(0f, 1f)
            val r = maxRadius * scoreFraction
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) devicePath.moveTo(x, y) else devicePath.lineTo(x, y)
        }
        devicePath.close()
        
        // Fill device path
        drawPath(
            path = devicePath,
            color = primaryColor.copy(alpha = 0.3f)
        )
        // Outline device path
        drawPath(
            path = devicePath,
            color = primaryColor,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}

@Composable
fun PercentileBar(percentile: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.bench_faster_than, percentile),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${percentile.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentile / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun ThermalStatusChip(status: Int, headroom: Float) {
    val (color, text) = when (status) {
        0 -> MaterialTheme.colorScheme.primary to "THERMAL: NORMAL"
        1 -> MaterialTheme.colorScheme.secondary to "THERMAL: LIGHT"
        2 -> MaterialTheme.colorScheme.tertiary to "THERMAL: MODERATE"
        3 -> MaterialTheme.colorScheme.error to "THERMAL: SEVERE"
        else -> MaterialTheme.colorScheme.error to "THERMAL: CRITICAL"
    }

    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val headroomText = if (headroom.isNaN()) "" else " (${String.format(Locale.US, "%.2f", headroom)}x)"
            Text(
                text = "$text$headroomText",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PhaseProgressRow(
    pillar: BenchmarkPillar,
    isActive: Boolean,
    isComplete: Boolean,
    score: Int
) {
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else if (isComplete) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            CircleShape
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = stringResource(pillar.displayNameRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        
        if (isComplete) {
            Text(
                text = "$score pts",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = if (isActive) "Running..." else "Pending",
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

@Composable
fun HistoryListItem(
    result: BenchmarkResult,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val date = remember(result.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(result.timestamp))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.deviceModel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$date | ${result.deviceSoc}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("HW: ${result.hardwareScore}") }
                    )
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Net: ${result.connectivityScore}") }
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = result.totalScore.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete run",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ConsentCard(
    onAccept: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var includeNetwork by remember { mutableStateOf(true) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = stringResource(R.string.bench_consent_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
            
            Text(
                text = stringResource(R.string.bench_consent_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.bench_consent_data),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.bench_consent_network),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.bench_consent_heating),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            HorizontalDivider()
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { includeNetwork = !includeNetwork }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = includeNetwork,
                    onCheckedChange = { includeNetwork = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.bench_include_network),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = stringResource(R.string.bench_consent_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onAccept(includeNetwork) },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.bench_consent_accept))
                }
            }
        }
    }
}

@Composable
fun getScoreBadgeColors(score: Int): Triple<Color, Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when {
        score >= 800 -> Triple(
            scheme.primaryContainer.copy(alpha = 0.5f),
            scheme.primary,
            scheme.primary.copy(alpha = 0.4f)
        )
        score >= 500 -> Triple(
            scheme.secondaryContainer.copy(alpha = 0.5f),
            scheme.secondary,
            scheme.secondary.copy(alpha = 0.4f)
        )
        score >= 300 -> Triple(
            scheme.tertiaryContainer.copy(alpha = 0.5f),
            scheme.tertiary,
            scheme.tertiary.copy(alpha = 0.4f)
        )
        else -> Triple(
            scheme.errorContainer.copy(alpha = 0.5f),
            scheme.error,
            scheme.error.copy(alpha = 0.4f)
        )
    }
}

