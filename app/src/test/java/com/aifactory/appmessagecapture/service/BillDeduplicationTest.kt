package com.aifactory.appmessagecapture.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for bill parsing & deduplication logic.
 *
 * Amount parsing and direction classification now call the production
 * [BillParsing] object directly (no mirrored regex drift).
 *
 * The dedup decision flow still simulates the Room queries with an in-memory
 * list mirroring the three DAO lookups used by
 * [MessageCaptureService.tryExtractBill]:
 *   - findRecentByAmountPackageAndTitle  (content-based dedup)
 *   - findRecentByAmountAndPackage        (same-app time-proximity dedup)
 *   - findRecentByAmount                  (cross-app merge)
 */
class BillDeduplicationTest {

    // ── Simulated bill record ──────────────────────────────────────────────
    data class FakeBill(
        val id: Long,
        val amount: Double,
        val packageName: String,
        val title: String,
        val isIncome: Boolean,
        val timestamp: Long
    )

    // In-memory "database"
    private val bills = mutableListOf<FakeBill>()
    private var nextId = 1L

    // ── Mirrors the three DAO queries ─────────────────────────────────────

    private fun findRecentByAmountPackageAndTitle(
        amount: Double, packageName: String, title: String, since: Long
    ): FakeBill? =
        bills.filter { it.amount == amount && it.packageName == packageName &&
                it.title == title && it.timestamp >= since }
            .maxByOrNull { it.timestamp }

    private fun findRecentByAmountAndPackage(
        amount: Double, packageName: String, since: Long
    ): FakeBill? =
        bills.filter { it.amount == amount && it.packageName == packageName &&
                it.timestamp >= since }
            .maxByOrNull { it.timestamp }

    private fun findRecentByAmount(
        amount: Double, since: Long
    ): FakeBill? =
        bills.filter { it.amount == amount && it.timestamp >= since }
            .maxByOrNull { it.timestamp }

    // ── Mirrors the dedup decision tree from tryExtractBill ──────────────────

    /**
     * Returns the action taken: "skipped" (dedup), "replaced" (cross-app merge),
     * or "inserted" (new bill).
     */
    private fun processBill(
        amount: Double,
        packageName: String,
        title: String,
        isIncome: Boolean,
        notificationTime: Long
    ): String {
        // 1a. Content-based dedup (60s, same direction)
        val contentSince = notificationTime - 60_000
        val contentDup = findRecentByAmountPackageAndTitle(amount, packageName, title, contentSince)
        if (contentDup != null && contentDup.isIncome == isIncome) {
            return "skipped"
        }

        // 1b. Same-app time-proximity dedup (60s): requires same direction
        //     AND same-origin titles so distinct purchases are kept.
        val timeSince = notificationTime - 60_000
        val sameApp = findRecentByAmountAndPackage(amount, packageName, timeSince)
        if (sameApp != null && sameApp.isIncome == isIncome &&
            BillParsing.isSameOriginTitle(title, sameApp.title)
        ) {
            return "skipped"
        }

        // 2. Cross-app merge (60s): only when direction matches
        val crossSince = notificationTime - 60_000
        val existing = findRecentByAmount(amount, crossSince)
        if (existing != null && existing.packageName != packageName &&
            existing.isIncome == isIncome
        ) {
            val existingWeight = getAppWeight(existing.packageName)
            val currentWeight = getAppWeight(packageName)
            if (currentWeight > existingWeight) {
                // Replace: update existing bill's metadata
                val idx = bills.indexOf(existing)
                bills[idx] = existing.copy(
                    packageName = packageName,
                    title = title
                )
                return "replaced"
            }
            return "skipped" // existing has equal or higher weight
        }

        // 3. Insert new bill
        bills.add(FakeBill(nextId++, amount, packageName, title, isIncome, notificationTime))
        return "inserted"
    }

    // Delegates to production weights — no mirrored table drift.
    private fun getAppWeight(packageName: String): Int =
        SupportedPaymentApps.appWeight(packageName)

    @Before
    fun setUp() {
        bills.clear()
        nextId = 1L
    }

    // ── Amount extraction tests (production BillParsing.parseAmount) ──────

    @Test
    fun `extract amount with yen sign`() {
        assertEquals(14.40, BillParsing.parseAmount("¥14.40")!!, 0.001)
    }

    @Test
    fun `extract amount with yuan suffix`() {
        assertEquals(20.00, BillParsing.parseAmount("您已成功付款 20.00 元")!!, 0.001)
    }

    @Test
    fun `extract amount from wechat notification`() {
        assertEquals(9.90, BillParsing.parseAmount("微信支付 收款到账9.90元")!!, 0.001)
    }

    @Test
    fun `extract amount with full-width yen sign`() {
        assertEquals(99.99, BillParsing.parseAmount("￥99.99")!!, 0.001)
    }

    @Test
    fun `extract amount returns null for no amount`() {
        assertNull(BillParsing.parseAmount("这是一条普通消息"))
    }

    @Test
    fun `extract amount with thousands separator`() {
        assertEquals(1299.00, BillParsing.parseAmount("支付成功 ¥1,299.00")!!, 0.001)
        assertEquals(1299.00, BillParsing.parseAmount("消费1,299.00元")!!, 0.001)
    }

    @Test
    fun `marketing text without payment verb is not extracted`() {
        assertNull(BillParsing.parseAmount("满300.00减30，进店立享优惠"))
    }

    @Test
    fun `verb-prefixed amount without symbol is extracted`() {
        assertEquals(14.4, BillParsing.parseAmount("已支付14.4")!!, 0.001)
    }

    // ── Direction classification tests (production BillParsing.isIncome) ──

    @Test
    fun `wechat income notification is classified as income`() {
        // Regression: title「微信支付」+ content「收款」used to be recorded as expense
        assertTrue(BillParsing.isIncome("微信支付收款9.90元，朋友到店消费"))
    }

    @Test
    fun `refund is income`() {
        assertTrue(BillParsing.isIncome("已退款14.40元，退款将原路退回"))
    }

    @Test
    fun `red packet received is income`() {
        assertTrue(BillParsing.isIncome("收到红包，金额8.88元"))
    }

    @Test
    fun `alipay income text is income`() {
        assertTrue(BillParsing.isIncome("您有一笔收入，金额15.00元"))
    }

    @Test
    fun `payment with merchant field is expense not income`() {
        // "收款方" (payee field) must not trigger the "收款" income keyword
        assertFalse(BillParsing.isIncome("支付成功，收款方：美团外卖，金额¥25.00"))
    }

    @Test
    fun `sent red packet is expense`() {
        assertFalse(BillParsing.isIncome("已发出红包，金额8.88元"))
    }

    @Test
    fun `transfer out is expense`() {
        assertFalse(BillParsing.isIncome("已转账给张三，金额200.00元"))
    }

    @Test
    fun `ordinary expense is expense`() {
        assertFalse(BillParsing.isIncome("付款金额¥14.40"))
    }

    // ── Title origin helper tests ─────────────────────────────────────────

    @Test
    fun `same origin titles share prefix`() {
        assertTrue(BillParsing.isSameOriginTitle("微信支付", "微信支付-新"))
        assertTrue(BillParsing.isSameOriginTitle("微信支付", "微信支付"))
    }

    @Test
    fun `different origin titles do not share prefix`() {
        assertFalse(BillParsing.isSameOriginTitle("微信支付", "美团外卖订单"))
    }

    // ── Same-app deduplication tests ───────────────────────────────────────

    @Test
    fun `same app - identical notification within 60s should be skipped (content dedup)`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", false, t)
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付", false, t + 30_000) // +30s

        assertEquals("inserted", result1)
        assertEquals("skipped", result2)
        assertEquals(1, bills.size)
    }

    @Test
    fun `same app - same amount related title within 60s should be skipped (time dedup)`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", false, t)
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付-新", false, t + 30_000) // +30s

        assertEquals("inserted", result1)
        assertEquals("skipped", result2)
        assertEquals(1, bills.size)
    }

    @Test
    fun `same app - distinct purchases with different titles within 60s should both insert`() {
        // Regression: the old 1b rule silently dropped the second legitimate purchase
        val t = 1000000L
        val result1 = processBill(10.00, "com.eg.android.AlipayGphone", "外卖订单", false, t)
        val result2 = processBill(10.00, "com.eg.android.AlipayGphone", "共享单车", false, t + 30_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    @Test
    fun `same app - payment then refund same amount same title within 60s both insert`() {
        // Different direction = genuinely different transaction, must not dedup
        val t = 1000000L
        val result1 = processBill(14.40, "com.tencent.mm", "微信支付", false, t)
        val result2 = processBill(14.40, "com.tencent.mm", "微信支付", true, t + 30_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    @Test
    fun `same app - same amount after 60s with different title should be inserted`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", false, t)
        // +90s, different title -> outside 60s window, not content match
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付-新通知", false, t + 90_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    @Test
    fun `same app - identical title after 2min should be inserted (outside all windows)`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", false, t)
        // +2 min -> outside 60s windows
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付", false, t + 120_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    @Test
    fun `same app - identical title at exactly 60s boundary should be skipped`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", false, t)
        // Exactly at 60s: since = (t+60000) - 60000 = t, bill timestamp = t >= t → match
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付", false, t + 60_000)

        assertEquals("inserted", result1)
        assertEquals("skipped", result2)
        assertEquals(1, bills.size)
    }

    // ── Cross-app deduplication tests ──────────────────────────────────────

    @Test
    fun `cross app - meituan then wechat same amount within 60s keeps meituan (higher weight)`() {
        val t = 1000000L
        val result1 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", false, t)
        // WeChat arrives 10 seconds later
        val result2 = processBill(20.00, "com.tencent.mm", "微信支付", false, t + 10_000)

        assertEquals("inserted", result1)
        assertEquals("skipped", result2) // Meituan weight 100 >= WeChat 50, keep existing
        assertEquals(1, bills.size)
        assertEquals("com.sankuai.meituan", bills[0].packageName)
    }

    @Test
    fun `cross app - wechat then meituan same amount within 60s replaces with meituan`() {
        val t = 1000000L
        val result1 = processBill(20.00, "com.tencent.mm", "微信支付", false, t)
        // Meituan arrives 30 seconds later
        val result2 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", false, t + 30_000)

        assertEquals("inserted", result1)
        assertEquals("replaced", result2) // Meituan weight 100 > WeChat 50
        assertEquals(1, bills.size)
        assertEquals("com.sankuai.meituan", bills[0].packageName)
    }

    @Test
    fun `cross app - wechat then alipay same amount keeps first (equal weight)`() {
        val t = 1000000L
        val result1 = processBill(15.00, "com.tencent.mm", "微信支付", false, t)
        val result2 = processBill(15.00, "com.eg.android.AlipayGphone", "交易提醒", false, t + 10_000)

        assertEquals("inserted", result1)
        assertEquals("skipped", result2) // Equal weight (50), keep existing
        assertEquals(1, bills.size)
        assertEquals("com.tencent.mm", bills[0].packageName)
    }

    @Test
    fun `cross app - same amount but different direction inserts separately`() {
        val t = 1000000L
        // Payment via Meituan, then a same-amount refund arrives via WeChat
        val result1 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", false, t)
        val result2 = processBill(20.00, "com.tencent.mm", "微信支付", true, t + 10_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    @Test
    fun `cross app - same amount after 60s should insert as separate bill`() {
        val t = 1000000L
        val result1 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", false, t)
        // WeChat arrives 2 minutes later -> outside cross-app window
        val result2 = processBill(20.00, "com.tencent.mm", "微信支付", false, t + 120_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    // ── Combined scenario tests (mirroring the actual bugs) ────────────────

    @Test
    fun `bug scenario 1 - duplicate wechat notifications for same payment`() {
        // Simulates: 06.16 Tuesday, two WeChat ¥9.90 at 18:57
        val t = 1000000L
        val r1 = processBill(9.90, "com.tencent.mm", "微信支付", false, t)
        // Second WeChat arrives 5 seconds later (same title)
        val r2 = processBill(9.90, "com.tencent.mm", "微信支付", false, t + 5_000)

        assertEquals("inserted", r1)
        assertEquals("skipped", r2)
        assertEquals("Only 1 bill should exist", 1, bills.size)
    }

    @Test
    fun `bug scenario 2 - meituan and wechat for same payment`() {
        // Simulates: 06.18 Today, Meituan ¥20.00 + WeChat ¥20.00 at 11:19
        val t = 1000000L
        // Meituan arrives first
        val r1 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", false, t)
        // WeChat arrives 3 seconds later (same payment via WeChat Pay)
        val r2 = processBill(20.00, "com.tencent.mm", "微信支付", false, t + 3_000)

        assertEquals("inserted", r1)
        assertEquals("skipped", r2)
        assertEquals("Only 1 bill should exist", 1, bills.size)
        assertEquals("Should keep Meituan (higher weight)", "com.sankuai.meituan", bills[0].packageName)
    }

    @Test
    fun `bug scenario 2 reversed - wechat arrives before meituan`() {
        val t = 1000000L
        // WeChat arrives first
        val r1 = processBill(20.00, "com.tencent.mm", "微信支付", false, t)
        // Meituan arrives 10 seconds later
        val r2 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", false, t + 10_000)

        assertEquals("inserted", r1)
        assertEquals("replaced", r2)
        assertEquals("Only 1 bill should exist", 1, bills.size)
        assertEquals("Should be replaced by Meituan", "com.sankuai.meituan", bills[0].packageName)
    }

    @Test
    fun `legitimate - two separate purchases same amount 1 hour apart`() {
        val t = 1000000L
        val r1 = processBill(20.00, "com.tencent.mm", "微信支付", false, t)
        // Same amount, 1 hour later (clearly a separate purchase)
        val r2 = processBill(20.00, "com.tencent.mm", "微信支付-新", false, t + 3_600_000)

        assertEquals("inserted", r1)
        assertEquals("inserted", r2)
        assertEquals("Should have 2 separate bills", 2, bills.size)
    }

    @Test
    fun `edge case - delayed notification with old postTime`() {
        // Simulates service processing delay: notification posted at t,
        // but processed much later. Using postTime as anchor should still work.
        val postTime = 1000000L
        // First bill inserted at postTime
        val r1 = processBill(50.00, "com.tencent.mm", "微信支付", false, postTime)
        // Second identical notification with same postTime (batched delivery)
        val r2 = processBill(50.00, "com.tencent.mm", "微信支付", false, postTime + 2_000)

        assertEquals("inserted", r1)
        assertEquals("skipped", r2)
        assertEquals(1, bills.size)
    }

    @Test
    fun `three apps - meituan wechat alipay all same payment within 60s`() {
        val t = 1000000L
        // WeChat arrives first
        val r1 = processBill(30.00, "com.tencent.mm", "微信支付", false, t)
        // Alipay arrives 10s later (same weight as WeChat, should be skipped)
        val r2 = processBill(30.00, "com.eg.android.AlipayGphone", "交易提醒", false, t + 10_000)
        // Meituan arrives 20s later (highest weight, should replace)
        val r3 = processBill(30.00, "com.sankuai.meituan", "您已成功付款 30.00 元", false, t + 20_000)

        assertEquals("inserted", r1)
        assertEquals("skipped", r2)
        assertEquals("replaced", r3)
        assertEquals("Only 1 bill should exist", 1, bills.size)
        assertEquals("Should be Meituan", "com.sankuai.meituan", bills[0].packageName)
    }
}
