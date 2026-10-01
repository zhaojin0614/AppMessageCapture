package com.aifactory.appmessagecapture.features

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 功能模块开关状态仓库（进程内单例）。
 *
 * 开关持久化在 SharedPreferences（记录**已关闭**模块的 id 集合，默认全部开启），
 * 并以 StateFlow 暴露：设置页改开关、导航/设置/服务读取订阅同一份数据，
 * 关闭即刻生效——UI 重组隐藏入口，后台管线按当前值短路。
 *
 * [init] 在 Application.onCreate 调用；未 init 前读取一律视为全部开启，
 * 因此服务/Worker 里的开关判断不会因初始化顺序产生 NPE 或误判。
 */
object FeatureRepository {

    private const val PREFS_NAME = "feature_toggles"
    private const val KEY_DISABLED = "disabled_modules"

    @Volatile
    private var prefs: SharedPreferences? = null

    private val _disabled = MutableStateFlow<Set<FeatureModule>>(emptySet())

    /** 当前已关闭的模块集合（空集 = 全部开启）。 */
    val disabled: StateFlow<Set<FeatureModule>> = _disabled.asStateFlow()

    /** Application.onCreate 时调用：读取持久化的开关状态。 */
    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs?.getStringSet(KEY_DISABLED, emptySet()).orEmpty()
        _disabled.value = FeatureModule.entries.filter { it.id in stored }.toSet()
    }

    fun isEnabled(module: FeatureModule): Boolean = module !in _disabled.value

    /**
     * 设置模块开关并持久化。
     * @return false 表示被守卫拦截（主页 Tab 至少保留一个），调用方应提示用户
     */
    fun setEnabled(context: Context, module: FeatureModule, enable: Boolean): Boolean {
        val next = computeNextDisabled(_disabled.value, module, enable) ?: return false
        val sp = prefs ?: context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { prefs = it }
        sp.edit().putStringSet(KEY_DISABLED, next.map { it.id }.toSet()).apply()
        _disabled.value = next
        return true
    }

    /**
     * 计算开关后的「已关闭」集合（纯函数，便于单测）。
     * @return null 表示拒绝本次关闭：Tab 级模块全部关闭会让 App 失去所有主页
     */
    internal fun computeNextDisabled(
        current: Set<FeatureModule>,
        module: FeatureModule,
        enable: Boolean
    ): Set<FeatureModule>? {
        val next = current.toMutableSet()
        if (enable) {
            next.remove(module)
            return next
        }
        val isLastEnabledTab = module.isTab &&
            FeatureModule.entries.none { it.isTab && it != module && it !in current }
        if (isLastEnabledTab) return null
        next.add(module)
        return next
    }
}
