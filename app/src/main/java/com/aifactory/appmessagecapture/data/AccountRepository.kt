package com.aifactory.appmessagecapture.data

import androidx.room.withTransaction
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog

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
            val newId = billDao.insert(bill)
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
            // 3. 更新账单
            billDao.updatePlatform(billId, platformId)
            BirthdayLog.i("[AccountRepo] reconcileBill id=$billId oldPlatform=$oldPlatformId newPlatform=$platformId")
        }
    }

    /**
     * 新增平台账户。
     */
    suspend fun addAccount(name: String, balance: Double): Long {
        val now = System.currentTimeMillis()
        val nextOrder = (platformDao.getAllOnce().maxOfOrNull { it.sortOrder } ?: -1) + 1
        val id = platformDao.insert(
            PlatformAccountEntity(name = name, balance = balance, sortOrder = nextOrder, createdAt = now, updatedAt = now)
        )
        BirthdayLog.i("[AccountRepo] addAccount id=$id name=$name balance=$balance")
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
     * 根据账单收支方向调整平台余额。
     * @param isIncome true=收入(余额+amount)，false=支出(余额-amount)
     */
    private suspend fun adjustPlatformForBill(platformId: Long, amount: Double, isIncome: Boolean) {
        val delta = if (isIncome) amount else -amount
        platformDao.adjustBalance(platformId, delta)
    }
}
