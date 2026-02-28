package com.pomodoro.tree.domain.model

enum class SessionStatus {
    COMPLETED,
    CANCELLED
}

data class Session(
    val id: Long = 0,
    val startTimestamp: Long,
    val plannedDurationMinutes: Int,
    val actualDurationSeconds: Long,
    val overtimeSeconds: Long = 0,
    val status: SessionStatus,
    val tag: String? = null,
    val treeType: String = "oak"
)
