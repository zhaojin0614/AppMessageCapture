@file:OptIn(ExperimentalMaterial3Api::class)

package com.aifactory.appmessagecapture.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.data.BillEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BluePrimary = Color(0xFF2196F3)
private val GreenPositive = Color(0xFF4CAF50)
private val CardBg = Color(0xFFFFFFFF)
private val BgGray = Color(0xFFF5F7FA)
private val TextGray = Color(0xFF999999)
private val TextDark = Color(0xFF333333)

@Composable
fun CategoryDetailScreen(
    category: String,
    isIncome: Boolean,
    startTime: Long,
    endTime: Long,
    onBack: () -> Unit,
    viewModel: CategoryDetailViewModel = viewModel()
) {
    val bills by viewModel.getBillsInTimeRange(category, isIncome, startTime, endTime)
        .collectAsState(initial = emptyList())

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGray)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            val grouped = bills.groupBy {
                Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            grouped.forEach { (date, dayBills) ->
                item {
                    Text(
                        text = formatDetailDate(date),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(dayBills) { bill ->
                    CategoryDetailBillItem(bill = bill)
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            // 留出底部 Tab 栏空间，避免被 MainApp 的 NavigationBar 遮挡
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }

            if (bills.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无数据",
                            fontSize = 14.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDetailBillItem(bill: BillEntity) {
    val context = LocalContext.current
    val iconBitmap = remember(bill.packageName) {
        if (bill.packageName.isBlank()) return@remember null
        try {
            context.packageManager.getApplicationIcon(bill.packageName)
                ?.toBitmap(width = 192, height = 192)
                ?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    val timeStr = remember(bill.timestamp) {
        val zoned = Instant.ofEpochMilli(bill.timestamp).atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("HH:mm").format(zoned)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    .background(
                        if (iconBitmap == null) BluePrimary.copy(alpha = 0.15f) else Color.Transparent
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
                        text = bill.appName.take(1),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
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
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GreenPositive.copy(alpha = 0.1f),
                        border = BorderStroke(0.5.dp, GreenPositive)
                    ) {
                        Text(
                            text = bill.category,
                            fontSize = 10.sp,
                            color = GreenPositive,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$timeStr | ${bill.title}",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            val amountColor = if (bill.isIncome) GreenPositive else Color(0xFFFF5252)
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
