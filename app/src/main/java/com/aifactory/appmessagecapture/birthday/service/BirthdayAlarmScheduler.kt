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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 使用 setAlarmClock：系统级闹钟，会显示在状态栏，优先级最高，能可靠唤醒设备
                val showIntent = Intent(context, com.aifactory.appmessagecapture.birthday.ui.AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(EXTRA_BIRTHDAY_ID, birthday.id)
                }
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    birthday.id,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
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

    /**
     * 为下一年（或下一次）的生日注册闹钟。
     *
     * 与 [schedule] 不同，此方法直接取当前闹钟目标的年份 +1，
     * 通过 [DateCalculator.solarBirthdayForYear] 计算下一年的生日公历日期，
     * 绕过了 DateCalculator 的"是否已过"判断逻辑，
     * 正确处理农历生日每年公历日期漂移约 11 天的问题。
     *
     * 典型场景：[BirthdayAlarmReceiver] 触发当前闹钟后，需要为下一年设置新闹钟。
     */
    fun scheduleForNextOccurrence(context: Context, birthday: BirthdayEntity) {
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
            val triggerMillis = calculateNextYearTriggerTime(birthday)
            if (triggerMillis <= System.currentTimeMillis()) {
                BirthdayLog.w(
                    "[$TAG] Next-year trigger time is still in the past for id=%d, skipping.",
                    birthday.id
                )
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = createAlarmPendingIntent(context, birthday.id)

            BirthdayLog.logMethodCall(
                "$TAG.scheduleForNextOccurrence",
                mapOf(
                    "id" to birthday.id,
                    "name" to birthday.name,
                    "triggerMillis" to triggerMillis,
                    "triggerTime" to formatMillis(triggerMillis)
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, com.aifactory.appmessagecapture.birthday.ui.AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(EXTRA_BIRTHDAY_ID, birthday.id)
                }
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    birthday.id,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }

            BirthdayLog.i(
                "[$TAG] Next-year alarm scheduled successfully. id=%d, triggerAt=%s",
                birthday.id,
                formatMillis(triggerMillis)
            )
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.scheduleForNextOccurrence", e)
        }
    }

    // -------------------------------------------------------------------------
    // 内部方法
    // -------------------------------------------------------------------------

    /**
     * 计算下一次（下一年）生日的闹钟触发时间戳（毫秒）。
     *
     * 策略：
     * 1. 先用正常的 [DateCalculator.calculate] 获取当前闹钟目标的生日信息
     * 2. 从结果中提取目标年份（nextSolarYear），然后直接计算 targetYear+1 年的生日公历日期
     * 3. 这种方法绕过了 DateCalculator 的"是否已过"判断逻辑，
     *    避免因农历日期每年公历漂移约 11 天而导致跳过一整年
     *
     * 为什么不直接 +1 年偏移基准日期？
     * 因为农历生日对应的公历日期每年漂移约 11 天（如 2026 年农历 4/24 = 公历 6/26，
     * 2027 年农历 4/24 = 公历 5/30）。如果基准日期偏移 +1 年到 2027-06-09，
     * DateCalculator 会发现 2027-05-30 < 2027-06-09（"已过"），从而跳到 2028 年。
     */
    private fun calculateNextYearTriggerTime(birthday: BirthdayEntity): Long {
        // ① 获取当前闹钟目标的生日信息
        val currentInfo = DateCalculator.calculate(birthday)
        val targetYear = currentInfo.nextSolarYear

        BirthdayLog.i(
            "[$TAG] Current alarm targets year %d (%s). Computing year %d.",
            targetYear,
            currentInfo.nextSolarDateString(),
            targetYear + 1
        )

        // ② 直接计算 targetYear+1 年的生日公历日期，不做"是否已过"判断
        val (nextYear, nextMonth, nextDay) = DateCalculator.solarBirthdayForYear(
            targetYear + 1, birthday
        )

        val nextYearInfo = DateCalculator.BirthdayInfo(
            nextSolarYear = nextYear,
            nextSolarMonth = nextMonth,
            nextSolarDay = nextDay,
            daysLeft = -1,
            ageTurning = if (birthday.birthYear != null && birthday.birthYear > 0) {
                nextYear - birthday.birthYear
            } else null
        )

        BirthdayLog.i(
            "[$TAG] Year %d birthday: %04d-%02d-%02d",
            targetYear + 1, nextYear, nextMonth, nextDay
        )

        return applyReminderOffset(nextYearInfo, birthday)
    }

    /**
     * 根据 [BirthdayInfo] 和提醒设置（提前天数 + 提醒时间），计算最终闹钟触发时间戳。
     */
    private fun applyReminderOffset(info: DateCalculator.BirthdayInfo, birthday: BirthdayEntity): Long {
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

        return cal.timeInMillis
    }

    /**
     * 计算闹钟触发时间戳（毫秒）。
     */
    private fun calculateTriggerTime(birthday: BirthdayEntity): Long {
        val info = DateCalculator.calculate(birthday)
        return applyReminderOffset(info, birthday)
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
