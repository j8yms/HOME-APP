package com.example.householdapp.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.repository.FinanceRepository
import com.example.householdapp.core.repository.ProfileRepository
import com.example.householdapp.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isRenaming: Boolean = false,
    val isSettingCurrency: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()
    private val financeRepository = FinanceRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun rename(displayName: String, onSuccess: () -> Unit) {
        val userId = SessionManager.sessionState.value.user?.userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRenaming = true, errorMessage = null) }
            runCatching { repository.updateDisplayName(userId, displayName.trim()) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(isRenaming = false, errorMessage = response.error?.message ?: "Unable to update name.")
                        }
                        return@launch
                    }
                    SessionManager.updateUser(data)
                    _uiState.update { it.copy(isRenaming = false, errorMessage = null) }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isRenaming = false, errorMessage = throwable.message ?: "Unable to update name.")
                    }
                }
        }
    }

    fun setSide(side: String) {
        val user = SessionManager.sessionState.value.user ?: return
        val currentSide = user.householdSide
        if (currentSide == side) return

        val displayName = user.displayName
        viewModelScope.launch {
            _uiState.update { it.copy(isRenaming = true, errorMessage = null) }
            runCatching { repository.updateDisplayName(user.userId, displayName, householdSide = side) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(isRenaming = false, errorMessage = response.error?.message ?: "Unable to update side.")
                        }
                        return@launch
                    }
                    SessionManager.updateUser(data)
                    _uiState.update { it.copy(isRenaming = false, errorMessage = null) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isRenaming = false, errorMessage = throwable.message ?: "Unable to update side.")
                    }
                }
        }
    }

    fun setCurrency(currency: String, onSuccess: () -> Unit) {
        val current = SessionManager.sessionState.value.currency
        if (current.equals(currency, ignoreCase = true)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSettingCurrency = true, errorMessage = null) }
            runCatching { financeRepository.setCurrency(currency) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(isSettingCurrency = false, errorMessage = response.error?.message ?: "Unable to update currency.")
                        }
                        return@launch
                    }
                    SessionManager.setCurrency(data.currency)
                    _uiState.update { it.copy(isSettingCurrency = false, errorMessage = null) }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSettingCurrency = false, errorMessage = throwable.message ?: "Unable to update currency.")
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
