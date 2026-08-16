package com.aifactory.appmessagecapture.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    indices = [
        // 时间范围统计（今日/本月/报表）与按时间排序
        Index(value = ["timestamp"]),
        // 去重查询：同 App 同金额时间窗
        Index(value = ["packageName", "timestamp"]),
        Index(value = ["amount", "timestamp"])
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val appName: String,
    val packageName: String,
    val secondaryAppName: String? = null,
    val secondaryPackageName: String? = null,
    val title: String,
    val category: String = "未分类",
    val isIncome: Boolean = false,
    val timestamp: Long,
    /**
     * 关联的平台账户 ID。
     * - 非空：已对账，支出从该平台扣款 / 收入存入该平台。
     * - null：未对账（待对账），金额未从任何平台扣除，需用户后续分配。
     * 旧账单迁移后默认为 null。
     */
    val platformAccountId: Long? = null
)
