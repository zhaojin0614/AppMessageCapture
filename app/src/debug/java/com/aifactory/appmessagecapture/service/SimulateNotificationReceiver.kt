package com.aifactory.appmessagecapture.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Debug 构建专用：接收 adb 广播，把指定包名/标题/正文的通知喂给
 * [MessageCaptureService] 的真实捕获管线（过滤 → 通知入库 → 账单提取 →
 * 去重 → 「记账成功」提醒），用于在未安装真实支付 App 时自测自动记账。
 *
 * 触发方式（终端需 UTF-8，Git Bash 默认满足）：
 * ```
 * adb shell am broadcast \
 *   -a com.aifactory.appmessagecapture.SIMULATE_NOTIFICATION \
 *   -n com.aifactory.appmessagecapture/.service.SimulateNotificationReceiver \
 *   --es pkg com.unionpay \
 *   --es title "支付助手：付款成功" \
 *   --es content "您尾号为5580的银行卡消费18.80元"
 * ```
 *
 * 结果码（`am broadcast` 输出中的 result=）：
 * -1 成功 | 1 参数缺失 | 2 通知监听服务未连接
 */
class SimulateNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val content = intent.getStringExtra(EXTRA_CONTENT)
        if (pkg.isNullOrBlank() || title == null || content == null) {
            resultCode = RESULT_BAD_ARGS
            resultData = "missing pkg/title/content extras"
            return
        }

        val service = MessageCaptureService.instance
        if (service == null) {
            Toast.makeText(
                context, "模拟失败：通知监听服务未连接，请先在系统设置开启「通知使用权」", Toast.LENGTH_LONG
            ).show()
            resultCode = RESULT_SERVICE_DOWN
            resultData = "MessageCaptureService not connected"
            return
        }

        service.simulateNotification(pkg, title, content)
        Toast.makeText(context, "已模拟来自 $pkg 的通知", Toast.LENGTH_SHORT).show()
        resultCode = Activity.RESULT_OK
        resultData = "simulated: $pkg"
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "pkg"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
        const val RESULT_BAD_ARGS = 1
        const val RESULT_SERVICE_DOWN = 2
    }
}
