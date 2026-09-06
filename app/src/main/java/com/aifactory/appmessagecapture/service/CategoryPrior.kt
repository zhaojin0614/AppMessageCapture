package com.aifactory.appmessagecapture.service

import java.time.Instant
import java.time.ZoneId

/**
 * 消费习惯统计先验（纯函数，无 Android 依赖，可单元测试）。
 *
 * 商户身份不可知时（微信「微信支付」/支付宝「交易提醒」等渠道通知没有
 * 商户信息），用用户自己的历史推断分类。三个信号：
 * - 来源 App（付款渠道）：同渠道消费倾向一致，权重最高（+2）
 * - 时段：早/午/晚/夜各档消费类型差异大（+1）
 * - 金额档：小额高频（早午餐饮）与大额低频（购物）区分（+1）
 *
 * 只用于分类推断，不用于平台对账（平台需要渠道绑定这类铁证，见
 * MerchantMemory.matchPlatform）——避免"统计猜错还扣了余额"。
 */
object CategoryPrior {

    /** 参与统计的一条历史消费样本 */
    data class ExpenseSample(
        val category: String,
        val packageName: String,
        val timestamp: Long,
        val amount: Double
    )

    /** 最少样本数（整个样本集），低于此值不推断 */
    const val MIN_TOTAL_SAMPLES = 5

    /** 胜出类目的最低得分 */
    const val MIN_SCORE = 3

    /** 胜出类目的最少历史出现次数（防止两三笔巧合定终身） */
    const val MIN_SUPPORT = 3

    /** 时段桶：5-9 早餐 / 10-14 午 / 15-16 下午 / 17-21 晚 / 其余夜宵 */
    fun hourBucket(hour: Int): Int = when (hour) {
        in 5..9 -> 0
        in 10..14 -> 1
        in 15..16 -> 2
        in 17..21 -> 3
        else -> 4
    }

    /** 金额档：<15 / 15-50 / 50-200 / ≥200 */
    fun amountBucket(amount: Double): Int = when {
        amount < 15 -> 0
        amount < 50 -> 1
        amount < 200 -> 2
        else -> 3
    }

    /**
     * 推断分类。逐样本计分：同渠道 +2、同时段 +1、同金额档 +1；
     * 最高分类目需同时满足 ≥[MIN_SCORE] 分且历史出现 ≥[MIN_SUPPORT] 次，
     * 否则返回 null（调用方回退关键词猜测）。平分时按类目名稳定排序。
     */
    fun infer(
        samples: List<ExpenseSample>,
        packageName: String,
        timestamp: Long,
        amount: Double
    ): String? {
        if (samples.size < MIN_TOTAL_SAMPLES) return null
        val hourBucket = hourBucket(hourOf(timestamp))
        val amountBucket = amountBucket(amount)
        val scores = HashMap<String, Int>()
        samples.forEach { s ->
            var score = 0
            if (packageName.isNotBlank() && s.packageName == packageName) score += 2
            if (hourBucket(hourOf(s.timestamp)) == hourBucket) score += 1
            if (amountBucket(s.amount) == amountBucket) score += 1
            if (score > 0) scores.merge(s.category, score, Int::plus)
        }
        val best = scores.entries.maxWithOrNull(
            compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        ) ?: return null
        val support = samples.count { it.category == best.key }
        return if (best.value >= MIN_SCORE && support >= MIN_SUPPORT) best.key else null
    }

    private fun hourOf(timestamp: Long): Int =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).hour
}
