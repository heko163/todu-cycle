package com.aiso.loopreminder.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiso.loopreminder.data.AppDatabase
import com.aiso.loopreminder.data.Category
import com.aiso.loopreminder.data.Task
import com.aiso.loopreminder.data.shouldShowPersistent
import com.aiso.loopreminder.reminder.NotificationHelper
import com.aiso.loopreminder.reminder.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).taskDao()
    private val catDao = AppDatabase.get(application).categoryDao()

    val tasks: StateFlow<List<Task>> =
        dao.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> =
        catDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedToday: StateFlow<Int> =
        dao.observeCompletedTodayCount(today())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun today(): String = LocalDate.now().toString()

    fun addTask(task: Task) {
        viewModelScope.launch {
            val id = dao.insert(task)
            ReminderScheduler.schedule(getApplication(), task.copy(id = id))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.update(task)
            ReminderScheduler.cancel(getApplication(), task.id)
            ReminderScheduler.schedule(getApplication(), task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.delete(task)
            ReminderScheduler.cancel(getApplication(), task.id)
        }
    }

    fun setCompleted(task: Task, done: Boolean) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            if (done) {
                val todayStr = today()
                dao.markCompleted(task.id, todayStr)
                // Stop today's nudge + clear today's notifications, but keep the task recurring:
                // re-arm the next occurrence so it fires again tomorrow.
                ReminderScheduler.cancelEscalation(ctx, task.id)
                NotificationHelper.cancelReminder(ctx, task.id)
                NotificationHelper.cancelEscalation(ctx, task.id)
                NotificationHelper.cancelPersistent(ctx, task.id)
                ReminderScheduler.schedule(ctx, task.copy(lastCompletedDate = todayStr))
            } else {
                dao.unmarkCompleted(task.id)
                ReminderScheduler.schedule(ctx, task.copy(lastCompletedDate = null))
                if (task.shouldShowPersistent()) NotificationHelper.postPersistent(ctx, task.copy(lastCompletedDate = null))
            }
        }
    }

    /** Re-nudge after "推迟" from the notification center / detail screen. */
    fun snooze(task: Task) {
        viewModelScope.launch {
            ReminderScheduler.scheduleEscalation(getApplication(), task)
        }
    }

    fun getTask(id: Long): StateFlow<Task?> =
        dao.observeById(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---- Categories ----
    suspend fun addCategory(name: String, color: Int): Long =
        catDao.insert(Category(name = name.trim(), color = color))

    fun updateCategory(category: Category) {
        viewModelScope.launch { catDao.update(category) }
    }

    /** Deleting a category reassigns its tasks to 未分类 (0) instead of removing them. */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            catDao.detachTasks(category.id)
            catDao.deleteById(category.id)
        }
    }
}
