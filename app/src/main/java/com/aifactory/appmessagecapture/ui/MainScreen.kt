package com.aifactory.appmessagecapture.ui

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class NotificationListItem {
    data class Header(val date: String) : NotificationListItem()
    data class Item(val notification: NotificationEntity) : NotificationListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: NotificationViewModel = viewModel()) {
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll-to-top visibility
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    // Auto mark-as-read when items become visible (debounced on scroll stop)
    LaunchedEffect(notifications) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { scrolling ->
                if (!scrolling) {
                    delay(300L)
                    val visible = listState.layoutInfo.visibleItemsInfo
                    val visibleIds = visible.mapNotNull { info ->
                        val idx = info.index
                        val grouped = buildGroupedList(notifications)
                        val item = grouped.getOrNull(idx)
                        (item as? NotificationListItem.Item)?.notification
                    }.filter { !it.isRead }.map { it.id }
                    if (visibleIds.isNotEmpty()) {
                        viewModel.markAsRead(visibleIds)
                    }
                }
            }
    }

    val groupedList = remember(notifications) {
        buildGroupedList(notifications)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // Mark all as read
                    if (unreadCount > 0) {
                        IconButton(onClick = { viewModel.markAllAsRead() }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "一键已读",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    // Export button
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = stringResource(R.string.export),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Filter button
                    BadgedBox(
                        badge = {
                            if (blockedApps.isNotEmpty()) {
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
                    // Settings button
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
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(index = 0)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.scroll_to_top)
                        )
                    }
                }
                if (notifications.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showClearDialog = true },
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_all))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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
                            is NotificationListItem.Item -> NotificationCard(
                                notification = item.notification,
                                onClick = { viewModel.markAsRead(item.notification.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空") },
            text = { Text("确定要清空所有已捕获的消息吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    }
                ) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showFilterDialog) {
        AppFilterDialog(
            apps = allApps,
            blockedApps = blockedApps,
            onToggle = { viewModel.toggleBlockApp(it) },
            onToggleAll = { viewModel.toggleSelectAll(allApps) },
            onDismiss = { showFilterDialog = false }
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
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Load app icon from package manager, cached by packageName
    val iconBitmap = remember(notification.packageName) {
        try {
            context.packageManager.getApplicationIcon(notification.packageName)
                ?.toBitmap(width = 192, height = 192)
                ?.asImageBitmap()
        } catch (_: Exception) { null }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 1.dp
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon (real icon from package) with fallback to first letter
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (iconBitmap == null)
                            if (notification.isRead) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primaryContainer
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
                        color = if (notification.isRead)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.appName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
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

                Spacer(modifier = Modifier.height(2.dp))

                if (notification.title.isNotBlank()) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
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

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
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
                color = MaterialTheme.colorScheme.primaryContainer
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
    blockedApps: Set<String>,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAllSelected = blockedApps.isEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_apps)) },
        text = {
            Column {
                Text(
                    text = "勾选表示允许捕获和显示，取消勾选表示屏蔽该应用",
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
                                checked = !blockedApps.contains(app.packageName),
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
                    text = if (isAllSelected)
                        stringResource(R.string.deselect_all)
                    else
                        stringResource(R.string.select_all)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    onClick = onExportJson,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
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
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
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
    return when {
        diff < 60000 -> "刚刚"
        diff < 3600000 -> "${diff / 60000}分钟前"
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 172800000 -> "昨天"
        else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
    }
}
