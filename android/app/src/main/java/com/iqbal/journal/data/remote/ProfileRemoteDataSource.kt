package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

class ProfileRemoteDataSource @Inject constructor(private val client: SupabaseClient) {
    /** Id pengguna saat ini; membuat akun anonim jika HP ini belum punya sesi. */
    suspend fun ensureSignedIn(): String {
        val auth = client.auth
        auth.awaitInitialization()
        auth.currentUserOrNull()?.let { return it.id }
        auth.signInAnonymously()
        return checkNotNull(auth.currentUserOrNull()) { "Sesi anonim tidak terbentuk" }.id
    }

    suspend fun upsertProfile(profile: ProfileDto) {
        client.from(Tables.PROFILES).upsert(profile)
    }

    /** Nama pengguna lain; RLS hanya mengembalikan yang satu grup dengan saya. */
    suspend fun fetchAuthors(ids: Collection<String>): List<AuthorDto> = client.from(Tables.PROFILES)
        .select(Columns.list("id", "display_name")) { filter { isIn("id", ids.toList()) } }
        .decodeList()
}
