package com.ahara.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MealEntry::class, UserProfile::class, Assessment::class, DailyLog::class, EnergyLog::class],
    version = 1,
    exportSchema = true
)
abstract class AharaDatabase : RoomDatabase() {
    abstract fun dao(): AharaDao

    companion object {
        @Volatile private var instance: AharaDatabase? = null

        fun get(context: Context): AharaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AharaDatabase::class.java,
                    "ahara.db"
                ).build().also { instance = it }
            }
    }
}
