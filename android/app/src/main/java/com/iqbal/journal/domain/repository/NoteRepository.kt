package com.iqbal.journal.domain.repository

import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.Outcome
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    /** Catatan sebuah grup dari cache lokal, yang terakhir diubah di atas. */
    fun notes(groupId: String): Flow<List<Note>>

    /** Satu catatan dari cache lokal; null jika tidak ada atau sudah dihapus. */
    fun note(noteId: String): Flow<Note?>

    /** Berbunyi setiap kali isi cache catatan berubah (untuk memperbarui widget). */
    val changes: Flow<Unit>

    /** Ambil ulang catatan terbaru sebuah grup dari server ke cache lokal. */
    suspend fun refresh(groupId: String): Outcome<Unit>

    /** Ambil satu catatan dari server ke cache lokal; null jika sudah dihapus. */
    suspend fun syncNote(noteId: String): Outcome<Note?>

    /** Buang catatan lama di luar batas cache per grup; catatan yang dipakai widget dipertahankan. */
    suspend fun trimCache()

    /**
     * Kirim perubahan yang tertunda (tulis/edit/hapus saat offline) ke server.
     * @return true bila antrean sudah kosong; false bila masih ada yang menunggu koneksi.
     */
    suspend fun flushPending(): Boolean

    suspend fun create(groupId: String, content: String): Outcome<Note>

    suspend fun update(noteId: String, content: String): Outcome<Note>

    /**
     * Soft delete agar penghapusan ikut tersinkron ke HP anggota lain.
     * Seperti tulis dan edit, tetap berhasil saat offline lalu dikirim saat online.
     */
    suspend fun delete(noteId: String): Outcome<Unit>

    /**
     * Terapkan perubahan catatan dari server secara langsung ke cache lokal.
     * [onSubscribed] dipanggil begitu langganan aktif: refresh di sana tidak meninggalkan celah perubahan
     * yang terlewat, karena perubahan selama refresh diantrekan lalu diterapkan sesudahnya.
     * Berjalan sampai coroutine pemanggil dibatalkan; kembali dengan [Outcome.Failure] jika koneksi gagal.
     */
    suspend fun listenForChanges(onSubscribed: suspend () -> Unit): Outcome<Unit>
}
