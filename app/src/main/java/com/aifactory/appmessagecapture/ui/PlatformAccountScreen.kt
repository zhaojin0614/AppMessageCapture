package com.aifactory.appmessagecapture.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.data.PlatformAccountEntity
import com.aifactory.appmessagecapture.ui.components.SoftButton
import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.components.SoftFab
import com.aifactory.appmessagecapture.ui.components.SoftGradientCard
import com.aifactory.appmessagecapture.ui.components.GlassCompactDialog
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.glassHighlightBrush
import com.aifactory.appmessagecapture.ui.components.isDarkTheme
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.AccentColorRepository
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import com.aifactory.appmessagecapture.ui.theme.PlatformColors

/**
 * 平台账户管理界面。
 *
 * 展示所有平台余额及总额，支持新增/编辑/删除平台账户。
 * 删除平台时关联账单会变为"待对账"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformAccountScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlatformAccountViewModel = viewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val lastDeleteLinkedCount by viewModel.lastDeleteLinkedCount.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<PlatformAccountEntity?>(null) }
    var deletingAccount by remember { mutableStateOf<PlatformAccountEntity?>(null) }
    // 删除完成后的提示（关联账单数）
    var deleteResultMessage by remember { mutableStateOf<String?>(null) }

    BackHandler { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "账户管理",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            SoftFab(
                icon = Icons.Default.Add,
                contentDescription = "添加平台",
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(bottom = 88.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 总金额大卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SoftGradientCard(
                    brush = Brush.linearGradient(
                        // 品牌渐变跟随设置页所选主色调
                        colors = listOf(
                            AccentColorRepository.current.primary,
                            AccentColorRepository.current.gradientEnd
                        )
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = 20.dp
                ) {
                    Text(
                        text = "账户总金额",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "¥${String.format("%.2f", totalBalance)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "共 ${accounts.size} 个平台",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                // 右上角装饰圆
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 20.dp, end = 20.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                )
            }

            if (accounts.isEmpty()) {
                EmptyPlatformState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(accounts, key = { it.id }) { account ->
                        PlatformAccountCard(
                            account = account,
                            onEdit = { editingAccount = account },
                            onDelete = { deletingAccount = account }
                        )
                    }
                }
            }
        }
    }
    }

    // 新增平台弹窗
    if (showAddDialog) {
        PlatformEditDialog(
            title = "添加平台",
            initialName = "",
            initialBalance = "",
            onConfirm = { name, balance ->
                viewModel.addAccount(name, balance)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // 编辑平台弹窗
    if (editingAccount != null) {
        val account = editingAccount!!
        PlatformEditDialog(
            title = "编辑平台",
            initialName = account.name,
            initialBalance = String.format("%.2f", account.balance),
            onConfirm = { name, balance ->
                viewModel.updateAccount(account, name, balance)
                editingAccount = null
            },
            onDismiss = { editingAccount = null }
        )
    }

    // 删除确认弹窗
    if (deletingAccount != null) {
        val account = deletingAccount!!
        GlassCompactDialog(
            onDismissRequest = {
                deletingAccount = null
                viewModel.consumeDeleteResult()
            },
            title = "删除平台",
            text = {
                Text(
                    "确定要删除「${account.name}」吗？\n" +
                        "该平台关联的账单将变为「待对账」状态，可后续重新分配平台。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(account.id)
                        deletingAccount = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deletingAccount = null
                    viewModel.consumeDeleteResult()
                }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除完成提示
    LaunchedEffect(lastDeleteLinkedCount) {
        if (lastDeleteLinkedCount >= 0) {
            deleteResultMessage = if (lastDeleteLinkedCount > 0) {
                "已删除平台，$lastDeleteLinkedCount 笔关联账单变为待对账"
            } else {
                "已删除平台"
            }
            viewModel.consumeDeleteResult()
        }
    }
    if (deleteResultMessage != null) {
        GlassCompactDialog(
            onDismissRequest = { deleteResultMessage = null },
            confirmButton = {
                TextButton(onClick = { deleteResultMessage = null }) { Text("知道了") }
            },
            title = "提示",
            text = { Text(deleteResultMessage!!) }
        )
    }
}

@Composable
private fun PlatformAccountCard(
    account: PlatformAccountEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 品牌色圆形图标（微信→微信绿等；无品牌色时名称哈希兜底）
            val platformColor = PlatformColors.resolveColor(account)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(platformColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = platformColor,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                val balanceColor = if (account.balance >= 0) IncomeGreen else ExpenseRed
                Text(
                    text = "¥${String.format("%.2f", account.balance)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PlatformEditDialog(
    title: String,
    initialName: String,
    initialBalance: String,
    onConfirm: (name: String, balance: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var balanceText by remember { mutableStateOf(initialBalance) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = if (isDarkTheme()) 0.90f else 0.93f
                    )
                )
                .border(glassBorder(), RoundedCornerShape(24.dp))
        ) {
            // Top light reflection (liquid glass highlight)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassHighlightBrush())
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = name,
                     colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    onValueChange = { name = it },
                    label = { Text("平台名称（如：微信钱包）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    value = balanceText,
                     colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    onValueChange = { balanceText = it },
                    label = { Text("当前余额") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("¥") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }
                    val balance = balanceText.toDoubleOrNull() ?: 0.0
                    SoftButton(
                        text = "保存",
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, balance)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlatformState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无平台账户",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "点击右下角添加各平台余额，记账时可关联扣款",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}
