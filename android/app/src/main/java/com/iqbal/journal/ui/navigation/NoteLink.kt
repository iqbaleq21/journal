package com.iqbal.journal.ui.navigation

import android.content.Context
import android.content.Intent
import com.iqbal.journal.MainActivity

/** Permintaan membuka satu catatan dari luar aplikasi (mis. tap widget). */
data class NoteLink(val groupId: String, val noteId: String)

private const val EXTRA_GROUP_ID = "com.iqbal.journal.extra.GROUP_ID"
private const val EXTRA_NOTE_ID = "com.iqbal.journal.extra.NOTE_ID"

fun Context.openNoteIntent(link: NoteLink): Intent = Intent(this, MainActivity::class.java)
    .putExtra(EXTRA_GROUP_ID, link.groupId)
    .putExtra(EXTRA_NOTE_ID, link.noteId)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

fun Intent.noteLink(): NoteLink? {
    val groupId = getStringExtra(EXTRA_GROUP_ID)
    val noteId = getStringExtra(EXTRA_NOTE_ID)
    return if (groupId != null && noteId != null) NoteLink(groupId, noteId) else null
}
