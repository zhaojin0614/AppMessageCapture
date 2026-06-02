package com.aifactory.appmessagecapture.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receiver to ensure the notification listener is re-enabled after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Wake up the service so onCreate() triggers requestRebind()
            val serviceIntent = Intent(context, MessageCaptureService::class.java)
            context.startService(serviceIntent)
        }
    }
}
