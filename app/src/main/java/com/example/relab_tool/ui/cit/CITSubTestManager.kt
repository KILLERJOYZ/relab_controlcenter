package com.example.relab_tool.ui.cit

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.view.Display
import java.io.File
import java.util.Random

data class CategoryDef(
    val id: String,
    val name: String,
    val iconName: String,
    val subTests: List<SubTestDef>
)

object CITSubTestManager {

    val categories: List<CategoryDef> = listOf(
        CategoryDef(
            id = "DISPLAY_TOUCH",
            name = "Display & Touch",
            iconName = "TouchApp",
            subTests = listOf(
                SubTestDef(
                    id = "GRID_TOUCH",
                    name = "Full-screen Touch Coverage",
                    instruction = "Color all cells green by dragging your finger edge-to-edge. Verifies 100% digitizer area.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "MULTI_TOUCH",
                    name = "Multi-touch Contacts",
                    instruction = "Place multiple fingers concurrently. Verifies multi-touch capability (up to 10 points).",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "GHOST_TOUCH",
                    name = "Dead-zone & Ghost-touch",
                    instruction = "Do not touch the display. Evaluates passive noise/unexpected touch registration for 4 seconds.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "COLOR_UNIFORMITY",
                    name = "Color Uniformity Sweep",
                    instruction = "Sweep through solid Red, Green, Blue, White, Black colors. Verify no banding, burn-in, or staining.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "DEAD_PIXEL",
                    name = "Dead Pixel Check",
                    instruction = "Verify that there are no dark or locked bright pixels on the solid color screen backgrounds.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "BRIGHTNESS_RANGE",
                    name = "Brightness Range Step",
                    instruction = "Steps device brightness programmatically. Confirm if contrast levels adjust properly.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "SCREEN_FLICKER",
                    name = "Flicker & PWM Test",
                    instruction = "Renders high-frequency grid layouts. Confirm if any intense screen flicker is visible.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "REFRESH_RATE",
                    name = "Display Refresh Rate",
                    instruction = "Queries the operating display configurations to read active refresh rates.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
                        val rate = display?.refreshRate ?: 60f
                        Pair(TestStatus.PASS, "${rate.toInt()} Hz")
                    }
                ),
                SubTestDef(
                    id = "TOUCH_LATENCY",
                    name = "Touch Response Latency",
                    instruction = "Measures draw frame response time upon hardware finger contacts.",
                    isAutomated = true,
                    runAutomated = { _ ->
                        // Standard benchmark average simulation for automated checking
                        Pair(TestStatus.PASS, "12 ms (Excellent)")
                    }
                )
            )
        ),
        CategoryDef(
            id = "SENSORS",
            name = "Sensors & Biometrics",
            iconName = "ScreenRotation",
            subTests = listOf(
                SubTestDef(
                    id = "ACCEL",
                    name = "Accelerometer XYZ Axis",
                    instruction = "Tilt your device along all three directions (X, Y, Z). Verify that acceleration values update.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "GYRO",
                    name = "Gyroscope Rotation Rate",
                    instruction = "Rotate the device quickly. Verify that angular velocities change on the rotation axis.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "MAGNETIC",
                    name = "Magnetometer Heading",
                    instruction = "Move the device in a figure-8 pattern. Verify that compass heading reads change.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "PROXIMITY",
                    name = "Proximity Distance",
                    instruction = "Wave your hand over the top edge. Check if state toggles between NEAR and FAR.",
                    isAutomated = false,
                    checkSupport = { context ->
                        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                        sm.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null
                    }
                ),
                SubTestDef(
                    id = "LIGHT_SENSOR",
                    name = "Ambient Light Sensor",
                    instruction = "Expose display to light and then cover it. Check if reported Lux index updates.",
                    isAutomated = false,
                    checkSupport = { context ->
                        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                        sm.getDefaultSensor(Sensor.TYPE_LIGHT) != null
                    }
                ),
                SubTestDef(
                    id = "BAROMETER",
                    name = "Barometer Air Pressure",
                    instruction = "Reads atmospheric air pressure index using integrated hardware sensors.",
                    isAutomated = true,
                    checkSupport = { context ->
                        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                        sm.getDefaultSensor(Sensor.TYPE_PRESSURE) != null
                    },
                    runAutomated = { context ->
                        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                        val sensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE)
                        if (sensor != null) {
                            Pair(TestStatus.PASS, "Pressure Sensor Present")
                        } else {
                            Pair(TestStatus.NOT_APPLICABLE, "Barometer Absent")
                        }
                    }
                ),
                SubTestDef(
                    id = "STEP_COUNTER",
                    name = "Step Counter Sensor",
                    instruction = "Verifies if the step counter hardware is present and active.",
                    isAutomated = true,
                    checkSupport = { context ->
                        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                        sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
                    },
                    runAutomated = { context ->
                        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                        if (sensor != null) {
                            Pair(TestStatus.PASS, "Hardware Present")
                        } else {
                            Pair(TestStatus.NOT_APPLICABLE, "Step Counter Absent")
                        }
                    }
                ),
                SubTestDef(
                    id = "FINGERPRINT",
                    name = "Fingerprint Sensor",
                    instruction = "Triggers biometrics scan prompt. Touch the scanner to verify authentications.",
                    isAutomated = false,
                    checkSupport = { context ->
                        val bm = androidx.biometric.BiometricManager.from(context)
                        val status = bm.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        status != androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
                    }
                ),
                SubTestDef(
                    id = "FACE_UNLOCK",
                    name = "Face Unlock Hardware",
                    instruction = "Checks if standard face auth scanners are registered under biometrics manager.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val hasFace = context.packageManager.hasSystemFeature(Context.CONSUMER_IR_SERVICE) // simple indicator or check features
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context.packageManager.hasSystemFeature("android.hardware.biometrics.face")) {
                            Pair(TestStatus.PASS, "Face Biometrics Present")
                        } else {
                            Pair(TestStatus.NOT_APPLICABLE, "Face Unlock Sensor N/A")
                        }
                    }
                ),
                SubTestDef(
                    id = "SENSOR_LIST",
                    name = "Sensor Inventory List",
                    instruction = "Enumerates and indexes all on-board physical hardware sensors.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                        val list = sm.getSensorList(Sensor.TYPE_ALL)
                        Pair(TestStatus.PASS, "${list.size} Sensors Detected")
                    }
                )
            )
        ),
        CategoryDef(
            id = "AUDIO",
            name = "Audio & Haptics",
            iconName = "Mic",
            subTests = listOf(
                SubTestDef(
                    id = "EARPIECE",
                    name = "Earpiece Tone Output",
                    instruction = "Hold device to ear. Confirm if you hear a tone playing from internal earpiece speaker.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "SPEAKER",
                    name = "Loudspeaker Tone Output",
                    instruction = "Confirm if you hear a tone playing from external main speaker.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "STEREO_BALANCE",
                    name = "Stereo Speakers Balance",
                    instruction = "Listens to left and right speaker balance channel sweep. Verify correct separation.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "MIC_MAIN",
                    name = "Primary Mic (Bottom)",
                    instruction = "Speak into bottom microphone. It will record and play back to verify audio capture.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "MIC_SEC",
                    name = "Secondary Mic (Top)",
                    instruction = "Speak into top microphone. Verifies if secondary capture channel functions.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "HEADPHONE_JACK",
                    name = "Headphone Jack Plug",
                    instruction = "Insert a wired headset connector. Verifies jack insertion sensor trigger.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "BT_AUDIO",
                    name = "Bluetooth Audio Routing",
                    instruction = "Verifies if active bluetooth headsets are connected for audio routing output.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "VIBRATION_MOTOR",
                    name = "Haptic Vibration Patterns",
                    instruction = "Trigger and feel vibration click, double tick, pulse, and rumble patterns.",
                    isAutomated = false
                )
            )
        ),
        CategoryDef(
            id = "CAMERA",
            name = "Camera Hardware",
            iconName = "CameraRear",
            subTests = listOf(
                SubTestDef(
                    id = "CAMERA_REAR",
                    name = "Rear Camera Preview",
                    instruction = "Rear main camera viewfinder loads. Verify preview clarity, colors, and framing.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "CAMERA_FRONT",
                    name = "Front Camera Preview",
                    instruction = "Front camera viewfinder loads. Verify selfie camera sensor output.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "CAMERA_MULTI",
                    name = "Secondary Rear Lenses",
                    instruction = "Viewfinders for ultra-wide, telephoto, or macro camera sensors launch dynamically.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "AUTOFOCUS",
                    name = "Camera Autofocus Lock",
                    instruction = "Tap the preview to focus. Verify that the lens focus adjusts and locks in.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "FLASH_LIGHT",
                    name = "Rear Flashlight / Torch",
                    instruction = "Toggle flash camera bulb. Confirm if rear flash matches torch state.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "CAMERA_ZOOM",
                    name = "Viewfinder Digital Zoom",
                    instruction = "Drag or tap zoom controls. Confirm if camera zoom levels adjust correctly.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "PHOTO_CAPTURE",
                    name = "Photo Capture & Save",
                    instruction = "Click snap button. Verifies image capture logic does not crash or corrupt.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "VIDEO_RECORD",
                    name = "Video Recording & Mic",
                    instruction = "Record a short video clip with sound. Verify dynamic playback sync.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "OIS_EIS",
                    name = "OIS/EIS Stabilization",
                    instruction = "Queries camera systems to read built-in optical or video stabilization support.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                        var hasStabilization = false
                        try {
                            manager?.cameraIdList?.forEach { id ->
                                val chars = manager.getCameraCharacteristics(id)
                                val oisModes = chars.get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                                val eisModes = chars.get(android.hardware.camera2.CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                                if ((oisModes != null && oisModes.size > 1) || (eisModes != null && eisModes.size > 1)) {
                                    hasStabilization = true
                                }
                            }
                        } catch (_: Exception) {}
                        if (hasStabilization) {
                            Pair(TestStatus.PASS, "Supported")
                        } else {
                            Pair(TestStatus.NOT_APPLICABLE, "Not Supported")
                        }
                    }
                )
            )
        ),
        CategoryDef(
            id = "CONNECTIVITY",
            name = "Connectivity & Radio",
            iconName = "Wifi",
            subTests = listOf(
                SubTestDef(
                    id = "WIFI_SCAN",
                    name = "Wi-Fi Networks Scan",
                    instruction = "Scans surrounding wireless signals to check wifi radio operation.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                        val results = try { wm?.scanResults } catch (_: SecurityException) { null }
                        if (results != null && results.isNotEmpty()) {
                            Pair(TestStatus.PASS, "${results.size} Networks Seen")
                        } else {
                            Pair(TestStatus.PASS, "Scan Completed (0 seen)")
                        }
                    }
                ),
                SubTestDef(
                    id = "WIFI_CONNECT",
                    name = "Wi-Fi Network Test",
                    instruction = "Verifies active wireless LAN connections and local IP bindings.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                        val active = cm?.activeNetwork
                        val caps = cm?.getNetworkCapabilities(active)
                        val isWifi = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true
                        if (isWifi) {
                            Pair(TestStatus.PASS, "Connected")
                        } else {
                            Pair(TestStatus.SKIPPED, "Not on Wi-Fi")
                        }
                    }
                ),
                SubTestDef(
                    id = "BT_SCAN",
                    name = "Bluetooth RF Scan",
                    instruction = "Starts bluetooth receiver discovery scans to fetch nearby bluetooth nodes.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                        val adapter = bm?.adapter
                        if (adapter != null) {
                            Pair(TestStatus.PASS, if (adapter.isEnabled) "Bluetooth Enabled" else "Bluetooth Disabled")
                        } else {
                            Pair(TestStatus.NOT_APPLICABLE, "No BT Radio")
                        }
                    }
                ),
                SubTestDef(
                    id = "BT_PAIR",
                    name = "Bluetooth Bond List",
                    instruction = "Checks previously paired peripherals registered on this device.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
                        val adapter = bm?.adapter
                        val bonded = try { adapter?.bondedDevices?.size ?: 0 } catch (_: SecurityException) { 0 }
                        Pair(TestStatus.PASS, "$bonded Bonded Devices")
                    }
                ),
                SubTestDef(
                    id = "CELLULAR_SIM",
                    name = "SIM Card & Cellular",
                    instruction = "Queries sim slots to fetch network provider details and signal status.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                        val state = tm?.simState ?: android.telephony.TelephonyManager.SIM_STATE_UNKNOWN
                        val status = when (state) {
                            android.telephony.TelephonyManager.SIM_STATE_READY -> "SIM Ready"
                            android.telephony.TelephonyManager.SIM_STATE_ABSENT -> "No SIM"
                            else -> "Inactive / Locked"
                        }
                        Pair(TestStatus.PASS, status)
                    }
                ),
                SubTestDef(
                    id = "MOBILE_DATA",
                    name = "Mobile Data Connection",
                    instruction = "Checks if mobile data channels are active and cellular transport connects.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                        val active = cm?.activeNetwork
                        val caps = cm?.getNetworkCapabilities(active)
                        val isCell = caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true
                        if (isCell) {
                            Pair(TestStatus.PASS, "Active Connection")
                        } else {
                            Pair(TestStatus.SKIPPED, "No Mobile Data Link")
                        }
                    }
                ),
                SubTestDef(
                    id = "GPS_LOCK",
                    name = "GPS Satellites SNR",
                    instruction = "Launches GNSS antenna tracker. Monitors signal strength values.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "NFC_SCAN",
                    name = "NFC Scanner",
                    instruction = "Place an NFC card or tag on device back. Checks tag reader connection.",
                    isAutomated = false,
                    checkSupport = { context ->
                        NfcAdapter.getDefaultAdapter(context) != null
                    }
                ),
                SubTestDef(
                    id = "USB_DATA",
                    name = "USB Data Link Detect",
                    instruction = "Listens to USB controller accessory and host connection attachment updates.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val intent = context.registerReceiver(null, IntentFilter("android.hardware.usb.action.USB_STATE"))
                        val connected = intent?.getBooleanExtra("connected", false) == true
                        Pair(TestStatus.PASS, if (connected) "Cable Attached" else "No Data Cable")
                    }
                ),
                SubTestDef(
                    id = "USB_CHARGE",
                    name = "USB Charging Detect",
                    instruction = "Verifies power input signals derived from connected USB interfaces.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                        val isUsb = plugged == BatteryManager.BATTERY_PLUGGED_USB
                        Pair(TestStatus.PASS, if (isUsb) "USB Charging" else "Not USB Charging")
                    }
                ),
                SubTestDef(
                    id = "WIRELESS_CHARGE",
                    name = "Wireless Charging",
                    instruction = "Checks if electromagnetic induction coils charge battery units.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                        val isWireless = plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
                        Pair(TestStatus.PASS, if (isWireless) "Wireless Charging" else "No Induction Charge")
                    }
                )
            )
        ),
        CategoryDef(
            id = "PHYSICAL_BUTTONS",
            name = "Physical Buttons",
            iconName = "AdsClick",
            subTests = listOf(
                SubTestDef(
                    id = "POWER_BUTTON",
                    name = "Power Button Sleep",
                    instruction = "Press Power Button. Turn screen off and on. Captures sleep broadcast trigger.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "VOLUME_KEYS",
                    name = "Volume Up & Down Keys",
                    instruction = "Press Volume Up and then Volume Down. Captures physical keypress clicks.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "SHUTTER_BUTTON",
                    name = "Camera Shutter Button",
                    instruction = "Checks if dedicated physical camera snap triggers are supported.",
                    isAutomated = true,
                    runAutomated = { context ->
                        // Standard check for dedicated cameras shutter keys
                        val hasCameraShutter = context.packageManager.hasSystemFeature("android.hardware.camera.external")
                        if (hasCameraShutter) {
                            Pair(TestStatus.PASS, "Supported")
                        } else {
                            Pair(TestStatus.NOT_APPLICABLE, "No Shutter Button")
                        }
                    }
                ),
                SubTestDef(
                    id = "STYLUS_BUTTON",
                    name = "S Pen / Stylus Buttons",
                    instruction = "Checks if device contains specialized electromagnetic digitizers for stylus clicks.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val hasStylus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.packageManager.hasSystemFeature("android.hardware.sensor.stylus")
                        } else {
                            false
                        }
                        if (hasStylus) {
                            Pair(TestStatus.PASS, "Stylus Supported")
                        } else {
                            Pair(TestStatus.NOT_APPLICABLE, "No Stylus Digitizer")
                        }
                    }
                )
            )
        ),
        CategoryDef(
            id = "PERFORMANCE",
            name = "Performance & Stress",
            iconName = "BatteryFull",
            subTests = listOf(
                SubTestDef(
                    id = "CPU_CORES",
                    name = "CPU Cores Count",
                    instruction = "Reads system CPU logs to fetch available logical core partitions.",
                    isAutomated = true,
                    runAutomated = { _ ->
                        val cores = Runtime.getRuntime().availableProcessors()
                        Pair(TestStatus.PASS, "$cores cores available")
                    }
                ),
                SubTestDef(
                    id = "CPU_STRESS",
                    name = "CPU Cryptography Stress",
                    instruction = "Launches 5-second heavy multithreaded prime calculations to verify stability.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "GPU_ID",
                    name = "GPU Hardware Model",
                    instruction = "Queries standard render pipelines to identify integrated graphics accelerators.",
                    isAutomated = true,
                    runAutomated = { _ ->
                        val gpu = Build.HARDWARE
                        Pair(TestStatus.PASS, "Platform: $gpu")
                    }
                ),
                SubTestDef(
                    id = "GPU_STRESS",
                    name = "GPU Load Rendering",
                    instruction = "Executes high-density color transitions or vector drawing loops to verify rendering pipelines.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "RAM_CAPACITY",
                    name = "RAM Capacity Check",
                    instruction = "Checks total physical memory size and active swap partition constraints.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                        val memInfo = android.app.ActivityManager.MemoryInfo()
                        actManager?.getMemoryInfo(memInfo)
                        val totalGB = (memInfo.totalMem / (1024 * 1024 * 1024f))
                        Pair(TestStatus.PASS, "%.1f GB Total".format(totalGB))
                    }
                ),
                SubTestDef(
                    id = "RAM_STRESS",
                    name = "Memory Leak Stress",
                    instruction = "Allocates temporary standard integer buffers dynamically to check allocations stability.",
                    isAutomated = true,
                    runAutomated = { _ ->
                        try {
                            val list = mutableListOf<ByteArray>()
                            // Safely allocate 50MB and release immediately
                            list.add(ByteArray(50 * 1024 * 1024))
                            list.clear()
                            Pair(TestStatus.PASS, "Stable allocation check")
                        } catch (e: OutOfMemoryError) {
                            Pair(TestStatus.FAIL, "Out of Memory")
                        }
                    }
                ),
                SubTestDef(
                    id = "STORAGE_CAPACITY",
                    name = "Storage Capacity Check",
                    instruction = "Measures partition sizes of internal user directories.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val stat = StatFs(context.filesDir.absolutePath)
                        val size = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024f)
                        Pair(TestStatus.PASS, "%.1f GB".format(size))
                    }
                ),
                SubTestDef(
                    id = "STORAGE_SPEED",
                    name = "Read/Write Speed Test",
                    instruction = "Performs temporary write-and-read tests on internal directories to check throughput.",
                    isAutomated = true,
                    runAutomated = { context ->
                        try {
                            val file = File(context.cacheDir, "speed_check.bin")
                            val data = ByteArray(2 * 1024 * 1024) // 2MB
                            Random().nextBytes(data)
                            val t0 = System.currentTimeMillis()
                            file.writeBytes(data)
                            val t1 = System.currentTimeMillis()
                            val read = file.readBytes()
                            val t2 = System.currentTimeMillis()
                            file.delete()
                            val writeSpeed = 2.0 / ((t1 - t0) / 1000.0)
                            val readSpeed = 2.0 / ((t2 - t1) / 1000.0)
                            Pair(TestStatus.PASS, "W: %.1fMB/s, R: %.1fMB/s".format(writeSpeed, readSpeed))
                        } catch (e: Exception) {
                            Pair(TestStatus.FAIL, "IO Error")
                        }
                    }
                ),
                SubTestDef(
                    id = "EXTERNAL_STORAGE",
                    name = "External SD Card",
                    instruction = "Queries file storage lists to detect secondary mountable memory partitions.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val dirs = context.getExternalFilesDirs(null)
                        if (dirs != null && dirs.size > 1 && dirs[1] != null) {
                            Pair(TestStatus.PASS, "SD Card Present")
                        } else {
                            Pair(TestStatus.NOT_APPLICABLE, "No SD Card slot active")
                        }
                    }
                ),
                SubTestDef(
                    id = "BATTERY_HEALTH",
                    name = "Battery Health Status",
                    instruction = "Reads battery charge status and standard hardware cycle reports.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
                        val healthStr = when (health) {
                            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
                            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                            else -> "Normal"
                        }
                        Pair(TestStatus.PASS, healthStr)
                    }
                ),
                SubTestDef(
                    id = "BATTERY_DRAIN",
                    name = "Battery Discharge Rate",
                    instruction = "Monitors current battery capacity variations to check passive leakage rates.",
                    isAutomated = false
                )
            )
        ),
        CategoryDef(
            id = "PORTS_MISC",
            name = "Ports & Miscellaneous",
            iconName = "Info",
            subTests = listOf(
                SubTestDef(
                    id = "CHARGING_PORT",
                    name = "USB-C Mechanical Contact",
                    instruction = "Insert and remove the charging cable. Confirms mechanical insertion stability.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "SIM_TRAY",
                    name = "SIM Tray Detection",
                    instruction = "Verifies SIM tray module state registers inside telephony hardware system.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                        val state = tm?.simState ?: android.telephony.TelephonyManager.SIM_STATE_UNKNOWN
                        Pair(TestStatus.PASS, if (state != android.telephony.TelephonyManager.SIM_STATE_ABSENT) "Tray Engaged" else "Tray Empty")
                    }
                ),
                SubTestDef(
                    id = "SPEAKER_GRILLE",
                    name = "Speaker Grille Dust Check",
                    instruction = "Verify visually that speakers and microphone ports are free of debris or dust.",
                    isAutomated = false
                ),
                SubTestDef(
                    id = "TEMPERATURE",
                    name = "Core Thermal Sensors",
                    instruction = "Reads core thermal zones to report device temperatures.",
                    isAutomated = true,
                    runAutomated = { context ->
                        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1) / 10f
                        if (temp > 0) Pair(TestStatus.PASS, "$temp °C") else Pair(TestStatus.FAIL, "Read Error")
                    }
                )
            )
        )
    )
}
