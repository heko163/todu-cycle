package com.aiso.loopreminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aiso.loopreminder.data.AppDatabase
import com.aiso.loopreminder.data.shouldShowPersistent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Routes reminder broadcasts:
 *  - TRIGGER  : the scheduled occurrence fired -> show notification (+ escalate + persistent)
 *  - ESCALATE : a "still incomplete" nudge -> show identical notification, reschedule next nudge
 *  - COMPLETE : user marked done from the notification -> stop everything
 *  - SNOOZE   : user postponed -> re-nudge after the interval
 */
class ReminderReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(ReminderContract.EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        scope.launch {
            val db = AppDatabase.get(context)
            when (action) {
                ReminderContract.ACTION_TRIGGER -> handleTrigger(context, db, taskId)
                ReminderContract.ACTION_ESCALATE -> handleEscalate(context, db, taskId)
                ReminderContract.ACTION_COMPLETE -> handleComplete(context, db, taskId)
                ReminderContract.ACTION_SNOOZE -> handleSnooze(context, db, taskId)
            }
        }
    }

    private suspend fun handleTrigger(context: Context, db: AppDatabase, taskId: Long) {
        val task = db.taskDao().getById(taskId) ?: return
        if (!task.active) return
        val today = LocalDate.now().toString()
        if (task.escalateEnabled && task.lastCompletedDate != today) {
            // First nudge of the day uses the escalation-style notification (re-pops hourly).
            NotificationHelper.postEscalation(context, task, even = true)
            ReminderScheduler.scheduleEscalation(context, task)
        } else {
            NotificationHelper.postReminder(context, task)
        }
        if (task.shouldShowPersistent()) NotificationHelper.postPersistent(context, task)
        // Arm the next occurrence.
        ReminderScheduler.schedule(context, task)
    }

    private suspend fun handleEscalate(context: Context, db: AppDatabase, taskId: Long) {
        val task = db.taskDao().getById(taskId) ?: return
        if (!task.active) return
        val today = LocalDate.now().toString()
        if (task.lastCompletedDate == today) return
        // Alternate the notification id each step so it always re-alerts (see contract helper).
        val stepMs = (task.escalateIntervalMinutes * 60_000L).coerceAtLeast(60_000L)
        val even = (System.currentTimeMillis() / stepMs) % 2 == 0L
        NotificationHelper.postEscalation(context, task, even = even)
        if (task.escalateEnabled && task.lastCompletedDate != today) {
            ReminderScheduler.scheduleEscalation(context, task)
        }
    }

    private suspend fun handleComplete(context: Context, db: AppDatabase, taskId: Long) {
        val task = db.taskDao().getById(taskId) ?: return
        val today = LocalDate.now().toString()
        db.taskDao().markCompleted(taskId, today)
        // Stop TODAY's nudge + clear today's notifications, but keep the task recurring:
        // re-arm the next occurrence so it fires again tomorrow (and the hour loop restarts).
        ReminderScheduler.cancelEscalation(context, taskId)
        NotificationHelper.cancelReminder(context, taskId)
        NotificationHelper.cancelEscalation(context, taskId)
        NotificationHelper.cancelPersistent(context, taskId)
        ReminderScheduler.schedule(context, task.copy(lastCompletedDate = today))
    }

    private suspend fun handleSnooze(context: Context, db: AppDatabase, taskId: Long) {
        val task = db.taskDao().getById(taskId) ?: return
        if (!task.active) return
        val today = LocalDate.now().toString()
        if (task.lastCompletedDate == today) return
        ReminderScheduler.scheduleEscalation(context, task)
        if (task.shouldShowPersistent()) NotificationHelper.postPersistent(context, task)
    }
}
