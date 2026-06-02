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
import com.aifactory.appmessagecapture.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val entity = NotificationEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        val dao = (application as AppMessageCaptureApplication).database.notificationDao()
        serviceScope.launch {
            dao.insert(entity)
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
        // Stock Android
        if (content.contains("Running in background") || content.contains("Tap for more info")) return true
        return false
    }
}
