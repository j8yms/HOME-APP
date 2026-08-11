package com.example.householdapp.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.DashboardData
import com.example.householdapp.core.model.Task
import com.example.householdapp.core.model.UserProfile
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.Avatar
import com.example.householdapp.core.ui.components.EmptyState
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.LevelRing
import com.example.householdapp.core.ui.components.Pill
import com.example.householdapp.core.ui.components.SectionHeader
import com.example.householdapp.core.ui.components.StatCard
import com.example.householdapp.core.ui.components.TaskRow
import com.example.householdapp.core.ui.theme.HouseholdTheme
import java.time.LocalTime

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (user != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                value = "${user.xpTotal}",
                                label = "XP",
                                icon = Icons.Filled.Bolt,
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                value = "${user.coinsTotal}",
                                label = "Coins",
                                icon = Icons.Filled.MonetizationOn,
                                accent = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                value = "${user.currentStreak}",
                                label = "Day streak",
                                icon = Icons.Filled.LocalFireDepartment,
                                accent = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        LevelProgressCard(user = user)
                    }

                    dashboardData.partner?.let { partner ->
                        item {
                            PartnerCard(partner = partner)
                        }
                    }
                }

                item {
                    CouplesStreakCard(
                        current = dashboardData.couplesStreak.currentStreak,
                        longest = dashboardData.couplesStreak.longestStreak
                    )
                }

                item {
                    SectionHeader(
                        title = "Today's tasks",
                        count = dashboardData.tasksDueToday.size
                    )
                }
                if (dashboardData.tasksDueToday.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Schedule,
                            title = "Nothing due today",
                            subtitle = "Enjoy the free time \u2014 or plan ahead.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    items(dashboardData.tasksDueToday, key = { it.taskId }) { task ->
                        TaskRow(task = task)
                    }
                }

                item {
                    SectionHeader(
                        title = "Overdue",
                        count = dashboardData.overdueTasks.size
                    )
                }
                if (dashboardData.overdueTasks.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.CheckCircle,
                            title = "All caught up",
                            subtitle = "No overdue tasks. Great teamwork!",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    items(dashboardData.overdueTasks, key = { it.taskId }) { task ->
                        TaskRow(task = task)
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
                        Task("t1", "Wash dishes", "", "General", "u1", "u1", "pending", "medium", "2024-07-20", null, 50, 10, true, 1),
                        Task("t2", "Take out the trash", "", "Cleaning", "u2", "u1", "pending", "high", "2024-07-20", null, 80, 15, true, 1)
                    ),
                    overdueTasks = listOf(
                        Task("t3", "Fix kitchen faucet", "", "Maintenance", "u1", "u2", "pending", "high", "2024-07-18", null, 80, 15, true, 1)
                    )
                )
            ),
            onRefresh = {}
        )
    }
}
