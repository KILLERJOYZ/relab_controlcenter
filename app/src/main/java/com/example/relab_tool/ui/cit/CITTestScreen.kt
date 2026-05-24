package com.example.relab_tool.ui.cit

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.relab_tool.R
import com.example.relab_tool.ui.cit.tests.*

enum class CITTestResult { NOT_TESTED, PASS, FAIL }

enum class CITTestRoute {
    DASHBOARD,
    LCD, BRIGHTNESS,
    TOUCH, GESTURE,
    BUTTONS,
    EARPIECE, SPEAKER, MIC, HEADPHONE,
    CAMERA_FRONT, CAMERA_REAR, FLASHLIGHT,
    ACCEL, GYRO, PROXIMITY, LIGHT, COMPASS, BAROMETER,
    WIFI, BLUETOOTH, GPS, NFC,
    VIBRATION, FINGERPRINT, BATTERY_INFO, DEVICE_INFO
}

data class CITTestItem(
    val id: CITTestRoute,
    val name: String,
    val icon: ImageVector,
    var status: CITTestResult = CITTestResult.NOT_TESTED
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CITRootScreen(onExit: () -> Unit, viewModel: CITViewModel = viewModel()) {
    var currentRoute by rememberSaveable { mutableStateOf(CITTestRoute.DASHBOARD.name) }
    val testResults by viewModel.testResults.collectAsStateWithLifecycle()

    val navigateTo: (CITTestRoute) -> Unit = { route ->
        currentRoute = route.name
    }

    val onTestComplete: (CITTestRoute, CITTestResult) -> Unit = { route, result ->
        viewModel.updateTestResult(route, result)
        currentRoute = CITTestRoute.DASHBOARD.name
    }

    val routeEnum = try { CITTestRoute.valueOf(currentRoute) } catch (e: Exception) { CITTestRoute.DASHBOARD }

    Crossfade(targetState = routeEnum, label = "CIT_Crossfade") { route ->
        when (route) {
            CITTestRoute.DASHBOARD -> CITDashboard(testResults, navigateTo, onExit)
            CITTestRoute.LCD -> DisplayLCDTest(onResult = { onTestComplete(CITTestRoute.LCD, it) })
            CITTestRoute.BRIGHTNESS -> BrightnessTest(onResult = { onTestComplete(CITTestRoute.BRIGHTNESS, it) })
            CITTestRoute.TOUCH -> TouchscreenTest(onResult = { onTestComplete(CITTestRoute.TOUCH, it) })
            CITTestRoute.GESTURE -> GestureTest(onResult = { onTestComplete(CITTestRoute.GESTURE, it) })
            CITTestRoute.BUTTONS -> HardwareButtonTest(onResult = { onTestComplete(CITTestRoute.BUTTONS, it) })
            CITTestRoute.EARPIECE -> EarpieceTest(onResult = { onTestComplete(CITTestRoute.EARPIECE, it) })
            CITTestRoute.SPEAKER -> SpeakerTest(onResult = { onTestComplete(CITTestRoute.SPEAKER, it) })
            CITTestRoute.MIC -> MicrophoneTest(onResult = { onTestComplete(CITTestRoute.MIC, it) })
            CITTestRoute.HEADPHONE -> HeadphoneTest(onResult = { onTestComplete(CITTestRoute.HEADPHONE, it) })
            CITTestRoute.CAMERA_FRONT -> FrontCameraTest(onResult = { onTestComplete(CITTestRoute.CAMERA_FRONT, it) })
            CITTestRoute.CAMERA_REAR -> RearCameraTest(onResult = { onTestComplete(CITTestRoute.CAMERA_REAR, it) })
            CITTestRoute.FLASHLIGHT -> FlashlightTest(onResult = { onTestComplete(CITTestRoute.FLASHLIGHT, it) })
            CITTestRoute.ACCEL -> AccelerometerTest(onResult = { onTestComplete(CITTestRoute.ACCEL, it) })
            CITTestRoute.GYRO -> GyroscopeTest(onResult = { onTestComplete(CITTestRoute.GYRO, it) })
            CITTestRoute.PROXIMITY -> ProximityTest(onResult = { onTestComplete(CITTestRoute.PROXIMITY, it) })
            CITTestRoute.LIGHT -> LightSensorTest(onResult = { onTestComplete(CITTestRoute.LIGHT, it) })
            CITTestRoute.COMPASS -> CompassTest(onResult = { onTestComplete(CITTestRoute.COMPASS, it) })
            CITTestRoute.BAROMETER -> BarometerTest(onResult = { onTestComplete(CITTestRoute.BAROMETER, it) })
            CITTestRoute.WIFI -> WifiTest(onResult = { onTestComplete(CITTestRoute.WIFI, it) })
            CITTestRoute.BLUETOOTH -> BluetoothTest(onResult = { onTestComplete(CITTestRoute.BLUETOOTH, it) })
            CITTestRoute.GPS -> GPSTest(onResult = { onTestComplete(CITTestRoute.GPS, it) })
            CITTestRoute.NFC -> NFCTest(onResult = { onTestComplete(CITTestRoute.NFC, it) })
            CITTestRoute.VIBRATION -> VibrationTest(onResult = { onTestComplete(CITTestRoute.VIBRATION, it) })
            CITTestRoute.FINGERPRINT -> FingerprintTest(onResult = { onTestComplete(CITTestRoute.FINGERPRINT, it) })
            CITTestRoute.BATTERY_INFO -> BatteryInfoTest(onResult = { onTestComplete(CITTestRoute.BATTERY_INFO, it) })
            CITTestRoute.DEVICE_INFO -> DeviceInfoSummaryTest(onResult = { onTestComplete(CITTestRoute.DEVICE_INFO, it) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CITDashboard(
    results: Map<CITTestRoute, CITTestResult>,
    onNavigate: (CITTestRoute) -> Unit,
    onExit: () -> Unit
) {
    val tests = listOf(
        CITTestItem(CITTestRoute.LCD, stringResource(id = R.string.cit_lcd_color_test), Icons.Default.ScreenshotMonitor, results[CITTestRoute.LCD] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.BRIGHTNESS, stringResource(id = R.string.cit_brightness), Icons.Default.Brightness6, results[CITTestRoute.BRIGHTNESS] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.TOUCH, stringResource(id = R.string.cit_touchscreen), Icons.Default.TouchApp, results[CITTestRoute.TOUCH] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.GESTURE, stringResource(id = R.string.cit_gestures), Icons.Default.Swipe, results[CITTestRoute.GESTURE] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.BUTTONS, stringResource(id = R.string.cit_hardware_buttons), Icons.Default.AdsClick, results[CITTestRoute.BUTTONS] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.EARPIECE, stringResource(id = R.string.cit_earpiece), Icons.Default.PhoneInTalk, results[CITTestRoute.EARPIECE] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.SPEAKER, stringResource(id = R.string.cit_loudspeaker), Icons.Default.Speaker, results[CITTestRoute.SPEAKER] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.MIC, stringResource(id = R.string.cit_microphone), Icons.Default.Mic, results[CITTestRoute.MIC] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.HEADPHONE, stringResource(id = R.string.cit_headphone_jack), Icons.Default.Headphones, results[CITTestRoute.HEADPHONE] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.CAMERA_FRONT, stringResource(id = R.string.front_camera_test), Icons.Default.CameraFront, results[CITTestRoute.CAMERA_FRONT] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.CAMERA_REAR, stringResource(id = R.string.rear_camera_test), Icons.Default.CameraRear, results[CITTestRoute.CAMERA_REAR] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.FLASHLIGHT, stringResource(id = R.string.flashlight_test_title), Icons.Default.FlashlightOn, results[CITTestRoute.FLASHLIGHT] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.ACCEL, stringResource(id = R.string.accelerometer_test), Icons.Default.ScreenRotation, results[CITTestRoute.ACCEL] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.GYRO, stringResource(id = R.string.gyroscope_test), Icons.AutoMirrored.Filled.RotateRight, results[CITTestRoute.GYRO] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.PROXIMITY, stringResource(id = R.string.proximity_test), Icons.Default.SettingsOverscan, results[CITTestRoute.PROXIMITY] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.LIGHT, stringResource(id = R.string.ambient_light_test), Icons.Default.WbSunny, results[CITTestRoute.LIGHT] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.COMPASS, stringResource(id = R.string.compass_test), Icons.Default.Explore, results[CITTestRoute.COMPASS] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.BAROMETER, stringResource(id = R.string.barometer_test), Icons.Default.Cloud, results[CITTestRoute.BAROMETER] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.WIFI, stringResource(id = R.string.tab_wifi), Icons.Default.Wifi, results[CITTestRoute.WIFI] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.BLUETOOTH, stringResource(id = R.string.bluetooth), Icons.Default.Bluetooth, results[CITTestRoute.BLUETOOTH] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.GPS, stringResource(id = R.string.cit_gps_location), Icons.Default.LocationOn, results[CITTestRoute.GPS] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.NFC, stringResource(id = R.string.nfc), Icons.Default.Nfc, results[CITTestRoute.NFC] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.VIBRATION, stringResource(id = R.string.cit_vibration), Icons.Default.Vibration, results[CITTestRoute.VIBRATION] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.FINGERPRINT, stringResource(id = R.string.cit_fingerprint), Icons.Default.Fingerprint, results[CITTestRoute.FINGERPRINT] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.BATTERY_INFO, stringResource(id = R.string.cit_battery_info), Icons.Default.BatteryFull, results[CITTestRoute.BATTERY_INFO] ?: CITTestResult.NOT_TESTED),
        CITTestItem(CITTestRoute.DEVICE_INFO, stringResource(id = R.string.cit_device_info), Icons.Default.Info, results[CITTestRoute.DEVICE_INFO] ?: CITTestResult.NOT_TESTED)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.hardware_diagnostics)) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tests) { test ->
                TestCard(item = test, onClick = { onNavigate(test.id) })
            }
        }
    }
}

@Composable
fun TestCard(item: CITTestItem, onClick: () -> Unit) {
    val cardColor = when (item.status) {
        CITTestResult.NOT_TESTED -> MaterialTheme.colorScheme.surfaceVariant
        CITTestResult.PASS -> MaterialTheme.colorScheme.primaryContainer
        CITTestResult.FAIL -> MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = when (item.status) {
        CITTestResult.NOT_TESTED -> MaterialTheme.colorScheme.onSurfaceVariant
        CITTestResult.PASS -> MaterialTheme.colorScheme.onPrimaryContainer
        CITTestResult.FAIL -> MaterialTheme.colorScheme.onErrorContainer
    }

    val statusText = when (item.status) {
        CITTestResult.NOT_TESTED -> stringResource(id = R.string.cit_not_tested)
        CITTestResult.PASS -> stringResource(id = R.string.cit_pass)
        CITTestResult.FAIL -> stringResource(id = R.string.cit_fail)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(contentColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
