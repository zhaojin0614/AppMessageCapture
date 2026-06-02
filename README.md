# 应用消息捕获 (App Message Capture)

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="App Icon">
</p>

<p align="center">
  <b>一款简洁、美观、本地的通知消息捕获与管理工具</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-brightgreen" alt="Android 8.0+"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0-blue" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-2025.02-orange" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Room-2.6.1-blueviolet" alt="Room"/>
</p>

---

## 📱 功能特性

- **🔔 通知捕获** — 通过系统 `NotificationListenerService` 实时捕获所有应用通知
- **💾 本地存储** — 使用 Room 数据库存储，所有数据保存在本地，不上传云端
- **📊 消息管理** — 支持搜索、按日期分组、按应用筛选/屏蔽
- **📖 阅读状态** — 自动标记已读 + 一键已读 + 未读红点提示
- **📤 消息导出** — 支持导出为 JSON / CSV 格式，通过系统分享发送
- **🎨 精美UI** — 温暖橙色 + 现代紫色配色，大圆角卡片设计，应用图标显示
- **🛡️ 智能过滤** — 自动过滤系统幽灵通知、前台服务通知、媒体播放通知

## 🏗️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 2.0.21 | 编程语言 |
| Jetpack Compose | BOM 2025.02 | UI 框架 |
| Material Design 3 | — | 设计系统 |
| Room | 2.6.1 | 本地数据库 |
| Kotlin Coroutines / Flow | — | 异步与响应式 |
| KSP | 2.0.21-1.0.27 | 注解处理 |

## 📁 项目结构

```
app/src/main/java/com/aifactory/appmessagecapture/
├── data/                          # 数据层
│   ├── AppDatabase.kt             # Room 数据库
│   ├── NotificationDao.kt         # 数据访问对象
│   └── NotificationEntity.kt      # 消息实体
├── service/                       # 服务层
│   ├── MessageCaptureService.kt   # 通知监听服务（核心）
│   └── BootReceiver.kt            # 开机自启广播
├── ui/                            # UI 层
│   ├── MainScreen.kt              # 主界面（Compose）
│   ├── NotificationViewModel.kt   # 视图模型
│   └── theme/                     # 主题配色
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── utils/                         # 工具类
│   ├── PreferencesManager.kt      # 偏好设置
│   └── NotificationServiceHelper.kt # 服务状态检测
└── MainActivity.kt                # 入口 Activity
```

## 🚀 安装与运行

### 环境要求
- Android Studio Ladybug 或更新版本
- JDK 17+
- Android SDK 35
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
1. 安装后打开应用
2. 点击右上角 **⚙️ 设置** 图标，跳转到系统通知访问权限页面
3. 开启「应用消息捕获」的权限
4. 返回应用，即可开始捕获通知消息

## 🔐 隐私说明

- **纯本地存储**：所有消息仅保存在设备本地 Room 数据库中
- **无网络请求**：应用不包含任何网络权限，数据不会上传
- **开源透明**：代码完全开源，可自行审计

## 📋 运行清单

详见 [`运行清单.md`](运行清单.md)

## 📄 许可证

```
Copyright 2025 AiFactory

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
