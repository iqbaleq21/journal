package com.iqbal.journal.sync

import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.testing.FakeGroupRepository
import com.iqbal.journal.testing.FakeNoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class RecordingNotifier : NoteNotifier {
    val shown = mutableListOf<Pair<String, String>>()

    override fun notifyNewNote(note: Note, groupName: String) {
        shown += note.id to groupName
    }
}

class NoteChangeHandlerTest {
    private val groups = FakeGroupRepository().apply { groups.value = listOf(FakeGroupRepository.group("g1", "Rumah")) }
    private val notes = FakeNoteRepository().apply { serverNotes["n1"] = FakeNoteRepository.note(id = "n1") }
    private val notifier = RecordingNotifier()
    private val handler = NoteChangeHandler(notes, groups, notifier)

    @Test
    fun `catatan baru disimpan ke cache dan dinotifikasi dengan nama grup`() = runTest {
        assertEquals(Outcome.Success(Unit), handler.handle(NoteChangedMessage("n1", isNew = true, notify = true)))

        assertEquals(listOf("n1"), notes.notes.value.map { it.id })
        assertEquals(listOf("n1" to "Rumah"), notifier.shown)
    }

    @Test
    fun `edit dan grup yang dibisukan tidak memunculkan notifikasi`() = runTest {
        handler.handle(NoteChangedMessage("n1", isNew = false, notify = true))
        handler.handle(NoteChangedMessage("n1", isNew = true, notify = false))

        assertTrue(notifier.shown.isEmpty())
        assertEquals(listOf("n1"), notes.notes.value.map { it.id })
    }

    @Test
    fun `catatan yang sudah dihapus dibuang dari cache tanpa notifikasi`() = runTest {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n2"))

        handler.handle(NoteChangedMessage("n2", isNew = true, notify = true))

        assertTrue(notes.notes.value.isEmpty())
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun `kegagalan jaringan diteruskan agar worker mencoba lagi`() = runTest {
        notes.syncFailure = Outcome.Failure(JournalError.Network)

        assertEquals(Outcome.Failure(JournalError.Network), handler.handle(NoteChangedMessage("n1", true, true)))
    }
}

class FullSyncTest {
    private val groups = FakeGroupRepository().apply {
        groups.value = listOf(FakeGroupRepository.group("g1"), FakeGroupRepository.group("g2"))
    }
    private val notes = FakeNoteRepository()

    @Test
    fun `semua grup diambil ulang catatannya`() = runTest {
        assertTrue(FullSync(groups, notes).run())
        assertEquals(listOf("g1", "g2"), notes.refreshCalls)
    }

    @Test
    fun `kegagalan satu langkah dilaporkan agar sinkron periodik diulang`() = runTest {
        notes.refreshResult = Outcome.Failure(JournalError.Network)

        assertFalse(FullSync(groups, notes).run())
    }
}
