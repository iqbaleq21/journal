package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.Outcome
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    /** Catatan sebuah grup dari cache lokal, yang terakhir diubah di atas. */
    fun notes(groupId: String): Flow<List<Note>>

    /** Satu catatan dari cache lokal; null jika tidak ada atau sudah dihapus. */
    fun note(noteId: String): Flow<Note?>

    /** Ambil ulang catatan terbaru sebuah grup dari server ke cache lokal. */
    suspend fun refresh(groupId: String): Outcome<Unit>

    suspend fun create(groupId: String, content: String): Outcome<Note>

    suspend fun update(noteId: String, content: String): Outcome<Note>

    /** Soft delete agar penghapusan ikut tersinkron ke HP anggota lain. */
    suspend fun delete(noteId: String): Outcome<Unit>

    /**
     * Terapkan perubahan catatan dari server secara langsung ke cache lokal.
     * Berjalan sampai coroutine pemanggil dibatalkan; kembali dengan [Outcome.Failure] jika koneksi gagal.
     */
    suspend fun listenForChanges(): Outcome<Unit>
}
