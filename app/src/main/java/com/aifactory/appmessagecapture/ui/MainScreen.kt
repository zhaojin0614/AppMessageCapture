@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aifactory.appmessagecapture.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aifactory.appmessagecapture.R
import com.aifactory.appmessagecapture.data.NotificationEntity
import com.aifactory.appmessagecapture.ui.components.SoftFab
import com.aifactory.appmessagecapture.ui.components.SwipeableItem
import com.aifactory.appmessagecapture.ui.components.GlassAlertDialog
import com.aifactory.appmessagecapture.ui.components.glassBorder
import com.aifactory.appmessagecapture.ui.components.glassFill
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

sealed class NotificationListItem {
    data class Header(val date: String) : NotificationListItem()
    data class Item(val notification: NotificationEntity) : NotificationListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = viewModel()
) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    val count by viewModel.notificationCount.collectAsState()
    val todayCount by viewModel.todayCount.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var notificationToDelete by remember { mutableStateOf<NotificationEntity?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Pull-down stats panel
    val pullOffset = remember { Animatable(0f) }
    val maxPullOffsetPx = with(LocalDensity.current) { 80.dp.toPx() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0 && listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0 &&
                    pullOffset.value < maxPullOffsetPx
                ) {
                    scope.launch {
                        pullOffset.snapTo(
                            (pullOffset.value + delta).coerceAtMost(maxPullOffsetPx)
                        )
                    }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 不再通过下滑收回面板，仅由松手时自动收回
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && pullOffset.value > 0f) {
            // 松手时自动收回，不再保留展开状态
            pullOffset.animateTo(0f, animationSpec = tween(250))
        }
    }

    // Auto collapse stats panel when list scrolls away from top
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if ((listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0) && pullOffset.value > 0f) {
            pullOffset.animateTo(0f, animationSpec = tween(200))
        }
    }

    // Scroll-to-top visibility
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    // Load more when scrolling near bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 5 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    val groupedList = remember(notifications) {
        buildGroupedList(notifications)
    }

    // Pressing back during selection mode exits selection, not the app
    BackHandler(enabled = isSelectionMode) {
        viewModel.exitSelectionMode()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            topBar = {
            TopAppBar(
                title = {
                    if (isSelectionMode) {
                        Text(
                            text = "已选择 ${selectedIds.size} 项",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    if (isSelectionMode) {
                        TextButton(onClick = {
                            val visibleIds = notifications.map { it.id }
                            if (selectedIds.containsAll(visibleIds)) {
                                viewModel.exitSelectionMode()
                            } else {
                                viewModel.selectAll(visibleIds)
                            }
                        }) {
                            Text(
                                text = if (selectedIds.containsAll(notifications.map { it.id })) "取消全选" else "全选",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { showDeleteSelectedDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除选中",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "取消",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = stringResource(R.string.export),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BadgedBox(
                            badge = {
                                if (filteredApps.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        ) {
                            IconButton(onClick = { showFilterDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filter_apps),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        BadgedBox(
                            badge = {
                                if (blockedApps.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error)
                                }
                            }
                        ) {
                            IconButton(onClick = { showBlockedDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DoNotDisturbOn,
                                    contentDescription = "屏蔽管理",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            startActivity(context, intent, null)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(modifier = Modifier.padding(bottom = 88.dp), horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    SoftFab(
                        icon = Icons.Default.ArrowUpward,
                        contentDescription = stringResource(R.string.scroll_to_top),
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(index = 0)
                            }
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                if (notifications.isNotEmpty()) {
                    SoftFab(
                        icon = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.clear_all),
                        onClick = { showClearDialog = true },
                        backgroundColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pull-down stats panel
            PullDownStatsPanel(
                pullOffset = pullOffset.value,
                maxPullOffset = maxPullOffsetPx,
                count = count,
                todayCount = todayCount,
                appCount = allApps.size
            )

            // Search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .border(glassBorder(), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = glassFill(),
                shadowElevation = 0.dp
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.updateSearchQuery(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.updateSearchQuery("")
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            if (notifications.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = groupedList,
                        key = { item ->
                            when (item) {
                                is NotificationListItem.Header -> "hdr_${item.date}"
                                is NotificationListItem.Item -> "item_${item.notification.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is NotificationListItem.Header -> DateHeader(item.date)
                            is NotificationListItem.Item -> SwipeableItem(
                                isSelectionMode = isSelectionMode,
                                onDelete = {
                                    notificationToDelete = item.notification
                                }
                            ) {
                                NotificationCard(
                                    notification = item.notification,
                                    isSelected = selectedIds.contains(item.notification.id),
                                    isSelectionMode = isSelectionMode,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleSelection(item.notification.id)
                                        } else {
                                            // Try to replay the original notification click (PendingIntent).
                                            // Falls back to launching the app if the PendingIntent is unavailable.
                                            val fired = viewModel.firePendingIntent(context, item.notification.id)
                                            if (!fired) {
                                                viewModel.launchApp(context, item.notification.packageName)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            viewModel.enterSelectionMode(item.notification.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showClearDialog) {
        val visibleIds = notifications.map { it.id }
        val isFiltered = visibleIds.size < count
        GlassAlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(if (isFiltered) "确认删除筛选结果" else "确认清空") },
            text = {
                Text(
                    if (isFiltered)
                        "确定要删除当前筛选/搜索到的 ${visibleIds.size} 条消息吗？此操作不可恢复。"
                    else
                        "确定要清空所有已捕获的消息吗？此操作不可恢复。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isFiltered) {
                            viewModel.deleteByIds(visibleIds)
                        } else {
                            viewModel.clearAll()
                        }
                        showClearDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Single item delete confirmation
    if (notificationToDelete != null) {
        GlassAlertDialog(
            onDismissRequest = { notificationToDelete = null },
            title = { Text("删除消息") },
            text = { Text("确定要删除这条消息吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        notificationToDelete?.let { viewModel.deleteById(it.id) }
                        notificationToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { notificationToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Multi-select delete confirmation
    if (showDeleteSelectedDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("删除选中消息") },
            text = { Text("确定要删除选中的 ${selectedIds.size} 条消息吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteSelectedDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showFilterDialog) {
        AppFilterDialog(
            apps = allApps,
            filteredApps = filteredApps,
            onToggle = { viewModel.toggleFilterApp(it) },
            onToggleAll = { viewModel.toggleSelectAllFilters(allApps) },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showBlockedDialog) {
        BlockedAppsDialog(
            apps = allApps,
            blockedApps = blockedApps,
            onToggle = { viewModel.toggleBlockedApp(it) },
            onToggleAll = { viewModel.toggleSelectAllBlocked(allApps) },
            onDismiss = { showBlockedDialog = false }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            onExportJson = {
                viewModel.exportToJson(context) { uri ->
                    uri?.let { viewModel.shareUri(context, it) }
                        ?: Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
                showExportDialog = false
            },
            onExportCsv = {
                viewModel.exportToCsv(context) { uri ->
                    uri?.let { viewModel.shareUri(context, it) }
                        ?: Toast.makeText(context, "导出失败", Toast.LENGTH_SHORT).show()
                }
                showExportDialog = false
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun PullDownStatsPanel(
    pullOffset: Float,
    maxPullOffset: Float,
    count: Int,
    todayCount: Int,
    appCount: Int
) {
    if (pullOffset <= 0f) return

    val progress = (pullOffset / maxPullOffset).coerceIn(0f, 1f)
    val panelHeight = with(LocalDensity.current) { pullOffset.toDp() }
    val contentAlpha = progress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight),
        contentAlignment = Alignment.Center
    ) {
        if (contentAlpha > 0f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(
                    value = count.toString(),
                    label = "总消息",
                    alpha = contentAlpha
                )
                StatItem(
                    value = todayCount.toString(),
                    label = "今日",
                    alpha = contentAlpha
                )
                StatItem(
                    value = appCount.toString(),
                    label = "应用",
                    alpha = contentAlpha
                )
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, alpha: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            maxLines = 1
        )
    }
}

private fun buildGroupedList(notifications: List<NotificationEntity>): List<NotificationListItem> {
    if (notifications.isEmpty()) return emptyList()
    return buildList {
        var lastDate = ""
        notifications.forEach { n ->
            val date = formatDateHeader(n.timestamp)
            if (date != lastDate) {
                add(NotificationListItem.Header(date))
                lastDate = date
            }
            add(NotificationListItem.Item(n))
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val iconBitmap = remember(notification.packageName) {
        try {
            context.packageManager.getApplicationIcon(notification.packageName)
                ?.toBitmap(width = 192, height = 192)
                ?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else glassFill()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected)
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        else glassBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            // Rounded-square app icon (Android 12+ style)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (iconBitmap == null)
                            MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = notification.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.appName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatShortTime(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                if (notification.title.isNotBlank()) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.empty_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.enable_service_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}

@Composable
fun AppFilterDialog(
    apps: List<com.aifactory.appmessagecapture.data.AppInfo>,
    filteredApps: Set<String>,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAllShown = filteredApps.isEmpty()

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_apps)) },
        text = {
            Column {
                Text(
                    text = "勾选表示在列表中显示该应用的消息，取消勾选表示隐藏",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    apps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = !filteredApps.contains(app.packageName),
                                onCheckedChange = { onToggle(app.packageName) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onToggleAll()
            }) {
                Text(
                    text = if (isAllShown)
                        stringResource(R.string.deselect_all)
                    else
                        stringResource(R.string.select_all)
                )
            }
        }
    )
}

@Composable
fun BlockedAppsDialog(
    apps: List<com.aifactory.appmessagecapture.data.AppInfo>,
    blockedApps: Set<String>,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val isNoneBlocked = blockedApps.isEmpty()

    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("屏蔽管理") },
        text = {
            Column {
                Text(
                    text = "勾选的应用将不会被捕获通知（服务级别屏蔽）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    apps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = blockedApps.contains(app.packageName),
                                onCheckedChange = { onToggle(app.packageName) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onToggleAll) {
                Text(
                    text = if (isNoneBlocked) "全部屏蔽" else "全部取消"
                )
            }
        }
    )
}

@Composable
fun ExportDialog(
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    onClick = onExportJson,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                    ),
                    border = glassBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.export_json),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Card(
                    onClick = onExportCsv,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                    ),
                    border = glassBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.export_csv),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun formatDateHeader(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dateStr = sdf.format(Date(timestamp))
    val todayStr = sdf.format(Date())
    val yesterdayStr = sdf.format(Date(System.currentTimeMillis() - 86400000))
    return when (dateStr) {
        todayStr -> "今天"
        yesterdayStr -> "昨天"
        else -> dateStr
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatShortTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 60000) return "刚刚"
    if (diff < 3600000) return "${diff / 60000}分钟前"

    val zoned = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val dayDiff = ChronoUnit.DAYS.between(zoned.toLocalDate(), LocalDate.now())
    val timeStr = DateTimeFormatter.ofPattern("HH:mm").format(zoned)
    return when (dayDiff) {
        0L -> timeStr
        1L -> "昨天 $timeStr"
        else -> DateTimeFormatter.ofPattern("MM/dd HH:mm").format(zoned)
    }
}
