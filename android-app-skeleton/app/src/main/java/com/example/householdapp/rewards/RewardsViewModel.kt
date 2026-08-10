package com.example.householdapp.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.Redemption
import com.example.householdapp.core.model.RewardItem
import com.example.householdapp.core.repository.RewardsRepository
import com.example.householdapp.core.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RewardsUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val rewards: List<RewardItem> = emptyList(),
    val redemptions: List<Redemption> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class RewardsViewModel(
    private val rewardsRepository: RewardsRepository = RewardsRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RewardsUiState())
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        loadRewards()
        loadRedemptions()
    }

    fun loadRewards(forceRefresh: Boolean = false) {
        val userId = SessionManager.sessionState.value.user?.userId.orEmpty()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !forceRefresh && it.rewards.isEmpty(),
                    errorMessage = null,
                    successMessage = null
                )
            }

            runCatching { rewardsRepository.getRewards(userId) }
                .onSuccess { response ->
                    val rewards = if (response.success) {
                        response.data?.rewards.orEmpty()
                    } else {
                        emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rewards = rewards,
                            errorMessage = if (response.success) null else response.error?.message
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Unable to load rewards."
                        )
                    }
                }
        }
    }

    fun createReward(
        title: String,
        description: String,
        costCoins: Int,
        costXp: Int,
        category: String,
        hideFromPartner: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        val userId = SessionManager.sessionState.value.user?.userId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in first to create rewards.") }
            return
        }
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a reward title.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            runCatching {
                rewardsRepository.createReward(
                    userId = userId,
                    title = title.trim(),
                    description = description.trim(),
                    costCoins = costCoins,
                    costXp = costXp,
                    category = category.trim().ifBlank { "Custom" },
                    hideFromPartner = hideFromPartner
                )
            }.onSuccess { response ->
                val reward = response.data?.reward
                if (!response.success || reward == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = response.error?.message ?: "Unable to create reward."
                        )
                    }
                    return@launch
                }

                _uiState.update { state ->
                    state.copy(
                        isSubmitting = false,
                        rewards = state.rewards + reward,
                        successMessage = "Created ${reward.title}.",
                        errorMessage = null
                    )
                }
                onSuccess()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message ?: "Unable to create reward."
                    )
                }
            }
        }
    }

    fun loadRedemptions(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            runCatching { rewardsRepository.listRedemptions() }
                .onSuccess { response ->
                    val redemptions = if (response.success) {
                        response.data?.redemptions.orEmpty()
                    } else {
                        emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            redemptions = redemptions,
                            errorMessage = if (response.success) null else response.error?.message
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            errorMessage = throwable.message ?: "Unable to load redemptions."
                        )
                    }
                }
        }
    }

    fun redeemReward(reward: RewardItem) {
        val userId = SessionManager.sessionState.value.user?.userId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in first to redeem rewards.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

            runCatching { rewardsRepository.redeemReward(userId, reward.rewardId) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = response.error?.message ?: "Unable to redeem reward."
                            )
                        }
                        return@launch
                    }

                    SessionManager.updateUser(data.user)
                    _uiState.update { state ->
                        state.copy(
                            isSubmitting = false,
                            successMessage = "Redeemed ${data.reward.title}.",
                            errorMessage = null,
                            rewards = state.rewards.filter { it.rewardId != reward.rewardId }
                        )
                    }
                    loadRedemptions(forceRefresh = true)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message ?: "Unable to redeem reward."
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
