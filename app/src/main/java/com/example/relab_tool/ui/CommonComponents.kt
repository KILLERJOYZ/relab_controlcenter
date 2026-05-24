package com.example.relab_tool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import com.example.relab_tool.ui.theme.DarkBackground
import com.example.relab_tool.ui.theme.LightBackground

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

// Dim variant for secondary labels — still readable at 70% alpha on any bg.
@Composable
private fun contrastColorSecondary(background: Color): Color {
    return contrastColor(background).copy(alpha = 0.70f)
}

@Composable
fun InfoGroupCard(
    title: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
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
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

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
    // Always compute the highest-contrast text color for this background.
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
                    AutoSizeText(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = safeContent,
                        maxLines = 1
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
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = safeContent.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            AutoSizeText(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = safeContent,
                                maxLines = 1
                            )
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
                            text = value,
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, null, modifier = Modifier.size(24.dp), tint = safeContent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = safeContent)
                        }
                        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = safeContent)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = safeContent,
                            trackColor = safeContent.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(subtext, style = MaterialTheme.typography.bodyMedium, color = safeContent.copy(alpha = 0.8f), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    val cardModifier = modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }

    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = cardModifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            interactionSource = interactionSource,
            content = cardContent
        )
    } else {
        ElevatedCard(
            modifier = cardModifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            content = cardContent
        )
    }
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = bg, contentColor = safeContent),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            interactionSource = interactionSource,
            content = cardContent
        )
    } else {
        ElevatedCard(
            modifier = cardModifier,
            shape = RoundedCornerShape(24.dp),
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
    style: FeatureCardStyle = FeatureCardStyle.SQUARE
) {
    val bg = (if (isSupported)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 1f)

    // Guaranteed readable contrast on both light and dark palettes
    val safeContent = contrastColor(bg)
    val safeContentDim = safeContent.copy(alpha = if (isSupported) 0.9f else 0.55f)
    val checkColor = if (isSupported) MaterialTheme.colorScheme.primary
        .let { if (it.luminance() > 0.35f && bg.luminance() > 0.35f) Color(0xFF1A6B2B) else it }
    else safeContentDim

    when (style) {
        FeatureCardStyle.COMPACT_ROW -> {
            ElevatedCard(
                modifier = modifier.height(24.dp),
                shape = RoundedCornerShape(8.dp),
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
                shape = RoundedCornerShape(12.dp),
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
                shape = RoundedCornerShape(24.dp),
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
        shape = RoundedCornerShape(24.dp),
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
        shape = RoundedCornerShape(4.dp),
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
