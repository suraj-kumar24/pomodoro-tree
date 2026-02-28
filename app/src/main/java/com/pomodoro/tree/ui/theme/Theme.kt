package com.pomodoro.tree.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreen,
    onPrimary = TextPrimary,
    primaryContainer = ForestGreenDark,
    onPrimaryContainer = TextPrimary,
    secondary = OvertimeAmber,
    onSecondary = DeepNavy,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    error = CancelRed,
    onError = TextPrimary,
    outline = TextMuted
)

@Composable
fun PomodoroTreeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = PomodoroTypography,
        content = content
    )
}
