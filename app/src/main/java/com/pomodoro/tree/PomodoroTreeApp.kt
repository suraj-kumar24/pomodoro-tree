package com.pomodoro.tree

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PomodoroTreeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            getString(R.string.notification_channel_timer),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing notification while a focus session is active"
            setShowBadge(false)
            setSound(null, null)
        }

        val completionChannel = NotificationChannel(
            CHANNEL_COMPLETION,
            getString(R.string.notification_channel_completion),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notification when a focus session completes"
            enableVibration(true)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Escalating alerts when you sit past your session"
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(timerChannel, completionChannel, alertChannel))
    }

    companion object {
        const val CHANNEL_TIMER = "timer_channel"
        const val CHANNEL_COMPLETION = "completion_channel"
        const val CHANNEL_ALERTS = "alert_channel"
    }
}
