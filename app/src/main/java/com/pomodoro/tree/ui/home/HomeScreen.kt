package com.pomodoro.tree.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.ui.components.ProgressRing
import com.pomodoro.tree.ui.components.TreeCanvas
import com.pomodoro.tree.ui.theme.ForestGreen
import com.pomodoro.tree.ui.theme.TextMuted
import com.pomodoro.tree.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartSession: () -> Unit,
    onNavigateToRewards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Pulsing animation for the seed
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Daily goal progress ring with seed inside
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(pulseScale)
                    .clickable { onStartSession() }
            ) {
                ProgressRing(
                    progress = if (uiState.dailyGoal > 0) {
                        uiState.completedToday.toFloat() / uiState.dailyGoal
                    } else 0f,
                    size = 200.dp,
                    strokeWidth = 3.dp
                )

                // Seed / starting tree icon
                TreeCanvas(
                    progress = 0f,
                    modifier = Modifier.size(100.dp)
                )
            }

            // "Tap to focus" hint text
            Text(
                text = "Tap to focus",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .alpha(0.6f)
            )

            // Daily goal progress text
            Text(
                text = "${uiState.completedToday}/${uiState.dailyGoal} today",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Tag selector pill at bottom
        if (uiState.tags.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .clickable { viewModel.cycleTag() }
            ) {
                Text(
                    text = uiState.selectedTag ?: "No tag",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.selectedTag != null) ForestGreen else TextMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Focus balance + rewards hint at bottom
        Text(
            text = "%.1fh earned".format(uiState.focusBalanceHours),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .clickable { onNavigateToRewards() }
                .alpha(0.5f)
        )
    }
}
