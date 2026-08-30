package com.aifactory.appmessagecapture.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantKeyTest {

    @Test
    fun `normalize 去掉数字与金额符号`() {
        assertEquals("京东支付", MerchantKey.normalize("京东支付¥14.58"))
        assertEquals("美团订单", MerchantKey.normalize("美团订单-123456"))
        assertEquals("滴滴出行", MerchantKey.normalize("滴滴出行 *#23.5"))
    }

    @Test
    fun `同商户不同金额共享商户键`() {
        assertEquals(
            MerchantKey.of("京东", "京东支付¥14.58"),
            MerchantKey.of("京东", "京东支付¥30.38")
        )
    }

    @Test
    fun `不同来源App不共享商户键`() {
        org.junit.Assert.assertNotEquals(
            MerchantKey.of("京东", "支付成功"),
            MerchantKey.of("微信", "支付成功")
        )
    }

    @Test
    fun `纯数字标题退回原标题避免撞键`() {
        assertEquals("123456", MerchantKey.normalize("123456"))
        // 全部由符号+数字构成：无法提取文字，回退原标题（含符号）保持可区分
        assertEquals("¥88.88", MerchantKey.normalize("¥88.88"))
    }
}
