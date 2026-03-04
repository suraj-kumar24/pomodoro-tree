package com.pomodoro.tree.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.pomodoro.tree.data.repository.SessionRepository
import kotlinx.coroutines.launch
import com.pomodoro.tree.domain.model.SessionStatus
import com.pomodoro.tree.domain.timer.TimerEngine
import com.pomodoro.tree.domain.timer.TimerState
import com.pomodoro.tree.receiver.AlertReceiver
import com.pomodoro.tree.service.TimerService
import com.pomodoro.tree.ui.active.ActiveSessionScreen
import com.pomodoro.tree.ui.analytics.AnalyticsScreen
import com.pomodoro.tree.ui.analytics.RewardsScreen
import com.pomodoro.tree.ui.analytics.RewardsViewModel
import com.pomodoro.tree.ui.completion.CompletionScreen
import com.pomodoro.tree.ui.home.HomeScreen
import com.pomodoro.tree.ui.home.HomeViewModel
import com.pomodoro.tree.ui.settings.SettingsScreen
import com.pomodoro.tree.ui.settings.SettingsViewModel

enum class Screen {
    HOME, ACTIVE, COMPLETION, ANALYTICS, SETTINGS, REWARDS
}

@Composable
fun AppNavigation(
    timerEngine: TimerEngine,
    sessionRepository: SessionRepository
) {
    val context = LocalContext.current
    val timerState by timerEngine.state.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var sessionStartTimestamp by remember { mutableStateOf(0L) }
    var sessionTag by remember { mutableStateOf<String?>(null) }
    var sessionDurationMinutes by remember { mutableStateOf(25) }

    // Save session and schedule alerts on state transitions
    LaunchedEffect(timerState) {
        when (val state = timerState) {
            is TimerState.Running -> {
                if (sessionStartTimestamp == 0L) {
                    sessionStartTimestamp = System.currentTimeMillis()
                }
            }
            is TimerState.Completed -> {
                if (state.overtimeMillis == 0L) {
                    scheduleEscalatingAlerts(context)
                }
            }
            is TimerState.Cancelled -> {
                sessionRepository.saveSession(
                    startTimestamp = sessionStartTimestamp,
                    plannedDurationMinutes = sessionDurationMinutes,
                    actualDurationSeconds = state.elapsedMillis / 1000,
                    overtimeSeconds = 0,
                    status = SessionStatus.CANCELLED,
                    tag = sessionTag
                )
                sessionStartTimestamp = 0L
            }
            is TimerState.Finished -> { }
            is TimerState.Idle -> { }
        }
    }

    // Auto-navigate based on timer state
    val autoScreen = when (timerState) {
        is TimerState.Running -> Screen.ACTIVE
        is TimerState.Completed -> Screen.COMPLETION
        is TimerState.Idle, is TimerState.Finished -> {
            if (currentScreen == Screen.ACTIVE || currentScreen == Screen.COMPLETION) Screen.HOME
            else currentScreen
        }
        is TimerState.Cancelled -> {
            if (currentScreen == Screen.ACTIVE) Screen.HOME
            else currentScreen
        }
    }
    if (autoScreen != currentScreen &&
        (autoScreen == Screen.ACTIVE || autoScreen == Screen.COMPLETION ||
            (autoScreen == Screen.HOME && (currentScreen == Screen.ACTIVE || currentScreen == Screen.COMPLETION)))
    ) {
        currentScreen = autoScreen
    }

    // Android back button — go back to HOME from sub-pages
    BackHandler(enabled = currentScreen != Screen.HOME && currentScreen != Screen.ACTIVE) {
        currentScreen = Screen.HOME
    }

    // Swipe detection for gesture navigation
    val swipeThreshold = 80f
    val currentScreenState by rememberUpdatedState(currentScreen)
    val swipeModifier = Modifier
        .pointerInput(Unit) {
            var totalX = 0f
            var totalY = 0f
            var swiped = false
            detectDragGestures(
                onDragStart = {
                    totalX = 0f
                    totalY = 0f
                    swiped = false
                },
                onDragEnd = {
                    totalX = 0f
                    totalY = 0f
                    swiped = false
                },
                onDragCancel = {
                    totalX = 0f
                    totalY = 0f
                    swiped = false
                },
                onDrag = { change, dragAmount ->
                    val screen = currentScreenState
                    if (screen == Screen.ACTIVE || screen == Screen.COMPLETION) return@detectDragGestures
                    change.consume()
                    totalX += dragAmount.x
                    totalY += dragAmount.y
                    if (!swiped) {
                        val isHorizontal = kotlin.math.abs(totalX) > kotlin.math.abs(totalY)
                        if (isHorizontal && kotlin.math.abs(totalX) > swipeThreshold) {
                            swiped = true
                            if (totalX < 0) { // Swipe left
                                currentScreen = when (screen) {
                                    Screen.HOME -> Screen.ANALYTICS
                                    else -> screen
                                }
                            } else { // Swipe right — always go back to HOME
                                currentScreen = when (screen) {
                                    Screen.ANALYTICS, Screen.SETTINGS, Screen.REWARDS -> Screen.HOME
                                    else -> screen
                                }
                            }
                        } else if (!isHorizontal && kotlin.math.abs(totalY) > swipeThreshold) {
                            swiped = true
                            if (totalY > 0) { // Swipe down
                                currentScreen = when (screen) {
                                    Screen.HOME -> Screen.SETTINGS
                                    else -> screen
                                }
                            } else { // Swipe up
                                currentScreen = when (screen) {
                                    Screen.HOME -> Screen.REWARDS
                                    else -> screen
                                }
                            }
                        }
                    }
                }
            )
        }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding().then(swipeModifier)) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                when {
                    targetState == Screen.ACTIVE || targetState == Screen.COMPLETION ->
                        slideInVertically { -it } togetherWith slideOutVertically { it }
                    // Going to sub-pages
                    initialState == Screen.HOME && targetState == Screen.ANALYTICS ->
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    initialState == Screen.HOME && targetState == Screen.SETTINGS ->
                        slideInVertically { it } togetherWith slideOutVertically { -it }
                    initialState == Screen.HOME && targetState == Screen.REWARDS ->
                        slideInVertically { -it } togetherWith slideOutVertically { it }
                    // Coming back to HOME
                    targetState == Screen.HOME && initialState == Screen.ANALYTICS ->
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    targetState == Screen.HOME && initialState == Screen.SETTINGS ->
                        slideInVertically { -it } togetherWith slideOutVertically { it }
                    targetState == Screen.HOME && initialState == Screen.REWARDS ->
                        slideInVertically { it } togetherWith slideOutVertically { -it }
                    else ->
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "screenTransition"
        ) { screen ->
            when (screen) {
                Screen.HOME -> {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    HomeScreen(
                        viewModel = homeViewModel,
                        onStartSession = {
                            val state = homeViewModel.uiState.value
                            sessionDurationMinutes = state.focusDurationMinutes
                            sessionTag = state.selectedTag
                            sessionStartTimestamp = System.currentTimeMillis()
                            context.startForegroundService(
                                TimerService.startIntent(context, state.focusDurationMinutes)
                            )
                        },
                        onNavigateToRewards = { currentScreen = Screen.REWARDS }
                    )
                }

                Screen.ACTIVE -> {
                    ActiveSessionScreen(
                        timerEngine = timerEngine,
                        onCancel = {
                            context.startService(TimerService.cancelIntent(context))
                        }
                    )
                }

                Screen.COMPLETION -> {
                    CompletionScreen(
                        timerEngine = timerEngine,
                        onAcknowledge = {
                            val completed = timerEngine.state.value as? TimerState.Completed
                            if (completed != null) {
                                kotlinx.coroutines.MainScope().launch {
                                    sessionRepository.saveSession(
                                        startTimestamp = sessionStartTimestamp,
                                        plannedDurationMinutes = sessionDurationMinutes,
                                        actualDurationSeconds = completed.durationMillis / 1000,
                                        overtimeSeconds = completed.overtimeMillis / 1000,
                                        status = SessionStatus.COMPLETED,
                                        tag = sessionTag
                                    )
                                    sessionStartTimestamp = 0L
                                }
                            }
                            cancelEscalatingAlerts(context)
                            context.startService(TimerService.acknowledgeIntent(context))
                            timerEngine.reset()
                        }
                    )
                }

                Screen.ANALYTICS -> {
                    AnalyticsScreen(
                        repository = sessionRepository,
                        onBack = { currentScreen = Screen.HOME }
                    )
                }

                Screen.SETTINGS -> {
                    val settingsViewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToRewards = { currentScreen = Screen.REWARDS },
                        onBack = { currentScreen = Screen.HOME }
                    )
                }

                Screen.REWARDS -> {
                    val rewardsViewModel: RewardsViewModel = hiltViewModel()
                    RewardsScreen(
                        viewModel = rewardsViewModel,
                        onBack = { currentScreen = Screen.HOME }
                    )
                }
            }
        }
    }
}

fun scheduleEscalatingAlerts(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    AlertReceiver.ALERT_SCHEDULE_MINUTES.forEachIndexed { index, delayMinutes ->
        val intent = Intent(context, AlertReceiver::class.java).apply {
            putExtra(AlertReceiver.EXTRA_ALERT_LEVEL, index.coerceAtMost(2))
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1000 + index,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + delayMinutes * 60 * 1000L,
            pendingIntent
        )
    }
}

fun cancelEscalatingAlerts(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    AlertReceiver.ALERT_SCHEDULE_MINUTES.forEachIndexed { index, _ ->
        val intent = Intent(context, AlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1000 + index,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
