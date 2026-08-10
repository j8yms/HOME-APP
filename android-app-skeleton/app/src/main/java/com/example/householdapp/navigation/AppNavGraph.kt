package com.example.householdapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.householdapp.activity.ActivityFeedScreen
import com.example.householdapp.auth.AuthScreen
import com.example.householdapp.dashboard.DashboardScreen
import com.example.householdapp.finance.FinanceScreen
import com.example.householdapp.householdlog.HouseholdLogScreen
import com.example.householdapp.profile.ProfileScreen
import com.example.householdapp.reference.ReferenceScreen
import com.example.householdapp.rewards.RewardsScreen
import com.example.householdapp.tasks.TasksScreen
import com.example.householdapp.workouts.WorkoutsScreen

sealed class AppRoute(val route: String, val label: String) {
    data object Auth : AppRoute("auth", "Auth")
    data object Dashboard : AppRoute("dashboard", "Dashboard")
    data object Tasks : AppRoute("tasks", "Tasks")
    data object Workouts : AppRoute("workouts", "Workouts")
    data object Rewards : AppRoute("rewards", "Rewards")
    data object Money : AppRoute("money", "Money")
    data object HouseholdLog : AppRoute("household_log", "Log")
    data object ActivityFeed : AppRoute("activity_feed", "Activity")
    data object Profile : AppRoute("profile", "Profile")
    data object Reference : AppRoute("reference/{category}", "Directories") {
        fun withCategory(category: String) = "reference/$category"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Auth.route,
        modifier = modifier
    ) {
        composable(AppRoute.Auth.route) {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(AppRoute.Dashboard.route) {
                        popUpTo(AppRoute.Auth.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.Dashboard.route) {
            DashboardScreen()
        }
        composable(AppRoute.Tasks.route) { TasksScreen() }
        composable(AppRoute.Workouts.route) { WorkoutsScreen() }
        composable(AppRoute.Rewards.route) { RewardsScreen() }
        composable(AppRoute.Money.route) { FinanceScreen() }
        composable(AppRoute.HouseholdLog.route) { HouseholdLogScreen() }
        composable(AppRoute.ActivityFeed.route) { ActivityFeedScreen() }
        composable(AppRoute.Profile.route) {
            ProfileScreen(
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                onOpenReference = { category ->
                    navController.navigate(AppRoute.Reference.withCategory(category))
                }
            )
        }
        composable(
            route = AppRoute.Reference.route,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = "maintenance"
                }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "maintenance"
            ReferenceScreen(category = category)
        }
    }
}
