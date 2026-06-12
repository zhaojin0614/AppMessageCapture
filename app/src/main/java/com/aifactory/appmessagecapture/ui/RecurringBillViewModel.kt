package com.aifactory.appmessagecapture.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.data.RecurringBillEntity
import com.aifactory.appmessagecapture.data.RecurringFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class RecurringBillViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val recurringBillDao = db.recurringBillDao()
    private val billDao = db.billDao()

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
        val currentTime = System.currentTimeMillis()
        val dueBills = recurringBillDao.getDueRecurringBills(currentTime)
        val zoneId = ZoneId.systemDefault()

        for (recurring in dueBills) {
            val bill = BillEntity(
                amount = recurring.amount,
                appName = "周期记账",
                packageName = "",
                title = recurring.title,
                category = recurring.category,
                isIncome = recurring.isIncome,
                timestamp = recurring.nextDueDate
            )
            billDao.insert(bill)

            // Calculate next due date
            val currentDueDate = Instant.ofEpochMilli(recurring.nextDueDate)
                .atZone(zoneId)
                .toLocalDate()

            val nextDate = when (recurring.frequency) {
                RecurringFrequency.DAILY.name -> currentDueDate.plusDays(1)
                RecurringFrequency.WEEKLY.name -> currentDueDate.plusWeeks(1)
                RecurringFrequency.BIWEEKLY.name -> currentDueDate.plusWeeks(2)
                RecurringFrequency.MONTHLY.name -> currentDueDate.plusMonths(1)
                RecurringFrequency.YEARLY.name -> currentDueDate.plusYears(1)
                else -> currentDueDate.plusMonths(1)
            }

            val nextDueMillis = nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            recurringBillDao.updateNextDueDate(recurring.id, nextDueMillis)
        }
    }
}
