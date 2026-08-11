package com.example.householdapp.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.FinanceSummary
import com.example.householdapp.core.model.LedgerEntry
import com.example.householdapp.core.model.TransferConfirmation
import com.example.householdapp.core.model.TransferRoute
import com.example.householdapp.core.model.TransferTarget
import com.example.householdapp.core.repository.FinanceRepository
import com.example.householdapp.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinanceUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val summary: FinanceSummary = FinanceSummary(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val sourceAccount: String = "",
    val destinationTarget: String = "",
    val transferAmount: Double = 0.0,
    val transferDirection: String = "out",
    val sourceBalance: Double = 0.0,
    val destinationBalance: Double = 0.0,
    val isTransferPending: Boolean = false,
    val transferValidationError: String? = null
)

class FinanceViewModel : ViewModel() {
    private val repository = FinanceRepository()

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.getSummary() }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = response.error?.message ?: "Unable to load finances.")
                        }
                    } else {
                        SessionManager.setCurrency(data.currency)
                        _uiState.update { it.copy(isLoading = false, summary = data) }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = throwable.message ?: "Unable to load finances.")
                    }
                }
        }
    }

    fun addTransaction(type: String, description: String, category: String, amount: Double, wallet: String, onSuccess: () -> Unit) {
        val userId = SessionManager.sessionState.value.user?.userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            runCatching { repository.addTransaction(type, description, category, amount, wallet, userId) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = response.error?.message ?: "Unable to add transaction.")
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            summary = data.summary,
                            successMessage = "Transaction added."
                        )
                    }
                    SessionManager.setCurrency(data.summary.currency)
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to add transaction.")
                    }
                }
        }
    }

    fun setGoals(vacationGoal: Double, dreamGoal: Double, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            runCatching { repository.setGoals(vacationGoal, dreamGoal) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = response.error?.message ?: "Unable to update goals.")
                        }
                        return@launch
                    }
                    refresh()
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Goals updated.") }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to update goals.")
                    }
                }
        }
    }

    fun setCurrency(currency: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            runCatching { repository.setCurrency(currency) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = response.error?.message ?: "Unable to update currency.")
                        }
                        return@launch
                    }
                    SessionManager.setCurrency(response.data.currency)
                    refresh()
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Currency updated.") }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to update currency.")
                    }
                }
        }
    }

    fun createBill(title: String, category: String, amount: Double, dueDate: String, onSuccess: () -> Unit) {
        val userId = SessionManager.sessionState.value.user?.userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            runCatching { repository.createBill(title, category, amount, dueDate, userId) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = response.error?.message ?: "Unable to create bill.")
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Bill added.") }
                    refresh()
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to create bill.")
                    }
                }
        }
    }

    fun markBillPaid(billId: String) {
        val userId = SessionManager.sessionState.value.user?.userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching { repository.markBillPaid(billId, userId) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = response.error?.message ?: "Unable to update bill.")
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Bill marked as paid.") }
                    refresh()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to update bill.")
                    }
                }
        }
    }

    fun createShoppingItem(title: String, category: String, estimatedCost: Double, onSuccess: () -> Unit) {
        val userId = SessionManager.sessionState.value.user?.userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            runCatching { repository.createShoppingItem(title, category, estimatedCost, userId) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = response.error?.message ?: "Unable to add shopping item.")
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Shopping item added.") }
                    refresh()
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to add shopping item.")
                    }
                }
        }
    }

    fun markItemPurchased(itemId: String) {
        val userId = SessionManager.sessionState.value.user?.userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching { repository.markItemPurchased(itemId, userId) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.update {
                            it.copy(isSubmitting = false, errorMessage = response.error?.message ?: "Unable to update item.")
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Item marked as purchased.") }
                    refresh()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to update item.")
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    // ---- Transfer functionality ----

    fun initiateTransfer(sourceAccount: String, destinationTarget: String, amount: Double, onSuccess: (TransferConfirmation) -> Unit, onError: (String) -> Unit) {
        val userId = SessionManager.sessionState.value.user?.userId ?: return

        if (amount <= 0) {
            onError("Amount must be a positive number.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTransferPending = true, errorMessage = null, transferValidationError = null) }

            val sourceBalance = getAccountBalance(sourceAccount, userId)
            if (sourceBalance < amount) {
                _uiState.update {
                    it.copy(
                        isTransferPending = false,
                        transferValidationError = "Insufficient liquidity in source account: $sourceAccount. Available: $sourceBalance."
                    )
                }
                onError("Insufficient liquidity. Available balance: $sourceBalance")
                return@launch
            }

            val destBalance = getAccountBalance(destinationTarget, userId)

            runCatching { repository.executeTransfer(sourceAccount, destinationTarget, amount, userId) }
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isTransferPending = false,
                            sourceBalance = sourceBalance - amount,
                            destinationBalance = destBalance + amount,
                            successMessage = "Transfer of ${formatMoney(amount)} completed."
                        )
                    }
                    onSuccess(TransferConfirmation(
                        sourceAccount = sourceAccount,
                        destinationTarget = destinationTarget,
                        amount = amount,
                        direction = "out",
                        sourceBalance = sourceBalance - amount,
                        destinationBalance = destBalance + amount,
                        timestamp = nowIso()
                    ))
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isTransferPending = false, errorMessage = throwable.message ?: "Transfer failed.")
                    }
                    onError("Transfer failed: ${throwable.message}")
                }
        }
    }

    suspend fun getAccountBalance(accountId: String, userId: String): Double {
        val ledgerRows = repository.getLedgerEntries(userId).data?.entries ?: emptyList()
        return ledgerRows
            .filter { it.sourceType == accountId || it.sourceId == accountId }
            .sumOf {
                if (it.direction == "out") -it.amount else it.amount
            }
    }

    suspend fun listTransfers(userId: String): List<TransferConfirmation> {
        return repository.listTransfers(userId).data?.transfers?.map { transfer ->
            TransferConfirmation(
                sourceAccount = transfer.sourceAccount,
                destinationTarget = transfer.destinationTarget,
                amount = transfer.amount,
                direction = transfer.direction,
                sourceBalance = 0.0,
                destinationBalance = 0.0,
                timestamp = transfer.timestamp
            )
        } ?: emptyList()
    }

    fun getAvailableSourceAccounts(userId: String): List<String> {
        return listOf("his", "hers", "joint")
    }

    fun getAvailableDestinationTargets(userId: String): List<String> {
        return listOf("joint", "vacation", "dream")
    }

    fun formatMoney(amount: Double): String {
        return "%.2f".format(amount)
    }

    private fun nowIso(): String {
        return java.time.Instant.now().toString()
    }
}