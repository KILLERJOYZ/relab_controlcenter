package com.example.relab_tool.model

data class DeviceSummary(
    val model: String,
    val manufacturer: String,
    val device: String,
    val board: String,
    val hardware: String,
    val androidId: String,
    val gsfId: String? = null,
    val buildFingerprint: String,
    val usbHost: String,
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

data class SystemInfo(
    val brand: String = "Unknown",
    val manufacturer: String = "Unknown",
    val model: String = "Unknown",
    val modelName: String = "Unknown",
    val androidVersion: String,
    val osVersion: String = "Unknown",
    val codeName: String,
    val sdkLevel: String,
    val device: String = "Unknown",
    val product: String = "Unknown",
    val board: String = "Unknown",
    val platform: String = "Unknown",
    val buildId: String = "Unknown",
    val javaVm: String,
    val securityPatch: String,
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
    val bootloader: String,
    val kernel: String,
    val openGlEs: String,
    val googlePlayServices: String = "Unknown",
    val deviceFeatures: String = "Unknown",
    val language: String = "Unknown",
    val timezone: String = "Unknown",
    val uptime: String = "Unknown"
)

data class CpuInfo(
    val processor: String,
    val architecture: String,
    val cores: Int,
    val supportedAbis: String,
    val cpuGovernor: String
)

data class BatteryInfo(
    val health: String,
    val level: Int,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false,
    val status: String,
    val powerSource: String,
    val technology: String,
    val temperature: String,
    val wattage: String,
    val capacity: String,
    val currentNow: String = "N/A",
    val chargeCounter: String = "N/A",
    val timeToFull: Long = -1L,
    val wear: String = "N/A",
    val actualCapacity: String = "N/A"
)

data class DisplayInfo(
    val currentResolution: String,
    val highestResolution: String = "N/A",
    val aspectRatio: String = "N/A",
    val density: String,
    val xDpi: String = "N/A",
    val yDpi: String = "N/A",
    val ppi: String = "N/A",
    val currentRefreshRate: String,
    val supportedRefreshRates: String = "N/A",
    val hdrSupport: String = "N/A",
    val wideColorGamut: Boolean = false,
    val physicalSize: String,
    val brightnessLevel: String,
    val screenTimeout: String,
    val orientation: String = "N/A",
    val colorDepth: String = "8-bit",
    val colorDepthSubtext: String = "Standard",
    val widevineLevel: String = "N/A"
)

data class ExternalStorageInfo(
    val name: String,
    val total: String,
    val free: String,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val type: String = "External Storage" // e.g., "SD Card", "USB Drive"
)

data class MemoryInfo(
    val totalRam: String,
    val availableRam: String,
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val ramType: String = "Unknown",
    val zRamTotal: String = "N/A",
    val zRamUsed: String = "N/A",
    val zRamTotalBytes: Long = 0L,
    val zRamUsedBytes: Long = 0L,
    val internalTotal: String,
    val internalFree: String,
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
    val flashType: String = "N/A"
)

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

data class DashboardData(
    val ramTotal: Long,
    val ramUsed: Long,
    val ramHistory: List<Float> = emptyList(),
    val cpuUsage: Int = 0,
    val cpuHistory: List<Float> = emptyList(),
    val gpuUsage: Int = 0,
    val gpuHistory: List<Float> = emptyList(),
    val cpuCoreFrequencies: List<Long> = emptyList(),
    val cpuCoreHistory: List<List<Float>> = emptyList(),
    val storageTotal: Long,
    val storageUsed: Long,
    val storageSmartStatus: Int = com.example.relab_tool.R.string.unknown,
    val batteryLevel: Int,
    val isCharging: Boolean = false,
    val batteryWattage: String,
    val batteryHistory: List<Float> = emptyList(),
    val wattageHistory: List<Float> = emptyList(),
    val currentRefreshRate: Float = 0f,
    val batteryTemp: String,
    val sensorsCount: Int,
    val appsCount: Int,
    val screenResolution: String,
    val screenInfo: String,
    val widevineLevel: String = "N/A",
    val downloadSpeed: String = "0 B/s",
    val uploadSpeed: String = "0 B/s",
    val wifiSsid: String? = null,
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
    // Real-time status
    val thermalStatus: Int = com.example.relab_tool.R.string.unknown,
    val touchSamplingRate: Int = 0,
    val chargingCurrentMa: Int = 0,
    val chargingVoltageV: Float = 0f,
    val batteryHealthPct: Int = 0,
    val batteryCycleCount: Int = 0,
    val deepSleepRatio: Float = 0f,
    val diskReadSpeed: String = "0 B/s",
    val diskWriteSpeed: String = "0 B/s",
    val ambientLightLux: Float = 0f,
    val pressureHpa: Float = 0f,
    val uptime: String = "",
    val isRooted: Boolean = false,
    val selinuxStatus: String = "Unknown",
    val securityPatch: String = ""
)

data class CpuCluster(
    val id: Int,
    val name: String = "",
    val architecture: String = "",
    val coreCount: Int,
    val minFreq: String,
    val maxFreq: String,
    val currentFreq: String = "N/A"
)

data class SocInfo(
    val processor: String,
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
    val governor: String,
    val supportedAbi: String = "Unknown",
    val gpu: String,
    val gpuVendor: String = "Unknown",
    val gpuArch: String = "Unknown",
    val gpuL2Cache: String = "Unknown",
    val gpuBusWidth: String = "Unknown",
    val openGlEs: String,
    val gpuFullVersion: String = "Unknown",
    val vulkanVersion: String,
    val gpuExtensions: String = "Unknown",
    val gpuClockSpeed: String = "Unknown",
    val gpuCores: String = "Unknown",
    val process: String,
    val instructionSets: String
)

data class PhysicalSensor(
    val model: String,
    val manufacturer: String = "Unknown",
    val resolution: String = "N/A",
    val role: String = "Unknown"
)

data class CameraInfo(
    val id: String,
    val facing: String,
    val resolution: String,
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
    val aperture: String,
    val focalLength: String,
    val focalLength35mm: String = "N/A",
    val focusModes: String = "N/A",
    val sensorSize: String = "N/A",
    val diagonal: String = "N/A",
    val pixelSize: String = "N/A",
    val zoom: String = "N/A",
    val imageFormats: String = "N/A",
    val angleOfView: String = "N/A",
    val cropFactor: String = "N/A",
    val isoRange: String,
    val colorFilter: String = "N/A",
    val orientation: String = "N/A",
    val flash: String = "no",
    val exposureRange: String,
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

data class UsbInfo(
    val name: String,
    val vendorId: String,
    val productId: String,
    val deviceClass: String
)

data class CodecInfo(
    val name: String,
    val mimeType: String,
    val type: String
)

data class ThermalInfo(
    val name: String,
    val temperature: String
)

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

data class CellularInfo(
    val state: String, // Connected, Disconnected, etc.
    val multiSimSupport: String,
    val phoneCount: Int = 1,
    val deviceType: String, // GSM, CDMA, etc.
    val apn: String? = null,
    val ipV4: String? = null,
    val ipV6: List<String> = emptyList(),
    val interfaceName: String? = null,
    val networkOperator: String? = null,
    val networkType: String? = null, // LTE, NR (5G), etc.
    val simInfos: List<SimInfo> = emptyList()
)

data class BluetoothFeature(
    val nameRes: Int,
    val isSupported: Boolean
)

data class BluetoothFeatureGroup(
    val titleRes: Int,
    val features: List<BluetoothFeature>
)

data class BluetoothInfo(
    val featureGroups: List<BluetoothFeatureGroup> = emptyList()
)

data class NetworkInfo(
    val type: String, // WiFi or Cellular
    val state: String = "Unknown",
    val cellularInfo: CellularInfo? = null,
    val wifiSsid: String? = null,
    val wifiBssid: String? = null,
    val wifiSignalDbm: Int? = null,
    val wifiStandard: String? = null,
    val vendor: String? = null,
    val linkSpeed: String? = null,
    val frequency: String? = null,
    val signalStrength: String? = null,
    val channel: String? = null,
    val width: String? = null,
    val standard: String? = null,
    val security: String? = null,
    val ipAddress: String? = null,
    val ipv6Address: String? = null,
    val gateway: String? = null,
    val netmask: String? = null,
    val dns1: String? = null,
    val dns2: String? = null,
    val dhcpServer: String? = null,
    val leaseDuration: String? = null,
    val interfaceName: String? = null,
    val macAddress: String? = null,
    val networkType: String? = null, // e.g. LTE, 5G
    // Capabilities
    val isWifiDirectSupported: Boolean = false,
    val is5GHzSupported: Boolean = false,
    val is6GHzSupported: Boolean = false,
    val isWifiAwareSupported: Boolean = false,
    val isP2pSupported: Boolean = false,
    val isApSupported: Boolean = false
)
