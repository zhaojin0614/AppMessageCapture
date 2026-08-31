package com.aifactory.appmessagecapture.data

import androidx.room.withTransaction
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import com.aifactory.appmessagecapture.utils.MerchantKey

/**
 * 账单与平台账户联动的事务仓库。
 *
 * 所有涉及"账单 + 平台余额"的操作都通过本类在单个 Room 事务中完成，
 * 确保余额与账单始终一致，不会因中途失败而出现孤立扣款或余额漂移。
 *
 * 余额调整规则：
 * - 支出账单关联平台：平台余额 -= amount（delta = -amount）
 * - 收入账单关联平台：平台余额 += amount（delta = +amount）
 * - 解除关联（对账回滚）：反向调整原平台余额
 */
class AccountRepository(
    private val db: AppDatabase,
    private val billDao: BillDao,
    private val platformDao: PlatformAccountDao
) {

    /**
     * 插入一笔账单，并在关联平台时同步调整余额。
     *
     * @param bill 待插入的账单（platformAccountId 已设置则对账，null 则待对账）
     */
    suspend fun addBillWithPlatform(bill: BillEntity) {
        db.withTransaction {
            // 手动记账同样补齐商户键，使其参与商户记忆
            val normalized = if (bill.merchantKey.isNullOrBlank())
                bill.copy(merchantKey = MerchantKey.of(bill.appName, bill.title))
            else bill
            val newId = billDao.insert(normalized)
            bill.platformAccountId?.let { platformId ->
                adjustPlatformForBill(platformId, bill.amount, bill.isIncome)
            }
            BirthdayLog.i("[AccountRepo] addBill id=$newId amount=${bill.amount} income=${bill.isIncome} platform=${bill.platformAccountId}")
        }
    }

    /**
     * 删除账单并回滚已关联平台的余额。
     *
     * - 已对账支出：把金额加回平台
     * - 已对账收入：从平台扣除金额
     * - 待对账：仅删除账单
     */
    suspend fun deleteBillWithRollback(billId: Long) {
        db.withTransaction {
            val bill = billDao.getBillByIdOnce(billId) ?: return@withTransaction
            bill.platformAccountId?.let { platformId ->
                // 回滚方向与原始扣款相反
                adjustPlatformForBill(platformId, bill.amount, !bill.isIncome)
            }
            billDao.deleteById(billId)
            BirthdayLog.i("[AccountRepo] deleteBill id=$billId amount=${bill.amount} income=${bill.isIncome} platform=${bill.platformAccountId}")
        }
    }

    /**
     * 为待对账账单分配平台（对账）。
     *
     * @param billId 账单 ID
     * @param platformId 平台 ID，传 null 表示取消对账（回滚到待对账）
     */
    suspend fun reconcileBill(billId: Long, platformId: Long?) {
        db.withTransaction {
            val bill = billDao.getBillByIdOnce(billId) ?: return@withTransaction
            val oldPlatformId = bill.platformAccountId

            // 若平台未变，无需调整
            if (oldPlatformId == platformId) return@withTransaction

            // 1. 回滚旧平台
            oldPlatformId?.let {
                adjustPlatformForBill(it, bill.amount, !bill.isIncome)
            }
            // 2. 应用新平台
            platformId?.let {
                adjustPlatformForBill(it, bill.amount, bill.isIncome)
            }
            // 3. 更新账单（无商户键的旧账单回填，使纠正成为后续记忆）
            if (bill.merchantKey.isNullOrBlank()) {
                billDao.update(
                    bill.copy(
                        merchantKey = MerchantKey.of(bill.appName, bill.title)
                    )
                )
            }
            billDao.updatePlatform(billId, platformId)
            BirthdayLog.i("[AccountRepo] reconcileBill id=$billId oldPlatform=$oldPlatformId newPlatform=$platformId")
        }
    }

    /**
     * 修改账单金额，并同步调整已关联平台的余额（差值方向调整）。
     *
     * - 已对账支出：平台余额 += (旧金额 - 新金额) 的差值效果，即按 delta = 新金额 - 旧金额 的支出方向调整
     * - 已对账收入：同样按差值调整
     * - 待对账：仅更新金额
     */
    suspend fun updateBillAmount(billId: Long, newAmount: Double) {
        db.withTransaction {
            val bill = billDao.getBillByIdOnce(billId) ?: return@withTransaction
            if (bill.amount == newAmount) return@withTransaction
            bill.platformAccountId?.let { platformId ->
                adjustPlatformForBill(platformId, newAmount - bill.amount, bill.isIncome)
            }
            billDao.updateAmount(billId, newAmount)
            BirthdayLog.i("[AccountRepo] updateBillAmount id=$billId old=${bill.amount} new=$newAmount")
        }
    }

    /**
     * 用户纠正分类：写库并回填商户键（无键的旧账单按当前标题补键），
     * 使这次纠正对之后同商户的捕获生效。
     */
    suspend fun updateCategoryRemembered(billId: Long, category: String) {
        db.withTransaction {
            val bill = billDao.getBillByIdOnce(billId) ?: return@withTransaction
            if (bill.merchantKey.isNullOrBlank()) {
                billDao.update(
                    bill.copy(
                        category = category,
                        merchantKey = MerchantKey.of(bill.appName, bill.title)
                    )
                )
            } else {
                billDao.updateCategory(billId, category)
            }
        }
    }

    /**
     * 用户改标题：标题参与商户键，需同步重算（旧键的记忆仍在历史账单上）。
     */
    suspend fun updateTitleRekeyed(billId: Long, title: String) {
        db.withTransaction {
            val bill = billDao.getBillByIdOnce(billId) ?: return@withTransaction
            billDao.update(bill.copy(title = title, merchantKey = MerchantKey.of(bill.appName, title)))
        }
    }

    /**
     * 新增平台账户。
     * 自动分配一个随机品牌候选色（colorArgb），报表平台构成据此区分颜色。
     */
    suspend fun addAccount(name: String, balance: Double): Long {
        val now = System.currentTimeMillis()
        val nextOrder = (platformDao.getMaxSortOrder() ?: -1) + 1
        val color = com.aifactory.appmessagecapture.ui.theme.PlatformColors.randomColor()
        val id = platformDao.insert(
            PlatformAccountEntity(
                name = name,
                balance = balance,
                colorArgb = color,
                sortOrder = nextOrder,
                createdAt = now,
                updatedAt = now
            )
        )
        BirthdayLog.i("[AccountRepo] addAccount id=$id name=$name balance=$balance colorArgb=$color")
        return id
    }

    /**
     * 更新平台账户（名称/余额等）。
     */
    suspend fun updateAccount(account: PlatformAccountEntity) {
        platformDao.update(account.copy(updatedAt = System.currentTimeMillis()))
        BirthdayLog.i("[AccountRepo] updateAccount id=${account.id} name=${account.name} balance=${account.balance}")
    }

    /**
     * 删除平台账户。关联该平台的账单会被置为待对账（platformAccountId = null）。
     *
     * @return 关联账单数量（供 UI 提示），-1 表示平台不存在
     */
    suspend fun deleteAccount(accountId: Long): Int {
        return db.withTransaction {
            val account = platformDao.getById(accountId) ?: return@withTransaction -1
            val linkedCount = platformDao.countBillsByPlatform(accountId)
            if (linkedCount > 0) {
                billDao.clearPlatformForAccount(accountId)
            }
            platformDao.deleteById(accountId)
            BirthdayLog.i("[AccountRepo] deleteAccount id=$accountId name=${account.name} linkedBills=$linkedCount")
            linkedCount
        }
    }

    // ── 内部工具 ──────────────────────────────────────────────────────────

    /**
     * 备份导入写入（合并 / 恢复两种模式），全程单事务。
     *
     * 余额规则：平台余额只来自备份文件里的「平台账户」表（新建账户用其初始值，
     * 已存在账户一律不动）——账单插入不触发余额调整，因为备份里的余额快照
     * 已经包含了这些账单的影响，若再调整会造成重复计算。
     *
     * @param platforms 备份文件解析出的平台（name/balance/sortOrder）
     * @param bills 备份文件解析出的账单（platformName=null 表示待对账）
     * @param overwrite true=恢复模式（清空现有账单与平台后全量重建）
     */
    suspend fun importBackup(
        platforms: List<BillBackupManager.ParsedPlatform>,
        bills: List<BillBackupManager.ParsedBill>,
        overwrite: Boolean
    ): BillBackupManager.BackupWriteResult = db.withTransaction {
        if (overwrite) {
            billDao.deleteAll()
            platformDao.deleteAllPlatforms()
        }

        // 平台：按名称匹配，已存在→复用（不改余额/排序），不存在→按备份值新建
        val idByName = HashMap<String, Long?>()
        platformDao.getAllOnce().forEach { idByName[it.name] = it.id }
        var platformsCreated = 0
        suspend fun ensurePlatform(name: String, balance: Double, sortOrder: Int) {
            if (name.isBlank() || idByName.containsKey(name)) return
            val now = System.currentTimeMillis()
            val id = platformDao.insert(
                PlatformAccountEntity(name = name, balance = balance, sortOrder = sortOrder, createdAt = now, updatedAt = now)
            )
            idByName[name] = id
            platformsCreated++
        }
        platforms.forEach { ensurePlatform(it.name, it.balance, it.sortOrder) }

        // 账单：指纹去重（时间+金额+标题+来源应用+方向），平台按名称回填 ID
        val fingerprints = if (overwrite) HashSet() else billDao.getAllBillsOnce()
            .mapTo(HashSet()) { "${it.timestamp}|${it.amount}|${it.title}|${it.appName}|${it.isIncome}" }
        var inserted = 0
        var skipped = 0
        var failed = 0
        bills.forEach { bill ->
            val fingerprint = "${bill.timestamp}|${bill.amount}|${bill.title}|${bill.appName}|${bill.isIncome}"
            if (fingerprint in fingerprints) {
                skipped++
                return@forEach
            }
            try {
                bill.platformName?.let { ensurePlatform(it, 0.0, Int.MAX_VALUE) }
                val newId = billDao.insert(
                    BillEntity(
                        amount = bill.amount,
                        appName = bill.appName,
                        packageName = bill.packageName,
                        secondaryAppName = bill.secondaryAppName,
                        secondaryPackageName = bill.secondaryPackageName,
                        title = bill.title,
                        category = bill.category,
                        isIncome = bill.isIncome,
                        timestamp = bill.timestamp,
                        platformAccountId = bill.platformName?.let { idByName[it] },
                        merchantKey = MerchantKey.of(bill.appName, bill.title)
                    )
                )
                fingerprints.add(fingerprint)
                BirthdayLog.i("[AccountRepo] importBill id=$newId amount=${bill.amount} income=${bill.isIncome}")
                inserted++
            } catch (e: Exception) {
                BirthdayLog.logException("[AccountRepo] importBill", e)
                failed++
            }
        }
        BillBackupManager.BackupWriteResult(
            total = bills.size, inserted = inserted, skipped = skipped,
            failed = failed, platformsCreated = platformsCreated
        )
    }

    // ── 内部工具 ──────────────────────────────────────────────────────────

    /**
     * 根据账单收支方向调整平台余额。
     * @param isIncome true=收入(余额+amount)，false=支出(余额-amount)
     */
    private suspend fun adjustPlatformForBill(platformId: Long, amount: Double, isIncome: Boolean) {
        val delta = if (isIncome) amount else -amount
        platformDao.adjustBalance(platformId, delta)
    }
}
