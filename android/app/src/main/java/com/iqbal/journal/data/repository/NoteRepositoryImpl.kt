package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.AuthorDao
import com.iqbal.journal.data.local.NoteDao
import com.iqbal.journal.data.remote.NoteChange
import com.iqbal.journal.data.remote.NoteDto
import com.iqbal.journal.data.remote.NoteRemoteDataSource
import com.iqbal.journal.data.remote.ProfileRemoteDataSource
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl @Inject constructor(
    private val remote: NoteRemoteDataSource,
    private val profileRemote: ProfileRemoteDataSource,
    private val noteDao: NoteDao,
    private val authorDao: AuthorDao,
    private val clock: Clock
) : NoteRepository {

    override fun notes(groupId: String): Flow<List<Note>> =
        noteDao.observeByGroup(groupId).map { list -> list.map { it.toDomain() } }

    override fun note(noteId: String): Flow<Note?> = noteDao.observeById(noteId).map { it?.toDomain() }

    override suspend fun refresh(groupId: String): Outcome<Unit> = outcomeOf {
        val notes = remote.fetchLatest(groupId, LOCAL_NOTES_PER_GROUP)
        noteDao.replaceForGroup(groupId, notes.map { it.toEntity() })
        // Nama bisa berubah di Pengaturan, jadi semua penulis diambil ulang, bukan hanya yang baru.
        cacheAuthors(notes.map { it.authorId }.toSet())
    }

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
        // Gagal mengambil nama tidak boleh memutus aliran Realtime; catatan tetap tampil tanpa nama.
        if (!authorDao.exists(note.authorId)) outcomeOf { cacheAuthors(setOf(note.authorId)) }
    }

    private suspend fun save(note: NoteDto): Note {
        apply(note)
        return checkNotNull(noteDao.observeById(note.id).first()) { "Catatan ${note.id} tidak tersimpan" }
            .toDomain()
    }

    private suspend fun cacheAuthors(ids: Set<String>) {
        if (ids.isEmpty()) return
        authorDao.upsertAll(profileRemote.fetchAuthors(ids).map { it.toEntity() })
    }

    companion object {
        /** Batas catatan per grup di HP (PRD §5.6); yang lebih lama tetap ada di server. */
        const val LOCAL_NOTES_PER_GROUP = 200L
    }
}
