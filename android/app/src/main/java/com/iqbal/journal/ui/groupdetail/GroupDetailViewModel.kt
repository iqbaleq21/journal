package com.iqbal.journal.ui.groupdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Member
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.validation.GroupRules
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Dialog yang sedang terbuka di layar Detail grup. */
sealed interface GroupDetailDialog {
    data class Rename(val name: String, val showError: Boolean = false) : GroupDetailDialog
    data class ConfirmRemove(val member: Member) : GroupDetailDialog
    data object ConfirmLeave : GroupDetailDialog
    data object ConfirmRegenerate : GroupDetailDialog
}

data class GroupDetailUiState(
    val group: Group? = null,
    val members: List<Member> = emptyList(),
    val myUserId: String? = null,
    val dialog: GroupDetailDialog? = null,
    val isBusy: Boolean = false,
    val error: JournalError? = null,
    /** Grup tidak lagi saya ikuti (keluar, dikeluarkan, atau diarsipkan); layar ditutup. */
    val isGone: Boolean = false
) {
    val isOwner: Boolean get() = group != null && group.ownerId == myUserId
}

/** State yang hanya hidup di layar ini. */
private data class DetailLocalState(
    val dialog: GroupDetailDialog? = null,
    val isBusy: Boolean = false,
    val error: JournalError? = null
)

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
    profileRepository: ProfileRepository
) : ViewModel() {

    // Nama kunci sama dengan properti GroupDetailDestination (navigasi type-safe).
    private val groupId: String = checkNotNull(savedStateHandle[ARG_GROUP_ID])

    private val local = MutableStateFlow(DetailLocalState())

    val uiState: StateFlow<GroupDetailUiState> = combine(
        groupRepository.groups,
        groupRepository.members(groupId),
        profileRepository.myProfile,
        local
    ) { groups, members, profile, state ->
        val group = groups.firstOrNull { it.id == groupId }
        GroupDetailUiState(
            group = group,
            members = members,
            myUserId = profile?.id,
            dialog = state.dialog,
            isBusy = state.isBusy,
            error = state.error,
            isGone = group == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), GroupDetailUiState())

    init {
        // Perubahan keanggotaan tidak dikirim lewat Realtime; ambil versi terbaru setiap layar dibuka.
        // Bila gagal (offline), data cache tetap tampil.
        viewModelScope.launch { groupRepository.refresh() }
    }

    fun onRenameClick() {
        val group = uiState.value.group ?: return
        local.update { it.copy(dialog = GroupDetailDialog.Rename(group.name)) }
    }

    fun onRenameChange(name: String) = local.update {
        val dialog = it.dialog as? GroupDetailDialog.Rename ?: return@update it
        it.copy(dialog = dialog.copy(name = name.take(GroupRules.MAX_NAME_LENGTH), showError = false))
    }

    fun onRemoveClick(member: Member) = local.update { it.copy(dialog = GroupDetailDialog.ConfirmRemove(member)) }

    fun onLeaveClick() = local.update { it.copy(dialog = GroupDetailDialog.ConfirmLeave) }

    fun onRegenerateClick() = local.update { it.copy(dialog = GroupDetailDialog.ConfirmRegenerate) }

    fun onDialogDismiss() = local.update { if (it.isBusy) it else it.copy(dialog = null) }

    fun onDialogConfirm() {
        val state = local.value
        if (state.isBusy) return
        when (val dialog = state.dialog) {
            is GroupDetailDialog.Rename -> {
                if (!GroupRules.isValidName(dialog.name)) {
                    local.update { it.copy(dialog = dialog.copy(showError = true)) }
                    return
                }
                run { groupRepository.renameGroup(groupId, dialog.name) }
            }

            is GroupDetailDialog.ConfirmRemove -> run { groupRepository.removeMember(groupId, dialog.member.userId) }

            GroupDetailDialog.ConfirmLeave -> run { groupRepository.leaveGroup(groupId) }

            GroupDetailDialog.ConfirmRegenerate -> run { groupRepository.regenerateInvite(groupId) }

            null -> Unit
        }
    }

    fun onErrorShown() = local.update { it.copy(error = null) }

    private fun run(action: suspend () -> Outcome<Unit>) {
        local.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val result = action()
            local.update {
                when (result) {
                    is Outcome.Success -> it.copy(isBusy = false, dialog = null)
                    is Outcome.Failure -> it.copy(isBusy = false, dialog = null, error = result.error)
                }
            }
        }
    }

    companion object {
        const val ARG_GROUP_ID = "groupId"
    }
}
