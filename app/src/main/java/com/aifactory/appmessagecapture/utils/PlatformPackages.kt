package com.aifactory.appmessagecapture.utils

/**
 * 平台账户 → 真实 App 包名映射。
 *
 * 平台账户页/选择弹窗/报表平台构成希望像消息列表那样展示真实 App 图标，
 * 但用户添加平台时只输入名称（如「微信」）。这里按名称关键字匹配到已知
 * 主流支付/消费 App 的包名，交给 [rememberAppIcon] 走系统图标缓存加载；
 * 匹配不上返回 null，调用方回退品牌色首字母。
 *
 * 与 [com.aifactory.appmessagecapture.service.SupportedPaymentApps]
 * 的包名清单保持一致的已知来源。
 */
object PlatformPackages {

    /** 平台名 → 包名（名称含关键字即命中，优先最长匹配） */
    private val PACKAGE_BY_KEYWORD = listOf(
        "美团外卖" to "com.sankuai.meituan.takeoutnew",
        "美团" to "com.sankuai.meituan",
        "支付宝" to "com.eg.android.AlipayGphone",
        "微信" to "com.tencent.mm",
        "云闪付" to "com.unionpay",
        "京东" to "com.jingdong.app.mall",
        "淘宝" to "com.taobao.taobao",
        "抖省省" to "com.ss.android.ugc.lifeservices",
        "抖音" to "com.ss.android.ugc.aweme",
        "拼多多" to "com.xunmeng.pinduoduo",
        "滴滴" to "com.sdu.didi.psnger",
        "银行" to "com.android.bankabc",
    )

    /** 按平台名解析包名；无匹配返回 null */
    fun packageForName(name: String): String? {
        val lower = name.lowercase()
        return PACKAGE_BY_KEYWORD.firstOrNull { (kw, _) ->
            lower.contains(kw.lowercase())
        }?.second
    }
}
