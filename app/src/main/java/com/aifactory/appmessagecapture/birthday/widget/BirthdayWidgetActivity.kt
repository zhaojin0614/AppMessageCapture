package com.aifactory.appmessagecapture.birthday.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.aifactory.appmessagecapture.MainActivity
import com.aifactory.appmessagecapture.birthday.utils.BirthdayLog

/**
 * 桌面组件点击中转 Activity。
 *
 * 无界面（Theme.NoDisplay），职责：
 * 1. 接收到 Glance widget 点击后，触发 widget 刷新广播，确保数据最新
 * 2. 启动 [MainActivity] 并定位到生日 Tab
 * 3. 立即 finish
 */
class BirthdayWidgetActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO_TAB, "birthday")
        }
        startActivity(intent)
        finish()
    }
}
