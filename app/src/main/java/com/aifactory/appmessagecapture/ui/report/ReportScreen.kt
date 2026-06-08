@file:OptIn(ExperimentalMaterial3Api::class)

package com.aifactory.appmessagecapture.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

private val BluePrimary = Color(0xFF2196F3)
private val BlueLight = Color(0xFF64B5F6)
private val BlueBg = Color(0xFFF5F9FF)
private val GreenPositive = Color(0xFF4CAF50)
private val CardBg = Color(0xFFFFFFFF)
private val BgGray = Color(0xFFF5F7FA)
private val TextGray = Color(0xFF999999)
private val TextDark = Color(0xFF333333)
private val DividerLight = Color(0xFFEEEEEE)

@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val periodType by viewModel.periodType.collectAsState()
    val showIncome by viewModel.showIncome.collectAsState()
    val currentOffset by viewModel.currentOffset.collectAsState()

    var selectedCategory by remember { mutableStateOf<String?>(null) }

    if (selectedCategory != null) {
        CategoryDetailScreen(
            category = selectedCategory!!,
            isIncome = showIncome,
            onBack = { selectedCategory = null }
        )
        return
    }

    BackHandler(enabled = true) {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收支报表", fontWeight = FontWeight.Bold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGray)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
        ) {
            // Period tabs
            PeriodTypeTabs(
                selected = periodType,
                onSelect = { viewModel.setPeriodType(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Date nav + income/expense toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateNavigation(
                    periodType = periodType,
                    label = uiState.periodLabel,
                    currentYear = uiState.currentYear,
                    currentMonth = uiState.currentMonth,
                    canGoNext = currentOffset < 0,
                    onPrev = { viewModel.prevPeriod() },
                    onNext = { viewModel.nextPeriod() },
                    onSelectMonth = { y, m -> viewModel.setMonth(y, m) },
                    onSelectYear = { y -> viewModel.setYear(y) }
                )
                IncomeExpenseToggle(showIncome) { viewModel.toggleShowIncome() }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary cards
            SummaryCards(
                periodType = periodType,
                showIncome = showIncome,
                periodTotal = uiState.periodTotal,
                dailyAvg = uiState.dailyAvg,
                prevDiff = uiState.prevDiff,
                balance = uiState.balance,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trend line chart
            TrendLineChartSection(
                periodType = periodType,
                showIncome = showIncome,
                data = uiState.trendData,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bar chart
            TrendBarChartSection(
                showIncome = showIncome,
                data = uiState.barData,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category breakdown
            CategorySection(
                showIncome = showIncome,
                data = uiState.categoryData,
                onItemClick = { category -> selectedCategory = category },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PeriodTypeTabs(
    selected: ReportViewModel.PeriodType,
    onSelect: (ReportViewModel.PeriodType) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        ReportViewModel.PeriodType.WEEK to "周报",
        ReportViewModel.PeriodType.MONTH to "月报",
        ReportViewModel.PeriodType.YEAR to "年报"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8E8E8))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEach { (type, label) ->
            val isSelected = selected == type
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) BluePrimary else Color.Transparent)
                    .clickable { onSelect(type) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextGray
                )
            }
        }
    }
}

@Composable
private fun DateNavigation(
    periodType: ReportViewModel.PeriodType,
    label: String,
    currentYear: Int,
    currentMonth: Int,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectMonth: (Int, Int) -> Unit,
    onSelectYear: (Int) -> Unit
) {
    when (periodType) {
        ReportViewModel.PeriodType.WEEK -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一周期", tint = TextGray)
                }
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = TextDark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp), enabled = canGoNext) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一周期", tint = if (canGoNext) TextGray else TextGray.copy(alpha = 0.3f))
                }
            }
        }
        ReportViewModel.PeriodType.MONTH -> {
            var yearExpanded by remember { mutableStateOf(false) }
            var monthExpanded by remember { mutableStateOf(false) }
            val years = remember { (2020..LocalDate.now().year + 1).toList() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Year dropdown
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { yearExpanded = true }
                    ) {
                        Text("${currentYear}年", fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text("${year}年") },
                                onClick = {
                                    onSelectMonth(year, currentMonth)
                                    yearExpanded = false
                                }
                            )
                        }
                    }
                }
                // Month dropdown
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { monthExpanded = true }
                    ) {
                        Text(String.format("%02d月", currentMonth), fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                        (1..12).forEach { month ->
                            DropdownMenuItem(
                                text = { Text("${month}月") },
                                onClick = {
                                    onSelectMonth(currentYear, month)
                                    monthExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        ReportViewModel.PeriodType.YEAR -> {
            var expanded by remember { mutableStateOf(false) }
            val years = remember {
                val now = java.time.Year.now().value
                (2020..now).map { it to "${it}年" }.reversed()
            }
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = true }
                ) {
                    Text(label, fontSize = 14.sp, color = TextDark, fontWeight = FontWeight.Medium)
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    years.forEach { (year, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                onSelectYear(year)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseToggle(showIncome: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8E8E8))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(false to "支出", true to "收入").forEach { (income, label) ->
            val selected = showIncome == income
            val bg = if (selected) {
                if (income) GreenPositive else Color(0xFFFF5252)
            } else Color.Transparent
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else TextGray
                )
            }
        }
    }
}

@Composable
private fun SummaryCards(
    periodType: ReportViewModel.PeriodType,
    showIncome: Boolean,
    periodTotal: Double,
    dailyAvg: Double,
    prevDiff: Double,
    balance: Double,
    modifier: Modifier = Modifier
) {
    val typeLabel = if (showIncome) "收入" else "支出"
    val (totalLabel, avgLabel, diffLabel) = when (periodType) {
        ReportViewModel.PeriodType.WEEK ->
            Triple("本周${typeLabel}（元）", "日均${typeLabel}（元）", "比上周${typeLabel}（元）")
        ReportViewModel.PeriodType.MONTH ->
            Triple("本月${typeLabel}（元）", "日均${typeLabel}（元）", "比上月${typeLabel}（元）")
        ReportViewModel.PeriodType.YEAR ->
            Triple("本年${typeLabel}（元）", "月均${typeLabel}（元）", "比上年${typeLabel}（元）")
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = totalLabel,
                value = "${String.format("%.2f", periodTotal)}",
                valueColor = TextDark,
                leftBorderColor = BluePrimary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = avgLabel,
                value = "${String.format("%.2f", dailyAvg)}",
                valueColor = TextDark,
                leftBorderColor = BluePrimary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val diffColor = if (prevDiff >= 0) GreenPositive else Color(0xFFFF5252)
            val diffSign = if (prevDiff >= 0) "+" else ""
            StatCard(
                modifier = Modifier.weight(1f),
                title = diffLabel,
                value = "$diffSign${String.format("%.2f", prevDiff)}",
                valueColor = diffColor,
                leftBorderColor = BluePrimary
            )
            val balanceColor = if (balance >= 0) GreenPositive else Color(0xFFFF5252)
            val balanceSign = if (balance >= 0) "+" else ""
            StatCard(
                modifier = Modifier.weight(1f),
                title = "收支结余（元）",
                value = "$balanceSign${String.format("%.2f", balance)}",
                valueColor = balanceColor,
                leftBorderColor = BluePrimary
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    title: String,
    value: String,
    valueColor: Color,
    leftBorderColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 80.dp)
                    .background(leftBorderColor)
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = modifier.padding(bottom = 10.dp, top = 4.dp)
    )
}

@Composable
private fun TrendLineChartSection(
    periodType: ReportViewModel.PeriodType,
    showIncome: Boolean,
    data: List<ReportViewModel.TrendPoint>,
    modifier: Modifier = Modifier
) {
    val title = when (periodType) {
        ReportViewModel.PeriodType.WEEK -> "本周趋势"
        ReportViewModel.PeriodType.MONTH -> "本月趋势"
        ReportViewModel.PeriodType.YEAR -> "本年趋势"
    }
    val typeLabel = if (showIncome) "收入" else "支出"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(title)
            if (data.isNotEmpty()) {
                TrendLineChart(
                    data = data,
                    typeLabel = typeLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else {
                EmptyChartState("暂无数据")
            }
        }
    }
}

@Composable
private fun TrendLineChart(
    data: List<ReportViewModel.TrendPoint>,
    typeLabel: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val paddingLeft = 44.dp.toPx()
                        val paddingRight = 16.dp.toPx()
                        val plotWidth = size.width.toFloat() - paddingLeft - paddingRight
                        val step = plotWidth / (data.size - 1).coerceAtLeast(1)
                        val idx = ((offset.x - paddingLeft + step / 2) / step)
                            .toInt()
                            .coerceIn(0, data.size - 1)
                        selectedIndex = if (selectedIndex == idx) -1 else idx
                    }
                }
        ) {
            val paddingLeft = 44.dp.toPx()
            val paddingBottom = 32.dp.toPx()
            val paddingTop = 28.dp.toPx()
            val paddingRight = 16.dp.toPx()

            val plotWidth = size.width - paddingLeft - paddingRight
            val plotHeight = size.height - paddingTop - paddingBottom

            val maxValue = (data.maxOfOrNull { it.amount } ?: 0.0).let {
                if (it == 0.0) 100.0 else ceil(it / 5.0) * 5.0
            }

            // Horizontal grid lines + Y labels
            for (i in 0..4) {
                val y = paddingTop + plotHeight * (1 - i / 4f)
                drawLine(
                    color = DividerLight,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 1f
                )
                val label = String.format("%.2f", maxValue * i / 4)
                val labelResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 10.sp, color = TextGray)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(
                        paddingLeft - labelResult.size.width - 6f,
                        y - labelResult.size.height / 2
                    ),
                    style = TextStyle(fontSize = 10.sp, color = TextGray)
                )
            }

            // Compute points
            val points = data.mapIndexed { index, point ->
                val x = if (data.size <= 1) {
                    paddingLeft + plotWidth / 2
                } else {
                    paddingLeft + plotWidth * index / (data.size - 1)
                }
                val y = paddingTop + plotHeight * (1 - (point.amount / maxValue).toFloat())
                Offset(x, y)
            }

            // X labels - sparse display to avoid crowding
            val labelStep = when (data.size) {
                in 0..10 -> 1
                in 11..20 -> 2
                in 21..31 -> 5
                else -> kotlin.math.max(1, data.size / 6)
            }
            data.forEachIndexed { index, point ->
                if (index % labelStep == 0 || index == data.lastIndex) {
                    val x = if (data.size <= 1) {
                        paddingLeft + plotWidth / 2
                    } else {
                        paddingLeft + plotWidth * index / (data.size - 1)
                    }
                    drawText(
                        textMeasurer = textMeasurer,
                        text = point.label,
                        topLeft = Offset(
                            x - 12.dp.toPx(),
                            size.height - paddingBottom + 6f
                        ),
                        style = TextStyle(fontSize = 10.sp, color = TextGray, textAlign = TextAlign.Center)
                    )
                }
            }

            // Fill area
            if (points.size > 1) {
                val fillPath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    lineTo(points.last().x, paddingTop + plotHeight)
                    lineTo(points[0].x, paddingTop + plotHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(BluePrimary.copy(alpha = 0.2f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + plotHeight
                    )
                )
            }

            // Line
            if (points.size > 1) {
                val linePath = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = BluePrimary,
                    style = Stroke(width = 2.5f)
                )
            }

            // Points
            points.forEachIndexed { index, point ->
                val radius = if (index == selectedIndex) 6f else 3.5f
                drawCircle(color = Color.White, radius = radius + 2f, center = point)
                drawCircle(color = BluePrimary, radius = radius, center = point)
            }

            // Tooltip
            if (selectedIndex in points.indices) {
                val point = data[selectedIndex]
                val pt = points[selectedIndex]
                val tooltipText = "${point.label}${typeLabel} ¥${String.format("%.2f", point.amount)}"
                val tooltipStyle = TextStyle(
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                val tooltipResult = textMeasurer.measure(text = tooltipText, style = tooltipStyle)
                val bubbleWidth = tooltipResult.size.width + 20f
                val bubbleHeight = tooltipResult.size.height + 10f
                val triangleHeight = 6f

                var bubbleLeft = pt.x - bubbleWidth / 2
                if (bubbleLeft < 4f) bubbleLeft = 4f
                if (bubbleLeft + bubbleWidth > size.width - 4f) {
                    bubbleLeft = size.width - 4f - bubbleWidth
                }

                val bubbleTop = pt.y - bubbleHeight - triangleHeight - 8f
                val bubbleBottom = bubbleTop + bubbleHeight

                drawRoundRect(
                    color = BluePrimary,
                    topLeft = Offset(bubbleLeft, bubbleTop),
                    size = Size(bubbleWidth, bubbleHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                val trianglePath = Path().apply {
                    moveTo(pt.x, pt.y - 8f)
                    lineTo(pt.x - triangleHeight, bubbleBottom)
                    lineTo(pt.x + triangleHeight, bubbleBottom)
                    close()
                }
                drawPath(trianglePath, color = BluePrimary)

                drawText(
                    textMeasurer = textMeasurer,
                    text = tooltipText,
                    topLeft = Offset(bubbleLeft + 10f, bubbleTop + 5f),
                    style = tooltipStyle
                )
            }
        }
    }
}

@Composable
private fun TrendBarChartSection(
    showIncome: Boolean,
    data: List<ReportViewModel.BarPoint>,
    modifier: Modifier = Modifier
) {
    val title = if (showIncome) "收入趋势" else "支出趋势"
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(title)
            if (data.isNotEmpty()) {
                TrendBarChart(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            } else {
                EmptyChartState("暂无数据")
            }
        }
    }
}

@Composable
private fun TrendBarChart(
    data: List<ReportViewModel.BarPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val paddingLeft = 44.dp.toPx()
                        val paddingRight = 16.dp.toPx()
                        val plotWidth = size.width.toFloat() - paddingLeft - paddingRight
                        val barSlotWidth = plotWidth / data.size
                        val idx = ((offset.x - paddingLeft) / barSlotWidth)
                            .toInt()
                            .coerceIn(0, data.size - 1)
                        selectedIndex = if (selectedIndex == idx) -1 else idx
                    }
                }
        ) {
            val paddingLeft = 44.dp.toPx()
            val paddingBottom = 32.dp.toPx()
            val paddingTop = 28.dp.toPx()
            val paddingRight = 16.dp.toPx()

            val plotWidth = size.width - paddingLeft - paddingRight
            val plotHeight = size.height - paddingTop - paddingBottom

            val maxValue = (data.maxOfOrNull { it.amount } ?: 0.0).let {
                if (it == 0.0) 100.0 else ceil(it / 5.0) * 5.0
            }

            // Grid lines + Y labels
            for (i in 0..4) {
                val y = paddingTop + plotHeight * (1 - i / 4f)
                drawLine(
                    color = DividerLight,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 1f
                )
                val label = String.format("%.2f", maxValue * i / 4)
                val labelResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 10.sp, color = TextGray)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(
                        paddingLeft - labelResult.size.width - 6f,
                        y - labelResult.size.height / 2
                    ),
                    style = TextStyle(fontSize = 10.sp, color = TextGray)
                )
            }

            // Bars
            val barSlotWidth = plotWidth / data.size
            val barWidth = barSlotWidth * 0.5f
            data.forEachIndexed { index, point ->
                val centerX = paddingLeft + barSlotWidth * index + barSlotWidth / 2
                val barHeight = (point.amount / maxValue).toFloat() * plotHeight
                val top = paddingTop + plotHeight - barHeight

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(BlueLight, BluePrimary),
                        startY = top,
                        endY = paddingTop + plotHeight
                    ),
                    topLeft = Offset(centerX - barWidth / 2, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }

            // X labels
            data.forEachIndexed { index, point ->
                val centerX = paddingLeft + barSlotWidth * index + barSlotWidth / 2
                drawText(
                    textMeasurer = textMeasurer,
                    text = point.label,
                    topLeft = Offset(
                        centerX - 12.dp.toPx(),
                        size.height - paddingBottom + 6f
                    ),
                    style = TextStyle(fontSize = 10.sp, color = TextGray, textAlign = TextAlign.Center)
                )
            }

            // Tooltip
            if (selectedIndex in data.indices) {
                val point = data[selectedIndex]
                val centerX = paddingLeft + barSlotWidth * selectedIndex + barSlotWidth / 2
                val barHeight = (point.amount / maxValue).toFloat() * plotHeight
                val top = paddingTop + plotHeight - barHeight

                val line1 = "¥${String.format("%.2f", point.amount)}"
                val line2 = point.tooltipLabel

                val style1 = TextStyle(
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                val style2 = TextStyle(
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                val result1 = textMeasurer.measure(text = line1, style = style1)
                val result2 = textMeasurer.measure(text = line2, style = style2)

                val bubbleWidth = maxOf(result1.size.width, result2.size.width) + 20f
                val bubbleHeight = result1.size.height + result2.size.height + 14f
                val triangleHeight = 6f

                var bubbleLeft = centerX - bubbleWidth / 2
                if (bubbleLeft < 4f) bubbleLeft = 4f
                if (bubbleLeft + bubbleWidth > size.width - 4f) {
                    bubbleLeft = size.width - 4f - bubbleWidth
                }

                val bubbleTop = top - bubbleHeight - triangleHeight - 6f
                val bubbleBottom = bubbleTop + bubbleHeight

                drawRoundRect(
                    color = BluePrimary,
                    topLeft = Offset(bubbleLeft, bubbleTop),
                    size = Size(bubbleWidth, bubbleHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                val trianglePath = Path().apply {
                    moveTo(centerX, top - 6f)
                    lineTo(centerX - triangleHeight, bubbleBottom)
                    lineTo(centerX + triangleHeight, bubbleBottom)
                    close()
                }
                drawPath(trianglePath, color = BluePrimary)

                drawText(
                    textMeasurer = textMeasurer,
                    text = line1,
                    topLeft = Offset(
                        bubbleLeft + bubbleWidth / 2 - result1.size.width / 2,
                        bubbleTop + 6f
                    ),
                    style = style1
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = line2,
                    topLeft = Offset(
                        bubbleLeft + bubbleWidth / 2 - result2.size.width / 2,
                        bubbleTop + 8f + result1.size.height
                    ),
                    style = style2
                )
            }
        }
    }
}

@Composable
private fun EmptyChartState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                tint = TextGray.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = TextGray
            )
        }
    }
}

@Composable
private fun CategorySection(
    showIncome: Boolean,
    data: List<ReportViewModel.CategoryStat>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle(if (showIncome) "收入分类构成" else "支出分类构成")

            if (data.isNotEmpty()) {
                DonutChartWithLabels(
                    data = data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                data.forEachIndexed { index, stat ->
                    CategoryListItem(
                        rank = index + 1,
                        stat = stat,
                        onClick = { onItemClick(stat.category) }
                    )
                    if (index < data.lastIndex) {
                        HorizontalDivider(
                            color = DividerLight,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            } else {
                EmptyChartState("暂无${if (showIncome) "收入" else "支出"}数据")
            }
        }
    }
}

@Composable
private fun DonutChartWithLabels(
    data: List<ReportViewModel.CategoryStat>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = listOf(
        Color(0xFF2196F3), Color(0xFF42A5F5), Color(0xFF64B5F6),
        Color(0xFF90CAF9), Color(0xFF03A9F4), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A),
        Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722)
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = (size.width.coerceAtMost(size.height) / 2) * 0.65f
            val strokeWidth = radius * 0.35f

            var startAngle = -90f
            data.forEachIndexed { index, stat ->
                val sweepAngle = (stat.percentage * 360).toFloat()
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )

                if (data.size <= 6) {
                    val midAngle = startAngle + sweepAngle / 2
                    val midRad = Math.toRadians(midAngle.toDouble())
                    val labelRadius = radius + strokeWidth / 2 + 18.dp.toPx()
                    val labelX = centerX + (labelRadius * cos(midRad)).toFloat()
                    val labelY = centerY + (labelRadius * sin(midRad)).toFloat()

                    val text =
                        "${stat.category} ${String.format("%.2f", stat.percentage * 100)}%"
                    val textStyle = TextStyle(fontSize = 11.sp, color = TextGray)
                    val textResult = textMeasurer.measure(text = text, style = textStyle)

                    val textOffset = if (labelX < centerX) {
                        Offset(
                            labelX - textResult.size.width - 4f,
                            labelY - textResult.size.height / 2
                        )
                    } else {
                        Offset(labelX + 4f, labelY - textResult.size.height / 2)
                    }

                    val lineStartX =
                        centerX + ((radius + strokeWidth / 2) * cos(midRad)).toFloat()
                    val lineStartY =
                        centerY + ((radius + strokeWidth / 2) * sin(midRad)).toFloat()
                    val lineEndX = if (labelX < centerX) {
                        textOffset.x + textResult.size.width + 2f
                    } else {
                        textOffset.x - 2f
                    }
                    val lineEndY = labelY

                    drawLine(
                        color = colors[index % colors.size],
                        start = Offset(lineStartX, lineStartY),
                        end = Offset(lineEndX, lineEndY),
                        strokeWidth = 1f
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = text,
                        topLeft = textOffset,
                        style = textStyle
                    )
                }

                startAngle += sweepAngle
            }
        }
    }
}

@Composable
private fun CategoryListItem(
    rank: Int,
    stat: ReportViewModel.CategoryStat,
    onClick: () -> Unit = {}
) {
    val colors = listOf(
        Color(0xFF2196F3), Color(0xFF42A5F5), Color(0xFF64B5F6),
        Color(0xFF90CAF9), Color(0xFF03A9F4), Color(0xFF00BCD4),
        Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A),
        Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722)
    )
    val color = colors[(rank - 1) % colors.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "$rank",
            fontSize = 13.sp,
            color = TextGray,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stat.category.take(1),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stat.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stat.count}笔",
                    fontSize = 11.sp,
                    color = TextGray,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Custom progress bar to avoid Material3 stop-indicator dot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DividerLight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(stat.percentage.toFloat())
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
        Text(
            text = "${String.format("%.1f", stat.percentage * 100)}%",
            fontSize = 12.sp,
            color = TextGray,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
        Text(
            text = "¥${String.format("%.2f", stat.amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextGray,
            modifier = Modifier.size(16.dp)
        )
    }
}
