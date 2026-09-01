package com.aifactory.appmessagecapture.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 预算阈值判定纯函数测试：支出/预算的比例决定提醒级别。
 * 0=正常 1=预警(≥80%) 2=超支(≥100%)，边界值精确验证。
 */
class BudgetNotifierTest {

    @Test
    fun `under 80 percent is ok`() {
        assertEquals(BudgetNotifier.LEVEL_OK, BudgetNotifier.budgetLevel(spent = 79.9, budget = 100.0))
        assertEquals(BudgetNotifier.LEVEL_OK, BudgetNotifier.budgetLevel(spent = 0.0, budget = 100.0))
    }

    @Test
    fun `at exactly 80 percent is warn`() {
        assertEquals(BudgetNotifier.LEVEL_WARN, BudgetNotifier.budgetLevel(spent = 80.0, budget = 100.0))
    }

    @Test
    fun `between 80 and 100 percent is warn`() {
        assertEquals(BudgetNotifier.LEVEL_WARN, BudgetNotifier.budgetLevel(spent = 95.0, budget = 100.0))
    }

    @Test
    fun `at exactly 100 percent is over`() {
        assertEquals(BudgetNotifier.LEVEL_OVER, BudgetNotifier.budgetLevel(spent = 100.0, budget = 100.0))
    }

    @Test
    fun `over 100 percent is over`() {
        assertEquals(BudgetNotifier.LEVEL_OVER, BudgetNotifier.budgetLevel(spent = 150.0, budget = 100.0))
    }

    @Test
    fun `category budget independently reaches warn then over`() {
        // 模拟某分类预算 50：支出 40 → 预警；支出 50 → 超支
        assertEquals(BudgetNotifier.LEVEL_WARN, BudgetNotifier.budgetLevel(spent = 40.0, budget = 50.0))
        assertEquals(BudgetNotifier.LEVEL_OVER, BudgetNotifier.budgetLevel(spent = 50.0, budget = 50.0))
    }
}
