package com.aiso.loopreminder.reminder

/**
 * Shared constants and request-code helpers for the reminder broadcast / notification pipeline.
 *
 * Request codes are kept distinct per (task, action) so PendingIntents never collide.
 */
object ReminderContract {
    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_ACTION = "action"

    const val ACTION_TRIGGER = "trigger"     // the daily/weekly/... occurrence fired
    const val ACTION_ESCALATE = "escalate"   // repeated nudge while still incomplete
    const val ACTION_COMPLETE = "complete"   // user marked done from the notification
    const val ACTION_SNOOZE = "snooze"       // user snoozed from the notification

    private fun safe(taskId: Long) = (taskId and 0x7FFFFFFF).toInt()

    fun triggerRc(taskId: Long) = safe(taskId)
    fun escalateRc(taskId: Long) = safe(taskId) + 1_000_000
    fun completeRc(taskId: Long) = safe(taskId) + 2_000_000
    fun snoozeRc(taskId: Long) = safe(taskId) + 3_000_000

    /** Notification id for the per-task "reminder" notification. */
    fun reminderId(taskId: Long) = safe(taskId) + 5_000_000

    /** Notification id for the per-task "persistent / ongoing" notification. */
    fun persistentId(taskId: Long) = safe(taskId) + 4_000_000

    /**
     * Two alternating notification ids for the escalation ("still incomplete") loop.
     * We ping the opposite id each cycle so Android always treats it as a NEW notification
     * and re-shows / re-vibrates it (updating a single notification id would stay silent).
     */
    fun escalationNotifyId(taskId: Long, even: Boolean) = safe(taskId) + if (even) 6_000_000 else 7_000_000
}
