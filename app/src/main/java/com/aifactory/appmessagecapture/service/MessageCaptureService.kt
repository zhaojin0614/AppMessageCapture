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
import com.aifactory.appmessagecapture.utils.PendingIntentCache
import com.aifactory.appmessagecapture.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Service that listens to system notifications and persists them locally.
 */
class MessageCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            // Auto-extract bill from payment notifications (BillIngestor serializes
            // all bill ingestion internally)
            tryExtractBill(app, sbn, title, content, appName)
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
     * 去重/合并/入库/提醒统一走 [BillIngestor]（与屏幕捕获共用）。
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

        // Use notification postTime as time anchor (not current wall-clock time).
        // This avoids timing mismatches caused by service processing delays or
        // batched notification delivery on OEM ROMs.
        val notificationTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()

        BillIngestor.record(
            context = app,
            packageName = packageName,
            appName = appName,
            title = title,
            fullText = fullText,
            amount = amount,
            isIncome = isIncome,
            timestamp = notificationTime
        )
    }

}
