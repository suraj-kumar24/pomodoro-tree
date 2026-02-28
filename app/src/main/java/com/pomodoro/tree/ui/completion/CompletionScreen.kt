package com.pomodoro.tree.ui.completion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.domain.timer.TimerEngine
import com.pomodoro.tree.domain.timer.TimerState
import com.pomodoro.tree.ui.components.TreeCanvas
import com.pomodoro.tree.ui.theme.OvertimeAmber
import com.pomodoro.tree.ui.theme.SuccessGreen
import com.pomodoro.tree.ui.theme.TextSecondary

@Composable
fun CompletionScreen(
    timerEngine: TimerEngine,
    onAcknowledge: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timerState by timerEngine.state.collectAsState()
    val completed = timerState as? TimerState.Completed

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // "Session Complete" heading
            Text(
                text = "Session Complete",
                style = MaterialTheme.typography.headlineLarge,
                color = SuccessGreen
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Full tree with overtime wilting
            TreeCanvas(
                progress = 1f,
                wiltProgress = completed?.wiltProgress ?: 0f,
                modifier = Modifier.size(220.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Session duration
            if (completed != null) {
                val minutes = completed.durationMillis / 1000 / 60
                Text(
                    text = "${minutes}min focus session",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }

            // Overtime counter (appears after 30 seconds)
            if (completed != null && completed.overtimeMillis > 30_000) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = completed.overtimeFormatted + " overtime",
                    style = MaterialTheme.typography.titleMedium,
                    color = OvertimeAmber
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Done button
            Button(
                onClick = onAcknowledge,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("Done")
            }
        }
    }
}
