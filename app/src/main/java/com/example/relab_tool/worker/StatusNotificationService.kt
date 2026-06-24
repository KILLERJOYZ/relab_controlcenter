package com.example.relab_tool.worker

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.relab_tool.MainActivity
import com.example.relab_tool.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class StatusNotificationService : Service() {

    companion object {
        private val _isRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
        val isRunning: kotlinx.coroutines.flow.StateFlow<Boolean> = _isRunning
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private lateinit var notificationManager: NotificationManager
    private val channelId = "status_monitor_channel"
    private val notificationId = 1001

    private var lastRxBytes = 0L
    private var lastTxBytes = 0L
    private var lastNetUpdateTime = 0L
    
    private var lastCpuTime = 0L
    private var lastIdleTime = 0L
    
    private val batteryRepo by lazy { com.example.relab_tool.data.BatteryHistoryRepository(applicationContext) }

    data class DeviceStats(
        val cpuPercent: Int,
        val ramUsedGb: Double,
        val ramTotalGb: Double,
        val ramPercent: Int,
        val storageUsedGb: Double,
        val storageTotalGb: Double,
        val storagePercent: Int,
        val batteryPercent: Int,
        val dlSpeed: Long,
        val ulSpeed: Long
    )

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        
        startForeground(notificationId, buildNotification("Initializing..."))
        _isRunning.value = true
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceJob.cancelChildren() // Cancel any existing
        
        lastRxBytes = TrafficStats.getTotalRxBytes()
        lastTxBytes = TrafficStats.getTotalTxBytes()
        lastNetUpdateTime = System.currentTimeMillis()
        
        serviceScope.launch {
            while (isActive) {
                val stats = collectStats()
                notificationManager.notify(notificationId, buildNotification(stats))
                delay(2000)
            }
        }
    }

    private fun collectStats(): DeviceStats {
        // CPU
        var cpuUsage = 0
        try {
            val statFile = File("/proc/stat")
            if (statFile.exists()) {
                val lines = statFile.readLines()
                if (lines.isNotEmpty()) {
                    val parts = lines[0].split(Regex("\\s+")).filter { it.isNotEmpty() }
                    if (parts[0] == "cpu") {
                        val user = parts[1].toLong()
                        val nice = parts[2].toLong()
                        val system = parts[3].toLong()
                        val idle = parts[4].toLong()
                        val iowait = parts[5].toLong()
                        val irq = parts[6].toLong()
                        val softirq = parts[7].toLong()
                        val steal = if (parts.size > 8) parts[8].toLong() else 0L

                        val total = user + nice + system + idle + iowait + irq + softirq + steal
                        val idleTotal = idle + iowait

                        if (lastCpuTime != 0L) {
                            val totalDiff = total - lastCpuTime
                            val idleDiff = idleTotal - lastIdleTime
                            if (totalDiff > 0) {
                                cpuUsage = (100 * (totalDiff - idleDiff) / totalDiff).toInt().coerceIn(0, 100)
                            }
                        }
                        lastCpuTime = total
                        lastIdleTime = idleTotal
                    }
                }
            }
        } catch (e: Exception) {
            // Permission denied on Android 8+, will fallback to frequency normalization
            cpuUsage = -1
        }

        if (cpuUsage == -1 || cpuUsage == 0) {
            val cpuCount = Runtime.getRuntime().availableProcessors()
            var totalNormalized = 0f
            for (i in 0 until cpuCount) {
                val freq = try {
                    File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq").readText().trim().toLong() / 1000
                } catch (e: Exception) { 0L }
                
                val max = try {
                    File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq").readText().trim().toLong()
                } catch (e: Exception) { 3000000L }
                
                val normalized = if (max > 0) ((freq * 1000).toFloat() / max.toFloat() * 100f).coerceIn(0f, 100f) else 0f
                totalNormalized += normalized
            }
            cpuUsage = if (cpuCount > 0) (totalNormalized / cpuCount).toInt() else 0
        }

        // RAM
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo) ?: return DeviceStats(0, 0.0, 0.0, 0, 0.0, 0.0, 0, 0, 0, 0)
        val ramUsedBytes = memoryInfo.totalMem - memoryInfo.availMem
        val ramTotalGb = memoryInfo.totalMem / (1024.0 * 1024 * 1024)
        val ramUsedGb = ramUsedBytes / (1024.0 * 1024 * 1024)
        val ramPercent = if (memoryInfo.totalMem > 0) (ramUsedBytes * 100 / memoryInfo.totalMem).toInt() else 0
        
        // Storage
        val internalStat = StatFs(Environment.getDataDirectory().path)
        val storageTotalBytes = internalStat.totalBytes
        val storageUsedBytes = storageTotalBytes - internalStat.availableBytes
        val storageTotalGb = storageTotalBytes / (1024.0 * 1024 * 1024)
        val storageUsedGb = storageUsedBytes / (1024.0 * 1024 * 1024)
        val storagePercent = if (storageTotalBytes > 0) (storageUsedBytes * 100 / storageTotalBytes).toInt() else 0

        // Battery
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val repository = batteryRepo
        repository.recordBatteryPoint(batteryPct, isCharging)

        // Network
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()
        var dlSpeed = 0L
        var ulSpeed = 0L
        if (lastNetUpdateTime > 0) {
            val timeDiff = (currentTime - lastNetUpdateTime) / 1000.0
            if (timeDiff > 0) {
                dlSpeed = ((currentRx - lastRxBytes) / timeDiff).toLong()
                ulSpeed = ((currentTx - lastTxBytes) / timeDiff).toLong()
            }
        }
        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastNetUpdateTime = currentTime

        return DeviceStats(
            cpuPercent = cpuUsage,
            ramUsedGb = ramUsedGb,
            ramTotalGb = ramTotalGb,
            ramPercent = ramPercent,
            storageUsedGb = storageUsedGb,
            storageTotalGb = storageTotalGb,
            storagePercent = storagePercent,
            batteryPercent = batteryPct,
            dlSpeed = dlSpeed,
            ulSpeed = ulSpeed
        )
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> "%.1f MB/s".format(Locale.US, bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> "%.1f KB/s".format(Locale.US, bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun buildNotification(stats: DeviceStats): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, StatusNotificationService::class.java).apply { action = "STOP_SERVICE" }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val remoteViews = RemoteViews(packageName, R.layout.notification_status)
        
        remoteViews.setProgressBar(R.id.cpu_progress, 100, stats.cpuPercent, false)
        remoteViews.setTextViewText(R.id.cpu_value, "${stats.cpuPercent}%")
        
        remoteViews.setProgressBar(R.id.ram_progress, 100, stats.ramPercent, false)
        remoteViews.setTextViewText(R.id.ram_value, "${stats.ramPercent}%")
        
        remoteViews.setProgressBar(R.id.disk_progress, 100, stats.storagePercent, false)
        remoteViews.setTextViewText(R.id.disk_value, "${stats.storagePercent}%")
        
        remoteViews.setProgressBar(R.id.bat_progress, 100, stats.batteryPercent, false)
        remoteViews.setTextViewText(R.id.bat_value, "${stats.batteryPercent}%")
        
        remoteViews.setTextViewText(R.id.net_value, "🌐 ↓ %s  ↑ %s".format(Locale.US, formatSpeed(stats.dlSpeed), formatSpeed(stats.ulSpeed)))

        val summary = "CPU: ${stats.cpuPercent}% | RAM: ${stats.ramPercent}% | Bat: ${stats.batteryPercent}%"

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Device Status")
            .setContentText(summary)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildNotification(initialText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Device Status")
            .setContentText(initialText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Device Status Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time CPU, RAM, Storage, Battery, and Network speeds"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        serviceJob.cancel()
    }
}
