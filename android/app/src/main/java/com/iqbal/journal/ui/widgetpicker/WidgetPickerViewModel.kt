package com.iqbal.journal.ui.widgetpicker

import android.appwidget.AppWidgetManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.Group
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.WidgetSelection
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WidgetPickerUiState(
    /** Pilihan lama widget belum terbaca. */
    val isLoading: Boolean = true,
    val hasProfile: Boolean = true,
    /** Widget sudah terpasang dan sedang diganti catatannya (US-9). */
    val isReconfiguring: Boolean = false,
    val groups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    val notes: List<Note> = emptyList(),
    val selectedNoteId: String? = null,
    val myUserId: String? = null,
    val isSaving: Boolean = false,
    val isDone: Boolean = false
) {
    val selectedGroup: Group? get() = groups.firstOrNull { it.id == selectedGroupId }
    val selectedNote: Note? get() = notes.firstOrNull { it.id == selectedNoteId }
    val canSave: Boolean get() = selectedNote != null && !isSaving
}

/** State yang dipilih di layar ini, belum disimpan. */
private data class PickerLocalState(
    val isLoading: Boolean = true,
    val isReconfiguring: Boolean = false,
    val groupId: String? = null,
    val noteId: String? = null,
    val isSaving: Boolean = false,
    val isDone: Boolean = false
)

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WidgetPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    profileRepository: ProfileRepository,
    noteRepository: NoteRepository,
    private val widgetRepository: WidgetRepository
) : ViewModel() {

    // Extra intent konfigurasi widget otomatis masuk ke SavedStateHandle activity.
    private val appWidgetId: Int = checkNotNull(savedStateHandle[AppWidgetManager.EXTRA_APPWIDGET_ID])

    private val local = MutableStateFlow(PickerLocalState())

    /** Grup yang tampil: pilihan pengguna, atau grup pertama bila belum memilih. */
    private val effectiveGroupId = combine(local, groupRepository.groups) { state, groups ->
        state.groupId?.takeIf { id -> groups.any { it.id == id } } ?: groups.firstOrNull()?.id
    }.distinctUntilChanged()

    private val notes = effectiveGroupId.flatMapLatest { groupId ->
        if (groupId == null) flowOf(emptyList()) else noteRepository.notes(groupId)
    }

    val uiState: StateFlow<WidgetPickerUiState> = combine(
        local,
        groupRepository.groups,
        effectiveGroupId,
        notes,
        profileRepository.myProfile
    ) { state, groups, groupId, notes, profile ->
        WidgetPickerUiState(
            isLoading = state.isLoading,
            hasProfile = profile != null,
            isReconfiguring = state.isReconfiguring,
            groups = groups,
            selectedGroupId = groupId,
            notes = notes,
            selectedNoteId = state.noteId,
            myUserId = profile?.id,
            isSaving = state.isSaving,
            isDone = state.isDone
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WidgetPickerUiState())

    init {
        viewModelScope.launch {
            val existing = widgetRepository.selection(appWidgetId)
            // Widget baru mulai dari grup yang sedang dibuka di Beranda.
            val groupId = existing?.groupId ?: groupRepository.selectedGroupId.first()
            local.update {
                it.copy(
                    isLoading = false,
                    isReconfiguring = existing != null,
                    groupId = groupId,
                    noteId = existing?.noteId
                )
            }
        }
    }

    fun onSelectGroup(groupId: String) = local.update {
        if (groupId == uiState.value.selectedGroupId) it else it.copy(groupId = groupId, noteId = null)
    }

    fun onSelectNote(noteId: String) = local.update { it.copy(noteId = noteId) }

    fun onSave() {
        val note = uiState.value.selectedNote ?: return
        if (local.value.isSaving) return
        local.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            widgetRepository.select(
                WidgetSelection(appWidgetId = appWidgetId, noteId = note.id, groupId = note.groupId)
            )
            local.update { it.copy(isSaving = false, isDone = true) }
        }
    }
}
