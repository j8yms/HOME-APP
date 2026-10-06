package com.example.householdapp.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.ActivityEntry
import com.example.householdapp.core.model.Bill
import com.example.householdapp.core.model.DashboardData
import com.example.householdapp.core.model.Event
import com.example.householdapp.core.model.MaintenanceItem
import com.example.householdapp.core.model.QuickActionsData
import com.example.householdapp.core.model.Reminder
import com.example.householdapp.core.model.SavingsGoal
import com.example.householdapp.core.model.ShoppingItem
import com.example.householdapp.core.model.SubscriptionInfo
import com.example.householdapp.core.model.Task
import com.example.householdapp.core.model.UserProfile
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.Avatar
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.LevelRing
import com.example.householdapp.core.ui.components.Pill
import com.example.householdapp.core.ui.components.SectionHeader
import com.example.householdapp.core.ui.components.TaskRow
import com.example.householdapp.core.ui.theme.HouseholdTheme
import java.time.LocalTime
import kotlin.math.roundToInt

private fun xpForNextLevel(currentLevel: Int): Int = 100 + (currentLevel * 50)

private fun greeting(): String {
    return when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun firstName(name: String): String = name.trim().substringBefore(' ').ifBlank { name }

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val sessionState by SessionManager.sessionState.collectAsState()
    val uiState by dashboardViewModel.uiState.collectAsState()

    LaunchedEffect(
        sessionState.user?.userId,
        sessionState.user?.xpTotal,
        sessionState.user?.coinsTotal,
        sessionState.user?.currentStreak
    ) {
        sessionState.user?.userId?.let { userId ->
            dashboardViewModel.loadDashboard(userId, forceRefresh = true)
        }
    }

    val dashboardData = uiState.dashboardData
    val user = dashboardData.currentUser ?: sessionState.user

    DashboardContent(
        user = user,
        uiState = uiState,
        onRefresh = {
            sessionState.user?.userId?.let { userId ->
                dashboardViewModel.loadDashboard(userId, forceRefresh = true)
            }
        }
    )
}

@Composable
fun DashboardContent(
    user: UserProfile?,
    uiState: DashboardUiState,
    onRefresh: () -> Unit
) {
    val dashboardData = uiState.dashboardData

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(
            title = if (user != null) "${greeting()}, ${firstName(user.displayName)}" else "Dashboard",
            subtitle = if (user != null) "Level ${user.level} \u00b7 ${user.xpTotal} XP" else "Loading your household...",
            actions = {
                IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            }
        )

        uiState.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (uiState.isLoading && user == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val data = dashboardData
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (user != null) {
                    // A. GREETING CARD
                    item {
                        GreetingCard(user = user)
                    }

                    // B. TODAY SECTION
                    item {
                        TodaySection(
                            tasksDueToday = data.today.tasksDueToday,
                            overdueTasks = data.today.overdueTasks,
                            todayBills = data.today.todayBills,
                            upcomingBills = data.today.upcomingBills,
                            overdueBills = data.today.overdueBills,
                            todayEvents = data.today.todayEvents,
                            quickActions = data.today.quickActions
                        )
                    }

                    // C. MONEY SECTION
                    item {
                        MoneySection(
                            totalBalance = data.money.totalBalance,
                            accountBreakdown = data.money.accountBreakdown,
                            incomeThisMonth = data.money.incomeThisMonth,
                            expensesThisMonth = data.money.expensesThisMonth,
                            remainingBudget = data.money.remainingBudget,
                            subscriptionTotalMonthly = data.money.subscriptionTotalMonthly,
                            upcomingSubscriptions = data.money.upcomingSubscriptions
                        )
                    }

                    // D. HOUSEHOLD SECTION
                    item {
                        HouseholdSection(
                            openTasks = data.household.openTasks,
                            completedTasks = data.household.completedTasks,
                            maintenanceIssues = data.household.maintenanceIssues,
                            shoppingItems = data.household.shoppingItems,
                            importantReminders = data.household.importantReminders
                        )
                    }

                    // E. GOALS SECTION
                    item {
                        GoalsSection(savingsGoals = data.goals.savingsGoals, monthlyTargets = data.goals.monthlyTargets)
                    }

                    // F. ACTIVITY SECTION
                    item {
                        ActivityFeedSection(entries = data.activity.entries)
                    }

                    // G. GAMIFICATION SECTION (secondary)
                    item {
                        GamificationSection(
                            xpTotal = data.gamification.xpTotal,
                            level = data.gamification.level,
                            coinsTotal = data.gamification.coinsTotal,
                            currentStreak = data.gamification.currentStreak,
                            longestStreak = data.gamification.longestStreak
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelProgressCard(user: UserProfile) {
    val nextLevelXp = xpForNextLevel(user.level)
    val progress = (user.xpTotal.toFloat() / nextLevelXp).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LevelRing(
                progress = progress,
                ringColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                strokeWidth = 8.dp,
                modifier = Modifier.size(72.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${user.level}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "LVL",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${user.xpTotal} XP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                )
                Text(
                    text = "${nextLevelXp - user.xpTotal} XP to level ${user.level + 1}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PartnerCard(partner: UserProfile) {
    val nextLevelXp = xpForNextLevel(partner.level)
    val progress = (partner.xpTotal.toFloat() / nextLevelXp).coerceIn(0f, 1f)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Avatar(name = partner.displayName, size = 44.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = partner.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Pill(
                        text = "${partner.currentStreak} day streak",
                        icon = Icons.Filled.LocalFireDepartment,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "Level ${partner.level} \u00b7 ${partner.xpTotal} XP \u00b7 ${partner.coinsTotal} coins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CouplesStreakCard(current: Int, longest: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$current day team streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Both partners completed a task today. Longest: $longest days together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    HouseholdTheme {
        DashboardContent(
            user = UserProfile(
                userId = "u1",
                email = "user@example.com",
                displayName = "John Doe",
                role = "member",
                xpTotal = 1250,
                level = 5,
                coinsTotal = 450,
                currentStreak = 3,
                longestStreak = 7
            ),
            uiState = DashboardUiState(
                dashboardData = DashboardData(
                    partner = UserProfile(
                        userId = "u2",
                        email = "partner@example.com",
                        displayName = "Jane Smith",
                        role = "member",
                        xpTotal = 900,
                        level = 4,
                        coinsTotal = 300,
                        currentStreak = 5,
                        longestStreak = 5
                    ),
                    tasksDueToday = listOf(
                        Task(taskId = "t1", title = "Wash dishes", description = "", category = "General", assignedToUserId = "u1", createdByUserId = "u1", status = "pending", priority = "medium", dueDate = "2024-07-20", repeatRule = null, xpReward = 50, coinReward = 10, streakEligible = true, version = 1),
                        Task(taskId = "t2", title = "Take out the trash", description = "", category = "Cleaning", assignedToUserId = "u2", createdByUserId = "u1", status = "pending", priority = "high", dueDate = "2024-07-20", repeatRule = null, xpReward = 80, coinReward = 15, streakEligible = true, version = 1)
                    ),
                    overdueTasks = listOf(
                        Task(taskId = "t3", title = "Fix kitchen faucet", description = "", category = "Maintenance", assignedToUserId = "u1", createdByUserId = "u2", status = "pending", priority = "high", dueDate = "2024-07-18", repeatRule = null, xpReward = 80, coinReward = 15, streakEligible = true, version = 1)
                    )
                )
            ),
            onRefresh = {}
        )
    }
}

// A. Greeting Card
@Composable
fun GreetingCard(user: UserProfile?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Avatar(name = user?.displayName ?: "User", size = 48.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = user?.displayName ?: "User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Level ${user?.level ?: 1} \u00b7 ${user?.xpTotal ?: 0} XP \u00b7 ${user?.coinsTotal ?: 0} coins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${user?.xpTotal ?: 0}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// B. Today Section
@Composable
fun TodaySection(
    tasksDueToday: List<Task>,
    overdueTasks: List<Task>,
    todayBills: List<Bill>,
    upcomingBills: List<Bill>,
    overdueBills: List<Bill>,
    todayEvents: List<Event>,
    quickActions: QuickActionsData
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tasks due today
            SectionHeader(title = "Tasks due today", count = tasksDueToday.size)
            if (tasksDueToday.isEmpty()) {
                Text(text = "Nothing due today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                tasksDueToday.forEach { task ->
                    TaskRow(task = task)
                }
            }

            // Overdue tasks
            SectionHeader(title = "Overdue", count = overdueTasks.size)
            if (overdueTasks.isEmpty()) {
                Text(text = "All caught up", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                overdueTasks.forEach { task ->
                    TaskRow(task = task)
                }
            }

            // Today's bills
            SectionHeader(title = "Today's bills", count = todayBills.size)
            if (todayBills.isEmpty()) {
                Text(text = "No bills due today \uD83C\uDF89", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                todayBills.forEach { bill ->
                    BillRow(bill = bill)
                }
            }

            // Upcoming bills
            SectionHeader(title = "Upcoming bills", count = upcomingBills.size)
            if (upcomingBills.isEmpty()) {
                Text(text = "No upcoming bills", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                upcomingBills.take(3).forEach { bill ->
                    BillRow(bill = bill, compact = true)
                }
            }

            // Today's events
            SectionHeader(title = "Today's events", count = todayEvents.size)
            if (todayEvents.isEmpty()) {
                Text(text = "No events today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                todayEvents.take(3).forEach { event ->
                    EventRow(event = event)
                }
            }

            // Quick actions
            SectionHeader(title = "Quick actions")
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pill(text = "Income: KES ${quickActions.incomeThisMonth}", containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                Pill(text = "Expenses: KES ${quickActions.expensesThisMonth}", containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                Pill(text = "Remaining: KES ${quickActions.remainingBudget.coerceAtLeast(0.0)}", containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

// C. Money Section
@Composable
fun MoneySection(
    totalBalance: Double,
    accountBreakdown: Map<String, Double>,
    incomeThisMonth: Double,
    expensesThisMonth: Double,
    remainingBudget: Double,
    subscriptionTotalMonthly: Double,
    upcomingSubscriptions: List<SubscriptionInfo>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total balance
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "KES ${totalBalance}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "KES ${remainingBudget.coerceAtLeast(0.0)} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remainingBudget >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            // Account breakdown
            Text(
                text = "Accounts:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            accountBreakdown.entries.forEach { (wallet, amount) ->
                Pill(text = "$wallet: KES ${amount}", containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
            }

            // Income/Expenses this month
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Pill(text = "Income: KES ${incomeThisMonth}", containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                Pill(text = "Expenses: KES ${expensesThisMonth}", containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            }

            // Subscription total
            Pill(text = "Subscriptions: KES ${subscriptionTotalMonthly}", containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)

            // Upcoming subscriptions
            if (upcomingSubscriptions.isNotEmpty()) {
                Text(
                    text = "Renewing soon:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                upcomingSubscriptions.take(3).forEach { sub ->
                    Pill(text = "${sub.serviceName} (next: ${sub.nextRenewalDate})", containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// D. Household Section
@Composable
fun HouseholdSection(
    openTasks: List<Task>,
    completedTasks: List<Task>,
    maintenanceIssues: List<MaintenanceItem>,
    shoppingItems: List<ShoppingItem>,
    importantReminders: List<Reminder>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Open tasks
            SectionHeader(title = "Open tasks", count = openTasks.size)
            if (openTasks.isEmpty()) {
                Text(text = "Nothing needs doing today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                openTasks.forEach { task ->
                    TaskRow(task = task)
                }
            }

            // Completed tasks
            SectionHeader(title = "Completed tasks", count = completedTasks.size)
            if (completedTasks.isEmpty()) {
                Text(text = "All caught up", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                completedTasks.take(3).forEach { task ->
                    TaskRow(task = task)
                }
            }

            // Maintenance issues
            SectionHeader(title = "Maintenance", count = maintenanceIssues.size)
            if (maintenanceIssues.isEmpty()) {
                Text(text = "No maintenance issues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                maintenanceIssues.take(3).forEach { item ->
                    MaintenanceRow(item = item)
                }
            }

            // Shopping items
            SectionHeader(title = "Shopping", count = shoppingItems.size)
            if (shoppingItems.isEmpty()) {
                Text(text = "Shopping list is empty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                shoppingItems.take(5).forEach { item ->
                    ShoppingItemRow(item = item)
                }
            }

            // Important reminders
            SectionHeader(title = "Reminders", count = importantReminders.size)
            if (importantReminders.isEmpty()) {
                Text(text = "No reminders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                importantReminders.take(3).forEach { reminder ->
                    ReminderRow(reminder = reminder)
                }
            }
        }
    }
}

// E. Goals Section
@Composable
fun GoalsSection(savingsGoals: List<SavingsGoal>, monthlyTargets: Map<String, Double>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Savings Goals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (savingsGoals.isEmpty()) {
                Text(
                    text = "No savings goals yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                savingsGoals.forEach { goal ->
                    GoalRow(goal = goal)
                }
            }
            
            // Monthly targets
            if (monthlyTargets.isNotEmpty()) {
                Text(
                    text = "Monthly targets:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                monthlyTargets.entries.forEach { (category, target) ->
                    Pill(text = "$category: KES $target", containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// F. Activity Feed Section
@Composable
fun ActivityFeedSection(entries: List<ActivityEntry>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recent activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (entries.isEmpty()) {
                Text(
                    text = "No recent activity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.take(5).forEach { entry ->
                    ActivityRow(entry = entry)
                }
            }
        }
    }
}

// G. Gamification Section (secondary)
@Composable
fun GamificationSection(
    xpTotal: Int,
    level: Int,
    coinsTotal: Int,
    currentStreak: Int,
    longestStreak: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // XP and level
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "XP: $xpTotal",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Pill(
                        text = "Level $level",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    LevelRing(
                        progress = if (xpForNextLevel(level) > 0) (xpTotal.toFloat() / xpForNextLevel(level)).coerceIn(0f, 1f) else 0f,
                        ringColor = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        strokeWidth = 8.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "to level $level",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Coins
            Pill(
                text = "$coinsTotal coins",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            // Streak
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Day streak: $currentStreak",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Longest: $longestStreak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper composables

@Composable
fun BillRow(bill: Bill, compact: Boolean = false) {
    val text = if (compact) "${bill.title}: KES ${bill.amount}" else "${bill.title} — KES ${bill.amount} — due ${bill.dueDate}"
    Pill(
        text = text,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun EventRow(event: Event) {
    Pill(
        text = event.title,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun MaintenanceRow(item: MaintenanceItem) {
    Pill(
        text = "${item.title} (due ${item.dueDate})",
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ShoppingItemRow(item: ShoppingItem) {
    Pill(
        text = "${item.title} (KES ${item.estimatedCost})",
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ReminderRow(reminder: Reminder) {
    Pill(
        text = reminder.title,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun GoalRow(goal: SavingsGoal) {
    val pct = if (goal.targetAmount > 0) ((goal.currentAmount / goal.targetAmount) * 100).coerceIn(0.0, 100.0).roundToInt() else 0
    Pill(
        text = "${goal.name}: KES ${goal.currentAmount}/${goal.targetAmount} ($pct%)",
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun ActivityRow(entry: ActivityEntry) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (entry.description.isNotBlank()) {
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Section header composable with default count
@Composable
fun SectionHeader(title: String, count: Int = 0) {
    Row(
        modifier = Modifier.padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        if (count > 0) {
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
