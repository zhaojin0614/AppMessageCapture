package com.aifactory.appmessagecapture.service

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Process
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
import kotlinx.coroutines.cancel
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

        /**
         * 当前运行的服务实例，供 debug 构建的 SimulateNotificationReceiver
         * 注入模拟通知使用（同进程访问）。
         */
        @Volatile
        internal var instance: MessageCaptureService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
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

        // Filter 4: Skip OEM foreground-service "running" notifications
        // e.g. MIUI/ColorOS: "短信正在运行" + "点按即可了解详情或停止应用"
        if (isRunningNotification(title, content)) return

        // Filter 5: Skip media playback notifications (QQ Music, NetEase Cloud Music, etc.)
        // These contain a MediaSession token and fire repeatedly on every song change
        if (extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return

        // Extract the PendingIntent before the coroutine — this is the click action
        // that the system fires when the user taps the notification in the shade.
        val contentIntent = notification.contentIntent
        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        val app = application as AppMessageCaptureApplication

        // Everything below is IO-bound (SharedPreferences first load is disk IO,
        // PackageManager lookups are binder IPC) — keep it off the main thread.
        // Notification storms (media/IM apps) otherwise jank the service thread.
        serviceScope.launch {
            // Filter: blocked apps (service-level block list)
            if (PreferencesManager.getInstance(this@MessageCaptureService)
                    .isAppBlocked(packageName)
            ) return@launch

            val (appName, isSystem) = appLabelCache.getOrLoad(packageName)

            // Filter 3: Skip non-clearable notifications from system apps
            // These are usually ongoing service status that don't appear in the shade
            if (!sbn.isClearable && isSystem) return@launch

            val entity = NotificationEntity(
                packageName = packageName,
                appName = appName,
                title = title,
                content = content,
                timestamp = postTime
            )

            val dao = app.database.notificationDao()

            // Filter 6: exact-duplicate suppression. ROMs (MIUI/HyperOS) re-deliver
            // the same StatusBarNotification to the listener — identical postTime,
            // title and content — which used to produce adjacent duplicate rows.
            if (dao.findExactDuplicate(postTime, packageName, title, content) != null) {
                return@launch
            }

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

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Debug-only: feed a synthetic notification from [packageName] through the
     * real onNotificationPosted pipeline (filters → insert → dedup → bill),
     * so bill capture can be tested without the actual payment apps installed.
     */
    internal fun simulateNotification(packageName: String, title: String, content: String) {
        val notification = Notification.Builder(this, "debug-simulation")
            .setContentTitle(title)
            .setContentText(content)
            .build()
        val sbn = StatusBarNotification(
            packageName,                       // pkg
            packageName,                       // opPkg
            System.currentTimeMillis().toInt(), // id
            null,                              // tag
            Process.myUid(),                   // uid
            Process.myPid(),                   // initialPid
            0,                                 // score
            notification,                      // notification
            Process.myUserHandle(),            // user
            System.currentTimeMillis()        // postTime
        )
        onNotificationPosted(sbn)
    }

    private fun packageNameOfThisApp(): String {
        return applicationContext.packageName
    }

    /**
     * Per-package cache of (label, isSystemApp). Each notification previously
     * cost up to two PackageManager binder calls on the main thread; now at most
     * one call per package for the service's lifetime.
     */
    private val appLabelCache = AppLabelCache()

    private inner class AppLabelCache {
        private val cache = android.util.LruCache<String, Pair<String, Boolean>>(128)

        suspend fun getOrLoad(packageName: String): Pair<String, Boolean> {
            cache.get(packageName)?.let { return it }
            return try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val label = packageManager.getApplicationLabel(appInfo)
                val entry = Pair(
                    if (!label.isNullOrBlank()) label.toString() else packageName,
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                )
                cache.put(packageName, entry)
                entry
            } catch (e: Exception) {
                packageName to false
            }
        }
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

        // Per-app gate: known payment apps only, with app-specific title/content
        // filters to exclude non-payment noise (coupons, marketing pushes, etc.)
        if (!SupportedPaymentApps.isBillNotification(packageName, title, content)) return

        // Direction is decided from content ONLY — titles like「微信支付」always
        // contain "支付" and would poison keyword matching for income/refunds.
        val isIncome = BillParsing.isIncome(content)
        if (!BillParsing.hasPaymentKeyword(fullText)) return

        val amount = BillParsing.parseAmount(fullText) ?: return

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

        // 1a. Content-based dedup: same app + same amount + same title + same
        //     direction = definite duplicate. Direction matters: a payment and a
        //     same-amount refund often share the identical title ("微信支付").
        //     Uses a 60-second window to catch delayed duplicate deliveries.
        val contentSince = notificationTime - 60_000
        val contentDuplicate = dao.findRecentByAmountPackageAndTitle(amount, packageName, title, contentSince)
        if (contentDuplicate != null && contentDuplicate.isIncome == isIncome) return

        // 1b. Time-proximity dedup: same app + same amount + same direction within
        //     60 seconds AND titles share a common origin prefix. Two purchases with
        //     clearly different titles are distinct transactions and must be kept.
        val timeSince = notificationTime - 60_000
        val sameAppDuplicate = dao.findRecentByAmountAndPackage(amount, packageName, timeSince)
        if (sameAppDuplicate != null &&
            sameAppDuplicate.isIncome == isIncome &&
            BillParsing.isSameOriginTitle(title, sameAppDuplicate.title)
        ) return

        // ── 2. Cross-app deduplication / merge ─────────────────────────────
        // If a different app already recorded a bill with the same amount within
        // 60 seconds, they likely represent the same payment seen through different
        // channels (e.g. merchant app + payment channel).  Keep the higher-weight app.
        // Only merge when the direction matches — a payment and a refund of the same
        // amount are genuinely different transactions.
        val crossSince = notificationTime - 60_000
        val existing = dao.findRecentByAmount(amount, crossSince)
        if (existing != null && existing.packageName != packageName && existing.isIncome == isIncome) {
            val existingWeight = SupportedPaymentApps.appWeight(existing.packageName)
            val currentWeight = SupportedPaymentApps.appWeight(packageName)

            if (currentWeight > existingWeight) {
                // Current app has higher weight (e.g. Meituan > UnionPay channel)
                // → replace existing bill's metadata with the merchant app's info
                //   entirely; the lower-weight channel notification is discarded
                //   (single-origin record, no merged icon display).
                val updatedBill = existing.copy(
                    appName = appName,
                    packageName = packageName,
                    title = title,
                    category = category
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

}
