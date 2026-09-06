package com.aifactory.appmessagecapture.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 商户记忆推断纯函数测试：多数投票、跨商户指纹归一、渠道→平台匹配。
 */
class MerchantMemoryTest {

    // ── 多数投票 ──────────────────────────────────────────────────────────

    @Test
    fun `分类取众数_平台取最近非空`() {
        val samples = listOf(
            MerchantMemory.MemorySample("餐饮美食", null),      // 最近
            MerchantMemory.MemorySample("餐饮美食", 7L),
            MerchantMemory.MemorySample("购物消费", null),
            MerchantMemory.MemorySample("餐饮美食", null),
            MerchantMemory.MemorySample("购物消费", null)
        )
        val vote = MerchantMemory.vote(samples)
        assertEquals("餐饮美食", vote.category)   // 3/5 众数
        assertEquals(7L, vote.platformId)        // 最近非空平台粘住
    }

    @Test
    fun `平票时取更近的分类`() {
        val samples = listOf(
            MerchantMemory.MemorySample("交通出行", null),  // 最近
            MerchantMemory.MemorySample("餐饮美食", null),
            MerchantMemory.MemorySample("交通出行", null),
            MerchantMemory.MemorySample("餐饮美食", null)
        )
        assertEquals("交通出行", MerchantMemory.vote(samples).category)
    }

    @Test
    fun `全部无平台时平台记忆为空`() {
        val samples = (1..4).map { MerchantMemory.MemorySample("餐饮美食", null) }
        val vote = MerchantMemory.vote(samples)
        assertEquals("餐饮美食", vote.category)
        assertNull(vote.platformId)
    }

    @Test
    fun `空样本返回空投票`() {
        val vote = MerchantMemory.vote(emptyList())
        assertNull(vote.category)
        assertNull(vote.platformId)
    }

    // ── 跨商户指纹 ────────────────────────────────────────────────────────

    @Test
    fun `闪购订单标题归一出去渠道前缀与金额尾巴`() {
        assertEquals(
            "袁记云饺文汇路店",
            MerchantMemory.core("淘宝闪购", "闪购 袁记云饺(文汇路店) 实付¥18.98")
        )
    }

    @Test
    fun `拼多多订单标题归一`() {
        assertEquals("羊羊羊大叔", MerchantMemory.core("拼多多", "羊羊羊大叔 实付¥3.84"))
    }

    @Test
    fun `店铺后缀剥离_同一商户跨入口对齐`() {
        // 淘宝全称 vs 美团短名 → 同一指纹
        assertEquals(
            MerchantMemory.core("淘宝", "丽邦家居生活 实付¥13.62"),
            MerchantMemory.core("拼多多", "丽邦家居生活官方旗舰店 实付¥13.62")
        )
        assertEquals("丽邦家居生活", MerchantMemory.core("拼多多", "丽邦家居生活官方旗舰店 实付¥13.62"))
    }

    @Test
    fun `叠名后缀循环剥离`() {
        assertEquals("某某家纺", MerchantMemory.core("拼多多", "某某家纺官方旗舰店官方旗舰 实付¥9.90"))
    }

    @Test
    fun `从已存储商户键还原指纹`() {
        // 商户键本身含「实付」尾巴（capture 标题构造所致），需截断
        assertEquals("羊羊羊大叔", MerchantMemory.coreFromKey("拼多多|羊羊羊大叔实付"))
        assertEquals("袁记云饺文汇路店", MerchantMemory.coreFromKey("淘宝闪购|袁记云饺文汇路店实付"))
    }

    @Test
    fun `无有效商户身份时返回null`() {
        assertNull(MerchantMemory.core("微信", "微信支付"))
        assertNull(MerchantMemory.core("拼多多", "实付¥3.84"))
        assertNull(MerchantMemory.coreFromKey("微信|微信支付"))
    }

    // ── 渠道 → 平台匹配 ──────────────────────────────────────────────────

    @Test
    fun `渠道包名匹配已绑定平台`() {
        val platforms = listOf(
            1L to "com.tencent.mm",
            2L to "com.eg.android.AlipayGphone",
            3L to null
        )
        assertEquals(1L, MerchantMemory.matchPlatform("com.tencent.mm", platforms))
        assertEquals(2L, MerchantMemory.matchPlatform("com.eg.android.AlipayGphone", platforms))
        assertNull(MerchantMemory.matchPlatform("com.unionpay", platforms))
        assertNull(MerchantMemory.matchPlatform(null, platforms))
    }
}
