package com.aifactory.appmessagecapture.birthday.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * 桌面长按 App 图标快捷方式的跳板（对应微博「添加小组件」的交互）：
 * 直接向桌面发起「固定生日小组件」请求后立即退出。
 *
 * 透明无窗（Theme.NoDisplay），请求本身是同步 IPC；是否弹确认框/直接固定
 * 由各桌面（Launcher）决定，桌面不支持固定时给出提示兜底。
 */
class AddBirthdayWidgetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(this, BirthdayWidgetReceiver::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            val accepted = manager.requestPinAppWidget(provider, null, null)
            if (!accepted) toast("当前桌面暂不支持快捷添加小组件")
        } else {
            toast("当前桌面不支持快捷添加，请在桌面空白处长按添加")
        }
        finish()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
