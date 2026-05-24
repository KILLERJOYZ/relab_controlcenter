package com.example.relab_tool.ui.cit.tests

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.relab_tool.R
import com.example.relab_tool.ui.cit.CITTestResult
import kotlinx.coroutines.delay

@Composable
fun DisplayLCDTest(onResult: (CITTestResult) -> Unit) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    var colorIndex by remember { mutableStateOf(0) }
    var showConfirmation by remember { mutableStateOf(false) }

    val animatedColor by animateColorAsState(
        targetValue = colors[colorIndex],
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "lcd_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showConfirmation) MaterialTheme.colorScheme.background else animatedColor)
            .clickable {
                if (!showConfirmation) {
                    if (colorIndex < colors.size - 1) {
                        colorIndex++
                    } else {
                        showConfirmation = true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!showConfirmation) {
            Text(
                text = stringResource(R.string.tap_to_change_color),
                color = if (animatedColor == Color.White) Color.Black else Color.White,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f)).padding(8.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.lcd_test_complete),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.display_test_instruction))
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onResult(CITTestResult.FAIL) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.cit_fail))
                    }
                    Button(
                        onClick = { onResult(CITTestResult.PASS) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.cit_pass))
                    }
                }
            }
        }
    }
}

@Composable
fun BrightnessTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    var testComplete by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf("Starting...") }

    // Backup original brightness
    val originalBrightness = remember { activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let {
                val params = it.window.attributes
                params.screenBrightness = originalBrightness
                it.window.attributes = params
            }
        }
    }

    LaunchedEffect(Unit) {
        if (activity == null) {
            currentPhase = "Error: Not an Activity"
            testComplete = true
            return@LaunchedEffect
        }

        // Ramp down
        currentPhase = "Dimming to 0%"
        for (i in 100 downTo 0 step 5) {
            val params = activity.window.attributes
            params.screenBrightness = i / 100f
            activity.window.attributes = params
            delay(50)
        }

        delay(500)

        // Ramp up
        currentPhase = "Increasing to 100%"
        for (i in 0..100 step 5) {
            val params = activity.window.attributes
            params.screenBrightness = i / 100f
            activity.window.attributes = params
            delay(50)
        }

        delay(500)

        // Restore
        val params = activity.window.attributes
        params.screenBrightness = originalBrightness
        activity.window.attributes = params

        currentPhase = "Test Finished"
        testComplete = true
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.cit_brightness),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(currentPhase)
            Spacer(modifier = Modifier.height(32.dp))

            if (testComplete) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { onResult(CITTestResult.FAIL) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.cit_fail))
                    }
                    Button(
                        onClick = { onResult(CITTestResult.PASS) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.cit_pass))
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
