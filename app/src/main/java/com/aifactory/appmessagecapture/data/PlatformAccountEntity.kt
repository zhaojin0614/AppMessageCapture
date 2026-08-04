package com.aifactory.appmessagecapture.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 平台账户实体
 *
 * 记录用户在各平台（如微信钱包、支付宝、银行卡等）的当前余额。
 * 账单可通过 [BillEntity.platformAccountId] 关联到某个平台：
 * - 支出账单：从对应平台余额中扣除
 * - 收入账单：增加到对应平台余额中
 * 未关联平台的账单标记为"待对账"，用户可后续分配。
 *
 * 总金额 = 所有平台余额之和，在记账界面下拉面板展示。
 */
@Entity(tableName = "platform_accounts")
data class PlatformAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // 平台名称，如"微信钱包"、"支付宝"
    val balance: Double,                 // 当前余额
    val icon: String? = null,            // 预留图标标识（暂用名称首字母展示）
    val sortOrder: Int = 0,              // 排序权重，越小越靠前
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
