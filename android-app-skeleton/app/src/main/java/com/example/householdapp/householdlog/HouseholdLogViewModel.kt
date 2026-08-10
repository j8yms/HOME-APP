package com.example.householdapp.householdlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.HouseholdLogEntry
import com.example.householdapp.core.repository.HouseholdLogRepository
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.LevelUpEvent
import com.example.householdapp.core.ui.components.detectLevelUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HouseholdLogUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val logs: List<HouseholdLogEntry> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val levelUpEvent: LevelUpEvent? = null
)

class HouseholdLogViewModel(
    private val repository: HouseholdLogRepository = HouseholdLogRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(HouseholdLogUiState())
    val uiState: StateFlow<HouseholdLogUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !forceRefresh && it.logs.isEmpty(),
                    errorMessage = null
                )
            }

            runCatching { repository.listLogs() }
                .onSuccess { response ->
                    val logs = if (response.success) response.data?.logs.orEmpty() else emptyList()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            logs = logs.sortedByDescending { log -> log.loggedAt },
                            errorMessage = if (response.success) null else response.error?.message
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unable to load logs."
                        )
                    }
                }
        }
    }

    fun createLog(type: String, title: String, details: String, onSuccess: () -> Unit = {}) {
        val userId = SessionManager.sessionState.value.user?.userId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in first.") }
            return
        }
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title is required.") }
            return
        }

        viewModelScope.launch {
            val previousLevel = SessionManager.sessionState.value.user?.level
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            runCatching { repository.createLog(userId, type, title.trim(), details.trim()) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = response.error?.message ?: "Unable to create log.")
                        }
                        return@launch
                    }

                    SessionManager.updateUser(data.user)
                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            logs = (listOf(data.log) + state.logs).sortedByDescending { it.loggedAt },
                            successMessage = "Logged: ${data.log.title}. +${data.log.xpReward} XP, +${data.log.coinReward} coins",
                            errorMessage = null,
                            levelUpEvent = detectLevelUp(previousLevel, data.user)?.let { LevelUpEvent(it) }
                        )
                    }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to create log.")
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun consumeLevelUp() {
        _uiState.update { it.copy(levelUpEvent = null) }
    }
}
