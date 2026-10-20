package com.iqbal.journal.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iqbal.journal.domain.model.Checklist
import com.iqbal.journal.domain.model.ChecklistItem
import com.iqbal.journal.domain.model.JournalError
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteColor
import com.iqbal.journal.domain.model.NoteDraft
import com.iqbal.journal.domain.model.NoteKind
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
    /** Isi mode teks. */
    val content: String = "",
    /** Isi mode checklist. */
    val items: List<ChecklistItem> = emptyList(),
    val kind: NoteKind = NoteKind.Text,
    val color: NoteColor = NoteColor.Plain,
    val shared: Boolean = false,
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
    val canEdit: Boolean get() = (isMine || shared) && !isLoading
    val canDelete: Boolean get() = !isNew && isMine

    /** Isi yang disimpan ke kolom `content`; checklist ikut dihitung terhadap batas panjang. */
    val body: String
        get() = if (kind == NoteKind.Checklist) Checklist.format(items) else NoteRules.normalizeContent(content)

    val draft: NoteDraft get() = NoteDraft(content = body, color = color, kind = kind, shared = shared)
    val isBusy: Boolean get() = pendingAction != null
}

// Satu fungsi per event UI (lihat CLAUDE.md), jadi jumlahnya mengikuti jumlah kontrol di Editor.
@Suppress("TooManyFunctions")
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
                // Isian yang belum disimpan tidak ditimpa, kecuali akses berbagi dicabut selagi dibuka.
                val keepTyped = (isMine || note.shared) && saved != null
                saved = note
                _uiState.update {
                    val typed = if (keepTyped) {
                        it
                    } else {
                        it.copy(
                            content = note.content,
                            items = Checklist.parse(note.content),
                            kind = note.kind,
                            color = note.color,
                            shared = note.shared
                        )
                    }
                    typed.copy(
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

    /** Isi dibawa ke mode baru: tiap baris jadi satu item, atau item jadi baris. */
    fun onKindChange(kind: NoteKind) = _uiState.update {
        when {
            !it.canEdit || kind == it.kind -> it

            kind == NoteKind.Checklist -> it.copy(
                kind = kind,
                items = Checklist.parse(it.content).ifEmpty { listOf(ChecklistItem("")) }
            )

            else -> it.copy(kind = kind, content = Checklist.toPlainText(it.items))
        }
    }

    fun onItemTextChange(index: Int, text: String) = updateItems { items ->
        items.mapIndexed { i, item ->
            if (i ==
                index
            ) {
                item.copy(text = text.oneLine())
            } else {
                item
            }
        }
    }

    fun onItemCheckedChange(index: Int, checked: Boolean) =
        updateItems { items -> items.mapIndexed { i, item -> if (i == index) item.copy(checked = checked) else item } }

    fun onAddItem() = updateItems { it + ChecklistItem("") }

    fun onRemoveItem(index: Int) = updateItems { items -> items.filterIndexed { i, _ -> i != index } }

    fun onSharedChange(shared: Boolean) = _uiState.update { if (it.isMine) it.copy(shared = shared) else it }

    /** Perubahan yang membuat isi melebihi batas panjang diabaikan. */
    private fun updateItems(transform: (List<ChecklistItem>) -> List<ChecklistItem>) = _uiState.update {
        val items = transform(it.items)
        if (it.canEdit && Checklist.format(items).length <= NoteRules.MAX_NOTE_LENGTH) {
            it.copy(items = items, showEmptyError = false)
        } else {
            it
        }
    }

    private fun String.oneLine() = replace('\n', ' ')

    fun onSave() {
        val state = _uiState.value
        if (state.isBusy || !state.canEdit) return
        val draft = state.draft
        val current = saved
        when {
            !NoteRules.isValidContent(draft.content) -> _uiState.update { it.copy(showEmptyError = true) }

            noteId == null -> run(EditorAction.Save) { noteRepository.create(groupId, draft) }

            current != null && draft == NoteDraft(current.content, current.color, current.kind, current.shared) ->
                _uiState.update { it.copy(isDone = true) }

            else -> run(EditorAction.Save) { noteRepository.update(noteId, draft) }
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
