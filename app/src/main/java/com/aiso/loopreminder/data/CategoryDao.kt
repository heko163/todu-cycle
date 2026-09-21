package com.aiso.loopreminder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun observeAll(): Flow<List<Category>>

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Reassign a category's tasks back to 未分类 (0) before deleting the category. */
    @Query("UPDATE tasks SET categoryId = 0 WHERE categoryId = :id")
    suspend fun detachTasks(id: Long)
}
