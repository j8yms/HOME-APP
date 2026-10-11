package com.example.householdapp.mpesa

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.EmptyState
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.SectionHeader
import com.example.householdapp.finance.CurrencyCatalog

private val mpesaWalletOptions = listOf(
    "joint" to "Joint Household",
    "his" to "His Wallet",
    "hers" to "Her Wallet"
)

private val mpesaCategories = listOf("M-Pesa", "Utilities", "Groceries", "Bills", "Shopping", "Other")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MpesaInboxScreen(
    viewModel: MpesaInboxViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var reviewing by remember { mutableStateOf<PendingMpesaItem?>(null) }
    var pasteText by remember { mutableStateOf("") }
    var showPaste by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh(context) }

    LaunchedEffect(Unit) { viewModel.refresh(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(
            title = "Pending M-Pesa",
            subtitle = "Review detections, then allocate",
            actions = {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f)
                ) {
                    IconButton(onClick = { viewModel.refresh(context) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh inbox",
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        )

        uiState.errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        uiState.successMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "How detection works",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "With your permission, the app reads incoming SMS from MPESA only, " +
                                "extracts the code, amount, date and counterparty on this device, " +
                                "and stores that summary here. Full message text is never saved and " +
                                "nothing is uploaded. Turn capture off any time; the permission can " +
                                "be revoked in system settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SMS capture", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = uiState.captureEnabled,
                                onCheckedChange = { viewModel.setCaptureEnabled(context, it) }
                            )
                        }
                        if (!uiState.smsPermissionGranted) {
                            OutlinedButton(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Enable SMS detection") }
                            OutlinedButton(
                                onClick = { showPaste = !showPaste },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (showPaste) "Hide manual import" else "Paste a message instead") }
                        }
                        if (showPaste || uiState.smsPermissionGranted) {
                            OutlinedTextField(
                                value = pasteText,
                                onValueChange = { pasteText = it },
                                label = { Text("Paste an M-Pesa message") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            Button(
                                onClick = {
                                    viewModel.clearMessage()
                                    viewModel.importPasted(context, pasteText)
                                    pasteText = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Import pasted message") }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "Needs allocation", count = uiState.pending.size.takeIf { it > 0 })
            }

            if (uiState.pending.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Sms,
                        title = "Nothing pending",
                        subtitle = "Detected M-Pesa messages will wait here for your review.",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(uiState.pending, key = { it.id }) { item ->
                    PendingMpesaCard(
                        item = item,
                        onReview = {
                            viewModel.clearMessage()
                            reviewing = item
                        },
                        onDismiss = { viewModel.dismiss(context, item) }
                    )
                }
            }
        }
    }

    reviewing?.let { item ->
        AllocateMpesaDialog(
            item = item,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { if (!uiState.isSubmitting) reviewing = null },
            onConfirm = { type, wallet, category, notes ->
                viewModel.confirm(context, item, type, wallet, category, notes) { reviewing = null }
            }
        )
    }
}

@Composable
private fun PendingMpesaCard(
    item: PendingMpesaItem,
    onReview: () -> Unit,
    onDismiss: () -> Unit
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
                    text = (if (item.code.isNotBlank()) "${item.code} • " else "") +
                        CurrencyCatalog.formatMoney(item.amount, "KES"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = buildString {
                        append(item.kind.lowercase().replaceFirstChar { it.uppercase() })
                        if (item.counterparty.isNotBlank()) append(" • ${item.counterparty}")
                        if (item.dateTime.isNotBlank()) append(" • ${item.dateTime}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pending — not yet in balances",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onReview) { Text("Review") }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Delete, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllocateMpesaDialog(
    item: PendingMpesaItem,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (type: String, wallet: String, category: String, notes: String) -> Unit
) {
    // Uncertain kinds (sent/withdraw/deposit/reversal) force an explicit
    // choice — never silently classified.
    var type by remember {
        mutableStateOf(if (item.suggestedType == "uncertain") "" else item.suggestedType)
    }
    val sessionState by SessionManager.sessionState.collectAsState()
    val ownerWallet = remember(sessionState.user?.householdSide) {
        when (sessionState.user?.householdSide?.lowercase()) {
            "his" -> "his"
            "hers" -> "hers"
            else -> "joint"
        }
    }
    var wallet by remember { mutableStateOf(ownerWallet) }
    var category by remember { mutableStateOf("M-Pesa") }
    var notes by remember { mutableStateOf("") }

    val canConfirm = !isSubmitting && (type == "income" || type == "expense") && wallet.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allocate M-Pesa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "${CurrencyCatalog.formatMoney(item.amount, "KES")}" +
                        (if (item.code.isNotBlank()) " • ${item.code}" else "") +
                        (if (item.counterparty.isNotBlank()) " • ${item.counterparty}" else ""),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (item.kind == "REVERSAL") {
                    Text(
                        "This looks like a reversal. Confirming records it as a new entry — " +
                            "check the original transaction in Activity first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text("Classification", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "income",
                        onClick = { type = "income" },
                        label = { Text("Income") }
                    )
                    FilterChip(
                        selected = type == "expense",
                        onClick = { type = "expense" },
                        label = { Text("Expense") }
                    )
                }
                if (type.isBlank()) {
                    Text(
                        "Choose how to classify this transaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text("Wallet", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mpesaWalletOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = wallet == value,
                            onClick = { wallet = value },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mpesaCategories.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isSubmitting) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (canConfirm) onConfirm(type, wallet, category, notes) },
                enabled = canConfirm
            ) { Text(if (isSubmitting) "Confirming..." else "Confirm") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Later") }
        }
    )
}
