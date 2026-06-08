package com.aifactory.appmessagecapture.birthday.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 开机完成广播接收器。
 *
 * 监听 [Intent.ACTION_BOOT_COMPLETED]，在设备重启后重新注册所有生日闹钟。
 */
class BirthdayBootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            BirthdayLog.i("[BirthdayBootReceiver] BOOT_COMPLETED received. Rescheduling all alarms...")
            scope.launch {
                try {
                    BirthdayAlarmScheduler.rescheduleAll(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
