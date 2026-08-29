package com.aifactory.appmessagecapture.service

/**
 * 支付应用识别（纯函数，无 Android 依赖，可单元测试）。
 *
 * 从 [MessageCaptureService] 抽出：原来按包名过滤的 when 块内嵌在服务私有方法里
 * 无法直接测试，测试只能镜像逻辑导致漂移风险。
 */
object SupportedPaymentApps {

    /**
     * 该通知是否为候选账单通知。仅放行已知支付应用，且各应用有独立的
     * 标题/正文门槛，用于排除营销推送、优惠券、后台服务等非消费通知。
     */
    fun isBillNotification(packageName: String, title: String, content: String): Boolean {
        return when (packageName) {
            "com.tencent.mm" ->                    // WeChat
                title.contains("微信支付")
            "com.eg.android.AlipayGphone" ->       // Alipay
                title.contains("交易提醒") &&
                    (content.contains("支出") || content.contains("收入"))
            "com.sankuai.meituan",
            "com.sankuai.meituan.takeoutnew" ->    // Meituan
                title.contains("付款")
            "com.unionpay" ->                      // UnionPay 云闪付
                title.contains("支付助手") && content.contains("消费")
            "com.android.bankabc" ->               // ABC 中国农业银行
                content.contains("支出")
            "com.ss.android.ugc.lifeservices" ->   // 抖省省（抖音团购）
                title.contains("支付成功")
            else -> false
        }
    }

    /**
     * 无障碍屏幕记账监视的包名（支付成功页捕获）。
     * 用于「付款了但不发通知/通知里没有金额」的应用：从支付完成页的
     * 屏幕文本里提取金额。系统层只投递这些包的窗口事件（见
     * res/xml/payment_screen_accessibility_config.xml 的 packageNames）。
     */
    val screenWatchPackages = setOf(
        "com.jingdong.app.mall"    // 京东（支付成功页不发系统通知）
    )

    fun isScreenCaptureApp(packageName: String): Boolean =
        packageName in screenWatchPackages

    /** 捕获通道标签（支持清单展示用） */
    const val CHANNEL_NOTIFY = "通知捕获"
    const val CHANNEL_SCREEN = "屏幕捕获"

    /** 支持自动记账的应用清单（记账页展示用；同步性由单测保证） */
    val supportedCaptureApps = listOf(
        SupportedCaptureApp("com.tencent.mm", "微信", CHANNEL_NOTIFY),
        SupportedCaptureApp("com.eg.android.AlipayGphone", "支付宝", CHANNEL_NOTIFY),
        SupportedCaptureApp("com.sankuai.meituan", "美团 / 美团外卖", CHANNEL_NOTIFY),
        SupportedCaptureApp("com.unionpay", "云闪付", CHANNEL_NOTIFY),
        SupportedCaptureApp("com.android.bankabc", "农业银行", CHANNEL_NOTIFY),
        SupportedCaptureApp("com.ss.android.ugc.lifeservices", "抖省省", CHANNEL_NOTIFY),
        SupportedCaptureApp("com.jingdong.app.mall", "京东", CHANNEL_SCREEN)
    )

    /**
     * App weight for cross-app merge priority.
     * Higher weight = primary app when merging bills.
     * E.g. Meituan (merchant) > WeChat Pay (payment channel).
     * Banks and UnionPay are payment channels, same tier as WeChat/Alipay.
     */
    fun appWeight(packageName: String): Int {
        return when (packageName) {
            "com.sankuai.meituan",
            "com.sankuai.meituan.takeoutnew" -> 100 // Meituan
            "com.ss.android.ugc.lifeservices" -> 90  // 抖省省（团购商户）
            "com.jingdong.app.mall" -> 80            // 京东（屏幕捕获）
            "com.eg.android.AlipayGphone" -> 50      // Alipay
            "com.tencent.mm" -> 50                   // WeChat
            "com.unionpay" -> 50                     // UnionPay
            "com.android.bankabc" -> 50              // ABC
            else -> 0
        }
    }
}

/**
 * 支持自动记账的应用清单（记账页「支持自动记账的App」展示用）。
 *
 * 与门槛逻辑（[isBillNotification]）和屏幕监视名单（[screenWatchPackages]）
 * 是两套数据，同步性由单测 `SupportedPaymentAppsTest.支持清单与捕获通道同步`
 * 保证——改门槛/监视名单时记得同步本清单，反之亦然。
 */
data class SupportedCaptureApp(
    val packageName: String,
    val appName: String,
    /** [SupportedPaymentApps.CHANNEL_NOTIFY] 或 [SupportedPaymentApps.CHANNEL_SCREEN] */
    val channel: String
)
