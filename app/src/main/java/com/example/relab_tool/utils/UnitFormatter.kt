package com.example.relab_tool.utils

import android.content.Context
import com.example.relab_tool.ui.theme.UnitSettings
import java.util.Locale

object UnitFormatter {
    fun getMeasuringSystem(context: Context): String {
        // Fallback check to flow value, but flow can be accessed statically.
        return UnitSettings.measuringSystem.value
    }

    fun formatTemperature(context: Context, tempInCelsius: Float, includeSpace: Boolean = false): String {
        return if (getMeasuringSystem(context) == "Imperial") {
            val tempF = tempInCelsius * 1.8f + 32f
            String.format(Locale.US, if (includeSpace) "%.1f °F" else "%.1f°F", tempF)
        } else {
            String.format(Locale.US, if (includeSpace) "%.1f °C" else "%.1f°C", tempInCelsius)
        }
    }

    fun formatDistance(context: Context, distInCm: Float): String {
        return if (getMeasuringSystem(context) == "Imperial") {
            val distInInches = distInCm / 2.54f
            String.format(Locale.US, "%.1f in", distInInches)
        } else {
            String.format(Locale.US, "%.1f cm", distInCm)
        }
    }

    fun formatAccuracy(context: Context, accuracyInMeters: Float): String {
        return if (getMeasuringSystem(context) == "Imperial") {
            val accInFeet = accuracyInMeters * 3.28084f
            String.format(Locale.US, "%.1f ft", accInFeet)
        } else {
            String.format(Locale.US, "%.1f m", accuracyInMeters)
        }
    }
}
