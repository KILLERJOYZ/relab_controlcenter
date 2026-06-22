package com.example.relab_tool.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.relab_tool.R

object NotificationHelper {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "hardware_monitoring",
                "Hardware Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for hardware monitoring services"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            notificationManager.createNotificationChannel(channel)
        }
    }
}
