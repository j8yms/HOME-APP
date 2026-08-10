package com.example.householdapp.finance

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.Bill
import com.example.householdapp.core.model.CurrencyCatalog
import com.example.householdapp.core.model.MoneyTransaction
import com.example.householdapp.core.model.ShoppingItem
import com.example.householdapp.core.model.WalletBreakdown
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.Avatar
import com.example.householdapp.core.ui.components.EmptyState
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.Pill
import java.time.Instant
import java.time.ZoneOffset

private val categoryOptions = listOf("Utilities", "Groceries", "Date Night", "Subscriptions", "Miscellaneous")
private val walletOptions = listOf(
    "joint" to "Joint Household",
    "his" to "His Wallet",
    "hers" to "Her Wallet"
)

@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by SessionManager.sessionState.collectAsState()
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showBillDialog by remember { mutableStateOf(false) }
    var showShoppingDialog by remember { mutableStateOf(false) }
    var showGoalsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GradientHeader(
                title = "Money",
                subtitle = "Household balance, wallets & goals",
                actions = {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.clearMessage()
                                viewModel.refresh()
                            },
                            enabled = !uiState.isLoading
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
                uiState.isLoading && uiState.summary.recentTransactions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            SummaryCards(
                                balance = uiState.summary.balance,
                                savings = uiState.summary.savings
                            )
                        }
                        item {
                            WalletCards(wallets = uiState.summary.wallets)
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearMessage()
                                        showBillDialog = true
                                    },
                                    enabled = !uiState.isSubmitting,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.EventNote, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Bill")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearMessage()
                                        showShoppingDialog = true
                                    },
                                    enabled = !uiState.isSubmitting,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Shopping")
                                }
                            }
                        }
                        item {
                            GoalCard(
                                emoji = "\u2708\uFE0F",
                                title = "Vacation Fund",
                                current = uiState.summary.balance,
                                goal = uiState.summary.goals.vacationGoal,
                                icon = Icons.Filled.Flight,
                                onEdit = { showGoalsDialog = true }
                            )
                        }
                        item {
                            GoalCard(
                                emoji = "\uD83C\uDF1F",
                                title = "Dream Milestone Fund",
                                current = uiState.summary.savings,
                                goal = uiState.summary.goals.dreamGoal,
                                icon = Icons.Filled.Star,
                                onEdit = { showGoalsDialog = true }
                            )
                        }
                        if (uiState.summary.overdueBills.isNotEmpty()) {
                            item {
                                SectionHeader("Bills overdue", count = uiState.summary.overdueBills.size, overdue = true)
                            }
                            items(uiState.summary.overdueBills, key = { "overdue_${it.billId}" }) { bill ->
                                BillCard(bill = bill, overdue = true, onMarkPaid = { viewModel.markBillPaid(bill.billId) })
                            }
                        }
                        if (uiState.summary.openBills.isNotEmpty()) {
                            item {
                                SectionHeader("Upcoming bills", count = uiState.summary.openBills.size)
                            }
                            items(uiState.summary.openBills, key = { "open_${it.billId}" }) { bill ->
                                BillCard(bill = bill, overdue = false, onMarkPaid = { viewModel.markBillPaid(bill.billId) })
                            }
                        }
                        if (uiState.summary.openShoppingItems.isNotEmpty()) {
                            item {
                                SectionHeader("Shopping list", count = uiState.summary.openShoppingItems.size)
                            }
                            items(uiState.summary.openShoppingItems, key = { "shop_${it.itemId}" }) { item ->
                                ShoppingItemCard(item = item, onPurchased = { viewModel.markItemPurchased(item.itemId) })
                            }
                        }
                        item {
                            SectionHeader("Shared ledger", count = uiState.summary.recentTransactions.size)
                        }
                        if (uiState.summary.recentTransactions.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Filled.AccountBalanceWallet,
                                    title = "No money activity yet",
                                    subtitle = "Tap + to log the first household transaction.",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            items(uiState.summary.recentTransactions, key = { "tx_${it.transactionId}" }) { transaction ->
                                TransactionCard(
                                    transaction = transaction,
                                    actorName = resolveActorName(
                                        transaction.userId,
                                        sessionState.user?.displayName,
                                        sessionState.partner?.displayName
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                viewModel.clearMessage()
                showTransactionDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add transaction")
        }
    }

    if (showTransactionDialog) {
        AddTransactionDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { if (!uiState.isSubmitting) showTransactionDialog = false },
            onSubmit = { type, description, category, amount, wallet ->
                viewModel.addTransaction(type, description, category, amount, wallet) { showTransactionDialog = false }
            }
        )
    }
    if (showBillDialog) {
        AddBillDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { if (!uiState.isSubmitting) showBillDialog = false },
            onSubmit = { title, category, amount, dueDate ->
                viewModel.createBill(title, category, amount, dueDate) { showBillDialog = false }
            }
        )
    }
    if (showShoppingDialog) {
        AddShoppingDialog(
            isSubmitting = uiState.isSubmitting,
            onDismiss = { if (!uiState.isSubmitting) showShoppingDialog = false },
            onSubmit = { title, category, cost ->
                viewModel.createShoppingItem(title, category, cost) { showShoppingDialog = false }
            }
        )
    }
    if (showGoalsDialog) {
        EditGoalsDialog(
            isSubmitting = uiState.isSubmitting,
            initialVacation = uiState.summary.goals.vacationGoal,
            initialDream = uiState.summary.goals.dreamGoal,
            onDismiss = { if (!uiState.isSubmitting) showGoalsDialog = false },
            onSubmit = { vacation, dream ->
                viewModel.setGoals(vacation, dream) { showGoalsDialog = false }
            }
        )
    }
}

private fun resolveActorName(userId: String, currentName: String?, partnerName: String?): String = when {
    currentName.isNullOrBlank() && partnerName.isNullOrBlank() -> "Home"
    partnerName.isNullOrBlank() -> currentName.orEmpty()
    currentName.isNullOrBlank() -> partnerName.orEmpty()
    userId == partnerName -> partnerName
    else -> currentName
}

@Composable
private fun SummaryCards(balance: Double, savings: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Home money",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = formatMoney(balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Savings",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = formatMoney(savings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun WalletCards(wallets: WalletBreakdown) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text(
                text = "Wallets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 10.dp)
            )
            WalletRow("Joint Household Wallet", wallets.joint, Icons.Filled.Home)
            WalletRow("His Personal Account", wallets.his, Icons.Filled.Person)
            WalletRow("Her Personal Account", wallets.hers, Icons.Filled.Person)
        }
    }
}

@Composable
private fun WalletRow(label: String, value: Double, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(6.dp).size(18.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatMoney(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GoalCard(
    emoji: String,
    title: String,
    current: Double,
    goal: Double,
    icon: ImageVector,
    onEdit: () -> Unit
) {
    val progress = if (goal > 0) (current / goal).toFloat().coerceIn(0f, 1f) else 0f
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$emoji $title",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (goal > 0) "${formatMoney(current)} of ${formatMoney(goal)} saved" else "Goal not set yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit, enabled = true) {
                    Icon(Icons.Filled.MoreHoriz, contentDescription = "Edit goal")
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int? = null, overdue: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        count?.let {
            Pill(
                text = "$it",
                icon = null,
                containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BillCard(bill: Bill, overdue: Boolean, onMarkPaid: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (overdue) {
                        Pill(
                            text = "OVERDUE",
                            icon = null,
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                if (bill.category.isNotBlank()) {
                    Text(
                        text = bill.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Due ${bill.dueDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatMoney(bill.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onMarkPaid,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Mark paid")
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemCard(item: ShoppingItem, onPurchased: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (item.category.isNotBlank()) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatMoney(item.estimatedCost),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onPurchased,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Purchased")
                }
            }
        }
    }
}

@Composable
private fun TransactionCard(transaction: MoneyTransaction, actorName: String) {
    val positive = transaction.type == "income" || transaction.type == "savings_withdraw"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = transactionCategoryIcon(transaction.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (transaction.category.isNotBlank()) {
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Pill(
                        text = walletLabel(transaction.wallet),
                        icon = null,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = (if (positive) "+" else "-") + formatMoney(transaction.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (positive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.End
            )
            Avatar(name = actorName, size = 26.dp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddTransactionDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (type: String, description: String, category: String, amount: Double, wallet: String) -> Unit
) {
    var isExpense by remember { mutableStateOf(true) }
    var wallet by remember { mutableStateOf("joint") }
    var category by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Income", style = MaterialTheme.typography.labelLarge)
                    Switch(checked = isExpense, onCheckedChange = { isExpense = it })
                    Text("Expense", style = MaterialTheme.typography.labelLarge)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Wallet target", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        walletOptions.forEach { (value, label) ->
                            FilterChip(
                                selected = wallet == value,
                                onClick = { wallet = value },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOptions.forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = if (category == option) "" else option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onSubmit(if (isExpense) "expense" else "income", notes.trim(), category, amount, wallet)
                    }
                },
                enabled = !isSubmitting && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(if (isSubmitting) "Saving..." else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditGoalsDialog(
    isSubmitting: Boolean,
    initialVacation: Double,
    initialDream: Double,
    onDismiss: () -> Unit,
    onSubmit: (vacation: Double, dream: Double) -> Unit
) {
    var vacationText by remember { mutableStateOf(initialVacation.ifInvalid("0")) }
    var dreamText by remember { mutableStateOf(initialDream.ifInvalid("0")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Savings Goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = vacationText,
                    onValueChange = { vacationText = it },
                    label = { Text("Vacation goal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dreamText,
                    onValueChange = { dreamText = it },
                    label = { Text("Dream fund goal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        vacationText.toDoubleOrNull() ?: 0.0,
                        dreamText.toDoubleOrNull() ?: 0.0
                    )
                },
                enabled = !isSubmitting &&
                    (vacationText.toDoubleOrNull() ?: -1.0) >= 0 &&
                    (dreamText.toDoubleOrNull() ?: -1.0) >= 0
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
private fun AddBillDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: String, amount: Double, dueDate: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (dueDate.isBlank()) "Pick due date" else "Due $dueDate")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amount > 0 && dueDate.isNotBlank()) {
                        onSubmit(title.trim(), category.trim(), amount, dueDate)
                    }
                },
                enabled = !isSubmitting && title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0 && dueDate.isNotBlank()
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
                        datePickerState.selectedDateMillis?.let { dueDate = millisToDateString(it) }
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

@Composable
private fun AddShoppingDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, category: String, estimatedCost: Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shopping Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Estimated cost (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(title.trim(), category.trim(), costText.toDoubleOrNull() ?: 0.0)
                    }
                },
                enabled = !isSubmitting && title.isNotBlank()
            ) {
                Text(if (isSubmitting) "Saving..." else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Cancel") }
        }
    )
}

private fun Double.ifInvalid(replacement: String): String =
    if (this <= 0) replacement else toString()

private fun transactionCategoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "utilities" -> Icons.Filled.Home
    "groceries" -> Icons.Filled.ShoppingCart
    "date night" -> Icons.Filled.Favorite
    "subscriptions" -> Icons.Filled.Star
    else -> Icons.Filled.MoreHoriz
}

private fun walletLabel(wallet: String): String = when (wallet) {
    "his" -> "His"
    "hers" -> "Hers"
    else -> "Joint"
}

@Composable
private fun formatMoney(amount: Double): String {
    val session by SessionManager.sessionState.collectAsState()
    return CurrencyCatalog.formatMoney(amount, session.currency)
}

private fun millisToDateString(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
