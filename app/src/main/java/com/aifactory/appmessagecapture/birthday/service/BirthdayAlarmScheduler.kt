package com.aifactory.appmessagecapture.birthday.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import com.aifactory.appmessagecapture.birthday.data.ReminderType
import com.aifactory.appmessagecapture.birthday.logic.DateCalculator
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 生日闹钟调度器。
 *
 * 封装 [AlarmManager] 的所有操作，包括：
 * - 为单条生日记录注册/取消精确闹钟
 * - 设备重启后批量重新注册
 * - 处理提醒类型（当天/提前 N 天）和提醒时间（HH:mm）
 *
 * **权限要求**：
 * - `android.permission.SCHEDULE_EXACT_ALARM`（Android 14+ 需用户手动授权）
 * - `android.permission.POST_NOTIFICATIONS`（Android 13+）
 * - `android.permission.RECEIVE_BOOT_COMPLETED`
 */
object BirthdayAlarmScheduler {

    private const val TAG = "BirthdayAlarmScheduler"
    private const val ACTION_BIRTHDAY_ALARM = "com.aifactory.appmessagecapture.ACTION_BIRTHDAY_ALARM"
    private const val EXTRA_BIRTHDAY_ID = "extra_birthday_id"

    /**
     * 检查当前应用是否有权限设置精确闹钟（Android 14+）。
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * 为单条生日记录注册闹钟。
     *
     * 若 [ReminderType] 为 [ReminderType.NONE]，则自动取消已有闹钟。
     */
    fun schedule(context: Context, birthday: BirthdayEntity) {
        if (birthday.reminderType == ReminderType.NONE) {
            BirthdayLog.i("[$TAG] ReminderType is NONE, cancelling alarm for id=%d", birthday.id)
            cancel(context, birthday.id)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms(context)) {
            BirthdayLog.w(
                "[$TAG] Cannot schedule exact alarm for id=%d: SCHEDULE_EXACT_ALARM permission not granted.",
                birthday.id
            )
            return
        }

        try {
            val triggerMillis = calculateTriggerTime(birthday)
            if (triggerMillis <= System.currentTimeMillis()) {
                BirthdayLog.w(
                    "[$TAG] Calculated trigger time is in the past for id=%d, skipping.",
                    birthday.id
                )
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = createAlarmPendingIntent(context, birthday.id)

            BirthdayLog.logMethodCall(
                "$TAG.schedule",
                mapOf(
                    "id" to birthday.id,
                    "name" to birthday.name,
                    "triggerMillis" to triggerMillis,
                    "triggerTime" to formatMillis(triggerMillis)
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }

            BirthdayLog.i(
                "[$TAG] Alarm scheduled successfully. id=%d, triggerAt=%s",
                birthday.id,
                formatMillis(triggerMillis)
            )
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.schedule", e)
        }
    }

    /**
     * 取消指定生日记录的闹钟。
     */
    fun cancel(context: Context, birthdayId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = createAlarmPendingIntent(context, birthdayId)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            BirthdayLog.i("[$TAG] Alarm cancelled. id=%d", birthdayId)
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.cancel", e)
        }
    }

    /**
     * 设备重启后重新注册所有闹钟。
     *
     * 应在 [BirthdayBootReceiver] 中调用。
     */
    suspend fun rescheduleAll(context: Context) {
        BirthdayLog.i("[$TAG] rescheduleAll started.")
        withContext(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).birthdayDao()
                val all = dao.getAllOnce()
                BirthdayLog.i("[$TAG] Found %d birthday records to reschedule.", all.size)

                var successCount = 0
                all.forEach { birthday ->
                    try {
                        schedule(context, birthday)
                        successCount++
                    } catch (e: Exception) {
                        BirthdayLog.logException("$TAG.rescheduleAll item ${birthday.id}", e)
                    }
                }

                BirthdayLog.i(
                    "[$TAG] rescheduleAll completed. total=%d, success=%d",
                    all.size,
                    successCount
                )
            } catch (e: Exception) {
                BirthdayLog.logException("$TAG.rescheduleAll", e)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 内部方法
    // -------------------------------------------------------------------------

    /**
     * 计算闹钟触发时间戳（毫秒）。
     */
    private fun calculateTriggerTime(birthday: BirthdayEntity): Long {
        val info = DateCalculator.calculate(birthday)
        val daysBefore = birthday.reminderType.daysBefore

        val cal = Calendar.getInstance().apply {
            set(info.nextSolarYear, info.nextSolarMonth - 1, info.nextSolarDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysBefore)
        }

        // 应用提醒时间（HH:mm）
        val timeStr = birthday.reminderTime ?: "09:00"
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // 如果 daysBefore=0 且设定的时间已经过了，说明是今天的闹钟但时间已过，直接跳过
        // （或者可以改为立即触发，但通常用户不会想这样）
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            BirthdayLog.w(
                "[$TAG] Trigger time is in the past after applying reminderTime. id=%d",
                birthday.id
            )
        }

        return cal.timeInMillis
    }

    private fun createAlarmPendingIntent(context: Context, birthdayId: Int): PendingIntent {
        val intent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
            action = ACTION_BIRTHDAY_ALARM
            putExtra(EXTRA_BIRTHDAY_ID, birthdayId)
        }
        return PendingIntent.getBroadcast(
            context,
            birthdayId, // requestCode 用 birthdayId 保证唯一性
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatMillis(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(
            "%04d-%02d-%02d %02d:%02d:%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
    }
}
