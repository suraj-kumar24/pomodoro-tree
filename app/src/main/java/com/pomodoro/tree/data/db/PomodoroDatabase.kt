package com.pomodoro.tree.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SessionEntity::class,
        TagEntity::class,
        DailyGoalEntity::class,
        RewardEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PomodoroDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun tagDao(): TagDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun rewardDao(): RewardDao
}
