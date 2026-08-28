package com.aifactory.appmessagecapture.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.aifactory.appmessagecapture.data.AccountRepository
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.data.PlatformAccountEntity
import com.aifactory.appmessagecapture.service.BillNotificationHelper
import com.aifactory.appmessagecapture.ui.theme.AppMessageCaptureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 账单快捷编辑弹窗（从「记账成功」通知的按钮打开，普通界面不可达）。
 *
 * Android 通知按钮不能内嵌下拉控件，所以两个按钮（改分类/扣款平台）都
 * 启动本透明弹窗 Activity。交互为「选择 → 确认」两步：
 * 点选项只切换选中态（弹窗保持打开，防止误触直接写入），
 * 点「保存」才写库、刷新通知并关闭；「取消」或点空白处直接关闭不写入。
 *
 * 保存走与 App 内一致的写入路径：
 * - 分类：billDao.updateCategory（分类不影响平台余额）
 * - 平台：AccountRepository.reconcileBill（事务内联动平台余额增减）
 */
class BillQuickEditActivity : ComponentActivity() {

    private val billId: Long by lazy {
        intent.getLongExtra(BillNotificationHelper.EXTRA_BILL_ID, -1L)
    }
    private val notificationId: Int by lazy {
        intent.getIntExtra(BillNotificationHelper.EXTRA_NOTIFICATION_ID, -1)
    }
    private val mode: String by lazy {
        intent.getStringExtra(BillNotificationHelper.EXTRA_MODE)
            ?: BillNotificationHelper.MODE_CATEGORY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (billId <= 0) {
            finish()
            return
        }
        setContent {
            AppMessageCaptureTheme {
                QuickEditDialog(
                    mode = mode,
                    billId = billId,
                    onDismiss = { finish() },
                    onSaveCategory = { applyCategory(it) },
                    onSavePlatform = { applyPlatform(it) }
                )
            }
        }
    }

    private fun applyCategory(category: String) {
        val appContext = applicationContext
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(appContext).billDao().updateCategory(billId, category)
                BillNotificationHelper.repostBillNotification(appContext, billId, notificationId)
            }
            Toast.makeText(appContext, "分类已改为「$category」", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun applyPlatform(platform: PlatformAccountEntity?) {
        val appContext = applicationContext
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(appContext)
            withContext(Dispatchers.IO) {
                AccountRepository(db, db.billDao(), db.platformAccountDao())
                    .reconcileBill(billId, platform?.id)
                BillNotificationHelper.repostBillNotification(appContext, billId, notificationId)
            }
            Toast.makeText(
                appContext,
                if (platform != null) "已关联扣款平台「${platform.name}」" else "已改为待对账",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }
}

@Composable
private fun QuickEditDialog(
    mode: String,
    billId: Long,
    onDismiss: () -> Unit,
    onSaveCategory: (String) -> Unit,
    onSavePlatform: (PlatformAccountEntity?) -> Unit
) {
    val context = LocalContext.current
    var bill by remember { mutableStateOf<BillEntity?>(null) }
    var platforms by remember { mutableStateOf<List<PlatformAccountEntity>>(emptyList()) }
    // 保存中：禁用全部交互，防止保存期间改选或误关弹窗
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            bill = db.billDao().getBillByIdOnce(billId)
            platforms = db.platformAccountDao().getAllOnce()
        }
    }

    // 半透明遮罩：点击空白处关闭（保存中不允许）；内容区消费点击防止穿透
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = !saving, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        val current = bill
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume click */ }
        ) {
            when {
                current == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                mode == BillNotificationHelper.MODE_CATEGORY -> CategoryOptions(
                    bill = current,
                    saving = saving,
                    onStartSave = { saving = true },
                    onConfirm = onSaveCategory,
                    onCancel = onDismiss
                )
                else -> PlatformOptions(
                    bill = current,
                    platforms = platforms,
                    saving = saving,
                    onStartSave = { saving = true },
                    onConfirm = onSavePlatform,
                    onCancel = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CategoryOptions(
    bill: BillEntity,
    saving: Boolean,
    onStartSave: () -> Unit,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val options = if (bill.isIncome) IncomeCategories.all else ExpenseCategories.all
    var selected by remember(bill.id) { mutableStateOf(bill.category) }

    DialogScaffold(
        title = if (bill.isIncome) "选择收入分类" else "选择支出分类",
        subtitle = "¥${String.format(Locale.getDefault(), "%.2f", bill.amount)} · ${bill.appName}",
        footer = {
            ConfirmFooter(
                confirmEnabled = !saving && selected != bill.category,
                saving = saving,
                onConfirm = {
                    onStartSave()
                    onConfirm(selected)
                },
                onCancel = onCancel
            )
        }
    ) {
        options.forEach { option ->
            OptionRow(
                text = option,
                selected = option == selected,
                enabled = !saving,
                onClick = { selected = option }
            )
        }
    }
}

@Composable
private fun PlatformOptions(
    bill: BillEntity,
    platforms: List<PlatformAccountEntity>,
    saving: Boolean,
    onStartSave: () -> Unit,
    onConfirm: (PlatformAccountEntity?) -> Unit,
    onCancel: () -> Unit
) {
    var selectedId by remember(bill.id) { mutableStateOf(bill.platformAccountId) }

    DialogScaffold(
        title = "选择扣款平台",
        subtitle = "¥${String.format(Locale.getDefault(), "%.2f", bill.amount)} · ${bill.category}",
        footer = {
            ConfirmFooter(
                confirmEnabled = !saving && selectedId != bill.platformAccountId,
                saving = saving,
                onConfirm = {
                    onStartSave()
                    onConfirm(platforms.firstOrNull { it.id == selectedId })
                },
                onCancel = onCancel
            )
        }
    ) {
        OptionRow(
            text = "待对账（暂不关联）",
            sub = "稍后可在记账页分配平台",
            selected = selectedId == null,
            enabled = !saving,
            onClick = { selectedId = null }
        )
        platforms.forEach { account ->
            OptionRow(
                text = account.name,
                sub = "余额 ¥${String.format(Locale.getDefault(), "%.2f", account.balance)}",
                selected = account.id == selectedId,
                enabled = !saving,
                onClick = { selectedId = account.id }
            )
        }
    }
}

@Composable
private fun DialogScaffold(
    title: String,
    subtitle: String,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            content()
        }
        footer()
    }
}

/** 底部操作行：取消 + 保存。选择未变化时保存按钮置灰 */
@Composable
private fun ConfirmFooter(
    confirmEnabled: Boolean,
    saving: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onCancel,
            enabled = !saving
        ) {
            Text("取消")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
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
}

@Composable
private fun OptionRow(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    sub: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    selected -> MaterialTheme.colorScheme.primary
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (sub != null) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
