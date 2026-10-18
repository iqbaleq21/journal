package com.iqbal.journal.domain.validation

/** Aturan isi catatan; harus sama dengan constraint tabel `notes` di database. */
object NoteRules {
    const val MAX_NOTE_LENGTH = 500

    fun normalizeContent(input: String): String = input.trim()

    fun isValidContent(content: String): Boolean = normalizeContent(content).length in 1..MAX_NOTE_LENGTH
}
