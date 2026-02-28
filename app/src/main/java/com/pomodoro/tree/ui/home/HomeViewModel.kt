package com.pomodoro.tree.ui.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pomodoro.tree.data.db.DailyGoalEntity
import com.pomodoro.tree.data.db.PomodoroDatabase
import com.pomodoro.tree.data.db.TagEntity
import com.pomodoro.tree.data.repository.SessionRepository
import com.pomodoro.tree.domain.timer.TimerEngine
import com.pomodoro.tree.domain.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val completedToday: Int = 0,
    val dailyGoal: Int = 8,
    val tags: List<TagEntity> = emptyList(),
    val selectedTag: String? = null,
    val focusDurationMinutes: Int = 25,
    val focusBalanceHours: Float = 0f,
    val availableRewards: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    val timerEngine: TimerEngine,
    private val sessionRepository: SessionRepository,
    private val db: PomodoroDatabase,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    val timerState: StateFlow<TimerState> = timerEngine.state

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.getCompletedCountToday(),
        db.tagDao().getAll(),
        db.dailyGoalDao().getLatestGoal(),
        _selectedTag
    ) { completedCount, tags, latestGoal, selectedTag ->
        HomeUiState(
            completedToday = completedCount,
            dailyGoal = latestGoal?.targetPomodoros ?: 8,
            tags = tags,
            selectedTag = selectedTag,
            focusDurationMinutes = prefs.getInt(PREF_FOCUS_DURATION, 25)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        initializeDefaultTags()
    }

    fun selectTag(tagName: String?) {
        _selectedTag.value = tagName
    }

    fun cycleTag() {
        viewModelScope.launch {
            val tags = uiState.value.tags
            if (tags.isEmpty()) return@launch

            val currentTag = _selectedTag.value
            val currentIndex = tags.indexOfFirst { it.name == currentTag }
            val nextIndex = if (currentIndex < 0 || currentIndex >= tags.size - 1) {
                0
            } else {
                currentIndex + 1
            }
            _selectedTag.value = tags[nextIndex].name
        }
    }

    private fun initializeDefaultTags() {
        viewModelScope.launch {
            val defaults = listOf(
                TagEntity("Work", 0xFF5C7AEA),
                TagEntity("Study", 0xFF9B59B6),
                TagEntity("Personal", 0xFF4A6741)
            )
            db.tagDao().insertAll(defaults)
        }
    }

    fun setDailyGoal(target: Int) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            db.dailyGoalDao().set(DailyGoalEntity(date = today, targetPomodoros = target))
        }
    }

    companion object {
        const val PREF_FOCUS_DURATION = "focus_duration_minutes"
    }
}
