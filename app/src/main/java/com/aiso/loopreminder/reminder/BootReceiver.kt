package com.aiso.loopreminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aiso.loopreminder.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-arms all active reminders after a device reboot or app update,
 * so recurring alarms and persistent notifications survive.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val db = AppDatabase.get(context)
            scope.launch { ReminderScheduler.rescheduleAll(context, db.taskDao()) }
        }
    }
}
