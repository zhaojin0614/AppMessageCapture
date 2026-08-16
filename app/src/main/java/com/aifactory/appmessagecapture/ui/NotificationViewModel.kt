package com.aifactory.appmessagecapture.ui

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.AppInfo
import com.aifactory.appmessagecapture.data.NotificationEntity
import com.aifactory.appmessagecapture.utils.PendingIntentCache
import com.aifactory.appmessagecapture.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).notificationDao()
    private val prefs = PreferencesManager.getInstance(application)

    private val searchQuery = MutableStateFlow("")
    private val _displayLimit = MutableStateFlow(30)

    fun loadMore() {
        _displayLimit.value += 30
    }

    fun refresh() {
        _displayLimit.value = 30
    }

    // UI display filter (does NOT affect service capture)
    private val _filteredApps = MutableStateFlow(prefs.getFilteredApps())
    val filteredApps: StateFlow<Set<String>> = _filteredApps

    // Service-level block (used by MessageCaptureService to skip capture)
    private val _blockedApps = MutableStateFlow(prefs.getBlockedApps())
    val blockedApps: StateFlow<Set<String>> = _blockedApps

    fun toggleBlockedApp(packageName: String) {
        val current = _blockedApps.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
            prefs.unblockApp(packageName)
        } else {
            current.add(packageName)
            prefs.blockApp(packageName)
        }
        _blockedApps.value = current
    }

    fun clearBlockedApps() {
        prefs.setBlockedApps(emptySet())
        _blockedApps.value = emptySet()
    }

    fun toggleSelectAllBlocked(apps: List<AppInfo>) {
        val allPackages = apps.map { it.packageName }.toSet()
        val currentBlocked = _blockedApps.value
        if (currentBlocked.isEmpty()) {
            prefs.setBlockedApps(allPackages)
            _blockedApps.value = allPackages
        } else {
            prefs.setBlockedApps(emptySet())
            _blockedApps.value = emptySet()
        }
    }

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    val isSelectionMode: StateFlow<Boolean> = _selectedIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun enterSelectionMode(id: Long) {
        _selectedIds.value = setOf(id)
    }

    fun exitSelectionMode() {
        _selectedIds.value = emptySet()
    }

    fun selectAll(ids: List<Long>) {
        _selectedIds.value = ids.toSet()
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isNotEmpty()) {
            deleteByIds(ids)
            _selectedIds.value = emptySet()
        }
    }

    /**
     * 当前显示的通知列表。
     *
     * 逻辑：
     * - 无筛选时：使用 limit 动态加载（默认30条），节省性能
     * - 有筛选时：SQL 层 NOT IN 过滤，避免全表拉取后在内存逐条过滤
     */
    val notifications: StateFlow<List<NotificationEntity>> = combine(
        searchQuery,
        _filteredApps,
        _displayLimit
    ) { query, filtered, limit -> Triple(query, filtered, limit) }
        .flatMapLatest { (query, filtered, limit) ->
            val excluded = filtered.toList()
            when {
                query.isBlank() && excluded.isEmpty() -> dao.getNotificationsLimit(limit)
                query.isBlank() -> dao.getNotificationsExcluding(excluded)
                excluded.isEmpty() -> dao.searchNotificationsLimit(query, limit)
                else -> dao.searchNotificationsExcluding(query, excluded)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationCount: StateFlow<Int> = dao.getNotificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * 每到次日零点自动重发当日起点，修复跨午夜后「今日」统计停留在
     * 启动那天的问题（旧实现在 VM 构造时计算一次就固定不变）。
     */
    private val startOfDayFlow: Flow<Long> = flow {
        while (true) {
            val zone = ZoneId.systemDefault()
            emit(LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli())
            val nextMidnight = LocalDate.now(zone).plusDays(1)
                .atStartOfDay(zone).toInstant().toEpochMilli()
            delay(nextMidnight - System.currentTimeMillis() + 500)
        }
    }

    val todayCount: StateFlow<Int> = startOfDayFlow
        .flatMapLatest { startOfDay -> dao.countNotificationsSince(startOfDay) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * 应用列表：基于 DISTINCT 投影查询（getAllApps），不再全表拉取通知
     * 后在内存去重。同时自动清理 _filteredApps 中已不存在的无效项。
     */
    val allApps: StateFlow<List<AppInfo>> = dao.getAllApps()
        .map { apps ->
            // 清理已不存在的筛选项
            val validPackages = apps.map { it.packageName }.toSet()
            val currentFiltered = _filteredApps.value
            val invalid = currentFiltered - validPackages
            if (invalid.isNotEmpty()) {
                val updated = currentFiltered - invalid
                _filteredApps.value = updated
                prefs.setFilteredApps(updated)
            }
            apps
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    // UI display filter methods (do NOT affect service capture)
    fun toggleFilterApp(packageName: String) {
        val current = filteredApps.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
            prefs.unfilterApp(packageName)
        } else {
            current.add(packageName)
            prefs.filterApp(packageName)
        }
        _filteredApps.value = current
    }

    fun clearFilteredApps() {
        prefs.setFilteredApps(emptySet())
        _filteredApps.value = emptySet()
    }

    /**
     * Toggle between "show all" and "hide all" for display filter.
     * If no apps are currently filtered -> filter all (hide all).
     * Otherwise -> show all.
     */
    fun toggleSelectAllFilters(apps: List<AppInfo>) {
        val allPackages = apps.map { it.packageName }.toSet()
        val currentFiltered = filteredApps.value

        if (currentFiltered.isEmpty()) {
            // All are currently shown -> hide all (filter all)
            prefs.setFilteredApps(allPackages)
            _filteredApps.value = allPackages
        } else {
            // Some or all are hidden -> show all
            prefs.setFilteredApps(emptySet())
            _filteredApps.value = emptySet()
        }
    }

    fun clearAll() {
        PendingIntentCache.clear()
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun deleteById(id: Long) {
        PendingIntentCache.remove(id)
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

    fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        PendingIntentCache.removeAll(ids)
        viewModelScope.launch {
            dao.deleteByIds(ids)
        }
    }

    /**
     * Fire the cached PendingIntent for the given notification.
     * This reproduces the same jump action as tapping the notification in the system shade.
     *
     * Returns true if the PendingIntent was found and fired, false otherwise.
     */
    fun firePendingIntent(context: Context, notificationId: Long): Boolean {
        val pendingIntent = PendingIntentCache.get(notificationId) ?: return false
        try {
            pendingIntent.send(
                context,
                0,
                null,
                null,  // no callback needed
                null   // no handler — use main thread
            )
            return true
        } catch (e: PendingIntent.CanceledException) {
            // The originating app cancelled this PendingIntent — no longer valid.
            PendingIntentCache.remove(notificationId)
            return false
        }
    }

    /**
     * Fallback: launch the target app's main activity when no PendingIntent is cached.
     * This happens for notifications restored from the database after an app restart.
     */
    fun launchApp(context: Context, packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "无法打开该应用", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportToJson(context: Context, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dao.getAllNotificationsOnce()
            val jsonArray = JSONArray()
            list.forEach { n ->
                val obj = JSONObject()
                obj.put("id", n.id)
                obj.put("packageName", n.packageName)
                obj.put("appName", n.appName)
                obj.put("title", n.title)
                obj.put("content", n.content)
                obj.put("timestamp", n.timestamp)
                jsonArray.put(obj)
            }
            val file = File(context.cacheDir, "notifications_export_${System.currentTimeMillis()}.json")
            file.writeText(jsonArray.toString(2))
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            withContext(Dispatchers.Main) { onComplete(uri) }
        }
    }

    fun exportToCsv(context: Context, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dao.getAllNotificationsOnce()
            val csv = StringBuilder()
            csv.appendLine("ID,AppName,PackageName,Title,Content,Timestamp")
            list.forEach { n ->
                csv.appendLine(
                    "${n.id},${escapeCsv(n.appName)},${escapeCsv(n.packageName)},${escapeCsv(n.title)},${escapeCsv(n.content)},${formatTime(n.timestamp)}"
                )
            }
            val file = File(context.cacheDir, "notifications_export_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { it.write(csv.toString()) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            withContext(Dispatchers.Main) { onComplete(uri) }
        }
    }

    fun shareUri(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uri) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享导出文件")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    companion object {
        fun computeStartOfDay(): Long {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
