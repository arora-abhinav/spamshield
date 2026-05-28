package com.example.spamshield.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = SpamRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A1515),
    secondary = Color(0xFF6C63FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E1E40),
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = SpamRed,
    onError = Color.White,
    outline = DarkBorder,
)

@Composable
fun SpamshieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
