package com.aifactory.appmessagecapture.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Query("SELECT * FROM bills WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getBillsSince(since: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBillsOnce(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getBillsSinceOnce(since: Long): List<BillEntity>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 0")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 0 AND timestamp >= :startOfDay")
    fun getTodayExpense(startOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 1 AND timestamp >= :startOfDay")
    fun getTodayIncome(startOfDay: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM bills")
    fun getBillCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM bills WHERE isIncome = 0")
    fun getExpenseCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM bills WHERE isIncome = 1")
    fun getIncomeCount(): Flow<Int>

    @Query("SELECT * FROM bills WHERE isIncome = :isIncome ORDER BY timestamp DESC")
    fun getBillsByType(isIncome: Boolean): Flow<List<BillEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bill: BillEntity): Long

    @Update
    suspend fun update(bill: BillEntity)

    @Query("UPDATE bills SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    /**
     * Batch-rename all bills that match [oldCategory] to [newCategory].
     * Used for category migration when category names change between versions.
     */
    @Query("UPDATE bills SET category = :newCategory WHERE category = :oldCategory")
    suspend fun batchUpdateCategory(oldCategory: String, newCategory: String): Int

    @Query("UPDATE bills SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM bills WHERE category = :category AND isIncome = :isIncome ORDER BY timestamp DESC")
    fun getBillsByCategory(isIncome: Boolean, category: String): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE category = :category AND isIncome = :isIncome AND timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getBillsByCategoryAndTimeRange(
        isIncome: Boolean,
        category: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<BillEntity>>

    @Query("DELETE FROM bills")
    suspend fun deleteAll()

    // Synchronous queries for background notification helper
    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 0 AND timestamp >= :startOfDay")
    fun getTodayExpenseOnce(startOfDay: Long): Double?

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 1 AND timestamp >= :startOfDay")
    fun getTodayIncomeOnce(startOfDay: Long): Double?

    @Query("SELECT COUNT(*) FROM bills WHERE timestamp >= :startOfDay")
    fun getTodayCountOnce(startOfDay: Long): Int?

    /**
     * Find a bill with the same amount within the recent time window (any app).
     * Used for merging duplicate bills from different apps (e.g. Meituan + WeChat Pay).
     */
    @Query("SELECT * FROM bills WHERE amount = :amount AND timestamp >= :since ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentByAmount(amount: Double, since: Long): BillEntity?

    /**
     * Find a bill with the same amount from the same app within the recent time window.
     * Used for deduplicating duplicate notifications from the same app.
     */
    @Query("SELECT * FROM bills WHERE amount = :amount AND packageName = :packageName AND timestamp >= :since ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentByAmountAndPackage(amount: Double, packageName: String, since: Long): BillEntity?

    /**
     * Find a bill with the same amount, same app, AND same title within the recent time window.
     * Used for content-based deduplication — if amount + app + title all match,
     * it is definitively the same notification posted again (e.g. WeChat sends
     * payment confirmation twice with identical text).
     */
    @Query("SELECT * FROM bills WHERE amount = :amount AND packageName = :packageName AND title = :title AND timestamp >= :since ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentByAmountPackageAndTitle(amount: Double, packageName: String, title: String, since: Long): BillEntity?
}
