package com.iqbal.journal.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.validation.NoteRules
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditorAction { Save, Delete }

data class EditorUiState(
    val groupName: String = "",
    val content: String = "",
    val color: NoteColor = NoteColor.Plain,
    val isNew: Boolean = true,
    /** Catatan lama belum terbaca dari cache lokal. */
    val isLoading: Boolean = false,
    val isMine: Boolean = true,
    val authorName: String? = null,
    val updatedAt: Instant? = null,
    val showEmptyError: Boolean = false,
    val isDeleteConfirmOpen: Boolean = false,
    val pendingAction: EditorAction? = null,
    val error: JournalError? = null,
    /** Layar selesai (tersimpan, terhapus, atau catatan hilang); UI kembali ke layar sebelumnya. */
    val isDone: Boolean = false
) {
    val canEdit: Boolean get() = isMine && !isLoading
    val canDelete: Boolean get() = !isNew && isMine
    val isBusy: Boolean get() = pendingAction != null
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    groupRepository: GroupRepository,
    private val profileRepository: ProfileRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    // Nama kunci sama dengan properti EditorDestination (navigasi type-safe).
    private val groupId: String = checkNotNull(savedStateHandle[ARG_GROUP_ID])
    private val noteId: String? = savedStateHandle[ARG_NOTE_ID]

    private val _uiState = MutableStateFlow(EditorUiState(isNew = noteId == null, isLoading = noteId != null))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** Versi tersimpan terakhir, untuk melewati simpan bila tidak ada perubahan. */
    private var saved: Note? = null

    init {
        viewModelScope.launch {
            groupRepository.groups.collect { groups ->
                _uiState.update { state ->
                    state.copy(groupName = groups.firstOrNull { it.id == groupId }?.name.orEmpty())
                }
            }
        }
        noteId?.let(::observeNote)
    }

    private fun observeNote(noteId: String) {
        viewModelScope.launch {
            combine(noteRepository.note(noteId), profileRepository.myProfile, ::Pair).collect { (note, me) ->
                if (note == null) {
                    // Dihapus di HP lain saat sedang dibuka.
                    if (saved != null) _uiState.update { it.copy(isDone = true) }
                    return@collect
                }
                val isMine = note.authorId == me?.id
                // Isi & warna milik sendiri tidak ditimpa agar pilihan yang belum disimpan tidak hilang.
                val keepTyped = isMine && saved != null
                saved = note
                _uiState.update {
                    it.copy(
                        content = if (keepTyped) it.content else note.content,
                        color = if (keepTyped) it.color else note.color,
                        isLoading = false,
                        isMine = isMine,
                        authorName = note.authorName,
                        updatedAt = note.updatedAt
                    )
                }
            }
        }
    }

    fun onContentChange(content: String) = _uiState.update {
        if (it.canEdit) it.copy(content = content.take(NoteRules.MAX_NOTE_LENGTH), showEmptyError = false) else it
    }

    fun onColorChange(color: NoteColor) = _uiState.update { if (it.canEdit) it.copy(color = color) else it }

    fun onSave() {
        val state = _uiState.value
        if (state.isBusy || !state.canEdit) return
        val content = NoteRules.normalizeContent(state.content)
        when {
            !NoteRules.isValidContent(content) -> _uiState.update { it.copy(showEmptyError = true) }
            noteId == null -> run(EditorAction.Save) { noteRepository.create(groupId, content, state.color) }
            content == saved?.content && state.color == saved?.color -> _uiState.update { it.copy(isDone = true) }
            else -> run(EditorAction.Save) { noteRepository.update(noteId, content, state.color) }
        }
    }

    fun onDeleteClick() = _uiState.update {
        if (it.canDelete &&
            !it.isBusy
        ) {
            it.copy(isDeleteConfirmOpen = true)
        } else {
            it
        }
    }

    fun onDeleteDismiss() = _uiState.update { it.copy(isDeleteConfirmOpen = false) }

    fun onDeleteConfirm() {
        val id = noteId ?: return
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isDeleteConfirmOpen = false) }
        run(EditorAction.Delete) { noteRepository.delete(id) }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    private fun run(action: EditorAction, block: suspend () -> Outcome<*>) {
        _uiState.update { it.copy(pendingAction = action) }
        viewModelScope.launch {
            when (val result = block()) {
                is Outcome.Success -> _uiState.update { it.copy(pendingAction = null, isDone = true) }
                is Outcome.Failure -> _uiState.update { it.copy(pendingAction = null, error = result.error) }
            }
        }
    }

    companion object {
        const val ARG_GROUP_ID = "groupId"
        const val ARG_NOTE_ID = "noteId"
    }
}
