package com.example.householdapp.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.network.SaveFinancialFreedomRequest
import com.example.householdapp.core.network.WealthItem
import com.example.householdapp.core.network.WealthSummaryResponse
import com.example.householdapp.core.repository.WealthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WealthSection(
    val key: String,
    val label: String,
    val singular: String
) {
    INVESTMENTS("investments", "Investments", "Investment"),
    ASSETS("assets", "Assets", "Asset"),
    LIABILITIES("liabilities", "Liabilities", "Liability"),
    MILESTONES("milestones", "Milestones", "Milestone"),
    INCOME("income", "Passive Income", "Income Source"),
    ACCOUNTS("accounts", "Accounts", "Account"),
    TRADES("trades", "Trade Journal", "Trade"),
    CONTRIBUTIONS("contributions", "Contributions", "Contribution"),
    PROP_ACCOUNTS("propAccounts", "Prop Accounts", "Prop Account"),
    PROP_PAYOUTS("propPayouts", "Prop Payouts", "Prop Payout");

    companion object {
        fun fromKey(key: String): WealthSection =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: INVESTMENTS
    }
}

data class WealthUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val summary: WealthSummaryResponse? = null,
    val section: WealthSection = WealthSection.INVESTMENTS,
    val items: List<WealthItem> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class WealthViewModel : ViewModel() {
    private val repository = WealthRepository()

    private val _uiState = MutableStateFlow(WealthUiState())
    val uiState: StateFlow<WealthUiState> = _uiState.asStateFlow()

    fun load(userId: String, section: WealthSection = _uiState.value.section) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                coroutineScope {
                    val summaryDeferred = async { repository.getSummary(userId).data }
                    val itemsDeferred = async {
                        repository.listSection(userId, section.key).data?.items ?: emptyList()
                    }
                    Pair(summaryDeferred.await(), itemsDeferred.await())
                }
            }.onSuccess { (summary, items) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    summary = summary ?: _uiState.value.summary,
                    section = section,
                    items = items
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unable to load wealth data."
                )
            }
        }
    }

    fun switchSection(userId: String, section: WealthSection) {
        if (_uiState.value.section == section && _uiState.value.items.isNotEmpty()) return
        _uiState.value = _uiState.value.copy(section = section)
        load(userId, section)
    }

    fun saveItem(userId: String, section: WealthSection, item: WealthItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            runCatching { repository.saveItem(userId, section.key, item) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = response.error?.message ?: "Unable to save item."
                        )
                        return@onSuccess
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "${section.singular} saved."
                    )
                    load(userId, section)
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "Unable to save item."
                    )
                }
        }
    }

    fun deleteItem(userId: String, section: WealthSection, id: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            runCatching { repository.deleteItem(userId, section.key, id) }
                .onSuccess { response ->
                    if (!response.success || response.data?.deleted != true) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = response.error?.message ?: "Unable to delete item."
                        )
                        return@onSuccess
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "${section.singular} deleted."
                    )
                    load(userId, section)
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "Unable to delete item."
                    )
                }
        }
    }

    fun saveFreedom(userId: String, request: SaveFinancialFreedomRequest, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            runCatching { repository.saveFreedom(request) }
                .onSuccess { response ->
                    if (!response.success || response.data == null) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = response.error?.message ?: "Unable to save target."
                        )
                        return@onSuccess
                    }
                    val updated = _uiState.value.summary
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "Financial freedom target updated.",
                        summary = updated?.copy(freedom = response.data)
                    )
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "Unable to save target."
                    )
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
