package com.example.householdapp.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.HouseholdLogEntry
import com.example.householdapp.core.model.MoneyTransaction
import com.example.householdapp.core.model.Task
import com.example.householdapp.core.repository.DashboardRepository
import com.example.householdapp.core.repository.FinanceRepository
import com.example.householdapp.core.repository.HistoryRepository
import com.example.householdapp.core.repository.HouseholdLogRepository
import com.example.householdapp.core.session.SessionManager
import kotlinx.coroutines.Job
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
    private val historyRepository: HistoryRepository = HistoryRepository(),
    private val financeRepository: FinanceRepository = FinanceRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityFeedUiState())
    val uiState: StateFlow<ActivityFeedUiState> = _uiState.asStateFlow()

    init {
        loadActivity()
    }

    // P0: single-flight — this fans out to 3 parallel requests (incl. a
    // full dashboard load), so overlapping invocations are expensive.
    private var loadJob: Job? = null

    fun loadActivity() {
        val userId = SessionManager.sessionState.value.user?.userId.orEmpty()

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val logsDeferred = async { runCatching { householdLogRepository.listLogs(userId) } }
            val dashboardDeferred = async { runCatching { dashboardRepository.getDashboard(userId) } }
            val ledgerDeferred = async { runCatching { historyRepository.getLedger() } }
            // Request 12: confirmed money movement must appear in Activity.
            val financeDeferred = async { runCatching { financeRepository.getSummary() } }

            val logsResult = logsDeferred.await()
            val dashboardResult = dashboardDeferred.await()
            val ledgerResult = ledgerDeferred.await()
            val financeResult = financeDeferred.await()

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
                    // Request 12: every completed task belongs in Activity, not
                    // only ones due today. Capped; stable ids dedupe below.
                    val completedToday = data?.tasksDueToday
                        ?.filter { it.status == "completed" }
                        .orEmpty()
                    val completedHistory = data?.household?.completedTasks.orEmpty()
                    (completedToday + completedHistory)
                        .distinctBy { it.taskId }
                        .sortedByDescending { it.completedAt.ifBlank { it.updatedAt } }
                        .take(30)
                        .forEach { task ->
                            activities.add(
                                ActivityItem(
                                    id = "task_${task.taskId}",
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

            financeResult.onSuccess { response ->
                if (response.success) {
                    activities.addAll(
                        mapTransactionsToActivity(response.data?.recentTransactions.orEmpty())
                    )
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

            // Request 12: each eligible event appears exactly once.
            val sortedActivities = activities
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }

            val error = if (logsResult.isFailure && dashboardResult.isFailure && ledgerResult.isFailure && financeResult.isFailure) {
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

    // Request 12: classify confirmed money movement. Linked wallet-to-wallet
    // transfer rows (expense 'Transfer to X' + income 'Transfer from Y',
    // same amount + timestamp) are paired into ONE transfer item so totals
    // and the feed never double-count them.
    private fun mapTransactionsToActivity(transactions: List<MoneyTransaction>): List<ActivityItem> {
        val items = mutableListOf<ActivityItem>()
        val transferRows = transactions.filter { it.category.equals("transfer", ignoreCase = true) }
        val pairedIds = mutableSetOf<String>()

        transferRows
            .groupBy { "${it.amount}|${it.createdAt}" }
            .values
            .forEach { group ->
                val outRow = group.firstOrNull { it.type == "expense" }
                val inRow = group.firstOrNull { it.type == "income" }
                if (outRow != null && inRow != null) {
                    val source = outRow.wallet.ifBlank { "wallet" }
                    val dest = inRow.wallet.ifBlank { "wallet" }
                    items.add(
                        ActivityItem(
                            id = "transfer_${outRow.amount}_${outRow.createdAt}",
                            type = "transfer",
                            title = "Transfer ${source.replaceFirstChar { it.uppercase() }} → ${dest.replaceFirstChar { it.uppercase() }}",
                            subtitle = "Transfer • ${outRow.amount}",
                            timestamp = outRow.createdAt
                        )
                    )
                    pairedIds.add(outRow.transactionId)
                    pairedIds.add(inRow.transactionId)
                }
            }

        transactions.forEach { tx ->
            if (tx.transactionId in pairedIds) return@forEach
            val item = when {
                tx.category.equals("transfer", ignoreCase = true) -> ActivityItem(
                    id = "tx_${tx.transactionId}",
                    type = "transfer",
                    title = tx.description.ifBlank { "Transfer" },
                    subtitle = "Transfer • ${tx.amount}",
                    timestamp = tx.createdAt
                )
                tx.type == "income" -> ActivityItem(
                    id = "tx_${tx.transactionId}",
                    type = "income",
                    title = tx.description.ifBlank { "Income" },
                    subtitle = "Income • +${tx.amount}" + (if (tx.category.isNotBlank()) " • ${tx.category}" else ""),
                    timestamp = tx.createdAt
                )
                tx.type == "expense" -> ActivityItem(
                    id = "tx_${tx.transactionId}",
                    type = "expense",
                    title = tx.description.ifBlank { "Expense" },
                    subtitle = "Expense • -${tx.amount}" + (if (tx.category.isNotBlank()) " • ${tx.category}" else ""),
                    timestamp = tx.createdAt
                )
                tx.type == "savings_deposit" -> ActivityItem(
                    id = "tx_${tx.transactionId}",
                    type = "savings",
                    title = tx.description.ifBlank { "Savings deposit" },
                    subtitle = "Saved • ${tx.amount}",
                    timestamp = tx.createdAt
                )
                tx.type == "savings_withdraw" -> ActivityItem(
                    id = "tx_${tx.transactionId}",
                    type = "savings",
                    title = tx.description.ifBlank { "Savings withdrawal" },
                    subtitle = "Withdrew • ${tx.amount}",
                    timestamp = tx.createdAt
                )
                else -> null
            }
            if (item != null) items.add(item)
        }
        return items
    }
}
