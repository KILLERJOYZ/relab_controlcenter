package com.example.relab_tool.ui.cit.tests

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.delay

@Composable
fun WifiTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }

    val scanningWifi = stringResource(R.string.scanning_wifi)
    val notConnectedStr = stringResource(R.string.cit_wifi_not_connected)
    val ssidFormatStr = stringResource(R.string.cit_wifi_ssid_format)
    var info by remember { mutableStateOf(scanningWifi) }
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager?.connectionInfo
            if (wifiInfo != null && wifiInfo.networkId != -1) {
                isConnected = true
                info = ssidFormatStr
                    .replace("%1\$s", wifiInfo.ssid)
                    .replace("%2\$d", wifiInfo.rssi.toString())
                    .replace("%3\$d", wifiInfo.linkSpeed.toString())
            } else {
                isConnected = false
                info = notConnectedStr
            }
            delay(2000)
        }
    }

    ConnectivityScreen(stringResource(R.string.wifi) + stringResource(R.string.cit_test_suffix), info, onResult)
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasPermissions = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            hasPermissions = true
        } else {
            launcher.launch(perms.toTypedArray())
        }
    }

    val waitingStr = stringResource(R.string.cit_bt_waiting_permissions)
    val disabledStr = stringResource(R.string.cit_bt_disabled)
    val enabledStr = stringResource(R.string.cit_bt_enabled)

    var info by remember { mutableStateOf(waitingStr) }
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter = bluetoothManager?.adapter

    DisposableEffect(hasPermissions) {
        if (hasPermissions && bluetoothAdapter != null) {
            info = try { if (!bluetoothAdapter.isEnabled) disabledStr else enabledStr } catch (_: Throwable) { disabledStr }
        }
        onDispose { }
    }

    ConnectivityScreen(stringResource(R.string.bluetooth) + stringResource(R.string.cit_test_suffix), info, onResult)
}

@SuppressLint("MissingPermission")
@Composable
fun GPSTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocationPermission = it }

    val waitingStr = stringResource(R.string.cit_gps_waiting)
    var locationText by remember { mutableStateOf(waitingStr) }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return@DisposableEffect onDispose {}
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    val isImperial = com.example.relab_tool.utils.UnitFormatter.getMeasuringSystem(context) == "Imperial"
                    locationText = if (isImperial) {
                        val accuracyInFeet = loc.accuracy * 3.28084f
                        context.getString(R.string.cit_gps_location_format_imperial,
                            loc.latitude, loc.longitude, accuracyInFeet)
                    } else {
                        context.getString(R.string.cit_gps_location_format,
                            loc.latitude, loc.longitude, loc.accuracy)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    ConnectivityScreen(stringResource(R.string.cit_gps_location) + stringResource(R.string.cit_test_suffix), locationText, onResult)
}

@Composable
fun NFCTest(onResult: (CITTestResult) -> Unit) {
    val context = LocalContext.current
    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

    val nfcNotPresentStr = stringResource(R.string.cit_nfc_not_present)
    val nfcWaitingStr = stringResource(R.string.cit_nfc_waiting)
    val nfcDisabledStr = stringResource(R.string.cit_nfc_disabled)

    var info by remember { mutableStateOf(if (nfcAdapter == null) nfcNotPresentStr else nfcWaitingStr) }
    var tagDetected by remember { mutableStateOf(false) }

    DisposableEffect(nfcAdapter) {
        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled) {
                info = nfcDisabledStr
            } else {
                val activity = context as? android.app.Activity
                activity?.let {
                    nfcAdapter.enableReaderMode(it, { tag ->
                        tagDetected = true
                        val tagId = tag.id.joinToString("") { byte -> "%02X".format(byte) }
                        info = context.getString(R.string.cit_nfc_detected_format, tagId)
                    }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V, null)
                }
            }
        }

        onDispose {
            val activity = context as? android.app.Activity
            if (nfcAdapter != null && activity != null) {
                nfcAdapter.disableReaderMode(activity)
            }
        }
    }

    ConnectivityScreen(stringResource(R.string.nfc) + stringResource(R.string.cit_test_suffix), info, onResult, if (tagDetected) MaterialTheme.colorScheme.primary else Color.Gray)
}

@Composable
fun ConnectivityScreen(title: String, info: String, onResult: (CITTestResult) -> Unit, highlightColor: Color = Color.Unspecified) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)

            Text(info, style = MaterialTheme.typography.bodyLarge, color = if (highlightColor != Color.Unspecified) highlightColor else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onResult(CITTestResult.FAIL) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.cit_fail)) }
                Button(onClick = { onResult(CITTestResult.PASS) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text(stringResource(R.string.cit_pass)) }
            }
        }
    }
}
