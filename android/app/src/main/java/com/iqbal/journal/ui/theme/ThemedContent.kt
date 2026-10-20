package com.iqbal.journal.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iqbal.journal.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** `setContent` dengan tema pilihan di Pengaturan; ikon bilah sistem ikut terang/gelap sesuai tema app. */
fun ComponentActivity.setJournalContent(themeMode: Flow<ThemeMode>, content: @Composable () -> Unit) {
    // Dibaca sebelum frame pertama agar layar tidak berkedip dari tema sistem ke tema pilihan.
    val initial = runBlocking { themeMode.first() }
    setContent {
        val mode by themeMode.collectAsStateWithLifecycle(initial)
        val darkTheme = when (mode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }
        DisposableEffect(darkTheme) {
            val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
            enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            onDispose {}
        }
        JournalTheme(darkTheme = darkTheme, content = content)
    }
}
