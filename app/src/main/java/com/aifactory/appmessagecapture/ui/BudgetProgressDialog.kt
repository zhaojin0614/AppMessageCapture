package com.aifactory.appmessagecapture.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aifactory.appmessagecapture.data.BudgetEntity
import com.aifactory.appmessagecapture.ui.components.GlassCompactDialog
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import java.util.Locale

/** 金额显示：整数不带小数位，带小数显示两位（与 BudgetDialog 输入框一致） */
private fun amountText(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else String.format(Locale.getDefault(), "%.2f", value)

/**
 * 分类预算进度弹窗：点记账页预算卡后展示本月总预算与各分类预算的
 * 实际支出进度（每分类一条进度条），方便一眼看出哪些分类快超支。
 * 与 [BudgetDialog]（编辑）分离：查看进度是高频操作，编辑是低频操作。
 */
@Composable
fun BudgetProgressDialog(
    budgets: List<BudgetEntity>,
    categorySpend: Map<String, Double>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val categoryBudgets = budgets.filter { it.category.isNotEmpty() }
    val totalBudget = budgets.firstOrNull { it.category == "" }?.amount ?: 0.0

    // 本月各分类支出总额（用于总预算进度）
    val totalSpend = categorySpend.values.sum()

    GlassCompactDialog(
        onDismissRequest = onDismiss,
        title = "本月预算进度",
        text = {
            Column {
                // 总预算进度
                Text(
                    text = "总预算",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (totalBudget > 0) {
                    BudgetProgressRow(
                        label = "总支出",
                        spent = totalSpend,
                        budget = totalBudget,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "未设置总预算，支出 ¥%.2f".format(totalSpend),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "分类预算",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (categoryBudgets.isEmpty()) {
                    Text(
                        text = "尚未设置分类预算，点击「编辑」可设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(categoryBudgets.size) { index ->
                            val budget = categoryBudgets[index]
                            val spent = categorySpend[budget.category] ?: 0.0
                            BudgetProgressRow(
                                label = budget.category,
                                spent = spent,
                                budget = budget.amount,
                                color = getCategoryColor(budget.category)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) { Text("编辑预算") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

/** 单条预算进度：左标签 + 中进度条 + 右侧 支出/预算 与百分比 */
@Composable
private fun BudgetProgressRow(
    label: String,
    spent: Double,
    budget: Double,
    color: Color
) {
    val over = spent > budget
    val ratio = (spent / budget).toFloat().coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "¥%s / ¥%s".format(amountText(spent), amountText(budget)),
                style = MaterialTheme.typography.labelSmall,
                color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "%d%%".format((ratio * 100).toInt()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (over) ExpenseRed else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (over) ExpenseRed else color)
            )
        }
    }
}
