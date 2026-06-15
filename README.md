# 应用消息捕获 (App Message Capture)


<p align="center">
  <b>一款集通知捕获、智能记账、生日管理于一体的本地化效率工具</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-brightgreen" alt="Android 8.0+"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-blue" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.02-orange" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Room-2.6.1-blueviolet" alt="Room"/>
</p>

---

## 📱 功能特性

### 🔔 通知消息捕获
- 通过系统 `NotificationListenerService` 实时捕获所有应用通知
- 支持搜索、按日期分组、按应用筛选/屏蔽
- 自动标记已读 + 一键已读 + 未读红点提示
- 支持导出为 JSON / CSV 格式，通过系统分享发送
- 智能过滤系统幽灵通知、前台服务通知、媒体播放通知

### 💰 智能记账
- **自动记账** — 监听支付类应用通知，自动识别金额并分类入账
- **手动记账** — 支持选择日期（补记/预记），当天自动记录当前时间
- **分类管理** — 12 种支出分类 + 8 种收入分类，每项配有独立图标和颜色
- **分类迁移** — 旧版分类名自动映射到新版，支持一键批量迁移
- **周期账单** — 房租、会员等固定支出设为周期性自动记账（每日/每周/双周/每月/每年）
- **收支报表** — 按周/月/年查看收支趋势，环形图展示分类构成，支持分类详情下钻

### 🎂 生日管理
- **生日记录** — 支持阳历/农历生日，自动计算距今天数和下次生日倒计时
- **提醒通知** — 生日当天及提前 N 天自动推送提醒（AlarmManager 精确调度）
- **桌面小组件** — Glance AppWidget 展示近期生日，支持点击跳转
- **导入导出** — 支持 JSON 格式批量备份与恢复，导入后界面即时刷新
- **分页加载** — 列表支持分页滚动加载，大量数据流畅展示

### 🎨 通用特性
- **纯本地存储** — Room 数据库 + SharedPreferences，所有数据留在设备
- **精美 UI** — Jetpack Compose + Material Design 3，温暖橙色 + 现代紫色配色
- **三栏 Tab** — 消息 / 记账 / 生日，底部导航栏快速切换
- **Edge-to-Edge** — 沉浸式状态栏和导航栏适配

## 🏗️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.0.21 | 编程语言 |
| Jetpack Compose | BOM 2025.02.00 | UI 框架 |
| Material Design 3 | — | 设计系统 |
| Room | 2.6.1 | 本地数据库（v11，含完整迁移链） |
| WorkManager | 2.9.1 | 周期任务调度（周期账单执行） |
| Glance | 1.1.1 | 桌面小组件 |
| Gson | 2.11.0 | JSON 序列化（导入导出） |
| Timber | 5.0.1 | 日志框架 |
| lunar | 1.7.7 | 农历日期计算 |
| KSP | 2.0.21-1.0.27 | 注解处理 |

## 📁 项目结构

```
app/src/main/java/com/aifactory/appmessagecapture/
├── data/                              # 通用数据层
│   ├── AppDatabase.kt                 # Room 数据库（v11）
│   ├── BillEntity.kt                  # 账单实体
│   ├── BillDao.kt                     # 账单 DAO
│   ├── RecurringBillEntity.kt         # 周期账单实体
│   ├── RecurringBillDao.kt            # 周期账单 DAO
│   ├── NotificationEntity.kt          # 通知消息实体
│   ├── NotificationDao.kt             # 通知 DAO
│   └── Categories.kt                  # 分类常量 + 迁移映射
├── service/                           # 服务层
│   ├── MessageCaptureService.kt       # 通知监听 + 自动记账
│   └── BootReceiver.kt                # 开机自启
├── worker/                            # 后台任务
│   └── RecurringBillWorker.kt         # 周期账单执行 Worker
├── ui/                                # 记账 UI 层
│   ├── BillScreen.kt                  # 账单主界面（列表 + 手动记账 + 日期选择）
│   ├── BillViewModel.kt               # 账单 ViewModel
│   ├── RecurringBillScreen.kt         # 周期账单界面
│   ├── RecurringBillViewModel.kt      # 周期账单 ViewModel
│   ├── CategoryMigrationScreen.kt     # 分类迁移界面
│   ├── Categories.kt                  # 分类颜色/图标映射
│   ├── report/                        # 收支报表
│   │   ├── ReportScreen.kt            # 报表主界面（趋势图 + 分类构成）
│   │   ├── ReportViewModel.kt         # 报表 ViewModel
│   │   ├── CategoryDetailScreen.kt    # 分类详情下钻
│   │   └── CategoryDetailViewModel.kt
│   ├── setup/                         # 引导页
│   └── theme/                         # 主题配色
├── birthday/                          # 生日管理模块
│   ├── data/
│   │   ├── BirthdayEntity.kt          # 生日实体
│   │   └── BirthdayDao.kt             # 生日 DAO
│   ├── logic/
│   │   ├── DateCalculator.kt          # 日期/倒计时计算
│   │   └── LunarCalendarAdapter.kt    # 农历适配
│   ├── service/
│   │   ├── BirthdayAlarmScheduler.kt  # 闹钟调度
│   │   ├── BirthdayAlarmReceiver.kt   # 闹钟广播接收
│   │   └── BirthdayBootReceiver.kt    # 开机重设闹钟
│   ├── ui/
│   │   ├── BirthdayScreen.kt          # 生日导航入口
│   │   ├── BirthdayListScreen.kt      # 生日列表
│   │   ├── BirthdayEditScreen.kt      # 新建/编辑生日
│   │   ├── BirthdayViewModel.kt       # 生日 ViewModel
│   │   ├── AlarmActivity.kt           # 闹钟弹窗
│   │   └── components/
│   │       └── BirthdayCard.kt        # 生日卡片组件
│   ├── utils/
│   │   ├── BackupManager.kt           # 导入导出
│   │   └── PermissionHelper.kt        # 权限辅助
│   └── widget/
│       ├── BirthdayWidget.kt          # Glance 小组件
│       ├── BirthdayWidgetReceiver.kt  # 小组件接收器
│       ├── BirthdayWidgetActivity.kt  # 小组件配置
│       └── BirthdayWidgetWorker.kt    # 小组件定时刷新
├── utils/                             # 工具类
│   ├── PreferencesManager.kt          # 偏好设置
│   └── NotificationServiceHelper.kt   # 服务状态检测
└── MainActivity.kt                    # 入口 Activity（三栏 Tab 导航）
```

## 🚀 安装与运行

### 环境要求
- Android Studio Ladybug 或更新版本
- JDK 17+
- Android SDK 35（compileSdk）
- 最低运行系统：Android 8.0 (API 26)

### 本地编译
```bash
# 1. 克隆项目
git clone <你的仓库地址>

# 2. 用 Android Studio 打开项目
# File -> Open -> 选择 AppMessageCapture 目录

# 3. 同步 Gradle
# 点击 "Sync Project with Gradle Files"

# 4. 编译安装
# 连接手机或启动模拟器，点击 Run
```

### 首次使用
1. 安装后打开应用，默认进入「消息」Tab
2. 点击右上角 **⚙️ 设置** 图标，开启通知访问权限
3. 返回应用，即可开始捕获通知消息
4. 切换到「记账」Tab 开始记录收支
5. 切换到「生日」Tab 管理生日提醒

## 🔐 隐私说明

- **纯本地存储**：所有数据仅保存在设备本地 Room 数据库中
- **无网络请求**：应用不包含任何网络权限，数据不会上传
- **开源透明**：代码完全开源，可自行审计

## 📋 运行清单

详见 [`运行清单.md`](运行清单.md)

## 📄 许可证

```
Copyright 2025-2026 AiFactory

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---
