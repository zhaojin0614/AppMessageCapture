package com.aifactory.appmessagecapture.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.Period
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

    init { loadData() }

    fun setPeriodType(type: PeriodType) {
        _periodType.value = type
        _currentOffset.value = 0
        loadData()
    }

    fun toggleShowIncome() {
        _showIncome.value = !_showIncome.value
        loadData()
    }

    fun prevPeriod() {
        _currentOffset.value -= 1
        loadData()
    }

    fun nextPeriod() {
        _currentOffset.value += 1
        loadData()
    }

    fun setMonth(year: Int, month: Int) {
        val now = LocalDate.now()
        val selected = LocalDate.of(year, month, 1)
        _currentOffset.value = (selected.year - now.year) * 12 + (selected.monthValue - now.monthValue)
        loadData()
    }

    fun setYear(year: Int) {
        val now = LocalDate.now()
        _currentOffset.value = year - now.year
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val bills = withContext(Dispatchers.IO) { billDao.getAllBillsOnce() }
            val type = _periodType.value
            val showIncome = _showIncome.value
            val offset = _currentOffset.value

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

            val daysInPeriod = when (type) {
                PeriodType.WEEK -> 7L
                PeriodType.MONTH -> ChronoUnit.DAYS.between(currentRange.start, currentRange.end) + 1
                PeriodType.YEAR -> 12L
            }.coerceAtLeast(1)

            val dailyAvg = when (type) {
                PeriodType.YEAR -> selectedCurrentTotal / 12.0
                else -> selectedCurrentTotal / daysInPeriod
            }

            val trendData = calculateTrendData(currentBills, type, showIncome)
            val barData = calculateBarData(bills, type, showIncome, offset)
            val categoryData = calculateCategoryData(currentBills, showIncome)

            _uiState.value = ReportUiState(
                periodLabel = currentRange.label,
                periodTotal = selectedCurrentTotal,
                dailyAvg = dailyAvg,
                prevDiff = selectedCurrentTotal - selectedPrevTotal,
                balance = balance,
                trendData = trendData,
                barData = barData,
                categoryData = categoryData,
                currentIncome = currentIncome,
                currentExpense = currentExpense,
                currentYear = currentRange.start.year,
                currentMonth = currentRange.start.monthValue,
                isLoading = false,
                showIncome = showIncome
            )
        }
    }

    private fun calculateTrendData(
        bills: List<BillEntity>,
        type: PeriodType,
        income: Boolean
    ): List<TrendPoint> {
        val filtered = bills.filter { it.isIncome == income }
        val offset = _currentOffset.value
        return when (type) {
            PeriodType.WEEK -> {
                val base = LocalDate.now().plusWeeks(offset.toLong())
                    .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                (0..6).map { dayOffset ->
                    val date = base.plusDays(dayOffset.toLong())
                    val dayBills = filtered.filter {
                        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() == date
                    }
                    val label = String.format("%02d.%02d", date.monthValue, date.dayOfMonth)
                    TrendPoint(label, dayBills.sumOf { it.amount }, date.toString())
                }
            }
            PeriodType.MONTH -> {
                val base = LocalDate.now().plusMonths(offset.toLong()).withDayOfMonth(1)
                val daysInMonth = base.lengthOfMonth()
                (0 until daysInMonth).map { dayOffset ->
                    val date = base.plusDays(dayOffset.toLong())
                    val dayBills = filtered.filter {
                        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate() == date
                    }
                    TrendPoint("${date.dayOfMonth}日", dayBills.sumOf { it.amount }, date.toString())
                }
            }
            PeriodType.YEAR -> {
                val base = LocalDate.now().plusYears(offset.toLong()).withMonth(1).withDayOfMonth(1)
                (0..11).map { monthOffset ->
                    val month = base.plusMonths(monthOffset.toLong())
                    val monthBills = filtered.filter {
                        val d = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        d.year == month.year && d.monthValue == month.monthValue
                    }
                    TrendPoint("${month.monthValue}月", monthBills.sumOf { it.amount }, "${month.year}-${month.monthValue}")
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
        val filtered = bills.filter { it.isIncome == income }
        return when (type) {
            PeriodType.WEEK -> {
                (-5..0).map { offset ->
                    val weekStart = LocalDate.now().plusWeeks((currentOffset + offset).toLong())
                        .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                    val weekEnd = weekStart.plusDays(6)
                    val weekBills = filtered.filter {
                        val d = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        !d.isBefore(weekStart) && !d.isAfter(weekEnd)
                    }
                    val weekNumber = weekStart.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
                    val label = when {
                        currentOffset == 0 && offset == 0 -> "本周"
                        currentOffset == 0 && offset == -1 -> "上周"
                        else -> "${weekNumber}周"
                    }
                    val tooltip = String.format(
                        "%d.%02d.%02d~%02d",
                        weekStart.year,
                        weekStart.monthValue,
                        weekStart.dayOfMonth,
                        weekEnd.dayOfMonth
                    )
                    BarPoint(label, weekBills.sumOf { it.amount }, tooltip)
                }
            }
            PeriodType.MONTH -> {
                (-5..0).map { offset ->
                    val monthStart = LocalDate.now().plusMonths((currentOffset + offset).toLong()).withDayOfMonth(1)
                    val monthBills = filtered.filter {
                        val d = Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        d.year == monthStart.year && d.monthValue == monthStart.monthValue
                    }
                    val label = when (offset) {
                        0 -> "本月"
                        -1 -> "上月"
                        else -> "${monthStart.monthValue}月"
                    }
                    val tooltip = String.format("%d年%02d月", monthStart.year, monthStart.monthValue)
                    BarPoint(label, monthBills.sumOf { it.amount }, tooltip)
                }
            }
            PeriodType.YEAR -> {
                (-5..0).map { offset ->
                    val year = LocalDate.now().plusYears((currentOffset + offset).toLong())
                    val yearBills = filtered.filter {
                        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().year == year.year
                    }
                    val label = when (offset) {
                        0 -> "本年"
                        -1 -> "上年"
                        else -> "${year.year}年"
                    }
                    BarPoint(label, yearBills.sumOf { it.amount }, "${year.year}年")
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
