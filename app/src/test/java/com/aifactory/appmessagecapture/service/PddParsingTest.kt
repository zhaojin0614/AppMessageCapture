package com.aifactory.appmessagecapture.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 拼多多订单详情页解析测试。
 *
 * 用例取自两张真实支付完成订单页截图（D:\AiExplore\拼多多1.jpg、拼多多2.jpg）
 * 的窗口文本，页面混有大量营销金额（今日已拼3.84元、再拼8.16元可返1元、
 * 底部广告价）与地址/货号等数字，重点验证：
 * 1. 门槛三要素（实付+订单编号+商品快照）区分订单列表页/商品页/收银台；
 * 2. 金额只认「实付」前缀，不被营销数字污染；
 * 3. 商户提取不被底部广告区（同样含官方旗舰店/先用后付字样）干扰。
 */
class PddParsingTest {

    /** 拼多多1.jpg：旗舰店订单（打包中，无「订单确认」时间横幅） */
    private val flagshipOrderNodes = listOf(
        "打包中",
        "预计拼单成功后2天内发货",
        "徐 138****1489",
        "上海市松江区",
        "人民北路2999号东华大学(松江校区)",
        "修改",
        "丽邦家居生活官方旗舰店",
        "官方旗舰",
        "回头客好店",
        "多人团 享7000+件超低价商品",
        "立享低价",
        "品牌丽邦",
        "多人团",
        "【320抽4大包】丽邦湿厕纸家庭装女性经期私...",
        "¥ 13.62",
        "×1",
        "湿擦肛肛好-金盏花湿厕纸,全家安心用：【4大包-共320抽】80抽/包",
        "7天无理由退货",
        "分享商品",
        "联系商家",
        "申请退款",
        "实付: ¥13.62 (免运费)",
        "订单编号: 260903-034141652481469",
        "复制",
        "商品快照: 发生交易争议时，可作为判断依据",
        "查看更多订单信息",
        // 底部广告区：同样含官方旗舰店/百亿补贴/先用后付等标志词与价格
        "百亿补贴 品牌 海飞丝 【官方",
        "官方旗舰店 先用后付",
        "最后59分钟¥29.96",
        "最后59分钟¥14.75",
        "订单备注",
        "设为匿名",
        "再次拼单",
        "催发货"
    )

    /** 拼多多2.jpg：个人店订单（含「订单确认」时间横幅） */
    private val personalOrderNodes = listOf(
        "打包中 预计16小时内发货",
        "订单确认，已通知商家配货",
        "2026-09-03 17:11:19",
        "徐 138****1489",
        "上海市松江区",
        "人民北路2999号东华大学(松江校区)",
        "修改",
        "羊羊羊大叔",
        "今日已拼3.84元,再拼8.16元可返1元",
        "继续拼单",
        "适用于小米手环7/6/5/4/3代表带nfc版腕带硅胶三四五六七运动替换",
        "¥ 3.84",
        "×1",
        "小米手环5表带【nfc通用】,黑色",
        "未发货可秒退",
        "7天无理由退货",
        "分享商品",
        "联系商家",
        "申请退款",
        "实付: ¥3.84 (免运费)",
        "订单编号: 260903-071502425601469",
        "复制",
        "商品快照: 发生交易争议时，可作为判断依据",
        "查看更多订单信息",
        // 底部广告区
        "官方旗舰店 已包邮",
        "已包邮 先用后付",
        "订单备注",
        "设为匿名",
        "再次拼单",
        "催发货"
    )

    @Test
    fun `支付完成订单页被识别`() {
        assertTrue(PddParsing.isOrderPage(flagshipOrderNodes.joinToString("\n")))
        assertTrue(PddParsing.isOrderPage(personalOrderNodes.joinToString("\n")))
    }

    @Test
    fun `订单列表页不识别`() {
        // 列表项有实付金额，但无订单编号/商品快照骨架词
        val listPage = listOf(
            "待发货", "丽邦家居生活官方旗舰店",
            "【320抽4大包】丽邦湿厕纸", "¥ 13.62", "实付: ¥13.62"
        ).joinToString("\n")
        assertFalse(PddParsing.isOrderPage(listPage))
    }

    @Test
    fun `商品详情页与收银台不识别`() {
        // 商品详情页：有价格无订单编号
        assertFalse(
            PddParsing.isOrderPage("【320抽4大包】丽邦湿厕纸\n¥ 13.62\n先用后付\n百亿补贴\n拼单")
        )
        // 收银台：应付金额无订单编号
        assertFalse(
            PddParsing.isOrderPage("应付金额\n¥13.62\n微信支付\n支付宝支付")
        )
    }

    @Test
    fun `实付金额提取`() {
        assertEquals(13.62, PddParsing.extractPaidAmount(flagshipOrderNodes)!!, 0.001)
        assertEquals(3.84, PddParsing.extractPaidAmount(personalOrderNodes)!!, 0.001)
    }

    @Test
    fun `实付金额变体与千分位`() {
        assertEquals(18.98, PddParsing.parsePaidAmount("实付¥18.98")!!, 0.001)
        assertEquals(13.62, PddParsing.parsePaidAmount("实付: ¥13.62 (免运费)")!!, 0.001)
        assertEquals(13.62, PddParsing.parsePaidAmount("实付：¥13.62")!!, 0.001)
        assertEquals(1234.50, PddParsing.parsePaidAmount("实付: ¥1,234.50")!!, 0.001)
    }

    @Test
    fun `营销数字与广告价不触发金额提取`() {
        assertNull(PddParsing.parsePaidAmount("今日已拼3.84元,再拼8.16元可返1元"))
        assertNull(PddParsing.parsePaidAmount("最后59分钟¥29.96"))
        assertNull(PddParsing.parsePaidAmount("¥ 13.62"))          // 商品价，无实付前缀
        assertNull(PddParsing.parsePaidAmount("实付满15即可计入任务进度"))
        assertNull(PddParsing.parsePaidAmount("拍1带走3件"))
        // 全页提取结果必须是实付行的金额，不是广告价
        assertEquals(13.62, PddParsing.extractPaidAmount(flagshipOrderNodes)!!, 0.001)
    }

    @Test
    fun `实付标签与金额被拆成相邻节点时拼接兜底`() {
        val splitNodes = listOf("订单确认", "实付:", "¥13.62", "(免运费)", "订单编号: 260903-034141652481469")
        assertEquals(13.62, PddParsing.extractPaidAmount(splitNodes)!!, 0.001)
    }

    @Test
    fun `商户提取_旗舰店与个人店`() {
        assertEquals("丽邦家居生活官方旗舰店", PddParsing.extractMerchant(flagshipOrderNodes))
        assertEquals("羊羊羊大叔", PddParsing.extractMerchant(personalOrderNodes))
    }

    @Test
    fun `商户提取不被底部广告区干扰`() {
        // 广告区同样有「官方旗舰店」「先用后付」，但只在与订单编号行之前的区域找商户
        assertEquals("羊羊羊大叔", PddParsing.extractMerchant(personalOrderNodes))
        // 标志词节点在订单编号之前但没有商户前节点（黑名单/价格）时返回 null
        val noMerchant = listOf("¥ 13.62", "×1", "多人团", "实付: ¥13.62", "订单编号: 260903-034141652481469")
        assertNull(PddParsing.extractMerchant(noMerchant))
    }

    @Test
    fun `订单编号提取`() {
        assertEquals("260903-034141652481469", PddParsing.extractOrderId(flagshipOrderNodes))
        assertEquals("260903-071502425601469", PddParsing.extractOrderId(personalOrderNodes))
        // 拆节点兜底
        assertEquals(
            "260903-034141652481469",
            PddParsing.extractOrderId(listOf("订单编号:", "260903-034141652481469"))
        )
    }

    @Test
    fun `确认时间解析_无横幅时为null`() {
        val expected = LocalDateTime.of(2026, 9, 3, 17, 11, 19)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, PddParsing.parseConfirmTimeMillis(personalOrderNodes))
        // 旗舰店订单无时间横幅（拼单中状态）
        assertNull(PddParsing.parseConfirmTimeMillis(flagshipOrderNodes))
    }
}
