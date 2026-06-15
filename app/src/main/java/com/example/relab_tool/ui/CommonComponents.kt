package com.example.relab_tool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset // Changed
import androidx.compose.foundation.Canvas // Changed
import androidx.compose.ui.graphics.Path // Changed
import androidx.compose.ui.graphics.drawscope.Stroke // Changed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.example.relab_tool.R
import com.example.relab_tool.ui.theme.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.outlined.Info

interface CardInteractionHandler {
    fun showCardInfo(cardId: String)
    fun triggerLongPress(cardId: String)
}

val LocalCardInteractionHandler = staticCompositionLocalOf<CardInteractionHandler?> { null }

// ── Contrast utility ────────────────────────────────────────────────────────
// Picks white or black text to guarantee ≥ 4.5:1 WCAG contrast against [background].
// Because we use semi-transparent cards, we must composite the color over the expected
// system background (LightBackground or DarkBackground) to get the "actual" perceived color.
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE
) {
    var fontSizeValue by remember(text) { mutableStateOf(style.fontSize) }

    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = fontSizeValue),
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        letterSpacing = letterSpacing,
        maxLines = maxLines,
        softWrap = true,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowHeight || textLayoutResult.didOverflowWidth) {
                if (fontSizeValue > 8.sp) {
                    fontSizeValue = fontSizeValue * 0.9f
                }
            }
        }
    )
}


@Composable
fun contrastColor(background: Color): Color {
    val isDark = isSystemInDarkTheme()
    val baseBg = if (isDark) DarkBackground else LightBackground
    val actualBg = if (background.alpha < 1f) background.compositeOver(baseBg) else background
    
    val lum = actualBg.luminance()
    return if (lum > 0.45f) Color(0xFF111111) else Color.White
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InfoGroupCard(
    title: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    cardId: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val baseBg = if (isDark) DarkBackground else LightBackground
    // If the containerColor has alpha < 1f, composite it over the system background to get the correct solid color.
    val bg = if (containerColor.alpha < 1f) containerColor.compositeOver(baseBg) else containerColor
    
    // Use the passed contentColor when it contrasts well, else fall back to guaranteed contrast.
    val textOnBg = contrastColor(bg)
    // Preserve the hue of contentColor when it has good contrast; otherwise use safe default.
    val accentColor = if (containerColor == MaterialTheme.colorScheme.surface) contentColor else textOnBg
    val handler = LocalCardInteractionHandler.current

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val cardModifier = modifier
        .fillMaxWidth()
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(
            if (onClick != null || (cardId != null && handler != null)) {
                Modifier
                    .clip(ShapeCard)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true),
                        onClick = { onClick?.invoke() },
                        onLongClick = {
                            if (cardId != null && handler != null) {
                                handler.triggerLongPress(cardId)
                            }
                        }
                    )
            } else {
                Modifier
            }
        )

    Card(
        modifier = cardModifier,
        shape = ShapeCard,
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (cardId != null && handler != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(18.dp)
                        .alpha(0.5f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            handler.showCardInfo(cardId)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = textOnBg,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(end = if (cardId != null) 24.dp else 0.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Provide computed contrast colors down the tree via CompositionLocal
                // so InfoRow children automatically get the right colors.
                CompositionLocalProvider(
                    LocalContentColor provides contrastColor(bg)
                ) {
                    content()
                }
            }
        }
    }
}
@Composable
fun translateValue(value: String): String {
    return when (value.trim()) {
        "Unknown" -> stringResource(R.string.unknown)
        "Supported" -> stringResource(R.string.supported)
        "Not supported", "Not Supported" -> stringResource(R.string.not_supported)
        "Enabled" -> stringResource(R.string.enabled)
        "Disabled" -> stringResource(R.string.disabled)
        "Yes" -> stringResource(R.string.yes)
        "No" -> stringResource(R.string.no)
        "None", "none" -> stringResource(R.string.misc_none)
        
        "Charging" -> stringResource(R.string.status_charging)
        "Discharging" -> stringResource(R.string.status_discharging)
        "Full" -> stringResource(R.string.status_full)
        "Not Charging", "Not charging" -> stringResource(R.string.status_not_charging)
        
        "Good" -> stringResource(R.string.health_good)
        "Overheat" -> stringResource(R.string.health_overheat)
        "Dead" -> stringResource(R.string.health_dead)
        "Over Voltage", "Over voltage" -> stringResource(R.string.health_over_voltage)
        "Cold" -> stringResource(R.string.health_cold)
        
        "Connected" -> stringResource(R.string.status_connected)
        "Disconnected" -> stringResource(R.string.status_disconnected)
        "Removable" -> stringResource(R.string.status_removable)
        "Secondary" -> stringResource(R.string.status_secondary)
        
        "Encrypted" -> stringResource(R.string.status_encrypted)
        "Not encrypted", "Not Encrypted" -> stringResource(R.string.status_not_encrypted)
        "Rooted" -> stringResource(R.string.status_rooted)
        "Not rooted", "Not Rooted" -> stringResource(R.string.status_not_rooted)
        "Enforcing" -> stringResource(R.string.status_enforcing)
        "Locked" -> stringResource(R.string.status_locked)
        "Unlocked" -> stringResource(R.string.status_unlocked)
        
        else -> value
    }
}

@Composable
fun InfoRow(label: String, value: String, trailing: (@Composable () -> Unit)? = null) {
    // Inherits LocalContentColor set by InfoGroupCard's CompositionLocalProvider,
    // so label and value are always readable regardless of card background.
    val textColor = LocalContentColor.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.65f)
            )
            if (trailing != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailing()
            }
        }
        Text(
            translateValue(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardStatusCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    value: String,
    subtext: String,
    containerColor: Color,
    contentColor: Color,
    progress: Float? = null,
    isCharging: Boolean = false,
    size: CardSize = CardSize.SIZE_2x2,
    history: List<Float>? = null, // Changed
    cardId: String? = null,
    onClick: (() -> Unit)? = null
) {
    val localizedValue = translateValue(value)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val bg = containerColor.copy(alpha = 1f)
    // Always compute the highest-contrast text color for this background.
    val safeContent = contrastColor(bg)
    val handler = LocalCardInteractionHandler.current

    val chartPath = remember { Path() }
    val chartFillPath = remember { Path() }

    val maxWattage = remember(history) { (history?.maxOrNull() ?: 1f).coerceAtLeast(10f) }
    val minWattage = remember(history) { (history?.minOrNull() ?: 0f).coerceAtMost(0f) }
    val range = remember(maxWattage, minWattage) { (maxWattage - minWattage).coerceAtLeast(1f) }

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Box(modifier = Modifier.fillMaxSize().background(bg)) { // Changed
            if (cardId != null && handler != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .alpha(0.5f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            handler.showCardInfo(cardId)
                        }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = safeContent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (history != null && history.isNotEmpty() && size >= CardSize.SIZE_2x2) { // Changed
                val chartColor = safeContent.copy(alpha = 0.15f) // Changed
                val lineColor = safeContent.copy(alpha = 0.35f) // Changed
                Canvas( // Changed
                    modifier = Modifier // Changed
                        .fillMaxWidth() // Changed
                        .height(if (size == CardSize.SIZE_4x4) 140.dp else 60.dp) // Changed
                        .align(Alignment.BottomCenter) // Changed
                ) { // Changed
                    if (minWattage < 0) { // Changed
                        val zeroY = this.size.height - ((0f - minWattage) / range * this.size.height) // Changed
                        drawLine( // Changed
                            color = safeContent.copy(alpha = 0.1f), // Changed
                            start = Offset(0f, zeroY), // Changed
                            end = Offset(this.size.width, zeroY), // Changed
                            strokeWidth = 1.dp.toPx() // Changed
                        ) // Changed
                    } // Changed
                    chartPath.reset()
                    val stepX = if (history.size > 1) this.size.width / (history.size - 1) else this.size.width // Changed
                    history.forEachIndexed { i, v -> // Changed
                        val x = i * stepX // Changed
                        val y = this.size.height - ((v - minWattage) / range * this.size.height) // Changed
                        if (i == 0) chartPath.moveTo(x, y) else chartPath.lineTo(x, y) // Changed
                    } // Changed
                    if (history.size > 1) { // Changed
                        chartFillPath.reset()
                        chartFillPath.addPath(chartPath) // Changed
                        chartFillPath.lineTo((history.size - 1) * stepX, this@Canvas.size.height) // Changed
                        chartFillPath.lineTo(0f, this@Canvas.size.height) // Changed
                        chartFillPath.close() // Changed
                        drawPath(chartFillPath, chartColor) // Changed
                        drawPath(chartPath, lineColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)) // Changed
                    } // Changed
                } // Changed
            } // Changed
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (size) {
                    CardSize.SIZE_1x1 -> {
                        Icon(
                            icon,
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = safeContent.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isCharging) {
                                Icon(
                                    Icons.Default.Bolt,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = safeContent
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            AutoSizeText(
                                text = localizedValue,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                color = safeContent,
                                maxLines = 1
                            )
                        }
                    }
                    CardSize.SIZE_2x1 -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                icon,
                                null,
                                modifier = Modifier.size(24.dp),
                                tint = safeContent.copy(alpha = 0.9f)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = safeContent.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    if (isCharging) {
                                        Icon(
                                            Icons.Default.Bolt,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = safeContent
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    AutoSizeText(
                                        text = localizedValue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = safeContent,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    CardSize.SIZE_2x2 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                icon,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = safeContent.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = safeContent.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCharging) {
                                Icon(
                                    Icons.Default.Bolt,
                                    null,
                                    modifier = Modifier.size(28.dp),
                                    tint = safeContent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            AutoSizeText(
                                text = localizedValue,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                color = safeContent,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1
                            )
                        }
                        if (progress != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = safeContent,
                                trackColor = safeContent.copy(alpha = 0.15f),
                                strokeCap = StrokeCap.Round
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = subtext,
                            style = MaterialTheme.typography.labelSmall,
                            color = safeContent.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CardSize.SIZE_4x2, CardSize.SIZE_4x4 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, modifier = Modifier.size(24.dp), tint = safeContent.copy(alpha = 0.9f))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = safeContent.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCharging) {
                                Icon(
                                    Icons.Default.Bolt,
                                    null,
                                    modifier = Modifier.size(28.dp),
                                    tint = safeContent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            AutoSizeText(
                                text = localizedValue,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = safeContent,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (progress != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(10.dp)
                                    .clip(CircleShape),
                                color = safeContent,
                                trackColor = safeContent.copy(alpha = 0.15f),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(
                            text = subtext,
                            style = MaterialTheme.typography.bodyMedium,
                            color = safeContent.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } // Changed
    }

    val cardModifier = modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }

    val finalModifier = cardModifier.then(
        if (onClick != null || (cardId != null && handler != null)) {
            Modifier.combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = { onClick?.invoke() },
                onLongClick = {
                    if (cardId != null && handler != null) {
                        handler.triggerLongPress(cardId)
                    }
                }
            )
        } else Modifier
    )

    ElevatedCard(
        modifier = finalModifier,
        shape = ShapeCard,
        colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        content = cardContent
    )
}

@Composable
fun DashboardCounterCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    count: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    size: CardSize = CardSize.SIZE_2x2,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val bg = containerColor.copy(alpha = 1f)
    val safeContent = contrastColor(bg)

    val cardContent: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (size) {
                CardSize.SIZE_1x1 -> {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(24.dp),
                        tint = safeContent.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        count,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = safeContent
                    )
                }
                CardSize.SIZE_2x1 -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            icon,
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = safeContent.copy(alpha = 0.9f)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = safeContent.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                count,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Black,
                                color = safeContent
                            )
                        }
                    }
                }
                CardSize.SIZE_2x2 -> {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(32.dp),
                        tint = safeContent.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        count,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = safeContent,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = safeContent.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                CardSize.SIZE_4x2, CardSize.SIZE_4x4 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, modifier = Modifier.size(32.dp), tint = safeContent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = safeContent)
                        }
                        Text(
                            count,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = safeContent
                        )
                    }
                }
            }
        }
    }

    val cardModifier = modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }

    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = cardModifier,
            shape = ShapeCard,
            colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            interactionSource = interactionSource,
            content = cardContent
        )
    } else {
        ElevatedCard(
            modifier = cardModifier,
            shape = ShapeCard,
            colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            content = cardContent
        )
    }
}

enum class FeatureCardStyle {
    SQUARE,
    RECTANGLE,
    COMPACT_ROW
}

@Composable
fun DashboardFeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSupported: Boolean,
    style: FeatureCardStyle = FeatureCardStyle.SQUARE,
    dangerWhenActive: Boolean = false // Changed — when true and isSupported=true, card shows red (risk)
) {
    val isDanger = dangerWhenActive && isSupported // Changed

    val bg = when { // Changed
        isDanger    -> MaterialTheme.colorScheme.errorContainer // Changed — red background when active risk
        isSupported -> MaterialTheme.colorScheme.primaryContainer // Changed — green when supported/good
        else        -> MaterialTheme.colorScheme.surfaceVariant  // Changed — gray when off/not supported
    }.copy(alpha = 1f) // Changed

    // Guaranteed readable contrast on both light and dark palettes
    val safeContent = contrastColor(bg)
    val safeContentDim = safeContent.copy(alpha = if (isSupported) 0.9f else 0.55f)
    val checkColor = when { // Changed
        isDanger    -> MaterialTheme.colorScheme.error // Changed — red icon when active risk
        isSupported -> MaterialTheme.colorScheme.primary
            .let { if (it.luminance() > 0.35f && bg.luminance() > 0.35f) it.copy(red = it.red * 0.6f, green = it.green * 0.6f, blue = it.blue * 0.6f) else it }
        else        -> safeContentDim
    } // Changed

    when (style) {
        FeatureCardStyle.COMPACT_ROW -> {
            ElevatedCard(
                modifier = modifier.height(24.dp),
                shape = ShapeSmall,
                colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = safeContentDim
                    )
                    AutoSizeText(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        fontWeight = FontWeight.Bold,
                        color = safeContent,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isSupported) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        null,
                        modifier = Modifier.size(10.dp),
                        tint = checkColor
                    )
                }
            }
        }
        FeatureCardStyle.RECTANGLE -> {
            ElevatedCard(
                modifier = modifier.height(40.dp),
                shape = ShapeMedium,
                colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        icon,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = safeContentDim
                    )
                    AutoSizeText(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = safeContent,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (isSupported) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = checkColor
                    )
                }
            }
        }
        FeatureCardStyle.SQUARE -> {
            ElevatedCard(
                modifier = modifier.aspectRatio(1f),
                shape = ShapeCard,
                colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            icon,
                            null,
                            modifier = Modifier.size(24.dp),
                            tint = safeContentDim
                        )
                        Icon(
                            if (isSupported) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = checkColor
                        )
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = safeContent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSponsorCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val bg = containerColor.copy(alpha = 1f)
    val safeContent = contrastColor(bg)

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = ShapeCard,
        colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(28.dp),
                tint = safeContent.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = safeContent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BadgeTag(text: String, color: Color) {
    // Ensure badge text is always readable regardless of badge color
    val bg = color.copy(alpha = 1f)
    Surface(
        color = bg,
        shape = ShapeExtraSmall,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contrastColor(bg)
        )
    }
}

@Composable
fun HardwareCapabilityCard(
    icon: ImageVector,
    label: String,
    isSupported: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isSupported) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isSupported) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val iconTint = if (isSupported) {
        MaterialTheme.colorScheme.primary
    } else {
        contentColor.copy(alpha = 0.5f)
    }
    
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = ShapeExtraLarge,
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
                if (isSupported) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                ),
                color = contentColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

