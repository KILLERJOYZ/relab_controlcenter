package com.example.relab_tool.data

import android.content.Context
import com.example.relab_tool.R
import com.example.relab_tool.model.*
import java.text.Normalizer

/**
 * Centralized search engine that builds a unified index from all device info
 * data sources and performs advanced search with synonym expansion.
 *
 * Architecture:
 * 1. buildIndex() flattens all data classes into List<SearchableItem>
 * 2. search() expands the query via synonym groups, then filters
 *    against both label (localized) and value (runtime) using
 *    case-insensitive partial matching.
 */
class SearchEngine(private val context: Context) {

    // ── Synonym groups (loaded once from string-array resources) ──────────

    private val synonymGroups: List<List<String>> by lazy {
        val arrayIds = listOf(
            R.array.synonyms_wifi, R.array.synonyms_cpu,
            R.array.synonyms_display, R.array.synonyms_battery,
            R.array.synonyms_memory, R.array.synonyms_camera,
            R.array.synonyms_bluetooth, R.array.synonyms_network,
            R.array.synonyms_security, R.array.synonyms_audio,
            R.array.synonyms_gpu, R.array.synonyms_sensor,
            R.array.synonyms_usb
        )
        arrayIds.map { id ->
            try {
                context.resources.getStringArray(id).map { it.normalize() }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Builds the complete unified search index from all available device info.
     * Called on Dispatchers.Default — safe for heavy computation.
     */
    fun buildIndex(
        summary: DeviceSummary?,
        system: SystemInfo?,
        cpu: CpuInfo?,
        battery: BatteryInfo?,
        display: DisplayInfo?,
        memory: MemoryInfo?,
        sensors: List<SensorInfo>,
        soc: SocInfo?,
        cameras: List<CameraInfo>,
        bluetooth: BluetoothInfo?,
        network: NetworkInfo?,
        cellular: CellularInfo?,
        audio: AudioInfo?,
        security: SecurityInfo?,
        usb: UsbStatusInfo?,
        drm: List<DrmSchemeInfo>,
        thermals: List<ThermalInfo>,
        codecs: List<CodecInfo>
    ): List<SearchableItem> {
        val items = mutableListOf<SearchableItem>()
        summary?.let  { items += indexSummary(it) }
        system?.let   { items += indexSystem(it) }
        cpu?.let      { items += indexCpu(it) }
        battery?.let  { items += indexBattery(it) }
        display?.let  { items += indexDisplay(it) }
        memory?.let   { items += indexMemory(it) }
        soc?.let      { items += indexSoc(it) }
        bluetooth?.let { items += indexBluetooth(it) }
        network?.let  { items += indexNetwork(it) }
        cellular?.let { items += indexCellular(it) }
        audio?.let    { items += indexAudio(it) }
        security?.let { items += indexSecurity(it) }
        usb?.let      { items += indexUsb(it) }
        items += indexSensors(sensors)
        items += indexCameras(cameras)
        items += indexDrm(drm)
        items += indexThermals(thermals)
        items += indexCodecs(codecs)
        return items
    }

    /**
     * Searches the index using synonym-expanded, case-insensitive partial matching.
     * Evaluates both the localized label AND the runtime value.
     * Returns results grouped by category.
     */
    fun search(query: String, index: List<SearchableItem>): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val normalizedQuery = query.normalize()
        val expandedTerms = expandWithSynonyms(normalizedQuery)

        return index.filter { item ->
            val label = context.getString(item.labelResId).normalize()
            val value = item.value.normalize()
            expandedTerms.any { term ->
                label.contains(term) || value.contains(term)
            }
        }.map { item ->
            SearchResult(
                label = context.getString(item.labelResId),
                value = item.value,
                category = context.getString(item.categoryResId),
                categoryResId = item.categoryResId
            )
        }
    }

    // ── Synonym expansion ────────────────────────────────────────────────

    /**
     * If the query matches any word in a synonym group, expands the search
     * to include all words in that group plus the original query.
     */
    private fun expandWithSynonyms(normalizedQuery: String): Set<String> {
        val terms = mutableSetOf(normalizedQuery)
        for (group in synonymGroups) {
            if (group.any { normalizedQuery.contains(it) || it.contains(normalizedQuery) }) {
                terms.addAll(group)
            }
        }
        return terms
    }

    // ── Index builders (one per data class) ──────────────────────────────

    private fun indexSummary(s: DeviceSummary): List<SearchableItem> = buildList {
        val c = R.string.tab_device
        item(R.string.model, s.model, c)
        item(R.string.manufacturer, s.manufacturer, c)
        item(R.string.board, s.board, c)
        item(R.string.hardware, s.hardware, c)
        item(R.string.android_id, s.androidId, c)
        item(R.string.gsf_id, s.gsfId, c)
        item(R.string.platform, s.platform, c)
        item(R.string.android_version, s.androidVersion, c)
        item(R.string.kernel, s.kernel, c)
        item(R.string.resolution, s.resolution, c)
        item(R.string.processor, s.cpuModel, c)
        item(R.string.gpu, s.gpuModel, c)
        item(R.string.touchscreen, s.touchscreen, c)
        item(R.string.accelerometer, s.accelerometer, c)
        item(R.string.magnetometer, s.magnetometer, c)
        item(R.string.gyroscope, s.gyroscope, c)
        item(R.string.charger, s.charger, c)
        item(R.string.nfc, s.nfc, c)
        item(R.string.fingerprint, s.fingerprintSensor, c)
        item(R.string.wifi, s.wifiChip, c)
        item(R.string.sound, s.soundChip, c)
        item(R.string.ram_type_label, s.ramType, c)
        item(R.string.storage_type, s.flashType, c)
    }

    private fun indexSystem(s: SystemInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_system
        item(R.string.brand, s.brand, c)
        item(R.string.manufacturer, s.manufacturer, c)
        item(R.string.model, s.model, c)
        item(R.string.model_name, s.modelName, c)
        item(R.string.android_version, s.androidVersion, c)
        item(R.string.os_name, s.osVersion, c)
        item(R.string.codename, s.codeName, c)
        item(R.string.api, s.sdkLevel, c)
        item(R.string.product, s.product, c)
        item(R.string.board, s.board, c)
        item(R.string.platform, s.platform, c)
        item(R.string.build_id, s.buildId, c)
        item(R.string.java_vm, s.javaVm, c)
        item(R.string.security_patch, s.securityPatch, c)
        item(R.string.baseband, s.baseband, c)
        item(R.string.gps, s.gps, c)
        item(R.string.bluetooth, s.bluetoothVersion, c)
        item(R.string.build_type, s.buildType, c)
        item(R.string.tags, s.tags, c)
        item(R.string.incremental, s.incremental, c)
        item(R.string.description, s.description, c)
        item(R.string.fingerprint, s.fingerprint, c)
        item(R.string.build_date, s.buildDate, c)
        item(R.string.builder, s.builder, c)
        item(R.string.bootloader, s.bootloader, c)
        item(R.string.kernel, s.kernel, c)
        item(R.string.opengl_es, s.openGlEs, c)
        item(R.string.gms, s.googlePlayServices, c)
        item(R.string.device_features, s.deviceFeatures, c)
        item(R.string.language, s.language, c)
        item(R.string.timezone, s.timezone, c)
        item(R.string.uptime, s.uptime, c)
    }

    private fun indexCpu(i: CpuInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_soc
        item(R.string.cpu_model, i.processor, c)
        item(R.string.architecture, i.architecture, c)
        item(R.string.cpu_cores, i.cores.toString(), c)
        item(R.string.supported_abi, i.supportedAbis, c)
        item(R.string.governor, i.cpuGovernor, c)
    }

    private fun indexBattery(i: BatteryInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_battery
        item(R.string.health, i.health, c)
        item(R.string.battery_level, i.levelString, c)
        item(R.string.battery_status, i.status, c)
        item(R.string.power_source, i.powerSource, c)
        item(R.string.technology, i.technology, c)
        item(R.string.battery_temperature, i.temperature, c)
        item(R.string.battery_wattage, i.wattage, c)
        item(R.string.battery_capacity, i.capacity, c)
        item(R.string.current_now, i.currentNow, c)
        item(R.string.charge_counter, i.chargeCounter, c)
        item(R.string.battery_wear, i.wear, c)
        item(R.string.actual_capacity, i.actualCapacity, c)
        item(R.string.battery_voltage, i.voltage, c)
    }

    private fun indexDisplay(i: DisplayInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_display
        item(R.string.screen_resolution, i.currentResolution, c)
        item(R.string.highest_resolution, i.highestResolution, c)
        item(R.string.aspect_ratio, i.aspectRatio, c)
        item(R.string.density, i.density, c)
        item(R.string.x_dpi, i.xDpi, c)
        item(R.string.y_dpi, i.yDpi, c)
        item(R.string.ppi, i.ppi, c)
        item(R.string.refresh_rate, i.currentRefreshRate, c)
        item(R.string.supported_rates, i.supportedRates, c)
        item(R.string.hdr_support, i.hdrSupport, c)
        item(R.string.wide_color_gamut, if (i.wideColorGamut) "Yes" else "No", c)
        item(R.string.screen_size, i.physicalSize, c)
        item(R.string.diagonal, i.diagonal, c)
        item(R.string.brightness, i.brightnessLevel, c)
        item(R.string.screen_timeout, i.screenTimeout, c)
        item(R.string.orientation_label, i.orientation, c)
        item(R.string.color_depth, i.colorDepth, c)
        item(R.string.widevine_level, i.widevineLevel, c)
        item(R.string.color_space, i.colorSpace, c)
    }

    private fun indexMemory(i: MemoryInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_memory
        item(R.string.total_ram, i.totalRam, c)
        item(R.string.available_ram, i.availableRam, c)
        item(R.string.ram_type_label, i.ramType, c)
        item(R.string.zram_total, i.zRamTotal, c)
        item(R.string.zram_used, i.zRamUsed, c)
        item(R.string.total_storage, i.internalTotal, c)
        item(R.string.internal_free, i.internalFree, c)
        item(R.string.internal_used, i.internalUsed, c)
        item(R.string.system_usage, i.internalUsedBySystem, c)
        item(R.string.apps_usage, i.internalUsedByApps, c)
        item(R.string.fs_type, i.internalFsType, c)
        item(R.string.block_size, i.internalBlockSize, c)
        item(R.string.memory_page_size, i.memoryPageSize, c)
        item(R.string.partition, i.internalPartition, c)
        item(R.string.storage_type, i.flashType, c)
    }

    private fun indexSoc(i: SocInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_soc
        item(R.string.processor, i.processor, c)
        item(R.string.vendor, i.vendor, c)
        item(R.string.cores, i.cores, c)
        item(R.string.cpu_big_little, i.bigLittle, c)
        item(R.string.clusters, i.clusters, c)
        item(R.string.family, i.family, c)
        item(R.string.mode, i.mode, c)
        item(R.string.machine, i.machine, c)
        item(R.string.abi, i.abi, c)
        item(R.string.instructions, i.instructions, c)
        item(R.string.revision, i.revision, c)
        item(R.string.clock_speed, i.clockSpeed, c)
        item(R.string.governor, i.governor, c)
        item(R.string.supported_abi, i.supportedAbi, c)
        item(R.string.gpu, i.gpu, c)
        item(R.string.gpu_vendor, i.gpuVendor, c)
        item(R.string.gpu_architecture, i.gpuArch, c)
        item(R.string.l2_cache, i.gpuL2Cache, c)
        item(R.string.gpu_bus_width, i.gpuBusWidth, c)
        item(R.string.opengl_es, i.openGlEs, c)
        item(R.string.gpu_full_version, i.gpuFullVersion, c)
        item(R.string.vulkan_support, i.vulkanVersion, c)
        item(R.string.gpu_cores, i.gpuCores, c)
        item(R.string.gpu_clock_speed, i.gpuClockSpeed, c)
        item(R.string.process, i.process, c)
        item(R.string.instruction_sets, i.instructionSets, c)
        item(R.string.architecture, i.architecture, c)
    }

    private fun indexBluetooth(i: BluetoothInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_bluetooth
        item(R.string.bt_state, i.state, c)
        item(R.string.bt_version, i.version, c)
        item(R.string.bt_device_name, i.name, c)
        item(R.string.bt_mac_address, i.address, c)
        item(R.string.bt_paired_devices, i.pairedDevicesCount.toString(), c)
        item(R.string.connected_devices, i.connectedDevicesCount.toString(), c)
    }

    private fun indexNetwork(i: NetworkInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_network
        item(R.string.type, i.type, c)
        item(R.string.network_state, i.state, c)
        item(R.string.network_connected, if (i.isConnected) "Yes" else "No", c)
        item(R.string.ssid, i.wifiSsid, c)
        item(R.string.bssid, i.wifiBssid, c)
        item(R.string.wifi_standard, i.wifiStandard, c)
        item(R.string.ip_address, i.ipAddress, c)
        item(R.string.vendor, i.vendor, c)
        item(R.string.link_speed, i.linkSpeed, c)
        item(R.string.frequency, i.frequency, c)
        item(R.string.signal_strength, i.signalStrength, c)
        item(R.string.wifi_channel, i.channel, c)
        item(R.string.wifi_bandwidth, i.width, c)
        item(R.string.security_label, i.security, c)
        item(R.string.ipv6_address, i.ipv6Address, c)
        item(R.string.gateway_label, i.gateway, c)
        item(R.string.netmask_label, i.netmask, c)
        item(R.string.dns1_label, i.dns1, c)
        item(R.string.dns2_label, i.dns2, c)
        item(R.string.dhcp_server, i.dhcpServer, c)
        item(R.string.lease_duration, i.leaseDuration, c)
        item(R.string.interface_label, i.interfaceName, c)
        item(R.string.mac_address, i.macAddress, c)
        item(R.string.wifi_direct, if (i.isWifiDirectSupported) "Yes" else "No", c)
        item(R.string.wifi_5ghz, if (i.is5GHzSupported) "Yes" else "No", c)
        item(R.string.wifi_6ghz, if (i.is6GHzSupported) "Yes" else "No", c)
        item(R.string.wifi_aware, if (i.isWifiAwareSupported) "Yes" else "No", c)
    }

    private fun indexCellular(i: CellularInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_cellular
        item(R.string.cellular_state, i.state, c)
        item(R.string.multi_sim, i.multiSimSupport, c)
        item(R.string.phone_count, i.phoneCount.toString(), c)
        item(R.string.device_type, i.deviceType, c)
        item(R.string.cellular_operator, i.operator, c)
        item(R.string.network_type, i.networkType, c)
        item(R.string.ipv4_address, i.ipV4, c)
        item(R.string.cellular_interface, i.interfaceName, c)
        // SIM cards
        for (sim in i.simInfos) {
            val slotLabel = context.getString(R.string.sim_slot, sim.slot)
            add(SearchableItem(R.string.carrier, "$slotLabel: ${sim.carrier}", c))
            sim.phoneNumber?.let { add(SearchableItem(R.string.phone_number, "$slotLabel: $it", c)) }
            add(SearchableItem(R.string.network_type, "$slotLabel: ${sim.networkType}", c))
        }
    }

    private fun indexAudio(i: AudioInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_audio
        item(R.string.audio_low_latency, if (i.lowLatency) "Yes" else "No", c)
        item(R.string.audio_pro, if (i.proAudio) "Yes" else "No", c)
        item(R.string.audio_midi, if (i.midiSupport) "Yes" else "No", c)
        item(R.string.audio_unprocessed, if (i.unprocessedSource) "Yes" else "No", c)
        item(R.string.audio_sample_rate, i.sampleRate, c)
        item(R.string.audio_buffer_size, i.bufferSize, c)
        item(R.string.audio_bit_depth, i.bitDepth, c)
        item(R.string.audio_output_route, i.outputRoute, c)
        item(R.string.audio_sample_rate_supported, i.supportedSampleRates, c)
        item(R.string.audio_latency_ms, i.audioLatency, c)
        item(R.string.audio_codec_supported, i.supportedCodecs, c)
        item(R.string.audio_codecs, i.audioCodecs, c)
        item(R.string.video_codecs, i.videoCodecs, c)
    }

    private fun indexSecurity(i: SecurityInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_security
        item(R.string.android_version, i.androidVersion, c)
        item(R.string.codename, i.codename, c)
        item(R.string.security_patch, i.securityPatch, c)
        item(R.string.build_id, i.buildNumber, c)
        item(R.string.build_date, i.buildDate, c)
        item(R.string.architecture, i.architecture, c)
        item(R.string.instruction_sets, i.instructionSets, c)
        item(R.string.kernel, i.kernelVersion, c)
        item(R.string.sec_treble, if (i.projectTreble) "Yes" else "No", c)
        item(R.string.sec_mainline, if (i.projectMainline) "Yes" else "No", c)
        item(R.string.sec_dynamic_partitions, if (i.dynamicPartitions) "Yes" else "No", c)
        item(R.string.sec_seamless_updates, if (i.seamlessUpdates) "Yes" else "No", c)
        item(R.string.sec_active_slot, i.activeSlot, c)
        item(R.string.sec_avb_version, i.avbVersion, c)
        item(R.string.sec_verified_boot, i.verifiedBootState, c)
        item(R.string.sec_dm_verity, i.dmVerity, c)
        item(R.string.sec_root_access, i.rootAccess, c)
        item(R.string.sec_selinux, i.selinuxStatus, c)
        item(R.string.sec_encryption, i.encryptionStatus, c)
        item(R.string.sec_fingerprint_supp, if (i.hasFingerprint) "Yes" else "No", c)
        item(R.string.sec_face_supp, if (i.hasFaceUnlock) "Yes" else "No", c)
        item(R.string.sec_biometric_class, i.biometricClass, c)
        item(R.string.sec_keystore_type, i.keystoreType, c)
        item(R.string.sec_hw_keystore, if (i.hardwareBackedKeystore) "Yes" else "No", c)
        item(R.string.sec_encryption_alg, i.encryptionAlgorithm, c)
        item(R.string.sec_vpn_active, if (i.vpnActive) "Yes" else "No", c)
        item(R.string.sec_private_dns, i.privateDnsStatus, c)
        item(R.string.sec_wifi_mac_rand, if (i.randomMacEnabled) "Yes" else "No", c)
        item(R.string.sec_dev_options, if (i.developerOptionsEnabled) "Yes" else "No", c)
        item(R.string.sec_adb_enabled, if (i.adbEnabled) "Yes" else "No", c)
        item(R.string.sec_wireless_debugging, if (i.wirelessDebuggingEnabled) "Yes" else "No", c)
    }

    private fun indexUsb(i: UsbStatusInfo): List<SearchableItem> = buildList {
        val c = R.string.tab_usb
        item(R.string.usb_connection_status, if (i.isConnected) "Connected" else "Disconnected", c)
        item(R.string.usb_mode, i.usbMode, c)
        item(R.string.usb_version, i.usbVersion, c)
        item(R.string.connector_type, i.connectorType, c)
        item(R.string.max_bandwidth, i.maxBandwidth, c)
        item(R.string.otg_support, if (i.isOtgSupported) "Yes" else "No", c)
        item(R.string.host_mode, if (i.isHostModeSupported) "Yes" else "No", c)
        item(R.string.displayport_alt, i.displayPortAltMode, c)
        item(R.string.hdmi_alt, i.hdmiAltMode, c)
        item(R.string.max_video_resolution, i.maxVideoResolution, c)
        item(R.string.thunderbolt, i.isThunderboltSupported, c)
        item(R.string.adb_status, if (i.adbStatus) "Enabled" else "Disabled", c)
        item(R.string.usb_tethering, if (i.usbTetheringStatus) "Active" else "Inactive", c)
        item(R.string.restricted_usb, i.isRestrictedUsb, c)
    }

    private fun indexSensors(sensors: List<SensorInfo>): List<SearchableItem> = buildList {
        val c = R.string.tab_sensors
        for (s in sensors) {
            add(SearchableItem(R.string.sensor, s.name, c))
            add(SearchableItem(R.string.sensor_vendor, s.vendor, c))
        }
    }

    private fun indexCameras(cameras: List<CameraInfo>): List<SearchableItem> = buildList {
        val c = R.string.tab_cameras
        for (cam in cameras) {
            val prefix = "${cam.facing} #${cam.id}"
            add(SearchableItem(R.string.cam_resolution, "$prefix: ${cam.resolution}", c))
            item(R.string.sensor_model, "$prefix: ${cam.sensorModel}", c)
            item(R.string.aperture, "$prefix: ${cam.aperture}", c)
            item(R.string.focal_length, "$prefix: ${cam.focalLength}", c)
            item(R.string.focal_length_35mm, "$prefix: ${cam.focalLength35mm}", c)
            item(R.string.cam_sensor_size, "$prefix: ${cam.sensorSize}", c)
            item(R.string.cam_iso_range, "$prefix: ${cam.isoRange}", c)
            item(R.string.video, "$prefix: ${cam.videoCapabilities}", c)
            item(R.string.camera2_api, "$prefix: ${cam.camera2ApiLevel}", c)
            item(R.string.zoom, "$prefix: ${cam.zoom}", c)
            item(R.string.pixel_size, "$prefix: ${cam.pixelSize}", c)
        }
    }

    private fun indexDrm(drms: List<DrmSchemeInfo>): List<SearchableItem> = buildList {
        val c = R.string.tab_security
        for (d in drms) {
            add(SearchableItem(R.string.drm_status, d.name, c))
            item(R.string.vendor, d.vendor, c)
            item(R.string.version, d.version, c)
            item(R.string.security_level, d.securityLevel, c)
            item(R.string.max_hdcp_level, d.maxHdcpLevel, c)
        }
    }

    private fun indexThermals(thermals: List<ThermalInfo>): List<SearchableItem> = buildList {
        val c = R.string.tab_thermal
        for (t in thermals) {
            add(SearchableItem(R.string.thermal_zone, "${t.name}: ${t.temperature}", c))
        }
    }

    private fun indexCodecs(codecs: List<CodecInfo>): List<SearchableItem> = buildList {
        val c = R.string.tab_codecs
        for (cd in codecs) {
            add(SearchableItem(R.string.codec_name, cd.name, c))
            add(SearchableItem(R.string.codec_mime_type, cd.mimeType, c))
            add(SearchableItem(R.string.codec_type, cd.type, c))
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Extension to add a SearchableItem only if the value is meaningful. */
    private fun MutableList<SearchableItem>.item(labelRes: Int, value: String?, catRes: Int) {
        if (!value.isNullOrBlank() && value != "Unknown" && value != "N/A" && value != "unknown") {
            add(SearchableItem(labelRes, value, catRes))
        }
    }

    companion object {
        /** Unicode-safe normalization for diacritics + case folding. */
        fun String.normalize(): String {
            val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
            return temp.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .lowercase()
                .replace("đ", "d")
                .replace("Đ", "d")
                .trim()
        }
    }
}
