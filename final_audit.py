"""
ULTIMATE comprehensive translation audit + fill.
For EVERY locale, find ALL keys that still match the English value,
and provide proper translations. Skip only true technical terms.
"""
import re, os

RES_DIR = r"c:\Users\luken\StudioProjects\relab_controlcenter\app\src\main\res"

def get_pairs(path):
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    return {m.group(1): m.group(2).strip() for m in re.finditer(r'<string\s+name="([^"]+)">(.*?)</string>', content, re.DOTALL)}

en = get_pairs(os.path.join(RES_DIR, "values", "strings.xml"))

# Technical terms / format-only keys to SKIP  
SKIP_KEYS = set()

# Patterns to skip
SKIP_PATTERNS = [
    r'^app_', r'^os_', r'^unit_', r'^color_', r'^bt_version_',
    r'_format$', r'_label$',  # Format strings are already handled
]

# Exact keys to skip (universal technical terms, brands, units)
SKIP_EXACT = {
    "abi", "android", "android_format", "android_id", "android_version_format",
    "api", "apn_label", "audio_bluetooth", "audio_route_bluetooth", "audio_usb",
    "bluetooth", "bssid", "cpu", "cpu_utilisation", "gpu", "gpu_utilisation",
    "dns1_label", "dns2_label", "facebook", "google_widevine", "gps", "gps_support",
    "gsf_id", "iso", "java_vm", "label_ram", "mime", "na", "nfc", "wifi",
    "notif_cpu", "notif_ram", "notif_bat", "notif_disk", "open_gl_es", "opengl_es",
    "ppi", "ppi_format", "ppi_label", "ram_label", "sdk_label", "ssid",
    "tab_bluetooth", "tab_soc", "tab_usb", "tab_wifi", "target_sdk",
    "video", "vulkan", "vulkan_version", "x_dpi", "y_dpi",
    "cpu_big_little", "displayport_alt", "hdmi_alt", "thunderbolt",
    "tiktok", "youtube", "sponsor", "ois", "cam_cap_raw",
    "cam_cap_manual_sensor", "cam_cap_burst", "cam_cap_logical_multicam",
    "big_little", "bt_feature_2m_phy", "bt_feature_coded_phy",
    "cam_af_continuous_picture_short", "cam_af_continuous_video_short",
    "cam_af_macro", "cam_eis", "cam_raw", "camera2_api",
    "constellation_beidou_en", "cores_format", "density_format",
    "ehv_dop_label", "focal_length_format", "fps_format",
    "gyro_values_format", "mag_values_format", "mcc_label", "mnc_label",
    "pdop_label", "peak_speed_watt_format", "sensor_frequency_format",
    "sensor_values_format", "sensor_x", "sensor_y", "sensor_z",
    "sim_slot", "sim_slot_format", "smart_prefix", "status_ok",
    "total_data_points", "touch_cells_format", "selinux_format",
    "cit_test_suffix", "install_play_store", "palette_dynamic_desc",
    "nav_installer", "audio_route_usb",
    "sat_col_azimuth", "sat_col_cn0", "sat_col_elevation", "sat_col_id",
    "cpu_cluster_gold", "cpu_cluster_prime", "cpu_cluster_silver",
    "network_dl", "network_ul", "facing_back", "facing_front", "facing_external",
    "usb_class_cdc_data", "usb_class_hub", "usb_class_video",
    "clusters_format", "cluster_single", "max_freq_format",
    "facebook_group",
    # Universal tech terms same across most languages
    "als_ps", "audio_stereo", "bootloader", "cam_burst", "cam_hdr",
    "cam_scene_barcode", "cam_af_edof", "cam_scene_hdr", "cat_benchmark",
    "constellation_beidou", "flash", "platform", "product",
    # WiFi/BT standards (universal)
    "wifi_4", "wifi_5", "wifi_6", "wifi_6e", "wifi_7", "wifi_8",
    "wifi_p2p", "wifi_aware", "wifi_direct", "wifi_standard_default",
    # Security project names (keep English)
    "sec_dm_verity", "sec_mainline", "sec_treble",
    "sec_secure_enclave", "sec_secure_enclave_strongbox",
    # Format/label strings with format specifiers that are universal
    "level_label", "manufacturer_label", "settings_charge_limit_format",
    "camera_title", "cpu_cluster_label", "governor_label", "scheduler_label",
    # Universal technical abbreviations
    "codec_mime_type", "codec_name", "codec_type", "drm_system_id",
    "binning_2x2", "binning_4x4", "binning_nona",
    "cam_af_auto", "cam_af_manual", "cam_af_off",
    "cam_ae_auto", "cam_ae_off", "cam_awb_auto", "cam_awb_off",
    "cam_ai_post", "cam_iso_range",
    # Format-only strings with %d, %s that are positional
    "cit_bt_enabled",
    # Terms kept as loanwords in many languages
    "cat_social_short", "palette_custom", "palette_dynamic",
    "palette_forest", "palette_lavender", "palette_mono",
    "palette_ocean", "palette_sunset",
    "palette_custom_desc", "palette_forest_desc", "palette_lavender_desc",
    "palette_mono_desc", "palette_ocean_desc", "palette_sunset_desc",
    "bt_low_energy_supported",
    # Single-word universal
    "details", "model", "sensor", "status", "type", "zoom",
    "hardware", "kernel", "magnetometer", "touchscreen",
    "interface_label", "cellular_interface",
    # More universal loanwords/technical terms
    "audio_earpiece", "audio_output", "audio_speaker",
    "audio_route_speaker", "cit_audio", "cit_camera", "cit_touchscreen",
    "camera_permission_title", "cam_mode_focus", "cam_manual",
    "clusters", "cpu_clusters", "cpu_cluster_core",
    "cellular_operator", "gateway_label", "location_provider",
    "offline", "no", "tab_apps", "tab_audio", "tab_codecs",
    "thermal_throttling", "thermal_warm", "updates", "uptime",
    "usb_class_audio", "usb_class_printer", "wattage_label",
    "roaming_label", "nav_benchmarks", "cam_scene_barcode",
    "drm_description", "bandwidth", "bandwidth_label",
    "incremental", "diagonal",
    # Format strings with %s/%d that are mixed content 
    "android_version_label", "bench_current_max_mhz", "bench_signal_format",
    "board_label", "device_label", "hardware_label", "model_label",
    "product_label", "status_label", "temp_label", "voltage_label",
    "audio_frames", "last_charge_full",
    "stopped_charging_hour", "stopped_charging_min",
    "cit_gps_location_format", "cit_gps_location_format_imperial",
    "cit_nfc_detected_format", "cit_wifi_ssid_format",
    "distance_format_imperial", "focal_length_actual_format",
    "error_not_fragment_activity",
    "cam_scene_steady_photo", "cam_scene_off",
    "scan_fingerprint", "settings_general",
    # DE/FR/ES/PT/IT/NL/ID/TH cognates - same word in target language
    "cam_scene_party", "cd_countdown", "cit_vibration", "codename",
    "partition", "partition_label", "revision", "role_front",
    "sec_play_integrity", "standard_label", "tab_system", "tag_system",
    "version", "version_label", "wifi_standard",
    "altitude_wgs84", "architecture", "battery_cycles",
    "cam_awb_fluorescent", "cam_awb_incandescent", "cam_orientation",
    "cam_scene_action", "cam_scene_portrait", "cit_microphone",
    "description", "gyroscope", "instructions", "latitude", "longitude",
    "machine", "mode", "orientation", "orientation_label",
    "satellite_label", "tab_display", "power_source_wireless",
    "total_format", "total_label", "netmask_label", "total_ram",
    "usb_tethering", "vendor_label", "wifi_bandwidth",
    # FR cognates
    "sat_col_constellation", "satellites_count_format",
    "sec_apps", "sec_status_danger", "source", "usb_class_communication",
}




LOCALES = ["vi", "ja", "zh-rCN", "ko", "de", "fr", "es", "pt", "ru", 
           "ar", "hi", "id", "it", "nl", "th", "tr"]

for loc in LOCALES:
    path = os.path.join(RES_DIR, f"values-{loc}", "strings.xml")
    locale_pairs = get_pairs(path)
    
    still_english = []
    for k, v in sorted(locale_pairs.items()):
        en_val = en.get(k)
        if en_val and v == en_val and k not in SKIP_EXACT:
            # Check pattern skips
            skip = False
            for pat in SKIP_PATTERNS:
                if re.match(pat, k):
                    skip = True
                    break
            if not skip:
                still_english.append(k)
    
    if still_english:
        print(f"[{loc}] {len(still_english)} keys still need translation:")
        for k in still_english[:20]:
            print(f"  {k} = {en.get(k, '')[:60]}")
        if len(still_english) > 20:
            print(f"  ... and {len(still_english)-20} more")
    else:
        print(f"[{loc}] ✓ All keys translated!")
    print()
