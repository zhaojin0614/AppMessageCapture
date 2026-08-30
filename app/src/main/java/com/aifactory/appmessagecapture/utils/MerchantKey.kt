package com.aifactory.appmessagecapture.utils

/**
 * 商户记忆键：来源 App + 标题去掉数字/金额符号/空白后的「商户指纹」。
 *
 * 例：「京东支付¥14.58」 → `京东|京东支付`。同一商户键最近一笔账单的
 * 分类与平台即为"记忆"——新账单自动套用，用户在完善账单里的纠正
 * （分类/平台编辑会回填键值）会成为之后的记忆。
 */
object MerchantKey {

    fun of(appName: String, title: String): String =
        "${appName.trim()}|${normalize(title)}"

    /** 去掉数字、金额符号、空白与常见标点，仅保留文字部分 */
    fun normalize(title: String): String {
        val sb = StringBuilder(title.length)
        title.forEach { ch ->
            when {
                ch.isDigit() -> {}
                ch in "¥￥.,-+*#:/ \t，。：·！!？?()（）" -> {}
                else -> sb.append(ch)
            }
        }
        val text = sb.toString().trim()
        // 全部是数字/符号（如纯金额标题）时退回原标题，避免不同账单撞键
        return text.ifEmpty { title.trim() }
    }
}
