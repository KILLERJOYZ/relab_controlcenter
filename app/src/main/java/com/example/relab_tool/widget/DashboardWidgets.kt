package com.example.relab_tool.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.relab_tool.MainActivity
import com.example.relab_tool.R

abstract class BaseDashboardWidget : AppWidgetProvider() {

    abstract val layoutId: Int

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val stats = WidgetStatsCollector.collect(context)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, stats)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        stats: WidgetStatsCollector.Stats
    ) {
        val views = RemoteViews(context.packageName, layoutId)

        // Bind data to layout views
        bindStats(views, stats)

        // Set click intent to launch MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)
        views.setOnClickPendingIntent(R.id.root_layout, pendingIntent)

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    abstract fun bindStats(views: RemoteViews, stats: WidgetStatsCollector.Stats)

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val stats = WidgetStatsCollector.collect(context)
            
            updateClassWidget(context, appWidgetManager, DashboardWidget1x1::class.java, stats)
            updateClassWidget(context, appWidgetManager, DashboardWidget2x2::class.java, stats)
            updateClassWidget(context, appWidgetManager, DashboardWidget4x2::class.java, stats)
            updateClassWidget(context, appWidgetManager, DashboardWidget4x4::class.java, stats)
        }

        private fun updateClassWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetClass: Class<out BaseDashboardWidget>,
            stats: WidgetStatsCollector.Stats
        ) {
            val componentName = ComponentName(context, widgetClass)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids.isEmpty()) return
            
            try {
                val widget = widgetClass.getDeclaredConstructor().newInstance()
                for (id in ids) {
                    val views = RemoteViews(context.packageName, widget.layoutId)
                    widget.bindStats(views, stats)

                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)
                    views.setOnClickPendingIntent(R.id.root_layout, pendingIntent)

                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class DashboardWidget1x1 : BaseDashboardWidget() {
    override val layoutId = R.layout.widget_layout_1x1

    override fun bindStats(views: RemoteViews, stats: WidgetStatsCollector.Stats) {
        views.setTextViewText(R.id.cpu_value, "${stats.cpuUsage}%")
    }
}

class DashboardWidget2x2 : BaseDashboardWidget() {
    override val layoutId = R.layout.widget_layout_2x2

    override fun bindStats(views: RemoteViews, stats: WidgetStatsCollector.Stats) {
        views.setTextViewText(R.id.cpu_value, "${stats.cpuUsage}%")
        views.setTextViewText(R.id.ram_value, "${stats.ramPercent}%")
        views.setTextViewText(R.id.bat_value, stats.batteryText)
        views.setTextViewText(R.id.temp_value, stats.batteryTempText)
    }
}

class DashboardWidget4x2 : BaseDashboardWidget() {
    override val layoutId = R.layout.widget_layout_4x2

    override fun bindStats(views: RemoteViews, stats: WidgetStatsCollector.Stats) {
        views.setTextViewText(R.id.ram_text, stats.ramText)
        views.setProgressBar(R.id.ram_progress, 100, stats.ramPercent, false)

        views.setTextViewText(R.id.cpu_value, "${stats.cpuUsage}%")
        views.setProgressBar(R.id.cpu_progress, 100, stats.cpuUsage, false)

        views.setTextViewText(R.id.gpu_value, "${stats.gpuUsage}%")
        views.setProgressBar(R.id.gpu_progress, 100, stats.gpuUsage, false)

        views.setTextViewText(R.id.bat_value, stats.batteryText)
        views.setProgressBar(R.id.bat_progress, 100, stats.batteryPercent, false)
    }
}

class DashboardWidget4x4 : BaseDashboardWidget() {
    override val layoutId = R.layout.widget_layout_4x4

    override fun bindStats(views: RemoteViews, stats: WidgetStatsCollector.Stats) {
        views.setTextViewText(R.id.widget_update_time, stats.uptimeText)

        views.setTextViewText(R.id.cpu_value, "${stats.cpuUsage}%")
        views.setProgressBar(R.id.cpu_progress, 100, stats.cpuUsage, false)

        views.setTextViewText(R.id.ram_text, stats.ramText)
        views.setProgressBar(R.id.ram_progress, 100, stats.ramPercent, false)

        views.setTextViewText(R.id.gpu_value, "${stats.gpuUsage}%")
        views.setProgressBar(R.id.gpu_progress, 100, stats.gpuUsage, false)

        views.setTextViewText(R.id.storage_text, stats.storageText)
        views.setProgressBar(R.id.storage_progress, 100, stats.storagePercent, false)

        views.setTextViewText(R.id.bat_value, stats.batteryText)
        views.setProgressBar(R.id.bat_progress, 100, stats.batteryPercent, false)

        views.setTextViewText(R.id.dl_speed, "↓ ${stats.dlSpeedText}")
        views.setTextViewText(R.id.ul_speed, "↑ ${stats.ulSpeedText}")

        views.setTextViewText(R.id.wifi_ssid_value, stats.wifiSsid)
    }
}
