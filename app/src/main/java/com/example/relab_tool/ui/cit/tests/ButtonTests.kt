package com.example.relab_tool.ui.cit.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.relab_tool.R
import com.example.relab_tool.ui.cit.CITTestResult

@Composable
fun HardwareButtonTest(onResult: (CITTestResult) -> Unit) {
    var volUpPressed by remember { mutableStateOf(false) }
    var volDownPressed by remember { mutableStateOf(false) }
    var powerPressed by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown || event.type == KeyEventType.KeyUp) {
                    when (event.key.keyCode) {
                        Key.VolumeUp.keyCode -> {
                            volUpPressed = true
                            true
                        }
                        Key.VolumeDown.keyCode -> {
                            volDownPressed = true
                            true
                        }
                        // KEYCODE_POWER is rarely caught, but we try anyway
                        Key.Power.keyCode -> {
                            powerPressed = true
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(stringResource(id = R.string.press_physical_buttons), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(id = R.string.power_button_detect_note), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))

            ButtonRepresentation(stringResource(id = R.string.volume_up), volUpPressed)
            ButtonRepresentation(stringResource(id = R.string.volume_down), volDownPressed)
            ButtonRepresentation(stringResource(id = R.string.power_button), powerPressed)

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(id = R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(id = R.string.cit_pass)) }
            }
        }
    }
}

@Composable
fun ButtonRepresentation(label: String, isPressed: Boolean) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(60.dp)
            .background(
                if (isPressed) Color(0xFF4CAF50) else Color.LightGray,
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else Color.Black,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
