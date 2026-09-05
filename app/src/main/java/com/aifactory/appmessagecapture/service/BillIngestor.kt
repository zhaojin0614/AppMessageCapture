package com.aifactory.appmessagecapture.service

import android.content.Context
import com.aifactory.appmessagecapture.AppMessageCaptureApplication
import com.aifactory.appmessagecapture.data.AccountRepository
import com.aifactory.appmessagecapture.data.BillEntity
import com.aifactory.appmessagecapture.ui.ExpenseCategories
import com.aifactory.appmessagecapture.utils.MerchantKey
import com.aifactory.appmessagecapture.ui.IncomeCategories
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 账单入库管线（通知捕获与无障碍屏幕捕获共用）。
 *
 * 从 [MessageCaptureService] 抽出：同 App 去重 → 跨 App 合并 → 插入 →
 * 「记账成功」提醒，两条来源通道对同一笔支付只记一条账。
 * 内部互斥锁串行化入库，避免两个服务同时处理同一笔支付时竞态。
 * 插入/合并统一走 [AccountRepository] 事务：账单带上商户记忆匹配的平台时，
 * 同步扣减/增加该平台余额，保证余额与账单始终一致。
 */
object BillIngestor {

    enum class Result {
        /** 新账单已插入 */
        INSERTED,

        /** 同 App 60 秒内同金额同方向，判为重复投递，丢弃 */
        DUPLICATE_SAME_APP,

        /** 覆盖了 60 秒内低权重 App 记的同笔账（金额相同、方向相同） */
        MERGED_OVER_LOWER_WEIGHT,

        /** 60 秒内已有同金额同方向账单且权重不低于当前来源，保留原账 */
        KEPT_EXISTING
    }

    private val mutex = Mutex()

    /**
     * 记一笔账（带去重与跨 App 合并）。
     *
     * @param fullText 用于分类猜测的完整文本（通知=标题+正文；屏幕=页面文本）
     * @param isIncome 收支方向。由调用方决定：通知来源从正文关键词推断；
     *                 屏幕来源固定 false（「支付成功」页含「领5元红包」等
     *                 营销词，不能让收入关键词污染方向判断）。
     */
    suspend fun record(
        context: Context,
        packageName: String,
        appName: String,
        title: String,
        fullText: String,
        amount: Double,
        isIncome: Boolean,
        timestamp: Long
    ): Result = mutex.withLock {
            val app = context.applicationContext as AppMessageCaptureApplication
            val dao = app.database.billDao()
            val repository = AccountRepository(app.database, dao, app.database.platformAccountDao())

            // ── 0. 商户记忆 ─────────────────────────────────────────────
            // 同商户键最近一笔同方向账单的分类/平台优先于关键词猜测——
            // 用户纠正过一次，之后同商户的消费就自动归类对账。
            val merchantKey = MerchantKey.of(appName, title)
            val memory = dao.findMerchantMemory(merchantKey, isIncome)
            val category = memory?.category
                ?: (if (isIncome) guessIncomeCategory(fullText, appName)
                    else guessCategory(fullText, appName))

            val bill = BillEntity(
                amount = amount,
                appName = appName,
                packageName = packageName,
                title = title,
                category = category,
                isIncome = isIncome,
                timestamp = timestamp,
                platformAccountId = memory?.platformId,
                merchantKey = merchantKey
            )

            // ── 1. 同 App 去重 ─────────────────────────────────────────────

            // 1a. 内容去重：同 App + 同金额 + 同标题 + 同方向 = 重复投递。
            //     方向参与比较：一笔支付和一笔同额退款可能共享相同标题（「微信支付」）。
            val contentSince = timestamp - 60_000
            val contentDuplicate =
                dao.findRecentByAmountPackageAndTitle(amount, packageName, title, contentSince)
            if (contentDuplicate != null && contentDuplicate.isIncome == isIncome) {
                return@withLock Result.DUPLICATE_SAME_APP
            }

            // 1b. 时间近似去重：同 App + 同金额 + 同方向且标题同源（公共前缀 ≥4）。
            //     标题明显不同的两笔同额消费是两笔真实交易，必须都保留。
            val sameAppDuplicate = dao.findRecentByAmountAndPackage(amount, packageName, contentSince)
            if (sameAppDuplicate != null &&
                sameAppDuplicate.isIncome == isIncome &&
                BillParsing.isSameOriginTitle(title, sameAppDuplicate.title)
            ) {
                return@withLock Result.DUPLICATE_SAME_APP
            }

            // ── 2. 跨 App 合并 ─────────────────────────────────────────────
            // 60 秒内不同 App 记了同金额同方向的账 → 大概率是同一笔支付被
            // 多个渠道看到（商户 App 成功页 + 支付渠道通知），保留权重高的一方。
            val existing = dao.findRecentByAmount(amount, contentSince)
            if (existing != null && existing.packageName != packageName && existing.isIncome == isIncome) {
                val existingWeight = SupportedPaymentApps.appWeight(existing.packageName)
                val currentWeight = SupportedPaymentApps.appWeight(packageName)

                if (currentWeight > existingWeight) {
                    // 当前来源权重更高（如商户 App > 支付渠道）→ 整体替换原账单的
                    // 归属信息；低权重渠道的记录被吸收（单一条目，不做图标拼接）。
                    val updatedBill = existing.copy(
                        appName = appName,
                        packageName = packageName,
                        title = title,
                        category = category,
                        platformAccountId = memory?.platformId ?: existing.platformAccountId,
                        merchantKey = merchantKey
                    )
                    repository.replaceBillAttribution(updatedBill)
                    BillNotificationHelper.showBillRecognizedNotification(
                        context = app,
                        billId = updatedBill.id
                    )
                    return@withLock Result.MERGED_OVER_LOWER_WEIGHT
                }
                // 权重不低于当前来源 → 保留原账
                return@withLock Result.KEPT_EXISTING
            }

            // 无重复 —— 插入新账单；商户记忆命中平台时在同一事务内联动余额
            val newId = repository.addBillWithPlatform(bill)
            BillNotificationHelper.showBillRecognizedNotification(
                context = app,
                billId = newId
            )
            BudgetNotifier.checkAndNotify(context)
            Result.INSERTED
        }

    private fun guessIncomeCategory(text: String, appName: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("工资") || lower.contains("薪") -> IncomeCategories.SALARY
            lower.contains("退款") || lower.contains("退货") -> IncomeCategories.REFUND
            lower.contains("红包") || lower.contains("利是") -> IncomeCategories.RED_PACKET
            lower.contains("收益") || lower.contains("利息") || lower.contains("理财") -> IncomeCategories.INVESTMENT
            lower.contains("转账") || lower.contains("转入") -> IncomeCategories.OTHER
            else -> IncomeCategories.OTHER
        }
    }

    private fun guessCategory(text: String, appName: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("外卖") || lower.contains("餐饮") || lower.contains("美食") ||
                    lower.contains("餐厅") || lower.contains("快餐") || appName.contains("美团") -> ExpenseCategories.FOOD
            lower.contains("打车") || lower.contains("滴滴") || lower.contains("出行") ||
                    lower.contains("地铁") || lower.contains("公交") || lower.contains("骑行") -> ExpenseCategories.TRANSPORT
            lower.contains("电影") || lower.contains("娱乐") || lower.contains("游戏") ||
                    lower.contains("会员") -> ExpenseCategories.ENTERTAINMENT
            lower.contains("超市") || lower.contains("购物") || lower.contains("商城") ||
                    lower.contains("淘宝") || lower.contains("京东") || lower.contains("拼多多") -> ExpenseCategories.SHOPPING
            lower.contains("水电") || lower.contains("话费") || lower.contains("宽带") ||
                    lower.contains("燃气") || lower.contains("物业") -> ExpenseCategories.LIVING
            lower.contains("医疗") || lower.contains("药店") || lower.contains("挂号") -> ExpenseCategories.MEDICAL
            else -> ExpenseCategories.OTHER
        }
    }
}
