package com.aifactory.appmessagecapture.service

import com.aifactory.appmessagecapture.service.CategoryPrior.ExpenseSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * 消费习惯统计先验测试：渠道/时段/金额档信号计分与置信度门槛。
 */
class CategoryPriorTest {

    private fun at(hour: Int, minute: Int = 0): Long =
        LocalDate.now().atTime(LocalTime.of(hour, minute))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun sample(
        category: String,
        pkg: String = "com.tencent.mm",
        hour: Int = 8,
        amount: Double = 10.0
    ) = ExpenseSample(category, pkg, at(hour), amount)

    @Test
    fun `时段与金额档划分`() {
        assertEquals(0, CategoryPrior.hourBucket(5))
        assertEquals(1, CategoryPrior.hourBucket(10))
        assertEquals(3, CategoryPrior.hourBucket(19))
        assertEquals(4, CategoryPrior.hourBucket(23))
        assertEquals(4, CategoryPrior.hourBucket(2))
        assertEquals(0, CategoryPrior.amountBucket(14.99))
        assertEquals(1, CategoryPrior.amountBucket(15.0))
        assertEquals(2, CategoryPrior.amountBucket(199.99))
        assertEquals(3, CategoryPrior.amountBucket(200.0))
    }

    @Test
    fun `微信早间小额高频消费推断为餐饮`() {
        val samples = (1..6).map { sample("餐饮美食", hour = 8, amount = 10.0) } +
            listOf(
                sample("购物消费", pkg = "com.eg.android.AlipayGphone", hour = 20, amount = 150.0),
                sample("购物消费", pkg = "com.eg.android.AlipayGphone", hour = 20, amount = 180.0)
            )
        // 微信 + 早间 + 小额 → 餐饮美食（同渠道+2 同时段+1 同档+1 = 4 分）
        assertEquals(
            "餐饮美食",
            CategoryPrior.infer(samples, "com.tencent.mm", at(8, 30), 12.0)
        )
    }

    @Test
    fun `样本不足时不推断`() {
        val samples = listOf(sample("餐饮美食"), sample("餐饮美食"))
        assertNull(CategoryPrior.infer(samples, "com.tencent.mm", at(8, 30), 12.0))
    }

    @Test
    fun `得分不够时不推断`() {
        // 样本足够多但信号分散：渠道/时段/金额档都对不上
        val samples = (1..6).map { sample("餐饮美食", pkg = "com.eg.android.AlipayGphone", hour = 20, amount = 300.0) }
        assertNull(CategoryPrior.infer(samples, "com.tencent.mm", at(8, 30), 12.0))
    }

    @Test
    fun `历史出现次数不足时不推断`() {
        // 高分但胜出类目历史只出现 2 次（< MIN_SUPPORT），两笔巧合不定终身
        val samples = listOf(
            sample("医疗健康", hour = 8, amount = 10.0),
            sample("医疗健康", hour = 8, amount = 10.0),
            sample("餐饮美食", hour = 8, amount = 10.0),
            sample("餐饮美食", hour = 8, amount = 10.0),
            sample("餐饮美食", hour = 8, amount = 10.0),
            sample("餐饮美食", hour = 8, amount = 10.0)
        )
        // 医疗 2 笔与餐饮 4 笔同信号同分（各 4 分），平分按类目名排序；
        // 无论谁胜出，医疗支持度不足、餐饮支持度足够 → 应稳定落在餐饮
        assertEquals("餐饮美食", CategoryPrior.infer(samples, "com.tencent.mm", at(8, 30), 12.0))
    }
}
