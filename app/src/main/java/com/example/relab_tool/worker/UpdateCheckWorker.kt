package com.example.relab_tool.worker

import android.content.Context
import androidx.work.*
import com.example.relab_tool.data.AppRepository
import com.example.relab_tool.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = AppRepository(applicationContext)
        repository.refresh()
        
        val appsWithUpdates = repository.apps.value.filter { it.hasUpdate }
        if (appsWithUpdates.isNotEmpty()) {
            NotificationHelper.showUpdateNotification(applicationContext, appsWithUpdates)
        }
        
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "UpdateCheckWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)
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
