package com.example.relab_tool.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.relab_tool.R

data class CardDetailsInfo(
    val id: String,
    val titleRes: Int,
    val explanationRes: Int,
    val howItWorksRes: Int,
    val whyItMattersRes: Int,
    val icon: ImageVector,
    val settingsIntentAction: String
)

object CardDetailsHelper {
    fun getDetailsForCard(cardId: String): CardDetailsInfo? {
        return cardDetailsMap[cardId]
    }

    fun launchSettingsIntent(context: Context, cardId: String) {
        val details = getDetailsForCard(cardId) ?: return
        val action = details.settingsIntentAction

        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                // Ignore failure if Settings cannot be opened at all
            }
        }
    }

    private val cardDetailsMap = mapOf(
        // --- Core Status Cards (Custom Content) ---
        "device_model" to CardDetailsInfo(
            id = "device_model",
            titleRes = R.string.model,
            explanationRes = R.string.card_explanation_device_model,
            howItWorksRes = R.string.card_how_it_works_device_model,
            whyItMattersRes = R.string.card_why_it_matters_device_model,
            icon = Icons.Outlined.Smartphone,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "android_version" to CardDetailsInfo(
            id = "android_version",
            titleRes = R.string.android_version,
            explanationRes = R.string.card_explanation_android_version,
            howItWorksRes = R.string.card_how_it_works_android_version,
            whyItMattersRes = R.string.card_why_it_matters_android_version,
            icon = Icons.Outlined.Android,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "os_version" to CardDetailsInfo(
            id = "os_version",
            titleRes = R.string.ui_version,
            explanationRes = R.string.card_explanation_os_version,
            howItWorksRes = R.string.card_how_it_works_os_version,
            whyItMattersRes = R.string.card_why_it_matters_os_version,
            icon = Icons.Outlined.AutoAwesome,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "soc" to CardDetailsInfo(
            id = "soc",
            titleRes = R.string.cpu_model,
            explanationRes = R.string.card_explanation_soc,
            howItWorksRes = R.string.card_how_it_works_soc,
            whyItMattersRes = R.string.card_why_it_matters_soc,
            icon = Icons.Outlined.Memory,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "battery_main" to CardDetailsInfo(
            id = "battery_main",
            titleRes = R.string.battery_status,
            explanationRes = R.string.card_explanation_battery_main,
            howItWorksRes = R.string.card_how_it_works_battery_main,
            whyItMattersRes = R.string.card_why_it_matters_battery_main,
            icon = Icons.Outlined.BatteryStd,
            settingsIntentAction = "android.intent.action.POWER_USAGE_SUMMARY"
        ),

        // --- Core Status Cards (Template-based Fallback) ---
        "cores" to CardDetailsInfo(
            id = "cores",
            titleRes = R.string.cpu_cores,
            explanationRes = R.string.card_explanation_cores,
            howItWorksRes = R.string.card_how_it_works_cores,
            whyItMattersRes = R.string.card_why_it_matters_cores,
            icon = Icons.Outlined.Numbers,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "gpu" to CardDetailsInfo(
            id = "gpu",
            titleRes = R.string.gpu,
            explanationRes = R.string.card_explanation_gpu,
            howItWorksRes = R.string.card_how_it_works_gpu,
            whyItMattersRes = R.string.card_why_it_matters_gpu,
            icon = Icons.Outlined.GraphicEq,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "battery_temp" to CardDetailsInfo(
            id = "battery_temp",
            titleRes = R.string.battery_temperature,
            explanationRes = R.string.card_explanation_battery_temp,
            howItWorksRes = R.string.card_how_it_works_battery_temp,
            whyItMattersRes = R.string.card_why_it_matters_battery_temp,
            icon = Icons.Outlined.Thermostat,
            settingsIntentAction = "android.intent.action.POWER_USAGE_SUMMARY"
        ),
        "battery_wattage" to CardDetailsInfo(
            id = "battery_wattage",
            titleRes = R.string.battery_wattage,
            explanationRes = R.string.card_explanation_battery_wattage,
            howItWorksRes = R.string.card_how_it_works_battery_wattage,
            whyItMattersRes = R.string.card_why_it_matters_battery_wattage,
            icon = Icons.Outlined.Bolt,
            settingsIntentAction = "android.intent.action.POWER_USAGE_SUMMARY"
        ),
        "battery_power" to CardDetailsInfo(
            id = "battery_power",
            titleRes = R.string.power_source,
            explanationRes = R.string.card_explanation_battery_power,
            howItWorksRes = R.string.card_how_it_works_battery_power,
            whyItMattersRes = R.string.card_why_it_matters_battery_power,
            icon = Icons.Outlined.Power,
            settingsIntentAction = "android.intent.action.POWER_USAGE_SUMMARY"
        ),
        "battery_wireless" to CardDetailsInfo(
            id = "battery_wireless",
            titleRes = R.string.wireless_charging,
            explanationRes = R.string.card_explanation_battery_wireless,
            howItWorksRes = R.string.card_how_it_works_battery_wireless,
            whyItMattersRes = R.string.card_why_it_matters_battery_wireless,
            icon = Icons.Outlined.Contactless,
            settingsIntentAction = "android.intent.action.POWER_USAGE_SUMMARY"
        ),
        "display_resolution" to CardDetailsInfo(
            id = "display_resolution",
            titleRes = R.string.screen_resolution,
            explanationRes = R.string.card_explanation_display_resolution,
            howItWorksRes = R.string.card_how_it_works_display_resolution,
            whyItMattersRes = R.string.card_why_it_matters_display_resolution,
            icon = Icons.Outlined.Monitor,
            settingsIntentAction = Settings.ACTION_DISPLAY_SETTINGS
        ),
        "display_color_depth" to CardDetailsInfo(
            id = "display_color_depth",
            titleRes = R.string.color_depth,
            explanationRes = R.string.card_explanation_display_color_depth,
            howItWorksRes = R.string.card_how_it_works_display_color_depth,
            whyItMattersRes = R.string.card_why_it_matters_display_color_depth,
            icon = Icons.Outlined.Palette,
            settingsIntentAction = Settings.ACTION_DISPLAY_SETTINGS
        ),
        "display_refresh_rate" to CardDetailsInfo(
            id = "display_refresh_rate",
            titleRes = R.string.refresh_rate,
            explanationRes = R.string.card_explanation_display_refresh_rate,
            howItWorksRes = R.string.card_how_it_works_display_refresh_rate,
            whyItMattersRes = R.string.card_why_it_matters_display_refresh_rate,
            icon = Icons.Outlined.Refresh,
            settingsIntentAction = Settings.ACTION_DISPLAY_SETTINGS
        ),
        "display_size" to CardDetailsInfo(
            id = "display_size",
            titleRes = R.string.physical_size,
            explanationRes = R.string.card_explanation_display_size,
            howItWorksRes = R.string.card_how_it_works_display_size,
            whyItMattersRes = R.string.card_why_it_matters_display_size,
            icon = Icons.Outlined.AspectRatio,
            settingsIntentAction = Settings.ACTION_DISPLAY_SETTINGS
        ),
        "display_gamut" to CardDetailsInfo(
            id = "display_gamut",
            titleRes = R.string.wide_color_gamut,
            explanationRes = R.string.card_explanation_display_gamut,
            howItWorksRes = R.string.card_how_it_works_display_gamut,
            whyItMattersRes = R.string.card_why_it_matters_display_gamut,
            icon = Icons.Outlined.Palette,
            settingsIntentAction = Settings.ACTION_DISPLAY_SETTINGS
        ),
        "bluetooth_version" to CardDetailsInfo(
            id = "bluetooth_version",
            titleRes = R.string.bt_version,
            explanationRes = R.string.card_explanation_bluetooth_version,
            howItWorksRes = R.string.card_how_it_works_bluetooth_version,
            whyItMattersRes = R.string.card_why_it_matters_bluetooth_version,
            icon = Icons.Outlined.Bluetooth,
            settingsIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS
        ),
        "bluetooth_paired" to CardDetailsInfo(
            id = "bluetooth_paired",
            titleRes = R.string.bt_paired_devices,
            explanationRes = R.string.card_explanation_bluetooth_paired,
            howItWorksRes = R.string.card_how_it_works_bluetooth_paired,
            whyItMattersRes = R.string.card_why_it_matters_bluetooth_paired,
            icon = Icons.Outlined.Devices,
            settingsIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS
        ),
        "bluetooth_connected" to CardDetailsInfo(
            id = "bluetooth_connected",
            titleRes = R.string.connected_devices,
            explanationRes = R.string.card_explanation_bluetooth_connected,
            howItWorksRes = R.string.card_how_it_works_bluetooth_connected,
            whyItMattersRes = R.string.card_why_it_matters_bluetooth_connected,
            icon = Icons.Outlined.BluetoothConnected,
            settingsIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS
        ),
        "wifi_card" to CardDetailsInfo(
            id = "wifi_card",
            titleRes = R.string.wifi_details_label,
            explanationRes = R.string.card_explanation_wifi_card,
            howItWorksRes = R.string.card_how_it_works_wifi_card,
            whyItMattersRes = R.string.card_why_it_matters_wifi_card,
            icon = Icons.Outlined.Wifi,
            settingsIntentAction = Settings.ACTION_WIFI_SETTINGS
        ),
        "cellular_card" to CardDetailsInfo(
            id = "cellular_card",
            titleRes = R.string.cellular_state,
            explanationRes = R.string.card_explanation_cellular_card,
            howItWorksRes = R.string.card_how_it_works_cellular_card,
            whyItMattersRes = R.string.card_why_it_matters_cellular_card,
            icon = Icons.Outlined.SignalCellularAlt,
            settingsIntentAction = Settings.ACTION_WIRELESS_SETTINGS
        ),
        "apps_installed" to CardDetailsInfo(
            id = "apps_installed",
            titleRes = R.string.apps_installed,
            explanationRes = R.string.card_explanation_apps_installed,
            howItWorksRes = R.string.card_how_it_works_apps_installed,
            whyItMattersRes = R.string.card_why_it_matters_apps_installed,
            icon = Icons.Outlined.Apps,
            settingsIntentAction = Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
        ),
        "security_patch" to CardDetailsInfo(
            id = "security_patch",
            titleRes = R.string.security_patch,
            explanationRes = R.string.card_explanation_security_patch,
            howItWorksRes = R.string.card_how_it_works_security_patch,
            whyItMattersRes = R.string.card_why_it_matters_security_patch,
            icon = Icons.Outlined.Security,
            settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
        ),
        "cameras_aperture" to CardDetailsInfo(
            id = "cameras_aperture",
            titleRes = R.string.aperture,
            explanationRes = R.string.card_explanation_cameras_aperture,
            howItWorksRes = R.string.card_how_it_works_cameras_aperture,
            whyItMattersRes = R.string.card_why_it_matters_cameras_aperture,
            icon = Icons.Outlined.BrightnessLow,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "cameras_focal" to CardDetailsInfo(
            id = "cameras_focal",
            titleRes = R.string.focal_length,
            explanationRes = R.string.card_explanation_cameras_focal,
            howItWorksRes = R.string.card_how_it_works_cameras_focal,
            whyItMattersRes = R.string.card_why_it_matters_cameras_focal,
            icon = Icons.Outlined.Straighten,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "cameras_pixel" to CardDetailsInfo(
            id = "cameras_pixel",
            titleRes = R.string.cam_pixel_size,
            explanationRes = R.string.card_explanation_cameras_pixel,
            howItWorksRes = R.string.card_how_it_works_cameras_pixel,
            whyItMattersRes = R.string.card_why_it_matters_cameras_pixel,
            icon = Icons.Outlined.BlurOn,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "cameras_resolution" to CardDetailsInfo(
            id = "cameras_resolution",
            titleRes = R.string.cam_resolution,
            explanationRes = R.string.card_explanation_cameras_resolution,
            howItWorksRes = R.string.card_how_it_works_cameras_resolution,
            whyItMattersRes = R.string.card_why_it_matters_cameras_resolution,
            icon = Icons.Outlined.SensorWindow,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "cameras_video" to CardDetailsInfo(
            id = "cameras_video",
            titleRes = R.string.cam_video_quality,
            explanationRes = R.string.card_explanation_cameras_video,
            howItWorksRes = R.string.card_how_it_works_cameras_video,
            whyItMattersRes = R.string.card_why_it_matters_cameras_video,
            icon = Icons.Outlined.Videocam,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),

        // --- Group Cards (Custom Content) ---
        "group_sec_system_status" to CardDetailsInfo(
            id = "group_sec_system_status",
            titleRes = R.string.sec_system_status,
            explanationRes = R.string.card_sys_status_explanation,
            howItWorksRes = R.string.card_sys_status_how_it_works,
            whyItMattersRes = R.string.card_sys_status_why_it_matters,
            icon = Icons.Outlined.Shield,
            settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
        ),
        "group_camera_specs" to CardDetailsInfo(
            id = "group_camera_specs",
            titleRes = R.string.cam_technical_params,
            explanationRes = R.string.card_cam_specs_explanation,
            howItWorksRes = R.string.card_cam_specs_how_it_works,
            whyItMattersRes = R.string.card_cam_specs_why_it_matters,
            icon = Icons.Outlined.PhotoCamera,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),

        // --- Group Cards (Template-based Fallback) ---
        "group_hardware_platform" to CardDetailsInfo(
            id = "group_hardware_platform",
            titleRes = R.string.hardware_platform,
            explanationRes = R.string.card_explanation_group_hardware_platform,
            howItWorksRes = R.string.card_how_it_works_group_hardware_platform,
            whyItMattersRes = R.string.card_why_it_matters_group_hardware_platform,
            icon = Icons.Outlined.SettingsInputComponent,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "group_system" to CardDetailsInfo(
            id = "group_system",
            titleRes = R.string.tab_system,
            explanationRes = R.string.card_explanation_group_system,
            howItWorksRes = R.string.card_how_it_works_group_system,
            whyItMattersRes = R.string.card_why_it_matters_group_system,
            icon = Icons.Outlined.Android,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "group_build_details" to CardDetailsInfo(
            id = "group_build_details",
            titleRes = R.string.build_details,
            explanationRes = R.string.card_explanation_group_build_details,
            howItWorksRes = R.string.card_how_it_works_group_build_details,
            whyItMattersRes = R.string.card_why_it_matters_group_build_details,
            icon = Icons.Outlined.Build,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "group_screen" to CardDetailsInfo(
            id = "group_screen",
            titleRes = R.string.screen,
            explanationRes = R.string.card_explanation_group_screen,
            howItWorksRes = R.string.card_how_it_works_group_screen,
            whyItMattersRes = R.string.card_why_it_matters_group_screen,
            icon = Icons.Outlined.DisplaySettings,
            settingsIntentAction = Settings.ACTION_DISPLAY_SETTINGS
        ),
        "group_system_identifiers" to CardDetailsInfo(
            id = "group_system_identifiers",
            titleRes = R.string.system_identifiers,
            explanationRes = R.string.card_explanation_group_system_identifiers,
            howItWorksRes = R.string.card_how_it_works_group_system_identifiers,
            whyItMattersRes = R.string.card_why_it_matters_group_system_identifiers,
            icon = Icons.Outlined.Fingerprint,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "group_miscellaneous" to CardDetailsInfo(
            id = "group_miscellaneous",
            titleRes = R.string.miscellaneous,
            explanationRes = R.string.card_explanation_group_miscellaneous,
            howItWorksRes = R.string.card_how_it_works_group_miscellaneous,
            whyItMattersRes = R.string.card_why_it_matters_group_miscellaneous,
            icon = Icons.Outlined.Dashboard,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "group_filesystem_details" to CardDetailsInfo(
            id = "group_filesystem_details",
            titleRes = R.string.filesystem_details_label,
            explanationRes = R.string.card_explanation_group_filesystem_details,
            howItWorksRes = R.string.card_how_it_works_group_filesystem_details,
            whyItMattersRes = R.string.card_why_it_matters_group_filesystem_details,
            icon = Icons.Outlined.Dns,
            settingsIntentAction = Settings.ACTION_INTERNAL_STORAGE_SETTINGS
        ),
        "group_connection" to CardDetailsInfo(
            id = "group_connection",
            titleRes = R.string.connection,
            explanationRes = R.string.card_explanation_group_connection,
            howItWorksRes = R.string.card_how_it_works_group_connection,
            whyItMattersRes = R.string.card_why_it_matters_group_connection,
            icon = Icons.Outlined.Wifi,
            settingsIntentAction = Settings.ACTION_WIRELESS_SETTINGS
        ),
        "group_wifi_details" to CardDetailsInfo(
            id = "group_wifi_details",
            titleRes = R.string.wifi_details_label,
            explanationRes = R.string.card_explanation_group_wifi_details,
            howItWorksRes = R.string.card_how_it_works_group_wifi_details,
            whyItMattersRes = R.string.card_why_it_matters_group_wifi_details,
            icon = Icons.Outlined.SettingsInputAntenna,
            settingsIntentAction = Settings.ACTION_WIFI_SETTINGS
        ),
        "group_wifi_dhcp" to CardDetailsInfo(
            id = "group_wifi_dhcp",
            titleRes = R.string.wifi_details_label,
            explanationRes = R.string.card_explanation_group_wifi_dhcp,
            howItWorksRes = R.string.card_how_it_works_group_wifi_dhcp,
            whyItMattersRes = R.string.card_why_it_matters_group_wifi_dhcp,
            icon = Icons.Outlined.Router,
            settingsIntentAction = Settings.ACTION_WIFI_SETTINGS
        ),
        "group_sim_status" to CardDetailsInfo(
            id = "group_sim_status",
            titleRes = R.string.sim_support,
            explanationRes = R.string.card_explanation_group_sim_status,
            howItWorksRes = R.string.card_how_it_works_group_sim_status,
            whyItMattersRes = R.string.card_why_it_matters_group_sim_status,
            icon = Icons.Outlined.SimCard,
            settingsIntentAction = Settings.ACTION_WIRELESS_SETTINGS
        ),
        "group_carrier_details" to CardDetailsInfo(
            id = "group_carrier_details",
            titleRes = R.string.carrier_label,
            explanationRes = R.string.card_explanation_group_carrier_details,
            howItWorksRes = R.string.card_how_it_works_group_carrier_details,
            whyItMattersRes = R.string.card_why_it_matters_group_carrier_details,
            icon = Icons.Outlined.CellTower,
            settingsIntentAction = Settings.ACTION_WIRELESS_SETTINGS
        ),
        "group_sec_biometrics" to CardDetailsInfo(
            id = "group_sec_biometrics",
            titleRes = R.string.sec_biometrics,
            explanationRes = R.string.card_explanation_group_sec_biometrics,
            howItWorksRes = R.string.card_how_it_works_group_sec_biometrics,
            whyItMattersRes = R.string.card_why_it_matters_group_sec_biometrics,
            icon = Icons.Outlined.Fingerprint,
            settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
        ),
        "group_sec_play_integrity" to CardDetailsInfo(
            id = "group_sec_play_integrity",
            titleRes = R.string.sec_play_integrity,
            explanationRes = R.string.card_explanation_group_sec_play_integrity,
            howItWorksRes = R.string.card_how_it_works_group_sec_play_integrity,
            whyItMattersRes = R.string.card_why_it_matters_group_sec_play_integrity,
            icon = Icons.Outlined.Lock,
            settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
        ),
        "group_sec_encryption_storage" to CardDetailsInfo(
            id = "group_sec_encryption_storage",
            titleRes = R.string.sec_encryption_storage,
            explanationRes = R.string.card_explanation_group_sec_encryption_storage,
            howItWorksRes = R.string.card_how_it_works_group_sec_encryption_storage,
            whyItMattersRes = R.string.card_why_it_matters_group_sec_encryption_storage,
            icon = Icons.Outlined.EnhancedEncryption,
            settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
        ),
        "group_sec_network_conn" to CardDetailsInfo(
            id = "group_sec_network_conn",
            titleRes = R.string.sec_network_conn,
            explanationRes = R.string.card_explanation_group_sec_network_conn,
            howItWorksRes = R.string.card_how_it_works_group_sec_network_conn,
            whyItMattersRes = R.string.card_why_it_matters_group_sec_network_conn,
            icon = Icons.Outlined.Security,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "group_sec_perm_audit" to CardDetailsInfo(
            id = "group_sec_perm_audit",
            titleRes = R.string.sec_perm_audit,
            explanationRes = R.string.card_explanation_group_sec_perm_audit,
            howItWorksRes = R.string.card_how_it_works_group_sec_perm_audit,
            whyItMattersRes = R.string.card_why_it_matters_group_sec_perm_audit,
            icon = Icons.Outlined.FolderShared,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "group_sec_apps" to CardDetailsInfo(
            id = "group_sec_apps",
            titleRes = R.string.sec_apps,
            explanationRes = R.string.card_explanation_group_sec_apps,
            howItWorksRes = R.string.card_how_it_works_group_sec_apps,
            whyItMattersRes = R.string.card_why_it_matters_group_sec_apps,
            icon = Icons.Outlined.Apps,
            settingsIntentAction = Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
        ),
        "group_sec_dev_options" to CardDetailsInfo(
            id = "group_sec_dev_options",
            titleRes = R.string.sec_dev_options,
            explanationRes = R.string.card_explanation_group_sec_dev_options,
            howItWorksRes = R.string.card_how_it_works_group_sec_dev_options,
            whyItMattersRes = R.string.card_why_it_matters_group_sec_dev_options,
            icon = Icons.Outlined.DeveloperMode,
            settingsIntentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        ),
        "group_health_specs" to CardDetailsInfo(
            id = "group_health_specs",
            titleRes = R.string.health_specs,
            explanationRes = R.string.card_explanation_group_health_specs,
            howItWorksRes = R.string.card_how_it_works_group_health_specs,
            whyItMattersRes = R.string.card_why_it_matters_group_health_specs,
            icon = Icons.Outlined.HealthAndSafety,
            settingsIntentAction = "android.intent.action.POWER_USAGE_SUMMARY"
        ),
        "group_tab_memory" to CardDetailsInfo(
            id = "group_tab_memory",
            titleRes = R.string.tab_memory,
            explanationRes = R.string.card_explanation_group_tab_memory,
            howItWorksRes = R.string.card_how_it_works_group_tab_memory,
            whyItMattersRes = R.string.card_why_it_matters_group_tab_memory,
            icon = Icons.Outlined.Memory,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "group_internal_storage" to CardDetailsInfo(
            id = "group_internal_storage",
            titleRes = R.string.internal_storage,
            explanationRes = R.string.card_explanation_group_internal_storage,
            howItWorksRes = R.string.card_how_it_works_group_internal_storage,
            whyItMattersRes = R.string.card_why_it_matters_group_internal_storage,
            icon = Icons.Outlined.SdStorage,
            settingsIntentAction = Settings.ACTION_INTERNAL_STORAGE_SETTINGS
        ),
        "group_battery_history" to CardDetailsInfo(
            id = "group_battery_history",
            titleRes = R.string.battery_history_title,
            explanationRes = R.string.card_explanation_group_battery_history,
            howItWorksRes = R.string.card_how_it_works_group_battery_history,
            whyItMattersRes = R.string.card_why_it_matters_group_battery_history,
            icon = Icons.Outlined.History,
            settingsIntentAction = "android.intent.action.POWER_USAGE_SUMMARY"
        ),
        "group_cpu" to CardDetailsInfo(
            id = "group_cpu",
            titleRes = R.string.cpu,
            explanationRes = R.string.card_explanation_group_cpu,
            howItWorksRes = R.string.card_how_it_works_group_cpu,
            whyItMattersRes = R.string.card_why_it_matters_group_cpu,
            icon = Icons.Outlined.Memory,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "group_instructions" to CardDetailsInfo(
            id = "group_instructions",
            titleRes = R.string.instructions,
            explanationRes = R.string.card_explanation_group_instructions,
            howItWorksRes = R.string.card_how_it_works_group_instructions,
            whyItMattersRes = R.string.card_why_it_matters_group_instructions,
            icon = Icons.Outlined.Terminal,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "group_technology" to CardDetailsInfo(
            id = "group_technology",
            titleRes = R.string.technology,
            explanationRes = R.string.card_explanation_group_technology,
            howItWorksRes = R.string.card_how_it_works_group_technology,
            whyItMattersRes = R.string.card_why_it_matters_group_technology,
            icon = Icons.Outlined.Science,
            settingsIntentAction = Settings.ACTION_DEVICE_INFO_SETTINGS
        ),
        "group_bluetooth" to CardDetailsInfo(
            id = "group_bluetooth",
            titleRes = R.string.tab_bluetooth,
            explanationRes = R.string.card_explanation_group_bluetooth,
            howItWorksRes = R.string.card_how_it_works_group_bluetooth,
            whyItMattersRes = R.string.card_why_it_matters_group_bluetooth,
            icon = Icons.Outlined.Bluetooth,
            settingsIntentAction = Settings.ACTION_BLUETOOTH_SETTINGS
        ),
        "group_thermal" to CardDetailsInfo(
            id = "group_thermal",
            titleRes = R.string.tab_thermal,
            explanationRes = R.string.card_explanation_group_thermal,
            howItWorksRes = R.string.card_how_it_works_group_thermal,
            whyItMattersRes = R.string.card_why_it_matters_group_thermal,
            icon = Icons.Outlined.Thermostat,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "group_sec_updates" to CardDetailsInfo(
            id = "group_sec_updates",
            titleRes = R.string.sec_updates,
            explanationRes = R.string.card_explanation_group_sec_updates,
            howItWorksRes = R.string.card_how_it_works_group_sec_updates,
            whyItMattersRes = R.string.card_why_it_matters_group_sec_updates,
            icon = Icons.Outlined.SystemUpdate,
            settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
        ),
        "group_drm" to CardDetailsInfo(
            id = "group_drm",
            titleRes = R.string.drm_status,
            explanationRes = R.string.card_explanation_group_drm,
            howItWorksRes = R.string.card_how_it_works_group_drm,
            whyItMattersRes = R.string.card_why_it_matters_group_drm,
            icon = Icons.Outlined.VerifiedUser,
            settingsIntentAction = Settings.ACTION_SECURITY_SETTINGS
        ),
        "group_codecs" to CardDetailsInfo(
            id = "group_codecs",
            titleRes = R.string.tab_codecs,
            explanationRes = R.string.card_explanation_group_codecs,
            howItWorksRes = R.string.card_how_it_works_group_codecs,
            whyItMattersRes = R.string.card_why_it_matters_group_codecs,
            icon = Icons.Outlined.Audiotrack,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "group_usb" to CardDetailsInfo(
            id = "group_usb",
            titleRes = R.string.tab_usb,
            explanationRes = R.string.card_explanation_group_usb,
            howItWorksRes = R.string.card_how_it_works_group_usb,
            whyItMattersRes = R.string.card_why_it_matters_group_usb,
            icon = Icons.Outlined.Usb,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "group_sensors" to CardDetailsInfo(
            id = "group_sensors",
            titleRes = R.string.tab_sensors,
            explanationRes = R.string.card_explanation_group_sensors,
            howItWorksRes = R.string.card_how_it_works_group_sensors,
            whyItMattersRes = R.string.card_why_it_matters_group_sensors,
            icon = Icons.Outlined.Sensors,
            settingsIntentAction = Settings.ACTION_SETTINGS
        ),
        "group_camera_modes" to CardDetailsInfo(
            id = "group_camera_modes",
            titleRes = R.string.cam_supported_modes,
            explanationRes = R.string.card_explanation_group_camera_modes,
            howItWorksRes = R.string.card_how_it_works_group_camera_modes,
            whyItMattersRes = R.string.card_why_it_matters_group_camera_modes,
            icon = Icons.Outlined.Camera,
            settingsIntentAction = Settings.ACTION_SETTINGS
        )
    )
}
