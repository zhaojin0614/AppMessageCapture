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

    /**
     * 按类型/分类直接查库（记账页筛选用），按条数分页（LIMIT）。
     * 与「全部」视图的时间窗口分页不同：筛选若也按周窗口切，
     * 窗口内没有目标类型账单时结果会恒空（如最近一周无收入却筛选收入），
     * 故按时间倒序取前 [limit] 条匹配记录，滚动到底再增大 limit。
     * 传 null 表示该维度不过滤。
     */
    @Query(
        """SELECT * FROM bills
           WHERE (:type IS NULL OR isIncome = :type)
             AND (:category IS NULL OR category = :category)
           ORDER BY timestamp DESC
           LIMIT :limit"""
    )
    fun getBillsFiltered(type: Boolean?, category: String?, limit: Int): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getBillsSinceOnce(since: Long): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillByIdOnce(id: Long): BillEntity?

    /** 全量账单（备份导入时做指纹去重用） */
    @Query("SELECT * FROM bills")
    suspend fun getAllBillsOnce(): List<BillEntity>

    /**
     * 商户记忆：同商户键最近一笔同方向账单的分类与平台。
     * 平台已被删除的记忆不返回（LEFT JOIN 过滤）。
     */
    @Query(
        """
        SELECT b.category AS category, b.platformAccountId AS platformId
        FROM bills b
        LEFT JOIN platform_accounts p ON p.id = b.platformAccountId
        WHERE b.merchantKey = :merchantKey AND b.isIncome = :isIncome
          AND (b.platformAccountId IS NULL OR p.id IS NOT NULL)
        ORDER BY b.timestamp DESC
        LIMIT 1
        """
    )
    suspend fun findMerchantMemory(merchantKey: String, isIncome: Boolean): MerchantMemory?

    /** 关键词搜索：标题/来源应用/分类模糊匹配 + 金额文本匹配，带类型/分类过滤与条数分页 */
    @Query(
        """
        SELECT * FROM bills
        WHERE (
            title LIKE '%' || :query || '%'
            OR appName LIKE '%' || :query || '%'
            OR category LIKE '%' || :query || '%'
            OR CAST(amount AS TEXT) LIKE '%' || :query || '%'
        )
        AND (:type IS NULL OR isIncome = :type)
        AND (:category IS NULL OR category = :category)
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun searchBills(query: String, type: Boolean?, category: String?, limit: Int): Flow<List<BillEntity>>

    /** 自某时点（月初）起的分类支出汇总（预算进度用） */
    @Query(
        """
        SELECT category AS category, SUM(amount) AS total
        FROM bills
        WHERE isIncome = 0 AND timestamp >= :monthStart
        GROUP BY category
        """
    )
    fun getMonthCategoryExpense(monthStart: Long): Flow<List<CategorySum>>

    /** 本月总支出（一次性，预算通知检查用） */
    @Query("SELECT SUM(amount) FROM bills WHERE isIncome = 0 AND timestamp >= :monthStart")
    suspend fun getMonthExpenseOnce(monthStart: Long): Double?

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

    /**
     * 按平台名查时间段内账单：报表「平台构成」点进明细用。
     * 「待对账」= 未关联平台的账单（platformAccountId IS NULL）。
     */
    @Query(
        """
        SELECT b.* FROM bills b
        LEFT JOIN platform_accounts p ON p.id = b.platformAccountId
        WHERE b.isIncome = :isIncome
          AND b.timestamp >= :startTime AND b.timestamp < :endTime
          AND (
                (:platformName = '待对账' AND b.platformAccountId IS NULL)
                OR p.name = :platformName
              )
        ORDER BY b.timestamp DESC
        """
    )
    fun getBillsByPlatformNameAndTimeRange(
        isIncome: Boolean,
        platformName: String,
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

/** 商户记忆查询结果：最近一笔同商户账单的分类与平台 */
data class MerchantMemory(
    val category: String,
    val platformId: Long?
)

/** 分类月度支出汇总（预算进度用） */
data class CategorySum(
    val category: String,
    val total: Double
)
