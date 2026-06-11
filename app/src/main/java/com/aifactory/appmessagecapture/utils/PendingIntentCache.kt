package com.aifactory.appmessagecapture.utils

import android.app.PendingIntent

/**
 * In-memory LRU cache for notification PendingIntents.
 *
 * PendingIntent is a system-level binder token (Parcelable, not Serializable),
 * so it cannot be persisted to a database. We store it in memory keyed by
 * the Room-generated notification ID so that clicking a notification card
 * inside the app can replay the same jump action as tapping the notification
 * in the system notification shade.
 *
 * Uses a LinkedHashMap with access-order to implement LRU eviction:
 * when the cache exceeds [MAX_SIZE], the least recently accessed entry is
 * automatically removed to prevent unbounded memory growth.
 *
 * Limitations:
 * - Cache is lost when the app process is killed.
 * - For notifications restored from the database after restart, the fallback
 *   behavior is to launch the target app's main activity via PackageManager.
 * - A PendingIntent may be cancelled by the originating app, but this is rare.
 */
object PendingIntentCache {

    private const val MAX_SIZE = 200

    private val cache = LinkedHashMap<Long, PendingIntent>(MAX_SIZE, 0.75f, true)

    /**
     * Store a PendingIntent for the given notification ID.
     * If the cache exceeds [MAX_SIZE], the least recently used entry is evicted.
     */
    fun put(notificationId: Long, pendingIntent: PendingIntent) {
        synchronized(cache) {
            cache[notificationId] = pendingIntent
            if (cache.size > MAX_SIZE) {
                val oldest = cache.entries.firstOrNull()
                if (oldest != null) {
                    cache.remove(oldest.key)
                }
            }
        }
    }

    /**
     * Retrieve the cached PendingIntent, or null if not available.
     * Accessing an entry promotes it to the most recently used position.
     */
    fun get(notificationId: Long): PendingIntent? {
        synchronized(cache) {
            return cache[notificationId]
        }
    }

    /**
     * Remove the cached PendingIntent for the given notification ID.
     */
    fun remove(notificationId: Long) {
        synchronized(cache) {
            cache.remove(notificationId)
        }
    }

    /**
     * Remove multiple entries at once (e.g. when bulk-deleting notifications).
     */
    fun removeAll(ids: List<Long>) {
        synchronized(cache) {
            ids.forEach { cache.remove(it) }
        }
    }

    /**
     * Clear all cached PendingIntents.
     */
    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }

    /**
     * Check if a PendingIntent exists for the given notification ID.
     */
    fun hasPendingIntent(notificationId: Long): Boolean {
        synchronized(cache) {
            return cache.containsKey(notificationId)
        }
    }

    /**
     * Current number of cached entries (for debugging).
     */
    val size: Int
        get() = synchronized(cache) { cache.size }
}
