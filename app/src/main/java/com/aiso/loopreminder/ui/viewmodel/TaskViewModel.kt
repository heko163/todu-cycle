package com.aiso.loopreminder.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiso.loopreminder.data.AppDatabase
import com.aiso.loopreminder.data.BackupUtil
import com.aiso.loopreminder.data.Category
import com.aiso.loopreminder.data.Task
import com.aiso.loopreminder.data.isDuplicateOf
import com.aiso.loopreminder.data.shouldShowPersistent
import com.aiso.loopreminder.reminder.NotificationHelper
import com.aiso.loopreminder.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.get(application).taskDao()
    private val catDao = AppDatabase.get(application).categoryDao()

    // Eagerly (not WhileSubscribed) keeps the upstream Room flow always live, so the list can
    // never get stuck on its initial emptyList() after a seed/restart — this fixed the
    // "today's todos don't refresh on open until you navigate away and back" bug.
    val tasks: StateFlow<List<Task>> =
        dao.observeActive().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> =
        catDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    // ---- Backup: export / import todos + categories as JSON ----

    /** Write all categories + tasks to the user-picked file (Storage Access Framework). */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val cats = catDao.getAll()
            val tks = dao.getAll()
            val json = BackupUtil.buildJson(cats, tks)
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    /**
     * Import a previously exported backup. Categories are inserted (IDs remapped, duplicate names
     * reused) and tasks are added with their category reference fixed up. Tasks identical to an
     * existing one are skipped to avoid duplicates on re-import.
     * Callback receives (tasksImported, categoriesCreated); (-1, -1) on read/parse failure.
     */
    fun importBackup(uri: Uri, onResult: (Int, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var json: String? = null
            try {
                json = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.use { it.bufferedReader().readText() }
            } catch (_: Exception) {
            }
            if (json == null) {
                withContext(Dispatchers.Main) { onResult(-1, -1) }
                return@launch
            }
            val data = runCatching { BackupUtil.parseJson(json!!) }.getOrNull()
            if (data == null) {
                withContext(Dispatchers.Main) { onResult(-1, -1) }
                return@launch
            }
            val existing = dao.getAll()
            val existingCats = catDao.getAll()
            val idMap = mutableMapOf<Long, Long>()
            var catCount = 0
            data.categories.forEach { c ->
                val reused = existingCats.firstOrNull { it.name == c.name }
                val newId = reused?.id ?: catDao.insert(c.copy(id = 0))
                idMap[c.id] = newId
                if (reused == null) catCount++
            }
            var taskCount = 0
            data.tasks.forEach { t ->
                val cand = t.copy(id = 0, categoryId = idMap[t.categoryId] ?: 0)
                if (existing.any { it.isDuplicateOf(cand) }) return@forEach
                val newId = dao.insert(cand)
                ReminderScheduler.schedule(getApplication(), cand.copy(id = newId))
                taskCount++
            }
            withContext(Dispatchers.Main) { onResult(taskCount, catCount) }
        }
    }
}
