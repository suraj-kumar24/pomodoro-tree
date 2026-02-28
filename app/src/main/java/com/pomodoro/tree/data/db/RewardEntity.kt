package com.pomodoro.tree.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val focusHoursCost: Float,
    val iconEmoji: String = "\uD83C\uDF81", // 🎁
    val createdAt: Long,
    val redeemedAt: Long? = null
)
