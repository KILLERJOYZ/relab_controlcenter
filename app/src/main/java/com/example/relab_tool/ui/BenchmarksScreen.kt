package com.example.relab_tool.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.relab_tool.R
import com.example.relab_tool.data.PerformanceRepository
import com.example.relab_tool.data.TowerInfoProvider
import com.example.relab_tool.utils.NetworkUtils
import com.example.relab_tool.worker.PerformanceOverlayService
import com.example.relab_tool.ui.theme.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun BenchmarksScreen(
    viewModel: PerformanceViewModel,
    windowSizeClass: WindowSizeClass = WindowSizeClass.calculateFromSize(androidx.compose.ui.unit.DpSize(400.dp, 800.dp))
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    
    // Performance states
    val isBenchmarking by viewModel.performanceRepository.isBenchmarking.collectAsStateWithLifecycle()
    val benchmarkResult by viewModel.performanceRepository.benchmarkResult.collectAsStateWithLifecycle()
    val throttlingHistory by viewModel.performanceRepository.throttlingHistory.collectAsStateWithLifecycle()
    
    val isOverlayActive by PerformanceOverlayService.isServiceRunningFlow.collectAsStateWithLifecycle()
    val isStabilityActive by viewModel.performanceRepository.isStabilityActive.collectAsStateWithLifecycle()

    // Connectivity states
    var downloadSpeed by remember { mutableStateOf(0.0) }
    var testProgress by remember { mutableStateOf(0f) }
    var isTesting by remember { mutableStateOf(false) }
    val towers by viewModel.towers.collectAsStateWithLifecycle()

    val bottomContentPadding = if (isWideScreen) 16.dp else 100.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize().then(
            if (isWideScreen) Modifier.widthIn(max = 840.dp) else Modifier
        ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = if (isWideScreen) Alignment.CenterHorizontally else Alignment.Start
    ) {
        // --- PERFORMANCE SECTION ---
        item {
            Text(stringResource(R.string.bench_system_performance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = ShapeCard) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Overlay Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.bench_realtime_overlay), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.bench_overlay_desc), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = isOverlayActive,
                            onCheckedChange = { active ->
                                if (active) {
                                    if (Settings.canDrawOverlays(context)) {
                                        context.startService(Intent(context, PerformanceOverlayService::class.java))
                                    } else {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                        context.startActivity(intent)
                                    }
                                } else {
                                    context.stopService(Intent(context, PerformanceOverlayService::class.java))
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                    // Throttling Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.bench_throttling_monitor), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.bench_throttling_desc), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = isStabilityActive,
                            onCheckedChange = { active ->
                                if (active) viewModel.startStabilityTest() else viewModel.stopStabilityTest()
                            }
                        )
                    }
                }
            }
        }

        if (throttlingHistory.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = ShapeCard) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StabilityGraph(history = throttlingHistory)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = ShapeCard) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BenchmarkItem(
                        title = stringResource(R.string.bench_cpu_test),
                        description = stringResource(R.string.bench_cpu_desc),
                        isLoading = isBenchmarking,
                        onRun = { viewModel.runCpuBenchmark() }
                    )
                    HorizontalDivider()
                    BenchmarkItem(
                        title = stringResource(R.string.bench_ram_test),
                        description = stringResource(R.string.bench_ram_desc),
                        isLoading = isBenchmarking,
                        onRun = { viewModel.runRamBenchmark() }
                    )
                }
            }
        }

        if (benchmarkResult != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = ShapeCard, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(text = benchmarkResult!!, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- CONNECTIVITY SECTION ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.bench_network), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = ShapeCard) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (downloadSpeed > 0) "%.1f Mbps".format(downloadSpeed) else stringResource(R.string.ready),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )
                    if (isTesting) {
                        LinearProgressIndicator(progress = { testProgress }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTesting = true
                                downloadSpeed = NetworkUtils.performDownloadSpeedTest("https://speed.cloudflare.com/__down?bytes=25000000") {
                                    testProgress = it
                                }
                                isTesting = false
                            }
                        },
                        enabled = !isTesting
                    ) {
                        Icon(Icons.Default.NetworkCheck, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.bench_start_speed_test))
                    }
                }
            }
        }

        item {
            Text(stringResource(R.string.bench_cellular_towers), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }

        items(towers) { tower ->
            val containerColor = if (tower.isRegistered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            val contentColor = if (tower.isRegistered) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeMedium,
                colors = CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SignalCellularAlt, null, tint = contentColor)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(tower.type, fontWeight = FontWeight.Bold, color = contentColor)
                        Text(tower.id, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
                        Text(stringResource(R.string.bench_signal_format, tower.signalStrength), style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.7f))
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(bottomContentPadding)) }
    }
}

@Composable
fun StabilityGraph(history: List<Pair<Long, Int>>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val maxFreq = history.maxOfOrNull { it.second } ?: 3000
    val minFreq = history.minOfOrNull { it.second } ?: 0
    val range = (maxFreq - minFreq).coerceAtLeast(100)
    val currentMaxLabel = stringResource(R.string.bench_current_max_mhz, history.lastOrNull()?.second ?: 0)

    Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val stepX = size.width / (history.size - 1).coerceAtLeast(1)
            
            history.forEachIndexed { i, pair ->
                val x = i * stepX
                val y = size.height - ((pair.second - minFreq).toFloat() / range * size.height)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
    Text(
        currentMaxLabel,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun BenchmarkItem(title: String, description: String, isLoading: Boolean, onRun: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onRun, enabled = !isLoading) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.bench_run))
        }
    }
}
