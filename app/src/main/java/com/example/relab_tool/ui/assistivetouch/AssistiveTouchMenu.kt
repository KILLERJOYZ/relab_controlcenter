package com.example.relab_tool.ui.assistivetouch

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.relab_tool.model.MenuAction

/**
 * Overlay menu that appears when the floating button is tapped.
 * Displays menu actions in a grid layout with scale+fade animation.
 *
 * @param actions List of actions to display.
 * @param menuItemCount Controls grid columns (4→2col, 6→3col, 8→4col).
 * @param isVisible Whether the menu is currently shown.
 * @param onActionClick Callback when an action is tapped.
 * @param onDismiss Callback when the user taps outside the menu.
 */
@Composable
fun AssistiveTouchMenu(
    actions: List<MenuAction>,
    menuItemCount: Int,
    isVisible: Boolean,
    onActionClick: (MenuAction) -> Unit,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) 200 else 150,
            easing = FastOutSlowInEasing
        ),
        label = "menu_scale"
    )
    val menuAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) 200 else 150,
            easing = FastOutSlowInEasing
        ),
        label = "menu_alpha"
    )

    if (scale > 0.01f) {
        // Full-screen dismiss layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Menu popup
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val columns = when {
                menuItemCount <= 4 -> 2
                menuItemCount <= 6 -> 3
                else -> 4
            }

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = menuAlpha
                    }
                    .shadow(24.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E2630))
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    actions.take(menuItemCount).chunked(columns).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { action ->
                                MenuActionItem(
                                    action = action,
                                    onClick = { onActionClick(action) },
                                    modifier = Modifier.width(72.dp)
                                )
                            }
                            // Fill empty slots to keep grid aligned
                            repeat(columns - row.size) {
                                Spacer(Modifier.width(72.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActionItem(
    action: MenuAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.1f)),
                onClick = onClick
            )
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = stringResource(action.labelRes),
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = stringResource(action.labelRes),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 12.sp
        )
    }
}
