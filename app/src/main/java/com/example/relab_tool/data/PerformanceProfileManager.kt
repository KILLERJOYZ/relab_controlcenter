package com.example.relab_tool.data

import android.content.Context

/** Profiles change this app's sampling rate, never global performance or UI priority. */
class PerformanceProfileManager(@Suppress("UNUSED_PARAMETER") context: Context) {
    fun apply(profile: PerformanceProfile) = TelemetryManager.setProfile(profile)
    fun release() = Unit
}
