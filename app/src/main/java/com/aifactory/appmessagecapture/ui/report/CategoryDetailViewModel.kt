package com.aifactory.appmessagecapture.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aifactory.appmessagecapture.data.AppDatabase
import com.aifactory.appmessagecapture.data.BillEntity
import kotlinx.coroutines.flow.Flow

class CategoryDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val billDao = AppDatabase.getDatabase(application).billDao()

    fun getBills(category: String, isIncome: Boolean): Flow<List<BillEntity>> {
        return billDao.getBillsByCategory(isIncome, category)
    }

    fun getBillsInTimeRange(
        category: String,
        isIncome: Boolean,
        startTime: Long,
        endTime: Long
    ): Flow<List<BillEntity>> {
        return billDao.getBillsByCategoryAndTimeRange(isIncome, category, startTime, endTime)
    }
}
