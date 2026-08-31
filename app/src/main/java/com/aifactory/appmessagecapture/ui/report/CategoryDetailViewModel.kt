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

    /** 报表「平台构成」点进明细：按平台名过滤（「待对账」= 未关联平台） */
    fun getPlatformBillsInTimeRange(
        platformName: String,
        isIncome: Boolean,
        startTime: Long,
        endTime: Long
    ): Flow<List<BillEntity>> {
        return billDao.getBillsByPlatformNameAndTimeRange(isIncome, platformName, startTime, endTime)
    }
}
