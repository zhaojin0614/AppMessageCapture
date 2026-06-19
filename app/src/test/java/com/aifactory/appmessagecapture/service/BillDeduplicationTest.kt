package com.aifactory.appmessagecapture.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for bill deduplication logic in [MessageCaptureService.tryExtractBill].
 *
 * Since the actual DAO requires a Room database (Android), this test simulates
 * the dedup decision flow using an in-memory list that mirrors the three Room queries:
 *   - findRecentByAmountPackageAndTitle  (content-based dedup)
 *   - findRecentByAmountAndPackage        (same-app time-proximity dedup)
 *   - findRecentByAmount                  (cross-app merge)
 *
 * The extraction helper [extractAmountForTest] replicates the regex logic from
 * [MessageCaptureService.extractAmount] so we can also verify amount parsing.
 */
class BillDeduplicationTest {

    // ── Simulated bill record ──────────────────────────────────────────────
    data class FakeBill(
        val id: Long,
        val amount: Double,
        val packageName: String,
        val title: String,
        val timestamp: Long
    )

    // In-memory "database"
    private val bills = mutableListOf<FakeBill>()
    private var nextId = 1L

    // ── Mirrors the three DAO queries ──────────────────────────────────────

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
        notificationTime: Long
    ): String {
        // 1a. Content-based dedup (60s)
        val contentSince = notificationTime - 60_000
        if (findRecentByAmountPackageAndTitle(amount, packageName, title, contentSince) != null) {
            return "skipped"
        }

        // 1b. Same-app time-proximity dedup (60s)
        val timeSince = notificationTime - 60_000
        if (findRecentByAmountAndPackage(amount, packageName, timeSince) != null) {
            return "skipped"
        }

        // 2. Cross-app merge (60s)
        val crossSince = notificationTime - 60_000
        val existing = findRecentByAmount(amount, crossSince)
        if (existing != null && existing.packageName != packageName) {
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
        bills.add(FakeBill(nextId++, amount, packageName, title, notificationTime))
        return "inserted"
    }

    private fun getAppWeight(packageName: String): Int = when (packageName) {
        "com.sankuai.meituan",
        "com.sankuai.meituan.takeoutnew" -> 100
        "com.dianping.v1" -> 90
        "com.jd.jrapp" -> 80
        "com.baidu.wallet" -> 70
        "com.eg.android.AlipayGphone" -> 50
        "com.tencent.mm" -> 50
        else -> 0
    }

    @Before
    fun setUp() {
        bills.clear()
        nextId = 1L
    }

    // ── Amount extraction tests ────────────────────────────────────────────

    private fun extractAmountForTest(text: String): Double? {
        val pattern1 = Regex("""[¥￥]\s*(\d+(?:\.\d{1,2})?)""")
        val pattern2 = Regex("""(\d+(?:\.\d{1,2})?)\s*[元円]""")
        val pattern3 = Regex("""(\d+\.\d{1,2})""")
        pattern1.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }
        pattern2.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }
        pattern3.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }
        return null
    }

    @Test
    fun `extract amount with yen sign`() {
        assertEquals(14.40, extractAmountForTest("¥14.40")!!, 0.001)
    }

    @Test
    fun `extract amount with yuan suffix`() {
        assertEquals(20.00, extractAmountForTest("您已成功付款 20.00 元")!!, 0.001)
    }

    @Test
    fun `extract amount from wechat notification`() {
        assertEquals(9.90, extractAmountForTest("微信支付 收款到账9.90元")!!, 0.001)
    }

    @Test
    fun `extract amount with full-width yen sign`() {
        assertEquals(99.99, extractAmountForTest("￥99.99")!!, 0.001)
    }

    @Test
    fun `extract amount returns null for no amount`() {
        assertNull(extractAmountForTest("这是一条普通消息"))
    }

    // ── Same-app deduplication tests ───────────────────────────────────────

    @Test
    fun `same app - identical notification within 60s should be skipped (content dedup)`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", t)
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付", t + 30_000) // +30s

        assertEquals("inserted", result1)
        assertEquals("skipped", result2)
        assertEquals(1, bills.size)
    }

    @Test
    fun `same app - same amount different title within 60s should be skipped (time dedup)`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", t)
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付-新", t + 30_000) // +30s

        assertEquals("inserted", result1)
        assertEquals("skipped", result2)
        assertEquals(1, bills.size)
    }

    @Test
    fun `same app - same amount after 60s with different title should be inserted`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", t)
        // +90s, different title -> outside 60s window, not content match
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付-新通知", t + 90_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    @Test
    fun `same app - identical title after 2min should be inserted (outside all windows)`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", t)
        // +2 min -> outside 60s windows
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付", t + 120_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    @Test
    fun `same app - identical title at exactly 60s boundary should be skipped`() {
        val t = 1000000L
        val result1 = processBill(9.90, "com.tencent.mm", "微信支付", t)
        // Exactly at 60s: since = (t+60000) - 60000 = t, bill timestamp = t >= t → match
        val result2 = processBill(9.90, "com.tencent.mm", "微信支付", t + 60_000)

        assertEquals("inserted", result1)
        assertEquals("skipped", result2)
        assertEquals(1, bills.size)
    }

    // ── Cross-app deduplication tests ──────────────────────────────────────

    @Test
    fun `cross app - meituan then wechat same amount within 60s keeps meituan (higher weight)`() {
        val t = 1000000L
        val result1 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", t)
        // WeChat arrives 10 seconds later
        val result2 = processBill(20.00, "com.tencent.mm", "微信支付", t + 10_000)

        assertEquals("inserted", result1)
        assertEquals("skipped", result2) // Meituan weight 100 >= WeChat 50, keep existing
        assertEquals(1, bills.size)
        assertEquals("com.sankuai.meituan", bills[0].packageName)
    }

    @Test
    fun `cross app - wechat then meituan same amount within 60s replaces with meituan`() {
        val t = 1000000L
        val result1 = processBill(20.00, "com.tencent.mm", "微信支付", t)
        // Meituan arrives 30 seconds later
        val result2 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", t + 30_000)

        assertEquals("inserted", result1)
        assertEquals("replaced", result2) // Meituan weight 100 > WeChat 50
        assertEquals(1, bills.size)
        assertEquals("com.sankuai.meituan", bills[0].packageName)
    }

    @Test
    fun `cross app - wechat then alipay same amount keeps first (equal weight)`() {
        val t = 1000000L
        val result1 = processBill(15.00, "com.tencent.mm", "微信支付", t)
        val result2 = processBill(15.00, "com.eg.android.AlipayGphone", "交易提醒", t + 10_000)

        assertEquals("inserted", result1)
        assertEquals("skipped", result2) // Equal weight (50), keep existing
        assertEquals(1, bills.size)
        assertEquals("com.tencent.mm", bills[0].packageName)
    }

    @Test
    fun `cross app - same amount after 60s should insert as separate bill`() {
        val t = 1000000L
        val result1 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", t)
        // WeChat arrives 2 minutes later -> outside cross-app window
        val result2 = processBill(20.00, "com.tencent.mm", "微信支付", t + 120_000)

        assertEquals("inserted", result1)
        assertEquals("inserted", result2)
        assertEquals(2, bills.size)
    }

    // ── Combined scenario tests (mirroring the actual bugs) ────────────────

    @Test
    fun `bug scenario 1 - duplicate wechat notifications for same payment`() {
        // Simulates: 06.16 Tuesday, two WeChat ¥9.90 at 18:57
        val t = 1000000L
        val r1 = processBill(9.90, "com.tencent.mm", "微信支付", t)
        // Second WeChat arrives 5 seconds later (same title)
        val r2 = processBill(9.90, "com.tencent.mm", "微信支付", t + 5_000)

        assertEquals("inserted", r1)
        assertEquals("skipped", r2)
        assertEquals("Only 1 bill should exist", 1, bills.size)
    }

    @Test
    fun `bug scenario 2 - meituan and wechat for same payment`() {
        // Simulates: 06.18 Today, Meituan ¥20.00 + WeChat ¥20.00 at 11:19
        val t = 1000000L
        // Meituan arrives first
        val r1 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", t)
        // WeChat arrives 3 seconds later (same payment via WeChat Pay)
        val r2 = processBill(20.00, "com.tencent.mm", "微信支付", t + 3_000)

        assertEquals("inserted", r1)
        assertEquals("skipped", r2)
        assertEquals("Only 1 bill should exist", 1, bills.size)
        assertEquals("Should keep Meituan (higher weight)", "com.sankuai.meituan", bills[0].packageName)
    }

    @Test
    fun `bug scenario 2 reversed - wechat arrives before meituan`() {
        val t = 1000000L
        // WeChat arrives first
        val r1 = processBill(20.00, "com.tencent.mm", "微信支付", t)
        // Meituan arrives 10 seconds later
        val r2 = processBill(20.00, "com.sankuai.meituan", "您已成功付款 20.00 元", t + 10_000)

        assertEquals("inserted", r1)
        assertEquals("replaced", r2)
        assertEquals("Only 1 bill should exist", 1, bills.size)
        assertEquals("Should be replaced by Meituan", "com.sankuai.meituan", bills[0].packageName)
    }

    @Test
    fun `legitimate - two separate purchases same amount 1 hour apart`() {
        val t = 1000000L
        val r1 = processBill(20.00, "com.tencent.mm", "微信支付", t)
        // Same amount, 1 hour later (clearly a separate purchase)
        val r2 = processBill(20.00, "com.tencent.mm", "微信支付-新", t + 3_600_000)

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
        val r1 = processBill(50.00, "com.tencent.mm", "微信支付", postTime)
        // Second identical notification with same postTime (batched delivery)
        val r2 = processBill(50.00, "com.tencent.mm", "微信支付", postTime + 2_000)

        assertEquals("inserted", r1)
        assertEquals("skipped", r2)
        assertEquals(1, bills.size)
    }

    @Test
    fun `three apps - meituan wechat alipay all same payment within 60s`() {
        val t = 1000000L
        // WeChat arrives first
        val r1 = processBill(30.00, "com.tencent.mm", "微信支付", t)
        // Alipay arrives 10s later (same weight as WeChat, should be skipped)
        val r2 = processBill(30.00, "com.eg.android.AlipayGphone", "交易提醒", t + 10_000)
        // Meituan arrives 20s later (highest weight, should replace)
        val r3 = processBill(30.00, "com.sankuai.meituan", "您已成功付款 30.00 元", t + 20_000)

        assertEquals("inserted", r1)
        assertEquals("skipped", r2)
        assertEquals("replaced", r3)
        assertEquals("Only 1 bill should exist", 1, bills.size)
        assertEquals("Should be Meituan", "com.sankuai.meituan", bills[0].packageName)
    }
}
