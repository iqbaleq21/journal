package com.iqbal.journal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Token warna: docs/DESIGN.md § Token.

// Terang = opsi D (neo-brutalism: tinta hitam + kuning), gelap = opsi C (hitam pekat + lime).
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD84D),
    onPrimaryContainer = Color(0xFF111111),
    background = Color(0xFFF4F4F0),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE8E8E2),
    onSurfaceVariant = Color(0xFF444444),
    secondaryContainer = Color(0xFFE8E8E2),
    onSecondaryContainer = Color(0xFF111111),
    // Lembar bawah & dialog memakai surfaceContainer*; tanpa ini Material mengisinya dengan ungu bawaan.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFE8E8E2),
    outline = Color(0xFF111111),
    outlineVariant = Color(0xFFD6D6CF),
    inverseSurface = Color(0xFF111111),
    inverseOnSurface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4FF3F),
    onPrimary = Color(0xFF0E0E10),
    primaryContainer = Color(0xFFD4FF3F),
    onPrimaryContainer = Color(0xFF0E0E10),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFF2F2F3),
    surface = Color(0xFF17171A),
    onSurface = Color(0xFFF2F2F3),
    surfaceVariant = Color(0xFF2A2A30),
    onSurfaceVariant = Color(0xFFA0A0A8),
    secondaryContainer = Color(0xFF2A2A30),
    onSecondaryContainer = Color(0xFFF2F2F3),
    surfaceContainerLowest = Color(0xFF0E0E10),
    surfaceContainerLow = Color(0xFF17171A),
    surfaceContainer = Color(0xFF17171A),
    surfaceContainerHigh = Color(0xFF1F1F23),
    surfaceContainerHighest = Color(0xFF2A2A30),
    // Lebih terang dari garis kartu (CardStyle) agar thumb switch & tepi chip tetap terlihat.
    outline = Color(0xFF6E6E76),
    outlineVariant = Color(0xFF2A2A30),
    inverseSurface = Color(0xFFF2F2F3),
    inverseOnSurface = Color(0xFF0E0E10),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF0E0E10)
)
