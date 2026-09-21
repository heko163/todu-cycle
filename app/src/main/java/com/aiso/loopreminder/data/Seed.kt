package com.aiso.loopreminder.data

import java.time.DayOfWeek
import java.time.LocalDate

/** Inserts a few demo tasks + categories on first launch so the screens are not empty. */
object Seed {
    suspend fun seedIfEmpty(db: AppDatabase) {
        val dao = db.taskDao()
        if (dao.countActive() > 0) return

        val catDao = db.categoryDao()
        val workId = catDao.insert(Category(name = "工作", color = 0xFF014DB2.toInt()))
        val lifeId = catDao.insert(Category(name = "生活", color = 0xFF10B981.toInt()))
        val healthId = catDao.insert(Category(name = "健康", color = 0xFFF59E0B.toInt()))

        val today = LocalDate.now().toString()
        val samples = listOf(
            Task(
                title = "吃降压药",
                recurrence = RecurrenceType.DAILY,
                timeMinuteOfDay = 10 * 60,
                escalateEnabled = true,
                escalateIntervalMinutes = 60,
                persistentNotification = true,
                categoryId = healthId,
                notes = "饭后服用，温水送服。忘记可按持续提醒补提醒。"
            ),
            Task(
                title = "喝水 8 杯",
                recurrence = RecurrenceType.DAILY,
                timeMinuteOfDay = 9 * 60,
                lastCompletedDate = today,
                categoryId = healthId
            ),
            Task(
                title = "午休散步",
                recurrence = RecurrenceType.WEEKLY,
                timeMinuteOfDay = 13 * 60,
                repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                categoryId = workId
            ),
            Task(
                title = "周报提交",
                recurrence = RecurrenceType.WEEKLY,
                timeMinuteOfDay = 18 * 60,
                repeatDays = setOf(DayOfWeek.FRIDAY),
                lastCompletedDate = today,
                categoryId = workId
            ),
            Task(
                title = "交房租",
                recurrence = RecurrenceType.MONTHLY,
                timeMinuteOfDay = 9 * 60,
                monthDay = 1,
                categoryId = lifeId
            ),
            Task(
                title = "年度体检",
                recurrence = RecurrenceType.YEARLY,
                timeMinuteOfDay = 8 * 60,
                yearMonth = 1,
                yearDay = 1,
                categoryId = healthId
            )
        )
        samples.forEach { dao.insert(it) }
    }
}
