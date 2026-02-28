package com.pomodoro.tree.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.pomodoro.tree.PomodoroTreeApp
import com.pomodoro.tree.R
import com.pomodoro.tree.domain.timer.TimerEngine
import com.pomodoro.tree.domain.timer.TimerState
import com.pomodoro.tree.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : LifecycleService() {

    @Inject lateinit var timerEngine: TimerEngine
    @Inject lateinit var prefs: SharedPreferences
    @Inject lateinit var dndManager: DndManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var notificationUpdateJob: Job? = null

    private val binder = TimerBinder()

    inner class TimerBinder : Binder() {
        val engine: TimerEngine get() = timerEngine
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START -> {
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION, 25)
                startTimer(durationMinutes)
            }
            ACTION_CANCEL -> {
                cancelTimer()
            }
            ACTION_ACKNOWLEDGE -> {
                acknowledgeCompletion()
            }
            ACTION_RESTORE -> {
                restoreTimer()
            }
        }

        return START_STICKY
    }

    private fun startTimer(durationMinutes: Int) {
        acquireWakeLock()
        dndManager.enableDnd()

        timerEngine.start(durationMinutes, lifecycleScope)
        saveTimerState(durationMinutes)

        startForeground(NOTIFICATION_ID, buildRunningNotification(durationMinutes * 60L))
        startNotificationUpdates()
    }

    private fun restoreTimer() {
        val startRealtime = prefs.getLong(PREF_START_REALTIME, -1)
        val durationMillis = prefs.getLong(PREF_DURATION_MILLIS, -1)
        if (startRealtime < 0 || durationMillis < 0) {
            stopSelf()
            return
        }

        acquireWakeLock()
        timerEngine.restore(startRealtime, durationMillis, lifecycleScope)
        startForeground(NOTIFICATION_ID, buildRunningNotification(durationMillis / 1000))
        startNotificationUpdates()
    }

    private fun cancelTimer() {
        timerEngine.cancel()
        dndManager.restoreDnd()
        clearTimerState()
        releaseWakeLock()
        notificationUpdateJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acknowledgeCompletion() {
        timerEngine.acknowledge()
        dndManager.restoreDnd()
        clearTimerState()
        releaseWakeLock()
        notificationUpdateJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = lifecycleScope.launch {
            while (true) {
                delay(30_000) // Update every 30 seconds
                val state = timerEngine.state.value
                when (state) {
                    is TimerState.Running -> {
                        val notification = buildRunningNotification(state.remainingMillis / 1000)
                        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    is TimerState.Completed -> {
                        val notification = buildCompletedNotification()
                        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                        break
                    }
                    else -> break
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Timer continues even if app is swiped from recents
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        notificationUpdateJob?.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    // --- Notifications ---

    private fun buildRunningNotification(remainingSeconds: Long): Notification {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val timeText = "%d:%02d remaining".format(minutes, seconds)

        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return NotificationCompat.Builder(this, PomodoroTreeApp.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus Session")
            .setContentText(timeText)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun buildCompletedNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return NotificationCompat.Builder(this, PomodoroTreeApp.CHANNEL_COMPLETION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Session Complete!")
            .setContentText("Your tree has grown. Tap to see it.")
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .build()
    }

    // --- Timer state persistence ---

    private fun saveTimerState(durationMinutes: Int) {
        val state = timerEngine.state.value
        if (state is TimerState.Running) {
            prefs.edit()
                .putLong(PREF_START_REALTIME, state.startElapsedRealtime)
                .putLong(PREF_DURATION_MILLIS, state.durationMillis)
                .putLong(PREF_START_TIMESTAMP, System.currentTimeMillis())
                .putInt(PREF_DURATION_MINUTES, durationMinutes)
                .apply()
        }
    }

    private fun clearTimerState() {
        prefs.edit()
            .remove(PREF_START_REALTIME)
            .remove(PREF_DURATION_MILLIS)
            .remove(PREF_START_TIMESTAMP)
            .remove(PREF_DURATION_MINUTES)
            .apply()
    }

    // --- Wake lock ---

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PomodoroTree::TimerWakeLock"
            ).apply {
                acquire(60 * 60 * 1000L) // Max 1 hour
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.pomodoro.tree.ACTION_START"
        const val ACTION_CANCEL = "com.pomodoro.tree.ACTION_CANCEL"
        const val ACTION_ACKNOWLEDGE = "com.pomodoro.tree.ACTION_ACKNOWLEDGE"
        const val ACTION_RESTORE = "com.pomodoro.tree.ACTION_RESTORE"
        const val EXTRA_DURATION = "extra_duration"

        const val PREF_START_REALTIME = "timer_start_realtime"
        const val PREF_DURATION_MILLIS = "timer_duration_millis"
        const val PREF_START_TIMESTAMP = "timer_start_timestamp"
        const val PREF_DURATION_MINUTES = "timer_duration_minutes"

        fun startIntent(context: Context, durationMinutes: Int): Intent {
            return Intent(context, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION, durationMinutes)
            }
        }

        fun cancelIntent(context: Context): Intent {
            return Intent(context, TimerService::class.java).apply {
                action = ACTION_CANCEL
            }
        }

        fun acknowledgeIntent(context: Context): Intent {
            return Intent(context, TimerService::class.java).apply {
                action = ACTION_ACKNOWLEDGE
            }
        }

        fun restoreIntent(context: Context): Intent {
            return Intent(context, TimerService::class.java).apply {
                action = ACTION_RESTORE
            }
        }
    }
}
