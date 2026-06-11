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
}

/**
 * Minimal data class for distinct app listings.
 */
data class AppInfo(
    val appName: String,
    val packageName: String
)
