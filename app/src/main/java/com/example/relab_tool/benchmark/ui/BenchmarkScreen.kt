package com.example.relab_tool.benchmark.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.relab_tool.R
import com.example.relab_tool.benchmark.domain.model.*
import com.example.relab_tool.utils.BenchmarkReportPdfGenerator
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import android.util.Log
import android.graphics.Color

private enum class BenchmarkScreenType {
    IDLE, AWAITING_CONSENT, RUNNING, COMPLETE, ERROR
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    viewModel: BenchmarkViewModel,
    windowSizeClass: WindowSizeClass
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val bottomContentPadding = if (isWideScreen) 16.dp else 100.dp

    val screenType = when (uiState) {
        is BenchmarkUiState.Idle -> BenchmarkScreenType.IDLE
        is BenchmarkUiState.AwaitingConsent -> BenchmarkScreenType.AWAITING_CONSENT
        is BenchmarkUiState.Running -> BenchmarkScreenType.RUNNING
        is BenchmarkUiState.Complete -> BenchmarkScreenType.COMPLETE
        is BenchmarkUiState.Error -> BenchmarkScreenType.ERROR
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = screenType,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "screen_transitions"
        ) { type ->
            when (type) {
                BenchmarkScreenType.IDLE -> {
                    IdleScreen(
                        history = history,
                        onRunFull = { viewModel.requestConsent() },
                        onRunQuick = { viewModel.startQuickBenchmark() },
                        onRunSinglePillar = { viewModel.startSinglePillar(it) },
                        onDeleteResult = { viewModel.deleteResult(it) },
                        onClearAll = { viewModel.clearHistory() },
                        onSelectResult = { viewModel.selectHistoricalResult(it) },
                        bottomPadding = bottomContentPadding
                    )
                }
                BenchmarkScreenType.AWAITING_CONSENT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ConsentCard(
                            onAccept = { includeNetwork ->
                                viewModel.startFullBenchmark(includeNetwork)
                            },
                            onCancel = { viewModel.resetToIdle() }
                        )
                    }
                }
                BenchmarkScreenType.RUNNING -> {
                    val runningState = uiState as? BenchmarkUiState.Running
                    if (runningState != null) {
                        RunningScreen(
                            state = runningState,
                            onCancel = { viewModel.cancelBenchmark() }
                        )
                    }
                }
                BenchmarkScreenType.COMPLETE -> {
                    val completeState = uiState as? BenchmarkUiState.Complete
                    if (completeState != null) {
                        CompleteScreen(
                            state = completeState,
                            onShare = { BenchmarkReportPdfGenerator.generateAndShareReport(context, completeState.result) },
                            onReset = { viewModel.resetToIdle() },
                            bottomPadding = bottomContentPadding
                        )
                    }
                }
                BenchmarkScreenType.ERROR -> {
                    val errorState = uiState as? BenchmarkUiState.Error
                    if (errorState != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = errorState.message,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                Button(onClick = { viewModel.resetToIdle() }) {
                                    Text(stringResource(R.string.nav_back))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IdleScreen(
    history: List<BenchmarkResult>,
    onRunFull: () -> Unit,
    onRunQuick: () -> Unit,
    onRunSinglePillar: (BenchmarkPillar) -> Unit,
    onDeleteResult: (BenchmarkResult) -> Unit,
    onClearAll: () -> Unit,
    onSelectResult: (BenchmarkResult) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val lastResult = history.firstOrNull()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (lastResult != null) {
            item {
                Text(
                    text = "LATEST BENCHMARK RESULT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                BenchmarkSummaryCard(
                    result = lastResult,
                    onClick = { onSelectResult(lastResult) }
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No Benchmarks Executed Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Run a comprehensive performance test suite to gauge your device parameters and capabilities.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        item {
            Text(
                text = "INDIVIDUAL PILLAR BENCHMARKS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
        
        val chunks = BenchmarkPillar.entries.filter { it.weight > 0f }.chunked(2)
        chunks.forEach { rowPillars ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPillars.forEach { pillar ->
                        Card(
                            onClick = { onRunSinglePillar(pillar) },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(pillar.displayNameRes),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Weight: ${(pillar.weight * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (rowPillars.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRunQuick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Timer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bench_quick_test),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Button(
                    onClick = onRunFull,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bench_full_test),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        if (history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.bench_history),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClearAll) {
                        Icon(imageVector = Icons.Default.ClearAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.bench_clear_history))
                    }
                }
            }
            
            items(history, key = { it.timestamp }) { result ->
                HistoryListItem(
                    result = result,
                    onDelete = { onDeleteResult(result) },
                    onClick = { onSelectResult(result) }
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(bottomPadding)) }
    }
}

@Composable
fun RunningScreen(
    state: BenchmarkUiState.Running,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.bench_running_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            ThermalStatusChip(status = state.thermalStatus, headroom = state.thermalHeadroom)
        }
        
        LinearProgressIndicator(
            progress = { state.overallProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = state.currentSubTestLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.bench_running_est_time, state.estimatedRemainingSeconds),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(BenchmarkPillar.entries.filter { it.weight > 0f }) { pillar ->
                    val isActive = state.currentPillar == pillar
                    val completedObj = state.completedPillarScores.find { it.pillar == pillar }
                    val isComplete = completedObj != null
                    val score = completedObj?.score ?: 0.0
                    
                    PhaseProgressRow(
                        pillar = pillar,
                        isActive = isActive,
                        isComplete = isComplete,
                        score = score
                    )
                }
            }
        }
        
        if (state.runningHardwareScore > 0) {
            Text(
                text = stringResource(R.string.bench_running_hardware_score, state.runningHardwareScore.toInt()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(R.string.bench_running_cancel), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteScreen(
    state: BenchmarkUiState.Complete,
    onShare: () -> Unit,
    onReset: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val scrollState = rememberScrollState()
    
    // Animate score from 0 to target
    var targetScore by remember { mutableStateOf(0f) }
    LaunchedEffect(state.result.totalScore) {
        targetScore = state.result.totalScore.toFloat()
    }
    val animatedScore by animateFloatAsState(
        targetValue = targetScore,
        animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
        label = "score_counter"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.bench_complete_title).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%.3f", animatedScore),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-2).sp
                )
                
                TierBadge(tier = state.result.tier)
                
                Text(
                    text = stringResource(tierDescriptionRes(state.result.tier)),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HARDWARE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.result.hardwareScore.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CONNECTIVITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(state.result.connectivityScore.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        

        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RADAR PROFILE MAP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                RadarChartView(
                    pillarScores = state.result.pillarScores,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Explanatory Legend for the Radar Chart
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Device Score", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Baseline (SD778G = 500)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "The blue area shows your device's relative score in each category. The gray outline represents a standard reference baseline (500 pts). A larger blue area signifies higher overall capability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
        
        Text(
            text = "DETAILED METRICS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        
        val expandedStates = remember { mutableStateMapOf<BenchmarkPillar, Boolean>() }
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.result.pillarScores.forEach { pillarScore ->
                val isExpanded = expandedStates[pillarScore.pillar] ?: false
                Card(
                    onClick = { expandedStates[pillarScore.pillar] = !isExpanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(pillarScore.pillar.displayNameRes),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${pillarScore.score} pts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (isExpanded && pillarScore.subScores.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                pillarScore.subScores.forEach { sub ->
                                    val (badgeBg, badgeText, badgeBorder) = getScoreBadgeColors(sub.score)
                                    val formattedVal = if (sub.rawValue % 1.0 == 0.0) {
                                        String.format(Locale.US, "%,.0f", sub.rawValue)
                                    } else {
                                        String.format(Locale.US, "%,.1f", sub.rawValue)
                                    }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                        ),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = sub.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$formattedVal ${sub.unit}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = badgeBg,
                                                    border = BorderStroke(0.5.dp, badgeBorder)
                                                ) {
                                                    Text(
                                                        text = "${sub.score} pts",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeText,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back to Dashboard", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1.2f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share PDF", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(bottomPadding))
    }
}

@Composable
fun BenchmarkSummaryCard(
    result: BenchmarkResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "LAST COMPOSITE SCORE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = String.format(java.util.Locale.US, "%.3f", result.totalScore),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-2).sp
            )
            TierBadge(tier = result.tier)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HARDWARE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(String.format(java.util.Locale.US, "%.3f", result.hardwareScore), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CONNECTIVITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(String.format(java.util.Locale.US, "%.3f", result.connectivityScore), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun tierDescriptionRes(tier: ScoreTier): Int {
    return when (tier) {
        ScoreTier.ENTRY     -> R.string.tier_entry_desc
        ScoreTier.ENTRY_MID -> R.string.tier_entry_mid_desc
        ScoreTier.MID       -> R.string.tier_mid_desc
        ScoreTier.MID_HIGH  -> R.string.tier_mid_high_desc
        ScoreTier.HIGH      -> R.string.tier_high_desc
        ScoreTier.FLAGSHIP  -> R.string.tier_flagship_desc
        ScoreTier.ELITE     -> R.string.tier_elite_desc
    }
}
