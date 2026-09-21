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
}
