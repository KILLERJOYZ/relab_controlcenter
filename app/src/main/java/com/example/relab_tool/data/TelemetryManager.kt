package com.example.relab_tool.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object TelemetryManager {
    data class TelemetryData(
        val cpuUsage: Int = 0,
        val gpuUsage: Int = 0,
        val fps: Float = 0f,
        val temperature: String = "N/A"
    )

    private val _data = MutableStateFlow(TelemetryData())
    val data = _data.asStateFlow()

    fun updateData(newData: TelemetryData) {
        _data.value = newData
    }
}
