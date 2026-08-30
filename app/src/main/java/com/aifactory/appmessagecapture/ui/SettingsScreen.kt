package com.aifactory.appmessagecapture.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.BuildConfig
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.service.PaymentScreenAccessibilityService
import com.aifactory.appmessagecapture.service.SupportedCaptureApp
import com.aifactory.appmessagecapture.service.SupportedPaymentApps
import com.aifactory.appmessagecapture.ui.components.GlassCompactDialog
import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import com.aifactory.appmessagecapture.utils.NotificationServiceHelper
import com.aifactory.appmessagecapture.utils.rememberAppIcon

/** xlsx 的标准 MIME（备份导出命名 / 导入过滤共用） */
private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/**
 * 设置页：记账页顶栏的低频配置入口统一收纳于此，按功能域分组。
 * 行类型三种：导航行（→子页/弹窗）、状态行（色点 + 当前权限状态）、值行（版本号）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: BillViewModel = viewModel()
    val db = AppDatabase.getDatabase(context)

    var showSupportedApps by remember { mutableStateOf(false) }
    var showPlatformAccounts by remember { mutableStateOf(false) }
    var showRecurringBills by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var importOverwrite by remember { mutableStateOf(false) }

    // 从系统设置页/子页面返回时刷新权限状态与计数
    var resumeKey by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 屏幕记账：系统登记 + 服务实际存活（登记≠生效，HyperOS 偶发断绑）
    val screenRegistered = remember(resumeKey) {
        Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains("PaymentScreenAccessibilityService") == true
    }
    val screenLive = remember(resumeKey) { PaymentScreenAccessibilityService.isLive }
    val (screenStatusText, screenStatusColor) = when {
        screenRegistered && screenLive -> "已生效" to IncomeGreen
        screenRegistered -> "已开启但未生效" to ExpenseRed
        else -> "未开启" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val notificationGranted = remember(resumeKey) {
        NotificationServiceHelper.isNotificationServiceEnabled(context)
    }

    var platformCount by remember { mutableStateOf(0) }
    LaunchedEffect(resumeKey) {
        platformCount = db.platformAccountDao().getAllOnce().size
    }
    val activeRecurring by db.recurringBillDao().getActiveCount().collectAsState(initial = 0)
    val budgets by viewModel.budgets.collectAsState()
    val totalBudget = budgets.firstOrNull { it.category == "" }?.amount

    // 备份导出/导入的 SAF 启动器
    val backupBusy by viewModel.backupBusy.collectAsState()
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(XLSX_MIME)
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it, importOverwrite) } }
    LaunchedEffect(Unit) {
        viewModel.backupMessage.collect { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.consumeBackupMessage()
            }
        }
    }

    if (showPlatformAccounts) {
        PlatformAccountScreen(onBack = { showPlatformAccounts = false })
        return
    }
    if (showRecurringBills) {
        RecurringBillScreen(onBack = { showRecurringBills = false })
        return
    }

    // 拦截系统返回手势/按键回到记账界面，而不是退出应用
    //（子页面各自注册了 BackHandler，优先处理其自身返回）
    BackHandler(enabled = true) { onBack() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SettingsGroup("自动记账") {
                SettingsNavigateRow(
                    icon = Icons.Default.FactCheck,
                    title = "支持自动记账的App",
                    value = "${SupportedPaymentApps.supportedCaptureApps.size} 个",
                    onClick = { showSupportedApps = true }
                )
                SettingsStatusRow(
                    icon = Icons.Default.Visibility,
                    title = "屏幕记账权限",
                    statusText = screenStatusText,
                    statusColor = screenStatusColor,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
                SettingsStatusRow(
                    icon = Icons.Default.Notifications,
                    title = "通知捕获权限",
                    statusText = if (notificationGranted) "已授权" else "未授权",
                    statusColor = if (notificationGranted) IncomeGreen else ExpenseRed,
                    onClick = { NotificationServiceHelper.openNotificationSettings(context) }
                )
            }

            SettingsGroup("记账管理") {
                SettingsNavigateRow(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "平台账户管理",
                    value = "$platformCount 个",
                    onClick = { showPlatformAccounts = true }
                )
                SettingsNavigateRow(
                    icon = Icons.Default.Repeat,
                    title = "周期账单",
                    value = "启用 $activeRecurring 条",
                    onClick = { showRecurringBills = true }
                )
                SettingsNavigateRow(
                    icon = Icons.Default.Savings,
                    title = "预算管理",
                    value = totalBudget?.let { "¥%.0f/月".format(it) } ?: "未设置",
                    onClick = { showBudgetDialog = true }
                )
            }

            SettingsGroup("数据") {
                SettingsNavigateRow(
                    icon = Icons.Default.Backup,
                    title = "备份与恢复",
                    value = "导出 / 导入",
                    onClick = { showBackupDialog = true }
                )
            }

            SettingsGroup("关于") {
                SettingsValueRow(
                    icon = Icons.Default.Info,
                    title = "版本",
                    value = BuildConfig.VERSION_NAME
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSupportedApps) {
        SupportedAppsDialog(onDismiss = { showSupportedApps = false })
    }

    if (showBudgetDialog) {
        BudgetDialog(viewModel = viewModel, onDismiss = { showBudgetDialog = false })
    }

    if (showBackupDialog) {
        GlassCompactDialog(
            onDismissRequest = { showBackupDialog = false },
            title = "备份与恢复",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackupActionRow(
                        icon = Icons.Default.TableChart,
                        title = "导出表格（Excel）",
                        subtitle = "支出/收入分表 + 平台账户余额",
                        enabled = !backupBusy,
                        onClick = {
                            showBackupDialog = false
                            val date = java.time.LocalDate.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            exportBackupLauncher.launch("捕账_备份_$date.xlsx")
                        }
                    )
                    BackupActionRow(
                        icon = Icons.Default.UploadFile,
                        title = "导入数据（合并）",
                        subtitle = "与现有账单去重，不改动现有平台余额",
                        enabled = !backupBusy,
                        onClick = {
                            showBackupDialog = false
                            importOverwrite = false
                            importBackupLauncher.launch(arrayOf(XLSX_MIME, "application/octet-stream"))
                        }
                    )
                    BackupActionRow(
                        icon = Icons.Default.SettingsBackupRestore,
                        title = "恢复备份（覆盖）",
                        subtitle = "清空当前账单与平台账户后按文件重建",
                        enabled = !backupBusy,
                        onClick = {
                            showBackupDialog = false
                            showRestoreConfirm = true
                        }
                    )
                    Text(
                        text = "平台余额以导出文件中的快照为准；导入不重复计算余额",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("关闭") }
            }
        )
    }

    if (showRestoreConfirm) {
        GlassCompactDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = "恢复备份",
            text = { Text("将清空当前所有账单与平台账户，并按所选文件重建，此操作不可撤销。确定继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        importOverwrite = true
                        importBackupLauncher.launch(arrayOf(XLSX_MIME, "application/octet-stream"))
                    }
                ) { Text("确定恢复", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("取消") }
            }
        )
    }
}

// ── 分组与行组件 ─────────────────────────────────────────────────────────

/** 分组卡片：组标题 + 圆角玻璃卡片内的若干行 */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)
    )
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(content = content)
    }
}

/** 行骨架：圆底图标 + 标题 + 右侧内容（可点击） */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

/** 导航行：右侧值 + 箭头 */
@Composable
private fun SettingsNavigateRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    SettingsRow(icon = icon, title = title, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 状态行：右侧色点 + 状态文字 + 箭头 */
@Composable
private fun SettingsStatusRow(
    icon: ImageVector,
    title: String,
    statusText: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    SettingsRow(icon = icon, title = title, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 值行：右侧纯文本（不可点击） */
@Composable
private fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    SettingsRow(icon = icon, title = title) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 备份弹窗的操作行：图标 + 标题 + 说明 */
@Composable
private fun BackupActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 「支持自动记账的App」清单弹窗：展示当前两条捕获通道支持的应用。
 * 数据源为 [SupportedPaymentApps.supportedCaptureApps]（与捕获门槛/
 * 监视名单的同步性由单测保证）。
 */
@Composable
fun SupportedAppsDialog(onDismiss: () -> Unit) {
    GlassCompactDialog(
        onDismissRequest = onDismiss,
        title = "支持自动记账的App",
        text = {
            Column {
                Text(
                    text = "以下应用产生支付信息时将自动记录账单，无需手动添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    SupportedPaymentApps.supportedCaptureApps.forEach { app ->
                        SupportedAppRow(app)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Text(
                    text = "通知捕获＝监听该App的支付通知；屏幕捕获＝无障碍读取" +
                        "支付成功页（需在设置中开启屏幕记账权限）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
private fun SupportedAppRow(app: SupportedCaptureApp) {
    val iconBitmap by rememberAppIcon(app.packageName, 28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 应用图标（未安装/取不到时回退首字占位）
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(7.dp)
                ),
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
                    text = app.appName.take(1),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (app.channel == SupportedPaymentApps.CHANNEL_SCREEN)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = app.channel,
                style = MaterialTheme.typography.labelSmall,
                color = if (app.channel == SupportedPaymentApps.CHANNEL_SCREEN)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
