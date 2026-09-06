package com.aifactory.appmessagecapture.service

import com.aifactory.appmessagecapture.utils.MerchantKey

/**
 * 商户记忆推断纯函数集（无 Android 依赖，可单元测试）。
 *
 * 记忆推断按置信度分层（高→低，见运行清单「商户记忆智能化」）：
 * 1. 手动覆写（管理界面设置，存 merchant_memory_overrides 表，BillIngestor 查询）
 * 2. 商户键多数投票：同键最近 N 笔账单，分类取众数、平台取最近非空
 * 3. 跨商户指纹：商户名核心归一后跨 App 投票（淘宝闪购/拼多多/美团同一
 *    线下商户共享记忆）
 * 4. 渠道→平台映射 + 消费习惯统计先验（[CategoryPrior]）
 *
 * 本对象只含第 2/3 层的纯计算与指纹归一；查询与组装在 BillIngestor。
 */
object MerchantMemory {

    /** 参与投票的最近账单数 */
    const val VOTE_SAMPLE_SIZE = 5

    /** 渠道/营销前缀词（标题开头的频道标识，不属于商户名本身） */
    private val CHANNEL_PREFIXES = listOf("闪购", "外卖")

    /** 店铺名后缀（归一时剥离，跨 App 对齐同一商户） */
    private val SHOP_SUFFIXES = listOf(
        "官方旗舰店", "官方直营店", "官方直营", "官方旗舰", "旗舰店",
        "专营店", "专卖店", "直营店", "回头客好店", "工厂店", "折扣店", "特约店", "官方"
    )

    /** 渠道通知标题本身（不是商户名，不产生指纹） */
    private val NON_MERCHANT_CORES = setOf(
        "微信支付", "交易提醒", "付款成功", "支付成功", "退款通知", "收款", "转账"
    )

    /** 参与投票的一条历史样本（同商户键最近一笔的分类与平台） */
    data class MemorySample(val category: String, val platformId: Long?)

    data class Vote(val category: String?, val platformId: Long?)

    /**
     * 多数投票。samples 必须按时间倒序（最近在前）。
     * - 分类：取众数，计数相同取更近的（LinkedHashMap 保持最近优先的插入序，
     *   严格大于才替换 → 平票时最近者胜）
     * - 平台：取最近一个非空——平台是"绑定一次即粘住"的信号（当前行为语义：
     *   用户对账过一次就持续生效）；彻底解绑走管理界面覆写
     */
    fun vote(samples: List<MemorySample>): Vote {
        if (samples.isEmpty()) return Vote(null, null)
        val counts = LinkedHashMap<String, Int>()
        samples.forEach { counts[it.category] = (counts[it.category] ?: 0) + 1 }
        var best: String? = null
        var bestCount = -1
        for ((cat, count) in counts) {
            if (count > bestCount) {
                best = cat
                bestCount = count
            }
        }
        val platform = samples.firstOrNull { it.platformId != null }?.platformId
        return Vote(best, platform)
    }

    /**
     * 跨商户指纹：从标题提取商户名核心（不带来源 App 前缀，跨入口可对齐）。
     * 例：(淘宝闪购,「袁记云饺(文汇路店) 实付¥18.98」) -> 「袁记云饺文汇路店」
     *     (拼多多,「羊羊羊大叔 实付¥3.84」)           -> 「羊羊羊大叔」
     * 先在实付/应付处截断金额尾巴，再剥渠道前缀与店铺后缀（循环剥到稳定，
     * 处理「XX官方旗舰店官方旗舰」这类叠名）。不足 2 字视为无有效商户身份。
     */
    fun core(appName: String, title: String): String? = coreOfTitle(title)

    /** 从已存储的商户键（"appName|normalizedTitle"）还原跨商户指纹核心 */
    fun coreFromKey(merchantKey: String): String? {
        val titlePart = merchantKey.substringAfter('|', "")
        if (titlePart.isEmpty()) return null
        return coreOfTitle(titlePart)
    }

    private fun coreOfTitle(title: String): String? {
        var t = title.substringBefore("实付").substringBefore("应付").substringBefore("价格明细")
        CHANNEL_PREFIXES.forEach { prefix ->
            while (t.startsWith(prefix)) t = t.removePrefix(prefix)
        }
        t = MerchantKey.normalize(t)
        var changed = true
        while (changed) {
            changed = false
            SHOP_SUFFIXES.forEach { suffix ->
                if (t.length > suffix.length && t.endsWith(suffix)) {
                    t = t.removeSuffix(suffix)
                    changed = true
                }
            }
        }
        return t.trim().takeIf { it.length >= 2 && it !in NON_MERCHANT_CORES }
    }

    /**
     * 渠道包名 → 已绑定平台 ID。platforms 为 (平台ID, 绑定渠道包名) 列表，
     * 第一个绑定该渠道的平台生效；渠道为空或无人绑定返回 null。
     */
    fun matchPlatform(channelPackage: String?, platforms: List<Pair<Long, String?>>): Long? {
        if (channelPackage.isNullOrBlank()) return null
        return platforms.firstOrNull { it.second == channelPackage }?.first
    }
}
