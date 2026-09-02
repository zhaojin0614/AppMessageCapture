package com.aifactory.appmessagecapture.service

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 淘宝闪购订单详情页解析（纯函数，无 Android 依赖，可单元测试）。
 *
 * 淘宝闪购是淘宝 App（[SupportedPaymentApps.TAOBAO_PACKAGE]）内的频道，也是
 * 独立 App（[SupportedPaymentApps.ELE_PACKAGE]，原饿了么换牌）。两个入口的
 * 支付完成订单页布局一致，门槛统一为「闪购」+「实付」+「下单时间」：
 * - 「下单时间」（精确到毫秒、每单唯一）→ 账单 timestamp；同一订单再次打开
 *   时由 [BillIngestor] 内容去重（时间窗口锚定在下单时间上）幂等丢弃；
 * - 商户名 + 实付金额 → 账单 title；
 * - 历史订单详情页「下单时间」折叠在「订单信息」里不命中门槛，天然不触发
 *   （重复记账防护的另一半）。
 *
 * 页面文本节点样例见 TaobaoShangouParsingTest（取自真实订单页截图）。
 */
object TaobaoShangouParsing {

    /** 金额数字语法与 [BillParsing] 一致：整数支持千分位，小数最多两位 */
    private const val AMOUNT_NUMBER = """(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?"""

    /** 「实付」后紧跟的货币金额（实付¥18.98 / 实付 ¥18.98） */
    private val PAID_AMOUNT = Regex("""实付\s*[¥￥]\s*$AMOUNT_NUMBER""")

    /** 「闪购 商户名」形式的商户行（闪购标与名称同行，尾部可能带箭头装饰） */
    private val MERCHANT_LINE = Regex("""闪购\s+(\S.*)""")

    /** 「2026-08-30 18:16:14.118」形式的时间戳（毫秒段可选） */
    private val ORDER_DATETIME =
        Regex("""(\d{4})-(\d{2})-(\d{2})[ ](\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,3}))?""")

    /**
     * 「闪购」节点拆分时商户候选词的黑名单：订单页底部的频道 Tab（如「首页」）
     * 也可能是独立的「闪购」文本节点，其后紧邻的 Tab 名不是商户。
     */
    private val NON_MERCHANT_WORDS = setOf(
        "首页", "消息", "购物车", "我的", "客服", "去下单", "查看订单", "回首页", "再下一单"
    )

    /**
     * 页面是否为支付完成后的淘宝闪购订单详情页（两个入口统一门槛）。
     *
     * 三个标志词缺一不可：「闪购」区分普通淘宝订单页；「实付」是金额提取的
     * 前提；「下单时间」区分订单列表页（无该行）与历史订单详情页（该行折叠
     * 在「订单信息」里不可见——只有支付完成后的页面默认展开，这也保证了
     * 历史订单不会被重复入账）。
     */
    fun isOrderPage(pageText: String): Boolean =
        pageText.contains("闪购") && pageText.contains("实付") && pageText.contains("下单时间")

    /** 从单行文本提取「实付」金额；营销行（实付满15…）等无货币金额时返回 null */
    fun parsePaidAmount(line: String): Double? =
        PAID_AMOUNT.find(line)?.toAmount()

    /** 节点列表中第一条含「实付」金额的行 */
    fun extractPaidAmount(nodes: List<String>): Double? =
        nodes.firstNotNullOfOrNull { parsePaidAmount(it) }

    /**
     * 商户名：优先「闪购 商户名」同行节点；闪购标与名称被拆成两个节点时退化为
     * 「闪购」节点的下一个候选（黑名单过滤 Tab 名）。找不到返回 null。
     */
    fun extractMerchant(nodes: List<String>): String? {
        nodes.forEach { line ->
            MERCHANT_LINE.find(line)?.let { return cleanupMerchant(it.groupValues[1]) }
        }
        val idx = nodes.indexOfFirst { it.trim() == "闪购" }
        if (idx in 0 until nodes.lastIndex) {
            val candidate = nodes[idx + 1].trim()
            if (candidate.length >= 2 && candidate !in NON_MERCHANT_WORDS) {
                return cleanupMerchant(candidate)
            }
        }
        return null
    }

    /** 单条文本是否含完整的「下单时间」时间戳（yyyy-MM-dd HH:mm:ss[.SSS]） */
    fun containsOrderDatetime(text: String): Boolean = ORDER_DATETIME.containsMatchIn(text)

    /**
     * 解析页面上的「下单时间」（yyyy-MM-dd HH:mm:ss[.SSS]）为 epoch millis。
     * 页面上唯一的全时间戳就是下单时间行（送达时间只有 MM-dd，预计送达只有
     * HH:mm），逐节点找第一个命中即可；缺失或非法返回 null。
     */
    fun parseOrderTimeMillis(nodes: List<String>, zone: ZoneId = ZoneId.systemDefault()): Long? {
        nodes.forEach { line ->
            ORDER_DATETIME.find(line)?.let { m ->
                val millis = m.groupValues[7].ifEmpty { "0" }.padEnd(3, '0').toInt()
                return LocalDateTime.of(
                    m.groupValues[1].toInt(),
                    m.groupValues[2].toInt(),
                    m.groupValues[3].toInt(),
                    m.groupValues[4].toInt(),
                    m.groupValues[5].toInt(),
                    m.groupValues[6].toInt(),
                    millis * 1_000_000
                ).atZone(zone).toInstant().toEpochMilli()
            }
        }
        return null
    }

    /** 去掉商户名尾部的箭头/空白装饰；清空后视为无商户 */
    private fun cleanupMerchant(raw: String): String? =
        raw.trim().trimEnd('>', '›', '｜', '|', ' ').trim().ifEmpty { null }

    private fun MatchResult.toAmount(): Double? {
        val integerPart = groupValues[1].replace(",", "")
        if (integerPart.isEmpty()) return null
        val fraction = groupValues[2]
        val value = if (fraction.isEmpty()) {
            integerPart.toDoubleOrNull()
        } else {
            "$integerPart.$fraction".toDoubleOrNull()
        }
        return value?.takeIf { it > 0 }
    }
}
