package com.aifactory.appmessagecapture.birthday.utils

import android.util.Log
import com.aifactory.appmessagecapture.BuildConfig
import timber.log.Timber

/**
 * BirthdayKeeper 全局日志工具类。
 *
 * 统一 TAG 为 [TAG]，封装 Timber 与标准 [Log]，确保在任何场景下都能输出日志。
 * 所有关键逻辑、数据库操作、文件读写、定时任务、异常捕获均需通过此类打印。
 */
object BirthdayLog {

    const val TAG = "BirthdayLog"

    /** 是否已安装 Timber Tree（在 Application.onCreate 中初始化） */
    @Volatile
    private var timberInstalled = false

    /**
     * 在 [Application.onCreate] 中调用，安装 [Timber.DebugTree]。
     * 重复调用是安全的。
     */
    fun install() {
        if (!timberInstalled) {
            synchronized(this) {
                if (!timberInstalled) {
                    if (BuildConfig.DEBUG) {
                        Timber.plant(Timber.DebugTree())
                    }
                    // 生产环境可在此添加 FileLoggingTree / CrashReportingTree
                    timberInstalled = true
                    i("BirthdayLog installed. Debug=${BuildConfig.DEBUG}")
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Verbose
    // -------------------------------------------------------------------------
    fun v(msg: String, vararg args: Any?) {
        if (timberInstalled) {
            Timber.tag(TAG).v(msg, *args)
        } else {
            Log.v(TAG, msg.format(args))
        }
    }

    // -------------------------------------------------------------------------
    // Debug
    // -------------------------------------------------------------------------
    fun d(msg: String, vararg args: Any?) {
        if (timberInstalled) {
            Timber.tag(TAG).d(msg, *args)
        } else {
            Log.d(TAG, msg.format(args))
        }
    }

    // -------------------------------------------------------------------------
    // Info
    // -------------------------------------------------------------------------
    fun i(msg: String, vararg args: Any?) {
        if (timberInstalled) {
            Timber.tag(TAG).i(msg, *args)
        } else {
            Log.i(TAG, msg.format(args))
        }
    }

    // -------------------------------------------------------------------------
    // Warning
    // -------------------------------------------------------------------------
    fun w(msg: String, vararg args: Any?) {
        if (timberInstalled) {
            Timber.tag(TAG).w(msg, *args)
        } else {
            Log.w(TAG, msg.format(args))
        }
    }

    fun w(throwable: Throwable, msg: String, vararg args: Any?) {
        if (timberInstalled) {
            Timber.tag(TAG).w(throwable, msg, *args)
        } else {
            Log.w(TAG, msg.format(args), throwable)
        }
    }

    // -------------------------------------------------------------------------
    // Error
    // -------------------------------------------------------------------------
    fun e(msg: String, vararg args: Any?) {
        if (timberInstalled) {
            Timber.tag(TAG).e(msg, *args)
        } else {
            Log.e(TAG, msg.format(args))
        }
    }

    fun e(throwable: Throwable, msg: String, vararg args: Any?) {
        if (timberInstalled) {
            Timber.tag(TAG).e(throwable, msg, *args)
        } else {
            Log.e(TAG, msg.format(args), throwable)
        }
    }

    // -------------------------------------------------------------------------
    // 便捷方法：打印异常详情（message + full stacktrace）
    // -------------------------------------------------------------------------
    fun logException(tagPrefix: String, throwable: Throwable) {
        val sb = StringBuilder()
        sb.appendLine("[$tagPrefix] Exception caught!")
        sb.appendLine("Message: ${throwable.message}")
        sb.appendLine("Stacktrace:")
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        throwable.printStackTrace(pw)
        sb.append(sw.toString())
        e(sb.toString())
    }

    // -------------------------------------------------------------------------
    // 便捷方法：打印方法的入参与出参（用于核心计算逻辑审计）
    // -------------------------------------------------------------------------
    fun logMethodCall(methodName: String, params: Map<String, Any?>, result: Any? = null) {
        val sb = StringBuilder()
        sb.appendLine("[$methodName] >>> CALL")
        params.forEach { (k, v) -> sb.appendLine("  Param | $k=$v") }
        result?.let { sb.appendLine("  Result| $it") }
        sb.append("[$methodName] <<< END")
        d(sb.toString())
    }

    // -------------------------------------------------------------------------
    // 私有辅助
    // -------------------------------------------------------------------------
    private fun String.format(args: Array<out Any?>): String {
        return if (args.isEmpty()) this else String.format(this, *args)
    }
}
