package com.pomodoro.tree.service

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DndManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPreferences
) {
    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun isDndEnabled(): Boolean {
        return prefs.getBoolean(PREF_DND_ENABLED, false)
    }

    fun setDndEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DND_ENABLED, enabled).apply()
    }

    fun hasPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun enableDnd() {
        if (!isDndEnabled() || !hasPermission()) return

        // Save current DND state so we can restore it later
        val currentFilter = notificationManager.currentInterruptionFilter
        prefs.edit().putInt(PREF_PREVIOUS_FILTER, currentFilter).apply()

        notificationManager.setInterruptionFilter(
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )
    }

    fun restoreDnd() {
        if (!isDndEnabled() || !hasPermission()) return

        val previousFilter = prefs.getInt(PREF_PREVIOUS_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        notificationManager.setInterruptionFilter(previousFilter)
        prefs.edit().remove(PREF_PREVIOUS_FILTER).apply()
    }

    companion object {
        private const val PREF_DND_ENABLED = "dnd_enabled"
        private const val PREF_PREVIOUS_FILTER = "dnd_previous_filter"
    }
}
