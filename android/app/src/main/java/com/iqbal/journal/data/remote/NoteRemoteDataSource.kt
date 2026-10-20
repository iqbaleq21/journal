package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeOldRecord
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Perubahan satu catatan yang diterima lewat Realtime. */
sealed interface NoteChange {
    /** Channel sudah aktif; perubahan setelah titik ini pasti diterima. */
    data object Subscribed : NoteChange

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

    /** Catatan tertentu, termasuk yang sudah di-soft-delete, agar penghapusannya bisa diterapkan. */
    suspend fun fetchByIds(noteIds: Collection<String>): List<NoteDto> = client.from(Tables.NOTES)
        .select { filter { isIn("id", noteIds.toList()) } }
        .decodeList()

    suspend fun insert(noteId: String, groupId: String, fields: NoteFieldsDto): NoteDto = client
        .from(Tables.NOTES)
        .insert(
            NewNoteDto(
                id = noteId,
                groupId = groupId,
                authorId = currentUserId(),
                content = fields.content,
                color = fields.color,
                kind = fields.kind,
                shared = fields.shared
            )
        ) {
            select()
        }
        .decodeSingle()

    /** Null bila RLS menyaring catatannya (mis. akses berbagi dicabut penulis). */
    suspend fun updateContent(noteId: String, fields: NoteFieldsDto): NoteDto? = client.from(Tables.NOTES)
        .update(fields) {
            select()
            filter { eq("id", noteId) }
        }
        .decodeSingleOrNull()

    suspend fun softDelete(noteId: String, deletedAt: Instant) {
        client.from(Tables.NOTES).update({ set("deleted_at", deletedAt.toString()) }) {
            filter { eq("id", noteId) }
        }
    }

    /**
     * Semua perubahan catatan yang boleh saya lihat; RLS di server menyaring grupnya.
     * Channel dilepas saat pengumpul flow berhenti.
     */
    fun changes(): Flow<NoteChange> = channelFlow {
        // Topik unik: pelepasan channel lama (topik sama) yang masih berjalan bisa ikut menutup channel baru.
        val channel = client.channel("$CHANNEL_NOTES-${UUID.randomUUID()}")
        val actions = channel.postgresChangeFlow<PostgresAction>(schema = SCHEMA) { table = Tables.NOTES }
        val system = channel.systemFlow()
        try {
            // Dikumpulkan sebelum subscribe agar tidak ada pesan yang lolos.
            launch(start = CoroutineStart.UNDISPATCHED) {
                actions.collect { it.toNoteChange()?.let { change -> send(change) } }
            }
            launch(start = CoroutineStart.UNDISPATCHED) {
                // Join channel dibalas sebelum server mulai mengalirkan postgres_changes; perubahan di antara
                // keduanya hilang. Batas waktu menjaga agar refresh tetap jalan bila pesan ini tidak datang.
                withTimeoutOrNull(POSTGRES_READY_TIMEOUT_MILLIS) { system.first { it.extension == EXTENSION_POSTGRES } }
                send(NoteChange.Subscribed)
            }
            channel.subscribe(blockUntilSubscribed = true)
            // Channel bisa ditutup dari sisi lain tanpa error; akhiri flow agar pemanggil menyambung ulang.
            channel.status.first { it == RealtimeChannel.Status.UNSUBSCRIBED }
            throw IOException("Channel Realtime tertutup")
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

    /** Tersedia meski offline: sesi tersimpan di HP. */
    fun currentUserId(): String = checkNotNull(client.auth.currentUserOrNull()) { "Belum ada sesi" }.id

    private companion object {
        const val SCHEMA = "public"
        const val CHANNEL_NOTES = "notes"
        const val EXTENSION_POSTGRES = "postgres_changes"
        const val POSTGRES_READY_TIMEOUT_MILLIS = 10_000L
    }
}
