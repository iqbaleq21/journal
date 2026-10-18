package com.iqbal.journal.ui.widgetpicker

import android.appwidget.AppWidgetManager
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.model.WidgetSelection
import com.iqbal.journal.testing.FakeGroupRepository
import com.iqbal.journal.testing.FakeNoteRepository
import com.iqbal.journal.testing.FakeProfileRepository
import com.iqbal.journal.testing.FakeWidgetRepository
import com.iqbal.journal.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WidgetPickerViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "a@b.id") }
    private val groups = FakeGroupRepository().apply {
        groups.value = listOf(FakeGroupRepository.group("g1", "Rumah"), FakeGroupRepository.group("g2", "Kantor"))
    }
    private val notes = FakeNoteRepository().apply {
        notes.value = listOf(
            FakeNoteRepository.note(id = "n1", groupId = "g1"),
            FakeNoteRepository.note(id = "n2", groupId = "g2", authorId = "ibu")
        )
    }
    private val widgets = FakeWidgetRepository()

    private fun viewModel() = WidgetPickerViewModel(
        SavedStateHandle(mapOf(AppWidgetManager.EXTRA_APPWIDGET_ID to WIDGET_ID)),
        groups,
        profiles,
        notes,
        widgets
    )

    @Test
    fun `widget baru mulai dari grup yang dibuka di Beranda dan belum bisa disimpan`() = runTest {
        groups.selectGroup("g2")
        val viewModel = viewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("g2", state.selectedGroupId)
            assertEquals(listOf("n2"), state.notes.map { it.id })
            assertFalse(state.isReconfiguring)
            assertFalse(state.canSave)
        }
    }

    @Test
    fun `memilih catatan lalu menyimpan mengisi pilihan widget`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            viewModel.onSelectNote("n1")
            viewModel.onSave()
            assertTrue(expectMostRecentItem().isDone)
        }

        assertEquals(WidgetSelection(WIDGET_ID, "n1", "g1"), widgets.selection(WIDGET_ID))
    }

    @Test
    fun `atur ulang membuka pilihan lama dan berganti grup mengosongkan catatan`() = runTest {
        widgets.select(WidgetSelection(WIDGET_ID, noteId = "n2", groupId = "g2"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.isReconfiguring)
            assertEquals("n2", state.selectedNote?.id)

            viewModel.onSelectGroup("g1")
            val changed = expectMostRecentItem()
            assertEquals("g1", changed.selectedGroupId)
            assertNull(changed.selectedNote)
        }
    }

    private companion object {
        const val WIDGET_ID = 42
    }
}
