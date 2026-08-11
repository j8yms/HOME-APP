package com.example.householdapp.finance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.Budget
import com.example.householdapp.core.model.LedgerEntry
import com.example.householdapp.core.model.SubscriptionInfo
import com.example.householdapp.core.network.AnalyticsCategory
import com.example.householdapp.core.network.TransferRecord
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.EmptyState
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.formatTimestamp
import java.time.Instant
import java.time.ZoneOffset

private const val WALLET_SOURCE_OPTIONS = "his,hers,joint"
private const val TRANSFER_TARGET_OPTIONS = "joint,vacation,dream"

@Composable
private fun currentUserId(): String =
    SessionManager.sessionState.collectAsState().value.user?.userId ?: ""

@Composable
private fun FinanceRefreshHeader(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    GradientHeader(
        title = title,
        subtitle = subtitle,
        actions = {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.White.copy(alpha = 0.15f)
            ) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            }
        }
    )
}

@Composable
private fun FinanceStateMessages(errorMessage: String?, successMessage: String?) {
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
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun formatMoney(amount: Double): String {
    val session by SessionManager.sessionState.collectAsState()
    return CurrencyCatalog.formatMoney(amount, session.currency)
}

@Composable
fun AnalyticsScreen(
    viewModel: FinanceFeaturesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = currentUserId()

    LaunchedEffect(userId) { if (userId.isNotBlank()) viewModel.loadAll(userId) }

    Column(modifier = Modifier.fillMaxSize()) {
        FinanceRefreshHeader(
            title = "Analytics",
            subtitle = "Category spending insights",
            isLoading = uiState.isLoading,
            onRefresh = { if (userId.isNotBlank()) viewModel.loadAll(userId) }
        )
        FinanceStateMessages(uiState.errorMessage, uiState.successMessage)

        when {
            uiState.isLoading && uiState.analytics == null -> LoadingBox()
            uiState.analytics?.categories.isNullOrEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.PieChart,
                    title = "No spending data yet",
                    subtitle = "Add income or expenses to see category insights.",
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                val analytics = uiState.analytics ?: return@Column
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    analytics.overview?.let { overview ->
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Combined total spent",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatMoney(overview.combinedTotal),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    overview.individualBreakdowns.forEach { member ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = member.displayName.ifBlank { member.userId },
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = formatMoney(member.totalSpent),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(analytics.categories, key = { it.category }) { category ->
                        CategoryBar(category)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(category: AnalyticsCategory) {
    val progress = if (category.expenditurePct > 0) {
        (category.expenditurePct / 100.0).toFloat().coerceIn(0f, 1f)
    } else 0f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatMoney(category.totalSpent),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@Composable
fun LedgerScreen(
    viewModel: FinanceFeaturesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = currentUserId()

    LaunchedEffect(userId) { if (userId.isNotBlank()) viewModel.loadAll(userId) }

    Column(modifier = Modifier.fillMaxSize()) {
        FinanceRefreshHeader(
            title = "Ledger",
            subtitle = "Household money timeline",
            isLoading = uiState.isLoading,
            onRefresh = { if (userId.isNotBlank()) viewModel.loadAll(userId) }
        )
        FinanceStateMessages(uiState.errorMessage, uiState.successMessage)

        when {
            uiState.isLoading && uiState.ledger.isEmpty() -> LoadingBox()
            uiState.ledger.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No ledger entries yet",
                    subtitle = "Transactions and transfers will appear here.",
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.ledger, key = { it.ledgerId }) { entry ->
                        LedgerEntryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerEntryCard(entry: LedgerEntry) {
    val isOut = entry.direction == "out"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.description.ifBlank { entry.actionType.ifBlank { entry.category.ifBlank { "Ledger entry" } } },
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entry.category.isNotBlank()) {
                        Text(
                            text = entry.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (entry.timestamp.isNotBlank()) {
                        Text(
                            text = formatTimestamp(entry.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Text(
                text = (if (isOut) "-" else "+") + formatMoney(entry.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isOut) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: FinanceFeaturesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = currentUserId()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) { if (userId.isNotBlank()) viewModel.loadAll(userId) }

    Column(modifier = Modifier.fillMaxSize()) {
        FinanceRefreshHeader(
            title = "Budgets",
            subtitle = "Envelope budgeting tracking",
            isLoading = uiState.isLoading,
            onRefresh = { if (userId.isNotBlank()) viewModel.loadAll(userId) }
        )
        FinanceStateMessages(uiState.errorMessage, uiState.successMessage)

        when {
            uiState.isLoading && uiState.budgets.isEmpty() -> LoadingBox()
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddDialog = true },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add budget")
                        }
                    }
                    if (uiState.budgets.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.DateRange,
                                title = "No budgets yet",
                                subtitle = "Set a monthly budget per category.",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        items(uiState.budgets, key = { it.budgetId }) { budget ->
                            BudgetCard(budget)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddBudgetDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { showAddDialog = false },
            onSubmit = { category, month, limit ->
                if (userId.isNotBlank()) {
                    viewModel.createBudget(userId, category, month, limit) { showAddDialog = false }
                }
            }
        )
    }
}

@Composable
private fun BudgetCard(budget: Budget) {
    val progress = if (budget.budgetLimit > 0) {
        (budget.currentSpent / budget.budgetLimit).toFloat().coerceIn(0f, 1f)
    } else 0f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = budget.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Month: ${budget.month}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${formatMoney(budget.currentSpent)} / ${formatMoney(budget.budgetLimit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (category: String, month: String, budgetLimit: Double) -> Unit
) {
    var category by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Groceries)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = month,
                    onValueChange = { month = it },
                    label = { Text("Month (yyyy-MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Budget limit") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    if (category.isNotBlank() && month.isNotBlank() && limit > 0) {
                        onSubmit(category.trim(), month.trim(), limit)
                    }
                },
                enabled = !isSubmitting && category.isNotBlank() && month.isNotBlank() && (limitText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(if (isSubmitting) "Saving..." else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: FinanceFeaturesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = currentUserId()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) { if (userId.isNotBlank()) viewModel.loadAll(userId) }

    Column(modifier = Modifier.fillMaxSize()) {
        FinanceRefreshHeader(
            title = "Subscriptions",
            subtitle = "Recurring subscriptions control matrix",
            isLoading = uiState.isLoading,
            onRefresh = { if (userId.isNotBlank()) viewModel.loadAll(userId) }
        )
        FinanceStateMessages(uiState.errorMessage, uiState.successMessage)

        when {
            uiState.isLoading && uiState.subscriptions.isEmpty() -> LoadingBox()
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddDialog = true },
                            enabled = !uiState.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add subscription")
                        }
                    }
                    if (uiState.subscriptions.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Filled.DateRange,
                                title = "No subscriptions yet",
                                subtitle = "Track recurring bills like streaming services.",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        items(uiState.subscriptions, key = { it.subscriptionId }) { subscription ->
                            SubscriptionCard(subscription, uiState.isSubmitting) { isActive ->
                                viewModel.toggleSubscription(userId, subscription.subscriptionId, isActive)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { showAddDialog = false },
            onSubmit = { serviceName, nextRenewalDate, billingInterval, terminationRule ->
                if (userId.isNotBlank()) {
                    viewModel.createSubscription(userId, serviceName, nextRenewalDate, billingInterval, terminationRule) { showAddDialog = false }
                }
            }
        )
    }
}

@Composable
private fun SubscriptionCard(
    subscription: SubscriptionInfo,
    isSubmitting: Boolean,
    onToggleActive: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = subscription.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Renews ${subscription.nextRenewalDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subscription.billingInterval.isNotBlank()) {
                    Text(
                        text = subscription.billingInterval,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = subscription.isActive,
                onCheckedChange = onToggleActive,
                enabled = !isSubmitting
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubscriptionDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (serviceName: String, nextRenewalDate: String, billingInterval: String, terminationRule: String) -> Unit
) {
    var serviceName by remember { mutableStateOf("") }
    var nextRenewalDate by remember { mutableStateOf("") }
    var billingInterval by remember { mutableStateOf("") }
    var terminationRule by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subscription") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Service name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (nextRenewalDate.isBlank()) "Pick next renewal date" else "Renews $nextRenewalDate")
                }
                OutlinedTextField(
                    value = billingInterval,
                    onValueChange = { billingInterval = it },
                    label = { Text("Billing interval (e.g. monthly)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = terminationRule,
                    onValueChange = { terminationRule = it },
                    label = { Text("Termination rule (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (serviceName.isNotBlank() && nextRenewalDate.isNotBlank()) {
                        onSubmit(serviceName.trim(), nextRenewalDate, billingInterval.trim(), terminationRule.trim())
                    }
                },
                enabled = !isSubmitting && serviceName.isNotBlank() && nextRenewalDate.isNotBlank()
            ) {
                Text(if (isSubmitting) "Saving..." else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            nextRenewalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransfersScreen(
    viewModel: FinanceFeaturesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userId = currentUserId()
    var sourceAccount by remember { mutableStateOf("joint") }
    var destinationTarget by remember { mutableStateOf("vacation") }
    var amountText by remember { mutableStateOf("") }

    LaunchedEffect(userId) { if (userId.isNotBlank()) viewModel.loadAll(userId) }

    Column(modifier = Modifier.fillMaxSize()) {
        FinanceRefreshHeader(
            title = "Transfers",
            subtitle = "Move money between accounts",
            isLoading = uiState.isLoading,
            onRefresh = { if (userId.isNotBlank()) viewModel.loadAll(userId) }
        )
        FinanceStateMessages(uiState.errorMessage, uiState.successMessage)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "New transfer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Source account", style = MaterialTheme.typography.labelMedium)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                WALLET_SOURCE_OPTIONS.split(",").forEach { option ->
                                    FilterChip(
                                        selected = sourceAccount == option,
                                        onClick = { sourceAccount = option },
                                        label = { Text(option.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Destination target", style = MaterialTheme.typography.labelMedium)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TRANSFER_TARGET_OPTIONS.split(",").forEach { option ->
                                    FilterChip(
                                        selected = destinationTarget == option,
                                        onClick = { destinationTarget = option },
                                        label = { Text(option.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                if (userId.isNotBlank()) {
                                    viewModel.executeTransfer(
                                        userId = userId,
                                        sourceAccount = sourceAccount,
                                        destinationTarget = destinationTarget,
                                        amount = amount,
                                        onSuccess = { amountText = "" },
                                        onError = {}
                                    )
                                }
                            },
                            enabled = !uiState.isSubmitting && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (uiState.isSubmitting) "Processing..." else "Transfer")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Transfer history",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.transfers.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.SwapHoriz,
                        title = "No transfers yet",
                        subtitle = "Transfers between wallets and funds will appear here.",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(uiState.transfers, key = { it.transferId }) { transfer ->
                    TransferCard(transfer)
                }
            }
        }
    }
}

@Composable
private fun TransferCard(transfer: TransferRecord) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${transfer.sourceAccount.replaceFirstChar { it.uppercase() }} to ${transfer.destinationTarget.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatTimestamp(transfer.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatMoney(transfer.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
