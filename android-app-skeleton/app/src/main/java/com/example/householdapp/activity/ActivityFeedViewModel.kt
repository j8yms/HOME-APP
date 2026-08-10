package com.example.householdapp.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.HouseholdLogEntry
import com.example.householdapp.core.model.Task
import com.example.householdapp.core.repository.DashboardRepository
import com.example.householdapp.core.repository.HistoryRepository
import com.example.householdapp.core.repository.HouseholdLogRepository
import com.example.householdapp.core.session.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityItem(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val timestamp: String,
    val xpReward: Int = 0,
    val coinReward: Int = 0
)

data class ActivityFeedUiState(
    val isLoading: Boolean = false,
    val activities: List<ActivityItem> = emptyList(),
    val errorMessage: String? = null
)

class ActivityFeedViewModel(
    private val householdLogRepository: HouseholdLogRepository = HouseholdLogRepository(),
    private val dashboardRepository: DashboardRepository = DashboardRepository(),
    private val historyRepository: HistoryRepository = HistoryRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityFeedUiState())
    val uiState: StateFlow<ActivityFeedUiState> = _uiState.asStateFlow()

    init {
        loadActivity()
    }

    fun loadActivity() {
        val userId = SessionManager.sessionState.value.user?.userId.orEmpty()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val logsDeferred = async { runCatching { householdLogRepository.listLogs(userId) } }
            val dashboardDeferred = async { runCatching { dashboardRepository.getDashboard(userId) } }
            val ledgerDeferred = async { runCatching { historyRepository.getLedger() } }

            val logsResult = logsDeferred.await()
            val dashboardResult = dashboardDeferred.await()
            val ledgerResult = ledgerDeferred.await()

            val activities = mutableListOf<ActivityItem>()

            logsResult.onSuccess { response ->
                if (response.success) {
                    response.data?.logs?.forEach { log ->
                        activities.add(
                            ActivityItem(
                                id = log.logId,
                                type = "household",
                                title = log.title,
                                subtitle = "${log.type.replaceFirstChar { it.uppercase() }} • +${log.xpReward} XP, +${log.coinReward} coins",
                                timestamp = log.loggedAt,
                                xpReward = log.xpReward,
                                coinReward = log.coinReward
                            )
                        )
                    }
                }
            }

            dashboardResult.onSuccess { response ->
                if (response.success) {
                    val data = response.data
                    data?.tasksDueToday?.filter { it.status == "completed" }?.forEach { task ->
                        activities.add(
                            ActivityItem(
                                id = "completed_${task.taskId}",
                                type = "task",
                                title = task.title,
                                subtitle = "Task completed • +${task.xpReward} XP, +${task.coinReward} coins",
                                timestamp = task.completedAt.ifBlank { task.updatedAt },
                                xpReward = task.xpReward,
                                coinReward = task.coinReward
                            )
                        )
                    }
                }
            }

            ledgerResult.onSuccess { response ->
                if (response.success) {
                    response.data?.entries?.forEach { entry ->
                        val item = when (entry.actionType) {
                            "workout_logged" -> ActivityItem(
                                id = entry.ledgerId,
                                type = "workout",
                                title = "Workout logged",
                                subtitle = "+${entry.xpDelta} XP, +${entry.coinDelta} coins",
                                timestamp = entry.createdAt,
                                xpReward = entry.xpDelta,
                                coinReward = entry.coinDelta
                            )
                            "streak_bonus" -> ActivityItem(
                                id = entry.ledgerId,
                                type = "streak",
                                title = "Weekly streak bonus",
                                subtitle = "Nice consistency! +${entry.xpDelta} XP",
                                timestamp = entry.createdAt,
                                xpReward = entry.xpDelta,
                                coinReward = entry.coinDelta
                            )
                            "reward_redeem" -> ActivityItem(
                                id = entry.ledgerId,
                                type = "reward",
                                title = entry.reason.ifBlank { "Reward redeemed" },
                                subtitle = "Spent ${-entry.coinDelta} coins, ${-entry.xpDelta} XP",
                                timestamp = entry.createdAt,
                                xpReward = entry.xpDelta,
                                coinReward = entry.coinDelta
                            )
                            else -> null
                        }
                        if (item != null) activities.add(item)
                    }
                }
            }

            val sortedActivities = activities.sortedByDescending { it.timestamp }

            val error = if (logsResult.isFailure && dashboardResult.isFailure && ledgerResult.isFailure) {
                logsResult.exceptionOrNull()?.message ?: "Unable to load activity."
            } else null

            _uiState.update {
                it.copy(
                    isLoading = false,
                    activities = sortedActivities,
                    errorMessage = error
                )
            }
        }
    }
}
