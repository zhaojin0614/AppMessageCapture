package com.aifactory.appmessagecapture.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.data.RecurringBillEntity
import com.aifactory.appmessagecapture.data.RecurringFrequency
import com.aifactory.appmessagecapture.ui.components.SoftButton
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
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecurringBillViewModel = viewModel()
) {
    val recurringBills by viewModel.allRecurringBills.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var billToDelete by remember { mutableStateOf<RecurringBillEntity?>(null) }
    var billToEdit by remember { mutableStateOf<RecurringBillEntity?>(null) }

    // Handle system back button
    BackHandler(enabled = true) {
        onBack()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "周期账单",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加周期账单", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        if (recurringBills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无周期账单",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "点击右下角按钮添加固定支出",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(recurringBills, key = { it.id }) { bill ->
                    RecurringBillCard(
                        bill = bill,
                        onToggleActive = { viewModel.toggleActive(bill.id, !bill.isActive) },
                        onDelete = { billToDelete = bill },
                        onClick = { billToEdit = bill }
                    )
                }
            }
        }
    }

    // Add recurring bill dialog
    if (showAddDialog) {
        AddRecurringBillDialog(
            onAdd = { title, amount, category, isIncome, frequency, startDate ->
                viewModel.addRecurringBill(title, amount, category, isIncome, frequency, startDate)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Delete confirmation dialog
    billToDelete?.let { bill ->
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text("删除周期账单") },
            text = { Text("确定要删除「${bill.title}」的周期账单吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecurringBill(bill.id)
                    billToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Edit recurring bill dialog
    billToEdit?.let { bill ->
        EditRecurringBillDialog(
            bill = bill,
            onConfirm = { updatedBill ->
                viewModel.updateRecurringBill(updatedBill)
                billToEdit = null
            },
            onDismiss = { billToEdit = null }
        )
    }
}

@Composable
private fun RecurringBillCard(
    bill: RecurringBillEntity,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val categoryColor = getRecurringCategoryColor(bill.category)
    val frequency = RecurringFrequency.values().find { it.name == bill.frequency }
    val frequencyText = frequency?.displayName ?: "每月"
    val nextDueDate = Instant.ofEpochMilli(bill.nextDueDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val dateText = nextDueDate.format(DateTimeFormatter.ofPattern("MM月dd日"))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (bill.isActive) categoryColor.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = getRecurringCategoryIconRes(bill.category)
                if (iconRes != 0) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (bill.isActive) categoryColor
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = bill.title.first().toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bill.isActive) categoryColor
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (bill.isActive)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = frequencyText,
                        fontSize = 11.sp,
                        color = if (bill.isActive) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " · ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "下次: $dateText",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (bill.isIncome) "+" else "-"}¥${String.format("%.2f", bill.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!bill.isActive)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else if (bill.isIncome) IncomeGreen
                    else ExpenseRed
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier.size(width = 36.dp, height = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Switch(
                            checked = bill.isActive,
                            onCheckedChange = { onToggleActive() },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecurringBillDialog(
    onAdd: (title: String, amount: Double, category: String, isIncome: Boolean, frequency: RecurringFrequency, startDate: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var showFrequencyMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }

    val expenseCategories = ExpenseCategories.all
    val incomeCategories = IncomeCategories.all
    val categories = if (isIncome) incomeCategories else expenseCategories
    var selectedCategory by remember(isIncome) {
        mutableStateOf(categories.first())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "添加周期账单",
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

                // Title
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题 (如: 房租)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Frequency selector
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { showFrequencyMenu = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "周期",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = selectedFrequency.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    DropdownMenu(
                        expanded = showFrequencyMenu,
                        onDismissRequest = { showFrequencyMenu = false }
                    ) {
                        RecurringFrequency.values().forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.displayName) },
                                onClick = {
                                    selectedFrequency = freq
                                    showFrequencyMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Start date picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "开始日期",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val dateText = if (selectedDate == today) "今天"
                    else selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
                    Text(
                        text = dateText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category grid
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
                        .heightIn(max = 180.dp),
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

                // Buttons
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
                                val startMillis = selectedDate
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                onAdd(title, amt, selectedCategory, isIncome, selectedFrequency, startMillis)
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRecurringBillDialog(
    bill: RecurringBillEntity,
    onConfirm: (RecurringBillEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(bill.title) }
    var amountText by remember { mutableStateOf(String.format("%.2f", bill.amount)) }
    var isIncome by remember { mutableStateOf(bill.isIncome) }
    var selectedFrequency by remember {
        mutableStateOf(RecurringFrequency.values().find { it.name == bill.frequency } ?: RecurringFrequency.MONTHLY)
    }
    var showFrequencyMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }
    var selectedDate by remember {
        mutableStateOf(Instant.ofEpochMilli(bill.nextDueDate).atZone(ZoneId.systemDefault()).toLocalDate())
    }

    val expenseCategories = ExpenseCategories.all
    val incomeCategories = IncomeCategories.all
    val categories = if (isIncome) incomeCategories else expenseCategories
    var selectedCategory by remember { mutableStateOf(bill.category) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "编辑周期账单",
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

                // Title
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题 (如: 房租)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Amount
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Frequency selector
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { showFrequencyMenu = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "周期",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = selectedFrequency.displayName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    DropdownMenu(
                        expanded = showFrequencyMenu,
                        onDismissRequest = { showFrequencyMenu = false }
                    ) {
                        RecurringFrequency.values().forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.displayName) },
                                onClick = {
                                    selectedFrequency = freq
                                    showFrequencyMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Start date picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "下次执行日期",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val dateText = if (selectedDate == today) "今天"
                    else selectedDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
                    Text(
                        text = dateText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category grid
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
                        .heightIn(max = 180.dp),
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

                // Buttons
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
                        text = "保存",
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && amt > 0) {
                                val nextDueMillis = selectedDate
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                                onConfirm(
                                    bill.copy(
                                        title = title,
                                        amount = amt,
                                        category = selectedCategory,
                                        isIncome = isIncome,
                                        frequency = selectedFrequency.name,
                                        nextDueDate = nextDueMillis,
                                        updatedAt = System.currentTimeMillis()
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
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
}

private fun getRecurringCategoryColor(category: String): Color {
    return when (category) {
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

private fun getRecurringCategoryIconRes(category: String): Int {
    return when (category) {
        ExpenseCategories.FOOD -> com.aifactory.appmessagecapture.R.drawable.ic_category_food
        ExpenseCategories.TRANSPORT -> com.aifactory.appmessagecapture.R.drawable.ic_category_transport
        ExpenseCategories.SHOPPING -> com.aifactory.appmessagecapture.R.drawable.ic_category_shopping
        ExpenseCategories.ENTERTAINMENT -> com.aifactory.appmessagecapture.R.drawable.ic_category_entertainment
        ExpenseCategories.LIVING -> com.aifactory.appmessagecapture.R.drawable.ic_category_living
        ExpenseCategories.MEDICAL -> com.aifactory.appmessagecapture.R.drawable.ic_category_medical
        ExpenseCategories.EDUCATION -> com.aifactory.appmessagecapture.R.drawable.ic_category_education
        ExpenseCategories.SOCIAL -> com.aifactory.appmessagecapture.R.drawable.ic_category_social
        ExpenseCategories.BEAUTY -> com.aifactory.appmessagecapture.R.drawable.ic_category_beauty
        ExpenseCategories.PET -> com.aifactory.appmessagecapture.R.drawable.ic_category_pet
        ExpenseCategories.FINANCE -> com.aifactory.appmessagecapture.R.drawable.ic_category_finance
        ExpenseCategories.OTHER -> com.aifactory.appmessagecapture.R.drawable.ic_category_other_expense
        IncomeCategories.SALARY -> com.aifactory.appmessagecapture.R.drawable.ic_category_salary
        IncomeCategories.PARTTIME -> com.aifactory.appmessagecapture.R.drawable.ic_category_parttime
        IncomeCategories.INVESTMENT -> com.aifactory.appmessagecapture.R.drawable.ic_category_investment
        IncomeCategories.RENTAL -> com.aifactory.appmessagecapture.R.drawable.ic_category_rental
        IncomeCategories.REFUND -> com.aifactory.appmessagecapture.R.drawable.ic_category_refund
        IncomeCategories.RED_PACKET -> com.aifactory.appmessagecapture.R.drawable.ic_category_redpacket
        IncomeCategories.REIMBURSEMENT -> com.aifactory.appmessagecapture.R.drawable.ic_category_reimbursement
        IncomeCategories.OTHER -> com.aifactory.appmessagecapture.R.drawable.ic_category_other_income
        else -> 0
    }
}
