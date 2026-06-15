package com.example.relab_tool.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.relab_tool.R

/**
 * Represents a single action that can appear in the AssistiveTouch menu.
 * Each action has an icon, a string resource label, and a unique identifier.
 */
enum class MenuAction(
    val icon: ImageVector,
    val labelRes: Int,
    val id: String
) {
    HOME(Icons.Default.Home, R.string.at_action_home, "home"),
    BACK(Icons.AutoMirrored.Filled.ArrowBack, R.string.at_action_back, "back"),
    RECENTS(Icons.Default.Layers, R.string.at_action_recents, "recents"),
    NOTIFICATIONS(Icons.Default.Notifications, R.string.at_action_notifications, "notifications"),
    QUICK_SETTINGS(Icons.Default.Settings, R.string.at_action_quick_settings, "quick_settings"),
    SCREENSHOT(Icons.Default.Screenshot, R.string.at_action_screenshot, "screenshot"),
    LOCK_SCREEN(Icons.Default.Lock, R.string.at_action_lock_screen, "lock_screen"),
    VOLUME(Icons.Default.VolumeUp, R.string.at_action_volume, "volume"),
    BRIGHTNESS(Icons.Default.LightMode, R.string.at_action_brightness, "brightness"),
    CUSTOM(Icons.Default.Apps, R.string.at_action_custom, "custom");

    companion object {
        fun fromId(id: String): MenuAction? = entries.find { it.id == id }
    }
}

/**
 * Floating button size options.
 */
enum class ButtonSize(val dpSize: Int, val labelRes: Int) {
    SMALL(40, R.string.at_size_small),
    MEDIUM(56, R.string.at_size_medium),
    LARGE(72, R.string.at_size_large)
}

/**
 * Complete configuration for the AssistiveTouch feature.
 * Persisted via DataStore.
 */
data class AssistiveTouchConfig(
    val isEnabled: Boolean = false,
    val menuActions: List<MenuAction> = DEFAULT_MENU_ACTIONS,
    val menuItemCount: Int = 6,
    val buttonSize: ButtonSize = ButtonSize.MEDIUM,
    val buttonColor: Long = DEFAULT_BUTTON_COLOR,
    val buttonPositionX: Int = -1,
    val buttonPositionY: Int = 200,
    val customAppPackage: String? = null
) {
    companion object {
        const val DEFAULT_BUTTON_COLOR = 0xFF607D8B // Blue Grey 500
        val DEFAULT_MENU_ACTIONS = listOf(
            MenuAction.HOME,
            MenuAction.BACK,
            MenuAction.RECENTS,
            MenuAction.NOTIFICATIONS,
            MenuAction.SCREENSHOT,
            MenuAction.LOCK_SCREEN
        )
    }
}
