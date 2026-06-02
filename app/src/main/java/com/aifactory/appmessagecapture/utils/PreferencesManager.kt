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

    companion object {
        private const val PREFS_NAME = "app_message_capture_prefs"
        private const val KEY_BLOCKED_APPS = "blocked_apps"

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
