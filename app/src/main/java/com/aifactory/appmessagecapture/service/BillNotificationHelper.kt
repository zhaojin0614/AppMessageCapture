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
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.ui.BillQuickEditActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 账单识别成功后的通知帮助类。
 *
 * 当后台服务成功识别并记录一笔账单时，推送一条系统通知。通知包含：
 * 记账成功金额、分类、扣款平台（或「待对账」）、时间、今日统计，以及三个操作：
 * - 去查看：打开 App 记账 Tab
 * - 改分类 / 扣款平台：打开 [BillQuickEditActivity] 弹窗直接选择，
 *   无需进 App 逐条确认。选择保存后通过 [repostBillNotification] 刷新本条通知。
 */
object BillNotificationHelper {

    private const val CHANNEL_ID = "bill_recognized_channel"
    private const val CHANNEL_NAME = "账单识别提醒"
    private const val NOTIFICATION_ID_BASE = 10_000

    private var notificationCounter = 0

    // BillQuickEditActivity 的 intent extras
    const val EXTRA_BILL_ID = "bill_id"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_MODE = "mode"
    const val MODE_CATEGORY = "category"
    const val MODE_PLATFORM = "platform"

    /**
     * 发送账单识别成功通知（入库后调用）。
     * 通知内容按 [billId] 从数据库现读，保证与入库数据一致。
     */
    suspend fun showBillRecognizedNotification(context: Context, billId: Long) =
        withContext(Dispatchers.IO) {
            try {
                val bill = AppDatabase.getDatabase(context).billDao().getBillByIdOnce(billId)
                    ?: return@withContext
                val notificationId = synchronized(this) {
                    NOTIFICATION_ID_BASE + notificationCounter.also {
                        notificationCounter = (notificationCounter + 1) % 100
                    }
                }
                post(context, bill, notificationId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    /**
     * 快捷编辑（改分类/换平台）保存后，按原通知 ID 重发以刷新通知内容。
     */
    suspend fun repostBillNotification(context: Context, billId: Long, notificationId: Int) =
        withContext(Dispatchers.IO) {
            try {
                if (notificationId < NOTIFICATION_ID_BASE) return@withContext
                val bill = AppDatabase.getDatabase(context).billDao().getBillByIdOnce(billId)
                    ?: return@withContext
                post(context, bill, notificationId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    private suspend fun post(context: Context, bill: BillEntity, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannelIfNeeded(notificationManager)

        val db = AppDatabase.getDatabase(context)
        val platformName = bill.platformAccountId
            ?.let { db.platformAccountDao().getById(it)?.name }

        // 查询今日统计（单条聚合 SQL）
        val stats = db.billDao().getTodayStatsOnce(getStartOfDayMillis())
        val statsText = if (bill.isIncome) {
            "今日收入${stats?.count ?: 0}笔，共收入${String.format("%.2f", stats?.income ?: 0.0)}元"
        } else {
            "今日消费${stats?.count ?: 0}笔，共支出${String.format("%.2f", stats?.expense ?: 0.0)}元"
        }

        val timeStr = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            .format(Date(bill.timestamp))
        val amountStr = String.format(Locale.getDefault(), "%.2f", bill.amount)
        val title = "您在${bill.appName}记账成功${amountStr}元"
        val subText = "${bill.category} · ${platformName ?: "待对账"} · $timeStr"

        // 点击打开 MainActivity 并跳转到记账 Tab
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO_TAB, "bills")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val color = if (bill.isIncome) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt()

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
            .addAction(0, "去查看", contentPendingIntent)
            .addAction(0, "改分类", quickEditPendingIntent(context, bill.id, notificationId, MODE_CATEGORY))
            .addAction(0, "扣款平台", quickEditPendingIntent(context, bill.id, notificationId, MODE_PLATFORM))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /** 「改分类/扣款平台」按钮 → 弹窗 Activity（带账单与通知 ID，保存后按原 ID 刷新通知） */
    private fun quickEditPendingIntent(
        context: Context,
        billId: Long,
        notificationId: Int,
        mode: String
    ): PendingIntent {
        val intent = Intent(context, BillQuickEditActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_BILL_ID, billId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_MODE, mode)
        }
        // requestCode 需按 (bill, mode) 区分，否则不同账单的按钮会互相覆盖 extras
        val requestCode = ((billId and 0x1FFFFFFF).toInt() shl 2) or
            if (mode == MODE_CATEGORY) 1 else 2
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
