package com.aifactory.appmessagecapture.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BillBackupManagerTest {

    private fun ts(local: LocalDateTime): Long =
        local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun sampleBills(): List<BillEntity> = listOf(
        BillEntity(
            id = 1, amount = 30.38, appName = "京东", packageName = "com.jingdong.app.mall",
            title = "京东支付", category = "购物", isIncome = false,
            timestamp = ts(LocalDateTime.of(2026, 8, 30, 22, 31, 5)), platformAccountId = 10
        ),
        BillEntity(
            id = 2, amount = 8500.0, appName = "手动记账-收入", packageName = "",
            title = "8月工资", category = "工资", isIncome = true,
            timestamp = ts(LocalDateTime.of(2026, 8, 10, 9, 0, 0)), platformAccountId = 11
        ),
        BillEntity(
            id = 3, amount = 25.6, appName = "美团", packageName = "com.sankuai.meituan",
            title = "美团外卖", category = "餐饮", isIncome = false,
            timestamp = ts(LocalDateTime.of(2026, 8, 29, 18, 40, 0)), platformAccountId = null
        )
    )

    private fun samplePlatforms(): List<PlatformAccountEntity> = listOf(
        PlatformAccountEntity(id = 10, name = "微信钱包", balance = 1234.56, sortOrder = 0),
        PlatformAccountEntity(id = 11, name = "支付宝", balance = 890.0, sortOrder = 1)
    )

    @Test
    fun `导出再解析 - 收支分表与平台按名回填`() {
        val bytes = BillBackupManager.buildWorkbook(sampleBills(), samplePlatforms())
        val parsed = BillBackupManager.parseWorkbook(bytes)

        assertEquals(0, parsed.badRows)
        assertEquals(3, parsed.bills.size)
        assertEquals(2, parsed.platforms.size)

        val expense = parsed.bills.filter { !it.isIncome }
        val income = parsed.bills.filter { it.isIncome }
        assertEquals(2, expense.size)
        assertEquals(1, income.size)

        val jd = expense.first { it.title == "京东支付" }
        assertEquals(30.38, jd.amount, 1e-9)
        assertEquals("购物", jd.category)
        assertEquals("com.jingdong.app.mall", jd.packageName)
        assertEquals("微信钱包", jd.platformName)
        assertEquals(
            ts(LocalDateTime.of(2026, 8, 30, 22, 31, 5)), jd.timestamp
        )

        val salary = income.single()
        assertEquals("支付宝", salary.platformName)
        assertEquals(8500.0, salary.amount, 1e-9)

        // 待对账 → platformName = null
        val meituan = expense.first { it.title == "美团外卖" }
        assertNull(meituan.platformName)

        assertEquals("微信钱包", parsed.platforms.first { it.name == "微信钱包" }.name)
        assertEquals(1234.56, parsed.platforms.first { it.name == "微信钱包" }.balance, 1e-9)
    }

    @Test
    fun `解析 - 乱序表头与多余列不受影响`() {
        val bytes = BillBackupManager.buildWorkbook(sampleBills(), samplePlatforms())
        val sheets = com.aifactory.appmessagecapture.utils.MiniXlsx.read(bytes)
        // 把「支出账单」的表头列顺序打乱后重新打包，验证按列名映射
        val expense = sheets.first { it.name == BillBackupManager.SHEET_EXPENSE }
        val header = expense.rows.first()
        val permuted = expense.rows.map { row -> header.indices.map { i -> row[header.size - 1 - i] } }
        val reBytes = com.aifactory.appmessagecapture.utils.MiniXlsx.write(
            listOf(
                com.aifactory.appmessagecapture.utils.MiniSheet(BillBackupManager.SHEET_EXPENSE, permuted),
                sheets.first { it.name == BillBackupManager.SHEET_INCOME },
                sheets.first { it.name == BillBackupManager.SHEET_PLATFORMS }
            )
        )
        val parsed = BillBackupManager.parseWorkbook(reBytes)
        val jd = parsed.bills.first { it.title == "京东支付" }
        assertEquals(30.38, jd.amount, 1e-9)
    }

    @Test
    fun `解析 - 坏行计入失败但不影响好行`() {
        val bytes = BillBackupManager.buildWorkbook(sampleBills(), samplePlatforms())
        // 直接构造坏行：日期不可解析 / 金额非法
        val sheets = com.aifactory.appmessagecapture.utils.MiniXlsx.read(bytes)
        val expense = sheets.first { it.name == BillBackupManager.SHEET_EXPENSE }
        val badRows = expense.rows + listOf(
            listOf<Any?>("不是日期", "餐饮", "坏行一", 10.0, "待对账", "美团", "", "pkg", ""),
            listOf<Any?>("2026-08-29 18:40:00", "餐饮", "坏行二", 0.0, "待对账", "美团", "", "pkg", "")
        )
        val reBytes = com.aifactory.appmessagecapture.utils.MiniXlsx.write(
            listOf(
                com.aifactory.appmessagecapture.utils.MiniSheet(BillBackupManager.SHEET_EXPENSE, badRows),
                sheets.first { it.name == BillBackupManager.SHEET_INCOME },
                sheets.first { it.name == BillBackupManager.SHEET_PLATFORMS }
            )
        )
        val parsed = BillBackupManager.parseWorkbook(reBytes)
        assertEquals(2, parsed.badRows)
        assertEquals(3, parsed.bills.size)
    }

    @Test
    fun `解析 - 缺少账单工作表时抛出`() {
        val bytes = com.aifactory.appmessagecapture.utils.MiniXlsx.write(
            listOf(com.aifactory.appmessagecapture.utils.MiniSheet(BillBackupManager.SHEET_PLATFORMS, listOf(listOf<Any?>("名称", "余额", "排序"))))
        )
        assertThrows(IllegalArgumentException::class.java) { BillBackupManager.parseWorkbook(bytes) }
    }

    @Test
    fun `解析 - 缺少必需列时抛出`() {
        val bytes = com.aifactory.appmessagecapture.utils.MiniXlsx.write(
            listOf(
                // 缺「金额」列
                com.aifactory.appmessagecapture.utils.MiniSheet(
                    BillBackupManager.SHEET_EXPENSE,
                    listOf(listOf<Any?>("日期时间", "标题"))
                )
            )
        )
        val ex = assertThrows(IllegalArgumentException::class.java) { BillBackupManager.parseWorkbook(bytes) }
        assert(ex.message!!.contains("金额"))
    }
}
