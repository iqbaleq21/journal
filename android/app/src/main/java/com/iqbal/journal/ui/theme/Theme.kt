package com.iqbal.journal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun JournalTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNoteColors provides if (darkTheme) DarkNoteColors else LightNoteColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = JournalTypography,
            shapes = JournalShapes,
            content = content
        )
    }
}

/** Akses token khusus jOURnal di luar MaterialTheme. */
object JournalTheme {
    val noteColors: NoteColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNoteColors.current
}
