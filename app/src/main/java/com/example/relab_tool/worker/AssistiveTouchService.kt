package com.example.relab_tool.worker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.*
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.relab_tool.data.AssistiveTouchRepository
import com.example.relab_tool.model.AssistiveTouchConfig
import com.example.relab_tool.model.MenuAction
import com.example.relab_tool.ui.assistivetouch.AssistiveTouchMenu
import com.example.relab_tool.ui.assistivetouch.FloatingButton
import com.example.relab_tool.ui.theme.Relab_toolTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class AssistiveTouchService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner {

    @Inject
    lateinit var repository: AssistiveTouchRepository

    private lateinit var windowManager: WindowManager
    private var buttonView: View? = null
    private var menuView: View? = null
    private var buttonParams: WindowManager.LayoutParams? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry =
        savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentConfig = AssistiveTouchConfig()
    private var isMenuVisible = mutableStateOf(false)
    private var screenWidth = 0
    private var screenHeight = 0

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        _isRunning.value = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        updateScreenDimensions()

        serviceScope.launch {
            repository.config.collectLatest { config ->
                val wasEnabled = currentConfig.isEnabled
                currentConfig = config
                if (config.isEnabled) {
                    if (buttonView == null) {
                        showFloatingButton()
                    } else {
                        // Config changed, recreate button with new size/color
                        removeFloatingButton()
                        showFloatingButton()
                    }
                } else {
                    removeFloatingButton()
                    removeMenu()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        serviceInfo = serviceInfo.apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op — we only need this service for performGlobalAction()
    }

    override fun onInterrupt() {
        // Cleanup on interrupt
        removeMenu()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateScreenDimensions()
        // Re-snap button to nearest edge after rotation
        buttonParams?.let { params ->
            val halfScreen = screenWidth / 2
            val targetX = if (params.x + currentConfig.buttonSize.dpSize / 2 < halfScreen) 0
            else screenWidth - dpToPx(currentConfig.buttonSize.dpSize)
            params.x = targetX
            params.y = params.y.coerceIn(0, screenHeight - dpToPx(currentConfig.buttonSize.dpSize))
            try {
                buttonView?.let { windowManager.updateViewLayout(it, params) }
            } catch (_: Exception) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        serviceScope.cancel()
        removeMenu()
        removeFloatingButton()
        try {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        } catch (_: Throwable) { }
    }

    // ─── Floating Button ─────────────────────────────────────────────────────

    private fun showFloatingButton() {
        if (!Settings.canDrawOverlays(this)) return

        val sizePx = dpToPx(currentConfig.buttonSize.dpSize)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (currentConfig.buttonPositionX >= 0) currentConfig.buttonPositionX
            else screenWidth - sizePx
            y = currentConfig.buttonPositionY.coerceIn(0, screenHeight - sizePx)
        }
        buttonParams = params

        val composeView = ComposeView(this).apply {
            setContent {
                Relab_toolTheme {
                    FloatingButton(
                        sizeDp = currentConfig.buttonSize.dpSize,
                        color = currentConfig.buttonColor,
                        onTap = { toggleMenu() },
                        onDrag = { dx, dy ->
                            params.x = (params.x + dx).coerceIn(0, screenWidth - sizePx)
                            params.y = (params.y + dy).coerceIn(0, screenHeight - sizePx)
                            try {
                                buttonView?.let { windowManager.updateViewLayout(it, params) }
                            } catch (_: Exception) { }
                        },
                        onDragEnd = {
                            snapToEdge(params, sizePx)
                            // Persist position
                            serviceScope.launch {
                                repository.setButtonPosition(params.x, params.y)
                            }
                        }
                    )
                }
            }
        }
        setupViewTreeOwners(composeView)

        try {
            windowManager.addView(composeView, params)
            buttonView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingButton() {
        buttonView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        buttonView = null
        buttonParams = null
    }

    // ─── Menu ────────────────────────────────────────────────────────────────

    private fun toggleMenu() {
        if (isMenuVisible.value) {
            isMenuVisible.value = false
        } else {
            showMenu()
        }
    }

    private fun showMenu() {
        removeMenu()
        isMenuVisible.value = true

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val composeView = ComposeView(this).apply {
            setContent {
                Relab_toolTheme {
                    val visible by isMenuVisible
                    AssistiveTouchMenu(
                        actions = currentConfig.menuActions,
                        menuItemCount = currentConfig.menuItemCount,
                        isVisible = visible,
                        onActionClick = { action ->
                            executeAction(action)
                            isMenuVisible.value = false
                        },
                        onDismiss = {
                            isMenuVisible.value = false
                        }
                    )

                    // Auto-remove menu view when animation finishes
                    LaunchedEffect(visible) {
                        if (!visible) {
                            delay(200) // Wait for exit animation
                            removeMenu()
                        }
                    }
                }
            }
        }
        setupViewTreeOwners(composeView)

        try {
            windowManager.addView(composeView, params)
            menuView = composeView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeMenu() {
        menuView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) { }
        }
        menuView = null
    }

    // ─── Action Execution ────────────────────────────────────────────────────

    private fun executeAction(action: MenuAction) {
        when (action) {
            MenuAction.HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            MenuAction.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            MenuAction.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            MenuAction.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            MenuAction.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            MenuAction.SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }
            MenuAction.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                }
            }
            MenuAction.VOLUME -> {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_SAME,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            MenuAction.BRIGHTNESS -> {
                try {
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                } catch (_: Exception) { }
            }
            MenuAction.CUSTOM -> {
                launchCustomApp()
            }
        }
    }

    private fun launchCustomApp() {
        val packageName = currentConfig.customAppPackage ?: return
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot launch app", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Edge Snapping ───────────────────────────────────────────────────────

    private fun snapToEdge(params: WindowManager.LayoutParams, sizePx: Int) {
        val currentX = params.x
        val halfScreen = screenWidth / 2
        val targetX = if (currentX + sizePx / 2 < halfScreen) 0
        else screenWidth - sizePx

        val animator = ValueAnimator.ofInt(currentX, targetX)
        animator.duration = 250
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            params.x = anim.animatedValue as Int
            try {
                buttonView?.let { windowManager.updateViewLayout(it, params) }
            } catch (_: Exception) {
                animator.cancel()
            }
        }
        animator.start()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun updateScreenDimensions() {
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupViewTreeOwners(view: ComposeView) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        val store = ViewModelStore()
        view.setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        })
    }

    // ─── Companion ───────────────────────────────────────────────────────────

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()
    }
}
