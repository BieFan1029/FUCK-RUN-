# 🏃‍♂️ FuckRun - 步频模拟

[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![LSPosed](https://img.shields.io/badge/LSPosed-Required-red.svg)](https://github.com/LSPosed/LSPosed)

**FuckRun** 是一款基于 LSPosed 框架的开源 Android 传感器仿真模块，基于 [uy-li/runhook](https://github.com/uy-li/runhook) 二次开发。通过 Hook 系统底层传感器分发机制，模拟真实的步频数据流与运动加速度波形。

---

## ✨ 核心特性

- **🔓 通用模式**：移除硬编码包名白名单，任何在 LSPosed 中勾选的 App 都会被 Hook，理论上支持所有传感器计步类 App
- **🔄 高精度物理模拟**：内置正弦波算法模拟人体运动时的重心起伏，生成符合物理规律的加速度与计步器数据流
- **⚡ 深度底层适配**：针对 ColorOS / OnePlus 等系统底层传感器分发路径进行专项优化，支持 OplusSensorManager 静默事件注入
- **🎨 全新 UI**：淡蓝色 + 白色卡片 Material Design 风格，半透明悬浮窗控制开关
- **🛠️ 动态步频配置**：支持 140 - 220 SPM 范围内步频自定义，随机生成避免规律检测
- **🔑 Root 自动授权**：集成 KernelSU / Magisk 静默提权逻辑，自动配置悬浮窗权限

## 🛠️ 安装与使用

1. **环境要求**：
   - 已获取 Root 权限（推荐 KernelSU、Magisk）
   - 已安装 LSPosed 框架（推荐 v2.0.1(7639)）

2. **激活模块**：
   - 在 [Latest Release](https://github.com/BieFan1029/FUCK-RUN-/releases) 下载并安装 APK
   - 在 LSPosed 管理器中启用本模块
   - **作用域选择**：勾选你需要进行传感器仿真的目标 App（⚠️ 不要勾选系统组件）

3. **开始使用**：
   - 重启手机使模块生效
   - 打开 FuckRun App，设定步频范围
   - 打开目标 App → 开始跑步 → 点击悬浮窗开启模拟

## 📱 已测试兼容

| App | 包名 |
|-----|------|
| 闪动校园 | `com.huachenjie.shandong_school` |
| 闪动校园PRO | `com.huachenjie.shandong_school_pro` |
| 体适能 | `com.bxkj.student` |
| 运动世界校园 | `com.zjwh.android_wh_physicalfitness` |
| 宥马运动 | `android.youma.com` |

> 💡 由于采用通用模式，其他传感器计步类 App 同样可能兼容，只需在 LSPosed 中勾选即可。

## 🧪 工作原理

拦截 Android 传感器接口，伪造以下传感器数据：

| 传感器 | 类型 | 作用 |
|--------|------|------|
| 加速度传感器 | `TYPE_ACCELEROMETER` | 正弦波模拟跑步时的身体震动 |
| 计步器 | `TYPE_STEP_COUNTER` | 基于时间 × 步频伪造累计步数 |
| 步检测器 | `TYPE_STEP_DETECTOR` | 触发单步检测事件 |

关键实现：
- **万能接口 Hook**：Hook 系统级 `SensorManager.registerListener`，拦截所有传感器监听注册
- **正弦波仿真**：Z 轴振幅 3.5m/s²，Y 轴振幅 1.5m/s²，随机抖动 ±0.15m/s²
- **时间戳伪造**：`SystemClock.elapsedRealtimeNanos()` 确保数据时效性
- **动态步频**：在设定范围内随机生成固定步频，避免步数曲线过于规律

## 📋 更新日志

### v1.0.6
- **🔓 通用模式**：移除硬编码包名白名单，改为 LSPosed 作用域模式，理论上支持所有传感器计步 App
- **🎨 UI 全面重构**：淡蓝色 (#4A90D9) + 白色卡片 Material Design 风格
- **🪟 半透明悬浮窗**：圆角胶囊形半透明卡片，可拖动，点击切换状态
- **📖 新增使用说明页**：替代原有的硬编码适配列表，包含完整操作步骤与注意事项
- **🖼️ 全新图标**：运动鞋 + 脚印 + 速度线设计
- **✏️ 重命名**：FUCK-RUN → FuckRun（步频模拟）
- **🔧 Toast 提示优化**：移除"一加专版"字样，统一为 FuckRun 品牌

### v1.0.5
- 支持闪动校园、闪动校园PRO、体适能、运动世界校园、宥马运动
- 基于 uy-li/runhook 二次开发

## 🤝 致谢

- 原作者 [uy-li](https://github.com/uy-li/runhook) - RunHook 传感器仿真引擎

## ⚠️ 免责声明

本项目仅供 **Android 开发调试**、**自动化测试** 与 **逆向安全研究** 使用。
严禁将本工具用于任何破坏公平性、欺诈或非法商业用途。开发者不对用户使用本工具导致的任何账号封禁、数据异常或违规处罚负责。**一旦使用，即表示你同意此声明。**

## 📄 许可证

本项目基于 [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0) 开源。

---

**Developer:** [BieFan](https://github.com/BieFan1029)
