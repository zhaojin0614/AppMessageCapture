package com.aifactory.appmessagecapture.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.utils.PendingIntentCache
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that prunes captured notification messages older than
 * [RETENTION_DAYS] days once per day, relieving local storage pressure.
 *
 * Only the `notifications` table is affected; bills are never deleted here.
 */
class NotificationCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationCleanup"
        private const val WORK_NAME = "notification_cleanup_daily"
        private const val RETENTION_DAYS = 30L

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
            BirthdayLog.i("[$TAG] Scheduled daily notification cleanup (retain $RETENTION_DAYS days)")
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
        val dao = AppDatabase.getDatabase(applicationContext).notificationDao()

        // Threshold: start of the day (RETENTION_DAYS - 1) days ago, i.e. keep the
        // last 30 calendar days inclusive of today.
        val thresholdDate = LocalDate.now().minusDays(RETENTION_DAYS - 1)
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
            BirthdayLog.i("[$TAG] Deleted $deleted stale notifications (retention=$RETENTION_DAYS days)")

            Result.success()
        } catch (e: Exception) {
            BirthdayLog.i("[$TAG] Error during cleanup: ${e.message}")
            Result.retry()
        }
    }
}
