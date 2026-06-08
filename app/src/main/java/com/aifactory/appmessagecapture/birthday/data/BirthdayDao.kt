package com.aifactory.appmessagecapture.birthday.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 生日记录数据访问对象 (DAO)。
 *
 * 提供对 [BirthdayEntity] 表的增删改查操作，支持 [Flow] 响应式查询。
 */
@Dao
interface BirthdayDao {

    /**
     * 插入一条生日记录。若主键冲突则替换（用于导入覆盖场景）。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(birthday: BirthdayEntity): Long

    /**
     * 批量插入生日记录。若主键冲突则替换。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(birthdays: List<BirthdayEntity>): List<Long>

    /**
     * 更新生日记录。
     */
    @Update
    suspend fun update(birthday: BirthdayEntity)

    /**
     * 删除生日记录。
     */
    @Delete
    suspend fun delete(birthday: BirthdayEntity)

    /**
     * 根据 ID 删除记录。
     */
    @Query("DELETE FROM birthdays WHERE id = :id")
    suspend fun deleteById(id: Int)

    /**
     * 根据 ID 查询单条记录。
     */
    @Query("SELECT * FROM birthdays WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): BirthdayEntity?

    /**
     * 分页查询生日记录（按 ID 升序，保证分页稳定性）。
     */
    @Query("SELECT * FROM birthdays ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getBirthdaysPaged(limit: Int, offset: Int): List<BirthdayEntity>

    /**
     * 查询所有生日记录（无排序）。
     */
    @Query("SELECT * FROM birthdays")
    fun getAll(): Flow<List<BirthdayEntity>>

    /**
     * 查询所有生日记录（一次性，用于导出/后台任务）。
     */
    @Query("SELECT * FROM birthdays")
    suspend fun getAllOnce(): List<BirthdayEntity>

    /**
     * 根据姓名查找记录（用于导入时排重）。
     */
    @Query("SELECT * FROM birthdays WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): BirthdayEntity?

    /**
     * 获取记录总数。
     */
    @Query("SELECT COUNT(*) FROM birthdays")
    suspend fun count(): Int
}
