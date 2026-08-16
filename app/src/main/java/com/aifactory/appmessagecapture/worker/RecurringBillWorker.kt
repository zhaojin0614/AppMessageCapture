package com.aifactory.appmessagecapture.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.RecurringBillProcessor
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that checks for due recurring bills and creates bill entries.
 * Runs once per day.
 */
class RecurringBillWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RecurringBillWorker"
        private const val WORK_NAME = "recurring_bill_daily_check"

        /**
         * Schedule the daily recurring bill check.
         * Uses KEEP policy so existing schedule is not overwritten.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringBillWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            BirthdayLog.i("[$TAG] Scheduled daily recurring bill check")
        }

        /**
         * Cancel the scheduled recurring bill check.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            BirthdayLog.i("[$TAG] Cancelled recurring bill check")
        }
    }

    override suspend fun doWork(): Result {
        BirthdayLog.i("[$TAG] Executing recurring bill check")

        val db = AppDatabase.getDatabase(applicationContext)

        return try {
            val created = RecurringBillProcessor.processDueBills(db)
            BirthdayLog.i("[$TAG] Recurring bill check completed. Created $created bills")
            Result.success()
        } catch (e: Exception) {
            BirthdayLog.i("[$TAG] Error during recurring bill check: ${e.message}")
            // Safe to retry: committed transactions already advanced nextDueDate,
            // so re-running never duplicates bills.
            Result.retry()
        }
    }
}
