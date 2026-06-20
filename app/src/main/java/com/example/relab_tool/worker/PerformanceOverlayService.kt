package com.example.relab_tool.worker

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.relab_tool.R
import com.example.relab_tool.ui.theme.Relab_toolTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import com.example.relab_tool.data.PerformanceProfile
import com.example.relab_tool.data.PerformanceProfileManager

class PerformanceOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var crosshairView: View? = null
    
    private var isCrosshairEnabled by mutableStateOf(false)
    private var crosshairSize by mutableStateOf(24.dp)
    private var crosshairColor by mutableStateOf(Color(0xFF00E676)) // Neon green for visibility
    private var crosshairStyle by mutableStateOf(CrosshairStyle.PLUS)
    private var isMinimized by mutableStateOf(false)
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    /** Manages real system performance changes (thread priority, WakeLock, HintSession). */
    private lateinit var profileManager: PerformanceProfileManager

    // Independent FPS tracking for overlay - works even when main app is backgrounded
    private val _overlayFps = MutableStateFlow(0f)
    private var fpsFrameCount = 0
    private var fpsLastUpdateMs = 0L
    private var fpsTrackingActive = false

    private val overlayFrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!fpsTrackingActive) return
            fpsFrameCount++
            val now = System.currentTimeMillis()
            if (fpsLastUpdateMs == 0L) fpsLastUpdateMs = now
            val elapsed = now - fpsLastUpdateMs
            if (elapsed >= 500L) {
                _overlayFps.value = (fpsFrameCount * 1000f) / elapsed
                fpsFrameCount = 0
                fpsLastUpdateMs = now
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun startOverlayFpsTracking() {
        if (!fpsTrackingActive) {
            fpsTrackingActive = true
            fpsFrameCount = 0
            fpsLastUpdateMs = System.currentTimeMillis()
            Choreographer.getInstance().postFrameCallback(overlayFrameCallback)
        }
    }

    private fun stopOverlayFpsTracking() {
        fpsTrackingActive = false
        Choreographer.getInstance().removeFrameCallback(overlayFrameCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        startForeground(NOTIFICATION_ID, createNotification())
        profileManager = PerformanceProfileManager(this)
        // Apply the currently-active profile (in case service was restarted)
        profileManager.apply(com.example.relab_tool.data.TelemetryManager.activeProfile.value)
        showOverlay()
        startOverlayFpsTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_STICKY
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        var composeView: ComposeView? = null
        composeView = ComposeView(this).apply {
            setContent {
                Relab_toolTheme {
                    OverlayContent(
                        onDrag = { dx, dy ->
                            params.x += dx
                            params.y += dy
                            try {
                                composeView?.let { windowManager.updateViewLayout(it, params) }
                            } catch (e: Exception) {}
                        }
                    )
                }
            }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    isMinimized = true
                }
                false
            }
        }

        // Essential for Compose in Service
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        val viewModelStore = ViewModelStore()
        composeView.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = viewModelStore
        })

        windowManager.addView(composeView, params)
        overlayView = composeView
    }

    private fun updateCrosshairView() {
        if (isCrosshairEnabled) {
            if (crosshairView == null) {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                val view = ComposeView(this).apply {
                    setContent {
                        CrosshairContent()
                    }
                }
                view.setViewTreeLifecycleOwner(this@PerformanceOverlayService)
                view.setViewTreeSavedStateRegistryOwner(this@PerformanceOverlayService)
                val store = ViewModelStore()
                view.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                    override val viewModelStore: ViewModelStore = store
                })
                try {
                    windowManager.addView(view, params)
                    crosshairView = view
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            crosshairView?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                crosshairView = null
            }
        }
    }

    @Composable
    private fun CrosshairContent() {
        val density = LocalDensity.current
        val thickness = 2.dp
        val stroke = remember(density) { Stroke(with(density) { thickness.toPx() }) }
        Box(
            modifier = Modifier.size(crosshairSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val color = crosshairColor
                val w = size.width
                val h = size.height
                val halfW = w / 2
                val halfH = h / 2
                val thicknessPx = thickness.toPx()
                
                when (crosshairStyle) {
                    CrosshairStyle.DOT -> {
                        drawCircle(color = color, radius = (w / 6).coerceAtLeast(2.dp.toPx()))
                    }
                    CrosshairStyle.PLUS -> {
                        val gap = (w / 8).coerceAtLeast(2.dp.toPx())
                        // Left
                        drawLine(color = color, start = Offset(0f, halfH), end = Offset(halfW - gap, halfH), strokeWidth = thicknessPx)
                        // Right
                        drawLine(color = color, start = Offset(halfW + gap, halfH), end = Offset(w, halfH), strokeWidth = thicknessPx)
                        // Top
                        drawLine(color = color, start = Offset(halfW, 0f), end = Offset(halfW, halfH - gap), strokeWidth = thicknessPx)
                        // Bottom
                        drawLine(color = color, start = Offset(halfW, halfH + gap), end = Offset(halfW, h), strokeWidth = thicknessPx)
                    }
                    CrosshairStyle.CIRCLE -> {
                        drawCircle(color = color, style = stroke)
                        drawCircle(color = color, radius = 2.dp.toPx())
                    }
                    CrosshairStyle.CROSS -> {
                        drawLine(color = color, start = Offset(0f, 0f), end = Offset(w, h), strokeWidth = thicknessPx)
                        drawLine(color = color, start = Offset(w, 0f), end = Offset(0f, h), strokeWidth = thicknessPx)
                    }
                }
            }
        }
    }

    @Composable
    private fun OverlayContent(onDrag: (Int, Int) -> Unit) {
        val telemetryData by com.example.relab_tool.data.TelemetryManager.data.collectAsState()
        // Use the service's own FPS tracker so it works regardless of app foreground state
        val overlayFps by _overlayFps.asStateFlow().collectAsState()

        var isCollapsed by remember { mutableStateOf(false) }
        var isLocked by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        var selectedProfile by remember {
            mutableStateOf(com.example.relab_tool.data.TelemetryManager.activeProfile.value.label)
        }
        // Keep selectedProfile in sync if changed externally
        val activeProfileFlow by com.example.relab_tool.data.TelemetryManager.activeProfile.collectAsState()
        LaunchedEffect(activeProfileFlow) { selectedProfile = activeProfileFlow.label }
        var customAlpha by remember { mutableStateOf(0.6f) }
        var customThemeColor by remember { mutableStateOf(Color(0xFF00E5FF)) } // Cyan

        var interactionTrigger by remember { mutableStateOf(0) }

        // 5-second auto-collapse timer — cancelled on any interaction
        LaunchedEffect(isMinimized, interactionTrigger) {
            if (!isMinimized) {
                delay(5000L)
                isMinimized = true
            }
        }

        // History lists
        val cpuHistory = remember { mutableStateListOf<Int>() }
        val gpuHistory = remember { mutableStateListOf<Int>() }
        val frameTimeHistory = remember { mutableStateListOf<Float>() }

        val cpuPath = remember { Path() }
        val cpuFillPath = remember { Path() }
        val gpuPath = remember { Path() }
        val gpuFillPath = remember { Path() }

        val density = LocalDensity.current
        val graphStroke = remember(density) { Stroke(with(density) { 1.5.dp.toPx() }) }
        val donutStroke = remember(density) { Stroke(with(density) { 4.dp.toPx() }) }
        val maxFrameTime = remember(frameTimeHistory) { (frameTimeHistory.maxOrNull() ?: 33.3f).coerceAtLeast(20f) }

        val context = LocalContext.current
        val activityManager = remember { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
        val memoryInfo = remember { ActivityManager.MemoryInfo() }

        var ramUsageStr by remember { mutableStateOf("12.4 GB / 32 GB") }
        var ramProgress by remember { mutableStateOf(0.4f) }

        val diskReadMb = remember(telemetryData.diskRead) {
            try {
                val clean = telemetryData.diskRead.uppercase().trim()
                val num = clean.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0f
                when {
                    clean.contains("GB") -> num * 1024f
                    clean.contains("MB") -> num
                    clean.contains("KB") -> num / 1024f
                    else -> num / (1024f * 1024f)
                }
            } catch (e: Exception) { 0f }
        }

        LaunchedEffect(telemetryData) {
            cpuHistory.add(telemetryData.cpuUsage)
            if (cpuHistory.size > 30) cpuHistory.removeAt(0)

            gpuHistory.add(telemetryData.gpuUsage)
            if (gpuHistory.size > 30) gpuHistory.removeAt(0)

            val currentFps = telemetryData.fps
            val frameTime = if (currentFps > 0) 1000f / currentFps else 16.7f
            frameTimeHistory.add(frameTime)
            if (frameTimeHistory.size > 25) frameTimeHistory.removeAt(0)

            // Read RAM
            try {
                activityManager.getMemoryInfo(memoryInfo)
                val totalGb = memoryInfo.totalMem / (1024 * 1024 * 1024f)
                val availGb = memoryInfo.availMem / (1024 * 1024 * 1024f)
                val usedGb = totalGb - availGb
                ramUsageStr = String.format(Locale.US, "%.1f GB / %.1f GB", usedGb, totalGb)
                ramProgress = if (totalGb > 0) usedGb / totalGb else 0f
            } catch (e: Exception) {}
        }

        // --- Smooth transition: single Surface with animated width + AnimatedVisibility for content ---
        // Animate card width: mini (108dp) <-> full (280dp)
        val cardWidth by animateDpAsState(
            targetValue = if (isMinimized) 108.dp else 280.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "cardWidth"
        )
        // Animate background opacity: translucent when mini, opaque when expanded
        val bgAlpha by animateFloatAsState(
            targetValue = if (isMinimized) customAlpha else 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            label = "bgAlpha"
        )
        val cornerRadius by animateDpAsState(
            targetValue = if (isMinimized) 14.dp else 16.dp,
            animationSpec = tween(250),
            label = "cornerRadius"
        )

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val scrollState = rememberScrollState()
        LaunchedEffect(scrollState.value) { interactionTrigger++ }

        Surface(
            modifier = Modifier
                .width(cardWidth)
                .heightIn(max = screenHeight - 16.dp)
                .padding(4.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isMinimized) 0.25f else 0.2f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                ),
            shape = RoundedCornerShape(cornerRadius),
            color = Color(0xFF151D24).copy(alpha = bgAlpha),
            tonalElevation = if (isMinimized) 4.dp else 8.dp
        ) {
            // Unified layout: FPS header always visible, expanded content slides in/out
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (!isMinimized) Modifier.verticalScroll(scrollState) else Modifier)
                        .padding(horizontal = 10.dp, vertical = if (isMinimized) 8.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isMinimized) 0.dp else 10.dp)
                ) {
                    // ── FPS header row: always present, acts as tap-to-expand + drag handle ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!isMinimized)
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                else Modifier
                            )
                            .then(
                                if (isMinimized) {
                                    Modifier.clickable {
                                        isMinimized = false
                                        interactionTrigger++
                                    }
                                } else Modifier.clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { interactionTrigger++ }
                            )
                            .then(
                                if (!isLocked) {
                                    Modifier.pointerInput(isMinimized) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                                            if (!isMinimized) interactionTrigger++
                                        }
                                    }
                                } else Modifier
                            )
                            .padding(horizontal = if (isMinimized) 0.dp else 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val displayFps = if (overlayFps > 0f) overlayFps else telemetryData.fps
                        Text(
                            text = "FPS: ${displayFps.toInt()}",
                            color = Color(0xFF00E676),
                            fontSize = if (isMinimized) 15.sp else 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isMinimized && !isLocked) {
                            Icon(
                                imageVector = Icons.Filled.DragHandle,
                                contentDescription = "Drag Handle",
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // ── Expanded content block: slides in/out smoothly ──
                    AnimatedVisibility(
                        visible = !isMinimized,
                        enter = fadeIn(animationSpec = tween(200, delayMillis = 60)) +
                                expandVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    expandFrom = Alignment.Top
                                ),
                        exit = fadeOut(animationSpec = tween(150)) +
                               shrinkVertically(
                                   animationSpec = spring(
                                       dampingRatio = Spring.DampingRatioNoBouncy,
                                       stiffness = Spring.StiffnessMedium
                                   ),
                                   shrinkTowards = Alignment.Top
                               )
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // CPU Block
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CPU: ${telemetryData.cpuUsage}%",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "History of 30s",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 9.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(2.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        if (cpuHistory.isNotEmpty()) {
                                            cpuPath.reset()
                                            val widthStep = size.width / (cpuHistory.size - 1).coerceAtLeast(1)
                                            cpuHistory.forEachIndexed { index, value ->
                                                val x = index * widthStep
                                                val y = size.height - (value / 100f * size.height)
                                                if (index == 0) cpuPath.moveTo(x, y) else cpuPath.lineTo(x, y)
                                            }
                                            drawPath(path = cpuPath, color = customThemeColor, style = graphStroke)
                                            cpuFillPath.reset()
                                            cpuFillPath.addPath(cpuPath)
                                            cpuFillPath.lineTo(size.width, size.height)
                                            cpuFillPath.lineTo(0f, size.height)
                                            cpuFillPath.close()
                                            drawPath(path = cpuFillPath, brush = Brush.verticalGradient(colors = listOf(customThemeColor.copy(alpha = 0.2f), Color.Transparent)))
                                        }
                                    }
                                }
                            }

                            // GPU Block
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "GPU: ${telemetryData.gpuUsage}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth().height(30.dp)
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(2.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        if (gpuHistory.isNotEmpty()) {
                                            gpuPath.reset()
                                            val widthStep = size.width / (gpuHistory.size - 1).coerceAtLeast(1)
                                            gpuHistory.forEachIndexed { index, value ->
                                                val x = index * widthStep
                                                val y = size.height - (value / 100f * size.height)
                                                if (index == 0) gpuPath.moveTo(x, y) else gpuPath.lineTo(x, y)
                                            }
                                            drawPath(path = gpuPath, color = customThemeColor, style = graphStroke)
                                            gpuFillPath.reset()
                                            gpuFillPath.addPath(gpuPath)
                                            gpuFillPath.lineTo(size.width, size.height)
                                            gpuFillPath.lineTo(0f, size.height)
                                            gpuFillPath.close()
                                            drawPath(path = gpuFillPath, brush = Brush.verticalGradient(colors = listOf(customThemeColor.copy(alpha = 0.2f), Color.Transparent)))
                                        }
                                    }
                                }
                            }

                            // GPU Temp
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "GPU Temp: ${telemetryData.temperature}", color = Color.White, fontSize = 12.sp)
                                Box(modifier = Modifier.size(6.dp).background(customThemeColor, CircleShape))
                            }

                            // Metadata: active profile + update rate
                            val profileUpdateRate = when (selectedProfile) {
                                "Gaming" -> "Update: 500ms"
                                "Saver"  -> "Update: 2s"
                                else     -> "Update: 1s"
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Profile: $selectedProfile", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                Text(text = "v1.2.0 · $profileUpdateRate", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                            }

                            // Profile Selector — each profile has a distinct accent color
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                data class ProfileSpec(val label: String, val color: Color)
                                listOf(
                                    ProfileSpec("Default", Color(0xFF00E5FF)), // Cyan
                                    ProfileSpec("Gaming",  Color(0xFFFF9100)), // Orange — high power
                                    ProfileSpec("Saver",   Color(0xFF69F0AE))  // Green — eco
                                ).forEach { spec ->
                                    val isActive = selectedProfile == spec.label
                                    val accentColor = spec.color
                                    Box(
                                        modifier = Modifier.weight(1f)
                                            .background(
                                                if (isActive) accentColor.copy(alpha = 0.22f) else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = if (isActive) 1.dp else 0.dp,
                                                color = if (isActive) accentColor.copy(alpha = 0.7f) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .clickable {
                                                selectedProfile = spec.label
                                                interactionTrigger++
                                                profileManager.apply(PerformanceProfile.fromLabel(spec.label))
                                            }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isActive) {
                                                Box(modifier = Modifier.size(5.dp).background(accentColor, CircleShape))
                                            }
                                            Text(
                                                text = spec.label,
                                                color = if (isActive) accentColor else Color.White.copy(alpha = 0.5f),
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Settings panel
                            AnimatedVisibility(visible = showSettings) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Transparency", color = Color.Gray, fontSize = 9.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0.4f, 0.6f, 0.8f, 1.0f).forEach { alphaVal ->
                                            val isActive = customAlpha == alphaVal
                                            Box(
                                                modifier = Modifier.weight(1f)
                                                    .background(if (isActive) customThemeColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                    .border(1.dp, if (isActive) customThemeColor else Color.Transparent, RoundedCornerShape(4.dp))
                                                    .clickable { customAlpha = alphaVal; interactionTrigger++ }
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("${(alphaVal * 100).toInt()}%", color = Color.White, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                    Text("Theme Color", color = Color.Gray, fontSize = 9.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(Color(0xFF00E5FF) to "Cyan", Color(0xFF00E676) to "Green", Color(0xFFD500F9) to "Purple", Color(0xFFFF9100) to "Orange").forEach { (colorVal, _) ->
                                            val isActive = customThemeColor == colorVal
                                            Box(
                                                modifier = Modifier.size(20.dp).background(colorVal, CircleShape)
                                                    .border(2.dp, if (isActive) Color.White else Color.Transparent, CircleShape)
                                                    .clickable { customThemeColor = colorVal; interactionTrigger++ }
                                            )
                                        }
                                    }
                                    if (isCrosshairEnabled) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
                                        Text("Crosshair Style", color = Color.Gray, fontSize = 9.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(
                                                CrosshairStyle.DOT to "Dot",
                                                CrosshairStyle.PLUS to "Plus",
                                                CrosshairStyle.CIRCLE to "Circle",
                                                CrosshairStyle.CROSS to "Cross"
                                            ).forEach { (style, label) ->
                                                val isActive = crosshairStyle == style
                                                Box(
                                                    modifier = Modifier.weight(1f)
                                                        .background(if (isActive) customThemeColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                        .border(1.dp, if (isActive) customThemeColor else Color.Transparent, RoundedCornerShape(4.dp))
                                                        .clickable { crosshairStyle = style; interactionTrigger++ }
                                                        .padding(vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label, color = Color.White, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                        Text("Crosshair Size", color = Color.Gray, fontSize = 9.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(16.dp to "S", 24.dp to "M", 32.dp to "L", 40.dp to "XL").forEach { (sz, label) ->
                                                val isActive = crosshairSize == sz
                                                Box(
                                                    modifier = Modifier.weight(1f)
                                                        .background(if (isActive) customThemeColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                                        .border(1.dp, if (isActive) customThemeColor else Color.Transparent, RoundedCornerShape(4.dp))
                                                        .clickable { crosshairSize = sz; interactionTrigger++ }
                                                        .padding(vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label, color = Color.White, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                        Text("Crosshair Color", color = Color.Gray, fontSize = 9.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(
                                                Color(0xFF00E676) to "Green",
                                                Color(0xFFFF1744) to "Red",
                                                Color(0xFFFFEA00) to "Yellow",
                                                Color(0xFF00E5FF) to "Cyan",
                                                Color(0xFFD500F9) to "Purple"
                                            ).forEach { (colorVal, _) ->
                                                val isActive = crosshairColor == colorVal
                                                Box(
                                                    modifier = Modifier.size(16.dp).background(colorVal, CircleShape)
                                                        .border(1.5.dp, if (isActive) Color.White else Color.Transparent, CircleShape)
                                                        .clickable { crosshairColor = colorVal; interactionTrigger++ }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Collapsible sub-panel
                            AnimatedVisibility(
                                visible = !isCollapsed,
                                enter = fadeIn(tween(180)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(tween(150)) + shrinkVertically(spring(stiffness = Spring.StiffnessMedium))
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                                    // System Info
                                    Column(
                                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = "System Info", color = customThemeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("RAM Usage", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                                Text(ramUsageStr, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                            }
                                            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(3.dp))) {
                                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(ramProgress).background(customThemeColor, RoundedCornerShape(3.dp)))
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Disk Read", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                            Text(telemetryData.diskRead, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Disk Write", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                            Text(telemetryData.diskWrite, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                        }
                                    }

                                    // Frame Time Card
                                    val currentFps = telemetryData.fps
                                    val frameTime = if (currentFps > 0f) 1000f / currentFps else 16.7f
                                    Column(
                                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Frame Time", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(String.format(Locale.US, "FT: %.1fms", frameTime), color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                        }
                                        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(vertical = 2.dp, horizontal = 4.dp)) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                if (frameTimeHistory.isNotEmpty()) {
                                                    val barWidth = 4.dp.toPx()
                                                    val spacing = 2.dp.toPx()
                                                    frameTimeHistory.forEachIndexed { index, value ->
                                                        val x = index * (barWidth + spacing)
                                                        val barHeight = (value / maxFrameTime) * size.height
                                                        drawRect(color = customThemeColor.copy(alpha = if (index == frameTimeHistory.lastIndex) 1f else 0.6f), topLeft = Offset(x, size.height - barHeight), size = Size(barWidth, barHeight))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Concentric Donut Chart
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val sw = 4.dp.toPx()
                                                val sp = 2.dp.toPx()
                                                val ctr = Offset(size.width / 2, size.height / 2)
                                                val r1 = size.width / 2 - sw / 2
                                                drawCircle(Color.White.copy(alpha = 0.06f), r1, style = donutStroke)
                                                drawArc(customThemeColor, -90f, (telemetryData.cpuUsage / 100f) * 360f, false, topLeft = Offset(ctr.x-r1,ctr.y-r1), size = Size(r1*2,r1*2), style = donutStroke)
                                                val r2 = r1 - sw - sp
                                                drawCircle(Color.White.copy(alpha = 0.06f), r2, style = donutStroke)
                                                drawArc(Color(0xFF2979FF), -90f, (telemetryData.gpuUsage / 100f) * 360f, false, topLeft = Offset(ctr.x-r2,ctr.y-r2), size = Size(r2*2,r2*2), style = donutStroke)
                                                val r3 = r2 - sw - sp
                                                drawCircle(Color.White.copy(alpha = 0.06f), r3, style = donutStroke)
                                                drawArc(Color(0xFFD500F9), -90f, ramProgress * 360f, false, topLeft = Offset(ctr.x-r3,ctr.y-r3), size = Size(r3*2,r3*2), style = donutStroke)
                                                val r4 = r3 - sw - sp
                                                drawCircle(Color.White.copy(alpha = 0.06f), r4, style = donutStroke)
                                                drawArc(Color(0xFFFF9100), -90f, (diskReadMb / 500f).coerceIn(0f,1f) * 360f, false, topLeft = Offset(ctr.x-r4,ctr.y-r4), size = Size(r4*2,r4*2), style = donutStroke)
                                            }
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                                            listOf(
                                                customThemeColor to "CPU (${telemetryData.cpuUsage}%)",
                                                Color(0xFF2979FF) to "GPU (${telemetryData.gpuUsage}%)",
                                                Color(0xFFD500F9) to "RAM (${(ramProgress * 100).toInt()}%)",
                                                Color(0xFFFF9100) to "Disk"
                                            ).forEach { (c, n) ->
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(modifier = Modifier.size(8.dp).background(c, CircleShape))
                                                    Text(text = n, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } // end expanded AnimatedVisibility
                    }
                } // end left Column

                // ── Right sidebar: only shown when expanded ──
                AnimatedVisibility(
                    visible = !isMinimized,
                    enter = fadeIn(tween(200, delayMillis = 80)) + expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(tween(120)) + shrinkHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    Column(
                        modifier = Modifier.width(44.dp).background(Color.Black.copy(alpha = 0.15f)).padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = { showSettings = !showSettings; interactionTrigger++ }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = if (showSettings) customThemeColor else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                isCrosshairEnabled = !isCrosshairEnabled
                                updateCrosshairView()
                                interactionTrigger++
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "Crosshair",
                                tint = if (isCrosshairEnabled) customThemeColor else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { isCollapsed = !isCollapsed; interactionTrigger++ }, modifier = Modifier.size(24.dp)) {
                            Icon(if (isCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, contentDescription = "Toggle Collapse", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { isLocked = !isLocked; interactionTrigger++ }, modifier = Modifier.size(24.dp)) {
                            Icon(if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = "Lock overlay", tint = if (isLocked) customThemeColor else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
    }
}

    private fun createNotification(): Notification {
        val channelId = "performance_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Performance Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Performance Overlay Active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        try { stopOverlayFpsTracking() } catch (_: Throwable) {}
        try { if (::profileManager.isInitialized) profileManager.release() } catch (_: Throwable) {}
        try { if (::windowManager.isInitialized) overlayView?.let { windowManager.removeView(it) } } catch (_: Throwable) {}
        try {
            if (::windowManager.isInitialized) crosshairView?.let { windowManager.removeView(it) }
            crosshairView = null
        } catch (_: Throwable) {}
        try { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY) } catch (_: Throwable) {}
    }

    companion object {
        private const val NOTIFICATION_ID = 1002
        private val _isServiceRunningFlow = MutableStateFlow(false)
        val isServiceRunningFlow = _isServiceRunningFlow.asStateFlow()

        var isServiceRunning: Boolean
            get() = _isServiceRunningFlow.value
            set(value) {
                _isServiceRunningFlow.value = value
            }
    }
}

enum class CrosshairStyle {
    DOT, PLUS, CIRCLE, CROSS
}
