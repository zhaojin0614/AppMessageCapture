package com.aifactory.appmessagecapture.birthday.utils

import android.content.Context
import android.net.Uri
import com.aifactory.appmessagecapture.birthday.data.BirthdayDao
import com.aifactory.appmessagecapture.birthday.data.BirthdayEntity
import com.aifactory.appmessagecapture.birthday.data.ReminderType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 生日数据备份管理器。
 *
 * 提供 JSON 格式的导入与导出功能：
 * - **导出**：将数据库中的生日记录序列化为 JSON，写入用户通过 SAF 选择的 URI。
 * - **导入**：从用户通过 SAF 选择的 JSON 文件读取，按姓名排重后插入或覆盖数据库。
 */
object BackupManager {

    private const val TAG = "BackupManager"

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    /**
     * 导出数据到指定 URI。
     *
     * @return 成功时返回导出的记录条数
     */
    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        birthdays: List<BirthdayEntity>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            BirthdayLog.i("[$TAG] Export started. recordCount=%d, uri=%s", birthdays.size, uri)

            val jsonString = gson.toJson(birthdays)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Failed to open output stream for URI: $uri")

            BirthdayLog.i("[$TAG] Export completed. writtenBytes=%d", jsonString.toByteArray().size)
            Result.success(birthdays.size)
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.exportToUri", e)
            Result.failure(e)
        }
    }

    /**
     * 从指定 URI 导入数据。
     *
     * **排重/覆盖策略**：
     * 遍历导入的每一条记录，通过 `name` 查找数据库中是否已存在同名记录：
     * - 若存在：保留原记录的 `id`，执行 **update**（覆盖）。
     * - 若不存在：将 `id` 置为 0，执行 **insert**（新增）。
     *
     * @return [ImportResult]，包含总数、新增数、覆盖数、失败数
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        dao: BirthdayDao
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            BirthdayLog.i("[$TAG] Import started. uri=%s", uri)

            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Failed to open input stream for URI: $uri")

            BirthdayLog.d("[$TAG] JSON content length=%d", jsonString.length)

            val listType = object : TypeToken<List<BirthdayBackupItem>>() {}.type
            val backupItems: List<BirthdayBackupItem> = gson.fromJson(jsonString, listType)
                ?: emptyList()

            BirthdayLog.i("[$TAG] Parsed %d records from JSON.", backupItems.size)

            var inserted = 0
            var updated = 0
            var failed = 0

            backupItems.forEachIndexed { index, item ->
                try {
                    val entity = item.toEntity()
                    val existing = dao.getByName(entity.name)
                    if (existing != null) {
                        // 覆盖：保留原 id
                        val toUpdate = entity.copy(id = existing.id)
                        dao.update(toUpdate)
                        updated++
                        BirthdayLog.d(
                            "[$TAG] Import update[%d] name=%s, id=%d",
                            index, toUpdate.name, toUpdate.id
                        )
                    } else {
                        // 新增：id = 0 让 Room 自增
                        val toInsert = entity.copy(id = 0)
                        val newId = dao.insert(toInsert)
                        inserted++
                        BirthdayLog.d(
                            "[$TAG] Import insert[%d] name=%s, newId=%d",
                            index, toInsert.name, newId
                        )
                    }
                } catch (e: Exception) {
                    failed++
                    BirthdayLog.logException("$TAG.importFromUri item[$index] name=${item.name}", e)
                }
            }

            val result = ImportResult(
                total = backupItems.size,
                inserted = inserted,
                updated = updated,
                failed = failed
            )
            BirthdayLog.i(
                "[$TAG] Import completed. total=%d, inserted=%d, updated=%d, failed=%d",
                result.total, result.inserted, result.updated, result.failed
            )
            Result.success(result)
        } catch (e: Exception) {
            BirthdayLog.logException("$TAG.importFromUri", e)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // 内部数据类
    // -------------------------------------------------------------------------

    /**
     * 备份 JSON 中的数据格式（与 [BirthdayEntity] 结构一致，但便于 Gson 反序列化）。
     */
    data class BirthdayBackupItem(
        val id: Int = 0,
        val name: String = "",
        val isLunar: Boolean = false,
        val birthYear: Int? = null,
        val birthMonth: Int = 1,
        val birthDay: Int = 1,
        val reminderType: String = "ON_DAY",
        val reminderTime: String? = null
    ) {
        fun toEntity(): BirthdayEntity = BirthdayEntity(
            id = id,
            name = name,
            isLunar = isLunar,
            birthYear = birthYear,
            birthMonth = birthMonth,
            birthDay = birthDay,
            reminderType = ReminderType.fromName(reminderType),
            reminderTime = reminderTime
        )
    }

    data class ImportResult(
        val total: Int,
        val inserted: Int,
        val updated: Int,
        val failed: Int
    )
}
