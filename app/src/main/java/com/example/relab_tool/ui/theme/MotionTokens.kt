package com.example.relab_tool.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable

/**
 * M3 Expressive motion tokens.
 *
 * Springs are frame-rate-independent: a spring that takes 300 ms at 60 Hz
 * automatically completes in ~150 ms at 120 Hz — no code changes needed.
 * This is why we use springs exclusively instead of tween() / keyframes().
 *
 * Usage:
 *   animateFloatAsState(
 *       targetValue = value,
 *       animationSpec = reducedMotionSpec(MotionTokens.SpringStandard),
 *       label = "my_animation"
 *   )
 */
object MotionTokens {

    /** Large surface transitions, screen enter/exit — slight overshoot for character */
    val SpringExpressive = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Most UI state changes — the default workhorse spring */
    val SpringStandard = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Toggles, chips, quick confirmations — near-instant, no bounce */
    val SpringSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    /** Items entering from off-screen — decelerates to rest, no overshoot */
    val SpringDecelerate = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Drag-follow and pointer-tracking — very loose, follows the finger */
    val SpringSpatial = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessVeryLow
    )
}

/**
 * Returns [full] under normal conditions, or [instant] (snap()) when the
 * system accessibility "reduce animations" flag is active.
 *
 * Apply this around EVERY animation spec — never bypass it.
 * Note: disabling animations does NOT disable high-refresh-rate rendering.
 */
@Composable
fun <T> reducedMotionSpec(
    full: AnimationSpec<T>,
    instant: AnimationSpec<T> = snap()
): AnimationSpec<T> = if (LocalReduceMotion.current) instant else full
