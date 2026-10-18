package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeOldRecord
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext

/** Perubahan satu catatan yang diterima lewat Realtime. */
sealed interface NoteChange {
    /** Catatan baru atau diubah; termasuk soft delete (lihat [NoteDto.deletedAt]). */
    data class Saved(val note: NoteDto) : NoteChange

    /** Catatan dihapus permanen oleh job pembersihan di server. */
    data class Purged(val noteId: String) : NoteChange
}

class NoteRemoteDataSource @Inject constructor(private val client: SupabaseClient) {
    /** Catatan yang belum dihapus, yang terakhir diubah lebih dulu. */
    suspend fun fetchLatest(groupId: String, limit: Long): List<NoteDto> = client.from(Tables.NOTES)
        .select {
            filter {
                eq("group_id", groupId)
                exact("deleted_at", null)
            }
            order("updated_at", Order.DESCENDING)
            limit(limit)
        }
        .decodeList()

    suspend fun insert(groupId: String, content: String): NoteDto = client.from(Tables.NOTES)
        .insert(NewNoteDto(groupId = groupId, authorId = currentUserId(), content = content)) { select() }
        .decodeSingle()

    suspend fun updateContent(noteId: String, content: String): NoteDto = client.from(Tables.NOTES)
        .update({ set("content", content) }) {
            select()
            filter { eq("id", noteId) }
        }
        .decodeSingle()

    suspend fun softDelete(noteId: String, deletedAt: Instant) {
        client.from(Tables.NOTES).update({ set("deleted_at", deletedAt.toString()) }) {
            filter { eq("id", noteId) }
        }
    }

    /**
     * Semua perubahan catatan yang boleh saya lihat; RLS di server menyaring grupnya.
     * Channel dilepas saat pengumpul flow berhenti.
     */
    fun changes(): Flow<NoteChange> = flow {
        val channel = client.channel(CHANNEL_NOTES)
        val actions = channel.postgresChangeFlow<PostgresAction>(schema = SCHEMA) { table = Tables.NOTES }
        channel.subscribe(blockUntilSubscribed = true)
        try {
            emitAll(actions.mapNotNull { it.toNoteChange() })
        } finally {
            withContext(NonCancellable) { client.realtime.removeChannel(channel) }
        }
    }

    private fun PostgresAction.toNoteChange(): NoteChange? = when (this) {
        is PostgresAction.Insert -> NoteChange.Saved(decodeRecord())
        is PostgresAction.Update -> NoteChange.Saved(decodeRecord())
        is PostgresAction.Delete -> NoteChange.Purged(decodeOldRecord<NoteIdDto>().id)
        is PostgresAction.Select -> null
    }

    private fun currentUserId(): String = checkNotNull(client.auth.currentUserOrNull()) { "Belum ada sesi" }.id

    private companion object {
        const val SCHEMA = "public"
        const val CHANNEL_NOTES = "notes"
    }
}
