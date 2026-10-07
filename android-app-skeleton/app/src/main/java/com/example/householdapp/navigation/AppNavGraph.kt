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
import com.example.householdapp.finance.AnalyticsScreen
import com.example.householdapp.finance.BudgetsScreen
import com.example.householdapp.finance.FinanceScreen
import com.example.householdapp.finance.LedgerScreen
import com.example.householdapp.finance.SubscriptionsScreen
import com.example.householdapp.finance.TransfersScreen
import com.example.householdapp.householdlog.HouseholdLogScreen
import com.example.householdapp.more.MoreScreen
import com.example.householdapp.profile.ProfileScreen
import com.example.householdapp.reference.ReferenceScreen
import com.example.householdapp.rewards.RewardsScreen
import com.example.householdapp.tasks.TasksScreen
import com.example.householdapp.wealth.WealthScreen
import com.example.householdapp.workouts.WorkoutsScreen

sealed class AppRoute(val route: String, val label: String) {
    data object Auth : AppRoute("auth", "Auth")
    data object Dashboard : AppRoute("dashboard", "Home")
    data object Wealth : AppRoute("wealth", "Wealth")
    data object Tasks : AppRoute("tasks", "Tasks")
    data object Money : AppRoute("money", "Money")
    data object More : AppRoute("more", "More")
    data object Workouts : AppRoute("workouts", "Workouts")
    data object Rewards : AppRoute("rewards", "Rewards")
    data object Analytics : AppRoute("analytics", "Analytics")
    data object Ledger : AppRoute("ledger", "Ledger")
    data object Budgets : AppRoute("budgets", "Budgets")
    data object Subscriptions : AppRoute("subscriptions", "Subscriptions")
    data object Transfers : AppRoute("transfers", "Transfers")
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
        composable(AppRoute.Dashboard.route) { DashboardScreen() }
        composable(AppRoute.Wealth.route) { WealthScreen() }
        composable(AppRoute.Tasks.route) { TasksScreen() }
        composable(AppRoute.Money.route) { FinanceScreen() }
        composable(AppRoute.More.route) {
            MoreScreen(
                onNavigateTo = { route -> navController.navigate(route.route) },
                onOpenReference = { category -> navController.navigate(AppRoute.Reference.withCategory(category)) }
            )
        }
        composable(AppRoute.Workouts.route) { WorkoutsScreen() }
        composable(AppRoute.Rewards.route) { RewardsScreen() }
        composable(AppRoute.Analytics.route) { AnalyticsScreen() }
        composable(AppRoute.Ledger.route) { LedgerScreen() }
        composable(AppRoute.Budgets.route) { BudgetsScreen() }
        composable(AppRoute.Subscriptions.route) { SubscriptionsScreen() }
        composable(AppRoute.Transfers.route) { TransfersScreen() }
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
