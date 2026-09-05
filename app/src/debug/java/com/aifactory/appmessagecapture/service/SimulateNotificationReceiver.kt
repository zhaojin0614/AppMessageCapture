package com.aifactory.appmessagecapture.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Debug 构建专用：接收 adb 广播，把指定包名/标题/正文的内容喂给真实捕获管线，
 * 用于在未安装真实支付 App 时自测自动记账。
 *
 * 两种模式：
 * - 默认（通知模式）：喂给 [MessageCaptureService]（过滤 → 通知入库 → 账单提取）
 * - `screen` extra = true（屏幕模式）：喂给 [PaymentScreenAccessibilityService]
 *   （成功页判定 → 动词金额提取 → 防抖 → 入库），模拟无障碍读到的窗口文本节点；
 *   title 与 content 中的多行文本按 \n 拆成独立节点（与真实事件粒度一致）
 *
 * 触发方式（终端需 UTF-8，Git Bash 默认满足）：
 * ```
 * adb shell am broadcast \
 *   -a com.zhaojin.billcatch.SIMULATE_NOTIFICATION \
 *   -n com.zhaojin.billcatch/com.aifactory.appmessagecapture.service.SimulateNotificationReceiver \
 *   --es pkg com.unionpay \
 *   --es title "支付助手：付款成功" \
 *   --es content "您尾号为5580的银行卡消费18.80元"
 *
 * # 屏幕模式（title/content 即窗口中的两行文本节点）
 * adb shell am broadcast ... --ez screen true \
 *   --es pkg com.jingdong.app.mall \
 *   --es title "支付成功" \
 *   --es content "京东支付¥30.38，共优惠¥0.02"
 * ```
 *
 * 结果码（`am broadcast` 输出中的 result=）：
 * -1 成功 | 1 参数缺失 | 2 通知监听服务未连接 | 3 无障碍屏幕记账服务未开启
 */
class SimulateNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val content = intent.getStringExtra(EXTRA_CONTENT)
        val screenMode = intent.getBooleanExtra(EXTRA_SCREEN, false)
        if (pkg.isNullOrBlank() || title == null || content == null) {
            resultCode = RESULT_BAD_ARGS
            resultData = "missing pkg/title/content extras"
            return
        }

        if (screenMode) {
            val service = PaymentScreenAccessibilityService.instance
            if (service == null) {
                Toast.makeText(
                    context, "模拟失败：屏幕记账服务未开启，请先在系统设置开启「屏幕记账（支付成功页）」无障碍服务", Toast.LENGTH_LONG
                ).show()
                resultCode = RESULT_SCREEN_SERVICE_DOWN
                resultData = "PaymentScreenAccessibilityService not enabled"
                return
            }
            service.simulatePage(pkg, title, content)
            Toast.makeText(context, "已模拟 $pkg 的支付成功页", Toast.LENGTH_SHORT).show()
            resultCode = Activity.RESULT_OK
            resultData = "simulated screen: $pkg"
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
        const val EXTRA_SCREEN = "screen"
        const val RESULT_BAD_ARGS = 1
        const val RESULT_SERVICE_DOWN = 2
        const val RESULT_SCREEN_SERVICE_DOWN = 3
    }
}
