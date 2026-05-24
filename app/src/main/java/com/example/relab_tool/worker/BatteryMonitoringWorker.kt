package com.example.relab_tool.worker

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.work.*
import com.example.relab_tool.data.BatteryHistoryRepository
import java.util.concurrent.TimeUnit

class BatteryMonitoringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = BatteryHistoryRepository(applicationContext)
        
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            applicationContext.registerReceiver(null, ifilter)
        }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        if (level != -1 && scale != -1) {
            val batteryPct = (level * 100 / scale.toFloat()).toInt()
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            
            repository.recordBatteryPoint(batteryPct, isCharging)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "BatteryMonitoringWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val request = PeriodicWorkRequestBuilder<BatteryMonitoringWorker>(
                15, TimeUnit.MINUTES // Minimum interval allowed by Android
            )
            .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
