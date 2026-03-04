package com.pomodoro.tree.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pomodoro.tree.data.db.DaySummary
import com.pomodoro.tree.data.db.SessionEntity
import com.pomodoro.tree.data.repository.SessionRepository
import com.pomodoro.tree.domain.model.SessionStatus
import com.pomodoro.tree.ui.components.MiniTreeCanvas
import com.pomodoro.tree.ui.components.ScreenHeader
import com.pomodoro.tree.ui.theme.ForestGreen
import com.pomodoro.tree.ui.theme.ForestGreenLight
import com.pomodoro.tree.ui.theme.HeatmapEmpty
import com.pomodoro.tree.ui.theme.HeatmapLevel1
import com.pomodoro.tree.ui.theme.HeatmapLevel2
import com.pomodoro.tree.ui.theme.HeatmapLevel3
import com.pomodoro.tree.ui.theme.HeatmapLevel4
import com.pomodoro.tree.ui.theme.OvertimeAmber
import com.pomodoro.tree.ui.theme.ProgressRingTrack
import com.pomodoro.tree.ui.theme.SuccessGreen
import com.pomodoro.tree.ui.theme.TextMuted
import com.pomodoro.tree.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    repository: SessionRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today", "Weekly", "Yearly")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        ScreenHeader(title = "Analytics", onBack = onBack)

        // Tab selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = ForestGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ForestGreen
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) ForestGreen else TextMuted
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab content
        when (selectedTab) {
            0 -> DailyContent(
                sessionsFlow = repository.getSessionsForToday()
            )
            1 -> WeeklyContent(repository = repository)
            2 -> YearlyContent(repository = repository)
        }
    }
}

// --- Daily Tab ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyContent(
    sessionsFlow: Flow<List<SessionEntity>>
) {
    val sessions by sessionsFlow.collectAsState(initial = emptyList())
    var selectedSession by remember { mutableStateOf<SessionEntity?>(null) }

    val completedCount = sessions.count { it.status == SessionStatus.COMPLETED }
    val totalMinutes = sessions.filter { it.status == SessionStatus.COMPLETED }
        .sumOf { it.actualDurationSeconds } / 60
    val totalOvertime = sessions.sumOf { it.overtimeSeconds } / 60

    Column {
        Text(
            text = buildString {
                append("$completedCount trees")
                append(" \u00b7 ${totalMinutes}m focus")
                if (totalOvertime > 0) append(" \u00b7 ${totalOvertime}m overtime")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No sessions yet today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }
        } else {
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
                                val wiltStart = 2 * 60L
                                val wiltFull = 10 * 60L
                                if (session.overtimeSeconds < wiltStart) 0f
                                else ((session.overtimeSeconds - wiltStart).toFloat() / (wiltFull - wiltStart)).coerceIn(0f, 1f)
                            } else 0f,
                            modifier = Modifier.size(64.dp)
                        )
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

// --- Weekly Tab ---

@Composable
private fun WeeklyContent(repository: SessionRepository) {
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
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        BarChart(
            summaries = thisWeekSummary,
            weekStart = thisWeekStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        MetricRow("Total focus", "${thisWeekTotal}min")

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

                drawRoundRect(
                    color = ProgressRingTrack,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(4f, 4f)
                )

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
    valueColor: Color = ForestGreenLight
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

// --- Yearly Tab ---

@Composable
private fun YearlyContent(repository: SessionRepository) {
    val year = LocalDate.now().year
    var heatmapData by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var selectedDay by remember { mutableStateOf<String?>(null) }
    var selectedCount by remember { mutableStateOf(0) }

    LaunchedEffect(year) {
        val summaries = repository.getYearlyHeatmap(year)
        heatmapData = summaries.associate { it.date to it.completedCount }
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // Month labels row
        val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            monthLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Heatmap grid — horizontally scrollable with square cells
        HeatmapGrid(
            year = year,
            data = heatmapData,
            onDayTap = { date, count ->
                selectedDay = date
                selectedCount = count
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.width(6.dp))
            listOf(HeatmapEmpty, HeatmapLevel1, HeatmapLevel2, HeatmapLevel3, HeatmapLevel4).forEach { color ->
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawRoundRect(color = color, cornerRadius = CornerRadius(2f, 2f))
                }
                Spacer(modifier = Modifier.width(3.dp))
            }
            Spacer(modifier = Modifier.width(3.dp))
            Text("More", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }

        // Selected day info
        if (selectedDay != null) {
            Spacer(modifier = Modifier.height(12.dp))
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
            text = "$yearTotal sessions in $year",
            style = MaterialTheme.typography.titleMedium,
            color = ForestGreen
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
        val cellPadding = 1.5f

        for (col in 0 until cols) {
            for (row in 0 until rows) {
                val date = startOfWeek.plusWeeks(col.toLong()).plusDays(row.toLong())
                if (date.isBefore(firstDay) || date.isAfter(lastDay)) continue

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
                    cornerRadius = CornerRadius(3f, 3f)
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
