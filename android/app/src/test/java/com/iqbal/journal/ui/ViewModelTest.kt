package com.iqbal.journal.ui

import app.cash.turbine.test
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.model.WidgetSelection
import com.iqbal.journal.testing.FakeGroupRepository
import com.iqbal.journal.testing.FakeNoteRepository
import com.iqbal.journal.testing.FakeProfileRepository
import com.iqbal.journal.testing.FakeWidgetRepository
import com.iqbal.journal.testing.MainDispatcherRule
import com.iqbal.journal.ui.home.HomeViewModel
import com.iqbal.journal.ui.onboarding.OnboardingViewModel
import com.iqbal.journal.ui.start.StartAction
import com.iqbal.journal.ui.start.StartViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val profiles = FakeProfileRepository()
    private val viewModel by lazy { OnboardingViewModel(profiles) }

    @Test
    fun `input tidak valid menampilkan error dan tidak memanggil repository`() {
        viewModel.onNameChange(" ")
        viewModel.onEmailChange("bukan-email")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertTrue(state.showNameError)
        assertTrue(state.showEmailError)
        assertTrue(profiles.createCalls.isEmpty())
    }

    @Test
    fun `input valid membuat profil dan menampilkan loading selama proses`() {
        val gate = CompletableDeferred<Unit>()
        profiles.gate = gate
        viewModel.onNameChange("Ayah")
        viewModel.onEmailChange("ayah@rumah.id")

        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.isSubmitting)

        gate.complete(Unit)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(listOf("Ayah" to "ayah@rumah.id"), profiles.createCalls)
        assertEquals("Ayah", profiles.myProfile.value?.displayName)
    }

    @Test
    fun `kegagalan jaringan diteruskan ke UI lalu dibersihkan setelah ditampilkan`() {
        profiles.nextResult = Outcome.Failure(JournalError.Network)
        viewModel.onNameChange("Ayah")
        viewModel.onEmailChange("ayah@rumah.id")

        viewModel.onSubmit()
        assertEquals(JournalError.Network, viewModel.uiState.value.error)

        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `sesi hilang mengisi ulang profil lama dan menampilkan penjelasan`() {
        profiles.myProfile.value = Profile("lama", "Ayah", "ayah@rumah.id")
        profiles.isSessionLost.value = true

        val state = viewModel.uiState.value
        assertTrue(state.isSessionLost)
        assertEquals("Ayah", state.name)
        assertEquals("ayah@rumah.id", state.email)

        viewModel.onSubmit()
        assertEquals(listOf("Ayah" to "ayah@rumah.id"), profiles.createCalls)
    }
}

class RootViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "a@b.id") }
    private val viewModel by lazy { RootViewModel(profiles, FakeGroupRepository()) }

    @Test
    fun `sesi yang dicabut server kembali ke Kenalan, lalu masuk lagi membuka aplikasi`() = runTest {
        viewModel.uiState.test {
            assertEquals(RootUiState.Ready(hasGroups = false), expectMostRecentItem())

            profiles.isSessionLost.value = true
            assertEquals(RootUiState.NeedsProfile, awaitItem())

            profiles.saveProfile("Ayah", "a@b.id")
            assertEquals(RootUiState.Ready(hasGroups = false), awaitItem())
        }
    }
}

class StartViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "a@b.id") }
    private val groups = FakeGroupRepository()

    // Lazy agar dibuat setelah MainDispatcherRule terpasang (init ViewModel meluncurkan coroutine).
    private val viewModel by lazy { StartViewModel(profiles, groups) }

    @Test
    fun `nama pengguna tampil di sapaan`() {
        assertEquals("Ayah", viewModel.uiState.value.displayName)
    }

    @Test
    fun `gabung hanya aktif saat kode lengkap`() {
        viewModel.onInviteCodeChange("k7q")
        assertFalse(viewModel.uiState.value.canJoin)
        viewModel.onJoin()
        assertTrue(groups.joinCalls.isEmpty())

        viewModel.onInviteCodeChange("k7qm4x")
        assertEquals("K7QM4X", viewModel.uiState.value.inviteCode)
        assertTrue(viewModel.uiState.value.canJoin)
    }

    @Test
    fun `gabung berhasil mengirim event groupReady`() = runTest {
        viewModel.groupReady.test {
            viewModel.onInviteCodeChange("K7QM4X")
            viewModel.onJoin()
            awaitItem()
        }
        assertEquals(listOf("K7QM4X"), groups.joinCalls)
        assertNull(viewModel.uiState.value.pendingAction)
    }

    @Test
    fun `kode salah menampilkan error dan input tetap ada`() {
        groups.nextGroupResult = Outcome.Failure(JournalError.InviteNotFound)
        viewModel.onInviteCodeChange("K7QM4X")
        viewModel.onJoin()

        val state = viewModel.uiState.value
        assertEquals(JournalError.InviteNotFound, state.error)
        assertEquals("K7QM4X", state.inviteCode)
        assertNull(state.pendingAction)
    }

    @Test
    fun `nama grup kosong ditolak sebelum memanggil server`() {
        viewModel.onCreateClick()
        viewModel.onGroupNameChange("  ")
        viewModel.onCreateConfirm()

        assertTrue(viewModel.uiState.value.showGroupNameError)
        assertTrue(groups.createCalls.isEmpty())
    }

    @Test
    fun `buat grup berhasil menutup dialog dan mengirim event`() = runTest {
        viewModel.groupReady.test {
            viewModel.onCreateClick()
            viewModel.onGroupNameChange("Rumah")
            viewModel.onCreateConfirm()
            awaitItem()
        }
        assertEquals(listOf("Rumah"), groups.createCalls)
        assertFalse(viewModel.uiState.value.isCreateDialogOpen)
        assertFalse(viewModel.uiState.value.pendingAction == StartAction.Create)
    }
}

class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val groups = FakeGroupRepository()
    private val notes = FakeNoteRepository()
    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "a@b.id") }

    private val widgets = FakeWidgetRepository()

    private fun viewModel() = HomeViewModel(groups, notes, profiles, widgets)

    @Test
    fun `memilih grup mengubah grup dan catatan yang tampil`() = runTest {
        groups.groups.value =
            listOf(FakeGroupRepository.group("g1", "Rumah"), FakeGroupRepository.group("g2", "Kantor"))
        notes.notes.value = listOf(
            FakeNoteRepository.note(id = "n1", groupId = "g1"),
            FakeNoteRepository.note(id = "n2", groupId = "g2")
        )
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onSelectGroup("g2")
            val state = expectMostRecentItem()
            assertEquals("Kantor", state.selectedGroup?.name)
            assertEquals(listOf("n2"), state.notes.map { it.id })
            assertEquals("me", state.myUserId)
        }
    }

    @Test
    fun `catatan yang dipakai widget ditandai`() = runTest {
        widgets.select(WidgetSelection(appWidgetId = 7, noteId = "n1", groupId = "g1"))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(setOf("n1"), expectMostRecentItem().widgetNoteIds)
        }
    }

    @Test
    fun `catatan grup terpilih diambil ulang dari server`() = runTest {
        val viewModel = viewModel()
        groups.selectGroup("g1")
        groups.selectGroup("g2")

        assertEquals(listOf("g1", "g2"), notes.refreshCalls)
        viewModel.uiState.test { expectMostRecentItem() }
    }

    @Test
    fun `gagal refresh catatan menampilkan error jaringan`() = runTest {
        notes.refreshResult = Outcome.Failure(JournalError.Network)
        groups.selectGroup("g1")
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(JournalError.Network, expectMostRecentItem().error)
        }
    }
}
