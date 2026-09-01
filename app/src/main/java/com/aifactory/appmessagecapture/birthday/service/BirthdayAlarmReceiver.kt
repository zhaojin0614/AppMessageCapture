package com.aifactory.appmessagecapture.birthday.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.aifactory.appmessagecapture.MainActivity
import com.aifactory.appmessagecapture.R
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.birthday.logic.DateCalculator
import com.aifactory.appmessagecapture.birthday.ui.AlarmActivity
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 生日闹钟广播接收器。
 *
 * 当 [AlarmManager] 触发时，此 Receiver 负责：
 * 1. 从数据库查询对应的生日记录
 * 2. 发送系统通知（Notification）
 * 3. 启动全屏 [AlarmActivity]（如果设备锁屏或应用未运行）
 * 4. 为下一年重新注册闹钟（实现每年重复提醒）
 */
class BirthdayAlarmReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val birthdayId = intent.getIntExtra(EXTRA_BIRTHDAY_ID, -1)
        BirthdayLog.i(
            "[BirthdayAlarmReceiver] onReceive triggered. birthdayId=%d, action=%s",
            birthdayId,
            intent.action ?: "null"
        )

        if (birthdayId == -1) {
            BirthdayLog.e("[BirthdayAlarmReceiver] Invalid birthdayId, aborting.")
            pendingResult.finish()
            return
        }

        scope.launch {
            try {
                val dao = AppDatabase.getDatabase(context).birthdayDao()
                val birthday = dao.getById(birthdayId)
                if (birthday == null) {
                    BirthdayLog.w(
                        "[BirthdayAlarmReceiver] Birthday record not found for id=%d, cancelling alarm.",
                        birthdayId
                    )
                    BirthdayAlarmScheduler.cancel(context, birthdayId)
                    return@launch
                }

                val info = DateCalculator.calculate(birthday)
                BirthdayLog.logMethodCall(
                    "[BirthdayAlarmReceiver] processing",
                    mapOf(
                        "id" to birthday.id,
                        "name" to birthday.name,
                        "daysLeft" to info.daysLeft,
                        "nextSolarDate" to info.nextSolarDateString()
                    )
                )

                // 发送通知
                showNotification(context, birthday, info)

                // 获取 WakeLock 点亮屏幕并唤醒设备
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_DIM_WAKE_LOCK,
                    "AppMessageCapture:BirthdayAlarm"
                )
                wakeLock.acquire(15 * 1000L)
                BirthdayLog.i("[BirthdayAlarmReceiver] WakeLock acquired for 15s")

                // 检查悬浮窗权限（亮屏时强制弹窗必需）
                val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.provider.Settings.canDrawOverlays(context)
                } else {
                    true
                }
                BirthdayLog.i("[BirthdayAlarmReceiver] Overlay permission granted: %b", hasOverlay)

                // 启动全屏闹钟 Activity —— 必须在主线程执行
                // 有悬浮窗权限时，Android 10+ 允许后台启动 Activity，亮屏时也能直接弹窗
                val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    putExtra(EXTRA_BIRTHDAY_ID, birthdayId)
                    putExtra(EXTRA_BIRTHDAY_NAME, birthday.name)
                    putExtra(EXTRA_AGE_TURNING, info.ageTurning ?: -1)
                }
                try {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        try {
                            context.startActivity(alarmIntent)
                            BirthdayLog.i("[BirthdayAlarmReceiver] AlarmActivity started for id=%d", birthdayId)
                        } catch (e: Exception) {
                            BirthdayLog.logException("[BirthdayAlarmReceiver] startActivity on main thread failed", e)
                            if (!hasOverlay) {
                                BirthdayLog.w("[BirthdayAlarmReceiver] Consider granting SYSTEM_ALERT_WINDOW permission for screen-on popup")
                            }
                        }
                    }
                } catch (e: Exception) {
                    BirthdayLog.logException("[BirthdayAlarmReceiver] post to main looper failed", e)
                }

                // 为下一年重新注册闹钟（使用 scheduleForNextOccurrence 强制计算下一年的日期，
                // 避免在生日当天触发时 DateCalculator 返回今年的已过期日期）
                BirthdayAlarmScheduler.scheduleForNextOccurrence(context, birthday)
                BirthdayLog.i(
                    "[BirthdayAlarmReceiver] Rescheduled alarm for next year. id=%d",
                    birthdayId
                )
            } catch (e: Exception) {
                BirthdayLog.logException("[BirthdayAlarmReceiver] onReceive", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        birthday: com.aifactory.appmessagecapture.birthday.data.BirthdayEntity,
        info: DateCalculator.BirthdayInfo
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "birthday_reminder_v2_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 旧渠道可能已被系统锁定（用户关过悬浮等），改用新 ID 让悬浮（heads-up）默认开启
            if (notificationManager.getNotificationChannel("birthday_reminder_channel") != null) {
                notificationManager.deleteNotificationChannel("birthday_reminder_channel")
            }
            val existing = notificationManager.getNotificationChannel(channelId)
            if (existing == null) {
                val channel = NotificationChannel(
                    channelId,
                    "生日提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "亲友生日倒计时提醒"
                    // 悬浮（heads-up）默认开启
                    setImportance(NotificationManager.IMPORTANCE_HIGH)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                notificationManager.createNotificationChannel(channel)
                BirthdayLog.i("[BirthdayAlarmReceiver] Notification channel created.")
            }
        }

        val contentText = buildString {
            append("今天是 ${birthday.name} 的生日")
            if (info.ageTurning != null) {
                append("，即将满 ${info.ageTurning} 岁")
            }
            append("！")
        }

        // 普通点击通知时打开 MainActivity
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            birthday.id,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 锁屏/全屏弹窗用的 PendingIntent（指向 AlarmActivity）
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BIRTHDAY_ID, birthday.id)
            putExtra(EXTRA_BIRTHDAY_NAME, birthday.name)
            putExtra(EXTRA_AGE_TURNING, info.ageTurning ?: -1)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            birthday.id + 10000, // 避免与 contentPendingIntent requestCode 冲突
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎂 生日提醒")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        notificationManager.notify(birthday.id, notification)
        BirthdayLog.i(
            "[BirthdayAlarmReceiver] Notification shown. id=%d, name=%s",
            birthday.id,
            birthday.name
        )
    }

    companion object {
        const val EXTRA_BIRTHDAY_ID = "extra_birthday_id"
        const val EXTRA_BIRTHDAY_NAME = "extra_birthday_name"
        const val EXTRA_AGE_TURNING = "extra_age_turning"
    }
}
