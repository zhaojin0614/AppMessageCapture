package com.aifactory.appmessagecapture.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.aifactory.appmessagecapture.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 屏幕记账无障碍服务：监视支付 App 的「支付成功」页，从窗口文本中提取金额入账。
 *
 * 解决「付款了但不发系统通知 / 通知里没有金额」的场景（如京东 App）。
 * 工作方式：
 * 1. 系统按 res/xml/payment_screen_accessibility_config.xml 的 packageNames
 *    只投递监视名单内应用（[SupportedPaymentApps.screenWatchPackages]）的
 *    窗口切换事件 —— 其他 App 的屏幕内容完全不会到达本服务；
 * 2. 事件到达后收集活动窗口的文本节点，先做页面级判断
 *    （[PaymentScreenParsing.isPaymentSuccessPage]，含「支付成功/付款成功」才继续）；
 * 3. 金额提取只认「支付/付款」动词后紧跟的数字
 *    （[PaymentScreenParsing.extractAmountLine]），排除满减/红包/到手价等营销金额；
 * 4. 入库走 [BillIngestor]（与通知捕获共用去重与跨 App 合并），同一笔支付
 *    「渠道通知 + 商户成功页」只记一条账。
 *
 * 隐私边界：仅白名单包名前台时读取窗口文本，不做截屏、不存储页面内容
 * （入库的 title 只有金额那一行文本）。
 */
class PaymentScreenAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 同一页面的重复窗口事件防抖：key = 包名|金额 → 上次入账时刻 */
    private val recentCaptures = ConcurrentHashMap<String, Long>()

    companion object {
        private const val TAG = "ScreenBill"

        /** 同一支付成功页可能触发多次窗口事件（Activity + Dialog），30 秒内同包同金额只记一次 */
        private const val DEBOUNCE_MS = 30_000L

        /** 无障碍树遍历上限。真实树含大量不可见节点（广告 WebView 子树），
         *  uiautomator 只显示可见部分，实际规模远大于可见节点数 */
        private const val MAX_NODES = 1500

        /** 内容变化事件触发频繁（滚动/动画），同包遍历最小间隔 */
        private const val TRAVERSE_INTERVAL_MS = 1_000L

        @Volatile
        var isLive: Boolean = false
            private set

        /**
         * 当前运行的服务实例，供 debug 构建的 SimulateNotificationReceiver
         * 注入模拟页面文本使用（同进程访问）。
         */
        @Volatile
        internal var instance: PaymentScreenAccessibilityService? = null
            private set

        /** 每包上次树遍历时刻（内容变化事件节流用） */
        private val lastTraversalAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isLive = true
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        if (!SupportedPaymentApps.isScreenCaptureApp(packageName)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // 窗口/Activity 切换：必定遍历
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // 成功页多为 Activity 内页面切换（Fragment/WebView），不发窗口
                // 切换事件，靠内容变化触发；该事件高频，按包节流遍历
                val now = System.currentTimeMillis()
                val allowed = synchronized(lastTraversalAt) {
                    val last = lastTraversalAt[packageName] ?: 0L
                    if (now - last >= TRAVERSE_INTERVAL_MS) {
                        lastTraversalAt[packageName] = now
                        true
                    } else false
                }
                if (!allowed) return
            }
            else -> return
        }

        // 树遍历是逐节点 binder IPC，放到 IO 线程；事件本身只携带窗口元数据
        serviceScope.launch {
            val texts = collectWindowTexts()
            if (texts.isNotEmpty()) {
                handlePageContent(packageName, texts)
            }
        }
    }

    /**
     * 处理一个窗口的文本节点（真实事件与 debug 模拟共用入口）。
     */
    internal suspend fun handlePageContent(packageName: String, nodeTexts: List<String>) {
        val pageText = nodeTexts.joinToString("\n")
        if (!PaymentScreenParsing.isPaymentSuccessPage(packageName, pageText)) return

        val (amountLine, amount) = PaymentScreenParsing.extractAmountLine(nodeTexts) ?: run {
            android.util.Log.d(
                TAG,
                "成功页但未找到支付金额行: $packageName nodes=${nodeTexts.size} " +
                    "含京东支付=${nodeTexts.any { it.contains("京东支付") }} " +
                    "样例=${nodeTexts.take(10)}"
            )
            return
        }

        val now = System.currentTimeMillis()
        val key = "$packageName|$amount"
        recentCaptures[key]?.let { last ->
            if (now - last < DEBOUNCE_MS) return
        }
        recentCaptures[key] = now
        if (recentCaptures.size > 64) {
            recentCaptures.entries.removeIf { now - it.value > DEBOUNCE_MS }
        }

        // 与通知捕获一致的屏蔽名单
        if (PreferencesManager.getInstance(this).isAppBlocked(packageName)) return

        val appName = resolveAppName(packageName)
        val result = BillIngestor.record(
            context = this,
            packageName = packageName,
            appName = appName,
            title = amountLine,
            fullText = pageText,
            amount = amount,
            // 成功页里的「领5元红包」「共优惠」是营销词，方向固定为支出，
            // 不做收入关键词推断
            isIncome = false,
            timestamp = now
        )
        android.util.Log.d(TAG, "屏幕记账 $packageName ¥$amount ($appName) → $result")
    }

    /**
     * 收集监视应用所有窗口的文本节点（广度优先：不可见子树剪枝 + 节点数上限）。
     *
     * 必须遍历 [windows] 而非只遍历 rootInActiveWindow：真机实测京东把成功页
     * 头部（「京东支付¥xx」金额行）渲染在独立弹窗窗口里，活动窗口里只有
     * 标题和下方活动区，只取活动窗口会漏掉金额行。
     */
    private fun collectWindowTexts(): List<String> {
        val out = mutableListOf<String>()
        val seen = HashSet<String>()
        var visited = 0

        fun traverse(root: android.view.accessibility.AccessibilityNodeInfo?) {
            root ?: return
            if (root.packageName?.toString() !in SupportedPaymentApps.screenWatchPackages) return
            val queue = ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty() && visited < MAX_NODES) {
                val node = queue.removeFirst()
                visited++
                if (node.isVisibleToUser) {
                    node.text?.toString()?.takeIf { it.isNotBlank() }?.let {
                        if (seen.add(it)) out.add(it)
                    }
                    node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let {
                        if (seen.add(it)) out.add(it)
                    }
                }
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { child ->
                        // 不可见子树直接剪枝：账单金额只出现在可见区域，
                        // 且京东页面不可见子树（广告 WebView 等）规模巨大
                        if (child.isVisibleToUser) {
                            queue.add(child)
                        }
                    }
                }
            }
        }

        traverse(rootInActiveWindow)
        try {
            windows?.forEach { window -> traverse(window.root) }
        } catch (_: Exception) {
            // 部分 ROM 上 windows 访问可能异常，忽略（活动窗口已遍历）
        }
        return out
    }

    private fun resolveAppName(packageName: String): String = try {
        val info = packageManager.getApplicationInfo(packageName, 0)
        val label = packageManager.getApplicationLabel(info)
        if (label.isNullOrBlank()) packageName else label.toString()
    } catch (_: Exception) {
        packageName
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        isLive = false
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isLive = false
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * Debug-only: 把构造的「页面文本」直接喂给与真实窗口事件相同的处理管线
     * （页面判定 → 金额提取 → 防抖 → 入库），用于在无真实支付时自测。
     */
    internal fun simulatePage(packageName: String, vararg lines: String) {
        serviceScope.launch {
            handlePageContent(packageName, lines.toList())
        }
    }
}
