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

    companion object {
        private const val PREFS_NAME = "app_message_capture_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
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
