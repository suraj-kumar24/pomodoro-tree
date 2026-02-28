package com.pomodoro.tree.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pomodoro.tree.domain.model.SessionStatus

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimestamp: Long,
    val plannedDurationMinutes: Int,
    val actualDurationSeconds: Long,
    val overtimeSeconds: Long = 0,
    val status: SessionStatus,
    val tag: String? = null,
    val treeType: String = "oak"
)
