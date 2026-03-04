package com.pomodoro.tree.ui.active

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.domain.timer.TimerEngine
import com.pomodoro.tree.domain.timer.TimerState
import com.pomodoro.tree.ui.components.ProgressRing
import com.pomodoro.tree.ui.components.TreeCanvas
import com.pomodoro.tree.ui.theme.CancelRed
import com.pomodoro.tree.ui.theme.TextMuted
import com.pomodoro.tree.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    timerEngine: TimerEngine,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timerState by timerEngine.state.collectAsState()
    var showCancelSheet by remember { mutableStateOf(false) }

    // Back button / swipe down triggers cancel confirmation
    BackHandler {
        showCancelSheet = true
    }

    val running = timerState as? TimerState.Running

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Progress ring with tree inside
            Box(contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = running?.progress ?: 0f,
                    size = 260.dp,
                    strokeWidth = 2.dp,
                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )

                TreeCanvas(
                    progress = running?.progress ?: 0f,
                    modifier = Modifier.size(180.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Time remaining (subtle)
            Text(
                text = running?.remainingFormatted ?: "0:00",
                style = MaterialTheme.typography.headlineMedium,
                color = TextSecondary
            )
        }
    }

    // Cancel confirmation bottom sheet
    if (showCancelSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCancelSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "End session early?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your tree will wither.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showCancelSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Keep Going")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        showCancelSheet = false
                        onCancel()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = CancelRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("End Session")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
