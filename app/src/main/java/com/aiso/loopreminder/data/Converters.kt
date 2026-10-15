package com.aiso.loopreminder.data

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {

    @TypeConverter
    fun recurrenceToString(v: RecurrenceType): String = v.name

    @TypeConverter
    fun stringToRecurrence(v: String): RecurrenceType = RecurrenceType.valueOf(v)

    /** Store the weekly selection as a bitmask (bit N = DayOfWeek.value == N). */
    @TypeConverter
    fun daysToInt(v: Set<DayOfWeek>): Int =
        v.fold(0) { acc, d -> acc or (1 shl d.value) }

    @TypeConverter
    fun intToDays(v: Int): Set<DayOfWeek> =
        DayOfWeek.values().filter { v and (1 shl it.value) != 0 }.toSet()

    /** 每月/每年多选日期列表，存为逗号分隔字符串，如 "4,5,12"。 */
    @TypeConverter
    fun intListToString(v: List<Int>): String = v.joinToString(",")

    @TypeConverter
    fun stringToIntList(v: String): List<Int> =
        if (v.isBlank()) emptyList()
        else v.split(",").mapNotNull { it.trim().toIntOrNull() }
}
