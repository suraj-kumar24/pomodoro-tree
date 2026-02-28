package com.pomodoro.tree.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pomodoro.tree.data.repository.SessionRepository
import com.pomodoro.tree.domain.timer.TimerEngine
import com.pomodoro.tree.ui.theme.DeepNavy
import com.pomodoro.tree.ui.theme.PomodoroTreeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var timerEngine: TimerEngine
    @Inject lateinit var sessionRepository: SessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PomodoroTreeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepNavy
                ) {
                    AppNavigation(
                        timerEngine = timerEngine,
                        sessionRepository = sessionRepository
                    )
                }
            }
        }
    }
}
