package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.NoteDao
import com.iqbal.journal.data.local.PendingOpDao
import com.iqbal.journal.data.local.PendingOpEntity
import com.iqbal.journal.data.local.PendingOpType
import com.iqbal.journal.data.remote.NoteDto
import com.iqbal.journal.data.remote.NoteRemoteDataSource
import com.iqbal.journal.data.toEntity
import io.github.jan.supabase.exceptions.RestException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteOutboxTest {
    private val remote = mockk<NoteRemoteDataSource>()
    private val noteDao = mockk<NoteDao>(relaxUnitFun = true)
    private val scheduler = mockk<OutboxScheduler>(relaxed = true)
    private val now = Instant.parse("2026-10-18T05:00:00Z")

    /** Antrean tiruan di memori, meniru tabel pending_ops. */
    private val queue = mutableListOf<PendingOpEntity>()
    private val pendingOpDao = mockk<PendingOpDao> {
        coEvery { getAll() } answers { queue.toList() }
        coEvery { delete(any()) } answers { queue.removeAll { it.id == firstArg<Long>() } }
        coEvery { hasPending(any()) } answers { queue.any { it.noteId == firstArg<String>() } }
        coEvery { insert(any()) } answers { queue += firstArg<PendingOpEntity>() }
    }
    private val outbox = NoteOutbox(remote, pendingOpDao, noteDao, scheduler, Clock.fixed(now, ZoneOffset.UTC))

    private fun dto(id: String, content: String = "isi") = NoteDto(
        id = id,
        groupId = "g1",
        authorId = "me",
        content = content,
        createdAt = "2026-10-18T05:00:00+00:00",
        updatedAt = "2026-10-18T05:00:01+00:00"
    )

    private fun op(id: Long, noteId: String, type: PendingOpType, content: String? = "isi", color: String? = null) =
        PendingOpEntity(id = id, noteId = noteId, groupId = "g1", type = type, content = content, color = color)

    @Test
    fun `antrean dikirim berurutan dan versi server disimpan setelah operasi terakhir`() = runTest {
        queue += listOf(op(1, "n1", PendingOpType.Create, "awal", "mint"), op(2, "n1", PendingOpType.Update, "akhir"))
        coEvery { remote.insert("n1", "g1", "awal", "mint") } returns dto("n1", "awal")
        coEvery { remote.updateContent("n1", "akhir", null) } returns dto("n1", "akhir")

        assertTrue(outbox.flush())

        assertTrue(queue.isEmpty())
        // Versi "awal" dari server tidak ditulis karena masih ada edit lokal yang lebih baru di antrean.
        coVerify(exactly = 0) { noteDao.upsert(dto("n1", "awal").toEntity()) }
        coVerify { noteDao.upsert(dto("n1", "akhir").toEntity()) }
    }

    @Test
    fun `gagal jaringan menghentikan pengiriman dan menjadwalkan ulang tanpa membuang antrean`() = runTest {
        queue += listOf(op(1, "n1", PendingOpType.Create), op(2, "n2", PendingOpType.Delete, null))
        coEvery { remote.insert(any(), any(), any(), any()) } throws IOException("offline")

        assertFalse(outbox.flush())

        assertTrue(queue.size == 2)
        verify { scheduler.scheduleFlush() }
        coVerify(exactly = 0) { remote.softDelete(any(), any()) }
    }

    @Test
    fun `ditolak server dibuang lalu cache memakai versi server`() = runTest {
        queue += op(1, "n1", PendingOpType.Update, "ditolak")
        coEvery { remote.updateContent("n1", "ditolak", null) } throws mockk<RestException>(relaxed = true)
        coEvery { remote.fetchByIds(listOf("n1")) } returns listOf(dto("n1", "versi server"))

        assertTrue(outbox.flush())

        assertTrue(queue.isEmpty())
        coVerify { noteDao.upsert(dto("n1", "versi server").toEntity()) }
    }

    @Test
    fun `hapus terkirim sebagai soft delete dengan waktu sekarang`() = runTest {
        queue += op(1, "n1", PendingOpType.Delete, null)
        coEvery { remote.softDelete("n1", now) } returns Unit

        assertTrue(outbox.flush())
        assertTrue(queue.isEmpty())
    }
}
