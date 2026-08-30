#!/usr/bin/env bash
# =============================================================================
# 模拟支付通知/支付成功页，测试 AppMessageCapture 的自动记账功能（仅 debug 构建）
#
# 用法:
#   ./scripts/simulate_notification.sh <应用别名|包名> <标题> <内容> [--screen]
#
#   默认为通知模式（喂给通知监听服务）；
#   加 --screen 为屏幕模式（把标题/内容当作无障碍读到的窗口文本节点，
#   喂给屏幕记账服务，需先开启「屏幕记账（支付成功页）」无障碍服务）
#
# 应用别名 → 包名:
#   wechat    微信        com.tencent.mm
#   alipay    支付宝      com.eg.android.AlipayGphone
#   meituan   美团        com.sankuai.meituan
#   waimai    美团外卖    com.sankuai.meituan.takeoutnew
#   jdapp     京东        com.jingdong.app.mall
#   unionpay  云闪付      com.unionpay
#   abc       农业银行    com.android.bankabc
#   dss       抖省省      com.ss.android.ugc.lifeservices
#   （其他值按原样作为包名使用）
#
# 示例（两条真实文案，注意引号包裹）:
#   ./scripts/simulate_notification.sh unionpay "支付助手：付款成功" \
#     "您尾号为5580的银行卡于19日18时38分消费18.80元。"
#
#   ./scripts/simulate_notification.sh abc "中国农业银行" \
#     "您尾号为7374的农行借记卡于08月13日18:45发生一笔支出17.66元，详情请点击。"
#
# 屏幕模式示例（模拟京东支付成功页，效果等同真实付款后的屏幕捕获）:
#   ./scripts/simulate_notification.sh jdapp "支付成功" \
#     "京东支付¥30.38，共优惠¥0.02" --screen
#
# 前置条件:
#   1. 已安装 debug 构建: ./gradlew :app:installDebug（release 包不含此入口）
#   2. 系统设置中已给本应用开启「通知使用权」（通知模式）或
#      「屏幕记账（支付成功页）」无障碍服务（屏幕模式）
#   3. 多台设备时用 ANDROID_SERIAL=<serial> 指定，如:
#      ANDROID_SERIAL=emulator-5554 ./scripts/simulate_notification.sh ...
#
# 结果: 广播输出 result=-1 表示已注入（result=2 通知服务未连接，
#       result=3 屏幕记账服务未开启）；捕获成功后设备会弹「记账成功」通知，
#       账单可在 App 首页查看。同一笔账 60 秒内重复发送会被去重规则忽略。
# =============================================================================
set -euo pipefail

# Resolve project root (script may be called from any directory)
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

ACTION="com.zhaojin.billcatch.SIMULATE_NOTIFICATION"
RECEIVER="com.zhaojin.billcatch/com.aifactory.appmessagecapture.service.SimulateNotificationReceiver"

SCREEN_MODE=0
ARGS=()
for arg in "$@"; do
    if [[ "$arg" == "--screen" ]]; then SCREEN_MODE=1; else ARGS+=("$arg"); fi
done

if [[ ${#ARGS[@]} -ne 3 ]]; then
    echo "用法: $0 <应用别名|包名> <标题> <内容> [--screen]"
    echo "别名: wechat alipay meituan waimai jdapp unionpay abc dss"
    exit 1
fi

case "${ARGS[0]}" in
    wechat)   PKG="com.tencent.mm" ;;
    alipay)   PKG="com.eg.android.AlipayGphone" ;;
    meituan)  PKG="com.sankuai.meituan" ;;
    waimai)   PKG="com.sankuai.meituan.takeoutnew" ;;
    jdapp)    PKG="com.jingdong.app.mall" ;;
    unionpay) PKG="com.unionpay" ;;
    abc)      PKG="com.android.bankabc" ;;
    dss)      PKG="com.ss.android.ugc.lifeservices" ;;
    *)        PKG="${ARGS[0]}" ;;
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

if [[ $SCREEN_MODE -eq 1 ]]; then
    echo "→ 模拟支付成功页（屏幕模式）  包名: $PKG"
else
    echo "→ 模拟通知  包名: $PKG"
fi
echo "→ 标题: ${ARGS[1]}"
echo "→ 内容: ${ARGS[2]}"

# MIUI/HyperOS 的 Greezer 会把发给缓存进程的广播转入延迟队列导致注入失效，
# 先把 App 拉到前台保证接收器立即执行（测试时也方便直接看到账单）。
"$ADB" shell am start -n com.zhaojin.billcatch/com.aifactory.appmessagecapture.MainActivity >/dev/null 2>&1 || true
sleep 1

# Quote an argument for the device-side shell: adb shell concatenates args and
# the remote /bin/sh re-parses them, so notification text containing > < & ' ;
# etc. must be single-quoted (with ' escaped as '\'') to survive both shells.
shquote() { printf "'%s'" "${1//\'/\'\\\'\'}"; }

SCREEN_EXTRA=""
if [[ $SCREEN_MODE -eq 1 ]]; then
    SCREEN_EXTRA="--ez screen true "
fi

"$ADB" shell "am broadcast -a $(shquote "$ACTION") -n $(shquote "$RECEIVER") ${SCREEN_EXTRA}--es pkg $(shquote "$PKG") --es title $(shquote "${ARGS[1]}") --es content $(shquote "${ARGS[2]}")"
