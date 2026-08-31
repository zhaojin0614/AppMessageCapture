@file:OptIn(ExperimentalMaterial3Api::class)

package com.aifactory.appmessagecapture.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.data.BillEntity

import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.glassFill
import com.aifactory.appmessagecapture.ui.components.gradientBrush
import com.aifactory.appmessagecapture.ui.theme.ComponentGap
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import com.aifactory.appmessagecapture.utils.rememberAppIcon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CategoryDetailScreen(
    category: String,
    isIncome: Boolean,
    startTime: Long,
    endTime: Long,
    onBack: () -> Unit,
    platformName: String? = null,
    viewModel: CategoryDetailViewModel = viewModel()
) {
    // platformName 非空 = 报表「平台构成」点进来的平台明细，按平台名过滤
    val bills by (if (platformName != null) {
        viewModel.getPlatformBillsInTimeRange(platformName, isIncome, startTime, endTime)
    } else {
        viewModel.getBillsInTimeRange(category, isIncome, startTime, endTime)
    }).collectAsState(initial = null)

    BackHandler { onBack() }

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
                title = {
                    Text(
                        text = platformName ?: category,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        val billList = bills
        if (billList != null) {
            // Grouped OUTSIDE the LazyColumn scope (not composable there)
            val grouped = remember(billList) {
                billList.groupBy {
                    Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                }
            }
            LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    grouped.forEach { (date, dayBills) ->
                        item(key = "hdr_$date") {
                            Text(
                                text = formatDetailDate(date),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = ComponentGap)
                            )
                        }
                        items(dayBills, key = { it.id }) { bill ->
                            CategoryDetailBillItem(bill = bill)
                        }
                    }

                    // 留出底部 Tab 栏空间，避免被 MainApp 的 NavigationBar 遮挡
                    item(key = "bottom_space") {
                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    if (billList.isEmpty()) {
                        item(key = "empty") {
                            Box(
                                modifier = Modifier.fillParentMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无数据",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDetailBillItem(bill: BillEntity) {
    // Blank packageName = 手动记账/周期记账, no launcher icon to look up
    val iconBitmap = if (bill.packageName.isBlank()) null
    else rememberAppIcon(bill.packageName).value

    val timeStr = remember(bill.timestamp) {
        val zoned = Instant.ofEpochMilli(bill.timestamp).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("HH:mm").format(zoned)
    }

    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            // 半间距：相邻卡片上下相加 = ComponentGap，与其他界面卡片间距一致
            .padding(horizontal = 16.dp, vertical = ComponentGap / 2),
        shape = RoundedCornerShape(16.dp),
        contentPadding = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(gradientBrush(MaterialTheme.colorScheme.secondary, alpha = 0.18f))
                    .border(glassBorder(), RoundedCornerShape(10.dp)),
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
                        text = bill.appName.take(1),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bill.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = IncomeGreen.copy(alpha = 0.10f),
                        border = BorderStroke(0.5.dp, IncomeGreen)
                    ) {
                        Text(
                            text = bill.category,
                            fontSize = 10.sp,
                            color = IncomeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$timeStr | ${bill.title}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    }
}

private fun formatDetailDate(date: LocalDate): String {
    val weekDays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
    val dayOfWeekIndex = date.dayOfWeek.value - 1
    return String.format(
        "%d.%02d.%02d %s",
        date.year,
        date.monthValue,
        date.dayOfMonth,
        weekDays[dayOfWeekIndex]
    )
}
