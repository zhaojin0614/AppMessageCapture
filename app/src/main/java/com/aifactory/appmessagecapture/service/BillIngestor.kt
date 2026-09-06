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

            // ── 0. 商户记忆推断（分层，置信度从高到低）──────────────────
            // 1. 手动覆写（管理界面）→ 2. 同商户键投票 → 3. 跨商户指纹 →
            // 4a. 渠道推断平台 → 4b. 消费习惯先验 → 兜底关键词猜测
            val merchantKey = MerchantKey.of(appName, title)
            val core = MerchantMemory.core(appName, title)
            val override = core?.let { app.database.merchantMemoryOverrideDao().getByFingerprint(it) }
            val ignoreAuto = override?.ignoreAuto == true
            var source: String? = null

            var category = override?.category
            var platformId = override?.platformAccountId
            if (category != null) source = "手动覆写"

            if ((category == null || platformId == null) && !ignoreAuto) {
                // 2. 同商户键最近 N 笔多数投票（分类众数；平台取最近非空）
                val vote = MerchantMemory.vote(
                    dao.findMemorySamples(merchantKey, isIncome, MerchantMemory.VOTE_SAMPLE_SIZE)
                        .map { MerchantMemory.MemorySample(it.category, it.platformAccountId) }
                )
                if (category == null && vote.category != null) {
                    category = vote.category
                    source = "商户记忆"
                }
                if (platformId == null) platformId = vote.platformId

                // 3. 跨商户指纹：同一线下商户在不同 App（淘宝闪购/拼多多/美团）
                //    标题不同、键不同，用归一核心跨键共享记忆
                if ((category == null || platformId == null) && core != null) {
                    val crossVote = MerchantMemory.vote(
                        dao.findMemorySamplesByCore(core, isIncome, MerchantMemory.VOTE_SAMPLE_SIZE)
                            .map { MerchantMemory.MemorySample(it.category, it.platformAccountId) }
                    )
                    if (category == null && crossVote.category != null) {
                        category = crossVote.category
                        source = "跨商户记忆"
                    }
                    if (platformId == null) platformId = crossVote.platformId
                }
            }

            if (platformId == null && !ignoreAuto) {
                // 4a. 渠道推断平台：该商户历史被合并吸收的渠道（「用微信付过」）
                // 或渠道通知自身来源（com.tencent.mm 等），映射到用户绑定的平台。
                // 只有用户在平台账户里绑定过渠道才生效——绑定即授权自动对账。
                val platforms = app.database.platformAccountDao().getAllOnce()
                val channel = core?.let {
                    dao.findChannelEvidence(core, isIncome, MerchantMemory.VOTE_SAMPLE_SIZE).firstOrNull()
                } ?: packageName
                MerchantMemory.matchPlatform(channel, platforms.map { it.id to it.boundPackageName })
                    ?.let {
                        platformId = it
                        source = "渠道推断"
                    }
            }

            if (category == null && !ignoreAuto) {
                // 4b. 消费习惯先验：商户身份不可知（「微信支付」类通知）时，
                // 用自己的历史（渠道×时段×金额档）推断分类
                val inferred = CategoryPrior.infer(
                    dao.findRecentExpenseStats(200).map {
                        CategoryPrior.ExpenseSample(it.category, it.packageName, it.timestamp, it.amount)
                    },
                    packageName, timestamp, amount
                )
                if (inferred != null) {
                    category = inferred
                    source = "消费习惯"
                }
            }

            if (category == null) {
                category = if (isIncome) guessIncomeCategory(fullText, appName)
                else guessCategory(fullText, appName)
            }

            val bill = BillEntity(
                amount = amount,
                appName = appName,
                packageName = packageName,
                title = title,
                category = category,
                isIncome = isIncome,
                timestamp = timestamp,
                platformAccountId = platformId,
                merchantKey = merchantKey,
                memorySource = source
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
                    // 渠道信息保留：被吸收方的来源 App 就是付款渠道，记入
                    // secondary 供渠道推断；平台则优先用新推断结果，推断不出时
                    // 保留原账单已有的平台（商户页通常比渠道通知少一个渠道信号）。
                    val updatedBill = existing.copy(
                        appName = appName,
                        packageName = packageName,
                        title = title,
                        category = category,
                        platformAccountId = platformId ?: existing.platformAccountId,
                        merchantKey = merchantKey,
                        memorySource = source ?: existing.memorySource,
                        secondaryPackageName = existing.packageName,
                        secondaryAppName = existing.appName
                    )
                    repository.replaceBillAttribution(updatedBill)
                    BillNotificationHelper.showBillRecognizedNotification(
                        context = app,
                        billId = updatedBill.id
                    )
                    return@withLock Result.MERGED_OVER_LOWER_WEIGHT
                }
                // 权重不低于当前来源 → 保留原账；当前来源（通常是渠道通知）的
                // 包名回填到账单 secondary 上，作为该商户的渠道证据供下次推断
                if (existing.secondaryPackageName == null) {
                    dao.updateSecondary(existing.id, packageName, appName)
                }
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
