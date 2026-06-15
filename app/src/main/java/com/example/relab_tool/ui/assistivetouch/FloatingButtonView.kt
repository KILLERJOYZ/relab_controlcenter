package com.example.relab_tool.ui.assistivetouch

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Composable for the floating AssistiveTouch button.
 *
 * @param sizeDp Diameter of the button in dp.
 * @param color  Button background color (ARGB Long).
 * @param onTap  Called when the user taps the button (should open the menu).
 * @param onDrag Called on every drag delta so the service can update WindowManager.LayoutParams.
 * @param onDragEnd Called when drag ends, with the current accumulated X for edge-snapping.
 */
@Composable
fun FloatingButton(
    sizeDp: Int,
    color: Long,
    onTap: () -> Unit,
    onDrag: (dx: Int, dy: Int) -> Unit,
    onDragEnd: () -> Unit
) {
    val buttonColor = Color(color)
    var isDragging by remember { mutableStateOf(false) }
    var isIdle by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var idleJob by remember { mutableStateOf<Job?>(null) }

    // Reset idle timer whenever user interacts
    fun resetIdleTimer() {
        isIdle = false
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(3_000L)
            isIdle = true
        }
    }

    // Start idle timer on first composition
    LaunchedEffect(Unit) {
        delay(3_000L)
        isIdle = true
    }

    val alphaTarget = when {
        isDragging -> 0.9f
        isIdle -> 0.3f
        else -> 0.7f
    }
    val alpha by animateFloatAsState(
        targetValue = alphaTarget,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "button_alpha"
    )

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(buttonColor.copy(alpha = alpha))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        resetIdleTimer()
                        onTap()
                    },
                    onPress = {
                        resetIdleTimer()
                        tryAwaitRelease()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        resetIdleTimer()
                    },
                    onDragEnd = {
                        isDragging = false
                        resetIdleTimer()
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        resetIdleTimer()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                        resetIdleTimer()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size((sizeDp * 0.5f).dp)
        )
    }
}
