package com.example.relab_tool.ui.cit.tests

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.example.relab_tool.R
import com.example.relab_tool.ui.cit.CITTestResult

@Composable
fun VibrationTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    var isVibrating by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.vibration_test_title), style = MaterialTheme.typography.headlineMedium)
            
            Button(
                onClick = {
                    isVibrating = true
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                            val vibrator = vibratorManager?.defaultVibrator
                            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator?.vibrate(500)
                            }
                        }
                    } catch (_: Throwable) {}
                }
            ) {
                Text(stringResource(R.string.trigger_vibration))
            }

            if (isVibrating) {
                Text(stringResource(R.string.vibration_test_instruction))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.cit_pass)) }
            }
        }
    }
}

@Composable
fun FingerprintTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    val readyToScan = stringResource(R.string.ready_to_scan)
    var authResult by remember { mutableStateOf(readyToScan) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.cit_fingerprint), style = MaterialTheme.typography.headlineMedium)
            Text(authResult, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            val promptTitle = stringResource(R.string.cit_device_info) + " " + stringResource(R.string.prompt_biometric_test)
            val promptSubtitle = stringResource(R.string.prompt_place_finger)
            val cancelText = stringResource(R.string.cancel)

            Button(
                onClick = {
                    val activity = context as? AppCompatActivity
                    if (activity == null) {
                        authResult = context.getString(R.string.error_not_fragment_activity)
                        return@Button
                    }
                    
                    val executor = ContextCompat.getMainExecutor(context)
                    val biometricPrompt = BiometricPrompt(activity, executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                super.onAuthenticationError(errorCode, errString)
                                authResult = "${context.getString(R.string.unknown)}: $errString"
                            }

                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                super.onAuthenticationSucceeded(result)
                                authResult = context.getString(R.string.auth_success)
                            }

                            override fun onAuthenticationFailed() {
                                super.onAuthenticationFailed()
                                authResult = context.getString(R.string.auth_failed)
                            }
                        })

                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(promptTitle)
                        .setSubtitle(promptSubtitle)
                        .setNegativeButtonText(cancelText)
                        .build()

                    biometricPrompt.authenticate(promptInfo)
                }
            ) {
                Text(stringResource(R.string.scan_fingerprint))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.cit_pass)) }
            }
        }
    }
}

@Composable
fun BatteryInfoTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    val readingBattery = stringResource(R.string.reading_battery)
    var batteryInfo by remember { mutableStateOf(readingBattery) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED && context != null) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
                    val volt = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    
                    val statusString = when (status) {
                        BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.status_charging)
                        BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.status_discharging)
                        BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.status_full)
                        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.status_not_charging)
                        else -> context.getString(R.string.unknown)
                    }

                    val tempText = if (com.example.relab_tool.utils.UnitFormatter.getMeasuringSystem(context) == "Imperial") {
                        val tempF = temp * 1.8f + 32f
                        context.getString(R.string.temp_label, tempF).replace("°C", "°F").replace("° C", "° F")
                    } else {
                        context.getString(R.string.temp_label, temp)
                    }
                    
                    batteryInfo = context.getString(R.string.level_label, (level * 100 / scale.toFloat()).toInt()) + "\n" +
                                  context.getString(R.string.status_label, statusString) + "\n" +
                                  tempText + "\n" +
                                  context.getString(R.string.voltage_label, volt)
                }
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Throwable) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.cit_battery_info), style = MaterialTheme.typography.headlineMedium)
            Text(batteryInfo, style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.cit_pass)) }
            }
        }
    }
}

@Composable
fun DeviceInfoSummaryTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    val info = listOf(
        context.getString(R.string.model_label, Build.MODEL),
        context.getString(R.string.manufacturer_label, Build.MANUFACTURER),
        context.getString(R.string.device_label, Build.DEVICE),
        context.getString(R.string.product_label, Build.PRODUCT),
        context.getString(R.string.android_version_label, Build.VERSION.RELEASE),
        context.getString(R.string.sdk_label, Build.VERSION.SDK_INT.toString()),
        context.getString(R.string.hardware_label, Build.HARDWARE),
        context.getString(R.string.board_label, Build.BOARD)
    ).joinToString("\n")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.cit_device_info), style = MaterialTheme.typography.headlineMedium)
            Text(info, style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(stringResource(R.string.cit_pass)) }
            }
        }
    }
}
