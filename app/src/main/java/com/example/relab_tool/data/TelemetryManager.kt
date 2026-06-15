package com.example.relab_tool.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Performance profiles selectable from the overlay HUD. */
enum class PerformanceProfile(
    /** Telemetry fast-loop poll interval in milliseconds. */
    val pollIntervalMs: Long,
    /** Label shown in the overlay. */
    val label: String
) {
    /** Balanced — default system behaviour. 1-second polling. */
    DEFAULT(pollIntervalMs = 1000L, label = "Default"),

    /** High-performance gaming mode. 500 ms polling, CPU hints boosted. */
    GAMING(pollIntervalMs = 500L, label = "Gaming"),

    /** Power-saver mode. 2-second polling, CPU hints reduced. */
    SAVER(pollIntervalMs = 2000L, label = "Saver");

    companion object {
        fun fromLabel(label: String) = entries.firstOrNull { it.label == label } ?: DEFAULT
    }
}

object TelemetryManager {
    data class TelemetryData(
        val cpuUsage: Int = 0,
        val gpuUsage: Int = 0,
        val fps: Float = 0f,
        val temperature: String = "N/A"
    )

    private val _data = MutableStateFlow(TelemetryData())
    val data = _data.asStateFlow()

    private val _activeProfile = MutableStateFlow(PerformanceProfile.DEFAULT)
    /** Observable active performance profile. ViewModel poll loop reads this. */
    val activeProfile = _activeProfile.asStateFlow()

    fun updateData(newData: TelemetryData) {
        _data.value = newData
    }

    fun setProfile(profile: PerformanceProfile) {
        _activeProfile.value = profile
    }
}
