package com.aiso.loopreminder.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.aiso.loopreminder.data.AppDatabase
import com.aiso.loopreminder.data.RecurrenceType
import com.aiso.loopreminder.data.effectiveDay
import com.aiso.loopreminder.data.shouldShowPersistent
import com.aiso.loopreminder.data.Task
import com.aiso.loopreminder.data.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules / cancels the alarms that drive recurring reminders and the escalation loop.
 */
object ReminderScheduler {

    private fun alarm(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun triggerIntent(context: Context, taskId: Long) = Intent(context, ReminderReceiver::class.java).apply {
        action = ReminderContract.ACTION_TRIGGER
        putExtra(ReminderContract.EXTRA_TASK_ID, taskId)
    }

    private fun escalateIntent(context: Context, taskId: Long) = Intent(context, ReminderReceiver::class.java).apply {
        action = ReminderContract.ACTION_ESCALATE
        putExtra(ReminderContract.EXTRA_TASK_ID, taskId)
    }

    private fun triggerPi(context: Context, task: Task) = PendingIntent.getBroadcast(
        context, ReminderContract.triggerRc(task.id), triggerIntent(context, task.id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun escalatePi(context: Context, task: Task) = PendingIntent.getBroadcast(
        context, ReminderContract.escalateRc(task.id), escalateIntent(context, task.id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /** True when the app is allowed to use exact alarms (Android 12+). */
    fun canScheduleExact(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarm(context).canScheduleExactAlarms()
        } else true
    }

    private fun setExact(context: Context, triggerAt: Long, pi: PendingIntent) {
        val am = alarm(context)
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /**
     * Next occurrence epoch millis, already shifted earlier by [Task.advanceNoticeMinutes].
     * Returns null only for WEEKLY with no selected days (won't schedule).
     */
    fun computeNextTrigger(task: Task, now: ZonedDateTime = ZonedDateTime.now()): Long? {
        val zone = now.zone
        val localTime = LocalTime.of(task.timeMinuteOfDay / 60, task.timeMinuteOfDay % 60)
        val notifyAt = localTime.minusMinutes(task.advanceNoticeMinutes.toLong())
        fun at(date: LocalDate) = ZonedDateTime.of(date, notifyAt, zone)

        return when (task.recurrence) {
            RecurrenceType.DAILY -> {
                var d = now.toLocalDate()
                var zdt = at(d)
                if (zdt.isBefore(now)) {
                    d = d.plusDays(1)
                    zdt = at(d)
                }
                zdt.toInstant().toEpochMilli()
            }

            RecurrenceType.WEEKLY -> generateSequence(now.toLocalDate()) { it.plusDays(1) }
                .take(8)
                .firstOrNull { d -> task.repeatDays.isNotEmpty() && task.repeatDays.contains(d.dayOfWeek) && !at(d).isBefore(now) }
                ?.let { at(it).toInstant().toEpochMilli() }

            RecurrenceType.MONTHLY -> {
                generateSequence(now.toLocalDate()) { it.plusDays(1) }
                    .take(400)
                    .firstOrNull { d ->
                        d.dayOfMonth == effectiveDay(d.year, d.monthValue, task.monthDay) && !at(d).isBefore(now)
                    }
                    ?.let { at(it).toInstant().toEpochMilli() }
            }

            RecurrenceType.YEARLY -> {
                generateSequence(now.toLocalDate()) { it.plusDays(1) }
                    .take(400)
                    .firstOrNull { d ->
                        d.monthValue == task.yearMonth &&
                            d.dayOfMonth == effectiveDay(d.year, task.yearMonth, task.yearDay) &&
                            !at(d).isBefore(now)
                    }
                    ?.let { at(it).toInstant().toEpochMilli() }
            }
        }
    }

    /** Schedule the next occurrence (+ persistent notification if enabled). */
    fun schedule(context: Context, task: Task) {
        NotificationHelper.createChannels(context)
        val trigger = computeNextTrigger(task) ?: return
        setExact(context, trigger, triggerPi(context, task))
        if (task.shouldShowPersistent()) NotificationHelper.postPersistent(context, task)
    }

    /** Schedule the next escalation nudge (the "未完成时持续提醒" loop). */
    fun scheduleEscalation(context: Context, task: Task) {
        val next = System.currentTimeMillis() + task.escalateIntervalMinutes * 60_000L
        setExact(context, next, escalatePi(context, task))
    }

    /** Cancel everything tied to a task (occurrence + escalations + notifications). */
    fun cancel(context: Context, taskId: Long) {
        val am = alarm(context)
        val dummy = Task(id = taskId, title = "") // only the id is used, for request codes
        am.cancel(triggerPi(context, dummy))
        am.cancel(escalatePi(context, dummy))
        NotificationHelper.cancelReminder(context, taskId)
        NotificationHelper.cancelEscalation(context, taskId)
        NotificationHelper.cancelPersistent(context, taskId)
    }

    /** Cancel only the escalation (nudge) alarm, leaving the next occurrence intact. */
    fun cancelEscalation(context: Context, taskId: Long) {
        val am = alarm(context)
        val dummy = Task(id = taskId, title = "")
        am.cancel(escalatePi(context, dummy))
    }

    /** Re-arm every active task. Called on app start and after reboot. */
    suspend fun rescheduleAll(context: Context, dao: TaskDao) = withContext(Dispatchers.IO) {
        NotificationHelper.createChannels(context)
        dao.getActive().forEach { task ->
            schedule(context, task)
            // Drop any stale pinned notification (e.g. from an older build) that no longer qualifies.
            if (!task.shouldShowPersistent()) NotificationHelper.cancelPersistent(context, task.id)
        }
    }
}
