package com.iqbal.journal.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRemoteDataSource @Inject constructor(private val client: SupabaseClient) {
    /** App tidak pernah logout sendiri, jadi tanpa sesi berarti belum masuk atau sesinya dicabut server. */
    val isSignedOut: Flow<Boolean> = client.auth.sessionStatus.map { it is SessionStatus.NotAuthenticated }

    /** Id pengguna saat ini; membuat akun anonim jika HP ini belum punya sesi. */
    suspend fun ensureSignedIn(): String {
        val auth = client.auth
        auth.awaitInitialization()
        auth.currentUserOrNull()?.let { return it.id }
        auth.signInAnonymously()
        return checkNotNull(auth.currentUserOrNull()) { "Sesi anonim tidak terbentuk" }.id
    }

    suspend fun upsertProfile(profile: ProfileDto) {
        client.withSession().from(Tables.PROFILES).upsert(profile)
    }

    /** Nama pengguna lain; RLS hanya mengembalikan yang satu grup dengan saya. */
    suspend fun fetchAuthors(ids: Collection<String>): List<AuthorDto> = client.withSession().from(Tables.PROFILES)
        .select(Columns.list("id", "display_name")) { filter { isIn("id", ids.toList()) } }
        .decodeList()
}
