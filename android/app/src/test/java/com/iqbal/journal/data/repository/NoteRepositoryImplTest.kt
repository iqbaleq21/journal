package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.AuthorDao
import com.iqbal.journal.data.local.NoteDao
import com.iqbal.journal.data.local.NoteEntity
import com.iqbal.journal.data.local.NoteWithAuthor
import com.iqbal.journal.data.local.PendingOpEntity
import com.iqbal.journal.data.local.PendingOpType
import com.iqbal.journal.data.local.WidgetSelectionDao
import com.iqbal.journal.data.remote.AuthorDto
import com.iqbal.journal.data.remote.NoteChange
import com.iqbal.journal.data.remote.NoteDto
import com.iqbal.journal.data.remote.NoteRemoteDataSource
import com.iqbal.journal.data.remote.ProfileRemoteDataSource
import com.iqbal.journal.data.toEntity
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.NoteDraft
import com.iqbal.journal.domain.model.Outcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteRepositoryImplTest {
    private val remote = mockk<NoteRemoteDataSource>()
    private val profileRemote = mockk<ProfileRemoteDataSource>()
    private val noteDao = mockk<NoteDao>(relaxUnitFun = true) { every { observeVersion() } returns emptyFlow() }
    private val authorDao = mockk<AuthorDao>(relaxUnitFun = true)
    private val widgetSelectionDao = mockk<WidgetSelectionDao> { coEvery { pinnedNoteIds(any()) } returns emptyList() }
    private val outbox = mockk<NoteOutbox>(relaxed = true) {
        coEvery { pendingNoteIds() } returns emptySet()
        coEvery { hasPending(any()) } returns false
    }
    private val now = Instant.parse("2026-10-18T05:00:00Z")
    private val repository =
        NoteRepositoryImpl(
            remote,
            noteDao,
            AuthorCache(profileRemote, authorDao),
            widgetSelectionDao,
            outbox,
            Clock.fixed(now, ZoneOffset.UTC)
        )

    private fun dto(id: String = "n1", authorId: String = "a1", deletedAt: String? = null) = NoteDto(
        id = id,
        groupId = "g1",
        authorId = authorId,
        content = "Galon habis",
        createdAt = "2026-10-18T01:00:00+00:00",
        updatedAt = "2026-10-18T01:00:00+00:00",
        deletedAt = deletedAt
    )

    @Test
    fun `refresh mengganti catatan grup dan mengambil nama semua penulis`() = runTest {
        val notes = listOf(dto("n1", "a1"), dto("n2", "a2"), dto("n3", "a1"))
        coEvery { remote.fetchLatest("g1", NoteRepositoryImpl.LOCAL_NOTES_PER_GROUP) } returns notes
        coEvery { profileRemote.fetchAuthors(setOf("a1", "a2")) } returns listOf(AuthorDto("a1", "Ibu"))

        assertEquals(Outcome.Success(Unit), repository.refresh("g1"))

        coVerify { noteDao.replaceForGroup("g1", notes.map { it.toEntity() }) }
        coVerify { authorDao.upsertAll(listOf(AuthorDto("a1", "Ibu").toEntity())) }
    }

    @Test
    fun `catatan widget di luar batas cache dicek ulang dan penghapusannya diterapkan`() = runTest {
        coEvery { remote.fetchLatest(any(), any()) } returns listOf(dto("n1"))
        coEvery { widgetSelectionDao.pinnedNoteIds("g1") } returns listOf("n1", "lama", "dihapus", "dibersihkan")
        coEvery { remote.fetchByIds(setOf("lama", "dihapus", "dibersihkan")) } returns
            listOf(dto("lama"), dto("dihapus", deletedAt = "2026-10-18T04:00:00+00:00"))
        coEvery { authorDao.exists(any()) } returns true
        coEvery { profileRemote.fetchAuthors(any()) } returns emptyList()

        repository.refresh("g1")

        coVerify { noteDao.upsert(dto("lama").toEntity()) }
        coVerify { noteDao.deleteById("dihapus") }
        coVerify { noteDao.deleteById("dibersihkan") }
    }

    @Test
    fun `sinkron satu catatan menyimpan versi server, atau membuangnya bila sudah dihapus`() = runTest {
        coEvery { remote.fetchByIds(listOf("n1")) } returns listOf(dto("n1"))
        coEvery { remote.fetchByIds(listOf("hilang")) } returns emptyList()
        coEvery { authorDao.exists(any()) } returns true
        every { noteDao.observeById("n1") } returns flowOf(NoteWithAuthor(dto("n1").toEntity(), "Ibu"))
        every { noteDao.observeById("hilang") } returns flowOf(null)

        assertEquals("n1", (repository.syncNote("n1") as Outcome.Success).value?.id)
        coVerify { noteDao.upsert(dto("n1").toEntity()) }

        assertEquals(Outcome.Success(null), repository.syncNote("hilang"))
        coVerify { noteDao.deleteById("hilang") }
    }

    @Test
    fun `refresh tanpa koneksi menjadi error jaringan`() = runTest {
        coEvery { remote.fetchLatest(any(), any()) } throws IOException("offline")

        assertEquals(Outcome.Failure(JournalError.Network), repository.refresh("g1"))
    }

    @Test
    fun `hapus langsung membuang dari cache lalu mengantrekan soft delete`() = runTest {
        every { noteDao.observeById("n1") } returns flowOf(NoteWithAuthor(dto("n1").toEntity(), "Ibu"))

        assertEquals(Outcome.Success(Unit), repository.delete("n1"))

        coVerify { noteDao.deleteById("n1") }
        coVerify {
            outbox.enqueue(PendingOpEntity(noteId = "n1", groupId = "g1", type = PendingOpType.Delete, content = null))
        }
        coVerify { outbox.flush() }
    }

    @Test
    fun `refresh dan realtime tidak menimpa catatan yang perubahannya belum terkirim`() = runTest {
        coEvery { outbox.pendingNoteIds() } returns setOf("lokal")
        coEvery { outbox.hasPending("lokal") } returns true
        coEvery { remote.fetchLatest(any(), any()) } returns listOf(dto("n1"), dto("lokal"))
        coEvery { profileRemote.fetchAuthors(any()) } returns emptyList()
        every { remote.changes() } returns
            flowOf(NoteChange.Saved(dto("lokal", deletedAt = "2026-10-18T04:00:00+00:00")))

        repository.refresh("g1")
        repository.listenForChanges(onSubscribed = {})

        coVerify { noteDao.replaceForGroup("g1", listOf(dto("n1").toEntity())) }
        coVerify(exactly = 0) { noteDao.deleteById("lokal") }
    }

    @Test
    fun `perubahan realtime diterapkan ke cache lokal`() = runTest {
        every { remote.changes() } returns flowOf(
            NoteChange.Saved(dto("n1", "a1")),
            NoteChange.Saved(dto("n2", deletedAt = "2026-10-18T04:00:00+00:00")),
            NoteChange.Purged("n3")
        )
        coEvery { authorDao.exists("a1") } returns true

        repository.listenForChanges(onSubscribed = {})

        coVerify { noteDao.upsert(dto("n1", "a1").toEntity()) }
        coVerify { noteDao.deleteById("n2") }
        coVerify { noteDao.deleteById("n3") }
        coVerify(exactly = 0) { profileRemote.fetchAuthors(any()) }
    }

    @Test
    fun `penulis baru diambil namanya, dan kegagalannya tidak memutus realtime`() = runTest {
        every { remote.changes() } returns
            flowOf(NoteChange.Saved(dto("n1", "baru")), NoteChange.Saved(dto("n2", "a1")))
        coEvery { authorDao.exists("baru") } returns false
        coEvery { authorDao.exists("a1") } returns true
        coEvery { profileRemote.fetchAuthors(setOf("baru")) } throws IOException("offline")

        repository.listenForChanges(onSubscribed = {})

        coVerify { noteDao.upsert(dto("n2", "a1").toEntity()) }
    }

    @Test
    fun `refresh dijalankan setelah langganan aktif dan sebelum perubahan berikutnya diterapkan`() = runTest {
        val order = mutableListOf<String>()
        every { remote.changes() } returns flowOf(NoteChange.Subscribed, NoteChange.Purged("n1"))
        coEvery { noteDao.deleteById("n1") } answers { order += "purge" }

        repository.listenForChanges(onSubscribed = { order += "refresh" })

        assertEquals(listOf("refresh", "purge"), order)
    }

    @Test
    fun `realtime yang terputus dilaporkan sebagai kegagalan`() = runTest {
        every { remote.changes() } returns flow { throw IOException("socket") }

        assertEquals(Outcome.Failure(JournalError.Network), repository.listenForChanges(onSubscribed = {}))
    }

    @Test
    fun `catatan baru langsung tersimpan di cache dengan id dari HP lalu diantrekan`() = runTest {
        every { remote.currentUserId() } returns "me"
        val saved = slot<NoteEntity>()
        coEvery { noteDao.upsert(capture(saved)) } returns Unit
        every { noteDao.observeById(any()) } answers
            { flowOf(NoteWithAuthor(saved.captured, "Ayah", isPending = true)) }

        val note = (
            repository.create(
                "g1",
                NoteDraft("  Galon habis ", color = NoteColor.Mint)
            ) as Outcome.Success
            ).value

        assertEquals("Galon habis", note.content)
        assertEquals("me", note.authorId)
        assertEquals(true, note.isPending)
        coVerify {
            outbox.enqueue(
                PendingOpEntity(
                    noteId = note.id,
                    groupId = "g1",
                    type = PendingOpType.Create,
                    content = "Galon habis",
                    color = "mint"
                )
            )
        }
        coVerify { outbox.flush() }
    }
}
