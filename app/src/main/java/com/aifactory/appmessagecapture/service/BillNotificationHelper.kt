package com.aifactory.appmessagecapture.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aifactory.appmessagecapture.MainActivity
import com.aifactory.appmessagecapture.R
import com.aifactory.appmessagecapture.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 账单识别成功后的通知帮助类。
 *
 * 当后台服务成功识别并记录一笔账单时，推送一条仿照图片样式的系统通知。
 * 通知包含：记账成功金额、分类、时间、今日统计，以及"去查看"操作按钮。
 */
object BillNotificationHelper {

    private const val CHANNEL_ID = "bill_recognized_channel"
    private const val CHANNEL_NAME = "账单识别提醒"
    private const val NOTIFICATION_ID_BASE = 10_000

    private var notificationCounter = 0

    /**
     * 发送账单识别成功通知。
     *
     * @param context 上下文
     * @param appName 应用名称（如"支付宝"）
     * @param amount 金额
     * @param category 分类
     * @param timestamp 时间戳
     * @param isIncome 是否为收入
     */
    suspend fun showBillRecognizedNotification(
        context: Context,
        appName: String,
        amount: Double,
        category: String,
        timestamp: Long,
        isIncome: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            createChannelIfNeeded(notificationManager)

            // 查询今日统计
            val todayStart = getStartOfDayMillis()
            val dao = AppDatabase.getDatabase(context).billDao()
            val todayExpense = dao.getTodayExpenseOnce(todayStart) ?: 0.0
            val todayIncome = dao.getTodayIncomeOnce(todayStart) ?: 0.0
            val todayCount = dao.getTodayCountOnce(todayStart) ?: 0

            val timeStr = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                .format(Date(timestamp))
            val amountStr = String.format(Locale.getDefault(), "%.2f", amount)

            val title = "您在${appName}记账成功${amountStr}元"
            val subText = "$category | $timeStr"
            val statsText = if (isIncome) {
                "今日收入${todayCount}笔，共收入${String.format("%.2f", todayIncome)}元"
            } else {
                "今日消费${todayCount}笔，共支出${String.format("%.2f", todayExpense)}元"
            }

            // 点击打开 MainActivity 并跳转到记账 Tab
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_NAVIGATE_TO_TAB, "bills")
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_BASE + notificationCounter,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val color = if (isIncome) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(subText)
                .setSubText(statsText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$subText\n$statsText")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setColor(color)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(
                    0,
                    "去查看",
                    contentPendingIntent
                )
                .build()

            notificationManager.notify(NOTIFICATION_ID_BASE + notificationCounter, notification)
            notificationCounter = (notificationCounter + 1) % 100
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createChannelIfNeeded(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "当成功识别到新的账单记录时推送通知"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun getStartOfDayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
