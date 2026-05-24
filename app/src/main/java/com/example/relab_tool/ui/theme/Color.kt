package com.example.relab_tool.ui.theme

import androidx.compose.ui.graphics.Color

// ── Legacy defaults ───────────────────────────────────────────────────────────
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ── Neutral surfaces (shared across all palettes) ─────────────────────────────
// Light mode: true white surfaces
val LightBackground = Color(0xFFFFFFFF)
val LightSurface    = Color(0xFFFFFFFF)
val LightSurfaceVar = Color(0xFFF0F0F3)
val LightOnSurface  = Color(0xFF1A1A1A)

// Dark mode: warm dark-grey surfaces (not AMOLED black)
val DarkBackground  = Color(0xFF1C1C1E)
val DarkSurface     = Color(0xFF2C2C2E)
val DarkSurfaceVar  = Color(0xFF3A3A3C)
val DarkOnSurface   = Color(0xFFF2F2F7)

// High contrast light: pure black text on white
val HCLightOnSurface = Color(0xFF000000)
val HCDarkOnSurface  = Color(0xFFFFFFFF)

// ── Palette: Ocean (blue-cyan) ────────────────────────────────────────────────
val OceanPrimary    = Color(0xFF1565C0)
val OceanSecondary  = Color(0xFF0097A7)
val OceanTertiary   = Color(0xFF00695C)
val OceanPrimaryDk  = Color(0xFF90CAF9)
val OceanSecDk      = Color(0xFF80DEEA)
val OceanTertDk     = Color(0xFF80CBC4)

// ── Palette: Forest (green) ───────────────────────────────────────────────────
val ForestPrimary   = Color(0xFF2E7D32)
val ForestSecondary = Color(0xFF558B2F)
val ForestTertiary  = Color(0xFF00695C)
val ForestPrimaryDk = Color(0xFFA5D6A7)
val ForestSecDk     = Color(0xFFDCEDC8)
val ForestTertDk    = Color(0xFF80CBC4)

// ── Palette: Sunset (red-orange-amber) ───────────────────────────────────────
val SunsetPrimary   = Color(0xFFC62828)
val SunsetSecondary = Color(0xFFE65100)
val SunsetTertiary  = Color(0xFFF57F17)
val SunsetPrimaryDk = Color(0xFFEF9A9A)
val SunsetSecDk     = Color(0xFFFFCCBC)
val SunsetTertDk    = Color(0xFFFFE082)

// ── Palette: Lavender (purple-pink-indigo) ───────────────────────────────────
val LavenderPrimary   = Color(0xFF6A1B9A)
val LavenderSecondary = Color(0xFFAD1457)
val LavenderTertiary  = Color(0xFF283593)
val LavenderPrimaryDk = Color(0xFFCE93D8)
val LavenderSecDk     = Color(0xFFF48FB1)
val LavenderTertDk    = Color(0xFF9FA8DA)

// ── Palette: Mono (blue-grey) ─────────────────────────────────────────────────
val MonoPrimary   = Color(0xFF37474F)
val MonoSecondary = Color(0xFF546E7A)
val MonoTertiary  = Color(0xFF607D8B)
val MonoPrimaryDk = Color(0xFFB0BEC5)
val MonoSecDk     = Color(0xFFCFD8DC)
val MonoTertDk    = Color(0xFFECEFF1)

// ── Custom accent colors ──────────────────────────────────────────────────────
val accentColors: List<Color> = listOf(
    Color(0xFF1565C0), Color(0xFF007B6E), Color(0xFF2E7D32), Color(0xFFF57F17),
    Color(0xFFE65100), Color(0xFFC62828), Color(0xFFAD1457), Color(0xFF6A1B9A),
    Color(0xFF283593), Color(0xFF37474F),
)
val accentColorsDark: List<Color> = listOf(
    Color(0xFF90CAF9), Color(0xFF5CF0D8), Color(0xFFA5D6A7), Color(0xFFFFE082),
    Color(0xFFFFAB91), Color(0xFFEF9A9A), Color(0xFFF48FB1), Color(0xFFCE93D8),
    Color(0xFF9FA8DA), Color(0xFF90A4AE),
)
val accentColorNames: List<String> = listOf(
    "Royal Blue", "Teal", "Forest Green", "Amber",
    "Deep Orange", "Red", "Pink", "Purple", "Indigo", "Blue-Grey"
)
