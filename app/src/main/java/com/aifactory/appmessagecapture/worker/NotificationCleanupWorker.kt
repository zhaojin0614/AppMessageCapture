package com.aifactory.appmessagecapture.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.utils.PendingIntentCache
import com.aifactory.appmessagecapture.utils.PreferencesManager
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that prunes captured notification messages older than the
 * user-configured retention period once per day, relieving local storage pressure.
 *
 * Only the `notifications` table is affected; bills are never deleted here.
 * 保留时长存于 [PreferencesManager]（0 = 永久保留，本次执行直接跳过）。
 */
class NotificationCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationCleanup"
        private const val WORK_NAME = "notification_cleanup_daily"
        private const val RUN_NOW_WORK_NAME = "notification_cleanup_now"

        /**
         * Schedule the daily notification cleanup.
         * Uses KEEP policy so an existing schedule is not overwritten.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationCleanupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            BirthdayLog.i("[$TAG] Scheduled daily notification cleanup")
        }

        /**
         * 立即按当前保留时长清理一次（用户在设置里修改保留时长后即时生效）。
         * REPLACE：短时间内连续修改只保留最后一次的执行。
         */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<NotificationCleanupWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                RUN_NOW_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            BirthdayLog.i("[$TAG] Triggered one-shot cleanup")
        }

        /**
         * Cancel the scheduled cleanup.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            BirthdayLog.i("[$TAG] Cancelled notification cleanup")
        }
    }

    override suspend fun doWork(): Result {
        val retentionDays = PreferencesManager.getInstance(applicationContext)
            .getNotificationRetentionDays()
        if (retentionDays <= 0) {
            BirthdayLog.i("[$TAG] Retention=permanent, skipping cleanup")
            return Result.success()
        }
        val dao = AppDatabase.getDatabase(applicationContext).notificationDao()

        // Threshold: start of the day (retentionDays - 1) days ago, i.e. keep the
        // last N calendar days inclusive of today.
        val thresholdDate = LocalDate.now().minusDays(retentionDays.toLong() - 1)
        val thresholdMillis = thresholdDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return try {
            // Evict cached PendingIntents of rows about to be deleted (they'd
            // otherwise linger until LRU eviction).
            val staleIds = dao.getIdsOlderThan(thresholdMillis)
            if (staleIds.isNotEmpty()) {
                PendingIntentCache.removeAll(staleIds)
            }

            val deleted = dao.deleteOlderThan(thresholdMillis)
            BirthdayLog.i("[$TAG] Deleted $deleted stale notifications (retention=$retentionDays days)")

            Result.success()
        } catch (e: Exception) {
            BirthdayLog.i("[$TAG] Error during cleanup: ${e.message}")
            Result.retry()
        }
    }
}
