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
 * 预算阈值通知：账单入库/编辑后检查本月支出与月度总预算 / 各分类预算的比例。
 *
 * 两级提醒（80% 预警 / 100% 超支），总预算与各分类预算独立监控。
 * 提醒按「级别上升」触发：与上次检查相比级别升高才发通知，回落则静默
 * 同步记录——误选分类触发提醒、纠正后支出回落时，之后真实消费再次跨
 * 阈值仍会重新提醒（若只记"已提醒过"就会永远沉默）。级别不变不重复发；
 * 状态记录在 SharedPreferences，跨月自动重置。
 * 通知固定 ID 原地更新，升级级别时覆盖旧提醒而不是堆叠。
 */
object BudgetNotifier {

    private const val CHANNEL_ID = "budget_alert_v2_channel"
    private const val CHANNEL_NAME = "预算提醒"
    private const val PREFS = "budget_notify_state"
    private const val KEY_MONTH = "month"
    private const val NOTIFICATION_ID = 9001

    /** 级别：0 正常 / 1 预警(≥80%) / 2 超支(≥100%) */
    const val LEVEL_OK = 0
    const val LEVEL_WARN = 1
    const val LEVEL_OVER = 2

    /**
     * 预算阈值判定（纯函数，便于单测）：
     * 已用 ≥ 预算 → 超支；已用 ≥ 预算×80% → 预警；否则正常。
     */
    internal fun budgetLevel(spent: Double, budget: Double): Int = when {
        spent >= budget -> LEVEL_OVER
        spent >= budget * 0.8 -> LEVEL_WARN
        else -> LEVEL_OK
    }

    /** 监控项：总预算（category=null）或某分类预算 */
    private data class BudgetWatch(
        val category: String?,
        val budget: Double,
        val spent: Double
    ) {
        val level: Int get() = budgetLevel(spent, budget)
        val label: String
            get() = category ?: "总预算"
    }

    suspend fun checkAndNotify(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            val allBudgets = db.budgetDao().getAllOnce()
            if (allBudgets.isEmpty()) return

            val totalBudget = allBudgets.firstOrNull { it.category == "" }?.amount ?: 0.0
            // 分类预算：排除总预算行（category == ""），金额 > 0 才算
            val categoryBudgets = allBudgets
                .filter { it.category.isNotEmpty() && it.amount > 0 }
                .associate { it.category to it.amount }

            // 没有任何预算（总预算也未设）→ 无意义
            if (totalBudget <= 0.0 && categoryBudgets.isEmpty()) return

            val monthStart = LocalDate.now().withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val watches = mutableListOf<BudgetWatch>()
            if (totalBudget > 0.0) {
                val totalSpent = db.billDao().getMonthExpenseOnce(monthStart) ?: 0.0
                watches += BudgetWatch(null, totalBudget, totalSpent)
            }
            if (categoryBudgets.isNotEmpty()) {
                // 本月各分类实际支出
                val spendByCategory = db.billDao().getMonthCategoryExpenseOnce(monthStart)
                    .associate { it.category to it.total }
                categoryBudgets.forEach { (category, budget) ->
                    val spent = spendByCategory[category] ?: 0.0
                    watches += BudgetWatch(category, budget, spent)
                }
            }

            // 与上次检查比较各监控项的级别：升高才提醒，回落静默同步记录
            // 键：level:<category|total>，值：上次检查时的级别
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val currentMonth = YearMonth.now().toString()
            if (prefs.getString(KEY_MONTH, "") != currentMonth) {
                prefs.edit().clear().putString(KEY_MONTH, currentMonth).apply()
            }

            val editor = prefs.edit()
            val toNotify = watches.filter { watch ->
                val key = "level:${watch.category ?: "total"}"
                val previous = prefs.getInt(key, LEVEL_OK)
                editor.putInt(key, watch.level)
                // 级别上升（如 80% 预警 → 100% 超支，或正常 → 预警）才提醒；
                // 回落（纠正误选分类）静默更新记录，不拦截之后的再次提醒
                watch.level > previous
            }
            editor.apply()
            if (toNotify.isEmpty()) return

            postNotification(context, toNotify)
            BirthdayLog.i(
                "[BudgetNotifier] notified " +
                    toNotify.joinToString { "${it.label}(level=${it.level},${it.spent}/${it.budget})" }
            )
        } catch (e: Exception) {
            BirthdayLog.logException("[BudgetNotifier] checkAndNotify", e)
        }
    }

    private fun postNotification(context: Context, watches: List<BudgetWatch>) {        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

        val overWatches = watches.filter { it.level == LEVEL_OVER }
        val warnWatches = watches.filter { it.level == LEVEL_WARN }

        val title = when {
            overWatches.isNotEmpty() && warnWatches.isNotEmpty() -> "本月预算已超支"
            overWatches.isNotEmpty() -> "本月预算已超支"
            else -> "本月预算预警"
        }

        // 多行文本：每项一行「名称 已用¥X / 预算¥Y」
        val lines = buildString {
            overWatches.forEach { watch ->
                append("超支 · ${watch.label}：已用 ¥%.2f / 预算 ¥%.2f\n".format(watch.spent, watch.budget))
            }
            warnWatches.forEach { watch ->
                append("预警 · ${watch.label}：已用 ¥%.2f / 预算 ¥%.2f\n".format(watch.spent, watch.budget))
            }
            append("点击查看明细")
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(lines.trim().substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.trim()))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
