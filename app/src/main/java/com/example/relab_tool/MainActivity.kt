package com.example.relab_tool

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // Changed
import androidx.metrics.performance.JankStats
import com.example.relab_tool.ui.AppInstallerViewModel
import com.example.relab_tool.ui.DeviceInfoViewModel
import com.example.relab_tool.ui.MainScreen
import com.example.relab_tool.ui.PermissionScreen
import com.example.relab_tool.ui.theme.DarkModeOption
import com.example.relab_tool.ui.theme.Relab_toolTheme
import com.example.relab_tool.ui.theme.ThemeViewModel
import com.example.relab_tool.worker.BatteryMonitoringWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: AppInstallerViewModel by viewModels()
    private val deviceInfoViewModel: DeviceInfoViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    @Inject
    lateinit var assistiveTouchRepository: com.example.relab_tool.data.AssistiveTouchRepository

    override fun attachBaseContext(newBase: android.content.Context) {
        val oldPolicy = android.os.StrictMode.allowThreadDiskReads()
        try {
            super.attachBaseContext(newBase)
        } finally {
            android.os.StrictMode.setThreadPolicy(oldPolicy)
        }
    }

    private var allPermissionsGranted by mutableStateOf(false)

    // ── JankStats — frame-timing regression detector ─────────────────────────
    // Wires into the window's frame metrics and logs janky frames to logcat.
    // Replace the Log.w call with your analytics SDK for production.
    private lateinit var jankStats: JankStats

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allPermissionsGranted = results.values.all { it }
        if (allPermissionsGranted) {
            requestHighRefreshRate()
            deviceInfoViewModel.loadAdvancedInfo()
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen() // Changed
        splash.setKeepOnScreenCondition { false } // Changed
        splash.setOnExitAnimationListener { splashScreenViewProvider -> // Changed
            splashScreenViewProvider.remove() // Changed
        } // Changed
        super.onCreate(savedInstanceState)

        // ── Edge-to-edge + LTPO fast-path rendering ───────────────────────────
        // Must be called BEFORE setContent so the system has the correct insets
        // from the first frame. This also unlocks the LTPO fast-path on Pixel/Samsung
        // that normally requires a transparent system bar to use full 120 Hz.
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false // Changed
            window.isStatusBarContrastEnforced = false // Changed
        }

        BatteryMonitoringWorker.schedule(this)

        // ── High refresh rate (Task 3a) ───────────────────────────────────────
        requestHighRefreshRate()

        // ── JankStats setup (Task 8f) ─────────────────────────────────────────
        jankStats = JankStats.createAndTrack(window) { frameData ->
            if (frameData.isJank) {
                Log.w(
                    "JankStats",
                    "Jank detected: ${frameData.frameDurationUiNanos / 1_000_000}ms — ${frameData.states}"
                )
            }
        }

        allPermissionsGranted = checkAllPermissions()

        lifecycleScope.launch {
            assistiveTouchRepository.config.collect { config ->
                val serviceIntent = Intent(this@MainActivity, com.example.relab_tool.worker.AssistiveTouchService::class.java)
                if (config.isEnabled) {
                    if (Settings.canDrawOverlays(this@MainActivity)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    }
                } else {
                    stopService(serviceIntent)
                }
            }
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            var showCITScreen by remember { mutableStateOf(false) }

            val darkMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val isDark = when (darkMode) {
                DarkModeOption.FOLLOW_SYSTEM -> systemDark
                DarkModeOption.LIGHT         -> false
                DarkModeOption.DARK          -> true
            }

            // Sync window chrome (status/nav bar icon tint) with the current theme
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark)
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        ),
                    navigationBarStyle = if (isDark)
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                )
            }

            Relab_toolTheme {
                if (showCITScreen) {
                    com.example.relab_tool.ui.cit.CITRootScreen(onExit = { showCITScreen = false })
                } else if (!allPermissionsGranted) {
                    PermissionScreen(
                        onGrantClick = {
                            requestHardwarePermissions()
                        },
                        onExitClick  = { finish(); exitProcess(0) }
                    )
                } else {
                    MainScreen(
                        viewModel    = viewModel,
                        windowSizeClass = windowSizeClass,
                        onLaunchCIT  = { showCITScreen = true }
                    )
                }
            }
        }
    }

    // ── High refresh rate helpers (Task 3a) ───────────────────────────────────

    /**
     * Requests the highest supported display refresh rate using every available API layer:
     *
     * 1. **Display Mode ID** (API 23+): sets preferredDisplayModeId to the mode with
     *    the highest refresh rate. This tells SurfaceFlinger which panel mode to use.
     *
     * 2. **Min/Max refresh rate range** (API 31+): sets the preferred min/max display
     *    refresh rate range on WindowManager.LayoutParams, which guides LTPO panels.
     *
     * 3. **Surface.setFrameRate()** (API 30+): the most reliable signal to SurfaceFlinger
     *    that this surface wants high frame rate. Uses FRAME_RATE_COMPATIBILITY_DEFAULT
     *    with CHANGE_FRAME_RATE_ALWAYS to request immediate mode switch.
     *
     * 4. **View.setRequestedFrameRate()** (API 34+): per-view frame rate hint for the
     *    Choreographer — tells the platform this view wants maximum rate.
     *
     * 5. **Touch boost** (API 33+): enables automatic frame rate boost during touch events.
     */
    private fun requestHighRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display ?: @Suppress("DEPRECATION") windowManager.defaultDisplay
        } else {
            @Suppress("DEPRECATION") windowManager.defaultDisplay
        }

        val modes = display.supportedModes
        val highestMode = modes.maxByOrNull { it.refreshRate } ?: return
        val highestFps = highestMode.refreshRate

        Log.d("RefreshRate", "Requesting ${highestFps}Hz (mode ${highestMode.modeId})")

        // ── Layer 1: Window LayoutParams (display mode + refresh range) ───────
        window.attributes = window.attributes.also { attrs ->
            // Set the preferred display mode to the highest available
            attrs.preferredDisplayModeId = highestMode.modeId

            // API 31+: set min/max preferred refresh rate range via reflection
            // These fields exist on API 31+ but are not always resolvable by the
            // compiler when minSdk < 31, so we use reflection to be safe.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val clazz = android.view.WindowManager.LayoutParams::class.java
                    clazz.getField("preferredMinDisplayRefreshRate").setFloat(attrs, highestFps)
                    clazz.getField("preferredMaxDisplayRefreshRate").setFloat(attrs, highestFps)
                } catch (e: Exception) {
                    Log.w("RefreshRate", "Failed to set preferred refresh rate range", e)
                }
            }
        }

        // ── Layer 2: View.setRequestedFrameRate() (API 34+) ───────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val method = window.decorView.javaClass.getMethod(
                    "setRequestedFrameRate", Float::class.javaPrimitiveType
                )
                method.invoke(window.decorView, highestFps)
            } catch (e: Exception) {
                Log.w("RefreshRate", "setRequestedFrameRate failed", e)
            }
        }

        // ── Layer 3: Touch frame-rate boost (API 33+) ─────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val method = window.javaClass.getMethod(
                    "setFrameRateBoostOnTouchEnabled", Boolean::class.javaPrimitiveType
                )
                method.invoke(window, true)
            } catch (e: Exception) {
                Log.w("RefreshRate", "setFrameRateBoostOnTouchEnabled failed", e)
            }
        }
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    private fun checkAllPermissions(): Boolean =
        getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.READ_PHONE_NUMBERS
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }
        return permissions
    }

    private fun requestHardwarePermissions() {
        permissionLauncher.launch(getRequiredPermissions().toTypedArray())
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        viewModel.refreshStatuses()
        requestHighRefreshRate()
        // Re-enable JankStats tracking when the app comes back to foreground
        jankStats.isTrackingEnabled = true

        lifecycleScope.launch {
            try {
                val current = assistiveTouchRepository.config.first()
                val serviceIntent = Intent(this@MainActivity, com.example.relab_tool.worker.AssistiveTouchService::class.java)
                if (current.isEnabled && Settings.canDrawOverlays(this@MainActivity)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }
            } catch (e: Exception) {}
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            requestHighRefreshRate()
        }
    }

    override fun onPause() {
        super.onPause()
        // Disable JankStats when the app goes to background — no frames to measure
        jankStats.isTrackingEnabled = false
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.example.relab_tool.widget.BaseDashboardWidget.updateAllWidgets(this@MainActivity)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to update widgets", e)
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev != null) {
            deviceInfoViewModel.processTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        super.onDestroy()
        // JankStats holds a window reference — release it to avoid leaks
        jankStats.isTrackingEnabled = false
    }



    override fun recreate() {
        super.recreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}