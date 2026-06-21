package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    val name: String = "User",
    val age: Int = 16,
    val gender: String = "Male",
    val currentHeight: Float = 165.0f,
    val targetHeight: Float = 180.0f,
    val fatherHeight: Float = 175.0f,
    val motherHeight: Float = 162.0f,
    val weight: Float = 55.0f
) {
    // Calculates mid-parental genetic height projection (in cm)
    fun getGeneticHeightLimit(): Float {
        return if (gender.equals("Male", ignoreCase = true)) {
            (fatherHeight + motherHeight + 13f) / 2f
        } else {
            (fatherHeight + motherHeight - 13f) / 2f
        }
    }
}

@Entity(tableName = "height_records")
data class HeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val height: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val durationHours: Float,
    val quality: Int, // 1 to 5 stars
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "nutrition_records")
data class NutritionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val foodName: String,
    val calories: Int,
    val proteinGrams: Float,
    val calciumMg: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercise_records")
data class ExerciseRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val exerciseName: String,
    val durationMinutes: Int,
    val caloriesBurned: Int = durationMinutes * 6, // generic estimate
    val timestamp: Long = System.currentTimeMillis()
)
