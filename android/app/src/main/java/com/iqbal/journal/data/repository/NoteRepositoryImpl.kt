package com.iqbal.journal.data.repository

import com.iqbal.journal.data.local.NoteDao
import com.iqbal.journal.data.local.NoteEntity
import com.iqbal.journal.data.local.PendingOpEntity
import com.iqbal.journal.data.local.PendingOpType
import com.iqbal.journal.data.local.WidgetSelectionDao
import com.iqbal.journal.data.remote.NoteChange
import com.iqbal.journal.data.remote.NoteDto
import com.iqbal.journal.data.remote.NoteRemoteDataSource
import com.iqbal.journal.data.remote.outcomeOf
import com.iqbal.journal.data.toDomain
import com.iqbal.journal.data.toEntity
import com.iqbal.journal.data.toKey
import com.iqbal.journal.domain.model.Note
import com.iqbal.journal.domain.model.NoteDraft
import com.iqbal.journal.domain.model.Outcome
import com.iqbal.journal.domain.repository.NoteRepository
import com.iqbal.journal.domain.validation.NoteRules
import java.time.Clock
import java.util.UUID
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
    private val outbox: NoteOutbox,
    private val clock: Clock
) : NoteRepository {

    override val changes: Flow<Unit> = noteDao.observeVersion().distinctUntilChanged().map { }

    override fun notes(groupId: String): Flow<List<Note>> =
        noteDao.observeByGroup(groupId).map { list -> list.map { it.toDomain() } }

    override fun note(noteId: String): Flow<Note?> = noteDao.observeById(noteId).map { it?.toDomain() }

    override suspend fun refresh(groupId: String): Outcome<Unit> = outcomeOf {
        val notes = remote.fetchLatest(groupId, LOCAL_NOTES_PER_GROUP)
        // Catatan dengan perubahan lokal yang belum terkirim tidak ditimpa versi server.
        val pendingIds = outbox.pendingNoteIds()
        noteDao.replaceForGroup(groupId, notes.filterNot { it.id in pendingIds }.map { it.toEntity() })
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

    override suspend fun flushPending(): Boolean = outbox.flush()

    // Tulis/edit/hapus langsung ke cache (tampil di UI & widget), lalu dikirim lewat antrean; tetap jalan saat offline.
    override suspend fun create(groupId: String, draft: NoteDraft): Outcome<Note> = outcomeOf {
        val now = clock.millis()
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            authorId = remote.currentUserId(),
            content = NoteRules.normalizeContent(draft.content),
            color = draft.color.toKey(),
            kind = draft.kind.toKey(),
            shared = draft.shared,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )
        saveLocally(note, PendingOpType.Create)
    }

    override suspend fun update(noteId: String, draft: NoteDraft): Outcome<Note> = outcomeOf {
        val current = checkNotNull(noteDao.observeById(noteId).first()) { "Catatan $noteId tidak ada" }.note
        saveLocally(
            current.copy(
                content = NoteRules.normalizeContent(draft.content),
                color = draft.color.toKey(),
                kind = draft.kind.toKey(),
                shared = draft.shared,
                updatedAtEpochMillis = clock.millis()
            ),
            PendingOpType.Update
        )
    }

    override suspend fun delete(noteId: String): Outcome<Unit> = outcomeOf {
        val current = checkNotNull(noteDao.observeById(noteId).first()) { "Catatan $noteId tidak ada" }.note
        noteDao.deleteById(noteId)
        outbox.enqueue(
            PendingOpEntity(noteId = noteId, groupId = current.groupId, type = PendingOpType.Delete, content = null)
        )
        outbox.flush()
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
        // Perubahan lokal yang belum terkirim lebih baru dari versi server mana pun yang tiba sekarang.
        if (outbox.hasPending(note.id)) return
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

    private suspend fun saveLocally(note: NoteEntity, type: PendingOpType): Note {
        noteDao.upsert(note)
        outbox.enqueue(
            PendingOpEntity(
                noteId = note.id,
                groupId = note.groupId,
                type = type,
                content = note.content,
                color = note.color,
                kind = note.kind,
                shared = note.shared
            )
        )
        outbox.flush()
        return checkNotNull(noteDao.observeById(note.id).first()) { "Catatan ${note.id} tidak tersimpan" }.toDomain()
    }

    companion object {
        /** Batas catatan per grup di HP (PRD §5.6); yang lebih lama tetap ada di server. */
        const val LOCAL_NOTES_PER_GROUP = 200L
    }
}
