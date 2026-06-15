package com.example.relab_tool.ui

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CardSize {
    SIZE_1x1,
    SIZE_2x1,
    SIZE_2x2,
    SIZE_4x2,
    SIZE_4x4
}

object DashboardLayoutSettings {
    private val _cardSizes = MutableStateFlow<Map<String, CardSize>>(emptyMap())
    val cardSizes: StateFlow<Map<String, CardSize>> = _cardSizes.asStateFlow()

    private val _cardOrder = MutableStateFlow<List<String>>(emptyList())
    val cardOrder: StateFlow<List<String>> = _cardOrder.asStateFlow()

    private var prefs: SharedPreferences? = null
    private var initialized = false

    // Default sizes for each card
    private val defaultSizes = mapOf(
        "ram" to CardSize.SIZE_4x2,
        "cpu" to CardSize.SIZE_2x2,
        "gpu" to CardSize.SIZE_2x2,
        "cpu_freqs" to CardSize.SIZE_4x2,
        "system_health" to CardSize.SIZE_4x2,
        "features" to CardSize.SIZE_4x2,
        "thermal" to CardSize.SIZE_2x2,
        "touch_sampling" to CardSize.SIZE_2x2,
        "wifi" to CardSize.SIZE_2x2,
        "satellite_compass" to CardSize.SIZE_2x2,
        "sim_1" to CardSize.SIZE_2x2,
        "sim_2" to CardSize.SIZE_2x2,
        "charging_current" to CardSize.SIZE_2x2,
        "battery_health" to CardSize.SIZE_2x2,
        "uptime" to CardSize.SIZE_2x2,
        "disk_io" to CardSize.SIZE_2x2,
        "bt_codec" to CardSize.SIZE_2x2,
        "ambient_light" to CardSize.SIZE_2x2,
        "security" to CardSize.SIZE_2x2,
        "bluetooth_count" to CardSize.SIZE_2x2,
        "storage" to CardSize.SIZE_2x2,
        "screen" to CardSize.SIZE_2x2,
        "battery" to CardSize.SIZE_2x2,
        "refresh_rate" to CardSize.SIZE_2x2,
        "color_depth" to CardSize.SIZE_2x2,
        "drm" to CardSize.SIZE_2x2,
        "download" to CardSize.SIZE_2x2,
        "upload" to CardSize.SIZE_2x2,
        "sensors" to CardSize.SIZE_2x2,
        "apps" to CardSize.SIZE_2x2
    )

    val defaultOrder = listOf(
        "ram", "cpu", "gpu", "cpu_freqs", "system_health",
        "thermal", "touch_sampling", "wifi", "satellite_compass", "sim_1", "sim_2",
        "charging_current", "battery_health", "uptime", "disk_io",
        "bt_codec", "ambient_light", "security", "bluetooth_count",
        "storage", "screen", "battery", "refresh_rate", "color_depth",
        "drm", "download", "upload", "sensors", "apps", "features"
    )

    fun initIfNeeded(context: Context) {
        if (initialized) return
        val p = context.applicationContext.getSharedPreferences("dashboard_layout", Context.MODE_PRIVATE)
        prefs = p
        
        val loadedSizes = defaultSizes.mapValues { (key, defaultVal) ->
            val savedStr = p.getString(key, defaultVal.name)
            runCatching { CardSize.valueOf(savedStr!!) }.getOrDefault(defaultVal)
        }
        _cardSizes.value = loadedSizes

        val savedOrderStr = p.getString("layout_order", null)
        val loadedOrder = if (savedOrderStr.isNullOrEmpty()) {
            defaultOrder
        } else {
            val savedList = savedOrderStr.split(",")
            val merged = (savedList.filter { it.isNotEmpty() } + defaultOrder).distinct()
            merged
        }
        _cardOrder.value = loadedOrder

        initialized = true
    }

    fun getCardSize(key: String): CardSize {
        return _cardSizes.value[key] ?: defaultSizes[key] ?: CardSize.SIZE_2x2
    }

    fun setCardSize(key: String, size: CardSize) {
        _cardSizes.update { current ->
            current + (key to size)
        }
        prefs?.edit()?.putString(key, size.name)?.apply()
    }

    fun setCardOrder(order: List<String>) {
        val uniqueOrder = order.distinct()
        _cardOrder.update { uniqueOrder }
        prefs?.edit()?.putString("layout_order", uniqueOrder.joinToString(","))?.apply()
    }

    fun setBulkLayout(order: List<String>, sizes: Map<String, CardSize>) {
        val uniqueOrder = order.distinct()
        _cardOrder.value = uniqueOrder
        _cardSizes.value = sizes
        
        prefs?.edit()?.apply {
            putString("layout_order", uniqueOrder.joinToString(","))
            sizes.forEach { (key, size) ->
                putString(key, size.name)
            }
            apply()
        }
    }
}
