package com.aifactory.appmessagecapture.service

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 拼多多订单详情页解析（纯函数，无 Android 依赖，可单元测试）。
 *
 * 捕获目标是支付完成后自动弹出/用户打开的订单详情页
 * （[SupportedPaymentApps.PDD_PACKAGE]）。页面无「拼多多」字样也无
 * 「支付成功」标题——来源由无障碍事件的包名保证（同京东），
 * 页面门槛为「实付」+「订单编号」两个标志词：
 * - 订单列表页/商品详情页/收银台无「订单编号」骨架元素；
 * - 「商品快照」标签在真实无障碍树中不出现（仅剩描述句），不能作为门槛。
 *
 * 真实节点样例（2026-09 真机 dump，见 PddParsingTest）：
 * - 金额行是「实付:,13.62元,(免运费)」——冒号后跟逗号、金额带元后缀、
 *   货币符 ¥ 是绘制的图形不进文本；兼容「实付: ¥13.62 (免运费)」变体；
 * - 时间：顶部「订单确认，已通知商家配货 2026-09-03 17:11:19」横幅（部分
 *   订单状态无横幅）；
 * - 订单编号「260903-034141652481469」前六位即下单日期 yyMMdd，无横幅时
 *   用它把账单锚定到下单日中午（幂等去重的稳定时间锚点）。
 */
object PddParsing {

    /** 金额数字语法与 [BillParsing] 一致：整数支持千分位，小数最多两位 */
    private const val AMOUNT_NUMBER = """(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?"""

    /**
     * 「实付」后紧跟的货币金额。真实页面两种形态：
     * 「实付:,13.62元,(免运费)」（冒号+逗号+元后缀，无货币符）与
     * 「实付: ¥13.62 (免运费)」（空格+货币符）。分隔符兼容冒号/逗号/空白。
     */
    private val PAID_AMOUNT = Regex("""实付[:：]?[、,，\s]*[¥￥]?\s*$AMOUNT_NUMBER""")

    /** 订单编号：260903-034141652481469（前段为 yyMMdd 下单日期，后段为序列号） */
    private val ORDER_ID = Regex("""(\d{2})(\d{2})(\d{2})-(\d{10,})""")

    /** 「订单确认」横幅时间：2026-09-03 17:11:19 */
    private val CONFIRM_DATETIME =
        Regex("""(\d{4})-(\d{2})-(\d{2})[ ](\d{2}):(\d{2}):(\d{2})""")

    /**
     * 店铺标签/促销行标志词。商户名节点是其前一个文本节点：
     * - 旗舰店布局：「丽邦家居生活官方旗舰店」「官方旗舰」「回头客好店」；
     * - 个人店布局：「羊羊羊大叔」「今日已拼3.84元,再拼8.16元可返1元」。
     * 店铺名自带「官方旗舰店」字样时节点本身就是商户（标志词在名称尾部）。
     */
    private val MERCHANT_MARKER = Regex("""官方旗舰|官方直营|回头客好店|今日已拼|多人团""")

    /** 商户候选黑名单（地址块尾/按钮/操作行等非商户节点） */
    private val NON_MERCHANT_WORDS = setOf(
        "修改", "复制", "分享商品", "联系商家", "申请退款", "查看更多订单信息", "继续拼单"
    )

    /**
     * 页面是否为拼多多订单详情页（含支付完成页与其后的物流/待取件视图，
     * 同一订单族）。「实付」是金额提取前提；「订单编号」区分订单列表页、
     * 商品详情页与收银台（均无该骨架元素）。
     */
    fun isOrderPage(pageText: String): Boolean =
        pageText.contains("实付") && pageText.contains("订单编号")

    /** 从单行文本提取「实付」金额；无「实付」前缀的货币数字（广告价/商品价）返回 null */
    fun parsePaidAmount(line: String): Double? =
        PAID_AMOUNT.find(line)?.toAmount()

    /**
     * 节点列表中提取实付金额：先逐节点找；「实付:」与「¥13.62」被拆成
     * 相邻节点时退化为整页拼接文本（实付标签全页唯一，拼接不引入误匹配）。
     */
    fun extractPaidAmount(nodes: List<String>): Double? =
        nodes.firstNotNullOfOrNull { parsePaidAmount(it) }
            ?: PAID_AMOUNT.find(nodes.joinToString(" "))?.toAmount()

    /** 页面上的订单编号（每单唯一；被拆节点时同样用拼接兜底） */
    fun extractOrderId(nodes: List<String>): String? =
        nodes.firstNotNullOfOrNull { ORDER_ID.find(it)?.value }
            ?: ORDER_ID.find(nodes.joinToString(" "))?.value

    /**
     * 从订单编号前六位（yyMMdd）解析下单日期，取当日 12:00 为账单时间锚点。
     * 用于无「订单确认」横幅的订单状态——同一订单无论何时重开，锚点稳定，
     * 内容去重可幂等丢弃。日期非法（如月/日越界）返回 null。
     */
    fun parseOrderDateMillis(orderId: String, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val m = ORDER_ID.find(orderId) ?: return null
        val (yy, mm, dd) = Triple(m.groupValues[1], m.groupValues[2], m.groupValues[3])
        val date = runCatching {
            LocalDate.of(2000 + yy.toInt(), mm.toInt(), dd.toInt())
        }.getOrNull() ?: return null
        return date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * 商户名。只在与「订单编号」行之前的区域查找——底部广告区也含
     * 「先用后付」「官方旗舰店」等标志词，不能混入。
     * 逐个标志词节点尝试：标志词前有正文字样 → 节点本身就是店铺名
     * （如「丽邦家居生活官方旗舰店」）；标志词独立成节点（如「官方旗舰」）
     * → 取其前一个候选节点（黑名单过滤）。
     */
    fun extractMerchant(nodes: List<String>): String? {
        val limit = nodes.indexOfFirst { it.contains("订单编号") }
        if (limit <= 0) return null
        for (idx in 0 until limit) {
            val marker = MERCHANT_MARKER.find(nodes[idx]) ?: continue
            val prefix = nodes[idx].substring(0, marker.range.first).trim()
            if (prefix.length >= 2) {
                cleanupMerchant(nodes[idx])?.let { return it }
            } else if (idx > 0) {
                val candidate = nodes[idx - 1].trim()
                if (candidate.length in 2..30 && candidate !in NON_MERCHANT_WORDS &&
                    !candidate.contains('¥') && !candidate.contains('￥') &&
                    !candidate.contains('×')
                ) {
                    cleanupMerchant(candidate)?.let { return it }
                }
            }
        }
        return null
    }

    /**
     * 解析「订单确认」横幅时间（yyyy-MM-dd HH:mm:ss）为 epoch millis。
     * 页面上唯一的全时间戳就是该横幅（地址里的门牌号/货号不含完整日期时间
     * 格式），逐节点找第一个命中即可；横幅不展示（如「预计拼单成功后发货」
     * 状态）时返回 null。
     */
    fun parseConfirmTimeMillis(nodes: List<String>, zone: ZoneId = ZoneId.systemDefault()): Long? {
        nodes.forEach { line ->
            CONFIRM_DATETIME.find(line)?.let { m ->
                return LocalDateTime.of(
                    m.groupValues[1].toInt(),
                    m.groupValues[2].toInt(),
                    m.groupValues[3].toInt(),
                    m.groupValues[4].toInt(),
                    m.groupValues[5].toInt(),
                    m.groupValues[6].toInt()
                ).atZone(zone).toInstant().toEpochMilli()
            }
        }
        return null
    }

    /** 去掉商户名尾部的箭头/店铺标签装饰与空白；清空后视为无商户 */
    private fun cleanupMerchant(raw: String): String? =
        raw.trim()
            .trimEnd('>', '›', '｜', '|', ' ')
            .replace(Regex("""(官方旗舰|官方直营|回头客好店|多人团)+\s*$"""), "")
            .trim()
            .ifEmpty { null }

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
