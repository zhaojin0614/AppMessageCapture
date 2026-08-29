package com.aifactory.appmessagecapture.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.aifactory.appmessagecapture.MainActivity
import com.aifactory.appmessagecapture.R
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.ui.BillQuickEditActivity
import com.aifactory.appmessagecapture.ui.BillQuickEditOverlayReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 账单识别成功后的通知帮助类。
 *
 * 当后台服务成功识别并记录一笔账单时，推送一条系统通知。通知包含：
 * 记账成功金额、分类、扣款平台（或「待对账」）、时间、今日统计，以及两个操作：
 * - 去查看：打开 App 记账 Tab（打开后该通知自动清除）
 * - 完善账单：打开 [BillQuickEditActivity] 双栏弹窗，左侧选分类、右侧选
 *   扣款平台，一次保存两项（平台经 AccountRepository 联动余额），
 *   保存后通知清除，无需进 App 逐条确认。
 *
 * 通知为常驻（setOngoing）：不会被系统回收、不能下滑清除，直到用户完成
 * 任一操作（保存/去查看）才消失。注：屏幕顶部的悬浮横幅在几秒后收起是
 * 系统行为（无公开 API 可钉住），收起后通知仍保留在通知栏直到处理完成。
 */
object BillNotificationHelper {

    private const val CHANNEL_ID = "bill_recognized_channel"
    private const val CHANNEL_NAME = "账单识别提醒"

    /** 账单通知 ID 下限（id 从它起滚动分配），外部用于校验/清除 */
    const val NOTIFICATION_ID_BASE = 10_000

    private var notificationCounter = 0

    // intent extras
    const val EXTRA_BILL_ID = "bill_id"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    const val EXTRA_CANCEL_NOTIFICATION_ID = "cancel_notification_id"

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

        // 点击打开 MainActivity 并跳转到记账 Tab，同时清除本条通知
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO_TAB, "bills")
            putExtra(EXTRA_CANCEL_NOTIFICATION_ID, notificationId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val color = if (bill.isIncome) 0xFF4CAF50.toInt() else 0xFFFF5252.toInt()

        // bill_id 写入通知 extras：账单删除时按它匹配并联动清除对应通知
        val extras = android.os.Bundle().apply { putLong(EXTRA_BILL_ID, bill.id) }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setExtras(extras)
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
            // 常驻：不自动消失、不可下滑清除，直到用户完成操作
            // （保存分类/平台或点「去查看」时由代码显式 cancel）
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "去查看", contentPendingIntent)
            .addAction(0, "完善账单", quickEditPendingIntent(context, bill.id, notificationId))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * 「完善账单」按钮入口，按悬浮窗权限二选一：
     * - 已授予「显示悬浮窗」→ 广播拉起 [BillQuickEditOverlay] 悬浮窗，
     *   在任意界面之上直接选择（不跳转 App）；
     * - 未授予 → 打开 [BillQuickEditActivity] 透明弹窗（回退路径；
     *   Android 12+ 禁止通知广播内启动 Activity，不能在接收器里回退）。
     */
    private fun quickEditPendingIntent(
        context: Context,
        billId: Long,
        notificationId: Int
    ): PendingIntent {
        // requestCode 按账单区分，避免不同账单的按钮互相覆盖 extras
        val requestCode = (billId and 0x3FFFFFFF).toInt() shl 1
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Settings.canDrawOverlays(context)) {
            val intent = Intent(context, BillQuickEditOverlayReceiver::class.java).apply {
                putExtra(EXTRA_BILL_ID, billId)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            PendingIntent.getBroadcast(context, requestCode, intent, piFlags)
        } else {
            val intent = Intent(context, BillQuickEditActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_BILL_ID, billId)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            PendingIntent.getActivity(context, requestCode, intent, piFlags)
        }
    }

    /**
     * 账单被删除后联动清除其常驻通知。
     * 按 [EXTRA_BILL_ID] 匹配当前活动的通知并逐条 cancel；
     * 传 null 表示清除本应用全部账单通知（清空账单用）。
     */
    fun cancelNotificationsForBills(context: Context, billIds: Collection<Long>?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val targets = billIds?.toHashSet()
        nm.activeNotifications
            .filter { sbn ->
                val extra = sbn.notification.extras.getLong(EXTRA_BILL_ID, -1L)
                when (targets) {
                    null -> extra >= 0            // 带 bill_id 标记的均为账单通知
                    else -> extra in targets
                }
            }
            .forEach { nm.cancel(it.id) }
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
