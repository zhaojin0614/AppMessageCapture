package com.aifactory.appmessagecapture.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.data.RecurringBillEntity
import com.aifactory.appmessagecapture.data.RecurringFrequency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
        val recurringBillDao = db.recurringBillDao()
        val billDao = db.billDao()
        val currentTime = System.currentTimeMillis()

        try {
            val dueBills = recurringBillDao.getDueRecurringBills(currentTime)
            BirthdayLog.i("[$TAG] Found ${dueBills.size} due recurring bills")

            for (recurring in dueBills) {
                // Create a bill entry for this recurring bill
                val bill = BillEntity(
                    amount = recurring.amount,
                    appName = "周期记账",
                    packageName = "",
                    title = recurring.title,
                    category = recurring.category,
                    isIncome = recurring.isIncome,
                    timestamp = recurring.nextDueDate
                )
                billDao.insert(bill)
                BirthdayLog.i("[$TAG] Created bill: ${recurring.title} ¥${recurring.amount}")

                // Calculate and update nextDueDate
                val nextDue = calculateNextDueDate(recurring)
                recurringBillDao.updateNextDueDate(recurring.id, nextDue)
                BirthdayLog.i("[$TAG] Updated nextDueDate for '${recurring.title}' to $nextDue")
            }

            BirthdayLog.i("[$TAG] Recurring bill check completed. Processed ${dueBills.size} bills")
            return Result.success()
        } catch (e: Exception) {
            BirthdayLog.i("[$TAG] Error during recurring bill check: ${e.message}")
            return Result.retry()
        }
    }

    /**
     * Calculate the next due date based on frequency.
     * Uses LocalDate for accurate calendar arithmetic (handles month/year boundaries).
     */
    private fun calculateNextDueDate(recurring: RecurringBillEntity): Long {
        val zoneId = ZoneId.systemDefault()
        val currentDueDate = Instant.ofEpochMilli(recurring.nextDueDate)
            .atZone(zoneId)
            .toLocalDate()

        val nextDate = when (recurring.frequency) {
            RecurringFrequency.DAILY.name -> currentDueDate.plusDays(1)
            RecurringFrequency.WEEKLY.name -> currentDueDate.plusWeeks(1)
            RecurringFrequency.BIWEEKLY.name -> currentDueDate.plusWeeks(2)
            RecurringFrequency.MONTHLY.name -> currentDueDate.plusMonths(1)
            RecurringFrequency.YEARLY.name -> currentDueDate.plusYears(1)
            else -> currentDueDate.plusMonths(1) // Default to monthly
        }

        return nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
