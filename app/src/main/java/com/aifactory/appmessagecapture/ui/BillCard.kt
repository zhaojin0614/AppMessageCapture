@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aifactory.appmessagecapture.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
fun BillCard(
    bill: BillEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    platformName: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onCategoryClick: () -> Unit,
    onReconcile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val iconBitmap by rememberAppIcon(bill.packageName)

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
                else Color.Transparent,
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(gradientBrush(MaterialTheme.colorScheme.primaryContainer, alpha = 0.75f))
                .border(glassBorder(), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            val icon = iconBitmap
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = bill.appName.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Row 1: App name + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bill.appName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                val amountColor = if (bill.isIncome) IncomeGreen else ExpenseRed
                val amountPrefix = if (bill.isIncome) "+" else "-"
                Text(
                    text = "$amountPrefix¥${String.format("%.2f", bill.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            // Row 2: Title + Category tag + Platform tag + Time (merged)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bill.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )
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
                // 平台标记：已对账显示平台名（灰），未对账显示橙色"待对账"徽章
                Spacer(modifier = Modifier.width(4.dp))
                if (platformName != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = platformName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ExpenseRed.copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onReconcile() }
                    ) {
                        Text(
                            text = "待对账",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatBillTimeOnly(bill.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private val billTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatBillTimeOnly(timestamp: Long): String {
    val zoned = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    return billTimeFormatter.format(zoned)
}
