package com.pomodoro.tree.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DaySummary(
    val date: String,
    val completedCount: Int,
    val cancelledCount: Int,
    val totalFocusMinutes: Int,
    val totalOvertimeMinutes: Int
)

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    /** All sessions for a given day (ISO date string like "2026-02-28"). */
    @Query("""
        SELECT * FROM sessions
        WHERE date(startTimestamp / 1000, 'unixepoch', 'localtime') = :date
        ORDER BY startTimestamp ASC
    """)
    fun getSessionsForDay(date: String): Flow<List<SessionEntity>>

    /** Count of completed sessions today. */
    @Query("""
        SELECT COUNT(*) FROM sessions
        WHERE date(startTimestamp / 1000, 'unixepoch', 'localtime') = :date
        AND status = 'COMPLETED'
    """)
    fun getCompletedCountForDay(date: String): Flow<Int>

    /** Total focus minutes for a date range (inclusive). */
    @Query("""
        SELECT COALESCE(SUM(actualDurationSeconds) / 60, 0) FROM sessions
        WHERE date(startTimestamp / 1000, 'unixepoch', 'localtime') BETWEEN :startDate AND :endDate
        AND status = 'COMPLETED'
    """)
    suspend fun getTotalFocusMinutes(startDate: String, endDate: String): Int

    /** Daily summary for a date range — for weekly trendline. */
    @Query("""
        SELECT
            date(startTimestamp / 1000, 'unixepoch', 'localtime') as date,
            SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completedCount,
            SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledCount,
            COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN actualDurationSeconds ELSE 0 END) / 60, 0) as totalFocusMinutes,
            COALESCE(SUM(overtimeSeconds) / 60, 0) as totalOvertimeMinutes
        FROM sessions
        WHERE date(startTimestamp / 1000, 'unixepoch', 'localtime') BETWEEN :startDate AND :endDate
        GROUP BY date(startTimestamp / 1000, 'unixepoch', 'localtime')
        ORDER BY date ASC
    """)
    suspend fun getDailySummaries(startDate: String, endDate: String): List<DaySummary>

    /** Completed session count per day for a whole year — for heatmap. */
    @Query("""
        SELECT
            date(startTimestamp / 1000, 'unixepoch', 'localtime') as date,
            COUNT(*) as completedCount,
            0 as cancelledCount,
            0 as totalFocusMinutes,
            0 as totalOvertimeMinutes
        FROM sessions
        WHERE date(startTimestamp / 1000, 'unixepoch', 'localtime') BETWEEN :startDate AND :endDate
        AND status = 'COMPLETED'
        GROUP BY date(startTimestamp / 1000, 'unixepoch', 'localtime')
    """)
    suspend fun getCompletedCountsByDay(startDate: String, endDate: String): List<DaySummary>

    /** Most used tag in a date range. */
    @Query("""
        SELECT tag FROM sessions
        WHERE date(startTimestamp / 1000, 'unixepoch', 'localtime') BETWEEN :startDate AND :endDate
        AND tag IS NOT NULL AND status = 'COMPLETED'
        GROUP BY tag
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getMostUsedTag(startDate: String, endDate: String): String?
}
