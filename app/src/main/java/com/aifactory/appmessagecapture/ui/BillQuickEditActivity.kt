package com.aifactory.appmessagecapture.ui

import android.app.NotificationManager
import android.content.Context
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
 * 启动本透明弹窗 Activity：单列选项、点选即保存并 finish，同时按原通知 ID
 * 重发通知刷新其内容，实现「通知里直接确认账单，不用进 App」。
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
                    onPickCategory = { applyCategory(it) },
                    onPickPlatform = { applyPlatform(it) }
                )
            }
        }
    }

    private fun applyCategory(category: String) {
        val appContext = applicationContext
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(appContext).billDao().updateCategory(billId, category)
                cancelBillNotification(appContext)
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
                cancelBillNotification(appContext)
            }
            Toast.makeText(
                appContext,
                if (platform != null) "已关联扣款平台「${platform.name}」" else "已改为待对账",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    /** 用户完成操作，清除常驻的账单通知 */
    private fun cancelBillNotification(context: Context) {
        if (notificationId >= BillNotificationHelper.NOTIFICATION_ID_BASE) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }
    }
}

@Composable
private fun QuickEditDialog(
    mode: String,
    billId: Long,
    onDismiss: () -> Unit,
    onPickCategory: (String) -> Unit,
    onPickPlatform: (PlatformAccountEntity?) -> Unit
) {
    val context = LocalContext.current
    var bill by remember { mutableStateOf<BillEntity?>(null) }
    var platforms by remember { mutableStateOf<List<PlatformAccountEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            bill = db.billDao().getBillByIdOnce(billId)
            platforms = db.platformAccountDao().getAllOnce()
        }
    }

    // 半透明遮罩：点击空白处关闭；内容区消费点击防止穿透
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
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
                    onPick = onPickCategory
                )
                else -> PlatformOptions(
                    bill = current,
                    platforms = platforms,
                    onPick = onPickPlatform
                )
            }
        }
    }
}

@Composable
private fun CategoryOptions(bill: BillEntity, onPick: (String) -> Unit) {
    val options = if (bill.isIncome) IncomeCategories.all else ExpenseCategories.all
    DialogScaffold(
        title = if (bill.isIncome) "选择收入分类" else "选择支出分类",
        subtitle = "¥${String.format(Locale.getDefault(), "%.2f", bill.amount)} · ${bill.appName}"
    ) {
        options.forEach { option ->
            OptionRow(
                text = option,
                sub = null,
                selected = option == bill.category,
                onClick = { onPick(option) }
            )
        }
    }
}

@Composable
private fun PlatformOptions(
    bill: BillEntity,
    platforms: List<PlatformAccountEntity>,
    onPick: (PlatformAccountEntity?) -> Unit
) {
    DialogScaffold(
        title = "选择扣款平台",
        subtitle = "¥${String.format(Locale.getDefault(), "%.2f", bill.amount)} · ${bill.category}"
    ) {
        OptionRow(
            text = "待对账（暂不关联）",
            sub = "稍后可在记账页分配平台",
            selected = bill.platformAccountId == null,
            onClick = { onPick(null) }
        )
        platforms.forEach { account ->
            OptionRow(
                text = account.name,
                sub = "余额 ¥${String.format(Locale.getDefault(), "%.2f", account.balance)}",
                selected = account.id == bill.platformAccountId,
                onClick = { onPick(account) }
            )
        }
    }
}

@Composable
private fun DialogScaffold(
    title: String,
    subtitle: String,
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
        Spacer(modifier = Modifier.padding(vertical = 6.dp))
        Column(
            modifier = Modifier
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    sub: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
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
