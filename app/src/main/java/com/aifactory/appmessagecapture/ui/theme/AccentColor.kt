package com.aifactory.appmessagecapture.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb

/**
 * 预设主色调：取自主流 App 的品牌强调色（微信绿/支付宝蓝/B站粉/iOS 系统色等）
 * 与经典 UI 配色，均校准到可承载白色前景的中深明度。
 */
enum class AccentColor(val label: String, val primary: Color) {
    MINT("薄荷绿", Color(0xFF0FA18D)),        // 默认 · 原品牌色
    WECHAT("微信绿", Color(0xFF07C160)),      // 微信
    FOREST("森林绿", Color(0xFF2F8F5B)),
    TEAL("湖水青", Color(0xFF1E9E9E)),
    SKY("天青蓝", Color(0xFF24A1DE)),         // Telegram
    ALIPAY("宝石蓝", Color(0xFF1677FF)),      // 支付宝
    OCEAN("海雾蓝", Color(0xFF4A7BC4)),
    IOSBLUE("知更蓝", Color(0xFF007AFF)),     // iOS systemBlue
    INDIGO("靛蓝", Color(0xFF5865D6)),        // iOS indigo / Discord 系
    NAVY("藏青", Color(0xFF3D5A80)),
    VIOLET("紫罗兰", Color(0xFF8B6FD6)),
    IMPERIAL("典雅紫", Color(0xFF6750A4)),    // Material 3 baseline
    ROSE("樱花粉", Color(0xFFD9648F)),
    BILIBILI("哔哩粉", Color(0xFFFB7299)),    // 哔哩哔哩
    MAGENTA("玫红", Color(0xFFE0486C)),
    CRIMSON("绯红", Color(0xFFDD4B4B)),       // 微博红系
    CORAL("珊瑚橙", Color(0xFFE07A4F)),
    GOLD("暖阳金", Color(0xFFC09A3E)),
    COFFEE("咖啡棕", Color(0xFF8B6A50)),
    SLATE("石墨灰", Color(0xFF64707D));
}

/**
 * 当前生效的主题色变体：预设（id = 枚举名）或用户自定义（id = [CUSTOM_ID]）。
 * primary 角色组的明暗容器色由 primary 插值派生，保证任意颜色下深浅主题都可用。
 */
data class AccentVariant(
    val id: String,
    val label: String,
    val primary: Color
) {
    val isCustom: Boolean get() = id == CUSTOM_ID

    /** 品牌渐变亮端（导航胶囊 / 英雄卡 / 渐变按钮的浅色段） */
    val gradientEnd: Color get() = lerp(primary, Color.White, 0.32f)

    /** 浅色主题容器角色（标签底色等） */
    val containerLight: Color get() = lerp(primary, Color.White, 0.84f)
    val onContainerLight: Color get() = lerp(primary, Color.Black, 0.45f)

    /** 深色主题容器角色 */
    val containerDark: Color get() = lerp(primary, Color.Black, 0.68f)
    val onContainerDark: Color get() = lerp(primary, Color.White, 0.72f)

    companion object {
        const val CUSTOM_ID = "custom"

        fun fromPreset(preset: AccentColor) = AccentVariant(preset.name, preset.label, preset.primary)
    }
}

/**
 * 主色调全局状态：单一 Compose 状态源，主题与任意组件直接读取，
 * 变更即全 app 重组。MainActivity.onCreate 时调用 [init] 载入持久化值。
 * 支持用户通过调色板自选任意颜色（ARGB 持久化）。
 */
object AccentColorRepository {
    private const val PREFS = "appearance_prefs"
    private const val KEY_ID = "accent_color"
    private const val KEY_CUSTOM = "custom_accent_color"

    var current by mutableStateOf(AccentVariant.fromPreset(AccentColor.MINT))
        private set

    /** 最近一次自定义的颜色（跨预设切换保留，供调色板回显） */
    var lastCustom by mutableStateOf<Color?>(null)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val customArgb = prefs.getInt(KEY_CUSTOM, -1)
        if (customArgb != -1) lastCustom = Color(customArgb)
        current = when (val savedId = prefs.getString(KEY_ID, null)) {
            AccentVariant.CUSTOM_ID ->
                if (customArgb != -1) AccentVariant(AccentVariant.CUSTOM_ID, "自定义", Color(customArgb))
                else AccentVariant.fromPreset(AccentColor.MINT)
            null -> AccentVariant.fromPreset(AccentColor.MINT)
            else -> AccentColor.entries.firstOrNull { it.name == savedId }
                ?.let(AccentVariant::fromPreset)
                ?: AccentVariant.fromPreset(AccentColor.MINT)
        }
    }

    fun setPreset(context: Context, preset: AccentColor) {
        current = AccentVariant.fromPreset(preset)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ID, preset.name)
            .apply()
    }

    fun setCustom(context: Context, color: Color) {
        current = AccentVariant(AccentVariant.CUSTOM_ID, "自定义", color)
        lastCustom = color
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ID, AccentVariant.CUSTOM_ID)
            .putInt(KEY_CUSTOM, color.toArgb())
            .apply()
    }
}
