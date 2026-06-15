package com.aifactory.appmessagecapture.birthday.ui

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.birthday.data.BirthdayDao
import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import com.aifactory.appmessagecapture.birthday.logic.DateCalculator
import com.aifactory.appmessagecapture.birthday.service.BirthdayAlarmScheduler
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.aifactory.appmessagecapture.birthday.widget.BirthdayWidget
import com.aifactory.appmessagecapture.birthday.widget.BirthdayWidgetReceiver
import com.aifactory.appmessagecapture.data.AppDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BirthdayKeeper 模块的 ViewModel。
 *
 * 管理生日列表数据、导航路由（列表/编辑）、多选删除模式以及与数据库的交互。
 */
class BirthdayViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: BirthdayDao = AppDatabase.getDatabase(application).birthdayDao()

    // -------------------------------------------------------------------------
    // 导航路由状态
    // -------------------------------------------------------------------------
    private val _route = MutableStateFlow<BirthdayRoute>(BirthdayRoute.List)
    val route: StateFlow<BirthdayRoute> = _route

    fun navigateTo(route: BirthdayRoute) {
        _route.value = route
        BirthdayLog.d("[BirthdayViewModel] navigateTo: %s", route)
    }

    fun navigateBack() {
        _route.value = BirthdayRoute.List
        BirthdayLog.d("[BirthdayViewModel] navigateBack to List")
    }

    // -------------------------------------------------------------------------
    // 列表数据状态（分页加载，每页20条）
    // -------------------------------------------------------------------------
    private val _loadedItems = MutableStateFlow<List<Pair<BirthdayEntity, DateCalculator.BirthdayInfo>>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private var currentOffset = 0
    private val pageSize = 20
    private var hasMore = true

    val listState: StateFlow<BirthdayListUiState> = combine(_loadedItems, _isLoading) { items, loading ->
        BirthdayListUiState(items = items, isLoading = loading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BirthdayListUiState(isLoading = true))

    init {
        loadMore()
    }

    fun loadMore() {
        if (_isLoading.value || !hasMore) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entities = dao.getBirthdaysPaged(pageSize, currentOffset)
                if (entities.isEmpty()) {
                    hasMore = false
                } else {
                    val calculated = DateCalculator.calculateAll(entities)
                    _loadedItems.value = _loadedItems.value + calculated
                    currentOffset += entities.size
                }
            } catch (e: Exception) {
                BirthdayLog.logException("[BirthdayViewModel] loadMore", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        _loadedItems.value = emptyList()
        currentOffset = 0
        hasMore = true
        loadMore()
    }

    // -------------------------------------------------------------------------
    // 多选删除模式（与其它界面保持一致）
    // -------------------------------------------------------------------------
    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIds: StateFlow<Set<Int>> = _selectedIds

    val isSelectionMode: StateFlow<Boolean> = _selectedIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleSelection(id: Int) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun enterSelectionMode(id: Int) {
        _selectedIds.value = setOf(id)
    }

    fun exitSelectionMode() {
        _selectedIds.value = emptySet()
    }

    fun selectAll(ids: List<Int>) {
        _selectedIds.value = ids.toSet()
    }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    BirthdayLog.i("[BirthdayViewModel] batch delete %d items", ids.size)
                    ids.forEach { id ->
                        BirthdayAlarmScheduler.cancel(getApplication(), id)
                        dao.deleteById(id)
                    }
                    refreshWidgets(getApplication())
                    _selectedIds.value = emptySet()
                    BirthdayLog.i("[BirthdayViewModel] batch delete success")
                    refresh()
                } catch (e: Exception) {
                    BirthdayLog.logException("[BirthdayViewModel] deleteSelected", e)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 数据库操作
    // -------------------------------------------------------------------------

    fun delete(id: Int) {
        viewModelScope.launch {
            try {
                BirthdayLog.i("[BirthdayViewModel] delete called. id=%d", id)
                BirthdayAlarmScheduler.cancel(getApplication(), id)
                dao.deleteById(id)
                refreshWidgets(getApplication())
                BirthdayLog.i("[BirthdayViewModel] delete success. id=%d, widget refreshed.", id)
                refresh()
            } catch (e: Exception) {
                BirthdayLog.logException("[BirthdayViewModel] delete", e)
            }
        }
    }

    fun save(birthday: BirthdayEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                BirthdayLog.logMethodCall(
                    "[BirthdayViewModel] save",
                    mapOf(
                        "id" to birthday.id,
                        "name" to birthday.name,
                        "isLunar" to birthday.isLunar,
                        "birthYear" to birthday.birthYear,
                        "birthMonth" to birthday.birthMonth,
                        "birthDay" to birthday.birthDay,
                        "reminderType" to birthday.reminderType,
                        "reminderTime" to birthday.reminderTime
                    )
                )
                val entityToSchedule = if (birthday.id == 0) {
                    val insertedId = dao.insert(birthday)
                    BirthdayLog.i("[BirthdayViewModel] insert success. newId=%d", insertedId)
                    birthday.copy(id = insertedId.toInt())
                } else {
                    dao.update(birthday)
                    BirthdayLog.i("[BirthdayViewModel] update success. id=%d", birthday.id)
                    birthday
                }
                BirthdayAlarmScheduler.schedule(getApplication(), entityToSchedule)
                refreshWidgets(getApplication())
                BirthdayLog.i("[BirthdayViewModel] Widget refreshed after save.")
                refresh()
                onComplete()
            } catch (e: Exception) {
                BirthdayLog.logException("[BirthdayViewModel] save", e)
            }
        }
    }

    suspend fun getById(id: Int): BirthdayEntity? {
        return try {
            BirthdayLog.d("[BirthdayViewModel] getById id=%d", id)
            dao.getById(id).also {
                BirthdayLog.d("[BirthdayViewModel] getById result=%s", it)
            }
        } catch (e: Exception) {
            BirthdayLog.logException("[BirthdayViewModel] getById", e)
            null
        }
    }

    // -------------------------------------------------------------------------
    // Widget 刷新（绕过 updateAll 的内部去重，逐个 GlanceId 强制 update）
    // -------------------------------------------------------------------------

    /**
     * 强制刷新桌面小组件。
     *
     * 策略：
     * 1. 先在 IO 线程 delay(200ms)，让 Room 事务完全落盘
     * 2. 切换到主线程，通过 [GlanceAppWidgetManager] 获取所有 [GlanceId]，
     *    使用单例 [BirthdayWidget] 逐个调用 [update]，避免多次实例化导致 session 混乱
     * 3. 若 Glance update 失败，发送 [ACTION_APPWIDGET_UPDATE] 广播兜底
     */
    private suspend fun refreshWidgets(context: android.content.Context) {
        // ① 在 IO 线程等待事务落盘
        withContext(Dispatchers.IO) {
            delay(200)
        }

        withContext(Dispatchers.Main) {
            var glanceUpdateOk = false

            // ② 使用单例 BirthdayWidget 逐个更新
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(BirthdayWidget::class.java)
                if (glanceIds.isNotEmpty()) {
                    glanceIds.forEach { glanceId ->
                        try {
                            BirthdayWidget.update(context, glanceId)
                            BirthdayLog.d(
                                "[BirthdayViewModel] refreshWidgets: BirthdayWidget.update succeeded. id=%s",
                                glanceId
                            )
                            glanceUpdateOk = true
                        } catch (e: CancellationException) {
                            BirthdayLog.d(
                                "[BirthdayViewModel] refreshWidgets: update cancelled for %s",
                                glanceId
                            )
                        } catch (e: Exception) {
                            BirthdayLog.logException(
                                "[BirthdayViewModel] refreshWidgets update $glanceId",
                                e
                            )
                        }
                    }
                } else {
                    BirthdayLog.d("[BirthdayViewModel] refreshWidgets: No GlanceIds found.")
                }
            } catch (e: Exception) {
                BirthdayLog.logException("[BirthdayViewModel] refreshWidgets GlanceAppWidgetManager", e)
            }

            // ③ 广播兜底（Glance 更新失败或没有 GlanceId 时）
            if (!glanceUpdateOk) {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, BirthdayWidgetReceiver::class.java)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                    if (appWidgetIds.isNotEmpty()) {
                        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                            component = componentName
                        }
                        context.sendBroadcast(intent)
                        BirthdayLog.d(
                            "[BirthdayViewModel] refreshWidgets: broadcast fallback sent. ids=%s",
                            appWidgetIds.contentToString()
                        )
                    }
                } catch (e: Exception) {
                    BirthdayLog.logException("[BirthdayViewModel] refreshWidgets broadcast", e)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 导入导出（供 UI 层调用）
    // -------------------------------------------------------------------------

    suspend fun exportAllToUri(context: android.content.Context, uri: android.net.Uri): Result<Int> {
        val all = dao.getAllOnce()
        return com.aifactory.appmessagecapture.birthday.utils.BackupManager.exportToUri(context, uri, all)
    }

    suspend fun importAllFromUri(context: android.content.Context, uri: android.net.Uri): Result<com.aifactory.appmessagecapture.birthday.utils.BackupManager.ImportResult> {
        val result = com.aifactory.appmessagecapture.birthday.utils.BackupManager.importFromUri(context, uri, dao)
        result.onSuccess {
            BirthdayLog.i("[BirthdayViewModel] Import success. inserted=%d, updated=%d. Refreshing widgets & rescheduling alarms...", it.inserted, it.updated)
            // ① 刷新列表数据
            refresh()
            // ② 刷新桌面组件
            refreshWidgets(context)
            // ③ 为所有记录重新设置闹钟（新导入的记录也需要注册）
            BirthdayAlarmScheduler.rescheduleAll(context)
            BirthdayLog.i("[BirthdayViewModel] Widgets refreshed and alarms rescheduled after import.")
        }.onFailure {
            BirthdayLog.w("[BirthdayViewModel] Import failed: %s", it.message)
        }
        return result
    }

    // -------------------------------------------------------------------------
    // 内部数据类
    // -------------------------------------------------------------------------
    data class BirthdayListUiState(
        val items: List<Pair<BirthdayEntity, DateCalculator.BirthdayInfo>> = emptyList(),
        val isLoading: Boolean = true
    )

    sealed class BirthdayRoute {
        data object List : BirthdayRoute()
        data class Edit(val birthdayId: Int? = null) : BirthdayRoute()
    }
}
