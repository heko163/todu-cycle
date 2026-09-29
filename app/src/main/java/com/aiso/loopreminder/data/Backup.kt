package com.aiso.loopreminder.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Dependency-free (org.json) serialization for the full backup of categories + tasks.
 *
 * The format is a flat snapshot — when importing, category IDs are remapped because the target
 * database will assign fresh IDs, and tasks reference categories through that remapped map.
 */
object BackupUtil {

    data class BackupData(val categories: List<Category>, val tasks: List<Task>)

    fun buildJson(categories: List<Category>, tasks: List<Task>): String {
        val root = JSONObject()
        root.put("app", "LoopReminder")
        root.put("version", 1)
        root.put("exportedAt", LocalDate.now().toString())

        val cats = JSONArray()
        categories.forEach { c ->
            val o = JSONObject()
            o.put("id", c.id)
            o.put("name", c.name)
            o.put("color", c.color)
            cats.put(o)
        }
        root.put("categories", cats)

        val tks = JSONArray()
        tasks.forEach { t ->
            val o = JSONObject()
            o.put("id", t.id)
            o.put("title", t.title)
            o.put("recurrence", t.recurrence.name)
            val days = JSONArray()
            t.repeatDays.forEach { days.put(it.value) }
            o.put("repeatDays", days)
            o.put("timeMinuteOfDay", t.timeMinuteOfDay)
            o.put("monthDay", t.monthDay)
            o.put("yearMonth", t.yearMonth)
            o.put("yearDay", t.yearDay)
            o.put("advanceNoticeMinutes", t.advanceNoticeMinutes)
            o.put("reminderMode", t.reminderMode)
            o.put("escalateEnabled", t.escalateEnabled)
            o.put("escalateIntervalMinutes", t.escalateIntervalMinutes)
            o.put("persistentNotification", t.persistentNotification)
            o.put("notes", t.notes)
            o.put("categoryId", t.categoryId)
            o.put("active", t.active)
            o.put("createdAt", t.createdAt)
            o.put("lastCompletedDate", t.lastCompletedDate ?: JSONObject.NULL)
            tks.put(o)
        }
        root.put("tasks", tks)
        return root.toString(2)
    }

    fun parseJson(json: String): BackupData {
        val root = JSONObject(json)

        val cats = mutableListOf<Category>()
        val catsArr = root.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until catsArr.length()) {
            val o = catsArr.getJSONObject(i)
            cats.add(
                Category(
                    id = o.optLong("id", 0),
                    name = o.optString("name", "未命名"),
                    color = o.optInt("color", 0xFF014DB2.toInt())
                )
            )
        }

        val tasks = mutableListOf<Task>()
        val tksArr = root.optJSONArray("tasks") ?: JSONArray()
        for (i in 0 until tksArr.length()) {
            val o = tksArr.getJSONObject(i)
            val days = mutableSetOf<DayOfWeek>()
            val dArr = o.optJSONArray("repeatDays")
            if (dArr != null) {
                for (j in 0 until dArr.length()) {
                    runCatching { days.add(DayOfWeek.of(dArr.getInt(j))) }
                }
            }
            tasks.add(
                Task(
                    id = o.optLong("id", 0),
                    title = o.optString("title", ""),
                    recurrence = runCatching {
                        RecurrenceType.valueOf(o.optString("recurrence", "DAILY"))
                    }.getOrDefault(RecurrenceType.DAILY),
                    timeMinuteOfDay = o.optInt("timeMinuteOfDay", 600),
                    repeatDays = days,
                    monthDay = o.optInt("monthDay", 1),
                    yearMonth = o.optInt("yearMonth", 1),
                    yearDay = o.optInt("yearDay", 1),
                    advanceNoticeMinutes = o.optInt("advanceNoticeMinutes", 0),
                    reminderMode = o.optString("reminderMode", "通知 + 声音"),
                    escalateEnabled = o.optBoolean("escalateEnabled", false),
                    escalateIntervalMinutes = o.optInt("escalateIntervalMinutes", 60),
                    persistentNotification = o.optBoolean("persistentNotification", false),
                    notes = o.optString("notes", ""),
                    categoryId = o.optLong("categoryId", 0),
                    active = o.optBoolean("active", true),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    lastCompletedDate = if (o.isNull("lastCompletedDate")) null else o.optString("lastCompletedDate")
                )
            )
        }
        return BackupData(cats, tasks)
    }
}

/**
 * Whether [this] task is the same as [other] by its meaningful, user-visible fields.
 * Used during import to skip duplicates when re-importing the same backup.
 */
fun Task.isDuplicateOf(other: Task): Boolean =
    title == other.title &&
        recurrence == other.recurrence &&
        timeMinuteOfDay == other.timeMinuteOfDay &&
        repeatDays == other.repeatDays &&
        monthDay == other.monthDay &&
        yearMonth == other.yearMonth &&
        yearDay == other.yearDay &&
        notes == other.notes &&
        categoryId == other.categoryId
