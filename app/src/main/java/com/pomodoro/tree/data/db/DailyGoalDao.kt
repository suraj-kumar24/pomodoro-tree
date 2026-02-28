package com.pomodoro.tree.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(goal: DailyGoalEntity)

    /** Get goal for a specific date, falling back to the most recent goal before that date. */
    @Query("""
        SELECT * FROM daily_goals
        WHERE date <= :date
        ORDER BY date DESC
        LIMIT 1
    """)
    suspend fun getGoalForDate(date: String): DailyGoalEntity?

    @Query("SELECT * FROM daily_goals ORDER BY date DESC LIMIT 1")
    fun getLatestGoal(): Flow<DailyGoalEntity?>
}
