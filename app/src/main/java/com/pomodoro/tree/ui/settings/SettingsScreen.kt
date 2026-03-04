package com.pomodoro.tree.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.ui.components.ScreenHeader
import com.pomodoro.tree.ui.theme.TextMuted
import com.pomodoro.tree.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToRewards: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = "Settings", onBack = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        // Focus duration
        SectionLabel("Focus Duration")
        Text(
            text = "${uiState.focusDurationMinutes} minutes",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(15, 25, 30, 45, 50).forEach { minutes ->
                val isSelected = uiState.focusDurationMinutes == minutes
                Button(
                    onClick = { viewModel.setFocusDuration(minutes) },
                    enabled = !isSelected,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("$minutes")
                }
            }
        }
        Slider(
            value = uiState.focusDurationMinutes.toFloat(),
            onValueChange = { viewModel.setFocusDuration(it.toInt()) },
            valueRange = 5f..90f,
            steps = 16,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Daily goal
        SectionLabel("Daily Goal")
        Text(
            text = "${uiState.dailyGoal} pomodoros",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Slider(
            value = uiState.dailyGoal.toFloat(),
            onValueChange = { viewModel.setDailyGoal(it.toInt()) },
            valueRange = 1f..16f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // DND toggle
        SettingsToggle(
            label = "Do Not Disturb during sessions",
            description = if (!uiState.hasDndPermission) "Permission required" else null,
            checked = uiState.dndEnabled,
            onToggle = {
                if (!uiState.hasDndPermission) {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    context.startActivity(intent)
                } else {
                    viewModel.setDndEnabled(it)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Overtime alerts toggle
        SettingsToggle(
            label = "Overtime alerts",
            description = "Escalating vibrations after session ends",
            checked = uiState.overtimeAlertsEnabled,
            onToggle = { viewModel.setOvertimeAlerts(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Manage rewards link
        SectionLabel("Rewards")
        Text(
            text = "Manage Rewards",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onNavigateToRewards() }
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Export
        SectionLabel("Data")
        Button(
            onClick = { viewModel.exportData(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export as JSON")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // About
        Text(
            text = "Pomodoro Tree v1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String?,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
