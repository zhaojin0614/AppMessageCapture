package com.aifactory.appmessagecapture.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val appName: String,
    val packageName: String,
    val secondaryAppName: String? = null,
    val secondaryPackageName: String? = null,
    val title: String,
    val category: String = "未分类",
    val isIncome: Boolean = false,
    val timestamp: Long
)
