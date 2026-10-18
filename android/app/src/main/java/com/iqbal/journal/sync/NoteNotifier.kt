package com.iqbal.journal.sync

import com.iqbal.journal.domain.model.Note

/** Notifikasi sistem untuk catatan baru (US-11). */
interface NoteNotifier {
    fun notifyNewNote(note: Note, groupName: String)
}
