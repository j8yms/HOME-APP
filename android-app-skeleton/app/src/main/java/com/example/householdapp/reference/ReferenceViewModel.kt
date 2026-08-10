package com.example.householdapp.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.ReferenceEntry
import com.example.householdapp.core.repository.ReferenceRepository
import com.example.householdapp.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReferenceUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val entries: List<ReferenceEntry> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val deletingEntryId: String? = null
)

class ReferenceViewModel(
    private val repository: ReferenceRepository = ReferenceRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReferenceUiState())
    val uiState: StateFlow<ReferenceUiState> = _uiState.asStateFlow()

    fun loadEntries(category: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !forceRefresh && it.entries.isEmpty(),
                    errorMessage = null
                )
            }

            runCatching { repository.listEntries(category) }
                .onSuccess { response ->
                    val entries = if (response.success) {
                        response.data?.entries.orEmpty()
                    } else {
                        emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = entries,
                            errorMessage = if (response.success) null else response.error?.message
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unable to load directory."
                        )
                    }
                }
        }
    }

    fun createEntry(category: String, title: String, content: String, onSuccess: () -> Unit = {}) {
        val userId = SessionManager.sessionState.value.user?.userId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in first to add entries.") }
            return
        }
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a title for the entry.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            runCatching {
                repository.createEntry(category, title.trim(), content.trim(), userId)
            }.onSuccess { response ->
                val entry = response.data?.entry
                if (!response.success || entry == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = response.error?.message ?: "Unable to add entry."
                        )
                    }
                    return@launch
                }

                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        entries = listOf(entry) + state.entries,
                        successMessage = "Added ${entry.title}.",
                        errorMessage = null
                    )
                }
                onSuccess()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Unable to add entry."
                    )
                }
            }
        }
    }

    fun updateEntry(entryId: String, title: String, content: String, onSuccess: () -> Unit = {}) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a title for the entry.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            runCatching { repository.updateEntry(entryId, title.trim(), content.trim()) }
                .onSuccess { response ->
                    val entry = response.data?.entry
                    if (!response.success || entry == null) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = response.error?.message ?: "Unable to update entry."
                            )
                        }
                        return@launch
                    }

                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            entries = state.entries.map { if (it.entryId == entryId) entry else it },
                            successMessage = "Updated ${entry.title}.",
                            errorMessage = null
                        )
                    }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message ?: "Unable to update entry."
                        )
                    }
                }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingEntryId = entryId, errorMessage = null) }

            runCatching { repository.deleteEntry(entryId) }
                .onSuccess { response ->
                    val deleted = response.data?.deleted == true || response.success
                    _uiState.update { state ->
                        state.copy(
                            deletingEntryId = null,
                            entries = if (deleted) state.entries.filter { it.entryId != entryId } else state.entries,
                            successMessage = if (deleted) "Entry removed." else state.successMessage,
                            errorMessage = if (deleted) null else response.error?.message
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            deletingEntryId = null,
                            errorMessage = throwable.message ?: "Unable to delete entry."
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
