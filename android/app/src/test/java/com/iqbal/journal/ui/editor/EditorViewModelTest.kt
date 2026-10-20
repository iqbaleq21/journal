package com.iqbal.journal.ui.editor

import androidx.lifecycle.SavedStateHandle
import com.iqbal.journal.domain.model.ChecklistItem
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.NoteKind
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.validation.NoteRules
import com.iqbal.journal.testing.FakeGroupRepository
import com.iqbal.journal.testing.FakeNoteRepository
import com.iqbal.journal.testing.FakeProfileRepository
import com.iqbal.journal.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditorViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val groups = FakeGroupRepository().apply { groups.value = listOf(FakeGroupRepository.group("g1", "Rumah")) }
    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "a@b.id") }
    private val notes = FakeNoteRepository()

    private fun viewModel(noteId: String? = null) = EditorViewModel(
        SavedStateHandle(mapOf(EditorViewModel.ARG_GROUP_ID to "g1", EditorViewModel.ARG_NOTE_ID to noteId)),
        groups,
        profiles,
        notes
    )

    @Test
    fun `catatan baru disimpan ke grup lalu layar selesai`() {
        val viewModel = viewModel()
        assertEquals("Rumah", viewModel.uiState.value.groupName)

        viewModel.onContentChange("  Galon habis  ")
        viewModel.onSave()

        assertEquals(listOf("g1" to "Galon habis"), notes.createCalls)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `warna yang dipilih ikut disimpan, dan ganti warna saja tetap dianggap perubahan`() {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1"))
        val viewModel = viewModel(noteId = "n1")

        viewModel.onColorChange(NoteColor.Peach)
        viewModel.onSave()

        assertEquals(listOf("n1" to "Galon habis"), notes.updateCalls)
        assertEquals(NoteColor.Peach, notes.lastColor)
    }

    @Test
    fun `catatan orang lain tidak bisa diganti warnanya`() {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1", authorId = "ibu"))
        val viewModel = viewModel(noteId = "n1")

        viewModel.onColorChange(NoteColor.Peach)

        assertEquals(NoteColor.Plain, viewModel.uiState.value.color)
    }

    @Test
    fun `catatan kosong tidak dikirim ke server`() {
        val viewModel = viewModel()
        viewModel.onContentChange("   ")
        viewModel.onSave()

        assertTrue(viewModel.uiState.value.showEmptyError)
        assertTrue(notes.createCalls.isEmpty())
    }

    @Test
    fun `isi dipotong di batas panjang catatan`() {
        val viewModel = viewModel()
        viewModel.onContentChange("a".repeat(NoteRules.MAX_NOTE_LENGTH + 10))

        assertEquals(NoteRules.MAX_NOTE_LENGTH, viewModel.uiState.value.content.length)
    }

    @Test
    fun `catatan sendiri bisa diedit dan simpan tanpa perubahan tidak memanggil server`() {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1", content = "Galon habis"))
        val viewModel = viewModel(noteId = "n1")
        assertTrue(viewModel.uiState.value.canEdit)
        assertEquals("Galon habis", viewModel.uiState.value.content)

        viewModel.onSave()
        assertTrue(notes.updateCalls.isEmpty())
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `perubahan catatan sendiri dikirim sebagai update`() {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1"))
        val viewModel = viewModel(noteId = "n1")

        viewModel.onContentChange("Galon sudah dipesan")
        viewModel.onSave()

        assertEquals(listOf("n1" to "Galon sudah dipesan"), notes.updateCalls)
    }

    @Test
    fun `catatan orang lain hanya bisa dibaca`() {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1", authorId = "ibu"))
        val viewModel = viewModel(noteId = "n1")

        viewModel.onContentChange("diubah")
        viewModel.onSave()
        viewModel.onDeleteClick()

        val state = viewModel.uiState.value
        assertFalse(state.canEdit)
        assertFalse(state.canDelete)
        assertFalse(state.isDeleteConfirmOpen)
        assertEquals("Galon habis", state.content)
        assertTrue(notes.updateCalls.isEmpty())
    }

    @Test
    fun `hapus butuh konfirmasi lalu menutup layar`() {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1"))
        val viewModel = viewModel(noteId = "n1")

        viewModel.onDeleteClick()
        assertTrue(viewModel.uiState.value.isDeleteConfirmOpen)
        assertTrue(notes.deleteCalls.isEmpty())

        viewModel.onDeleteConfirm()
        assertEquals(listOf("n1"), notes.deleteCalls)
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `gagal simpan menampilkan error dan ketikan tetap ada`() {
        notes.writeFailure = Outcome.Failure(JournalError.Network)
        val viewModel = viewModel()
        viewModel.onContentChange("Galon habis")
        viewModel.onSave()

        val state = viewModel.uiState.value
        assertEquals(JournalError.Network, state.error)
        assertEquals("Galon habis", state.content)
        assertFalse(state.isDone)
        assertNull(state.pendingAction)
    }

    @Test
    fun `catatan yang dihapus di HP lain menutup layar`() {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1", authorId = "ibu"))
        val viewModel = viewModel(noteId = "n1")
        assertFalse(viewModel.uiState.value.isDone)

        notes.notes.value = emptyList()
        assertTrue(viewModel.uiState.value.isDone)
    }

    @Test
    fun `catatan orang lain yang dibagikan bisa diedit tapi tidak bisa dihapus atau diatur aksesnya`() {
        notes.notes.value = listOf(FakeNoteRepository.note(id = "n1", authorId = "ibu", shared = true))
        val viewModel = viewModel(noteId = "n1")

        viewModel.onSharedChange(false)
        viewModel.onContentChange("Galon sudah dipesan")
        viewModel.onSave()

        assertFalse(viewModel.uiState.value.canDelete)
        assertEquals(listOf("n1" to "Galon sudah dipesan"), notes.updateCalls)
        assertEquals(true, notes.lastDraft?.shared)
    }

    @Test
    fun `ganti ke checklist mengubah tiap baris jadi item dan disimpan dengan penanda`() {
        val viewModel = viewModel()
        viewModel.onContentChange("Susu\nRoti")

        viewModel.onKindChange(NoteKind.Checklist)
        viewModel.onItemCheckedChange(0, true)
        viewModel.onSave()

        assertEquals(
            listOf(ChecklistItem("Susu", checked = true), ChecklistItem("Roti")),
            viewModel.uiState.value.items
        )
        assertEquals(NoteKind.Checklist, notes.lastDraft?.kind)
        assertEquals("[x] Susu\n[ ] Roti", notes.lastDraft?.content)
    }

    @Test
    fun `item checklist tidak bisa membuat isi melebihi batas panjang`() {
        val viewModel = viewModel()
        viewModel.onKindChange(NoteKind.Checklist)

        viewModel.onItemTextChange(0, "a".repeat(NoteRules.MAX_NOTE_LENGTH))

        assertEquals("", viewModel.uiState.value.items.single().text)
    }
}
