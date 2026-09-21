package com.aiso.loopreminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-defined category (e.g. 工作 / 生活 / 健康). Tasks reference one via [Task.categoryId].
 * [color] is an ARGB int so it round-trips cleanly through Room.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int = 0xFF014DB2.toInt()
)

/** Preset palette offered when creating / recoloring a category. */
val CATEGORY_PALETTE: List<Int> = listOf(
    0xFF014DB2.toInt(), // primary blue
    0xFF10B981.toInt(), // success green
    0xFFF59E0B.toInt(), // amber
    0xFFEF4444.toInt(), // danger red
    0xFF7C3AED.toInt(), // purple
    0xFFEC4899.toInt(), // pink
    0xFF14B8A6.toInt(), // teal
    0xFF0EA5E9.toInt()  // sky
)
