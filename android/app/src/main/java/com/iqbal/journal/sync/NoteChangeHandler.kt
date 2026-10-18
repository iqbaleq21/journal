package com.iqbal.journal.sync

import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Pesan push `note_changed` dari Edge Function notify-members. */
data class NoteChangedMessage(val noteId: String, val isNew: Boolean, val notify: Boolean)

/** Menerapkan satu perubahan catatan yang dikabarkan lewat push. */
class NoteChangeHandler @Inject constructor(
    private val noteRepository: NoteRepository,
    private val groupRepository: GroupRepository,
    private val notifier: NoteNotifier
) {
    suspend fun handle(message: NoteChangedMessage): Outcome<Unit> =
        when (val result = noteRepository.syncNote(message.noteId)) {
            is Outcome.Failure -> result

            is Outcome.Success -> {
                val note = result.value
                // Hanya catatan baru yang dinotifikasi (bukan edit), dan hanya jika grupnya tidak dibisukan.
                if (note != null && message.isNew && message.notify) {
                    val groupName = groupRepository.groups.first().firstOrNull { it.id == note.groupId }?.name
                    notifier.notifyNewNote(note, groupName.orEmpty())
                }
                Outcome.Success(Unit)
            }
        }
}
