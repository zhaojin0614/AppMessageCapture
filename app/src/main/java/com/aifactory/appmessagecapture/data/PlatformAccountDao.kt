package com.aifactory.appmessagecapture.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 平台账户数据访问对象。
 *
 * 余额的增减应通过 [adjustBalance] 原子操作完成，而非先查后改，
 * 以避免并发场景下的余额不一致。所有涉及"账单 + 平台余额"联动的
 * 操作请走 [AccountRepository] 的事务方法。
 */
@Dao
interface PlatformAccountDao {

    @Query("SELECT * FROM platform_accounts ORDER BY sortOrder ASC, createdAt ASC")
    fun getAll(): Flow<List<PlatformAccountEntity>>

    @Query("SELECT * FROM platform_accounts ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllOnce(): List<PlatformAccountEntity>

    @Query("SELECT * FROM platform_accounts WHERE id = :id")
    suspend fun getById(id: Long): PlatformAccountEntity?

    /** 取最大排序号（新增账户排在末尾用），避免全表加载 */
    @Query("SELECT MAX(sortOrder) FROM platform_accounts")
    suspend fun getMaxSortOrder(): Int?

    @Query("SELECT COUNT(*) FROM bills WHERE platformAccountId = :id")
    suspend fun countBillsByPlatform(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: PlatformAccountEntity): Long

    @Update
    suspend fun update(account: PlatformAccountEntity)

    @Query("DELETE FROM platform_accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 清空全部平台账户（仅备份恢复模式使用） */
    @Query("DELETE FROM platform_accounts")
    suspend fun deleteAllPlatforms()

    /**
     * 原子地增减平台余额。支出传负 delta，收入传正 delta。
     * 仅更新余额与更新时间，避免覆盖其他字段的并发修改。
     */
    @Query("UPDATE platform_accounts SET balance = balance + :delta, updatedAt = :now WHERE id = :id")
    suspend fun adjustBalance(id: Long, delta: Double, now: Long = System.currentTimeMillis())

    @Query("SELECT SUM(balance) FROM platform_accounts")
    fun getTotalBalance(): Flow<Double?>
}
