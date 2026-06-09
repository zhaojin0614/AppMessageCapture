package com.aifactory.appmessagecapture.birthday.utils

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.aifactory.appmessagecapture.birthday.service.BirthdayAlarmScheduler

/**
 * 生日提醒相关权限辅助类。
 *
 * 封装以下权限的检查与引导逻辑：
 * - `POST_NOTIFICATIONS`（Android 13+）
 * - `SCHEDULE_EXACT_ALARM`（Android 14+）
 */
object PermissionHelper {

    private const val TAG = "PermissionHelper"

    /**
     * 检查是否已授予通知权限（Android 13+）。
     */
    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 检查是否可设置精确闹钟（Android 14+）。
     */
    fun hasScheduleExactAlarmPermission(context: Context): Boolean {
        return BirthdayAlarmScheduler.canScheduleExactAlarms(context)
    }

    /**
     * 检查是否已授予悬浮窗权限（用于亮屏时强制弹窗）。
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 跳转系统设置页，让用户手动开启悬浮窗权限。
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                BirthdayLog.i("[$TAG] Opened overlay settings.")
            } catch (e: Exception) {
                BirthdayLog.logException("$TAG.openOverlaySettings", e)
                openAppSettings(context)
            }
        }
    }

    /**
     * 跳转系统设置页，让用户手动开启精确闹钟权限。
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                BirthdayLog.i("[$TAG] Opened exact alarm settings.")
            } catch (e: Exception) {
                BirthdayLog.logException("$TAG.openExactAlarmSettings", e)
                // Fallback to app settings
                openAppSettings(context)
            }
        }
    }

    /**
     * 跳转应用详情设置页。
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
