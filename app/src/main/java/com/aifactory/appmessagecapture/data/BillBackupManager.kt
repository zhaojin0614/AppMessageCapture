package com.aifactory.appmessagecapture.data

import android.content.Context
import android.net.Uri
import com.aifactory.appmessagecapture.utils.MiniSheet
import com.aifactory.appmessagecapture.utils.MiniXlsx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 账单数据备份管理器：单一 xlsx 工作簿承载全部记账数据。
 *
 * Sheet 结构：
 * - 「支出账单」「收入账单」：日期时间/分类/标题/金额/平台/来源应用/次要来源/来源包名/次要包名
 * - 「平台账户」：名称/余额/排序
 *
 * 账单用平台**名称**引用平台（人可读、跨库可恢复），导入时按名称与平台表对上；
 * 平台余额只来自平台表快照，导入账单不再调整余额（快照已含其影响）。
 * 表头按列名映射（顺序无关、多余列忽略），向前兼容未来加列。
 */
object BillBackupManager {

    const val SHEET_EXPENSE = "支出账单"
    const val SHEET_INCOME = "收入账单"
    const val SHEET_PLATFORMS = "平台账户"
    const val UNASSIGNED = "待对账"

    private val OUT_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** 导入可接受的日期写法（含 Excel 编辑后常见的斜杠变体） */
    private val IN_DATETIME_PATTERNS = listOf(
        "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss",
        "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm"
    )
    private val IN_DATE_ONLY_PATTERNS = listOf("yyyy-MM-dd", "yyyy/MM/dd")

    private val BILL_HEADERS = listOf("日期时间", "分类", "标题", "金额", "平台", "来源应用", "次要来源", "来源包名", "次要包名")
    private val PLATFORM_HEADERS = listOf("名称", "余额", "排序")

    data class ParsedPlatform(val name: String, val balance: Double, val sortOrder: Int)

    data class ParsedBill(
        val amount: Double,
        val appName: String,
        val packageName: String,
        val secondaryAppName: String?,
        val secondaryPackageName: String?,
        val title: String,
        val category: String,
        val isIncome: Boolean,
        val timestamp: Long,
        val platformName: String? // null = 待对账
    )

    data class ParsedWorkbook(
        val platforms: List<ParsedPlatform>,
        val bills: List<ParsedBill>,
        val badRows: Int
    )

    data class BackupWriteResult(
        val total: Int,
        val inserted: Int,
        val skipped: Int,
        val failed: Int,
        val platformsCreated: Int
    ) {
        fun summary(): String = buildString {
            append("新增 $inserted 笔、跳过 $skipped 笔、失败 $failed 笔")
            if (platformsCreated > 0) append("，新增平台 $platformsCreated 个")
        }
    }

    // ------------------------------------------------------------------
    // 导出
    // ------------------------------------------------------------------

    /** 构建工作簿字节（纯函数，便于单测） */
    fun buildWorkbook(bills: List<BillEntity>, platforms: List<PlatformAccountEntity>): ByteArray {
        val nameById = platforms.associate { it.id to it.name }
        fun platformLabel(bill: BillEntity) = bill.platformAccountId?.let { nameById[it] } ?: UNASSIGNED
        fun formatTime(ts: Long) =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(OUT_FMT)

        fun billRows(list: List<BillEntity>): List<List<Any?>> =
            listOf(BILL_HEADERS) + list.sortedBy { it.timestamp }.map { b ->
                listOf<Any?>(
                    formatTime(b.timestamp), b.category, b.title, b.amount, platformLabel(b),
                    b.appName, b.secondaryAppName ?: "", b.packageName, b.secondaryPackageName ?: ""
                )
            }

        val platformRows: List<List<Any?>> =
            listOf(PLATFORM_HEADERS) + platforms.sortedBy { it.sortOrder }.map {
                listOf<Any?>(it.name, it.balance, it.sortOrder)
            }

        return MiniXlsx.write(
            listOf(
                MiniSheet(SHEET_EXPENSE, billRows(bills.filterNot { it.isIncome })),
                MiniSheet(SHEET_INCOME, billRows(bills.filter { it.isIncome })),
                MiniSheet(SHEET_PLATFORMS, platformRows)
            )
        )
    }

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        bills: List<BillEntity>,
        platforms: List<PlatformAccountEntity>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = buildWorkbook(bills, platforms)
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("无法写入所选位置")
            bills.size
        }
    }

    // ------------------------------------------------------------------
    // 导入
    // ------------------------------------------------------------------

    /** 解析工作簿（纯函数，便于单测）；结构缺失抛 IllegalArgumentException。 */
    fun parseWorkbook(bytes: ByteArray): ParsedWorkbook {
        val sheets = MiniXlsx.read(bytes)
        fun find(name: String) = sheets.firstOrNull { it.name == name }
        val expense = find(SHEET_EXPENSE)
        val income = find(SHEET_INCOME)
        if (expense == null && income == null) {
            throw IllegalArgumentException("未找到「$SHEET_EXPENSE」或「$SHEET_INCOME」工作表")
        }
        val platformSheet = find(SHEET_PLATFORMS)

        val platforms = ArrayList<ParsedPlatform>()
        platformSheet?.let { sheet ->
            val col = headerMap(sheet.rows.firstOrNull(), PLATFORM_HEADERS, listOf("名称"), SHEET_PLATFORMS)
            sheet.rows.drop(1).forEach { row ->
                val name = str(row, col, "名称") ?: return@forEach
                platforms += ParsedPlatform(
                    name = name,
                    balance = str(row, col, "余额")?.toDoubleOrNull() ?: 0.0,
                    sortOrder = str(row, col, "排序")?.toIntOrNull() ?: platforms.size
                )
            }
        }

        val bills = ArrayList<ParsedBill>()
        var badRows = 0
        listOf(expense to false, income to true).forEach { (sheet, isIncome) ->
            sheet ?: return@forEach
            val col = headerMap(sheet.rows.firstOrNull(), BILL_HEADERS, listOf("日期时间", "标题", "金额"), sheet.name)
            sheet.rows.drop(1).forEach { row ->
                if (row.all { it == null || (it is String && it.isBlank()) }) return@forEach
                try {
                    val timestamp = parseTimestamp(str(row, col, "日期时间") ?: throw IllegalStateException("缺少日期"))
                        ?: throw IllegalStateException("日期格式无法识别")
                    val amount = str(row, col, "金额")?.toDoubleOrNull()
                        ?.takeIf { it > 0 } ?: throw IllegalStateException("金额无效")
                    bills += ParsedBill(
                        amount = amount,
                        appName = str(row, col, "来源应用") ?: "",
                        packageName = str(row, col, "来源包名") ?: "",
                        secondaryAppName = str(row, col, "次要来源")?.takeIf { it.isNotBlank() },
                        secondaryPackageName = str(row, col, "次要包名")?.takeIf { it.isNotBlank() },
                        title = str(row, col, "标题") ?: "",
                        category = str(row, col, "分类") ?: "未分类",
                        isIncome = isIncome,
                        timestamp = timestamp,
                        platformName = str(row, col, "平台")
                            ?.takeIf { it.isNotBlank() && it != UNASSIGNED }
                    )
                } catch (e: Exception) {
                    badRows++
                }
            }
        }
        return ParsedWorkbook(platforms, bills, badRows)
    }

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        repository: AccountRepository,
        overwrite: Boolean
    ): Result<BackupWriteResult> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("无法读取所选文件")
            val parsed = parseWorkbook(bytes)
            repository.importBackup(parsed.platforms, parsed.bills, overwrite)
                .copy(failed = parsed.badRows)
        }
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /** 首行表头 → 列号映射；缺失必需列时抛出并指明所在 sheet。 */
    private fun headerMap(
        headerRow: List<Any?>?,
        allHeaders: List<String>,
        required: List<String>,
        sheetName: String
    ): Map<String, Int> {
        val map = HashMap<String, Int>()
        headerRow?.forEachIndexed { index, text ->
            text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { map.putIfAbsent(it, index) }
        }
        val missing = required.filter { it !in map }
        if (missing.isNotEmpty()) {
            throw IllegalArgumentException("工作表「$sheetName」缺少列：${missing.joinToString("、")}")
        }
        return map
    }

    private fun str(row: List<Any?>, col: Map<String, Int>, header: String): String? =
        col[header]?.let { row.getOrNull(it) }?.toString()?.trim()

    /** 解析导入日期；无法识别返回 null（由调用方按坏行计数） */
    private fun parseTimestamp(text: String): Long? {
        val clean = text.trim()
        for (pattern in IN_DATETIME_PATTERNS) {
            try {
                return LocalDateTime.parse(clean, DateTimeFormatter.ofPattern(pattern))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) { /* 尝试下一个格式 */ }
        }
        for (pattern in IN_DATE_ONLY_PATTERNS) {
            try {
                return LocalDate.parse(clean, DateTimeFormatter.ofPattern(pattern))
                    .atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (e: Exception) { /* 尝试下一个格式 */ }
        }
        return null
    }
}
