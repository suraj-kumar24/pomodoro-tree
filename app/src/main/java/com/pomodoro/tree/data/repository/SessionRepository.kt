package com.pomodoro.tree.data.repository

import com.pomodoro.tree.data.db.DaySummary
import com.pomodoro.tree.data.db.PomodoroDatabase
import com.pomodoro.tree.data.db.SessionEntity
import com.pomodoro.tree.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val db: PomodoroDatabase
) {
    private val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE

    fun getSessionsForToday(): Flow<List<SessionEntity>> {
        return db.sessionDao().getSessionsForDay(LocalDate.now().format(dateFormat))
    }

    fun getSessionsForDay(date: LocalDate): Flow<List<SessionEntity>> {
        return db.sessionDao().getSessionsForDay(date.format(dateFormat))
    }

    fun getCompletedCountToday(): Flow<Int> {
        return db.sessionDao().getCompletedCountForDay(LocalDate.now().format(dateFormat))
    }

    suspend fun saveSession(
        startTimestamp: Long,
        plannedDurationMinutes: Int,
        actualDurationSeconds: Long,
        overtimeSeconds: Long = 0,
        status: SessionStatus,
        tag: String? = null
    ): Long {
        return db.sessionDao().insert(
            SessionEntity(
                startTimestamp = startTimestamp,
                plannedDurationMinutes = plannedDurationMinutes,
                actualDurationSeconds = actualDurationSeconds,
                overtimeSeconds = overtimeSeconds,
                status = status,
                tag = tag
            )
        )
    }

    suspend fun getWeeklySummary(weekStartDate: LocalDate): List<DaySummary> {
        val start = weekStartDate.format(dateFormat)
        val end = weekStartDate.plusDays(6).format(dateFormat)
        return db.sessionDao().getDailySummaries(start, end)
    }

    suspend fun getTotalFocusMinutesForWeek(weekStartDate: LocalDate): Int {
        val start = weekStartDate.format(dateFormat)
        val end = weekStartDate.plusDays(6).format(dateFormat)
        return db.sessionDao().getTotalFocusMinutes(start, end)
    }

    suspend fun getYearlyHeatmap(year: Int): List<DaySummary> {
        val start = LocalDate.of(year, 1, 1).format(dateFormat)
        val end = LocalDate.of(year, 12, 31).format(dateFormat)
        return db.sessionDao().getCompletedCountsByDay(start, end)
    }

    suspend fun getMostUsedTag(startDate: LocalDate, endDate: LocalDate): String? {
        return db.sessionDao().getMostUsedTag(
            startDate.format(dateFormat),
            endDate.format(dateFormat)
        )
    }
}
