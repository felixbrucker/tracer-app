package com.felixbrucker.tracer.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryGreen,
    tertiary = PrimaryCyan,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = OnDarkText,
    onSurface = OnDarkText,
    onSurfaceVariant = OnDarkTextMuted,
    error = ErrorRed
)

@Composable
fun TracerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
