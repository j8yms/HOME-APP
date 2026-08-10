package com.example.householdapp.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.repository.AuthRepository
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sessionState: SessionState = SessionState()
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun bootstrap(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching { authRepository.bootstrap(email) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.error?.message ?: "Bootstrap failed."
                        )
                        return@launch
                    }

                    if (!data.authorized || data.user == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "This Google account is not authorized for the household."
                        )
                        return@launch
                    }

                    val session = SessionState(
                        isAuthenticated = true,
                        user = data.user,
                        partner = data.partner,
                        email = data.user.email
                    )
                    SessionManager.setSession(session)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        sessionState = session
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = friendlyNetworkError(throwable, "Unable to sign in.")
                    )
                }
        }
    }

    fun bootstrapDevice(deviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            runCatching { authRepository.bootstrapDevice(deviceId) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = response.error?.message ?: "Bootstrap failed."
                        )
                        return@launch
                    }

                    if (!data.authorized || data.user == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = data.reason ?: "This device is not authorized for HOME MANAGER."
                        )
                        return@launch
                    }

                    val session = SessionState(
                        isAuthenticated = true,
                        user = data.user,
                        partner = data.partner,
                        email = data.user.email
                    )
                    SessionManager.setSession(session)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        sessionState = session
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = friendlyNetworkError(throwable, "Unable to bootstrap device.")
                    )
                }
        }
    }

    private fun friendlyNetworkError(throwable: Throwable, fallback: String): String {
        val message = throwable.message.orEmpty().lowercase()
        return when {
            throwable is java.net.SocketTimeoutException || message.contains("timeout") || message.contains("timed out") ->
                "The household server took too long to respond. Please check your connection and try again."
            throwable is java.net.ConnectException || message.contains("failed to connect") ->
                "Could not reach the household server. Check your internet connection and try again."
            else -> throwable.message ?: fallback
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message, isLoading = false)
    }
}
