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

    @Query("SELECT * FROM bills WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getBillsSinceOnce(since: Long): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillByIdOnce(id: Long): BillEntity?

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 0")
    fun getTotalExpense(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 0 AND timestamp >= :startOfDay")
    fun getTodayExpense(startOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 1 AND timestamp >= :startOfDay")
    fun getTodayIncome(startOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 0 AND timestamp >= :startOfMonth")
    fun getMonthExpense(startOfMonth: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 1 AND timestamp >= :startOfMonth")
    fun getMonthIncome(startOfMonth: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM bills WHERE isIncome = 0")
    fun getExpenseCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM bills WHERE isIncome = 1")
    fun getIncomeCount(): Flow<Int>

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

    @Query("UPDATE bills SET amount = :amount WHERE id = :id")
    suspend fun updateAmount(id: Long, amount: Double)

    /**
     * Update the platform account associated with a bill.
     * Pass null to mark the bill as unreconciled (待对账).
     */
    @Query("UPDATE bills SET platformAccountId = :platformId WHERE id = :id")
    suspend fun updatePlatform(id: Long, platformId: Long?)

    /**
     * Detach all bills from a given platform account (set platformAccountId = null).
     * Used when deleting a platform account so its bills become 待对账.
     */
    @Query("UPDATE bills SET platformAccountId = NULL WHERE platformAccountId = :accountId")
    suspend fun clearPlatformForAccount(accountId: Long)

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

    // Combined today stats for the bill-recognized notification (single round trip
    // instead of three separate aggregate queries per notification)
    @Query("SELECT SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) AS expense, SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) AS income, COUNT(*) AS count FROM bills WHERE timestamp >= :startOfDay")
    fun getTodayStatsOnce(startOfDay: Long): TodayStats?

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

/** 单次查询返回的今日收支统计（账单识别通知用） */
data class TodayStats(
    val expense: Double?,
    val income: Double?,
    val count: Int?
)
