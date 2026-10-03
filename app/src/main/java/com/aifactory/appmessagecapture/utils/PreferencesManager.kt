package com.aifactory.appmessagecapture.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences wrapper for app-level settings.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBlockedApps(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun setBlockedApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_APPS, apps).apply()
    }

    fun isAppBlocked(packageName: String): Boolean {
        return getBlockedApps().contains(packageName)
    }

    fun blockApp(packageName: String) {
        val current = getBlockedApps().toMutableSet()
        current.add(packageName)
        setBlockedApps(current)
    }

    fun unblockApp(packageName: String) {
        val current = getBlockedApps().toMutableSet()
        current.remove(packageName)
        setBlockedApps(current)
    }

    fun getFilteredApps(): Set<String> {
        return prefs.getStringSet(KEY_FILTERED_APPS, emptySet()) ?: emptySet()
    }

    /**
     * 消息保留天数（仅通知/消息表；账单永不自动删除）。
     * 0 = 永久保留（每日清理 Worker 读到 0 时跳过）。默认 30 天。
     */
    fun getNotificationRetentionDays(): Int =
        prefs.getInt(KEY_NOTIFICATION_RETENTION_DAYS, DEFAULT_NOTIFICATION_RETENTION_DAYS)

    fun setNotificationRetentionDays(days: Int) {
        prefs.edit().putInt(KEY_NOTIFICATION_RETENTION_DAYS, days).apply()
    }

    fun setFilteredApps(apps: Set<String>) {
        prefs.edit().putStringSet(KEY_FILTERED_APPS, apps).apply()
    }

    fun isAppFiltered(packageName: String): Boolean {
        return getFilteredApps().contains(packageName)
    }

    fun filterApp(packageName: String) {
        val current = getFilteredApps().toMutableSet()
        current.add(packageName)
        setFilteredApps(current)
    }

    fun unfilterApp(packageName: String) {
        val current = getFilteredApps().toMutableSet()
        current.remove(packageName)
        setFilteredApps(current)
    }

    companion object {
        private const val PREFS_NAME = "app_message_capture_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_FILTERED_APPS = "filtered_apps"
        private const val KEY_NOTIFICATION_RETENTION_DAYS = "notification_retention_days"

        /** 消息保留天数默认值（历史行为：30 天） */
        const val DEFAULT_NOTIFICATION_RETENTION_DAYS = 30

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
