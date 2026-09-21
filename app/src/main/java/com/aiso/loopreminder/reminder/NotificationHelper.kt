package com.aiso.loopreminder.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aiso.loopreminder.MainActivity
import com.aiso.loopreminder.R
import com.aiso.loopreminder.data.Task

/**
 * Builds and posts the two kinds of notifications used by the app:
 *  - "reminder"  : the standard alert (and identical escalation nudge)
 *  - "persistent": an ongoing notification pinned in the status bar until the task is completed
 */
object NotificationHelper {

    const val CHANNEL_REMINDER = "channel_reminder"
    const val CHANNEL_PERSISTENT = "channel_persistent"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_REMINDER) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDER,
                    context.getString(R.string.channel_reminder_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = context.getString(R.string.channel_reminder_desc) }
            )
        }
        if (mgr.getNotificationChannel(CHANNEL_PERSISTENT) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PERSISTENT,
                    context.getString(R.string.channel_persistent_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.channel_persistent_desc)
                    setShowBadge(false)
                }
            )
        }
    }

    private fun contentText(task: Task): String = "${task.recurrence.label} ${task.timeText} · 待完成"

    private fun openIntent(context: Context, task: Task): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("task_id", task.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            ReminderContract.triggerRc(task.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildReminder(context: Context, task: Task): Notification {
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderContract.ACTION_COMPLETE
            putExtra(ReminderContract.EXTRA_TASK_ID, task.id)
        }
        val completePi = PendingIntent.getBroadcast(
            context,
            ReminderContract.completeRc(task.id),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderContract.ACTION_SNOOZE
            putExtra(ReminderContract.EXTRA_TASK_ID, task.id)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            ReminderContract.snoozeRc(task.id),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(task.title)
            .setContentText(contentText(task))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent(context, task))
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "标记完成", completePi)
            .addAction(android.R.drawable.ic_menu_recent_history, "推迟", snoozePi)
            .build()
    }

    fun buildPersistent(context: Context, task: Task): Notification =
        NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(task.title)
            .setContentText("常驻提醒 · ${task.recurrence.label} ${task.timeText}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(openIntent(context, task))
            .setAutoCancel(false)
            .build()

    fun postReminder(context: Context, task: Task) {
        createChannels(context)
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.notify(ReminderContract.reminderId(task.id), buildReminder(context, task))
    }

    fun cancelReminder(context: Context, taskId: Long) {
        context.getSystemService(NotificationManager::class.java)?.cancel(ReminderContract.reminderId(taskId))
    }

    fun postPersistent(context: Context, task: Task) {
        createChannels(context)
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.notify(ReminderContract.persistentId(task.id), buildPersistent(context, task))
    }

    fun cancelPersistent(context: Context, taskId: Long) {
        context.getSystemService(NotificationManager::class.java)?.cancel(ReminderContract.persistentId(taskId))
    }

    /**
     * Posts one escalation ("still incomplete") nudge. Uses an alternating notification id
     * (the opposite one is cancelled first) so every cycle is a brand-new notification that
     * re-pops / re-vibrates — updating a single id would stay silent on many devices.
     */
    fun postEscalation(context: Context, task: Task, even: Boolean) {
        createChannels(context)
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.cancel(ReminderContract.escalationNotifyId(task.id, !even))
        mgr.notify(ReminderContract.escalationNotifyId(task.id, even), buildEscalation(context, task))
    }

    fun cancelEscalation(context: Context, taskId: Long) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        mgr.cancel(ReminderContract.escalationNotifyId(taskId, true))
        mgr.cancel(ReminderContract.escalationNotifyId(taskId, false))
    }

    private fun buildEscalation(context: Context, task: Task): Notification {
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderContract.ACTION_COMPLETE
            putExtra(ReminderContract.EXTRA_TASK_ID, task.id)
        }
        val completePi = PendingIntent.getBroadcast(
            context,
            ReminderContract.completeRc(task.id),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderContract.ACTION_SNOOZE
            putExtra(ReminderContract.EXTRA_TASK_ID, task.id)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context,
            ReminderContract.snoozeRc(task.id),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(task.title)
            .setContentText("还没完成 · ${task.recurrence.label} ${task.timeText} · 每 ${task.escalateIntervalMinutes} 分钟提醒")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent(context, task))
            .setOnlyAlertOnce(false)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "标记完成", completePi)
            .addAction(android.R.drawable.ic_menu_recent_history, "推迟", snoozePi)
            .build()
    }
}
