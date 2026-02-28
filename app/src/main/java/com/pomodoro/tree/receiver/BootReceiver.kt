package com.pomodoro.tree.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pomodoro.tree.service.TimerService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        val startRealtime = prefs.getLong(TimerService.PREF_START_REALTIME, -1)
        val durationMillis = prefs.getLong(TimerService.PREF_DURATION_MILLIS, -1)

        if (startRealtime >= 0 && durationMillis >= 0) {
            // There was an active session when the phone rebooted — restore it
            val restoreIntent = TimerService.restoreIntent(context)
            context.startForegroundService(restoreIntent)
        }
    }
}
