package com.aifactory.appmessagecapture.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 周期性账单实体
 * 用于存储用户设置的周期性账单（如房租、会员费、话费等）
 */
@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                    // 账单标题（如：房租、Netflix会员）
    val amount: Double,                   // 金额
    val category: String,                 // 分类（使用 Categories.kt 中的常量）
    val isIncome: Boolean = false,        // false=支出, true=收入
    val frequency: String,                // 频率：DAILY, WEEKLY, BIWEEKLY, MONTHLY, YEARLY
    val startDate: Long,                  // 开始日期（毫秒时间戳）
    val nextDueDate: Long,                // 下次执行日期（毫秒时间戳）
    val isActive: Boolean = true,         // 是否启用
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 周期频率枚举
 */
enum class RecurringFrequency(val displayName: String, val days: Int) {
    DAILY("每天", 1),
    WEEKLY("每周", 7),
    BIWEEKLY("每两周", 14),
    MONTHLY("每月", 30),
    YEARLY("每年", 365)
}
