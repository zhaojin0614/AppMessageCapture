package com.aifactory.appmessagecapture.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.BuildConfig
import com.aifactory.appmessagecapture.R
import com.aifactory.appmessagecapture.birthday.service.BirthdayAlarmScheduler
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.features.FeatureModule
import com.aifactory.appmessagecapture.features.FeatureRepository
import com.aifactory.appmessagecapture.service.PaymentScreenAccessibilityService
import com.aifactory.appmessagecapture.service.SupportedCaptureApp
import com.aifactory.appmessagecapture.service.SupportedPaymentApps
import com.aifactory.appmessagecapture.ui.components.GlassCompactDialog
import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.theme.AccentColor
import com.aifactory.appmessagecapture.ui.theme.AccentColorRepository
import com.aifactory.appmessagecapture.ui.theme.AccentVariant
import com.aifactory.appmessagecapture.ui.theme.ComponentGap
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import com.aifactory.appmessagecapture.utils.NotificationServiceHelper
import com.aifactory.appmessagecapture.utils.PreferencesManager
import com.aifactory.appmessagecapture.utils.rememberAppIcon
import com.aifactory.appmessagecapture.worker.NotificationCleanupWorker
import kotlinx.coroutines.launch

/** xlsx 的标准 MIME（备份导出命名 / 导入过滤共用） */
private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/**
 * 「我的」页：全局设置的正式宿主（不属于任何功能模块、不可被关闭）。
 *
 * 全部全局设置平铺在一级界面：功能开关（8 个模块）、记账管理、捕获与权限、
 * 通用（主题色/备份/消息保留时长）、关于。各功能页的页面专属入口
 * （消息页的筛选/屏蔽/搜索/导出，生日页的齿轮弹窗）保留在原页面上，
 * 不在此页重复。行类型：开关行 / 导航行（→子页/弹窗）/ 状态行 / 值行。
 */
@Composable
fun MineScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: BillViewModel = viewModel()
    val db = AppDatabase.getDatabase(context)

    var showSupportedApps by remember { mutableStateOf(false) }
    var showPlatformAccounts by remember { mutableStateOf(false) }
    var showMerchantMemories by remember { mutableStateOf(false) }
    var showRecurringBills by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showCustomPicker by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    // 消息保留天数：0 = 永久保留；改动即持久化并触发一次立即清理
    var retentionDays by remember {
        mutableStateOf(PreferencesManager.getInstance(context).getNotificationRetentionDays())
    }
    // 弹窗内的草稿选择：点色块/调色板只改草稿，「使用此颜色」统一应用
    var draftAccent by remember { mutableStateOf(AccentVariant.fromPreset(AccentColor.MINT)) }
    var draftCustomColor by remember { mutableStateOf(AccentColor.MINT.primary) }
    var importOverwrite by remember { mutableStateOf(false) }

    // 主色调：全局单例状态，选色后即时生效（读取处自动订阅重组）
    val currentAccent = AccentColorRepository.current

    // 功能开关：全局模块显隐的唯一事实源（主导航/各入口/后台管线统一读取）
    val disabledFeatures by FeatureRepository.disabled.collectAsState()
    val scope = rememberCoroutineScope()

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
    if (showMerchantMemories) {
        MerchantMemoryScreen(onBack = { showMerchantMemories = false })
        return
    }
    if (showRecurringBills) {
        RecurringBillScreen(onBack = { showRecurringBills = false })
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // 头部：应用图标 + 名称 + 版本。不能用 painterResource(R.mipmap.ic_launcher)：
        // 自适应图标是 AdaptiveIconDrawable，painterResource 只支持 Vector/位图，会抛
        // IllegalArgumentException。改为手动叠层复现自适应图标（背景层 + 前景层）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = ComponentGap / 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "捕账",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v" + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsGroup("功能开关") {
            FeatureModule.entries.forEach { module ->
                FeatureToggleRow(
                    icon = module.settingsIcon(),
                    title = module.label,
                    subtitle = module.description,
                    enabled = module !in disabledFeatures,
                    onToggle = { checked ->
                        val ok = FeatureRepository.setEnabled(context, module, checked)
                        if (!ok) {
                            Toast.makeText(
                                context, "至少保留一个主页模块开启", Toast.LENGTH_SHORT
                            ).show()
                        } else if (module == FeatureModule.BIRTHDAY) {
                            // 关闭立即撤掉全部已注册闹钟；重新开启时重新注册
                            scope.launch {
                                if (checked) BirthdayAlarmScheduler.rescheduleAll(context)
                                else BirthdayAlarmScheduler.cancelAll(context)
                            }
                        }
                    }
                )
            }
        }

        // 记账管理各行跟随对应模块开关：关闭的模块整行隐藏，
        // 全部关闭时整个分组隐藏
        val accountingRows = listOf(
            FeatureModule.PLATFORMS,
            FeatureModule.MERCHANT_MEMORY,
            FeatureModule.RECURRING,
            FeatureModule.BUDGET
        )
        if (accountingRows.any { it !in disabledFeatures }) {
            SettingsGroup("记账管理") {
                if (FeatureModule.PLATFORMS !in disabledFeatures) {
                    SettingsNavigateRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "平台账户管理",
                        value = "$platformCount 个",
                        onClick = { showPlatformAccounts = true }
                    )
                }
                if (FeatureModule.MERCHANT_MEMORY !in disabledFeatures) {
                    SettingsNavigateRow(
                        icon = Icons.Default.Memory,
                        title = "商户记忆管理",
                        value = "查看与修正",
                        onClick = { showMerchantMemories = true }
                    )
                }
                if (FeatureModule.RECURRING !in disabledFeatures) {
                    SettingsNavigateRow(
                        icon = Icons.Default.Repeat,
                        title = "周期账单",
                        value = "启用 $activeRecurring 条",
                        onClick = { showRecurringBills = true }
                    )
                }
                if (FeatureModule.BUDGET !in disabledFeatures) {
                    SettingsNavigateRow(
                        icon = Icons.Default.Savings,
                        title = "预算管理",
                        value = totalBudget?.let { "¥%.0f/月".format(it) } ?: "未设置",
                        onClick = { showBudgetDialog = true }
                    )
                }
            }
        }

        SettingsGroup("捕获与权限") {
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

        SettingsGroup("通用") {
            SettingsRow(
                icon = Icons.Default.Palette,
                title = "主题色",
                onClick = {
                    // 每次打开弹窗，草稿重置为当前已应用的主题色
                    draftAccent = AccentColorRepository.current
                    showCustomPicker = false
                    showAccentDialog = true
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(currentAccent.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentAccent.label,
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
            SettingsNavigateRow(
                icon = Icons.Default.AutoDelete,
                title = "消息保留时长",
                value = retentionLabel(retentionDays),
                onClick = { showRetentionDialog = true }
            )
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

    if (showSupportedApps) {
        SupportedAppsDialog(onDismiss = { showSupportedApps = false })
    }

    if (showBudgetDialog) {
        BudgetDialog(viewModel = viewModel, onDismiss = { showBudgetDialog = false })
    }

    if (showAccentDialog) {
        GlassCompactDialog(
            onDismissRequest = { showAccentDialog = false },
            title = "主题色",
            text = {
                // 可滚动：展开调色板或键盘弹出时内容不会顶飞
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "已选：${draftAccent.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 20 个预设 = 4 行 × 5 列满排，两端对齐消除右侧空白
                    AccentColor.entries.chunked(5).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowPresets.forEach { preset ->
                                AccentSwatch(
                                    color = preset.primary,
                                    selected = draftAccent.id == preset.name,
                                    onClick = { draftAccent = AccentVariant.fromPreset(preset) }
                                )
                            }
                        }
                    }
                    // 自定义入口：点击在下方展开/收起调色板（不再弹独立窗口）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                            .clickable {
                                if (!showCustomPicker) {
                                    draftCustomColor = if (draftAccent.isCustom) draftAccent.primary
                                    else AccentColorRepository.lastCustom ?: AccentColor.MINT.primary
                                }
                                showCustomPicker = !showCustomPicker
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFE5484D), Color(0xFFE5B048), Color(0xFF7BC96A),
                                            Color(0xFF3FB6C9), Color(0xFF4C6FE5), Color(0xFFB45CE5),
                                            Color(0xFFE5484D)
                                        )
                                    )
                                )
                                .border(glassBorder(), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自定义颜色",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "调色板选取或输入十六进制色值",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (draftAccent.isCustom) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选自定义颜色",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = if (showCustomPicker) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showCustomPicker) "收起调色板" else "展开调色板",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // 内联调色板：展开在弹窗内部，避免多窗口叠放冲突
                    AnimatedVisibility(visible = showCustomPicker) {
                        AccentHsvPickerContent(
                            initial = draftCustomColor,
                            onColorChanged = { picked ->
                                draftCustomColor = picked
                                draftAccent = AccentVariant(AccentVariant.CUSTOM_ID, "自定义", picked)
                            }
                        )
                    }
                    Text(
                        text = "点击「使用此颜色」后生效；按钮、导航、选中态等会跟随主题色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draftAccent.isCustom) {
                        AccentColorRepository.setCustom(context, draftAccent.primary)
                    } else {
                        AccentColor.entries.firstOrNull { it.name == draftAccent.id }
                            ?.let { AccentColorRepository.setPreset(context, it) }
                    }
                    showAccentDialog = false
                }) { Text("使用此颜色") }
            },
            dismissButton = {
                TextButton(onClick = { showAccentDialog = false }) { Text("取消") }
            }
        )
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

    if (showRetentionDialog) {
        GlassCompactDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = "消息保留时长",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "超过保留时长的消息会在每日清理时删除，修改后立即清理一次。账单不受影响。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(7, 30, 90, 0).forEach { days ->
                        RetentionOptionRow(
                            label = retentionLabel(days),
                            selected = retentionDays == days,
                            onClick = {
                                retentionDays = days
                                PreferencesManager.getInstance(context)
                                    .setNotificationRetentionDays(days)
                                NotificationCleanupWorker.runNow(context)
                                showRetentionDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRetentionDialog = false }) { Text("取消") }
            }
        )
    }
}

// ── 分组与行组件 ─────────────────────────────────────────────────────────

/** 保留天数的展示文案：0 = 永久保留 */
private fun retentionLabel(days: Int): String = if (days <= 0) "永久保留" else "$days 天"

/** 消息保留时长弹窗的选项行：标签 + 选中对勾 */
@Composable
private fun RetentionOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 功能模块在设置里的展示图标（与主导航/记账页入口的图标语义一致） */
private fun FeatureModule.settingsIcon(): ImageVector = when (this) {
    FeatureModule.MESSAGES -> Icons.Default.Notifications
    FeatureModule.BILLS -> Icons.Default.Receipt
    FeatureModule.BIRTHDAY -> Icons.Default.Cake
    FeatureModule.REPORT -> Icons.Default.BarChart
    FeatureModule.BUDGET -> Icons.Default.Savings
    FeatureModule.PLATFORMS -> Icons.Default.AccountBalanceWallet
    FeatureModule.MERCHANT_MEMORY -> Icons.Default.Memory
    FeatureModule.RECURRING -> Icons.Default.Repeat
}

/** 功能开关行：图标 + 标题/说明 + 开关（「我的」·功能开关组专用） */
@Composable
private fun FeatureToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

/** 主题色色块：圆形色点，选中时深色描边并显示对勾 */
@Composable
private fun AccentSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                else glassBorder(),
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

/**
 * 自定义颜色调色板：SV 选色面板（横=饱和度，纵=明度）+ 色相滑条 + 十六进制输入。
 * 颜色状态（HSV）由本组件持有，任意途径变更都通过 [onColorChanged] 回调。
 */
@Composable
private fun AccentHsvPickerContent(
    initial: Color,
    onColorChanged: (Color) -> Unit
) {
    val initialHsv = remember {
        FloatArray(3).also { AndroidColor.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var valueF by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember {
        mutableStateOf("#%06X".format(0xFFFFFF and AndroidColor.HSVToColor(initialHsv)))
    }

    fun push(h: Float = hue, s: Float = sat, v: Float = valueF, syncHex: Boolean = true) {
        hue = h.coerceIn(0f, 360f)
        sat = s.coerceIn(0f, 1f)
        valueF = v.coerceIn(0f, 1f)
        if (syncHex) {
            hexText = "#%06X".format(
                0xFFFFFF and AndroidColor.HSVToColor(floatArrayOf(hue, sat, valueF))
            )
        }
        onColorChanged(Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, valueF))))
    }

    // 必须是单一根节点：AnimatedVisibility 等单槽容器会把多个平级子组件叠放在同一位置
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // SV 面板：底层白→纯色横渐变，叠加透明→黑纵渐变
        val pureHue = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(glassBorder(), RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        push(s = pos.x / size.width, v = 1f - pos.y / size.height)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        push(s = change.position.x / size.width, v = 1f - change.position.y / size.height)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, pureHue)))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                // 圆环指示器夹在面板内，拖到边缘时不被裁剪
                val r = 8.dp.toPx()
                drawCircle(
                    Color.White,
                    radius = r,
                    center = Offset(
                        (sat * size.width).coerceIn(r, size.width - r),
                        ((1f - valueF) * size.height).coerceIn(r, size.height - r)
                    ),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 色相滑条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(CircleShape)
                .border(glassBorder(), CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures { pos -> push(h = pos.x / size.width * 360f) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        push(h = change.position.x / size.width * 360f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    Brush.horizontalGradient(
                        List(13) { Color(AndroidColor.HSVToColor(floatArrayOf(it * 30f, 1f, 1f))) }
                    )
                )
                drawCircle(
                    Color.White,
                    radius = 7.dp.toPx(),
                    center = Offset(hue / 360f * size.width, size.height / 2f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 当前颜色预览 + 十六进制输入
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, valueF))))
                    .border(glassBorder(), CircleShape)
            )
            TextField(
                value = hexText,
                onValueChange = { input ->
                    hexText = input
                    parseHexColor(input)?.let { argb ->
                        val hsv = FloatArray(3).also { AndroidColor.colorToHSV(argb, it) }
                        push(h = hsv[0], s = hsv[1], v = hsv[2], syncHex = false)
                    }
                },
                placeholder = {
                    Text(
                        text = "#RRGGBB",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
        }
    }
}

/** 解析 #RRGGBB / RRGGBB 十六进制颜色，非法输入返回 null */
private fun parseHexColor(input: String): Int? {
    val clean = input.trim().removePrefix("#")
    val valid = clean.length == 6 && clean.all {
        it.isDigit() || it in 'a'..'f' || it in 'A'..'F'
    }
    return if (valid) 0xFF000000.toInt() or clean.toInt(16) else null
}

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
        // 组间距遵循全局 ComponentGap（Dimens.kt），标题与所属卡片之间
        // 用半间距派生值，让标题在视觉上紧贴自己的卡片
        modifier = Modifier.padding(start = 4.dp, top = ComponentGap, bottom = ComponentGap / 2)
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
