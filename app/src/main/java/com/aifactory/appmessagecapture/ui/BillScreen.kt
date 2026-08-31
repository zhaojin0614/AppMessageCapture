@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aifactory.appmessagecapture.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.ui.components.PillToggle
import com.aifactory.appmessagecapture.ui.components.SoftButton
import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.components.SoftFab
import com.aifactory.appmessagecapture.ui.components.SoftGradientCard
import com.aifactory.appmessagecapture.ui.components.SwipeableItem
import com.aifactory.appmessagecapture.ui.components.SwipeableItemCoordinator
import com.aifactory.appmessagecapture.ui.components.GlassCompactDialog
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.glassFill
import com.aifactory.appmessagecapture.ui.components.glassHighlightBrush
import com.aifactory.appmessagecapture.ui.components.gradientBrush
import com.aifactory.appmessagecapture.ui.theme.ComponentGap
import com.aifactory.appmessagecapture.ui.components.isDarkTheme
import com.aifactory.appmessagecapture.ui.theme.CategoryBeauty
import com.aifactory.appmessagecapture.ui.theme.CategoryEducation
import com.aifactory.appmessagecapture.ui.theme.CategoryEntertainment
import com.aifactory.appmessagecapture.ui.theme.CategoryFinance
import com.aifactory.appmessagecapture.ui.theme.CategoryFood
import com.aifactory.appmessagecapture.ui.theme.CategoryInvestment
import com.aifactory.appmessagecapture.ui.theme.CategoryLiving
import com.aifactory.appmessagecapture.ui.theme.CategoryMedical
import com.aifactory.appmessagecapture.ui.theme.CategoryOtherIncome
import com.aifactory.appmessagecapture.ui.theme.CategoryParttime
import com.aifactory.appmessagecapture.ui.theme.CategoryPet
import com.aifactory.appmessagecapture.ui.theme.CategoryRedPacket
import com.aifactory.appmessagecapture.ui.theme.CategoryRefund
import com.aifactory.appmessagecapture.ui.theme.CategoryReimbursement
import com.aifactory.appmessagecapture.ui.theme.CategoryRental
import com.aifactory.appmessagecapture.ui.theme.CategorySalary
import com.aifactory.appmessagecapture.ui.theme.CategoryShopping
import com.aifactory.appmessagecapture.ui.theme.CategorySocial
import com.aifactory.appmessagecapture.ui.theme.CategoryTransport
import com.aifactory.appmessagecapture.ui.theme.CategoryUncategorized
import com.aifactory.appmessagecapture.R
import com.aifactory.appmessagecapture.service.PaymentScreenAccessibilityService
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.GradientExpenseEnd
import com.aifactory.appmessagecapture.ui.theme.GradientExpenseStart
import com.aifactory.appmessagecapture.ui.theme.GradientIncomeEnd
import com.aifactory.appmessagecapture.ui.theme.GradientIncomeStart
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = viewModel()
) {
    val bills by viewModel.bills.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalAccountBalance by viewModel.totalAccountBalance.collectAsState()
    val platforms by viewModel.platforms.collectAsState()
    val monthExpense by viewModel.monthExpense.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val monthIncome by viewModel.monthIncome.collectAsState()
    val expenseCount by viewModel.expenseCount.collectAsState()
    val incomeCount by viewModel.incomeCount.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var billToDelete by remember { mutableStateOf<BillEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) } // null/全部, 支出, 收入
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<BillEntity?>(null) }
    var showReport by remember { mutableStateOf(false) }
    // 待对账账单的平台分配弹窗
    var reconcileBill by remember { mutableStateOf<BillEntity?>(null) }


    val context = LocalContext.current

    // 设置页
    var showSettings by remember { mutableStateOf(false) }
    // 账单搜索
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    // 预算弹窗
    var showBudgetDialog by remember { mutableStateOf(false) }
    // 屏幕记账（无障碍）开关状态：从系统设置页返回时刷新角标
    var a11yResumeKey by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) a11yResumeKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val screenBillLive = remember(a11yResumeKey) { PaymentScreenAccessibilityService.isLive }

    if (showReport) {
        com.aifactory.appmessagecapture.ui.report.ReportScreen(
              onBack = { showReport = false }
        )
        return
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }


    val expenseCategories = listOf("全部") + ExpenseCategories.all
    val incomeCategories = listOf("全部") + IncomeCategories.all
    val allCategories = expenseCategories + incomeCategories.drop(1) // 去重后的全部

    val categories = remember(selectedType) {
        when (selectedType) {
            "支出" -> expenseCategories
            "收入" -> incomeCategories
            else -> allCategories
        }
    }
    val typeFilters = listOf("全部", "支出", "收入")

    // 筛选条件下沉到 ViewModel 直接查库（类型/分类变化即重查）：
    // 「全部」走时间窗口分页，筛选走条数分页，均为滚动加载更多
    LaunchedEffect(selectedType, selectedCategory) {
        viewModel.setTypeFilter(selectedType)
        viewModel.setCategoryFilter(selectedCategory)
    }

    // Pull-down stats panel
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(0f) }
    val maxPullOffsetPx = with(LocalDensity.current) { 80.dp.toPx() }

    // Load more when scrolling near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 2 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0 && listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0 &&
                    pullOffset.value < maxPullOffsetPx
                ) {
                    scope.launch {
                        pullOffset.snapTo(
                            (pullOffset.value + delta).coerceAtMost(maxPullOffsetPx)
                        )
                    }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && pullOffset.value > 0f) {
            pullOffset.animateTo(0f, animationSpec = tween(250))
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if ((listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) && pullOffset.value > 0f) {
            pullOffset.animateTo(0f, animationSpec = tween(200))
        }
    }

    // Pressing back during selection mode exits selection, not the app
    BackHandler(enabled = isSelectionMode) {
        viewModel.exitSelectionMode()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        if (isSelectionMode) {
                            Text(
                                text = "已选择 ${selectedIds.size} 项",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        } else {
                            Text(
                                text = "智能记账",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        if (isSelectionMode) {
                            TextButton(onClick = {
                                val visibleIds = bills.map { it.id }
                                if (selectedIds.containsAll(visibleIds)) {
                                    viewModel.exitSelectionMode()
                                } else {
                                    viewModel.selectAll(visibleIds)
                                }
                            }) {
                                Text(
                                    text = if (selectedIds.containsAll(bills.map { it.id })) "取消全选" else "全选",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { showDeleteSelectedDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除选中",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(onClick = { viewModel.exitSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "取消",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(onClick = { showReport = true }) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "报表",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                showSearch = !showSearch
                                if (!showSearch) {
                                    searchText = ""
                                    viewModel.setSearchQuery("")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "搜索账单",
                                    tint = if (showSearch) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    SoftFab(
                        icon = Icons.Default.Add,
                        contentDescription = "添加账单",
                        onClick = { showAddDialog = true },
                        modifier = Modifier.padding(bottom = 88.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Pull-down stats panel
                BillPullDownStatsPanel(
                    pullOffset = pullOffset.value,
                    maxPullOffset = maxPullOffsetPx,
                    expenseCount = expenseCount,
                    incomeCount = incomeCount,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    totalAccountBalance = totalAccountBalance
                )
    
                // Income / Expense Summary Cards
                IncomeExpenseSummary(
                    monthExpense = monthExpense,
                    monthIncome = monthIncome
                )

                Spacer(modifier = Modifier.height(ComponentGap))
    
                // Type Filter (全部/支出/收入) — equal-width pill toggle
                PillToggle(
                    options = listOf(
                        "全部" to MaterialTheme.colorScheme.primary,
                        "支出" to ExpenseRed,
                        "收入" to IncomeGreen
                    ),
                    selectedIndex = when (selectedType) {
                        "支出" -> 1
                        "收入" -> 2
                        else -> 0
                    },
                    onSelect = { index ->
                        selectedType = when (index) {
                            1 -> "支出"
                            2 -> "收入"
                            else -> null
                        }
                        // 类型切换后分类列表整组变化，旧的分类选择必然失配
                        // （如残留支出分类时切到收入 → 列表恒空），重置为全部
                        selectedCategory = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
    
                Spacer(modifier = Modifier.height(ComponentGap))
    
                // Category Filter Chips
                // padding 必须放在 horizontalScroll 之后：滚动容器会在自身边界裁剪
                // 内容，第一颗 chip 的描边/阴影贴着边界会被切出一条平边
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat || (selectedCategory == null && cat == "全部")
                        CategoryChip(
                            label = cat,
                            isSelected = isSelected,
                            onClick = { selectedCategory = if (cat == "全部") null else cat },
                            showIcon = true
                        )
                    }
                }
    
                Spacer(modifier = Modifier.height(ComponentGap))

                val totalBudget = budgets.firstOrNull { it.category == "" }?.amount ?: 0.0
                if (totalBudget > 0) {
                    val over = monthExpense > totalBudget
                    val ratio = (monthExpense / totalBudget).toFloat().coerceIn(0f, 1f)
                    // 与日卡片同款液态玻璃面板：glassFill + 顶部高光 + 渐变描边
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = glassFill(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showBudgetDialog = true }
                            .border(glassBorder(), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(glassHighlightBrush())
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = if (over) ExpenseRed else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (over) "本月已超预算 ¥%.2f".format(monthExpense - totalBudget)
                                    else "本月已支出 ¥%.2f / 预算 ¥%.2f".format(monthExpense, totalBudget),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%d%%".format((ratio * 100).toInt()),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            // 自绘进度条：M3 LinearProgressIndicator 默认带端点圆点与缺口，
                            // 观感割裂，这里用双层 Box 按比例填充
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(if (over) ExpenseRed else MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(ComponentGap))
                }

                if (showSearch) {
                    // 与日卡片同款液态玻璃面板；用 BasicTextField 自控高度，
                    // M3 TextField 有最小内容高度，强行压矮会裁掉文字
                    SoftCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        BasicTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                viewModel.setSearchQuery(it)
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (searchText.isEmpty()) {
                                            Text(
                                                text = "搜索标题 / 商户 / 分类 / 金额",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (searchText.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                searchText = ""
                                                viewModel.setSearchQuery("")
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "清除搜索",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(ComponentGap))
                }

                // Bill List
                if (bills.isEmpty()) {
                    EmptyBillState()
                } else {
                    val groupedBills = remember(bills) {
                        bills.groupBy {
                            Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        }.toList().sortedByDescending { it.first }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(ComponentGap)
                    ) {
                        items(
                            items = groupedBills,
                            key = { "day_${it.first}" }
                        ) { (date, dayBills) ->
                            DayGroupCard(
                                date = date,
                                bills = dayBills,
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                platforms = platforms,
                                onBillClick = { bill ->
                                    if (isSelectionMode) {
                                        viewModel.toggleSelection(bill.id)
                                    } else {
                                        billToEdit = bill
                                        showEditDialog = true
                                    }
                                },
                                onBillLongClick = { bill ->
                                    if (!isSelectionMode) {
                                        viewModel.enterSelectionMode(bill.id)
                                    }
                                },
                                onCategoryClick = { bill ->
                                    if (!isSelectionMode) {
                                        // 点分类标签 = 打开同一个综合编辑界面
                                        billToEdit = bill
                                        showEditDialog = true
                                    }
                                },
                                onReconcile = { bill ->
                                    if (!isSelectionMode) {
                                        reconcileBill = bill
                                    }
                                },
                                onDelete = { bill ->
                                    billToDelete = bill
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        BudgetDialog(viewModel = viewModel, onDismiss = { showBudgetDialog = false })
    }

    // Add bill dialog
    if (showAddDialog) {
        AddBillDialog(
            platforms = platforms,
            onAdd = { bill ->
                viewModel.addBill(bill)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Edit bill dialog：标题/金额/分类/平台一个界面改完（与通知「完善账单」弹窗同布局）
    if (showEditDialog && billToEdit != null) {
        val bill = billToEdit!!
        var editTitle by remember(bill.id) { mutableStateOf(bill.title) }
        var editAmount by remember(bill.id) {
            mutableStateOf(
                if (bill.amount % 1.0 == 0.0) bill.amount.toLong().toString()
                else bill.amount.toString()
            )
        }
        var editCategory by remember(bill.id) { mutableStateOf(bill.category) }
        var editPlatformId by remember(bill.id) { mutableStateOf(bill.platformAccountId) }

        val titleChanged = editTitle.isNotBlank() && editTitle.trim() != bill.title
        val amountChanged = (editAmount.toDoubleOrNull() ?: bill.amount) != bill.amount
        val categoryChanged = editCategory != bill.category
        val platformChanged = editPlatformId != bill.platformAccountId
        val hasChanges = titleChanged || amountChanged || categoryChanged || platformChanged

        GlassCompactDialog(
            onDismissRequest = {
                showEditDialog = false
                billToEdit = null
            },
            title = if (bill.isIncome) "编辑收入账单" else "编辑支出账单",
            text = {
                Column {
                    TextField(
                        value = editTitle,
                         colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                        onValueChange = { editTitle = it },
                        label = { Text("标题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = editAmount,
                         colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                        onValueChange = { newValue ->
                            // 仅允许数字与小数点
                            if (newValue.all { it.isDigit() || it == '.' }) {
                                editAmount = newValue
                            }
                        },
                        label = { Text("金额") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        prefix = { Text("¥") },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 双栏：左分类 / 右平台，独立滚动
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        BillEditColumn(title = "分类", modifier = Modifier.weight(1f)) {
                            val availableCategories = if (bill.isIncome) IncomeCategories.all
                            else ExpenseCategories.all
                            availableCategories.forEach { cat ->
                                BillEditChip(
                                    label = cat,
                                    sub = null,
                                    dotColor = getCategoryColor(cat),
                                    selected = cat == editCategory,
                                    onClick = { editCategory = cat }
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        BillEditColumn(
                            title = if (bill.isIncome) "存入平台" else "扣款平台",
                            modifier = Modifier.weight(1f)
                        ) {
                            BillEditChip(
                                label = "待对账",
                                sub = null,
                                dotColor = null,
                                selected = editPlatformId == null,
                                onClick = { editPlatformId = null }
                            )
                            platforms.forEach { account ->
                                BillEditChip(
                                    label = account.name,
                                    sub = "余额 ¥${String.format(Locale.getDefault(), "%.2f", account.balance)}",
                                    dotColor = null,
                                    selected = account.id == editPlatformId,
                                    onClick = { editPlatformId = account.id }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (titleChanged) viewModel.updateTitle(bill.id, editTitle.trim())
                        if (amountChanged) {
                            editAmount.toDoubleOrNull()?.let { viewModel.updateAmount(bill.id, it) }
                        }
                        if (categoryChanged) viewModel.updateCategory(bill.id, editCategory)
                        if (platformChanged) viewModel.reconcileBill(bill.id, editPlatformId)
                        showEditDialog = false
                        billToEdit = null
                    },
                    enabled = hasChanges
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    billToEdit = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // Single item delete confirmation
    if (showDeleteDialog && billToDelete != null) {
        GlassCompactDialog(
            onDismissRequest = {
                showDeleteDialog = false
                billToDelete = null
            },
            title = "删除账单",
            text = { Text("确定要删除这条账单记录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        SwipeableItemCoordinator.reset()
                        billToDelete?.let { viewModel.deleteBill(it.id) }
                        showDeleteDialog = false
                        billToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    billToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }

    // Multi-select delete confirmation
    if (showDeleteSelectedDialog) {
        GlassCompactDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = "删除选中账单",
            text = { Text("确定要删除选中的 ${selectedIds.size} 条账单记录吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteSelectedDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 对账弹窗：为账单分配扣款平台
    if (reconcileBill != null) {
        val bill = reconcileBill!!
        ReconcilePlatformDialog(
            bill = bill,
            platforms = platforms,
            onAssign = { platformId ->
                viewModel.reconcileBill(bill.id, platformId)
                reconcileBill = null
            },
            onDismiss = { reconcileBill = null }
        )
    }
}

@Composable
fun BillPullDownStatsPanel(
    pullOffset: Float,
    maxPullOffset: Float,
    expenseCount: Int,
    incomeCount: Int,
    totalExpense: Double,
    totalIncome: Double,
    totalAccountBalance: Double
) {
    if (pullOffset <= 0f) return

    val progress = (pullOffset / maxPullOffset).coerceIn(0f, 1f)
    val panelHeight = with(LocalDensity.current) { pullOffset.toDp() }
    val contentAlpha = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight),
        contentAlignment = Alignment.Center
    ) {
        if (contentAlpha > 0f) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BillStatItem(
                        value = expenseCount.toString(),
                        label = "支出笔数",
                        alpha = contentAlpha
                    )
                    BillStatItem(
                        value = "¥${String.format("%.2f", totalExpense)}",
                        label = "累计支出",
                        alpha = contentAlpha,
                        valueColor = ExpenseRed.copy(alpha = contentAlpha)
                    )
                    BillStatItem(
                        value = "¥${String.format("%.2f", totalIncome)}",
                        label = "累计收入",
                        alpha = contentAlpha,
                        valueColor = IncomeGreen.copy(alpha = contentAlpha)
                    )
                    BillStatItem(
                        value = incomeCount.toString(),
                        label = "收入笔数",
                        alpha = contentAlpha
                    )
                }
                // 账户总金额（所有平台余额之和）
                Text(
                    text = "账户总额 ¥${String.format("%.2f", totalAccountBalance)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseSummary(
    monthExpense: Double,
    monthIncome: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month Expense
            SoftGradientCard(
                modifier = Modifier.weight(1f).heightIn(min = 96.dp),
                brush = Brush.linearGradient(
                    colors = listOf(GradientExpenseStart, GradientExpenseEnd)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = 14.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "本月支出",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¥${String.format("%.2f", monthExpense)}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Month Income
            SoftGradientCard(
                modifier = Modifier.weight(1f).heightIn(min = 96.dp),
                brush = Brush.linearGradient(
                    colors = listOf(GradientIncomeStart, GradientIncomeEnd)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = 14.dp
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "本月收入",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¥${String.format("%.2f", monthIncome)}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BillStatItem(
    value: String,
    label: String,
    alpha: Float,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showIcon: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.92f) else glassFill(),
        shadowElevation = 0.dp,
        modifier = Modifier
            .clickable { onClick() }
            .border(glassBorder(), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcon) {
                val iconRes = getCategoryIconRes(label)
                if (iconRes != 0) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DayGroupCard(
    date: LocalDate,
    bills: List<BillEntity>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    platforms: List<com.aifactory.appmessagecapture.data.PlatformAccountEntity>,
    onBillClick: (BillEntity) -> Unit,
    onBillLongClick: (BillEntity) -> Unit,
    onCategoryClick: (BillEntity) -> Unit,
    onReconcile: (BillEntity) -> Unit,
    onDelete: (BillEntity) -> Unit
) {
    val dayExpense = remember(bills) { bills.filter { !it.isIncome }.sumOf { it.amount } }
    val dayIncome = remember(bills) { bills.filter { it.isIncome }.sumOf { it.amount } }
    // 平台ID -> 名称，用于在卡片上显示扣款平台
    val platformNameById = remember(platforms) {
        platforms.associate { it.id to it.name }
    }

    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            // Header: date + daily summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDayHeader(date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "支${String.format("%.2f", dayExpense)} 收${String.format("%.2f", dayIncome)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            Spacer(modifier = Modifier.height(4.dp))

            // Bills inside day card
            bills.forEachIndexed { index, bill ->
                // key 让卡片组合状态（图标缓存状态、滑动偏移）跟随账单本身：
                // 新账单插入列表头部时若不 key，Compose 会按位置复用旧槽位，
                // 下面的卡片会继承上一条账单的内部状态
                key(bill.id) {
                    SwipeableItem(
                        isSelectionMode = isSelectionMode,
                        onDelete = { onDelete(bill) },
                        itemKey = bill.id
                    ) {
                        BillCard(
                            bill = bill,
                            isSelected = selectedIds.contains(bill.id),
                            isSelectionMode = isSelectionMode,
                            platformName = bill.platformAccountId?.let { platformNameById[it] },
                            onClick = { onBillClick(bill) },
                            onLongClick = { onBillLongClick(bill) },
                            onCategoryClick = { onCategoryClick(bill) },
                            onReconcile = { onReconcile(bill) }
                        )
                    }
                }
                if (index < bills.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 50.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBillState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无账单",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(ComponentGap))
            Text(
                text = "当收到微信支付、美团等消费通知时，将自动记账",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}

private fun formatDayHeader(date: LocalDate): String {
    val now = LocalDate.now()
    val weekDays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    val dayOfWeek = weekDays[date.dayOfWeek.value - 1]
    val dayStr = String.format("%d.%02d.%02d", date.year, date.monthValue, date.dayOfMonth)
    return when (date) {
        now -> "$dayStr 今天"
        now.minusDays(1) -> "$dayStr 昨天"
        else -> "$dayStr $dayOfWeek"
    }
}


/**
 * Vertical grid item for category selection dialogs (AddBill / ChangeCategory).
 * Icon on top with a colored circle background, label below.
 * Uses Modifier.weight(1f) so 4 items fit perfectly in a row.
 */
@Composable
fun CategoryGridItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(label)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) categoryColor
                    else categoryColor.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconRes = getCategoryIconRes(label)
            if (iconRes != 0) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (isSelected) Color.White else categoryColor
                )
            }
        }
        Spacer(modifier = Modifier.height(ComponentGap))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 编辑弹窗的双栏单列：栏标题 + 纵向滚动选项（分类/平台共用） */
@Composable
private fun BillEditColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
        ) {
            content()
            Spacer(modifier = Modifier.height(ComponentGap))
        }
    }
}

/** 编辑弹窗的紧凑选项行：可选色点（分类）+ 名称 + 可选副文本（平台余额） */
@Composable
private fun BillEditChip(
    label: String,
    sub: String?,
    dotColor: Color?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sub != null) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
