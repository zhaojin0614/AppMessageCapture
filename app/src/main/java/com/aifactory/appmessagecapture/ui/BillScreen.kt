@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aifactory.appmessagecapture.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.ui.components.PillToggle
import com.aifactory.appmessagecapture.ui.components.SoftButton
import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.components.SoftFab
import com.aifactory.appmessagecapture.ui.components.SoftGradientCard
import com.aifactory.appmessagecapture.ui.components.SwipeableItem
import com.aifactory.appmessagecapture.ui.components.SwipeableItemCoordinator
import com.aifactory.appmessagecapture.ui.components.GlassAlertDialog
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.glassFill
import com.aifactory.appmessagecapture.ui.components.glassHighlightBrush
import com.aifactory.appmessagecapture.ui.components.gradientBrush
import com.aifactory.appmessagecapture.ui.components.isDarkTheme
import com.aifactory.appmessagecapture.utils.rememberAppIcon
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
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.GradientExpenseEnd
import com.aifactory.appmessagecapture.ui.theme.GradientExpenseStart
import com.aifactory.appmessagecapture.ui.theme.GradientIncomeEnd
import com.aifactory.appmessagecapture.ui.theme.GradientIncomeStart
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.time.Instant
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
    val bills by viewModel.allBills.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalAccountBalance by viewModel.totalAccountBalance.collectAsState()
    val platforms by viewModel.platforms.collectAsState()
    val monthExpense by viewModel.monthExpense.collectAsState()
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
    var showCategoryPicker by remember { mutableStateOf(false) }
    var categoryBillToEdit by remember { mutableStateOf<BillEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<BillEntity?>(null) }
    var showReport by remember { mutableStateOf(false) }
    var showRecurringBills by remember { mutableStateOf(false) }
    var showPlatformAccounts by remember { mutableStateOf(false) }
    // 待对账账单的平台分配弹窗
    var reconcileBill by remember { mutableStateOf<BillEntity?>(null) }

    if (showReport) {
        com.aifactory.appmessagecapture.ui.report.ReportScreen(
              onBack = { showReport = false }
        )
        return
    }

    if (showRecurringBills) {
        RecurringBillScreen(
            onBack = { showRecurringBills = false },
            modifier = modifier
        )
        return
    }

    if (showPlatformAccounts) {
        PlatformAccountScreen(
            onBack = { showPlatformAccounts = false },
            modifier = modifier
        )
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

    val filteredBills = remember(selectedCategory, selectedType, bills) {
        bills.filter { bill ->
            val categoryMatch = selectedCategory == null || selectedCategory == "全部" || bill.category == selectedCategory
            val typeMatch = when (selectedType) {
                "支出" -> !bill.isIncome
                "收入" -> bill.isIncome
                else -> true
            }
            categoryMatch && typeMatch
        }
    }

    // 过滤结果为空时自动扩大历史窗口：列表默认只加载最近一周，收入等低频
    // 账单可能在更早的周里；而「滚动到底才加载更多」在空列表下永远不会触发，
    // 不扩窗的话收入/支出视图会恒为空。扩到库里再无更早账单为止。
    val currentFilteredBills by rememberUpdatedState(filteredBills)
    LaunchedEffect(Unit) {
        viewModel.hasEarlierBills.collect { hasEarlier ->
            if (hasEarlier && currentFilteredBills.isEmpty()) {
                viewModel.loadMoreWeeks()
            }
        }
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
            viewModel.loadMoreWeeks()
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
                                val visibleIds = filteredBills.map { it.id }
                                if (selectedIds.containsAll(visibleIds)) {
                                    viewModel.exitSelectionMode()
                                } else {
                                    viewModel.selectAll(visibleIds)
                                }
                            }) {
                                Text(
                                    text = if (selectedIds.containsAll(filteredBills.map { it.id })) "取消全选" else "全选",
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
                            IconButton(onClick = { showPlatformAccounts = true }) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = "账户管理",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showRecurringBills = true }) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "周期账单",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showReport = true }) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = "报表",
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
    
                Spacer(modifier = Modifier.height(8.dp))
    
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
    
                Spacer(modifier = Modifier.height(6.dp))
    
                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
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
    
                Spacer(modifier = Modifier.height(8.dp))
    
                // Bill List
                if (filteredBills.isEmpty()) {
                    EmptyBillState()
                } else {
                    val groupedBills = remember(filteredBills) {
                        filteredBills.groupBy {
                            Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                        }.toList().sortedByDescending { it.first }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nestedScrollConnection),
                        contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        categoryBillToEdit = bill
                                        showCategoryPicker = true
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

    // Category picker dialog
    if (showCategoryPicker && categoryBillToEdit != null) {
        val bill = categoryBillToEdit!!
        val availableCategories = if (bill.isIncome) {
            IncomeCategories.all
        } else {
            ExpenseCategories.all
        }
        GlassAlertDialog(
            onDismissRequest = {
                showCategoryPicker = false
                categoryBillToEdit = null
            },
            title = { Text(if (bill.isIncome) "修改收入分类" else "修改支出分类") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(availableCategories) { cat ->
                        CategoryGridItem(
                            label = cat,
                            isSelected = bill.category == cat,
                            onClick = {
                                viewModel.updateCategory(bill.id, cat)
                                showCategoryPicker = false
                                categoryBillToEdit = null
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showCategoryPicker = false
                    categoryBillToEdit = null
                }) {
                    Text("取消")
                }
            }
        )
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

    // Edit bill dialog
    if (showEditDialog && billToEdit != null) {
        val bill = billToEdit!!
        var editTitle by remember { mutableStateOf(bill.title) }
        var editAmount by remember {
            mutableStateOf(
                if (bill.amount % 1.0 == 0.0) bill.amount.toLong().toString()
                else bill.amount.toString()
            )
        }
        var showEditPlatformPicker by remember { mutableStateOf(false) }
        GlassAlertDialog(
            onDismissRequest = {
                showEditDialog = false
                billToEdit = null
            },
            title = { Text("编辑账单") },
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
                    Spacer(modifier = Modifier.height(10.dp))
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
                    Spacer(modifier = Modifier.height(12.dp))
                    // 平台修改入口
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showEditPlatformPicker = true }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (bill.isIncome) "存入平台" else "扣款平台",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        val currentPlatformName = bill.platformAccountId?.let { id ->
                            platforms.firstOrNull { it.id == id }?.name
                        }
                        Text(
                            text = currentPlatformName ?: "待对账",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (bill.platformAccountId != null)
                                MaterialTheme.colorScheme.onSurface
                            else ExpenseRed
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateTitle(bill.id, editTitle)
                        }
                        val newAmount = editAmount.toDoubleOrNull()
                        if (newAmount != null && newAmount > 0 && newAmount != bill.amount) {
                            viewModel.updateAmount(bill.id, newAmount)
                        }
                        showEditDialog = false
                        billToEdit = null
                    }
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

        // 编辑弹窗内的平台选择
        if (showEditPlatformPicker) {
            PlatformPickerDialog(
                platforms = platforms,
                selectedId = bill.platformAccountId,
                onSelect = { id ->
                    viewModel.reconcileBill(bill.id, id)
                    showEditPlatformPicker = false
                    // 更新本地引用以便 UI 即时反映
                    billToEdit = bill.copy(platformAccountId = id)
                },
                onDismiss = { showEditPlatformPicker = false }
            )
        }
    }

    // Single item delete confirmation
    if (showDeleteDialog && billToDelete != null) {
        GlassAlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                billToDelete = null
            },
            title = { Text("删除账单") },
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
        GlassAlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("删除选中账单") },
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
        shadowElevation = if (isSelected) 2.dp else 0.dp,
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
        Spacer(modifier = Modifier.height(6.dp))
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
            Spacer(modifier = Modifier.height(6.dp))
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
