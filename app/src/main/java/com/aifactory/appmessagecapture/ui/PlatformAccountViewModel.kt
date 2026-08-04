package com.aifactory.appmessagecapture.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.data.AccountRepository
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.PlatformAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 平台账户管理 ViewModel。
 *
 * 暴露账户列表与总余额（所有平台余额之和），并提供增删改操作。
 * 所有写操作走 [AccountRepository] 以保证与账单联动的事务一致性。
 */
class PlatformAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val platformDao = db.platformAccountDao()
    private val repository = AccountRepository(db, db.billDao(), platformDao)

    val accounts: StateFlow<List<PlatformAccountEntity>> = platformDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalance: StateFlow<Double> = platformDao.getTotalBalance()
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** 删除平台时返回的关联账单数量，用于 UI 提示。 */
    private val _lastDeleteLinkedCount = MutableStateFlow(-1)
    val lastDeleteLinkedCount: StateFlow<Int> = _lastDeleteLinkedCount

    fun addAccount(name: String, balance: Double) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addAccount(name.trim(), balance)
        }
    }

    fun updateAccount(account: PlatformAccountEntity, newName: String, newBalance: Double) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAccount(
                account.copy(name = newName.trim(), balance = newBalance)
            )
        }
    }

    /**
     * 删除平台账户。关联该平台的账单会被置为待对账。
     * 删除完成后 [lastDeleteLinkedCount] 会更新为受影响的账单数。
     */
    fun deleteAccount(accountId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _lastDeleteLinkedCount.value = repository.deleteAccount(accountId)
        }
    }

    fun consumeDeleteResult() {
        _lastDeleteLinkedCount.value = -1
    }
}
