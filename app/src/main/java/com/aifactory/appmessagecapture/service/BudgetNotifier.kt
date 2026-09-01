package com.aifactory.appmessagecapture.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aifactory.appmessagecapture.MainActivity
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

/**
 * 预算阈值通知：账单入库后检查本月支出与月度总预算的比例。
 *
 * 两级提醒（80% 预警 / 100% 超支），每级每个自然月只提醒一次
 * （状态记录在 SharedPreferences，跨月自动重置）。通知固定 ID 原地更新，
 * 升级级别时覆盖旧提醒而不是堆叠。
 */
object BudgetNotifier {

    private const val CHANNEL_ID = "budget_alert_v2_channel"
    private const val CHANNEL_NAME = "预算提醒"
    private const val PREFS = "budget_notify_state"
    private const val KEY_MONTH = "month"
    private const val KEY_LEVEL = "notified_level"
    private const val NOTIFICATION_ID = 9001

    /** 级别：0 正常 / 1 预警(≥80%) / 2 超支(≥100%) */
    private const val LEVEL_OK = 0
    private const val LEVEL_WARN = 1
    private const val LEVEL_OVER = 2

    suspend fun checkAndNotify(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val total = db.budgetDao().getByCategory("")?.amount ?: return
            if (total <= 0.0) return

            val monthStart = LocalDate.now().withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val spent = db.billDao().getMonthExpenseOnce(monthStart) ?: return

            val level = when {
                spent >= total -> LEVEL_OVER
                spent >= total * 0.8 -> LEVEL_WARN
                else -> LEVEL_OK
            }
            if (level == LEVEL_OK) return

            // 每个自然月只提醒到已达到的最高级别；跨月自动重置
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val currentMonth = YearMonth.now().toString()
            if (prefs.getString(KEY_MONTH, "") != currentMonth) {
                prefs.edit().clear().putString(KEY_MONTH, currentMonth).apply()
            }
            if (prefs.getInt(KEY_LEVEL, LEVEL_OK) >= level) return
            prefs.edit().putInt(KEY_LEVEL, level).apply()

            postNotification(context, level, spent, total)
            BirthdayLog.i("[BudgetNotifier] notified level=$level spent=$spent total=$total")
        } catch (e: Exception) {
            BirthdayLog.logException("[BudgetNotifier] checkAndNotify", e)
        }
    }

    private fun postNotification(context: Context, level: Int, spent: Double, total: Double) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 旧渠道可能已被系统锁定（用户关过悬浮等），改用新渠道 ID 让悬浮走默认开启
        if (manager.getNotificationChannel("budget_alert_channel") != null) {
            manager.deleteNotificationChannel("budget_alert_channel")
        }
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "月度预算达到 80% / 超支时推送提醒"
                    // 悬浮（heads-up）默认开启
                    setImportance(NotificationManager.IMPORTANCE_HIGH)
                }
            )
        }

        val title = if (level == LEVEL_OVER) "本月预算已超支" else "本月预算预警"
        val text = if (level == LEVEL_OVER)
            "本月已支出 ¥%.2f，超出预算 ¥%.2f，点击查看明细".format(spent, spent - total)
        else
            "本月已支出 ¥%.2f，达到预算 ¥%.2f 的 80%%".format(spent, total)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
