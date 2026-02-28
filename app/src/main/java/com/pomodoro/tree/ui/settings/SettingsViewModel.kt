package com.pomodoro.tree.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.tree.data.db.DailyGoalEntity
import com.pomodoro.tree.data.db.PomodoroDatabase
import com.pomodoro.tree.data.export.JsonExporter
import com.pomodoro.tree.service.DndManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val focusDurationMinutes: Int = 25,
    val dailyGoal: Int = 8,
    val dndEnabled: Boolean = false,
    val hasDndPermission: Boolean = false,
    val overtimeAlertsEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    private val db: PomodoroDatabase,
    private val dndManager: DndManager,
    private val jsonExporter: JsonExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            focusDurationMinutes = prefs.getInt(PREF_FOCUS_DURATION, 25),
            dailyGoal = prefs.getInt(PREF_DAILY_GOAL, 8),
            dndEnabled = dndManager.isDndEnabled(),
            hasDndPermission = dndManager.hasPermission(),
            overtimeAlertsEnabled = prefs.getBoolean(PREF_OVERTIME_ALERTS, true)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setFocusDuration(minutes: Int) {
        val clamped = minutes.coerceIn(5, 90)
        prefs.edit().putInt(PREF_FOCUS_DURATION, clamped).apply()
        _uiState.update { it.copy(focusDurationMinutes = clamped) }
    }

    fun setDailyGoal(target: Int) {
        val clamped = target.coerceIn(1, 16)
        prefs.edit().putInt(PREF_DAILY_GOAL, clamped).apply()
        _uiState.update { it.copy(dailyGoal = clamped) }

        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            db.dailyGoalDao().set(DailyGoalEntity(date = today, targetPomodoros = clamped))
        }
    }

    fun setDndEnabled(enabled: Boolean) {
        dndManager.setDndEnabled(enabled)
        _uiState.update { it.copy(dndEnabled = enabled, hasDndPermission = dndManager.hasPermission()) }
    }

    fun setOvertimeAlerts(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_OVERTIME_ALERTS, enabled).apply()
        _uiState.update { it.copy(overtimeAlertsEnabled = enabled) }
    }

    fun exportData(context: Context) {
        viewModelScope.launch {
            val json = jsonExporter.exportAll()
            val file = File(context.cacheDir, "pomodoro_tree_export.json")
            file.writeText(json)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export data"))
        }
    }

    companion object {
        const val PREF_FOCUS_DURATION = "focus_duration_minutes"
        const val PREF_DAILY_GOAL = "daily_goal"
        const val PREF_OVERTIME_ALERTS = "overtime_alerts_enabled"
    }
}
