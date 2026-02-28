package com.pomodoro.tree.ui.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.data.db.RewardEntity
import com.pomodoro.tree.ui.theme.ForestGreen
import com.pomodoro.tree.ui.theme.OvertimeAmber
import com.pomodoro.tree.ui.theme.TextMuted
import com.pomodoro.tree.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val EMOJI_OPTIONS = listOf(
    "\uD83C\uDF81", "\u2615", "\uD83D\uDCDA", "\uD83C\uDFAC", "\uD83C\uDF55",
    "\uD83C\uDFAE", "\uD83C\uDFA7", "\u2708\uFE0F", "\uD83D\uDECD\uFE0F", "\uD83E\uDDC1",
    "\uD83C\uDF7D\uFE0F", "\uD83C\uDFB5", "\uD83D\uDC5F", "\uD83C\uDF89", "\uD83C\uDF3A",
    "\uD83D\uDCBB", "\uD83C\uDFD6\uFE0F", "\uD83D\uDE97", "\uD83C\uDFB3", "\uD83E\uDDD8"
)

@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var redeemConfirm by remember { mutableStateOf<RewardEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Balance header
        item {
            Column {
                Text(
                    text = "Rewards",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "%.1fh".format(uiState.focusBalanceHours),
                    style = MaterialTheme.typography.displayLarge,
                    color = ForestGreen
                )
                Text(
                    text = "Focus earned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Active rewards
        if (uiState.activeRewards.isEmpty()) {
            item {
                Text(
                    text = "No rewards yet. Add one to get started!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }

        items(uiState.activeRewards) { reward ->
            RewardCard(
                reward = reward,
                balance = uiState.focusBalanceHours,
                onRedeem = { redeemConfirm = reward },
                onDelete = { viewModel.deleteReward(reward.id) }
            )
        }

        // Add reward button
        item {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("+ Add Reward")
            }
        }

        // Redeemed section
        if (uiState.redeemedRewards.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Redeemed (${uiState.redeemedRewards.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.clickable { viewModel.toggleRedeemedSection() }
                )
            }

            if (uiState.showRedeemedSection) {
                items(uiState.redeemedRewards) { reward ->
                    RedeemedCard(reward)
                }
            }
        }
    }

    // Add reward dialog
    if (showAddDialog) {
        AddRewardDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, cost, emoji ->
                viewModel.addReward(name, cost, emoji)
                showAddDialog = false
            }
        )
    }

    // Redeem confirmation dialog
    if (redeemConfirm != null) {
        val reward = redeemConfirm!!
        AlertDialog(
            onDismissRequest = { redeemConfirm = null },
            title = { Text("Treat yourself!") },
            text = {
                Text("Redeem \"${reward.name}\"? This uses ${reward.focusHoursCost}h from your balance.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.redeemReward(reward.id)
                    redeemConfirm = null
                }) {
                    Text("Redeem")
                }
            },
            dismissButton = {
                TextButton(onClick = { redeemConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RewardCard(
    reward: RewardEntity,
    balance: Float,
    onRedeem: () -> Unit,
    onDelete: () -> Unit
) {
    val canAfford = balance >= reward.focusHoursCost
    val progress = (balance / reward.focusHoursCost).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = reward.iconEmoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reward.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (canAfford) "Ready!" else "%.1fh more".format(reward.focusHoursCost - balance),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (canAfford) ForestGreen else TextMuted
                )
            }
            Text(
                text = "%.1fh".format(reward.focusHoursCost),
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = if (canAfford) ForestGreen else OvertimeAmber.copy(alpha = 0.6f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        if (canAfford) {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onRedeem,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Redeem")
            }
        }
    }
}

@Composable
private fun RedeemedCard(reward: RewardEntity) {
    val date = reward.redeemedAt?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = reward.iconEmoji, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reward.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "Redeemed $date",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Text(
            text = "%.1fh".format(reward.focusHoursCost),
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

@Composable
private fun AddRewardDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, cost: Float, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(EMOJI_OPTIONS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reward") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Reward name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Cost (hours)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))

                // Emoji grid (4 columns, 5 rows)
                Column {
                    EMOJI_OPTIONS.chunked(5).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .clickable { selectedEmoji = emoji }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costText.toFloatOrNull()
                    if (name.isNotBlank() && cost != null && cost > 0) {
                        onAdd(name, cost, selectedEmoji)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
