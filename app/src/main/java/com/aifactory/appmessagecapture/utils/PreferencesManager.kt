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

    /**
     * 拼多多订单号幂等集：订单详情页是持久页面且部分订单状态无时间横幅
     * （内容去重的时间窗口无法锚定），用订单号做精确的"已入账"标记。
     * @return true = 首次记录（应入账）；false = 已记录过（重复打开忽略）
     */
    fun markPddOrderCaptured(orderId: String): Boolean {
        val seen = prefs.getStringSet(KEY_PDD_ORDER_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        val fresh = seen.add(orderId)
        if (fresh) {
            val trimmed = if (seen.size > MAX_CACHED_ORDER_IDS) {
                // 超上限时随机裁剪一半（Set 无序，最坏情况个别订单重复入账一次）
                seen.take(MAX_CACHED_ORDER_IDS / 2).toSet()
            } else {
                seen
            }
            prefs.edit().putStringSet(KEY_PDD_ORDER_IDS, trimmed).apply()
        }
        return fresh
    }

    companion object {
        private const val PREFS_NAME = "app_message_capture_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_FILTERED_APPS = "filtered_apps"
        private const val KEY_PDD_ORDER_IDS = "pdd_captured_order_ids"

        /** 拼多多订单号缓存上限（防偏好文件无限膨胀） */
        private const val MAX_CACHED_ORDER_IDS = 512

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
