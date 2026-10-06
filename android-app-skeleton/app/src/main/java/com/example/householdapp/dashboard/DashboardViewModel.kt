package com.example.householdapp.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.ActivityFeed
import com.example.householdapp.core.model.CouplesStreak
import com.example.householdapp.core.model.DashboardData
import com.example.householdapp.core.model.GamificationData
import com.example.householdapp.core.model.GoalsData
import com.example.householdapp.core.model.GreetingData
import com.example.householdapp.core.model.HouseholdData
import com.example.householdapp.core.model.MoneyData
import com.example.householdapp.core.model.QuickActionsData
import com.example.householdapp.core.model.TodayData
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
                    isLoading = !forceRefresh && (it.dashboardData.currentUser == null && it.dashboardData.greeting.currentUser == null),
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

                    // Map API response to DashboardData
                    val greeting = GreetingData(
                        currentUser = data.currentUser,
                        date = data.greeting?.date ?: "",
                        householdName = data.greeting?.householdName ?: "Our Household"
                    )

                    val today = TodayData(
                        tasksDueToday = data.tasksDueToday ?: emptyList(),
                        overdueTasks = data.overdueTasks ?: emptyList(),
                        todayBills = data.today?.todayBills ?: emptyList(),
                        upcomingBills = data.today?.upcomingBills ?: emptyList(),
                        overdueBills = data.today?.overdueBills ?: emptyList(),
                        todayEvents = data.today?.todayEvents ?: emptyList(),
                        quickActions = data.today?.quickActions ?: QuickActionsData()
                    )

                    val money = MoneyData(
                        totalBalance = data.money?.totalBalance ?: 0.0,
                        accountBreakdown = data.money?.accountBreakdown ?: emptyMap(),
                        incomeThisMonth = data.money?.incomeThisMonth ?: 0.0,
                        expensesThisMonth = data.money?.expensesThisMonth ?: 0.0,
                        remainingBudget = data.money?.remainingBudget ?: 0.0,
                        subscriptionTotalMonthly = data.money?.subscriptionTotalMonthly ?: 0.0,
                        upcomingSubscriptions = data.money?.upcomingSubscriptions ?: emptyList()
                    )

                    val household = HouseholdData(
                        openTasks = data.household?.openTasks ?: emptyList(),
                        completedTasks = data.household?.completedTasks ?: emptyList(),
                        maintenanceIssues = data.household?.maintenanceIssues ?: emptyList(),
                        shoppingItems = data.household?.shoppingItems ?: emptyList(),
                        importantReminders = data.household?.importantReminders ?: emptyList()
                    )

                    val goals = GoalsData(
                        savingsGoals = data.goals?.savingsGoals ?: emptyList(),
                        monthlyTargets = data.goals?.monthlyTargets ?: emptyMap()
                    )

                    val activity = ActivityFeed(
                        entries = data.activity?.entries ?: emptyList()
                    )

                    val gamification = GamificationData(
                        xpTotal = data.gamification?.xpTotal ?: 0,
                        level = data.gamification?.level ?: 1,
                        coinsTotal = data.gamification?.coinsTotal ?: 0,
                        currentStreak = data.gamification?.currentStreak ?: 0,
                        longestStreak = data.gamification?.longestStreak ?: 0
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null,
                            dashboardData = DashboardData(
                                currentUser = data.currentUser,
                                partner = data.partner,
                                tasksDueToday = data.tasksDueToday ?: emptyList(),
                                overdueTasks = data.overdueTasks ?: emptyList(),
                                couplesStreak = data.couplesStreak ?: CouplesStreak(),
                                greeting = greeting,
                                today = today,
                                money = money,
                                household = household,
                                goals = goals,
                                activity = activity,
                                gamification = gamification
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
