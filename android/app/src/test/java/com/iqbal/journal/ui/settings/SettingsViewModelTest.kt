package com.iqbal.journal.ui.settings

import app.cash.turbine.test
import com.iqbal.journal.domain.model.AppLanguage
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.Profile
import com.iqbal.journal.domain.model.ThemeMode
import com.iqbal.journal.testing.FakeAppearanceRepository
import com.iqbal.journal.testing.FakeGroupRepository
import com.iqbal.journal.testing.FakeProfileRepository
import com.iqbal.journal.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val profiles = FakeProfileRepository().apply { myProfile.value = Profile("me", "Ayah", "ayah@rumah.id") }
    private val groups = FakeGroupRepository().apply { groups.value = listOf(FakeGroupRepository.group("g1", "Rumah")) }

    @Test
    fun `profil terisi dari data tersimpan dan tombol simpan aktif hanya saat ada perubahan`() = runTest {
        val viewModel = SettingsViewModel(profiles, groups, FakeAppearanceRepository())
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals("Ayah", state.name)
            assertFalse(state.hasProfileChanges)

            viewModel.onNameChange("Bapak")
            assertTrue(expectMostRecentItem().hasProfileChanges)
        }
    }

    @Test
    fun `email tidak valid ditolak, perubahan valid disimpan`() = runTest {
        val viewModel = SettingsViewModel(profiles, groups, FakeAppearanceRepository())
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onEmailChange("bukan-email")
            viewModel.onSaveProfile()
            assertTrue(expectMostRecentItem().showEmailError)
            assertTrue(profiles.createCalls.isEmpty())

            viewModel.onEmailChange("bapak@rumah.id")
            viewModel.onSaveProfile()
            assertEquals(listOf("Ayah" to "bapak@rumah.id"), profiles.createCalls)
            assertFalse(expectMostRecentItem().hasProfileChanges)
        }
    }

    @Test
    fun `notifikasi grup bisa dimatikan, kegagalan ditampilkan`() = runTest {
        val viewModel = SettingsViewModel(profiles, groups, FakeAppearanceRepository())
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onNotifyChange("g1", false)
            assertFalse(expectMostRecentItem().groups.single().notify)

            groups.actionResult = Outcome.Failure(JournalError.Network)
            viewModel.onNotifyChange("g1", true)
            assertEquals(JournalError.Network, expectMostRecentItem().error)
        }
    }

    @Test
    fun `tema yang dipilih tersimpan dan tampil di layar`() = runTest {
        val appearance = FakeAppearanceRepository()
        val viewModel = SettingsViewModel(profiles, groups, appearance)
        viewModel.uiState.test {
            assertEquals(ThemeMode.System, expectMostRecentItem().themeMode)

            viewModel.onThemeModeChange(ThemeMode.Dark)

            assertEquals(ThemeMode.Dark, expectMostRecentItem().themeMode)
        }
    }

    @Test
    fun `bahasa bawaan Indonesia dan pilihan bahasa tersimpan`() = runTest {
        val appearance = FakeAppearanceRepository()
        val viewModel = SettingsViewModel(profiles, groups, appearance)
        viewModel.uiState.test {
            assertEquals(AppLanguage.Indonesian, expectMostRecentItem().language)

            viewModel.onLanguageChange(AppLanguage.English)

            assertEquals(AppLanguage.English, expectMostRecentItem().language)
        }
    }
}
