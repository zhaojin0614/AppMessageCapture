package com.aifactory.appmessagecapture.data

import androidx.room.withTransaction
import java.time.Instant
import java.time.ZoneId

/**
 * 周期账单执行处理器。
 *
 * 由 [com.aifactory.appmessagecapture.worker.RecurringBillWorker]（每日调度）
 * 和 RecurringBillViewModel（前台即时触发）共同调用，替代原先两处复制粘贴的实现。
 *
 * 相比旧实现的三个修复：
 * 1. 每条周期账单的 insert + updateNextDueDate 在同一事务中——进程中途被杀
 *    或 Worker retry 不会造成重复记账；
 * 2. 补齐循环：设备长期关机错过的每个周期都会按各自的到期时间补记一笔，
 *    而不是每次执行只补一笔、nextDueDate 永远追不上当前时间；
 * 3. 事务内重读最新状态——Worker 与前台并发触发时不会重复执行同一条。
 */
object RecurringBillProcessor {

    /**
     * 处理所有到期（nextDueDate <= [now]）的周期账单。
     * @return 本次新建的账单条数
     */
    suspend fun processDueBills(db: AppDatabase, now: Long = System.currentTimeMillis()): Int {
        val recurringDao = db.recurringBillDao()
        val billDao = db.billDao()
        var created = 0

        val dueBills = recurringDao.getDueRecurringBills(now)
        for (recurring in dueBills) {
            db.withTransaction {
                // Re-read inside the transaction: another processor (Worker vs UI)
                // may have already advanced this row since the outer query.
                val fresh = recurringDao.getRecurringBillById(recurring.id) ?: return@withTransaction
                if (!fresh.isActive || fresh.nextDueDate > now) return@withTransaction

                var due = fresh.nextDueDate
                while (due <= now) {
                    billDao.insert(
                        BillEntity(
                            amount = fresh.amount,
                            appName = "周期记账",
                            packageName = "",
                            title = fresh.title,
                            category = fresh.category,
                            isIncome = fresh.isIncome,
                            timestamp = due
                        )
                    )
                    created++
                    due = nextDueDateAfter(due, fresh.frequency)
                }
                recurringDao.updateNextDueDate(fresh.id, due)
            }
        }
        return created
    }

    /**
     * 计算下一个到期时间（基于 LocalDate 日历运算，正确处理月末与闰年边界：
     * 如 1月31日按月推进会落在 2月28/29日）。
     */
    fun nextDueDateAfter(
        fromMillis: Long,
        frequency: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val currentDate = Instant.ofEpochMilli(fromMillis).atZone(zoneId).toLocalDate()
        val nextDate = when (frequency) {
            RecurringFrequency.DAILY.name -> currentDate.plusDays(1)
            RecurringFrequency.WEEKLY.name -> currentDate.plusWeeks(1)
            RecurringFrequency.BIWEEKLY.name -> currentDate.plusWeeks(2)
            RecurringFrequency.MONTHLY.name -> currentDate.plusMonths(1)
            RecurringFrequency.YEARLY.name -> currentDate.plusYears(1)
            else -> currentDate.plusMonths(1) // 未知频率按月处理
        }
        return nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
