package com.aifactory.appmessagecapture.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.AppInfo
import com.aifactory.appmessagecapture.data.NotificationEntity
import com.aifactory.appmessagecapture.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).notificationDao()
    private val prefs = PreferencesManager.getInstance(application)

    private val searchQuery = MutableStateFlow("")

    // UI display filter (does NOT affect service capture)
    private val _filteredApps = MutableStateFlow(prefs.getFilteredApps())
    val filteredApps: StateFlow<Set<String>> = _filteredApps

    // Service-level block (used by MessageCaptureService to skip capture)
    private val _blockedApps = MutableStateFlow(prefs.getBlockedApps())
    val blockedApps: StateFlow<Set<String>> = _blockedApps

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

    val notifications: StateFlow<List<NotificationEntity>> = combine(
        searchQuery,
        _filteredApps,
        dao.getAllNotifications()
    ) { query, filtered, list ->
        list.filter { item ->
            val matchesSearch = query.isBlank() ||
                    item.appName.contains(query, ignoreCase = true) ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.content.contains(query, ignoreCase = true)
            val notFiltered = !filtered.contains(item.packageName)
            matchesSearch && notFiltered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationCount: StateFlow<Int> = dao.getNotificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadCount: StateFlow<Int> = dao.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayCount: StateFlow<Int> = dao.getAllNotifications()
        .map { list ->
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            list.count { it.timestamp >= startOfDay }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allApps: StateFlow<List<AppInfo>> = dao.getAllApps()
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
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

    fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            dao.deleteByIds(ids)
        }
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            dao.markAsRead(id)
        }
    }

    fun markAsRead(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            dao.markAsRead(ids)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            dao.markAllAsRead()
        }
    }

    fun exportToJson(context: Context, onComplete: (Uri?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = notifications.value
            val jsonArray = JSONArray()
            list.forEach { n ->
                val obj = JSONObject()
                obj.put("id", n.id)
                obj.put("packageName", n.packageName)
                obj.put("appName", n.appName)
                obj.put("title", n.title)
                obj.put("content", n.content)
                obj.put("timestamp", n.timestamp)
                obj.put("isRead", n.isRead)
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
            val list = notifications.value
            val csv = StringBuilder()
            csv.appendLine("ID,AppName,PackageName,Title,Content,Timestamp,IsRead")
            list.forEach { n ->
                csv.appendLine(
                    "${n.id},${escapeCsv(n.appName)},${escapeCsv(n.packageName)},${escapeCsv(n.title)},${escapeCsv(n.content)},${formatTime(n.timestamp)},${n.isRead}"
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
}
