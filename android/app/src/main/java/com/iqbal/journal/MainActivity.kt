package com.iqbal.journal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.iqbal.journal.ui.JournalApp
import com.iqbal.journal.ui.navigation.NoteLink
import com.iqbal.journal.ui.navigation.noteLink
import com.iqbal.journal.ui.theme.JournalTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Catatan yang diminta dibuka lewat intent; dikosongkan setelah dinavigasi. */
    private var pendingNoteLink by mutableStateOf<NoteLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Saat dibuat ulang (mis. rotasi) intent lama tidak boleh membuka catatan lagi.
        if (savedInstanceState == null) pendingNoteLink = intent.noteLink()
        setContent {
            JournalTheme {
                JournalApp(noteLink = pendingNoteLink, onNoteLinkHandled = { pendingNoteLink = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.noteLink()?.let { pendingNoteLink = it }
    }
}
