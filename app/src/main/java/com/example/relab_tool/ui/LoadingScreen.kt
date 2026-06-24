package com.example.relab_tool.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.relab_tool.R

/**
 * Full-screen branded loading screen displayed while [DeviceInfoViewModel]
 * loads initial device data. Bridges the gap between the system splash
 * and the main UI so the user never sees a blank frame.
 *
 * Design:
 *   - Dark gradient background
 *   - Animated app icon (scale-in → gentle pulse)
 *   - App name with staggered fade-in
 *   - Three-dot pulsing loading indicator
 */
@Composable
fun LoadingScreen() {
    // ── Icon entrance animation ──────────────────────────────────────────────
    val iconScale = remember { Animatable(0.4f) }
    val iconAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val dotsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Icon scale-in
        iconAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        iconScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }
    LaunchedEffect(Unit) {
        // Staggered text fade-in
        kotlinx.coroutines.delay(300)
        textAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        // Dots fade-in
        kotlinx.coroutines.delay(600)
        dotsAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    }

    // ── Gentle pulse loop for the icon ───────────────────────────────────────
    val pulseTransition = rememberInfiniteTransition(label = "icon_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // ── Background gradient ─────────────────────────────────────────────────
    val surfaceColor = MaterialTheme.colorScheme.background
    val surfaceDark = MaterialTheme.colorScheme.surfaceContainerLowest

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(surfaceDark, surfaceColor)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── App icon ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(iconScale.value * pulseScale)
                    .alpha(iconAlpha.value)
                    .clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── App name ────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Device Diagnostics",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Pulsing dots indicator ──────────────────────────────────────
            Row(
                modifier = Modifier.alpha(dotsAlpha.value),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    PulsingDot(
                        delayMs = index * 200,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingDot(delayMs: Int, color: Color) {
    val transition = rememberInfiniteTransition(label = "dot_$delayMs")
    val scale by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale_$delayMs"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha_$delayMs"
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}
