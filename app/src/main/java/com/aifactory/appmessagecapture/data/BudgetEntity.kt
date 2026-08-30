package com.aifactory.appmessagecapture.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 月度预算：[category] 为空串表示月度总预算，否则为该分类的月度预算。
 * 同一分类只有一条预算记录（唯一索引），金额每月复用（不做按月历史）。
 */
@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val amount: Double
)
