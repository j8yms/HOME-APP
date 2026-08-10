package com.example.householdapp.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.CurrencyCatalog
import com.example.householdapp.core.model.UserProfile
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.Avatar
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.LevelRing
import com.example.householdapp.core.ui.components.SectionHeader
import com.example.householdapp.core.ui.components.StatCard

private fun xpForNextLevel(currentLevel: Int): Int = 100 + (currentLevel * 50)

@Composable
fun ProfileScreen(
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    onOpenReference: (String) -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val sessionState by SessionManager.sessionState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val user = sessionState.user
    val partner = sessionState.partner
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(
            title = "Profile",
            subtitle = "Your household progress"
        )

        if (user == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sign in to view profile details.")
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                UserProfileCard(
                    user = user,
                    onEditName = {
                        viewModel.clearError()
                        showRenameDialog = true
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                    StatCard(
                        value = "${user.longestStreak}",
                        label = "Longest streak",
                        icon = Icons.Filled.TrendingUp,
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                partner?.let {
                    SectionHeader(title = "Partner")
                    PartnerCard(it)
                }

                SectionHeader(title = "Household Directories")
                DirectoryCard(
                    title = "Home Maintenance",
                    subtitle = "Plumbers, electricians, warranties & repairs",
                    icon = Icons.Filled.Build,
                    onClick = { onOpenReference("maintenance") }
                )
                DirectoryCard(
                    title = "Shared Digital Vault",
                    subtitle = "Passwords, accounts, documents & codes",
                    icon = Icons.Filled.Lock,
                    onClick = { onOpenReference("vault") }
                )
                DirectoryCard(
                    title = "Family Health Hub",
                    subtitle = "Medications, allergies, doctors & records",
                    icon = Icons.Filled.Favorite,
                    onClick = { onOpenReference("health") }
                )

                SectionHeader(title = "Settings")

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Household side", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Shows on task cards as His / Hers / Shared",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("his", "hers").forEach { side ->
                                val label = side.replaceFirstChar { it.uppercase() }
                                OutlinedButton(
                                    onClick = { viewModel.setSide(side) },
                                    enabled = !uiState.isRenaming
                                ) {
                                    Text(
                                        text = label,
                                        color = if (user.householdSide == side) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (isDarkTheme) "Enabled" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme(it) }
                        )
                    }
                }

                CurrencyCard(
                    currency = sessionState.currency,
                    onClick = {
                        viewModel.clearError()
                        showCurrencyDialog = true
                    }
                )

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Sign Out", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your data will be cleared from this device.") },
            confirmButton = {
                Button(onClick = {
                    SessionManager.clearSession()
                    showLogoutDialog = false
                }) { Text("Sign Out") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(user?.displayName ?: "") }
        AlertDialog(
            onDismissRequest = { if (!uiState.isRenaming) showRenameDialog = false },
            title = { Text("Rename Yourself") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        viewModel.clearError()
                    },
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !uiState.isRenaming,
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.rename(newName) { showRenameDialog = false }
                        }
                    },
                    enabled = !uiState.isRenaming && newName.isNotBlank()
                ) {
                    Text(if (uiState.isRenaming) "Saving..." else "Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRenameDialog = false },
                    enabled = !uiState.isRenaming
                ) { Text("Cancel") }
            }
        )
    }

    if (showCurrencyDialog) {
        val selected = sessionState.currency
        AlertDialog(
            onDismissRequest = { if (!uiState.isSettingCurrency) showCurrencyDialog = false },
            title = { Text("Local Currency") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Shown on all money amounts across the household.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CurrencyCatalog.supported.forEach { info ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !uiState.isSettingCurrency) {
                                    viewModel.setCurrency(info.code) { showCurrencyDialog = false }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selected.equals(info.code, ignoreCase = true),
                                onClick = null,
                                enabled = !uiState.isSettingCurrency
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${info.code} \u2014 ${info.label}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Text(
                                text = info.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { showCurrencyDialog = false },
                    enabled = !uiState.isSettingCurrency
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CurrencyCard(currency: String, onClick: () -> Unit) {
    val info = CurrencyCatalog.byCode(currency)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Local Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Applies to all money amounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${info.symbol} ${info.code}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UserProfileCard(user: UserProfile, onEditName: () -> Unit) {
    val nextLevelXp = xpForNextLevel(user.level)
    val progress = (user.xpTotal.toFloat() / nextLevelXp).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(name = user.displayName, size = 64.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(onClick = onEditName) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Rename",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (user.email.isNotBlank()) {
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }
                LevelRing(
                    progress = progress,
                    ringColor = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    strokeWidth = 7.dp,
                    modifier = Modifier.size(64.dp)
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
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    value = "${user.xpTotal}",
                    label = "XP",
                    icon = Icons.Filled.Bolt,
                    accent = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            )
            Text(
                text = "${nextLevelXp - user.xpTotal} XP to level ${user.level + 1}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DirectoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    Text(
                        text = "Level ${partner.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${partner.xpTotal} XP \u00b7 ${partner.coinsTotal} coins \u00b7 ${partner.currentStreak} day streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
