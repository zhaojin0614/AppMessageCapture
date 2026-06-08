package com.aifactory.appmessagecapture.birthday.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 桌面小组件定时刷新 Worker。
 *
 * 每天 00:00 触发，更新所有 BirthdayWidget 实例。
 */
class BirthdayWidgetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        BirthdayLog.i("[BirthdayWidgetWorker] doWork started.")
        return try {
            BirthdayWidget.updateAll(applicationContext)
            BirthdayLog.i("[BirthdayWidgetWorker] Widget updateAll succeeded.")
            Result.success()
        } catch (e: Exception) {
            BirthdayLog.logException("[BirthdayWidgetWorker] doWork", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "BirthdayWidgetDailyRefresh"

        /**
         * 安排每天 00:00 执行的周期性任务。
         * 若已存在则保持现有任务（KEEP）。
         */
        fun schedule(context: Context) {
            val initialDelay = calculateDelayToMidnight()
            BirthdayLog.i(
                "[BirthdayWidgetWorker] Scheduling daily refresh. initialDelayMs=%d",
                initialDelay
            )

            val request = PeriodicWorkRequestBuilder<BirthdayWidgetWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateDelayToMidnight(): Long {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return midnight.timeInMillis - now.timeInMillis
        }
    }
}
