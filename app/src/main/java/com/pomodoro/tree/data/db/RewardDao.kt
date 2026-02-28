package com.pomodoro.tree.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {

    @Query("SELECT * FROM rewards WHERE redeemedAt IS NULL ORDER BY focusHoursCost ASC")
    fun getActiveRewards(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards WHERE redeemedAt IS NOT NULL ORDER BY redeemedAt DESC")
    fun getRedeemedRewards(): Flow<List<RewardEntity>>

    @Insert
    suspend fun insert(reward: RewardEntity): Long

    @Query("UPDATE rewards SET redeemedAt = :redeemedAt WHERE id = :id")
    suspend fun redeem(id: Long, redeemedAt: Long)

    @Query("DELETE FROM rewards WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(SUM(focusHoursCost), 0.0) FROM rewards WHERE redeemedAt IS NOT NULL")
    suspend fun getTotalRedeemedHours(): Float
}
