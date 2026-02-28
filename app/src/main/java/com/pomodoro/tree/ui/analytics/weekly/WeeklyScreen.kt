package com.pomodoro.tree.ui.analytics.weekly

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.data.db.DaySummary
import com.pomodoro.tree.data.repository.SessionRepository
import com.pomodoro.tree.ui.theme.ForestGreen
import com.pomodoro.tree.ui.theme.ForestGreenLight
import com.pomodoro.tree.ui.theme.OvertimeAmber
import com.pomodoro.tree.ui.theme.ProgressRingTrack
import com.pomodoro.tree.ui.theme.SuccessGreen
import com.pomodoro.tree.ui.theme.TextMuted
import com.pomodoro.tree.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun WeeklyScreen(
    repository: SessionRepository,
    modifier: Modifier = Modifier
) {
    var thisWeekSummary by remember { mutableStateOf<List<DaySummary>>(emptyList()) }
    var thisWeekTotal by remember { mutableIntStateOf(0) }
    var lastWeekTotal by remember { mutableIntStateOf(0) }
    var mostUsedTag by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now()
    val thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastWeekStart = thisWeekStart.minusWeeks(1)

    LaunchedEffect(Unit) {
        thisWeekSummary = repository.getWeeklySummary(thisWeekStart)
        thisWeekTotal = repository.getTotalFocusMinutesForWeek(thisWeekStart)
        lastWeekTotal = repository.getTotalFocusMinutesForWeek(lastWeekStart)
        mostUsedTag = repository.getMostUsedTag(thisWeekStart, thisWeekStart.plusDays(6))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "This Week",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bar chart
        BarChart(
            summaries = thisWeekSummary,
            weekStart = thisWeekStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Key metrics
        MetricRow("Total focus", "${thisWeekTotal}min")

        // Week comparison
        if (lastWeekTotal > 0) {
            val change = ((thisWeekTotal - lastWeekTotal).toFloat() / lastWeekTotal * 100).toInt()
            val prefix = if (change >= 0) "+" else ""
            MetricRow(
                "vs last week",
                "$prefix$change%",
                valueColor = if (change >= 0) SuccessGreen else OvertimeAmber
            )
        }

        val totalCompleted = thisWeekSummary.sumOf { it.completedCount }
        val totalCancelled = thisWeekSummary.sumOf { it.cancelledCount }
        MetricRow("Sessions", "$totalCompleted completed, $totalCancelled cancelled")

        val totalOvertime = thisWeekSummary.sumOf { it.totalOvertimeMinutes }
        if (totalOvertime > 0) {
            MetricRow("Overtime", "${totalOvertime}min", valueColor = OvertimeAmber)
        }

        if (mostUsedTag != null) {
            MetricRow("Top tag", mostUsedTag!!)
        }
    }
}

@Composable
private fun BarChart(
    summaries: List<DaySummary>,
    weekStart: LocalDate,
    modifier: Modifier = Modifier
) {
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val summaryMap = summaries.associateBy { it.date }
    val maxMinutes = summaries.maxOfOrNull { it.totalFocusMinutes }?.coerceAtLeast(30) ?: 30

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val barCount = 7
            val barSpacing = size.width * 0.04f
            val barWidth = (size.width - barSpacing * (barCount + 1)) / barCount

            for (i in 0 until barCount) {
                val date = weekStart.plusDays(i.toLong()).toString()
                val summary = summaryMap[date]
                val minutes = summary?.totalFocusMinutes ?: 0
                val barHeight = if (maxMinutes > 0) {
                    (minutes.toFloat() / maxMinutes) * size.height * 0.9f
                } else 0f

                val x = barSpacing + i * (barWidth + barSpacing)
                val completionRate = if (summary != null && (summary.completedCount + summary.cancelledCount) > 0) {
                    summary.completedCount.toFloat() / (summary.completedCount + summary.cancelledCount)
                } else 1f

                // Background track
                drawRoundRect(
                    color = ProgressRingTrack,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Value bar
                if (barHeight > 0) {
                    val barColor = ForestGreen.copy(alpha = 0.4f + completionRate * 0.6f)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }

        // Day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = ForestGreenLight
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor
        )
    }
}
