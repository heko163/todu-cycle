# 循环提醒 LoopReminder

> 一个只做「循环提醒」这一件事的 Android 待办应用：每天 / 每周 / 每月 / 每年重复，**没做完就一直提醒到你完成为止**。

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose%20BOM-2024.06.00-4285F4?logo=jetpackcompose)
![API](https://img.shields.io/badge/API-26%2B-3DDC84?logo=android)
![License](https://img.shields.io/badge/License-MIT-yellow)

**English:** LoopReminder is a small Android app for *recurring* reminders — daily, weekly, monthly or yearly. Its defining behaviour: if you don't mark a task done, it keeps re-notifying you (same message, no escalation, no limit) until you do.

---

## ✨ 特性

**循环规则**

- 每天 / 每周 / 每月 / 每年 四种循环
- 每周可指定具体星期几（可多选）
- 每月可选 1–31 日；当月没有这一天时（比如 2 月的 31 日）自动落在该月最后一天
- 每年可指定「几月几日」，日期随该月天数自动收敛

**提醒行为**

- 到点推送通知，可设置「提前 N 分钟」提醒
- **未完成时持续提醒**：当天没标记完成，就按设定间隔（如每 60 分钟）重复推送 —— 内容与第一次完全一致、不加码、没有次数上限，直到你标记完成
- **通知栏常驻**：当天该做的、以及逾期未完成的待办，以常驻通知形式留在通知栏，标记完成后自动移除
- 手机重启 / 应用更新后自动重排所有提醒

**组织与查看**

- 自定义分类（自己起名、挑颜色），分类 → 待办两级浏览
- 「今日待办」只显示今天该做的，并把之前没做完的逾期待办一并带上、排在前面
- 日历月视图，可切换月份，点任意日期查看当天待办
- 待办支持新建、编辑、删除、标记完成

首次启动会写入几个示例分类和示例任务，方便直接看效果。

---

## 🛠 技术栈

| | |
|---|---|
| 语言 | Kotlin 1.9.24 |
| UI | Jetpack Compose（Material 3，BOM 2024.06.00） |
| 构建 | AGP 8.5.2 / Gradle 8.9 / JDK 17 |
| 数据 | Room 2.6.1（数据库当前 version 3） |
| 调度 | AlarmManager（精确闹钟）+ BroadcastReceiver |
| 其他 | Navigation Compose、WorkManager、DataStore |

- 包名 `com.aiso.loopreminder`
- `minSdk 26`（Android 8.0）/ `targetSdk 34`（Android 14）

---

## 🚀 快速开始

**环境**：JDK 17 + Android SDK（compileSdk 34）。

```bash
git clone <你的仓库地址>
cd <仓库目录>

./gradlew assembleDebug          # macOS / Linux
gradlew.bat assembleDebug        # Windows
```

APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`。

也可以直接用 **Android Studio（Hedgehog 或更新）** 打开仓库根目录，点 ▶ Run。

> **国内网络**：如果 `google()` / `mavenCentral()` 拉依赖很慢，打开 `settings.gradle.kts`，按文件里的注释把仓库换成腾讯镜像即可。

---

## 🔐 权限说明

| 权限 | 用途 | 备注 |
|------|------|------|
| `POST_NOTIFICATIONS` | 弹出提醒通知 | Android 13+ 首次启动会弹系统授权框 |
| `SCHEDULE_EXACT_ALARM` | 精确到点的循环闹钟 | Android 12+；部分 ROM 需在系统设置 → 应用 → 闹钟与提醒里手动打开 |
| `USE_EXACT_ALARM` | 同上（Android 13+ 引入） | 未授予时系统会降级为不精确闹钟 |
| `RECEIVE_BOOT_COMPLETED` | 重启后重新排程 | 静默处理，无界面 |

---

## 📁 项目结构

```
app/src/main/java/com/aiso/loopreminder/
├── LoopReminderApp.kt        # Application：首启播种示例数据 + 重排提醒
├── data/
│   ├── Task.kt               # 任务实体（循环类型、时间、每月/每年日期、分类、最近完成日）
│   ├── Category.kt           # 自定义分类
│   ├── TaskSchedule.kt       # 循环判定核心：firesOn / previousOccurrence / 逾期 / 是否常驻
│   ├── AppDatabase.kt        # Room 数据库 + 迁移
│   ├── TaskDao.kt / CategoryDao.kt / Converters.kt / Seed.kt
│   └── RecurrenceType.kt
├── reminder/
│   ├── ReminderScheduler.kt  # 计算下次触发时间 + 排精确闹钟 + 升级重排
│   ├── ReminderReceiver.kt   # 触发 / 升级 / 完成 / 推迟 分发
│   ├── NotificationHelper.kt # 通知渠道、普通通知、常驻通知
│   ├── ReminderContract.kt   # 通知 ID 与 Intent 约定
│   └── BootReceiver.kt       # 开机 / 应用更新后重排
└── ui/
    ├── MainActivity.kt       # 导航宿主
    ├── screens/              # 今日待办 / 分类 / 分类详情 / 日历 / 新建·编辑提醒 / 任务详情
    ├── viewmodel/TaskViewModel.kt
    ├── components/Components.kt
    ├── navigation/BottomBar.kt
    ├── theme/                # 配色与主题
    └── util/TaskExt.kt       # 循环规则的可读文案等
```

---

## ⚙️ 提醒引擎是怎么工作的

1. **排程**：`ReminderScheduler.computeNextTrigger` 按循环规则算出下一次触发时间，用 `AlarmManager.setExactAndAllowWhileIdle` 排一个精确闹钟；每次触发后再排下一个周期。
2. **未完成持续提醒**：触发后如果当天还没标记完成、且开了这个开关，就按 `escalateIntervalMinutes` 再排一次提醒。**内容与第一次完全一致、不加码、没有次数上限**，直到标记完成。
3. **通知栏常驻**：用一条 `setOngoing(true)` 的常驻通知实现（不是前台服务），进程被杀也能留住；标记完成或当天不再需要时移除。
4. **月末兜底**：每月循环选了 31 日，遇到只有 30 天或 28/29 天的月份，会自动落在该月最后一天，不会漏掉。
5. **重启恢复**：`BootReceiver` 监听开机与应用更新，重新排所有提醒。

---

## 🗄 数据模型

- Room 数据库当前 **version 3**。
- `v1 → v2` 是破坏性迁移（会清空数据），`v2 → v3` 为**非破坏性**迁移（新增分类表、任务增加分类字段），升级不丢数据。
- 任务只记录 `lastCompletedDate`（最近一次完成日），没有完整的打卡历史表 —— 见下方已知限制。

---

## ⚠️ 已知限制

- **没有完整打卡历史**：只存了最近一次完成日，做不出真实的「最近 N 天打卡」记录。需要的话建议新增一张 completions 表。
- **没有设置页**：时区、免打扰时段、数据导入导出都还没做。
- **提醒音 / 震动**由系统通知渠道控制，应用内没有单独配置。
- **字体**沿用系统默认，没有内置设计稿里的 Noto Sans SC / Inter Tight。
- **没有测试**：目前没有任何单元测试或 UI 测试。
- **Room schema 导出不完整**：`app/schemas` 下只有 `1.json` / `2.json`，version 3 的 schema 没有导出。

---

## 🤝 贡献

欢迎 Issue 和 PR。改动较大的功能建议先开 Issue 聊一下方案。

提交前请确认 `./gradlew assembleDebug` 能构建通过，代码风格与现有文件保持一致。

---

## 📄 许可证

[MIT](LICENSE) © 2026 heko163
