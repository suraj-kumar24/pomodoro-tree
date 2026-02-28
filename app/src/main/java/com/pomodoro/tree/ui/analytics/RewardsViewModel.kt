package com.pomodoro.tree.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.tree.data.db.RewardEntity
import com.pomodoro.tree.data.repository.RewardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RewardsUiState(
    val focusBalanceHours: Float = 0f,
    val activeRewards: List<RewardEntity> = emptyList(),
    val redeemedRewards: List<RewardEntity> = emptyList(),
    val showRedeemedSection: Boolean = false
)

@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewardRepository: RewardRepository
) : ViewModel() {

    private val _balance = MutableStateFlow(0f)
    private val _showRedeemed = MutableStateFlow(false)

    val uiState: StateFlow<RewardsUiState> = combine(
        _balance,
        rewardRepository.getActiveRewards(),
        rewardRepository.getRedeemedRewards(),
        _showRedeemed
    ) { balance, active, redeemed, showRedeemed ->
        RewardsUiState(
            focusBalanceHours = balance,
            activeRewards = active,
            redeemedRewards = redeemed,
            showRedeemedSection = showRedeemed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RewardsUiState())

    init {
        refreshBalance()
    }

    fun refreshBalance() {
        viewModelScope.launch {
            _balance.value = rewardRepository.getFocusBalanceHours()
        }
    }

    fun addReward(name: String, focusHoursCost: Float, iconEmoji: String) {
        viewModelScope.launch {
            rewardRepository.addReward(name, focusHoursCost, iconEmoji)
        }
    }

    fun redeemReward(id: Long) {
        viewModelScope.launch {
            rewardRepository.redeemReward(id)
            refreshBalance()
        }
    }

    fun deleteReward(id: Long) {
        viewModelScope.launch {
            rewardRepository.deleteReward(id)
            refreshBalance()
        }
    }

    fun toggleRedeemedSection() {
        _showRedeemed.value = !_showRedeemed.value
    }
}
