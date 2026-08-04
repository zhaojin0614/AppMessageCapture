package com.aifactory.appmessagecapture.ui.report

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 单元测试：验证报表日均/月均的分母计算逻辑。
 *
 * 复刻 [ReportViewModel.loadData] 中 elapsedDays 的计算规则：
 * - 历史周期（offset < 0）按完整周期天数算
 * - 当前/未来周期（offset >= 0）按周期内已过去的实际天数算（含今天）
 *
 * 由于 [ReportViewModel] 的逻辑内联在 loadData() 中且依赖 AndroidViewModel，
 * 这里抽取相同的计算规则独立验证，确保逻辑正确性。
 */
class ReportAvgTest {

    /** 与 ReportViewModel.loadData 中 elapsedDays 相同的计算逻辑。 */
    private fun computeElapsedDays(
        type: ReportViewModel.PeriodType,
        offset: Int,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        today: LocalDate
    ): Long {
        return when {
            offset < 0 -> when (type) {
                ReportViewModel.PeriodType.WEEK -> 7L
                ReportViewModel.PeriodType.MONTH -> ChronoUnit.DAYS.between(periodStart, periodEnd) + 1
                ReportViewModel.PeriodType.YEAR -> 12L
            }
            else -> {
                when {
                    today.isBefore(periodStart) -> 1L
                    today.isAfter(periodEnd) -> when (type) {
                        ReportViewModel.PeriodType.WEEK -> 7L
                        ReportViewModel.PeriodType.MONTH -> ChronoUnit.DAYS.between(periodStart, periodEnd) + 1
                        ReportViewModel.PeriodType.YEAR -> 12L
                    }
                    type == ReportViewModel.PeriodType.YEAR -> today.monthValue.toLong()
                    else -> ChronoUnit.DAYS.between(periodStart, today) + 1
                }
            }
        }.coerceAtLeast(1)
    }

    @Test
    fun `当前周周二查看本周，分母为2`() {
        // 假设今天是周二，本周周一是1号
        val monday = LocalDate.of(2026, 8, 3)   // 周一
        val sunday = LocalDate.of(2026, 8, 9)   // 周日
        val tuesday = LocalDate.of(2026, 8, 4)  // 周二
        val days = computeElapsedDays(
            ReportViewModel.PeriodType.WEEK, 0, monday, sunday, tuesday
        )
        assertEquals(2L, days)
        // 周二支出100元，日均应为 100/2 = 50，而非 100/7
        assertEquals(50.0, 100.0 / days, 0.001)
    }

    @Test
    fun `当前周周一查看本周，分母为1`() {
        val monday = LocalDate.of(2026, 8, 3)
        val sunday = LocalDate.of(2026, 8, 9)
        val days = computeElapsedDays(
            ReportViewModel.PeriodType.WEEK, 0, monday, sunday, monday
        )
        assertEquals(1L, days)
    }

    @Test
    fun `当前周周日查看本周，分母为7`() {
        val monday = LocalDate.of(2026, 8, 3)
        val sunday = LocalDate.of(2026, 8, 9)
        val days = computeElapsedDays(
            ReportViewModel.PeriodType.WEEK, 0, monday, sunday, sunday
        )
        assertEquals(7L, days)
    }

    @Test
    fun `历史完整周，分母为7`() {
        val lastMon = LocalDate.of(2026, 7, 27)
        val lastSun = LocalDate.of(2026, 8, 2)
        val today = LocalDate.of(2026, 8, 4)
        val days = computeElapsedDays(
            ReportViewModel.PeriodType.WEEK, -1, lastMon, lastSun, today
        )
        assertEquals(7L, days)
    }

    @Test
    fun `当前月中(15号)查看本月，分母为15`() {
        val monthStart = LocalDate.of(2026, 8, 1)
        val monthEnd = LocalDate.of(2026, 8, 31)
        val fifteenth = LocalDate.of(2026, 8, 15)
        val days = computeElapsedDays(
            ReportViewModel.PeriodType.MONTH, 0, monthStart, monthEnd, fifteenth
        )
        assertEquals(15L, days)
    }

    @Test
    fun `历史完整月，分母为整月天数`() {
        val julyStart = LocalDate.of(2026, 7, 1)
        val julyEnd = LocalDate.of(2026, 7, 31)
        val today = LocalDate.of(2026, 8, 4)
        val days = computeElapsedDays(
            ReportViewModel.PeriodType.MONTH, -1, julyStart, julyEnd, today
        )
        assertEquals(31L, days)
    }

    @Test
    fun `当前年8月查看本年，月均分母为8`() {
        val yearStart = LocalDate.of(2026, 1, 1)
        val yearEnd = LocalDate.of(2026, 12, 31)
        val aug = LocalDate.of(2026, 8, 4)
        val days = computeElapsedDays(
            ReportViewModel.PeriodType.YEAR, 0, yearStart, yearEnd, aug
        )
        assertEquals(8L, days)
    }

    @Test
    fun `历史完整年，月均分母为12`() {
        val lastYearStart = LocalDate.of(2025, 1, 1)
        val lastYearEnd = LocalDate.of(2025, 12, 31)
        val today = LocalDate.of(2026, 8, 4)
        val days = computeElapsedDays(
            ReportViewModel.PeriodType.YEAR, -1, lastYearStart, lastYearEnd, today
        )
        assertEquals(12L, days)
    }
}
