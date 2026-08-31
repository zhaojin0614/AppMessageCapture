package com.aifactory.appmessagecapture.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * 可选主色调：设置页切换，即时生效并持久化。
 *
 * 只覆盖 MaterialTheme 的 primary 角色组（primary / onPrimary / primaryContainer /
 * onPrimaryContainer），底部导航胶囊与平台页英雄卡的品牌渐变同样跟随。
 * secondary（雾蓝）与 tertiary（暖金）保持固定——图表配色与生日模块需要稳定辨识色，
 * 环境光斑背景本身是多色流动装饰，也不随主色变化。
 */
enum class AccentColor(val label: String, val primary: Color) {
    MINT("薄荷绿", Color(0xFF0FA18D)),
    OCEAN("海雾蓝", Color(0xFF4A7BC4)),
    VIOLET("紫罗兰", Color(0xFF8B6FD6)),
    ROSE("樱花粉", Color(0xFFD9648F)),
    CORAL("珊瑚橙", Color(0xFFE07A4F)),
    GOLD("暖阳金", Color(0xFFC09A3E)),
    SLATE("石墨灰", Color(0xFF64707D));

    /** 品牌渐变亮端（导航胶囊 / 英雄卡 / 渐变按钮的浅色段） */
    val gradientEnd: Color get() = lerp(primary, Color.White, 0.32f)

    /** 浅色主题容器角色（标签底色等） */
    val containerLight: Color get() = lerp(primary, Color.White, 0.84f)
    val onContainerLight: Color get() = lerp(primary, Color.Black, 0.45f)

    /** 深色主题容器角色 */
    val containerDark: Color get() = lerp(primary, Color.Black, 0.68f)
    val onContainerDark: Color get() = lerp(primary, Color.White, 0.72f)
}

/**
 * 主色调全局状态：单一 Compose 状态源，主题与任意组件直接读取，
 * 变更即全 app 重组。MainActivity.onCreate 时调用 [init] 载入持久化值。
 */
object AccentColorRepository {
    private const val PREFS = "appearance_prefs"
    private const val KEY_ACCENT = "accent_color"

    var current by mutableStateOf(AccentColor.MINT)
        private set

    fun init(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACCENT, null)
        current = saved?.let { id -> AccentColor.entries.firstOrNull { it.name == id } }
            ?: AccentColor.MINT
    }

    fun set(context: Context, accent: AccentColor) {
        current = accent
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACCENT, accent.name).apply()
    }
}
