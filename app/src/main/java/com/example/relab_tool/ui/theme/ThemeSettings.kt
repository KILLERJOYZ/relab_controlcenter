package com.example.relab_tool.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Named color palette presets */
enum class ColorPalette {
    FOLLOW_SYSTEM,  // Material You on Android 12+, else Ocean
    OCEAN,
    FOREST,
    SUNSET,
    LAVENDER,
    MONO,
    CUSTOM          // User picks from accentColors list
}

/** Dark / Light / Follow-system */
enum class DarkModeOption { FOLLOW_SYSTEM, LIGHT, DARK }

/** Retained for backward-compat with any remaining references */
enum class ThemeStyle { FOLLOW_SYSTEM, COLOR_PALETTE, SINGLE_COLOR }

object ThemeSettings {

    private val _colorPalette = MutableStateFlow(ColorPalette.FOLLOW_SYSTEM)
    val colorPalette: StateFlow<ColorPalette> = _colorPalette.asStateFlow()

    private val _darkMode = MutableStateFlow(DarkModeOption.FOLLOW_SYSTEM)
    val darkMode: StateFlow<DarkModeOption> = _darkMode.asStateFlow()

    private val _highContrast = MutableStateFlow(false)
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    private val _customAccentIndex = MutableStateFlow(0)
    val customAccentIndex: StateFlow<Int> = _customAccentIndex.asStateFlow()

    private val _customColorHex = MutableStateFlow<String?>(null)
    val customColorHex: StateFlow<String?> = _customColorHex.asStateFlow()

    // ── Persistence — nullable to avoid UninitializedPropertyAccessException ──
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.applicationContext
            .getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
        prefs = p
        _colorPalette.value = runCatching {
            ColorPalette.valueOf(p.getString("color_palette", ColorPalette.FOLLOW_SYSTEM.name)!!)
        }.getOrDefault(ColorPalette.FOLLOW_SYSTEM)
        _darkMode.value = runCatching {
            DarkModeOption.valueOf(p.getString("dark_mode", DarkModeOption.FOLLOW_SYSTEM.name)!!)
        }.getOrDefault(DarkModeOption.FOLLOW_SYSTEM)
        _highContrast.value = p.getBoolean("high_contrast", false)
        _customAccentIndex.value = p.getInt("accent_index", 0)
        _customColorHex.value = p.getString("custom_color_hex", null)
    }

    fun setColorPalette(palette: ColorPalette) {
        _colorPalette.value = palette
        prefs?.edit()?.putString("color_palette", palette.name)?.apply()
    }

    fun setDarkMode(mode: DarkModeOption) {
        _darkMode.value = mode
        prefs?.edit()?.putString("dark_mode", mode.name)?.apply()
    }

    fun setHighContrast(enabled: Boolean) {
        _highContrast.value = enabled
        prefs?.edit()?.putBoolean("high_contrast", enabled)?.apply()
    }

    fun setCustomAccentIndex(index: Int) {
        _customAccentIndex.value = index
        _customColorHex.value = null
        prefs?.edit()?.putInt("accent_index", index)?.remove("custom_color_hex")?.apply()
    }

    fun setCustomColor(color: Color) {
        val hex = String.format("#%08X", color.toArgb())
        _customColorHex.value = hex
        _colorPalette.value = ColorPalette.CUSTOM
        prefs?.edit()
            ?.putString("custom_color_hex", hex)
            ?.putString("color_palette", ColorPalette.CUSTOM.name)
            ?.apply()
    }

    // Legacy setters (keep for any surviving callers)
    fun setThemeStyle(style: ThemeStyle) {
        setColorPalette(when (style) {
            ThemeStyle.FOLLOW_SYSTEM -> ColorPalette.FOLLOW_SYSTEM
            ThemeStyle.COLOR_PALETTE -> ColorPalette.OCEAN
            ThemeStyle.SINGLE_COLOR  -> ColorPalette.CUSTOM
        })
    }
    fun setAccentIndex(i: Int) = setCustomAccentIndex(i)
    fun setThemeStyleOld(style: ThemeStyle) = setThemeStyle(style)
}
