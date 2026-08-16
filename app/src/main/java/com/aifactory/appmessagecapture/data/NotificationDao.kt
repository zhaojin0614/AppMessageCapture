package com.aifactory.appmessagecapture.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for notification operations.
 */
@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getAllNotificationsOnce(): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE appName LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchNotifications(query: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    fun getNotificationsLimit(limit: Int): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE appName LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT :limit")
    fun searchNotificationsLimit(query: String, limit: Int): Flow<List<NotificationEntity>>

    /** 筛选：排除指定应用包名的全部通知（SQL 层过滤，替代全表拉取后内存过滤） */
    @Query("SELECT * FROM notifications WHERE packageName NOT IN (:excludedPackages) ORDER BY timestamp DESC")
    fun getNotificationsExcluding(excludedPackages: List<String>): Flow<List<NotificationEntity>>

    /** 筛选 + 搜索：排除指定应用包名并按关键词搜索 */
    @Query("SELECT * FROM notifications WHERE packageName NOT IN (:excludedPackages) AND (appName LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchNotificationsExcluding(query: String, excludedPackages: List<String>): Flow<List<NotificationEntity>>

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM notifications")
    fun getNotificationCount(): Flow<Int>

    @Query("SELECT DISTINCT appName, packageName FROM notifications ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppInfo>>

    @Query("SELECT COUNT(*) FROM notifications WHERE timestamp >= :since")
    fun countNotificationsSince(since: Long): Flow<Int>

    /**
     * Delete notifications older than [before] timestamp.
     * Used by [com.aifactory.appmessagecapture.worker.NotificationCleanupWorker]
     * to prune messages older than one month and relieve storage pressure.
     *
     * @return number of deleted rows.
     */
    @Query("DELETE FROM notifications WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long): Int

    /** Ids of notifications older than [before] — used to evict PendingIntentCache entries. */
    @Query("SELECT id FROM notifications WHERE timestamp < :before")
    suspend fun getIdsOlderThan(before: Long): List<Long>
}

/**
 * Minimal data class for distinct app listings.
 */
data class AppInfo(
    val appName: String,
    val packageName: String
)
