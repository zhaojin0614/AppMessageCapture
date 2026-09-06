package com.aifactory.appmessagecapture.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 商户记忆手动覆写的读写（商户记忆管理界面与 BillIngestor 推断使用） */
@Dao
interface MerchantMemoryOverrideDao {

    @Query("SELECT * FROM merchant_memory_overrides WHERE fingerprint = :fingerprint")
    suspend fun getByFingerprint(fingerprint: String): MerchantMemoryOverrideEntity?

    @Query("SELECT * FROM merchant_memory_overrides ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<MerchantMemoryOverrideEntity>

    @Query("SELECT * FROM merchant_memory_overrides ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MerchantMemoryOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MerchantMemoryOverrideEntity)

    @Update
    suspend fun update(entity: MerchantMemoryOverrideEntity)

    @Query("DELETE FROM merchant_memory_overrides WHERE fingerprint = :fingerprint")
    suspend fun delete(fingerprint: String)
}
