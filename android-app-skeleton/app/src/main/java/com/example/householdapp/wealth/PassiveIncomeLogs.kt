package com.example.householdapp.wealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.householdapp.core.network.PassiveIncomeLog
import com.example.householdapp.core.network.WealthItem
import com.example.householdapp.core.ui.components.EmptyState
import com.example.householdapp.core.ui.components.SectionHeader
import com.example.householdapp.finance.CurrencyCatalog
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale

private fun currentMonthKey(): String = YearMonth.now().toString()

private fun todayKey(): String = LocalDate.now().toString()

private fun shiftMonth(month: String, delta: Long): String =
    try {
        YearMonth.parse(month).plusMonths(delta).toString()
    } catch (e: Exception) {
        currentMonthKey()
    }

private fun parseDateSafe(raw: String): LocalDate? =
    try {
        LocalDate.parse(raw.substring(0, 10))
    } catch (e: Exception) {
        null
    }

private fun money(amount: Double, currency: String): String =
    CurrencyCatalog.formatMoney(amount, currency.ifBlank { "KES" })

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassiveLogsSection(
    viewModel: WealthViewModel,
    userId: String,
    currency: String,
    sources: List<WealthItem>
) {
    val uiState by viewModel.uiState.collectAsState()
    var month by remember { mutableStateOf(currentMonthKey()) }
    var showLogDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<PassiveIncomeLog?>(null) }
    var deleteTarget by remember { mutableStateOf<PassiveIncomeLog?>(null) }

    LaunchedEffect(userId, month) {
        if (userId.isNotBlank()) viewModel.loadPassiveLogs(userId, month)
    }

    val logs = uiState.passiveLogs
    val today = remember { todayKey() }
    val weekFields = remember { WeekFields.of(Locale.getDefault()) }

    // Request 8: actual vs projected are strictly separated; projected never
    // touches confirmed balances.
    val actualLogs = remember(logs) { logs.filter { it.entryKind.lowercase() != "projected" } }
    val projectedLogs = remember(logs) { logs.filter { it.entryKind.lowercase() == "projected" } }

    fun sumFor(dates: Set<String>): Double =
        actualLogs.filter { it.incomeDate in dates }.sumOf { it.amount }

    val todayTotal = remember(actualLogs, today) { sumFor(setOf(today)) }
    val weekDates = remember(actualLogs, today) {
        val now = parseDateSafe(today) ?: LocalDate.now()
        actualLogs.mapNotNull { parseDateSafe(it.incomeDate) }
            .filter {
                it.get(weekFields.weekOfWeekBasedYear()) == now.get(weekFields.weekOfWeekBasedYear()) &&
                    it.get(weekFields.weekBasedYear()) == now.get(weekFields.weekBasedYear())
            }
            .map { it.toString() }.toSet()
    }
    val weekTotal = remember(actualLogs, weekDates) { sumFor(weekDates) }
    val monthTotal = remember(actualLogs) { actualLogs.sumOf { it.amount } }
    val yearPrefix = remember(month) { month.substring(0, 4) }
    val yearTotal = remember(actualLogs, yearPrefix) {
        actualLogs.filter { it.incomeDate.startsWith(yearPrefix) }.sumOf { it.amount }
    }
    val projectedTotal = remember(projectedLogs) { projectedLogs.sumOf { it.amount } }
    val bySource = remember(actualLogs) {
        actualLogs.groupBy { it.sourceName.ifBlank { "Unknown" } }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
    }

    SectionHeader(
        title = "Daily logs — $month",
        count = logs.size.takeIf { it > 0 },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = shiftMonth(month, -1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                IconButton(
                    onClick = { month = shiftMonth(month, 1) },
                    enabled = month < currentMonthKey()
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
                IconButton(
                    onClick = {
                        editingLog = null
                        showLogDialog = true
                    },
                    enabled = !uiState.isSaving
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Log income", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Received totals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Today ${money(todayTotal, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text("Week ${money(weekTotal, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text("Month ${money(monthTotal, currency)}", style = MaterialTheme.typography.bodyMedium)
                Text("Year ${money(yearTotal, currency)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (projectedTotal > 0) {
                Text(
                    "Projected (not received): ${money(projectedTotal, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (bySource.isNotEmpty()) {
                Text("By source", style = MaterialTheme.typography.labelMedium)
                bySource.take(6).forEach { (source, total) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(source, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text(money(total, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    when {
        uiState.isLoadingPassiveLogs && logs.isEmpty() -> {
            CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        }
        logs.isEmpty() -> {
            EmptyState(
                icon = Icons.Filled.Savings,
                title = "No income logged for $month",
                subtitle = "Tap + to log the first receipt.",
                modifier = Modifier.fillMaxWidth()
            )
        }
        else -> {
            logs.forEach { log ->
                PassiveLogRow(
                    log = log,
                    currency = currency,
                    isDeleting = uiState.deletingPassiveLogId == log.logId,
                    onEdit = {
                        editingLog = log
                        showLogDialog = true
                    },
                    onDelete = { deleteTarget = log }
                )
            }
        }
    }

    if (showLogDialog) {
        PassiveLogDialog(
            initial = editingLog,
            sources = sources,
            isSaving = uiState.isSaving,
            onDismiss = {
                if (!uiState.isSaving) {
                    showLogDialog = false
                    editingLog = null
                }
            },
            onSave = { incomeId, sourceName, amount, incomeDate, incomeType, notes, entryKind, linkTransaction ->
                viewModel.savePassiveLog(
                    userId = userId,
                    month = month,
                    logId = editingLog?.logId.orEmpty(),
                    incomeId = incomeId,
                    sourceName = sourceName,
                    amount = amount,
                    incomeDate = incomeDate,
                    incomeType = incomeType,
                    notes = notes,
                    entryKind = entryKind,
                    linkTransaction = linkTransaction
                ) {
                    showLogDialog = false
                    editingLog = null
                }
            }
        )
    }

    deleteTarget?.let { target ->
        val deleting = uiState.deletingPassiveLogId == target.logId
        AlertDialog(
            onDismissRequest = { if (!deleting) deleteTarget = null },
            title = { Text("Delete income log?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${target.sourceName.ifBlank { "Income" }} • ${money(target.amount, currency)} • ${target.incomeDate}",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (target.linkedTransactionId.isNotBlank()) {
                        Text(
                            "Its linked finance record will be removed too so balances stay consistent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePassiveLog(userId, month, target.logId) { deleteTarget = null }
                    },
                    enabled = !deleting
                ) { Text(if (deleting) "Deleting..." else "Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }, enabled = !deleting) { Text("Keep") }
            }
        )
    }
}

@Composable
private fun PassiveLogRow(
    log: PassiveIncomeLog,
    currency: String,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                    text = log.sourceName.ifBlank { "Income" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = buildString {
                        append(log.incomeDate)
                        if (log.incomeType.isNotBlank()) append(" • ${log.incomeType}")
                        if (log.entryKind.equals("projected", ignoreCase = true)) append(" • projected")
                        if (log.linkedTransactionId.isNotBlank()) append(" • linked")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (log.notes.isNotBlank()) {
                    Text(
                        text = log.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = money(log.amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (isDeleting) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit log")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete log", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PassiveLogDialog(
    initial: PassiveIncomeLog?,
    sources: List<WealthItem>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (incomeId: String, sourceName: String, amount: Double, incomeDate: String, incomeType: String, notes: String, entryKind: String, linkTransaction: Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf(if ((initial?.amount ?: 0.0) > 0) initial?.amount.toString() else "") }
    // Request 9: date picker, defaulting to today; historical dates allowed.
    var incomeDate by remember { mutableStateOf(initial?.incomeDate?.takeIf { it.isNotBlank() } ?: todayKey()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var incomeId by remember { mutableStateOf(initial?.incomeId ?: "") }
    var customSource by remember { mutableStateOf(if ((initial?.incomeId ?: "").isBlank()) initial?.sourceName ?: "" else "") }
    var incomeType by remember { mutableStateOf(initial?.incomeType ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var entryKind by remember { mutableStateOf(initial?.entryKind?.lowercase()?.takeIf { it == "projected" } ?: "actual") }
    var linkTransaction by remember { mutableStateOf(initial?.linkedTransactionId?.isNotBlank() == true) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val resolvedSourceName = sources.firstOrNull { it.id == incomeId }?.name ?: customSource.trim()
    val canSave = !isSaving && amount > 0 && incomeDate.isNotBlank() &&
        (incomeId.isNotBlank() || customSource.trim().isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Log passive income" else "Edit income log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                ) { Text("Date: $incomeDate") }
                Text("Source", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sources.forEach { source ->
                        FilterChip(
                            selected = incomeId == source.id,
                            onClick = {
                                incomeId = if (incomeId == source.id) "" else source.id
                                if (incomeId.isNotBlank()) customSource = ""
                            },
                            label = { Text(source.name.ifBlank { "Source" }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = customSource,
                    onValueChange = {
                        customSource = it
                        if (it.isNotBlank()) incomeId = ""
                    },
                    label = { Text("Or custom source name (no new source needed)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = incomeType,
                    onValueChange = { incomeType = it },
                    label = { Text("Income type / category (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = entryKind == "actual",
                        onClick = {
                            entryKind = "actual"
                        },
                        label = { Text("Received") }
                    )
                    FilterChip(
                        selected = entryKind == "projected",
                        onClick = {
                            entryKind = "projected"
                            linkTransaction = false
                        },
                        label = { Text("Projected") }
                    )
                }
                if (entryKind == "actual") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Also record in Money as income",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = linkTransaction, onCheckedChange = { linkTransaction = it })
                    }
                    Text(
                        "One linked finance record is created and kept in sync — never double-counted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Projected entries never affect confirmed balances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(incomeId, resolvedSourceName, amount, incomeDate, incomeType.trim(), notes.trim(), entryKind, linkTransaction && entryKind == "actual")
                    }
                },
                enabled = canSave
            ) { Text(if (isSaving) "Saving..." else "Save") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateSafe(incomeDate)
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            incomeDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
