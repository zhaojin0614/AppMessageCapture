package com.aifactory.appmessagecapture.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val billDao = AppDatabase.getDatabase(application).billDao()

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState

    private val _periodType = MutableStateFlow(PeriodType.WEEK)
    val periodType: StateFlow<PeriodType> = _periodType

    private val _showIncome = MutableStateFlow(false)
    val showIncome: StateFlow<Boolean> = _showIncome

    private val _currentOffset = MutableStateFlow(0)
    val currentOffset: StateFlow<Int> = _currentOffset

    init {
        // Reactive pipeline: any change of period/income/offset re-queries the
        // time-ranged Flow, and new bills inserted while the report is open
        // automatically refresh it (previously a one-shot load that went stale).
        viewModelScope.launch {
            combine(_periodType, _showIncome, _currentOffset) { type, income, offset ->
                Triple(type, income, offset)
            }.flatMapLatest { (type, income, offset) ->
                val earliestMillis = earliestMillisFor(type, offset)
                billDao.getBillsSince(earliestMillis).mapLatest { bills ->
                    buildUiState(bills, type, income, offset)
                }
            }.collect { _uiState.value = it }
        }
    }

    fun setPeriodType(type: PeriodType) {
        _periodType.value = type
        _currentOffset.value = 0
    }

    fun toggleShowIncome() {
        _showIncome.value = !_showIncome.value
    }

    fun prevPeriod() {
        _currentOffset.value -= 1
    }

    fun nextPeriod() {
        _currentOffset.value += 1
    }

    fun setMonth(year: Int, month: Int) {
        val now = LocalDate.now()
        val selected = LocalDate.of(year, month, 1)
        _currentOffset.value = (selected.year - now.year) * 12 + (selected.monthValue - now.monthValue)
    }

    fun setYear(year: Int) {
        val now = LocalDate.now()
        _currentOffset.value = year - now.year
    }

    // ------------------------------------------------------------------
    // UI state derivation (pure, given a bill list)
    // ------------------------------------------------------------------

    private fun earliestMillisFor(type: PeriodType, offset: Int): Long {
        val now = LocalDate.now()
        // Bar data goes back 5 periods from the current one
        val earliestDate = when (type) {
            PeriodType.WEEK -> now.plusWeeks((offset - 5).toLong())
                .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
            PeriodType.MONTH -> now.plusMonths((offset - 5).toLong()).withDayOfMonth(1)
            PeriodType.YEAR -> now.plusYears((offset - 5).toLong()).withMonth(1).withDayOfMonth(1)
        }
        return earliestDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun buildUiState(
        bills: List<BillEntity>,
        type: PeriodType,
        showIncome: Boolean,
        offset: Int
    ): ReportUiState {
        val now = LocalDate.now()

        val currentRange = when (type) {
            PeriodType.WEEK -> getWeekRange(now.plusWeeks(offset.toLong()))
            PeriodType.MONTH -> getMonthRange(now.plusMonths(offset.toLong()))
            PeriodType.YEAR -> getYearRange(now.plusYears(offset.toLong()))
        }
        val prevRange = when (type) {
            PeriodType.WEEK -> getWeekRange(now.plusWeeks(offset.toLong()).minusWeeks(1))
            PeriodType.MONTH -> getMonthRange(now.plusMonths(offset.toLong()).minusMonths(1))
            PeriodType.YEAR -> getYearRange(now.plusYears(offset.toLong()).minusYears(1))
        }

        val currentBills = bills.filter { it.timestamp in currentRange.startMillis..currentRange.endMillis }
        val prevBills = bills.filter { it.timestamp in prevRange.startMillis..prevRange.endMillis }

        val currentIncome = currentBills.filter { it.isIncome }.sumOf { it.amount }
        val currentExpense = currentBills.filter { !it.isIncome }.sumOf { it.amount }
        val prevIncome = prevBills.filter { it.isIncome }.sumOf { it.amount }
        val prevExpense = prevBills.filter { !it.isIncome }.sumOf { it.amount }

        val selectedCurrentTotal = if (showIncome) currentIncome else currentExpense
        val selectedPrevTotal = if (showIncome) prevIncome else prevExpense
        val balance = currentIncome - currentExpense

        val elapsedDays = calculateElapsedDays(type, offset, currentRange.start, currentRange.end, LocalDate.now())
        val dailyAvg = selectedCurrentTotal / elapsedDays

        return ReportUiState(
            periodLabel = currentRange.label,
            periodTotal = selectedCurrentTotal,
            dailyAvg = dailyAvg,
            prevDiff = selectedCurrentTotal - selectedPrevTotal,
            balance = balance,
            trendData = calculateTrendData(currentBills, type, showIncome, offset),
            barData = calculateBarData(bills, type, showIncome, offset),
            categoryData = calculateCategoryData(currentBills, showIncome),
            currentIncome = currentIncome,
            currentExpense = currentExpense,
            currentYear = currentRange.start.year,
            currentMonth = currentRange.start.monthValue,
            periodStartMillis = currentRange.startMillis,
            periodEndMillis = currentRange.endMillis,
            isLoading = false,
            showIncome = showIncome
        )
    }

    private fun calculateTrendData(
        bills: List<BillEntity>,
        type: PeriodType,
        income: Boolean,
        offset: Int
    ): List<TrendPoint> {
        val zone = ZoneId.systemDefault()
        // One-pass bucketing: group bills by date once instead of re-filtering
        // the whole list (with a timezone conversion per bill) for every bucket.
        val filtered = bills.filter { it.isIncome == income }
        val byDate = filtered.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }

        return when (type) {
            PeriodType.WEEK -> {
                val base = LocalDate.now().plusWeeks(offset.toLong())
                    .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                (0..6).map { dayOffset ->
                    val date = base.plusDays(dayOffset.toLong())
                    val amount = byDate[date].orEmpty().sumOf { it.amount }
                    val label = String.format("%02d.%02d", date.monthValue, date.dayOfMonth)
                    TrendPoint(label, amount, date.toString())
                }
            }
            PeriodType.MONTH -> {
                val base = LocalDate.now().plusMonths(offset.toLong()).withDayOfMonth(1)
                val daysInMonth = base.lengthOfMonth()
                (0 until daysInMonth).map { dayOffset ->
                    val date = base.plusDays(dayOffset.toLong())
                    val amount = byDate[date].orEmpty().sumOf { it.amount }
                    TrendPoint("${date.dayOfMonth}日", amount, date.toString())
                }
            }
            PeriodType.YEAR -> {
                val base = LocalDate.now().plusYears(offset.toLong()).withMonth(1).withDayOfMonth(1)
                val byMonth = filtered.groupBy {
                    YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate())
                }
                (0..11).map { monthOffset ->
                    val month = base.plusMonths(monthOffset.toLong())
                    val amount = byMonth[YearMonth.from(month)].orEmpty().sumOf { it.amount }
                    TrendPoint("${month.monthValue}月", amount, "${month.year}-${month.monthValue}")
                }
            }
        }
    }

    private fun calculateBarData(
        bills: List<BillEntity>,
        type: PeriodType,
        income: Boolean,
        currentOffset: Int
    ): List<BarPoint> {
        val zone = ZoneId.systemDefault()
        val filtered = bills.filter { it.isIncome == income }
        val byDate = filtered.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }

        fun sumDays(start: LocalDate, dayCount: Long): Double =
            (0 until dayCount).sumOf { i -> byDate[start.plusDays(i)].orEmpty().sumOf { it.amount } }

        return when (type) {
            PeriodType.WEEK -> {
                (-5..0).map { offset ->
                    val weekStart = LocalDate.now().plusWeeks((currentOffset + offset).toLong())
                        .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                    val weekEnd = weekStart.plusDays(6)
                    val weekNumber = weekStart.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
                    val label = when {
                        currentOffset == 0 && offset == 0 -> "本周"
                        currentOffset == 0 && offset == -1 -> "上周"
                        else -> "${weekNumber}周"
                    }
                    val tooltip = String.format(
                        "%d.%02d.%02d~%02d",
                        weekStart.year, weekStart.monthValue, weekStart.dayOfMonth, weekEnd.dayOfMonth
                    )
                    BarPoint(label, sumDays(weekStart, 7), tooltip)
                }
            }
            PeriodType.MONTH -> {
                val byMonth = filtered.groupBy {
                    YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate())
                }
                (-5..0).map { offset ->
                    val monthStart = LocalDate.now().plusMonths((currentOffset + offset).toLong()).withDayOfMonth(1)
                    val label = when (offset) {
                        0 -> "本月"
                        -1 -> "上月"
                        else -> "${monthStart.monthValue}月"
                    }
                    val tooltip = String.format("%d年%02d月", monthStart.year, monthStart.monthValue)
                    BarPoint(label, byMonth[YearMonth.from(monthStart)].orEmpty().sumOf { it.amount }, tooltip)
                }
            }
            PeriodType.YEAR -> {
                val byYear = filtered.groupBy {
                    Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate().year
                }
                (-5..0).map { offset ->
                    val year = LocalDate.now().plusYears((currentOffset + offset).toLong())
                    val label = when (offset) {
                        0 -> "本年"
                        -1 -> "上年"
                        else -> "${year.year}年"
                    }
                    BarPoint(label, byYear[year.year].orEmpty().sumOf { it.amount }, "${year.year}年")
                }
            }
        }
    }

    private fun calculateCategoryData(
        bills: List<BillEntity>,
        showIncome: Boolean
    ): List<CategoryStat> {
        val filtered = bills.filter { it.isIncome == showIncome }
        val total = filtered.sumOf { it.amount }.takeIf { it > 0 } ?: return emptyList()
        return filtered.groupBy { it.category }
            .map { (category, list) ->
                CategoryStat(
                    category,
                    list.sumOf { it.amount },
                    list.size,
                    list.sumOf { it.amount } / total
                )
            }
            .sortedByDescending { it.amount }
    }

    private fun getWeekRange(date: LocalDate): DateRange {
        val start = date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
        val end = start.plusDays(6)
        return DateRange(
            start, end,
            start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            String.format("%d.%02d.%02d~%02d.%02d", start.year, start.monthValue, start.dayOfMonth, end.monthValue, end.dayOfMonth)
        )
    }

    private fun getMonthRange(date: LocalDate): DateRange {
        val start = date.withDayOfMonth(1)
        val end = start.plusMonths(1).minusDays(1)
        return DateRange(
            start, end,
            start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            String.format("%d年%02d月", date.year, date.monthValue)
        )
    }

    private fun getYearRange(date: LocalDate): DateRange {
        val start = date.withMonth(1).withDayOfMonth(1)
        val end = date.withMonth(12).withDayOfMonth(31)
        return DateRange(
            start, end,
            start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            String.format("%d年", date.year)
        )
    }

    data class ReportUiState(
        val periodLabel: String = "",
        val periodTotal: Double = 0.0,
        val dailyAvg: Double = 0.0,
        val prevDiff: Double = 0.0,
        val balance: Double = 0.0,
        val trendData: List<TrendPoint> = emptyList(),
        val barData: List<BarPoint> = emptyList(),
        val categoryData: List<CategoryStat> = emptyList(),
        val currentIncome: Double = 0.0,
        val currentExpense: Double = 0.0,
        val currentYear: Int = java.time.LocalDate.now().year,
        val currentMonth: Int = java.time.LocalDate.now().monthValue,
        val periodStartMillis: Long = 0L,
        val periodEndMillis: Long = 0L,
        val isLoading: Boolean = true,
        val showIncome: Boolean = false
    )

    data class TrendPoint(val label: String, val amount: Double, val dateKey: String)
    data class BarPoint(val label: String, val amount: Double, val tooltipLabel: String)
    data class CategoryStat(val category: String, val amount: Double, val count: Int, val percentage: Double)
    data class DateRange(
        val start: LocalDate,
        val end: LocalDate,
        val startMillis: Long,
        val endMillis: Long,
        val label: String
    )

    enum class PeriodType { WEEK, MONTH, YEAR }
}

/**
 * 日均/月均分母计算（顶层纯函数，单元测试直连）：
 * - 历史周期（offset < 0）按完整周期天数算
 * - 当前/未来周期（offset >= 0）按周期内已过去的实际天数算（含今天），
 *   年视图按已过月数（含当月）
 */
internal fun calculateElapsedDays(
    type: ReportViewModel.PeriodType,
    offset: Int,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    today: LocalDate
): Long {
    return when {
        // 历史周期已完整结束，按完整周期天数算
        offset < 0 -> when (type) {
            ReportViewModel.PeriodType.WEEK -> 7L
            ReportViewModel.PeriodType.MONTH -> ChronoUnit.DAYS.between(periodStart, periodEnd) + 1
            ReportViewModel.PeriodType.YEAR -> 12L
        }
        // 当前/未来周期：按周期内已过去的实际天数算
        else -> {
            when {
                // 周期尚未开始（未来周期）
                today.isBefore(periodStart) -> 1L
                // 周期已结束（offset>=0 的边界情况），按完整周期
                today.isAfter(periodEnd) -> when (type) {
                    ReportViewModel.PeriodType.WEEK -> 7L
                    ReportViewModel.PeriodType.MONTH -> ChronoUnit.DAYS.between(periodStart, periodEnd) + 1
                    ReportViewModel.PeriodType.YEAR -> 12L
                }
                // 年视图按已过月数算（含当月）
                type == ReportViewModel.PeriodType.YEAR -> today.monthValue.toLong()
                // 周/月视图按已过天数算（含今天）
                else -> ChronoUnit.DAYS.between(periodStart, today) + 1
            }
        }
    }.coerceAtLeast(1)
}
