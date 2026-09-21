package com.aiso.loopreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE active = 1 ORDER BY timeMinuteOfDay ASC")
    fun observeActive(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE active = 1 ORDER BY timeMinuteOfDay ASC")
    suspend fun getActive(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Query("SELECT COUNT(*) FROM tasks WHERE active = 1 AND lastCompletedDate = :today")
    fun observeCompletedTodayCount(today: String): Flow<Int>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET lastCompletedDate = :date WHERE id = :id")
    suspend fun markCompleted(id: Long, date: String)

    @Query("UPDATE tasks SET lastCompletedDate = NULL WHERE id = :id")
    suspend fun unmarkCompleted(id: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE active = 1")
    suspend fun countActive(): Int
}
