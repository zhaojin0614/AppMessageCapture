package com.aifactory.appmessagecapture.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.MerchantMemoryOverrideEntity
import com.aifactory.appmessagecapture.data.MemoryGroupRow
import com.aifactory.appmessagecapture.data.PlatformAccountEntity
import com.aifactory.appmessagecapture.service.MerchantMemory
import com.aifactory.appmessagecapture.ui.components.SoftCard
import com.aifactory.appmessagecapture.ui.theme.ExpenseRed
import com.aifactory.appmessagecapture.ui.theme.IncomeGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 商户记忆管理界面：把隐式的商户记忆（按商户键/指纹从账单统计）可视化。
 *
 * 每条记忆展示：商户名（跨商户指纹聚合）、当前生效的分类与平台、账单数、
 * 最近时间、手动覆写标记。点击可手动覆写分类/平台、忽略该商户的自动记忆，
 * 或清除覆写恢复统计推断。覆写优先级高于一切自动推断（BillIngestor 查询）。
 */
class MerchantMemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val billDao = db.billDao()
    private val overrideDao = db.merchantMemoryOverrideDao()
    private val platformDao = db.platformAccountDao()

    private val _items = MutableStateFlow<List<MerchantMemoryUiItem>>(emptyList())
    val items: StateFlow<List<MerchantMemoryUiItem>> = _items

    private val _platforms = MutableStateFlow<List<PlatformAccountEntity>>(emptyList())
    val platforms: StateFlow<List<PlatformAccountEntity>> = _platforms

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val groups = billDao.findMemoryGroups(MEMORY_GROUP_LIMIT)
            val overrides = overrideDao.getAllOnce().associateBy { it.fingerprint }
            val platforms = platformDao.getAllOnce()
            _platforms.value = platforms
            _items.value = aggregate(groups, overrides, platforms)
        }
    }

    fun saveOverride(fingerprint: String, category: String?, platformId: Long?, ignoreAuto: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = overrideDao.getByFingerprint(fingerprint)
            val entity = MerchantMemoryOverrideEntity(
                fingerprint = fingerprint,
                category = category,
                platformAccountId = platformId,
                ignoreAuto = ignoreAuto,
                updatedAt = System.currentTimeMillis()
            )
            if (category == null && platformId == null && !ignoreAuto) {
                overrideDao.delete(fingerprint)
            } else if (existing != null) {
                overrideDao.update(entity)
            } else {
                overrideDao.upsert(entity)
            }
            refresh()
        }
    }

    private fun aggregate(
        groups: List<MemoryGroupRow>,
        overrides: Map<String, MerchantMemoryOverrideEntity>,
        platforms: List<PlatformAccountEntity>
    ): List<MerchantMemoryUiItem> {
        data class Member(val key: String, val category: String?, val platformId: Long?, val count: Int, val last: Long)

        val byFingerprint = LinkedHashMap<String, MutableList<Member>>()
        groups.forEach { g ->
            val core = MerchantMemory.coreFromKey(g.merchantKey) ?: return@forEach
            byFingerprint.getOrPut(core) { mutableListOf() }.add(
                Member(g.merchantKey, g.category, g.platformAccountId, g.billCount, g.lastTimestamp)
            )
        }
        return byFingerprint.map { (core, members) ->
            val override = overrides[core]
            // 展示名取账单最多的成员键的标题部分（去掉"App|"前缀）
            val displayKey = members.maxByOrNull { it.count }?.key ?: core
            val displayName = displayKey.substringAfter('|', core)
            // 分类：覆写优先，否则成员最近分类众数（平票取账单更多的成员）
            val categoryVotes = members.groupingBy { it.category }.eachCount()
            val autoCategory = members
                .sortedWith(compareByDescending<Member> { categoryVotes[it.category] }.thenByDescending { it.count })
                .firstOrNull()?.category
            // 平台：覆写优先，否则最近有平台的成员
            val autoPlatform = members.filter { it.platformId != null }.maxByOrNull { it.last }?.platformId
            MerchantMemoryUiItem(
                fingerprint = core,
                displayName = displayName,
                category = override?.category ?: autoCategory,
                platformId = override?.platformAccountId ?: autoPlatform,
                platformName = (override?.platformAccountId ?: autoPlatform)?.let { pid ->
                    platforms.firstOrNull { it.id == pid }?.name
                },
                channelPackage = members.maxByOrNull { it.last }?.let { m ->
                    groups.firstOrNull { g -> g.merchantKey == m.key }?.channelPackage
                },
                billCount = members.sumOf { it.count },
                lastTimestamp = members.maxOf { it.last },
                override = override
            )
        }.sortedByDescending { it.lastTimestamp }
    }

    private companion object {
        /** 管理界面最多展示的商户键分组数 */
        const val MEMORY_GROUP_LIMIT = 300
    }
}

/** 管理界面的一条聚合记忆 */
data class MerchantMemoryUiItem(
    val fingerprint: String,
    val displayName: String,
    val category: String?,
    val platformId: Long?,
    val platformName: String?,
    val channelPackage: String?,
    val billCount: Int,
    val lastTimestamp: Long,
    val override: MerchantMemoryOverrideEntity?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantMemoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantMemoryViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val platforms by viewModel.platforms.collectAsState()
    var editing by remember { mutableStateOf<MerchantMemoryUiItem?>(null) }

    BackHandler { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "商户记忆管理",
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "还没有商户记忆",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "自动捕获或手动记账后，同商户的分类与平台\n会在这里沉淀为记忆，可随时手动修正",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items.size, key = { items[it].fingerprint }) { index ->
                        val item = items[index]
                        MerchantMemoryItem(
                            item = item,
                            onClick = { editing = item }
                        )
                    }
                    item {
                        Text(
                            text = "记忆由账单统计得出；在账单里纠正分类或平台会自动更新对应记忆。手动覆写优先于自动推断。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    editing?.let { item ->
        MerchantMemoryEditDialog(
            item = item,
            platforms = platforms,
            onConfirm = { category, platformId, ignoreAuto ->
                viewModel.saveOverride(item.fingerprint, category, platformId, ignoreAuto)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun MerchantMemoryItem(item: MerchantMemoryUiItem, onClick: () -> Unit) {
    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        contentPadding = 14.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    if (item.override != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier
                        ) {
                            Text(
                                text = "已手动设定",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${item.billCount} 笔 · 最近 ${formatMemoryDate(item.lastTimestamp)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.channelPackage?.let { ch ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "渠道 ${channelLabel(ch)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.category ?: "未学习",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = item.category?.let { getCategoryColor(it) }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.platformName ?: "待对账",
                    fontSize = 12.sp,
                    color = if (item.platformName != null) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

@Composable
private fun MerchantMemoryEditDialog(
    item: MerchantMemoryUiItem,
    platforms: List<PlatformAccountEntity>,
    onConfirm: (category: String?, platformId: Long?, ignoreAuto: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(item.override?.category ?: item.category) }
    var selectedPlatformId by remember { mutableStateOf(item.override?.platformAccountId ?: item.platformId) }
    var ignoreAuto by remember { mutableStateOf(item.override?.ignoreAuto == true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = item.displayName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "${item.billCount} 笔账单 · 最近 ${formatMemoryDate(item.lastTimestamp)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "分类", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowChipRow(
                    options = listOf<Pair<String?, String>>(null to "跟随推断") +
                        (ExpenseCategories.all + IncomeCategories.all).map { it to it },
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "扣款平台", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowChipRow(
                    options = listOf<Pair<Long?, String>>(null to "跟随推断") +
                        platforms.map { it.id to it.name },
                    selected = selectedPlatformId,
                    onSelect = { selectedPlatformId = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "忽略此商户的自动记忆", fontSize = 14.sp)
                        Text(
                            text = "开启后不再套用统计出的分类/平台",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = ignoreAuto, onCheckedChange = { ignoreAuto = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedCategory, selectedPlatformId, ignoreAuto) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 弹窗内的单行流式选择 chips；value 为 null 的选项表示「跟随推断」 */
@Composable
internal fun <T : Any> FlowChipRow(
    options: List<Pair<T?, String>>,
    selected: T?,
    onSelect: (T?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private val memoryDateFormat = DateTimeFormatter.ofPattern("M月d日")

private fun formatMemoryDate(timestamp: Long): String =
    memoryDateFormat.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

/** 渠道包名 → 展示名（未知的显示包名尾段） */
private fun channelLabel(packageName: String): String = when (packageName) {
    "com.tencent.mm" -> "微信"
    "com.eg.android.AlipayGphone" -> "支付宝"
    "com.unionpay" -> "云闪付"
    "com.android.bankabc" -> "农业银行"
    "com.sankuai.meituan", "com.sankuai.meituan.takeoutnew" -> "美团"
    "com.jingdong.app.mall" -> "京东"
    "com.xunmeng.pinduoduo" -> "拼多多"
    else -> packageName.substringAfterLast('.').ifEmpty { packageName }
}
