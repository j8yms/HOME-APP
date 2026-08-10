package com.example.householdapp.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.DashboardData
import com.example.householdapp.core.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val dashboardData: DashboardData = DashboardData()
)

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository = DashboardRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard(userId: String, forceRefresh: Boolean = false) {
        if (userId.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !forceRefresh && it.dashboardData.currentUser == null,
                    isRefreshing = forceRefresh,
                    errorMessage = null
                )
            }

            runCatching { dashboardRepository.getDashboard(userId) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = response.error?.message ?: "Unable to load dashboard."
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            dashboardData = DashboardData(
                                currentUser = data.currentUser,
                                partner = data.partner,
                                tasksDueToday = data.tasksDueToday,
                                overdueTasks = data.overdueTasks,
                                couplesStreak = data.couplesStreak
                            )
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = throwable.message ?: "Unable to load dashboard."
                        )
                    }
                }
        }
    }
}
