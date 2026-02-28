package com.pomodoro.tree.data.repository

import com.pomodoro.tree.data.db.PomodoroDatabase
import com.pomodoro.tree.data.db.RewardEntity
import com.pomodoro.tree.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardRepository @Inject constructor(
    private val db: PomodoroDatabase
) {

    fun getActiveRewards(): Flow<List<RewardEntity>> = db.rewardDao().getActiveRewards()

    fun getRedeemedRewards(): Flow<List<RewardEntity>> = db.rewardDao().getRedeemedRewards()

    suspend fun addReward(name: String, focusHoursCost: Float, iconEmoji: String): Long {
        return db.rewardDao().insert(
            RewardEntity(
                name = name,
                focusHoursCost = focusHoursCost,
                iconEmoji = iconEmoji,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun redeemReward(id: Long) {
        db.rewardDao().redeem(id, System.currentTimeMillis())
    }

    suspend fun deleteReward(id: Long) {
        db.rewardDao().delete(id)
    }

    /**
     * Calculate focus balance: total completed planned hours - total redeemed reward hours.
     * Only planned duration counts (not overtime).
     */
    suspend fun getFocusBalanceHours(): Float {
        val totalEarnedMinutes = db.sessionDao().getTotalFocusMinutes("2000-01-01", "2099-12-31")
        val totalRedeemed = db.rewardDao().getTotalRedeemedHours()
        return (totalEarnedMinutes / 60f) - totalRedeemed
    }
}
