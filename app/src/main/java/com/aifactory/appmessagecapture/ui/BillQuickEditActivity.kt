package com.aifactory.appmessagecapture.ui

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aifactory.appmessagecapture.service.BillNotificationHelper
import com.aifactory.appmessagecapture.ui.theme.AppMessageCaptureTheme

/**
 * 账单快捷编辑弹窗（Activity 宿主）。
 *
 * 从「记账成功」通知的「完善账单」按钮打开。UI 与保存逻辑在共享的
 * [QuickEditDialogContent]（与悬浮窗宿主 [BillQuickEditOverlay] 共用）。
 *
 * 双通道说明：已授予「显示悬浮窗」权限的新通知会直接在任意界面弹出
 * 悬浮选择窗（见 BillNotificationHelper 按权限选择广播/Activity 入口）；
 * 本 Activity 是未授权时的回退路径（Android 12+ 禁止通知广播内启动
 * Activity，无法在接收器里做回退跳转）。
 */
class BillQuickEditActivity : ComponentActivity() {

    private val billId: Long by lazy {
        intent.getLongExtra(BillNotificationHelper.EXTRA_BILL_ID, -1L)
    }
    private val notificationId: Int by lazy {
        intent.getIntExtra(BillNotificationHelper.EXTRA_NOTIFICATION_ID, -1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (billId <= 0) {
            finish()
            return
        }
        setContent {
            AppMessageCaptureTheme {
                QuickEditDialogContent(
                    billId = billId,
                    showOverlayHint = !Settings.canDrawOverlays(this),
                    onDismiss = { finish() },
                    onCommitted = { cancelBillNotification() },
                    onBillMissing = { onBillMissing() }
                )
            }
        }
    }

    /**
     * 账单已不存在（App 内已删除/清空，但常驻通知还挂着）：
     * 清除残留通知后退出弹窗，避免无限加载。
     */
    private fun onBillMissing() {
        val appContext = applicationContext
        cancelBillNotification(appContext)
        Toast.makeText(appContext, "该账单已删除，通知已清除", Toast.LENGTH_SHORT).show()
        finish()
    }

    /** 用户完成操作，清除常驻的账单通知 */
    private fun cancelBillNotification(context: Context = applicationContext) {
        if (notificationId >= BillNotificationHelper.NOTIFICATION_ID_BASE) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .cancel(notificationId)
        }
    }
}
