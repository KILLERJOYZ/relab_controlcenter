package com.example.relab_tool.ui.cit.tests

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.relab_tool.R
import com.example.relab_tool.ui.cit.CITTestResult
import kotlin.math.abs

@Composable
fun TouchscreenTest(onResult: (CITTestResult) -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val columns = 8
    val rows = 16
    
    // Create a set to track touched grid indices
    var touchedCells by remember { mutableStateOf(setOf<Int>()) }
    var testComplete by remember { mutableStateOf(false) }

    if (testComplete) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.touchscreen_test_complete), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
                    Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.cit_pass)) }
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (size.width > 0 && size.height > 0) {
                            val col = (offset.x / (size.width / columns.toFloat())).toInt().coerceIn(0, columns - 1)
                            val row = (offset.y / (size.height / rows.toFloat())).toInt().coerceIn(0, rows - 1)
                            val index = row * columns + col
                            touchedCells = touchedCells + index
                            if (touchedCells.size == columns * rows) testComplete = true
                        }
                    },
                    onDrag = { change, _ ->
                        if (size.width > 0 && size.height > 0) {
                            val col = (change.position.x / (size.width / columns.toFloat())).toInt().coerceIn(0, columns - 1)
                            val row = (change.position.y / (size.height / rows.toFloat())).toInt().coerceIn(0, rows - 1)
                            val index = row * columns + col
                            touchedCells = touchedCells + index
                            if (touchedCells.size == columns * rows) testComplete = true
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidth = size.width / columns.toFloat()
            val cellHeight = size.height / rows.toFloat()

            for (r in 0 until rows) {
                for (c in 0 until columns) {
                    val index = r * columns + c
                    val isTouched = touchedCells.contains(index)
                    drawRect(
                        color = if (isTouched) Color(0xFF4CAF50) else Color.LightGray,
                        topLeft = Offset(c * cellWidth, r * cellHeight),
                        size = Size(cellWidth, cellHeight)
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellWidth, r * cellHeight),
                        size = Size(cellWidth, cellHeight),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
        
        Text(
            text = stringResource(R.string.touch_cells_format, touchedCells.size, columns * rows),
            modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.6f)).padding(8.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GestureTest(onResult: (CITTestResult) -> Unit) {
    var swipeUp by remember { mutableStateOf(false) }
    var swipeDown by remember { mutableStateOf(false) }
    var swipeLeft by remember { mutableStateOf(false) }
    var swipeRight by remember { mutableStateOf(false) }
    var pinched by remember { mutableStateOf(false) }

    val allDone = swipeUp && swipeDown && swipeLeft && swipeRight && pinched

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val (x, y) = dragAmount
                    if (abs(x) > abs(y)) {
                        if (x > 20) swipeRight = true
                        if (x < -20) swipeLeft = true
                    } else {
                        if (y > 20) swipeDown = true
                        if (y < -20) swipeUp = true
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom > 1.2f || zoom < 0.8f) {
                        pinched = true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.perform_gestures_instruction), style = MaterialTheme.typography.titleLarge)
            
            GestureRow(stringResource(R.string.gesture_swipe_up), swipeUp)
            GestureRow(stringResource(R.string.gesture_swipe_down), swipeDown)
            GestureRow(stringResource(R.string.gesture_swipe_left), swipeLeft)
            GestureRow(stringResource(R.string.gesture_swipe_right), swipeRight)
            GestureRow(stringResource(R.string.gesture_pinch_zoom), pinched)

            Spacer(modifier = Modifier.height(32.dp))

            if (allDone) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
                    Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.cit_pass)) }
                }
            } else {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.skip_and_fail)) }
            }
        }
    }
}

@Composable
fun GestureRow(name: String, completed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(200.dp)) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (completed) Color(0xFF4CAF50) else Color.LightGray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal)
    }
}
