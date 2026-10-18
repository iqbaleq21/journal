package com.iqbal.journal.widget

import app.cash.turbine.test
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.model.WidgetSelection
import com.iqbal.journal.testing.FakeGroupRepository
import com.iqbal.journal.testing.FakeNoteRepository
import com.iqbal.journal.testing.FakeProfileRepository
import com.iqbal.journal.testing.FakeWidgetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteWidgetStateLoaderTest {
    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "a@b.id") }
    private val groups = FakeGroupRepository().apply { groups.value = listOf(FakeGroupRepository.group("g1", "Rumah")) }
    private val notes = FakeNoteRepository()
    private val widgets = FakeWidgetRepository()
    private val loader = NoteWidgetStateLoader(profiles, groups, notes, widgets)

    private suspend fun pin(noteId: String = "n1", groupId: String = "g1") =
        widgets.select(WidgetSelection(appWidgetId = WIDGET_ID, noteId = noteId, groupId = groupId))

    @Test
    fun `catatan terpilih ditampilkan beserta nama grupnya`() = runTest {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1", authorId = "ibu"))
        pin()

        val state = loader.observe(WIDGET_ID).first() as NoteWidgetState.Showing
        assertEquals("n1", state.note.id)
        assertEquals("Rumah", state.groupName)
        assertEquals(false, state.isMine)
    }

    @Test
    fun `state berubah mengikuti pilihan dan catatan tanpa dimuat ulang`() = runTest {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1"))
        loader.observe(WIDGET_ID).test {
            assertEquals(NoteWidgetState.NotConfigured, awaitItem())
            pin()
            assertEquals("n1", (awaitItem() as NoteWidgetState.Showing).note.id)
            notes.notes.value = emptyList()
            assertEquals(NoteWidgetState.NoteDeleted, awaitItem())
        }
    }

    @Test
    fun `mode terbaru mengikuti catatan yang terakhir diubah di grup`() = runTest {
        widgets.select(WidgetSelection(appWidgetId = WIDGET_ID, noteId = null, groupId = "g1"))
        loader.observe(WIDGET_ID).test {
            assertEquals(NoteWidgetState.NoNotesYet, awaitItem())
            notes.notes.value = listOf(FakeNoteRepository.note(id = "baru"), FakeNoteRepository.note(id = "lama"))
            assertEquals("baru", (awaitItem() as NoteWidgetState.Showing).note.id)
            notes.notes.value = listOf(FakeNoteRepository.note(id = "lama"))
            assertEquals("lama", (awaitItem() as NoteWidgetState.Showing).note.id)
        }
    }

    @Test
    fun `profil belum diisi didahulukan dari state lain`() = runTest {
        profiles.myProfile.value = null
        pin()

        assertEquals(NoteWidgetState.NeedsProfile, loader.observe(WIDGET_ID).first())
    }

    @Test
    fun `widget tanpa pilihan meminta memilih catatan`() = runTest {
        assertEquals(NoteWidgetState.NotConfigured, loader.observe(WIDGET_ID).first())
    }

    @Test
    fun `grup yang sudah ditinggalkan dibedakan dari catatan yang dihapus`() = runTest {
        pin(groupId = "lama")
        assertEquals(NoteWidgetState.LeftGroup, loader.observe(WIDGET_ID).first())

        pin(noteId = "hilang")
        assertEquals(NoteWidgetState.NoteDeleted, loader.observe(WIDGET_ID).first())
    }

    private companion object {
        const val WIDGET_ID = 42
    }
}
