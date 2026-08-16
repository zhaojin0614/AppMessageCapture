package com.aifactory.appmessagecapture.service

/**
 * 账单解析纯函数集合（无 Android 依赖，可单元测试）。
 *
 * 从 [MessageCaptureService] 抽出，修复了三个历史缺陷：
 * 1. 收支方向误判：微信支付类通知标题恒为「微信支付」，拼接标题后关键词匹配
 *    导致收款/退款/红包全部被记为支出 → 现在方向判断只看正文内容。
 * 2. 千分位金额：`¥1,299.00` 旧正则只解析出 1.00 → 现支持千分位分隔符。
 * 3. 兜底金额误提取：`满300.00减30`/`余额5000.00` 会提取出无关数字 →
 *    兜底正则改为仅在支付动词后紧跟数字时生效。
 */
object BillParsing {

    /** 金额数字：整数部分支持千分位（1,299），小数最多两位 */
    private const val AMOUNT_NUMBER = """(\d{1,3}(?:,\d{3})+|\d+)(?:\.(\d{1,2}))?"""

    private val AMOUNT_WITH_SYMBOL = Regex("""[¥￥]\s*$AMOUNT_NUMBER""")
    private val AMOUNT_WITH_UNIT = Regex("""$AMOUNT_NUMBER\s*[元円]""")
    // 兜底：仅在支付动词后紧跟的数字才算金额，避免营销文案/余额数字被误提取
    private val AMOUNT_AFTER_VERB =
        Regex("""(?:支付|付款|消费|扣款|已付|收款|退款|入账|到账|收入)\s*[¥￥]?\s*$AMOUNT_NUMBER""")

    val EXPENSE_KEYWORDS = listOf("付款", "支付", "消费", "支出", "扣款", "已付", "交易", "订单已支付")

    /** 「收款」需排除「收款方」（付款通知中的商户字段） */
    private val INCOME_KEYWORD_REGEXES = listOf(
        Regex("收款(?!方)"), Regex("入账"), Regex("到账"), Regex("转入"), Regex("存入"),
        Regex("退款"), Regex("收益"), Regex("工资"), Regex("红包"), Regex("收入")
    )

    /** 明确的支出信号：出现任一即优先判为支出（如发出红包、转账给他人） */
    private val EXPENSE_OVERRIDE_KEYWORDS = listOf("发出红包", "已发出", "转账给", "付款给", "已转给")

    /**
     * 从支付通知文本中提取金额。优先级：货币符号 > 元/円后缀 > 支付动词后数字。
     * 找不到可信金额时返回 null。
     */
    fun parseAmount(text: String): Double? {
        AMOUNT_WITH_SYMBOL.find(text)?.let { return it.groupValues.toAmount() }
        AMOUNT_WITH_UNIT.find(text)?.let { return it.groupValues.toAmount() }
        AMOUNT_AFTER_VERB.find(text)?.let { return it.groupValues.toAmount() }
        return null
    }

    private fun List<String>.toAmount(): Double? {
        val integerPart = this[1].replace(",", "")
        if (integerPart.isEmpty()) return null
        val fraction = this[2]
        val value = if (fraction.isEmpty()) {
            integerPart.toDoubleOrNull()
        } else {
            "$integerPart.$fraction".toDoubleOrNull()
        }
        return value?.takeIf { it > 0 }
    }

    /** 是否包含任意支付相关关键词（标题或正文均可，仅作相关性门槛） */
    fun hasPaymentKeyword(text: String): Boolean =
        EXPENSE_KEYWORDS.any { text.contains(it) } ||
            INCOME_KEYWORD_REGEXES.any { it.containsMatchIn(text) }

    /**
     * 判断收支方向。只根据[content]正文判断（不含标题），
     * 因为「微信支付」类标题恒含「支付」字样会污染关键词匹配。
     *
     * 收入关键词优先：真实收款通知（如「微信支付收款9.90元」）正文会同时含
     * 「支付」与「收款」，只有收入优先才能得到正确方向；支出覆写词
     * （如「发出红包」「转账给」）再优先于收入词，覆盖明确的转出场景。
     */
    fun isIncome(content: String): Boolean {
        if (EXPENSE_OVERRIDE_KEYWORDS.any { content.contains(it) }) return false
        return INCOME_KEYWORD_REGEXES.any { it.containsMatchIn(content) }
    }

    /**
     * 同 App 时间近似去重的辅助判断：两条通知标题是否同源。
     * 要求共享长度 ≥4 的公共前缀（如「微信支付」与「微信支付-新」），
     * 避免把同 App 同金额但明显是两笔不同消费的通知误判为重复。
     */
    fun isSameOriginTitle(titleA: String, titleB: String): Boolean {
        if (titleA == titleB) return true
        val shorter = minOf(titleA.length, titleB.length)
        var common = 0
        while (common < shorter && titleA[common] == titleB[common]) common++
        return common >= 4
    }
}
