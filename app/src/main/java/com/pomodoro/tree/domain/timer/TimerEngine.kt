package com.pomodoro.tree.domain.timer

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TimerState {
    data object Idle : TimerState()

    data class Running(
        val startElapsedRealtime: Long,
        val durationMillis: Long,
        val elapsedMillis: Long = 0
    ) : TimerState() {
        val progress: Float
            get() = (elapsedMillis.toFloat() / durationMillis).coerceIn(0f, 1f)

        val remainingMillis: Long
            get() = (durationMillis - elapsedMillis).coerceAtLeast(0)

        val remainingFormatted: String
            get() {
                val totalSeconds = remainingMillis / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                return "%d:%02d".format(minutes, seconds)
            }
    }

    data class Completed(
        val durationMillis: Long,
        val overtimeMillis: Long = 0,
        val completedAtElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ) : TimerState() {
        val overtimeFormatted: String
            get() {
                val totalSeconds = overtimeMillis / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                return "+%d:%02d".format(minutes, seconds)
            }

        /** 0.0 = just completed, 1.0 = fully wilted (at 10 min overtime) */
        val wiltProgress: Float
            get() {
                val wiltStartMs = 2 * 60 * 1000L  // wilting starts at 2 min overtime
                val wiltFullMs = 10 * 60 * 1000L   // fully wilted at 10 min
                if (overtimeMillis < wiltStartMs) return 0f
                return ((overtimeMillis - wiltStartMs).toFloat() / (wiltFullMs - wiltStartMs))
                    .coerceIn(0f, 1f)
            }
    }

    data class Cancelled(
        val elapsedMillis: Long,
        val durationMillis: Long
    ) : TimerState()

    /** Terminal state — session has been acknowledged and saved. */
    data object Finished : TimerState()
}

class TimerEngine {

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var tickJob: Job? = null

    fun start(durationMinutes: Int, scope: CoroutineScope) {
        if (_state.value !is TimerState.Idle && _state.value !is TimerState.Finished) return

        val durationMillis = durationMinutes * 60 * 1000L
        val startRealtime = SystemClock.elapsedRealtime()

        _state.value = TimerState.Running(
            startElapsedRealtime = startRealtime,
            durationMillis = durationMillis
        )

        tickJob = scope.launch {
            while (true) {
                delay(250) // update 4x/sec for smooth progress
                val current = _state.value
                if (current !is TimerState.Running) break

                val elapsed = SystemClock.elapsedRealtime() - current.startElapsedRealtime
                if (elapsed >= current.durationMillis) {
                    _state.value = TimerState.Completed(
                        durationMillis = current.durationMillis,
                        completedAtElapsedRealtime = SystemClock.elapsedRealtime()
                    )
                    // Start overtime tracking
                    trackOvertime(scope)
                    break
                } else {
                    _state.value = current.copy(elapsedMillis = elapsed)
                }
            }
        }
    }

    /**
     * Restore a running timer from saved state (e.g., after process death or reboot).
     */
    fun restore(startElapsedRealtime: Long, durationMillis: Long, scope: CoroutineScope) {
        val elapsed = SystemClock.elapsedRealtime() - startElapsedRealtime
        if (elapsed >= durationMillis) {
            // Timer already completed while we were dead
            _state.value = TimerState.Completed(
                durationMillis = durationMillis,
                overtimeMillis = elapsed - durationMillis,
                completedAtElapsedRealtime = startElapsedRealtime + durationMillis
            )
            trackOvertime(scope)
        } else {
            _state.value = TimerState.Running(
                startElapsedRealtime = startElapsedRealtime,
                durationMillis = durationMillis,
                elapsedMillis = elapsed
            )
            tickJob = scope.launch {
                while (true) {
                    delay(250)
                    val current = _state.value
                    if (current !is TimerState.Running) break

                    val currentElapsed = SystemClock.elapsedRealtime() - current.startElapsedRealtime
                    if (currentElapsed >= current.durationMillis) {
                        _state.value = TimerState.Completed(
                            durationMillis = current.durationMillis,
                            completedAtElapsedRealtime = SystemClock.elapsedRealtime()
                        )
                        trackOvertime(scope)
                        break
                    } else {
                        _state.value = current.copy(elapsedMillis = currentElapsed)
                    }
                }
            }
        }
    }

    fun cancel() {
        val current = _state.value
        tickJob?.cancel()
        tickJob = null

        when (current) {
            is TimerState.Running -> {
                _state.value = TimerState.Cancelled(
                    elapsedMillis = current.elapsedMillis,
                    durationMillis = current.durationMillis
                )
            }
            else -> { /* ignore cancel if not running */ }
        }
    }

    fun acknowledge() {
        tickJob?.cancel()
        tickJob = null
        _state.value = TimerState.Finished
    }

    fun reset() {
        tickJob?.cancel()
        tickJob = null
        _state.value = TimerState.Idle
    }

    private fun trackOvertime(scope: CoroutineScope) {
        tickJob = scope.launch {
            while (true) {
                delay(1000) // update every second for overtime counter
                val current = _state.value
                if (current !is TimerState.Completed) break

                val overtime = SystemClock.elapsedRealtime() - current.completedAtElapsedRealtime
                _state.value = current.copy(overtimeMillis = overtime)
            }
        }
    }
}
