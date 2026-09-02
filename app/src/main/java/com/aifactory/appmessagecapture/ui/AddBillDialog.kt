@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aifactory.appmessagecapture.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
fun AddBillDialog(
    platforms: List<com.aifactory.appmessagecapture.data.PlatformAccountEntity>,
    onAdd: (BillEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedPlatformId by remember { mutableStateOf<Long?>(null) }
    var showPlatformPicker by remember { mutableStateOf(false) }
    val expenseCategories = ExpenseCategories.all
    val incomeCategories = IncomeCategories.all
    val categories = if (isIncome) incomeCategories else expenseCategories
    var selectedCategory by remember(isIncome) {
        mutableStateOf(categories.first())
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Frosted glass: translucent surface lets the animated ambient
        // background glow show through, highlight keeps the glass look.
        containerColor = MaterialTheme.colorScheme.surface.copy(
            alpha = if (isDarkTheme()) 0.82f else 0.88f
        ),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Top light reflection (liquid glass highlight)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(glassHighlightBrush())
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
            ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = "添加账单",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Income / Expense toggle
                PillToggle(
                    options = listOf("支出" to ExpenseRed, "收入" to IncomeGreen),
                    selectedIndex = if (isIncome) 1 else 0,
                    onSelect = { isIncome = it == 1; selectedCategory = categories.first() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = title,
                     colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    onValueChange = { title = it },
                    label = { Text("标题 (如: 晚餐)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = amountText,
                     colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    onValueChange = { amountText = it },
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Date picker row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "选择日期",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "日期",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val dateLabel = when (selectedDate) {
                        today -> "今天"
                        today.minusDays(1) -> "昨天"
                        today.plusDays(1) -> "明天"
                        else -> selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日"))
                    }
                    val weekDay = selectedDate.format(DateTimeFormatter.ofPattern(" EEE"))
                    Text(
                        text = "$dateLabel$weekDay",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedDate != today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 平台选择行（支出=扣款平台，收入=存入平台），不选则为待对账
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showPlatformPicker = true }
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
                        text = if (isIncome) "存入平台" else "扣款平台",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val platformLabel = selectedPlatformId?.let { id ->
                        platforms.firstOrNull { it.id == id }?.name
                    } ?: "未选择（待对账）"
                    Text(
                        text = platformLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedPlatformId != null)
                            MaterialTheme.colorScheme.onSurface
                        else ExpenseRed
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category grid using LazyVerticalGrid for perfect 4-column layout
                Text(
                    text = "选择分类",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 分类网格：外层已是 verticalScroll，改用普通分行布局避免
                // 嵌套同向滚动的手势冲突（条目固定且少，无需 lazy）。
                // 支出 12 项 3 行 / 收入 8 项 2 行，切换时高度变化交给
                // animateContentSize 平滑过渡，避免底部面板整体跳动。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 500f
                            )
                        )
                ) {
                    categories.chunked(4).forEach { rowCategories ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowCategories.forEach { cat ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryGridItem(
                                        label = cat,
                                        isSelected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat }
                                    )
                                }
                            }
                            // 末行不足4个时补齐占位保持等宽
                            repeat(4 - rowCategories.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
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

                    SoftButton(
                        text = "添加",
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                val ts = if (selectedDate == today) {
                                    System.currentTimeMillis()
                                } else {
                                    selectedDate
                                        .atTime(12, 0)
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()
                                }
                                onAdd(
                                    BillEntity(
                                        amount = amt,
                                        appName = if (isIncome) "手动记账-收入" else "手动记账-支出",
                                        packageName = "",
                                        title = title,
                                        category = selectedCategory,
                                        isIncome = isIncome,
                                        timestamp = ts,
                                        platformAccountId = selectedPlatformId
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val todayMillis = remember {
            LocalDate.now()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            modifier = Modifier.border(glassBorder(), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface.copy(
                    alpha = if (isDarkTheme()) 0.90f else 0.93f
                )
            ),
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Platform picker dialog
    if (showPlatformPicker) {
        PlatformPickerDialog(
            platforms = platforms,
            selectedId = selectedPlatformId,
            onSelect = { id ->
                selectedPlatformId = id
                showPlatformPicker = false
            },
            onDismiss = { showPlatformPicker = false }
        )
    }
}
