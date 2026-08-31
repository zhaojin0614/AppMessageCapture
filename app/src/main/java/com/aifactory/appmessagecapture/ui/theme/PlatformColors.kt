package com.aifactory.appmessagecapture.ui.theme

import androidx.compose.ui.graphics.Color
import com.aifactory.appmessagecapture.data.PlatformAccountEntity
import kotlin.random.Random

/**
 * 平台账户品牌色工具。
 *
 * 解决报表「平台构成」全是一个颜色的历史缺陷：此前平台名不在分类色表里，
 * 全部落到兜底色。现在：
 * - 新建平台时随机分配一个颜色并持久化到 [PlatformAccountEntity.colorArgb]；
 * - 展示时优先取持久化颜色；老数据（colorArgb=null）按平台名匹配品牌色，
 *   匹配不上再落随机兜底色（基于名称哈希，同一平台颜色稳定）。
 *
 * 品牌色映射按平台名关键字命中（如「微信」→ 微信绿），命中即返回品牌色，
 * 让常见平台的图标/构成天然呈现品牌辨识度。
 */
object PlatformColors {

    /** 新建平台时从候选色板随机取一个（排除接近主题主色的颜色可后续优化） */
    fun randomColor(existingArgb: Collection<Int> = emptyList()): Int {
        var pick = PALETTE.random()
        // 简单防撞：已有颜色里已用过的则换一个（最多重试几次）
        repeat(PALETTE.size) {
            if (pick in existingArgb) pick = PALETTE.random() else return pick
        }
        return pick
    }

    fun toColor(argb: Int): Color = Color(argb)

    /** 平台名 → 品牌主色（稳定映射，名称含关键字即命中） */
    fun brandColorFor(name: String): Int? {
        val lower = name.lowercase()
        return BRAND_MAP.entries.firstOrNull { (keyword, _) ->
            lower.contains(keyword)
        }?.value
    }

    /** 从平台账户解析展示颜色：持久化 > 品牌色 > 名称哈希兜底 */
    fun resolveColor(account: PlatformAccountEntity): Color {
        account.colorArgb?.let { return Color(it) }
        brandColorFor(account.name)?.let { return Color(it) }
        return Color(hashFallback(account.name))
    }

    /** 报表按平台名取色（不含账户对象时的入口，如"待对账"） */
    fun colorForPlatformName(name: String): Color {
        brandColorFor(name)?.let { return Color(it) }
        return Color(hashFallback(name))
    }

    private fun hashFallback(name: String): Int {
        val idx = (name.hashCode() and Int.MAX_VALUE) % PALETTE.size
        return PALETTE[idx]
    }

    /** 候选色板：与分类色表观感协调的柔和色 */
    private val PALETTE = listOf(
        0xFF5B9BD8.toInt(), // 蓝
        0xFF7D8FF2.toInt(), // 靛
        0xFFE07A4F.toInt(), // 橙
        0xFF3FAE7E.toInt(), // 绿
        0xFFEE7BA6.toInt(), // 粉
        0xFF9B8CE8.toInt(), // 紫
        0xFFF2B84B.toInt(), // 黄
        0xFF4CB5C0.toInt(), // 青
        0xFFA58C6D.toInt(), // 棕
        0xFF7C8B98.toInt(), // 灰
    )

    private val BRAND_MAP = linkedMapOf(
        "微信" to 0xFF07C160,
        "支付宝" to 0xFF1677FF,
        "美团" to 0xFFFFC300,
        "云闪付" to 0xFFE8484F,
        "京东" to 0xFFE1251B,
        "淘宝" to 0xFFFF5000,
        "银行" to 0xFF0E6EB8,
        "农业银行" to 0xFF0E6EB8,
        "工商银行" to 0xFFC7000B,
        "招商银行" to 0xFF9E1F63,
        "建设银行" to 0xFF003B8F,
        "中国银行" to 0xFFB32424,
        "交通银行" to 0xFF1F2D5A,
        "邮储" to 0xFF00843D,
        "储蓄" to 0xFF00843D,
        "现金" to 0xFF2E9E5B,
    ).map { (k, v) -> k to v.toInt() }.toMap()
}
