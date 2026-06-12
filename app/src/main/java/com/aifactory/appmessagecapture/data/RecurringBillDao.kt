package com.aifactory.appmessagecapture.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 周期性账单数据访问对象
 */
@Dao
interface RecurringBillDao {

    @Query("SELECT * FROM recurring_bills ORDER BY nextDueDate ASC")
    fun getAllRecurringBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurringBills(): Flow<List<RecurringBillEntity>>

    @Query("SELECT * FROM recurring_bills WHERE id = :id")
    suspend fun getRecurringBillById(id: Long): RecurringBillEntity?

    /**
     * 获取所有到期（nextDueDate <= currentTime）且处于激活状态的周期性账单
     */
    @Query("SELECT * FROM recurring_bills WHERE isActive = 1 AND nextDueDate <= :currentTime")
    suspend fun getDueRecurringBills(currentTime: Long): List<RecurringBillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringBill: RecurringBillEntity): Long

    @Update
    suspend fun update(recurringBill: RecurringBillEntity)

    @Delete
    suspend fun delete(recurringBill: RecurringBillEntity)

    @Query("DELETE FROM recurring_bills WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 更新下次执行日期
     */
    @Query("UPDATE recurring_bills SET nextDueDate = :nextDueDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNextDueDate(id: Long, nextDueDate: Long, updatedAt: Long = System.currentTimeMillis())

    /**
     * 切换启用/禁用状态
     */
    @Query("UPDATE recurring_bills SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM recurring_bills WHERE isActive = 1")
    fun getActiveCount(): Flow<Int>
}
