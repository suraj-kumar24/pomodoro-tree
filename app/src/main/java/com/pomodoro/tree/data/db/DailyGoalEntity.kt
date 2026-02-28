package com.pomodoro.tree.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey
    val date: String, // ISO date format: "2026-02-28"
    val targetPomodoros: Int
)
