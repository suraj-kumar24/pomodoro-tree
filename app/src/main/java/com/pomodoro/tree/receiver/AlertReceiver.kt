package com.pomodoro.tree.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.pomodoro.tree.PomodoroTreeApp
import com.pomodoro.tree.R

class AlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alertLevel = intent.getIntExtra(EXTRA_ALERT_LEVEL, 0)
        vibrate(context, alertLevel)
        showAlertNotification(context, alertLevel)
    }

    private fun vibrate(context: Context, level: Int) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val effect = when (level) {
            0 -> VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE) // +1 min: medium
            1 -> VibrationEffect.createOneShot(500, 200) // +3 min: strong
            else -> VibrationEffect.createWaveform( // +5 min+: continuous 3 sec
                longArrayOf(0, 500, 100, 500, 100, 500),
                intArrayOf(0, 255, 0, 255, 0, 255),
                -1
            )
        }

        vibrator.vibrate(effect)
    }

    private fun showAlertNotification(context: Context, level: Int) {
        val message = when (level) {
            0 -> "You've been sitting for 1 minute past your session"
            1 -> "3 minutes past your session — time to stand up!"
            else -> "Still sitting! Take a break now."
        }

        val notification = NotificationCompat.Builder(context, PomodoroTreeApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to get up!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ALERT_NOTIFICATION_ID + level, notification)
    }

    companion object {
        const val EXTRA_ALERT_LEVEL = "alert_level"
        const val ALERT_NOTIFICATION_ID = 2000

        // Alert schedule: minutes after completion
        val ALERT_SCHEDULE_MINUTES = listOf(1, 3, 5, 8, 11, 14, 17, 20)
    }
}
