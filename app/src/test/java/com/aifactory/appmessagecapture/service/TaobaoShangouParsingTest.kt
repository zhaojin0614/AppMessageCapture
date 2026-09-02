package com.aifactory.appmessagecapture.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 淘宝闪购订单详情页解析测试。
 *
 * 用例取自真实订单页截图（D:\AiExplore\淘宝闪购订单界面.jpg）的窗口文本，
 * 页面上除「实付」金额外还有大量营销/进度数字（领到18元红包、满40可用、
 * 再下单41.1元返12元外卖红包、实付满15、18.9/120 元、预计 18:51-19:06），
 * 重点验证金额只从「实付」行提取不被污染；另验证订单页门槛能区分
 * 订单列表页与普通淘宝订单页。
 */
class TaobaoShangouParsingTest {

    /** 淘宝闪购订单详情页截图中的窗口文本节点（按屏幕顺序） */
    private val shangouOrderPageNodes = listOf(
        "6:19",
        "商家正在备餐",                        // 状态标题（随订单状态变化，无金额）
        "客服",
        "我帮您领到1个18元夜宵爆红包",         // 营销金额
        "满40可用",
        "去看看",
        "预计 18:51-19:06",
        "改订单信息", "联系商家", "催单", "取消订单", "开发票",
        "多单挑战",
        "再下单41.1元返12元外卖红包",          // 营销金额
        "实付满15即可计入任务进度",            // 「实付」后无货币金额，不得命中
        "18.9/120 元",
        "去下单",
        "闪购 袁记云饺(文汇路店)",             // 商户行
        "校园送",
        "共1件",
        "价格明细",
        "总优惠¥33 实付¥18.98",               // 金额行（同节点含营销金额）
        "收货信息",
        "备注", "依据餐量提供餐具", "修改",
        "发票", "未添加开票信息",
        "订单号 208302618161614",
        "送达时间 08-30(周日)18:51-19:06",     // 无年份，不得当作下单时间
        "下单时间 2026-08-30 18:16:14.118",
        "支付方式 支付宝"
    )

    private val zone = ZoneId.of("Asia/Shanghai")

    /** me.ele 独立 App 订单页节点（「下单时间」折叠在「订单信息」里，默认不可见） */
    private val eleOrderPageNodes = listOf(
        "订单已送达",
        "送至 松江大学城2期西桥星巴克旁美团外卖柜 徐东杰",
        "再买一单", "评价", "联系商家", "联系骑士", "打赏骑士", "更多",
        "闪购 袁记云饺(文汇路店)",
        "校园送",
        "共1件",
        "价格明细",
        "总优惠¥33 实付¥18.98",
        "发票", "未添加开票信息",
        "订单号 8023786204058882481 复制",
        "订单信息",
        "安心权益",
        "常见问题"
    )

    @Test
    fun `订单详情页被识别`() {
        assertTrue(
            TaobaoShangouParsing.isOrderPage(SupportedPaymentApps.TAOBAO_PACKAGE, shangouOrderPageNodes.joinToString("\n"))
        )
    }

    @Test
    fun `独立App订单页被识别且不依赖下单时间`() {
        val pageText = eleOrderPageNodes.joinToString("\n")
        assertTrue(TaobaoShangouParsing.isOrderPage(SupportedPaymentApps.ELE_PACKAGE, pageText))
        // 同一页面若出现在淘宝内，因缺「下单时间」不命中（淘宝内门槛更严）
        assertFalse(TaobaoShangouParsing.isOrderPage(SupportedPaymentApps.TAOBAO_PACKAGE, pageText))
        // 缺订单号（如「订单信息」未渲染完的过渡态）不命中
        val noId = eleOrderPageNodes.filterNot { it.startsWith("订单号") }.joinToString("\n")
        assertFalse(TaobaoShangouParsing.isOrderPage(SupportedPaymentApps.ELE_PACKAGE, noId))
    }

    @Test
    fun `订单列表页不识别为订单详情页`() {
        // 列表页有「闪购」频道标和条目「实付」，但没有「下单时间」行
        val listPage = listOf(
            "我的订单", "闪购", "全部", "待收货",
            "袁记云饺(文汇路店)", "共1件", "实付¥18.98",
            "再下一单"
        ).joinToString("\n")
        assertFalse(TaobaoShangouParsing.isOrderPage(SupportedPaymentApps.TAOBAO_PACKAGE, listPage))
        assertFalse(TaobaoShangouParsing.isOrderPage(SupportedPaymentApps.ELE_PACKAGE, listPage))
    }

    @Test
    fun `普通淘宝订单页不识别为闪购订单页`() {
        // 普通淘宝订单详情有「实付」「下单时间」但没有「闪购」标
        val normalPage = listOf(
            "等待发货", "官方旗舰店", "实付¥129.00",
            "下单时间 2026-08-30 10:00:00"
        ).joinToString("\n")
        assertFalse(TaobaoShangouParsing.isOrderPage(SupportedPaymentApps.TAOBAO_PACKAGE, normalPage))
    }

    @Test
    fun `独立App提取订单号`() {
        assertEquals("8023786204058882481", TaobaoShangouParsing.extractOrderId(eleOrderPageNodes))
        // 标签与数字拆成两个节点时取纯数字节点
        assertEquals(
            "8023786204058882481",
            TaobaoShangouParsing.extractOrderId(listOf("订单号", "8023786204058882481", "复制"))
        )
        assertNull(TaobaoShangouParsing.extractOrderId(shangouOrderPageNodes.dropLast(4)))
    }

    @Test
    fun `实付金额从金额行提取且不被营销数字污染`() {
        assertEquals(18.98, TaobaoShangouParsing.extractPaidAmount(shangouOrderPageNodes)!!, 0.001)
    }

    @Test
    fun `实付金额行变体`() {
        assertEquals(18.98, TaobaoShangouParsing.parsePaidAmount("实付¥18.98")!!, 0.001)
        assertEquals(18.98, TaobaoShangouParsing.parsePaidAmount("实付 ¥ 18.98")!!, 0.001)
        assertEquals(18.98, TaobaoShangouParsing.parsePaidAmount("总优惠¥33 实付¥18.98")!!, 0.001)
        assertEquals(1234.5, TaobaoShangouParsing.parsePaidAmount("实付¥1,234.50")!!, 0.001)
        assertEquals(88.0, TaobaoShangouParsing.parsePaidAmount("实付¥88")!!, 0.001)
    }

    @Test
    fun `营销与进度数字不触发实付金额提取`() {
        assertNull(TaobaoShangouParsing.parsePaidAmount("总优惠¥33"))
        assertNull(TaobaoShangouParsing.parsePaidAmount("实付满15即可计入任务进度"))
        assertNull(TaobaoShangouParsing.parsePaidAmount("再下单41.1元返12元外卖红包"))
        assertNull(TaobaoShangouParsing.parsePaidAmount("我帮您领到1个18元夜宵爆红包"))
        assertNull(TaobaoShangouParsing.parsePaidAmount("预计 18:51-19:06"))
        assertNull(TaobaoShangouParsing.parsePaidAmount("实付满15"))
    }

    @Test
    fun `无实付金额行时返回null`() {
        assertNull(
            TaobaoShangouParsing.extractPaidAmount(listOf("闪购 袁记云饺(文汇路店)", "下单时间 2026-08-30 18:16:14.118"))
        )
    }

    @Test
    fun `商户名从闪购同行节点提取`() {
        assertEquals("袁记云饺(文汇路店)", TaobaoShangouParsing.extractMerchant(shangouOrderPageNodes))
        assertEquals("肯德基(宅急送)", TaobaoShangouParsing.extractMerchant(listOf("闪购 肯德基(宅急送) >")))
    }

    @Test
    fun `闪购标与商户名拆成两个节点时取下一节点`() {
        assertEquals(
            "袁记云饺(文汇路店)",
            TaobaoShangouParsing.extractMerchant(listOf("价格明细", "闪购", "袁记云饺(文汇路店)", "共1件"))
        )
    }

    @Test
    fun `闪购节点后是Tab名时不误取商户`() {
        assertNull(TaobaoShangouParsing.extractMerchant(listOf("闪购", "首页", "袁记云饺(文汇路店)")))
        assertNull(TaobaoShangouParsing.extractMerchant(listOf("价格明细", "实付¥18.98")))
    }

    @Test
    fun `下单时间解析为epoch毫秒`() {
        val expected = LocalDateTime.of(2026, 8, 30, 18, 16, 14, 118_000_000)
            .atZone(zone).toInstant().toEpochMilli()

        // 标签与值在同一个节点 / 拆成两个节点，都能解析
        assertEquals(
            expected,
            TaobaoShangouParsing.parseOrderTimeMillis(shangouOrderPageNodes, zone)
        )
        assertEquals(
            expected,
            TaobaoShangouParsing.parseOrderTimeMillis(listOf("下单时间", "2026-08-30 18:16:14.118"), zone)
        )
        // 毫秒段缺失（两位或无毫秒）
        val noMillis = LocalDateTime.of(2026, 8, 30, 18, 16, 14)
            .atZone(zone).toInstant().toEpochMilli()
        assertEquals(
            noMillis,
            TaobaoShangouParsing.parseOrderTimeMillis(listOf("下单时间 2026-08-30 18:16:14"), zone)
        )
        assertEquals(
            LocalDateTime.of(2026, 8, 30, 18, 16, 14, 110_000_000).atZone(zone).toInstant().toEpochMilli(),
            TaobaoShangouParsing.parseOrderTimeMillis(listOf("2026-08-30 18:16:14.11"), zone)
        )
    }

    @Test
    fun `非下单时间的时刻文本不参与解析`() {
        // 送达时间只有 MM-dd、预计送达只有 HH:mm，均不命中全时间戳
        assertNull(
            TaobaoShangouParsing.parseOrderTimeMillis(
                listOf("送达时间 08-30(周日)18:51-19:06", "预计 18:51-19:06"), zone
            )
        )
        assertNull(TaobaoShangouParsing.parseOrderTimeMillis(emptyList(), zone))
    }
}
