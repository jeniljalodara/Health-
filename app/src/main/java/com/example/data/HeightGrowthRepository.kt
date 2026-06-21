package com.example.data

import kotlinx.coroutines.flow.Flow

class HeightGrowthRepository(private val dao: HeightGrowthDao) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfile()

    suspend fun getUserProfileOneShot(): UserProfile? = dao.getUserProfileOneShot()

    suspend fun insertUserProfile(profile: UserProfile) = dao.insertUserProfile(profile)

    // Height
    val allHeightRecords: Flow<List<HeightRecord>> = dao.getAllHeightRecords()

    suspend fun insertHeightRecord(record: HeightRecord) = dao.insertHeightRecord(record)

    suspend fun deleteHeightRecord(id: Int) = dao.deleteHeightRecord(id)

    // Sleep
    val allSleepRecords: Flow<List<SleepRecord>> = dao.getAllSleepRecords()

    fun getSleepRecordsForDate(date: String): Flow<List<SleepRecord>> = dao.getSleepRecordsForDate(date)

    suspend fun insertSleepRecord(record: SleepRecord) = dao.insertSleepRecord(record)

    suspend fun deleteSleepRecord(id: Int) = dao.deleteSleepRecord(id)

    // Nutrition
    val allNutritionRecords: Flow<List<NutritionRecord>> = dao.getAllNutritionRecords()

    fun getNutritionRecordsForDate(date: String): Flow<List<NutritionRecord>> = dao.getNutritionRecordsForDate(date)

    suspend fun insertNutritionRecord(record: NutritionRecord) = dao.insertNutritionRecord(record)

    suspend fun deleteNutritionRecord(id: Int) = dao.deleteNutritionRecord(id)

    // Exercises
    val allExerciseRecords: Flow<List<ExerciseRecord>> = dao.getAllExerciseRecords()

    fun getExerciseRecordsForDate(date: String): Flow<List<ExerciseRecord>> = dao.getExerciseRecordsForDate(date)

    suspend fun insertExerciseRecord(record: ExerciseRecord) = dao.insertExerciseRecord(record)

    suspend fun deleteExerciseRecord(id: Int) = dao.deleteExerciseRecord(id)
}
