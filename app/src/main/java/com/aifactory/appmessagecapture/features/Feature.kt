package com.aifactory.appmessagecapture.features

/**
 * 功能模块注册表：App 的每个独立功能作为一个模块在此统一声明。
 *
 * 模块分两类：
 * - [isTab] = true：主导航 Tab 级模块（消息/记账/生日），关闭后整个 Tab 隐藏，
 *   且至少保留一个开启（守卫见 [FeatureRepository.computeNextDisabled]）；
 * - [isTab] = false：子功能模块（报表/预算/平台账户/商户记忆/周期账单），
 *   关闭后隐藏对应入口，相关后台动作（预算提醒、周期补记）一并停用。
 *
 * 需要按模块开关渲染的界面与后台服务统一引用本注册表取状态，
 * 不要在调用点散落硬编码的开关判断。
 */
enum class FeatureModule(
    val id: String,
    val label: String,
    val description: String,
    val isTab: Boolean
) {
    /** 消息捕获：监听并保存系统通知（关闭后通知不再入库） */
    MESSAGES("messages", "消息捕获", "监听并保存系统通知", isTab = true),

    /** 自动记账：通知与支付成功页两条捕获通道（关闭后不再自动生成账单） */
    BILLS("bills", "自动记账", "支付通知与成功页自动记入账单", isTab = true),

    /** 生日提醒：闹钟、通知与桌面小组件（关闭后撤掉已注册的闹钟） */
    BIRTHDAY("birthday", "生日提醒", "生日闹钟提醒与桌面小组件", isTab = true),

    /** 报表统计：记账页顶栏入口 */
    REPORT("report", "报表统计", "收支趋势、分类构成与平台构成", isTab = false),

    /** 预算管理：预算设置、进度卡与超额提醒 */
    BUDGET("budget", "预算管理", "月度预算进度与超额提醒", isTab = false),

    /** 平台账户：余额管理与自动对账的管理界面 */
    PLATFORMS("platforms", "平台账户", "平台余额管理与自动对账", isTab = false),

    /** 商户记忆：自动分类记忆的管理界面 */
    MERCHANT_MEMORY("merchant_memory", "商户记忆", "自动分类的记忆查看与修正", isTab = false),

    /** 周期账单：固定周期自动补记 */
    RECURRING("recurring", "周期账单", "房租订阅等固定周期自动记账", isTab = false);

    companion object {
        fun byId(id: String): FeatureModule? = entries.firstOrNull { it.id == id }
    }
}
