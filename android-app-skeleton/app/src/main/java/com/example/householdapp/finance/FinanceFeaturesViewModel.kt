package com.example.householdapp.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.Budget
import com.example.householdapp.core.model.LedgerEntry
import com.example.householdapp.core.model.SubscriptionInfo
import com.example.householdapp.core.model.WalletBreakdown
import com.example.householdapp.core.network.AnalyticsResponse
import com.example.householdapp.core.network.TransferRecord
import com.example.householdapp.core.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FinanceFeaturesUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val ledger: List<LedgerEntry> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val subscriptions: List<SubscriptionInfo> = emptyList(),
    val transfers: List<TransferRecord> = emptyList(),
    val analytics: AnalyticsResponse? = null,
    // Request 11: wallet balances for transfer source validation/display.
    val walletBalances: WalletBreakdown = WalletBreakdown(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class FinanceFeaturesViewModel : ViewModel() {
    private val repository = FinanceRepository()

    private val _uiState = MutableStateFlow(FinanceFeaturesUiState())
    val uiState: StateFlow<FinanceFeaturesUiState> = _uiState.asStateFlow()

    private fun loadSection(
        sectionName: String,
        userId: String,
        block: suspend () -> (FinanceFeaturesUiState) -> FinanceFeaturesUiState
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { block() }
                .onSuccess { transform ->
                    _uiState.value = transform(_uiState.value).copy(isLoading = false)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Unable to load $sectionName."
                    )
                }
        }
    }

    // Crash fix: keyed lists must never contain duplicate ids (duplicate
    // LazyColumn keys kill the process).
    fun loadLedger(userId: String) = loadSection("ledger", userId) {
        val entries = repository.getLedgerEntries(userId).data?.entries.orEmpty().distinctBy { it.ledgerId }
        return@loadSection { state: FinanceFeaturesUiState -> state.copy(ledger = entries) }
    }

    fun loadBudgets(userId: String) = loadSection("budgets", userId) {
        val budgets = repository.listBudgets(userId).data?.budgets.orEmpty().distinctBy { it.budgetId }
        return@loadSection { state: FinanceFeaturesUiState -> state.copy(budgets = budgets) }
    }

    fun loadSubscriptions(userId: String) = loadSection("subscriptions", userId) {
        val subscriptions = repository.listSubscriptions(userId).data?.subscriptions.orEmpty().distinctBy { it.subscriptionId }
        return@loadSection { state: FinanceFeaturesUiState -> state.copy(subscriptions = subscriptions) }
    }

    fun loadTransfers(userId: String) = loadSection("transfers", userId) {
        val transfers = repository.listTransfers(userId).data?.transfers.orEmpty().distinctBy { it.transferId }
        return@loadSection { state: FinanceFeaturesUiState -> state.copy(transfers = transfers) }
    }

    // Request 11: balances for the transfer form (source validation + display).
    fun loadWalletBalances() {
        viewModelScope.launch {
            runCatching { repository.getSummary() }
                .onSuccess { response ->
                    val wallets = response.data?.wallets ?: WalletBreakdown()
                    _uiState.value = _uiState.value.copy(walletBalances = wallets)
                }
                .onFailure {
                    // Balances stay at last known values; transfer validation
                    // treats unknown balances as unverified (warn, don't block).
                }
        }
    }

    fun loadAnalytics(userId: String) = loadSection("analytics", userId) {
        val analytics = repository.getAnalytics(userId).data
        return@loadSection { state: FinanceFeaturesUiState -> state.copy(analytics = analytics) }
    }

    fun createBudget(userId: String, category: String, month: String, budgetLimit: Double, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null, successMessage = null)
            runCatching { repository.createBudget(userId, category, month, budgetLimit) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = response.error?.message ?: "Unable to create budget."
                        )
                        return@onSuccess
                    }
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = "Budget created."
                    )
                    loadBudgets(userId)
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Unable to create budget."
                    )
                }
        }
    }

    fun createSubscription(userId: String, serviceName: String, nextRenewalDate: String, billingInterval: String, terminationRule: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null, successMessage = null)
            runCatching { repository.createSubscription(userId, serviceName, nextRenewalDate, billingInterval, terminationRule) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = response.error?.message ?: "Unable to create subscription."
                        )
                        return@onSuccess
                    }
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = "Subscription added."
                    )
                    loadSubscriptions(userId)
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Unable to create subscription."
                    )
                }
        }
    }

    fun toggleSubscription(userId: String, subscriptionId: String, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null, successMessage = null)
            runCatching { repository.updateSubscription(subscriptionId, isActive = isActive) }
                .onSuccess { response ->
                    if (!response.success) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = response.error?.message ?: "Unable to update subscription."
                        )
                        return@onSuccess
                    }
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = "Subscription updated."
                    )
                    loadSubscriptions(userId)
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Unable to update subscription."
                    )
                }
        }
    }

    fun executeTransfer(userId: String, sourceAccount: String, destinationTarget: String, amount: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (amount <= 0) {
            onError("Amount must be a positive number.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null, successMessage = null)
            runCatching { repository.executeTransfer(sourceAccount, destinationTarget, amount, userId) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            errorMessage = response.error?.message ?: "Transfer failed."
                        )
                        onError(response.error?.message ?: "Transfer failed.")
                        return@onSuccess
                    }
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = "Transfer of ${response.data.transfer.amount} from ${response.data.transfer.sourceAccount} to ${response.data.transfer.destinationTarget} completed. Balances — ${response.data.transfer.sourceAccount}: ${response.data.transfer.sourceBalance}, ${response.data.transfer.destinationTarget}: ${response.data.transfer.destinationBalance}."
                    )
                    loadWalletBalances()
                    loadTransfers(userId)
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Transfer failed."
                    )
                    onError(throwable.message ?: "Transfer failed.")
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
