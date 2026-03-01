package com.pomodoro.tree.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pomodoro.tree.domain.timer.TimerEngine
import com.pomodoro.tree.domain.timer.TimerState
import com.pomodoro.tree.ui.MainActivity

class PomodoroGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Read timer state from SharedPreferences (widget can't access Hilt singletons directly)
        val prefs = context.getSharedPreferences("pomodoro_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getLong("timer_start_realtime", -1) >= 0
        val completedToday = 0 // Widget reads from a simpler source or updates via service

        provideContent {
            GlanceTheme {
                WidgetContent(isRunning = isRunning)
            }
        }
    }
}

@Composable
private fun WidgetContent(isRunning: Boolean) {
    val bgColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF1A1A2E))
    val textColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFFD4D4D4))
    val accentColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF4A6741))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tree emoji as placeholder (Canvas not available in Glance)
            Text(
                text = if (isRunning) "\uD83C\uDF33" else "\uD83C\uDF31", // 🌳 or 🌱
                style = TextStyle(fontSize = 36.sp)
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = if (isRunning) "Focusing..." else "Focus",
                style = TextStyle(
                    color = if (isRunning) accentColor else textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

class PomodoroWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PomodoroGlanceWidget()
}
