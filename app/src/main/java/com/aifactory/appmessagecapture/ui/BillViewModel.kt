package com.aifactory.appmessagecapture.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.data.AccountRepository
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.data.PlatformAccountEntity
import com.aifactory.appmessagecapture.service.BillNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val billDao = db.billDao()
    private val platformDao = db.platformAccountDao()
    private val repository = AccountRepository(db, billDao, platformDao)

    private val _weeksToLoad = MutableStateFlow(1)

    companion object {
        /** 筛选视图每页条数（按时间倒序的 LIMIT 分页） */
        private const val FILTER_PAGE_SIZE = 30
    }

    // ── 记账页筛选状态（null = 不过滤该维度）────────────────────────────
    private val _typeFilter = MutableStateFlow<Boolean?>(null)     // true=收入 false=支出
    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _filterLimit = MutableStateFlow(FILTER_PAGE_SIZE)

    /** UI 调用：切换 全部/支出/收入 类型筛选（重置筛选分页） */
    fun setTypeFilter(typeLabel: String?) {
        _typeFilter.value = when (typeLabel) {
            "收入" -> true
            "支出" -> false
            else -> null
        }
        _filterLimit.value = FILTER_PAGE_SIZE
    }

    /** UI 调用：切换分类筛选（null/「全部」= 不过滤；重置筛选分页） */
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category?.takeIf { it != "全部" }
        _filterLimit.value = FILTER_PAGE_SIZE
    }

    /**
     * 记账页账单列表，两种分页策略：
     *
     * - 无任何筛选：按周时间窗口分页（[_weeksToLoad]，默认最近 1 周，
     *   滚动到底加载更多周），避免全量列表常驻内存；
     * - 有筛选：按条数 LIMIT 分页（[_filterLimit]，默认 30 条，滚动到底
     *   增大 limit）。不能用周窗口切筛选——窗口内没有目标类型账单时
     *   结果会恒空（如最近一周无收入却筛选收入），条数分页没有此问题。
     */
    /** 一次账单列表查询的全部参数（任一变化即重查） */
    private data class BillQuery(
        val type: Boolean?,
        val category: String?,
        val weeks: Int,
        val filterLimit: Int
    )

    val bills: StateFlow<List<BillEntity>> = combine(
        _typeFilter, _categoryFilter, _weeksToLoad, _filterLimit, ::BillQuery
    ).flatMapLatest { q ->
        if (q.type == null && q.category == null) {
            val since = System.currentTimeMillis() - q.weeks * 7L * 24 * 60 * 60 * 1000
            billDao.getBillsSince(since)
        } else {
            billDao.getBillsFiltered(q.type, q.category, q.filterLimit)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 滚动到底部加载更多：无筛选时扩周窗口，有筛选时增大条数分页 */
    fun loadMore() {
        if (_typeFilter.value != null || _categoryFilter.value != null) {
            _filterLimit.value += FILTER_PAGE_SIZE
        } else {
            loadMoreWeeks()
        }
    }

    fun loadMoreWeeks() {
        // 上限 520 周（约 10 年），防止异常数据导致窗口无限膨胀
        if (_weeksToLoad.value < 520) _weeksToLoad.value += 1
    }

    val totalExpense: StateFlow<Double> = billDao.getTotalExpense()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIncome: StateFlow<Double> = billDao.getTotalIncome()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * 所有平台账户余额之和，展示在记账界面下拉面板。
     */
    val totalAccountBalance: StateFlow<Double> = platformDao.getTotalBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * 平台账户列表，供添加账单/对账时选择扣款平台。
     */
    val platforms: StateFlow<List<PlatformAccountEntity>> = platformDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Reactive startOfMonth that re-emits at every month boundary,
     * ensuring this month's stats stay correct across month changes.
     */
    private val reactiveStartOfMonth: StateFlow<Long> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            val start = computeStartOfMonth(now)
            emit(start)
            // 下月 1 日零点触发刷新
            val nextMonthStart = computeStartOfMonth(start + 35L * 24 * 60 * 60 * 1000)
            delay(nextMonthStart - now + 1000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), computeStartOfMonth(System.currentTimeMillis()))

    private fun computeStartOfMonth(nowMillis: Long): Long =
        java.time.Instant.ofEpochMilli(nowMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /** 本月支出合计（记账界面大卡片展示） */
    val monthExpense: StateFlow<Double> = reactiveStartOfMonth
        .flatMapLatest { start ->
            billDao.getMonthExpense(start).map { it ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** 本月收入合计（记账界面大卡片展示） */
    val monthIncome: StateFlow<Double> = reactiveStartOfMonth
        .flatMapLatest { start ->
            billDao.getMonthIncome(start).map { it ?: 0.0 }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val expenseCount: StateFlow<Int> = billDao.getExpenseCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val incomeCount: StateFlow<Int> = billDao.getIncomeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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
            viewModelScope.launch(Dispatchers.IO) {
                // 走 repository 逐个删除，保证已对账账单的余额回滚
                ids.forEach { repository.deleteBillWithRollback(it) }
                // 联动清除被删账单的常驻通知，避免通知栏残留死入口
                BillNotificationHelper.cancelNotificationsForBills(getApplication(), ids)
            }
            _selectedIds.value = emptySet()
        }
    }

    fun addBill(bill: BillEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBillWithPlatform(bill)
        }
    }

    fun updateCategory(id: Long, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            billDao.updateCategory(id, category)
        }
    }

    fun updateTitle(id: Long, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            billDao.updateTitle(id, title)
        }
    }

    /**
     * 修改账单金额，已对账平台余额同步按差值调整。
     */
    fun updateAmount(id: Long, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBillAmount(id, amount)
        }
    }

    /**
     * 为账单分配扣款平台（对账）。传 null 表示取消对账。
     * 会自动回滚旧平台余额并应用新平台余额。
     */
    fun reconcileBill(billId: Long, platformId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.reconcileBill(billId, platformId)
        }
    }

    fun deleteBill(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBillWithRollback(id)
            // 联动清除该账单的常驻通知
            BillNotificationHelper.cancelNotificationsForBills(getApplication(), listOf(id))
        }
    }

    fun deleteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            billDao.deleteAll()
            // 清空账单时移除全部账单通知
            BillNotificationHelper.cancelNotificationsForBills(getApplication(), null)
        }
    }
}
