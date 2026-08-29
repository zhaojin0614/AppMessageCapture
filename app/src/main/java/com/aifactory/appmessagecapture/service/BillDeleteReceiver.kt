package com.aifactory.appmessagecapture.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.aifactory.appmessagecapture.data.AccountRepository
import com.aifactory.appmessagecapture.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 通知「删除」按钮入口：把这笔自动捕获的账单直接删掉，无需进 App。
 *
 * 走 [AccountRepository.deleteBillWithRollback] 事务——已对账平台的
 * 余额自动回滚；删除完成后清除对应的常驻通知。
 */
class BillDeleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val billId = intent.getLongExtra(BillNotificationHelper.EXTRA_BILL_ID, -1L)
        val notificationId = intent.getIntExtra(BillNotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        if (billId <= 0) return

        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                AccountRepository(db, db.billDao(), db.platformAccountDao())
                    .deleteBillWithRollback(billId)
                if (notificationId >= BillNotificationHelper.NOTIFICATION_ID_BASE) {
                    (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .cancel(notificationId)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "已删除该账单", Toast.LENGTH_SHORT).show()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
