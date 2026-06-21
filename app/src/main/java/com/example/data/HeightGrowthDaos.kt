package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HeightGrowthDao {

    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 0 LIMIT 1")
    suspend fun getUserProfileOneShot(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // Height Records
    @Query("SELECT * FROM height_records ORDER BY timestamp ASC")
    fun getAllHeightRecords(): Flow<List<HeightRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeightRecord(record: HeightRecord)

    @Query("DELETE FROM height_records WHERE id = :id")
    suspend fun deleteHeightRecord(id: Int)

    // Sleep Records
    @Query("SELECT * FROM sleep_records ORDER BY date DESC, timestamp DESC")
    fun getAllSleepRecords(): Flow<List<SleepRecord>>

    @Query("SELECT * FROM sleep_records WHERE date = :date ORDER BY timestamp DESC")
    fun getSleepRecordsForDate(date: String): Flow<List<SleepRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepRecord(record: SleepRecord)

    @Query("DELETE FROM sleep_records WHERE id = :id")
    suspend fun deleteSleepRecord(id: Int)

    // Nutrition Records
    @Query("SELECT * FROM nutrition_records ORDER BY date DESC, timestamp DESC")
    fun getAllNutritionRecords(): Flow<List<NutritionRecord>>

    @Query("SELECT * FROM nutrition_records WHERE date = :date ORDER BY timestamp DESC")
    fun getNutritionRecordsForDate(date: String): Flow<List<NutritionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionRecord(record: NutritionRecord)

    @Query("DELETE FROM nutrition_records WHERE id = :id")
    suspend fun deleteNutritionRecord(id: Int)

    // Exercise Records
    @Query("SELECT * FROM exercise_records ORDER BY date DESC, timestamp DESC")
    fun getAllExerciseRecords(): Flow<List<ExerciseRecord>>

    @Query("SELECT * FROM exercise_records WHERE date = :date ORDER BY timestamp DESC")
    fun getExerciseRecordsForDate(date: String): Flow<List<ExerciseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseRecord(record: ExerciseRecord)

    @Query("DELETE FROM exercise_records WHERE id = :id")
    suspend fun deleteExerciseRecord(id: Int)
}
