package com.aifactory.appmessagecapture.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aifactory.appmessagecapture.utils.NotificationServiceHelper

/**
 * Receiver to ensure the notification listener is re-bound after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // The listener service is system-bound; requestRebind() asks the system
            // to reconnect it. startService() is not allowed for a listener service
            // and throws IllegalStateException from the background on Android 8+.
            NotificationServiceHelper.requestRebind(context)
        }
    }
}
