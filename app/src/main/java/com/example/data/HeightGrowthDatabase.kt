package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfile::class,
        HeightRecord::class,
        SleepRecord::class,
        NutritionRecord::class,
        ExerciseRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class HeightGrowthDatabase : RoomDatabase() {

    abstract fun heightGrowthDao(): HeightGrowthDao

    companion object {
        @Volatile
        private var INSTANCE: HeightGrowthDatabase? = null

        fun getDatabase(context: Context): HeightGrowthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HeightGrowthDatabase::class.java,
                    "height_growth_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
