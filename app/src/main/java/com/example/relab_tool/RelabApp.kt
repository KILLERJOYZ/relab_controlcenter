package com.example.relab_tool

import android.app.Application
import com.example.relab_tool.utils.NotificationHelper
import com.example.relab_tool.worker.UpdateCheckWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RelabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        UpdateCheckWorker.schedule(this)
    }
}
