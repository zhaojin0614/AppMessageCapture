#!/usr/bin/env bash
# =============================================================================
# 模拟支付通知，测试 AppMessageCapture 的自动记账功能（仅 debug 构建）
#
# 用法:
#   ./scripts/simulate_notification.sh <应用别名|包名> <标题> <内容>
#
# 应用别名 → 包名:
#   wechat    微信        com.tencent.mm
#   alipay    支付宝      com.eg.android.AlipayGphone
#   meituan   美团        com.sankuai.meituan
#   waimai    美团外卖    com.sankuai.meituan.takeoutnew
#   dianping  大众点评    com.dianping.v1
#   jd        京东金融    com.jd.jrapp
#   wallet    百度钱包    com.baidu.wallet
#   unionpay  云闪付      com.unionpay
#   abc       农业银行    com.android.bankabc
#   （其他值按原样作为包名使用）
#
# 示例（两条真实文案，注意引号包裹）:
#   ./scripts/simulate_notification.sh unionpay "支付助手：付款成功" \
#     "您尾号为5580的银行卡于19日18时38分消费18.80元。"
#
#   ./scripts/simulate_notification.sh abc "中国农业银行" \
#     "您尾号为7374的农行借记卡于08月13日18:45发生一笔支出17.66元，详情请点击。"
#
# 前置条件:
#   1. 已安装 debug 构建: ./gradlew :app:installDebug（release 包不含此入口）
#   2. 系统设置中已给本应用开启「通知使用权」
#   3. 多台设备时用 ANDROID_SERIAL=<serial> 指定，如:
#      ANDROID_SERIAL=emulator-5554 ./scripts/simulate_notification.sh ...
#
# 结果: 广播输出 result=-1 表示已注入；捕获成功后设备会弹「记账成功」通知，
#       账单可在 App 首页查看。同一消息 60 秒内重复发送会被去重规则忽略。
# =============================================================================
set -euo pipefail

# Resolve project root (script may be called from any directory)
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

ACTION="com.aifactory.appmessagecapture.SIMULATE_NOTIFICATION"
RECEIVER="com.aifactory.appmessagecapture/.service.SimulateNotificationReceiver"

if [[ $# -ne 3 ]]; then
    echo "用法: $0 <应用别名|包名> <标题> <内容>"
    echo "别名: wechat alipay meituan waimai dianping jd wallet unionpay abc"
    exit 1
fi

case "$1" in
    wechat)   PKG="com.tencent.mm" ;;
    alipay)   PKG="com.eg.android.AlipayGphone" ;;
    meituan)  PKG="com.sankuai.meituan" ;;
    waimai)   PKG="com.sankuai.meituan.takeoutnew" ;;
    dianping) PKG="com.dianping.v1" ;;
    jd)       PKG="com.jd.jrapp" ;;
    wallet)   PKG="com.baidu.wallet" ;;
    unionpay) PKG="com.unionpay" ;;
    abc)      PKG="com.android.bankabc" ;;
    *)        PKG="$1" ;;
esac

# Locate adb: PATH > project local.properties > ANDROID_HOME > default SDK dirs
if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
else
    ADB=""
    _SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [[ -z "$_SDK" && -f "$PROJECT_DIR/local.properties" ]]; then
        # local.properties stores the path in Java-properties escaped form,
        # e.g. sdk.dir=D\:\\APPS\\Android\\SDK → unescape to D:/APPS/Android/SDK
        _SDK="$(grep '^sdk\.dir=' "$PROJECT_DIR/local.properties" | head -1 | cut -d= -f2-)"
        _SDK="${_SDK//\\\\//}"   # \\ → /
        _SDK="${_SDK//\\:/:}"    # \: → :
        _SDK="${_SDK//\\//}"     # any leftover backslash → /
    fi
    if [[ -n "$_SDK" && -x "$_SDK/platform-tools/adb.exe" ]]; then
        ADB="$_SDK/platform-tools/adb.exe"
    elif [[ -n "$LOCALAPPDATA" && -x "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" ]]; then
        ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
    fi
fi

if [[ -z "$ADB" ]]; then
    echo "错误: 找不到 adb（PATH / local.properties / ANDROID_HOME / 默认 SDK 位置均未找到）" >&2
    exit 1
fi

echo "→ 模拟通知  包名: $PKG"
echo "→ 标题: $2"
echo "→ 内容: $3"

# MIUI/HyperOS 的 Greezer 会把发给缓存进程的广播转入延迟队列导致注入失效，
# 先把 App 拉到前台保证接收器立即执行（测试时也方便直接看到账单）。
"$ADB" shell am start -n com.aifactory.appmessagecapture/.MainActivity >/dev/null 2>&1 || true
sleep 1

"$ADB" shell am broadcast \
    -a "$ACTION" \
    -n "$RECEIVER" \
    --es pkg "$PKG" \
    --es title "$2" \
    --es content "$3"
