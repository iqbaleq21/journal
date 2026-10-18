package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.NoteDao
import com.iqbal.journal.data.local.WidgetSelectionDao
import com.iqbal.journal.data.remote.NoteChange
import com.iqbal.journal.data.remote.NoteDto
import com.iqbal.journal.data.remote.NoteRemoteDataSource
import com.iqbal.journal.data.remote.outcomeOf
import com.iqbal.journal.data.toDomain
import com.iqbal.journal.data.toEntity
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.validation.NoteRules
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl @Inject constructor(
    private val remote: NoteRemoteDataSource,
    private val noteDao: NoteDao,
    private val authorCache: AuthorCache,
    private val widgetSelectionDao: WidgetSelectionDao,
    private val clock: Clock
) : NoteRepository {

    override val changes: Flow<Unit> = noteDao.observeVersion().distinctUntilChanged().map { }

    override fun notes(groupId: String): Flow<List<Note>> =
        noteDao.observeByGroup(groupId).map { list -> list.map { it.toDomain() } }

    override fun note(noteId: String): Flow<Note?> = noteDao.observeById(noteId).map { it?.toDomain() }

    override suspend fun refresh(groupId: String): Outcome<Unit> = outcomeOf {
        val notes = remote.fetchLatest(groupId, LOCAL_NOTES_PER_GROUP)
        noteDao.replaceForGroup(groupId, notes.map { it.toEntity() })
        refreshPinnedOutside(groupId, fetchedIds = notes.map { it.id }.toSet())
        // Nama bisa berubah di Pengaturan, jadi semua penulis diambil ulang, bukan hanya yang baru.
        authorCache.refresh(notes.map { it.authorId }.toSet())
    }

    override suspend fun syncNote(noteId: String): Outcome<Note?> = outcomeOf {
        val note = remote.fetchByIds(listOf(noteId)).firstOrNull()
        // Tidak dikembalikan server = sudah dihapus permanen.
        if (note == null) noteDao.deleteById(noteId) else apply(note)
        noteDao.observeById(noteId).first()?.toDomain()
    }

    override suspend fun trimCache() = noteDao.deleteBeyondLimit(LOCAL_NOTES_PER_GROUP)

    override suspend fun create(groupId: String, content: String): Outcome<Note> = outcomeOf {
        save(remote.insert(groupId, NoteRules.normalizeContent(content)))
    }

    override suspend fun update(noteId: String, content: String): Outcome<Note> = outcomeOf {
        save(remote.updateContent(noteId, NoteRules.normalizeContent(content)))
    }

    override suspend fun delete(noteId: String): Outcome<Unit> = outcomeOf {
        remote.softDelete(noteId, clock.instant())
        noteDao.deleteById(noteId)
    }

    override suspend fun listenForChanges(onSubscribed: suspend () -> Unit): Outcome<Unit> = outcomeOf {
        remote.changes().collect { change ->
            when (change) {
                NoteChange.Subscribed -> onSubscribed()
                is NoteChange.Saved -> apply(change.note)
                is NoteChange.Purged -> noteDao.deleteById(change.noteId)
            }
        }
    }

    private suspend fun apply(note: NoteDto) {
        if (note.deletedAt != null) {
            noteDao.deleteById(note.id)
            return
        }
        noteDao.upsert(note.toEntity())
        authorCache.ensureKnown(note.authorId)
    }

    /** Catatan widget di luar batas cache tidak ikut terambil, jadi statusnya dicek satu per satu. */
    private suspend fun refreshPinnedOutside(groupId: String, fetchedIds: Set<String>) {
        val pinned = widgetSelectionDao.pinnedNoteIds(groupId).toSet() - fetchedIds
        if (pinned.isEmpty()) return
        val found = remote.fetchByIds(pinned)
        found.forEach { apply(it) }
        // Tidak dikembalikan server = sudah dihapus permanen.
        (pinned - found.map { it.id }.toSet()).forEach { noteDao.deleteById(it) }
    }

    private suspend fun save(note: NoteDto): Note {
        apply(note)
        return checkNotNull(noteDao.observeById(note.id).first()) { "Catatan ${note.id} tidak tersimpan" }
            .toDomain()
    }

    companion object {
        /** Batas catatan per grup di HP (PRD §5.6); yang lebih lama tetap ada di server. */
        const val LOCAL_NOTES_PER_GROUP = 200L
    }
}
