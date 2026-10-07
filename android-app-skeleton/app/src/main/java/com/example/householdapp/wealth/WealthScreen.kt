package com.example.householdapp.wealth

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.CurrencyCatalog
import com.example.householdapp.core.network.WealthItem
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.EmptyState
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.Pill
import com.example.householdapp.core.ui.components.SectionHeader
import com.example.householdapp.core.ui.components.StatCard
import kotlinx.coroutines.delay
import java.util.Locale

private const val POSITIVE_GREEN = 0xFF2E7D32

private fun money(amount: Double, currency: String): String =
    CurrencyCatalog.formatMoney(amount, currency)

private fun signedMoney(amount: Double, currency: String): String {
    val sign = if (amount > 0.0) "+" else if (amount < 0.0) "-" else ""
    return sign + CurrencyCatalog.formatMoney(amount, currency)
}

private fun pctText(value: Double): String = "%.1f%%".format(Locale.US, value)

private fun plainNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

private fun itemTitle(item: WealthItem, section: WealthSection): String {
    val candidate = item.name.ifBlank {
        item.type.ifBlank {
            item.provider.ifBlank {
                item.symbol.ifBlank { item.reference.ifBlank { "" } }
            }
        }
    }
    return candidate.ifBlank { section.singular }
}

private fun itemDetails(item: WealthItem, section: WealthSection, currency: String): String {
    val parts = mutableListOf<String>()
    when (section) {
        WealthSection.INVESTMENTS -> {
            if (item.type.isNotBlank()) parts += item.type
            if (item.provider.isNotBlank()) parts += item.provider
            if (item.yieldPct != 0.0) parts += "${pctText(item.yieldPct)} yield"
            if (item.targetValue != 0.0) parts += "Invested ${money(item.targetValue, currency)}"
        }

        WealthSection.ASSETS -> {
            if (item.type.isNotBlank()) parts += item.type
        }

        WealthSection.LIABILITIES -> {
            if (item.type.isNotBlank()) parts += item.type
            if (item.interestRate != 0.0) parts += "${pctText(item.interestRate)} interest"
            if (item.monthlyAmount != 0.0) parts += "Min ${money(item.monthlyAmount, currency)}"
        }

        WealthSection.MILESTONES -> {
            parts += if (item.achieved) "Achieved" else "In progress"
            if (item.targetValue != 0.0) parts += "Target ${money(item.targetValue, currency)}"
            if (item.targetDate.isNotBlank()) parts += "By ${item.targetDate}"
        }

        WealthSection.INCOME -> {
            if (item.type.isNotBlank()) parts += item.type
            val status = item.status.lowercase()
            if (status.isNotBlank() && status != "active") parts += item.status
        }

        WealthSection.ACCOUNTS -> {
            if (item.type.isNotBlank()) parts += item.type
            if (item.provider.isNotBlank()) parts += item.provider
        }

        WealthSection.TRADES -> {
            if (item.action.isNotBlank()) parts += item.action
            if (item.quantity != 0.0) parts += "Qty ${plainNumber(item.quantity)}"
            if (item.price != 0.0) parts += "Price ${money(item.price, currency)}"
            if (item.date.isNotBlank()) parts += item.date
            if (item.strategy.isNotBlank()) parts += item.strategy
        }

        WealthSection.CONTRIBUTIONS -> {
            if (item.transactionType.isNotBlank()) {
                parts += item.transactionType.replaceFirstChar { it.uppercase() }
            }
            if (item.date.isNotBlank()) parts += item.date
        }

        WealthSection.PROP_ACCOUNTS -> {
            if (item.provider.isNotBlank()) parts += item.provider
            val status = item.status.lowercase()
            if (status.isNotBlank() && status != "active") parts += item.status
        }

        WealthSection.PROP_PAYOUTS -> {
            if (item.date.isNotBlank()) parts += item.date
            if (item.status.isNotBlank() && item.status.lowercase() != "paid") parts += item.status
        }
    }
    return parts.joinToString("  |  ")
}

private fun cardAmount(item: WealthItem, section: WealthSection, currency: String): String {
    if (section == WealthSection.TRADES) {
        val sign = if (item.pnl > 0.0) "+" else if (item.pnl < 0.0) "-" else ""
        return sign + CurrencyCatalog.formatMoney(item.pnl, currency)
    }
    return CurrencyCatalog.formatMoney(item.value, currency)
}

@Composable
private fun WealthStateMessages(errorMessage: String?, successMessage: String?) {
    errorMessage?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    successMessage?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun WealthItemCard(
    item: WealthItem,
    section: WealthSection,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = itemTitle(item, section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val details = itemDetails(item, section, currency)
                if (details.isNotBlank()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val amountColor = when {
                    section == WealthSection.TRADES && item.pnl > 0.0 -> Color(POSITIVE_GREEN)
                    section == WealthSection.TRADES && item.pnl < 0.0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = cardAmount(item, section, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit"
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WealthScreen(viewModel: WealthViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by SessionManager.sessionState.collectAsState()
    val userId = sessionState.user?.userId.orEmpty()
    val currency = uiState.summary?.currency ?: sessionState.currency

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<WealthItem?>(null) }
    var deleteTarget by remember { mutableStateOf<WealthItem?>(null) }
    var showFreedomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) viewModel.load(userId)
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        if (uiState.errorMessage != null || uiState.successMessage != null) {
            delay(4000)
            viewModel.clearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(
            title = "Wealth & Freedom",
            subtitle = "Net worth, investments & financial freedom"
        )

        WealthStateMessages(uiState.errorMessage, uiState.successMessage)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WealthSection.entries.forEach { section ->
                FilterChip(
                    selected = uiState.section == section,
                    onClick = {
                        if (userId.isNotBlank() && uiState.section != section) {
                            showAddDialog = false
                            editingItem = null
                            deleteTarget = null
                            viewModel.switchSection(userId, section)
                        }
                    },
                    label = { Text(text = section.label) }
                )
            }
        }

        if (uiState.isLoading && uiState.summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    val summary = uiState.summary
                    if (summary == null) {
                        EmptyState(
                            icon = Icons.Filled.AccountBalance,
                            title = "No wealth data yet",
                            subtitle = "Add your first entry to start tracking.",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        val totals = summary.totals
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Net worth",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = money(totals.netWorth, currency),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Pill(
                                        text = "Assets " + money(totals.assets, currency),
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                    Pill(
                                        text = "Liabilities " + money(totals.liabilities, currency),
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                    Pill(
                                        text = "Cash " + money(totals.cash, currency),
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                    Pill(
                                        text = "Investments " + money(totals.investments, currency),
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                val change = totals.monthlyChange
                                Text(
                                    text = "This month " + signedMoney(change, currency) +
                                        " (" + pctText(totals.monthlyChangePct) + ")",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        change > 0.0 -> Color(POSITIVE_GREEN)
                                        change < 0.0 -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    val freedom = uiState.summary?.freedom
                    if (freedom != null) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Financial freedom",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${freedom.progressPct}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Target passive income " +
                                        money(freedom.targetPassiveIncome, currency) + " / month",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Current passive income " +
                                        money(freedom.currentPassiveIncome, currency) + " / month",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                LinearProgressIndicator(
                                    progress = { (freedom.progressPct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Net worth target " + money(freedom.targetNetWorth, currency),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (freedom.targetDate.isNotBlank()) {
                                        Text(
                                            text = "By ${freedom.targetDate}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "Monthly contribution " + money(freedom.monthlyContribution, currency),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearMessage()
                                        showFreedomDialog = true
                                    },
                                    enabled = !uiState.isSaving,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Edit target")
                                }
                            }
                        }
                    }
                }

                val summary = uiState.summary
                if (summary != null) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                value = money(summary.totals.netWorth, currency),
                                label = "Net worth",
                                icon = Icons.Filled.AccountBalance,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                value = money(summary.totals.passiveMonthly, currency),
                                label = "Passive income / month",
                                icon = Icons.Filled.TrendingUp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatCard(
                                value = "${summary.milestones.achieved}/${summary.milestones.total}",
                                label = "Milestones achieved",
                                icon = Icons.Filled.Flag,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                value = money(summary.totals.liabilities, currency),
                                label = "Liabilities",
                                icon = Icons.Filled.CreditCard,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = uiState.section.label,
                        count = uiState.items.size,
                        trailing = {
                            IconButton(
                                onClick = {
                                    viewModel.clearMessage()
                                    showAddDialog = true
                                },
                                enabled = !uiState.isSaving
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add ${uiState.section.singular}",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }

                if (uiState.items.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            EmptyState(
                                icon = Icons.Filled.Add,
                                title = "No ${uiState.section.label.lowercase()} yet",
                                subtitle = "Add your first ${uiState.section.singular.lowercase()} to get started."
                            )
                            Button(onClick = {
                                viewModel.clearMessage()
                                showAddDialog = true
                            }) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Add ${uiState.section.singular}")
                            }
                        }
                    }
                } else {
                    items(uiState.items) { item ->
                        WealthItemCard(
                            item = item,
                            section = uiState.section,
                            currency = currency,
                            onEdit = {
                                viewModel.clearMessage()
                                editingItem = item
                            },
                            onDelete = {
                                viewModel.clearMessage()
                                deleteTarget = item
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        WealthItemDialog(
            section = uiState.section,
            initial = null,
            isSaving = uiState.isSaving,
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                if (userId.isNotBlank()) {
                    viewModel.saveItem(userId, uiState.section, item) {
                        showAddDialog = false
                    }
                }
            }
        )
    }

    editingItem?.let { original ->
        WealthItemDialog(
            section = uiState.section,
            initial = original,
            isSaving = uiState.isSaving,
            onDismiss = { editingItem = null },
            onSave = { item ->
                if (userId.isNotBlank()) {
                    viewModel.saveItem(userId, uiState.section, item) {
                        editingItem = null
                    }
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(text = "Delete ${uiState.section.singular}?") },
            text = {
                Text(text = "${itemTitle(target, uiState.section)} will be permanently removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = target.id
                        if (userId.isNotBlank() && id.isNotBlank()) {
                            viewModel.deleteItem(userId, uiState.section, id) {
                                deleteTarget = null
                            }
                        } else {
                            deleteTarget = null
                        }
                    },
                    enabled = !uiState.isSaving
                ) {
                    Text(text = if (uiState.isSaving) "Deleting..." else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }, enabled = !uiState.isSaving) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    if (showFreedomDialog) {
        uiState.summary?.freedom?.let { freedom ->
            FreedomDialog(
                freedom = freedom,
                isSaving = uiState.isSaving,
                onDismiss = { showFreedomDialog = false },
                onSave = { request ->
                    if (userId.isNotBlank()) {
                        viewModel.saveFreedom(userId, request) {
                            showFreedomDialog = false
                        }
                    }
                }
            )
        }
    }
}
