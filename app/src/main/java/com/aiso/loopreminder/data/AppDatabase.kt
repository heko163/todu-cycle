package com.aiso.loopreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Task::class, Category::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v2 -> v3: introduce user-defined categories without wiping existing tasks.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, color INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE tasks ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v3 -> v4: 每月/每年日期从单选改为多选（新增逗号分隔列表列，旧单选数据回填，不清库）。
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN monthDays TEXT NOT NULL DEFAULT '1'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN yearDays TEXT NOT NULL DEFAULT '1'")
                db.execSQL("UPDATE tasks SET monthDays = CAST(monthDay AS TEXT)")
                db.execSQL("UPDATE tasks SET yearDays = CAST(yearDay AS TEXT)")
            }
        }

        // v4 -> v5: 新增「按农历」循环（MONTHLY/YEARLY 的日期可解释为农历）。
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isLunar INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loop_reminder.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
