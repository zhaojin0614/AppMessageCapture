package com.aifactory.appmessagecapture.service

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 淘宝闪购订单详情页解析（纯函数，无 Android 依赖，可单元测试）。
 *
 * 淘宝闪购是淘宝 App（[SupportedPaymentApps.TAOBAO_PACKAGE]）内的频道，也是
 * 独立 App（[SupportedPaymentApps.ELE_PACKAGE]，原饿了么换牌）。两者订单详情页
 * 布局基本一致，但独立 App 默认不可见「下单时间」（折叠在「订单信息」里），
 * 幂等策略因此分两路：
 * - 淘宝内：「下单时间」（精确到毫秒、每单唯一）→ 账单 timestamp，同订单
 *   反复打开由 [BillIngestor] 内容去重（时间窗口锚定下单时间）幂等丢弃；
 * - 独立 App：页面打开瞬间拿不到下单时间，timestamp 回退为捕获时刻
 *   （真实支付后页面立即打开，now≈下单时间），幂等改由「订单号」已见集合
 *   保证（见 [extractOrderId] 与服务层的持久化去重）。
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

    /** 「订单号」后紧跟的长数字（me.ele 独立 App 订单页的默认可见区没有下单时间行） */
    private val ORDER_ID_LINE = Regex("""订单号[^\d]{0,4}(\d{12,})""")

    /**
     * 「闪购」节点拆分时商户候选词的黑名单：订单页底部的频道 Tab（如「首页」）
     * 也可能是独立的「闪购」文本节点，其后紧邻的 Tab 名不是商户。
     */
    private val NON_MERCHANT_WORDS = setOf(
        "首页", "消息", "购物车", "我的", "客服", "去下单", "查看订单", "回首页", "再下一单"
    )

    /**
     * 页面是否为淘宝闪购订单详情页。
     *
     * 两个入口的默认可见区不同：
     * - 淘宝内闪购频道（[SupportedPaymentApps.TAOBAO_PACKAGE]）：有「闪购」标 +
     *   「实付」+「下单时间」行；要求频道标是为了区分普通淘宝订单页；
     * - 独立淘宝闪购 App（[SupportedPaymentApps.ELE_PACKAGE]）：「下单时间」
     *   折叠在「订单信息」里，页面打开瞬间不可见——门槛改为「闪购」+「实付」+
     *   「订单号」。
     * 订单列表页两类标志都不全（无「实付」行），不会误触发。
     */
    fun isOrderPage(packageName: String, pageText: String): Boolean {
        val core = pageText.contains("闪购") && pageText.contains("实付")
        return when (packageName) {
            SupportedPaymentApps.ELE_PACKAGE -> core && containsOrderId(pageText)
            else -> core && pageText.contains("下单时间")
        }
    }

    /** 单条文本是否为订单号行（「订单号」+ 长数字），或独立的纯数字订单号节点 */
    fun containsOrderId(text: String): Boolean =
        ORDER_ID_LINE.containsMatchIn(text) || text.matches(Regex("""\d{12,}"""))

    /** 提取订单号（页面上的长数字串）；缺失返回 null */
    fun extractOrderId(nodes: List<String>): String? {
        nodes.forEach { line ->
            ORDER_ID_LINE.find(line)?.let { return it.groupValues[1] }
        }
        return nodes.firstOrNull { it.matches(Regex("""\d{12,}""")) }
    }

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
