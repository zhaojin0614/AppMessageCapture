#!/usr/bin/env bash
# =============================================================================
# 旧版「捕账」→ 新版数据迁移（仅 debug 构建，需 adb 与两端 app 均为 debug 包）
#
# 场景：应用改名换包（com.aifactory.appmessagecapture → com.zhaojin.billcatch）
# 后，系统视为两个独立应用，旧 app 私有目录里的 Room 数据库（账单/消息/
# 生日/预算/平台账户）与 SharedPreferences（主题色等）无法自动带过来。
# 本脚本用 adb run-as 把这些文件从旧 app 拷进新 app，实现原地迁移。
#
# 用法:
#   ./scripts/migrate_old_app.sh [--yes] [--dry-run] [--no-prefs]
#       [--old <旧包名>] [--new <新包名>] [--db <数据库名>]
#
# 默认: old=com.aifactory.appmessagecapture  new=com.zhaojin.billcatch
#       db=notification_database
#
# 行为:
#   1. 校验两个包都已安装且 run-as 可用（debug 构建才有 run-as）
#   2. 强停旧 app（让 WAL 落盘成一致状态）→ 拉取 数据库 + -wal/-shm
#      + shared_prefs/*.xml 到本地临时目录
#   3. 把新 app 当前的同名文件备份到 <项目>/migration_backup_<时间戳>/
#      （覆盖前留后路，可手动推回回滚）
#   4. 确认后强停新 app → 推入旧数据覆盖 → chmod 660 → 完成
#   --dry-run : 只验证迁移通路（真实拉取 + 往新 app 写一个探针文件再删除），
#               不覆盖新 app 的任何真实数据
#   --no-prefs: 不迁移 SharedPreferences（仅迁移数据库）
#
# 注意:
#   - 新 app 建议「装好但从未打开」时迁移；已打开过也没关系，脚本会强停再覆盖。
#     首次打开新 app 时 Room 自动把旧库结构升级到最新版本（迁移链完整）。
#   - release 包（非 debuggable）run-as 不可用，此路不通；此时可把工程临时
#     改回旧包名构建 debug 包（同签名）覆盖安装旧 app，用应用内导出/导入迁移。
#   - 多设备连接时用 ANDROID_SERIAL=<serial> 指定目标。
# =============================================================================
set -euo pipefail

# Git Bash 会把 /data/... 参数转换成本地 Windows 路径，这里关掉
export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL="*"

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

OLD_PKG="com.aifactory.appmessagecapture"
NEW_PKG="com.zhaojin.billcatch"
DB_NAME="notification_database"
COPY_PREFS=1
ASSUME_YES=0
DRY_RUN=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --old) OLD_PKG="$2"; shift 2 ;;
        --new) NEW_PKG="$2"; shift 2 ;;
        --db)  DB_NAME="$2"; shift 2 ;;
        --no-prefs) COPY_PREFS=0; shift ;;
        --yes) ASSUME_YES=1; shift ;;
        --dry-run) DRY_RUN=1; shift ;;
        *) echo "未知参数: $1"; exit 1 ;;
    esac
done

# ── 定位 adb（与 simulate_notification.sh 同一套逻辑）─────────────────────
if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
else
    ADB=""
    _SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [[ -z "$_SDK" && -f "$PROJECT_DIR/local.properties" ]]; then
        _SDK="$(grep '^sdk\.dir=' "$PROJECT_DIR/local.properties" | head -1 | cut -d= -f2-)"
        _SDK="${_SDK//\\\\//}"; _SDK="${_SDK//\\:/:}"; _SDK="${_SDK//\\//}"
    fi
    if [[ -n "$_SDK" && -x "$_SDK/platform-tools/adb.exe" ]]; then
        ADB="$_SDK/platform-tools/adb.exe"
    elif [[ -n "${LOCALAPPDATA:-}" && -x "$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" ]]; then
        ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
    fi
fi
[[ -z "$ADB" ]] && { echo "错误: 找不到 adb"; exit 1; }

installed() { "$ADB" shell pm list packages 2>/dev/null | grep -q "package:$1"; }
runas_ok()  { "$ADB" shell "run-as $1 ls files" >/dev/null 2>&1; }

echo "════════════════════════════════════════════════════"
echo " 旧版数据迁移  $OLD_PKG → $NEW_PKG"
echo "════════════════════════════════════════════════════"

# ── 1. 前置校验 ───────────────────────────────────────────────────────────
installed "$OLD_PKG" || { echo "错误: 旧 app $OLD_PKG 未安装"; exit 1; }
installed "$NEW_PKG" || { echo "错误: 新 app $NEW_PKG 未安装"; exit 1; }
runas_ok "$OLD_PKG" || { echo "错误: 旧 app 不可 run-as（非 debug 构建），见脚本头注释的替代方案"; exit 1; }
runas_ok "$NEW_PKG" || { echo "错误: 新 app 不可 run-as（请用 :app:installDebug 安装 debug 构建）"; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
echo "→ 本地暂存目录: $WORK"

# run-as cat 拉取私有文件（exec-out 二进制安全）
pull_file() { # <包名> <app内相对路径> <本地输出>
    "$ADB" exec-out "run-as $1 cat $2" > "$3"
}
# push → /data/local/tmp 中转 → run-as cp 进私有目录。
# 本地路径需转成 Windows 形式：MSYS_NO_PATHCONV 已关闭自动转换，
# 而 adb.exe 看不见 Git Bash 的 /tmp 等虚拟路径
push_file() { # <包名> <本地文件> <app内相对路径>
    local src="$2"
    command -v cygpath >/dev/null 2>&1 && src="$(cygpath -w "$2")"
    "$ADB" push "$src" /data/local/tmp/_mig_push.tmp >/dev/null
    "$ADB" shell "run-as $1 sh -c 'mkdir -p \$(dirname $3); cp /data/local/tmp/_mig_push.tmp $3; chmod 660 $3'" >/dev/null
    "$ADB" shell "rm -f /data/local/tmp/_mig_push.tmp"
}
# 列出 app 私有目录文件（不存在则返回空）
ls_private() { "$ADB" shell "run-as $1 ls $2" 2>/dev/null | tr -d '\r' || true; }

# ── 2. 从旧 app 拉取数据 ──────────────────────────────────────────────────
"$ADB" shell am force-stop "$OLD_PKG"
sleep 1

DB_FILES=()
for suf in "" "-wal" "-shm"; do
    if ls_private "$OLD_PKG" "databases/$DB_NAME$suf" | grep -q "$DB_NAME$suf"; then
        pull_file "$OLD_PKG" "databases/$DB_NAME$suf" "$WORK/$DB_NAME$suf"
        DB_FILES+=("$DB_NAME$suf")
    fi
done
[[ -s "$WORK/$DB_NAME" ]] || { echo "错误: 旧 app 没有可用的 $DB_NAME"; exit 1; }
echo "→ 已拉取旧数据库: ${DB_FILES[*]} ($(wc -c < "$WORK/$DB_NAME") 字节)"

PREF_FILES=()
if [[ $COPY_PREFS -eq 1 ]]; then
    while IFS= read -r f; do
        [[ -n "$f" ]] || continue
        pull_file "$OLD_PKG" "shared_prefs/$f" "$WORK/$f"
        PREF_FILES+=("$f")
    done < <(ls_private "$OLD_PKG" "shared_prefs" | grep '\.xml$')
    [[ ${#PREF_FILES[@]} -gt 0 ]] && echo "→ 已拉取偏好设置: ${PREF_FILES[*]}"
fi

# ── 3. 备份新 app 当前数据（覆盖前留后路）────────────────────────────────
BACKUP_DIR="$PROJECT_DIR/migration_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
for f in "${DB_FILES[@]}"; do
    if ls_private "$NEW_PKG" "databases/$f" | grep -q "$f"; then
        pull_file "$NEW_PKG" "databases/$f" "$BACKUP_DIR/$f"
    fi
done
echo "→ 新 app 原有数据已备份到: $BACKUP_DIR"

# ── 4. 确认 & 写入 ────────────────────────────────────────────────────────
if [[ $DRY_RUN -eq 1 ]]; then
    echo "→ [dry-run] 验证写入通路：往新 app databases/ 写探针文件（不动真实数据）"
    # 探针内容直接取自已拉取数据库的前 4KB，避免依赖 /dev/urandom 等平台差异
    head -c 4096 "$WORK/$DB_NAME" > "$WORK/_probe.bin"
    push_file "$NEW_PKG" "$WORK/_probe.bin" "databases/_migration_probe.bin"
    pull_file "$NEW_PKG" "databases/_migration_probe.bin" "$WORK/_probe_out.bin"
    "$ADB" shell "run-as $NEW_PKG rm -f databases/_migration_probe.bin"
    if cmp -s "$WORK/_probe.bin" "$WORK/_probe_out.bin"; then
        echo "→ [dry-run] 通路校验通过（探针哈希一致），真实数据未改动。去掉 --dry-run 执行正式迁移。"
    else
        echo "错误: [dry-run] 探针校验不一致，写入通路异常，中止"; exit 1
    fi
    exit 0
fi

if [[ $ASSUME_YES -ne 1 ]]; then
    if [[ ! -t 0 ]]; then
        echo "错误: 非交互环境请显式加 --yes（会覆盖新 app 现有数据）"; exit 1
    fi
    read -r -p "将用旧数据覆盖新 app 的 $DB_NAME 与偏好设置，输入 yes 继续: " ans
    [[ "$ans" == "yes" ]] || { echo "已取消"; exit 0; }
fi

"$ADB" shell am force-stop "$NEW_PKG"
sleep 1
for f in "${DB_FILES[@]}"; do
    push_file "$NEW_PKG" "$WORK/$f" "databases/$f"
done
if [[ $COPY_PREFS -eq 1 && ${#PREF_FILES[@]} -gt 0 ]]; then
    for f in "${PREF_FILES[@]}"; do
        push_file "$NEW_PKG" "$WORK/$f" "shared_prefs/$f"
    done
fi

echo "→ 迁移完成，新 app databases/ 现状:"
"$ADB" shell "run-as $NEW_PKG ls -la databases"
"$ADB" shell am start -n "$NEW_PKG/com.aifactory.appmessagecapture.MainActivity" >/dev/null 2>&1 || true
echo "→ 已尝试拉起新 app，首次打开会自动完成数据库版本升级（Room 迁移）。"
echo "→ 若要回滚：强停新 app，把 $BACKUP_DIR 里的文件按同名 push 回 databases/ 即可。"
