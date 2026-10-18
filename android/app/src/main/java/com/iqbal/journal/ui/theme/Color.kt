package com.iqbal.journal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Token warna: docs/DESIGN.md § Token.

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F3BD1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF2F2580),
    background = Color(0xFFF6F5FA),
    onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFECEAF3),
    onSurfaceVariant = Color(0xFF5E5B6B),
    outline = Color(0xFFDCD9E6),
    inverseSurface = Color(0xFF1C1B22),
    inverseOnSurface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFC5BCFF),
    onPrimary = Color(0xFF251A7A),
    primaryContainer = Color(0xFF3A2BA8),
    onPrimaryContainer = Color(0xFFE6E0FF),
    background = Color(0xFF131218),
    onBackground = Color(0xFFEDEBF5),
    surface = Color(0xFF1C1B22),
    onSurface = Color(0xFFEDEBF5),
    surfaceVariant = Color(0xFF2E2C38),
    onSurfaceVariant = Color(0xFFC9C5D8),
    outline = Color(0xFF4A4757),
    inverseSurface = Color(0xFFEDEBF5),
    inverseOnSurface = Color(0xFF1C1B22),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)
