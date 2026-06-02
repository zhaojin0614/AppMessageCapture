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
    private val _blockedApps = MutableStateFlow(prefs.getBlockedApps())
    val blockedApps: StateFlow<Set<String>> = _blockedApps

    val notifications: StateFlow<List<NotificationEntity>> = combine(
        searchQuery,
        _blockedApps,
        dao.getAllNotifications()
    ) { query, blocked, list ->
        list.filter { item ->
            val matchesSearch = query.isBlank() ||
                    item.appName.contains(query, ignoreCase = true) ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.content.contains(query, ignoreCase = true)
            val notBlocked = !blocked.contains(item.packageName)
            matchesSearch && notBlocked
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationCount: StateFlow<Int> = dao.getNotificationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val unreadCount: StateFlow<Int> = dao.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allApps: StateFlow<List<AppInfo>> = dao.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleBlockApp(packageName: String) {
        val current = blockedApps.value.toMutableSet()
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

    /**
     * Toggle between "select all" and "deselect all".
     * If all apps are currently allowed -> block all.
     * Otherwise -> allow all.
     */
    fun toggleSelectAll(apps: List<AppInfo>) {
        val allPackages = apps.map { it.packageName }.toSet()
        val currentBlocked = blockedApps.value

        if (currentBlocked.isEmpty()) {
            // All are currently allowed -> block all (deselect all)
            prefs.setBlockedApps(allPackages)
            _blockedApps.value = allPackages
        } else {
            // Some or all are blocked -> allow all (select all)
            prefs.setBlockedApps(emptySet())
            _blockedApps.value = emptySet()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            dao.deleteAll()
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
