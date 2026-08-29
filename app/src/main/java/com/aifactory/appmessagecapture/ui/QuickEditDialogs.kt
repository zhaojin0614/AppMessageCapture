package com.aifactory.appmessagecapture.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.data.AccountRepository
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.data.PlatformAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** 一次快捷编辑的变更集合。changed 标记区分「未改」与「改为待对账」（null 是合法值） */
data class QuickEditSelection(
    val newCategory: String?,
    val newPlatform: PlatformAccountEntity?,
    val categoryChanged: Boolean,
    val platformChanged: Boolean
)

/**
 * 应用快捷编辑变更（分类 + 平台一次提交）。
 * 平台变更走 [AccountRepository.reconcileBill] 事务联动余额。
 * @return 变更摘要（Toast/成功态展示用）
 */
suspend fun applyQuickEdits(context: Context, billId: Long, selection: QuickEditSelection): String {
    val changes = mutableListOf<String>()
    withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        if (selection.categoryChanged && selection.newCategory != null) {
            db.billDao().updateCategory(billId, selection.newCategory)
            changes.add("分类「${selection.newCategory}」")
        }
        if (selection.platformChanged) {
            AccountRepository(db, db.billDao(), db.platformAccountDao())
                .reconcileBill(billId, selection.newPlatform?.id)
            changes.add(
                if (selection.newPlatform != null) "平台「${selection.newPlatform.name}」"
                else "平台改为待对账"
            )
        }
    }
    return changes.joinToString("、")
}

private sealed interface QuickEditPhase {
    data object Selecting : QuickEditPhase
    data object Saving : QuickEditPhase
    data class Saved(val summary: String) : QuickEditPhase
}

/**
 * 账单快捷编辑双栏弹窗（Activity 宿主与悬浮窗宿主共用）。
 *
 * 左栏分类（带分类色圆点）、右栏扣款平台（含「待对账」），两栏独立滚动，
 * 一次完成两项选择，底部「保存」统一提交——只改一项时另一项不写库。
 * 保存成功后展示「已保存」成功态约 1 秒再回调 [onDismiss] 收尾。
 *
 * @param onCommitted 保存落库成功后调用（宿主在此清除对应常驻通知）
 * @param onBillMissing 账单已不存在（App 内已删，通知残留）时调用
 * @param showOverlayHint 未授予悬浮窗权限时显示一行引导（仅 Activity 宿主传 true）
 */
@Composable
fun QuickEditDialogContent(
    billId: Long,
    showOverlayHint: Boolean,
    onDismiss: () -> Unit,
    onCommitted: () -> Unit,
    onBillMissing: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bill by remember { mutableStateOf<BillEntity?>(null) }
    var platforms by remember { mutableStateOf<List<PlatformAccountEntity>>(emptyList()) }
    var phase by remember { mutableStateOf<QuickEditPhase>(QuickEditPhase.Selecting) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val b = db.billDao().getBillByIdOnce(billId)
            val p = db.platformAccountDao().getAllOnce()
            b to p
        }
        bill = loaded.first
        platforms = loaded.second
        if (loaded.first == null) onBillMissing()
    }

    // 保存成功 → 成功态停留约 1 秒 → 收尾
    LaunchedEffect(phase) {
        if (phase is QuickEditPhase.Saved) {
            delay(1000)
            onDismiss()
        }
    }

    when (val p = phase) {
        is QuickEditPhase.Saved -> {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "已保存 ${p.summary}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        else -> {
            val current = bill
            when {
                current == null -> {
                    // 加载中（账单不存在时 onBillMissing 会收尾）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                else -> TwoColumnEditor(
                    bill = current,
                    platforms = platforms,
                    saving = p == QuickEditPhase.Saving,
                    showOverlayHint = showOverlayHint,
                    onStartSave = { phase = QuickEditPhase.Saving },
                    onCommit = { selection ->
                        scope.launch {
                            val summary = applyQuickEdits(context, billId, selection)
                            onCommitted()
                            phase = QuickEditPhase.Saved(summary)
                        }
                    },
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TwoColumnEditor(
    bill: BillEntity,
    platforms: List<PlatformAccountEntity>,
    saving: Boolean,
    showOverlayHint: Boolean,
    onStartSave: () -> Unit,
    onCommit: (QuickEditSelection) -> Unit,
    onCancel: () -> Unit
) {
    var selectedCategory by remember(bill.id) { mutableStateOf(bill.category) }
    var selectedPlatformId by remember(bill.id) { mutableStateOf(bill.platformAccountId) }

    val categoryOptions = if (bill.isIncome) IncomeCategories.all else ExpenseCategories.all
    val categoryChanged = selectedCategory != bill.category
    val platformChanged = selectedPlatformId != bill.platformAccountId

    Column(modifier = Modifier.padding(vertical = 20.dp)) {
        Text(
            text = if (bill.isIncome) "完善收入账单" else "完善支出账单",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = "¥${String.format(Locale.getDefault(), "%.2f", bill.amount)} · " +
                "${bill.appName} · ${if (bill.isIncome) "收入" else "支出"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 双栏：左分类 / 右平台，独立滚动
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            OptionColumn(title = "分类", modifier = Modifier.weight(1f)) {
                categoryOptions.forEach { option ->
                    CategoryChip(
                        label = option,
                        dotColor = getCategoryColor(option),
                        selected = option == selectedCategory,
                        enabled = !saving,
                        onClick = { selectedCategory = option }
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
            OptionColumn(title = "扣款平台", modifier = Modifier.weight(1f)) {
                PlatformChip(
                    label = "待对账",
                    sub = "暂不关联",
                    selected = selectedPlatformId == null,
                    enabled = !saving,
                    onClick = { selectedPlatformId = null }
                )
                platforms.forEach { account ->
                    PlatformChip(
                        label = account.name,
                        sub = "余额 ¥${String.format(Locale.getDefault(), "%.2f", account.balance)}",
                        selected = account.id == selectedPlatformId,
                        enabled = !saving,
                        onClick = { selectedPlatformId = account.id }
                    )
                }
            }
        }

        // 底部操作行：取消 + 保存（无任何变更时置灰）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel, enabled = !saving) {
                Text("取消")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    onStartSave()
                    onCommit(
                        QuickEditSelection(
                            newCategory = selectedCategory,
                            newPlatform = platforms.firstOrNull { it.id == selectedPlatformId },
                            categoryChanged = categoryChanged,
                            platformChanged = platformChanged
                        )
                    )
                },
                enabled = !saving && (categoryChanged || platformChanged),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存中…")
                } else {
                    Text("保存")
                }
            }
        }
        if (showOverlayHint) {
            OverlayPermissionHint()
        }
    }
}

/** 未授予悬浮窗权限时的一行引导（Activity 宿主展示） */
@Composable
private fun OverlayPermissionHint() {
    val context = LocalContext.current
    TextButton(onClick = {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        } catch (_: Exception) {
        }
    }, modifier = Modifier.padding(horizontal = 12.dp)) {
        Text(
            text = "开启「显示悬浮窗」权限后，通知按钮可在任意界面直接弹出选择",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 单栏容器：栏标题 + 纵向滚动选项列表 */
@Composable
private fun OptionColumn(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxHeight()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
        ) {
            content()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
private fun Modifier.fillMaxHeight(): Modifier = this.then(Modifier)

/** 分类选项：色点 + 名称，紧凑单行 */
@Composable
private fun CategoryChip(
    label: String,
    dotColor: Color,
    selected: Boolean,
    enabled: Boolean,
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
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (selected) {
                Spacer(modifier = Modifier.weight(1f))
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

/** 平台选项：名称 + 余额/说明，两行紧凑 */
@Composable
private fun PlatformChip(
    label: String,
    sub: String,
    selected: Boolean,
    enabled: Boolean,
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
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
