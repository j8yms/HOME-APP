package com.example.householdapp.mpesa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.repository.FinanceRepository
import com.example.householdapp.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MpesaInboxUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val pending: List<PendingMpesaItem> = emptyList(),
    val captureEnabled: Boolean = true,
    val smsPermissionGranted: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class MpesaInboxViewModel(
    private val financeRepository: FinanceRepository = FinanceRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MpesaInboxUiState())
    val uiState: StateFlow<MpesaInboxUiState> = _uiState.asStateFlow()

    fun refresh(context: Context) {
        val appContext = context.applicationContext
        _uiState.update {
            it.copy(
                pending = PendingMpesaStore.listPending(appContext),
                captureEnabled = PendingMpesaStore.isCaptureEnabled(appContext),
                smsPermissionGranted = ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.RECEIVE_SMS
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    fun setCaptureEnabled(context: Context, enabled: Boolean) {
        PendingMpesaStore.setCaptureEnabled(context.applicationContext, enabled)
        refresh(context)
    }

    // Manual fallback: paste a message body; parsed on-device, no permission.
    fun importPasted(context: Context, text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null, successMessage = null) }
            val parsed = MpesaParser.parse("MPESA", text)
            if (parsed == null) {
                _uiState.update { it.copy(errorMessage = "That text was not recognised as a supported M-Pesa message.") }
                return@launch
            }
            val added = PendingMpesaStore.addIfNew(context.applicationContext, parsed)
            if (added == null) {
                _uiState.update { it.copy(successMessage = "Already in the inbox — no duplicate created.") }
            } else {
                _uiState.update { it.copy(successMessage = "Pending item created. Review and allocate it below.") }
            }
            refresh(context)
        }
    }

    // Confirmation creates exactly ONE normal finance record, then marks the
    // pending item confirmed (dedupe key retained).
    fun confirm(
        context: Context,
        item: PendingMpesaItem,
        type: String,
        wallet: String,
        category: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        val userId = SessionManager.sessionState.value.user?.userId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in first to confirm transactions.") }
            return
        }
        if (type != "income" && type != "expense") {
            _uiState.update { it.copy(errorMessage = "Choose Income or Expense to confirm.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            val description = buildString {
                append("M-Pesa")
                if (item.code.isNotBlank()) append(" ${item.code}")
                if (item.counterparty.isNotBlank()) append(": ${item.counterparty}")
                if (notes.isNotBlank()) append(" — ${notes.trim()}")
            }
            runCatching {
                financeRepository.addTransaction(type, description, category, item.amount, wallet, userId)
            }.onSuccess { response ->
                val data = response.data
                if (!response.success || data == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = response.error?.message ?: "Unable to confirm transaction."
                        )
                    }
                    return@launch
                }
                PendingMpesaStore.markConfirmed(context.applicationContext, item.id)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "Confirmed. One transaction recorded in Activity.",
                        pending = PendingMpesaStore.listPending(context.applicationContext)
                    )
                }
                onSuccess()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = throwable.message ?: "Unable to confirm transaction.")
                }
            }
        }
    }

    fun dismiss(context: Context, item: PendingMpesaItem) {
        PendingMpesaStore.dismiss(context.applicationContext, item.id)
        refresh(context)
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
