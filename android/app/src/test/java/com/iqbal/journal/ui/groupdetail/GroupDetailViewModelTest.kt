package com.iqbal.journal.ui.groupdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.validation.GroupRules
import com.iqbal.journal.testing.FakeGroupRepository
import com.iqbal.journal.testing.FakeProfileRepository
import com.iqbal.journal.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroupDetailViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "a@b.id") }
    private val groups = FakeGroupRepository().apply {
        groups.value = listOf(FakeGroupRepository.group("g1", "Rumah", ownerId = "me"))
        membersByGroup.value = mapOf(
            "g1" to listOf(FakeGroupRepository.member("me", "Ayah"), FakeGroupRepository.member("ibu", "Ibu"))
        )
    }

    private fun viewModel() =
        GroupDetailViewModel(SavedStateHandle(mapOf(GroupDetailViewModel.ARG_GROUP_ID to "g1")), groups, profiles)

    @Test
    fun `pemilik melihat anggota dan bisa mengeluarkan anggota setelah konfirmasi`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.isOwner)
            assertEquals(listOf("Ayah", "Ibu"), state.members.map { it.displayName })

            viewModel.onRemoveClick(state.members[1])
            assertTrue(groups.actions.isEmpty())
            viewModel.onDialogConfirm()

            assertEquals(listOf("remove g1 ibu"), groups.actions)
            val after = expectMostRecentItem()
            assertEquals(listOf("Ayah"), after.members.map { it.displayName })
            assertNull(after.dialog)
        }
    }

    @Test
    fun `bukan pemilik tidak dianggap pemilik`() = runTest {
        groups.groups.value = listOf(FakeGroupRepository.group("g1", ownerId = "ibu"))
        viewModel().uiState.test {
            assertFalse(expectMostRecentItem().isOwner)
        }
    }

    @Test
    fun `ganti nama kosong ditolak tanpa memanggil server, nama valid disimpan`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onRenameClick()
            viewModel.onRenameChange("   ")
            viewModel.onDialogConfirm()
            assertTrue((expectMostRecentItem().dialog as GroupDetailDialog.Rename).showError)
            assertTrue(groups.actions.isEmpty())

            viewModel.onRenameChange("a".repeat(GroupRules.MAX_NAME_LENGTH + 5))
            assertEquals(
                GroupRules.MAX_NAME_LENGTH,
                (expectMostRecentItem().dialog as GroupDetailDialog.Rename).name.length
            )

            viewModel.onRenameChange("Keluarga")
            viewModel.onDialogConfirm()
            assertEquals("Keluarga", expectMostRecentItem().group?.name)
        }
    }

    @Test
    fun `keluar grup menutup layar karena grupnya hilang dari daftar`() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onLeaveClick()
            viewModel.onDialogConfirm()

            assertEquals(listOf("leave g1"), groups.actions)
            assertTrue(expectMostRecentItem().isGone)
        }
    }

    @Test
    fun `kegagalan server menutup dialog dan menampilkan error`() = runTest {
        groups.actionResult = Outcome.Failure(JournalError.NotOwner)
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onRegenerateClick()
            viewModel.onDialogConfirm()

            val state = expectMostRecentItem()
            assertEquals(JournalError.NotOwner, state.error)
            assertNull(state.dialog)
            assertFalse(state.isBusy)
        }
    }
}
