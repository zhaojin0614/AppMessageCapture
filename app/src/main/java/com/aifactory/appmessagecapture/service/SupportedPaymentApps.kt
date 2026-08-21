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
            "com.dianping.v1",                     // Dianping
            "com.jd.jrapp",                        // JD Finance
            "com.baidu.wallet" -> true             // Baidu Wallet
            else -> false
        }
    }

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
            "com.dianping.v1" -> 90                  // Dianping
            "com.ss.android.ugc.lifeservices" -> 90  // 抖省省（团购商户）
            "com.jd.jrapp" -> 80                     // JD Finance
            "com.baidu.wallet" -> 70                 // Baidu Wallet
            "com.eg.android.AlipayGphone" -> 50      // Alipay
            "com.tencent.mm" -> 50                   // WeChat
            "com.unionpay" -> 50                     // UnionPay
            "com.android.bankabc" -> 50              // ABC
            else -> 0
        }
    }
}
