package com.aiso.loopreminder.ui.util

import com.aiso.loopreminder.data.RecurrenceType
import com.aiso.loopreminder.data.Task
import com.aiso.loopreminder.data.firesOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

val Task.isCompletedToday: Boolean
    get() = lastCompletedDate == LocalDate.now().toString()

/** True when this task's occurrence for `now` is in the past and not yet completed. */
fun Task.isOverdue(
    now: LocalDate = LocalDate.now(),
    nowMinute: Int = LocalTime.now().toSecondOfDay() / 60
): Boolean {
    if (isCompletedToday) return false
    return firesOn(now) && timeMinuteOfDay < nowMinute
}

private val WEEKDAY_CN = mapOf(
    DayOfWeek.MONDAY to "一", DayOfWeek.TUESDAY to "二", DayOfWeek.WEDNESDAY to "三",
    DayOfWeek.THURSDAY to "四", DayOfWeek.FRIDAY to "五", DayOfWeek.SATURDAY to "六", DayOfWeek.SUNDAY to "日"
)

/** Human-readable recurrence rule, e.g. "每月 15 日", "每年 3 月 1 日", "每周 一 三 五". */
fun Task.recurrenceSummary(): String = when (recurrence) {
    RecurrenceType.DAILY -> "每天"
    RecurrenceType.WEEKLY ->
        if (repeatDays.isEmpty()) "每周"
        else "每周 " + repeatDays.sortedBy { it.value }.joinToString(" ") { WEEKDAY_CN[it] ?: it.name }
    RecurrenceType.MONTHLY -> "每月 ${monthDay} 日"
    RecurrenceType.YEARLY -> "每年 ${yearMonth} 月 ${yearDay} 日"
}
