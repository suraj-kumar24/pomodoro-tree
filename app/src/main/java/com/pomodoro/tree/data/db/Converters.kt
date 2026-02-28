package com.pomodoro.tree.data.db

import androidx.room.TypeConverter
import com.pomodoro.tree.domain.model.SessionStatus

class Converters {

    @TypeConverter
    fun fromSessionStatus(status: SessionStatus): String = status.name

    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
}
