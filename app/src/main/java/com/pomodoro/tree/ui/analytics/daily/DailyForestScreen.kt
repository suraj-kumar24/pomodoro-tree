package com.pomodoro.tree.ui.analytics.daily

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.data.db.SessionEntity
import com.pomodoro.tree.domain.model.SessionStatus
import com.pomodoro.tree.ui.components.MiniTreeCanvas
import com.pomodoro.tree.ui.theme.TextMuted
import com.pomodoro.tree.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyForestScreen(
    sessionsFlow: Flow<List<SessionEntity>>,
    dateLabel: String,
    modifier: Modifier = Modifier
) {
    val sessions by sessionsFlow.collectAsState(initial = emptyList())
    var selectedSession by remember { mutableStateOf<SessionEntity?>(null) }

    val completedCount = sessions.count { it.status == SessionStatus.COMPLETED }
    val totalMinutes = sessions.filter { it.status == SessionStatus.COMPLETED }
        .sumOf { it.actualDurationSeconds } / 60
    val totalOvertime = sessions.sumOf { it.overtimeSeconds } / 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Date and summary
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = buildString {
                append("$completedCount trees")
                append(" · ${totalMinutes}m focus")
                if (totalOvertime > 0) append(" · ${totalOvertime}m overtime")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No sessions yet today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }
        } else {
            // Tree grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions) { session ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedSession = session }
                    ) {
                        MiniTreeCanvas(
                            isCompleted = session.status == SessionStatus.COMPLETED,
                            wiltProgress = if (session.status == SessionStatus.COMPLETED) {
                                // Calculate wilt from overtime
                                val wiltStart = 2 * 60L
                                val wiltFull = 10 * 60L
                                if (session.overtimeSeconds < wiltStart) 0f
                                else ((session.overtimeSeconds - wiltStart).toFloat() / (wiltFull - wiltStart)).coerceIn(0f, 1f)
                            } else 0f,
                            modifier = Modifier.size(64.dp)
                        )

                        // Tag dot
                        if (session.tag != null) {
                            Text(
                                text = session.tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // Session detail bottom sheet
    if (selectedSession != null) {
        val session = selectedSession!!
        ModalBottomSheet(
            onDismissRequest = { selectedSession = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                val time = Instant.ofEpochMilli(session.startTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("h:mm a"))

                Text(
                    text = if (session.status == SessionStatus.COMPLETED) "Completed Session" else "Cancelled Session",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Started at $time", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Duration: ${session.actualDurationSeconds / 60}min", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                if (session.overtimeSeconds > 0) {
                    Text("Overtime: ${session.overtimeSeconds / 60}min", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }

                if (session.tag != null) {
                    Text("Tag: ${session.tag}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
