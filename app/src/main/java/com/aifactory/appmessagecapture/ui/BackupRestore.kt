package com.aifactory.appmessagecapture.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.ui.components.GlassCompactDialog

/** xlsx 的标准 MIME（备份导出命名 / 导入过滤共用） */
internal const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/**
 * 备份与恢复功能宿主：挂在记账页顶栏入口下。
 *
 * 必须在宿主界面**全程组合**（[show]=false 时仅不渲染弹窗）——SAF 启动器
 * 不能随弹窗一起离开组合：点弹窗动作行时弹窗先关闭再 launch，结果回调
 * 仍需送达。内含备份弹窗、覆盖恢复的二次确认弹窗与结果 Toast。
 */
@Composable
fun BackupRestoreHost(
    viewModel: BillViewModel,
    show: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val backupBusy by viewModel.backupBusy.collectAsState()
    var importOverwrite by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

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

    if (!show) return

    GlassCompactDialog(
        onDismissRequest = onDismiss,
        title = "备份与恢复",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BackupActionRow(
                    icon = Icons.Default.TableChart,
                    title = "导出表格（Excel）",
                    subtitle = "支出/收入分表 + 平台账户余额",
                    enabled = !backupBusy,
                    onClick = {
                        onDismiss()
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
                        onDismiss()
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
                        onDismiss()
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
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )

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

/** 备份弹窗的操作行：图标 + 标题 + 说明 */
@Composable
private fun BackupActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
