package com.iqbal.journal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.iqbal.journal.domain.repository.AppearanceRepository
import com.iqbal.journal.ui.JournalApp
import com.iqbal.journal.ui.navigation.NoteLink
import com.iqbal.journal.ui.navigation.noteLink
import com.iqbal.journal.ui.theme.setJournalContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var appearance: AppearanceRepository

    /** Catatan yang diminta dibuka lewat intent; dikosongkan setelah dinavigasi. */
    private var pendingNoteLink by mutableStateOf<NoteLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Saat dibuat ulang (mis. rotasi) intent lama tidak boleh membuka catatan lagi.
        if (savedInstanceState == null) pendingNoteLink = intent.noteLink()
        setJournalContent(appearance.themeMode) {
            JournalApp(noteLink = pendingNoteLink, onNoteLinkHandled = { pendingNoteLink = null })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.noteLink()?.let { pendingNoteLink = it }
    }
}
