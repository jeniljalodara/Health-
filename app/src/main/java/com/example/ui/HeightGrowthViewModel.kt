package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeightGrowthViewModel(private val repository: HeightGrowthRepository) : ViewModel() {

    // Selected Date for displays & logs
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Observe DB elements reactively
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val heightRecords: StateFlow<List<HeightRecord>> = repository.allHeightRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sleepRecords: StateFlow<List<SleepRecord>> = repository.allSleepRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val nutritionRecords: StateFlow<List<NutritionRecord>> = repository.allNutritionRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val exerciseRecords: StateFlow<List<ExerciseRecord>> = repository.allExerciseRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Chat session state
    private val _chatHistory = MutableStateFlow<List<Content>>(
        listOf(
            Content(
                parts = listOf(
                    Part(
                        text = "Hello! I am Dr. Heighten, your personal Pediatric Height & Health Growth Coach. " +
                                "Ask me anything about stretching exercises, sleeping strategies, nutrition recipes, or general heights genetics! " +
                                "Together, we can fully unlock your natural development capacity."
                    )
                ),
                role = "model"
            )
        )
    )
    val chatHistory: StateFlow<List<Content>> = _chatHistory.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        // Create dynamic initial profile if db is empty
        viewModelScope.launch {
            val current = repository.getUserProfileOneShot()
            if (current == null) {
                repository.insertUserProfile(UserProfile())
                // And insert an initial height record
                repository.insertHeightRecord(HeightRecord(height = 165.0f))
            }
        }
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    // --- Profile Operations ---
    fun updateProfile(
        name: String,
        age: Int,
        gender: String,
        currentHeight: Float,
        targetHeight: Float,
        fatherHeight: Float,
        motherHeight: Float,
        weight: Float
    ) {
        viewModelScope.launch {
            val updated = UserProfile(
                id = 0,
                name = name,
                age = age,
                gender = gender,
                currentHeight = currentHeight,
                targetHeight = targetHeight,
                fatherHeight = fatherHeight,
                motherHeight = motherHeight,
                weight = weight
            )
            repository.insertUserProfile(updated)

            // Auto log a height record when updating height
            val latestRecord = repository.allHeightRecords.stateIn(viewModelScope).value.lastOrNull()
            if (latestRecord == null || latestRecord.height != currentHeight) {
                repository.insertHeightRecord(HeightRecord(height = currentHeight))
            }
        }
    }

    // --- Height Operations ---
    fun addHeightRecord(height: Float) {
        viewModelScope.launch {
            repository.insertHeightRecord(HeightRecord(height = height))
            // Update profile's current height to match the latest logged height as well!
            val profile = repository.getUserProfileOneShot() ?: UserProfile()
            repository.insertUserProfile(profile.copy(currentHeight = height))
        }
    }

    fun deleteHeightRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteHeightRecord(id)
        }
    }

    // --- Sleep Operations ---
    fun addSleepRecord(duration: Float, quality: Int) {
        viewModelScope.launch {
            repository.insertSleepRecord(
                SleepRecord(
                    date = _selectedDate.value,
                    durationHours = duration,
                    quality = quality
                )
            )
        }
    }

    fun deleteSleepRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteSleepRecord(id)
        }
    }

    // --- Nutrition Operations ---
    fun addNutritionRecord(foodName: String, calories: Int, protein: Float, calcium: Float) {
        viewModelScope.launch {
            repository.insertNutritionRecord(
                NutritionRecord(
                    date = _selectedDate.value,
                    foodName = foodName,
                    calories = calories,
                    proteinGrams = protein,
                    calciumMg = calcium
                )
            )
        }
    }

    fun deleteNutritionRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteNutritionRecord(id)
        }
    }

    // --- Exercise Operations ---
    fun addExerciseRecord(exerciseName: String, durationMinutes: Int) {
        viewModelScope.launch {
            repository.insertExerciseRecord(
                ExerciseRecord(
                    date = _selectedDate.value,
                    exerciseName = exerciseName,
                    durationMinutes = durationMinutes
                )
            )
        }
    }

    fun deleteExerciseRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteExerciseRecord(id)
        }
    }

    // --- Chat Operations ---
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        val currentHistory = _chatHistory.value.toMutableList()

        // 1. Add user message
        val userContent = Content(parts = listOf(Part(text = message)), role = "user")
        currentHistory.add(userContent)
        _chatHistory.value = currentHistory

        _isChatLoading.value = true

        // 2. Fetch response from Gemini
        viewModelScope.launch {
            val responseText = GeminiClient.getCoachResponse(currentHistory)

            val coachContent = Content(parts = listOf(Part(text = responseText)), role = "model")
            val updatedHistory = _chatHistory.value.toMutableList()
            updatedHistory.add(coachContent)

            _chatHistory.value = updatedHistory
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatHistory.value = listOf(
            Content(
                parts = listOf(
                    Part(
                        text = "Hello! I am Dr. Heighten, your personal Pediatric Height & Health Growth Coach. " +
                                "Ask me anything about stretching exercises, sleeping strategies, nutrition recipes, or general heights genetics! " +
                                "Together, we can fully unlock your natural development capacity."
                    )
                ),
                role = "model"
            )
        )
    }

    companion object {
        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}

class HeightGrowthViewModelFactory(private val repository: HeightGrowthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HeightGrowthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HeightGrowthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
