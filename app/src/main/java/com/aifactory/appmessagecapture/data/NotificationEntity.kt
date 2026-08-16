package com.aifactory.appmessagecapture.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a captured app notification stored locally.
 */
@Entity(
    tableName = "notifications",
    indices = [Index(value = ["timestamp"])]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long
)
