package com.ray.authigel.util.autoBackup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AutoBackupScheduler {
    private const val AUTO_BACKUP_WORK_NAME = "auto_backup_worker"

    fun runImmediateBackup(context: Context) {
        val request = OneTimeWorkRequestBuilder<AutoBackupWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun schedulePeriodicBackup(context: Context, periodDays: Int) {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(
            periodDays.toLong(),
            TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                AUTO_BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    fun cancelPeriodicBackup(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
    }
}