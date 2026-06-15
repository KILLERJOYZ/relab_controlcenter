package com.example.relab_tool

import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.example.relab_tool.ui.theme.UnitSettings
import com.example.relab_tool.ui.theme.ThemeSettings
import com.example.relab_tool.ui.DashboardLayoutSettings
import com.example.relab_tool.utils.NotificationHelper
import com.example.relab_tool.worker.UpdateCheckWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RelabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        UpdateCheckWorker.schedule(this)
        UnitSettings.init(this)
        ThemeSettings.init(this)
        DashboardLayoutSettings.initIfNeeded(this)

        if (BuildConfig.DEBUG) {
            // StrictMode: log-only policy. We intentionally do NOT throw or kill
            // on violations because OEM frameworks (ColorOS, MIUI, OneUI, HarmonyOS)
            // perform unavoidable disk reads from their own code that appear in our
            // app's stack traces. A diagnostic tool must never crash from its own
            // diagnostic infrastructure.
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
    }
}
