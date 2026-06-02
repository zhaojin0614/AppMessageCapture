package com.aifactory.appmessagecapture.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.aifactory.appmessagecapture.service.MessageCaptureService

/**
 * Helper for checking and re-binding the notification listener service.
 */
object NotificationServiceHelper {

    /**
     * Check if the user has granted notification listener access in system settings.
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val component = ComponentName(context, MessageCaptureService::class.java).flattenToString()
        return flat.contains(component)
    }

    /**
     * Request system to rebind the notification listener service.
     * Works on API 24+; on older devices we rely on the user toggling the setting.
     */
    fun requestRebind(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationListenerService.requestRebind(
                ComponentName(context, MessageCaptureService::class.java)
            )
        }
    }

    /**
     * Open system notification listener settings.
     */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Check whether the app is currently ignoring battery optimizations.
     * If not, the system is more likely to kill our service in the background.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Open the battery optimization settings so the user can whitelist this app.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    /**
     * Open generic app settings where users can enable auto-start / background lock
     * (useful for Chinese OEM ROMs like MIUI, EMUI, ColorOS).
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
