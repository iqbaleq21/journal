package com.iqbal.journal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val isInviteSheetOpen: Boolean = false,
    val error: JournalError? = null
) {
    val selectedGroup: Group? get() = groups.firstOrNull { it.id == selectedGroupId }
}

/** State yang hanya hidup di layar ini (bukan dari repository). */
private data class HomeLocalState(val isInviteSheetOpen: Boolean = false, val error: JournalError? = null)

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class HomeViewModel @Inject constructor(private val groupRepository: GroupRepository) : ViewModel() {

    private val localState = MutableStateFlow(HomeLocalState())

    val uiState: StateFlow<HomeUiState> =
        combine(groupRepository.groups, groupRepository.selectedGroupId, localState) { groups, selectedId, local ->
            HomeUiState(
                groups = groups,
                selectedGroupId = selectedId,
                isInviteSheetOpen = local.isInviteSheetOpen,
                error = local.error
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val result = groupRepository.refresh()
            if (result is Outcome.Failure) localState.update { it.copy(error = result.error) }
        }
    }

    fun onSelectGroup(groupId: String) {
        viewModelScope.launch { groupRepository.selectGroup(groupId) }
    }

    fun onInviteClick() = localState.update { it.copy(isInviteSheetOpen = true) }

    fun onInviteDismiss() = localState.update { it.copy(isInviteSheetOpen = false) }

    fun onErrorShown() = localState.update { it.copy(error = null) }
}
