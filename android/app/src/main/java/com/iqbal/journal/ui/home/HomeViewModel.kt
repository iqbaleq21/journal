package com.iqbal.journal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.repository.WidgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val notes: List<Note> = emptyList(),
    val myUserId: String? = null,
    val widgetNoteIds: Set<String> = emptySet(),
    val isInviteSheetOpen: Boolean = false,
    val error: JournalError? = null
) {
    val selectedGroup: Group? get() = groups.firstOrNull { it.id == selectedGroupId }
}

/** State yang hanya hidup di layar ini (bukan dari repository). */
private data class HomeLocalState(val isInviteSheetOpen: Boolean = false, val error: JournalError? = null)

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val noteRepository: NoteRepository,
    profileRepository: ProfileRepository,
    widgetRepository: WidgetRepository
) : ViewModel() {

    private val localState = MutableStateFlow(HomeLocalState())

    private val selectedNotes = groupRepository.selectedGroupId.flatMapLatest { groupId ->
        if (groupId == null) flowOf(emptyList()) else noteRepository.notes(groupId)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        groupRepository.groups,
        groupRepository.selectedGroupId,
        selectedNotes,
        combine(profileRepository.myProfile, widgetRepository.pinnedNoteIds, ::Pair),
        localState
    ) { groups, selectedId, notes, (profile, widgetNoteIds), local ->
        HomeUiState(
            groups = groups,
            selectedGroupId = selectedId,
            notes = notes,
            myUserId = profile?.id,
            widgetNoteIds = widgetNoteIds,
            isInviteSheetOpen = local.isInviteSheetOpen,
            error = local.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    init {
        // Grup yang baru dibuka (mis. baru digabungi) langsung diambil catatannya.
        viewModelScope.launch {
            groupRepository.selectedGroupId.filterNotNull().distinctUntilChanged().collectLatest { groupId ->
                val result = noteRepository.refresh(groupId)
                if (result is Outcome.Failure) localState.update { it.copy(error = result.error) }
            }
        }
    }

    fun onSelectGroup(groupId: String) {
        viewModelScope.launch { groupRepository.selectGroup(groupId) }
    }

    fun onInviteClick() = localState.update { it.copy(isInviteSheetOpen = true) }

    fun onInviteDismiss() = localState.update { it.copy(isInviteSheetOpen = false) }

    fun onErrorShown() = localState.update { it.copy(error = null) }
}
