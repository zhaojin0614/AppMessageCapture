package com.aifactory.appmessagecapture.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 商户记忆手动覆写（商户记忆管理界面写入）。
 *
 * 覆写优先级高于账单统计投票：
 * - [category]/[platformAccountId] 非空时直接采用（含平台余额联动）；
 * - [ignoreAuto] = true 时忽略该商户的一切自动记忆（投票/跨商户/渠道证据），
 *   分类走统计先验或关键词猜测，平台保持待对账；
 * - 三个字段全空且 ignoreAuto=false 的行等于无覆写（管理界面清除后删除行）。
 */
@Entity(tableName = "merchant_memory_overrides")
data class MerchantMemoryOverrideEntity(
    /** 跨商户指纹核心（MerchantMemory.core），全局唯一 */
    @PrimaryKey val fingerprint: String,
    val category: String? = null,
    val platformAccountId: Long? = null,
    val ignoreAuto: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
