package com.example.myschemes.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myschemes.data.model.Scheme

@Database(
    entities = [Scheme::class],
    version = 7,  // ← увеличиваем с 6 на 7
    exportSchema = false
)
abstract class SchemeDatabase : RoomDatabase() {
    abstract fun schemeDao(): SchemeDao

    companion object {
        @Volatile
        private var INSTANCE: SchemeDatabase? = null

        // ТОЛЬКО ОДНА МИГРАЦИЯ - с 6 на 7
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем колонку isInspected
                database.execSQL("ALTER TABLE schemes ADD COLUMN isInspected INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): SchemeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SchemeDatabase::class.java,
                    "schemes_database"
                )
                    .addMigrations(MIGRATION_6_7)  // ← ТОЛЬКО ЭТА МИГРАЦИЯ
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}