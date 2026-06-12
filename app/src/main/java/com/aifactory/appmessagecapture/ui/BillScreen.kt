@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aifactory.appmessagecapture.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.ui.components.SwipeableItem
import com.aifactory.appmessagecapture.ui.components.SwipeableItemCoordinator
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
import com.aifactory.appmessagecapture.ui.theme.ExpenseRedLight
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import com.aifactory.appmessagecapture.ui.theme.IncomeGreenLight
import com.aifactory.appmessagecapture.ui.theme.PrimaryOrange
import com.aifactory.appmessagecapture.ui.theme.PrimaryOrangeDark
import com.aifactory.appmessagecapture.ui.theme.PrimaryOrangeLight
import com.aifactory.appmessagecapture.ui.theme.SecondaryPurple
import com.aifactory.appmessagecapture.ui.theme.SecondaryPurpleLight
import com.aifactory.appmessagecapture.ui.theme.SecondaryPurpleLighter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = viewModel()
) {
    val bills by viewModel.allBills.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val todayExpense by viewModel.todayExpense.collectAsState()
    val todayIncome by viewModel.todayIncome.collectAsState()
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

    if (showReport) {
        com.aifactory.appmessagecapture.ui.report.ReportScreen(
            onBack = { showReport = false }
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

    Scaffold(
        modifier = modifier,
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
                    containerColor = MaterialTheme.colorScheme.background
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
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryOrange,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加账单")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Pull-down stats panel
            BillPullDownStatsPanel(
                pullOffset = pullOffset.value,
                maxPullOffset = maxPullOffsetPx,
                expenseCount = expenseCount,
                incomeCount = incomeCount,
                totalExpense = totalExpense,
                totalIncome = totalIncome
            )

            // Income / Expense Summary Cards
            IncomeExpenseSummary(
                todayExpense = todayExpense,
                todayIncome = todayIncome
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Type Filter Chips (支出/收入/全部)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                typeFilters.forEach { type ->
                    val isSelected = selectedType == type || (selectedType == null && type == "全部")
                    val chipColor = when (type) {
                        "支出" -> ExpenseRed
                        "收入" -> IncomeGreen
                        else -> PrimaryOrange
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) chipColor else MaterialTheme.colorScheme.surface,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier.clickable { selectedType = if (type == "全部") null else type }
                    ) {
                        Text(
                            text = type,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

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
                    contentPadding = PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
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

    // Category picker dialog
    if (showCategoryPicker && categoryBillToEdit != null) {
        val bill = categoryBillToEdit!!
        val availableCategories = if (bill.isIncome) {
            IncomeCategories.all
        } else {
            ExpenseCategories.all
        }
        AlertDialog(
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
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                billToEdit = null
            },
            title = { Text("修改标题") },
            text = {
                TextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateTitle(bill.id, editTitle)
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
    }

    // Single item delete confirmation
    if (showDeleteDialog && billToDelete != null) {
        AlertDialog(
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
        AlertDialog(
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
}

@Composable
fun BillPullDownStatsPanel(
    pullOffset: Float,
    maxPullOffset: Float,
    expenseCount: Int,
    incomeCount: Int,
    totalExpense: Double,
    totalIncome: Double
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
        }
    }
}

@Composable
fun IncomeExpenseSummary(
    todayExpense: Double,
    todayIncome: Double
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
            // Today Expense
            BentoCard(
                modifier = Modifier.weight(1f).heightIn(min = 96.dp),
                brush = Brush.linearGradient(
                    colors = listOf(ExpenseRed, ExpenseRedLight)
                )
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
                            text = "今日支出",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¥${String.format("%.2f", todayExpense)}",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Today Income
            BentoCard(
                modifier = Modifier.weight(1f).heightIn(min = 96.dp),
                brush = Brush.linearGradient(
                    colors = listOf(IncomeGreen, IncomeGreenLight)
                )
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
                            text = "今日收入",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "¥${String.format("%.2f", todayIncome)}",
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
fun BentoCard(
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    backgroundColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = if (brush != null) {
                Modifier
                    .fillMaxWidth()
                    .background(brush, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            }
        ) {
            content()
        }
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
        color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = Modifier.clickable { onClick() }
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
fun MergedAppIcon(
    primaryPackage: String,
    secondaryPackage: String?,
    sizeDp: androidx.compose.ui.unit.Dp = 40.dp
) {
    val context = LocalContext.current
    val pxSize = with(LocalDensity.current) { sizeDp.toPx().toInt() }

    val primaryIcon = remember(primaryPackage, pxSize) {
        try {
            context.packageManager.getApplicationIcon(primaryPackage)
                ?.toBitmap(width = pxSize, height = pxSize)
                ?.asImageBitmap()
        } catch (_: Exception) { null }
    }
    val secondaryIcon = remember(secondaryPackage, pxSize) {
        if (secondaryPackage == null) return@remember null
        try {
            context.packageManager.getApplicationIcon(secondaryPackage)
                ?.toBitmap(width = pxSize, height = pxSize)
                ?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    Box(
        modifier = Modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        if (secondaryIcon != null && primaryIcon != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val s = size.width
                val iSize = s.toInt()

                // Primary icon (left/bottom triangle)
                val pathPrimary = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(s, 0f)
                    lineTo(0f, s)
                    close()
                }
                clipPath(pathPrimary) {
                    drawImage(
                        image = primaryIcon,
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(iSize, iSize)
                    )
                }

                // Secondary icon (right/top triangle)
                val pathSecondary = Path().apply {
                    moveTo(s, 0f)
                    lineTo(s, s)
                    lineTo(0f, s)
                    close()
                }
                clipPath(pathSecondary) {
                    drawImage(
                        image = secondaryIcon,
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(iSize, iSize)
                    )
                }

                // 45° diagonal separator line (top-right to bottom-left)
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(s, 0f),
                    end = Offset(0f, s),
                    strokeWidth = 2.5f
                )
            }
        } else if (primaryIcon != null) {
            Image(
                bitmap = primaryIcon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun BillCard(
    bill: BillEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hasMergedIcon = bill.secondaryPackageName != null
    val iconBitmap = remember(bill.packageName) {
        try {
            context.packageManager.getApplicationIcon(bill.packageName)
                ?.toBitmap(width = 192, height = 192)
                ?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        if (hasMergedIcon) {
            MergedAppIcon(
                primaryPackage = bill.packageName,
                secondaryPackage = bill.secondaryPackageName,
                sizeDp = 40.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (iconBitmap == null) PrimaryOrangeLight else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = bill.appName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrangeDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Row 1: App name + Category tag + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val displayAppName = if (bill.secondaryAppName != null) {
                        "${bill.appName} - ${bill.secondaryAppName}"
                    } else {
                        bill.appName
                    }
                    Text(
                        text = displayAppName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getCategoryColor(bill.category).copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onCategoryClick() }
                    ) {
                        Text(
                            text = bill.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = getCategoryColor(bill.category),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                val amountColor = if (bill.isIncome) IncomeGreen else ExpenseRed
                val amountPrefix = if (bill.isIncome) "+" else "-"
                Text(
                    text = "$amountPrefix¥${String.format("%.2f", bill.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            // Row 2: Title on left, Time on right (under amount)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bill.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = formatBillTimeOnly(bill.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DayGroupCard(
    date: LocalDate,
    bills: List<BillEntity>,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onBillClick: (BillEntity) -> Unit,
    onBillLongClick: (BillEntity) -> Unit,
    onCategoryClick: (BillEntity) -> Unit,
    onDelete: (BillEntity) -> Unit
) {
    val dayExpense = bills.filter { !it.isIncome }.sumOf { it.amount }
    val dayIncome = bills.filter { it.isIncome }.sumOf { it.amount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(4.dp))

            // Bills inside day card
            bills.forEachIndexed { index, bill ->
                SwipeableItem(
                    isSelectionMode = isSelectionMode,
                    onDelete = { onDelete(bill) },
                    itemKey = bill.id
                ) {
                    BillCard(
                        bill = bill,
                        isSelected = selectedIds.contains(bill.id),
                        isSelectionMode = isSelectionMode,
                        onClick = { onBillClick(bill) },
                        onLongClick = { onBillLongClick(bill) },
                        onCategoryClick = { onCategoryClick(bill) }
                    )
                }
                if (index < bills.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 50.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddBillDialog(
    onAdd: (BillEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    val expenseCategories = ExpenseCategories.all
    val incomeCategories = IncomeCategories.all
    val categories = if (isIncome) incomeCategories else expenseCategories
    var selectedCategory by remember(isIncome) {
        mutableStateOf(categories.first())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "添加账单",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Income / Expense toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(false to "支出", true to "收入").forEach { (income, label) ->
                        val isSelected = isIncome == income
                        val bgColor = if (isSelected) {
                            if (income) IncomeGreen else ExpenseRed
                        } else Color.Transparent
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { isIncome = income; selectedCategory = categories.first() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题 (如: 晚餐)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category grid using LazyVerticalGrid for perfect 4-column layout
                Text(
                    text = "选择分类",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 210.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(categories) { cat ->
                        CategoryGridItem(
                            label = cat,
                            isSelected = selectedCategory == cat,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }

                    val btnColor = if (isIncome) IncomeGreen else PrimaryOrange
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(btnColor)
                            .clickable {
                                val amt = amountText.toDoubleOrNull() ?: 0.0
                                if (title.isNotBlank() && amt > 0) {
                                    onAdd(
                                        BillEntity(
                                            amount = amt,
                                            appName = if (isIncome) "手动记账-收入" else "手动记账-支出",
                                            packageName = "",
                                            title = title,
                                            category = selectedCategory,
                                            isIncome = isIncome,
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "添加",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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
                color = PrimaryOrangeLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = PrimaryOrange.copy(alpha = 0.5f),
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

private fun getCategoryColor(category: String): Color {
    return when (category) {
        // 支出类别
        ExpenseCategories.FOOD -> CategoryFood
        ExpenseCategories.TRANSPORT -> CategoryTransport
        ExpenseCategories.SHOPPING -> CategoryShopping
        ExpenseCategories.ENTERTAINMENT -> CategoryEntertainment
        ExpenseCategories.LIVING -> CategoryLiving
        ExpenseCategories.MEDICAL -> CategoryMedical
        ExpenseCategories.EDUCATION -> CategoryEducation
        ExpenseCategories.SOCIAL -> CategorySocial
        ExpenseCategories.BEAUTY -> CategoryBeauty
        ExpenseCategories.PET -> CategoryPet
        ExpenseCategories.FINANCE -> CategoryFinance
        ExpenseCategories.OTHER -> CategoryUncategorized

        // 收入类别
        IncomeCategories.SALARY -> CategorySalary
        IncomeCategories.PARTTIME -> CategoryParttime
        IncomeCategories.INVESTMENT -> CategoryInvestment
        IncomeCategories.RENTAL -> CategoryRental
        IncomeCategories.REFUND -> CategoryRefund
        IncomeCategories.RED_PACKET -> CategoryRedPacket
        IncomeCategories.REIMBURSEMENT -> CategoryReimbursement
        IncomeCategories.OTHER -> CategoryOtherIncome

        else -> CategoryUncategorized
    }
}

private fun getCategoryIconRes(category: String): Int {
    return when (category) {
        // 支出类别
        ExpenseCategories.FOOD -> R.drawable.ic_category_food
        ExpenseCategories.TRANSPORT -> R.drawable.ic_category_transport
        ExpenseCategories.SHOPPING -> R.drawable.ic_category_shopping
        ExpenseCategories.ENTERTAINMENT -> R.drawable.ic_category_entertainment
        ExpenseCategories.LIVING -> R.drawable.ic_category_living
        ExpenseCategories.MEDICAL -> R.drawable.ic_category_medical
        ExpenseCategories.EDUCATION -> R.drawable.ic_category_education
        ExpenseCategories.SOCIAL -> R.drawable.ic_category_social
        ExpenseCategories.BEAUTY -> R.drawable.ic_category_beauty
        ExpenseCategories.PET -> R.drawable.ic_category_pet
        ExpenseCategories.FINANCE -> R.drawable.ic_category_finance
        ExpenseCategories.OTHER -> R.drawable.ic_category_other_expense

        // 收入类别
        IncomeCategories.SALARY -> R.drawable.ic_category_salary
        IncomeCategories.PARTTIME -> R.drawable.ic_category_parttime
        IncomeCategories.INVESTMENT -> R.drawable.ic_category_investment
        IncomeCategories.RENTAL -> R.drawable.ic_category_rental
        IncomeCategories.REFUND -> R.drawable.ic_category_refund
        IncomeCategories.RED_PACKET -> R.drawable.ic_category_redpacket
        IncomeCategories.REIMBURSEMENT -> R.drawable.ic_category_reimbursement
        IncomeCategories.OTHER -> R.drawable.ic_category_other_income

        else -> 0
    }
}

private fun formatBillTimeOnly(timestamp: Long): String {
    val zoned = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern("HH:mm").format(zoned)
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
