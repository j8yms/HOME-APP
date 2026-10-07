package com.example.householdapp.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.navigation.AppRoute

@Composable
fun MoreScreen(
    onNavigateTo: (AppRoute) -> Unit,
    onOpenReference: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(
            title = "More Features",
            subtitle = "Secondary destinations & household tools"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category 1: Financial Analytics & Tools
            item {
                MoreSectionHeader(title = "Financial Analytics & Operations")
            }
            item {
                MoreGrid(
                    items = listOf(
                        MoreItem("Analytics", "Category breakdown", Icons.Filled.PieChart) { onNavigateTo(AppRoute.Analytics) },
                        MoreItem("Ledger", "Transaction logs", Icons.Filled.ReceiptLong) { onNavigateTo(AppRoute.Ledger) },
                        MoreItem("Budgets", "Monthly limits", Icons.Filled.DateRange) { onNavigateTo(AppRoute.Budgets) },
                        MoreItem("Subscriptions", "Recurring services", Icons.Filled.EventNote) { onNavigateTo(AppRoute.Subscriptions) },
                        MoreItem("Transfers", "Move money", Icons.Filled.SwapHoriz) { onNavigateTo(AppRoute.Transfers) }
                    )
                )
            }

            // Category 2: Household & Activity
            item {
                MoreSectionHeader(title = "Household & Lifestyle")
            }
            item {
                MoreGrid(
                    items = listOf(
                        MoreItem("Activity", "Recent updates", Icons.Filled.History) { onNavigateTo(AppRoute.ActivityFeed) },
                        MoreItem("Workouts", "Fitness tracking", Icons.Filled.FitnessCenter) { onNavigateTo(AppRoute.Workouts) },
                        MoreItem("Rewards", "Coins & rewards", Icons.Filled.CardGiftcard) { onNavigateTo(AppRoute.Rewards) },
                        MoreItem("Household Log", "Event logging", Icons.Filled.ListAlt) { onNavigateTo(AppRoute.HouseholdLog) },
                        MoreItem("Library", "Directories & notes", Icons.Filled.Book) { onOpenReference("maintenance") }
                    )
                )
            }

            // Category 3: Account
            item {
                MoreSectionHeader(title = "Account & Settings")
            }
            item {
                MoreGrid(
                    items = listOf(
                        MoreItem("Profile", "Household & partner settings", Icons.Filled.Person) { onNavigateTo(AppRoute.Profile) }
                    )
                )
            }
        }
    }
}

private data class MoreItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun MoreSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun MoreGrid(items: List<MoreItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (index < items.size - 1) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ) {}
                }
            }
        }
    }
}
