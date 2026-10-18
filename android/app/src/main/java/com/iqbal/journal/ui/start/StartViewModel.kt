package com.iqbal.journal.ui.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.validation.GroupRules
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StartAction { Create, Join }

data class StartUiState(
    val displayName: String = "",
    val isCreateDialogOpen: Boolean = false,
    val groupName: String = "",
    val showGroupNameError: Boolean = false,
    val inviteCode: String = "",
    val pendingAction: StartAction? = null,
    val error: JournalError? = null
) {
    val isBusy: Boolean get() = pendingAction != null
    val canJoin: Boolean get() = !isBusy && GroupRules.isCompleteInviteCode(inviteCode)
}

@HiltViewModel
class StartViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartUiState())
    val uiState: StateFlow<StartUiState> = _uiState.asStateFlow()

    private val groupReadyChannel = Channel<Unit>(Channel.BUFFERED)

    /** Sekali kirim setiap kali grup berhasil dibuat atau digabungi. */
    val groupReady: Flow<Unit> = groupReadyChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            profileRepository.myProfile.filterNotNull().collect { profile ->
                _uiState.update { it.copy(displayName = profile.displayName) }
            }
        }
    }

    fun onCreateClick() = _uiState.update { it.copy(isCreateDialogOpen = true) }

    fun onCreateDismiss() = _uiState.update {
        if (it.isBusy) it else it.copy(isCreateDialogOpen = false, groupName = "", showGroupNameError = false)
    }

    fun onGroupNameChange(name: String) =
        _uiState.update { it.copy(groupName = name.take(GroupRules.MAX_NAME_LENGTH), showGroupNameError = false) }

    fun onCreateConfirm() {
        val state = _uiState.value
        if (state.isBusy) return
        if (!GroupRules.isValidName(state.groupName)) {
            _uiState.update { it.copy(showGroupNameError = true) }
            return
        }
        run(StartAction.Create) { groupRepository.createGroup(state.groupName) }
    }

    fun onInviteCodeChange(code: String) =
        _uiState.update { it.copy(inviteCode = GroupRules.normalizeInviteCode(code)) }

    fun onJoin() {
        val state = _uiState.value
        if (!state.canJoin) return
        run(StartAction.Join) { groupRepository.joinGroup(state.inviteCode) }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    private fun run(action: StartAction, block: suspend () -> Outcome<Group>) {
        _uiState.update { it.copy(pendingAction = action) }
        viewModelScope.launch {
            when (val result = block()) {
                is Outcome.Success -> {
                    _uiState.update { StartUiState(displayName = it.displayName) }
                    groupReadyChannel.send(Unit)
                }

                is Outcome.Failure -> _uiState.update { it.copy(pendingAction = null, error = result.error) }
            }
        }
    }
}
