package com.aiso.loopreminder.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Clamp a desired day-of-month to the actual last day of [year]-[month] (e.g. 31 -> 28/29/30). */
fun effectiveDay(year: Int, month: Int, day: Int): Int =
    minOf(day, YearMonth.of(year, month).lengthOfMonth())

/** Whether this task fires (has a scheduled occurrence) on the given [date]. */
fun Task.firesOn(date: LocalDate): Boolean = when (recurrence) {
    RecurrenceType.DAILY -> true
    RecurrenceType.WEEKLY -> repeatDays.contains(date.dayOfWeek)
    RecurrenceType.MONTHLY -> date.dayOfMonth == effectiveDay(date.year, date.monthValue, monthDay)
    RecurrenceType.YEARLY ->
        date.monthValue == yearMonth && date.dayOfMonth == effectiveDay(date.year, yearMonth, yearDay)
}

/**
 * Most recent occurrence day on or before [from] that is not before the task was created.
 * Returns null when the task has no past occurrence (e.g. a future-only weekly task).
 */
fun Task.previousOccurrence(from: LocalDate = LocalDate.now()): LocalDate? {
    val created = LocalDate.ofEpochDay(createdAt / 86400000)
    var d = from
    repeat(400) {
        if (d.isBefore(created)) return null
        if (firesOn(d)) return d
        d = d.minusDays(1)
    }
    return null
}

/**
 * Whether this task should currently hold a pinned (ongoing) notification in the status bar.
 *
 * Pinned notifications are only for "today's tasks" and "incomplete overdue reminders" —
 * NOT for future reminders whose next occurrence is still ahead.
 */
fun Task.shouldShowPersistent(today: LocalDate = LocalDate.now()): Boolean {
    if (!persistentNotification) return false
    if (isOverdueUnfinished(today)) return true                // a past occurrence was missed & never completed
    // Otherwise: only "today's task" qualifies, and only if not already done today.
    return firesOn(today) && lastCompletedDate != today.toString()
}

/**
 * Whether this task still needs attention "today" beyond just firing today:
 * it has a past occurrence (before [today]) that was never completed.
 * Used by the "今日待办" list so overdue items (e.g. a Monday task left undone
 * and viewed on Wednesday) are not silently dropped.
 */
fun Task.isOverdueUnfinished(today: LocalDate = LocalDate.now()): Boolean {
    val prev = previousOccurrence(today) ?: return false
    if (!prev.isBefore(today)) return false
    val last = lastCompletedDate ?: return true
    if (last.isEmpty()) return true
    val lastDate = runCatching { LocalDate.parse(last) }.getOrNull() ?: return true
    return prev.isAfter(lastDate)
}
