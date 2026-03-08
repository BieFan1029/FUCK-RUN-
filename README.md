# 🏃‍♂️ RunHook (Sensor Simulator Engine)

[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![LSPosed](https://img.shields.io/badge/LSPosed-Required-red.svg)](https://github.com/LSPosed/LSPosed)

**RunHook** 是一款基于 LSPosed 框架的开源 Android 传感器仿真插件，专为 **Android 开发者调试与压力测试** 设计。它通过 Hook 系统底层传感器分发机制，模拟真实的步频数据流与运动加速度波形。

---

## ✨ 核心特性

- **🎨 MD3 Expressive 设计**：采用最新的 Material Design 3 Expressive 风格界面，支持动态色彩与流体交互。
- **🧪 开发者调试工具**：为需要传感器数据的 App 开发者提供稳定的模拟环境，支持 **140 - 220 SPM** 范围内的步频数据仿真。
- **🔄 高精度物理模拟**：内置**正弦波算法**模拟人体运动时的重心起伏，生成符合物理规律的加速度（Accelerometer）与计步器（Step Counter）数据流。
- **⚡ 深度底层适配**：针对黑厂系统底层传感器分发路径进行专项优化，支持静默事件注入。
- **🔓 Root 自动授权**：集成 SukiSU/KernelSU 静默提权逻辑，自动配置 `SYSTEM_ALERT_WINDOW` 权限，简化开发调试流程。


## 🛠️ 安装与使用

1. **环境要求**：
   - 已获取 Root 权限（推荐使用 KernelSU、SukiSU 或 Magisk）。
   - 已安装 LSPosed 管理器。
2. **激活模块**：
   - 在 [Latest Release](https://github.com/uy-li/runhook/releases) 下载并安装 APK。
   - 在 LSPosed 管理器中启用本模块。
   - **作用域选择**：请根据你的开发测试需求，勾选需要进行传感器仿真的目标应用及 `系统框架`。
3. **开始测试**：
   - 重启手机以确保系统框架 Hook 生效。
   - 打开 RunHook App，设定测试所需的步频范围，开启仿真开关。
   - 进入目标应用查看传感器数据反馈。

## ⚠️ 免责声明

本项目仅供 **Android 开发调试**、**自动化测试** 与 **逆向安全研究** 使用。
严禁将本工具用于任何破坏公平性、欺诈或非法商业用途。开发者不对用户使用本工具导致的任何账号封禁、数据异常或违规处罚负责。**一旦使用，即表示你同意此声明。**

## 🤝 贡献与反馈

欢迎提交 Issue 或 Pull Request 来完善这个项目。如果你觉得这个调试工具对你有帮助，请给个 **Star** 🌟！

---
**Developer:** [uy-li](https://github.com/uy-li)
