@file:OptIn(ExperimentalMaterial3Api::class)

package com.aifactory.appmessagecapture.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

import com.aifactory.appmessagecapture.ui.components.PillToggle
import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.gradientBrush
import com.aifactory.appmessagecapture.ui.getCategoryColor
import com.aifactory.appmessagecapture.ui.getCategoryIconRes
import com.aifactory.appmessagecapture.ui.theme.ComponentGap
import com.aifactory.appmessagecapture.utils.PlatformPackages
import com.aifactory.appmessagecapture.utils.rememberAppIcon
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import com.aifactory.appmessagecapture.ui.theme.ReportBlue
import com.aifactory.appmessagecapture.ui.theme.ReportBlueLight
import kotlinx.coroutines.launch

// ============================================================
// Theme-aware report palette: light/dark via CompositionLocal
// ============================================================

private data class ReportColors(
    val cardBg: Color,
    val bgGray: Color,
    val textDark: Color,
    val textGray: Color,
    val divider: Color,
    val neutralGray: Color
)

private val LocalReportColors = compositionLocalOf {
    ReportColors(
        cardBg = Color(0xFFFFFFFF),
        bgGray = Color(0xFFF6F8F7),
        textDark = Color(0xFF2A3833),
        textGray = Color(0xFF8A9B96),
        divider = Color(0xFFE6EDEA),
        neutralGray = Color(0xFFE7EEEA)
    )
}

@Composable
private fun rememberReportColors(): ReportColors {
    val scheme = MaterialTheme.colorScheme
    return ReportColors(
        cardBg = scheme.surface,
        bgGray = scheme.background,
        textDark = scheme.onSurface,
        textGray = scheme.onSurfaceVariant,
        divider = scheme.outline.copy(alpha = 0.35f),
        neutralGray = scheme.surfaceVariant
    )
}

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
    // 平台构成点击进入的平台明细（按平台名过滤账单）
    var selectedPlatform by remember { mutableStateOf<String?>(null) }

    if (selectedCategory != null) {
        CategoryDetailScreen(
            category = selectedCategory!!,
            isIncome = showIncome,
            startTime = uiState.periodStartMillis,
            endTime = uiState.periodEndMillis,
            onBack = { selectedCategory = null }
        )
        return
    }

    if (selectedPlatform != null) {
        CategoryDetailScreen(
            category = selectedPlatform!!,
            platformName = selectedPlatform,
            isIncome = showIncome,
            startTime = uiState.periodStartMillis,
            endTime = uiState.periodEndMillis,
            onBack = { selectedPlatform = null }
        )
        return
    }

    var showCustomRangePicker by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        onBack()
    }

    if (showCustomRangePicker) {
        CustomRangePickerDialog(
            initialStart = viewModel.customRange.value?.start,
            initialEnd = viewModel.customRange.value?.end,
            onDismiss = { showCustomRangePicker = false },
            onConfirm = { start, end ->
                viewModel.setCustomRange(start, end)
                showCustomRangePicker = false
            }
        )
    }

    CompositionLocalProvider(LocalReportColors provides rememberReportColors()) {
        // 背景由 MainApp 根布局的 AmbientBackground 提供（此页原来又叠了一层
        // 不透明底色+一套无限动画背景，同屏双倍动画开销）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("收支报表", fontWeight = FontWeight.Bold, color = LocalReportColors.current.textDark) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = LocalReportColors.current.textDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
            ) {
                // Period tabs：与记账界面同款 PillToggle（同高度/同边距），
                // 紧贴标题栏，不再额外留 12dp 纵向空白
                PillToggle(
                    options = listOf(
                        "周报" to MaterialTheme.colorScheme.primary,
                        "月报" to MaterialTheme.colorScheme.primary,
                        "年报" to MaterialTheme.colorScheme.primary,
                        "自定义" to MaterialTheme.colorScheme.primary
                    ),
                    selectedIndex = when (periodType) {
                        ReportViewModel.PeriodType.WEEK -> 0
                        ReportViewModel.PeriodType.MONTH -> 1
                        ReportViewModel.PeriodType.YEAR -> 2
                        ReportViewModel.PeriodType.CUSTOM -> 3
                    },
                    onSelect = { index ->
                        viewModel.setPeriodType(
                            when (index) {
                                0 -> ReportViewModel.PeriodType.WEEK
                                1 -> ReportViewModel.PeriodType.MONTH
                                2 -> ReportViewModel.PeriodType.YEAR
                                else -> ReportViewModel.PeriodType.CUSTOM
                            }
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                // 自定义时间段的选择入口只有下方 DateNavigation 一处：
                // 「2026.07.15~07.17（点击选择）」既展示当前区间又可点击打开选择器

                // 报表主体：切换周期类型时轻微淡入淡出，与 PillToggle 滑块动画配合
                Crossfade(
                    targetState = periodType,
                    animationSpec = tween(100, easing = FastOutSlowInEasing),
                    label = "reportBody"
                ) { bodyType ->
                    Column {
                Spacer(modifier = Modifier.height(ComponentGap))

                // Date nav + income/expense toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateNavigation(
                        periodType = bodyType,
                        label = uiState.periodLabel,
                        currentYear = uiState.currentYear,
                        currentMonth = uiState.currentMonth,
                        canGoNext = currentOffset < 0,
                        onPrev = { viewModel.prevPeriod() },
                        onNext = { viewModel.nextPeriod() },
                        onSelectMonth = { y, m -> viewModel.setMonth(y, m) },
                        onSelectYear = { y -> viewModel.setYear(y) },
                        onSelectCustomRange = { showCustomRangePicker = true }
                    )
                    IncomeExpenseToggle(
                        showIncome,
                        { viewModel.toggleShowIncome() }
                    )
                }

                Spacer(modifier = Modifier.height(ComponentGap))

                // Summary cards
                SummaryCards(
                    periodType = bodyType,
                    showIncome = showIncome,
                    periodTotal = uiState.periodTotal,
                    dailyAvg = uiState.dailyAvg,
                    prevDiff = uiState.prevDiff,
                    balance = uiState.balance,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(ComponentGap))

                // Trend line chart
                TrendLineChartSection(
                    periodType = bodyType,
                    showIncome = showIncome,
                    data = uiState.trendData,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Bar chart（自定义时间段无上一周期对比，隐藏）
                if (uiState.barData.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(ComponentGap))
                    TrendBarChartSection(
                        showIncome = showIncome,
                        data = uiState.barData,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(ComponentGap))

                // Category breakdown
                CategorySection(
                    showIncome = showIncome,
                    data = uiState.categoryData,
                    onItemClick = { category -> selectedCategory = category },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Platform breakdown（与分类构成一样可点击查看明细账单）
                if (uiState.platformData.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(ComponentGap))
                    CategorySection(
                        showIncome = showIncome,
                        data = uiState.platformData,
                        title = if (showIncome) "存入平台构成" else "平台构成",
                        onItemClick = { platformName -> selectedPlatform = platformName },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        isPlatform = true
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
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
    onSelectYear: (Int) -> Unit,
    onSelectCustomRange: () -> Unit = {}
) {
    when (periodType) {
        ReportViewModel.PeriodType.CUSTOM -> {
            Text(
                text = "$label（点击选择）",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectCustomRange() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        ReportViewModel.PeriodType.WEEK -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一周期", tint = LocalReportColors.current.textGray)
                }
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = LocalReportColors.current.textDark,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 4.dp)
                )
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp), enabled = canGoNext) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一周期", tint = if (canGoNext) LocalReportColors.current.textGray else LocalReportColors.current.textGray.copy(alpha = 0.3f))
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
                        Text("${currentYear}年", fontSize = 14.sp, color = LocalReportColors.current.textDark, fontWeight = FontWeight.Medium)
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = LocalReportColors.current.textGray,
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
                        Text(String.format("%02d月", currentMonth), fontSize = 14.sp, color = LocalReportColors.current.textDark, fontWeight = FontWeight.Medium)
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = LocalReportColors.current.textGray,
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
                    Text(label, fontSize = 14.sp, color = LocalReportColors.current.textDark, fontWeight = FontWeight.Medium)
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = LocalReportColors.current.textGray,
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
private fun IncomeExpenseToggle(
    showIncome: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("支出" to ExpenseRed, "收入" to IncomeGreen)
    var itemLefts by remember { mutableStateOf(FloatArray(options.size)) }
    var itemWidths by remember { mutableStateOf(IntArray(options.size)) }
    var measured by remember { mutableStateOf(false) }
    val sliderLeft = remember { Animatable(0f) }
    val sliderWidth = remember { Animatable(0f) }
    val selectedIndex = if (showIncome) 1 else 0

    LaunchedEffect(itemLefts, itemWidths, selectedIndex) {
        if (!measured) return@LaunchedEffect
        val targetLeft = itemLefts[selectedIndex]
        val targetWidth = itemWidths[selectedIndex].toFloat()
        if (sliderWidth.value == 0f) {
            sliderLeft.snapTo(targetLeft)
            sliderWidth.snapTo(targetWidth)
        } else {
            launch {
                sliderLeft.animateTo(
                    targetLeft,
                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
                )
            }
            sliderWidth.animateTo(
                targetWidth,
                spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh)
            )
        }
    }

    val selectedBrush = gradientBrush(options[selectedIndex].second, alpha = 0.92f)

    // 紧凑自适应小胶囊：字号比周期选择框（labelLarge 14sp）小一级（13sp，同记账页分类 chip），
    // 圆角与记账页选择框同级（16dp），高度压到约 30dp；选中胶囊为滑动滑块
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LocalReportColors.current.neutralGray.copy(alpha = 0.35f))
            .border(glassBorder(), RoundedCornerShape(16.dp))
            .drawBehind {
                if (!measured || sliderWidth.value <= 0f) return@drawBehind
                val pad = 2.dp.toPx()
                drawRoundRect(
                    brush = selectedBrush,
                    topLeft = Offset(pad + sliderLeft.value, pad),
                    size = Size(sliderWidth.value, this.size.height - pad * 2),
                    cornerRadius = CornerRadius(14.dp.toPx())
                )
            }
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, (label, _) ->
            val selected = selectedIndex == index
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coords ->
                        val left = coords.positionInParent().x
                        val width = coords.size.width
                        if (itemLefts[index] != left || itemWidths[index] != width) {
                            val newLefts = itemLefts.copyOf(); newLefts[index] = left
                            val newWidths = itemWidths.copyOf(); newWidths[index] = width
                            itemLefts = newLefts
                            itemWidths = newWidths
                            measured = true
                        }
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { if (!selected) onToggle() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) Color.White else LocalReportColors.current.textGray
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
        ReportViewModel.PeriodType.CUSTOM ->
            Triple("时段${typeLabel}（元）", "日均${typeLabel}（元）", "比上一时段（元）")
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ComponentGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ComponentGap)) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = totalLabel,
                value = "${String.format("%.2f", periodTotal)}",
                valueColor = LocalReportColors.current.textDark,
                leftBorderColor = MaterialTheme.colorScheme.secondary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = avgLabel,
                value = "${String.format("%.2f", dailyAvg)}",
                valueColor = LocalReportColors.current.textDark,
                leftBorderColor = MaterialTheme.colorScheme.secondary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ComponentGap)) {
            val diffColor = if (prevDiff >= 0) IncomeGreen else ExpenseRed
            val diffSign = if (prevDiff >= 0) "+" else ""
            StatCard(
                modifier = Modifier.weight(1f),
                title = diffLabel,
                value = "$diffSign${String.format("%.2f", prevDiff)}",
                valueColor = diffColor,
                leftBorderColor = MaterialTheme.colorScheme.secondary
            )
            val balanceColor = if (balance >= 0) IncomeGreen else ExpenseRed
            val balanceSign = if (balance >= 0) "+" else ""
            StatCard(
                modifier = Modifier.weight(1f),
                title = "收支结余（元）",
                value = "$balanceSign${String.format("%.2f", balance)}",
                valueColor = balanceColor,
                leftBorderColor = MaterialTheme.colorScheme.secondary
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
    SoftCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = LocalReportColors.current.cardBg
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
                    color = LocalReportColors.current.textGray,
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
        color = LocalReportColors.current.textDark,
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
        ReportViewModel.PeriodType.CUSTOM -> "时间段趋势"
    }
    val typeLabel = if (showIncome) "收入" else "支出"

    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = LocalReportColors.current.cardBg,
        contentPadding = 16.dp
    ) {
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

@Composable
private fun TrendLineChart(
    data: List<ReportViewModel.TrendPoint>,
    typeLabel: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()
    val colors = LocalReportColors.current

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
                    color = colors.divider,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 1f
                )
                val label = String.format("%.2f", maxValue * i / 4)
                val labelResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 10.sp, color = colors.textGray)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(
                        paddingLeft - labelResult.size.width - 6f,
                        y - labelResult.size.height / 2
                    ),
                    style = TextStyle(fontSize = 10.sp, color = colors.textGray)
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
                        style = TextStyle(fontSize = 10.sp, color = colors.textGray, textAlign = TextAlign.Center)
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
                        colors = listOf(ReportBlue.copy(alpha = 0.2f), Color.Transparent),
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
                    color = ReportBlue,
                    style = Stroke(width = 2.5f)
                )
            }

            // Points
            points.forEachIndexed { index, point ->
                val radius = if (index == selectedIndex) 6f else 3.5f
                drawCircle(color = Color.White, radius = radius + 2f, center = point)
                drawCircle(color = ReportBlue, radius = radius, center = point)
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
                    color = ReportBlue,
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
                drawPath(trianglePath, color = ReportBlue)

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
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = LocalReportColors.current.cardBg,
        contentPadding = 16.dp
    ) {
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

@Composable
private fun TrendBarChart(
    data: List<ReportViewModel.BarPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()
    val colors = LocalReportColors.current

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
                    color = colors.divider,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 1f
                )
                val label = String.format("%.2f", maxValue * i / 4)
                val labelResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 10.sp, color = colors.textGray)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(
                        paddingLeft - labelResult.size.width - 6f,
                        y - labelResult.size.height / 2
                    ),
                    style = TextStyle(fontSize = 10.sp, color = colors.textGray)
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
                        colors = listOf(ReportBlueLight, ReportBlue),
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
                    style = TextStyle(fontSize = 10.sp, color = colors.textGray, textAlign = TextAlign.Center)
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
                    color = ReportBlue,
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
                drawPath(trianglePath, color = ReportBlue)

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
                tint = LocalReportColors.current.textGray.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = LocalReportColors.current.textGray
            )
        }
    }
}

@Composable
private fun CategorySection(
    showIncome: Boolean,
    data: List<ReportViewModel.CategoryStat>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    isPlatform: Boolean = false
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = LocalReportColors.current.cardBg,
        contentPadding = 16.dp
    ) {
        SectionTitle(title ?: if (showIncome) "收入分类构成" else "支出分类构成")

        if (data.isNotEmpty()) {
            DonutChartWithLabels(
                data = data,
                isPlatform = isPlatform,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(ComponentGap))
            data.forEachIndexed { index, stat ->
                CategoryListItem(
                    rank = index + 1,
                    stat = stat,
                    showIncome = showIncome,
                    isPlatform = isPlatform,
                    onClick = { onItemClick(stat.category) }
                )
                if (index < data.lastIndex) {
                    HorizontalDivider(
                        color = LocalReportColors.current.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = ComponentGap / 2)
                    )
                }
            }
        } else {
            EmptyChartState("暂无${if (showIncome) "收入" else "支出"}数据")
        }
    }
}

@Composable
private fun DonutChartWithLabels(
    data: List<ReportViewModel.CategoryStat>,
    modifier: Modifier = Modifier,
    isPlatform: Boolean = false
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = LocalReportColors.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val radius = (size.width.coerceAtMost(size.height) / 2) * 0.65f
            val strokeWidth = radius * 0.35f

            var startAngle = -90f
            data.forEachIndexed { index, stat ->
                val sweepAngle = (stat.percentage * 360).toFloat()
                val arcColor = if (isPlatform)
                    com.aifactory.appmessagecapture.ui.theme.PlatformColors.colorForPlatformName(stat.category)
                else
                    getCategoryColor(stat.category)
                drawArc(
                    color = arcColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )

                if (data.size <= 6 || stat.percentage >= 0.05) {
                    val midAngle = startAngle + sweepAngle / 2
                    val midRad = Math.toRadians(midAngle.toDouble())
                    val labelRadius = radius + strokeWidth / 2 + 18.dp.toPx()
                    val labelX = centerX + (labelRadius * cos(midRad)).toFloat()
                    val labelY = centerY + (labelRadius * sin(midRad)).toFloat()

                    val text = "${stat.category} ${String.format("%.1f", stat.percentage * 100)}%"
                    val textStyle = TextStyle(fontSize = 10.sp, color = colors.textGray)
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
                        color = arcColor,
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
    showIncome: Boolean = false,
    isPlatform: Boolean = false,
    onClick: () -> Unit = {}
) {
    val color = if (isPlatform)
        com.aifactory.appmessagecapture.ui.theme.PlatformColors.colorForPlatformName(stat.category)
    else
        getCategoryColor(stat.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            fontSize = 13.sp,
            color = LocalReportColors.current.textGray,
            modifier = Modifier.width(20.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            if (isPlatform) {
                // 平台构成：优先真实 App 图标（与平台账户页一致），回退首文字
                val appIcon = rememberAppIcon(
                    PlatformPackages.packageForName(stat.category) ?: "",
                    sizeDp = 36.dp
                ).value
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = stat.category,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = stat.category.take(1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            } else {
                val iconRes = getCategoryIconRes(stat.category)
                if (iconRes != 0) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = stat.category,
                        modifier = Modifier.size(18.dp),
                        tint = color
                    )
                } else {
                    Text(
                        text = stat.category.take(1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stat.category,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = LocalReportColors.current.textDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stat.count}笔",
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    color = LocalReportColors.current.textGray,
                    maxLines = 1
                )
            }
            stat.prevAmount?.let { prev ->
                if (prev > 0 && stat.amount != prev) {
                    val deltaPct = (stat.amount - prev) / prev * 100
                    Text(
                        text = "较上期 %+.1f%%".format(deltaPct),
                        fontSize = 9.sp,
                        lineHeight = 12.sp,
                        color = if ((deltaPct > 0) == showIncome) IncomeGreen else ExpenseRed
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(LocalReportColors.current.divider)
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
//        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${String.format("%.1f", stat.percentage * 100)}%",
            fontSize = 12.sp,
            color = LocalReportColors.current.textGray,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "¥${String.format("%.2f", stat.amount)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = LocalReportColors.current.textDark,
            modifier = Modifier.width(85.dp),
            textAlign = TextAlign.End
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = LocalReportColors.current.textGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * 自定义时间段选择弹窗：自绘月历（最长 366 天）。
 * 不用 M3 DateRangePicker：窄宽度下 7 列会挤成 6 列、不支持年月快速跳转、
 * 区间底色按单元格绘制导致断开。自绘实现保证——
 * 7 列完整等宽；点「2026年7月」展开年/月快跳面板；选中区间的底色带
 * 从起点圆右缘一直连到终点圆左缘（起点右半格 + 中间整格 + 终点左半格拼接）。
 */
@Composable
private fun CustomRangePickerDialog(
    initialStart: LocalDate?,
    initialEnd: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    var displayMonth by remember { mutableStateOf(YearMonth.from(initialStart ?: today)) }
    var rangeStart by remember { mutableStateOf(initialStart) }
    var rangeEnd by remember { mutableStateOf(initialEnd) }
    var showJumpPanel by remember { mutableStateOf(false) }
    var jumpYear by remember { mutableStateOf(displayMonth.year) }

    val colors = LocalReportColors.current
    val bandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val accent = MaterialTheme.colorScheme.primary

    val rangeTooLong = rangeStart != null && rangeEnd != null &&
        (rangeEnd!!.toEpochDay() - rangeStart!!.toEpochDay()) > 365

    fun onDayClick(date: LocalDate) {
        val s = rangeStart
        when {
            // 未开始选，或上一段已选完 → 开始新一段
            s == null || rangeEnd != null -> { rangeStart = date; rangeEnd = null }
            date.isBefore(s) -> rangeStart = date
            else -> rangeEnd = date
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                // ── 年月导航：◀ 2026年7月 ▾ ▶ ──────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { displayMonth = displayMonth.minusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft, contentDescription = "上一月",
                            tint = colors.textDark, modifier = Modifier.size(22.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                jumpYear = displayMonth.year
                                showJumpPanel = !showJumpPanel
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${displayMonth.year}年${displayMonth.monthValue}月",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showJumpPanel) accent else colors.textDark
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown, contentDescription = "选择年月",
                            tint = colors.textGray, modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { displayMonth = displayMonth.plusMonths(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight, contentDescription = "下一月",
                            tint = colors.textDark, modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // ── 年/月快跳面板 ──────────────────────────────────────
                if (showJumpPanel) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val years = remember(today) { (2020..today.year + 1).toList() }
                    Text(
                        text = "年份",
                        fontSize = 11.sp,
                        color = colors.textGray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        years.forEach { year ->
                            val selected = year == jumpYear
                            Text(
                                text = "${year}年",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) accent else colors.textDark,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) accent.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { jumpYear = year }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "月份",
                        fontSize = 11.sp,
                        color = colors.textGray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    listOf(1..6, 7..12).forEach { months ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            months.forEach { month ->
                                val selected = jumpYear == displayMonth.year && month == displayMonth.monthValue
                                Text(
                                    text = "${month}月",
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) accent else colors.textDark,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 3.dp, vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) accent.copy(alpha = 0.15f) else colors.divider)
                                        .clickable {
                                            displayMonth = YearMonth.of(jumpYear, month)
                                            showJumpPanel = false
                                        }
                                        .padding(vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ── 星期表头：7 列完整等宽 ─────────────────────────────
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            color = colors.textGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

                // ── 月历网格：周一开头，区间底色连续拼接 ───────────────
                val leadingBlanks = displayMonth.atDay(1).dayOfWeek.value - 1
                val cells: List<LocalDate?> =
                    List(leadingBlanks) { null } + (1..displayMonth.lengthOfMonth()).map { displayMonth.atDay(it) }
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            RangePickerDayCell(
                                date = date,
                                rangeStart = rangeStart,
                                rangeEnd = rangeEnd,
                                today = today,
                                bandColor = bandColor,
                                accent = accent,
                                onClick = { date?.let { onDayClick(it) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── 已选摘要 + 操作 ────────────────────────────────────
                val fmt: (LocalDate) -> String = { "%d.%02d.%02d".format(it.year, it.monthValue, it.dayOfMonth) }
                val summary = when {
                    rangeStart != null && rangeEnd != null -> "已选 ${fmt(rangeStart!!)} ~ ${fmt(rangeEnd!!)}"
                    rangeStart != null -> "已选 ${fmt(rangeStart!!)}，请选择结束日期"
                    else -> "点选起止日期"
                }
                Text(
                    text = if (rangeTooLong) "最长可选 366 天，请重新选择" else summary,
                    fontSize = 12.sp,
                    color = if (rangeTooLong) ExpenseRed else colors.textGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(
                        enabled = rangeStart != null && rangeEnd != null && !rangeTooLong,
                        onClick = { onConfirm(rangeStart!!, rangeEnd!!) }
                    ) { Text("确定") }
                }
            }
        }
    }
}

/** 月历单格：端点实心圆 + 区间底色带（起点右半格/中间整格/终点左半格拼成连续色带） */
@Composable
private fun RangePickerDayCell(
    date: LocalDate?,
    rangeStart: LocalDate?,
    rangeEnd: LocalDate?,
    today: LocalDate,
    bandColor: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .then(if (date != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        date ?: return@Box
        val isStart = date == rangeStart
        val isEnd = date == rangeEnd
        val inRange = rangeStart != null && rangeEnd != null &&
            date.isAfter(rangeStart) && date.isBefore(rangeEnd)

        // 底色带高度略小于圆，视觉上像环绕日期的横带
        if (rangeStart != null && rangeEnd != null) {
            when {
                isStart && isEnd -> {}
                isStart -> Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.66f)
                        .fillMaxWidth(0.5f)
                        .background(bandColor)
                )
                isEnd -> Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight(0.66f)
                        .fillMaxWidth(0.5f)
                        .background(bandColor)
                )
                inRange -> Box(
                    modifier = Modifier
                        .fillMaxHeight(0.66f)
                        .fillMaxWidth()
                        .background(bandColor)
                )
            }
        }

        if (isStart || isEnd) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 13.sp,
                color = if (date == today) accent else LocalReportColors.current.textDark,
                fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
