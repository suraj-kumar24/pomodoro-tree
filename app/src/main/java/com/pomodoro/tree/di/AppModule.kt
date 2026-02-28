package com.pomodoro.tree.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.pomodoro.tree.data.db.PomodoroDatabase
import com.pomodoro.tree.domain.timer.TimerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PomodoroDatabase {
        return Room.databaseBuilder(
            context,
            PomodoroDatabase::class.java,
            "pomodoro_tree.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTimerEngine(): TimerEngine = TimerEngine()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
    }
}
