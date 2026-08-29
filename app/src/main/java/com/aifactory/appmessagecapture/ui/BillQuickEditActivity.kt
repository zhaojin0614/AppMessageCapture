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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 * 账单快捷编辑弹窗（从「记账成功」通知的「完善账单」按钮打开）。
 *
 * 双栏布局：左侧分类（带分类色圆点）、右侧扣款平台（含「待对账」），
 * 两栏各自滚动，一次完成两项选择，底部「保存」统一提交——
 * 解决单字段弹窗「一笔账只能改一项」的问题。
 *
 * 交互：点选项只切换选中态；「保存」才写库并清除常驻账单通知；
 * 只改其中一项时另一项保持原值不写库。「取消」/点空白处直接关闭；
 * 左下角「删除」两段式确认（首点变「确认删除？」再点执行），用于
 * 丢弃不需要的自动捕获账单，删除走事务回滚已对账平台余额。
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (billId <= 0) {
            finish()
            return
        }
        setContent {
            AppMessageCaptureTheme {
                QuickEditDialog(
                    billId = billId,
                    onDismiss = { finish() },
                    onBillMissing = { onBillMissing() },
                    onSave = { applyEdits(it.first, it.second, it.third, it.fourth) },
                    onDelete = { applyDelete() }
                )
            }
        }
    }

    /**
     * 删除这笔自动捕获的账单：走 [AccountRepository.deleteBillWithRollback]
     * 事务回滚已对账平台的余额，并清除常驻通知。
     */
    private fun applyDelete() {
        val appContext = applicationContext
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(appContext)
                AccountRepository(db, db.billDao(), db.platformAccountDao())
                    .deleteBillWithRollback(billId)
                cancelBillNotification(appContext)
            }
            Toast.makeText(appContext, "已删除该账单", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 账单已不存在（App 内已删除/清空，但常驻通知还挂着）：
     * 清除残留通知后退出弹窗，避免无限加载。
     */
    private fun onBillMissing() {
        val appContext = applicationContext
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { cancelBillNotification(appContext) }
            Toast.makeText(appContext, "该账单已删除，通知已清除", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 保存变更项并清除常驻通知。两个 changed 标记由弹窗按「与原值比较」得出，
     * 未变化的字段不写库（platform 的 null 是合法值=待对账，不能用 null 表示"未改"）。
     */
    private fun applyEdits(
        newCategory: String?,
        newPlatform: PlatformAccountEntity?,
        categoryChanged: Boolean,
        platformChanged: Boolean
    ) {
        val appContext = applicationContext
        lifecycleScope.launch {
            val changes = mutableListOf<String>()
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(appContext)
                if (categoryChanged && newCategory != null) {
                    db.billDao().updateCategory(billId, newCategory)
                    changes.add("分类「$newCategory」")
                }
                if (platformChanged) {
                    AccountRepository(db, db.billDao(), db.platformAccountDao())
                        .reconcileBill(billId, newPlatform?.id)
                    changes.add(
                        if (newPlatform != null) "平台「${newPlatform.name}」" else "平台改为待对账"
                    )
                }
                cancelBillNotification(appContext)
            }
            Toast.makeText(appContext, "已保存 ${changes.joinToString("、")}", Toast.LENGTH_SHORT).show()
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
    billId: Long,
    onDismiss: () -> Unit,
    onBillMissing: () -> Unit,
    onSave: (Quadruple<String?, PlatformAccountEntity?, Boolean, Boolean>) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var bill by remember { mutableStateOf<BillEntity?>(null) }
    var platforms by remember { mutableStateOf<List<PlatformAccountEntity>>(emptyList()) }
    // 保存中：禁用全部交互，防止保存期间改选或误关弹窗
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var missing = false
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val loaded = db.billDao().getBillByIdOnce(billId)
            platforms = db.platformAccountDao().getAllOnce()
            bill = loaded
            missing = loaded == null
        }
        // 账单已删除（通知残留）→ 清除通知并退出，不能停在加载态
        if (missing) onBillMissing()
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
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume click */ }
        ) {
            if (current == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                TwoColumnEditor(
                    bill = current,
                    platforms = platforms,
                    saving = saving,
                    onStartSave = { saving = true },
                    onSave = onSave,
                    onDelete = onDelete,
                    onCancel = onDismiss
                )
            }
        }
    }
}

/** 四元组（Kotlin 标准库无 Quad，简单承载保存参数） */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
private fun TwoColumnEditor(
    bill: BillEntity,
    platforms: List<PlatformAccountEntity>,
    saving: Boolean,
    onStartSave: () -> Unit,
    onSave: (Quadruple<String?, PlatformAccountEntity?, Boolean, Boolean>) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    var selectedCategory by remember(bill.id) { mutableStateOf(bill.category) }
    var selectedPlatformId by remember(bill.id) { mutableStateOf(bill.platformAccountId) }
    // 删除两段式确认：首点变「确认删除？」，再点才真正删除，防误触
    var confirmingDelete by remember(bill.id) { mutableStateOf(false) }

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
            // 竖分隔线
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

        // 底部操作行：左「删除」（两段确认），右 取消 + 保存（无任何变更时置灰）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (confirmingDelete) onDelete() else confirmingDelete = true
                },
                enabled = !saving,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (confirmingDelete) "确认删除？" else "删除")
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onCancel()
                    },
                    enabled = !saving
                ) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onStartSave()
                        onSave(
                            Quadruple(
                                selectedCategory,
                                platforms.firstOrNull { it.id == selectedPlatformId },
                                categoryChanged,
                                platformChanged
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
        }
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
