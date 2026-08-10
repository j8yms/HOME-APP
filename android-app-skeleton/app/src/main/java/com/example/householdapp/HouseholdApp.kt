package com.example.householdapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.theme.HouseholdTheme
import com.example.householdapp.navigation.AppNavGraph
import com.example.householdapp.navigation.AppRoute

@Composable
fun HouseholdApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var isDarkTheme by remember { mutableStateOf(false) }

    val sessionState by SessionManager.sessionState.collectAsState()

    LaunchedEffect(sessionState.isAuthenticated) {
        if (!sessionState.isAuthenticated && currentRoute != AppRoute.Auth.route) {
            navController.navigate(AppRoute.Auth.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    data class NavItem(val route: AppRoute, val icon: ImageVector)

    val destinations = listOf(
        NavItem(AppRoute.Dashboard, Icons.Filled.Home),
        NavItem(AppRoute.Tasks, Icons.Filled.Rule),
        NavItem(AppRoute.Workouts, Icons.Filled.FitnessCenter),
        NavItem(AppRoute.Rewards, Icons.Filled.CardGiftcard),
        NavItem(AppRoute.Money, Icons.Filled.MonetizationOn),
        NavItem(AppRoute.ActivityFeed, Icons.Filled.History),
        NavItem(AppRoute.Profile, Icons.Filled.Person)
    )

    val showBottomBar = currentRoute != AppRoute.Auth.route

    HouseholdTheme(darkTheme = isDarkTheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (showBottomBar && currentRoute == AppRoute.Dashboard.route) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(AppRoute.Tasks.route) {
                                launchSingleTop = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Quick add task"
                        )
                    }
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 0.dp
                    ) {
                        destinations.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route.route,
                                onClick = {
                                    if (currentRoute != item.route.route) {
                                        navController.navigate(item.route.route) {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                label = { Text(item.route.label) },
                                icon = { Icon(item.icon, contentDescription = item.route.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            AppNavGraph(
                navController = navController,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = { isDarkTheme = it },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
