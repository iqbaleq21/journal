package com.iqbal.journal.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.NoteDraft
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val notes: List<Note> = emptyList(),
    val myUserId: String? = null,
    val widgetNoteIds: Set<String> = emptySet(),
    /** Nama anggota grup terpilih, untuk tumpukan avatar di kartu grup. */
    val memberNames: List<String> = emptyList(),
    /** Catatan yang dipilih untuk aksi massal (US-23); kosong = mode biasa. */
    val selectedNoteIds: Set<String> = emptySet(),
    val isBulkSheetOpen: Boolean = false,
    val isBulkDeleteConfirmOpen: Boolean = false,
    val isBulkBusy: Boolean = false,
    val error: JournalError? = null
) {
    val selectedGroup: Group? get() = groups.firstOrNull { it.id == selectedGroupId }
    val isSelecting: Boolean get() = selectedNoteIds.isNotEmpty()
    private val selectedNotes: List<Note> get() = notes.filter { it.id in selectedNoteIds }

    /** Hapus dan atur akses tetap hak penulis (US-18); catatan orang lain dilewati. */
    val ownSelectedNotes: List<Note> get() = selectedNotes.filter { it.authorId == myUserId }
    val editableSelectedNotes: List<Note> get() = selectedNotes.filter { it.authorId == myUserId || it.shared }
}

/** State yang hanya hidup di layar ini (bukan dari repository). */
private data class HomeLocalState(
    val selectedNoteIds: Set<String> = emptySet(),
    val isBulkSheetOpen: Boolean = false,
    val isBulkDeleteConfirmOpen: Boolean = false,
    val isBulkBusy: Boolean = false,
    val error: JournalError? = null
)

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
// Satu fungsi per event UI (lihat CLAUDE.md), termasuk aksi massal.
@Suppress("TooManyFunctions")
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

    private val selectedMemberNames = groupRepository.selectedGroupId.flatMapLatest { groupId ->
        if (groupId == null) flowOf(emptyList()) else groupRepository.members(groupId)
    }.map { members -> members.mapNotNull { it.displayName } }

    val uiState: StateFlow<HomeUiState> = combine(
        groupRepository.groups,
        groupRepository.selectedGroupId,
        combine(selectedNotes, selectedMemberNames, ::Pair),
        combine(profileRepository.myProfile, widgetRepository.pinnedNoteIds, ::Pair),
        localState
    ) { groups, selectedId, (notes, memberNames), (profile, widgetNoteIds), local ->
        HomeUiState(
            groups = groups,
            selectedGroupId = selectedId,
            notes = notes,
            myUserId = profile?.id,
            widgetNoteIds = widgetNoteIds,
            memberNames = memberNames,
            // Catatan yang hilang (mis. dihapus di HP lain) ikut keluar dari pilihan.
            selectedNoteIds = local.selectedNoteIds intersect notes.map { it.id }.toSet(),
            isBulkSheetOpen = local.isBulkSheetOpen,
            isBulkDeleteConfirmOpen = local.isBulkDeleteConfirmOpen,
            isBulkBusy = local.isBulkBusy,
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
        localState.update { it.copy(selectedNoteIds = emptySet()) }
        viewModelScope.launch { groupRepository.selectGroup(groupId) }
    }

    fun onErrorShown() = localState.update { it.copy(error = null) }

    fun onToggleSelection(noteId: String) = localState.update {
        if (it.isBulkBusy) it else it.copy(selectedNoteIds = it.selectedNoteIds.toggle(noteId))
    }

    fun onClearSelection() = localState.update { if (it.isBulkBusy) it else it.copy(selectedNoteIds = emptySet()) }

    fun onBulkSheetOpen() = localState.update { it.copy(isBulkSheetOpen = true) }

    fun onBulkSheetDismiss() = localState.update { it.copy(isBulkSheetOpen = false) }

    fun onBulkColorChange(color: NoteColor) = runBulk(
        uiState.value.editableSelectedNotes.filter { it.color != color }
    ) { noteRepository.update(it.id, it.toDraft().copy(color = color)) }

    fun onBulkSharedChange(shared: Boolean) = runBulk(
        uiState.value.ownSelectedNotes.filter { it.shared != shared }
    ) { noteRepository.update(it.id, it.toDraft().copy(shared = shared)) }

    fun onBulkDeleteClick() = localState.update {
        if (uiState.value.ownSelectedNotes.isEmpty()) {
            it
        } else {
            it.copy(isBulkSheetOpen = false, isBulkDeleteConfirmOpen = true)
        }
    }

    fun onBulkDeleteDismiss() = localState.update { it.copy(isBulkDeleteConfirmOpen = false) }

    fun onBulkDeleteConfirm() = runBulk(uiState.value.ownSelectedNotes) { noteRepository.delete(it.id) }

    /** Dikirim satu per satu lewat antrean yang sama dengan Editor; error pertama yang ditampilkan. */
    private fun runBulk(notes: List<Note>, action: suspend (Note) -> Outcome<*>) {
        if (localState.value.isBulkBusy) return
        localState.update { it.copy(isBulkBusy = true, isBulkSheetOpen = false, isBulkDeleteConfirmOpen = false) }
        viewModelScope.launch {
            val failure = notes.map { action(it) }.filterIsInstance<Outcome.Failure>().firstOrNull()
            localState.update {
                it.copy(isBulkBusy = false, selectedNoteIds = emptySet(), error = failure?.error ?: it.error)
            }
        }
    }

    private fun Set<String>.toggle(id: String) = if (id in this) this - id else this + id

    private fun Note.toDraft() = NoteDraft(content, title, color, kind, shared)
}
