package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.NoteDao
import com.iqbal.journal.data.local.PendingOpDao
import com.iqbal.journal.data.local.PendingOpEntity
import com.iqbal.journal.data.local.PendingOpType
import com.iqbal.journal.data.remote.NoteDto
import com.iqbal.journal.data.remote.NoteFieldsDto
import com.iqbal.journal.data.remote.NoteRemoteDataSource
import com.iqbal.journal.data.toEntity
import io.github.jan.supabase.exceptions.RestException
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Menjadwalkan pengiriman antrean saat koneksi tersedia (diimplementasi dengan WorkManager). */
fun interface OutboxScheduler {
    fun scheduleFlush()
}

/** Antrean perubahan catatan yang dibuat di HP ini (US-12): dikirim berurutan, diulang sampai berhasil. */
@Singleton
class NoteOutbox @Inject constructor(
    private val remote: NoteRemoteDataSource,
    private val pendingOpDao: PendingOpDao,
    private val noteDao: NoteDao,
    private val scheduler: OutboxScheduler,
    private val clock: Clock
) {
    // Dipanggil dari editor, worker, dan sinkron saat app dibuka; satu per satu agar tidak terkirim dua kali.
    private val mutex = Mutex()

    /** Catatan yang punya perubahan lokal belum terkirim; versi server tidak boleh menimpanya. */
    suspend fun pendingNoteIds(): Set<String> = pendingOpDao.pendingNoteIds().toSet()

    suspend fun hasPending(noteId: String): Boolean = pendingOpDao.hasPending(noteId)

    suspend fun enqueue(op: PendingOpEntity) {
        pendingOpDao.insert(op)
        scheduler.scheduleFlush()
    }

    /** @return true bila antrean kosong; false bila masih menunggu koneksi. */
    suspend fun flush(): Boolean = mutex.withLock {
        for (op in pendingOpDao.getAll()) {
            try {
                send(op)
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("SwallowedException") e: RestException
            ) {
                // Ditolak server (mis. sudah bukan anggota, atau terkirim ganda): buang, pakai versi server.
                pendingOpDao.delete(op.id)
                resync(op.noteId)
            } catch (
                @Suppress("SwallowedException") e: NoteRejectedException
            ) {
                pendingOpDao.delete(op.id)
                resync(op.noteId)
            } catch (
                @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception
            ) {
                // Gagal jaringan: urutan harus dijaga, jadi berhenti dan coba lagi nanti.
                scheduler.scheduleFlush()
                return@withLock false
            }
        }
        true
    }

    private suspend fun send(op: PendingOpEntity) {
        val saved: NoteDto? = when (op.type) {
            PendingOpType.Create -> remote.insert(op.noteId, op.groupId, op.fields())

            PendingOpType.Update -> remote.updateContent(op.noteId, op.fields()) ?: throw NoteRejectedException()

            PendingOpType.Delete -> {
                remote.softDelete(op.noteId, clock.instant())
                null
            }
        }
        pendingOpDao.delete(op.id)
        // Versi server (mis. updated_at resmi) dipakai hanya bila tidak ada perubahan lokal yang lebih baru.
        if (saved != null && !pendingOpDao.hasPending(op.noteId)) noteDao.upsert(saved.toEntity())
    }

    private fun PendingOpEntity.fields() = NoteFieldsDto(checkNotNull(content), color, kind, shared)

    private suspend fun resync(noteId: String) {
        if (pendingOpDao.hasPending(noteId)) return
        val note = remote.fetchByIds(listOf(noteId)).firstOrNull()
        if (note == null || note.deletedAt != null) noteDao.deleteById(noteId) else noteDao.upsert(note.toEntity())
    }
}

/** Update tidak mengenai baris mana pun: RLS menolaknya tanpa error (mis. akses berbagi sudah dicabut). */
private class NoteRejectedException : Exception()
