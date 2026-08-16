package com.aifactory.appmessagecapture.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.RecurringBillEntity
import com.aifactory.appmessagecapture.data.RecurringBillProcessor
import com.aifactory.appmessagecapture.data.RecurringFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringBillViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val recurringBillDao = db.recurringBillDao()

    val allRecurringBills: StateFlow<List<RecurringBillEntity>> = recurringBillDao.getAllRecurringBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCount: StateFlow<Int> = recurringBillDao.getActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            executeDueBillsInternal()
        }
    }

    fun addRecurringBill(
        title: String,
        amount: Double,
        category: String,
        isIncome: Boolean,
        frequency: RecurringFrequency,
        startDate: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringBillDao.insert(
                RecurringBillEntity(
                    title = title,
                    amount = amount,
                    category = category,
                    isIncome = isIncome,
                    frequency = frequency.name,
                    startDate = startDate,
                    nextDueDate = startDate
                )
            )
            // 立即执行到期账单，确保当天到期的账单被记录
            executeDueBillsInternal()
        }
    }

    fun deleteRecurringBill(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringBillDao.deleteById(id)
        }
    }

    fun toggleActive(id: Long, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringBillDao.setActive(id, isActive)
        }
    }

    fun updateRecurringBill(recurringBill: RecurringBillEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            recurringBillDao.update(recurringBill)
        }
    }

    /**
     * Execute all due recurring bills now (for testing or manual trigger).
     */
    fun executeDueBills() {
        viewModelScope.launch(Dispatchers.IO) {
            executeDueBillsInternal()
        }
    }

    private suspend fun executeDueBillsInternal() {
        // Shared with RecurringBillWorker: transactional + catch-up execution,
        // so concurrent triggers from UI and Worker never duplicate bills.
        RecurringBillProcessor.processDueBills(db)
    }
}
