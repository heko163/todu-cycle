# 循环提醒 todu-cycle

> 一个只做「循环提醒」这一件事的 Android 待办应用：每天 / 每周 / 每月 / 每年重复，**没做完就一直提醒到你完成为止**。


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

## 📄 许可证

[MIT](LICENSE) © 2026 heko163
