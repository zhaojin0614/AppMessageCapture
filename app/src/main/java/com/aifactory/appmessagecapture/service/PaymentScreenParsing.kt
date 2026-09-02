package com.aifactory.appmessagecapture.service

/**
 * 支付成功页识别与金额提取（纯函数，无 Android 依赖，可单元测试）。
 *
 * 无障碍服务把窗口内的文本节点收集为列表后交给这里判断：
 * 1. 页面是否为支付成功页 —— 包名在监视名单内 + 页面含「支付成功/付款成功」；
 * 2. 从节点列表中提取支付金额行 —— 只认「支付/付款」动词后紧跟的数字，
 *    排除「领5元红包」「满6减5」「¥3 到手价」等营销金额干扰。
 */
object PaymentScreenParsing {

    /** 支付成功页的页面级标志文本（京东：标题「支付成功」） */
    private const val SUCCESS_TITLE_KEYWORDS = "支付成功|付款成功"

    /** 单条文本是否命中成功页门槛词（采集期早退 + 页面级判断共用） */
    fun isSuccessText(text: String): Boolean =
        SUCCESS_TITLE_KEYWORDS.split("|").any { text.contains(it) }

    fun isPaymentSuccessPage(packageName: String, pageText: String): Boolean {
        if (!SupportedPaymentApps.isScreenCaptureApp(packageName)) return false
        return isSuccessText(pageText)
    }

    /**
     * 屏幕捕获的页面级门槛唯一入口：按包名路由到各自的页面判定。
     * 京东走「支付成功」页门槛；淘宝闪购走订单详情页门槛
     * （[TaobaoShangouParsing.isOrderPage]，页面持久存在、无「支付成功」字样）。
     */
    fun isCapturePage(packageName: String, pageText: String): Boolean =
        when (packageName) {
            SupportedPaymentApps.TAOBAO_PACKAGE -> TaobaoShangouParsing.isOrderPage(pageText)
            else -> isPaymentSuccessPage(packageName, pageText)
        }

    /**
     * 从窗口文本节点中找出支付金额行。
     * 返回 (该行原文, 金额)；找不到返回 null。
     *
     * 只用「支付动词后紧跟金额」这一条规则（[BillParsing.parseAmountAfterPaymentVerb]）：
     * 「支付成功」标题节点无金额自然跳过，「共优惠¥0.02」「¥168 到手价」这类
     * 节点不含支付动词也不会命中，从根上避免误抓营销数字。
     */
    fun extractAmountLine(nodes: List<String>): Pair<String, Double>? =
        nodes.firstNotNullOfOrNull { line ->
            BillParsing.parseAmountAfterPaymentVerb(line)?.let { line to it }
        }
}
