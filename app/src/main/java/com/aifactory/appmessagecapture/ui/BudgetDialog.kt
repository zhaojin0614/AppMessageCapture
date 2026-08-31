package com.aifactory.appmessagecapture.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifactory.appmessagecapture.ui.components.GlassCompactDialog
import java.util.Locale

/**
 * 月度预算编辑弹窗：总预算 + 各支出分类预算，留空表示不设该项。
 * 同时展示本月各分类已支出金额，方便对照调整。
 */
@Composable
fun BudgetDialog(
    viewModel: BillViewModel,
    onDismiss: () -> Unit
) {
    val budgets by viewModel.budgets.collectAsState()
    val categorySpend by viewModel.monthCategorySpend.collectAsState()
    val expenseCategories = ExpenseCategories.all

    fun amountText(value: Double?): String =
        value?.let { if (it % 1.0 == 0.0) it.toLong().toString() else String.format(Locale.getDefault(), "%.2f", it) } ?: ""

    // 本地编辑态：以当前预算初始化，保存时统一提交
    var totalText by remember(budgets) { mutableStateOf(amountText(budgets.firstOrNull { it.category == "" }?.amount)) }
    var categoryTexts by remember(budgets) {
        mutableStateOf(expenseCategories.associateWith { cat -> amountText(budgets.firstOrNull { it.category == cat }?.amount) })
    }

    GlassCompactDialog(
        onDismissRequest = onDismiss,
        title = "月度预算",
        text = {
            Column {
                Text(
                    text = "月度总预算",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                BudgetTextField(
                    value = totalText,
                    onValueChange = { totalText = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "分类预算（可选）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(expenseCategories.size) { index ->
                        val category = expenseCategories[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(getCategoryColor(category), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val spent = categorySpend[category] ?: 0.0
                                Text(
                                    text = "本月已用 ¥%.2f".format(spent),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BudgetTextField(
                                value = categoryTexts[category] ?: "",
                                onValueChange = { new -> categoryTexts = categoryTexts.toMutableMap().apply { put(category, new) } },
                                modifier = Modifier.width(84.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "留空表示不设该项预算；达到 80%/100% 时会提醒",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val entries = mutableMapOf<String, Double?>()
                entries[""] = totalText.toDoubleOrNull()?.takeIf { it > 0 }
                categoryTexts.forEach { (category, text) ->
                    entries[category] = text.toDoubleOrNull()?.takeIf { it > 0 }
                }
                viewModel.saveBudgets(entries)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 数字预算输入框：仅允许数字与小数点 */
@Composable
private fun BudgetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = { new -> if (new.all { it.isDigit() || it == '.' }) onValueChange(new) },
        placeholder = {
            Text(
                text = "金额",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        prefix = {
            Text(text = "¥", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.outline,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.fillMaxWidth()
    )
}
