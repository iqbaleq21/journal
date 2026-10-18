package com.iqbal.journal.sync

import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.GroupRepository
import com.iqbal.journal.domain.repository.NoteRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Ambil ulang daftar grup lalu catatan setiap grup; dipakai saat app dibuka dan oleh sinkron periodik. */
class FullSync @Inject constructor(
    private val groupRepository: GroupRepository,
    private val noteRepository: NoteRepository
) {
    /** @return true bila semua langkah berhasil. */
    suspend fun run(): Boolean {
        val groupsOk = groupRepository.refresh() is Outcome.Success
        val notesOk = groupRepository.groups.first().map { noteRepository.refresh(it.id) }
            .all { it is Outcome.Success }
        return groupsOk && notesOk
    }
}
