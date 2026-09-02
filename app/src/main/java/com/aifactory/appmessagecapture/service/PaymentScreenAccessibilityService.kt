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
 * 屏幕记账无障碍服务：监视支付 App 的「支付成功」页与淘宝闪购支付完成
 * 订单页，从窗口文本中提取支付信息入账。
 *
 * 解决「付款了但不发系统通知 / 通知里没有金额」的场景（京东 App、淘宝闪购）。
 * 工作方式：
 * 1. 系统按 res/xml/payment_screen_accessibility_config.xml 的 packageNames
 *    只投递监视名单内应用（[SupportedPaymentApps.screenWatchPackages]）的
 *    窗口切换事件 —— 其他 App 的屏幕内容完全不会到达本服务；
 * 2. 事件到达后收集窗口的文本节点，先做页面级判断
 *    （[PaymentScreenParsing.isCapturePage] 按包名路由：京东「支付成功」页 /
 *    淘宝闪购支付完成订单页）；
 * 3. 金额提取：京东只认「支付/付款」动词后紧跟的数字；淘宝闪购只认
 *    「实付」后紧跟的货币金额，排除满减/红包/到手价等营销金额；
 * 4. 入库走 [BillIngestor]（与通知捕获共用去重与跨 App 合并），同一笔支付
 *    「渠道通知 + 商户页」只记一条账。
 *
 * 隐私边界：仅白名单包名前台时读取窗口文本，不做截屏、不存储页面内容
 * （入库的 title 只有金额那一行文本）。
 */
class PaymentScreenAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        isLive = true
        instance = this
    }

    companion object {
        private const val TAG = "ScreenBill"

        /** 同一支付成功页可能触发多次窗口事件（Activity + Dialog），30 秒内同包同金额只记一次 */
        private const val DEBOUNCE_MS = 30_000L

        /** 无障碍树遍历兜底上限。实测京东成功页全树仅 236 节点，500 留一倍余量；
         *  正常情况下「找到即停」在此之前触发，不会触顶 */
        private const val MAX_NODES = 500

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
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 同一页面的重复窗口事件防抖：key = 包名|金额|标题 → 上次入账时刻 */
    private val recentCaptures = ConcurrentHashMap<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        if (!SupportedPaymentApps.isScreenCaptureApp(packageName)) return

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
        if (!PaymentScreenParsing.isCapturePage(packageName, pageText)) return

        if (packageName == SupportedPaymentApps.TAOBAO_PACKAGE ||
            packageName == SupportedPaymentApps.ELE_PACKAGE
        ) {
            recordShangouOrder(packageName, nodeTexts, pageText)
            return
        }

        val (amountLine, amount) = PaymentScreenParsing.extractAmountLine(nodeTexts) ?: run {
            android.util.Log.d(
                TAG,
                "成功页但未找到支付金额行: $packageName nodes=${nodeTexts.size} " +
                    "含京东支付=${nodeTexts.any { it.contains("京东支付") }} " +
                    "样例=${nodeTexts.take(10)}"
            )
            return
        }

        val title = amountLine
        if (!passesDebounce("$packageName|$amount|$title")) return

        // 与通知捕获一致的屏蔽名单
        if (PreferencesManager.getInstance(this).isAppBlocked(packageName)) return

        val appName = resolveAppName(packageName)
        val result = BillIngestor.record(
            context = this,
            packageName = packageName,
            appName = appName,
            title = title,
            fullText = pageText,
            amount = amount,
            // 成功页里的「领5元红包」「共优惠」是营销词，方向固定为支出，
            // 不做收入关键词推断
            isIncome = false,
            timestamp = System.currentTimeMillis()
        )
        android.util.Log.d(TAG, "屏幕记账 $packageName ¥$amount ($appName) → $result")
    }

    /**
     * 淘宝闪购订单页入库：实付金额 + 商户名 + 下单时间。
     *
     * 订单页是持久页面，幂等性靠两个确定性字段交给 [BillIngestor] 内容去重：
     * timestamp = 页面上的「下单时间」（每单唯一，历史订单页该行折叠不可见、
     * 不命中门槛，天然不会重复入账）、title = 商户名 + 实付金额。同一订单
     * 再次打开时，同 App + 同金额 + 同标题 + 同方向且时间窗口锚定在下单时间
     * 上，必然判为重复丢弃。
     */
    private suspend fun recordShangouOrder(
        packageName: String,
        nodeTexts: List<String>,
        pageText: String
    ) {
        val amount = TaobaoShangouParsing.extractPaidAmount(nodeTexts) ?: run {
            android.util.Log.d(TAG, "闪购订单页但未找到实付金额行 nodes=${nodeTexts.size} 样例=${nodeTexts.take(10)}")
            return
        }
        val merchant = TaobaoShangouParsing.extractMerchant(nodeTexts)
        val orderTime = TaobaoShangouParsing.parseOrderTimeMillis(nodeTexts)

        val title = buildString {
            merchant?.let { append(it).append(' ') }
            append("实付¥").append(String.format("%.2f", amount))
        }
        if (!passesDebounce("$packageName|$amount|$title")) return

        if (PreferencesManager.getInstance(this).isAppBlocked(packageName)) return

        val result = BillIngestor.record(
            context = this,
            packageName = packageName,
            appName = SupportedPaymentApps.screenAppDisplayName(packageName) ?: resolveAppName(packageName),
            title = title,
            fullText = pageText,
            amount = amount,
            // 页面含「返12元外卖红包」等营销词，方向固定为支出
            isIncome = false,
            // 下单时间是每单唯一的确定值：既是账单的真实发生时间，
            // 也是重复打开订单页时内容去重的锚点
            timestamp = orderTime ?: System.currentTimeMillis()
        )
        android.util.Log.d(
            TAG,
            "闪购订单记账 $title 下单时间=$orderTime → $result"
        )
    }

    /** 同一页面可能触发多次窗口事件（Activity + Dialog），30 秒内同 key 只处理一次 */
    private fun passesDebounce(key: String): Boolean {
        val now = System.currentTimeMillis()
        recentCaptures[key]?.let { last ->
            if (now - last < DEBOUNCE_MS) return false
        }
        recentCaptures[key] = now
        if (recentCaptures.size > 64) {
            recentCaptures.entries.removeIf { now - it.value > DEBOUNCE_MS }
        }
        return true
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
        var windowCount = 0

        // 「找到即停」：门槛词与金额行都拿到后，剩余子树无需再遍历。
        // 京东：成功页标题 + 支付动词金额行；
        // 淘宝闪购：闪购标 + 实付金额行 + 下单时间（三者分别在不同的
        // 文本节点上，缺一会读不到关键字段）
        var seenSuccess = false
        var amountFound = false
        var seenShangou = false
        var seenPaidAmount = false
        var seenOrderDatetime = false

        fun earlyStop(): Boolean =
            (seenSuccess && amountFound) ||
                (seenShangou && seenPaidAmount && seenOrderDatetime)

        fun accept(text: String) {
            if (!seen.add(text)) return
            out.add(text)
            if (PaymentScreenParsing.isSuccessText(text)) seenSuccess = true
            if (BillParsing.parseAmountAfterPaymentVerb(text) != null) amountFound = true
            if (text.contains("闪购")) seenShangou = true
            if (TaobaoShangouParsing.parsePaidAmount(text) != null) seenPaidAmount = true
            if (text.contains("下单时间") || TaobaoShangouParsing.containsOrderDatetime(text)) {
                seenOrderDatetime = true
            }
        }

        fun traverse(root: android.view.accessibility.AccessibilityNodeInfo?) {
            root ?: return
            if (root.packageName?.toString() !in SupportedPaymentApps.screenWatchPackages) return
            windowCount++
            val queue = ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
            queue.add(root)
            while (queue.isNotEmpty() && visited < MAX_NODES) {
                val node = queue.removeFirst()
                visited++
                if (node.isVisibleToUser) {
                    node.text?.toString()?.takeIf { it.isNotBlank() }?.let { accept(it) }
                    if (earlyStop()) return
                    node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { accept(it) }
                    if (earlyStop()) return
                }
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { child ->
                        // 不可见子树直接剪枝：账单金额只出现在可见区域，
                        // 且京东页面不可见子树（广告 WebView 等）规模不小
                        if (child.isVisibleToUser) {
                            queue.add(child)
                        }
                    }
                }
            }
        }

        traverse(rootInActiveWindow)
        if (!earlyStop()) {
            try {
                windows?.forEach { window ->
                    traverse(window.root)
                    if (earlyStop()) return@forEach
                }
            } catch (_: Exception) {
                // 部分 ROM 上 windows 访问可能异常，忽略（活动窗口已遍历）
            }
        }
        android.util.Log.d(TAG, "遍历完成: 窗口数=$windowCount visited=$visited collected=${out.size} 找到即停=${earlyStop()}")
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
