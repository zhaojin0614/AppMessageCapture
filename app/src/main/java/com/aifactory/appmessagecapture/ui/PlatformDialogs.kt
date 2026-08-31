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
import com.aifactory.appmessagecapture.ui.components.GlassCompactDialog
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
import com.aifactory.appmessagecapture.ui.theme.PlatformColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * 平台选择对话框（用于添加账单时选择扣款/存入平台）。
 * 可选"未选择"（待对账）或具体平台。
 */
@Composable
fun PlatformPickerDialog(
    platforms: List<com.aifactory.appmessagecapture.data.PlatformAccountEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    GlassCompactDialog(
        onDismissRequest = onDismiss,
        title = "选择平台",
        text = {
            if (platforms.isEmpty()) {
                Text(
                    text = "暂无平台账户，请先在「账户管理」中添加平台。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 未选择（待对账）选项
                    item {
                        PlatformPickerItem(
                            name = "未选择（待对账）",
                            balance = null,
                            isSelected = selectedId == null,
                            isUnreconciled = true,
                            onClick = { onSelect(null) }
                        )
                    }
                    items(platforms, key = { it.id }) { platform ->
                        PlatformPickerItem(
                            name = platform.name,
                            balance = platform.balance,
                            isSelected = selectedId == platform.id,
                            isUnreconciled = false,
                            onClick = { onSelect(platform.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PlatformPickerItem(
    name: String,
    balance: Double?,
    isSelected: Boolean,
    isUnreconciled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isUnreconciled) ExpenseRed.copy(alpha = 0.12f)
                    else PlatformColors.colorForPlatformName(name).copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isUnreconciled) "?" else name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = if (isUnreconciled) ExpenseRed
                else PlatformColors.colorForPlatformName(name),
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (balance != null) {
            Text(
                text = "¥${String.format("%.2f", balance)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 对账对话框：为待对账（或已对账）账单分配/更换扣款平台。
 * 支出从所选平台扣款，收入存入所选平台；选"取消对账"回滚到待对账。
 */
@Composable
fun ReconcilePlatformDialog(
    bill: BillEntity,
    platforms: List<com.aifactory.appmessagecapture.data.PlatformAccountEntity>,
    onAssign: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val typeLabel = if (bill.isIncome) "存入平台" else "扣款平台"
    val amountPrefix = if (bill.isIncome) "+" else "-"
    GlassCompactDialog(
        onDismissRequest = onDismiss,
        title = "对账 - ${bill.title}",
        text = {
            Column {
                Text(
                    text = "$amountPrefix¥${String.format("%.2f", bill.amount)} · 选择$typeLabel",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (platforms.isEmpty()) {
                    Text(
                        text = "暂无平台账户，请先在「账户管理」中添加平台。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item {
                            PlatformPickerItem(
                                name = "取消对账（待对账）",
                                balance = null,
                                isSelected = bill.platformAccountId == null,
                                isUnreconciled = true,
                                onClick = { onAssign(null) }
                            )
                        }
                        items(platforms, key = { it.id }) { platform ->
                            PlatformPickerItem(
                                name = platform.name,
                                balance = platform.balance,
                                isSelected = bill.platformAccountId == platform.id,
                                isUnreconciled = false,
                                onClick = { onAssign(platform.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
