package com.iqbal.journal.widget

import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.repository.ProfileRepository
import com.iqbal.journal.domain.repository.WidgetRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Apa yang ditampilkan satu widget (PRD §5.4). */
sealed interface NoteWidgetState {
    data object NeedsProfile : NoteWidgetState
    data object NotConfigured : NoteWidgetState
    data object LeftGroup : NoteWidgetState
    data object NoteDeleted : NoteWidgetState

    /** Mode "catatan terbaru" untuk grup yang belum punya catatan. */
    data object NoNotesYet : NoteWidgetState
    data class Showing(val note: Note, val groupName: String, val isMine: Boolean) : NoteWidgetState
}

/** Menyusun state widget hanya dari cache lokal; widget tidak pernah mengakses jaringan. */
@OptIn(ExperimentalCoroutinesApi::class)
class NoteWidgetStateLoader @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val groupRepository: GroupRepository,
    private val noteRepository: NoteRepository,
    private val widgetRepository: WidgetRepository
) {
    /** Berubah setiap kali profil, pilihan widget, grup, atau catatannya berubah. */
    fun observe(appWidgetId: Int): Flow<NoteWidgetState> = combine(
        profileRepository.myProfile,
        widgetRepository.observeSelection(appWidgetId),
        groupRepository.groups,
        ::Triple
    ).flatMapLatest { (profile, selection, groups) ->
        val group = selection?.let { groups.firstOrNull { it.id == selection.groupId } }
        // Urutan pengecekan menentukan pesan yang tampil.
        when {
            profile == null -> flowOf(NoteWidgetState.NeedsProfile)

            selection == null -> flowOf(NoteWidgetState.NotConfigured)

            group == null -> flowOf(NoteWidgetState.LeftGroup)

            else -> {
                // US-16: tanpa catatan pilihan, widget mengikuti catatan yang terakhir diubah di grup.
                val noteFlow = selection.noteId?.let(noteRepository::note)
                    ?: noteRepository.notes(group.id).map { it.firstOrNull() }
                noteFlow.map { note ->
                    when {
                        note != null -> NoteWidgetState.Showing(
                            note = note,
                            groupName = group.name,
                            isMine = note.authorId == profile.id
                        )

                        selection.followsLatest -> NoteWidgetState.NoNotesYet

                        else -> NoteWidgetState.NoteDeleted
                    }
                }
            }
        }
    }.distinctUntilChanged()
}
