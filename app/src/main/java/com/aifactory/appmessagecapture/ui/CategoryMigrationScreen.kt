package com.aifactory.appmessagecapture.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aifactory.appmessagecapture.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryMigrationScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val billDao = AppDatabase.getDatabase(context).billDao()
    
    val categoryMapping = remember {
        mutableStateMapOf(
            "餐饮" to ExpenseCategories.FOOD,
            "交通" to ExpenseCategories.TRANSPORT,
            "购物" to ExpenseCategories.SHOPPING,
            "娱乐" to ExpenseCategories.ENTERTAINMENT,
            "生活缴费" to ExpenseCategories.LIVING,
            "医疗" to ExpenseCategories.MEDICAL,
            "其他" to ExpenseCategories.OTHER,
            "工资" to IncomeCategories.SALARY,
            "退款" to IncomeCategories.REFUND,
            "红包" to IncomeCategories.RED_PACKET,
            "理财收益" to IncomeCategories.INVESTMENT,
            "转账" to IncomeCategories.OTHER,
            "其他收入" to IncomeCategories.OTHER
        )
    }
    
    var migrationComplete by remember { mutableStateOf(false) }
    var migrationCount by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("类别迁移工具") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "类别映射设置",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "将旧类别映射到新类别，点击\"执行迁移\"按钮完成迁移。",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            categoryMapping.forEach { (oldCategory, newCategory) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = oldCategory,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = newCategory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val bills = billDao.getAllBillsOnce()
                            var count = 0
                            for (bill in bills) {
                                categoryMapping[bill.category]?.let { newCategory ->
                                    billDao.updateCategory(bill.id, newCategory)
                                    count++
                                }
                            }
                            migrationCount = count
                        }
                        migrationComplete = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !migrationComplete
            ) {
                Text("执行迁移")
            }
            
            if (migrationComplete) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "迁移完成！共迁移 $migrationCount 条记录。",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}