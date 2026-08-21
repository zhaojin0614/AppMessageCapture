package com.aifactory.appmessagecapture.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 端到端账单捕获测试：用真实通知文案走一遍 tryExtractBill 的纯逻辑链路
 * （应用门槛 → 支付关键词 → 金额提取 → 收支方向），全部调用生产函数，
 * 验证给定通知能被自动捕获为账单。
 */
class SupportedPaymentAppsTest {

    /**
     * 云闪付付款通知：门槛要求标题含「支付助手」、正文含「消费」。
     * 预期：通过门槛，识别为支出账单，金额 18.80。
     */
    @Test
    fun `unionpay payment assistant notification is captured as expense bill`() {
        val packageName = "com.unionpay"
        val title = "支付助手：付款成功"
        val content = "您尾号为5580的银行卡于19日18时38分消费18.80元。"
        val fullText = "$title $content"

        assertTrue(
            "Should pass the UnionPay app gate",
            SupportedPaymentApps.isBillNotification(packageName, title, content)
        )
        assertTrue(
            "Should contain a payment keyword",
            BillParsing.hasPaymentKeyword(fullText)
        )
        assertEquals(
            "Card tail number 5580 / time digits must not be picked as the amount",
            18.80,
            BillParsing.parseAmount(fullText)!!,
            0.001
        )
        assertFalse(
            "Consumption is an expense, not income",
            BillParsing.isIncome(content)
        )

        // Gate rejects notifications missing the required title/content markers
        assertFalse(SupportedPaymentApps.isBillNotification(packageName, "云闪付", content))
        assertFalse(
            SupportedPaymentApps.isBillNotification(packageName, title, "您有一笔新的优惠到账")
        )
    }

    /**
     * 中国农业银行扣款通知：门槛要求正文含「支出」。
     * 预期：通过门槛，识别为支出账单，金额 17.66。
     */
    @Test
    fun `abc bank expense notification is captured as expense bill`() {
        val packageName = "com.android.bankabc"
        val title = "中国农业银行"
        val content = "您尾号为7374的农行借记卡于08月13日18:45发生一笔支出17.66元，详情请点击。"
        val fullText = "$title $content"

        assertTrue(
            "Should pass the ABC app gate",
            SupportedPaymentApps.isBillNotification(packageName, title, content)
        )
        assertTrue(
            "Should contain a payment keyword",
            BillParsing.hasPaymentKeyword(fullText)
        )
        assertEquals(
            "Card tail number 7374 / date digits must not be picked as the amount",
            17.66,
            BillParsing.parseAmount(fullText)!!,
            0.001
        )
        assertFalse(
            "支出 is an expense, not income",
            BillParsing.isIncome(content)
        )

        // Gate rejects notifications whose content lacks the expense marker
        assertFalse(
            SupportedPaymentApps.isBillNotification(
                packageName, title, "农行邀您领取专属消费红包，详情请点击。"
            )
        )
    }

    /**
     * 抖省省团购订单通知：门槛要求标题含「支付成功」，金额出现在标题中。
     * 预期：通过门槛，识别为支出账单，金额取自标题的 11.9。
     */
    @Test
    fun `doushengsheng order notification captures amount from title`() {
        val packageName = "com.ss.android.ugc.lifeservices"
        val title = "支付成功11.9元"
        // 注意内容里「成 功」中间夹了空格，不能依赖内容判断支付状态
        val content = "【大大大】霸王炸鸡卷了件套订单已支付成 功，点击查看详情>"
        val fullText = "$title $content"

        assertTrue(
            "Should pass the Doushengsheng app gate",
            SupportedPaymentApps.isBillNotification(packageName, title, content)
        )
        assertTrue(
            "Should contain a payment keyword",
            BillParsing.hasPaymentKeyword(fullText)
        )
        assertEquals(
            "Amount lives in the title, not the content",
            11.9,
            BillParsing.parseAmount(fullText)!!,
            0.001
        )
        assertFalse(
            "Paid order is an expense, not income",
            BillParsing.isIncome(content)
        )

        // Gate rejects titles without the 支付成功 marker (marketing pushes etc.)
        assertFalse(
            SupportedPaymentApps.isBillNotification(
                packageName, "您有一张优惠券待使用", content
            )
        )
    }
}
