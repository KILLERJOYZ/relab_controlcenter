package com.example.relab_tool.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.relab_tool.R

/**
 * M3 Expressive Typography.
 *
 * Performance rules:
 * - Font() references are resolved at the top level (classload time), NOT inside
 *   composables. Lazy font loading inside a composable allocates on every recompose.
 * - All TextStyle objects are top-level vals — same immutability guarantee.
 * - The full M3 type scale is declared so every role has an intentional style.
 *
 * Font files must be placed in res/font/:
 *   inter_thin.ttf, inter_extralight.ttf, inter_light.ttf, inter_regular.ttf,
 *   inter_medium.ttf, inter_semibold.ttf, inter_bold.ttf, inter_extrabold.ttf,
 *   inter_black.ttf
 *
 * If the font files are not yet present the fallback FontFamily.Default is used
 * so the app compiles and runs — replace with real font refs once files are added.
 */

// ── Font family — loaded once at classload ────────────────────────────────────
// Using system default (Roboto on Android) as the safe fallback.
// To switch to Inter: add the font files to res/font/ and uncomment the block below.

// val InterFontFamily = FontFamily(
//     Font(R.font.inter_thin,       FontWeight.Thin),
//     Font(R.font.inter_extralight, FontWeight.ExtraLight),
//     Font(R.font.inter_light,      FontWeight.Light),
//     Font(R.font.inter_regular,    FontWeight.Normal),
//     Font(R.font.inter_medium,     FontWeight.Medium),
//     Font(R.font.inter_semibold,   FontWeight.SemiBold),
//     Font(R.font.inter_bold,       FontWeight.Bold),
//     Font(R.font.inter_extrabold,  FontWeight.ExtraBold),
//     Font(R.font.inter_black,      FontWeight.Black),
// )

val AppFontFamily: FontFamily = FontFamily.Default

// ── Full M3 Expressive type scale ─────────────────────────────────────────────

val Typography = Typography(
    // Display — hero/marketing copy, large numerals on dashboard cards
    displayLarge = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Black,
        fontSize     = 57.sp,
        lineHeight   = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 45.sp,
        lineHeight   = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 36.sp,
        lineHeight   = 44.sp,
        letterSpacing = 0.sp
    ),

    // Headline — section titles, card headers
    headlineLarge = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 32.sp,
        lineHeight   = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 28.sp,
        lineHeight   = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 24.sp,
        lineHeight   = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title — app bar titles, dialog titles
    titleLarge = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 22.sp,
        lineHeight   = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body — primary reading content
    bodyLarge = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label — buttons, chips, navigation items
    labelLarge = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = AppFontFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.5.sp
    ),
)