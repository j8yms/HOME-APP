package com.example.householdapp.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.WorkoutEntry
import com.example.householdapp.core.repository.WorkoutRepository
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.LevelUpEvent
import com.example.householdapp.core.ui.components.detectLevelUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutUiState(
    val isLoadingHistory: Boolean = false,
    val isSubmitting: Boolean = false,
    val workouts: List<WorkoutEntry> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val levelUpEvent: LevelUpEvent? = null
)

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository = WorkoutRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        loadWorkouts()
    }

    fun loadWorkouts(forceRefresh: Boolean = false) {
        val userId = SessionManager.sessionState.value.user?.userId.orEmpty()
        if (userId.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingHistory = !forceRefresh && it.workouts.isEmpty(),
                    errorMessage = null
                )
            }

            runCatching { workoutRepository.listWorkouts(userId) }
                .onSuccess { response ->
                    val workouts = if (response.success) {
                        response.data?.workouts.orEmpty()
                    } else {
                        emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            workouts = workouts,
                            errorMessage = if (response.success) null else response.error?.message
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingHistory = false,
                            errorMessage = throwable.message ?: "Unable to load workouts."
                        )
                    }
                }
        }
    }

    fun submitWorkout(
        workoutType: String,
        durationText: String,
        intensity: String,
        notes: String,
        exerciseTogether: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        val sessionUser = SessionManager.sessionState.value.user
        if (sessionUser == null) {
            _uiState.update { it.copy(errorMessage = "Sign in first to log workouts.") }
            return
        }
        val userId = sessionUser.userId

        val durationMinutes = durationText.toIntOrNull()
        if (workoutType.isBlank() || durationMinutes == null || durationMinutes <= 0) {
            _uiState.update {
                it.copy(errorMessage = "Enter a workout type and a valid duration in minutes.")
            }
            return
        }

        viewModelScope.launch {
            val previousLevel = SessionManager.sessionState.value.user?.level

            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            runCatching {
                workoutRepository.logWorkout(
                    userId = userId,
                    workoutType = workoutType.trim(),
                    durationMinutes = durationMinutes,
                    intensity = intensity,
                    notes = notes.trim(),
                    bothPartners = exerciseTogether
                )
            }.onSuccess { response ->
                val data = response.data
                if (!response.success || data == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = response.error?.message ?: "Unable to log workout."
                        )
                    }
                    return@launch
                }

                SessionManager.updateUser(data.user)
                var message = "Workout logged for ${data.xpReward} XP and ${data.coinReward} coins."
                if (data.loggedForBoth && data.partnerUser != null) {
                    SessionManager.updatePartner(data.partnerUser)
                    message = "Exercise together! You earned ${data.xpReward} XP / ${data.coinReward} coins, and your partner earned ${data.partnerXpReward} XP / ${data.partnerCoinReward} coins."
                }
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = message,
                        errorMessage = null,
                        levelUpEvent = detectLevelUp(previousLevel, data.user)?.let { LevelUpEvent(it) }
                    )
                }
                onSuccess()
                loadWorkouts(forceRefresh = true)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Unable to log workout."
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun consumeLevelUp() {
        _uiState.update { it.copy(levelUpEvent = null) }
    }
}
