package com.aifactory.appmessagecapture.service

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aifactory.appmessagecapture.AppMessageCaptureApplication
import com.aifactory.appmessagecapture.data.NotificationEntity
import com.aifactory.appmessagecapture.ui.ExpenseCategories
import com.aifactory.appmessagecapture.ui.IncomeCategories
import com.aifactory.appmessagecapture.utils.PendingIntentCache
import com.aifactory.appmessagecapture.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/**
 * Service that listens to system notifications and persists them locally.
 */
class MessageCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val billMutex = kotlinx.coroutines.sync.Mutex()

    companion object {
        @Volatile
        var isConnected: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, MessageCaptureService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName ?: return

        // Skip this app itself to avoid noise
        if (packageName == packageNameOfThisApp()) return

        // Skip blocked apps
        if (PreferencesManager.getInstance(this).isAppBlocked(packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val content = if (bigText.isNotBlank()) bigText else text

        // Filter 1: Skip completely empty notifications (no title and no content)
        if (title.isBlank() && content.isBlank()) return

        // Filter 2: Skip common system packages that send invisible/ghost notifications
        if (isGhostNotificationPackage(packageName)) return

        // Filter 3: Skip non-clearable notifications from system apps
        // These are usually ongoing service status that don't appear in the notification shade
        if (!sbn.isClearable && isSystemApp(packageName)) return

        // Filter 4: Skip OEM foreground-service "running" notifications
        // e.g. MIUI/ColorOS: "短信正在运行" + "点按即可了解详情或停止应用"
        if (isRunningNotification(title, content)) return

        // Filter 5: Skip media playback notifications (QQ Music, NetEase Cloud Music, etc.)
        // These contain a MediaSession token and fire repeatedly on every song change
        if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val label = packageManager.getApplicationLabel(appInfo)
            if (!label.isNullOrBlank()) label.toString() else packageName
        } catch (e: Exception) {
            packageName
        }

        // Extract the PendingIntent before the coroutine — this is the click action
        // that the system fires when the user taps the notification in the shade.
        val contentIntent = notification.contentIntent

        val entity = NotificationEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            timestamp = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
        )

        val app = application as AppMessageCaptureApplication
        val dao = app.database.notificationDao()
        serviceScope.launch {
            val insertedId = dao.insert(entity)
            // Cache the PendingIntent in memory so the UI can replay the click action.
            // Room auto-increment ID is used as the cache key.
            if (contentIntent != null) {
                PendingIntentCache.put(insertedId, contentIntent)
            }
            // Auto-extract bill from payment notifications (serialized to avoid race conditions)
            billMutex.withLock {
                tryExtractBill(app, sbn, title, content, appName)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: handle notification removal if needed
    }

    private fun packageNameOfThisApp(): String {
        return applicationContext.packageName
    }

    /**
     * Common packages that send invisible/ghost notifications
     * which do not appear in the notification shade.
     */
    private fun isGhostNotificationPackage(packageName: String): Boolean {
        return packageName == "com.android.systemui" ||
                packageName == "android" ||
                packageName.startsWith("com.android.system") ||
                packageName == "com.google.android.gms" ||
                packageName == "com.google.android.googlequicksearchbox"
    }

    /**
     * Check whether the package belongs to a system app.
     */
    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Detect OEM ROM foreground-service notifications like:
     *   - "短信正在运行" / "点按即可了解详情或停止应用"
     *   - "Running in background" / "Tap for more info"
     */
    private fun isRunningNotification(title: String, content: String): Boolean {
        // Chinese OEM ROMs (MIUI, ColorOS, HarmonyOS, etc.)
        if (content.contains("正在运行") &&
            (content.contains("停止应用") || content.contains("了解详情") || content.contains("后台运行"))
        ) return true
        if (title.contains("正在运行") && content.contains("停止应用")) return true
        if (content.contains("视频通话中") || content.contains("你有一条新消息")) return true
        if (title.contains("语音通话") || content.contains("语音通话")) return true
        // Stock Android
        if (content.contains("Running in background") || content.contains("Tap for more info")) return true
        return false
    }

    /**
     * Try to extract payment/expense info from known payment apps.
     */
    private suspend fun tryExtractBill(
        app: AppMessageCaptureApplication,
        sbn: StatusBarNotification,
        title: String,
        content: String,
        appName: String
    ) {
        val packageName = sbn.packageName ?: return
        val fullText = "$title $content"

        when (packageName) {
            "com.tencent.mm" -> {               // WeChat
                // WeChat pay notifications MUST have title containing payment keywords
                val validTitle = title.contains("微信支付")
                if (!validTitle) return
            }
            "com.eg.android.AlipayGphone" -> {   // Alipay
                // Title or content must contain Alipay/payment keywords
                val validTitle = title.contains("交易提醒")
                val validContent = content.contains("支出") || content.contains("收入")
                if (!validTitle || !validContent) return
            }
            "com.sankuai.meituan",
            "com.sankuai.meituan.takeoutnew" -> { // Meituan
                // Exclude non-expense notifications like rewards, coupons, refunds
                val validTitle = title.contains("付款")
                if (!validTitle) return
            }
            "com.dianping.v1" -> { }             // Dianping
            "com.jd.jrapp" -> { }                // JD Finance
            "com.baidu.wallet" -> { }           // Baidu Wallet
            else -> return
        }

        // Keywords that indicate a payment/expense notification
        val expenseKeywords = listOf("付款", "支付", "消费", "支出", "扣款", "已付", "交易", "订单已支付")
        val incomeKeywords = listOf("收款", "入账", "到账", "转入", "存入", "退款", "收益", "工资", "红包")
        val isIncome = incomeKeywords.any { fullText.contains(it) } && !expenseKeywords.any { fullText.contains(it) }
        val hasPaymentKeyword = expenseKeywords.any { fullText.contains(it) } || incomeKeywords.any { fullText.contains(it) }
        if (!hasPaymentKeyword) return

        val amount = extractAmount(fullText) ?: return

        // Guess category from content keywords
        val category = if (isIncome) guessIncomeCategory(fullText, appName) else guessCategory(fullText, appName)

        val bill = com.aifactory.appmessagecapture.data.BillEntity(
            amount = amount,
            appName = appName,
            packageName = packageName,
            title = title,
            category = category,
            isIncome = isIncome,
            timestamp = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
        )

        val dao = app.database.billDao()

        // Use notification postTime as time anchor (not current wall-clock time).
        // This avoids timing mismatches caused by service processing delays or
        // batched notification delivery on OEM ROMs.
        val notificationTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        // ── 1. Same-app deduplication ──────────────────────────────────────

        // 1a. Content-based dedup: same app + same amount + same title = definite duplicate.
        //     Uses a 60-second window to catch delayed duplicate deliveries.
        val contentSince = notificationTime - 60_000
        val contentDuplicate = dao.findRecentByAmountPackageAndTitle(amount, packageName, title, contentSince)
        if (contentDuplicate != null) return

        // 1b. Time-proximity dedup: same app + same amount within 60 seconds.
        //     Even if the title text differs slightly, two notifications from the same
        //     app with the same amount within 60 seconds are almost certainly the same payment.
        val timeSince = notificationTime - 60_000
        val sameAppDuplicate = dao.findRecentByAmountAndPackage(amount, packageName, timeSince)
        if (sameAppDuplicate != null) return

        // ── 2. Cross-app deduplication / merge ─────────────────────────────
        // If a different app already recorded a bill with the same amount within
        // 60 seconds, they likely represent the same payment seen through different
        // channels (e.g. merchant app + payment channel).  Keep the higher-weight app.
        val crossSince = notificationTime - 60_000
        val existing = dao.findRecentByAmount(amount, crossSince)
        if (existing != null && existing.packageName != packageName) {
            val existingWeight = getAppWeight(existing.packageName)
            val currentWeight = getAppWeight(packageName)

            if (currentWeight > existingWeight) {
                // Current app has higher weight (e.g. Meituan > WeChat Pay)
                // → replace existing bill's metadata with the merchant app's info
                val updatedBill = existing.copy(
                    appName = appName,
                    packageName = packageName,
                    title = title,
                    category = category,
                    secondaryAppName = null,
                    secondaryPackageName = null
                )
                dao.update(updatedBill)
                BillNotificationHelper.showBillRecognizedNotification(
                    context = app,
                    appName = updatedBill.appName,
                    amount = updatedBill.amount,
                    category = updatedBill.category,
                    timestamp = updatedBill.timestamp,
                    isIncome = updatedBill.isIncome
                )
            }
            // If existing weight >= current weight, do nothing (keep existing)
        } else {
            // No duplicate found — insert as new bill
            dao.insert(bill)
            BillNotificationHelper.showBillRecognizedNotification(
                context = app,
                appName = bill.appName,
                amount = bill.amount,
                category = bill.category,
                timestamp = bill.timestamp,
                isIncome = bill.isIncome
            )
        }
    }

    /**
     * Extract monetary amount from Chinese payment notification text.
     * Supports formats like: ¥14.40, 14.40元, 已支付14.4, etc.
     */
    private fun extractAmount(text: String): Double? {
        // Pattern 1: ¥14.40 or ¥ 14.40
        val pattern1 = Regex("""[¥￥]\s*(\d+(?:\.\d{1,2})?)""")
        // Pattern 2: 14.40元 or 14.4元
        val pattern2 = Regex("""(\d+(?:\.\d{1,2})?)\s*[元円]""")
        // Pattern 3: generic number with decimal (fallback)
        val pattern3 = Regex("""(\d+\.\d{1,2})""")

        pattern1.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }
        pattern2.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }
        pattern3.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }
        return null
    }

    /**
     * Guess income category from notification text.
     */
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

    /**
     * Guess expense category from notification text.
     */
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

    /**
     * App weight for merge priority.
     * Higher weight = primary app when merging bills.
     * E.g. Meituan (merchant) > WeChat Pay (payment channel).
     */
    private fun getAppWeight(packageName: String): Int {
        return when (packageName) {
            "com.sankuai.meituan",
            "com.sankuai.meituan.takeoutnew" -> 100 // Meituan
            "com.dianping.v1" -> 90                  // Dianping
            "com.jd.jrapp" -> 80                     // JD Finance
            "com.baidu.wallet" -> 70                 // Baidu Wallet
            "com.eg.android.AlipayGphone" -> 50      // Alipay
            "com.tencent.mm" -> 50                   // WeChat
            else -> 0
        }
    }
}
