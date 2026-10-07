package com.example.householdapp.wealth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.householdapp.core.network.FinancialFreedom
import com.example.householdapp.core.network.SaveFinancialFreedomRequest
import com.example.householdapp.core.network.WealthItem

private val TRANSACTION_TYPES = listOf("contribution", "withdrawal", "return", "fee")

private val VALUE_REQUIRED_SECTIONS = setOf(
    WealthSection.INVESTMENTS,
    WealthSection.ASSETS,
    WealthSection.LIABILITIES,
    WealthSection.INCOME,
    WealthSection.CONTRIBUTIONS,
    WealthSection.PROP_PAYOUTS
)

private fun parseNumber(text: String): Double = text.toDoubleOrNull() ?: 0.0

private fun numberText(value: Double?): String {
    if (value == null || value == 0.0) return ""
    return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}

private fun initialActive(section: WealthSection, initial: WealthItem?): Boolean {
    if (initial == null) return true
    if (section == WealthSection.INCOME || section == WealthSection.PROP_ACCOUNTS) {
        val status = initial.status.lowercase()
        if (status.isNotBlank()) return status != "inactive" && status != "paused"
    }
    return initial.isActive
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorLabel: String = "Required",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            {
                Text(text = errorLabel)
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WealthItemDialog(
    section: WealthSection,
    initial: WealthItem?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (WealthItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "") }
    var provider by remember { mutableStateOf(initial?.provider ?: "") }
    var reference by remember { mutableStateOf(initial?.reference ?: "") }
    var valueText by remember { mutableStateOf(numberText(initial?.value)) }
    var startingValueText by remember { mutableStateOf(numberText(initial?.startingValue)) }
    var targetValueText by remember { mutableStateOf(numberText(initial?.targetValue)) }
    var monthlyText by remember { mutableStateOf(numberText(initial?.monthlyAmount)) }
    var yieldText by remember { mutableStateOf(numberText(initial?.yieldPct)) }
    var interestText by remember { mutableStateOf(numberText(initial?.interestRate)) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: "") }
    var targetDate by remember { mutableStateOf(initial?.targetDate ?: "") }
    var achieved by remember { mutableStateOf(initial?.achieved ?: false) }
    var isActive by remember { mutableStateOf(initialActive(section, initial)) }
    var investmentId by remember { mutableStateOf(initial?.investmentId ?: "") }
    var transactionType by remember { mutableStateOf(initial?.transactionType ?: "") }
    var symbol by remember { mutableStateOf(initial?.symbol ?: "") }
    var action by remember { mutableStateOf(initial?.action ?: "") }
    var quantityText by remember { mutableStateOf(numberText(initial?.quantity)) }
    var priceText by remember { mutableStateOf(numberText(initial?.price)) }
    var strategy by remember { mutableStateOf(initial?.strategy ?: "") }
    var pnlText by remember { mutableStateOf(numberText(initial?.pnl)) }
    var feesText by remember { mutableStateOf(numberText(initial?.fees)) }
    var showErrors by remember { mutableStateOf(false) }

    val nameInvalid = section != WealthSection.TRADES &&
        section != WealthSection.CONTRIBUTIONS &&
        name.isBlank()
    val symbolInvalid = section == WealthSection.TRADES && symbol.isBlank()
    val investmentIdInvalid = section == WealthSection.CONTRIBUTIONS && investmentId.isBlank()
    val transactionTypeInvalid = section == WealthSection.CONTRIBUTIONS && transactionType.isBlank()
    val valueInvalid = section in VALUE_REQUIRED_SECTIONS && parseNumber(valueText) <= 0.0
    val targetValueInvalid = section == WealthSection.MILESTONES && parseNumber(targetValueText) <= 0.0
    val invalid = nameInvalid || symbolInvalid || investmentIdInvalid ||
        transactionTypeInvalid || valueInvalid || targetValueInvalid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) {
                    "Add ${section.singular}"
                } else {
                    "Edit ${section.singular}"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (section) {
                    WealthSection.INVESTMENTS -> {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Name",
                            isError = showErrors && nameInvalid
                        )
                        FormTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = "Type (e.g. stocks, bonds)"
                        )
                        FormTextField(
                            value = provider,
                            onValueChange = { provider = it },
                            label = "Provider"
                        )
                        FormTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = "Current value",
                            isError = showErrors && valueInvalid,
                            errorLabel = "Enter a positive amount",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = startingValueText,
                            onValueChange = { startingValueText = it },
                            label = "Starting value",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = targetValueText,
                            onValueChange = { targetValueText = it },
                            label = "Amount invested",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = yieldText,
                            onValueChange = { yieldText = it },
                            label = "Yield %",
                            keyboardType = KeyboardType.Decimal
                        )
                        ToggleRow(label = "Active", checked = isActive, onCheckedChange = { isActive = it })
                    }

                    WealthSection.ASSETS -> {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Name",
                            isError = showErrors && nameInvalid
                        )
                        FormTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = "Category"
                        )
                        FormTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = "Value",
                            isError = showErrors && valueInvalid,
                            errorLabel = "Enter a positive amount",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes"
                        )
                    }

                    WealthSection.LIABILITIES -> {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Name",
                            isError = showErrors && nameInvalid
                        )
                        FormTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = "Category"
                        )
                        FormTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = "Amount owed",
                            isError = showErrors && valueInvalid,
                            errorLabel = "Enter a positive amount",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = interestText,
                            onValueChange = { interestText = it },
                            label = "Interest %",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes"
                        )
                    }

                    WealthSection.MILESTONES -> {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Name",
                            isError = showErrors && nameInvalid
                        )
                        FormTextField(
                            value = targetValueText,
                            onValueChange = { targetValueText = it },
                            label = "Target amount",
                            isError = showErrors && targetValueInvalid,
                            errorLabel = "Enter a positive amount",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = targetDate,
                            onValueChange = { targetDate = it },
                            label = "Target date (yyyy-MM-dd)"
                        )
                        ToggleRow(label = "Achieved", checked = achieved, onCheckedChange = { achieved = it })
                        FormTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes"
                        )
                    }

                    WealthSection.INCOME -> {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Name",
                            isError = showErrors && nameInvalid
                        )
                        FormTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = "Monthly amount",
                            isError = showErrors && valueInvalid,
                            errorLabel = "Enter a positive amount",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes"
                        )
                        ToggleRow(label = "Active", checked = isActive, onCheckedChange = { isActive = it })
                    }

                    WealthSection.ACCOUNTS -> {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Account name",
                            isError = showErrors && nameInvalid
                        )
                        FormTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = "Account type"
                        )
                        FormTextField(
                            value = provider,
                            onValueChange = { provider = it },
                            label = "Provider"
                        )
                        FormTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = "Balance",
                            keyboardType = KeyboardType.Decimal
                        )
                        ToggleRow(label = "Active", checked = isActive, onCheckedChange = { isActive = it })
                    }

                    WealthSection.TRADES -> {
                        FormTextField(
                            value = symbol,
                            onValueChange = { symbol = it },
                            label = "Symbol",
                            isError = showErrors && symbolInvalid
                        )
                        FormTextField(
                            value = action,
                            onValueChange = { action = it },
                            label = "Action (buy/sell)"
                        )
                        FormTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = "Quantity",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = "Price",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = "Date (yyyy-MM-dd)"
                        )
                        FormTextField(
                            value = strategy,
                            onValueChange = { strategy = it },
                            label = "Strategy"
                        )
                        FormTextField(
                            value = pnlText,
                            onValueChange = { pnlText = it },
                            label = "Profit / loss",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = feesText,
                            onValueChange = { feesText = it },
                            label = "Fees",
                            keyboardType = KeyboardType.Decimal
                        )
                    }

                    WealthSection.CONTRIBUTIONS -> {
                        FormTextField(
                            value = investmentId,
                            onValueChange = { investmentId = it },
                            label = "Investment ID",
                            isError = showErrors && investmentIdInvalid,
                            errorLabel = "Enter the investment id"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Transaction type", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TRANSACTION_TYPES.forEach { option ->
                                    FilterChip(
                                        selected = transactionType == option,
                                        onClick = { transactionType = option },
                                        label = {
                                            Text(text = option.replaceFirstChar { it.uppercase() })
                                        }
                                    )
                                }
                            }
                            if (showErrors && transactionTypeInvalid) {
                                Text(
                                    text = "Select a transaction type",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        FormTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = "Amount",
                            isError = showErrors && valueInvalid,
                            errorLabel = "Enter a positive amount",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = "Date (yyyy-MM-dd)"
                        )
                        FormTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes"
                        )
                    }

                    WealthSection.PROP_ACCOUNTS -> {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Name",
                            isError = showErrors && nameInvalid
                        )
                        FormTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = "Balance",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes"
                        )
                        ToggleRow(label = "Active", checked = isActive, onCheckedChange = { isActive = it })
                    }

                    WealthSection.PROP_PAYOUTS -> {
                        FormTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Account name",
                            isError = showErrors && nameInvalid
                        )
                        FormTextField(
                            value = valueText,
                            onValueChange = { valueText = it },
                            label = "Amount",
                            isError = showErrors && valueInvalid,
                            errorLabel = "Enter a positive amount",
                            keyboardType = KeyboardType.Decimal
                        )
                        FormTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = "Date (yyyy-MM-dd)"
                        )
                        FormTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes"
                        )
                    }
                }

                if (showErrors && invalid) {
                    Text(
                        text = "Fill in the required fields.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (invalid) {
                        showErrors = true
                    } else {
                        val derivedStatus = when (section) {
                            WealthSection.INCOME, WealthSection.PROP_ACCOUNTS ->
                                if (isActive) "active" else "inactive"
                            else -> initial?.status ?: ""
                        }
                        val item = (initial ?: WealthItem()).copy(
                            section = section.key,
                            name = name.trim(),
                            type = type.trim(),
                            provider = provider.trim(),
                            reference = reference.trim(),
                            value = parseNumber(valueText),
                            startingValue = parseNumber(startingValueText),
                            targetValue = parseNumber(targetValueText),
                            monthlyAmount = parseNumber(monthlyText),
                            yieldPct = parseNumber(yieldText),
                            interestRate = parseNumber(interestText),
                            notes = notes.trim(),
                            date = date.trim(),
                            status = derivedStatus,
                            targetDate = targetDate.trim(),
                            isActive = isActive,
                            investmentId = investmentId.trim(),
                            transactionType = transactionType.trim(),
                            symbol = symbol.trim(),
                            action = action.trim(),
                            quantity = parseNumber(quantityText),
                            price = parseNumber(priceText),
                            strategy = strategy.trim(),
                            pnl = parseNumber(pnlText),
                            fees = parseNumber(feesText),
                            accountId = if (section == WealthSection.PROP_PAYOUTS) {
                                name.trim()
                            } else {
                                initial?.accountId ?: ""
                            }
                        )
                        onSave(item)
                    }
                },
                enabled = !isSaving
            ) {
                Text(text = if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun FreedomDialog(
    freedom: FinancialFreedom,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (SaveFinancialFreedomRequest) -> Unit
) {
    var targetPassiveText by remember { mutableStateOf(numberText(freedom.targetPassiveIncome)) }
    var targetNetWorthText by remember { mutableStateOf(numberText(freedom.targetNetWorth)) }
    var monthlyContributionText by remember { mutableStateOf(numberText(freedom.monthlyContribution)) }
    var monthlyRequirementText by remember { mutableStateOf(numberText(freedom.monthlyRequirement)) }
    var targetDateText by remember { mutableStateOf(freedom.targetDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Financial Freedom Target") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FormTextField(
                    value = targetPassiveText,
                    onValueChange = { targetPassiveText = it },
                    label = "Target passive income / month",
                    keyboardType = KeyboardType.Decimal
                )
                FormTextField(
                    value = targetNetWorthText,
                    onValueChange = { targetNetWorthText = it },
                    label = "Target net worth",
                    keyboardType = KeyboardType.Decimal
                )
                FormTextField(
                    value = monthlyContributionText,
                    onValueChange = { monthlyContributionText = it },
                    label = "Monthly contribution",
                    keyboardType = KeyboardType.Decimal
                )
                FormTextField(
                    value = monthlyRequirementText,
                    onValueChange = { monthlyRequirementText = it },
                    label = "Monthly requirement",
                    keyboardType = KeyboardType.Decimal
                )
                FormTextField(
                    value = targetDateText,
                    onValueChange = { targetDateText = it },
                    label = "Target date (yyyy-MM-dd)"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        SaveFinancialFreedomRequest(
                            monthlyRequirement = parseNumber(monthlyRequirementText),
                            annualRequirement = freedom.annualRequirement,
                            targetPassiveIncome = parseNumber(targetPassiveText),
                            targetNetWorth = parseNumber(targetNetWorthText),
                            targetDate = targetDateText.trim(),
                            monthlyContribution = parseNumber(monthlyContributionText)
                        )
                    )
                },
                enabled = !isSaving
            ) {
                Text(text = if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) {
                Text(text = "Cancel")
            }
        }
    )
}
