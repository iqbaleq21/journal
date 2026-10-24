package com.iqbal.journal.domain.validation

/** Aturan isi catatan; harus sama dengan constraint tabel `notes` di database. */
object NoteRules {
    const val MAX_NOTE_LENGTH = 500
    const val MAX_TITLE_LENGTH = 60

    fun normalizeContent(input: String): String = input.trim()

    fun normalizeTitle(input: String): String = input.trim().take(MAX_TITLE_LENGTH)

    fun isValidContent(content: String): Boolean = normalizeContent(content).length in 1..MAX_NOTE_LENGTH
}
