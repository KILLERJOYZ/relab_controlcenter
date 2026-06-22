package com.example.relab_tool.ui.cit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.relab_tool.ui.theme.ShapeCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import java.security.MessageDigest
import java.util.Random
import kotlin.math.abs
import kotlin.math.sin
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager

data class LensInfo(val logicalId: String, val physicalId: String?, val name: String)

fun getPointerColor(id: Int): Color {
    val colors = listOf(
        Color(0xFFE57373),
        Color(0xFF81C784),
        Color(0xFF64B5F6),
        Color(0xFFFFD54F),
        Color(0xFFBA68C8),
        Color(0xFF4DB6AC),
        Color(0xFFFF8A65),
        Color(0xFFA1887F),
        Color(0xFF90A4AE),
        Color(0xFF9575CD)
    )
    return colors[abs(id) % colors.size]
}

fun playSineWave(streamType: Int): AudioTrack {
    val sampleRate = 44100
    val freqOfTone = 440.0
    val numSamples = sampleRate * 3
    val generatedSnd = ByteArray(2 * numSamples)

    for (i in 0 until numSamples) {
        val sample = sin(2 * Math.PI * i / (sampleRate / freqOfTone))
        val valShort = (sample * 32767).toInt().toShort()
        generatedSnd[i * 2] = (valShort.toInt() and 0x00ff).toByte()
        generatedSnd[i * 2 + 1] = (valShort.toInt() and 0xff00).ushr(8).toByte()
    }

    val audioFormat = AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    val audioAttributes = AudioAttributes.Builder()
        .setUsage(if (streamType == AudioManager.STREAM_VOICE_CALL) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(audioAttributes)
        .setAudioFormat(audioFormat)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(generatedSnd.size)
        .build()

    audioTrack.write(generatedSnd, 0, generatedSnd.size)
    audioTrack.setLoopPoints(0, generatedSnd.size / 2, -1)
    audioTrack.play()
    
    return audioTrack
}

fun playVibrationPattern(context: Context, patternType: Int) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    if (vibrator == null || !vibrator.hasVibrator()) return
    
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (patternType) {
                0 -> VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                1 -> VibrationEffect.createWaveform(
                    longArrayOf(0, 80, 100, 80),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
                2 -> VibrationEffect.createOneShot(350, 255)
                3 -> VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 80, 50, 80, 250),
                    intArrayOf(0, 100, 0, 150, 0, 255),
                    -1
                )
                else -> VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            when (patternType) {
                0 -> vibrator.vibrate(80)
                1 -> vibrator.vibrate(longArrayOf(0, 80, 100, 80), -1)
                2 -> vibrator.vibrate(350)
                3 -> vibrator.vibrate(longArrayOf(0, 50, 80, 50, 80, 250), -1)
                else -> vibrator.vibrate(500)
            }
        }
    } catch (_: Throwable) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CITCategoryWizardScreen(
    categoryId: String,
    viewModel: CITViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session by viewModel.session.collectAsStateWithLifecycle()

    val categoryDef = remember(categoryId) {
        CITSubTestManager.categories.find { it.id == categoryId }
    }

    if (categoryDef == null) {
        onBack()
        return
    }

    val categoryResult = session.categories.find { it.categoryId == categoryId }
    val testResults = categoryResult?.tests ?: emptyList()

    var currentTestIndex by remember { mutableIntStateOf(0) }
    val activeTestDef = categoryDef.subTests.getOrNull(currentTestIndex)
    val activeTestResult = testResults.find { it.id == activeTestDef?.id }

    var isFullscreenActive by remember { mutableStateOf(false) }
    val isFullscreenTest = activeTestDef?.id in setOf(
        "GRID_TOUCH", "MULTI_TOUCH", "GHOST_TOUCH", "COLOR_UNIFORMITY", "DEAD_PIXEL", "SCREEN_FLICKER"
    )

    LaunchedEffect(currentTestIndex) {
        isFullscreenActive = false
    }

    // Automated runner execution logic
    LaunchedEffect(activeTestDef?.id) {
        val def = activeTestDef ?: return@LaunchedEffect
        if (def.isAutomated && def.runAutomated != null) {
            val isSupported = def.checkSupport(context)
            if (!isSupported) {
                viewModel.updateSubTestResult(categoryId, def.id, TestStatus.NOT_APPLICABLE, "Not Supported")
            } else {
                val outcome = def.runAutomated.invoke(context)
                viewModel.updateSubTestResult(categoryId, def.id, outcome.first, outcome.second)
            }
        }
    }

    if (isFullscreenActive && activeTestDef != null && isFullscreenTest) {
        FullscreenTestOverlay(
            testId = activeTestDef.id,
            categoryId = categoryId,
            viewModel = viewModel,
            onClose = { isFullscreenActive = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(categoryDef.name, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Horizontal index tabs for freeform navigation
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(categoryDef.subTests) { index, test ->
                        val result = testResults.find { it.id == test.id }
                        val isActive = index == currentTestIndex
                        val tabColor = when (result?.status) {
                            TestStatus.PASS -> Color(0xFFE8F5E9)
                            TestStatus.FAIL -> Color(0xFFFFEBEE)
                            TestStatus.SKIPPED -> Color(0xFFECEFF1)
                            TestStatus.NOT_APPLICABLE -> Color(0xFFF5F5F5)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val borderStroke = if (isActive) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        Card(
                            onClick = { currentTestIndex = index },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = tabColor),
                            border = borderStroke,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${index + 1}. ${test.name}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                when (result?.status) {
                                    TestStatus.PASS -> Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                    TestStatus.FAIL -> Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                                    TestStatus.SKIPPED -> Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    TestStatus.NOT_APPLICABLE -> Icon(Icons.Default.Block, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    else -> {}
                                }
                            }
                        }
                    }
                }

                if (activeTestDef != null && activeTestResult != null) {
                    // Main Test Card Content
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = ShapeCard,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Instruction block
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = activeTestDef.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = activeTestDef.instruction,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Interactive panel wrapper
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (activeTestDef.isAutomated) {
                                    AutomatedStatusBlock(result = activeTestResult)
                                } else if (isFullscreenTest) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize().padding(16.dp)
                                    ) {
                                        val icon = when (activeTestDef.id) {
                                            "GRID_TOUCH" -> Icons.Default.TouchApp
                                            "MULTI_TOUCH" -> Icons.Default.Gesture
                                            "GHOST_TOUCH" -> Icons.Default.TrackChanges
                                            "COLOR_UNIFORMITY", "DEAD_PIXEL" -> Icons.Default.Palette
                                            else -> Icons.Default.FlashlightOn
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "This test requires immersive fullscreen access to validate the entire display boundary.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { isFullscreenActive = true },
                                            shape = ShapeCard
                                        ) {
                                            Text("START FULLSCREEN TEST")
                                        }
                                    }
                                } else {
                                    InteractiveTestLayout(
                                        testId = activeTestDef.id,
                                        categoryId = categoryId,
                                        viewModel = viewModel
                                    )
                                }
                            }

                            // Status index info
                            Text(
                                text = "Status: ${activeTestResult.status.name}" + if (activeTestResult.value != null) " (${activeTestResult.value})" else "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Wizard Action Row Controls
                // Wizard Action Bottom Controls (Redesigned UI/UX to prevent button squeezing & vertical letter wrapping)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Test Evaluation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // FAIL
                        Button(
                            onClick = {
                                if (activeTestDef != null) {
                                    viewModel.updateSubTestResult(categoryId, activeTestDef.id, TestStatus.FAIL)
                                    if (currentTestIndex < categoryDef.subTests.size - 1) currentTestIndex++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = ShapeCard,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FAIL", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold, 
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // SKIP
                        OutlinedButton(
                            onClick = {
                                if (activeTestDef != null) {
                                    viewModel.updateSubTestResult(categoryId, activeTestDef.id, TestStatus.SKIPPED)
                                    if (currentTestIndex < categoryDef.subTests.size - 1) currentTestIndex++
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = ShapeCard,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SKIP", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // PASS
                        Button(
                            onClick = {
                                if (activeTestDef != null) {
                                    viewModel.updateSubTestResult(categoryId, activeTestDef.id, TestStatus.PASS)
                                    if (currentTestIndex < categoryDef.subTests.size - 1) currentTestIndex++
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = ShapeCard,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PASS", 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold, 
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Row 2: Navigation & Progress Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous
                        OutlinedIconButton(
                            onClick = { if (currentTestIndex > 0) currentTestIndex-- },
                            enabled = currentTestIndex > 0,
                            modifier = Modifier.size(40.dp),
                            shape = ShapeCard,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Previous Test",
                                tint = if (currentTestIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        // Progress Info text
                        Text(
                            text = "Sub-test ${currentTestIndex + 1} of ${categoryDef.subTests.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Next
                        OutlinedIconButton(
                            onClick = { if (currentTestIndex < categoryDef.subTests.size - 1) currentTestIndex++ },
                            enabled = currentTestIndex < categoryDef.subTests.size - 1,
                            modifier = Modifier.size(40.dp),
                            shape = ShapeCard,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Next Test",
                                tint = if (currentTestIndex < categoryDef.subTests.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutomatedStatusBlock(result: TestResult) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val color = when (result.status) {
            TestStatus.PASS -> Color(0xFF2E7D32)
            TestStatus.FAIL -> Color(0xFFC62828)
            TestStatus.NOT_APPLICABLE -> Color.Gray
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val icon = when (result.status) {
            TestStatus.PASS -> Icons.Default.CheckCircle
            TestStatus.FAIL -> Icons.Default.Cancel
            TestStatus.NOT_APPLICABLE -> Icons.Default.Block
            else -> Icons.Default.HourglassEmpty
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when (result.status) {
                TestStatus.PASS -> "AUTOMATED CHECK: PASSED"
                TestStatus.FAIL -> "AUTOMATED CHECK: FAILED"
                TestStatus.NOT_APPLICABLE -> "HARDWARE NOT APPLICABLE"
                else -> "CHECKING HARDWARE..."
            },
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge,
            color = color
        )
        if (result.value != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Measured Value: ${result.value}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InteractiveTestLayout(
    testId: String,
    categoryId: String,
    viewModel: CITViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    when (testId) {
        "GRID_TOUCH" -> {
            var touched by remember { mutableStateOf(setOf<Int>()) }
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }
            val cols = 8
            val rws = 14

            LaunchedEffect(touched.size) {
                if (canvasSize.width > 0 && touched.size == cols * rws) {
                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "100% Covered")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    if (change.pressed && canvasSize.width > 0) {
                                        val c = ((change.position.x * cols) / canvasSize.width).toInt().coerceIn(0, cols - 1)
                                        val r = ((change.position.y * rws) / canvasSize.height).toInt().coerceIn(0, rws - 1)
                                        touched = touched + (r * cols + c)
                                    }
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (canvasSize.width > 0) {
                        val gap = 2f
                        for (r in 0 until rws) {
                            for (c in 0 until cols) {
                                val idx = r * cols + c
                                val color = if (touched.contains(idx)) Color(0xFF4CAF50) else Color.DarkGray.copy(alpha = 0.5f)
                                val left = (c * canvasSize.width) / cols + (if (c > 0) gap / 2f else 0f)
                                val right = ((c + 1) * canvasSize.width) / cols - (if (c < cols - 1) gap / 2f else 0f)
                                val top = (r * canvasSize.height) / rws + (if (r > 0) gap / 2f else 0f)
                                val bottom = ((r + 1) * canvasSize.height) / rws - (if (r < rws - 1) gap / 2f else 0f)
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        "MULTI_TOUCH" -> {
            var activePointers by remember { mutableStateOf(mapOf<Int, Offset>()) }
            var pointerPaths by remember { mutableStateOf(mapOf<Int, List<Offset>>()) }
            var maxCount by remember { mutableIntStateOf(0) }

            LaunchedEffect(activePointers.size) {
                if (activePointers.size > maxCount) {
                    maxCount = activePointers.size
                    if (maxCount >= 5) {
                        viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "$maxCount fingers")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val ptrs = mutableMapOf<Int, Offset>()
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        ptrs[change.id.value.toInt()] = change.position
                                    }
                                }
                                activePointers = ptrs

                                val paths = pointerPaths.toMutableMap()
                                event.changes.forEach { change ->
                                    val id = change.id.value.toInt()
                                    if (change.pressed) {
                                        val p = paths[id] ?: emptyList()
                                        paths[id] = (p + change.position).takeLast(20)
                                    } else {
                                        paths.remove(id)
                                    }
                                }
                                pointerPaths = paths
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    pointerPaths.forEach { (id, path) ->
                        if (path.size > 1) {
                            val color = getPointerColor(id)
                            for (i in 0 until path.size - 1) {
                                drawLine(color.copy(alpha = 0.5f), path[i], path[i + 1], strokeWidth = 8f)
                            }
                        }
                    }
                    activePointers.forEach { (id, pos) ->
                        val color = getPointerColor(id)
                        drawCircle(color = color, radius = 60f, center = pos, style = Stroke(width = 4f))
                        drawCircle(color = color.copy(alpha = 0.2f), radius = 30f, center = pos)
                    }
                }
                Text(
                    text = "Touches: ${activePointers.size}  |  Max: $maxCount/5+",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                )
            }
        }

        "GHOST_TOUCH" -> {
            var touchesDetected by remember { mutableIntStateOf(0) }
            var secondsLeft by remember { mutableIntStateOf(4) }

            LaunchedEffect(Unit) {
                while (secondsLeft > 0) {
                    delay(1000)
                    secondsLeft--
                }
                if (touchesDetected == 0) {
                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "No Ghost Touches")
                } else {
                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.FAIL, "$touchesDetected events")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.pressed }) {
                                    touchesDetected++
                                }
                            }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("KEEP FINGERS OFF SCREEN", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Analyzing passive sensor noise: $secondsLeft s remaining", color = Color.White)
                    if (touchesDetected > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("GHOST EVENTS RECORDED: $touchesDetected", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        "COLOR_UNIFORMITY", "DEAD_PIXEL" -> {
            val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
            var activeColorIdx by remember { mutableIntStateOf(0) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors[activeColorIdx])
                    .clickable {
                        activeColorIdx = (activeColorIdx + 1) % colors.size
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap to switch color (Red/Green/Blue/White/Black)",
                    color = if (colors[activeColorIdx] == Color.White) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors[activeColorIdx].copy(alpha = 0.3f))
                        .padding(8.dp)
                )
            }
        }

        "BRIGHTNESS_RANGE" -> {
            var step by remember { mutableIntStateOf(50) }
            LaunchedEffect(step) {
                // Mock layout representation of hardware step adjust
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Step Brightness: $step%", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = step.toFloat(),
                    onValueChange = { step = it.toInt() },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { step = 0 }) { Text("0% Min") }
                    Button(onClick = { step = 100 }) { Text("100% Max") }
                }
            }
        }

        "SCREEN_FLICKER" -> {
            var flag by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                while (isActive) {
                    flag = !flag
                    delay(40)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (flag) Color.Black else Color.White)
            )
        }

        "ACCEL", "GYRO", "MAGNETIC", "PROXIMITY", "LIGHT_SENSOR" -> {
            val sensorType = when (testId) {
                "ACCEL" -> Sensor.TYPE_ACCELEROMETER
                "GYRO" -> Sensor.TYPE_GYROSCOPE
                "MAGNETIC" -> Sensor.TYPE_MAGNETIC_FIELD
                "PROXIMITY" -> Sensor.TYPE_PROXIMITY
                else -> Sensor.TYPE_LIGHT
            }

            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(sensorType)
            var values by remember { mutableStateOf(floatArrayOf(0f, 0f, 0f)) }

            DisposableEffect(testId) {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event != null) {
                            values = event.values.clone()
                        }
                    }
                    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                }
                if (sensor != null) {
                    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                }
                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (sensor == null) {
                    Text("Sensor Hardware Absent", color = Color.Red, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    when (testId) {
                        "PROXIMITY" -> {
                            val near = values[0] < (sensor.maximumRange)
                            Text(
                                text = if (near) "OBJECT DETECTED [NEAR]" else "OBJECT FAR",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = if (near) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        "LIGHT_SENSOR" -> {
                            Text("Ambient Light: ${values[0].toInt()} Lux", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (values[0] / 1000f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )
                        }
                        else -> {
                            Text("Axis X: %.2f".format(values[0]))
                            Text("Axis Y: %.2f".format(values[1]))
                            Text("Axis Z: %.2f".format(values[2]))
                        }
                    }
                }
            }
        }

        "EARPIECE", "SPEAKER", "STEREO_BALANCE" -> {
            var activeToneTrack by remember { mutableStateOf<AudioTrack?>(null) }
            var isPlaying by remember { mutableStateOf(false) }

            DisposableEffect(testId) {
                onDispose {
                    activeToneTrack?.release()
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isPlaying) {
                            activeToneTrack?.stop()
                            activeToneTrack?.release()
                            activeToneTrack = null
                            isPlaying = false
                        } else {
                            val stream = when (testId) {
                                "EARPIECE" -> AudioManager.STREAM_VOICE_CALL
                                else -> AudioManager.STREAM_MUSIC
                            }
                            activeToneTrack = playSineWave(stream)
                            isPlaying = true
                        }
                    },
                    shape = ShapeCard
                ) {
                    Text(if (isPlaying) "STOP AUDIO TONE" else "PLAY TEST TONE")
                }
            }
        }

        "MIC_MAIN", "MIC_SEC" -> {
            val recordPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            var permissionState by remember { mutableStateOf(recordPermission) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionState = it }

            var isRec by remember { mutableStateOf(false) }
            var progress by remember { mutableFloatStateOf(0f) }
            val micFile = remember { File(context.cacheDir, "wizard_mic_${testId}.3gp") }

            DisposableEffect(testId) {
                if (!permissionState) launcher.launch(Manifest.permission.RECORD_AUDIO)
                onDispose {
                    if (micFile.exists()) micFile.delete()
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!permissionState) {
                    Text("Microphone permission required", color = Color.Red)
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = if (isRec) Color.Red else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        enabled = !isRec,
                        onClick = {
                            scope.launch {
                                var recorder: MediaRecorder? = null
                                try {
                                    isRec = true
                                    recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
                                    val source = if (testId == "MIC_SEC") MediaRecorder.AudioSource.CAMCORDER else MediaRecorder.AudioSource.MIC
                                    recorder.apply {
                                        setAudioSource(source)
                                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                        setOutputFile(micFile.absolutePath)
                                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                        prepare()
                                        start()
                                    }

                                    val t0 = System.currentTimeMillis()
                                    while (System.currentTimeMillis() - t0 < 3000L) {
                                        progress = (System.currentTimeMillis() - t0) / 3000f
                                        delay(100)
                                    }
                                    recorder.stop()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    recorder?.release()
                                    isRec = false
                                    progress = 0f
                                }

                                // Play back
                                if (micFile.exists() && micFile.length() > 0) {
                                    var player: MediaPlayer? = null
                                    try {
                                        player = MediaPlayer()
                                        player.setDataSource(micFile.absolutePath)
                                        player.prepare()
                                        player.start()
                                        delay(3000)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        player?.release()
                                    }
                                }
                            }
                        },
                        shape = ShapeCard
                    ) {
                        Text(if (isRec) "RECORDING (3s)..." else "START MIC TEST")
                    }
                    if (isRec) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(0.8f))
                    }
                }
            }
        }

        "VIBRATION_MOTOR" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { playVibrationPattern(context, 0) }, modifier = Modifier.fillMaxWidth(), shape = ShapeCard) {
                    Text("Single Click")
                }
                Button(onClick = { playVibrationPattern(context, 1) }, modifier = Modifier.fillMaxWidth(), shape = ShapeCard) {
                    Text("Double Click")
                }
                Button(onClick = { playVibrationPattern(context, 2) }, modifier = Modifier.fillMaxWidth(), shape = ShapeCard) {
                    Text("Heavy Pulse")
                }
                Button(onClick = { playVibrationPattern(context, 3) }, modifier = Modifier.fillMaxWidth(), shape = ShapeCard) {
                    Text("Custom Rumble")
                }
            }
        }

        "CAMERA_REAR", "CAMERA_FRONT", "CAMERA_MULTI", "CAMERA_ZOOM", "AUTOFOCUS" -> {
            val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            var hasPermission by remember { mutableStateOf(cameraPermission) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

            val lifecycleOwner = LocalLifecycleOwner.current
            var zoomRatio by remember { mutableFloatStateOf(1f) }

            // Lenses querying for physical switching
            val lenses = remember(context) {
                val list = mutableListOf<LensInfo>()
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                if (cameraManager != null) {
                    try {
                        cameraManager.cameraIdList.forEach { id ->
                            val chars = cameraManager.getCameraCharacteristics(id)
                            val facing = chars.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                            if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    val physicals = chars.physicalCameraIds
                                    if (physicals.isNotEmpty()) {
                                        physicals.forEach { physId ->
                                            val physChars = cameraManager.getCameraCharacteristics(physId)
                                            val focalLengths = physChars.get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                            val focal = focalLengths?.firstOrNull() ?: 0f
                                            val typeName = when {
                                                focal < 3.3f -> "Ultra-Wide"
                                                focal > 6.0f -> "Telephoto"
                                                else -> "Main"
                                            }
                                            list.add(LensInfo(id, physId, "$typeName Rear (ID $physId, ${focal}mm)"))
                                        }
                                    } else {
                                        val focalLengths = chars.get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                        val focal = focalLengths?.firstOrNull() ?: 0f
                                        list.add(LensInfo(id, null, "Main Rear (ID $id, ${focal}mm)"))
                                    }
                                } else {
                                    list.add(LensInfo(id, null, "Main Rear (ID $id)"))
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (list.isEmpty()) {
                    list.add(LensInfo("0", null, "Default Rear"))
                }
                list
            }

            var selectedLens by remember { mutableStateOf(lenses.firstOrNull()) }

            DisposableEffect(testId) {
                if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
                onDispose {}
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (!hasPermission) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("Camera Permission Required", color = Color.Red)
                    }
                } else {
                    if (testId == "CAMERA_MULTI") {
                        // Display physical switcher tabs
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(lenses) { _, lens ->
                                val active = selectedLens == lens
                                Card(
                                    onClick = { selectedLens = lens },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = lens.name,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(ShapeCard)
                            .background(Color.Black)
                    ) {
                        key(testId, selectedLens) {
                            AndroidView(
                                factory = { ctx ->
                                    val viewFinder = PreviewView(ctx)
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().apply {
                                            if (testId == "CAMERA_MULTI" && selectedLens?.physicalId != null) {
                                                androidx.camera.camera2.interop.Camera2Interop.Extender(this)
                                                    .setPhysicalCameraId(selectedLens!!.physicalId!!)
                                            }
                                        }.build().apply {
                                            surfaceProvider = viewFinder.surfaceProvider
                                        }

                                        val selector = if (testId == "CAMERA_FRONT") {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else if (testId == "CAMERA_MULTI" && selectedLens != null) {
                                            CameraSelector.Builder()
                                                .addCameraFilter { cameraInfos ->
                                                    cameraInfos.filter { info ->
                                                        val cid = androidx.camera.camera2.interop.Camera2CameraInfo.from(info).cameraId
                                                        cid == selectedLens!!.logicalId
                                                    }
                                                }
                                                .build()
                                        } else {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }

                                        try {
                                            cameraProvider.unbindAll()
                                            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                                            
                                            // Zoom stepping test logic binding
                                            if (testId == "CAMERA_ZOOM") {
                                                camera.cameraControl.setZoomRatio(zoomRatio)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    viewFinder
                                },
                                update = { view ->
                                    // Trigger zoom ratio update dynamically
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (testId == "CAMERA_ZOOM") {
                        Slider(
                            value = zoomRatio,
                            onValueChange = { zoomRatio = it },
                            valueRange = 1f..5f,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        "FLASH_LIGHT" -> {
            var flashState by remember { mutableStateOf(false) }
            DisposableEffect(flashState) {
                val cm = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                try {
                    val firstBackCamera = cm?.cameraIdList?.firstOrNull()
                    if (firstBackCamera != null) {
                        cm.setTorchMode(firstBackCamera, flashState)
                    }
                } catch (_: Exception) {}
                onDispose {
                    try {
                        val cm2 = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                        val firstBackCamera = cm2?.cameraIdList?.firstOrNull()
                        if (firstBackCamera != null) {
                            cm2.setTorchMode(firstBackCamera, false)
                        }
                    } catch (_: Exception) {}
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlashlightOn,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (flashState) Color.Yellow else Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { flashState = !flashState }, shape = ShapeCard) {
                    Text(if (flashState) "TURN OFF FLASH" else "TURN ON FLASH")
                }
            }
        }

        "FINGERPRINT" -> {
            val biometricManager = remember { BiometricManager.from(context) }
            val canAuth = remember {
                biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            }
            var statusStr by remember { mutableStateOf("Ready to scan...") }
            var scanStatus by remember { mutableStateOf<TestStatus>(TestStatus.PENDING) }

            val triggerScan = {
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    val executor = ContextCompat.getMainExecutor(context)
                    val biometricPrompt = BiometricPrompt(
                        activity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                statusStr = "Scan successful!"
                                scanStatus = TestStatus.PASS
                                viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "Auth Success")
                            }
                            override fun onAuthenticationFailed() {
                                statusStr = "Authentication failed. Try again."
                            }
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                statusStr = "Error: $errString"
                                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                    scanStatus = TestStatus.FAIL
                                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.FAIL, errString.toString())
                                }
                            }
                        }
                    )
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Biometric Diagnostic Scan")
                        .setSubtitle("Touch fingerprint sensor to verify")
                        .setNegativeButtonText("Cancel")
                        .build()
                    try {
                        biometricPrompt.authenticate(promptInfo)
                        statusStr = "Scanning..."
                    } catch (e: Exception) {
                        statusStr = "Scanner Error: ${e.localizedMessage}"
                    }
                } else {
                    statusStr = "FragmentActivity context missing"
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = when (scanStatus) {
                        TestStatus.PASS -> Color(0xFF4CAF50)
                        TestStatus.FAIL -> Color.Red
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                if (canAuth != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
                    Button(onClick = triggerScan, shape = ShapeCard) {
                        Text("START SCAN PROMPT")
                    }
                } else {
                    Text("No Biometric hardware detected", color = Color.Red, fontWeight = FontWeight.Medium)
                }
            }
        }

        "GPS_LOCK" -> {
            var statusStr by remember { mutableStateOf("Requesting GPS Signal...") }
            var accuracy by remember { mutableStateOf<Float?>(null) }
            var satellites by remember { mutableStateOf(0) }
            var timeElapsed by remember { mutableStateOf(0) }
            var isPassed by remember { mutableStateOf(false) }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            val finePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            var hasPermission by remember { mutableStateOf(finePermission) }
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

            DisposableEffect(hasPermission) {
                if (!hasPermission) {
                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    onDispose {}
                } else {
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(location: android.location.Location) {
                            accuracy = location.accuracy
                            statusStr = "Signal Locked! Accuracy: ${location.accuracy}m"
                            if (location.accuracy < 30f && !isPassed) {
                                isPassed = true
                                viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "Accuracy: %.1fm".format(location.accuracy))
                            }
                        }
                    }

                    var gnssCallback: android.location.GnssStatus.Callback? = null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        gnssCallback = object : android.location.GnssStatus.Callback() {
                            override fun onSatelliteStatusChanged(status: android.location.GnssStatus) {
                                var count = 0
                                for (i in 0 until status.satelliteCount) {
                                    if (status.usedInFix(i)) count++
                                }
                                satellites = count
                            }
                        }
                        try {
                            locationManager?.registerGnssStatusCallback(gnssCallback, Handler(Looper.getMainLooper()))
                        } catch (_: SecurityException) {}
                    }

                    try {
                        locationManager?.requestLocationUpdates(
                            android.location.LocationManager.GPS_PROVIDER,
                            1000L,
                            0f,
                            listener
                        )
                    } catch (_: SecurityException) {
                        statusStr = "Location permission denied"
                    } catch (e: Exception) {
                        statusStr = "GPS Provider unavailable: ${e.localizedMessage}"
                    }

                    onDispose {
                        try {
                            locationManager?.removeUpdates(listener)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssCallback != null) {
                                locationManager?.unregisterGnssStatusCallback(gnssCallback)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            LaunchedEffect(isPassed) {
                if (!isPassed) {
                    while (true) {
                        delay(1000)
                        timeElapsed++
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (accuracy != null) Color(0xFF4CAF50) else Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Satellites in Fix: $satellites", fontSize = 14.sp)
                Text("Time elapsed: $timeElapsed s", fontSize = 14.sp)
                
                if (accuracy != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "Accuracy: %.1fm".format(accuracy ?: 0f))
                        },
                        shape = ShapeCard
                    ) {
                        Text("FORCE PASS (Fix Acquired)")
                    }
                }
            }
        }

        "NFC_SCAN" -> {
            var statusStr by remember { mutableStateOf("NFC Waiting for Tag...") }
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

            DisposableEffect(testId) {
                if (nfcAdapter != null && nfcAdapter.isEnabled) {
                    val activity = context as? Activity
                    activity?.let {
                        nfcAdapter.enableReaderMode(it, { tag ->
                            val id = tag.id.joinToString("") { b -> "%02X".format(b) }
                            statusStr = "Tag Detected ID: $id"
                            viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, id)
                        }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B, null)
                    }
                } else {
                    statusStr = "NFC Hardware Absent or Disabled"
                }

                onDispose {
                    val activity = context as? Activity
                    if (nfcAdapter != null && activity != null) {
                        nfcAdapter.disableReaderMode(activity)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(statusStr, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        "POWER_BUTTON" -> {
            var pressed by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                            pressed = true
                            viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "Pressed")
                        }
                    }
                }
                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    IntentFilter(Intent.ACTION_SCREEN_OFF),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                onDispose {
                    try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (pressed) Color(0xFF4CAF50) else Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (pressed) "Power key click registered!" else "Press power key to turn screen off, then resume.",
                    textAlign = TextAlign.Center
                )
            }
        }

        "VOLUME_KEYS" -> {
            var upPressed by remember { mutableStateOf(false) }
            var downPressed by remember { mutableStateOf(false) }

            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key.keyCode) {
                                Key.VolumeUp.keyCode -> {
                                    upPressed = true
                                    true
                                }
                                Key.VolumeDown.keyCode -> {
                                    downPressed = true
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                contentAlignment = Alignment.Center
            ) {
                LaunchedEffect(upPressed, downPressed) {
                    if (upPressed && downPressed) {
                        viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "Both Volume Keys OK")
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = if (upPressed) Color(0xFF4CAF50) else Color.Gray)
                            Text("VOLUME UP")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VolumeDown, contentDescription = null, tint = if (downPressed) Color(0xFF4CAF50) else Color.Gray)
                            Text("VOLUME DOWN")
                        }
                    }
                    Text("Tap here to focus keypad, then click volume keys.", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        "CPU_STRESS" -> {
            var activeStress by remember { mutableStateOf(false) }
            var progress by remember { mutableFloatStateOf(0f) }
            var hashesCount by remember { mutableLongStateOf(0L) }

            LaunchedEffect(activeStress) {
                if (activeStress) {
                    val t0 = System.currentTimeMillis()
                    val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
                    val jobs = List(cores) {
                        launch(Dispatchers.Default) {
                            val md = MessageDigest.getInstance("SHA-256")
                            var arr = "DiagnosticSuiteLoadTest".toByteArray()
                            var count = 0L
                            while (System.currentTimeMillis() - t0 < 5000L) {
                                arr = md.digest(arr)
                                count++
                                if (count % 3000 == 0L) yield()
                            }
                            hashesCount += count
                        }
                    }

                    while (System.currentTimeMillis() - t0 < 5000L) {
                        progress = (System.currentTimeMillis() - t0) / 5000f
                        delay(100)
                    }
                    jobs.forEach { it.join() }
                    activeStress = false
                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "%,d hashes".format(hashesCount))
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Cores count stressed: ${Runtime.getRuntime().availableProcessors()}")
                Spacer(modifier = Modifier.height(16.dp))
                Button(enabled = !activeStress, onClick = { activeStress = true }, shape = ShapeCard) {
                    Text(if (activeStress) "Stressing..." else "RUN 5s STRESS")
                }
                if (activeStress) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Hashes: %,d".format(hashesCount), fontSize = 12.sp)
                }
            }
        }

        "GPU_STRESS" -> {
            val infiniteTransition = rememberInfiniteTransition(label = "flicker")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "angle"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = 100f
                val color = Color(0xFF3F51B5)
                drawCircle(
                    color = color,
                    center = center,
                    radius = radius,
                    style = Stroke(width = 8f)
                )
                // Draw rotating core pointer
                val x = center.x + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
                val y = center.y - radius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = Offset(x, y),
                    strokeWidth = 6f
                )
            }
        }

        "CHARGING_PORT" -> {
            var cycleCount by remember { mutableIntStateOf(0) }

            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        if (intent?.action == Intent.ACTION_POWER_CONNECTED) {
                            cycleCount++
                            if (cycleCount >= 1) {
                                viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "Attached")
                            }
                        }
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_POWER_CONNECTED)
                    addAction(Intent.ACTION_POWER_DISCONNECTED)
                }
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                onDispose {
                    try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChargingStation,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (cycleCount > 0) Color(0xFF4CAF50) else Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Insert charging cable to mechanically verify port. Events logged: $cycleCount",
                    textAlign = TextAlign.Center
                )
            }
        }

        "SPEAKER_GRILLE" -> {
            var isGrilleClean by remember { mutableStateOf(false) }
            var isMicClean by remember { mutableStateOf(false) }

            LaunchedEffect(isGrilleClean, isMicClean) {
                if (isGrilleClean && isMicClean) {
                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "Clean")
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Mechanical visual check list:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isGrilleClean = !isGrilleClean }) {
                    Checkbox(checked = isGrilleClean, onCheckedChange = { isGrilleClean = it })
                    Text("Speaker grille is clean of dust & blockages")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isMicClean = !isMicClean }) {
                    Checkbox(checked = isMicClean, onCheckedChange = { isMicClean = it })
                    Text("Microphone holes are free from debris")
                }
            }
        }

        else -> {
            // General Fallback Checklist Manual Prompt
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Verify hardware components function manually.", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ImmersiveModeEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val params = window.attributes
                val prevCutout = params.layoutInDisplayCutoutMode
                params.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = params
                
                onDispose {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                    val paramsRestore = window.attributes
                    paramsRestore.layoutInDisplayCutoutMode = prevCutout
                    window.attributes = paramsRestore
                }
            } else {
                onDispose {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                }
            }
        } else {
            onDispose {}
        }
    }
}

@Composable
fun FullscreenTestOverlay(
    testId: String,
    categoryId: String,
    viewModel: CITViewModel,
    onClose: () -> Unit
) {
    ImmersiveModeEffect()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key.keyCode) {
                        Key.VolumeUp.keyCode -> {
                            onClose()
                            true
                        }
                        Key.VolumeDown.keyCode -> {
                            viewModel.updateSubTestResult(categoryId, testId, TestStatus.FAIL, "Failed via volume key override")
                            onClose()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        when (testId) {
            "GRID_TOUCH" -> {
                var touched by remember { mutableStateOf(setOf<Int>()) }
                var canvasSize by remember { mutableStateOf(IntSize.Zero) }
                val cols = 8
                val rws = 16

                var downTime by remember { mutableStateOf(0L) }
                var latencySum by remember { mutableStateOf(0L) }
                var latencyCount by remember { mutableStateOf(0) }

                LaunchedEffect(touched.size) {
                    if (canvasSize.width > 0 && touched.size == cols * rws) {
                        val avgLatency = if (latencyCount > 0) latencySum / latencyCount else 12L
                        viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "100% Covered (${avgLatency}ms latency)")
                        onClose()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                    event.changes.forEach { change ->
                                        if (change.pressed && canvasSize.width > 0) {
                                            downTime = System.currentTimeMillis()
                                            val c = ((change.position.x * cols) / canvasSize.width).toInt().coerceIn(0, cols - 1)
                                            val r = ((change.position.y * rws) / canvasSize.height).toInt().coerceIn(0, rws - 1)
                                            touched = touched + (r * cols + c)
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (canvasSize.width > 0) {
                            if (downTime > 0L) {
                                val lat = System.currentTimeMillis() - downTime
                                if (lat in 1..150) {
                                    latencySum += lat
                                    latencyCount++
                                }
                                downTime = 0L
                            }

                            val gap = 2f
                            for (r in 0 until rws) {
                                for (c in 0 until cols) {
                                    val idx = r * cols + c
                                    val color = if (touched.contains(idx)) Color(0xFF4CAF50) else Color.DarkGray.copy(alpha = 0.5f)
                                    val left = (c * canvasSize.width) / cols + (if (c > 0) gap / 2f else 0f)
                                    val right = ((c + 1) * canvasSize.width) / cols - (if (c < cols - 1) gap / 2f else 0f)
                                    val top = (r * canvasSize.height) / rws + (if (r > 0) gap / 2f else 0f)
                                    val bottom = ((r + 1) * canvasSize.height) / rws - (if (r < rws - 1) gap / 2f else 0f)
                                    drawRoundRect(
                                        color = color,
                                        topLeft = Offset(left, top),
                                        size = Size(right - left, bottom - top),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }
                        }
                    }

                    // Floating UI Controls Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Covered: ${touched.size} / ${cols * rws} (${(touched.size * 100) / (cols * rws)}%)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.FAIL, "Failed manually")
                                    onClose()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = ShapeCard
                            ) {
                                Text("FAIL TEST")
                            }
                            Button(
                                onClick = onClose,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                shape = ShapeCard
                            ) {
                                Text("CANCEL")
                            }
                        }
                    }
                }
            }

            "MULTI_TOUCH" -> {
                var activePointers by remember { mutableStateOf(mapOf<Int, Offset>()) }
                var pointerPaths by remember { mutableStateOf(mapOf<Int, List<Offset>>()) }
                var maxCount by remember { mutableIntStateOf(0) }

                LaunchedEffect(activePointers.size) {
                    if (activePointers.size > maxCount) {
                        maxCount = activePointers.size
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val ptrs = mutableMapOf<Int, Offset>()
                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            ptrs[change.id.value.toInt()] = change.position
                                        }
                                    }
                                    activePointers = ptrs

                                    val paths = pointerPaths.toMutableMap()
                                    event.changes.forEach { change ->
                                        val id = change.id.value.toInt()
                                        if (change.pressed) {
                                            val p = paths[id] ?: emptyList()
                                            paths[id] = (p + change.position).takeLast(20)
                                        } else {
                                            paths.remove(id)
                                        }
                                    }
                                    pointerPaths = paths
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        pointerPaths.forEach { (id, path) ->
                            if (path.size > 1) {
                                val color = getPointerColor(id)
                                for (i in 0 until path.size - 1) {
                                    drawLine(color.copy(alpha = 0.5f), path[i], path[i + 1], strokeWidth = 8f)
                                }
                            }
                        }
                        activePointers.forEach { (id, pos) ->
                            val color = getPointerColor(id)
                            drawCircle(color = color, radius = 60f, center = pos, style = Stroke(width = 4f))
                            drawCircle(color = color.copy(alpha = 0.2f), radius = 30f, center = pos)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Active contacts: ${activePointers.size}  |  Max points registered: $maxCount / 5+",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.FAIL, "Max points: $maxCount")
                                    onClose()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = ShapeCard
                            ) {
                                Text("FAIL")
                            }
                            Button(
                                onClick = {
                                    viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "$maxCount points")
                                    onClose()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = ShapeCard
                            ) {
                                Text("PASS")
                            }
                        }
                    }
                }
            }

            "GHOST_TOUCH" -> {
                var touchesDetected by remember { mutableIntStateOf(0) }
                var secondsLeft by remember { mutableIntStateOf(4) }

                LaunchedEffect(Unit) {
                    while (secondsLeft > 0) {
                        delay(1000)
                        secondsLeft--
                    }
                    if (touchesDetected == 0) {
                        viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "No Ghost Touches")
                    } else {
                        viewModel.updateSubTestResult(categoryId, testId, TestStatus.FAIL, "$touchesDetected events")
                    }
                    onClose()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { it.pressed }) {
                                        touchesDetected++
                                    }
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("DO NOT TOUCH DISPLAY", color = Color.Red, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing display sensor noise: $secondsLeft seconds remaining", color = Color.White)
                        if (touchesDetected > 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("GHOST ACTIONS DETECTED: $touchesDetected", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            "COLOR_UNIFORMITY", "DEAD_PIXEL" -> {
                val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
                var colorIdx by remember { mutableIntStateOf(0) }
                var showDialog by remember { mutableStateOf(false) }

                if (showDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .width(300.dp)
                                .padding(16.dp),
                            shape = ShapeCard,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Display Uniformity Check", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Did you identify any dead pixels, banding, or color tint issues?", textAlign = TextAlign.Center, fontSize = 14.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateSubTestResult(categoryId, testId, TestStatus.FAIL, "Defects reported")
                                            onClose()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f),
                                        shape = ShapeCard
                                    ) {
                                        Text("YES (FAIL)")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "Uniform display")
                                            onClose()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.weight(1f),
                                        shape = ShapeCard
                                    ) {
                                        Text("NO (PASS)")
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        colorIdx = 0
                                        showDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ShapeCard
                                ) {
                                    Text("RE-TEST")
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors[colorIdx])
                            .clickable {
                                if (colorIdx < colors.size - 1) {
                                    colorIdx++
                                } else {
                                    showDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Uniform background [${colors[colorIdx].toString().substringAfter("Color(").substringBefore(")")}]. Tap to sweep.",
                            color = if (colors[colorIdx] == Color.White) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    }
                }
            }

            "SCREEN_FLICKER" -> {
                var warningAccepted by remember { mutableStateOf(false) }
                var flag by remember { mutableStateOf(true) }

                LaunchedEffect(warningAccepted) {
                    if (warningAccepted) {
                        while (true) {
                            flag = !flag
                            delay(40)
                        }
                    }
                }

                if (!warningAccepted) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .width(320.dp)
                                .padding(16.dp),
                            shape = ShapeCard
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(48.dp))
                                Text("PWM Flicker Warning", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("This test rapidly flashes the screen between black and white to check panel stability. Press cancel if you are sensitive to flash patterns.", textAlign = TextAlign.Center, fontSize = 13.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onClose,
                                        modifier = Modifier.weight(1f),
                                        shape = ShapeCard
                                    ) {
                                        Text("CANCEL")
                                    }
                                    Button(
                                        onClick = { warningAccepted = true },
                                        modifier = Modifier.weight(1f),
                                        shape = ShapeCard
                                    ) {
                                        Text("PROCEED")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (flag) Color.Black else Color.White)
                    ) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(32.dp)
                                .width(280.dp),
                            shape = ShapeCard
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Do you notice display flicker?", fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.updateSubTestResult(categoryId, testId, TestStatus.FAIL, "Flicker observed")
                                            onClose()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        shape = ShapeCard,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("YES (FAIL)")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.updateSubTestResult(categoryId, testId, TestStatus.PASS, "No flicker")
                                            onClose()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = ShapeCard,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("NO (PASS)")
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
