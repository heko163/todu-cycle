package com.aiso.loopreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val recurrence: RecurrenceType = RecurrenceType.DAILY,
    val timeMinuteOfDay: Int = 10 * 60,            // minutes from midnight
    val repeatDays: Set<DayOfWeek> = emptySet(),   // for WEEKLY
    val monthDay: Int = 1,                          // 每月几日 (1-31)，用于 MONTHLY
    val yearMonth: Int = 1,                         // 每年几月 (1-12)，用于 YEARLY
    val yearDay: Int = 1,                           // 每年几日 (1-31)，用于 YEARLY
    val advanceNoticeMinutes: Int = 0,             // 0 = no advance notice
    val reminderMode: String = "通知 + 声音",
    val escalateEnabled: Boolean = false,          // 未完成时持续提醒
    val escalateIntervalMinutes: Int = 60,         // 每 N 分钟重复
    val persistentNotification: Boolean = false,   // 通知栏常驻
    val notes: String = "",
    val categoryId: Long = 0,                    // 0 = 未分类
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastCompletedDate: String? = null          // yyyy-MM-dd
) {
    val timeText: String
        get() {
            val h = timeMinuteOfDay / 60
            val m = timeMinuteOfDay % 60
            return "%02d:%02d".format(h, m)
        }
}
