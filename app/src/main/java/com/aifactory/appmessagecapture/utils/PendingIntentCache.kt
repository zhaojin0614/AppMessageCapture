package com.aifactory.appmessagecapture.utils

import android.app.PendingIntent
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for notification PendingIntents.
 *
 * PendingIntent is a system-level binder token (Parcelable, not Serializable),
 * so it cannot be persisted to a database. We store it in memory keyed by
 * the Room-generated notification ID so that clicking a notification card
 * inside the app can replay the same jump action as tapping the notification
 * in the system notification shade.
 *
 * Limitations:
 * - Cache is lost when the app process is killed.
 * - For notifications restored from the database after restart, the fallback
 *   behavior is to launch the target app's main activity via PackageManager.
 * - A PendingIntent may be cancelled by the originating app, but this is rare.
 */
object PendingIntentCache {

    private val cache = ConcurrentHashMap<Long, PendingIntent>()

    /**
     * Store a PendingIntent for the given notification ID.
     */
    fun put(notificationId: Long, pendingIntent: PendingIntent) {
        cache[notificationId] = pendingIntent
    }

    /**
     * Retrieve the cached PendingIntent, or null if not available.
     */
    fun get(notificationId: Long): PendingIntent? {
        return cache[notificationId]
    }

    /**
     * Remove the cached PendingIntent for the given notification ID.
     */
    fun remove(notificationId: Long) {
        cache.remove(notificationId)
    }

    /**
     * Remove multiple entries at once (e.g. when bulk-deleting notifications).
     */
    fun removeAll(ids: List<Long>) {
        ids.forEach { cache.remove(it) }
    }

    /**
     * Clear all cached PendingIntents.
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Check if a PendingIntent exists for the given notification ID.
     */
    fun hasPendingIntent(notificationId: Long): Boolean {
        return cache.containsKey(notificationId)
    }
}
