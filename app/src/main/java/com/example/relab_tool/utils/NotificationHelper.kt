package com.example.relab_tool.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.relab_tool.R
import com.example.relab_tool.model.AppInfo

object NotificationHelper {
    private const val CHANNEL_ID = "app_hub_updates"
    private const val CHANNEL_NAME = "App Hub Updates"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for app updates"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUpdateNotification(context: Context, appsWithUpdates: List<AppInfo>) {
        if (appsWithUpdates.isEmpty()) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val title = "App Hub — Updates Available"
        val content = if (appsWithUpdates.size == 1) {
            val app = appsWithUpdates[0]
            "${app.name}: ${app.installedVersion} → ${app.latestVersion}"
        } else {
            "${appsWithUpdates.size} apps have updates available"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Use a better icon if available
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (appsWithUpdates.size > 1) {
            val bigText = StringBuilder()
            appsWithUpdates.forEach { app ->
                bigText.append("• ${app.name}: ${app.installedVersion} → ${app.latestVersion}\n")
            }
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText.toString()))
        }

        notificationManager.notify(1001, builder.build())
    }
}
