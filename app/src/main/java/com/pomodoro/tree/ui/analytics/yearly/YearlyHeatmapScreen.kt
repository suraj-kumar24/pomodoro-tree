package com.pomodoro.tree.ui.analytics.yearly

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.data.db.DaySummary
import com.pomodoro.tree.data.repository.SessionRepository
import com.pomodoro.tree.ui.theme.HeatmapEmpty
import com.pomodoro.tree.ui.theme.HeatmapLevel1
import com.pomodoro.tree.ui.theme.HeatmapLevel2
import com.pomodoro.tree.ui.theme.HeatmapLevel3
import com.pomodoro.tree.ui.theme.HeatmapLevel4
import com.pomodoro.tree.ui.theme.TextMuted
import com.pomodoro.tree.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun YearlyHeatmapScreen(
    repository: SessionRepository,
    modifier: Modifier = Modifier
) {
    val year = LocalDate.now().year
    var heatmapData by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var selectedCount by remember { mutableStateOf(0) }

    LaunchedEffect(year) {
        val summaries = repository.getYearlyHeatmap(year)
        heatmapData = summaries.associate { it.date to it.completedCount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = year.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Heatmap grid
        HeatmapGrid(
            year = year,
            data = heatmapData,
            onDayTap = { date, count ->
                selectedDay = date
                selectedCount = count
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.width(4.dp))
            listOf(HeatmapEmpty, HeatmapLevel1, HeatmapLevel2, HeatmapLevel3, HeatmapLevel4).forEach { color ->
                Canvas(modifier = Modifier
                    .width(12.dp)
                    .height(12.dp)) {
                    drawRoundRect(color = color, cornerRadius = CornerRadius(2f, 2f))
                }
                Spacer(modifier = Modifier.width(2.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }

        // Selected day info
        if (selectedDay != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$selectedDay: $selectedCount sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Year total
        val yearTotal = heatmapData.values.sum()
        Text(
            text = "$yearTotal sessions this year",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun HeatmapGrid(
    year: Int,
    data: Map<String, Int>,
    onDayTap: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDay = LocalDate.of(year, 1, 1)
    val lastDay = LocalDate.of(year, 12, 31)
    val startOfWeek = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                // Calculate which cell was tapped
                val cellSize = size.width / 53f
                val col = (offset.x / cellSize).toInt()
                val row = (offset.y / (size.height / 7f)).toInt()
                val tappedDate = startOfWeek.plusWeeks(col.toLong()).plusDays(row.toLong())

                if (!tappedDate.isBefore(firstDay) && !tappedDate.isAfter(lastDay)) {
                    val dateStr = tappedDate.toString()
                    onDayTap(dateStr, data[dateStr] ?: 0)
                }
            }
        }
    ) {
        val cols = 53
        val rows = 7
        val cellWidth = size.width / cols
        val cellHeight = size.height / rows
        val cellPadding = 1f

        var current = startOfWeek

        for (col in 0 until cols) {
            for (row in 0 until rows) {
                val date = current.plusWeeks(col.toLong()).plusDays(row.toLong())
                if (date.isBefore(firstDay) || date.isAfter(lastDay)) {
                    // Outside the year — draw nothing
                    continue
                }

                val dateStr = date.toString()
                val count = data[dateStr] ?: 0
                val color = heatmapColor(count)

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        col * cellWidth + cellPadding,
                        row * cellHeight + cellPadding
                    ),
                    size = Size(
                        cellWidth - cellPadding * 2,
                        cellHeight - cellPadding * 2
                    ),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

private fun heatmapColor(count: Int): Color {
    return when {
        count == 0 -> HeatmapEmpty
        count <= 2 -> HeatmapLevel1
        count <= 4 -> HeatmapLevel2
        count <= 6 -> HeatmapLevel3
        else -> HeatmapLevel4
    }
}
