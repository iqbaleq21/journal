package com.iqbal.journal.ui.home

import app.cash.turbine.test
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.testing.FakeGroupRepository
import com.iqbal.journal.testing.FakeNoteRepository
import com.iqbal.journal.testing.FakeProfileRepository
import com.iqbal.journal.testing.FakeWidgetRepository
import com.iqbal.journal.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val groups = FakeGroupRepository().apply {
        groups.value = listOf(FakeGroupRepository.group("g1", "Rumah"))
    }
    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "a@b.id") }
    private val notes = FakeNoteRepository().apply {
        notes.value = listOf(
            FakeNoteRepository.note(id = "mine"),
            FakeNoteRepository.note(id = "shared", authorId = "ibu", shared = true),
            FakeNoteRepository.note(id = "private", authorId = "ibu")
        )
    }

    private fun viewModel() = HomeViewModel(groups, notes, profiles, FakeWidgetRepository()).also {
        it.onSelectGroup("g1")
    }

    private fun HomeViewModel.selectAll() = listOf("mine", "shared", "private").forEach(::onToggleSelection)

    @Test
    fun `hapus massal butuh konfirmasi dan hanya menghapus catatan sendiri`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            viewModel.selectAll()
            assertEquals(3, expectMostRecentItem().selectedNoteIds.size)

            viewModel.onBulkDeleteClick()
            assertTrue(expectMostRecentItem().isBulkDeleteConfirmOpen)
            assertTrue(notes.deleteCalls.isEmpty())

            viewModel.onBulkDeleteConfirm()
            assertEquals(listOf("mine"), notes.deleteCalls)
            val after = expectMostRecentItem()
            assertFalse(after.isSelecting)
            assertFalse(after.isBulkDeleteConfirmOpen)
        }
    }

    @Test
    fun `akses edit massal hanya untuk catatan sendiri, warna untuk semua yang boleh diedit`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            viewModel.selectAll()
            expectMostRecentItem()
            viewModel.onBulkSharedChange(true)
            assertEquals(listOf("mine"), notes.updateCalls.map { it.first })
            assertTrue(notes.lastDraft!!.shared)

            notes.updateCalls.clear()
            viewModel.selectAll()
            expectMostRecentItem()
            viewModel.onBulkColorChange(NoteColor.Mint)
            assertEquals(setOf("mine", "shared"), notes.updateCalls.map { it.first }.toSet())
            assertEquals(NoteColor.Mint, notes.lastColor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pindah grup mengosongkan pilihan`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            viewModel.onToggleSelection("mine")
            assertTrue(expectMostRecentItem().isSelecting)
            viewModel.onSelectGroup("g2")
            assertFalse(expectMostRecentItem().isSelecting)
        }
    }
}
