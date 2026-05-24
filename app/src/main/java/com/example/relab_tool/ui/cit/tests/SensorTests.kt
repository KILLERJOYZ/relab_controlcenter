package com.example.relab_tool.ui.cit.tests

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.relab_tool.R
import com.example.relab_tool.ui.cit.CITTestResult

@Composable
fun AccelerometerTest(onResult: (CITTestResult) -> Unit) {
    SensorTestScreen(title = stringResource(id = R.string.accelerometer_test), sensorType = Sensor.TYPE_ACCELEROMETER, onResult = onResult) { values ->
        val (x, y, z) = if (values.size >= 3) values else FloatArray(3) { 0f }
        Text(stringResource(id = R.string.sensor_values_format, x, y, z), style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Simple live bar chart
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.height(100.dp), verticalAlignment = Alignment.CenterVertically) {
            SensorBar(stringResource(id = R.string.sensor_x), x, Color.Red)
            SensorBar(stringResource(id = R.string.sensor_y), y, Color.Green)
            SensorBar(stringResource(id = R.string.sensor_z), z, Color.Blue)
        }
    }
}

@Composable
fun GyroscopeTest(onResult: (CITTestResult) -> Unit) {
    SensorTestScreen(title = stringResource(id = R.string.gyroscope_test), sensorType = Sensor.TYPE_GYROSCOPE, onResult = onResult) { values ->
        val (x, y, z) = if (values.size >= 3) values else FloatArray(3) { 0f }
        Text(stringResource(id = R.string.gyro_values_format, x, y, z), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ProximityTest(onResult: (CITTestResult) -> Unit) {
    SensorTestScreen(title = stringResource(id = R.string.proximity_test), sensorType = Sensor.TYPE_PROXIMITY, onResult = onResult) { values ->
        val dist = if (values.isNotEmpty()) values[0] else 0f
        Text(stringResource(id = R.string.distance_format, dist), style = MaterialTheme.typography.headlineMedium)
        Text(if (dist < 2.0f) stringResource(id = R.string.near_covered) else stringResource(id = R.string.far_clear), fontWeight = FontWeight.Bold, color = if (dist < 2.0f) Color.Red else Color(0xFF4CAF50))
    }
}

@Composable
fun LightSensorTest(onResult: (CITTestResult) -> Unit) {
    SensorTestScreen(title = stringResource(id = R.string.ambient_light_test), sensorType = Sensor.TYPE_LIGHT, onResult = onResult) { values ->
        val lux = if (values.isNotEmpty()) values[0] else 0f
        Text(stringResource(id = R.string.luminance_format, lux), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(id = R.string.cover_sensor_instruction), color = Color.Gray)
    }
}

@Composable
fun CompassTest(onResult: (CITTestResult) -> Unit) {
    SensorTestScreen(title = stringResource(id = R.string.compass_test), sensorType = Sensor.TYPE_MAGNETIC_FIELD, onResult = onResult) { values ->
        val (x, y, z) = if (values.size >= 3) values else FloatArray(3) { 0f }
        
        // Very basic compass simulation without accelerometer fusion
        var azimuth by remember { mutableStateOf(0f) }
        LaunchedEffect(x, y) {
            azimuth = Math.toDegrees(kotlin.math.atan2(y.toDouble(), x.toDouble())).toFloat()
        }

        Text(stringResource(id = R.string.mag_values_format, x, y, z), style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Canvas(modifier = Modifier.size(100.dp)) {
            drawCircle(color = Color.LightGray, radius = size.width / 2)
            rotate(azimuth, pivot = center) {
                drawLine(
                    color = Color.Red,
                    start = center,
                    end = androidx.compose.ui.geometry.Offset(center.x, 0f),
                    strokeWidth = 8f
                )
            }
        }
    }
}

@Composable
fun BarometerTest(onResult: (CITTestResult) -> Unit) {
    SensorTestScreen(title = stringResource(id = R.string.barometer_test), sensorType = Sensor.TYPE_PRESSURE, onResult = onResult) { values ->
        val pressure = if (values.isNotEmpty()) values[0] else 0f
        Text(stringResource(id = R.string.pressure_format, pressure), style = MaterialTheme.typography.headlineMedium)
    }
}


@Composable
fun SensorBar(label: String, value: Float, color: Color) {
    val height = (kotlin.math.abs(value) * 10).coerceIn(0f, 100f).dp
    val animatedHeight by animateDpAsState(
        targetValue = height,
        animationSpec = spring(stiffness = 50f, dampingRatio = 0.5f),
        label = "sensor_bar_height"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.width(20.dp).height(animatedHeight).background(color))
        Text(label)
    }
}

@Composable
fun SensorTestScreen(
    title: String,
    sensorType: Int,
    onResult: (CITTestResult) -> Unit,
    content: @Composable (FloatArray) -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(sensorType) }
    
    var sensorValues by remember { mutableStateOf(FloatArray(0)) }

    DisposableEffect(sensor) {
        if (sensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.values?.let { sensorValues = it.clone() }
                }
                override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        } else {
            onDispose { }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            
            if (sensor == null) {
                Text(stringResource(id = R.string.sensor_not_available), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { onResult(CITTestResult.NOT_TESTED) }) { Text(stringResource(id = R.string.cit_go_back)) }
            } else {
                content(sensorValues)

                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(id = R.string.cit_fail)) }
                    Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(id = R.string.cit_pass)) }
                }
            }
        }
    }
}
