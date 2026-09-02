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

    /**
     * 淘宝闪购（独立 App）订单号已见集合：独立 App 订单页打开瞬间拿不到
     * 「下单时间」，无法用账单时间锚定内容去重，改按订单号持久化去重——
     * 历史订单页被再次打开时直接跳过。集合存逗号拼接串，超容量丢最旧的。
     */
    fun isShangouOrderSeen(orderId: String): Boolean = getShangouSeenOrders().contains(orderId)

    fun markShangouOrderSeen(orderId: String) {
        val updated = (getShangouSeenOrders() + orderId).takeLast(SHANGOU_SEEN_ORDERS_MAX)
        prefs.edit().putString(KEY_SHANGOU_SEEN_ORDERS, updated.joinToString(",")).apply()
    }

    private fun getShangouSeenOrders(): List<String> =
        prefs.getString(KEY_SHANGOU_SEEN_ORDERS, null)?.split(',')?.filter { it.isNotBlank() } ?: emptyList()

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
        private const val KEY_SHANGOU_SEEN_ORDERS = "shangou_seen_orders"
        private const val SHANGOU_SEEN_ORDERS_MAX = 300
        private const val KEY_FILTERED_APPS = "filtered_apps"

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
