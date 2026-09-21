package com.aiso.loopreminder

import android.app.Application
import com.aiso.loopreminder.data.AppDatabase
import com.aiso.loopreminder.data.Seed
import com.aiso.loopreminder.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LoopReminderApp : Application() {

    val database by lazy { AppDatabase.get(this) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            Seed.seedIfEmpty(database)
            // Re-arm alarms + persistent notifications after a fresh start / update.
            ReminderScheduler.rescheduleAll(this@LoopReminderApp, database.taskDao())
        }
    }
}
