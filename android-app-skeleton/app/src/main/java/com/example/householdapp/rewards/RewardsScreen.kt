package com.example.householdapp.rewards

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FreeBreakfast
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.Redemption
import com.example.householdapp.core.model.RewardItem
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.Pill
import com.example.householdapp.core.ui.components.SectionHeader
import com.example.householdapp.core.ui.formatTimestamp

private fun rewardIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "date" -> Icons.Filled.Favorite
        "break", "coffee", "relax" -> Icons.Filled.FreeBreakfast
        "gift", "treat" -> Icons.Filled.CardGiftcard
        "game", "fun" -> Icons.Filled.SportsEsports
        else -> Icons.Filled.Redeem
    }
}

private fun redemptionStatusLabel(status: String): String {
    return when (status.lowercase()) {
        "pending_approval" -> "Pending Partner Approval"
        "approved" -> "Approved"
        "resolved" -> "Resolved"
        "redeemed" -> "Redeemed"
        "denied", "rejected" -> "Denied"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

@Composable
fun RewardsScreen(
    rewardsViewModel: RewardsViewModel = viewModel()
) {
    val sessionState by SessionManager.sessionState.collectAsState()
    val uiState by rewardsViewModel.uiState.collectAsState()
    val user = sessionState.user
    var rewardToRedeem by remember { mutableStateOf<RewardItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        rewardsViewModel.loadRewards()
        rewardsViewModel.loadRedemptions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(
            title = "Rewards",
            subtitle = if (user != null) "Spend your hard-earned coins" else "Sign in to redeem rewards",
            actions = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            IconButton(
                                onClick = { showCreateDialog = true },
                                enabled = !uiState.isSubmitting
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Create voucher",
                                    tint = Color.White
                                )
                            }
                        }
                        Pill(
                            text = "${user.coinsTotal}",
                            icon = Icons.Filled.MonetizationOn,
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        )
                        Pill(
                            text = "${user.xpTotal} XP",
                            icon = Icons.Filled.Bolt,
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        )
                    }
                }
            }
        )

        uiState.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
        uiState.successMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.rewards.isEmpty()) {
                        item {
                            Text(
                                text = "No rewards in the store right now \u2014 new rewards are added by your household.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(uiState.rewards, key = { it.rewardId }) { reward ->
                            val canRedeem = user != null &&
                                user.coinsTotal >= reward.costCoins &&
                                user.xpTotal >= reward.costXp
                            RewardCard(
                                reward = reward,
                                canRedeem = canRedeem,
                                isSubmitting = uiState.isSubmitting,
                                onRedeem = { rewardToRedeem = reward }
                            )
                        }
                    }

                    item {
                        SectionHeader(
                            title = "Your redemptions",
                            count = uiState.redemptions.size.takeIf { it > 0 },
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (uiState.redemptions.isEmpty()) {
                        item {
                            Text(
                                text = "Rewards you redeem will show up here with their status.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(uiState.redemptions, key = { it.redemptionId }) { redemption ->
                            RedemptionCard(redemption)
                        }
                    }
                }
            }
        }
    }

    rewardToRedeem?.let { reward ->
        AlertDialog(
            onDismissRequest = { if (!uiState.isSubmitting) rewardToRedeem = null },
            title = { Text("Confirm purchase") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Buy \"${reward.title}\"? Your partner will need to approve the voucher before it's earned.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(
                            text = "${reward.costCoins} coins",
                            icon = Icons.Filled.MonetizationOn,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (reward.costXp > 0) {
                            Pill(
                                text = "${reward.costXp} XP",
                                icon = Icons.Filled.Bolt,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    user?.let {
                        Text(
                            "Your balance: ${it.coinsTotal} coins, ${it.xpTotal} XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        rewardsViewModel.redeemReward(reward)
                        rewardToRedeem = null
                    },
                    enabled = !uiState.isSubmitting
                ) {
                    Text(if (uiState.isSubmitting) "Purchasing..." else "Buy")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { rewardToRedeem = null },
                    enabled = !uiState.isSubmitting
                ) { Text("Cancel") }
            }
        )
    }

    if (showCreateDialog) {
        var voucherTitle by remember { mutableStateOf("") }
        var voucherDescription by remember { mutableStateOf("") }
        var voucherCoins by remember { mutableStateOf("") }
        var voucherXp by remember { mutableStateOf("") }
        var voucherCategory by remember { mutableStateOf("Custom") }
        var hideFromPartner by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!uiState.isSubmitting) showCreateDialog = false },
            title = { Text("Create a custom voucher") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = voucherTitle,
                        onValueChange = { voucherTitle = it },
                        label = { Text("Voucher title") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = voucherDescription,
                        onValueChange = { voucherDescription = it },
                        label = { Text("Description (optional)") },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = voucherCoins,
                        onValueChange = { voucherCoins = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Cost in coins") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = voucherXp,
                        onValueChange = { voucherXp = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Cost in XP (optional)") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = voucherCategory,
                        onValueChange = { voucherCategory = it },
                        label = { Text("Category") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Keep private", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Only you can see this voucher",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = hideFromPartner,
                            onCheckedChange = { hideFromPartner = it },
                            enabled = !uiState.isSubmitting
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        rewardsViewModel.createReward(
                            title = voucherTitle,
                            description = voucherDescription,
                            costCoins = voucherCoins.toIntOrNull() ?: 0,
                            costXp = voucherXp.toIntOrNull() ?: 0,
                            category = voucherCategory,
                            hideFromPartner = hideFromPartner
                        ) {
                            showCreateDialog = false
                        }
                    },
                    enabled = !uiState.isSubmitting && voucherTitle.isNotBlank()
                ) {
                    Text(if (uiState.isSubmitting) "Creating..." else "Create")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateDialog = false },
                    enabled = !uiState.isSubmitting
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RewardCard(
    reward: RewardItem,
    canRedeem: Boolean,
    isSubmitting: Boolean,
    onRedeem: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (canRedeem) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = rewardIcon(reward.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reward.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (reward.description.isNotBlank()) {
                        Text(
                            text = reward.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        text = "${reward.costCoins} coins",
                        icon = Icons.Filled.MonetizationOn,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    if (reward.costXp > 0) {
                        Pill(
                            text = "${reward.costXp} XP",
                            icon = Icons.Filled.Bolt,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Button(
                    onClick = onRedeem,
                    enabled = canRedeem && !isSubmitting
                ) {
                    Text(
                        text = when {
                            reward.costCoins > 0 -> "Buy Voucher for ${reward.costCoins} coins"
                            reward.costXp > 0 -> "Buy Voucher for ${reward.costXp} XP"
                            else -> "Buy Voucher"
                        }
                    )
                }
            }

            if (!canRedeem && reward.costCoins > 0) {
                Text(
                    text = "Not enough coins or XP yet \u2014 keep going!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RedemptionCard(redemption: Redemption) {
    val (container, content) = when (redemption.status.lowercase()) {
        "approved", "resolved" -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        "denied", "rejected" -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        else -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = redemption.rewardTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatTimestamp(redemption.redeemedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Pill(
                    text = redemptionStatusLabel(redemption.status),
                    containerColor = container,
                    contentColor = content
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (redemption.costCoins > 0) {
                    Pill(
                        text = "${redemption.costCoins} coins",
                        icon = Icons.Filled.MonetizationOn,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                if (redemption.costXp > 0) {
                    Pill(
                        text = "${redemption.costXp} XP",
                        icon = Icons.Filled.Bolt,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
