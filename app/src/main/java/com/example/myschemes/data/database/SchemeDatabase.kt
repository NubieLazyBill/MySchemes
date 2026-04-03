package com.example.myschemes.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myschemes.data.model.Scheme

@Database(
    entities = [Scheme::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SchemeDatabase : RoomDatabase() {
    abstract fun schemeDao(): SchemeDao

    companion object {
        @Volatile
        private var INSTANCE: SchemeDatabase? = null

        fun getInstance(context: Context): SchemeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SchemeDatabase::class.java,
                    "schemes_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}