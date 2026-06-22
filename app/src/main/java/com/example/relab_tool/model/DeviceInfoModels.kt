package com.example.relab_tool.model

import androidx.compose.runtime.Immutable
import com.example.relab_tool.data.WifiSsidState

@Immutable
data class DeviceSummary(
    val model: String = "Unknown",
    val manufacturer: String = "Unknown",
    val device: String = "Unknown",
    val board: String = "Unknown",
    val hardware: String = "Unknown",
    val androidId: String = "Unknown",
    val gsfId: String? = null,
    val buildFingerprint: String = "Unknown",
    val usbHost: String = "Unknown",
    val cpuModel: String = "Unknown",
    val gpuModel: String = "Unknown",
    // Detailed hardware components
    val resolution: String = "Unknown",
    val platform: String = "Unknown",
    val androidVersion: String = "Unknown",
    val kernel: String = "Unknown",
    val touchscreen: String = "Unknown",
    val accelerometer: String = "Unknown",
    val als_ps: String = "Unknown",
    val magnetometer: String = "Unknown",
    val gyroscope: String = "Unknown",
    val charger: String = "Unknown",
    val nfc: String = "Unknown",
    val fingerprintSensor: String = "Unknown",
    val wifiChip: String = "Unknown",
    val soundChip: String = "Unknown",
    val ramType: String = "Unknown",
    val flashType: String = "Unknown"
)

@Immutable
data class SystemInfo(
    val brand: String = "Unknown",
    val manufacturer: String = "Unknown",
    val model: String = "Unknown",
    val modelName: String = "Unknown",
    val androidVersion: String = "Unknown",
    val osVersion: String = "Unknown",
    val codeName: String = "Unknown",
    val sdkLevel: String = "Unknown",
    val device: String = "Unknown",
    val product: String = "Unknown",
    val board: String = "Unknown",
    val platform: String = "Unknown",
    val buildId: String = "Unknown",
    val javaVm: String = "Unknown",
    val securityPatch: String = "Unknown",
    val baseband: String = "Unknown",
    val gps: String = "Unknown",
    val bluetoothVersion: String = "Unknown",
    val buildType: String = "Unknown",
    val tags: String = "Unknown",
    val incremental: String = "Unknown",
    val description: String = "Unknown",
    val fingerprint: String = "Unknown",
    val buildDate: String = "Unknown",
    val builder: String = "Unknown",
    val bootloader: String = "Unknown",
    val kernel: String = "Unknown",
    val openGlEs: String = "Unknown",
    val googlePlayServices: String = "Unknown",
    val deviceFeatures: String = "Unknown",
    val language: String = "Unknown",
    val timezone: String = "Unknown",
    val uptime: String = "Unknown",
    val gmsVersion: String = "Unknown"
)

@Immutable
data class CpuInfo(
    val processor: String,
    val architecture: String,
    val cores: Int,
    val supportedAbis: String,
    val cpuGovernor: String
)

@Immutable
data class BatteryInfo(
    val health: String = "Unknown",
    val level: Int = 0,
    val levelString: String = "0%",
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val status: String = "Unknown",
    val powerSource: String = "N/A",
    val technology: String = "Li-ion",
    val temperature: String = "N/A",
    val wattage: String = "0.00 W",
    val capacity: String = "N/A",
    val currentNow: String = "N/A",
    val chargeCounter: String = "N/A",
    val timeToFull: Long = -1L,
    val wear: String = "N/A",
    val actualCapacity: String = "N/A",
    val voltage: String = "N/A",
    val isWirelessSupported: Boolean = false
)

@Immutable
data class DisplayInfo(
    val currentResolution: String = "Unknown",
    val highestResolution: String = "N/A",
    val aspectRatio: String = "N/A",
    val density: String = "Unknown",
    val xDpi: String = "N/A",
    val yDpi: String = "N/A",
    val ppi: String = "N/A",
    val currentRefreshRate: String = "Unknown",
    val supportedRefreshRates: String = "N/A",
    val supportedRates: String = "N/A",
    val hdrSupport: String = "N/A",
    val wideColorGamut: Boolean = false,
    val physicalSize: String = "Unknown",
    val diagonal: String = "Unknown",
    val brightnessLevel: String = "Unknown",
    val screenTimeout: String = "Unknown",
    val orientation: String = "N/A",
    val colorDepth: String = "8-bit",
    val colorDepthSubtext: String = "Standard",
    val widevineLevel: String = "N/A",
    val colorSpace: String = "N/A"
)

@Immutable
data class ExternalStorageInfo(
    val name: String,
    val total: String,
    val free: String,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val type: String = "External Storage" // e.g., "SD Card", "USB Drive"
)

@Immutable
data class MemoryInfo(
    val totalRam: String = "Unknown",
    val availableRam: String = "Unknown",
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val ramType: String = "Unknown",
    val zRamTotal: String = "N/A",
    val zRamUsed: String = "N/A",
    val zRamTotalBytes: Long = 0L,
    val zRamUsedBytes: Long = 0L,
    val internalTotal: String = "Unknown",
    val internalFree: String = "Unknown",
    val internalUsed: String = "Unknown",
    val internalTotalBytes: Long = 0L,
    val internalFreeBytes: Long = 0L,
    val internalUsedBySystem: String = "N/A",
    val internalUsedBySystemBytes: Long = 0L,
    val internalUsedByApps: String = "N/A",
    val internalUsedByAppsBytes: Long = 0L,
    val internalFsType: String = "N/A",
    val internalBlockSize: String = "N/A",
    val memoryPageSize: String = "4 KB",
    val internalPartition: String = "N/A",
    val externalStorages: List<ExternalStorageInfo> = emptyList(),
    val externalTotal: String = "N/A",
    val externalUsed: String = "N/A",
    val flashType: String = "N/A"
)

@Immutable
data class SensorInfo(
    val name: String,
    val vendor: String,
    val version: Int,
    val type: Int,
    val power: Float,
    val resolution: Float,
    val maxDelay: Int = 0,
    val minDelay: Int = 0
)

@Immutable
data class DrmSchemeInfo(
    val name: String,
    val vendor: String = "Unknown",
    val version: String = "Unknown",
    val description: String = "Unknown",
    val algorithms: String = "Unknown",
    val securityLevel: String = "Unknown",
    val deviceId: String = "Unknown",
    val maxHdcpLevel: String = "Unknown",
    val currentHdcpLevel: String = "Unknown",
    val systemId: String = "Unknown"
)

/**
 * Fast-changing dashboard data emitted every poll tick (~1s).
 * Only cards that display real-time metrics (CPU/GPU/RAM gauges, core frequencies,
 * network speed, disk I/O, refresh rate, touch rate) should observe this flow.
 * Keeping this separate from slow data prevents full-grid recomposition storms.
 */
@Immutable
data class DashboardRealtimeData(
    val ramTotal: Long = 0L,
    val ramUsed: Long = 0L,
    val ramHistory: List<Float> = emptyList(),
    val cpuUsage: Int = 0,
    val cpuHistory: List<Float> = emptyList(),
    val gpuUsage: Int = 0,
    val gpuHistory: List<Float> = emptyList(),
    val cpuCoreFrequencies: List<Long> = emptyList(),
    val cpuCoreHistory: List<List<Float>> = emptyList(),
    // Pre-computed cluster histories to avoid computation in Compose composition
    val clusterHistories: List<List<Float>> = emptyList(),
    val currentRefreshRate: Float = 0f,
    val downloadSpeed: String = "0 B/s",
    val uploadSpeed: String = "0 B/s",
    val downloadSpeedHistory: List<Float> = emptyList(),
    val uploadSpeedHistory: List<Float> = emptyList(),
    val touchSamplingRate: Int = 0,
    val diskReadSpeed: String = "0 B/s",
    val diskWriteSpeed: String = "0 B/s",
    val ambientLightLux: Float = 0f,
    val pressureHpa: Float = 0f,
    val cpuTemperature: Float = 0f,
    val cpuTempHistory: List<Float> = emptyList(),
    val gpuFreq: Float = 0f,
    val gpuFreqHistory: List<Float> = emptyList(),
    val gpuMemory: Float = 0f,
    val gpuMemoryHistory: List<Float> = emptyList(),
    val gpuTemperature: Float = 0f,
    val gpuTempHistory: List<Float> = emptyList(),
    val fpsHistory: List<Float> = emptyList()
)

/**
 * Slow-changing dashboard data emitted every ~5 seconds.
 * Cards that display connectivity, battery, storage, security, features etc.
 * observe this flow and only recompose when their data actually changes.
 */
@Immutable
data class DashboardSlowData(
    val storageTotal: Long = 0L,
    val storageUsed: Long = 0L,
    val storageSmartStatus: Int = com.example.relab_tool.R.string.unknown,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val batteryWattage: String = "0.00 W",
    val batteryHistory: List<Float> = emptyList(),
    val wattageHistory: List<Float> = emptyList(),
    val batteryTemp: String = "N/A",
    val sensorsCount: Int = 0,
    val appsCount: Int = 0,
    val screenResolution: String = "",
    val screenInfo: String = "",
    val widevineLevel: String = "N/A",
    val wifiSsid: String? = null,
    val wifiBssid: String? = null,
    val wifiSignalDbm: Int? = null,
    val wifiStandard: String? = null,
    val simInfos: List<SimInfo> = emptyList(),
    val bluetoothConnectedDevices: Int = 0,
    val bluetoothCodec: String? = null,
    val colorDepth: String = "8-bit",
    val colorDepthSubtext: String = "Standard",
    // Hardware features
    val usbStorage: Boolean = false,
    val usbAccessory: Boolean = false,
    val irisScanner: Boolean = false,
    val faceRecognition: Boolean = false,
    val infrared: Boolean = false,
    val uwb: Boolean = false,
    val nfc: Boolean = false,
    val secureNfc: Boolean = false,
    val gps: Boolean = false,
    // Semi-realtime (updated every tick but consumed by slow cards)
    val thermalStatus: Int = com.example.relab_tool.R.string.unknown,
    val chargingCurrentMa: Int = 0,
    val chargingVoltageV: Float = 0f,
    val batteryHealthPct: Int = 0,
    val batteryCycleCount: Int = 0,
    val deepSleepRatio: Float = 0f,
    val uptime: String = "",
    val isRooted: Boolean = false,
    val selinuxStatus: String = "Unknown",
    val securityPatch: String = ""
)


@Immutable
data class CpuCluster(
    val id: Int,
    val name: String = "",
    val architecture: String = "",
    val coreCount: Int,
    val minFreq: String,
    val maxFreq: String,
    val currentFreq: String = "N/A",
    val coreIndices: List<Int> = emptyList()
)

@Immutable
data class SocInfo(
    val processor: String = "Unknown",
    val vendor: String = "Unknown",
    val cores: String = "Unknown",
    val bigLittle: String = "Unknown",
    val clusters: String = "Unknown",
    val cpuClusters: List<CpuCluster> = emptyList(),
    val family: String = "Unknown",
    val mode: String = "Unknown",
    val machine: String = "Unknown",
    val abi: String = "Unknown",
    val instructions: String = "Unknown",
    val revision: String = "Unknown",
    val clockSpeed: String = "Unknown",
    val governor: String = "Unknown",
    val supportedAbi: String = "Unknown",
    val gpu: String = "Unknown",
    val gpuVendor: String = "Unknown",
    val gpuArch: String = "Unknown",
    val gpuL2Cache: String = "Unknown",
    val gpuBusWidth: String = "Unknown",
    val openGlEs: String = "Unknown",
    val gpuFullVersion: String = "Unknown",
    val vulkanVersion: String = "Unknown",
    val gpuExtensions: String = "Unknown",
    val gpuClockSpeed: String = "Unknown",
    val gpuCores: String = "Unknown",
    val process: String = "Unknown",
    val instructionSets: String = "Unknown",
    val architecture: String = "Unknown"
)

@Immutable
data class PhysicalSensor(
    val model: String,
    val manufacturer: String = "Unknown",
    val resolution: String = "N/A",
    val role: String = "Unknown"
)

@Immutable
data class CameraInfo(
    val id: String = "0",
    val facing: String = "Unknown",
    val resolution: String = "Unknown",
    val sensorResolution: String = "N/A",
    val sensorModel: String = "Unknown",
    val supportedHardware: String = "N/A",
    val physicalSensors: List<PhysicalSensor> = emptyList(),
    val physicalMP: Double = 0.0,
    val binnedMP: Double = 0.0,
    val binningFactor: String = "none",
    val binningType: String = "Full Readout",
    val hasHighResMode: Boolean = false,
    val maxResolution: String = "N/A",
    val aperture: String = "N/A",
    val focalLength: String = "N/A",
    val focalLength35mm: String = "N/A",
    val focusModes: String = "N/A",
    val sensorSize: String = "N/A",
    val diagonal: String = "N/A",
    val pixelSize: String = "N/A",
    val zoom: String = "N/A",
    val imageFormats: String = "N/A",
    val angleOfView: String = "N/A",
    val cropFactor: String = "N/A",
    val isoRange: String = "N/A",
    val colorFilter: String = "N/A",
    val orientation: String = "N/A",
    val flash: String = "no",
    val exposureRange: String = "N/A",
    val shutterSpeedRange: String = "N/A",
    val videoStabilization: Boolean = false,
    val opticalStabilization: Boolean = false,
    val autoExposureLock: Boolean = false,
    val autoWhiteBalanceLock: Boolean = false,
    val capabilities: List<String> = emptyList(),
    val exposureModes: List<String> = emptyList(),
    val focusModesList: List<String> = emptyList(),
    val whiteBalanceModes: List<String> = emptyList(),
    val sceneModes: List<String> = emptyList(),
    val vendor: String = "Unknown",
    val videoCapabilities: String = "N/A",
    val camera2ApiLevel: String = "N/A",
    val isPhysical: Boolean = false,
    val parentLogicalId: String? = null,
    val allResolutions: Map<String, List<String>> = emptyMap()
)

@Immutable
data class UsbInfo(
    val name: String,
    val vendorId: String,
    val productId: String,
    val deviceClass: String,
    val manufacturerName: String? = null,
    val productName: String? = null,
    val serialNumber: String? = null
)

@Immutable
data class UsbStatusInfo(
    val isConnected: Boolean = false,
    val usbMode: String = "N/A",
    val connectionUptime: String = "N/A",
    val usbVersion: String = "Unknown",
    val connectorType: String = "Unknown",
    val maxBandwidth: String = "N/A",
    val isOtgSupported: Boolean = false,
    val isHostModeSupported: Boolean = false,
    val otgDevices: List<UsbInfo> = emptyList(),
    val displayPortAltMode: String = "Unknown",
    val hdmiAltMode: String = "Unknown",
    val maxVideoResolution: String = "Unknown",
    val isThunderboltSupported: String = "Unknown",
    val adbStatus: Boolean = false,
    val usbTetheringStatus: Boolean = false,
    val isRestrictedUsb: String = "Not Supported"
)

@Immutable
data class CodecInfo(
    val name: String,
    val mimeType: String,
    val type: String
)

@Immutable
data class ThermalInfo(
    val name: String,
    val temperature: String
)

@Immutable
data class AppEntry(
    val name: String,
    val packageName: String,
    val version: String,
    val sdk: String,
    val isSystem: Boolean,
    val isGame: Boolean = false,
    val latestVersion: String? = null,
    val updateUrl: String? = null,
    val isCheckingUpdate: Boolean = false
)

@Immutable
data class SimInfo(
    val slot: Int,
    val carrier: String,
    val phoneNumber: String? = null,
    val countryIso: String? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val serviceProviderId: String? = null,
    val roaming: Boolean = false,
    val state: String = "Unknown",
    val signalDbm: Int? = null,
    val networkType: String = "Unknown"
)

@Immutable
data class CellularInfo(
    val state: String = "Unknown",
    val multiSimSupport: String = "Unknown",
    val phoneCount: Int = 1,
    val deviceType: String = "Unknown",
    val operator: String? = null,
    val apn: String? = null,
    val ipV4: String? = null,
    val ipV6: List<String> = emptyList(),
    val interfaceName: String? = null,
    val networkOperator: String? = null,
    val networkType: String? = null,
    val simInfos: List<SimInfo> = emptyList()
)

@Immutable
data class BluetoothFeature(
    val nameRes: Int,
    val isSupported: Boolean
)

@Immutable
data class BluetoothFeatureGroup(
    val titleRes: Int,
    val features: List<BluetoothFeature>
)

@Immutable
data class BluetoothInfo(
    val featureGroups: List<BluetoothFeatureGroup> = emptyList(),
    val state: String = "Unknown",
    val version: String = "Unknown",
    val name: String = "Unknown",
    val address: String = "02:00:00:00:00:00",
    val pairedDevicesCount: Int = 0,
    val connectedDevicesCount: Int = 0
)

@Immutable
data class NetworkInfo(
    val type: String = "Unknown",
    val state: String = "Unknown",
    val isConnected: Boolean = false,
    val cellularInfo: CellularInfo? = null,
    val wifiSsid: String? = null,
    val wifiSsidState: WifiSsidState = WifiSsidState.NotConnected,
    val isLocationPermissionGranted: Boolean = false,
    val isLocationServicesEnabled: Boolean = false,
    val wifiBssid: String? = null,
    val wifiSignalDbm: Int? = null,
    val wifiStandard: String? = null,
    val ipAddress: String? = null,
    val vendor: String? = null,
    val linkSpeed: String? = null,
    val frequency: String? = null,
    val signalStrength: String? = null,
    val channel: String? = null,
    val width: String? = null,
    val standard: String? = null,
    val security: String? = null,
    val ipv6Address: String? = null,
    val gateway: String? = null,
    val netmask: String? = null,
    val dns1: String? = null,
    val dns2: String? = null,
    val dhcpServer: String? = null,
    val leaseDuration: String? = null,
    val interfaceName: String? = null,
    val macAddress: String? = null,
    val networkType: String? = null,
    val isWifiDirectSupported: Boolean = false,
    val is5GHzSupported: Boolean = false,
    val is6GHzSupported: Boolean = false,
    val isWifiAwareSupported: Boolean = false,
    val isP2pSupported: Boolean = false,
    val isApSupported: Boolean = false,
    val isWifi4Supported: Boolean = false,
    val isWifi5Supported: Boolean = false,
    val isWifi6Supported: Boolean = false,
    val isWifi6eSupported: Boolean = false,
    val isWifi7Supported: Boolean = false,
    val isWifi8Supported: Boolean = false
)

@Immutable
data class AudioInfo(
    val lowLatency: Boolean = false,
    val proAudio: Boolean = false,
    val midiSupport: Boolean = false,
    val unprocessedSource: Boolean = false,
    val sampleRate: String = "Unknown",
    val bufferSize: String = "Unknown",
    val bitDepth: String = "Unknown",
    val outputRoute: String = "Unknown",
    val supportedSampleRates: String = "Unknown",
    val audioLatency: String = "Unknown",
    val supportedCodecs: String = "Unknown",
    val audioCodecs: String = "Unknown",
    val videoCodecs: String = "Unknown"
)

@Immutable
data class SecurityInfo(
    val androidVersion: String = "Unknown",
    val codename: String = "Unknown",
    val securityPatch: String = "Unknown",
    val buildNumber: String = "Unknown",
    val buildDate: String = "Unknown",
    val architecture: String = "Unknown",
    val instructionSets: String = "Unknown",
    val kernelVersion: String = "Unknown",
    val projectTreble: Boolean = false,
    val projectMainline: Boolean = false,
    val dynamicPartitions: Boolean = false,
    val seamlessUpdates: Boolean = false,
    val activeSlot: String = "Unknown",
    val avbVersion: String = "Unknown",
    val verifiedBootState: String = "Unknown",
    val dmVerity: String = "Unknown",
    val bootloaderStatus: String = "Unknown",
    val rootAccess: String = "Unknown",
    val selinuxStatus: String = "Unknown",
    val encryptionStatus: String = "Unknown",
    val hasFingerprint: Boolean = false,
    val hasFaceUnlock: Boolean = false,
    val hasIrisScanner: Boolean = false,
    val biometricClass: String = "Unknown",
    val hasStrongBox: Boolean = false,
    val meetsBasicIntegrity: Boolean = false,
    val meetsDeviceIntegrity: Boolean = false,
    val meetsStrongIntegrity: Boolean = false,
    val integrityFailureReason: String? = null,
    val keystoreType: String = "Unknown",
    val hardwareBackedKeystore: Boolean = false,
    val encryptionAlgorithm: String = "Unknown",
    val vpnActive: Boolean = false,
    val privateDnsStatus: String = "Unknown",
    val randomMacEnabled: Boolean = false,
    val cleartextPermitted: Boolean = false,
    val appsCameraCount: Int = 0,
    val appsMicCount: Int = 0,
    val appsLocationCount: Int = 0,
    val appsContactsSmsCount: Int = 0,
    val accessibilityApps: List<String> = emptyList(),
    val deviceAdminApps: List<String> = emptyList(),
    val overlayAppsCount: Int = 0,
    val unknownSourcesCount: Int = 0,
    val nonPlayStoreCount: Int = 0,
    val developerOptionsEnabled: Boolean = false,
    val adbEnabled: Boolean = false,
    val wirelessDebuggingEnabled: Boolean = false
)
// Changed

@Immutable
data class GnssSatellite(
    val svid: Int,
    val constellationType: Int,
    val cn0DbHz: Float,
    val elevationDegrees: Float,
    val azimuthDegrees: Float,
    val usedInFix: Boolean
) {
    fun usedInFix(): Boolean = usedInFix
}

@Immutable
data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val altitudeMsl: Double = 0.0,
    val speed: Float = 0.0f,
    val pdop: Float = 99.99f,
    val hdop: Float = 99.99f,
    val vdop: Float = 99.99f,
    val hAccuracy: Float = 0.0f,
    val vAccuracy: Float = 0.0f,
    val satelliteCount: Int = 0,
    val provider: String = "Unknown"
)
