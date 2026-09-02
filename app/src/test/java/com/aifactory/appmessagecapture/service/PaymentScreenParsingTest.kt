package com.aifactory.appmessagecapture.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 屏幕记账（无障碍）解析测试。
 *
 * 用例取自真实京东支付成功页截图（D:\AiExplore\京东付款截图.jpg）的窗口文本，
 * 页面上除支付金额外还有大量营销金额（领5元红包、满6减5、到手价等），
 * 重点验证金额提取不被污染。
 */
class PaymentScreenParsingTest {

    /** 京东支付成功页截图中的窗口文本节点（按屏幕顺序） */
    private val jdSuccessPageNodes = listOf(
        "6:09",
        "支付成功",                       // 标题（无金额，不应被当作金额行）
        "回首页",
        "京东支付¥30.38，共优惠¥0.02",    // 金额行
        "查看订单",
        "领5元红包",
        "下单抽抽乐",
        "恭喜你，抽中3项奖励",
        "满6减5",
        "满0.01减3.01",
        "超补好运券",
        "满15减10",
        "超级餐补券",
        "立即兑奖",
        "立即使用",
        "专属推荐",
        "京东好价",
        "¥3 到手价 销量100万+",
        "¥168 到手价 销量200万+",
        "香辣鸡腿堡"
    )

    @Test
    fun `京东支付成功页被识别且金额提取为30_38`() {
        val pageText = jdSuccessPageNodes.joinToString("\n")
        assertTrue(PaymentScreenParsing.isPaymentSuccessPage("com.jingdong.app.mall", pageText))

        val (line, amount) = PaymentScreenParsing.extractAmountLine(jdSuccessPageNodes)!!
        assertEquals("京东支付¥30.38，共优惠¥0.02", line)
        assertEquals(30.38, amount, 0.001)
    }

    @Test
    fun `非监视名单包名不识别为成功页`() {
        val pageText = jdSuccessPageNodes.joinToString("\n")
        assertFalse(PaymentScreenParsing.isPaymentSuccessPage("com.tencent.mm", pageText))
        assertFalse(PaymentScreenParsing.isPaymentSuccessPage("com.unionpay", pageText))
    }

    @Test
    fun `非成功页不识别`() {
        val pageText = jdSuccessPageNodes.joinToString("\n")
            // 去掉标题，模拟普通商品页
            .replace("支付成功\n", "")
        assertFalse(PaymentScreenParsing.isPaymentSuccessPage("com.jingdong.app.mall", pageText))

        // 退款成功页不是支付成功页（防止退款被记为支出）
        assertFalse(
            PaymentScreenParsing.isPaymentSuccessPage(
                "com.jingdong.app.mall", "退款成功\n退款¥30.38已原路退回"
            )
        )
        assertTrue(
            PaymentScreenParsing.isPaymentSuccessPage(
                "com.jingdong.app.mall", "付款成功\n京东支付¥12.00"
            )
        )
    }

    @Test
    fun `成功页但无支付动词金额行时返回null`() {
        // 只有标题没有金额行的成功页（金额自绘在图片里的极端情况）
        assertNull(PaymentScreenParsing.extractAmountLine(listOf("支付成功", "查看订单", "回首页")))
    }

    @Test
    fun `营销文案不触发动词金额提取`() {
        // 「支付」不在数字前（数字在别的词后面），不能提取
        assertNull(BillParsing.parseAmountAfterPaymentVerb("用5元红包支付"))
        assertNull(BillParsing.parseAmountAfterPaymentVerb("支付成功"))
        assertNull(BillParsing.parseAmountAfterPaymentVerb("支付时可用满15减10券"))
        // 动词后紧跟金额：正常提取（含千分位）
        assertEquals(30.38, BillParsing.parseAmountAfterPaymentVerb("京东支付¥30.38")!!, 0.001)
        assertEquals(12345.67, BillParsing.parseAmountAfterPaymentVerb("付款12,345.67元")!!, 0.001)
        assertEquals(88.0, BillParsing.parseAmountAfterPaymentVerb("已支付88")!!, 0.001)
    }

    @Test
    fun `金额行取第一个命中动词规则的节点`() {
        // 标题带金额的变体（部分版本是「支付成功¥30.38」整体一个节点）
        val (line, amount) = PaymentScreenParsing.extractAmountLine(
            listOf("支付成功¥30.38", "查看订单")
        )!!
        assertEquals("支付成功¥30.38", line)
        assertEquals(30.38, amount, 0.001)
    }

    @Test
    fun `监视名单与通知名单互不影响`() {
        // 京东 App 加入了屏幕监视名单，但其通知仍不应被通知通道捕获（营销推送噪音大）
        assertFalse(SupportedPaymentApps.isBillNotification("com.jingdong.app.mall", "京东", "任意内容"))
        assertTrue(SupportedPaymentApps.isScreenCaptureApp("com.jingdong.app.mall"))
        // 淘宝 App 在屏幕监视名单（淘宝闪购订单页），通知通道保持关闭
        assertFalse(SupportedPaymentApps.isBillNotification("com.taobao.taobao", "淘宝", "任意内容"))
        assertTrue(SupportedPaymentApps.isScreenCaptureApp("com.taobao.taobao"))
        // 未实测的应用不在监视名单（大众点评/京东金融/百度钱包已移除）
        assertFalse(SupportedPaymentApps.isScreenCaptureApp("com.jd.jrapp"))
        assertFalse(SupportedPaymentApps.isScreenCaptureApp("com.dianping.v1"))
        assertFalse(SupportedPaymentApps.isScreenCaptureApp("com.baidu.wallet"))
        assertFalse(SupportedPaymentApps.isScreenCaptureApp("com.tencent.mm"))
        // 其通知与通知门槛同样被移除（未实测）
        assertFalse(SupportedPaymentApps.isBillNotification("com.dianping.v1", "任意", "任意"))
        assertFalse(SupportedPaymentApps.isBillNotification("com.baidu.wallet", "任意", "任意"))
    }

    @Test
    fun `页面门槛路由_淘宝闪购走订单页门槛_京东走成功页门槛`() {
        // 淘宝闪购订单页：isCapturePage 命中订单页门槛
        val shangouPage = listOf(
            "闪购 袁记云饺(文汇路店)", "价格明细", "总优惠¥33 实付¥18.98",
            "下单时间 2026-08-30 18:16:14.118"
        ).joinToString("\n")
        assertTrue(
            PaymentScreenParsing.isCapturePage(SupportedPaymentApps.TAOBAO_PACKAGE, shangouPage)
        )
        // 淘宝闪购页不含「支付成功」，若误走京东门槛应不命中
        assertFalse(
            PaymentScreenParsing.isPaymentSuccessPage(SupportedPaymentApps.TAOBAO_PACKAGE, shangouPage)
        )
        // 缺「下单时间」（订单列表页形态）不命中
        val listPage = "闪购\n袁记云饺(文汇路店)\n实付¥18.98"
        assertFalse(PaymentScreenParsing.isCapturePage(SupportedPaymentApps.TAOBAO_PACKAGE, listPage))

        // 独立 App（me.ele）：同一门槛——有「下单时间」才命中
        val elePage = listOf(
            "闪购 袁记云饺(文汇路店)", "总优惠¥33 实付¥18.98",
            "订单号 8023786204058882481", "下单时间 2026-08-30 18:16:14.118"
        ).joinToString("\n")
        assertTrue(PaymentScreenParsing.isCapturePage(SupportedPaymentApps.ELE_PACKAGE, elePage))
        // 历史订单形态（下单时间折叠）：不命中
        val eleHistory = "闪购 袁记云饺(文汇路店)\n总优惠¥33 实付¥18.98\n订单号 8023786204058882481"
        assertFalse(PaymentScreenParsing.isCapturePage(SupportedPaymentApps.ELE_PACKAGE, eleHistory))

        // 京东成功页：isCapturePage 保持原行为
        assertTrue(
            PaymentScreenParsing.isCapturePage("com.jingdong.app.mall", "支付成功\n京东支付¥30.38")
        )
        assertFalse(
            PaymentScreenParsing.isCapturePage("com.jingdong.app.mall", "商品详情\n¥59.0")
        )
    }
}
