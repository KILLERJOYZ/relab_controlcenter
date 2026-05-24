package com.example.relab_tool.ui.theme

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt

// ── Composition local for system reduced-motion flag ──────────────────────────

/**
 * Provides `true` when the user has enabled "Remove animations" in Accessibility
 * settings. Every animation spec MUST be wrapped with [reducedMotionSpec] which
 * reads this local to decide whether to use the full spring or snap() instead.
 *
 * Important: this flag controls ANIMATION DURATION only, not refresh rate.
 * High-refresh-rate rendering continues regardless.
 */
val LocalReduceMotion = compositionLocalOf { false }

// ── Color scheme helpers ───────────────────────────────────────────────────────
// All Color() objects are top-level vals — allocated ONCE at classload.
// NEVER construct Color() inside a composable; it allocates on every recomposition.

private fun makeLight(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    highContrast: Boolean
): ColorScheme = lightColorScheme(
    primary              = primary,
    onPrimary            = Color.White,
    primaryContainer     = primary.copy(alpha = 0.15f),
    onPrimaryContainer   = if (highContrast) Color.Black else primary,
    secondary            = secondary,
    onSecondary          = Color.White,
    secondaryContainer   = secondary.copy(alpha = 0.15f),
    onSecondaryContainer = if (highContrast) Color.Black else secondary,
    tertiary             = tertiary,
    onTertiary           = Color.White,
    tertiaryContainer    = tertiary.copy(alpha = 0.15f),
    onTertiaryContainer  = if (highContrast) Color.Black else tertiary,
    background           = LightBackground,
    onBackground         = if (highContrast) HCLightOnSurface else LightOnSurface,
    surface              = LightSurface,
    onSurface            = if (highContrast) HCLightOnSurface else LightOnSurface,
    surfaceVariant       = LightSurfaceVar,
    onSurfaceVariant     = if (highContrast) Color(0xFF111111) else Color(0xFF555566),
    outline              = if (highContrast) Color(0xFF000000) else Color(0xFFBBBBCC),
    outlineVariant       = if (highContrast) Color(0xFF333333) else Color(0xFFDDDDEE),
)

private fun makeDark(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    highContrast: Boolean
): ColorScheme = darkColorScheme(
    primary              = primary,
    onPrimary            = Color(0xFF111111),
    primaryContainer     = primary.copy(alpha = 0.22f),
    onPrimaryContainer   = primary,
    secondary            = secondary,
    onSecondary          = Color(0xFF111111),
    secondaryContainer   = secondary.copy(alpha = 0.22f),
    onSecondaryContainer = secondary,
    tertiary             = tertiary,
    onTertiary           = Color(0xFF111111),
    tertiaryContainer    = tertiary.copy(alpha = 0.22f),
    onTertiaryContainer  = tertiary,
    background           = DarkBackground,
    onBackground         = if (highContrast) HCDarkOnSurface else DarkOnSurface,
    surface              = DarkSurface,
    onSurface            = if (highContrast) HCDarkOnSurface else DarkOnSurface,
    surfaceVariant       = DarkSurfaceVar,
    onSurfaceVariant     = if (highContrast) Color(0xFFEEEEEE) else Color(0xFFAAAAAA),
    outline              = if (highContrast) Color(0xFFFFFFFF) else Color(0xFF555555),
    outlineVariant       = if (highContrast) Color(0xFFCCCCCC) else Color(0xFF3A3A3A),
)

// ── M3 Expressive Theme composable ────────────────────────────────────────────

/**
 * Top-level theme composable that wires:
 *   - Dynamic color (Material You on API 31+) with brand-palette fallback
 *   - M3 Expressive shape tokens ([AppShapes])
 *   - Full M3 Expressive type scale ([Typography])
 *   - [LocalReduceMotion] so every animation can respect accessibility settings
 *
 * The theme name is kept as [Relab_toolTheme] for backwards compatibility with
 * all existing call-sites in the codebase.
 */
@Composable
fun Relab_toolTheme(content: @Composable () -> Unit) {

    val systemDark   = isSystemInDarkTheme()
    val context      = LocalContext.current

    val palette      by ThemeSettings.colorPalette.collectAsState()
    val darkMode     by ThemeSettings.darkMode.collectAsState()
    val highContrast by ThemeSettings.highContrast.collectAsState()
    val accentIdx    by ThemeSettings.customAccentIndex.collectAsState()
    val customHex    by ThemeSettings.customColorHex.collectAsState()

    val isDark = when (darkMode) {
        DarkModeOption.FOLLOW_SYSTEM -> systemDark
        DarkModeOption.LIGHT         -> false
        DarkModeOption.DARK          -> true
    }

    // Read system reduced-motion flag once per composition
    val reduceMotion = remember(context) {
        // "Remove animations" is globally controlled by the animator duration scale.
        // If scale is 0, animations should be disabled/skipped for accessibility.
        // This check is compatible with all Android versions.
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        scale == 0f
    }

    val colorScheme: ColorScheme = when (palette) {

        ColorPalette.FOLLOW_SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Material You dynamic accents — pull from device wallpaper seed
                val dynScheme = if (isDark)
                    dynamicDarkColorScheme(context)
                else
                    dynamicLightColorScheme(context)
                // Re-apply our carefully crafted neutral surfaces over dynamic accents
                if (isDark)
                    makeDark(dynScheme.primary, dynScheme.secondary, dynScheme.tertiary, highContrast)
                else
                    makeLight(dynScheme.primary, dynScheme.secondary, dynScheme.tertiary, highContrast)
            } else {
                // Pre-API-31 fallback: Ocean palette
                if (isDark) makeDark(OceanPrimaryDk, OceanSecDk, OceanTertDk, highContrast)
                else        makeLight(OceanPrimary, OceanSecondary, OceanTertiary, highContrast)
            }
        }

        ColorPalette.OCEAN ->
            if (isDark) makeDark(OceanPrimaryDk, OceanSecDk, OceanTertDk, highContrast)
            else        makeLight(OceanPrimary, OceanSecondary, OceanTertiary, highContrast)

        ColorPalette.FOREST ->
            if (isDark) makeDark(ForestPrimaryDk, ForestSecDk, ForestTertDk, highContrast)
            else        makeLight(ForestPrimary, ForestSecondary, ForestTertiary, highContrast)

        ColorPalette.SUNSET ->
            if (isDark) makeDark(SunsetPrimaryDk, SunsetSecDk, SunsetTertDk, highContrast)
            else        makeLight(SunsetPrimary, SunsetSecondary, SunsetTertiary, highContrast)

        ColorPalette.LAVENDER ->
            if (isDark) makeDark(LavenderPrimaryDk, LavenderSecDk, LavenderTertDk, highContrast)
            else        makeLight(LavenderPrimary, LavenderSecondary, LavenderTertiary, highContrast)

        ColorPalette.MONO ->
            if (isDark) makeDark(MonoPrimaryDk, MonoSecDk, MonoTertDk, highContrast)
            else        makeLight(MonoPrimary, MonoSecondary, MonoTertiary, highContrast)

        ColorPalette.CUSTOM -> {
            val baseAccent = customHex?.let { Color(it.toColorInt()) }
                ?: (if (isDark) accentColorsDark.getOrElse(accentIdx) { accentColorsDark[0] }
                    else accentColors.getOrElse(accentIdx) { accentColors[0] })

            val lightAccent = if (customHex != null) baseAccent
                else accentColors.getOrElse(accentIdx) { accentColors[0] }
            val darkAccent  = if (customHex != null) baseAccent
                else accentColorsDark.getOrElse(accentIdx) { accentColorsDark[0] }

            val secFactor  = 0.78f
            val tertFactor = 0.58f
            if (isDark) {
                val sec  = Color(
                    red   = darkAccent.red * secFactor,
                    green = darkAccent.green * secFactor,
                    blue  = darkAccent.blue,
                    alpha = 1f
                )
                val tert = Color(
                    red   = darkAccent.red * tertFactor,
                    green = darkAccent.green,
                    blue  = darkAccent.blue * tertFactor,
                    alpha = 1f
                )
                makeDark(darkAccent, sec, tert, highContrast)
            } else {
                val sec  = Color(
                    red   = lightAccent.red * secFactor,
                    green = lightAccent.green * secFactor,
                    blue  = lightAccent.blue,
                    alpha = 1f
                )
                val tert = Color(
                    red   = lightAccent.red * tertFactor,
                    green = lightAccent.green,
                    blue  = lightAccent.blue * tertFactor,
                    alpha = 1f
                )
                makeLight(lightAccent, sec, tert, highContrast)
            }
        }
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes      = AppShapes,      // M3 Expressive shape tokens
            typography  = Typography,     // Full 15-role M3 Expressive type scale
            content     = content,
        )
    }
}
