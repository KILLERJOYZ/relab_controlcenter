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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.metrics.performance.JankStats
import com.example.relab_tool.ui.AppInstallerViewModel
import com.example.relab_tool.ui.DeviceInfoViewModel
import com.example.relab_tool.ui.InstallPermissionScreen
import com.example.relab_tool.ui.MainScreen
import com.example.relab_tool.ui.PermissionScreen
import com.example.relab_tool.ui.theme.DarkModeOption
import com.example.relab_tool.ui.theme.Relab_toolTheme
import com.example.relab_tool.ui.theme.ThemeViewModel
import com.example.relab_tool.worker.BatteryMonitoringWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: AppInstallerViewModel by viewModels()
    private val deviceInfoViewModel: DeviceInfoViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    private var allPermissionsGranted by mutableStateOf(false)
    private var installPermissionGranted by mutableStateOf(true)

    // ── JankStats — frame-timing regression detector ─────────────────────────
    // Wires into the window's frame metrics and logs janky frames to logcat.
    // Replace the Log.w call with your analytics SDK for production.
    private lateinit var jankStats: JankStats

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allPermissionsGranted = results.values.all { it }
        if (allPermissionsGranted) {
            installPermissionGranted = checkInstallPermissionGranted()
            requestHighRefreshRate()
            deviceInfoViewModel.loadAdvancedInfo()
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Edge-to-edge + LTPO fast-path rendering ───────────────────────────
        // Must be called BEFORE setContent so the system has the correct insets
        // from the first frame. This also unlocks the LTPO fast-path on Pixel/Samsung
        // that normally requires a transparent system bar to use full 120 Hz.
        enableEdgeToEdge()

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
        installPermissionGranted = checkInstallPermissionGranted()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            var showCITScreen by remember { mutableStateOf(false) }

            val darkMode by themeViewModel.themeMode.collectAsState()

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
                        onGrantClick = { requestHardwarePermissions() },
                        onExitClick  = { finish(); exitProcess(0) }
                    )
                } else if (!installPermissionGranted) {
                    InstallPermissionScreen(
                        onOpenSettingsClick = { openInstallPermissionSettings() },
                        onExitClick         = { finish(); exitProcess(0) }
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
     * Requests the highest supported display refresh rate from the OS.
     * - API 30+: uses the stable Display.getSupportedModes() path.
     * - API 27–29: uses the deprecated preferredRefreshRate hint as a fallback.
     * - Below API 27: no action needed (devices are 60 Hz only).
     */
    private fun requestHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display     = display ?: @Suppress("DEPRECATION") windowManager.defaultDisplay
            val modes       = display.supportedModes
            val highestMode = modes.maxByOrNull { it.refreshRate }
            val highestFps  = highestMode?.refreshRate ?: 120f
            
            window.attributes = window.attributes.also { attrs ->
                // ONLY set preferredDisplayModeId on API < 31 to prevent panel mode switch jank/flicker
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    attrs.preferredDisplayModeId = highestMode?.modeId ?: 0
                }
                
                // preferredMinDisplayRefreshRate and preferredMaxDisplayRefreshRate are hidden (@hide) fields in LayoutParams,
                // so we must access them reflectively even when targeting compileSdk 31+.
                try {
                    val minField = attrs.javaClass.getField("preferredMinDisplayRefreshRate")
                    val maxField = attrs.javaClass.getField("preferredMaxDisplayRefreshRate")
                    minField.setFloat(attrs, 60f.coerceAtMost(highestFps))
                    maxField.setFloat(attrs, highestFps)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to set LTPO dynamic refresh rate reflectively", e)
                }
            }

            // On API 34+ (Android 14+), request highest frame rate category on decorView to ensure full 120Hz/144Hz
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    window.decorView.setRequestedFrameRate(highestFps)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to setRequestedFrameRate on decorView", e)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.attributes = window.attributes.also { it.preferredRefreshRate = 120f }
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
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.READ_PHONE_NUMBERS
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
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
        installPermissionGranted = checkInstallPermissionGranted()
        requestHighRefreshRate()
        // Re-enable JankStats tracking when the app comes back to foreground
        jankStats.isTrackingEnabled = true
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

    // ── Install permission helpers ────────────────────────────────────────────

    private fun checkInstallPermissionGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    private fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
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