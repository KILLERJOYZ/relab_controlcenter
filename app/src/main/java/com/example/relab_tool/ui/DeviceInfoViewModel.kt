package com.example.relab_tool.ui

import android.app.ActivityManager
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.display.DisplayManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.MediaDrm
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.*
import android.provider.Settings
import android.system.Os
import android.system.OsConstants
import android.util.DisplayMetrics
import android.util.Size
import android.view.Choreographer
import android.view.Display
import android.view.MotionEvent
import android.view.WindowManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.GnssStatus
import android.location.OnNmeaMessageListener
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.relab_tool.R
import com.example.relab_tool.data.BatteryHistoryRepository
import com.example.relab_tool.utils.GpuUtils
import com.example.relab_tool.model.*
import java.text.SimpleDateFormat
import com.example.relab_tool.utils.SoCUtils
import com.example.relab_tool.utils.spec.SpecLoader
import android.app.admin.DevicePolicyManager // Changed
import android.content.pm.ApplicationInfo // Changed
import android.content.pm.PackageInfo
import android.security.NetworkSecurityPolicy // Changed
import android.security.keystore.KeyGenParameterSpec // Changed
import android.security.keystore.KeyProperties // Changed
import android.security.keystore.KeyInfo // Changed
import java.security.KeyPairGenerator // Changed
import java.security.KeyStore // Changed
import java.security.KeyFactory // Changed
import java.io.File
import java.io.RandomAccessFile
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.relab_tool.data.SearchEngine

class DeviceInfoViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        /** Pre-compiled regex for splitting whitespace in /proc/stat etc. — avoids per-tick Regex allocation. */
        private val WHITESPACE_REGEX = Regex("\\s+")
        /**
         * Number of history samples to keep for sparkline charts.
         * 30 is enough for visual context and halves Canvas draw cost vs. 60
         * on ultra-low-end devices (Snapdragon 2xx / Helio G25).
         */
        private const val HISTORY_SIZE = 30
    }

    /** Replaces raw "Unknown" fallback values from utility classes with the locale-aware translation. */
    private fun String.translateUnknown(): String =
        if (this == "Unknown") getApplication<Application>().getString(R.string.unknown) else this


    private val wifiInfoProvider = com.example.relab_tool.data.WifiInfoProvider(application)

    private val _deviceSummary = MutableStateFlow<DeviceSummary?>(null)
    val deviceSummary = _deviceSummary.asStateFlow()

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo = _systemInfo.asStateFlow()

    private val _cpuInfo = MutableStateFlow<CpuInfo?>(null)
    val cpuInfo = _cpuInfo.asStateFlow()

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo = _batteryInfo.asStateFlow()

    private val _displayInfo = MutableStateFlow<DisplayInfo?>(null)
    val displayInfo = _displayInfo.asStateFlow()

    private val _memoryInfo = MutableStateFlow<MemoryInfo?>(null)
    val memoryInfo = _memoryInfo.asStateFlow()

    private val _sensors = MutableStateFlow<List<SensorInfo>>(emptyList())
    val sensors = _sensors.asStateFlow()

    private val _socInfo = MutableStateFlow<SocInfo?>(null)
    val socInfo = _socInfo.asStateFlow()

    private val _cameras = MutableStateFlow<List<CameraInfo>>(emptyList())
    val cameras = _cameras.asStateFlow()

    private val _bluetoothInfo = MutableStateFlow<BluetoothInfo?>(null)
    val bluetoothInfo = _bluetoothInfo.asStateFlow()

    private val _usbDevices = MutableStateFlow<List<UsbInfo>>(emptyList())
    val usbDevices = _usbDevices.asStateFlow()

    private val _usbStatus = MutableStateFlow(UsbStatusInfo())
    val usbStatus = _usbStatus.asStateFlow()

    private var usbConnectionTime: Long = 0

    private val _codecs = MutableStateFlow<List<CodecInfo>>(emptyList())
    val codecs = _codecs.asStateFlow()

    private val _audioInfo = MutableStateFlow<AudioInfo?>(null) // Changed
    val audioInfo = _audioInfo.asStateFlow() // Changed
    private val _securityInfo = MutableStateFlow<SecurityInfo?>(null) // Changed
    val securityInfo = _securityInfo.asStateFlow() // Changed

    data class PermissionAuditData(
        val appsCameraCount: Int,
        val appsMicCount: Int,
        val appsLocationCount: Int,
        val appsContactsSmsCount: Int,
        val overlayAppsCount: Int,
        val unknownSourcesCount: Int,
        val nonPlayStoreCount: Int,
        val accessibilityApps: List<String>,
        val deviceAdminApps: List<String>
    )

    @Volatile private var cachedPermissionAudit: PermissionAuditData? = null
    @Volatile private var cachedKeystoreType: String? = null
    @Volatile private var cachedHardwareBackedKeystore: Boolean? = null
    @Volatile private var cachedStorageFsType: String? = null
    @Volatile private var cachedAudioCodecs: String? = null
    @Volatile private var cachedVideoCodecs: String? = null

    // Thread-safe set that records which tab keys have already been loaded.
    private val loadedTabs = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private val _locationData = MutableStateFlow(LocationData())
    val locationData = _locationData.asStateFlow()

    private val _satellitesList = MutableStateFlow<List<GnssSatellite>>(emptyList())
    val satellitesList = _satellitesList.asStateFlow()

    private var isLocationTrackingActive = false
    private var locationListener: LocationListener? = null
    private var gnssStatusCallback: GnssStatus.Callback? = null
    private var nmeaMessageListener: OnNmeaMessageListener? = null

    private val _thermalZones = MutableStateFlow<List<ThermalInfo>>(emptyList())
    val thermalZones = _thermalZones.asStateFlow()

    private val _drmInfos = MutableStateFlow<List<DrmSchemeInfo>>(emptyList())
    val drmInfos = _drmInfos.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val _networkInfo = MutableStateFlow<NetworkInfo?>(null)
    val networkInfo = _networkInfo.asStateFlow()

    private val _touchSamplingRate = MutableStateFlow(0)
    val touchSamplingRate = _touchSamplingRate.asStateFlow()

    private val _touchSamplingRatePeak = MutableStateFlow(0)
    val touchSamplingRatePeak = _touchSamplingRatePeak.asStateFlow()

    private val touchTimestamps = java.util.ArrayDeque<Long>()
    private var lastUiUpdateTimestamp = 0L

    fun processTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchTimestamps.clear()
                _touchSamplingRate.value = 0
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    touchTimestamps.addLast(event.getHistoricalEventTime(i))
                }
                touchTimestamps.addLast(event.eventTime)
                
                val newest = touchTimestamps.peekLast() ?: return
                while (touchTimestamps.isNotEmpty() && newest - touchTimestamps.peekFirst()!! > 1000) {
                    touchTimestamps.removeFirst()
                }
                
                val currentHz = touchTimestamps.size
                if (currentHz > _touchSamplingRatePeak.value) {
                    _touchSamplingRatePeak.update { currentHz }
                }
                
                val currentTime = SystemClock.uptimeMillis()
                if (currentTime - lastUiUpdateTimestamp > 50) {
                    _touchSamplingRate.update { currentHz }
                    lastUiUpdateTimestamp = currentTime
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchTimestamps.clear()
                _touchSamplingRate.value = 0
            }
        }
    }

    // ── Split Dashboard Flows ───────────────────────────────────────────────
    // Fast-changing data (CPU/GPU/RAM/core frequencies/network) — emitted every tick
    private val _dashboardRealtime = MutableStateFlow<DashboardRealtimeData?>(null)
    val dashboardRealtime = _dashboardRealtime.asStateFlow()

    // Slow-changing data (storage, battery, WiFi, BT, features) — emitted every 5s
    private val _dashboardSlow = MutableStateFlow<DashboardSlowData?>(null)
    val dashboardSlow = _dashboardSlow.asStateFlow()

    // ── Ring Buffer Histories (zero allocation per tick) ──────────────────
    // Fixed-capacity ArrayDeque avoids takeLast().toMutableList() allocation storm
    private val ramHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val cpuHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val gpuHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val batteryHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val wattageHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val coreHistoryBuffers = mutableListOf<ArrayDeque<Float>>()
    private val cpuTempHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    @Volatile private var cpuTempHistorySnapshot: List<Float> = emptyList()
    private val gpuFreqHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val gpuMemoryHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val gpuTempHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val fpsHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val downloadSpeedHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    private val uploadSpeedHistoryBuffer = ArrayDeque<Float>(HISTORY_SIZE)
    @Volatile private var gpuFreqHistorySnapshot: List<Float> = emptyList()
    @Volatile private var gpuMemoryHistorySnapshot: List<Float> = emptyList()
    @Volatile private var gpuTempHistorySnapshot: List<Float> = emptyList()
    @Volatile private var fpsHistorySnapshot: List<Float> = emptyList()
    @Volatile private var downloadSpeedHistorySnapshot: List<Float> = emptyList()
    @Volatile private var uploadSpeedHistorySnapshot: List<Float> = emptyList()

    // Backward-compat bridge: BatteryTab still reads wattageHistory from here
    @Suppress("DEPRECATION")
    val dashboardData = _dashboardSlow.asStateFlow()

    private val _fullWattageHistory = MutableStateFlow<List<Float>>(emptyList())
    val fullWattageHistory = _fullWattageHistory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val searchEngine = SearchEngine(application)
    private var bluetoothA2dp: android.bluetooth.BluetoothA2dp? = null

    init {
        initSearchPipeline()
        val bluetoothAdapter = (application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (bluetoothAdapter != null) {
            try {
                bluetoothAdapter.getProfileProxy(application, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.A2DP) {
                            bluetoothA2dp = proxy as? android.bluetooth.BluetoothA2dp
                        }
                    }
                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.A2DP) {
                            bluetoothA2dp = null
                        }
                    }
                }, BluetoothProfile.A2DP)
            } catch (e: Exception) {}
        }
    }

    private val _requestedInfoTab = MutableStateFlow<Int?>(null)
    val requestedInfoTab = _requestedInfoTab.asStateFlow()
    fun requestInfoTab(tabIndex: Int) { _requestedInfoTab.value = tabIndex }
    fun clearRequestedInfoTab()       { _requestedInfoTab.value = null }

    private val _isSatelliteCompassActive = MutableStateFlow(false)
    val isSatelliteCompassActive = _isSatelliteCompassActive.asStateFlow()
    fun setSatelliteCompassActive(active: Boolean) { _isSatelliteCompassActive.value = active }

    fun updateBluetoothInfo() {
        val context = getApplication<Application>()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter == null) {
            _bluetoothInfo.value = BluetoothInfo(featureGroups = emptyList())
            return
        }

        try {
            fun safeBool(block: () -> Boolean): Boolean = try { block() } catch (_: Throwable) { false }

            val b4Features = listOf(
                BluetoothFeature(R.string.bt_feature_le, context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)),
                BluetoothFeature(R.string.bt_feature_advertising, safeBool { adapter.isMultipleAdvertisementSupported }),
                BluetoothFeature(R.string.bt_feature_filtering, safeBool { adapter.isOffloadedFilteringSupported }),
                BluetoothFeature(R.string.bt_feature_batch_scan, safeBool { adapter.isOffloadedScanBatchingSupported })
            )

            val b5Features = mutableListOf(
                BluetoothFeature(R.string.bt_feature_periodic_adv, safeBool { adapter.isLePeriodicAdvertisingSupported }),
                BluetoothFeature(R.string.bt_feature_extended_adv, safeBool { adapter.isLeExtendedAdvertisingSupported }),
                BluetoothFeature(R.string.bt_feature_2m_phy, safeBool { adapter.isLe2MPhySupported }),
                BluetoothFeature(R.string.bt_feature_coded_phy, safeBool { adapter.isLeCodedPhySupported })
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                b5Features.add(BluetoothFeature(R.string.bt_feature_audio, safeBool { adapter.isLeAudioSupported == 0 }))
            }

            val stateStr = try { if (adapter.isEnabled) context.getString(R.string.enabled) else context.getString(R.string.disabled) } catch (_: Throwable) { getApplication<Application>().getString(R.string.unknown) }
            val versionStr = getBluetoothVersion()

            val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val addressStr = if (hasConnectPermission) {
                try { adapter.address ?: "02:00:00:00:00:00" } catch (_: Throwable) { "02:00:00:00:00:00" }
            } else {
                "02:00:00:00:00:00"
            }

            val nameStr = if (hasConnectPermission) {
                try { adapter.name ?: getApplication<Application>().getString(R.string.unknown) } catch (_: Throwable) { getApplication<Application>().getString(R.string.unknown) }
            } else {
                "Unknown"
            }

            val pairedDevicesCountVal = if (hasConnectPermission) {
                try { adapter.bondedDevices.size } catch (_: Throwable) { 0 }
            } else {
                0
            }

            val connectedDevicesCountVal = getBluetoothConnectedDevicesCount()

            _bluetoothInfo.value = BluetoothInfo(
                featureGroups = listOf(
                    BluetoothFeatureGroup(R.string.bt_group_v4, b4Features),
                    BluetoothFeatureGroup(R.string.bt_group_v5, b5Features)
                ),
                state = stateStr,
                version = versionStr,
                name = nameStr,
                address = addressStr,
                pairedDevicesCount = pairedDevicesCountVal,
                connectedDevicesCount = connectedDevicesCountVal
            )
        } catch (_: Throwable) {
            // OPPO ColorOS may throw on any Bluetooth adapter access
            _bluetoothInfo.value = BluetoothInfo(featureGroups = emptyList())
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    @OptIn(FlowPreview::class)
    private fun initSearchPipeline() {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query -> performSearch(query) }
            .launchIn(viewModelScope)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }

            // Ensure all lazy sections are loaded so search is complete
            if (loadedTabs.size < 8) {
                loadAdvancedInfoSuspending()
            }

            val index = searchEngine.buildIndex(
                summary = _deviceSummary.value,
                system = _systemInfo.value,
                cpu = _cpuInfo.value,
                battery = _batteryInfo.value,
                display = _displayInfo.value,
                memory = _memoryInfo.value,
                sensors = _sensors.value,
                soc = _socInfo.value,
                cameras = _cameras.value,
                bluetooth = _bluetoothInfo.value,
                network = _networkInfo.value,
                cellular = _networkInfo.value?.cellularInfo,
                audio = _audioInfo.value,
                security = _securityInfo.value,
                usb = _usbStatus.value,
                drm = _drmInfos.value,
                thermals = _thermalZones.value,
                codecs = _codecs.value
            )

            kotlinx.coroutines.withContext(Dispatchers.Default) {
                _searchResults.value = searchEngine.search(query, index)
            }
        }
    }

    private val batteryRepository = BatteryHistoryRepository(application)
    private val _batteryCapacityHistory = MutableStateFlow<List<Triple<Long, Int, Boolean>>>(emptyList())
    val batteryCapacityHistory = _batteryCapacityHistory.asStateFlow()

    private val _lastFullChargeTs = MutableStateFlow(0L)
    val lastFullChargeTs = _lastFullChargeTs.asStateFlow()

    private val _lastStoppedChargingTs = MutableStateFlow(0L)
    val lastStoppedChargingTs = _lastStoppedChargingTs.asStateFlow()

    private var lastCpuTime = 0L
    private var lastIdleTime = 0L
    private val cpuCoreMinMax = mutableMapOf<Int, Pair<Long, Long>>()
    
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastNetUpdateTime = 0L
    private var widevineLevelCached: String = "N/A"
    private var lastDiskReadBytes = 0L
    private var lastDiskWriteBytes = 0L
    private var lastDiskTime = 0L
    private var currentDiskReadSpeed = 0L
    private var currentDiskWriteSpeed = 0L
    
    private var ambientLightLux = 0f
    private var pressureHpa = 0f

    private val environmentListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_LIGHT -> ambientLightLux = event.values[0]
                Sensor.TYPE_PRESSURE -> pressureHpa = event.values[0]
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private var cachedStorageTotal = 0L
    private var cachedStorageUsed = 0L
    private var cachedSensorsCount = 0
    private var cachedAppsCount = 0
    private var cachedScreenResolution = ""
    private var cachedPhysicalSize = ""
    private var staticFeaturesLoaded = false
    private var cachedStorageSmartStatus = com.example.relab_tool.R.string.unknown
    
    // Slow-changing value caches (updated in slow loop every 5s, not every realtime tick)
    @Volatile private var cachedBluetoothConnectedDevices = 0
    @Volatile private var cachedBluetoothCodec: String? = null

    // Feature Cache
    private var featureUsbStorage = false
    private var featureUsbAccessory = false
    private var featureIrisScanner = false
    private var featureFaceRecognition = false
    private var featureInfrared = false
    private var featureUwb = false
    private var featureNfc = false
    private var featureSecureNfc = false
    private var featureGps = false

    // Cached slow utility values (updated every 5s instead of every tick)
    @Volatile private var cachedUptime = ""
    @Volatile private var cachedBatteryHealthPct = 0
    @Volatile private var cachedBatteryCycleCount = 0
    @Volatile private var cachedDeepSleepRatio = 0f
    @Volatile private var cachedIsRooted = false
    @Volatile private var cachedSelinuxStatus = "Unknown"
    @Volatile private var cachedSecurityPatch = ""
    // CPU frequency readers (reusable to avoid per-tick File object allocation)
    private var cpuFreqReaders: Array<RandomAccessFile?> = emptyArray()
    private var procStatReader: RandomAccessFile? = null

    // ── Cached RAM total (never changes at runtime) ──────────────────────
    private var cachedRamTotal = 0L

    // ── Snapshotted history lists ─────────────────────────────────────────
    // Instead of calling .toList() every tick (creates new List objects),
    // we keep stable List snapshots that are only regenerated when the
    // underlying ring buffer actually changes.
    @Volatile private var ramHistorySnapshot: List<Float> = emptyList()
    @Volatile private var cpuHistorySnapshot: List<Float> = emptyList()
    @Volatile private var gpuHistorySnapshot: List<Float> = emptyList()
    @Volatile private var coreHistorySnapshot: List<List<Float>> = emptyList()
    @Volatile private var clusterHistorySnapshot: List<List<Float>> = emptyList()
    @Volatile private var batteryHistorySnapshot: List<Float> = emptyList()
    @Volatile private var wattageHistorySnapshot: List<Float> = emptyList()

    /** Appends a value to a ring buffer, evicting the oldest if at capacity. */
    private fun ArrayDeque<Float>.addCapped(value: Float) {
        if (size >= HISTORY_SIZE) removeFirst()
        addLast(value)
    }

    /**
     * Returns the existing [current] snapshot if the buffer hasn't visibly changed,
     * otherwise allocates a new List via toList().
     * "Changed" = different size OR different newest sample.
     * This eliminates ~12 List allocations per tick when values are stable.
     */
    private fun snapshotIfChanged(buffer: ArrayDeque<Float>, current: List<Float>): List<Float> {
        if (buffer.size != current.size) return buffer.toList()
        if (buffer.isEmpty()) return current
        if (buffer.last() != current.lastOrNull()) return buffer.toList()
        return current  // reference reuse — no allocation
    }

    // Cached CPU core frequency list — avoids LongArray.toList() every tick
    @Volatile private var cpuFreqsSnapshot: List<Long> = emptyList()
    // Cached last-emitted realtime data — skip emission when structurally identical
    private var lastEmittedRealtime: DashboardRealtimeData? = null
    // Reference guard for cluster history recomputation
    @Volatile private var lastCoreSnapshotRef: List<List<Float>>? = null

    // Battery Estimation Enhancements
    private var smoothedCurrentMa = 0.0
    private var batteryDataPoints = 0
    private var lastEstimationValue = -1L
    private var lastEstimationUpdateTs = 0L
    private val levelChangeHistory = mutableListOf<Pair<Long, Int>>()
    private var lastLevelRecorded = -1
    private var lastManualLevel = -1
    @Volatile private var lastVoltageMv = 0

    private var fpsCount = 0
    private var lastFpsUpdateTime = 0L
    @Volatile private var currentFpsValue = 0f
    @Volatile private var lastFrameTimeNanos = 0L
    @Volatile private var isAppInForeground = false
    @Volatile private var isFpsTrackingActive = false

    fun setAppInForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (inForeground) {
            startFpsTracking()
        } else {
            stopFpsTracking()
        }
    }

    fun startFpsTracking() {
        if (!isFpsTrackingActive) {
            isFpsTrackingActive = true
            fpsCount = 0
            lastFpsUpdateTime = System.currentTimeMillis()
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stopFpsTracking() {
        if (isFpsTrackingActive) {
            isFpsTrackingActive = false
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isFpsTrackingActive) return
            fpsCount++
            lastFrameTimeNanos = frameTimeNanos
            val now = System.currentTimeMillis()
            if (lastFpsUpdateTime == 0L) lastFpsUpdateTime = now
            val elapsed = now - lastFpsUpdateTime
            
            if (elapsed >= 500) {
                currentFpsValue = (fpsCount * 1000f) / elapsed
                fpsCount = 0
                lastFpsUpdateTime = now
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModelScope.launch(Dispatchers.IO) {
                updateBatteryInfo(intent)
            }
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED,
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    loadAdvancedInfo() // Refresh OTG devices
                }
                "android.hardware.usb.action.USB_STATE" -> {
                    val connected = intent.getBooleanExtra("connected", false)
                    if (connected && usbConnectionTime == 0L) {
                        usbConnectionTime = SystemClock.elapsedRealtime()
                    } else if (!connected) {
                        usbConnectionTime = 0L
                    }
                    updateUsbStatus(intent)
                }
            }
        }
    }

    private val _isPlayStoreAvailable = MutableStateFlow(false)
    val isPlayStoreAvailable = _isPlayStoreAvailable.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadDrmInfo()
        }
        // Battery history — standalone, no downstream dependency
        viewModelScope.launch(Dispatchers.IO) {
            val batteryHistory = batteryRepository.getBatteryHistory()
            val fullChargeTs = batteryRepository.getLastFullChargeTs()
            val stoppedChargingTs = batteryRepository.getLastStoppedChargingTs()
            // MutableStateFlow.value= is thread-safe; no need for Main dispatcher
            _batteryCapacityHistory.value = batteryHistory
            _lastFullChargeTs.value = fullChargeTs
            _lastStoppedChargingTs.value = stoppedChargingTs
        }
        // Parallelise: each loader is independent — no data dependency between them
        viewModelScope.launch(Dispatchers.IO) { loadStaticInfo() }
        
        // Register battery receiver for state changes
        ContextCompat.registerReceiver(
            getApplication<Application>(),
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Register USB receiver
        val usbFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction("android.hardware.usb.action.USB_STATE")
        }
        ContextCompat.registerReceiver(
            getApplication<Application>(),
            usbReceiver,
            usbFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        // Register environment sensors
        val sensorManager = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sensorManager.registerListener(environmentListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sensorManager.registerListener(environmentListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Pre-emit one initial tick so DashboardTab has data on the very first frame
            updateRealtimeDashboardInfo()
            updateBatteryRealtime()
            updateDisplayRealtime()

            var tick = 0
            while (true) {
                val isOverlayActive = try {
                    com.example.relab_tool.worker.PerformanceOverlayService.isServiceRunning
                } catch (e: Exception) { false }

                if (isAppInForeground || isOverlayActive) {
                    updateRealtimeDashboardInfo()
                    // Update battery wattage and time remaining more frequently than broadcast
                    updateBatteryRealtime()
                    updateDisplayRealtime()
                    if (tick % 3 == 0) {
                        updateThermalInfo()
                        updateNetworkSignalRealtime()
                    }
                    tick++
                    // Poll interval scales with active performance profile:
                    // Gaming=500ms (faster updates), Default=1000ms, Saver=2000ms
                    val pollMs = com.example.relab_tool.data.TelemetryManager.activeProfile.value.pollIntervalMs
                    delay(pollMs)
                } else {
                    delay(2000)
                }
            }
        }

        // Slow loop for stats that don't change often (storage, counts)
        viewModelScope.launch(Dispatchers.IO) {
            // Pre-emit one slow tick so DashboardTab gets slowState != null immediately
            updateSlowDashboardInfo()
            emitSlowDashboard()

            while (true) {
                val isOverlayActive = try {
                    com.example.relab_tool.worker.PerformanceOverlayService.isServiceRunning
                } catch (e: Exception) { false }

                if (isAppInForeground || isOverlayActive) {
                    updateSlowDashboardInfo()
                    updateDynamicInfo()
                    if (loadedTabs.contains("bluetooth")) {
                        updateBluetoothInfo()
                    }
                    // Refresh cached BT values used by the fast dashboard loop
                    cachedBluetoothConnectedDevices = getBluetoothConnectedDevicesCount()
                    cachedBluetoothCodec = getBluetoothAudioCodec()

                    // ── Emit slow dashboard flow (every 5s, NOT every tick) ───
                    emitSlowDashboard()

                    // Sync battery history updated in background
                    val batteryHistory = batteryRepository.getBatteryHistory()
                    val fullChargeTs = batteryRepository.getLastFullChargeTs()
                    val stoppedChargingTs = batteryRepository.getLastStoppedChargingTs()
                    // MutableStateFlow.value= is thread-safe; no need for Main dispatcher
                    _batteryCapacityHistory.value = batteryHistory
                    _lastFullChargeTs.value = fullChargeTs
                    _lastStoppedChargingTs.value = stoppedChargingTs
                    delay(5000)
                } else {
                    delay(5000)
                }
            }
        }

        // Battery History Tracker (Every 5 minutes)
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                recordBatteryPoint()
                delay(5 * 60 * 1000)
            }
        }

        startDiskMonitoring()
    }

    private fun startDiskMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                updateDiskIoSpeed()
                delay(1000)
            }
        }
    }

    private fun updateDiskIoSpeed() {
        try {
            val file = File("/proc/diskstats")
            if (file.exists()) {
                val lines = file.readLines()
                var totalReadSectors = 0L
                var totalWriteSectors = 0L
                for (line in lines) {
                    val parts = line.trim().split(WHITESPACE_REGEX)
                    if (parts.size >= 14 && (parts[2].startsWith("mmcblk") || parts[2].startsWith("sd") || parts[2].startsWith("dm-"))) {
                        totalReadSectors += parts[5].toLong()
                        totalWriteSectors += parts[9].toLong()
                    }
                }
                
                val currentTime = SystemClock.elapsedRealtime()
                if (lastDiskTime > 0) {
                    val timeDiff = (currentTime - lastDiskTime) / 1000.0
                    if (timeDiff > 0) {
                        // Sector is usually 512 bytes
                        currentDiskReadSpeed = (((totalReadSectors - lastDiskReadBytes) * 512) / timeDiff).toLong()
                        currentDiskWriteSpeed = (((totalWriteSectors - lastDiskWriteBytes) * 512) / timeDiff).toLong()
                    }
                }
                lastDiskReadBytes = totalReadSectors
                lastDiskWriteBytes = totalWriteSectors
                lastDiskTime = currentTime
            }
        } catch (e: Exception) {}
    }

    private fun updateNetworkSignalRealtime() {
        updateNetworkInfo()
    }

    private fun isRooted(): Boolean {
        val paths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su")
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun getSelinuxStatus(): String {
        val cached = cachedSelinuxStatus
        if (cached != "Unknown" && cached.isNotEmpty()) {
            return cached
        }
        return try {
            val process = Runtime.getRuntime().exec("getenforce")
            val reader = process.inputStream.bufferedReader()
            val status = reader.readLine()
            reader.close()
            val finalStatus = status ?: getApplication<Application>().getString(R.string.unknown)
            cachedSelinuxStatus = finalStatus
            finalStatus
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getDeepSleepRatio(): Float {
        val uptime = SystemClock.elapsedRealtime().toFloat()
        val wakeupTime = SystemClock.uptimeMillis().toFloat()
        if (uptime == 0f) return 0f
        return (uptime - wakeupTime) / uptime
    }

    private fun recordBatteryPoint() {
        _batteryInfo.value?.let { info ->
            val level = info.level.toInt()
            batteryRepository.recordBatteryPoint(level, info.isCharging)
        }
    }

    private fun updateSlowDashboardInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            // Storage
            val internal = File(Environment.getDataDirectory().path)
            val stat = StatFs(internal.path)
            cachedStorageTotal = stat.blockSizeLong * stat.blockCountLong
            cachedStorageUsed = cachedStorageTotal - (stat.blockSizeLong * stat.availableBlocksLong)
            
            // Sensors count
            if (cachedSensorsCount == 0) {
                val sensorManager = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE) as SensorManager
                cachedSensorsCount = sensorManager.getSensorList(Sensor.TYPE_ALL).size
            }
            
            // Apps count
            if (cachedAppsCount == 0) {
                val pm = getApplication<Application>().packageManager
                cachedAppsCount = try {
                    pm.getInstalledApplications(0).size
                } catch (e: Exception) { 0 }
            }
            
            // Static display info if not loaded
            if (cachedScreenResolution.isEmpty()) {
                // WindowManager requires a visual (Activity) context and crashes from Application context.
                // DisplayManager works from any context and provides the same display info.
                val displayManager = getApplication<Application>().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                if (display != null) {
                    val metrics = DisplayMetrics()
                    display.getRealMetrics(metrics)
                    cachedScreenResolution = "${metrics.widthPixels} x ${metrics.heightPixels}"
                    cachedPhysicalSize = getPhysicalSize(metrics)
                }
            }

            // ── Cache slow utility values (moved from every-tick to every-5s) ──
            cachedUptime = getUptime()
            cachedBatteryHealthPct = getBatteryHealthPct()
            cachedBatteryCycleCount = getBatteryCycleCount()
            cachedDeepSleepRatio = getDeepSleepRatio()
            cachedIsRooted = try {
                File("/system/app/Superuser.apk").exists() ||
                File("/sbin/su").exists() ||
                File("/system/bin/su").exists() ||
                File("/system/xbin/su").exists()
            } catch (_: Exception) { false }
            val currentSelinux = cachedSelinuxStatus
            if (currentSelinux == "Unknown" || currentSelinux.isEmpty()) {
                cachedSelinuxStatus = try {
                    val proc = Runtime.getRuntime().exec("getenforce")
                    proc.inputStream.bufferedReader().readLine()?.trim() ?: "Unknown"
                } catch (_: Exception) { "Unknown" }
            }
            cachedSecurityPatch = Build.VERSION.SECURITY_PATCH
        }
    }

    /**
     * Emits the slow dashboard flow. Called from the slow loop only (every 5s),
     * NOT from the fast tick loop. This eliminates the #1 cause of status card
     * recomposition storms — previously, _dashboardSlow was rebuilt every tick
     * even though 90% of its fields were identical.
     */
    private fun emitSlowDashboard() {
        val context = getApplication<Application>()
        val bInfo = _batteryInfo.value
        val nowNanos = System.nanoTime()
        val nanosSinceLastFrame = nowNanos - lastFrameTimeNanos
        val finalFps = if (nanosSinceLastFrame > 500_000_000L) {
            if (nanosSinceLastFrame > 1_000_000_000L) 1f else 10f
        } else {
            currentFpsValue
        }
        val refreshStr = context.getString(R.string.unit_hz_short, finalFps)

        _dashboardSlow.value = DashboardSlowData(
            storageTotal = cachedStorageTotal,
            storageUsed = cachedStorageUsed,
            storageSmartStatus = cachedStorageSmartStatus,
            batteryLevel = bInfo?.level ?: 0,
            isCharging = bInfo?.isCharging ?: false,
            batteryWattage = bInfo?.wattage ?: context.getString(R.string.unit_w, 0f),
            batteryHistory = batteryHistorySnapshot,
            wattageHistory = wattageHistorySnapshot,
            batteryTemp = bInfo?.temperature ?: context.getString(R.string.na),
            sensorsCount = cachedSensorsCount,
            appsCount = cachedAppsCount,
            screenResolution = cachedScreenResolution,
            screenInfo = "$cachedPhysicalSize | $refreshStr",
            widevineLevel = widevineLevelCached,
            wifiSsid = _networkInfo.value?.wifiSsid,
            wifiSignalDbm = _networkInfo.value?.wifiSignalDbm,
            wifiStandard = _networkInfo.value?.wifiStandard,
            simInfos = _networkInfo.value?.cellularInfo?.simInfos ?: emptyList(),
            bluetoothConnectedDevices = cachedBluetoothConnectedDevices,
            bluetoothCodec = cachedBluetoothCodec,
            colorDepth = _displayInfo.value?.colorDepth ?: context.getString(R.string.unit_bit_suffix, "8"),
            colorDepthSubtext = _displayInfo.value?.colorDepthSubtext ?: context.getString(R.string.tag_standard_rgb),
            usbStorage = featureUsbStorage,
            usbAccessory = featureUsbAccessory,
            irisScanner = featureIrisScanner,
            faceRecognition = featureFaceRecognition,
            infrared = featureInfrared,
            uwb = featureUwb,
            nfc = featureNfc,
            secureNfc = featureSecureNfc,
            gps = featureGps,
            thermalStatus = getThermalStatusLabel(),
            chargingCurrentMa = run {
                val rawCurrent = getBatteryCurrentMa()
                if (rawCurrent != Long.MAX_VALUE) {
                    if (bInfo?.isCharging == true) Math.abs(rawCurrent.toInt()) else -Math.abs(rawCurrent.toInt())
                } else 0
            },
            chargingVoltageV = lastVoltageMv / 1000f,
            batteryHealthPct = cachedBatteryHealthPct,
            batteryCycleCount = cachedBatteryCycleCount,
            deepSleepRatio = cachedDeepSleepRatio,
            uptime = cachedUptime,
            isRooted = cachedIsRooted,
            selinuxStatus = cachedSelinuxStatus,
            securityPatch = cachedSecurityPatch
        )
    }

    private fun updateRealtimeDashboardInfo() {
        val context = getApplication<Application>()
        
        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val ramUsed = memoryInfo.totalMem - memoryInfo.availMem

        // GPU Usage
        val gpuUsage = GpuUtils.getGpuUsage()

        // Network Speed
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()
        var dlSpeed = "0 B/s"
        var ulSpeed = "0 B/s"
        var dlSpeedRaw = 0f
        var ulSpeedRaw = 0f
        if (lastNetUpdateTime > 0) {
            val timeDiff = (currentTime - lastNetUpdateTime) / 1000.0
            if (timeDiff > 0) {
                val dlBytesSec = ((currentRx - lastRxBytes) / timeDiff).toLong()
                val ulBytesSec = ((currentTx - lastTxBytes) / timeDiff).toLong()
                dlSpeed = formatSize(dlBytesSec)
                ulSpeed = formatSize(ulBytesSec)
                dlSpeedRaw = dlBytesSec.toFloat()
                ulSpeedRaw = ulBytesSec.toFloat()
            }
        }
        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastNetUpdateTime = currentTime

        // Update Dashboard Info with the latest realtime values
        updateDashboardInfo(ramUsed, gpuUsage, dlSpeed, ulSpeed, dlSpeedRaw, ulSpeedRaw)
    }

    private fun getBluetoothConnectedDevicesCount(): Int {
        val context = getApplication<Application>()
        val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasConnectPermission) return 0

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return 0
        val adapter = bluetoothManager.adapter ?: return 0

        if (!adapter.isEnabled) return 0

        return try {
            val connectedAddresses = mutableSetOf<String>()

            // 1. Method: getConnectedDevices for GATT (reliable for many LE devices)
            try {
                bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).forEach {
                    connectedAddresses.add(it.address)
                }
            } catch (e: Exception) {}

            // 2. Method: Hidden isConnected() via reflection
            // This is the most reliable way to check connection state for bonded devices
            // (A2DP, HFP, HID) without needing asynchronous profile proxies.
            try {
                adapter.bondedDevices.forEach { device ->
                    try {
                        val isConnectedMethod = device.javaClass.getMethod("isConnected")
                        if (isConnectedMethod.invoke(device) as Boolean) {
                            connectedAddresses.add(device.address)
                        }
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {}

            // 3. Method: Fallback to profile check for common profiles
            // Even though docs say some return empty, OEMs sometimes implement them.
            val profiles = intArrayOf(
                BluetoothProfile.A2DP,
                BluetoothProfile.HEADSET,
                4, // HID_HOST (API 11)
                22 // LE_AUDIO (API 31)
            )
            for (profile in profiles) {
                try {
                    bluetoothManager.getConnectedDevices(profile).forEach {
                        connectedAddresses.add(it.address)
                    }
                } catch (e: Exception) {}
            }

            connectedAddresses.size
        } catch (e: Exception) {
            0
        }
    }

    private fun getCurrentCpuTemp(): Float {
        val zones = _thermalZones.value
        if (zones.isEmpty()) {
            val thermalDir = File("/sys/class/thermal/")
            if (thermalDir.exists()) {
                val zoneFiles = thermalDir.listFiles { _, name -> name.startsWith("thermal_zone") } ?: emptyArray()
                for (zone in zoneFiles) {
                    try {
                        val type = File(zone, "type").readText().trim().lowercase()
                        if (type.contains("cpu") || type.contains("soc") || type.contains("tsens") || type.contains("battery") || type.contains("batt") || type.contains("system")) {
                            val t = File(zone, "temp").readText().trim().toDouble()
                            val tempC = if (t > 1000) t / 1000.0 else t
                            if (tempC in 0.0..100.0) return tempC.toFloat()
                        }
                    } catch (_: Exception) {}
                }
            }
            return 0f
        }
        val cpuTempZone = zones.find { it.name.lowercase().contains("cpu") || it.name.lowercase().contains("soc") || it.name.lowercase().contains("tsens") }
            ?: zones.maxByOrNull { it.temperature.substringBefore(" ").toDoubleOrNull() ?: 0.0 }
        return cpuTempZone?.temperature?.substringBefore(" ")?.toFloatOrNull() ?: 0f
    }

    private fun parseMaxGpuFreq(freqStr: String): Float {
        val numbers = Regex("\\d+").findAll(freqStr).map { it.value.toFloat() }.toList()
        return if (numbers.isNotEmpty()) {
            numbers.maxOrNull() ?: 800f
        } else {
            800f
        }
    }

    private fun getCurrentGpuFreq(maxFreq: Float, gpuUsage: Int): Float {
        val paths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpuclk",
            "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
            "/sys/class/misc/mali0/device/clock"
        )
        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val freqHz = file.readText().trim().toLong()
                    val freqMhz = if (freqHz > 100_000_000L) freqHz / 1_000_000f else freqHz.toFloat()
                    if (freqMhz > 10f) return freqMhz
                }
            } catch (_: Exception) {}
        }
        return 0f
    }

    private fun getCurrentGpuMemory(gpuUsage: Int): Float {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val memoryInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memoryInfo)
                val graphicsStr = memoryInfo.getMemoryStat("summary.graphics")
                val graphicsKb = graphicsStr?.toLongOrNull() ?: 0L
                if (graphicsKb > 0L) {
                    return graphicsKb / 1024f
                }
            }
        } catch (_: Exception) {}
        return 0f
    }

    private fun getCurrentGpuTemp(cpuTemp: Float): Float {
        val zones = _thermalZones.value
        if (zones.isNotEmpty()) {
            val gpuZone = zones.find { it.name.lowercase().contains("gpu") }
            if (gpuZone != null) {
                val t = gpuZone.temperature.substringBefore(" ").toFloatOrNull()
                if (t != null && t in 0f..100f) return t
            }
        }
        val thermalDir = File("/sys/class/thermal/")
        if (thermalDir.exists()) {
            val zoneFiles = thermalDir.listFiles { _, name -> name.startsWith("thermal_zone") } ?: emptyArray()
            for (zone in zoneFiles) {
                try {
                    val type = File(zone, "type").readText().trim().lowercase()
                    if (type.contains("gpu")) {
                        val t = File(zone, "temp").readText().trim().toDouble()
                        val tempC = if (t > 1000) t / 1000.0 else t
                        if (tempC in 0.0..100.0) return tempC.toFloat()
                    }
                } catch (_: Exception) {}
            }
        }
        return 0f
    }

    private fun updateDashboardInfo(ramUsedVal: Long, gpuUsageVal: Int, dlSpeed: String, ulSpeed: String, dlSpeedRaw: Float, ulSpeedRaw: Float) {
        val context = getApplication<Application>()
        val coreCount = Runtime.getRuntime().availableProcessors()

        // ── CPU Frequencies — reuse RandomAccessFile handles ──────────────
        // Lazy-init the reader array once
        if (cpuFreqReaders.size != coreCount) {
            cpuFreqReaders.forEach { try { it?.close() } catch (_: Exception) {} }
            cpuFreqReaders = Array(coreCount) { i ->
                try { RandomAccessFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq", "r") } catch (_: Exception) { null }
            }
        }
        val cpuFrequencies = LongArray(coreCount)
        for (i in 0 until coreCount) {
            try {
                if (!cpuCoreMinMax.containsKey(i)) {
                    val maxFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                    val maxVal = if (maxFile.exists()) maxFile.readText().trim().toLong() / 1000 else 3000L
                    val minFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
                    val minVal = if (minFile.exists()) minFile.readText().trim().toLong() / 1000 else 300L
                    cpuCoreMinMax[i] = Pair(minVal, maxVal)
                }
                val reader = cpuFreqReaders[i]
                if (reader != null) {
                    reader.seek(0)
                    val line = reader.readLine()
                    cpuFrequencies[i] = if (line != null) line.trim().toLong() / 1000 else 0L
                }
            } catch (_: Exception) { cpuFrequencies[i] = 0L }
        }

        // ── CPU Load from /proc/stat — reuse RandomAccessFile ────────────
        var actualLoad = -1
        try {
            if (procStatReader == null) {
                procStatReader = RandomAccessFile("/proc/stat", "r")
            }
            val reader = procStatReader!!
            reader.seek(0)
            val line = reader.readLine()
            if (line != null) {
                val parts = line.split(WHITESPACE_REGEX)
                val user = parts[1].toLong()
                val nice = parts[2].toLong()
                val system = parts[3].toLong()
                val idle = parts[4].toLong()
                val iowait = parts[5].toLong()
                val irq = parts[6].toLong()
                val softirq = parts[7].toLong()

                val totalTime = user + nice + system + idle + iowait + irq + softirq
                val idleTime = idle + iowait

                if (lastCpuTime > 0) {
                    val totalDiff = totalTime - lastCpuTime
                    val idleDiff = idleTime - lastIdleTime
                    if (totalDiff > 0) {
                        actualLoad = ((totalDiff - idleDiff) * 100 / totalDiff).toInt()
                    }
                }
                lastCpuTime = totalTime
                lastIdleTime = idleTime
            }
        } catch (_: Exception) {}

        val cpuUsageVal = if (actualLoad >= 0) {
            actualLoad
        } else {
            if (coreCount > 0) {
                var totalNormalized = 0f
                for (i in 0 until coreCount) {
                    val freq = cpuFrequencies[i]
                    val max = cpuCoreMinMax[i]?.second ?: 3000L
                    val normalized = if (max > 0) (freq.toFloat() / max.toFloat() * 100f).coerceIn(0f, 100f) else 0f
                    totalNormalized += normalized
                }
                (totalNormalized / coreCount).toInt().coerceIn(0, 100)
            } else 0
        }
        // ── Cached RAM total (read once, never changes) ─────────────────────
        if (cachedRamTotal == 0L) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            cachedRamTotal = memoryInfo.totalMem
        }
        val ramTotal = cachedRamTotal

        // ── Ring Buffer History Updates + snapshot ─────────────────────────
        ramHistoryBuffer.addCapped(if (ramTotal > 0) (ramUsedVal.toFloat() / ramTotal.toFloat()) * 100f else 0f)
        cpuHistoryBuffer.addCapped(cpuUsageVal.toFloat())
        gpuHistoryBuffer.addCapped(gpuUsageVal.toFloat())
        downloadSpeedHistoryBuffer.addCapped(dlSpeedRaw)
        uploadSpeedHistoryBuffer.addCapped(ulSpeedRaw)

        val bInfo = _batteryInfo.value
        batteryHistoryBuffer.addCapped((bInfo?.level ?: 0).toFloat())
        val currentWattage = try { bInfo?.wattage?.substringBefore(" ")?.toFloat() ?: 0f } catch (_: Exception) { 0f }
        wattageHistoryBuffer.addCapped(currentWattage)

        val currentTemp = getCurrentCpuTemp()
        cpuTempHistoryBuffer.addCapped(currentTemp)

        // Calculate dynamic GPU clock/frequency
        val maxGpuClockStr = _socInfo.value?.gpuClockSpeed ?: ""
        val maxGpuFreqVal = parseMaxGpuFreq(maxGpuClockStr)
        val currentGpuFreqVal = getCurrentGpuFreq(maxGpuFreqVal, gpuUsageVal)
        gpuFreqHistoryBuffer.addCapped(currentGpuFreqVal)

        // Calculate dynamic GPU memory usage
        val currentGpuMemoryVal = getCurrentGpuMemory(gpuUsageVal)
        gpuMemoryHistoryBuffer.addCapped(currentGpuMemoryVal)

        // Calculate dynamic GPU temperature
        val currentGpuTemp = getCurrentGpuTemp(currentTemp)
        gpuTempHistoryBuffer.addCapped(currentGpuTemp)

        // Per-core history ring buffers
        while (coreHistoryBuffers.size < coreCount) {
            coreHistoryBuffers.add(ArrayDeque(HISTORY_SIZE))
        }
        for (i in 0 until coreCount) {
            val maxFreq = cpuCoreMinMax[i]?.second ?: 3000L
            coreHistoryBuffers[i].addCapped((cpuFrequencies[i].toFloat() / maxFreq.toFloat()) * 100f)
        }

        val nowNanos = System.nanoTime()
        val nanosSinceLastFrame = nowNanos - lastFrameTimeNanos
        val finalFps = if (nanosSinceLastFrame > 500_000_000L) {
            if (nanosSinceLastFrame > 1_000_000_000L) 1f else 10f
        } else {
            currentFpsValue
        }
        fpsHistoryBuffer.addCapped(finalFps)

        // ── Snapshot lists (reuse reference when buffer tail hasn't changed) ──
        ramHistorySnapshot = snapshotIfChanged(ramHistoryBuffer, ramHistorySnapshot)
        cpuHistorySnapshot = snapshotIfChanged(cpuHistoryBuffer, cpuHistorySnapshot)
        gpuHistorySnapshot = snapshotIfChanged(gpuHistoryBuffer, gpuHistorySnapshot)
        cpuTempHistorySnapshot = snapshotIfChanged(cpuTempHistoryBuffer, cpuTempHistorySnapshot)
        gpuFreqHistorySnapshot = snapshotIfChanged(gpuFreqHistoryBuffer, gpuFreqHistorySnapshot)
        gpuMemoryHistorySnapshot = snapshotIfChanged(gpuMemoryHistoryBuffer, gpuMemoryHistorySnapshot)
        gpuTempHistorySnapshot = snapshotIfChanged(gpuTempHistoryBuffer, gpuTempHistorySnapshot)
        fpsHistorySnapshot = snapshotIfChanged(fpsHistoryBuffer, fpsHistorySnapshot)
        downloadSpeedHistorySnapshot = snapshotIfChanged(downloadSpeedHistoryBuffer, downloadSpeedHistorySnapshot)
        uploadSpeedHistorySnapshot = snapshotIfChanged(uploadSpeedHistoryBuffer, uploadSpeedHistorySnapshot)

        // Per-core snapshots: only regenerate the outer list if any inner buffer changed
        val newCoreSnapshot = coreHistoryBuffers.map { snapshotIfChanged(it,
            coreHistorySnapshot.getOrElse(coreHistoryBuffers.indexOf(it)) { emptyList() }
        ) }
        if (newCoreSnapshot != coreHistorySnapshot) coreHistorySnapshot = newCoreSnapshot

        // ── Pre-compute cluster histories (only if core data changed) ────
        if (coreHistorySnapshot !== lastCoreSnapshotRef) {
            lastCoreSnapshotRef = coreHistorySnapshot
            val clusters = _socInfo.value?.cpuClusters ?: emptyList()
            clusterHistorySnapshot = if (clusters.isNotEmpty()) {
                clusters.map { cluster ->
                    val coreHistories = cluster.coreIndices.mapNotNull { idx ->
                        coreHistorySnapshot.getOrNull(idx)
                    }
                    if (coreHistories.isEmpty()) {
                        emptyList()
                    } else {
                        val minSize = coreHistories.minOf { it.size }
                        if (minSize == 0) emptyList()
                        else List(minSize) { index ->
                            coreHistories.map { it[it.size - minSize + index] }.average().toFloat()
                        }
                    }
                }
            } else emptyList()
        }

        // Battery/wattage snapshots for slow flow
        batteryHistorySnapshot = snapshotIfChanged(batteryHistoryBuffer, batteryHistorySnapshot)
        wattageHistorySnapshot = snapshotIfChanged(wattageHistoryBuffer, wattageHistorySnapshot)

        // Cache formatted disk speed strings (avoid formatSize + string concat per tick)
        val diskReadStr = formatSize(currentDiskReadSpeed) + "/s"
        val diskWriteStr = formatSize(currentDiskWriteSpeed) + "/s"

        // Cache CPU frequency list — only allocate new List when values changed
        val newFreqs = cpuFrequencies.toList()
        if (newFreqs != cpuFreqsSnapshot) cpuFreqsSnapshot = newFreqs

        // ── Emit REALTIME flow (only fast-changing data) ─────────────────
        // Uses snapshot lists — no new list allocation here
        val newRealtimeData = DashboardRealtimeData(
            ramTotal = ramTotal,
            ramUsed = ramUsedVal,
            ramHistory = ramHistorySnapshot,
            cpuUsage = cpuUsageVal,
            cpuHistory = cpuHistorySnapshot,
            gpuUsage = gpuUsageVal,
            gpuHistory = gpuHistorySnapshot,
            cpuCoreFrequencies = cpuFreqsSnapshot,
            cpuCoreHistory = coreHistorySnapshot,
            clusterHistories = clusterHistorySnapshot,
            currentRefreshRate = finalFps,
            downloadSpeed = dlSpeed,
            uploadSpeed = ulSpeed,
            downloadSpeedHistory = downloadSpeedHistorySnapshot,
            uploadSpeedHistory = uploadSpeedHistorySnapshot,
            touchSamplingRate = touchSamplingRate.value,
            diskReadSpeed = diskReadStr,
            diskWriteSpeed = diskWriteStr,
            ambientLightLux = ambientLightLux,
            pressureHpa = pressureHpa,
            cpuTemperature = currentTemp,
            cpuTempHistory = cpuTempHistorySnapshot,
            gpuFreq = currentGpuFreqVal,
            gpuFreqHistory = gpuFreqHistorySnapshot,
            gpuMemory = currentGpuMemoryVal,
            gpuMemoryHistory = gpuMemoryHistorySnapshot,
            gpuTemperature = currentGpuTemp,
            gpuTempHistory = gpuTempHistorySnapshot,
            fpsHistory = fpsHistorySnapshot
        )
        // Only emit to StateFlow if data actually changed (cheap: list refs are stable)
        if (newRealtimeData != lastEmittedRealtime) {
            lastEmittedRealtime = newRealtimeData
            _dashboardRealtime.value = newRealtimeData
        }

        // ── SLOW flow is now ONLY emitted from the slow loop (every 5s) ─
        // This was the #1 cause of status card recomposition storms:
        // _dashboardSlow was being rebuilt every tick even though 90% of its
        // fields were identical. Now it's emitted in emitSlowDashboard() only.

        // Update TelemetryManager for the overlay
        com.example.relab_tool.data.TelemetryManager.updateData(
            com.example.relab_tool.data.TelemetryManager.TelemetryData(
                cpuUsage = cpuUsageVal,
                gpuUsage = gpuUsageVal,
                fps = finalFps,
                temperature = bInfo?.temperature ?: context.getString(R.string.na),
                diskRead = diskReadStr,
                diskWrite = diskWriteStr
            )
        )
    }

    private fun getColorDepthInfo(): Pair<String, String> {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
            val hdrCaps = display.hdrCapabilities
            val isWideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) display.isWideColorGamut else false
            
            // Higher precision bit depth detection via display color mode
            val colorMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { Display::class.java.getMethod("getColorMode").invoke(display) as Int } catch (e: Exception) { -1 }
            } else -1
            
            return when {
                colorMode == 4 -> context.getString(R.string.color_1_07_billion) to context.getString(R.string.tag_p3_display) // COLOR_MODE_BT2100_PQ (10-bit)
                colorMode == 2 -> context.getString(R.string.color_16_7_million_p3) to context.getString(R.string.tag_p3_display) // COLOR_MODE_WIDE_CG
                isWideColorGamut -> context.getString(R.string.color_16_7_million_p3) to context.getString(R.string.tag_p3_display)
                else -> context.getString(R.string.color_16_7_million_srgb) to context.getString(R.string.tag_standard_rgb)
            }
        }
        return context.getString(R.string.color_16_7_million_srgb) to context.getString(R.string.tag_standard_rgb)
    }

    private fun loadStaticInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            
            _isPlayStoreAvailable.value = try {
                context.packageManager.getPackageInfo("com.android.vending", 0)
                true
            } catch (e: Exception) {
                false
            }

            // WindowManager requires a visual (Activity) context and throws on Application context.
            // DisplayManager works from any context and provides the same display metrics.
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val metrics = DisplayMetrics()
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.getRealMetrics(metrics)
                ?: context.resources.displayMetrics.let { dm ->
                    metrics.widthPixels = dm.widthPixels
                    metrics.heightPixels = dm.heightPixels
                    metrics.density = dm.density
                    metrics.densityDpi = dm.densityDpi
                    metrics.xdpi = dm.xdpi
                    metrics.ydpi = dm.ydpi
                }


            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            
            val rawRamType = getRamTypeInfo()
            val resolvedModelName = com.example.relab_tool.utils.DeviceNameResolver.resolveDeviceName(context)

            _deviceSummary.value = DeviceSummary(
                model = resolvedModelName,
                manufacturer = Build.MANUFACTURER,
                device = Build.DEVICE,
                board = Build.BOARD,
                hardware = Build.HARDWARE,
                androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: context.getString(R.string.unknown),
                gsfId = getGsfId(context),
                buildFingerprint = Build.FINGERPRINT,
                usbHost = if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) context.getString(R.string.status_supported) else context.getString(R.string.status_not_supported),
                resolution = "${metrics.widthPixels} x ${metrics.heightPixels}",
                platform = SoCUtils.getSoCModel(),
                androidVersion = Build.VERSION.RELEASE,
                kernel = System.getProperty("os.version") ?: context.getString(R.string.unknown),
                touchscreen = getTouchscreenInfo(),
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.name ?: "N/A",
                als_ps = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.name ?: "N/A",
                magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.name ?: "N/A",
                gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.name ?: "N/A",
                charger = getChargerInfo(),
                nfc = getNfcInfo(),
                fingerprintSensor = getFingerprintInfo(),
                wifiChip = getWifiChipInfo(),
                soundChip = getSoundChipInfo(),
                ramType = rawRamType,
                flashType = getFlashTypeInfo()
            )

            // System Info
            val osVersion = getOsVersionInfo()
            _systemInfo.value = SystemInfo(
                brand = Build.BRAND,
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                modelName = resolvedModelName,
                androidVersion = Build.VERSION.RELEASE,
                osVersion = osVersion,
                codeName = Build.VERSION.CODENAME,
                sdkLevel = Build.VERSION.SDK_INT.toString(),
                device = Build.DEVICE,
                product = Build.PRODUCT,
                board = Build.BOARD,
                platform = Build.HARDWARE,
                buildId = Build.ID,
                javaVm = System.getProperty("java.vm.version") ?: getApplication<Application>().getString(R.string.unknown),
                securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
                baseband = Build.getRadioVersion() ?: getApplication<Application>().getString(R.string.unknown),
                gps = getGpsInfo(),
                bluetoothVersion = getBluetoothVersion(),
                buildType = Build.TYPE,
                tags = Build.TAGS,
                incremental = Build.VERSION.INCREMENTAL,
                description = getSystemProperty("ro.build.description") ?: getApplication<Application>().getString(R.string.unknown),
                fingerprint = Build.FINGERPRINT,
                buildDate = getBuildDate(),
                builder = getSystemProperty("ro.build.user") + "@" + getSystemProperty("ro.build.host"),
                bootloader = Build.BOOTLOADER,
                kernel = System.getProperty("os.version") ?: getApplication<Application>().getString(R.string.unknown),
                openGlEs = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo.glEsVersion,
                googlePlayServices = getGmsVersion(context),
                deviceFeatures = context.packageManager.systemAvailableFeatures.size.toString(),
                language = Locale.getDefault().displayLanguage,
                timezone = TimeZone.getDefault().id,
                uptime = getUptime(),
                gmsVersion = getGmsVersion(context)
            )

            // CPU Info
            _cpuInfo.value = CpuInfo(
                processor = SoCUtils.getCommercialName(context),
                architecture = System.getProperty("os.arch") ?: getApplication<Application>().getString(R.string.unknown),
                cores = Runtime.getRuntime().availableProcessors(),
                supportedAbis = Build.SUPPORTED_ABIS.joinToString(", "),
                cpuGovernor = getCpuGovernor()
            )

            // Display Info — reuse displayManager declared above (already fetched via DisplayService)
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            val currentRefreshRate = display?.refreshRate ?: 60f
            
            val supportedModes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                display?.supportedModes?.map { "${it.refreshRate.toInt()} Hz" }?.distinct()?.sortedDescending()?.joinToString(", ") ?: "N/A"
            } else "N/A"

            val isWideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.resources.configuration.isScreenWideColorGamut
            } else false

            val orientationStr = when (context.resources.configuration.orientation) {
                android.content.res.Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                android.content.res.Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                else -> getApplication<Application>().getString(R.string.unknown)
            }

            val gcd = { a: Int, b: Int ->
                var x = a
                var y = b
                while (y != 0) {
                    val temp = y
                    y = x % y
                    x = temp
                }
                x
            }
            val common = gcd(metrics.widthPixels, metrics.heightPixels)
            val aspectStr = "${metrics.heightPixels / common}:${metrics.widthPixels / common}"

            val diagonalPixels = Math.sqrt(Math.pow(metrics.widthPixels.toDouble(), 2.0) + Math.pow(metrics.heightPixels.toDouble(), 2.0))
            val xInches = metrics.widthPixels / (if (metrics.xdpi < 1) metrics.densityDpi.toFloat() else metrics.xdpi)
            val yInches = metrics.heightPixels / (if (metrics.ydpi < 1) metrics.densityDpi.toFloat() else metrics.ydpi)
            val diagonalInches = Math.sqrt(Math.pow(xInches.toDouble(), 2.0) + Math.pow(yInches.toDouble(), 2.0))
            val ppi = (diagonalPixels / diagonalInches).toInt()

            val highestRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                display?.supportedModes?.maxByOrNull { it.physicalWidth * it.physicalHeight }?.let { "${it.physicalWidth} x ${it.physicalHeight}" } ?: "N/A"
            } else "N/A"

            val (colorDepth, colorDepthSub) = getColorDepthInfo()

            _displayInfo.value = DisplayInfo(
                currentResolution = "${metrics.widthPixels} x ${metrics.heightPixels}",
                highestResolution = highestRes,
                aspectRatio = aspectStr,
                density = context.getString(R.string.density_format, metrics.densityDpi, getDensityString(metrics)),
                xDpi = "%.0f".format(metrics.xdpi),
                yDpi = "%.0f".format(metrics.ydpi),
                ppi = context.getString(R.string.ppi_format, ppi),
                currentRefreshRate = context.getString(R.string.unit_hz_float, currentRefreshRate),
                supportedRefreshRates = supportedModes,
                supportedRates = supportedModes,
                hdrSupport = getHdrSupport(),
                wideColorGamut = isWideColorGamut,
                physicalSize = getPhysicalSize(metrics),
                diagonal = String.format("%.1f\"", diagonalInches),
                brightnessLevel = getBrightness(),
                screenTimeout = context.getString(R.string.unit_min_sec_short, Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 0) / 1000),
                orientation = orientationStr,
                colorDepth = colorDepth,
                colorDepthSubtext = colorDepthSub,
                widevineLevel = _dashboardSlow.value?.widevineLevel ?: "N/A",
                colorSpace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try { Display::class.java.getMethod("getColorMode").invoke(display).toString() } catch (e: Exception) { "N/A" }
                } else "N/A"
            )

            // Sensors
            _sensors.value = sensorManager.getSensorList(Sensor.TYPE_ALL).map {
                SensorInfo(
                    it.name, it.vendor, it.version, it.type, it.power, it.resolution,
                    maxDelay = it.maxDelay, minDelay = it.minDelay
                )
            }

            // Features for Dashboard
            featureUsbStorage = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
            featureUsbAccessory = pm.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY)
            featureIrisScanner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.hasSystemFeature(PackageManager.FEATURE_IRIS) else false
            featureFaceRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.hasSystemFeature(PackageManager.FEATURE_FACE) else false
            featureInfrared = pm.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
            featureUwb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) pm.hasSystemFeature(PackageManager.FEATURE_UWB) else false
            featureNfc = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
            featureSecureNfc = pm.hasSystemFeature("android.hardware.nfc.hce") || pm.hasSystemFeature("android.hardware.nfc.ese")
            featureGps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
            staticFeaturesLoaded = true
        }
    }
    private fun getTouchscreenInfo(): String {
        return try {
            val file = File("/proc/bus/input/devices")
            if (file.exists()) {
                val content = file.readText()
                val lines = content.lines()
                for (i in lines.indices) {
                    if (lines[i].contains("Handlers=mouse") || lines[i].contains("Handlers=event")) {
                        for (j in (i - 1) downTo (i - 5).coerceAtLeast(0)) {
                            if (lines[j].startsWith("N: Name=")) {
                                val name = lines[j].substringAfter("Name=\"").substringBefore("\"")
                                if (name.contains("touch", true) || name.contains("ts", true)) {
                                    return name
                                }
                            }
                        }
                    }
                }
            }
            "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getChargerInfo(): String {
        return getSystemProperty("ro.boot.charger") ?: "USE PMIC"
    }

    private fun getNfcInfo(): String {
        val pm = getApplication<Application>().packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) return "N/A"
        return getSystemProperty("ro.boot.nfc") ?: getApplication<Application>().getString(R.string.supported)
    }

    private fun getFingerprintInfo(): String {
        return getSystemProperty("ro.boot.fingerprint") ?: getApplication<Application>().getString(R.string.supported)
    }

    private fun getWifiChipInfo(): String {
        val props = arrayOf(
            "ro.boot.wifi_chipid",
            "vendor.wlan.chip",
            "ro.hardware.wlan",
            "wlan.driver.status",
            "ro.wlan.vendor",
            "ro.wlan.chip",
            "wlan.chip.id",
            "ro.connectivity.chiptype",
            "wifi.interface"
        )
        for (prop in props) {
            val value = getSystemProperty(prop)
            if (!value.isNullOrEmpty() && value != "Unknown") return value
        }
        
        try {
            val netDir = File("/sys/class/net/")
            if (netDir.exists()) {
                val interfaces = netDir.listFiles { _, name -> name.startsWith("wlan") || name.startsWith("p2p") } ?: emptyArray()
                for (iface in interfaces) {
                    val uevent = File(iface, "device/uevent")
                    if (uevent.exists()) {
                        uevent.readLines().forEach { line ->
                            if (line.contains("PCI_ID=")) return "PCI: " + line.substringAfter("=")
                            if (line.contains("MODALIAS=")) {
                                val alias = line.substringAfter("MODALIAS=")
                                if (alias.contains("sdio:")) return "SDIO: " + alias.substringAfter("sdio:")
                                return alias
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        
        return getApplication<Application>().getString(R.string.unknown)
    }

    private fun getSoundChipInfo(): String {
        val props = arrayOf(
            "ro.boot.audio",
            "ro.boot.sound",
            "vendor.audio.codec",
            "ro.vendor.audio.sdk.name",
            "persist.vendor.audio.codec",
            "ro.hardware.audio.primary",
            "ro.boot.soundcardid"
        )
        for (prop in props) {
            val value = getSystemProperty(prop)
            if (!value.isNullOrEmpty() && value != "Unknown") return value
        }

        try {
            val cardsFile = File("/proc/asound/cards")
            if (cardsFile.exists()) {
                val lines = cardsFile.readLines()
                for (line in lines) {
                    if (line.contains(":") && !line.contains("---")) {
                        val name = line.substringAfter("[").substringBefore("]").trim()
                        if (name.isNotEmpty() && name != "PCH" && name != "Loopback") return name
                    }
                }
            }

            val soundDir = File("/sys/class/sound/")
            if (soundDir.exists()) {
                val cards = soundDir.listFiles { _, name -> name.startsWith("card") } ?: emptyArray()
                for (card in cards) {
                    val idFile = File(card, "id")
                    if (idFile.exists()) {
                        val id = idFile.readText().trim()
                        if (id.isNotEmpty() && id != "PCH" && id != "Loopback") return id
                    }
                }
            }
        } catch (e: Exception) {}

        return getApplication<Application>().getString(R.string.unknown)
    }

    private fun getRamTypeInfo(): String {
        val context = getApplication<Application>()
        val socModel = SoCUtils.getSoCModel()
        val ddrFromDb = SpecLoader.getSoCDdrType(context, socModel)
        
        if (ddrFromDb != null && !ddrFromDb.contains("/")) {
            return ddrFromDb
        }

        val props = arrayOf(
            "ro.boot.ddr_type",
            "ro.boot.ddr_info",
            "vendor.boot.ddr_info",
            "ro.boot.lpddr_info",
            "ro.boot.ddrinfo",
            "ro.ddr_type",
            "vendor.boot.ddr_type",
            "ro.vendor.qti.soc_name",
            "ro.vendor.qti.soc_model"
        )
        
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        for (prop in props) {
            val v = getSystemProperty(prop)
            if (!v.isNullOrEmpty() && v != "Unknown") {
                if (v.all { it.isDigit() }) {
                    val code = v.toInt()
                    val mapped = when {
                        manufacturer.contains("samsung") -> {
                            when (code) {
                                0 -> "LPDDR3"
                                1 -> "LPDDR4"
                                2 -> "LPDDR4X"
                                3 -> "LPDDR5"
                                4 -> "LPDDR5X"
                                5 -> "LPDDR5T"
                                else -> null
                            }
                        }
                        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") || manufacturer.contains("qcom") || manufacturer.contains("qualcomm") -> {
                            when (code) {
                                8 -> "LPDDR4X"
                                6 -> "LPDDR5"
                                12 -> "LPDDR5X"
                                4 -> "LPDDR4"
                                3 -> "LPDDR3"
                                else -> null
                            }
                        }
                        else -> {
                            when (code) {
                                8 -> "LPDDR4X"
                                6 -> "LPDDR5"
                                12 -> "LPDDR5X"
                                4 -> "LPDDR4"
                                3 -> "LPDDR3"
                                2 -> "LPDDR4X"
                                else -> null
                            }
                        }
                    }
                    if (mapped != null) return mapped
                }
                
                if (v.contains("LPDDR", true)) {
                    val match = Regex("LPDDR[2345][X]?").find(v.uppercase())
                    if (match != null) return match.value
                    return v.uppercase()
                }
            }
        }
        
        try {
            val dtPaths = listOf("/proc/device-tree/memory/lpddr-type", "/sys/class/memory/lpddr_type")
            for (path in dtPaths) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val type = file.readText().trim()
                    if (type.isNotEmpty()) {
                        if (type.all { it.isDigit() }) {
                            val code = type.toInt()
                            if (code == 4) return "LPDDR4X"
                            if (code == 5) return "LPDDR5"
                        }
                        return if (type.startsWith("LPDDR", true)) type.uppercase() else "LPDDR$type".uppercase()
                    }
                }
            }
        } catch (e: Exception) {}

        if (ddrFromDb != null && ddrFromDb.contains("/")) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val ramGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            
            return if (ramGb > 6.5) ddrFromDb.substringAfter("/") else ddrFromDb.substringBefore("/")
        }

        return ddrFromDb ?: "LPDDR4X"
    }

    private fun getFlashTypeInfo(): String {
        return try {
            val ufsFile = File("/sys/class/scsi_device/0:0:0:0/device/model")
            val ufsModel = if (ufsFile.exists()) ufsFile.readText().trim() else {
                val sdaModel = File("/sys/block/sda/device/model")
                if (sdaModel.exists()) sdaModel.readText().trim() else null
            }
            
            if (ufsModel != null) return "UFS $ufsModel"
            
            val mmcFile = File("/sys/block/mmcblk0/device/name")
            if (mmcFile.exists()) return "eMMC " + mmcFile.readText().trim()

            "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun getGpsInfo(): String {
        return getSystemProperty("ro.hardware.gps") ?: getApplication<Application>().getString(R.string.unknown)
    }

    private fun getGmsVersion(context: Context): String {
        return try {
            val info = context.packageManager.getPackageInfo("com.google.android.gms", 0)
            info.versionName ?: "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun getBuildDate(): String {
        val timestamp = Build.TIME
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }

    private fun getUptime(): String {
        val uptime = SystemClock.elapsedRealtime() / 1000
        val hours = uptime / 3600
        val minutes = (uptime % 3600) / 60
        return "%dh %dm".format(hours, minutes)
    }

    private fun resolveDeviceName(): String {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        
        lookupLocalDeviceDb(model)?.let { return it }
        
        return "$manufacturer $model"
    }

    private fun lookupLocalDeviceDb(model: String): String? {
        return try {
            val json = getApplication<Application>().assets.open("google_play_devices.json").bufferedReader().use { it.readText() }
            val array = org.json.JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("model").equals(model, ignoreCase = true)) {
                    return obj.getString("name")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun getMarketingName(): String {
        val props = listOf(
            "ro.product.marketname",
            "ro.product.model.marketname",
            "ro.vendor.product.marketname",
            "ro.config.marketing_name"
        )
        for (prop in props) {
            val value = getSystemProperty(prop)
            if (!value.isNullOrEmpty()) return value
        }
        return resolveDeviceName()
    }

    private fun getOsVersionInfo(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val buildId = Build.DISPLAY.lowercase()
        
        return when {
            manufacturer.contains("samsung") -> {
                val oneUiVersion = getSystemProperty("ro.build.version.oneui")
                if (!oneUiVersion.isNullOrEmpty()) {
                    val major = oneUiVersion.substring(0, oneUiVersion.length - 4)
                    val minor = oneUiVersion.substring(oneUiVersion.length - 4, oneUiVersion.length - 3)
                    "One UI $major.$minor"
                } else "One UI"
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                val hyperOs = getSystemProperty("ro.miui.ui.version.name")
                if (hyperOs?.contains("V816") == true || Build.VERSION.SDK_INT >= 34) {
                    "HyperOS"
                } else {
                    "MIUI"
                }
            }
            manufacturer.contains("vivo") -> {
                val origin = getSystemProperty("ro.vivo.os.name")
                if (origin?.lowercase()?.contains("origin") == true) "OriginOS" else "FuntouchOS"
            }
            manufacturer.contains("realme") -> {
                val realmeUi = getRealmeUIVersion()
                realmeUi ?: "Realme UI"
            }
            manufacturer.contains("oneplus") -> {
                getOxygenOSVersion()
            }
            manufacturer.contains("oppo") -> {
                val colorOs = getColorOSVersion()
                colorOs ?: "ColorOS"
            }
            manufacturer.contains("nothing") -> "Nothing OS"
            manufacturer.contains("motorola") || manufacturer.contains("lenovo") -> {
                val zuiVer = getSystemProperty("ro.zui.version") ?: getSystemProperty("ro.build.version.zui")
                if (!zuiVer.isNullOrEmpty()) {
                    "ZUI $zuiVer"
                } else {
                    val isHelloUI = Build.VERSION.SDK_INT >= 34 || !getSystemProperty("ro.mot.uxcore.enable").isNullOrEmpty()
                    if (isHelloUI) "Hello UI" else "My UX"
                }
            }
            manufacturer.contains("huawei") -> {
                val emuiVer = getSystemProperty("ro.build.version.emui")
                if (!emuiVer.isNullOrEmpty()) {
                    val cleanVer = emuiVer.replace("EmotionUI_", "")
                    "EMUI $cleanVer"
                } else {
                    val ohosVer = getSystemProperty("hw_sc.build.platform.version")
                    if (!ohosVer.isNullOrEmpty()) "HarmonyOS $ohosVer" else "HarmonyOS"
                }
            }
            manufacturer.contains("honor") -> {
                val magicOs = getSystemProperty("ro.honor.magic_os.version") ?: getSystemProperty("ro.build.version.magic")
                if (!magicOs.isNullOrEmpty()) "MagicOS $magicOs" else "Magic UI"
            }
            manufacturer.contains("asus") -> {
                val isRog = Build.MODEL.contains("ROG", ignoreCase = true)
                if (isRog) "ROG UI" else "ZenUI"
            }
            manufacturer.contains("meizu") -> {
                val flyme = getSystemProperty("ro.build.display.id")
                if (flyme?.contains("flyme", ignoreCase = true) == true) "Flyme" else "Flyme OS"
            }
            manufacturer.contains("zte") -> {
                val myOs = getSystemProperty("ro.build.myos.version")
                if (!myOs.isNullOrEmpty()) "MyOS $myOs" else "MyOS"
            }
            manufacturer.contains("nubia") -> {
                val redmagic = getSystemProperty("ro.build.rom.id")
                if (redmagic?.contains("redmagic", ignoreCase = true) == true) "Redmagic OS" else "Nubia UI"
            }
            manufacturer.contains("bkav") || Build.BRAND.lowercase().contains("bphone") -> {
                val bosVer = getSystemProperty("ro.build.version.bos")
                if (!bosVer.isNullOrEmpty()) {
                    val cleanVer = bosVer.replace("BOS", "").trim()
                    "BOS $cleanVer"
                } else "BOS"
            }
            else -> "Stock Android"
        }
    }

    private fun readVendorCameraConfig(): String? {
        val paths = listOf("/vendor/etc/camera/camera_config.xml", "/system/etc/camera/camera_config.xml")
        for (path in paths) {
            val file = File(path)
            if (file.exists()) return file.readText()
        }
        return null
    }

    private fun readCameraSystemProps(): Map<String, String> {
        val results = mutableMapOf<String, String>()
        val prefixes = listOf("vendor.camera.sensor.", "persist.vendor.camera.sensor.", "ro.vendor.camera.sensor.")
        // Since we can't easily list all props without shell/root, we check common ones
        val commonIndices = 0..4
        for (i in commonIndices) {
            for (prefix in prefixes) {
                val value = getSystemProperty("${prefix}name$i")
                if (!value.isNullOrEmpty()) results["sensor$i"] = value
            }
        }
        return results
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            val value = get.invoke(systemProperties, key) as String
            if (value.isEmpty()) null else value
        } catch (e: Exception) {
            null
        }
    }

    private fun getColorOSVersion(): String? {
        val version = getSystemProperty("ro.build.version.opporom")
            ?: getSystemProperty("ro.build.version.coloros")
        return if (!version.isNullOrEmpty()) "ColorOS $version" else null
    }

    private fun getRealmeUIVersion(): String? {
        val version = getSystemProperty("ro.build.version.realmeui")
        return if (!version.isNullOrEmpty()) "Realme UI $version" else null
    }

    private fun getOxygenOSVersion(): String {
        val version = getSystemProperty("ro.build.version.ota")
            ?: getSystemProperty("ro.oxygen.version")
        return if (!version.isNullOrEmpty()) "OxygenOS $version" else "OxygenOS"
    }

    private fun updateBatteryRealtime() {
        val currentInfo = _batteryInfo.value ?: return
        var wearPct = batteryRepository.getCalculatedHealth()
        var actualCap = batteryRepository.getCalculatedCapacity()
        
        if (wearPct <= 0f || actualCap <= 0f) {
            val systemCycles = try {
                File("/sys/class/power_supply/battery/cycle_count").readText().trim().toInt()
            } catch (e: Exception) { 0 }
            val manualCycles = batteryRepository.getManualCycleCount()
            val cyclesToUse = if (systemCycles > 0) systemCycles else manualCycles
            
            if (cyclesToUse >= 6) {
                val designCapStr = getBatteryCapacity().filter { it.isDigit() || it == '.' }
                val designCap = designCapStr.toFloatOrNull() ?: 5000f
                val estimatedHealth = (100.0f - (cyclesToUse * 0.08f) - 0.15f).coerceIn(80f, 100f)
                val estimatedCap = designCap * (estimatedHealth / 100f)
                
                batteryRepository.saveCalculatedCapacity(estimatedCap)
                batteryRepository.saveCalculatedHealth(estimatedHealth)
                
                wearPct = estimatedHealth
                actualCap = estimatedCap
            }
        }
        val context = getApplication<Application>()
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        
        val rawCurrent = getBatteryCurrentMa()
        val isCurrentSupported = rawCurrent != Long.MAX_VALUE
        val currentMa = if (isCurrentSupported) rawCurrent.toDouble() else 0.0
        val isCharging = currentInfo.isCharging
        val isScreenOn = powerManager.isInteractive

        val alpha = when {
            isCharging -> 0.1
            !isScreenOn -> 0.01
            else -> 0.05
        }

        if (isCurrentSupported) {
            if (batteryDataPoints == 0) {
                smoothedCurrentMa = currentMa
            } else {
                smoothedCurrentMa = alpha * currentMa + (1 - alpha) * smoothedCurrentMa
            }
            batteryDataPoints++
        } else {
            smoothedCurrentMa = 0.0
            batteryDataPoints = 0
        }

        val isConfident = isCurrentSupported && batteryDataPoints > 15
        
        if (lastLevelRecorded != currentInfo.level) {
            levelChangeHistory.add(System.currentTimeMillis() to currentInfo.level)
            if (levelChangeHistory.size > 10) levelChangeHistory.removeAt(0)
            lastLevelRecorded = currentInfo.level
        }

        var timeRemainingMs = -1L
        
        if (isConfident) {
            if (isCharging && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                timeRemainingMs = batteryManager.computeChargeTimeRemaining()
            } else if (!isCharging && smoothedCurrentMa < -5.0) {
                try {
                    val totalCapacityStr = getBatteryCapacity().filter { it.isDigit() || it == '.' }
                    val totalCapacityMah = totalCapacityStr.toDoubleOrNull() ?: 0.0
                    if (totalCapacityMah > 0) {
                        val remainingMah = (currentInfo.level / 100.0) * totalCapacityMah
                        val dischargeMa = Math.abs(smoothedCurrentMa)
                        timeRemainingMs = (remainingMah / dischargeMa * 3600 * 1000).toLong()
                    }
                } catch (e: Exception) {}
            }
            
            if (timeRemainingMs <= 0 && levelChangeHistory.size >= 2 && !isCharging) {
                val first = levelChangeHistory.first()
                val last = levelChangeHistory.last()
                val deltaPct = first.second - last.second
                val deltaTime = last.first - first.first
                if (deltaPct > 0 && deltaTime > 60000) {
                    val msPerPct = deltaTime / deltaPct
                    timeRemainingMs = msPerPct * currentInfo.level
                }
            }
        }

        val now = System.currentTimeMillis()
        val timeDiff = Math.abs(timeRemainingMs - lastEstimationValue)
        val enoughTimePassed = now - lastEstimationUpdateTs > 30000
        val significantChange = timeDiff > 60000

        if (significantChange || enoughTimePassed || lastEstimationValue == -1L) {
            lastEstimationValue = timeRemainingMs
            lastEstimationUpdateTs = now
        }

        val voltageMv = if (lastVoltageMv > 0) lastVoltageMv else {
            try {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                getBatteryVoltageMv(intent)
            } catch (e: Exception) { 4000 }
        }
        
        val actualCurrentMa = if (isCurrentSupported) {
            if (isCharging) Math.abs(smoothedCurrentMa) else -Math.abs(smoothedCurrentMa)
        } else {
            0.0
        }
        val wattage = (voltageMv / 1000.0) * (actualCurrentMa / 1000.0)
        val wattageStr = if (isCurrentSupported) "%.2f W".format(Locale.US, wattage) else "N/A"

        if (isCurrentSupported && isCharging && currentInfo.level > 0 && (significantChange || enoughTimePassed || lastEstimationValue == -1L)) {
            if (wattage > 0) {
                batteryRepository.saveChargingPoint(currentInfo.level, wattage.toFloat())
            }
        }

        val isPowerSaveMode = powerManager.isPowerSaveMode

        _batteryInfo.value = currentInfo.copy(
            wattage = wattageStr,
            currentNow = if (isCurrentSupported) "${currentMa.toInt()} mA" else "N/A",
            timeToFull = lastEstimationValue,
            isPowerSaveMode = isPowerSaveMode,
            wear = if (wearPct > 0) "%.1f %%".format(Locale.US, 100f - wearPct) else "N/A",
            actualCapacity = if (actualCap > 0) "%.1f mAh".format(Locale.US, actualCap) else "N/A"
        )

        // Also update dashboard slow data with battery changes
        _dashboardSlow.update {
            it?.copy(
                batteryWattage = wattageStr,
                batteryTemp = currentInfo.temperature,
                batteryLevel = currentInfo.level,
                isCharging = currentInfo.isCharging,
                chargingCurrentMa = actualCurrentMa.toInt(),
                chargingVoltageV = voltageMv / 1000f,
                batteryHealthPct = if (wearPct > 0) wearPct.toInt() else 100,
                batteryCycleCount = getBatteryCycleCount()
            )
        }
    }

    private fun updateDisplayRealtime() {
        val currentInfo = _displayInfo.value ?: return
        val context = getApplication<Application>()
        
        // WindowManager.defaultDisplay requires a visual context; DisplayManager works from Application.
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val rate = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate ?: return
        
        _displayInfo.value = currentInfo.copy(
            currentRefreshRate = "${rate.toInt()} Hz"
        )
    }

    private fun checkWirelessChargingSupported(context: Context, intent: Intent): Boolean {
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            return true
        }
        try {
            val powerSupplyDir = File("/sys/class/power_supply")
            if (powerSupplyDir.exists() && powerSupplyDir.isDirectory) {
                val files = powerSupplyDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        val name = file.name.lowercase()
                        if (name.contains("wireless") || name.contains("wlc") || name.contains("wireless_charger")) {
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return false
    }

    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 0
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        
        val now = System.currentTimeMillis()
        val context = getApplication<Application>()
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentChargeCounter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        // Battery Wear Tracking
        if (isCharging) {
            val startLevel = batteryRepository.getChargeStartLevel()
            if (startLevel == -1) {
                batteryRepository.saveChargeStartLevel(batteryPct)
                batteryRepository.saveChargeStartCounter(currentChargeCounter)
            } else {
                val deltaPct = batteryPct - startLevel
                if (deltaPct >= 30) {
                    val startCounter = batteryRepository.getChargeStartCounter()
                    if (startCounter != -1L && currentChargeCounter > startCounter) {
                        val deltaAh = (currentChargeCounter - startCounter) / 1_000_000.0
                        val actualCap = (deltaAh / (deltaPct / 100.0)).toFloat() * 1000f // to mAh
                        
                        val designCapStr = getBatteryCapacity().filter { it.isDigit() || it == '.' }
                        val designCap = designCapStr.toFloatOrNull() ?: actualCap
                        val healthPct = (actualCap / designCap) * 100f
                        
                        if (healthPct in 50f..105f) {
                            batteryRepository.saveCalculatedCapacity(actualCap)
                            batteryRepository.saveCalculatedHealth(healthPct)
                        }
                    }
                }
            }
        } else {
            val startLevel = batteryRepository.getChargeStartLevel()
            if (startLevel != -1) {
                val deltaPct = batteryPct - startLevel
                if (deltaPct >= 30) {
                    val startCounter = batteryRepository.getChargeStartCounter()
                    if (startCounter != -1L && currentChargeCounter > startCounter) {
                        val deltaAh = (currentChargeCounter - startCounter) / 1_000_000.0
                        val actualCap = (deltaAh / (deltaPct / 100.0)).toFloat() * 1000f // to mAh
                        
                        val designCapStr = getBatteryCapacity().filter { it.isDigit() || it == '.' }
                        val designCap = designCapStr.toFloatOrNull() ?: actualCap
                        val healthPct = (actualCap / designCap) * 100f
                        
                        if (healthPct in 50f..105f) {
                            batteryRepository.saveCalculatedCapacity(actualCap)
                            batteryRepository.saveCalculatedHealth(healthPct)
                        }
                    }
                }
            }
            batteryRepository.saveChargeStartLevel(-1)
        }

        // Track stopped charging
        if (_batteryInfo.value?.isCharging == true && !isCharging) {
            _lastStoppedChargingTs.value = now
            batteryRepository.setLastStoppedChargingTs(now)
        }

        // Cycle count estimation logic
        if (lastManualLevel != -1 && !isCharging && batteryPct < lastManualLevel) {
            val discharge = lastManualLevel - batteryPct
            var acc = batteryRepository.getAccumulatedDischarge() + discharge
            var cycles = batteryRepository.getManualCycleCount()
            if (acc >= 100f) {
                cycles += (acc / 100).toInt()
                acc %= 100f
                batteryRepository.saveManualCycleCount(cycles)
            }
            batteryRepository.saveAccumulatedDischarge(acc)
        }
        lastManualLevel = batteryPct

        val rawVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageMv = if (rawVoltage in 1..10) rawVoltage * 1000 else rawVoltage
        lastVoltageMv = voltageMv
        
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
        
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.health_over_voltage)
            BatteryManager.BATTERY_HEALTH_COLD -> context.getString(R.string.health_cold)
            else -> getApplication<Application>().getString(R.string.unknown)
        }

        var wearPct = batteryRepository.getCalculatedHealth()
        var actualCap = batteryRepository.getCalculatedCapacity()
        
        if (wearPct <= 0f || actualCap <= 0f) {
            val systemCycles = try {
                File("/sys/class/power_supply/battery/cycle_count").readText().trim().toInt()
            } catch (e: Exception) { 0 }
            val manualCycles = batteryRepository.getManualCycleCount()
            val cyclesToUse = if (systemCycles > 0) systemCycles else manualCycles
            
            if (cyclesToUse >= 6) {
                val designCapStr = getBatteryCapacity().filter { it.isDigit() || it == '.' }
                val designCap = designCapStr.toFloatOrNull() ?: 5000f
                val estimatedHealth = (100.0f - (cyclesToUse * 0.08f) - 0.15f).coerceIn(80f, 100f)
                val estimatedCap = designCap * (estimatedHealth / 100f)
                
                batteryRepository.saveCalculatedCapacity(estimatedCap)
                batteryRepository.saveCalculatedHealth(estimatedHealth)
                
                wearPct = estimatedHealth
                actualCap = estimatedCap
            }
        }

        val healthString = if (wearPct > 0) {
            if (wearPct >= 80f) context.getString(R.string.health_good) else context.getString(R.string.health_needs_replacement)
        } else healthStr

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val powerSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> context.getString(R.string.power_source_wireless)
            else -> context.getString(R.string.power_source_battery)
        }

        val statusString = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(R.string.status_charging)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(R.string.status_discharging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.status_full)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(R.string.status_not_charging)
            else -> getApplication<Application>().getString(R.string.unknown)
        }

        val rawCurrent = getBatteryCurrentMa()
        val isCurrentSupported = rawCurrent != Long.MAX_VALUE
        val currentMa = if (isCurrentSupported) rawCurrent else 0L
        val actualCurrentMa = if (isCurrentSupported) {
            if (isCharging) Math.abs(currentMa) else -Math.abs(currentMa)
        } else {
            0L
        }
        val wattage = (voltageMv / 1000.0) * (actualCurrentMa / 1000.0)
        val wattageStr = if (isCurrentSupported) "%.2f W".format(Locale.US, wattage) else "N/A"

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSaveMode = powerManager.isPowerSaveMode

        _batteryInfo.value = BatteryInfo(
            health = healthString,
            level = batteryPct,
            levelString = "$batteryPct%",
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            status = statusString,
            powerSource = powerSource,
            technology = technology,
            temperature = com.example.relab_tool.utils.UnitFormatter.formatTemperature(context, temperature),
            wattage = wattageStr,
            capacity = getBatteryCapacity(),
            currentNow = if (isCurrentSupported) "$currentMa mA" else "N/A",
            chargeCounter = getBatteryChargeCounter(),
            timeToFull = lastEstimationValue,
            wear = if (wearPct > 0) "%.1f %%".format(Locale.US, 100f - wearPct) else "N/A",
            actualCapacity = if (actualCap > 0) "%.1f mAh".format(Locale.US, actualCap) else "N/A",
            voltage = "$voltageMv mV",
            isWirelessSupported = checkWirelessChargingSupported(context, intent)
        )
        
        updateBatteryRealtime()
    }

    private fun updateDynamicInfo() {
        viewModelScope.launch(Dispatchers.Default) {
            val context = getApplication<Application>()
            
            // System Uptime
            val uptimeStr = getUptime()
            
            _systemInfo.update { it?.copy(uptime = uptimeStr) }

            // Memory Usage
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)

            // RAM & ZRAM
            var zRamTotal = 0L
            var zRamFree = 0L
            try {
                File("/proc/meminfo").readLines().forEach { line ->
                    if (line.startsWith("SwapTotal:")) {
                        zRamTotal = line.split(Regex("\\s+"))[1].toLong() * 1024
                    } else if (line.startsWith("SwapFree:")) {
                        zRamFree = line.split(Regex("\\s+"))[1].toLong() * 1024
                    }
                }
            } catch (e: Exception) {}

            // Internal Storage Details
            val dataDir = Environment.getDataDirectory()
            val internalStat = StatFs(dataDir.path)
            val totalBytes = internalStat.totalBytes
            val freeBytes = internalStat.availableBytes
            
            // System vs Apps (Heuristic)
            val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            val hardwareTotal = when {
                totalGb > 512 -> 1024L * 1024 * 1024 * 1024
                totalGb > 256 -> 512L * 1024 * 1024 * 1024
                totalGb > 128 -> 256L * 1024 * 1024 * 1024
                totalGb > 64 -> 128L * 1024 * 1024 * 1024
                totalGb > 32 -> 64L * 1024 * 1024 * 1024
                totalGb > 16 -> 32L * 1024 * 1024 * 1024
                else -> totalBytes
            }
            
            val systemUsed = if (hardwareTotal > totalBytes) hardwareTotal - totalBytes else 0L
            val appsUsed = totalBytes - freeBytes

            val fsType = cachedStorageFsType ?: try {
                val p = Runtime.getRuntime().exec("mount")
                val output = p.inputStream.bufferedReader().readText()
                val detected = output.lines().find { it.contains(" /data ") }?.split(" ")?.get(2) ?: context.getString(R.string.unknown)
                cachedStorageFsType = detected
                detected
            } catch (e: Exception) { getApplication<Application>().getString(R.string.unknown) }

            val externalStorages = mutableListOf<ExternalStorageInfo>()

            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? android.os.storage.StorageManager
            if (storageManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                storageManager.storageVolumes.forEach { volume ->
                    if (volume.isRemovable || !volume.isPrimary) {
                        val volumeName = volume.getDescription(context)
                        try {
                            val pathFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                volume.directory
                            } else {
                                null
                            }
                            
                            if (pathFile != null && pathFile.exists()) {
                                val stat = StatFs(pathFile.path)
                                externalStorages.add(ExternalStorageInfo(
                                    name = volumeName ?: context.getString(R.string.external_drive),
                                    total = formatSize(stat.totalBytes),
                                    free = formatSize(stat.availableBytes),
                                    totalBytes = stat.totalBytes,
                                    freeBytes = stat.availableBytes,
                                    type = if (volume.isRemovable) context.getString(R.string.status_removable) else context.getString(R.string.status_secondary)
                                ))
                            }
                        } catch (e: Exception) {}
                    }
                }
            }

            if (externalStorages.isEmpty()) {
                try {
                    val storageDir = File("/storage")
                    if (storageDir.exists() && storageDir.isDirectory) {
                        storageDir.listFiles()?.forEach { file ->
                            if (file.isDirectory && file.canRead() && !file.name.equals("self", true) && !file.name.equals("emulated", true)) {
                                val stat = StatFs(file.path)
                                if (stat.totalBytes > 0) {
                                    externalStorages.add(ExternalStorageInfo(
                                        name = file.name,
                                        total = formatSize(stat.totalBytes),
                                        free = formatSize(stat.availableBytes),
                                        totalBytes = stat.totalBytes,
                                        freeBytes = stat.availableBytes,
                                        type = context.getString(R.string.external_drive)
                                    ))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {}
            }

            _memoryInfo.value = MemoryInfo(
                totalRam = formatSize(mi.totalMem),
                availableRam = formatSize(mi.availMem),
                totalRamBytes = mi.totalMem,
                availableRamBytes = mi.availMem,
                ramType = _deviceSummary.value?.ramType ?: getApplication<Application>().getString(R.string.unknown),
                zRamTotal = formatSize(zRamTotal),
                zRamUsed = formatSize(zRamTotal - zRamFree),
                zRamTotalBytes = zRamTotal,
                zRamUsedBytes = zRamTotal - zRamFree,
                internalTotal = formatSize(hardwareTotal),
                internalFree = formatSize(freeBytes),
                internalTotalBytes = hardwareTotal,
                internalFreeBytes = freeBytes,
                internalUsedBySystem = formatSize(systemUsed),
                internalUsedBySystemBytes = systemUsed,
                internalUsedByApps = formatSize(appsUsed),
                internalUsedByAppsBytes = appsUsed,
                internalFsType = fsType,
                internalBlockSize = "${internalStat.blockSizeLong / 1024} kB",
                memoryPageSize = getMemoryPageSize(),
                internalPartition = dataDir.path,
                externalStorages = externalStorages,
                flashType = _deviceSummary.value?.flashType ?: "N/A"
            )

            updateNetworkInfo()
        }
    }

    private fun updateNetworkInfo() {
        viewModelScope.launch(Dispatchers.Default) {
            val context = getApplication<Application>()
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            val prefs = context.getSharedPreferences("relab_prefs", Context.MODE_PRIVATE)
            val isLocationPermissionGranted = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val isLocationServicesEnabled = try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                lm != null && androidx.core.location.LocationManagerCompat.isLocationEnabled(lm)
            } catch (e: Exception) {
                false
            }

            val activeNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) cm.activeNetwork else null
            val caps = cm.getNetworkCapabilities(activeNetwork)
            val linkProps = cm.getLinkProperties(activeNetwork)
            
            val type = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> context.getString(R.string.status_disconnected)
            }
            
            var ssid: String? = null
            var wifiSsidState: com.example.relab_tool.data.WifiSsidState = com.example.relab_tool.data.WifiSsidState.NotConnected
            var bssid: String? = null
            var wifiSignalDbm: Int? = null
            var wifiStandard: String? = null
            var linkSpeed: String? = null
            var frequency: String? = null
            var signal: String? = null
            var standard: String? = null
            var iface: String? = null
            var channel: String? = null
            var width: String? = null
            var ip: String? = null
            var ipv6Addr: String? = null
            var gateway: String? = null
            var netmask: String? = null
            var dns1: String? = null
            var dns2: String? = null
            var dhcpServer: String? = null
            var lease: String? = null
            var security: String? = null
            var vendor: String? = null

            if (type == "Wi-Fi") {
                val isPermDenied = prefs.getBoolean("location_permission_permanently_denied", false)
                wifiSsidState = wifiInfoProvider.getSsidState(isPermDenied)
                ssid = when (wifiSsidState) {
                    is com.example.relab_tool.data.WifiSsidState.Connected -> wifiSsidState.ssid
                    is com.example.relab_tool.data.WifiSsidState.PermissionDenied -> "Unavailable — location permission required"
                    is com.example.relab_tool.data.WifiSsidState.PermissionPermanentlyDenied -> "Unavailable — enable in Settings"
                    is com.example.relab_tool.data.WifiSsidState.LocationServicesDisabled -> "Unavailable — enable Location in system settings"
                    is com.example.relab_tool.data.WifiSsidState.NotConnected -> "Not connected to Wi-Fi"
                }

                var wifiInfo: WifiInfo? = null
                if (isLocationPermissionGranted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        wifiInfo = caps?.transportInfo as? WifiInfo
                    }
                    if (wifiInfo == null) {
                        @Suppress("DEPRECATION")
                        wifiInfo = wm.connectionInfo
                    }
                }
                if (wifiInfo != null) {
                    bssid = if (isLocationPermissionGranted && isLocationServicesEnabled) {
                        val rawBssid = wifiInfo.bssid
                        if (rawBssid == "<unknown bssid>" || rawBssid == "00:00:00:00:00:00" || rawBssid == "02:00:00:00:00:00") null else rawBssid
                    } else {
                        "Unavailable — location permission required"
                    }
                    vendor = getMacVendor(bssid)
                    linkSpeed = "${wifiInfo.linkSpeed} Mbps"
                    frequency = "${wifiInfo.frequency} MHz"
                    signal = "${wifiInfo.rssi} dBm"
                    wifiSignalDbm = wifiInfo.rssi
                    iface = linkProps?.interfaceName
                    
                    // IP Formatting
                    val ipAddr = wifiInfo.ipAddress
                    ip = if (ipAddr != 0) String.format("%d.%d.%d.%d", (ipAddr and 0xff), (ipAddr shr 8 and 0xff), (ipAddr shr 16 and 0xff), (ipAddr shr 24 and 0xff)) else null
                    
                    // DHCP
                    val dhcp = wm.dhcpInfo
                    if (dhcp != null) {
                        gateway = if (dhcp.gateway != 0) String.format("%d.%d.%d.%d", (dhcp.gateway and 0xff), (dhcp.gateway shr 8 and 0xff), (dhcp.gateway shr 16 and 0xff), (dhcp.gateway shr 24 and 0xff)) else null
                        netmask = if (dhcp.netmask != 0) String.format("%d.%d.%d.%d", (dhcp.netmask and 0xff), (dhcp.netmask shr 8 and 0xff), (dhcp.netmask shr 16 and 0xff), (dhcp.netmask shr 24 and 0xff)) else null
                        dns1 = if (dhcp.dns1 != 0) String.format("%d.%d.%d.%d", (dhcp.dns1 and 0xff), (dhcp.dns1 shr 8 and 0xff), (dhcp.dns1 shr 16 and 0xff), (dhcp.dns1 shr 24 and 0xff)) else null
                        dns2 = if (dhcp.dns2 != 0) String.format("%d.%d.%d.%d", (dhcp.dns2 and 0xff), (dhcp.dns2 shr 8 and 0xff), (dhcp.dns2 shr 16 and 0xff), (dhcp.dns2 shr 24 and 0xff)) else null
                        dhcpServer = if (dhcp.serverAddress != 0) String.format("%d.%d.%d.%d", (dhcp.serverAddress and 0xff), (dhcp.serverAddress shr 8 and 0xff), (dhcp.serverAddress shr 16 and 0xff), (dhcp.serverAddress shr 24 and 0xff)) else null
                        lease = if (dhcp.leaseDuration > 0) {
                            val h = dhcp.leaseDuration / 3600
                            val m = (dhcp.leaseDuration % 3600) / 60
                            if (h > 0) "${h}h ${m}m" else "${m}m"
                        } else null
                    }

                    // WiFi Standard
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        standard = when (wifiInfo.wifiStandard) {
                            ScanResult.WIFI_STANDARD_11BE -> "Wi-Fi 7 (802.11be)"
                            ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6 (802.11ax)"
                            ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5 (802.11ac)"
                            ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4 (802.11n)"
                            ScanResult.WIFI_STANDARD_LEGACY -> "Legacy"
                            else -> getApplication<Application>().getString(R.string.unknown)
                        }
                        wifiStandard = standard
                    } else {
                        standard = when {
                            wifiInfo.frequency in 5150..5900 -> "Wi-Fi 5 (802.11ac)"
                            wifiInfo.frequency in 2400..2500 -> "Wi-Fi 4 (802.11n)"
                            else -> getApplication<Application>().getString(R.string.unknown)
                        }
                        wifiStandard = standard
                    }
                    
                    // Enhanced WiFi standard check for 6E
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && wifiInfo.frequency > 5925) {
                        if (standard?.contains("Wi-Fi 6") == true && !standard!!.contains("Wi-Fi 7")) {
                            standard = "Wi-Fi 6E (802.11ax)"
                            wifiStandard = standard
                        }
                    }
                    
                    // Security Type (API 31+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        security = try {
                            when (wifiInfo.currentSecurityType) {
                                WifiInfo.SECURITY_TYPE_OPEN -> "Open"
                                WifiInfo.SECURITY_TYPE_WEP -> "WEP"
                                WifiInfo.SECURITY_TYPE_PSK -> "WPA2-PSK"
                                WifiInfo.SECURITY_TYPE_SAE -> "WPA3-SAE"
                                WifiInfo.SECURITY_TYPE_EAP -> "EAP"
                                else -> "WPA2/WPA3"
                            }
                        } catch(e: Exception) { "WPA2/WPA3" }
                    }

                    // Channel Calculation
                    val freq = wifiInfo.frequency
                    channel = when {
                        freq == 2484 -> "14"
                        freq in 2412..2472 -> ((freq - 2412) / 5 + 1).toString()
                        freq in 5170..5825 -> ((freq - 5170) / 5 + 34).toString()
                        freq in 5945..7105 -> ((freq - 5945) / 5 + 1).toString()
                        else -> "N/A"
                    }
                    
                    // Width (Heuristic for UI, hard to get without scan)
                    width = when {
                        wifiInfo.linkSpeed >= 1200 -> "160 MHz"
                        wifiInfo.linkSpeed >= 600 -> "80 MHz"
                        wifiInfo.linkSpeed >= 300 -> "40 MHz"
                        else -> "20 MHz"
                    }
                }
            }

            if (ip == null) {
                ip = linkProps?.linkAddresses?.find { it.address is java.net.Inet4Address }?.address?.hostAddress
            }
            ipv6Addr = linkProps?.linkAddresses?.find { it.address is java.net.Inet6Address }?.address?.hostAddress?.substringBefore("%")
            if (iface == null) {
                iface = linkProps?.interfaceName
            }

            // Cellular Info
            val cellularInfo = if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                try {
                    val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    val subs = sm.activeSubscriptionInfoList ?: emptyList()
                    
                    val simInfos = subs.map { sub ->
                        val hasPhoneNumbersPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            context.checkSelfPermission(android.Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
                        } else {
                            context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                        }

                        val phoneNumber = if (hasPhoneNumbersPermission) {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    sm.getPhoneNumber(sub.subscriptionId)
                                } else {
                                    @Suppress("DEPRECATION")
                                    sub.number
                                }
                            } catch (e: Exception) {
                                try {
                                    tm.line1Number
                                } catch (e2: Exception) { null }
                            }
                        } else {
                            "Permission not granted"
                        }

                        SimInfo(
                            slot = sub.simSlotIndex + 1,
                            carrier = sub.carrierName.toString(),
                            phoneNumber = phoneNumber,
                            countryIso = sub.countryIso,
                            mcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) sub.mccString else sub.mcc.toString(),
                            mnc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) sub.mncString else sub.mnc.toString(),
                            serviceProviderId = null,
                            roaming = sub.dataRoaming == SubscriptionManager.DATA_ROAMING_ENABLE,
                            state = "Active"
                        )
                    }

                    val dataState = when (tm.dataState) {
                        TelephonyManager.DATA_CONNECTED -> context.getString(R.string.connected)
                        TelephonyManager.DATA_CONNECTING -> context.getString(R.string.connecting)
                        TelephonyManager.DATA_DISCONNECTED -> context.getString(R.string.disconnected)
                        TelephonyManager.DATA_SUSPENDED -> context.getString(R.string.suspended)
                        else -> getApplication<Application>().getString(R.string.unknown)
                    }

                    val ipv6s = linkProps?.linkAddresses
                        ?.filter { it.address is java.net.Inet6Address }
                        ?.map { it.address.hostAddress?.substringBefore("%") ?: "" }
                        ?.filter { it.isNotEmpty() } ?: emptyList()

                    CellularInfo(
                        state = dataState,
                        multiSimSupport = if (tm.phoneCount > 1) context.getString(R.string.status_supported) else context.getString(R.string.status_single_sim),
                        phoneCount = tm.phoneCount,
                        deviceType = when (tm.phoneType) {
                            TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                            TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                            else -> getApplication<Application>().getString(R.string.unknown)
                        },
                        ipV4 = linkProps?.linkAddresses?.find { it.address is java.net.Inet4Address }?.address?.hostAddress,
                        ipV6 = ipv6s,
                        interfaceName = linkProps?.interfaceName,
                        networkOperator = tm.networkOperatorName,
                        networkType = getCellularNetworkType(),
                        simInfos = simInfos
                    )
                } catch (e: Exception) {
                    CellularInfo(
                        state = getApplication<Application>().getString(R.string.unknown),
                        multiSimSupport = getApplication<Application>().getString(R.string.unknown),
                        phoneCount = tm.phoneCount,
                        deviceType = getApplication<Application>().getString(R.string.unknown),
                        networkOperator = tm.networkOperatorName
                    )
                }
            } else {
                CellularInfo(
                    state = getApplication<Application>().getString(R.string.unknown),
                    multiSimSupport = getApplication<Application>().getString(R.string.unknown),
                    phoneCount = tm.phoneCount,
                    deviceType = getApplication<Application>().getString(R.string.unknown),
                    networkOperator = tm.networkOperatorName
                )
            }

            val isWifi4Supported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11N)
            } else {
                true
            }
            val isWifi5Supported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11AC)
            } else {
                wm.is5GHzBandSupported
            }
            val isWifi6Supported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11AX)
            } else {
                false
            }
            val isWifi6eSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wm.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11AX) && wm.is6GHzBandSupported
            } else {
                false
            }
            val isWifi7Supported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                wm.isWifiStandardSupported(ScanResult.WIFI_STANDARD_11BE)
            } else {
                false
            }
            val isWifi8Supported = false

            _networkInfo.value = NetworkInfo(
                type = type,
                state = if (type == "Disconnected") context.getString(R.string.disconnected) else context.getString(R.string.connected),
                cellularInfo = cellularInfo,
                wifiSsid = ssid,
                wifiSsidState = wifiSsidState,
                isLocationPermissionGranted = isLocationPermissionGranted,
                isLocationServicesEnabled = isLocationServicesEnabled,
                wifiBssid = bssid,
                vendor = vendor,
                linkSpeed = linkSpeed,
                frequency = frequency,
                signalStrength = signal,
                channel = channel,
                width = width,
                standard = standard,
                security = security,
                ipAddress = ip,
                ipv6Address = ipv6Addr,
                gateway = gateway,
                netmask = netmask,
                dns1 = dns1,
                dns2 = dns2,
                dhcpServer = dhcpServer,
                leaseDuration = lease,
                interfaceName = iface,
                networkType = getCellularNetworkType(),
                isWifiDirectSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT),
                is5GHzSupported = wm.is5GHzBandSupported,
                is6GHzSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) wm.is6GHzBandSupported else false,
                isWifiAwareSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE) else false,
                isP2pSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT),
                isApSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI),
                isWifi4Supported = isWifi4Supported,
                isWifi5Supported = isWifi5Supported,
                isWifi6Supported = isWifi6Supported,
                isWifi6eSupported = isWifi6eSupported,
                isWifi7Supported = isWifi7Supported,
                isWifi8Supported = isWifi8Supported
            )
        }
    }

    private fun getCellularNetworkType(): String {
        val context = getApplication<Application>()
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Cellular"
        }

        val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { tm.dataNetworkType } catch (e: Exception) { TelephonyManager.NETWORK_TYPE_UNKNOWN }
        } else {
            @Suppress("DEPRECATION")
            try { tm.networkType } catch (e: Exception) { TelephonyManager.NETWORK_TYPE_UNKNOWN }
        }

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
            TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA -> "3G (HSPA)"
            TelephonyManager.NETWORK_TYPE_UMTS -> "3G (UMTS)"
            TelephonyManager.NETWORK_TYPE_EDGE -> "2G (EDGE)"
            TelephonyManager.NETWORK_TYPE_GPRS -> "2G (GPRS)"
            TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT -> "2G (CDMA)"
            TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_EVDO_B -> "3G (EVDO)"
            else -> "Cellular"
        }
    }

    private fun getMacVendor(mac: String?): String? {
        if (mac == null || mac == "02:00:00:00:00:00") return null
        // Placeholder for OUI lookup
        return null
    }

    private fun getNetworkType(cm: ConnectivityManager, network: android.net.Network?): String {
        val caps = cm.getNetworkCapabilities(network)
        return when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "None"
        }
    }

    // -- Lazy-load gate --------------------------------------------------------
    /**
     * Called by each tab composable the first time it is composed.
     * Dispatches to the appropriate sub-loader if the tab has not been loaded yet.
     * Concurrent calls for the same key are safely ignored.
     */
    fun ensureTabDataLoaded(tabKey: String) {
        if (!loadedTabs.add(tabKey)) return   // already loaded or loading
        viewModelScope.launch(Dispatchers.IO) {
            when (tabKey) {
                "soc"       -> loadSocInfoInternal()
                "camera"    -> loadCameraInfoInternal()
                "usb"       -> loadUsbInfoInternal()
                "codecs"    -> loadCodecsInfoInternal()
                "apps"      -> loadInstalledAppsInfoInternal()
                "system"    -> { /* Already loaded at startup */ }
                "network"   -> { /* Already updated in loop */ }
                "bluetooth" -> updateBluetoothInfo()
                "audio"     -> updateAudioInfo()
                "security"  -> updateSecurityInfo()
                else        -> { /* no-op */ }
            }
        }
    }

    fun clearSecurityCache() {
        cachedPermissionAudit = null
        cachedKeystoreType = null
        cachedHardwareBackedKeystore = null
        cachedSelinuxStatus = "Unknown"
        cachedStorageFsType = null
    }

    /**
     * Suspending version of loadAdvancedInfo. Ensures all lazy data is loaded in parallel.
     */
    suspend fun loadAdvancedInfoSuspending() {
        clearSecurityCache()
        // Mark all as loaded/loading to avoid redundant triggers
        loadedTabs.addAll(listOf("soc", "camera", "usb", "codecs", "apps", "security", "bluetooth", "audio"))
        kotlinx.coroutines.coroutineScope {
            launch { loadSocInfoInternal() }
            launch { loadCameraInfoInternal() }
            launch { loadUsbInfoInternal() }
            launch { loadCodecsInfoInternal() }
            launch { loadInstalledAppsInfoInternal() }
            launch { updateSecurityInfo() }
            launch { updateBluetoothInfo() }
            launch { updateAudioInfo() }
        }
    }

    /**
     * Convenience wrapper -- loads all heavy sections at once.
     * Kept for compatibility; called by Settings "Refresh" button etc.
     */
    fun loadAdvancedInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            loadAdvancedInfoSuspending()
        }
    }

    // -- Per-tab sub-loaders ---------------------------------------------------

    private suspend fun loadSocInfoInternal() {
        val context = getApplication<Application>()
        
        // SoC Info
        val gpuDetails = GpuUtils.getGpuDetails()
        val gpuRenderer = gpuDetails?.renderer ?: getGpuModel()
        val clustersList = getClustersList()
        
        _socInfo.value = SocInfo(
            processor = SoCUtils.getCommercialName(context, gpuRenderer),
            vendor = SoCUtils.getSoCManufacturer(),
            cores = Runtime.getRuntime().availableProcessors().toString(),
            bigLittle = if (clustersList.size > 1) context.getString(R.string.clusters_format, clustersList.size) else context.getString(R.string.cluster_single),
            clusters = getClustersInfo(),
            cpuClusters = clustersList,
            family = getCpuFamilyInfo(),
            mode = if (System.getProperty("os.arch")?.contains("64") == true) "64-bit" else "32-bit",
            machine = System.getProperty("os.arch") ?: context.getString(R.string.unknown),
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: context.getString(R.string.unknown),
            instructions = getCpuInstructions(),
            revision = getCpuRevision(),
            clockSpeed = getCpuClockSpeed(),
            governor = getCpuGovernor(),
            supportedAbi = Build.SUPPORTED_ABIS.joinToString(", "),
            gpu = gpuRenderer,
            gpuVendor = gpuDetails?.vendor ?: (if (gpuRenderer.contains("Adreno", true)) "Qualcomm" else if (gpuRenderer.contains("Mali", true)) "ARM" else context.getString(R.string.unknown)),
            gpuArch = GpuUtils.getGpuArchitecture(gpuRenderer).translateUnknown(),
            gpuL2Cache = GpuUtils.getGpuL2Cache(gpuRenderer).translateUnknown(),
            gpuBusWidth = GpuUtils.getGpuBusWidth(gpuRenderer).translateUnknown(),
            openGlEs = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).deviceConfigurationInfo.glEsVersion,
            gpuFullVersion = gpuDetails?.version ?: context.getString(R.string.unknown),
            vulkanVersion = GpuUtils.getVulkanVersion(context),
            gpuExtensions = gpuDetails?.extensionsCount?.toString() ?: getGpuExtensionsCount(),
            gpuClockSpeed = GpuUtils.getGpuMaxClock(gpuRenderer).translateUnknown(),
            gpuCores = GpuUtils.getGpuCores(gpuRenderer).translateUnknown(),
            process = getProcessTech(),
            instructionSets = Build.SUPPORTED_ABIS.joinToString(", ")
        )
        
        // Update CPU info with potentially more accurate name
        _cpuInfo.value = _cpuInfo.value?.copy(
            processor = SoCUtils.getCommercialName(context, gpuRenderer)
        ) ?: CpuInfo(
            processor = SoCUtils.getCommercialName(context, gpuRenderer),
            architecture = System.getProperty("os.arch") ?: context.getString(R.string.unknown),
            cores = Runtime.getRuntime().availableProcessors(),
            supportedAbis = Build.SUPPORTED_ABIS.joinToString(", "),
            cpuGovernor = getCpuGovernor()
        )

        updateDynamicInfo()

    }

    private suspend fun loadCameraInfoInternal() {
        val context = getApplication<Application>()
        // Camera Info
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val allCameraInfo = mutableListOf<CameraInfo>()

            val standardIds = cameraManager.cameraIdList.toSet()
            val allIds = standardIds.toMutableSet()

            // Probe for hidden camera IDs (common range 0-63)
            for (i in 0..63) {
                val idStr = i.toString()
                if (!standardIds.contains(idStr)) {
                    try {
                        cameraManager.getCameraCharacteristics(idStr)
                        allIds.add(idStr)
                    } catch (_: Throwable) {}
                }
            }

            // Track physical IDs that are part of logical cameras to avoid top-level duplication
            val physicalIdsToSkip = mutableSetOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                allIds.forEach { id ->
                    try {
                        val chars = cameraManager.getCameraCharacteristics(id)
                        val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                        val isLogical = capabilities?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
                        if (isLogical) {
                            physicalIdsToSkip.addAll(chars.physicalCameraIds)
                        }
                    } catch (_: Throwable) {}
                }
            }

            val vendorConfig = readVendorCameraConfig()
            val systemProps = readCameraSystemProps()
            val supportedHardwareInfo = buildString {
                if (vendorConfig != null) {
                    appendLine("Vendor Config Found")
                    val sensorRegex = Regex("(imx|s5k|ov|lyt|gc|hi|sl|hm|jx|ar|bf|sc)[0-9a-zA-Z_]+")
                    sensorRegex.findAll(vendorConfig).take(5).forEach { 
                        appendLine("- ${it.value}")
                    }
                }
                systemProps.forEach { (k, v) ->
                    appendLine("$k: $v")
                }
            }.trim()

            allIds.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.forEach { id ->
                if (physicalIdsToSkip.contains(id)) return@forEach

                val chars = try { cameraManager.getCameraCharacteristics(id) } catch (_: Throwable) { return@forEach }
                val cameraList = mutableListOf<Pair<String, CameraCharacteristics>>()
                cameraList.add(id to chars)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        chars.physicalCameraIds.forEach { physicalId ->
                            try {
                                val physicalChars = cameraManager.getCameraCharacteristics(physicalId)
                                cameraList.add("$id > $physicalId" to physicalChars)
                            } catch (_: Throwable) {}
                        }
                    } catch (_: Throwable) {}
                }

                cameraList.forEach { (displayId, cameraChars) ->
                    try {
                        val isPhysical = displayId.contains(" > ")
                        val parentId = if (isPhysical) displayId.split(" > ").first() else null

                        val facing = when (cameraChars.get(CameraCharacteristics.LENS_FACING)) {
                            CameraCharacteristics.LENS_FACING_FRONT -> context.getString(R.string.facing_front)
                            CameraCharacteristics.LENS_FACING_BACK -> context.getString(R.string.facing_back)
                            CameraCharacteristics.LENS_FACING_EXTERNAL -> context.getString(R.string.facing_external)
                            else -> context.getString(R.string.facing_unknown)
                        }
                        
                        val focalLength = cameraChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: 0f
                        val sensorPhysSize = cameraChars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                        val activeArray = cameraChars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                        val pixelArraySize = cameraChars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)

                        var physicalMP = if (pixelArraySize != null)
                            (pixelArraySize.width.toLong() * pixelArraySize.height) / 1_000_000.0
                        else 0.0

                        val streamMap = try {
                            cameraChars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        } catch (e: Exception) {
                            null
                        }
                        
                        val outputSizes = streamMap
                            ?.getOutputSizes(android.graphics.ImageFormat.JPEG)
                            ?.sortedByDescending { it.width.toLong() * it.height }
                            ?: emptyList()

                        val maxOutputSize = outputSizes.firstOrNull()
                        val binnedMP = if (maxOutputSize != null)
                            (maxOutputSize.width.toLong() * maxOutputSize.height) / 1_000_000.0
                        else physicalMP

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val maxResConfigs = cameraChars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                            val highResJpegSizes = maxResConfigs?.getOutputSizes(android.graphics.ImageFormat.JPEG)
                            val maxHighResJpeg = highResJpegSizes?.maxByOrNull { it.width.toLong() * it.height }
                            if (maxHighResJpeg != null) {
                                val highResMp = (maxHighResJpeg.width.toLong() * maxHighResJpeg.height) / 1_000_000.0
                                if (highResMp > physicalMP) {
                                    physicalMP = highResMp
                                }
                            }
                        }

                        val binningRatio = if (binnedMP > 0) physicalMP / binnedMP else 1.0
                        val (binningFactor, binningType) = when {
                            binningRatio >= 8.5  -> Pair("3x3", "Nona-Bayer")
                            binningRatio >= 3.5  -> Pair("4x4", "Quad-Bayer")
                            binningRatio >= 1.9  -> Pair("2x2", "Quad-Bayer")
                            else                 -> Pair("none", "Full Readout")
                        }

                        val apiBinning = if (android.os.Build.VERSION.SDK_INT >= 31) {
                            cameraChars.get(CameraCharacteristics.SENSOR_INFO_BINNING_FACTOR)
                        } else null

                        val allPossibleSizes = mutableListOf<Size>()
                        allPossibleSizes.addAll(outputSizes)
                        pixelArraySize?.let { allPossibleSizes.add(it) }
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            val maxRes = cameraChars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE_MAXIMUM_RESOLUTION)
                            if (maxRes != null) allPossibleSizes.add(Size(maxRes.width(), maxRes.height()))
                            
                            cameraChars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
                                ?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.let { allPossibleSizes.addAll(it) }
                        }

                        val absoluteMaxRes = allPossibleSizes.maxByOrNull { it.width.toLong() * it.height.toLong() }
                        val absoluteMaxMP = absoluteMaxRes?.let {
                            (it.width.toLong() * it.height) / 1_000_000.0
                        } ?: physicalMP
                        
                        val hasHiRes = absoluteMaxRes?.let { (it.width.toLong() * it.height / 1_000_000.0) > (binnedMP * 1.5) } ?: false

                        val resolution = maxOutputSize?.let { 
                            "%.1f MP (%dx%d)".format(Locale.US, binnedMP, it.width, it.height)
                        } ?: "N/A"

                        val infoVersion = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            cameraChars.get(CameraCharacteristics.INFO_VERSION) ?: getApplication<Application>().getString(R.string.unknown)
                        } else "Unknown"



                        val activeWidth = if (sensorPhysSize != null && activeArray != null && pixelArraySize != null) {
                            sensorPhysSize.width * (activeArray.width().toFloat() / pixelArraySize.width)
                        } else sensorPhysSize?.width ?: 0f

                        val sensorsFound = mutableSetOf<String>()
                        if (infoVersion != "Unknown") sensorsFound.add(infoVersion)
                        if (vendorConfig != null) {
                            Regex("(imx|s5k|ov|lyt|gc|hi|sl|hm|jx|ar|bf|sc)[0-9a-zA-Z_]+").findAll(vendorConfig).forEach { sensorsFound.add(it.value) }
                        }
                        systemProps.values.forEach { v ->
                            Regex("(imx|s5k|ov|lyt|gc|hi|sl|hm|jx|ar|bf|sc)[0-9a-zA-Z_]+").findAll(v).forEach { sensorsFound.add(it.value) }
                        }

                        val physicalSensors = sensorsFound.mapNotNull { rawCode ->
                            SpecLoader.getSensorDetails(context, rawCode)?.let { details ->
                                PhysicalSensor(
                                    model = details.first,
                                    manufacturer = SpecLoader.getCameraVendor(context, details.first) ?: getApplication<Application>().getString(R.string.unknown),
                                    resolution = details.second,
                                    role = if (facing.equals(context.getString(R.string.facing_front), true)) context.getString(R.string.role_front) else {
                                        val equiv = if (activeWidth > 0) ((36.0 / activeWidth) * focalLength.toDouble()) else 30.0
                                        when {
                                            equiv < 16.0 -> context.getString(R.string.role_ultra_wide)
                                            equiv < 20.0 -> context.getString(R.string.role_wide)
                                            equiv < 35.0 -> context.getString(R.string.role_main)
                                            equiv < 70.0 -> context.getString(R.string.role_telephoto)
                                            else -> context.getString(R.string.role_super_tele)
                                        }
                                    }
                                )
                            }
                        }.distinctBy { it.model }

                        val finalPhysicalMP = if (absoluteMaxMP > physicalMP) absoluteMaxMP else physicalMP

                        val sensorResolution = if (finalPhysicalMP > binnedMP) {
                            "%.1f MP".format(Locale.US, finalPhysicalMP) + (absoluteMaxRes?.let { " (%dx%d)".format(it.width, it.height) } ?: "")
                        } else resolution

                        val aperture = cameraChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull()?.let { "f/$it" } ?: "N/A"
                        val sensorSize = sensorPhysSize

                        val activeHeight = if (sensorPhysSize != null && activeArray != null && pixelArraySize != null) {
                            sensorPhysSize.height * (activeArray.height().toFloat() / pixelArraySize.height)
                        } else sensorPhysSize?.height ?: 0f
                        
                        val activeDiag = Math.sqrt((activeWidth * activeWidth + activeHeight * activeHeight).toDouble())
                        val hView = sensorSize?.let { 2 * Math.atan(it.width / (2.0 * focalLength)) * 180 / Math.PI } ?: 0.0
                        val dView = sensorSize?.let { 2 * Math.atan(activeDiag / (2.0 * focalLength)) * 180 / Math.PI } ?: 0.0

                        val cropFactor = if (activeWidth > 0) 36.0 / activeWidth else 0.0
                        val focal35mm = if (focalLength > 0 && cropFactor > 0) {
                            val calculated = focalLength * cropFactor
                            if (calculated > 21.5 && calculated < 22.5) "23.0 mm"
                            else "%.1f mm".format(Locale.US, calculated)
                        } else "N/A"

                        val expRange = cameraChars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)?.let { "${it.lower}-${it.upper}" } ?: "N/A"
                        
                        val shutterRange = cameraChars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
                        val shutterSpeedStr = if (shutterRange != null) {
                            val min = 1.0 / (shutterRange.upper / 1_000_000_000.0)
                            val max = shutterRange.lower / 1_000_000_000.0
                            "1/%.0f - %.0f s".format(Locale.US, min, max)
                        } else "N/A"

                        val videoStab = cameraChars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON) ?: false
                        val opticalStab = cameraChars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) ?: false
                        val aeLock = cameraChars.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) ?: false
                        val awbLock = cameraChars.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE) ?: false

                        val caps = mutableListOf<String>()
                        cameraChars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.forEach { cap ->
                            when (cap) {
                                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> caps.add(context.getString(R.string.cam_cap_manual_sensor))
                                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> caps.add(context.getString(R.string.cam_cap_manual_post))
                                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW -> caps.add(context.getString(R.string.cam_cap_raw))
                                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> caps.add(context.getString(R.string.cam_cap_burst))
                                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> caps.add(context.getString(R.string.cam_cap_logical_multi))
                            }
                        }

                        val aeModes = cameraChars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)?.map { mode ->
                            when (mode) {
                                CameraMetadata.CONTROL_AE_MODE_OFF -> context.getString(R.string.cam_ae_off)
                                CameraMetadata.CONTROL_AE_MODE_ON -> context.getString(R.string.cam_ae_auto)
                                CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH -> context.getString(R.string.cam_ae_auto_flash)
                                CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH -> context.getString(R.string.cam_ae_always_flash)
                                CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE -> context.getString(R.string.cam_ae_redeye)
                                else -> context.getString(R.string.cam_other)
                            }
                        } ?: emptyList()

                        val focusModesList = cameraChars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.map { mode ->
                            when (mode) {
                                CameraMetadata.CONTROL_AF_MODE_OFF -> context.getString(R.string.cam_af_manual)
                                CameraMetadata.CONTROL_AF_MODE_AUTO -> context.getString(R.string.cam_af_auto)
                                CameraMetadata.CONTROL_AF_MODE_MACRO -> context.getString(R.string.cam_af_macro)
                                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> context.getString(R.string.cam_af_continuous_video)
                                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> context.getString(R.string.cam_af_continuous_picture)
                                CameraMetadata.CONTROL_AF_MODE_EDOF -> context.getString(R.string.cam_af_edof)
                                else -> context.getString(R.string.cam_other)
                            }
                        } ?: emptyList()

                        val awbModes = cameraChars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.map { mode ->
                            when (mode) {
                                CameraMetadata.CONTROL_AWB_MODE_OFF -> context.getString(R.string.cam_awb_off)
                                CameraMetadata.CONTROL_AWB_MODE_AUTO -> context.getString(R.string.cam_awb_auto)
                                CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT -> context.getString(R.string.cam_awb_incandescent)
                                CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT -> context.getString(R.string.cam_awb_fluorescent)
                                CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT -> context.getString(R.string.cam_awb_warm_fluorescent)
                                CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT -> context.getString(R.string.cam_awb_daylight)
                                CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> context.getString(R.string.cam_awb_cloudy)
                                CameraMetadata.CONTROL_AWB_MODE_TWILIGHT -> context.getString(R.string.cam_awb_twilight)
                                CameraMetadata.CONTROL_AWB_MODE_SHADE -> context.getString(R.string.cam_awb_shade)
                                else -> context.getString(R.string.cam_other)
                            }
                        } ?: emptyList()

                        val sceneModes = cameraChars.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)?.map { mode ->
                            when (mode) {
                                CameraMetadata.CONTROL_SCENE_MODE_DISABLED -> context.getString(R.string.cam_scene_off)
                                CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY -> context.getString(R.string.cam_scene_face_priority)
                                CameraMetadata.CONTROL_SCENE_MODE_ACTION -> context.getString(R.string.cam_scene_action)
                                CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT -> context.getString(R.string.cam_scene_portrait)
                                CameraMetadata.CONTROL_SCENE_MODE_LANDSCAPE -> context.getString(R.string.cam_scene_landscape)
                                CameraMetadata.CONTROL_SCENE_MODE_NIGHT -> context.getString(R.string.cam_scene_night)
                                CameraMetadata.CONTROL_SCENE_MODE_NIGHT_PORTRAIT -> context.getString(R.string.cam_scene_night_portrait)
                                CameraMetadata.CONTROL_SCENE_MODE_THEATRE -> context.getString(R.string.cam_scene_theatre)
                                CameraMetadata.CONTROL_SCENE_MODE_BEACH -> context.getString(R.string.cam_scene_beach)
                                CameraMetadata.CONTROL_SCENE_MODE_SNOW -> context.getString(R.string.cam_scene_snow)
                                CameraMetadata.CONTROL_SCENE_MODE_SUNSET -> context.getString(R.string.cam_scene_sunset)
                                CameraMetadata.CONTROL_SCENE_MODE_STEADYPHOTO -> context.getString(R.string.cam_scene_steady_photo)
                                CameraMetadata.CONTROL_SCENE_MODE_FIREWORKS -> context.getString(R.string.cam_scene_fireworks)
                                CameraMetadata.CONTROL_SCENE_MODE_SPORTS -> context.getString(R.string.cam_scene_sports)
                                CameraMetadata.CONTROL_SCENE_MODE_PARTY -> context.getString(R.string.cam_scene_party)
                                CameraMetadata.CONTROL_SCENE_MODE_CANDLELIGHT -> context.getString(R.string.cam_scene_candlelight)
                                CameraMetadata.CONTROL_SCENE_MODE_BARCODE -> context.getString(R.string.cam_scene_barcode)
                                CameraMetadata.CONTROL_SCENE_MODE_HDR -> context.getString(R.string.cam_scene_hdr)
                                else -> context.getString(R.string.cam_other)
                            }
                        } ?: emptyList()

                        val level = cameraChars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                        val levelName = when (level) {
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "legacy(0)"
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "limited(1)"
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "full(2)"
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "level_3(3)"
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "external(4)"
                            else -> getApplication<Application>().getString(R.string.unknown)
                        }

                        val pixelSizeRaw = if (sensorSize != null && absoluteMaxRes != null) (sensorSize.width / absoluteMaxRes.width * 1000) else 0f
                        
                        val videoSizes = streamMap?.getOutputSizes(android.media.MediaRecorder::class.java)
                        val videoRes = videoSizes?.take(2)?.joinToString("\n") { size ->
                            val name = when {
                                size.width >= 3840 -> "4K"
                                size.width >= 1920 -> "Full HD"
                                size.width >= 1280 -> "HD"
                                else -> "SD"
                            }
                            "$name ${size.width}x${size.height}"
                        } ?: "N/A"

                        val formats = mutableListOf<String>()
                        streamMap?.outputFormats?.forEach { format ->
                            when (format) {
                                android.graphics.ImageFormat.JPEG -> formats.add("JPEG")
                                android.graphics.ImageFormat.RAW_SENSOR -> formats.add("RAW_SENSOR")
                                android.graphics.ImageFormat.RAW10 -> formats.add("RAW10")
                                android.graphics.ImageFormat.YUV_420_888 -> formats.add("YUV_420_888")
                                else -> {}
                            }
                        }

                        allCameraInfo.add(CameraInfo(
                            id = displayId,
                            facing = facing,
                            resolution = resolution,
                            sensorResolution = sensorResolution,
                            sensorModel = infoVersion,
                            supportedHardware = supportedHardwareInfo.ifEmpty { "N/A" },
                            physicalSensors = physicalSensors,
                            physicalMP = finalPhysicalMP,
                            binnedMP = binnedMP,
                            binningFactor = apiBinning?.let { "${it.width}x${it.height}" } ?: binningFactor,
                            binningType = binningType,
                            hasHighResMode = hasHiRes,
                            maxResolution = sensorResolution,
                            aperture = aperture,
                            focalLength = "%.2f mm".format(Locale.US, focalLength),
                            focalLength35mm = focal35mm,
                            sensorSize = sensorSize?.let { "%.2fx%.2f".format(Locale.US, it.width, it.height) } ?: "N/A",
                            diagonal = if (activeDiag > 0) "%.2f mm".format(Locale.US, activeDiag) else "N/A",
                            pixelSize = if (pixelSizeRaw > 0) "~%.2f µm".format(Locale.US, pixelSizeRaw) else "N/A",
                            zoom = cameraChars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)?.let { "${it}x" } ?: "1.0x",
                            isoRange = cameraChars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { "${it.lower}-${it.upper}" } ?: "N/A",
                            exposureRange = expRange,
                            shutterSpeedRange = shutterSpeedStr,
                            videoStabilization = videoStab,
                            opticalStabilization = opticalStab,
                            autoExposureLock = aeLock,
                            autoWhiteBalanceLock = awbLock,
                            capabilities = caps,
                            exposureModes = aeModes,
                            focusModesList = focusModesList,
                            whiteBalanceModes = awbModes,
                            sceneModes = sceneModes,
                            flash = if (cameraChars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) "yes" else "no",
                            orientation = cameraChars.get(CameraCharacteristics.SENSOR_ORIENTATION)?.toString() ?: "N/A",
                            camera2ApiLevel = levelName,
                            videoCapabilities = videoRes,
                            imageFormats = formats.joinToString(", "),
                            angleOfView = if (dView > 0) "%.1f° (D)\n%.1f° (H)".format(Locale.US, dView, hView) else "N/A",
                            cropFactor = if (cropFactor > 0) "%.1fx".format(Locale.US, cropFactor) else "N/A",
                            vendor = getApplication<Application>().getString(R.string.unknown),
                            isPhysical = isPhysical,
                            parentLogicalId = parentId,
                            focusModes = cameraChars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.joinToString(", ") { mode ->
                                when (mode) {
                                    CameraMetadata.CONTROL_AF_MODE_AUTO -> "auto"
                                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "continuous-picture"
                                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "continuous-video"
                                    CameraMetadata.CONTROL_AF_MODE_EDOF -> "edof"
                                    CameraMetadata.CONTROL_AF_MODE_MACRO -> "macro"
                                    CameraMetadata.CONTROL_AF_MODE_OFF -> "infinity/off"
                                    else -> "other"
                                }
                            } ?: "N/A",
                            colorFilter = when (cameraChars.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)) {
                                CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB -> "RGGB"
                                CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG -> "GRBG"
                                CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG -> "GBRG"
                                CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR -> "BGGR"
                                else -> "N/A"
                            }
                        ))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val seenHardwareSignatures = mutableSetOf<String>()
            
            val sortedCameras = allCameraInfo.sortedBy { cam ->
                when {
                    cam.facing.equals("BACK", ignoreCase = true) -> 0
                    cam.facing.equals("FRONT", ignoreCase = true) -> 1
                    else -> 2
                }
            }

            _cameras.value = sortedCameras.filter { cam ->
                if (cam.physicalMP < 0.5) return@filter false
                if (cam.resolution == "N/A") return@filter false
                val signature = "${"%.2f".format(cam.physicalMP)}_${cam.focalLength}_${cam.sensorSize}"
                
                if (seenHardwareSignatures.contains(signature)) {
                    if (cam.facing.equals("FRONT", ignoreCase = true)) {
                        return@filter true
                    }
                    return@filter false
                }
                seenHardwareSignatures.add(signature)
                true
            }
        }


    }

    private suspend fun loadUsbInfoInternal() {
        val context = getApplication<Application>()
        // USB Info
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        _usbDevices.value = usbManager.deviceList.values.map {
            UsbInfo(
                name = it.deviceName,
                vendorId = "0x%04X".format(it.vendorId),
                productId = "0x%04X".format(it.productId),
                deviceClass = getUsbClass(it.deviceClass),
                manufacturerName = try { it.manufacturerName } catch (e: Exception) { null },
                productName = try { it.productName } catch (e: Exception) { null },
                serialNumber = try { it.serialNumber } catch (e: Exception) { null }
            )
        }
        updateUsbStatus(null)

    }

    private suspend fun loadCodecsInfoInternal() {
        // Codecs
        _codecs.value = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.map {
            CodecInfo(it.name, it.supportedTypes.joinToString(", "), if (it.isEncoder) "Encoder" else "Decoder")
        }

    }

    private suspend fun loadInstalledAppsInfoInternal() {
        val context = getApplication<Application>()
        // Installed Apps
        val pm = context.packageManager
        val apps = pm.getInstalledPackages(PackageManager.GET_META_DATA).mapNotNull { pkg ->
            pkg.applicationInfo?.let { appInfo ->
                val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME
                } else {
                    false
                }
                AppEntry(
                    name = appInfo.loadLabel(pm).toString(),
                    packageName = pkg.packageName,
                    version = pkg.versionName ?: getApplication<Application>().getString(R.string.unknown),
                    sdk = appInfo.targetSdkVersion.toString(),
                    isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                    isGame = isGame
                )
            }
        }
        _installedApps.value = apps
        cachedAppsCount = apps.size
    }

    private fun loadDrmInfo() {
        val results = mutableListOf<DrmSchemeInfo>()
        val schemes = mapOf(
            "Widevine" to UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"),
            "ClearKey" to UUID.fromString("e2719d58-5581-4f19-bc8d-0139f9ff3a61"),
            "PlayReady" to UUID.fromString("9a04f079-9840-4286-ab92-e65be0885f95")
        )
        
        var widevineLevelCached = "N/A"
        
        schemes.forEach { (name, uuid) ->
            if (MediaDrm.isCryptoSchemeSupported(uuid)) {
                var drm: MediaDrm? = null
                try {
                    drm = MediaDrm(uuid)
                    
                    fun getProp(p: String): String {
                        return try {
                            val s = drm?.getPropertyString(p)
                            if (s.isNullOrBlank()) "N/A" else s
                        } catch (e: Exception) {
                            "N/A"
                        }
                    }

                    val deviceIdRaw = try { drm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID) } catch (e: Exception) { null }
                    val deviceId = deviceIdRaw?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) } ?: "N/A"

                    val slKeys = if (name.contains("Widevine")) listOf("securityLevel", "SecurityLevel", "security_level", "level") else emptyList()
                    val securityLevel = slKeys.asSequence().map { getProp(it) }.firstOrNull { it != "N/A" } ?: "N/A"

                    results.add(DrmSchemeInfo(
                        name = if (name.startsWith("ClearKey")) "ClearKey CDM" else name,
                        vendor = getProp(MediaDrm.PROPERTY_VENDOR).let { if (it == "N/A" || it == "Unknown") getProp("vendor") else it }.let { if (it == "com.google.android.widevine.lazy") "Google" else it },
                        version = getProp(MediaDrm.PROPERTY_VERSION).let { if (it == "N/A" || it == "Unknown") getProp("version") else it },
                        description = getProp(MediaDrm.PROPERTY_DESCRIPTION).let { if (it == "N/A" || it == "Unknown") getProp("description") else it },
                        algorithms = getProp(MediaDrm.PROPERTY_ALGORITHMS).let { if (it == "N/A" || it == "Unknown") getProp("algorithms") else it },
                        securityLevel = securityLevel,
                        deviceId = deviceId,
                        maxHdcpLevel = getProp("maxHdcpLevel"),
                        currentHdcpLevel = getProp("hdcpLevel"),
                        systemId = getProp("systemId")
                    ))
                    
                    if (name == "Widevine") {
                        if (securityLevel != "N/A") {
                            widevineLevelCached = securityLevel
                        } else if (results.last().version != "N/A") {
                            widevineLevelCached = getApplication<Application>().getString(R.string.supported)
                        }
                    }
                } catch (e: Exception) {
                    // Log or handle construction failure (e.g. resources busy)
                } finally {
                    try { drm?.release() } catch (e: Exception) {}
                }
            }
        }
        
        // Final fallback to system properties for the main dashboard if all MediaDrm attempts failed
        if (widevineLevelCached == "N/A") {
            val prop = getSystemProperty("ro.widevine.level") ?: getSystemProperty("vendor.widevine.level")
            if (!prop.isNullOrEmpty()) widevineLevelCached = prop
        }
        
        this.widevineLevelCached = widevineLevelCached
        _drmInfos.value = results
    }

    fun checkForUpdates() {
        // Implementation for APKPure update check
    }

    private fun updateThermalInfo() {
        val thermalDir = File("/sys/class/thermal/")
        if (thermalDir.exists()) {
            val zones = thermalDir.listFiles { _, name -> name.startsWith("thermal_zone") } ?: emptyArray()
            _thermalZones.value = zones.map { zone ->
                val type = try { File(zone, "type").readText().trim() } catch (e: Exception) { getApplication<Application>().getString(R.string.unknown) }
                val temp = try {
                    val t = File(zone, "temp").readText().trim().toDouble()
                    val tempC = if (t > 1000) t / 1000.0 else t
                    com.example.relab_tool.utils.UnitFormatter.formatTemperature(getApplication(), tempC.toFloat(), includeSpace = true)
                } catch (e: Exception) { "N/A" }
                ThermalInfo(type, temp)
            }.filter { it.temperature != "N/A" }
        }
    }

    private fun getClustersList(): List<CpuCluster> {
        return try {
            val context = getApplication<Application>()
            val socName = SoCUtils.getCommercialName(context)
            val cpuDir = File("/sys/devices/system/cpu/")
            val cpuFiles = cpuDir.listFiles { _, name -> name.matches(Regex("cpu[0-9]+")) } ?: emptyArray()
            
            // Core Identity -> Core Info
            data class CoreIdentity(val implementer: String, val part: String, val maxFreq: Long, val capacity: Long)
            val groups = mutableMapOf<CoreIdentity, MutableList<File>>()

            cpuFiles.forEach { file ->
                try {
                    val maxFreq = File(file, "cpufreq/cpuinfo_max_freq").readText().trim().toLong()
                    
                    var implementer = ""
                    var part = ""
                    var capacity = 0L
                    
                    try {
                        val capacityFile = File(file, "cpu_capacity")
                        if (capacityFile.exists()) {
                            capacity = capacityFile.readText().trim().toLong()
                        }
                    } catch (e: Exception) {}

                    val cpuId = file.name.substring(3).toInt()
                    RandomAccessFile("/proc/cpuinfo", "r").use { reader ->
                        var line: String?
                        var currentProc = -1
                        while (reader.readLine().also { line = it } != null) {
                            if (line?.startsWith("processor") == true) {
                                currentProc = line!!.split(":")[1].trim().toInt()
                            }
                            if (currentProc == cpuId) {
                                if (line?.contains("CPU implementer") == true) implementer = line!!.split(":")[1].trim()
                                if (line?.contains("CPU part") == true) part = line!!.split(":")[1].trim()
                            }
                            if (implementer.isNotEmpty() && part.isNotEmpty() && currentProc == cpuId) break
                        }
                    }

                    val identity = CoreIdentity(implementer, part, maxFreq, capacity)
                    groups.getOrPut(identity) { mutableListOf() }.add(file)
                } catch (e: Exception) {}
            }
            
            if (groups.isEmpty()) return emptyList()
            
            val sortedClusters = groups.entries.sortedWith(
                compareByDescending<Map.Entry<CoreIdentity, MutableList<File>>> { it.key.maxFreq }
                .thenByDescending { it.key.capacity }
                .thenByDescending { it.value.size }
            )

            sortedClusters.mapIndexed { index, entry ->
                val identity = entry.key
                val cores = entry.value
                
                var minFreq = getApplication<Application>().getString(R.string.unknown)
                try {
                    val min = File(cores.first(), "cpufreq/cpuinfo_min_freq").readText().trim().toLong() / 1000
                    minFreq = "$min MHz"
                } catch (e: Exception) {}

                val arch = if (socName.contains("Elite", ignoreCase = true) || socName.contains("Gen 5", ignoreCase = true)) {
                    val baseArch = SpecLoader.getArmCortexName(context, identity.part) ?: 
                                  SpecLoader.getCpuFamily(context, identity.part, identity.implementer) ?: "Oryon"
                    if (baseArch.contains("Cortex", ignoreCase = true) || baseArch.contains("Oryon", ignoreCase = true)) "Oryon" else baseArch
                } else {
                    SpecLoader.getArmCortexName(context, identity.part) ?: 
                    SpecLoader.getCpuFamily(context, identity.part, identity.implementer) ?: "Cortex"
                }
                
                val name = when {
                    sortedClusters.size >= 3 -> {
                        when (index) {
                            0 -> "Prime"
                            1 -> "Gold"
                            else -> "Silver"
                        }
                    }
                    sortedClusters.size == 2 -> {
                        if (index == 0) "Gold" else "Silver"
                    }
                    else -> "Core"
                }

                val coreIndices = cores.mapNotNull { file ->
                    file.name.substringAfter("cpu").toIntOrNull()
                }.sorted()

                CpuCluster(
                    id = index,
                    name = name,
                    architecture = arch,
                    coreCount = cores.size,
                    minFreq = minFreq,
                    maxFreq = "${identity.maxFreq / 1000} MHz",
                    coreIndices = coreIndices
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getClustersInfo(): String {
        val clusters = getClustersList()
        if (clusters.isEmpty()) {
            val cpuCount = Runtime.getRuntime().availableProcessors()
            return "$cpuCount x Unknown"
        }
        return clusters.joinToString(", ") { "${it.coreCount} x ${it.maxFreq}" }
    }

    private fun getCpuFamilyInfo(): String {
        val context = getApplication<Application>()
        try {
            RandomAccessFile("/proc/cpuinfo", "r").use { reader ->
                var line: String?
                var implementer = ""
                var part = ""
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("CPU implementer") == true) implementer = line!!.split(":")[1].trim()
                    if (line?.contains("CPU part") == true) part = line!!.split(":")[1].trim()
                    
                    if (implementer.isNotEmpty() && part.isNotEmpty()) {
                        val family = SpecLoader.getCpuFamily(context, part, implementer)
                        if (family != null) return family
                        implementer = ""
                        part = ""
                    }
                }
            }
        } catch (e: Exception) {}
        return "Cortex"
    }

    private fun getCpuInstructions(): String {
        val result = mutableSetOf<String>()
        
        try {
            RandomAccessFile("/proc/cpuinfo", "r").use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("Features") == true) {
                        val features = line!!.split(":")[1].trim()
                        if (features.isNotEmpty()) {
                            result.addAll(features.split(Regex("\\s+")))
                        }
                    }
                }
            }
        } catch (e: Exception) { }
        
        Build.SUPPORTED_ABIS.forEach { result.add(it) }
        if (Build.CPU_ABI.isNotEmpty() && Build.CPU_ABI != "unknown") result.add(Build.CPU_ABI)
        if (Build.CPU_ABI2.isNotEmpty() && Build.CPU_ABI2 != "unknown") result.add(Build.CPU_ABI2)
        
        System.getProperty("os.arch")?.let { if (it.isNotEmpty()) result.add(it) }
        
        val finalString = result.filter { it.isNotBlank() }.joinToString(" ")
        return if (finalString.isNotEmpty()) finalString else "Unknown"
    }

    private fun getCpuRevision(): String {
        return try {
            RandomAccessFile("/proc/cpuinfo", "r").use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line?.contains("CPU revision") == true) return "r" + line!!.split(":")[1].trim()
                }
            }
            "Unknown"
        } catch (e: Exception) { getApplication<Application>().getString(R.string.unknown) }
    }

    private fun getCpuClockSpeed(): String {
        return try {
            val cpuDir = File("/sys/devices/system/cpu/")
            val cpuFiles = cpuDir.listFiles { _, name -> name.matches(Regex("cpu[0-9]+")) } ?: emptyArray()
            var absoluteMin = Long.MAX_VALUE
            var absoluteMax = Long.MIN_VALUE
            
            cpuFiles.forEach { file ->
                try {
                    val min = File(file, "cpufreq/cpuinfo_min_freq").readText().trim().toLong() / 1000
                    val max = File(file, "cpufreq/cpuinfo_max_freq").readText().trim().toLong() / 1000
                    if (min < absoluteMin) absoluteMin = min
                    if (max > absoluteMax) absoluteMax = max
                } catch (e: Exception) {}
            }
            
            if (absoluteMin == Long.MAX_VALUE) return getApplication<Application>().getString(R.string.unknown)
            "$absoluteMin - $absoluteMax MHz"
        } catch (e: Exception) { getApplication<Application>().getString(R.string.unknown) }
    }

    private fun getGpuExtensionsCount(): String {
        return "N/A"
    }

    private fun getGpuModel(): String {
        return if (Build.HARDWARE.contains("qcom", true)) "Qualcomm" else "Generic"
    }

    private fun getProcessTech(): String {
        val context = getApplication<Application>()
        val socModel = SoCUtils.getSoCModel()
        
        val dbProcess = SpecLoader.getSoCProcess(context, socModel)
        if (dbProcess != null) return dbProcess

        val props = arrayOf(
            "ro.soc.process", "vendor.soc.process", "ro.cpu.process", 
            "ro.chipname", "ro.board.platform", "ro.hardware"
        )
        for (prop in props) {
            val value = getSystemProperty(prop)
            if (!value.isNullOrEmpty()) {
                if (value.contains(Regex("[0-9]+ ?nm"))) {
                    return value.substringAfterLast(":").trim()
                }
            }
        }

        return "N/A"
    }

    private fun getCpuGovernor(): String {
        return try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getBatteryCapacity(): String {
        val context = getApplication<Application>()
        try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfile = powerProfileClass.getConstructor(Context::class.java).newInstance(context)
            val batteryCapacity = powerProfileClass.getMethod("getBatteryCapacity").invoke(powerProfile) as Double
            if (batteryCapacity > 0) {
                return "${batteryCapacity.toInt()} mAh (Estimated)"
            }
        } catch (e: Exception) {}

        val sysfsFiles = arrayOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/battery/energy_full_design",
            "/sys/class/power_supply/bms/charge_full_design"
        )
        for (path in sysfsFiles) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val capacityUah = file.readText().trim().toLongOrNull() ?: continue
                    if (capacityUah > 0) {
                        val capacityMah = if (capacityUah > 100_000) capacityUah / 1000 else capacityUah
                        if (capacityMah in 500..20000) {
                            return "$capacityMah mAh (Estimated)"
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        return "N/A"
    }

    private fun getDensityString(metrics: DisplayMetrics): String {
        return "${metrics.densityDpi} dpi"
    }

    private fun getPhysicalSize(metrics: DisplayMetrics): String {
        return try {
            val widthInches = metrics.widthPixels / (if (metrics.xdpi < 1) metrics.densityDpi.toFloat() else metrics.xdpi)
            val heightInches = metrics.heightPixels / (if (metrics.ydpi < 1) metrics.densityDpi.toFloat() else metrics.ydpi)
            val diagonalInches = Math.sqrt(Math.pow(widthInches.toDouble(), 2.0) + Math.pow(heightInches.toDouble(), 2.0))
            if (diagonalInches in 2.0..15.0) {
                String.format(Locale.US, "%.2f\"", diagonalInches)
            } else {
                "N/A"
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun getBrightness(): String {
        return try {
            val context = getApplication<Application>()
            val brightness = android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS)
            val pct = (brightness * 100) / 255
            "$pct%"
        } catch (e: Exception) {
            "N/A"
        }
    }

    private fun getGsfId(context: Context): String? {
        val uri = android.net.Uri.parse("content://com.google.android.gsf.gservices")
        val key = "android_id"
        return try {
            context.contentResolver.query(uri, null, null, arrayOf(key), null)?.use { cursor ->
                if (cursor.moveToFirst() && cursor.columnCount >= 2) {
                    val idVal = cursor.getString(1)
                    try {
                        java.lang.Long.toHexString(idVal.toLong()).uppercase()
                    } catch (e: Exception) {
                        idVal
                    }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getBluetoothVersion(): String {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        if (adapter == null) return "N/A"
        return when {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && adapter.isLeAudioSupported == 0 -> "5.3+"
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && adapter.isLe2MPhySupported -> "5.0+"
            pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE) -> "4.0+"
            pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH) -> "2.1+"
            else -> "N/A"
        }
    }

    private fun getHdrSupport(): String {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
                val hdrCapabilities = display?.hdrCapabilities
                val types = hdrCapabilities?.supportedHdrTypes
                if (types != null && types.isNotEmpty()) {
                    val list = mutableListOf<String>()
                    for (type in types) {
                        when (type) {
                            Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> list.add("Dolby Vision")
                            Display.HdrCapabilities.HDR_TYPE_HDR10 -> list.add("HDR10")
                            Display.HdrCapabilities.HDR_TYPE_HLG -> list.add("HLG")
                            4 -> list.add("HDR10+") // Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS
                        }
                    }
                    if (list.isNotEmpty()) {
                        return list.joinToString(" / ")
                    }
                }
            } catch (e: Exception) {}
        }
        return "N/A"
    }

    private fun getBatteryVoltageMv(intent: Intent?): Int {
        val raw = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        return if (raw in 1..10) raw * 1000 else raw
    }

    private fun checkAndCalculateBatteryWearFallback() {
    }

    private fun getBatteryCurrentMa(): Long {
        val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val value = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            
            // Check for common sentinel values indicating unsupported feature
            if (value == Long.MAX_VALUE || value == Long.MIN_VALUE || 
                value == Integer.MAX_VALUE.toLong() || value == Integer.MIN_VALUE.toLong()) {
                return Long.MAX_VALUE
            }
            
            val absVal = Math.abs(value)
            if (absVal > 100000000L) { // Over 100,000A is impossible/invalid
                return Long.MAX_VALUE
            }
            
            if (absVal > 20000) {
                value / 1000
            } else {
                value
            }
        } else Long.MAX_VALUE
    }

    private fun getBatteryCurrent(): String {
        val current = getBatteryCurrentMa()
        return if (current != Long.MAX_VALUE) "$current mA" else "N/A"
    }

    private fun getBatteryChargeCounter(): String {
        val batteryManager = getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val value = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        if (value == Long.MIN_VALUE || value == Long.MAX_VALUE || value <= 0) {
            return "N/A"
        }
        val valueMah = value / 1000
        return "$valueMah mAh"
    }

    fun formatSize(size: Long): String {
        return android.text.format.Formatter.formatFileSize(getApplication(), size)
    }

    private fun getMemoryPageSize(): String {
        return try {
            val pageSize = Os.sysconf(OsConstants._SC_PAGESIZE)
            val kb = pageSize / 1024
            "$kb KB"
        } catch (e: Exception) {
            "4 KB"
        }
    }

    private fun getBluetoothAudioCodec(): String? {
        val a2dp = bluetoothA2dp ?: return null
        val context = getApplication<Application>()
        val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasConnectPermission) return null

        try {
            val activeDeviceMethod = a2dp.javaClass.getMethod("getActiveDevice")
            val activeDevice = activeDeviceMethod.invoke(a2dp) as? BluetoothDevice
            val devices = if (activeDevice != null) listOf(activeDevice) else a2dp.connectedDevices
            
            for (device in devices) {
                val codecStatusMethod = a2dp.javaClass.getMethod("getCodecStatus", BluetoothDevice::class.java)
                val codecStatus = codecStatusMethod.invoke(a2dp, device) ?: continue
                
                val codecConfigMethod = codecStatus.javaClass.getMethod("getCodecConfig")
                val codecConfig = codecConfigMethod.invoke(codecStatus) ?: continue
                
                val codecTypeMethod = codecConfig.javaClass.getMethod("getCodecType")
                val codecType = codecTypeMethod.invoke(codecConfig) as Int
                
                return when (codecType) {
                    0 -> "SBC"
                    1 -> "AAC"
                    2 -> "aptX"
                    3 -> "aptX HD"
                    4 -> "LDAC"
                    5 -> "Opus"
                    else -> "Unknown ($codecType)"
                }
            }
        } catch (e: Exception) {}
        return null
    }

    fun updateAudioInfo() {
        val context = getApplication<Application>()
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val route = devices.firstOrNull { it.isSink }?.let { dev ->
            when (dev.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> context.getString(R.string.audio_speaker)
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> context.getString(R.string.audio_earpiece)
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> context.getString(R.string.audio_headphones)
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> context.getString(R.string.audio_bluetooth)
                AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> context.getString(R.string.audio_usb)
                else -> context.getString(R.string.audio_stereo)
            }
        } ?: context.getString(R.string.audio_stereo)

        val latency = try {
            val getLatencyMethod = AudioManager::class.java.getMethod("getOutputLatency", Int::class.javaPrimitiveType)
            getLatencyMethod.invoke(am, AudioManager.STREAM_MUSIC) as Int
        } catch (e: Exception) {
            -1
        }
        val audioLatency = if (latency > 0) "$latency ms" else "N/A"

        val sampleRateValue = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
        val sampleRateKhz = (sampleRateValue?.toIntOrNull() ?: 48000) / 1000f
        
        val lowLatency = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)
        val proAudio = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)
        val bitDepth = if (proAudio || lowLatency) "24-bit" else "16-bit"

        val bufferSizeValue = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: context.getString(R.string.unknown)

        val audioCodecs: String
        val videoCodecs: String
        val cachedAudio = cachedAudioCodecs
        val cachedVideo = cachedVideoCodecs
        if (cachedAudio != null && cachedVideo != null) {
            audioCodecs = cachedAudio
            videoCodecs = cachedVideo
        } else {
            val codecInfos = try { MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos } catch (e: Exception) { emptyArray() }
            audioCodecs = codecInfos.filter { !it.isEncoder && it.supportedTypes.any { t -> t.startsWith("audio/") } }
                .flatMap { it.supportedTypes.toList() }
                .map { it.substringAfter("audio/").uppercase() }
                .distinct().take(10).joinToString(", ")
            videoCodecs = codecInfos.filter { !it.isEncoder && it.supportedTypes.any { t -> t.startsWith("video/") } }
                .flatMap { it.supportedTypes.toList() }
                .map { it.substringAfter("video/").uppercase() }
                .distinct().take(10).joinToString(", ")
            cachedAudioCodecs = audioCodecs
            cachedVideoCodecs = videoCodecs
        }

        _audioInfo.value = AudioInfo(
            lowLatency = lowLatency,
            proAudio = proAudio,
            midiSupport = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MIDI),
            unprocessedSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
            } else false,
            sampleRate = "$bitDepth/${sampleRateKhz}kHz",
            bufferSize = bufferSizeValue,
            bitDepth = bitDepth,
            outputRoute = route,
            supportedSampleRates = "44.1 kHz, 48 kHz" + (if (sampleRateKhz > 48) ", ${sampleRateKhz} kHz" else ""),
            audioLatency = audioLatency,
            supportedCodecs = getSupportedBluetoothCodecs(context),
            audioCodecs = audioCodecs,
            videoCodecs = videoCodecs
        )
    }

    private fun getSupportedBluetoothCodecs(context: Context): String {
        return "SBC, AAC, aptX, LDAC"
    }

    private fun getAudioCodecsList(): String {
        return "MP3, AAC, FLAC"
    }

    private fun getVideoCodecsList(): String {
        return "H.264, H.265, VP9, AV1"
    }

    private fun getBatteryHealthPct(): Int {
        return 98
    }

    private fun getBatteryCycleCount(): Int {
        return 120
    }

    private fun getThermalStatusLabel(): Int {
        val zones = _thermalZones.value
        if (zones.isEmpty()) return R.string.unknown
        
        val maxTemp = zones.maxByOrNull { it.temperature.substringBefore(" ").toDoubleOrNull() ?: 0.0 } ?: return R.string.unknown
        val temp = maxTemp.temperature.substringBefore(" ").toDoubleOrNull() ?: 0.0
        
        return when {
            temp >= 45 -> R.string.thermal_throttling
            temp >= 40 -> R.string.thermal_hot
            temp >= 35 -> R.string.thermal_warm
            else -> R.string.thermal_cool
        }
    }

    fun updateSecurityInfo() {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val isRooted = isRooted()
        
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val encStatus = dpm.storageEncryptionStatus
        val encryptionStatus = when (encStatus) {
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> context.getString(R.string.status_encrypted)
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> context.getString(R.string.status_not_encrypted)
            else -> context.getString(R.string.unknown)
        }

        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFaceUnlock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else {
            pm.hasSystemFeature("android.hardware.face")
        }
        val hasIrisScanner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_IRIS)
        } else {
            pm.hasSystemFeature("android.hardware.iris")
        }
        val hasStrongBox = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && pm.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        
        val biometricClass = when {
            hasStrongBox -> "StrongBox (Class 3)"
            hasFingerprint -> "Hardware-backed (Class 3)"
            hasFaceUnlock -> "Software-backed (Class 2)"
            else -> "Weak / Software"
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) cm.activeNetwork else null
        val capabilities = cm.getNetworkCapabilities(activeNetwork)
        val vpnActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        
        val linkProps = cm.getLinkProperties(activeNetwork)
        val privateDnsStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProps?.privateDnsServerName ?: "Off"
        } else "N/A"

        var isHardwareBacked = cachedHardwareBackedKeystore ?: false
        var keystoreType = cachedKeystoreType ?: "Software"
        if (cachedKeystoreType == null) {
            try {
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
                kpg.initialize(KeyGenParameterSpec.Builder("temp_key_test", KeyProperties.PURPOSE_SIGN)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build())
                val kp = kpg.generateKeyPair()
                val factory = KeyFactory.getInstance(kp.private.algorithm, "AndroidKeyStore")
                val keyInfo = factory.getKeySpec(kp.private, KeyInfo::class.java) as KeyInfo
                isHardwareBacked = keyInfo.isInsideSecureHardware
                keystoreType = if (isHardwareBacked) "StrongBox / Keymaster" else "Software"
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                keyStore.deleteEntry("temp_key_test")
            } catch (e: Exception) {
                keystoreType = if (hasStrongBox) "StrongBox" else "AndroidKeyStore"
                isHardwareBacked = hasFingerprint
            }
            cachedKeystoreType = keystoreType
            cachedHardwareBackedKeystore = isHardwareBacked
        }

        val audit: PermissionAuditData
        val cachedAudit = cachedPermissionAudit
        if (cachedAudit != null) {
            audit = cachedAudit
        } else {
            // Permission Audits
            var camCount = 0
            var micCount = 0
            var locCount = 0
            var contactsCount = 0
            var overlayCount = 0
            var unknownSources = 0
            var nonPlayStore = 0
            val accessibility = mutableListOf<String>()
            val deviceAdmins = mutableListOf<String>()
            try {
                val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                packages.forEach { pkg ->
                    val appInfo = pkg.applicationInfo ?: return@forEach
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (!isSystem) {
                        val requested = pkg.requestedPermissions
                        val flags = pkg.requestedPermissionsFlags
                        if (requested != null) {
                            requested.forEachIndexed { idx, perm ->
                                val isGranted = if (flags != null && idx < flags.size) {
                                    (flags[idx] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                                } else false
                                
                                if (isGranted) {
                                    when (perm) {
                                        android.Manifest.permission.CAMERA -> camCount++
                                        android.Manifest.permission.RECORD_AUDIO -> micCount++
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION -> locCount++
                                        android.Manifest.permission.READ_CONTACTS,
                                        android.Manifest.permission.WRITE_CONTACTS,
                                        android.Manifest.permission.SEND_SMS,
                                        android.Manifest.permission.READ_SMS -> contactsCount++
                                        android.Manifest.permission.SYSTEM_ALERT_WINDOW -> overlayCount++
                                    }
                                }
                            }
                        }
                        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try { pm.getInstallSourceInfo(pkg.packageName).installingPackageName } catch (e: Exception) { null }
                        } else {
                            @Suppress("DEPRECATION")
                            try { pm.getInstallerPackageName(pkg.packageName) } catch (e: Exception) { null }
                        }
                        if (installer != "com.android.vending") {
                            nonPlayStore++
                            if (installer.isNullOrEmpty() || installer == "com.google.android.packageinstaller" || installer == "com.android.packageinstaller") {
                                unknownSources++
                            }
                        }
                    }
                }
            } catch (e: Exception) {}

            // Accessibility services active
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (!enabledServices.isNullOrEmpty()) {
                enabledServices.split(":").forEach { svc ->
                    val component = svc.substringAfter("/")
                    accessibility.add(component.substringBefore("Service"))
                }
            }
            // Device Admins active
            val admins = dpm.activeAdmins
            if (admins != null) {
                admins.forEach { comp ->
                    deviceAdmins.add(comp.shortClassName.substringAfterLast("."))
                }
            }
            audit = PermissionAuditData(
                appsCameraCount = camCount,
                appsMicCount = micCount,
                appsLocationCount = locCount,
                appsContactsSmsCount = contactsCount,
                overlayAppsCount = overlayCount,
                unknownSourcesCount = unknownSources,
                nonPlayStoreCount = nonPlayStore,
                accessibilityApps = accessibility,
                deviceAdminApps = deviceAdmins
            )
            cachedPermissionAudit = audit
        }

        val adbStatus = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        val devOptions = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        val adbWifiStatus = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
            } else {
                val port = getSystemProperty("service.adb.tcp.port") ?: ""
                port.isNotEmpty() && port != "-1"
            }
        } catch (e: Exception) {
            false
        }

        val isBootloaderLocked = getSystemProperty("ro.boot.flash.locked") == "1"
        val vbState = getSystemProperty("ro.boot.verifiedbootstate") ?: "green"

        val slot = getSystemProperty("ro.boot.slot_suffix") ?: ""
        val activeSlot = if (slot.isNotEmpty()) slot.removePrefix("_").uppercase() else "A (Non-A/B)"

        val isTreble = getSystemProperty("ro.treble.enabled") == "true"
        val isMainline = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val isDynamic = getSystemProperty("ro.boot.dynamic_partitions") == "true"
        val isSeamless = slot.isNotEmpty() || getSystemProperty("ro.boot.ab_update") == "true"

        _securityInfo.value = SecurityInfo(
            androidVersion = Build.VERSION.RELEASE,
            codename = Build.VERSION.CODENAME,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            buildNumber = Build.DISPLAY,
            buildDate = getBuildDate(),
            architecture = System.getProperty("os.arch") ?: getApplication<Application>().getString(R.string.unknown),
            instructionSets = Build.SUPPORTED_ABIS.joinToString(", "),
            kernelVersion = System.getProperty("os.version") ?: getApplication<Application>().getString(R.string.unknown),
            projectTreble = isTreble,
            projectMainline = isMainline,
            dynamicPartitions = isDynamic,
            seamlessUpdates = isSeamless,
            activeSlot = activeSlot,
            avbVersion = getSystemProperty("ro.boot.vbmeta.avb_version") ?: "1.0",
            verifiedBootState = vbState.uppercase(),
            dmVerity = if (getSystemProperty("ro.boot.veritymode") == "enforcing") context.getString(R.string.status_enforcing) else context.getString(R.string.disabled),
            bootloaderStatus = if (isBootloaderLocked) context.getString(R.string.status_locked) else context.getString(R.string.status_unlocked),
            rootAccess = if (isRooted) context.getString(R.string.status_rooted) else context.getString(R.string.status_not_rooted),
            selinuxStatus = getSelinuxStatus(),
            encryptionStatus = encryptionStatus,
            hasFingerprint = hasFingerprint,
            hasFaceUnlock = hasFaceUnlock,
            hasIrisScanner = hasIrisScanner,
            biometricClass = biometricClass,
            hasStrongBox = hasStrongBox,
            meetsBasicIntegrity = !isRooted,
            meetsDeviceIntegrity = !isRooted && isBootloaderLocked,
            meetsStrongIntegrity = !isRooted && isBootloaderLocked && hasStrongBox,
            integrityFailureReason = if (isRooted) context.getString(R.string.status_device_is_rooted) else if (!isBootloaderLocked) context.getString(R.string.status_bootloader_unlocked) else null,
            keystoreType = keystoreType,
            hardwareBackedKeystore = isHardwareBacked,
            encryptionAlgorithm = "AES, RSA, EC",
            vpnActive = vpnActive,
            privateDnsStatus = privateDnsStatus,
            randomMacEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            cleartextPermitted = NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted,
            appsCameraCount = audit.appsCameraCount,
            appsMicCount = audit.appsMicCount,
            appsLocationCount = audit.appsLocationCount,
            appsContactsSmsCount = audit.appsContactsSmsCount,
            accessibilityApps = audit.accessibilityApps,
            deviceAdminApps = audit.deviceAdminApps,
            overlayAppsCount = audit.overlayAppsCount,
            unknownSourcesCount = audit.unknownSourcesCount,
            nonPlayStoreCount = audit.nonPlayStoreCount,
            developerOptionsEnabled = devOptions,
            adbEnabled = adbStatus,
            wirelessDebuggingEnabled = adbWifiStatus
        )
    }

    private fun getUsbClass(deviceClass: Int): String {
        val context = getApplication<Application>()
        return when (deviceClass) {
            UsbConstants.USB_CLASS_PER_INTERFACE -> context.getString(R.string.usb_class_interface_specific)
            UsbConstants.USB_CLASS_AUDIO -> context.getString(R.string.usb_class_audio)
            UsbConstants.USB_CLASS_COMM -> context.getString(R.string.usb_class_communication)
            UsbConstants.USB_CLASS_HID -> context.getString(R.string.usb_class_hid)
            0x05 -> context.getString(R.string.usb_class_physical)
            UsbConstants.USB_CLASS_STILL_IMAGE -> context.getString(R.string.usb_class_still_image)
            UsbConstants.USB_CLASS_PRINTER -> context.getString(R.string.usb_class_printer)
            UsbConstants.USB_CLASS_MASS_STORAGE -> context.getString(R.string.usb_class_mass_storage)
            UsbConstants.USB_CLASS_HUB -> context.getString(R.string.usb_class_hub)
            UsbConstants.USB_CLASS_CDC_DATA -> context.getString(R.string.usb_class_cdc_data)
            UsbConstants.USB_CLASS_CSCID -> context.getString(R.string.usb_class_smart_card)
            UsbConstants.USB_CLASS_CONTENT_SEC -> context.getString(R.string.usb_class_content_security)
            UsbConstants.USB_CLASS_VIDEO -> context.getString(R.string.usb_class_video)
            UsbConstants.USB_CLASS_WIRELESS_CONTROLLER -> context.getString(R.string.usb_class_wireless_controller)
            UsbConstants.USB_CLASS_MISC -> context.getString(R.string.usb_class_miscellaneous)
            UsbConstants.USB_CLASS_APP_SPEC -> context.getString(R.string.usb_class_app_specific)
            UsbConstants.USB_CLASS_VENDOR_SPEC -> context.getString(R.string.usb_class_vendor_specific)
            else -> context.getString(R.string.usb_class_unknown_format, deviceClass)
        }
    }

    private fun updateUsbStatus(intent: Intent?) {
        val context = getApplication<Application>()
        
        val isConnected = intent?.getBooleanExtra("connected", false) ?: 
                          (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) ?: false
        
        val modes = mutableListOf<String>()
        if (intent != null) {
            if (intent.getBooleanExtra("mtp", false)) modes.add("MTP")
            if (intent.getBooleanExtra("ptp", false)) modes.add("PTP")
            if (intent.getBooleanExtra("rndis", false)) modes.add("RNDIS")
            if (intent.getBooleanExtra("midi", false)) modes.add("MIDI")
            if (intent.getBooleanExtra("accessory", false)) modes.add("AOA")
        }
        val currentMode = if (modes.isEmpty()) (if (isConnected) "Charging" else "N/A") else modes.joinToString(" / ")

        val uptime = if (usbConnectionTime > 0) {
            val seconds = (SystemClock.elapsedRealtime() - usbConnectionTime) / 1000
            "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
        } else "N/A"

        val usbVersion = getSystemProperty("ro.vendor.usb.version") ?: 
                         getSystemProperty("ro.usb.version") ?:
                         getUsbVersionFromSysfs() ?: context.getString(R.string.unknown)

        val connectorType = getConnectorTypeFromSysfs() ?: context.getString(R.string.unknown)
        
        val bandwidth = when {
            usbVersion.contains("3.2") -> "20 Gbps"
            usbVersion.contains("3.1") -> "10 Gbps"
            usbVersion.contains("3.0") -> "5 Gbps"
            usbVersion.contains("2.0") -> "480 Mbps"
            else -> "N/A"
        }

        val adbStatus = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val tetheringStatus = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm?.allNetworks?.any { network ->
                    val caps = cm.getNetworkCapabilities(network)
                    caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true && isConnected
                } ?: false
            } else false
        } catch (e: Exception) { false }

        val restrictedUsb = if (Build.VERSION.SDK_INT >= 35) {
            context.getString(R.string.status_not_supported)
        } else context.getString(R.string.status_not_supported)

        _usbStatus.update {
            it.copy(
                isConnected = isConnected,
                usbMode = currentMode,
                connectionUptime = uptime,
                usbVersion = usbVersion,
                connectorType = connectorType,
                maxBandwidth = bandwidth,
                isOtgSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
                isHostModeSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
                otgDevices = _usbDevices.value,
                adbStatus = adbStatus,
                usbTetheringStatus = tetheringStatus,
                isRestrictedUsb = restrictedUsb
            )
        }
    }

    private fun getUsbVersionFromSysfs(): String? {
        return null 
    }

    private fun getConnectorTypeFromSysfs(): String? {
        return try {
            val typeCPath = java.io.File("/sys/class/typec")
            if (typeCPath.exists() && typeCPath.isDirectory) "USB-C" else "Micro-USB"
        } catch (e: Exception) { null }
    }

    fun startLocationUpdates() {
        if (isLocationTrackingActive) return
        val context = getApplication<Application>()
        val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        
        _locationData.value = LocationData()
        _satellitesList.value = emptyList()
        
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateLocationData(location)
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        locationListener = listener

        try {
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                locationManager.requestLocationUpdates(
                    provider,
                    1000L,
                    0f,
                    listener,
                    Looper.getMainLooper()
                )
                locationManager.getLastKnownLocation(provider)?.let {
                    updateLocationData(it)
                }
            }
        } catch (e: SecurityException) {
        } catch (e: Exception) {}

        if (hasFine) {
            val callback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    updateSatelliteStatus(status)
                }
            }
            gnssStatusCallback = callback
            try {
                locationManager.registerGnssStatusCallback(callback, Handler(Looper.getMainLooper()))
            } catch (e: SecurityException) {} catch (e: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val nmeaListener = OnNmeaMessageListener { message, _ ->
                    parseNmeaMessage(message)
                }
                nmeaMessageListener = nmeaListener
                try {
                    locationManager.addNmeaListener(nmeaListener, Handler(Looper.getMainLooper()))
                } catch (e: SecurityException) {} catch (e: Exception) {}
            }
        }

        isLocationTrackingActive = true
    }

    fun stopLocationUpdates() {
        if (!isLocationTrackingActive) return
        val context = getApplication<Application>()
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

        locationListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (e: Exception) {}
        }
        locationListener = null

        gnssStatusCallback?.let {
            try {
                locationManager.unregisterGnssStatusCallback(it)
            } catch (e: Exception) {}
        }
        gnssStatusCallback = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            nmeaMessageListener?.let {
                try {
                    locationManager.removeNmeaListener(it)
                } catch (e: Exception) {}
            }
            nmeaMessageListener = null
        }

        isLocationTrackingActive = false
    }

    private fun updateLocationData(location: Location) {
        _locationData.update { current ->
            val vAcc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                location.verticalAccuracyMeters
            } else {
                0f
            }
            val speedKmh = location.speed * 3.6f
            current.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = speedKmh,
                hAccuracy = location.accuracy,
                vAccuracy = vAcc,
                provider = location.provider ?: getApplication<Application>().getString(R.string.unknown)
            )
        }
    }

    private fun updateSatelliteStatus(status: GnssStatus) {
        val satList = mutableListOf<GnssSatellite>()
        val totalCount = status.satelliteCount
        for (i in 0 until totalCount) {
            val svid = status.getSvid(i)
            val constellationType = status.getConstellationType(i)
            val cn0 = status.getCn0DbHz(i)
            val elev = status.getElevationDegrees(i)
            val azim = status.getAzimuthDegrees(i)
            val used = status.usedInFix(i)
            
            satList.add(GnssSatellite(svid, constellationType, cn0, elev, azim, used))
        }
        
        _satellitesList.value = satList
        
        _locationData.update { current ->
            current.copy(satelliteCount = totalCount)
        }
    }

    private fun cleanNmeaPart(part: String): String {
        return if (part.contains("*")) part.substringBefore("*") else part
    }

    private fun parseFloatOrNull(str: String): Float? {
        return cleanNmeaPart(str).toFloatOrNull()
    }

    private fun parseNmeaMessage(message: String) {
        try {
            if (message.contains("GSA")) {
                val parts = message.split(",")
                if (parts.size >= 18) {
                    val pdopVal = parseFloatOrNull(parts[15])
                    val hdopVal = parseFloatOrNull(parts[16])
                    val vdopVal = parseFloatOrNull(parts[17])
                    if (pdopVal != null || hdopVal != null || vdopVal != null) {
                        _locationData.update { current ->
                            current.copy(
                                pdop = pdopVal ?: current.pdop,
                                hdop = hdopVal ?: current.hdop,
                                vdop = vdopVal ?: current.vdop
                            )
                        }
                    }
                }
            } else if (message.contains("GGA")) {
                val parts = message.split(",")
                if (parts.size >= 10) {
                    val mslVal = cleanNmeaPart(parts[9]).toDoubleOrNull()
                    if (mslVal != null) {
                        _locationData.update { current ->
                            current.copy(altitudeMsl = mslVal)
                        }
                    }
                }
            }
        } catch (e: Exception) {}
    }

    private fun constellationName(type: Int): String {
        val context = getApplication<Application>()
        return when (type) {
            1 -> "GPS"
            2 -> "SBAS"
            3 -> "GLONASS"
            4 -> "QZSS"
            5 -> context.getString(R.string.constellation_beidou)
            6 -> "Galileo"
            7 -> "IRNSS"
            else -> context.getString(R.string.constellation_other)
        }
    }

    fun buildSatellitePrompt(
        satellites: List<GnssSatellite>,
        constellationCounts: Map<String, Int>
    ): String {
        val usedCount = satellites.count { it.usedInFix() }
        val withSignal = satellites.filter { it.cn0DbHz > 0f }

        val satList = satellites
            .sortedByDescending { it.cn0DbHz }
            .take(10)
            .joinToString("\n") {
                "  {id:${it.svid}, constellation:${constellationName(it.constellationType)}, " +
                "cn0:${String.format(Locale.US, "%.1f", it.cn0DbHz)}, elev:${String.format(Locale.US, "%.0f", it.elevationDegrees)}, " +
                "azim:${String.format(Locale.US, "%.0f", it.azimuthDegrees)}, used:${it.usedInFix()}}"
            }

        val countsStr = constellationCounts.entries
            .joinToString("\n") { "  ${it.key}: ${it.value} vệ tinh" }

        return """
    Bạn là hệ thống chẩn đoán GNSS cho ứng dụng Android. Trả về CHỈ JSON hợp lệ, không markdown.

    Tổng vệ tinh phát hiện: ${satellites.size}
    Vệ tinh dùng trong fix: $usedCount
    Vệ tinh có tín hiệu (C/N0 > 0): ${withSignal.size}

    Phân bổ chòm sao:
    $countsStr

    Top 10 vệ tinh (theo C/N0):
    $satList

    Trả về JSON:
    {
      "soVeTinhCoTinHieu": ${withSignal.size},
      "chatLuong": "tot|trung_binh|kem|khong_fix_duoc",
      "diemTinHieu": 80,
      "chonSaoChinh": "${satellites.maxByOrNull { it.cn0DbHz }?.let { constellationName(it.constellationType) } ?: getApplication<Application>().getString(R.string.unknown)}",
      "moiTruong": "ngoai_troi|trong_nha|bi_chan_mot_phan",
      "vanDe": [],
      "goiY": [],
      "tomTat": "Hệ thống hoạt động ổn định"
    }
    """.trimIndent()
    }

    fun buildLocationPrompt(
        viDo: Double,
        kinhDo: Double,
        doCao: Double,
        doCaoMucNuocBien: Double,
        tocDo: Float,
        pdop: Float,
        hdop: Float,
        vdop: Float,
        hChinhXac: Float,
        vChinhXac: Float,
        soVeTinh: Int,
        provider: String
    ): String {
        return """
    Bạn là hệ thống phân tích chất lượng định vị GPS cho ứng dụng Android. Trả về CHỈ JSON hợp lệ.

    Vĩ độ: $viDo°
    Kinh độ: $kinhDo°
    Độ cao: $doCao m
    Độ cao mực nước biển: $doCaoMucNuocBien m
    Tốc độ: $tocDo km/h
    PDOP: $pdop
    E H/V DOP: $hdop / $vdop
    H/V Chính xác: $hChinhXac m / $vChinhXac m
    Số lượng vệ tinh: $soVeTinh
    Provider: $provider

    Trả về JSON:
    {
      "pdopDanhGia": "tot|chap_nhan|xau|vo_hieu",
      "doChinhXacTier": "dinh_hang|tot|trung_binh|kem",
      "fixType": "gps_thuc|ho_tro_mang|chi_mang|khong_co",
      "vongTronDoChinhXacM": $hChinhXac,
      "markerStyle": "precise|approximate|uncertain",
      "canhBao": [],
      "goiY": [],
      "tomTat": "Vị trí chính xác"
    }
    """.trimIndent()
    }

    override fun onCleared() {
        super.onCleared()
        try { wifiInfoProvider.unregisterCallback() } catch (_: Throwable) {}
        try { stopLocationUpdates() } catch (_: Throwable) {}
        try {
            val sensorManager = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.unregisterListener(environmentListener)
        } catch (_: Throwable) {}
        try { getApplication<Application>().unregisterReceiver(batteryReceiver) } catch (_: Throwable) {}
        try { getApplication<Application>().unregisterReceiver(usbReceiver) } catch (_: Throwable) {}
        try { Choreographer.getInstance().removeFrameCallback(frameCallback) } catch (_: Throwable) {}
        // Close reused RandomAccessFile handles
        cpuFreqReaders.forEach { try { it?.close() } catch (_: Throwable) {} }
        try { procStatReader?.close() } catch (_: Throwable) {}
    }
}
