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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class EditorAction { Save, Delete }

data class EditorUiState(
    val groupName: String = "",
    val title: String = "",
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
    val isSettingsOpen: Boolean = false,
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

    val draft: NoteDraft get() = NoteDraft(
        content = body,
        title = NoteRules.normalizeTitle(title),
        color = color,
        kind = kind,
        shared = shared
    )
    val isBusy: Boolean get() = pendingAction != null
}

@OptIn(FlowPreview::class)
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

    /** Null sampai catatan baru tersimpan pertama kali oleh simpan otomatis. */
    private var noteId: String? = savedStateHandle[ARG_NOTE_ID]

    private val _uiState = MutableStateFlow(EditorUiState(isNew = noteId == null, isLoading = noteId != null))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** Versi tersimpan terakhir, untuk melewati simpan bila tidak ada perubahan. */
    private var saved: Note? = null
    private val saveMutex = Mutex()

    init {
        viewModelScope.launch {
            groupRepository.groups.collect { groups ->
                _uiState.update { state ->
                    state.copy(groupName = groups.firstOrNull { it.id == groupId }?.name.orEmpty())
                }
            }
        }
        noteId?.let(::observeNote)
        viewModelScope.launch {
            uiState.map { it.draft }.distinctUntilChanged().debounce(AUTOSAVE_DELAY_MILLIS).collect { save() }
        }
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
                val isFirstLoad = saved == null
                saved = note
                _uiState.update {
                    // Isian yang belum disimpan tidak ditimpa selama masih boleh diedit.
                    val keepTyped = !isFirstLoad && it.canEdit && (isMine || note.shared)
                    val typed = if (keepTyped) {
                        it
                    } else {
                        it.copy(
                            title = note.title,
                            content = note.content,
                            items = Checklist.parse(note.content),
                            kind = note.kind,
                            color = note.color,
                            shared = note.shared
                        )
                    }
                    typed.copy(
                        // Hanya penulis yang mengatur akses; bagi anggota lain nilai server selalu berlaku.
                        shared = if (isMine) typed.shared else note.shared,
                        isLoading = false,
                        isMine = isMine,
                        authorName = note.authorName,
                        updatedAt = note.updatedAt
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) = _uiState.update {
        if (it.canEdit) it.copy(title = title.replace('\n', ' ').take(NoteRules.MAX_TITLE_LENGTH)) else it
    }

    fun onContentChange(content: String) = _uiState.update {
        if (it.canEdit) it.copy(content = content.take(NoteRules.MAX_NOTE_LENGTH)) else it
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
            it.copy(items = items)
        } else {
            it
        }
    }

    private fun String.oneLine() = replace('\n', ' ')

    /** Kembali: perubahan terakhir disimpan dulu; catatan baru yang kosong dibuang. */
    fun onLeave() {
        val state = _uiState.value
        if (state.isBusy) return
        _uiState.update { it.copy(pendingAction = EditorAction.Save) }
        viewModelScope.launch {
            when (val result = save()) {
                is Outcome.Failure -> _uiState.update { it.copy(pendingAction = null, error = result.error) }
                else -> _uiState.update { it.copy(pendingAction = null, isDone = true) }
            }
        }
    }

    /**
     * Null bila tidak ada yang perlu dikirim: tidak boleh diedit, tidak berubah, atau isi kosong (catatan lama
     * yang dikosongkan tetap memakai versi tersimpan; hapus lewat Pengaturan catatan).
     */
    private suspend fun save(): Outcome<Note>? = withContext(NonCancellable) {
        // Simpan otomatis dan tombol kembali bisa bersamaan; catatan baru tidak boleh terbuat dua kali.
        saveMutex.withLock {
            val state = _uiState.value
            val draft = state.draft
            val id = noteId
            val current = saved
            when {
                !state.canEdit || !NoteRules.isValidContent(draft.content) -> null

                id == null -> noteRepository.create(groupId, draft).also { result ->
                    if (result is Outcome.Success) {
                        // Diisi sebelum observasi agar ketikan setelah simpan tidak ditimpa isi dari cache.
                        saved = result.value
                        noteId = result.value.id
                        _uiState.update { it.copy(isNew = false) }
                        observeNote(result.value.id)
                    }
                }

                current != null && draft == current.toDraft() -> null

                else -> noteRepository.update(id, draft)
            }?.also { result -> if (result is Outcome.Failure) _uiState.update { it.copy(error = result.error) } }
        }
    }

    private fun Note.toDraft() = NoteDraft(content, title, color, kind, shared)

    fun onSettingsClick() = _uiState.update { if (it.canEdit) it.copy(isSettingsOpen = true) else it }

    fun onSettingsDismiss() = _uiState.update { it.copy(isSettingsOpen = false) }

    fun onDeleteClick() = _uiState.update {
        if (it.canDelete &&
            !it.isBusy
        ) {
            it.copy(isSettingsOpen = false, isDeleteConfirmOpen = true)
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

        /** Jeda setelah berhenti mengetik sebelum disimpan; tiap simpan mengirim push ke anggota grup. */
        const val AUTOSAVE_DELAY_MILLIS = 1_500L
    }
}
