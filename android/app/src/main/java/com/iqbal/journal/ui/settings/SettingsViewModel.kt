package com.iqbal.journal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.AppLanguage
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.model.ThemeMode
import com.iqbal.journal.domain.repository.AppearanceRepository
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.validation.ProfileRules
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val name: String = "",
    val email: String = "",
    /** Profil yang tersimpan, untuk tahu apakah ada perubahan yang belum disimpan. */
    val savedName: String = "",
    val savedEmail: String = "",
    val showValidation: Boolean = false,
    val isSavingProfile: Boolean = false,
    val groups: List<Group> = emptyList(),
    /** Grup yang pengaturan notifikasinya sedang dikirim. */
    val pendingNotifyGroupIds: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.Indonesian,
    val error: JournalError? = null
) {
    val showNameError: Boolean get() = showValidation && !ProfileRules.isValidName(name)
    val showEmailError: Boolean get() = showValidation && !ProfileRules.isValidEmail(email)
    val hasProfileChanges: Boolean
        get() = ProfileRules.normalizeName(name) != savedName || ProfileRules.normalizeEmail(email) != savedEmail
}

/** State yang hanya hidup di layar ini. */
private data class SettingsLocalState(
    val name: String = "",
    val email: String = "",
    val showValidation: Boolean = false,
    val isSavingProfile: Boolean = false,
    val pendingNotifyGroupIds: Set<String> = emptySet(),
    val error: JournalError? = null
)

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val groupRepository: GroupRepository,
    private val appearanceRepository: AppearanceRepository
) : ViewModel() {

    private val local = MutableStateFlow(SettingsLocalState())

    val uiState: StateFlow<SettingsUiState> = combine(
        local,
        profileRepository.myProfile.filterNotNull(),
        groupRepository.groups,
        appearanceRepository.themeMode,
        appearanceRepository.language
    ) { state, profile, groups, themeMode, language ->
        SettingsUiState(
            name = state.name,
            email = state.email,
            savedName = profile.displayName,
            savedEmail = profile.email,
            showValidation = state.showValidation,
            isSavingProfile = state.isSavingProfile,
            groups = groups,
            pendingNotifyGroupIds = state.pendingNotifyGroupIds,
            themeMode = themeMode,
            language = language,
            error = state.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    init {
        viewModelScope.launch {
            val profile = profileRepository.myProfile.filterNotNull().first()
            local.update { it.copy(name = profile.displayName, email = profile.email) }
        }
    }

    fun onNameChange(name: String) = local.update { it.copy(name = name.take(ProfileRules.MAX_NAME_LENGTH)) }

    fun onEmailChange(email: String) = local.update { it.copy(email = email) }

    fun onSaveProfile() {
        val state = local.value
        if (state.isSavingProfile) return
        if (!ProfileRules.isValidName(state.name) || !ProfileRules.isValidEmail(state.email)) {
            local.update { it.copy(showValidation = true) }
            return
        }
        local.update { it.copy(isSavingProfile = true, showValidation = true) }
        viewModelScope.launch {
            val result = profileRepository.saveProfile(state.name, state.email)
            local.update {
                when (result) {
                    is Outcome.Success -> it.copy(
                        isSavingProfile = false,
                        showValidation = false,
                        name = result.value.displayName,
                        email = result.value.email
                    )

                    is Outcome.Failure -> it.copy(isSavingProfile = false, error = result.error)
                }
            }
        }
    }

    fun onNotifyChange(groupId: String, enabled: Boolean) {
        if (groupId in local.value.pendingNotifyGroupIds) return
        local.update { it.copy(pendingNotifyGroupIds = it.pendingNotifyGroupIds + groupId) }
        viewModelScope.launch {
            val result = groupRepository.setNotify(groupId, enabled)
            local.update {
                it.copy(
                    pendingNotifyGroupIds = it.pendingNotifyGroupIds - groupId,
                    error = (result as? Outcome.Failure)?.error ?: it.error
                )
            }
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { appearanceRepository.setThemeMode(mode) }
    }

    fun onLanguageChange(language: AppLanguage) {
        viewModelScope.launch { appearanceRepository.setLanguage(language) }
    }

    fun onErrorShown() = local.update { it.copy(error = null) }
}
