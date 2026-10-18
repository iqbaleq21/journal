package com.iqbal.journal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface RootUiState {
    data object Loading : RootUiState
    data object NeedsProfile : RootUiState
    data class Ready(val hasGroups: Boolean) : RootUiState
}

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Menentukan bagian aplikasi mana yang tampil: Kenalan, atau aplikasi utama. */
@HiltViewModel
class RootViewModel @Inject constructor(profileRepository: ProfileRepository, groupRepository: GroupRepository) :
    ViewModel() {
    val uiState: StateFlow<RootUiState> =
        combine(profileRepository.myProfile, groupRepository.groups) { profile, groups ->
            if (profile == null) RootUiState.NeedsProfile else RootUiState.Ready(hasGroups = groups.isNotEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RootUiState.Loading)
}
