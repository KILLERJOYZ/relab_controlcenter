package com.example.relab_tool.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnitSettings {
    private val _measuringSystem = MutableStateFlow("Metric")
    val measuringSystem: StateFlow<String> = _measuringSystem.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.applicationContext
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        _measuringSystem.value = p.getString("measuring_system", "Metric") ?: "Metric"
    }

    fun setMeasuringSystem(system: String) {
        _measuringSystem.value = system
        prefs?.edit()?.putString("measuring_system", system)?.apply()
    }
}
